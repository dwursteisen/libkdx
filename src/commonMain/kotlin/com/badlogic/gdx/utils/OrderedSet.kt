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

import com.badlogic.gdx.utils.ObjectIntMap
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.ObjectSet
import com.badlogic.gdx.utils.ObjectSet.ObjectSetIterator
import com.badlogic.gdx.utils.OrderedMap
import com.badlogic.gdx.utils.OrderedMap.OrderedMapEntries
import com.badlogic.gdx.utils.OrderedMap.OrderedMapKeys
import com.badlogic.gdx.utils.OrderedMap.OrderedMapValues
import java.lang.IllegalStateException
import java.util.NoSuchElementException

/**
 * An [ObjectSet] that also stores keys in an [Array] using the insertion order. [Iteration][.iterator] is
 * ordered and faster than an unordered set. Keys can also be accessed and the order changed using [.orderedItems]. There
 * is some additional overhead for put and remove. When used for faster iteration versus ObjectSet and the order does not actually
 * matter, copying during remove can be greatly reduced by setting [Array.ordered] to false for
 * [OrderedSet.orderedItems].
 *
 * @author Nathan Sweet
 */
class OrderedSet<T> : ObjectSet<T> {

    val items: Array<T>
    var iterator1: OrderedSetIterator<*>? = null
    var iterator2: OrderedSetIterator<*>? = null

    constructor() {
        items = Array()
    }

    constructor(initialCapacity: Int, loadFactor: Float) : super(initialCapacity, loadFactor) {
        items = Array(capacity)
    }

    constructor(initialCapacity: Int) : super(initialCapacity) {
        items = Array(capacity)
    }

    constructor(set: OrderedSet<out T>) : super(set) {
        items = Array(capacity)
        items.addAll(set.items)
    }

    fun add(key: T): Boolean {
        if (!super.add(key)) return false
        items.add(key)
        return true
    }

    fun add(key: T, index: Int): Boolean {
        if (!super.add(key)) {
            items.removeValue(key, true)
            items.insert(index, key)
            return false
        }
        items.insert(index, key)
        return true
    }

    override fun remove(key: T): Boolean {
        if (!super.remove(key)) return false
        items.removeValue(key, false)
        return true
    }

    fun removeIndex(index: Int): T {
        val key: T = items.removeIndex(index)
        super.remove(key)
        return key
    }

    override fun clear(maximumCapacity: Int) {
        items.clear()
        super.clear(maximumCapacity)
    }

    override fun clear() {
        items.clear()
        super.clear()
    }

    fun orderedItems(): Array<T> {
        return items
    }

    override operator fun iterator(): OrderedSetIterator<T?>? {
        if (Collections.allocateIterators) return OrderedSetIterator<Any?>(this)
        if (iterator1 == null) {
            iterator1 = OrderedSetIterator<Any?>(this)
            iterator2 = OrderedSetIterator<Any?>(this)
        }
        if (!iterator1.valid) {
            iterator1.reset()
            iterator1.valid = true
            iterator2.valid = false
            return iterator1
        }
        iterator2!!.reset()
        iterator2.valid = true
        iterator1.valid = false
        return iterator2
    }

    override fun toString(): String {
        if (size === 0) return "{}"
        val items: Array<T> = items.items
        val buffer: java.lang.StringBuilder = java.lang.StringBuilder(32)
        buffer.append('{')
        buffer.append(items[0])
        for (i in 1 until size) {
            buffer.append(", ")
            buffer.append(items[i])
        }
        buffer.append('}')
        return buffer.toString()
    }

    override fun toString(separator: String?): String {
        return items.toString(separator)
    }

    class OrderedSetIterator<T>(set: OrderedSet<T>) : ObjectSetIterator<T>(set) {
        private val items: Array<T>
        override fun reset() {
            nextIndex = 0
            hasNext = set.size > 0
        }

        override operator fun next(): T {
            if (!hasNext) throw NoSuchElementException()
            if (!valid) throw GdxRuntimeException("#iterator() cannot be used nested.")
            val key = items[nextIndex]
            nextIndex++
            hasNext = nextIndex < set.size
            return key
        }

        override fun remove() {
            check(nextIndex >= 0) { "next must be called before remove." }
            nextIndex--
            (set as OrderedSet<*>).removeIndex(nextIndex)
        }

        init {
            items = set.items
        }
    }
}
