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

import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.ArrayMap
import com.badlogic.gdx.utils.Base64Coder
import com.badlogic.gdx.utils.Base64Coder.CharMap
import com.badlogic.gdx.utils.reflect.ArrayReflection
import kotlin.math.max
import kotlin.reflect.KClass

/**
 * A resizable, ordered or unordered array of objects. If unordered, this class avoids a memory copy when removing elements (the
 * last element is moved to the removed element's position).
 *
 * @author Nathan Sweet
 */
open class Array<T> : Iterable<T> {

    /**
     * Provides direct access to the underlying array. If the Array's generic type is not Object, this field may only be accessed
     * if the [Array.Array] constructor was used.
     */
    var items: kotlin.Array<T?>
    var size = 0
    var ordered: Boolean
    private var iterable: ArrayIterable<*>? = null
    private var predicateIterable: Predicate.PredicateIterable<T>? = null

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
    constructor(ordered: Boolean = true, capacity: Int = 16) {
        this.ordered = ordered
        items = arrayOfNulls<Any>(capacity) as kotlin.Array<T?>
    }

    /**
     * Creates a new array with [.items] of the specified type.
     *
     * @param ordered  If false, methods that remove elements may change the order of other elements in the array, which avoids a
     * memory copy.
     * @param capacity Any elements added beyond this will cause the backing array to be grown.
     */
    constructor(ordered: Boolean, capacity: Int, arrayType: KClass<Any>) {
        this.ordered = ordered
        items = ArrayReflection.newInstance(arrayType, capacity)
    }

    /**
     * Creates an ordered array with [.items] of the specified type and a capacity of 16.
     */
    constructor(arrayType: KClass) : this(true, 16, arrayType) {}

    /**
     * Creates a new array containing the elements in the specified array. The new array will have the same type of backing array
     * and will be ordered if the specified array is ordered. The capacity is set to the number of elements, so any subsequent
     * elements added will cause the backing array to be grown.
     */
    constructor(array: Array<out T>) : this(array.ordered, array.size, array.items[0]::class) {
        size = array.size
        java.lang.System.arraycopy(array.items, 0, items, 0, size)
    }

    /**
     * Creates a new ordered array containing the elements in the specified array. The new array will have the same type of
     * backing array. The capacity is set to the number of elements, so any subsequent elements added will cause the backing array
     * to be grown.
     */
    constructor(array: kotlin.Array<T>) : this(true, array, 0, array.size) {}

    /**
     * Creates a new array containing the elements in the specified array. The new array will have the same type of backing array.
     * The capacity is set to the number of elements, so any subsequent elements added will cause the backing array to be grown.
     *
     * @param ordered If false, methods that remove elements may change the order of other elements in the array, which avoids a
     * memory copy.
     */
    constructor(ordered: Boolean, array: kotlin.Array<T>, start: Int, count: Int) : this(ordered, count, array.javaClass.getComponentType() as java.lang.Class) {
        size = count
        java.lang.System.arraycopy(array, start, items, 0, size)
    }

    fun add(value: T) {
        var items: kotlin.Array<T> = items
        if (size == items.size) items = resize(max(8, (size * 1.75f).toInt()))
        items[size++] = value
    }

    fun add(value1: T, value2: T) {
        var items: kotlin.Array<T> = items
        if (size + 1 >= items.size) items = resize(max(8, (size * 1.75f).toInt()))
        items[size] = value1
        items[size + 1] = value2
        size += 2
    }

    fun add(value1: T, value2: T, value3: T) {
        var items: kotlin.Array<T> = items
        if (size + 2 >= items.size) items = resize(max(8, (size * 1.75f).toInt()))
        items[size] = value1
        items[size + 1] = value2
        items[size + 2] = value3
        size += 3
    }

    fun add(value1: T, value2: T, value3: T, value4: T) {
        var items: kotlin.Array<T> = items
        if (size + 3 >= items.size) items = resize(max(8, (size * 1.8f).toInt())) // 1.75 isn't enough when size=5.
        items[size] = value1
        items[size + 1] = value2
        items[size + 2] = value3
        items[size + 3] = value4
        size += 4
    }

    fun addAll(array: Array<out T>) {
        addAll(array.items, 0, array.size)
    }

    fun addAll(array: Array<out T>, start: Int, count: Int) {
        if (start + count > array.size) throw IllegalArgumentException("start + count must be <= size: " + start + " + " + count + " <= " + array.size)
        addAll(array.items as kotlin.Array<T>, start, count)
    }

