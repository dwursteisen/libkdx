/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.badlogic.gdx.utils

import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.utils.ObjectIntMap
import com.badlogic.gdx.utils.ObjectSet
import com.badlogic.gdx.utils.ObjectSet.ObjectSetIterator
import com.badlogic.gdx.utils.OrderedMap
import com.badlogic.gdx.utils.OrderedMap.OrderedMapEntries
import com.badlogic.gdx.utils.OrderedMap.OrderedMapKeys
import com.badlogic.gdx.utils.OrderedMap.OrderedMapValues
import com.badlogic.gdx.utils.OrderedSet
import com.badlogic.gdx.utils.OrderedSet.OrderedSetIterator
import java.lang.IllegalStateException
import java.util.NoSuchElementException

/**
 * An unordered map. This implementation is a cuckoo hash map using 3 hashes, random walking, and a small stash for problematic
 * keys. Null keys are not allowed. Null values are allowed. No allocation is done except when growing the table size. <br></br>
 * <br></br>
 * This map performs very fast get, containsKey, and remove (typically O(1), worst case O(log(n))). Put may be a bit slower,
 * depending on hash collisions. Load factors greater than 0.91 greatly increase the chances the map will have to rehash to the
 * next higher POT size.<br></br>
 * <br></br>
 * Iteration can be very slow for a map with a large capacity. [.clear] and [.shrink] can be used to reduce
 * the capacity. [OrderedMap] provides much faster iteration.
 *
 * @author Nathan Sweet
 */
class ObjectMap<K, V> @JvmOverloads constructor(initialCapacity: Int = 51, loadFactor: Float = 0.8f) : Iterable<ObjectMap.Entry<K, V>?> {

    var size = 0
    var keyTable: Array<K?>
    var valueTable: Array<V?>
    var capacity: Int
    var stashSize = 0
    private val loadFactor: Float
    private var hashShift: Int
    private var mask: Int
    private var threshold: Int
    private var stashCapacity: Int
    private var pushIterations: Int
    var entries1: Entries<*, *>? = null
    var entries2: Entries<*, *>? = null
    var values1: Values<*>? = null
    var values2: Values<*>? = null
    var keys1: Keys<*>? = null
    var keys2: Keys<*>? = null

    /**
     * Creates a new map identical to the specified map.
     */
    constructor(map: ObjectMap<out K, out V>) : this(java.lang.Math.floor(map.capacity * map.loadFactor.toDouble()) as Int, map.loadFactor) {
        stashSize = map.stashSize
        java.lang.System.arraycopy(map.keyTable, 0, keyTable, 0, map.keyTable.size)
        java.lang.System.arraycopy(map.valueTable, 0, valueTable, 0, map.valueTable.size)
        size = map.size
    }

    /**
     * Returns the old value associated with the specified key, or null.
     */
    fun put(key: K?, value: V): V? {
        if (key == null) throw IllegalArgumentException("key cannot be null.")
        val keyTable: Array<K> = keyTable

        // Check for existing keys.
        val hashCode = key.hashCode()
        val index1 = hashCode and mask
        val key1: K? = keyTable[index1]
        if (key == key1) {
            val oldValue = valueTable[index1]
            valueTable[index1] = value
            return oldValue
        }
        val index2 = hash2(hashCode)
        val key2: K? = keyTable[index2]
        if (key == key2) {
            val oldValue = valueTable[index2]
            valueTable[index2] = value
            return oldValue
        }
        val index3 = hash3(hashCode)
        val key3: K? = keyTable[index3]
        if (key == key3) {
            val oldValue = valueTable[index3]
            valueTable[index3] = value
            return oldValue
        }

        // Update key in the stash.
        var i = capacity
        val n = i + stashSize
        while (i < n) {
            if (key == keyTable[i]) {
                val oldValue = valueTable[i]
                valueTable[i] = value
                return oldValue
            }
            i++
        }

        // Check for empty buckets.
        if (key1 == null) {
            keyTable[index1] = key
            valueTable[index1] = value
            if (size++ >= threshold) resize(capacity shl 1)
            return null
        }
        if (key2 == null) {
            keyTable[index2] = key
            valueTable[index2] = value
            if (size++ >= threshold) resize(capacity shl 1)
            return null
        }
        if (key3 == null) {
            keyTable[index3] = key
            valueTable[index3] = value
            if (size++ >= threshold) resize(capacity shl 1)
            return null
        }
        push(key, value, index1, key1, index2, key2, index3, key3)
        return null
    }

