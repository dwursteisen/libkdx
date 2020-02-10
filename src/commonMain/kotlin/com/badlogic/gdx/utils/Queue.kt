/*******************************************************************************
 * Copyright 2015 See AUTHORS file.
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

import com.badlogic.gdx.utils.Pools
import com.badlogic.gdx.utils.Predicate.PredicateIterator
import com.badlogic.gdx.utils.PropertiesUtils
import com.badlogic.gdx.utils.Scaling
import com.badlogic.gdx.utils.ScreenUtils
import java.lang.IndexOutOfBoundsException
import java.lang.NullPointerException
import java.lang.RuntimeException
import kotlin.jvm.Throws

/**
 * A resizable, ordered array of objects with efficient add and remove at the beginning and end. Values in the backing array may
 * wrap back to the beginning, making add and remove at the beginning and end O(1) (unless the backing array needs to resize when
 * adding). Deque functionality is provided via [.removeLast] and [.addFirst].
 */
class Queue<T> : Iterable<T> {

    /**
     * Contains the values in the queue. Head and tail indices go in a circle around this array, wrapping at the end.
     */
    protected var values: Array<T>

    /**
     * Index of first element. Logically smaller than tail. Unless empty, it points to a valid element inside queue.
     */
    protected var head = 0

    /**
     * Index of last element. Logically bigger than head. Usually points to an empty position, but points to the head when full
     * (size == values.length).
     */
    protected var tail = 0

    /**
     * Number of elements in the queue.
     */
    var size = 0
    private var iterable: QueueIterable<*>? = null
    /**
     * Creates a new Queue which can hold the specified number of values without needing to resize backing array.
     */
    /**
     * Creates a new Queue which can hold 16 values without needing to resize backing array.
     */
    @JvmOverloads
    constructor(initialSize: Int = 16) {
        // noinspection unchecked
        values = arrayOfNulls<Any>(initialSize) as Array<T?>
    }

    /**
     * Creates a new Queue which can hold the specified number of values without needing to resize backing array. This creates
     * backing array of the specified type via reflection, which is necessary only when accessing the backing array directly.
     */
    constructor(initialSize: Int, type: java.lang.Class<T>?) {
        // noinspection unchecked
        values = ArrayReflection.newInstance(type, initialSize)
    }

    /**
     * Append given object to the tail. (enqueue to tail) Unless backing array needs resizing, operates in O(1) time.
     *
     * @param object can be null
     */
    fun addLast(`object`: T) {
        var values = values
        if (size == values.size) {
            resize(values.size shl 1) // * 2
            values = this.values
        }
        values[tail++] = `object`
        if (tail == values.size) {
            tail = 0
        }
        size++
    }

    /**
     * Prepend given object to the head. (enqueue to head) Unless backing array needs resizing, operates in O(1) time.
     *
     * @param object can be null
     * @see .addLast
     */
    fun addFirst(`object`: T) {
        var values = values
        if (size == values.size) {
            resize(values.size shl 1) // * 2
            values = this.values
        }
        var head = head
        head--
        if (head == -1) {
            head = values.size - 1
        }
        values[head] = `object`
        this.head = head
        size++
    }

    /**
     * Increases the size of the backing array to accommodate the specified number of additional items. Useful before adding many
     * items to avoid multiple backing array resizes.
     */
    fun ensureCapacity(additional: Int) {
        val needed = size + additional
        if (values.size < needed) {
            resize(needed)
        }
    }

    /**
     * Resize backing array. newSize must be bigger than current size.
     */
    protected fun resize(newSize: Int) {
        val values = values
        val head = head
        val tail = tail
        val newArray = ArrayReflection.newInstance(values.javaClass.getComponentType(), newSize) as Array<T>
        if (head < tail) {
            // Continuous
            java.lang.System.arraycopy(values, head, newArray, 0, tail - head)
        } else if (size > 0) {
            // Wrapped
            val rest = values.size - head
            java.lang.System.arraycopy(values, head, newArray, 0, rest)
            java.lang.System.arraycopy(values, 0, newArray, rest, tail)
        }
        this.values = newArray
        this.head = 0
        this.tail = size
    }

    /**
     * Remove the first item from the queue. (dequeue from head) Always O(1).
     *
     * @return removed object
     * @throws NoSuchElementException when queue is empty
     */
    fun removeFirst(): T? {
        if (size == 0) {
            // Underflow
            throw NoSuchElementException("Queue is empty.")
        }
        val values: Array<T?> = values
        val result = values[head]
        values[head] = null
        head++
        if (head == values.size) {
            head = 0
        }
        size--
        return result
    }

    /**
     * Remove the last item from the queue. (dequeue from tail) Always O(1).
     *
     * @return removed object
     * @throws NoSuchElementException when queue is empty
     * @see .removeFirst
     */
    fun removeLast(): T? {
        if (size == 0) {
            throw NoSuchElementException("Queue is empty.")
        }
        val values: Array<T?> = values
        var tail = tail
        tail--
        if (tail == -1) {
            tail = values.size - 1
        }
        val result = values[tail]
        values[tail] = null
        this.tail = tail
        size--
        return result
    }