    fun addAll(vararg array: T) {
        addAll(array, 0, array.size)
    }

    fun addAll(array: kotlin.Array<T>?, start: Int, count: Int) {
        var items: kotlin.Array<T> = items
        val sizeNeeded = size + count
        if (sizeNeeded > items.size) items = resize(max(8, (sizeNeeded * 1.75f).toInt()))
        java.lang.System.arraycopy(array, start, items, size, count)
        size += count
    }

    operator fun get(index: Int): T? {
        if (index >= size) throw IndexOutOfBoundsException("index can't be >= size: $index >= $size")
        return items[index]
    }

    operator fun set(index: Int, value: T) {
        if (index >= size) throw IndexOutOfBoundsException("index can't be >= size: $index >= $size")
        items[index] = value
    }

    fun insert(index: Int, value: T) {
        if (index > size) throw IndexOutOfBoundsException("index can't be > size: $index > $size")
        var items: kotlin.Array<T> = items
        if (size == items.size) items = resize(max(8, (size * 1.75f).toInt()))
        if (ordered) java.lang.System.arraycopy(items, index, items, index + 1, size - index) else items[size] = items[index]
        size++
        items[index] = value
    }

    fun swap(first: Int, second: Int) {
        if (first >= size) throw IndexOutOfBoundsException("first can't be >= size: $first >= $size")
        if (second >= size) throw IndexOutOfBoundsException("second can't be >= size: $second >= $size")
        val items: kotlin.Array<T> = items
        val firstValue = items[first]
        items[first] = items[second]
        items[second] = firstValue
    }

    /**
     * Returns true if this array contains the specified value.
     *
     * @param value    May be null.
     * @param identity If true, == comparison will be used. If false, .equals() comparison will be used.
     */
    fun contains(value: T?, identity: Boolean): Boolean {
        val items: kotlin.Array<T> = items
        var i = size - 1
        if (identity || value == null) {
            while (i >= 0) if (items[i--] === value) return true
        } else {
            while (i >= 0) if (value == items[i--]) return true
        }
        return false
    }

    /**
     * Returns true if this array contains all the specified values.
     *
     * @param values   May contains nulls.
     * @param identity If true, == comparison will be used. If false, .equals() comparison will be used.
     */
    fun containsAll(values: Array<out T>, identity: Boolean): Boolean {
        val items: kotlin.Array<T> = values.items as kotlin.Array<T>
        var i = 0
        val n = values.size
        while (i < n) {
            if (!contains(items[i], identity)) return false
            i++
        }
        return true
    }

    /**
     * Returns true if this array contains any the specified values.
     *
     * @param values   May contains nulls.
     * @param identity If true, == comparison will be used. If false, .equals() comparison will be used.
     */
    fun containsAny(values: Array<out T>, identity: Boolean): Boolean {
        val items: kotlin.Array<T> = values.items as kotlin.Array<T>
        var i = 0
        val n = values.size
        while (i < n) {
            if (contains(items[i], identity)) return true
            i++
        }
        return false
    }

    /**
     * Returns the index of first occurrence of value in the array, or -1 if no such value exists.
     *
     * @param value    May be null.
     * @param identity If true, == comparison will be used. If false, .equals() comparison will be used.
     * @return An index of first occurrence of value in array or -1 if no such value exists
     */
    fun indexOf(value: T?, identity: Boolean): Int {
        val items: kotlin.Array<T> = items
        if (identity || value == null) {
            var i = 0
            val n = size
            while (i < n) {
                if (items[i] === value) return i
                i++
            }
        } else {
            var i = 0
            val n = size
            while (i < n) {
                if (value == items[i]) return i
                i++
            }
        }
        return -1
    }

    /**
     * Returns an index of last occurrence of value in array or -1 if no such value exists. Search is started from the end of an
     * array.
     *
     * @param value    May be null.
     * @param identity If true, == comparison will be used. If false, .equals() comparison will be used.
     * @return An index of last occurrence of value in array or -1 if no such value exists
     */
    fun lastIndexOf(value: T?, identity: Boolean): Int {
        val items: kotlin.Array<T> = items
        if (identity || value == null) {
            for (i in size - 1 downTo 0) if (items[i] === value) return i
        } else {
            for (i in size - 1 downTo 0) if (value == items[i]) return i
        }
        return -1
    }