    fun putAll(map: ObjectMap<out K, out V>) {
        ensureCapacity(map.size)
        for (entry in map) put(entry.key, entry.value)
    }

    /**
     * Skips checks for existing keys.
     */
    private fun putResize(key: K?, value: V?) {
        // Check for empty buckets.
        val hashCode = key.hashCode()
        val index1 = hashCode and mask
        val key1 = keyTable[index1]
        if (key1 == null) {
            keyTable[index1] = key
            valueTable[index1] = value
            if (size++ >= threshold) resize(capacity shl 1)
            return
        }
        val index2 = hash2(hashCode)
        val key2 = keyTable[index2]
        if (key2 == null) {
            keyTable[index2] = key
            valueTable[index2] = value
            if (size++ >= threshold) resize(capacity shl 1)
            return
        }
        val index3 = hash3(hashCode)
        val key3 = keyTable[index3]
        if (key3 == null) {
            keyTable[index3] = key
            valueTable[index3] = value
            if (size++ >= threshold) resize(capacity shl 1)
            return
        }
        push(key, value, index1, key1, index2, key2, index3, key3)
    }

    private fun push(insertKey: K?, insertValue: V?, index1: Int, key1: K, index2: Int, key2: K, index3: Int, key3: K) {
        var insertKey = insertKey
        var insertValue = insertValue
        var index1 = index1
        var key1: K? = key1
        var index2 = index2
        var key2: K? = key2
        var index3 = index3
        var key3: K? = key3
        val keyTable = keyTable
        val valueTable = valueTable
        val mask = mask

        // Push keys until an empty bucket is found.
        var evictedKey: K?
        var evictedValue: V?
        var i = 0
        val pushIterations = pushIterations
        do {
            // Replace the key and value for one of the hashes.
            when (MathUtils.random(2)) {
                0 -> {
                    evictedKey = key1
                    evictedValue = valueTable[index1]
                    keyTable[index1] = insertKey
                    valueTable[index1] = insertValue
                }
                1 -> {
                    evictedKey = key2
                    evictedValue = valueTable[index2]
                    keyTable[index2] = insertKey
                    valueTable[index2] = insertValue
                }
                else -> {
                    evictedKey = key3
                    evictedValue = valueTable[index3]
                    keyTable[index3] = insertKey
                    valueTable[index3] = insertValue
                }
            }

            // If the evicted key hashes to an empty bucket, put it there and stop.
            val hashCode = evictedKey.hashCode()
            index1 = hashCode and mask
            key1 = keyTable[index1]
            if (key1 == null) {
                keyTable[index1] = evictedKey
                valueTable[index1] = evictedValue
                if (size++ >= threshold) resize(capacity shl 1)
                return
            }
            index2 = hash2(hashCode)
            key2 = keyTable[index2]
            if (key2 == null) {
                keyTable[index2] = evictedKey
                valueTable[index2] = evictedValue
                if (size++ >= threshold) resize(capacity shl 1)
                return
            }
            index3 = hash3(hashCode)
            key3 = keyTable[index3]
            if (key3 == null) {
                keyTable[index3] = evictedKey
                valueTable[index3] = evictedValue
                if (size++ >= threshold) resize(capacity shl 1)
                return
            }
            if (++i == pushIterations) break
            insertKey = evictedKey
            insertValue = evictedValue
        } while (true)
        putStash(evictedKey, evictedValue)
    }

