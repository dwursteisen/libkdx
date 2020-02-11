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

import com.badlogic.gdx.utils.ByteArray
import java.lang.IllegalStateException
import java.lang.IndexOutOfBoundsException

/**
 * A resizable, ordered or unordered boolean array. Avoids the boxing that occurs with ArrayList<Boolean>. It is less memory
 * efficient than [BitSet], except for very small sizes. It more CPU efficient than [BitSet], except for very large
 * sizes or if BitSet functionality such as and, or, xor, etc are needed. If unordered, this class avoids a memory copy when
 * removing elements (the last element is moved to the removed element's position).
 *
 * @author Nathan Sweet
</Boolean> */
class BooleanArray {

    var items: BooleanArray
    var size = 0
    var ordered: Boolean

    /**
     * Creates an ordered array with the specified capacity.
     */
    constructor(capacity: Int) : this(true, capacity) {}
    /**
     * @param ordered  If false, methods that remove elements may change the order of other elements in the array, which avoids a
     * memory copy.
     * @param capacity Any elements added beyond this will cause the backing array to be grown.
     */
    /**
     * Creates an ordered array with a capacity of 16.
     */
    @JvmOverloads
    constructor(ordered: Boolean = true, capacity: Int = 16) {
        this.ordered = ordered
        items = BooleanArray(capacity)
    }

    /**
     * Creates a new array containing the elements in the specific array. The new array will be ordered if the specific array is
     * ordered. The capacity is set to the number of elements, so any subsequent elements added will cause the backing array to be
     * grown.
     */
    constructor(array: BooleanArray) {
        ordered = array.ordered
        size = array.size
        items = BooleanArray(size)
        java.lang.System.arraycopy(array.items, 0, items, 0, size)
    }

    /**
     * Creates a new ordered array containing the elements in the specified array. The capacity is set to the number of elements,
     * so any subsequent elements added will cause the backing array to be grown.
     */
    constructor(array: BooleanArray) : this(true, array, 0, array.size) {}

    /**
     * Creates a new array containing the elements in the specified array. The capacity is set to the number of elements, so any
     * subsequent elements added will cause the backing array to be grown.
     *
     * @param ordered If false, methods that remove elements may change the order of other elements in the array, which avoids a
     * memory copy.
     */
    constructor(ordered: Boolean, array: BooleanArray?, startIndex: Int, count: Int) : this(ordered, count) {
        size = count
        java.lang.System.arraycopy(array, startIndex, items, 0, count)
    }

    fun add(value: Boolean) {
        var items = items
        if (size == items.size) items = resize(java.lang.Math.max(8, (size * 1.75f).toInt()))
        items[size++] = value
    }

    fun add(value1: Boolean, value2: Boolean) {
        var items = items
        if (size + 1 >= items.size) items = resize(java.lang.Math.max(8, (size * 1.75f).toInt()))
        items[size] = value1
        items[size + 1] = value2
        size += 2
    }

    fun add(value1: Boolean, value2: Boolean, value3: Boolean) {
        var items = items
        if (size + 2 >= items.size) items = resize(java.lang.Math.max(8, (size * 1.75f).toInt()))
        items[size] = value1
        items[size + 1] = value2
        items[size + 2] = value3
        size += 3
    }

    fun add(value1: Boolean, value2: Boolean, value3: Boolean, value4: Boolean) {
        var items = items
        if (size + 3 >= items.size) items = resize(java.lang.Math.max(8, (size * 1.8f).toInt())) // 1.75 isn't enough when size=5.
        items[size] = value1
        items[size + 1] = value2
        items[size + 2] = value3
        items[size + 3] = value4
        size += 4
    }

    fun addAll(array: BooleanArray) {
        addAll(array.items, 0, array.size)
    }

    fun addAll(array: BooleanArray, offset: Int, length: Int) {
        if (offset + length > array.size) throw java.lang.IllegalArgumentException("offset + length must be <= size: " + offset + " + " + length + " <= " + array.size)
        addAll(array.items, offset, length)
    }