    /**
     * Removes the first instance of the specified value in the array.
     *
     * @param value    May be null.
     * @param identity If true, == comparison will be used. If false, .equals() comparison will be used.
     * @return true if value was found and removed, false otherwise
     */
    fun removeValue(value: T?, identity: Boolean): Boolean {
        val items: kotlin.Array<T> = items
        if (identity || value == null) {
            var i = 0
            val n = size
            while (i < n) {
                if (items[i] === value) {
                    removeIndex(i)
                    return true
                }
                i++
            }
        } else {
            var i = 0
            val n = size
            while (i < n) {
                if (value == items[i]) {
                    removeIndex(i)
                    return true
                }
                i++
            }
        }
        return false
    }

    /**
     * Removes and returns the item at the specified index.
     */
    fun removeIndex(index: Int): T? {
        if (index >= size) throw IndexOutOfBoundsException("index can't be >= size: $index >= $size")
        val items = items
        size--
        if (ordered) java.lang.System.arraycopy(items, index + 1, items, index, size - index) else items[index] = items[size]
        items[size] = null
        return items[index]
    }

    /**
     * Removes the items between the specified indices, inclusive.
     */
    fun removeRange(start: Int, end: Int) {
        val n = size
        if (end >= n) throw IndexOutOfBoundsException("end can't be >= size: $end >= $size")
        if (start > end) throw IndexOutOfBoundsException("start can't be > end: $start > $end")
        val items = items
        val count = end - start + 1
        val lastIndex = n - count
        if (ordered) java.lang.System.arraycopy(items, start + count, items, start, n - (start + count)) else {
            val i: Int = max(lastIndex, end + 1)
            java.lang.System.arraycopy(items, i, items, start, n - i)
        }
        for (i in lastIndex until n) items[i] = null
        size = n - count
    }

    /**
     * Removes from this array all of elements contained in the specified array.
     *
     * @param identity True to use ==, false to use .equals().
     * @return true if this array was modified.
     */
    fun removeAll(array: Array<out T>, identity: Boolean): Boolean {
        var size = size
        val startSize = size
        val items: kotlin.Array<T> = items
        if (identity) {
            var i = 0
            val n = array.size
            while (i < n) {
                val item: T = array[i]!!
                for (ii in 0 until size) {
                    if (item === items[ii]) {
                        removeIndex(ii)
                        size--
                        break
                    }
                }
                i++
            }
        } else {
            var i = 0
            val n = array.size
            while (i < n) {
                val item: T = array[i]!!
                for (ii in 0 until size) {
                    if (item == items[ii]) {
                        removeIndex(ii)
                        size--
                        break
                    }
                }
                i++
            }
        }
        return size != startSize
    }

    /**
     * Removes and returns the last item.
     */
    fun pop(): T? {
        check(size != 0) { "Array is empty." }
        --size
        val item = items[size]
        items[size] = null
        return item
    }

    /**
     * Returns the last item.
     */
    fun peek(): T? {
        check(size != 0) { "Array is empty." }
        return items[size - 1]
    }

    /**
     * Returns the first item.
     */
    fun first(): T? {
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
        val items = items
        var i = 0
        val n = size
        while (i < n) {
            items[i] = null
            i++
        }
        size = 0
    }

    /**
     * Reduces the size of the backing array to the size of the actual items. This is useful to release memory when many items
     * have been removed, or if it is known that more items will not be added.
     *
     * @return [.items]
     */
    fun shrink(): kotlin.Array<T?> {
        if (items.size != size) resize(size)
        return items
    }

    /**
     * Increases the size of the backing array to accommodate the specified number of additional items. Useful before adding many
     * items to avoid multiple backing array resizes.
     *
     * @return [.items]
     */
    fun ensureCapacity(additionalCapacity: Int): kotlin.Array<T?> {
        if (additionalCapacity < 0) throw IllegalArgumentException("additionalCapacity must be >= 0: $additionalCapacity")
        val sizeNeeded = size + additionalCapacity
        if (sizeNeeded > items.size) resize(max(8, sizeNeeded))
        return items
    }

    /**
     * Sets the array size, leaving any values beyond the current size null.
     *
     * @return [.items]
     */
    fun setSize(newSize: Int): kotlin.Array<T?> {
        truncate(newSize)
        if (newSize > items.size) resize(max(8, newSize))
        size = newSize
        return items
    }

    /**
     * Creates a new backing array with the specified size containing the current items.
     */
    protected fun resize(newSize: Int): kotlin.Array<T> {
        val items: kotlin.Array<T> = items
        val newItems = ArrayReflection.newInstance(items.javaClass.getComponentType(), newSize) as kotlin.Array<T>
        java.lang.System.arraycopy(items, 0, newItems, 0, java.lang.Math.min(size, newItems.size))
        this.items = newItems
        return newItems
    }

