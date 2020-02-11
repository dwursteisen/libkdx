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

import java.lang.IllegalStateException

/**
 * An array that queues removal during iteration until the iteration has completed. Queues any removals done after
 * [.begin] is called to occur once [.end] is called. This can allow code out of your control to remove items
 * without affecting iteration. Between begin and end, most mutator methods will throw IllegalStateException. Only
 * [.removeIndex], [.removeValue], [.removeRange], [.clear], and add
 * methods are allowed.
 *
 *
 * Note that DelayedRemovalArray is not for thread safety, only for removal during iteration.
 *
 *
 * Code using this class must not rely on items being removed immediately. Consider using [SnapshotArray] if this is a
 * problem.
 *
 * @author Nathan Sweet
 */
class DelayedRemovalArray<T> : Array<T> {

    private var iterating = 0
    private val remove = IntArray(0)
    private var clear = 0

    constructor() : super() {}
    constructor(array: Array<*>?) : super(array) {}
    constructor(ordered: Boolean, capacity: Int, arrayType: java.lang.Class?) : super(ordered, capacity, arrayType) {}
    constructor(ordered: Boolean, capacity: Int) : super(ordered, capacity) {}
    constructor(ordered: Boolean, array: kotlin.Array<T>?, startIndex: Int, count: Int) : super(ordered, array!!, startIndex, count) {}
    constructor(arrayType: java.lang.Class?) : super(arrayType) {}
    constructor(capacity: Int) : super(capacity) {}
    constructor(array: kotlin.Array<T>?) : super(array) {}

    fun begin() {
        iterating++
    }

    fun end() {
        check(iterating != 0) { "begin must be called before end." }
        iterating--
        if (iterating == 0) {
            if (clear > 0 && clear == size) {
                remove.clear()
                clear()
            } else {
                run {
                    var i = 0
                    val n = remove.size
                    while (i < n) {
                        val index = remove.pop()
                        if (index >= clear) removeIndex(index)
                        i++
                    }
                }
                for (i in clear - 1 downTo 0) removeIndex(i)
            }
            clear = 0
        }
    }

    private fun remove(index: Int) {
        if (index < clear) return
        var i = 0
        val n = remove.size
        while (i < n) {
            val removeIndex = remove[i]
            if (index == removeIndex) return
            if (index < removeIndex) {
                remove.insert(i, index)
                return
            }
            i++
        }
        remove.add(index)
    }

    override fun removeValue(value: T?, identity: Boolean): Boolean {
        if (iterating > 0) {
            val index = indexOf(value, identity)
            if (index == -1) return false
            remove(index)
            return true
        }
        return super.removeValue(value, identity)
    }

    override fun removeIndex(index: Int): T? {
        if (iterating > 0) {
            remove(index)
            return get(index)
        }
        return super.removeIndex(index)
    }

    override fun removeRange(start: Int, end: Int) {
        if (iterating > 0) {
            for (i in end downTo start) remove(i)
        } else super.removeRange(start, end)
    }

    override fun clear() {
        if (iterating > 0) {
            clear = size
            return
        }
        super.clear()
    }

    override fun set(index: Int, value: T) {
        check(iterating <= 0) { "Invalid between begin/end." }
        super.set(index, value)
    }

    override fun insert(index: Int, value: T) {
        check(iterating <= 0) { "Invalid between begin/end." }
        super.insert(index, value)
    }

    override fun swap(first: Int, second: Int) {
        check(iterating <= 0) { "Invalid between begin/end." }
        super.swap(first, second)
    }

    override fun pop(): T? {
        check(iterating <= 0) { "Invalid between begin/end." }
        return super.pop()
    }

    override fun sort() {
        check(iterating <= 0) { "Invalid between begin/end." }
        super.sort()
    }

    override fun sort(comparator: Comparator<in T>?) {
        check(iterating <= 0) { "Invalid between begin/end." }
        super.sort(comparator)
    }

    override fun reverse() {
        check(iterating <= 0) { "Invalid between begin/end." }
        super.reverse()
    }

    override fun shuffle() {
        check(iterating <= 0) { "Invalid between begin/end." }
        super.shuffle()
    }

    override fun truncate(newSize: Int) {
        check(iterating <= 0) { "Invalid between begin/end." }
        super.truncate(newSize)
    }

    override fun setSize(newSize: Int): kotlin.Array<T?> {
        check(iterating <= 0) { "Invalid between begin/end." }
        return super.setSize(newSize)
    }

    companion object {
        /**
         * @see .DelayedRemovalArray
         */
        fun <T> with(vararg array: T): DelayedRemovalArray<T?> {
            return DelayedRemovalArray<Any?>(array)
        }
    }
}