    fun addAll(vararg array: Boolean) {
        addAll(array, 0, array.size)
    }

    fun addAll(array: BooleanArray?, offset: Int, length: Int) {
        var items = items
        val sizeNeeded = size + length
        if (sizeNeeded > items.size) items = resize(java.lang.Math.max(8, (sizeNeeded * 1.75f).toInt()))
        java.lang.System.arraycopy(array, offset, items, size, length)
        size += length
    }

    operator fun get(index: Int): Boolean {
        if (index >= size) throw IndexOutOfBoundsException("index can't be >= size: $index >= $size")
        return items[index]
    }

    operator fun set(index: Int, value: Boolean) {
        if (index >= size) throw IndexOutOfBoundsException("index can't be >= size: $index >= $size")
        items[index] = value
    }

    fun insert(index: Int, value: Boolean) {
        if (index > size) throw IndexOutOfBoundsException("index can't be > size: $index > $size")
        var items = items
        if (size == items.size) items = resize(java.lang.Math.max(8, (size * 1.75f).toInt()))
        if (ordered) java.lang.System.arraycopy(items, index, items, index + 1, size - index) else items[size] = items[index]
        size++
        items[index] = value
    }

    fun swap(first: Int, second: Int) {
        if (first >= size) throw IndexOutOfBoundsException("first can't be >= size: $first >= $size")
        if (second >= size) throw IndexOutOfBoundsException("second can't be >= size: $second >= $size")
        val items = items
        val firstValue = items[first]
        items[first] = items[second]
        items[second] = firstValue
    }

    /**
     * Removes and returns the item at the specified index.
     */
    fun removeIndex(index: Int): Boolean {
        if (index >= size) throw IndexOutOfBoundsException("index can't be >= size: $index >= $size")
        val items = items
        val value = items[index]
        size--
        if (ordered) java.lang.System.arraycopy(items, index + 1, items, index, size - index) else items[index] = items[size]
        return value
    }

    /**
     * Removes the items between the specified indices, inclusive.
     */
    fun removeRange(start: Int, end: Int) {
        val n = size
        if (end >= n) throw IndexOutOfBoundsException("end can't be >= size: $end >= $size")
        if (start > end) throw IndexOutOfBoundsException("start can't be > end: $start > $end")
        val count = end - start + 1
        val lastIndex = n - count
        if (ordered) java.lang.System.arraycopy(items, start + count, items, start, n - (start + count)) else {
            val i: Int = java.lang.Math.max(lastIndex, end + 1)
            java.lang.System.arraycopy(items, i, items, start, n - i)
        }
        size = n - count
    }

    /**
     * Removes from this array all of elements contained in the specified array.
     *
     * @return true if this array was modified.
     */
    fun removeAll(array: BooleanArray): Boolean {
        var size = size
        val startSize = size
        val items = items
        var i = 0
        val n = array.size
        while (i < n) {
            val item = array[i]
            for (ii in 0 until size) {
                if (item == items[ii]) {
                    removeIndex(ii)
                    size--
                    break
                }
            }
            i++
        }
        return size != startSize
    }

    /**
     * Removes and returns the last item.
     */
    fun pop(): Boolean {
        return items[--size]
    }

    /**
     * Returns the last item.
     */
    fun peek(): Boolean {
        return items[size - 1]
    }

    /**
     * Returns the first item.
     */
    fun first(): Boolean {
        check(size != 0) { "Array is empty." }
        return items[0]
    }

    /**
     * Returns true if the array has one or more items.
     */
    fun notEmpty(): Boolean {
        return size > 0
    }

    /**
     * Returns true if the array is empty.
     */
    val isEmpty: Boolean
        get() = size == 0

    fun clear() {
        size = 0
    }