    /**
     * Sorts this array. The array elements must implement [Comparable]. This method is not thread safe (uses
     * [Sort.instance]).
     */
    fun sort() {
        Sort.instance().sort(items, 0, size)
    }

    /**
     * Sorts the array. This method is not thread safe (uses [Sort.instance]).
     */
    fun sort(comparator: Comparator<in T>?) {
        Sort.instance().sort(items, comparator, 0, size)
    }

    /**
     * Selects the nth-lowest element from the Array according to Comparator ranking. This might partially sort the Array. The
     * array must have a size greater than 0, or a [com.badlogic.gdx.utils.GdxRuntimeException] will be thrown.
     *
     * @param comparator used for comparison
     * @param kthLowest  rank of desired object according to comparison, n is based on ordinal numbers, not array indices. for min
     * value use 1, for max value use size of array, using 0 results in runtime exception.
     * @return the value of the Nth lowest ranked object.
     * @see Select
     */
    fun selectRanked(comparator: Comparator<T>?, kthLowest: Int): T {
        if (kthLowest < 1) {
            throw GdxRuntimeException("nth_lowest must be greater than 0, 1 = first, 2 = second...")
        }
        return Select.instance()!!.select(items, comparator, kthLowest, size)
    }

    /**
     * @param comparator used for comparison
     * @param kthLowest  rank of desired object according to comparison, n is based on ordinal numbers, not array indices. for min
     * value use 1, for max value use size of array, using 0 results in runtime exception.
     * @return the index of the Nth lowest ranked object.
     * @see Array.selectRanked
     */
    fun selectRankedIndex(comparator: Comparator<T>?, kthLowest: Int): Int {
        if (kthLowest < 1) {
            throw GdxRuntimeException("nth_lowest must be greater than 0, 1 = first, 2 = second...")
        }
        return Select.instance()!!.selectIndex(items, comparator, kthLowest, size)
    }

    fun reverse() {
        val items: kotlin.Array<T> = items
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
        val items: kotlin.Array<T> = items
        for (i in size - 1 downTo 0) {
            val ii: Int = MathUtils.random(i)
            val temp = items[i]
            items[i] = items[ii]
            items[ii] = temp
        }
    }

    /**
     * Returns an iterator for the items in the array. Remove is supported.
     *
     *
     * If [Collections.allocateIterators] is false, the same iterator instance is returned each time this method is called.
     * Use the [ArrayIterator] constructor for nested or multithreaded iteration.
     */
    override fun iterator(): Iterator<T> {
        if (Collections.allocateIterators) return ArrayIterator<Any?>(this, true)
        if (iterable == null) iterable = ArrayIterable<Any?>(this)
        return iterable.iterator()
    }

    /**
     * Returns an iterable for the selected items in the array. Remove is supported, but not between hasNext() and next().
     *
     *
     * If [Collections.allocateIterators] is false, the same iterable instance is returned each time this method is called.
     * Use the [Predicate.PredicateIterable] constructor for nested or multithreaded iteration.
     */
    fun select(predicate: Predicate<T>?): Iterable<T> {
        if (Collections.allocateIterators) Predicate.PredicateIterable(this, predicate)
        if (predicateIterable == null) predicateIterable = Predicate.PredicateIterable(this, predicate) else predicateIterable.set(this, predicate)
        return predicateIterable
    }

    /**
     * Reduces the size of the array to the specified size. If the array is already smaller than the specified size, no action is
     * taken.
     */
    fun truncate(newSize: Int) {
        if (newSize < 0) throw IllegalArgumentException("newSize must be >= 0: $newSize")
        if (size <= newSize) return
        for (i in newSize until size) items[i] = null
        size = newSize
    }

    /**
     * Returns a random item from the array, or null if the array is empty.
     */
    fun random(): T? {
        return if (size == 0) null else items[MathUtils.random(0, size - 1)]
    }

    /**
     * Returns the items as an array. Note the array is typed, so the [.Array] constructor must have been used.
     * Otherwise use [.toArray] to specify the array type.
     */
    fun toArray(): kotlin.Array<T> {
        return toArray(items.javaClass.getComponentType()) as kotlin.Array<T>
    }

    fun <V> toArray(type: java.lang.Class<V>?): kotlin.Array<V> {
        val result = ArrayReflection.newInstance(type, size) as kotlin.Array<V>
        java.lang.System.arraycopy(items, 0, result, 0, size)
        return result
    }