    private fun putStash(key: K?, value: V?) {
        if (stashSize == stashCapacity) {
            // Too many pushes occurred and the stash is full, increase the table size.
            resize(capacity shl 1)
            putResize(key, value)
            return
        }
        // Store key in the stash.
        val index = capacity + stashSize
        keyTable[index] = key
        valueTable[index] = value
        stashSize++
        size++
    }

    /**
     * Returns the value (which may be null) for the specified key, or null if the key is not in the map.
     */
    operator fun get(key: K): V? {
        val hashCode = key.hashCode()
        var index = hashCode and mask
        if (key != keyTable[index]) {
            index = hash2(hashCode)
            if (key != keyTable[index]) {
                index = hash3(hashCode)
                if (key != keyTable[index]) return getStash(key, null)
            }
        }
        return valueTable[index]
    }

    /**
     * Returns the value (which may be null) for the specified key, or the default value if the key is not in the map.
     */
    operator fun get(key: K, defaultValue: V): V? {
        val hashCode = key.hashCode()
        var index = hashCode and mask
        if (key != keyTable[index]) {
            index = hash2(hashCode)
            if (key != keyTable[index]) {
                index = hash3(hashCode)
                if (key != keyTable[index]) return getStash(key, defaultValue)
            }
        }
        return valueTable[index]
    }

    private fun getStash(key: K, defaultValue: V?): V? {
        val keyTable: Array<K> = keyTable
        var i = capacity
        val n = i + stashSize
        while (i < n) {
            if (key == keyTable[i]) return valueTable[i]
            i++
        }
        return defaultValue
    }

    /**
     * Returns the value associated with the key, or null.
     */
    fun remove(key: K): V? {
        val hashCode = key.hashCode()
        var index = hashCode and mask
        if (key == keyTable[index]) {
            keyTable[index] = null
            val oldValue = valueTable[index]
            valueTable[index] = null
            size--
            return oldValue
        }
        index = hash2(hashCode)
        if (key == keyTable[index]) {
            keyTable[index] = null
            val oldValue = valueTable[index]
            valueTable[index] = null
            size--
            return oldValue
        }
        index = hash3(hashCode)
        if (key == keyTable[index]) {
            keyTable[index] = null
            val oldValue = valueTable[index]
            valueTable[index] = null
            size--
            return oldValue
        }
        return removeStash(key)
    }

    fun removeStash(key: K): V? {
        val keyTable: Array<K> = keyTable
        var i = capacity
        val n = i + stashSize
        while (i < n) {
            if (key == keyTable[i]) {
                val oldValue = valueTable[i]
                removeStashIndex(i)
                size--
                return oldValue
            }
            i++
        }
        return null
    }

    fun removeStashIndex(index: Int) {
        // If the removed location was not last, move the last tuple to the removed location.
        stashSize--
        val lastIndex = capacity + stashSize
        if (index < lastIndex) {
            keyTable[index] = keyTable[lastIndex]
            valueTable[index] = valueTable[lastIndex]
            keyTable[lastIndex] = null
            valueTable[lastIndex] = null
        } else {
            keyTable[index] = null
            valueTable[index] = null
        }
    }

    /**
     * Returns true if the map has one or more items.
     */
    fun notEmpty(): Boolean {
        return size > 0
    }

    /**
     * Returns true if the map is empty.
     */
    val isEmpty: Boolean
        get() = size == 0

    /**
     * Reduces the size of the backing arrays to be the specified capacity or less. If the capacity is already less, nothing is
     * done. If the map contains more items than the specified capacity, the next highest power of two capacity is used instead.
     */
    fun shrink(maximumCapacity: Int) {
        var maximumCapacity = maximumCapacity
        if (maximumCapacity < 0) throw IllegalArgumentException("maximumCapacity must be >= 0: $maximumCapacity")
        if (size > maximumCapacity) maximumCapacity = size
        if (capacity <= maximumCapacity) return
        maximumCapacity = MathUtils.nextPowerOfTwo(maximumCapacity)
        resize(maximumCapacity)
    }

