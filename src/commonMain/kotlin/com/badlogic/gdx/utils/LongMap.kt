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

import com.badlogic.gdx.utils.JsonValue
import com.badlogic.gdx.utils.JsonValue.JsonIterator
import com.badlogic.gdx.utils.JsonValue.PrettyPrintSettings
import com.badlogic.gdx.utils.JsonWriter
import com.badlogic.gdx.utils.LongArray
import com.badlogic.gdx.utils.ObjectFloatMap
import java.lang.IllegalStateException
import java.lang.IndexOutOfBoundsException
import kotlin.jvm.Throws

/**
 * An unordered map that uses long keys. This implementation is a cuckoo hash map using 3 hashes, random walking, and a small
 * stash for problematic keys. Null values are allowed. No allocation is done except when growing the table size. <br></br>
 * <br></br>
 * This map performs very fast get, containsKey, and remove (typically O(1), worst case O(log(n))). Put may be a bit slower,
 * depending on hash collisions. Load factors greater than 0.91 greatly increase the chances the map will have to rehash to the
 * next higher POT size.
 *
 * @author Nathan Sweet
 */
class LongMap<V> @JvmOverloads constructor(initialCapacity: Int = 51, loadFactor: Float = 0.8f) : Iterable<LongMap.Entry<V>?> {

    var size = 0
    var keyTable: LongArray
    var valueTable: Array<V?>
    var capacity: Int
    var stashSize = 0
    var zeroValue: V? = null
    var hasZeroValue = false
    private val loadFactor: Float
    private var hashShift: Int
    private var mask: Int
    private var threshold: Int
    private var stashCapacity: Int
    private var pushIterations: Int
    private var entries1: Entries<*>? = null
    private var entries2: Entries<*>? = null
    private var values1: Values<*>? = null
    private var values2: Values<*>? = null
    private var keys1: Keys? = null
    private var keys2: Keys? = null

    /**
     * Creates a new map identical to the specified map.
     */
    constructor(map: LongMap<out V>) : this(java.lang.Math.floor(map.capacity * map.loadFactor.toDouble()) as Int, map.loadFactor) {
        stashSize = map.stashSize
        java.lang.System.arraycopy(map.keyTable, 0, keyTable, 0, map.keyTable.size)
        java.lang.System.arraycopy(map.valueTable, 0, valueTable, 0, map.valueTable.size)
        size = map.size
        zeroValue = map.zeroValue
        hasZeroValue = map.hasZeroValue
    }

    fun put(key: Long, value: V): V? {
        if (key == 0L) {
            val oldValue = zeroValue
            zeroValue = value
            if (!hasZeroValue) {
                hasZeroValue = true
                size++
            }
            return oldValue
        }
        val keyTable = keyTable

        // Check for existing keys.
        val index1 = (key and mask.toLong()).toInt()
        val key1 = keyTable[index1]
        if (key1 == key) {
            val oldValue = valueTable[index1]
            valueTable[index1] = value
            return oldValue
        }
        val index2 = hash2(key)
        val key2 = keyTable[index2]
        if (key2 == key) {
            val oldValue = valueTable[index2]
            valueTable[index2] = value
            return oldValue
        }
        val index3 = hash3(key)
        val key3 = keyTable[index3]
        if (key3 == key) {
            val oldValue = valueTable[index3]
            valueTable[index3] = value
            return oldValue
        }

        // Update key in the stash.
        var i = capacity
        val n = i + stashSize
        while (i < n) {
            if (keyTable[i] == key) {
                val oldValue = valueTable[i]
                valueTable[i] = value
                return oldValue
            }
            i++
        }

        // Check for empty buckets.
        if (key1 == EMPTY.toLong()) {
            keyTable[index1] = key
            valueTable[index1] = value
            if (size++ >= threshold) resize(capacity shl 1)
            return null
        }
        if (key2 == EMPTY.toLong()) {
            keyTable[index2] = key
            valueTable[index2] = value
            if (size++ >= threshold) resize(capacity shl 1)
            return null
        }
        if (key3 == EMPTY.toLong()) {
            keyTable[index3] = key
            valueTable[index3] = value
            if (size++ >= threshold) resize(capacity shl 1)
            return null
        }
        push(key, value, index1, key1, index2, key2, index3, key3)
        return null
    }

    fun putAll(map: LongMap<out V>) {
        for (entry in map.entries()!!) put(entry.key, entry.value)
    }