    /**
     * Reduces the size of the backing array to the size of the actual items. This is useful to release memory when many items
     * have been removed, or if it is known that more items will not be added.
     *
     * @return [.items]
     */
    fun shrink(): BooleanArray {
        if (items.size != size) resize(size)
        return items
    }

    /**
     * Increases the size of the backing array to accommodate the specified number of additional items. Useful before adding many
     * items to avoid multiple backing array resizes.
     *
     * @return [.items]
     */
    fun ensureCapacity(additionalCapacity: Int): BooleanArray {
        if (additionalCapacity < 0) throw java.lang.IllegalArgumentException("additionalCapacity must be >= 0: $additionalCapacity")
        val sizeNeeded = size + additionalCapacity
        if (sizeNeeded > items.size) resize(java.lang.Math.max(8, sizeNeeded))
        return items
    }

    /**
     * Sets the array size, leaving any values beyond the current size undefined.
     *
     * @return [.items]
     */
    fun setSize(newSize: Int): BooleanArray {
        if (newSize < 0) throw java.lang.IllegalArgumentException("newSize must be >= 0: $newSize")
        if (newSize > items.size) resize(java.lang.Math.max(8, newSize))
        size = newSize
        return items
    }

    protected fun resize(newSize: Int): BooleanArray {
        val newItems = BooleanArray(newSize)
        val items = items
        java.lang.System.arraycopy(items, 0, newItems, 0, java.lang.Math.min(size, newItems.size))
        this.items = newItems
        return newItems
    }

    fun reverse() {
        val items = items
        var i = 0
        val lastIndex = size - 1
        val n = size / 2
        while (i < n) {
            val ii = lastIndex - i
            val temp = items[i]
            items[i] = items[ii]
            items[ii] = temp
            i++
        }
    }

    fun shuffle() {
        val items = items
        for (i in size - 1 downTo 0) {
            val ii: Int = MathUtils.random(i)
            val temp = items[i]
            items[i] = items[ii]
            items[ii] = temp
        }
    }

    /**
     * Reduces the size of the array to the specified size. If the array is already smaller than the specified size, no action is
     * taken.
     */
    fun truncate(newSize: Int) {
        if (size > newSize) size = newSize
    }

    /**
     * Returns a random item from the array, or false if the array is empty.
     */
    fun random(): Boolean {
        return if (size == 0) false else items[MathUtils.random(0, size - 1)]
    }

    fun toArray(): BooleanArray {
        val array = BooleanArray(size)
        java.lang.System.arraycopy(items, 0, array, 0, size)
        return array
    }

    override fun hashCode(): Int {
        if (!ordered) return super.hashCode()
        val items = items
        var h = 1
        var i = 0
        val n = size
        while (i < n) {
            h = h * 31 + if (items[i]) 1231 else 1237
            i++
        }
        return h
    }

    /**
     * Returns false if either array is unordered.
     */
    override fun equals(`object`: Any?): Boolean {
        if (`object` === this) return true
        if (!ordered) return false
        if (`object` !is BooleanArray) return false
        val array = `object`
        if (!array.ordered) return false
        val n = size
        if (n != array.size) return false
        val items1 = items
        val items2 = array.items
        for (i in 0 until n) if (items1[i] != items2[i]) return false
        return true
    }

    override fun toString(): String {
        if (size == 0) return "[]"
        val items = items
        val buffer = StringBuilder(32)
        buffer.append('[')
        buffer.append(items[0])
        for (i in 1 until size) {
            buffer.append(", ")
            buffer.append(items[i])
        }
        buffer.append(']')
        return buffer.toString()
    }

    fun toString(separator: String?): String {
        if (size == 0) return ""
        val items = items
        val buffer = StringBuilder(32)
        buffer.append(items[0])
        for (i in 1 until size) {
            buffer.append(separator)
            buffer.append(items[i])
        }
        return buffer.toString()
    }

    companion object {
        /**
         * @see .BooleanArray
         */
        fun with(vararg array: Boolean): BooleanArray {
            return BooleanArray(array)
        }
    }
}