    /**
     * Clears the map and reduces the size of the backing arrays to be the specified capacity, if they are larger. The reduction
     * is done by allocating new arrays, though for large arrays this can be faster than clearing the existing array.
     */
    fun clear(maximumCapacity: Int) {
        if (capacity <= maximumCapacity) {
            clear()
            return
        }
        size = 0
        resize(maximumCapacity)
    }

    /**
     * Clears the map, leaving the backing arrays at the current capacity. When the capacity is high and the population is low,
     * iteration can be unnecessarily slow. [.clear] can be used to reduce the capacity.
     */
    fun clear() {
        if (size == 0) return
        val keyTable = keyTable
        val valueTable = valueTable
        var i = capacity + stashSize
        while (i-- > 0) {
            keyTable[i] = null
            valueTable[i] = null
        }
        size = 0
        stashSize = 0
    }

    /**
     * Returns true if the specified value is in the map. Note this traverses the entire map and compares every value, which may
     * be an expensive operation.
     *
     * @param identity If true, uses == to compare the specified value with values in the map. If false, uses
     * [.equals].
     */
    fun containsValue(value: Any?, identity: Boolean): Boolean {
        val valueTable = valueTable
        if (value == null) {
            val keyTable = keyTable
            var i = capacity + stashSize
            while (i-- > 0) {
                if (keyTable[i] != null && valueTable[i] == null) return true
            }
        } else if (identity) {
            var i = capacity + stashSize
            while (i-- > 0) {
                if (valueTable[i] === value) return true
            }
        } else {
            var i = capacity + stashSize
            while (i-- > 0) {
                if (value == valueTable[i]) return true
            }
        }
        return false
    }

    fun containsKey(key: K): Boolean {
        val hashCode = key.hashCode()
        var index = hashCode and mask
        if (key != keyTable[index]) {
            index = hash2(hashCode)
            if (key != keyTable[index]) {
                index = hash3(hashCode)
                if (key != keyTable[index]) return containsKeyStash(key)
            }
        }
        return true
    }

    private fun containsKeyStash(key: K): Boolean {
        val keyTable: Array<K> = keyTable
        var i = capacity
        val n = i + stashSize
        while (i < n) {
            if (key == keyTable[i]) return true
            i++
        }
        return false
    }

    /**
     * Returns the key for the specified value, or null if it is not in the map. Note this traverses the entire map and compares
     * every value, which may be an expensive operation.
     *
     * @param identity If true, uses == to compare the specified value with values in the map. If false, uses
     * [.equals].
     */
    fun findKey(value: Any?, identity: Boolean): K? {
        val valueTable = valueTable
        if (value == null) {
            val keyTable = keyTable
            var i = capacity + stashSize
            while (i-- > 0) {
                if (keyTable[i] != null && valueTable[i] == null) return keyTable[i]
            }
        } else if (identity) {
            var i = capacity + stashSize
            while (i-- > 0) {
                if (valueTable[i] === value) return keyTable[i]
            }
        } else {
            var i = capacity + stashSize
            while (i-- > 0) {
                if (value == valueTable[i]) return keyTable[i]
            }
        }
        return null
    }

    /**
     * Increases the size of the backing array to accommodate the specified number of additional items. Useful before adding many
     * items to avoid multiple backing array resizes.
     */
    fun ensureCapacity(additionalCapacity: Int) {
        if (additionalCapacity < 0) throw IllegalArgumentException("additionalCapacity must be >= 0: $additionalCapacity")
        val sizeNeeded = size + additionalCapacity
        if (sizeNeeded >= threshold) resize(MathUtils.nextPowerOfTwo(java.lang.Math.ceil(sizeNeeded / loadFactor.toDouble()) as Int))
    }

