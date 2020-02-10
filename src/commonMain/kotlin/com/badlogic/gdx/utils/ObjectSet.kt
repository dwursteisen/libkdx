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
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.OrderedMap
import com.badlogic.gdx.utils.OrderedMap.OrderedMapEntries
import com.badlogic.gdx.utils.OrderedMap.OrderedMapKeys
import com.badlogic.gdx.utils.OrderedMap.OrderedMapValues
import com.badlogic.gdx.utils.OrderedSet
import com.badlogic.gdx.utils.OrderedSet.OrderedSetIterator
import java.lang.IllegalStateException
import java.util.NoSuchElementException

/**
 * An unordered set where the keys are objects. This implementation uses cuckoo hashing using 3 hashes, random walking, and a
 * small stash for problematic keys. Null keys are not allowed. No allocation is done except when growing the table size. <br></br>
 * <br></br>
 * This set performs very fast contains and remove (typically O(1), worst case O(log(n))). Add may be a bit slower, depending on
 * hash collisions. Load factors greater than 0.91 greatly increase the chances the set will have to rehash to the next higher POT
 * size.<br></br>
 * <br></br>
 * Iteration can be very slow for a set with a large capacity. [.clear] and [.shrink] can be used to reduce
 * the capacity. [OrderedSet] provides much faster iteration.
 *
 * @author Nathan Sweet
 */
class ObjectSet<T> @JvmOverloads constructor(initialCapacity: Int = 51, loadFactor: Float = 0.8f) : Iterable<T> {

    var size = 0
    var keyTable: Array<T?>
    var capacity: Int
    var stashSize = 0
    private val loadFactor: Float
    private var hashShift: Int
    private var mask: Int
    private var threshold: Int
    private var stashCapacity: Int
    private var pushIterations: Int
    private var iterator1: ObjectSetIterator<*>? = null
    private var iterator2: ObjectSetIterator<*>? = null

    /**
     * Creates a new set identical to the specified set.
     */
    constructor(set: ObjectSet<out T>) : this(java.lang.Math.floor(set.capacity * set.loadFactor.toDouble()) as Int, set.loadFactor) {
        stashSize = set.stashSize
        java.lang.System.arraycopy(set.keyTable, 0, keyTable, 0, set.keyTable.size)
        size = set.size
    }

    /**
     * Returns true if the key was not already in the set. If this set already contains the key, the call leaves the set unchanged
     * and returns false.
     */
    fun add(key: T?): Boolean {
        if (key == null) throw IllegalArgumentException("key cannot be null.")
        val keyTable: Array<T> = keyTable

        // Check for existing keys.
        val hashCode = key.hashCode()
        val index1 = hashCode and mask
        val key1: T? = keyTable[index1]
        if (key == key1) return false
        val index2 = hash2(hashCode)
        val key2: T? = keyTable[index2]
        if (key == key2) return false
        val index3 = hash3(hashCode)
        val key3: T? = keyTable[index3]
        if (key == key3) return false

        // Find key in the stash.
        var i = capacity
        val n = i + stashSize
        while (i < n) {
            if (key == keyTable[i]) return false
            i++
        }

        // Check for empty buckets.
        if (key1 == null) {
            keyTable[index1] = key
            if (size++ >= threshold) resize(capacity shl 1)
            return true
        }
        if (key2 == null) {
            keyTable[index2] = key
            if (size++ >= threshold) resize(capacity shl 1)
            return true
        }
        if (key3 == null) {
            keyTable[index3] = key
            if (size++ >= threshold) resize(capacity shl 1)
            return true
        }
        push(key, index1, key1, index2, key2, index3, key3)
        return true
    }

    fun addAll(array: Array<out T>) {
        addAll(array.items, 0, array.size)
    }

    fun addAll(array: Array<out T>, offset: Int, length: Int) {
        if (offset + length > array.size) throw IllegalArgumentException("offset + length must be <= size: " + offset + " + " + length + " <= " + array.size)
        addAll(array.items as Array<T>, offset, length)
    }

