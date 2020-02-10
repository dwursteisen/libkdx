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
 * An array that allows modification during iteration. Guarantees that array entries provided by [.begin] between indexes
 * 0 and [.size] at the time begin was called will not be modified until [.end] is called. If modification of the
 * SnapshotArray occurs between begin/end, the backing array is copied prior to the modification, ensuring that the backing array
 * that was returned by [.begin] is unaffected. To avoid allocation, an attempt is made to reuse any extra array created
 * as a result of this copy on subsequent copies.
 *
 *
 * Note that SnapshotArray is not for thread safety, only for modification during iteration.
 *
 *
 * It is suggested iteration be done in this specific way:
 *
 * <pre>
 * SnapshotArray array = new SnapshotArray();
 * // ...
 * Object[] items = array.begin();
 * for (int i = 0, n = array.size; i &lt; n; i++) {
 * Object item = items[i];
 * // ...
 * }
 * array.end();
</pre> *
 *
 * @author Nathan Sweet
 */
class SnapshotArray<T> : Array<T> {

    private var snapshot: Array<T?>?
    private var recycled: Array<T?>?
    private var snapshots = 0

    constructor() : super() {}
    constructor(array: Array?) : super(array) {}
    constructor(ordered: Boolean, capacity: Int, arrayType: java.lang.Class?) : super(ordered, capacity, arrayType) {}
    constructor(ordered: Boolean, capacity: Int) : super(ordered, capacity) {}
    constructor(ordered: Boolean, array: Array<T>?, startIndex: Int, count: Int) : super(ordered, array, startIndex, count) {}
    constructor(arrayType: java.lang.Class?) : super(arrayType) {}
    constructor(capacity: Int) : super(capacity) {}
    constructor(array: Array<T>?) : super(array) {}

    /**
     * Returns the backing array, which is guaranteed to not be modified before [.end].
     */
    fun begin(): Array<T> {
        modified()
        snapshot = items
        snapshots++
        return items
    }

    /**
     * Releases the guarantee that the array returned by [.begin] won't be modified.
     */
    fun end() {
        snapshots = java.lang.Math.max(0, snapshots - 1)
        if (snapshot == null) return
        if (snapshot != items && snapshots == 0) {
            // The backing array was copied, keep around the old array.
            recycled = snapshot
            var i = 0
            val n = recycled!!.size
            while (i < n) {
                recycled!![i] = null
                i++
            }
        }
        snapshot = null
    }

    private fun modified() {
        if (snapshot == null || snapshot != items) return
        // Snapshot is in use, copy backing array to recycled array or create new backing array.
        if (recycled != null && recycled.size >= size) {
            java.lang.System.arraycopy(items, 0, recycled, 0, size)
            items = recycled
            recycled = null
        } else resize(items.length)
    }

    override fun set(index: Int, value: T) {
        modified()
        super.set(index, value)
    }

    fun insert(index: Int, value: T) {
        modified()
        super.insert(index, value)
    }

    fun swap(first: Int, second: Int) {
        modified()
        super.swap(first, second)
    }

    fun removeValue(value: T, identity: Boolean): Boolean {
        modified()
        return super.removeValue(value, identity)
    }

    fun removeIndex(index: Int): T {
        modified()
        return super.removeIndex(index)
    }

    fun removeRange(start: Int, end: Int) {
        modified()
        super.removeRange(start, end)
    }

    fun removeAll(array: Array<out T>?, identity: Boolean): Boolean {
        modified()
        return super.removeAll(array, identity)
    }

    fun pop(): T {
        modified()
        return super.pop()
    }

    fun clear() {
        modified()
        super.clear()
    }

    fun sort() {
        modified()
        super.sort()
    }

    fun sort(comparator: Comparator<in T>?) {
        modified()
        super.sort(comparator)
    }

    fun reverse() {
        modified()
        super.reverse()
    }

    fun shuffle() {
        modified()
        super.shuffle()
    }

    fun truncate(newSize: Int) {
        modified()
        super.truncate(newSize)
    }

    fun setSize(newSize: Int): Array<T> {
        modified()
        return super.setSize(newSize)
    }

    companion object {
        /**
         * @see .SnapshotArray
         */
        fun <T> with(vararg array: T): SnapshotArray<T> {
            return SnapshotArray<Any?>(array)
        }
    }
}