    private fun resize(newSize: Int) {
        val oldEndIndex = capacity + stashSize
        capacity = newSize
        threshold = (newSize * loadFactor).toInt()
        mask = newSize - 1
        hashShift = 31 - java.lang.Integer.numberOfTrailingZeros(newSize)
        stashCapacity = max(3, java.lang.Math.ceil(java.lang.Math.log(newSize.toDouble())) as Int * 2)
        pushIterations = max(java.lang.Math.min(newSize, 8), java.lang.Math.sqrt(newSize.toDouble()) as Int / 8)
        val oldKeyTable = keyTable
        val oldValueTable = valueTable
        keyTable = arrayOfNulls<Any>(newSize + stashCapacity) as Array<K?>
        valueTable = arrayOfNulls<Any>(newSize + stashCapacity) as Array<V?>
        val oldSize = size
        size = 0
        stashSize = 0
        if (oldSize > 0) {
            for (i in 0 until oldEndIndex) {
                val key = oldKeyTable[i]
                if (key != null) putResize(key, oldValueTable[i])
            }
        }
    }

    private fun hash2(h: Int): Int {
        var h = h
        h *= PRIME2
        return h xor h ushr hashShift and mask
    }

    private fun hash3(h: Int): Int {
        var h = h
        h *= PRIME3
        return h xor h ushr hashShift and mask
    }

    override fun hashCode(): Int {
        var h = 0
        val keyTable: Array<K> = keyTable
        val valueTable: Array<V> = valueTable
        var i = 0
        val n = capacity + stashSize
        while (i < n) {
            val key: K? = keyTable[i]
            if (key != null) {
                h += key.hashCode() * 31
                val value: V? = valueTable[i]
                if (value != null) {
                    h += value.hashCode()
                }
            }
            i++
        }
        return h
    }

    override fun equals(obj: Any?): Boolean {
        if (obj === this) return true
        if (obj !is ObjectMap<*, *>) return false
        val other = obj
        if (other.size != size) return false
        val keyTable: Array<K> = keyTable
        val valueTable: Array<V> = valueTable
        var i = 0
        val n = capacity + stashSize
        while (i < n) {
            val key: K? = keyTable[i]
            if (key != null) {
                val value: V? = valueTable[i]
                if (value == null) {
                    if (other.get(key, dummy) != null) return false
                } else {
                    if (value != other.get(key)) return false
                }
            }
            i++
        }
        return true
    }

    /**
     * Uses == for comparison of each value.
     */
    fun equalsIdentity(obj: Any): Boolean {
        if (obj === this) return true
        if (obj !is IdentityMap) return false
        val other: IdentityMap = obj as IdentityMap
        if (other.size !== size) return false
        val keyTable: Array<K> = keyTable
        val valueTable: Array<V> = valueTable
        var i = 0
        val n = capacity + stashSize
        while (i < n) {
            val key: K? = keyTable[i]
            if (key != null && valueTable[i] !== other.get(key, dummy)) return false
            i++
        }
        return true
    }

    fun toString(separator: String): String {
        return toString(separator, false)
    }

    override fun toString(): String {
        return toString(", ", true)
    }

    private fun toString(separator: String, braces: Boolean): String {
        if (size == 0) return if (braces) "{}" else ""
        val buffer: java.lang.StringBuilder = java.lang.StringBuilder(32)
        if (braces) buffer.append('{')
        val keyTable: Array<K> = keyTable
        val valueTable: Array<V> = valueTable
        var i = keyTable.size
        while (i-- > 0) {
            val key: K = keyTable[i] ?: continue
            buffer.append(key)
            buffer.append('=')
            buffer.append(valueTable[i])
            break
        }
        while (i-- > 0) {
            val key: K = keyTable[i] ?: continue
            buffer.append(separator)
            buffer.append(key)
            buffer.append('=')
            buffer.append(valueTable[i])
        }
        if (braces) buffer.append('}')
        return buffer.toString()
    }

    override fun iterator(): Iterator<Entry<K, V>?> {
        return entries()!!
    }