    override fun hashCode(): Int {
        if (!ordered) return super.hashCode()
        val items: kotlin.Array<Any> = items
        var h = 1
        var i = 0
        val n = size
        while (i < n) {
            h *= 31
            val item = items[i]
            if (item != null) h += item.hashCode()
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
        if (`object` !is Array<*>) return false
        val array = `object`
        if (!array.ordered) return false
        val n = size
        if (n != array.size) return false
        val items1: kotlin.Array<Any> = items
        val items2: kotlin.Array<Any> = array.items
        for (i in 0 until n) {
            val o1 = items1[i]
            val o2 = items2[i]
            if (!(if (o1 == null) o2 == null else o1 == o2)) return false
        }
        return true
    }

    /**
     * Uses == for comparison of each item. Returns false if either array is unordered.
     */
    fun equalsIdentity(`object`: Any): Boolean {
        if (`object` === this) return true
        if (!ordered) return false
        if (`object` !is Array<*>) return false
        val array = `object`
        if (!array.ordered) return false
        val n = size
        if (n != array.size) return false
        val items1: kotlin.Array<Any> = items
        val items2: kotlin.Array<Any> = array.items
        for (i in 0 until n) if (items1[i] !== items2[i]) return false
        return true
    }

    override fun toString(): String {
        if (size == 0) return "[]"
        val items: kotlin.Array<T> = items
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
        val items: kotlin.Array<T> = items
        val buffer = StringBuilder(32)
        buffer.append(items[0])
        for (i in 1 until size) {
            buffer.append(separator)
            buffer.append(items[i])
        }
        return buffer.toString()
    }

    class ArrayIterator<T> // ArrayIterable<T> iterable;
    @JvmOverloads constructor(private val array: Array<T>, private val allowRemove: Boolean = true) : Iterator<T>, Iterable<T> {

        var index = 0
        var valid = true
        override fun hasNext(): Boolean {
            if (!valid) {
// System.out.println(iterable.lastAcquire);
                throw GdxRuntimeException("#iterator() cannot be used nested.")
            }
            return index < array.size
        }

        override fun next(): T {
            if (index >= array.size) throw NoSuchElementException(index.toString())
            if (!valid) {
// System.out.println(iterable.lastAcquire);
                throw GdxRuntimeException("#iterator() cannot be used nested.")
            }
            return array.items[index++]
        }

        fun remove() {
            if (!allowRemove) throw GdxRuntimeException("Remove not allowed.")
            index--
            array.removeIndex(index)
        }

        fun reset() {
            index = 0
        }

        override fun iterator(): Iterator<T> {
            return this
        }
    }

    class ArrayIterable<T> @JvmOverloads constructor(array: Array<T>, allowRemove: Boolean = true) : Iterable<T> {
        private val array: Array<T?>
        private val allowRemove: Boolean
        private var iterator1: ArrayIterator<*>? = null
        private var iterator2: ArrayIterator<*>? = null

        /**
         * @see Collections.allocateIterators
         */
        override fun iterator(): Iterator<T> {
            if (Collections.allocateIterators) return ArrayIterator<Any?>(array, allowRemove)
            // lastAcquire.getBuffer().setLength(0);
// new Throwable().printStackTrace(new java.io.PrintWriter(lastAcquire));
            if (iterator1 == null) {
                iterator1 = ArrayIterator<Any?>(array, allowRemove)
                iterator2 = ArrayIterator<Any?>(array, allowRemove)
                // iterator1.iterable = this;
// iterator2.iterable = this;
            }
            if (!iterator1.valid) {
                iterator1.index = 0
                iterator1.valid = true
                iterator2!!.valid = false
                return iterator1
            }
            iterator2!!.index = 0
            iterator2!!.valid = true
            iterator1.valid = false
            return iterator2
        }

        // java.io.StringWriter lastAcquire = new java.io.StringWriter();
        init {
            this.array = array
            this.allowRemove = allowRemove
        }
    }

    companion object {
        /**
         * @see .Array
         */
        fun <T> of(arrayType: java.lang.Class<T>?): Array<T?> {
            return Array<Any?>(arrayType)
        }

        /**
         * @see .Array
         */
        fun <T> of(ordered: Boolean, capacity: Int, arrayType: java.lang.Class<T>?): Array<T?> {
            return Array<Any?>(ordered, capacity, arrayType)
        }

        /**
         * @see .Array
         */
        fun <T> with(vararg array: T): Array<T?> {
            return Array<Any?>(array)
        }
    }
}