    fun addAll(vararg array: T) {
        addAll(array, 0, array.size)
    }

    fun addAll(array: Array<T>, offset: Int, length: Int) {
        ensureCapacity(length)
        var i = offset
        val n = i + length
        while (i < n) {
            add(array[i])
            i++
        }
    }

    fun addAll(set: ObjectSet<T>) {
        ensureCapacity(set.size)
        for (key in set) add(key)
    }

    /**
     * Skips checks for existing keys.
     */
    private fun addResize(key: T?) {
        // Check for empty buckets.
        val hashCode = key.hashCode()
        val index1 = hashCode and mask
        val key1 = keyTable[index1]
        if (key1 == null) {
            keyTable[index1] = key
            if (size++ >= threshold) resize(capacity shl 1)
            return
        }
        val index2 = hash2(hashCode)
        val key2 = keyTable[index2]
        if (key2 == null) {
            keyTable[index2] = key
            if (size++ >= threshold) resize(capacity shl 1)
            return
        }
        val index3 = hash3(hashCode)
        val key3 = keyTable[index3]
        if (key3 == null) {
            keyTable[index3] = key
            if (size++ >= threshold) resize(capacity shl 1)
            return
        }
        push(key, index1, key1, index2, key2, index3, key3)
    }

    private fun push(insertKey: T?, index1: Int, key1: T, index2: Int, key2: T, index3: Int, key3: T) {
        var insertKey = insertKey
        var index1 = index1
        var key1: T? = key1
        var index2 = index2
        var key2: T? = key2
        var index3 = index3
        var key3: T? = key3
        val keyTable = keyTable
        val mask = mask

        // Push keys until an empty bucket is found.
        var evictedKey: T?
        var i = 0
        val pushIterations = pushIterations
        do {
            // Replace the key and value for one of the hashes.
            when (MathUtils.random(2)) {
                0 -> {
                    evictedKey = key1
                    keyTable[index1] = insertKey
                }
                1 -> {
                    evictedKey = key2
                    keyTable[index2] = insertKey
                }
                else -> {
                    evictedKey = key3
                    keyTable[index3] = insertKey
                }
            }

            // If the evicted key hashes to an empty bucket, put it there and stop.
            val hashCode = evictedKey.hashCode()
            index1 = hashCode and mask
            key1 = keyTable[index1]
            if (key1 == null) {
                keyTable[index1] = evictedKey
                if (size++ >= threshold) resize(capacity shl 1)
                return
            }
            index2 = hash2(hashCode)
            key2 = keyTable[index2]
            if (key2 == null) {
                keyTable[index2] = evictedKey
                if (size++ >= threshold) resize(capacity shl 1)
                return
            }
            index3 = hash3(hashCode)
            key3 = keyTable[index3]
            if (key3 == null) {
                keyTable[index3] = evictedKey
                if (size++ >= threshold) resize(capacity shl 1)
                return
            }
            if (++i == pushIterations) break
            insertKey = evictedKey
        } while (true)
        addStash(evictedKey)
    }

    private fun addStash(key: T?) {
        if (stashSize == stashCapacity) {
            // Too many pushes occurred and the stash is full, increase the table size.
            resize(capacity shl 1)
            addResize(key)
            return
        }
        // Store key in the stash.
        val index = capacity + stashSize
        keyTable[index] = key
        stashSize++
        size++
    }

    /**
     * Returns true if the key was removed.
     */
    fun remove(key: T): Boolean {
        val hashCode = key.hashCode()
        var index = hashCode and mask
        if (key == keyTable[index]) {
            keyTable[index] = null
            size--
            return true
        }
        index = hash2(hashCode)
        if (key == keyTable[index]) {
            keyTable[index] = null
            size--
            return true
        }
        index = hash3(hashCode)
        if (key == keyTable[index]) {
            keyTable[index] = null
            size--
            return true
        }
        return removeStash(key)
    }

