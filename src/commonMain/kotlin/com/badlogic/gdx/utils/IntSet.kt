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

/**
 * An unordered set that uses int keys. This implementation uses cuckoo hashing using 3 hashes, random walking, and a small stash
 * for problematic keys. No allocation is done except when growing the table size. <br></br>
 * <br></br>
 * This set performs very fast contains and remove (typically O(1), worst case O(log(n))). Add may be a bit slower, depending on
 * hash collisions. Load factors greater than 0.91 greatly increase the chances the set will have to rehash to the next higher POT
 * size.
 *
 * @author Nathan Sweet
 */
class IntSet @JvmOverloads constructor(initialCapacity: Int = 51, loadFactor: Float = 0.8f) {

    var size = 0
    var keyTable: kotlin.IntArray
    var capacity: Int
    var stashSize = 0
    var hasZeroValue = false
    private val loadFactor: Float
    private var hashShift: Int
    private var mask: Int
    private var threshold: Int
    private var stashCapacity: Int
    private var pushIterations: Int
    private var iterator1: IntSetIterator? = null
    private var iterator2: IntSetIterator? = null

    /**
     * Creates a new set identical to the specified set.
     */
    constructor(set: IntSet) : this(java.lang.Math.floor(set.capacity * set.loadFactor.toDouble()) as Int, set.loadFactor) {
        stashSize = set.stashSize
        java.lang.System.arraycopy(set.keyTable, 0, keyTable, 0, set.keyTable.size)
        size = set.size
        hasZeroValue = set.hasZeroValue
    }

    /**
     * Returns true if the key was not already in the set.
     */
    fun add(key: Int): Boolean {
        if (key == 0) {
            if (hasZeroValue) return false
            hasZeroValue = true
            size++
            return true
        }
        val keyTable = keyTable

        // Check for existing keys.
        val index1 = key and mask
        val key1 = keyTable[index1]
        if (key1 == key) return false
        val index2 = hash2(key)
        val key2 = keyTable[index2]
        if (key2 == key) return false
        val index3 = hash3(key)
        val key3 = keyTable[index3]
        if (key3 == key) return false

        // Find key in the stash.
        var i = capacity
        val n = i + stashSize
        while (i < n) {
            if (keyTable[i] == key) return false
            i++
        }

        // Check for empty buckets.
        if (key1 == EMPTY) {
            keyTable[index1] = key
            if (size++ >= threshold) resize(capacity shl 1)
            return true
        }
        if (key2 == EMPTY) {
            keyTable[index2] = key
            if (size++ >= threshold) resize(capacity shl 1)
            return true
        }
        if (key3 == EMPTY) {
            keyTable[index3] = key
            if (size++ >= threshold) resize(capacity shl 1)
            return true
        }
        push(key, index1, key1, index2, key2, index3, key3)
        return true
    }

    fun addAll(array: IntArray) {
        addAll(array.items, 0, array.size)
    }

    fun addAll(array: IntArray, offset: Int, length: Int) {
        if (offset + length > array.size) throw IllegalArgumentException("offset + length must be <= size: " + offset + " + " + length + " <= " + array.size)
        addAll(array.items, offset, length)
    }

    fun addAll(vararg array: Int) {
        addAll(array, 0, array.size)
    }

    fun addAll(array: kotlin.IntArray, offset: Int, length: Int) {
        ensureCapacity(length)
        var i = offset
        val n = i + length
        while (i < n) {
            add(array[i])
            i++
        }
    }

    fun addAll(set: IntSet) {
        ensureCapacity(set.size)
        val iterator = set.iterator()
        while (iterator!!.hasNext) add(iterator.next())
    }

    /**
     * Skips checks for existing keys.
     */
    private fun addResize(key: Int) {
        if (key == 0) {
            hasZeroValue = true
            return
        }

        // Check for empty buckets.
        val index1 = key and mask
        val key1 = keyTable[index1]
        if (key1 == EMPTY) {
            keyTable[index1] = key
            if (size++ >= threshold) resize(capacity shl 1)
            return
        }
        val index2 = hash2(key)
        val key2 = keyTable[index2]
        if (key2 == EMPTY) {
            keyTable[index2] = key
            if (size++ >= threshold) resize(capacity shl 1)
            return
        }
        val index3 = hash3(key)
        val key3 = keyTable[index3]
        if (key3 == EMPTY) {
            keyTable[index3] = key
            if (size++ >= threshold) resize(capacity shl 1)
            return
        }
        push(key, index1, key1, index2, key2, index3, key3)
    }