    /**
     * Returns an iterator for the entries in the map. Remove is supported.
     *
     *
     * If [Collections.allocateIterators] is false, the same iterator instance is returned each time this method is called.
     * Use the [Entries] constructor for nested or multithreaded iteration.
     */
    fun entries(): Entries<K, V?>? {
        if (Collections.allocateIterators) return Entries<Any?, Any?>(this)
        if (entries1 == null) {
            entries1 = Entries<Any?, Any?>(this)
            entries2 = Entries<Any?, Any?>(this)
        }
        if (!entries1.valid) {
            entries1.reset()
            entries1.valid = true
            entries2!!.valid = false
            return entries1
        }
        entries2!!.reset()
        entries2!!.valid = true
        entries1.valid = false
        return entries2
    }

    /**
     * Returns an iterator for the values in the map. Remove is supported.
     *
     *
     * If [Collections.allocateIterators] is false, the same iterator instance is returned each time this method is called.
     * Use the [Values] constructor for nested or multithreaded iteration.
     */
    fun values(): Values<V?>? {
        if (Collections.allocateIterators) return Values<Any?>(this)
        if (values1 == null) {
            values1 = Values<Any?>(this)
            values2 = Values<Any?>(this)
        }
        if (!values1.valid) {
            values1.reset()
            values1.valid = true
            values2!!.valid = false
            return values1
        }
        values2!!.reset()
        values2!!.valid = true
        values1.valid = false
        return values2
    }

    /**
     * Returns an iterator for the keys in the map. Remove is supported.
     *
     *
     * If [Collections.allocateIterators] is false, the same iterator instance is returned each time this method is called.
     * Use the [Keys] constructor for nested or multithreaded iteration.
     */
    fun keys(): Keys<K?>? {
        if (Collections.allocateIterators) return Keys<Any?>(this)
        if (keys1 == null) {
            keys1 = Keys<Any?>(this)
            keys2 = Keys<Any?>(this)
        }
        if (!keys1.valid) {
            keys1.reset()
            keys1.valid = true
            keys2!!.valid = false
            return keys1
        }
        keys2!!.reset()
        keys2!!.valid = true
        keys1.valid = false
        return keys2
    }

    class Entry<K, V> {
        var key: K? = null
        var value: V? = null
        override fun toString(): String {
            return key.toString() + "=" + value
        }
    }

    abstract class MapIterator<K, V, I>(val map: ObjectMap<K, V>) : Iterable<I>, MutableIterator<I> {
        var hasNext = false
        var nextIndex = 0
        var currentIndex = 0
        var valid = true
        fun reset() {
            currentIndex = -1
            nextIndex = -1
            findNextIndex()
        }

        fun findNextIndex() {
            hasNext = false
            val keyTable: Array<K> = map.keyTable
            val n = map.capacity + map.stashSize
            while (++nextIndex < n) {
                if (keyTable[nextIndex] != null) {
                    hasNext = true
                    break
                }
            }
        }

        override fun remove() {
            check(currentIndex >= 0) { "next must be called before remove." }
            if (currentIndex >= map.capacity) {
                map.removeStashIndex(currentIndex)
                nextIndex = currentIndex - 1
                findNextIndex()
            } else {
                map.keyTable[currentIndex] = null
                map.valueTable[currentIndex] = null
            }
            currentIndex = -1
            map.size--
        }

        init {
            reset()
        }
    }

    class Entries<K, V>(map: ObjectMap<K, V>) : MapIterator<K, V, Entry<K, V>?>(map) {
        var entry: Entry<K?, V?> = Entry<Any?, Any?>()

        /**
         * Note the same entry instance is returned each time this method is called.
         */
        override fun next(): Entry<K, V>? {
            if (!hasNext) throw NoSuchElementException()
            if (!valid) throw GdxRuntimeException("#iterator() cannot be used nested.")
            val keyTable: Array<K> = map.keyTable
            entry.key = keyTable[nextIndex]
            entry.value = map.valueTable[nextIndex]
            currentIndex = nextIndex
            findNextIndex()
            return entry
        }

        override fun hasNext(): Boolean {
            if (!valid) throw GdxRuntimeException("#iterator() cannot be used nested.")
            return hasNext
        }