    /**
     * Returns the index of first occurrence of value in the queue, or -1 if no such value exists.
     *
     * @param identity If true, == comparison will be used. If false, .equals() comparison will be used.
     * @return An index of first occurrence of value in queue or -1 if no such value exists
     */
    fun indexOf(value: T?, identity: Boolean): Int {
        if (size == 0) return -1
        val values = values
        val head = head
        val tail = tail
        if (identity || value == null) {
            if (head < tail) {
                for (i in head until tail) if (values[i] === value) return i - head
            } else {
                run {
                    var i = head
                    val n = values.size
                    while (i < n) {
                        if (values[i] === value) return i - head
                        i++
                    }
                }
                for (i in 0 until tail) if (values[i] === value) return i + values.size - head
            }
        } else {
            if (head < tail) {
                for (i in head until tail) if (value == values[i]) return i - head
            } else {
                run {
                    var i = head
                    val n = values.size
                    while (i < n) {
                        if (value == values[i]) return i - head
                        i++
                    }
                }
                for (i in 0 until tail) if (value == values[i]) return i + values.size - head
            }
        }
        return -1
    }

    /**
     * Removes the first instance of the specified value in the queue.
     *
     * @param identity If true, == comparison will be used. If false, .equals() comparison will be used.
     * @return true if value was found and removed, false otherwise
     */
    fun removeValue(value: T, identity: Boolean): Boolean {
        val index = indexOf(value, identity)
        if (index == -1) return false
        removeIndex(index)
        return true
    }

    /**
     * Removes and returns the item at the specified index.
     */
    fun removeIndex(index: Int): T? {
        var index = index
        if (index < 0) throw IndexOutOfBoundsException("index can't be < 0: $index")
        if (index >= size) throw IndexOutOfBoundsException("index can't be >= size: $index >= $size")
        val values: Array<T?> = values
        val head = head
        val tail = tail
        index += head
        val value: T?
        if (head < tail) { // index is between head and tail.
            value = values[index]
            java.lang.System.arraycopy(values, index + 1, values, index, tail - index)
            values[tail] = null
            this.tail--
        } else if (index >= values.size) { // index is between 0 and tail.
            index -= values.size
            value = values[index]
            java.lang.System.arraycopy(values, index + 1, values, index, tail - index)
            this.tail--
        } else { // index is between head and values.length.
            value = values[index]
            java.lang.System.arraycopy(values, head, values, head + 1, index - head)
            values[head] = null
            this.head++
            if (this.head == values.size) {
                this.head = 0
            }
        }
        size--
        return value
    }

    /**
     * Returns true if the queue has one or more items.
     */
    fun notEmpty(): Boolean {
        return size > 0
    }

    /**
     * Returns true if the queue is empty.
     */
    val isEmpty: Boolean
        get() = size == 0

    /**
     * Returns the first (head) item in the queue (without removing it).
     *
     * @throws NoSuchElementException when queue is empty
     * @see .addFirst
     * @see .removeFirst
     */
    fun first(): T {
        if (size == 0) {
            // Underflow
            throw NoSuchElementException("Queue is empty.")
        }
        return values[head]
    }

    /**
     * Returns the last (tail) item in the queue (without removing it).
     *
     * @throws NoSuchElementException when queue is empty
     * @see .addLast
     * @see .removeLast
     */
    fun last(): T {
        if (size == 0) {
            // Underflow
            throw NoSuchElementException("Queue is empty.")
        }
        val values = values
        var tail = tail
        tail--
        if (tail == -1) {
            tail = values.size - 1
        }
        return values[tail]
    }

    /**
     * Retrieves the value in queue without removing it. Indexing is from the front to back, zero based. Therefore get(0) is the
     * same as [.first].
     *
     * @throws IndexOutOfBoundsException when the index is negative or >= size
     */
    operator fun get(index: Int): T {
        if (index < 0) throw IndexOutOfBoundsException("index can't be < 0: $index")
        if (index >= size) throw IndexOutOfBoundsException("index can't be >= size: $index >= $size")
        val values = values
        var i = head + index
        if (i >= values.size) {
            i -= values.size
        }
        return values[i]
    }

    /**
     * Removes all values from this queue. Values in backing array are set to null to prevent memory leak, so this operates in
     * O(n).
     */
    fun clear() {
        if (size == 0) return
        val values: Array<T?> = values
        val head = head
        val tail = tail
        if (head < tail) {
            // Continuous
            for (i in head until tail) {
                values[i] = null
            }
        } else {
            // Wrapped
            for (i in head until values.size) {
                values[i] = null
            }
            for (i in 0 until tail) {
                values[i] = null
            }
        }
        this.head = 0
        this.tail = 0
        size = 0
    }