    private fun push(insertKey: Int, index1: Int, key1: Int, index2: Int, key2: Int, index3: Int, key3: Int) {
        var insertKey = insertKey
        var index1 = index1
        var key1 = key1
        var index2 = index2
        var key2 = key2
        var index3 = index3
        var key3 = key3
        val keyTable = keyTable
        val mask = mask

        // Push keys until an empty bucket is found.
        var evictedKey: Int
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
            index1 = evictedKey and mask
            key1 = keyTable[index1]
            if (key1 == EMPTY) {
                keyTable[index1] = evictedKey
                if (size++ >= threshold) resize(capacity shl 1)
                return
            }
            index2 = hash2(evictedKey)
            key2 = keyTable[index2]
            if (key2 == EMPTY) {
                keyTable[index2] = evictedKey
                if (size++ >= threshold) resize(capacity shl 1)
                return
            }
            index3 = hash3(evictedKey)
            key3 = keyTable[index3]
            if (key3 == EMPTY) {
                keyTable[index3] = evictedKey
                if (size++ >= threshold) resize(capacity shl 1)
                return
            }
            if (++i == pushIterations) break
            insertKey = evictedKey
        } while (true)
        addStash(evictedKey)
    }

    private fun addStash(key: Int) {
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
    fun remove(key: Int): Boolean {
        if (key == 0) {
            if (!hasZeroValue) return false
            hasZeroValue = false
            size--
            return true
        }
        var index = key and mask
        if (keyTable[index] == key) {
            keyTable[index] = EMPTY
            size--
            return true
        }
        index = hash2(key)
        if (keyTable[index] == key) {
            keyTable[index] = EMPTY
            size--
            return true
        }
        index = hash3(key)
        if (keyTable[index] == key) {
            keyTable[index] = EMPTY
            size--
            return true
        }
        return removeStash(key)
    }

    fun removeStash(key: Int): Boolean {
        val keyTable = keyTable
        var i = capacity
        val n = i + stashSize
        while (i < n) {
            if (keyTable[i] == key) {
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
        if (index < lastIndex) keyTable[index] = keyTable[lastIndex]
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
     * Clears the set and reduces the size of the backing arrays to be the specified capacity if they are larger.
     */
    fun clear(maximumCapacity: Int) {
        if (capacity <= maximumCapacity) {
            clear()
            return
        }
        hasZeroValue = false
        size = 0
        resize(maximumCapacity)
    }

    fun clear() {
        if (size == 0) return
        val keyTable = keyTable
        var i = capacity + stashSize
        while (i-- > 0) {
            keyTable[i] = EMPTY
        }
        size = 0
        stashSize = 0
        hasZeroValue = false
    }

    operator fun contains(key: Int): Boolean {
        if (key == 0) return hasZeroValue
        var index = key and mask
        if (keyTable[index] != key) {
            index = hash2(key)
            if (keyTable[index] != key) {
                index = hash3(key)
                if (keyTable[index] != key) return containsKeyStash(key)
            }
        }
        return true
    }

    private fun containsKeyStash(key: Int): Boolean {
        val keyTable = keyTable
        var i = capacity
        val n = i + stashSize
        while (i < n) {
            if (keyTable[i] == key) return true
            i++
        }
        return false
    }

    fun first(): Int {
        if (hasZeroValue) return 0
        val keyTable = keyTable
        var i = 0
        val n = capacity + stashSize
        while (i < n) {
            if (keyTable[i] != EMPTY) return keyTable[i]
            i++
        }
        throw IllegalStateException("IntSet is empty.")
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
        keyTable = kotlin.IntArray(newSize + stashCapacity)
        val oldSize = size
        size = if (hasZeroValue) 1 else 0
        stashSize = 0
        if (oldSize > 0) {
            for (i in 0 until oldEndIndex) {
                val key = oldKeyTable[i]
                if (key != EMPTY) addResize(key)
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
            if (keyTable[i] != EMPTY) h += keyTable[i]
            i++
        }
        return h
    }

    override fun equals(obj: Any?): Boolean {
        if (obj !is IntSet) return false
        val other = obj
        if (other.size != size) return false
        if (other.hasZeroValue != hasZeroValue) return false
        val keyTable = keyTable
        var i = 0
        val n = capacity + stashSize
        while (i < n) {
            if (keyTable[i] != EMPTY && !other.contains(keyTable[i])) return false
            i++
        }
        return true
    }

    override fun toString(): String {
        if (size == 0) return "[]"
        val buffer = StringBuilder(32)
        buffer.append('[')
        val keyTable = keyTable
        var i = keyTable.size
        if (hasZeroValue) buffer.append("0") else {
            while (i-- > 0) {
                val key = keyTable[i]
                if (key == EMPTY) continue
                buffer.append(key.toFloat())
                break
            }
        }
        while (i-- > 0) {
            val key = keyTable[i]
            if (key == EMPTY) continue
            buffer.append(", ")
            buffer.append(key.toFloat())
        }
        buffer.append(']')
        return buffer.toString()
    }

    /**
     * Returns an iterator for the keys in the set. Remove is supported.
     *
     *
     * If [Collections.allocateIterators] is false, the same iterator instance is returned each time this method is called.
     * Use the [IntSetIterator] constructor for nested or multithreaded iteration.
     */
    operator fun iterator(): IntSetIterator? {
        if (Collections.allocateIterators) return IntSetIterator(this)
        if (iterator1 == null) {
            iterator1 = IntSetIterator(this)
            iterator2 = IntSetIterator(this)
        }
        if (!iterator1!!.valid) {
            iterator1!!.reset()
            iterator1!!.valid = true
            iterator2!!.valid = false
            return iterator1
        }
        iterator2!!.reset()
        iterator2!!.valid = true
        iterator1!!.valid = false
        return iterator2
    }

    class IntSetIterator(val set: IntSet) {
        var hasNext = false
        var nextIndex = 0
        var currentIndex = 0
        var valid = true
        fun reset() {
            currentIndex = INDEX_ILLEGAL
            nextIndex = INDEX_ZERO
            if (set.hasZeroValue) hasNext = true else findNextIndex()
        }

        fun findNextIndex() {
            hasNext = false
            val keyTable = set.keyTable
            val n = set.capacity + set.stashSize
            while (++nextIndex < n) {
                if (keyTable[nextIndex] != EMPTY) {
                    hasNext = true
                    break
                }
            }
        }

        fun remove() {
            if (currentIndex == INDEX_ZERO && set.hasZeroValue) {
                set.hasZeroValue = false
            } else check(currentIndex >= 0) { "next must be called before remove." }
                if (currentIndex >= set.capacity) {
                    set.removeStashIndex(currentIndex)
                    nextIndex = currentIndex - 1
                    findNextIndex()
                } else {
                    set.keyTable[currentIndex] = EMPTY
                }
            currentIndex = INDEX_ILLEGAL
            set.size--
        }

        operator fun next(): Int {
            if (!hasNext) throw NoSuchElementException()
            if (!valid) throw GdxRuntimeException("#iterator() cannot be used nested.")
            val key = if (nextIndex == INDEX_ZERO) 0 else set.keyTable[nextIndex]
            currentIndex = nextIndex
            findNextIndex()
            return key
        }

        /**
         * Returns a new array containing the remaining keys.
         */
        fun toArray(): IntArray {
            val array = IntArray(true, set.size)
            while (hasNext) array.add(next())
            return array
        }

        companion object {
            const val INDEX_ILLEGAL = -2
            const val INDEX_ZERO = -1
        }

        init {
            reset()
        }
    }

    companion object {
        private const val PRIME1 = -0x41e0eb4f
        private const val PRIME2 = -0x4b47d1c7
        private const val PRIME3 = -0x312e3dbf
        private const val EMPTY = 0
        fun with(vararg array: Int): IntSet {
            val set = IntSet()
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
        keyTable = kotlin.IntArray(capacity + stashCapacity)
    }
}