    fun removeStash(key: T): Boolean {
        val keyTable: Array<T> = keyTable
        var i = capacity
        val n = i + stashSize
        while (i < n) {
            if (key == keyTable[i]) {
                removeStashIndex(i)
                size--
                return true
            }
            i++
        }
        return false
    }

    fun removeStashIndex(index: Int) {
        // If the removed location was not last, move the last tuple to the removed location.
        stashSize--
        val lastIndex = capacity + stashSize
        if (index < lastIndex) {
            keyTable[index] = keyTable[lastIndex]
            keyTable[lastIndex] = null
        }
    }

    /**
     * Returns true if the set has one or more items.
     */
    fun notEmpty(): Boolean {
        return size > 0
    }

    /**
     * Returns true if the set is empty.
     */
    val isEmpty: Boolean
        get() = size == 0

    /**
     * Reduces the size of the backing arrays to be the specified capacity or less. If the capacity is already less, nothing is
     * done. If the set contains more items than the specified capacity, the next highest power of two capacity is used instead.
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
     * Clears the set and reduces the size of the backing arrays to be the specified capacity, if they are larger. The reduction
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
     * Clears the set, leaving the backing arrays at the current capacity. When the capacity is high and the population is low,
     * iteration can be unnecessarily slow. [.clear] can be used to reduce the capacity.
     */
    fun clear() {
        if (size == 0) return
        val keyTable = keyTable
        var i = capacity + stashSize
        while (i-- > 0) {
            keyTable[i] = null
        }
        size = 0
        stashSize = 0
    }

    operator fun contains(key: T): Boolean {
        val hashCode = key.hashCode()
        var index = hashCode and mask
        if (key != keyTable[index]) {
            index = hash2(hashCode)
            if (key != keyTable[index]) {
                index = hash3(hashCode)
                if (key != keyTable[index]) return getKeyStash(key) != null
            }
        }
        return true
    }

    /**
     * @return May be null.
     */
    operator fun get(key: T): T? {
        val hashCode = key.hashCode()
        var index = hashCode and mask
        var found = keyTable[index]
        if (key != found) {
            index = hash2(hashCode)
            found = keyTable[index]
            if (key != found) {
                index = hash3(hashCode)
                found = keyTable[index]
                if (key != found) return getKeyStash(key)
            }
        }
        return found
    }

    private fun getKeyStash(key: T): T? {
        val keyTable: Array<T> = keyTable
        var i = capacity
        val n = i + stashSize
        while (i < n) {
            if (key == keyTable[i]) return keyTable[i]
            i++
        }
        return null
    }