    /**
     * Returns an iterator for the items in the queue. Remove is supported.
     *
     *
     * If [Collections.allocateIterators] is false, the same iterator instance is returned each time this method is called.
     * Use the [QueueIterator] constructor for nested or multithreaded iteration.
     */
    override fun iterator(): Iterator<T> {
        if (Collections.allocateIterators) return QueueIterator<Any?>(this, true)
        if (iterable == null) iterable = QueueIterable<Any?>(this)
        return iterable.iterator()
    }

    override fun toString(): String {
        if (size == 0) {
            return "[]"
        }
        val values = values
        val head = head
        val tail = tail
        val sb = StringBuilder(64)
        sb.append('[')
        sb.append(values[head])
        var i = (head + 1) % values.size
        while (i != tail) {
            sb.append(", ").append(values[i])
            i = (i + 1) % values.size
        }
        sb.append(']')
        return sb.toString()
    }

    fun toString(separator: String?): String {
        if (size == 0) return ""
        val values = values
        val head = head
        val tail = tail
        val sb = StringBuilder(64)
        sb.append(values[head])
        var i = (head + 1) % values.size
        while (i != tail) {
            sb.append(separator).append(values[i])
            i = (i + 1) % values.size
        }
        return sb.toString()
    }

    override fun hashCode(): Int {
        val size = size
        val values = values
        val backingLength = values.size
        var index = head
        var hash = size + 1
        for (s in 0 until size) {
            val value: T? = values[index]
            hash *= 31
            if (value != null) hash += value.hashCode()
            index++
            if (index == backingLength) index = 0
        }
        return hash
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || o !is Queue<*>) return false
        val q = o
        val size = size
        if (q.size != size) return false
        val myValues = values
        val myBackingLength = myValues.size
        val itsValues: Array<Any> = q.values
        val itsBackingLength = itsValues.size
        var myIndex = head
        var itsIndex = q.head
        for (s in 0 until size) {
            val myValue: T? = myValues[myIndex]
            val itsValue = itsValues[itsIndex]
            if (!(if (myValue == null) itsValue == null else myValue == itsValue)) return false
            myIndex++
            itsIndex++
            if (myIndex == myBackingLength) myIndex = 0
            if (itsIndex == itsBackingLength) itsIndex = 0
        }
        return true
    }

    /**
     * Uses == for comparison of each item.
     */
    fun equalsIdentity(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || o !is Queue<*>) return false
        val q = o
        val size = size
        if (q.size != size) return false
        val myValues = values
        val myBackingLength = myValues.size
        val itsValues: Array<Any> = q.values
        val itsBackingLength = itsValues.size
        var myIndex = head
        var itsIndex = q.head
        for (s in 0 until size) {
            if (myValues[myIndex] !== itsValues[itsIndex]) return false
            myIndex++
            itsIndex++
            if (myIndex == myBackingLength) myIndex = 0
            if (itsIndex == itsBackingLength) itsIndex = 0
        }
        return true
    }

    class QueueIterator<T> // QueueIterable<T> iterable;
    @JvmOverloads constructor(private val queue: Queue<T>, private val allowRemove: Boolean = true) : Iterator<T>, Iterable<T> {

        var index = 0
        var valid = true
        override fun hasNext(): Boolean {
            if (!valid) {
// System.out.println(iterable.lastAcquire);
                throw GdxRuntimeException("#iterator() cannot be used nested.")
            }
            return index < queue.size
        }

        override fun next(): T {
            if (index >= queue.size) throw NoSuchElementException(index.toString())
            if (!valid) {
// System.out.println(iterable.lastAcquire);
                throw GdxRuntimeException("#iterator() cannot be used nested.")
            }
            return queue[index++]
        }

        fun remove() {
            if (!allowRemove) throw GdxRuntimeException("Remove not allowed.")
            index--
            queue.removeIndex(index)
        }

        fun reset() {
            index = 0
        }

        override fun iterator(): Iterator<T> {
            return this
        }
    }

    class QueueIterable<T> @JvmOverloads constructor(queue: Queue<T>, allowRemove: Boolean = true) : Iterable<T> {
        private val queue: Queue<T?>
        private val allowRemove: Boolean
        private var iterator1: QueueIterator<*>? = null
        private var iterator2: QueueIterator<*>? = null

        /**
         * @see Collections.allocateIterators
         */
        override fun iterator(): Iterator<T> {
            if (Collections.allocateIterators) return QueueIterator<Any?>(queue, allowRemove)
            // lastAcquire.getBuffer().setLength(0);
// new Throwable().printStackTrace(new java.io.PrintWriter(lastAcquire));
            if (iterator1 == null) {
                iterator1 = QueueIterator<Any?>(queue, allowRemove)
                iterator2 = QueueIterator<Any?>(queue, allowRemove)
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
            this.queue = queue
            this.allowRemove = allowRemove
        }
    }
}