    /**
     * Skips checks for existing keys.
     */
    private fun putResize(key: Long, value: V?) {
        if (key == 0L) {
            zeroValue = value
            hasZeroValue = true
            return
        }

        // Check for empty buckets.
        val index1 = (key and mask.toLong()).toInt()
        val key1 = keyTable[index1]
        if (key1 == EMPTY.toLong()) {
            keyTable[index1] = key
            valueTable[index1] = value
            if (size++ >= threshold) resize(capacity shl 1)
            return
        }
        val index2 = hash2(key)
        val key2 = keyTable[index2]
        if (key2 == EMPTY.toLong()) {
            keyTable[index2] = key
            valueTable[index2] = value
            if (size++ >= threshold) resize(capacity shl 1)
            return
        }
        val index3 = hash3(key)
        val key3 = keyTable[index3]
        if (key3 == EMPTY.toLong()) {
            keyTable[index3] = key
            valueTable[index3] = value
            if (size++ >= threshold) resize(capacity shl 1)
            return
        }
        push(key, value, index1, key1, index2, key2, index3, key3)
    }

    private fun push(insertKey: Long, insertValue: V?, index1: Int, key1: Long, index2: Int, key2: Long, index3: Int, key3: Long) {
        var insertKey = insertKey
        var insertValue = insertValue
        var index1 = index1
        var key1 = key1
        var index2 = index2
        var key2 = key2
        var index3 = index3
        var key3 = key3
        val keyTable = keyTable
        val valueTable = valueTable
        val mask = mask

        // Push keys until an empty bucket is found.
        var evictedKey: Long
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
            index1 = (evictedKey and mask.toLong()).toInt()
            key1 = keyTable[index1]
            if (key1 == EMPTY.toLong()) {
                keyTable[index1] = evictedKey
                valueTable[index1] = evictedValue
                if (size++ >= threshold) resize(capacity shl 1)
                return
            }
            index2 = hash2(evictedKey)
            key2 = keyTable[index2]
            if (key2 == EMPTY.toLong()) {
                keyTable[index2] = evictedKey
                valueTable[index2] = evictedValue
                if (size++ >= threshold) resize(capacity shl 1)
                return
            }
            index3 = hash3(evictedKey)
            key3 = keyTable[index3]
            if (key3 == EMPTY.toLong()) {
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

    private fun putStash(key: Long, value: V?) {
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

    operator fun get(key: Long): V? {
        if (key == 0L) {
            return if (!hasZeroValue) null else zeroValue
        }
        var index = (key and mask.toLong()).toInt()
        if (keyTable[index] != key) {
            index = hash2(key)
            if (keyTable[index] != key) {
                index = hash3(key)
                if (keyTable[index] != key) return getStash(key, null)
            }
        }
        return valueTable[index]
    }

    operator fun get(key: Long, defaultValue: V): V? {
        if (key == 0L) {
            return if (!hasZeroValue) defaultValue else zeroValue
        }
        var index = (key and mask.toLong()).toInt()
        if (keyTable[index] != key) {
            index = hash2(key)
            if (keyTable[index] != key) {
                index = hash3(key)
                if (keyTable[index] != key) return getStash(key, defaultValue)
            }
        }
        return valueTable[index]
    }

    private fun getStash(key: Long, defaultValue: V?): V? {
        val keyTable = keyTable
        var i = capacity
        val n = i + stashSize
        while (i < n) {
            if (keyTable[i] == key) return valueTable[i]
            i++
        }
        return defaultValue
    }

    fun remove(key: Long): V? {
        if (key == 0L) {
            if (!hasZeroValue) return null
            val oldValue = zeroValue
            zeroValue = null
            hasZeroValue = false
            size--
            return oldValue
        }
        var index = (key and mask.toLong()).toInt()
        if (keyTable[index] == key) {
            keyTable[index] = EMPTY.toLong()
            val oldValue = valueTable[index]
            valueTable[index] = null
            size--
            return oldValue
        }
        index = hash2(key)
        if (keyTable[index] == key) {
            keyTable[index] = EMPTY.toLong()
            val oldValue = valueTable[index]
            valueTable[index] = null
            size--
            return oldValue
        }
        index = hash3(key)
        if (keyTable[index] == key) {
            keyTable[index] = EMPTY.toLong()
            val oldValue = valueTable[index]
            valueTable[index] = null
            size--
            return oldValue
        }
        return removeStash(key)
    }

    fun removeStash(key: Long): V? {
        val keyTable = keyTable
        var i = capacity
        val n = i + stashSize
        while (i < n) {
            if (keyTable[i] == key) {
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
            valueTable[lastIndex] = null
        } else valueTable[index] = null
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
     * Clears the map and reduces the size of the backing arrays to be the specified capacity if they are larger.
     */
    fun clear(maximumCapacity: Int) {
        if (capacity <= maximumCapacity) {
            clear()
            return
        }
        zeroValue = null
        hasZeroValue = false
        size = 0
        resize(maximumCapacity)
    }

    fun clear() {
        if (size == 0) return
        val keyTable = keyTable
        val valueTable = valueTable
        var i = capacity + stashSize
        while (i-- > 0) {
            keyTable[i] = EMPTY.toLong()
            valueTable[i] = null
        }
        size = 0
        stashSize = 0
        zeroValue = null
        hasZeroValue = false
    }

    /**
     * Returns true if the specified value is in the map. Note this traverses the entire map and compares every value, which may
     * be an expensive operation.
     */
    fun containsValue(value: Any?, identity: Boolean): Boolean {
        val valueTable = valueTable
        if (value == null) {
            if (hasZeroValue && zeroValue == null) return true
            val keyTable = keyTable
            var i = capacity + stashSize
            while (i-- > 0) {
                if (keyTable[i] != EMPTY && valueTable[i] == null) return true
            }
        } else if (identity) {
            if (value === zeroValue) return true
            var i = capacity + stashSize
            while (i-- > 0) {
                if (valueTable[i] === value) return true
            }
        } else {
            if (hasZeroValue && value == zeroValue) return true
            var i = capacity + stashSize
            while (i-- > 0) {
                if (value == valueTable[i]) return true
            }
        }
        return false
    }

    fun containsKey(key: Long): Boolean {
        if (key == 0L) return hasZeroValue
        var index = (key and mask.toLong()).toInt()
        if (keyTable[index] != key) {
            index = hash2(key)
            if (keyTable[index] != key) {
                index = hash3(key)
                if (keyTable[index] != key) return containsKeyStash(key)
            }
        }
        return true
    }

    private fun containsKeyStash(key: Long): Boolean {
        val keyTable = keyTable
        var i = capacity
        val n = i + stashSize
        while (i < n) {
            if (keyTable[i] == key) return true
            i++
        }
        return false
    }

    /**
     * Returns the key for the specified value, or <tt>notFound</tt> if it is not in the map. Note this traverses the entire map
     * and compares every value, which may be an expensive operation.
     *
     * @param identity If true, uses == to compare the specified value with values in the map. If false, uses
     * [.equals].
     */
    fun findKey(value: Any?, identity: Boolean, notFound: Long): Long {
        val valueTable = valueTable
        if (value == null) {
            if (hasZeroValue && zeroValue == null) return 0
            val keyTable = keyTable
            var i = capacity + stashSize
            while (i-- > 0) {
                if (keyTable[i] != EMPTY && valueTable[i] == null) return keyTable[i]
            }
        } else if (identity) {
            if (value === zeroValue) return 0
            var i = capacity + stashSize
            while (i-- > 0) {
                if (valueTable[i] === value) return keyTable[i]
            }
        } else {
            if (hasZeroValue && value == zeroValue) return 0
            var i = capacity + stashSize
            while (i-- > 0) {
                if (value == valueTable[i]) return keyTable[i]
            }
        }
        return notFound
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
        hashShift = 63 - java.lang.Long.numberOfTrailingZeros(newSize.toLong())
        stashCapacity = max(3, java.lang.Math.ceil(java.lang.Math.log(newSize.toDouble())) as Int * 2)
        pushIterations = max(java.lang.Math.min(newSize, 8), java.lang.Math.sqrt(newSize.toDouble()) as Int / 8)
        val oldKeyTable = keyTable
        val oldValueTable = valueTable
        keyTable = LongArray(newSize + stashCapacity)
        valueTable = arrayOfNulls<Any>(newSize + stashCapacity) as Array<V?>
        val oldSize = size
        size = if (hasZeroValue) 1 else 0
        stashSize = 0
        if (oldSize > 0) {
            for (i in 0 until oldEndIndex) {
                val key = oldKeyTable[i]
                if (key != EMPTY.toLong()) putResize(key, oldValueTable[i])
            }
        }
    }

    private fun hash2(h: Long): Int {
        var h = h
        h *= PRIME2.toLong()
        return (h xor h ushr hashShift and mask.toLong()).toInt()
    }

    private fun hash3(h: Long): Int {
        var h = h
        h *= PRIME3.toLong()
        return (h xor h ushr hashShift and mask.toLong()).toInt()
    }

    override fun hashCode(): Int {
        var h = 0
        if (hasZeroValue && zeroValue != null) {
            h += zeroValue.hashCode()
        }
        val keyTable = keyTable
        val valueTable: Array<V> = valueTable
        var i = 0
        val n = capacity + stashSize
        while (i < n) {
            val key = keyTable[i]
            if (key != EMPTY.toLong()) {
                h += (key xor (key ushr 32)).toInt() * 31
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
        if (obj !is LongMap<*>) return false
        val other = obj
        if (other.size != size) return false
        if (other.hasZeroValue != hasZeroValue) return false
        if (hasZeroValue) {
            if (other.zeroValue == null) {
                if (zeroValue != null) return false
            } else {
                if (other.zeroValue != zeroValue) return false
            }
        }
        val keyTable = keyTable
        val valueTable: Array<V> = valueTable
        var i = 0
        val n = capacity + stashSize
        while (i < n) {
            val key = keyTable[i]
            if (key != EMPTY.toLong()) {
                val value: V? = valueTable[i]
                if (value == null) {
                    if (other.get(key, ObjectMap.dummy) != null) return false
                } else {
                    if (value != other[key]) return false
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
        if (obj !is LongMap<*>) return false
        val other = obj
        if (other.size != size) return false
        if (other.hasZeroValue != hasZeroValue) return false
        if (hasZeroValue && zeroValue !== other.zeroValue) return false
        val keyTable = keyTable
        val valueTable: Array<V> = valueTable
        var i = 0
        val n = capacity + stashSize
        while (i < n) {
            val key = keyTable[i]
            if (key != EMPTY.toLong() && valueTable[i] !== other.get(key, ObjectMap.dummy)) return false
            i++
        }
        return true
    }

    override fun toString(): String {
        if (size == 0) return "[]"
        val buffer = StringBuilder(32)
        buffer.append('[')
        val keyTable = keyTable
        val valueTable: Array<V> = valueTable
        var i = keyTable.size
        while (i-- > 0) {
            val key = keyTable[i]
            if (key == EMPTY.toLong()) continue
            buffer.append(key.toFloat())
            buffer.append('=')
            buffer.append(valueTable[i])
            break
        }
        while (i-- > 0) {
            val key = keyTable[i]
            if (key == EMPTY.toLong()) continue
            buffer.append(", ")
            buffer.append(key.toFloat())
            buffer.append('=')
            buffer.append(valueTable[i])
        }
        buffer.append(']')
        return buffer.toString()
    }

    override fun iterator(): Iterator<Entry<V>> {
        return entries()
    }

    /**
     * Returns an iterator for the entries in the map. Remove is supported.
     *
     *
     * If [Collections.allocateIterators] is false, the same iterator instance is returned each time this method is called.
     * Use the [Entries] constructor for nested or multithreaded iteration.
     */
    fun entries(): Entries<V>? {
        if (Collections.allocateIterators) return Entries<Any?>(this)
        if (entries1 == null) {
            entries1 = Entries<Any?>(this)
            entries2 = Entries<Any?>(this)
        }
        if (!entries1.valid) {
            entries1.reset()
            entries1.valid = true
            entries2.valid = false
            return entries1
        }
        entries2.reset()
        entries2.valid = true
        entries1.valid = false
        return entries2
    }

    /**
     * Returns an iterator for the values in the map. Remove is supported.
     *
     *
     * If [Collections.allocateIterators] is false, the same iterator instance is returned each time this method is called.
     * Use the [Entries] constructor for nested or multithreaded iteration.
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
            values2.valid = false
            return values1
        }
        values2.reset()
        values2.valid = true
        values1.valid = false
        return values2
    }

    /**
     * Returns an iterator for the keys in the map. Remove is supported.
     *
     *
     * If [Collections.allocateIterators] is false, the same iterator instance is returned each time this method is called.
     * Use the [Entries] constructor for nested or multithreaded iteration.
     */
    fun keys(): Keys? {
        if (Collections.allocateIterators) return Keys(this)
        if (keys1 == null) {
            keys1 = Keys(this)
            keys2 = Keys(this)
        }
        if (!keys1.valid) {
            keys1.reset()
            keys1.valid = true
            keys2.valid = false
            return keys1
        }
        keys2.reset()
        keys2.valid = true
        keys1.valid = false
        return keys2
    }

    class Entry<V> {
        var key: Long = 0
        var value: V? = null
        override fun toString(): String {
            return "$key=$value"
        }
    }

    class MapIterator<V>(map: LongMap<V>) {
        var hasNext = false
        val map: LongMap<V?>
        var nextIndex = 0
        var currentIndex = 0
        var valid = true
        fun reset() {
            currentIndex = INDEX_ILLEGAL
            nextIndex = INDEX_ZERO
            if (map.hasZeroValue) hasNext = true else findNextIndex()
        }

        fun findNextIndex() {
            hasNext = false
            val keyTable = map.keyTable
            val n = map.capacity + map.stashSize
            while (++nextIndex < n) {
                if (keyTable[nextIndex] != EMPTY) {
                    hasNext = true
                    break
                }
            }
        }

        fun remove() {
            if (currentIndex == INDEX_ZERO && map.hasZeroValue) {
                map.zeroValue = null
                map.hasZeroValue = false
            } else check(currentIndex >= 0) { "next must be called before remove." }
                if (currentIndex >= map.capacity) {
                    map.removeStashIndex(currentIndex)
                    nextIndex = currentIndex - 1
                    findNextIndex()
                } else {
                    map.keyTable[currentIndex] = EMPTY.toLong()
                    map.valueTable[currentIndex] = null
                }
            currentIndex = INDEX_ILLEGAL
            map.size--
        }

        companion object {
            const val INDEX_ILLEGAL = -2
            const val INDEX_ZERO = -1
        }

        init {
            this.map = map
            reset()
        }
    }

    class Entries<V>(map: LongMap<*>) : MapIterator<V>(map), Iterable<Entry<V>?>, Iterator<Entry<V>?> {
        private val entry: Entry<V?> = Entry<Any?>()

        /**
         * Note the same entry instance is returned each time this method is called.
         */
        override fun next(): Entry<V?> {
            if (!hasNext) throw NoSuchElementException()
            if (!valid) throw GdxRuntimeException("#iterator() cannot be used nested.")
            val keyTable: LongArray = map.keyTable
            if (nextIndex === INDEX_ZERO) {
                entry.key = 0
                entry.value = map.zeroValue
            } else {
                entry.key = keyTable[nextIndex]
                entry.value = map.valueTable.get(nextIndex)
            }
            currentIndex = nextIndex
            findNextIndex()
            return entry
        }

        override fun hasNext(): Boolean {
            if (!valid) throw GdxRuntimeException("#iterator() cannot be used nested.")
            return hasNext
        }

        override fun iterator(): Iterator<Entry<V>> {
            return this
        }

        override fun remove() {
            super.remove()
        }
    }

    class Values<V>(map: LongMap<V>) : MapIterator<V>(map), Iterable<V>, Iterator<V> {
        override fun hasNext(): Boolean {
            if (!valid) throw GdxRuntimeException("#iterator() cannot be used nested.")
            return hasNext
        }

        override fun next(): V {
            if (!hasNext) throw NoSuchElementException()
            if (!valid) throw GdxRuntimeException("#iterator() cannot be used nested.")
            val value: V
            value = if (nextIndex === INDEX_ZERO) map.zeroValue else map.valueTable.get(nextIndex)
            currentIndex = nextIndex
            findNextIndex()
            return value
        }

        override fun iterator(): Iterator<V> {
            return this
        }

        /**
         * Returns a new array containing the remaining values.
         */
        fun toArray(): Array<V> {
            val array = Array(true, map.size)
            while (hasNext) array.add(next())
            return array
        }

        override fun remove() {
            super.remove()
        }
    }

    class Keys(map: LongMap<*>) : MapIterator<Any?>(map) {
        operator fun next(): Long {
            if (!hasNext) throw NoSuchElementException()
            if (!valid) throw GdxRuntimeException("#iterator() cannot be used nested.")
            val key: Long = if (nextIndex === INDEX_ZERO) 0 else map.keyTable.get(nextIndex).toLong()
            currentIndex = nextIndex
            findNextIndex()
            return key
        }

        /**
         * Returns a new array containing the remaining values.
         */
        fun toArray(): LongArray {
            val array = LongArray(true, map.size)
            while (hasNext) array.add(next())
            return array
        }
    }

    companion object {
        private const val PRIME1 = -0x41e0eb4f
        private const val PRIME2 = -0x4b47d1c7
        private const val PRIME3 = -0x312e3dbf
        private const val EMPTY = 0
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
        hashShift = 63 - java.lang.Long.numberOfTrailingZeros(capacity.toLong())
        stashCapacity = max(3, java.lang.Math.ceil(java.lang.Math.log(capacity.toDouble())) as Int * 2)
        pushIterations = max(java.lang.Math.min(capacity, 8), java.lang.Math.sqrt(capacity.toDouble()) as Int / 8)
        keyTable = LongArray(capacity + stashCapacity)
        valueTable = arrayOfNulls<Any>(keyTable.size) as Array<V?>
    }
}