        override fun iterator(): Entries<K, V> {
            return this
        }
    }

    class Values<V>(map: ObjectMap<*, V>) : MapIterator<Any?, V, V>(map as ObjectMap<Any, V>) {
        override fun hasNext(): Boolean {
            if (!valid) throw GdxRuntimeException("#iterator() cannot be used nested.")
            return hasNext
        }

        override fun next(): V {
            if (!hasNext) throw NoSuchElementException()
            if (!valid) throw GdxRuntimeException("#iterator() cannot be used nested.")
            val value: V = map.valueTable[nextIndex]
            currentIndex = nextIndex
            findNextIndex()
            return value
        }

        override fun iterator(): Values<V> {
            return this
        }

        /**
         * Returns a new array containing the remaining values.
         */
        fun toArray(): Array<V> {
            return toArray(Array(true, map.size))
        }

        /**
         * Adds the remaining values to the specified array.
         */
        fun toArray(array: Array<V>): Array<V> {
            while (hasNext) array.add(next())
            return array
        }
    }

    class Keys<K>(map: ObjectMap<K, *>) : MapIterator<K, Any?, K>(map as ObjectMap<K, Any>) {
        override fun hasNext(): Boolean {
            if (!valid) throw GdxRuntimeException("#iterator() cannot be used nested.")
            return hasNext
        }

        override fun next(): K {
            if (!hasNext) throw NoSuchElementException()
            if (!valid) throw GdxRuntimeException("#iterator() cannot be used nested.")
            val key: K = map.keyTable[nextIndex]
            currentIndex = nextIndex
            findNextIndex()
            return key
        }

        override fun iterator(): Keys<K> {
            return this
        }

        /**
         * Returns a new array containing the remaining keys.
         */
        fun toArray(): Array<K> {
            return toArray(Array(true, map.size))
        }

        /**
         * Adds the remaining keys to the array.
         */
        fun toArray(array: Array<K>): Array<K> {
            while (hasNext) array.add(next())
            return array
        }
    }

    companion object {
        private const val PRIME1 = -0x41e0eb4f
        private const val PRIME2 = -0x4b47d1c7
        private const val PRIME3 = -0x312e3dbf
        val dummy = Any()
    }
    /**
     * Creates a new map with the specified initial capacity and load factor. This map will hold initialCapacity items before
     * growing the backing table.
     *
     * @param initialCapacity If not a power of two, it is increased to the next nearest power of two.
     */
    /**
     * Creates a new map with an initial capacity of 51 and a load factor of 0.8.
     */
    /**
     * Creates a new map with a load factor of 0.8.
     *
     * @param initialCapacity If not a power of two, it is increased to the next nearest power of two.
     */
    init {
        var initialCapacity = initialCapacity
        if (loadFactor <= 0) throw IllegalArgumentException("loadFactor must be > 0: $loadFactor")
        this.loadFactor = loadFactor
        if (initialCapacity < 0) throw IllegalArgumentException("initialCapacity must be >= 0: $initialCapacity")
        initialCapacity = MathUtils.nextPowerOfTwo(java.lang.Math.ceil(initialCapacity / loadFactor.toDouble()) as Int)
        if (initialCapacity > 1 shl 30) throw IllegalArgumentException("initialCapacity is too large: $initialCapacity")
        capacity = initialCapacity
        threshold = (capacity * loadFactor).toInt()
        mask = capacity - 1
        hashShift = 31 - java.lang.Integer.numberOfTrailingZeros(capacity)
        stashCapacity = max(3, java.lang.Math.ceil(java.lang.Math.log(capacity.toDouble())) as Int * 2)
        pushIterations = max(java.lang.Math.min(capacity, 8), java.lang.Math.sqrt(capacity.toDouble()) as Int / 8)
        keyTable = arrayOfNulls<Any>(capacity + stashCapacity) as Array<K?>
        valueTable = arrayOfNulls<Any>(keyTable.size) as Array<V?>
    }
}