    fun first(): T? {
        val keyTable = keyTable
        var i = 0
        val n = capacity + stashSize
        while (i < n) {
            if (keyTable[i] != null) return keyTable[i]
            i++
        }
        throw IllegalStateException("ObjectSet is empty.")
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
        keyTable = arrayOfNulls<Any>(newSize + stashCapacity) as Array<T?>
        val oldSize = size
        size = 0
        stashSize = 0
        if (oldSize > 0) {
            for (i in 0 until oldEndIndex) {
                val key = oldKeyTable[i]
                key?.let { addResize(it) }
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
        var i = 0
        val n = capacity + stashSize
        while (i < n) {
            if (keyTable[i] != null) h += keyTable[i].hashCode()
            i++
        }
        return h
    }

    override fun equals(obj: Any?): Boolean {
        if (obj !is ObjectSet<*>) return false
        val other = obj
        if (other.size != size) return false
        val keyTable = keyTable
        var i = 0
        val n = capacity + stashSize
        while (i < n) {
            if (keyTable[i] != null && !other.contains(keyTable[i])) return false
            i++
        }
        return true
    }

    override fun toString(): String {
        return '{'.toString() + toString(", ") + '}'
    }

    fun toString(separator: String?): String {
        if (size == 0) return ""
        val buffer: java.lang.StringBuilder = java.lang.StringBuilder(32)
        val keyTable: Array<T> = keyTable
        var i = keyTable.size
        while (i-- > 0) {
            val key: T = keyTable[i] ?: continue
            buffer.append(key)
            break
        }
        while (i-- > 0) {
            val key: T = keyTable[i] ?: continue
            buffer.append(separator)
            buffer.append(key)
        }
        return buffer.toString()
    }

    /**
     * Returns an iterator for the keys in the set. Remove is supported.
     *
     *
     * If [Collections.allocateIterators] is false, the same iterator instance is returned each time this method is called.
     * Use the [ObjectSetIterator] constructor for nested or multithreaded iteration.
     */
    override fun iterator(): ObjectSetIterator<T> {
        if (Collections.allocateIterators) return ObjectSetIterator<Any?>(this)
        if (iterator1 == null) {
            iterator1 = ObjectSetIterator<Any?>(this)
            iterator2 = ObjectSetIterator<Any?>(this)
        }
        if (!iterator1.valid) {
            iterator1.reset()
            iterator1.valid = true
            iterator2!!.valid = false
            return iterator1
        }
        iterator2!!.reset()
        iterator2!!.valid = true
        iterator1.valid = false
        return iterator2
    }

    class ObjectSetIterator<K>(val set: ObjectSet<K>) : Iterable<K>, MutableIterator<K> {
        var hasNext = false
        var nextIndex = 0
        var currentIndex = 0
        var valid = true
        fun reset() {
            currentIndex = -1
            nextIndex = -1
            findNextIndex()
        }

        private fun findNextIndex() {
            hasNext = false
            val keyTable: Array<K> = set.keyTable
            val n = set.capacity + set.stashSize
            while (++nextIndex < n) {
                if (keyTable[nextIndex] != null) {
                    hasNext = true
                    break
                }
            }
        }

        override fun remove() {
            check(currentIndex >= 0) { "next must be called before remove." }
            if (currentIndex >= set.capacity) {
                set.removeStashIndex(currentIndex)
                nextIndex = currentIndex - 1
                findNextIndex()
            } else {
                set.keyTable[currentIndex] = null
            }
            currentIndex = -1
            set.size--
        }

        override fun hasNext(): Boolean {
            if (!valid) throw GdxRuntimeException("#iterator() cannot be used nested.")
            return hasNext
        }

        override fun next(): K {
            if (!hasNext) throw NoSuchElementException()
            if (!valid) throw GdxRuntimeException("#iterator() cannot be used nested.")
            val key: K = set.keyTable[nextIndex]
            currentIndex = nextIndex
            findNextIndex()
            return key
        }

        override fun iterator(): ObjectSetIterator<K> {
            return this
        }

        /**
         * Adds the remaining values to the array.
         */
        fun toArray(array: Array<K>): Array<K> {
            while (hasNext) array.add(next())
            return array
        }

        /**
         * Returns a new array containing the remaining values.
         */
        fun toArray(): Array<K> {
            return toArray(Array(true, set.size))
        }

        init {
            reset()
        }
    }

    companion object {
        private const val PRIME1 = -0x41e0eb4f
        private const val PRIME2 = -0x4b47d1c7
        private const val PRIME3 = -0x312e3dbf
        fun <T> with(vararg array: T): ObjectSet<T> {
            val set: ObjectSet<*> = ObjectSet<Any?>()
            set.addAll(*array)
            return set
        }
    }
    /**
     * Creates a new set with the specified initial capacity and load factor. This set will hold initialCapacity items before
     * growing the backing table.
     *
     * @param initialCapacity If not a power of two, it is increased to the next nearest power of two.
     */
    /**
     * Creates a new set with an initial capacity of 51 and a load factor of 0.8.
     */
    /**
     * Creates a new set with a load factor of 0.8.
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
        keyTable = arrayOfNulls<Any>(capacity + stashCapacity) as Array<T?>
    }
}
