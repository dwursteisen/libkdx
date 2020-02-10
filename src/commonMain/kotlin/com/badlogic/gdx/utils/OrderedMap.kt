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
import com.badlogic.gdx.utils.OrderedSet
import com.badlogic.gdx.utils.OrderedSet.OrderedSetIterator
import java.lang.IllegalStateException
import java.util.NoSuchElementException

/**
 * An [ObjectMap] that also stores keys in an [Array] using the insertion order. Iteration over the
 * [.entries], [.keys], and [.values] is ordered and faster than an unordered map. Keys can also be
 * accessed and the order changed using [.orderedKeys]. There is some additional overhead for put and remove. When used
 * for faster iteration versus ObjectMap and the order does not actually matter, copying during remove can be greatly reduced by
 * setting [Array.ordered] to false for [OrderedMap.orderedKeys].
 *
 * @author Nathan Sweet
 */
class OrderedMap<K, V> : ObjectMap<K, V> {

    val keys: Array<K>

    constructor() {
        keys = Array()
    }

    constructor(initialCapacity: Int) : super(initialCapacity) {
        keys = Array(capacity)
    }

    constructor(initialCapacity: Int, loadFactor: Float) : super(initialCapacity, loadFactor) {
        keys = Array(capacity)
    }

    constructor(map: OrderedMap<out K, out V>) : super(map) {
        keys = Array(map.keys)
    }

    fun put(key: K, value: V): V {
        if (!containsKey(key)) keys.add(key)
        return super.put(key, value)
    }

    override fun remove(key: K): V {
        keys.removeValue(key, false)
        return super.remove(key)
    }

    fun removeIndex(index: Int): V {
        return super.remove(keys.removeIndex(index))
    }

    override fun clear(maximumCapacity: Int) {
        keys.clear()
        super.clear(maximumCapacity)
    }

    override fun clear() {
        keys.clear()
        super.clear()
    }

    fun orderedKeys(): Array<K> {
        return keys
    }

    override operator fun iterator(): Entries<K, V> {
        return entries()
    }

    /**
     * Returns an iterator for the entries in the map. Remove is supported.
     *
     *
     * If [Collections.allocateIterators] is false, the same iterator instance is returned each time this method is called. Use the
     * [OrderedMapEntries] constructor for nested or multithreaded iteration.
     */
    override fun entries(): Entries<K, V> {
        if (Collections.allocateIterators) return Entries(this)
        if (entries1 == null) {
            entries1 = OrderedMapEntries<Any?, Any?>(this)
            entries2 = OrderedMapEntries<Any?, Any?>(this)
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
     * If [Collections.allocateIterators] is false, the same iterator instance is returned each time this method is called. Use the
     * [OrderedMapValues] constructor for nested or multithreaded iteration.
     */
    override fun values(): Values<V> {
        if (Collections.allocateIterators) return Values(this)
        if (values1 == null) {
            values1 = OrderedMapValues<Any?>(this)
            values2 = OrderedMapValues<Any?>(this)
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
     * If [Collections.allocateIterators] is false, the same iterator instance is returned each time this method is called. Use the
     * [OrderedMapKeys] constructor for nested or multithreaded iteration.
     */
    override fun keys(): Keys<K> {
        if (Collections.allocateIterators) return Keys(this)
        if (keys1 == null) {
            keys1 = OrderedMapKeys<Any?>(this)
            keys2 = OrderedMapKeys<Any?>(this)
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

    override fun toString(): String {
        if (size === 0) return "{}"
        val buffer: java.lang.StringBuilder = java.lang.StringBuilder(32)
        buffer.append('{')
        val keys = keys
        var i = 0
        val n = keys.size
        while (i < n) {
            val key = keys[i]
            if (i > 0) buffer.append(", ")
            buffer.append(key)
            buffer.append('=')
            buffer.append(get(key))
            i++
        }
        buffer.append('}')
        return buffer.toString()
    }

    class OrderedMapEntries<K, V>(map: OrderedMap<K, V>) : Entries<K, V>(map) {
        private val keys: Array<K>
        override fun reset() {
            nextIndex = 0
            hasNext = map.size > 0
        }

        override operator fun next(): Entry {
            if (!hasNext) throw NoSuchElementException()
            if (!valid) throw GdxRuntimeException("#iterator() cannot be used nested.")
            entry.key = keys[nextIndex]
            entry.value = map.get(entry.key)
            nextIndex++
            hasNext = nextIndex < map.size
            return entry
        }

        override fun remove() {
            check(currentIndex >= 0) { "next must be called before remove." }
            map.remove(entry.key)
            nextIndex--
        }

        init {
            keys = map.keys
        }
    }

    class OrderedMapKeys<K>(map: OrderedMap<K, *>) : Keys<K>(map) {
        private val keys: Array<K>
        override fun reset() {
            nextIndex = 0
            hasNext = map.size > 0
        }

        override operator fun next(): K {
            if (!hasNext) throw NoSuchElementException()
            if (!valid) throw GdxRuntimeException("#iterator() cannot be used nested.")
            val key = keys[nextIndex]
            currentIndex = nextIndex
            nextIndex++
            hasNext = nextIndex < map.size
            return key
        }

        override fun remove() {
            check(currentIndex >= 0) { "next must be called before remove." }
            (map as OrderedMap<*, *>).removeIndex(nextIndex - 1)
            nextIndex = currentIndex
            currentIndex = -1
        }

        init {
            keys = map.keys
        }
    }

    class OrderedMapValues<V>(map: OrderedMap<*, V>) : Values<V>(map) {
        private val keys: Array
        override fun reset() {
            nextIndex = 0
            hasNext = map.size > 0
        }

        override operator fun next(): V {
            if (!hasNext) throw NoSuchElementException()
            if (!valid) throw GdxRuntimeException("#iterator() cannot be used nested.")
            val value = map.get(keys.get(nextIndex)) as V
            currentIndex = nextIndex
            nextIndex++
            hasNext = nextIndex < map.size
            return value
        }

        override fun remove() {
            check(currentIndex >= 0) { "next must be called before remove." }
            (map as OrderedMap<*, *>).removeIndex(currentIndex)
            nextIndex = currentIndex
            currentIndex = -1
        }

        init {
            keys = map.keys
        }
    }
}
