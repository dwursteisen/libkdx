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
import com.badlogic.gdx.utils.Array.ArrayIterable
import com.badlogic.gdx.utils.Base64Coder
import com.badlogic.gdx.utils.Base64Coder.CharMap
import java.lang.IllegalStateException
import java.lang.IndexOutOfBoundsException

/**
 * An ordered or unordered map of objects. This implementation uses arrays to store the keys and values, which means
 * [gets][.getKey] do a comparison for each key in the map. This is slower than a typical hash map
 * implementation, but may be acceptable for small maps and has the benefits that keys and values can be accessed by index, which
 * makes iteration fast. Like [Array], if ordered is false, this class avoids a memory copy when removing elements (the last
 * element is moved to the removed element's position).
 *
 * @author Nathan Sweet
 */
class ArrayMap<K, V> : Iterable<ObjectMap.Entry<K, V>?> {

    var keys: Array<K?>
    var values: Array<V?>
    var size = 0
    var ordered: Boolean
    private var entries1: Entries<*, *>? = null
    private var entries2: Entries<*, *>? = null
    private var values1: Values<*>? = null
    private var values2: Values<*>? = null
    private var keys1: Keys<*>? = null
    private var keys2: Keys<*>? = null

    /**
     * Creates an ordered map with the specified capacity.
     */
    constructor(capacity: Int) : this(true, capacity) {}
    /**
     * @param ordered  If false, methods that remove elements may change the order of other elements in the arrays, which avoids a
     * memory copy.
     * @param capacity Any elements added beyond this will cause the backing arrays to be grown.
     */
    /**
     * Creates an ordered map with a capacity of 16.
     */
    @JvmOverloads
    constructor(ordered: Boolean = true, capacity: Int = 16) {
        this.ordered = ordered
        keys = arrayOfNulls<Any>(capacity) as Array<K?>
        values = arrayOfNulls<Any>(capacity) as Array<V?>
    }

    /**
     * Creates a new map with [.keys] and [.values] of the specified type.
     *
     * @param ordered  If false, methods that remove elements may change the order of other elements in the arrays, which avoids a
     * memory copy.
     * @param capacity Any elements added beyond this will cause the backing arrays to be grown.
     */
    constructor(ordered: Boolean, capacity: Int, keyArrayType: java.lang.Class?, valueArrayType: java.lang.Class?) {
        this.ordered = ordered
        keys = ArrayReflection.newInstance(keyArrayType, capacity)
        values = ArrayReflection.newInstance(valueArrayType, capacity)
    }

    /**
     * Creates an ordered map with [.keys] and [.values] of the specified type and a capacity of 16.
     */
    constructor(keyArrayType: java.lang.Class?, valueArrayType: java.lang.Class?) : this(false, 16, keyArrayType, valueArrayType) {}

    /**
     * Creates a new map containing the elements in the specified map. The new map will have the same type of backing arrays and
     * will be ordered if the specified map is ordered. The capacity is set to the number of elements, so any subsequent elements
     * added will cause the backing arrays to be grown.
     */
    constructor(array: ArrayMap<*, *>) : this(array.ordered, array.size, array.keys.javaClass.getComponentType(), array.values.javaClass.getComponentType()) {
        size = array.size
        java.lang.System.arraycopy(array.keys, 0, keys, 0, size)
        java.lang.System.arraycopy(array.values, 0, values, 0, size)
    }

    fun put(key: K, value: V): Int {
        var index = indexOfKey(key)
        if (index == -1) {
            if (size == keys.size) resize(java.lang.Math.max(8, (size * 1.75f).toInt()))
            index = size++
        }
        keys[index] = key
        values[index] = value
        return index
    }

    fun put(key: K, value: V, index: Int): Int {
        val existingIndex = indexOfKey(key)
        if (existingIndex != -1) removeIndex(existingIndex) else if (size == keys.size) //
            resize(java.lang.Math.max(8, (size * 1.75f).toInt()))
        java.lang.System.arraycopy(keys, index, keys, index + 1, size - index)
        java.lang.System.arraycopy(values, index, values, index + 1, size - index)
        keys[index] = key
        values[index] = value
        size++
        return index
    }

    @JvmOverloads
    fun putAll(map: ArrayMap<out K, out V>, offset: Int = 0, length: Int = map.size) {
        if (offset + length > map.size) throw java.lang.IllegalArgumentException("offset + length must be <= size: " + offset + " + " + length + " <= " + map.size)
        val sizeNeeded = size + length - offset
        if (sizeNeeded >= keys.size) resize(java.lang.Math.max(8, (sizeNeeded * 1.75f).toInt()))
        java.lang.System.arraycopy(map.keys, offset, keys, size, length)
        java.lang.System.arraycopy(map.values, offset, values, size, length)
        size += length
    }
    /**
     * Returns the value (which may be null) for the specified key, or the default value if the key is not in the map. Note this
     * does a .equals() comparison of each key in reverse order until the specified key is found.
     */
    /**
     * Returns the value (which may be null) for the specified key, or null if the key is not in the map. Note this does a
     * .equals() comparison of each key in reverse order until the specified key is found.
     */
    @JvmOverloads
    operator fun get(key: K?, defaultValue: V? = null): V? {
        val keys: Array<Any> = keys
        var i = size - 1
        if (key == null) {
            while (i >= 0) {
                if (keys[i] === key) return values[i]
                i--
            }
        } else {
            while (i >= 0) {
                if (key == keys[i]) return values[i]
                i--
            }
        }
        return defaultValue
    }

    /**
     * Returns the key for the specified value. Note this does a comparison of each value in reverse order until the specified
     * value is found.
     *
     * @param identity If true, == comparison will be used. If false, .equals() comparison will be used.
     */
    fun getKey(value: V?, identity: Boolean): K? {
        val values: Array<Any> = values
        var i = size - 1
        if (identity || value == null) {
            while (i >= 0) {
                if (values[i] === value) return keys[i]
                i--
            }
        } else {
            while (i >= 0) {
                if (value == values[i]) return keys[i]
                i--
            }
        }
        return null
    }

    fun getKeyAt(index: Int): K? {
        if (index >= size) throw IndexOutOfBoundsException(index.toString())
        return keys[index]
    }

    fun getValueAt(index: Int): V? {
        if (index >= size) throw IndexOutOfBoundsException(index.toString())
        return values[index]
    }

    fun firstKey(): K? {
        check(size != 0) { "Map is empty." }
        return keys[0]
    }

    fun firstValue(): V? {
        check(size != 0) { "Map is empty." }
        return values[0]
    }

    fun setKey(index: Int, key: K) {
        if (index >= size) throw IndexOutOfBoundsException(index.toString())
        keys[index] = key
    }

    fun setValue(index: Int, value: V) {
        if (index >= size) throw IndexOutOfBoundsException(index.toString())
        values[index] = value
    }

    fun insert(index: Int, key: K, value: V) {
        if (index > size) throw IndexOutOfBoundsException(index.toString())
        if (size == keys.size) resize(java.lang.Math.max(8, (size * 1.75f).toInt()))
        if (ordered) {
            java.lang.System.arraycopy(keys, index, keys, index + 1, size - index)
            java.lang.System.arraycopy(values, index, values, index + 1, size - index)
        } else {
            keys[size] = keys[index]
            values[size] = values[index]
        }
        size++
        keys[index] = key
        values[index] = value
    }

    fun containsKey(key: K?): Boolean {
        val keys: Array<K> = keys
        var i = size - 1
        if (key == null) {
            while (i >= 0) if (keys[i--] === key) return true
        } else {
            while (i >= 0) if (key == keys[i--]) return true
        }
        return false
    }

    /**
     * @param identity If true, == comparison will be used. If false, .equals() comparison will be used.
     */
    fun containsValue(value: V?, identity: Boolean): Boolean {
        val values: Array<V> = values
        var i = size - 1
        if (identity || value == null) {
            while (i >= 0) if (values[i--] === value) return true
        } else {
            while (i >= 0) if (value == values[i--]) return true
        }
        return false
    }

    fun indexOfKey(key: K?): Int {
        val keys: Array<Any> = keys
        if (key == null) {
            var i = 0
            val n = size
            while (i < n) {
                if (keys[i] === key) return i
                i++
            }
        } else {
            var i = 0
            val n = size
            while (i < n) {
                if (key == keys[i]) return i
                i++
            }
        }
        return -1
    }

    fun indexOfValue(value: V?, identity: Boolean): Int {
        val values: Array<Any> = values
        if (identity || value == null) {
            var i = 0
            val n = size
            while (i < n) {
                if (values[i] === value) return i
                i++
            }
        } else {
            var i = 0
            val n = size
            while (i < n) {
                if (value == values[i]) return i
                i++
            }
        }
        return -1
    }

    fun removeKey(key: K?): V? {
        val keys: Array<Any> = keys
        if (key == null) {
            var i = 0
            val n = size
            while (i < n) {
                if (keys[i] === key) {
                    val value = values[i]
                    removeIndex(i)
                    return value
                }
                i++
            }
        } else {
            var i = 0
            val n = size
            while (i < n) {
                if (key == keys[i]) {
                    val value = values[i]
                    removeIndex(i)
                    return value
                }
                i++
            }
        }
        return null
    }

    fun removeValue(value: V?, identity: Boolean): Boolean {
        val values: Array<Any> = values
        if (identity || value == null) {
            var i = 0
            val n = size
            while (i < n) {
                if (values[i] === value) {
                    removeIndex(i)
                    return true
                }
                i++
            }
        } else {
            var i = 0
            val n = size
            while (i < n) {
                if (value == values[i]) {
                    removeIndex(i)
                    return true
                }
                i++
            }
        }
        return false
    }

    /**
     * Removes and returns the key/values pair at the specified index.
     */
    fun removeIndex(index: Int) {
        if (index >= size) throw IndexOutOfBoundsException(index.toString())
        val keys: Array<Any?> = keys
        size--
        if (ordered) {
            java.lang.System.arraycopy(keys, index + 1, keys, index, size - index)
            java.lang.System.arraycopy(values, index + 1, values, index, size - index)
        } else {
            keys[index] = keys[size]
            values[index] = values[size]
        }
        keys[size] = null
        values[size] = null
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
     * Returns the last key.
     */
    fun peekKey(): K? {
        return keys[size - 1]
    }

    /**
     * Returns the last value.
     */
    fun peekValue(): V? {
        return values[size - 1]
    }

    /**
     * Clears the map and reduces the size of the backing arrays to be the specified capacity if they are larger.
     */
    fun clear(maximumCapacity: Int) {
        if (keys.size <= maximumCapacity) {
            clear()
            return
        }
        size = 0
        resize(maximumCapacity)
    }

    fun clear() {
        val keys = keys
        val values = values
        var i = 0
        val n = size
        while (i < n) {
            keys[i] = null
            values[i] = null
            i++
        }
        size = 0
    }

    /**
     * Reduces the size of the backing arrays to the size of the actual number of entries. This is useful to release memory when
     * many items have been removed, or if it is known that more entries will not be added.
     */
    fun shrink() {
        if (keys.size == size) return
        resize(size)
    }

    /**
     * Increases the size of the backing arrays to accommodate the specified number of additional entries. Useful before adding
     * many entries to avoid multiple backing array resizes.
     */
    fun ensureCapacity(additionalCapacity: Int) {
        if (additionalCapacity < 0) throw java.lang.IllegalArgumentException("additionalCapacity must be >= 0: $additionalCapacity")
        val sizeNeeded = size + additionalCapacity
        if (sizeNeeded >= keys.size) resize(java.lang.Math.max(8, sizeNeeded))
    }

    protected fun resize(newSize: Int) {
        val newKeys = ArrayReflection.newInstance(keys.javaClass.getComponentType(), newSize) as Array<K>
        java.lang.System.arraycopy(keys, 0, newKeys, 0, java.lang.Math.min(size, newKeys.size))
        keys = newKeys
        val newValues = ArrayReflection.newInstance(values.javaClass.getComponentType(), newSize) as Array<V>
        java.lang.System.arraycopy(values, 0, newValues, 0, java.lang.Math.min(size, newValues.size))
        values = newValues
    }

    fun reverse() {
        var i = 0
        val lastIndex = size - 1
        val n = size / 2
        while (i < n) {
            val ii = lastIndex - i
            val tempKey = keys[i]
            keys[i] = keys[ii]
            keys[ii] = tempKey
            val tempValue = values[i]
            values[i] = values[ii]
            values[ii] = tempValue
            i++
        }
    }

    fun shuffle() {
        for (i in size - 1 downTo 0) {
            val ii: Int = MathUtils.random(i)
            val tempKey = keys[i]
            keys[i] = keys[ii]
            keys[ii] = tempKey
            val tempValue = values[i]
            values[i] = values[ii]
            values[ii] = tempValue
        }
    }

    /**
     * Reduces the size of the arrays to the specified size. If the arrays are already smaller than the specified size, no action
     * is taken.
     */
    fun truncate(newSize: Int) {
        if (size <= newSize) return
        for (i in newSize until size) {
            keys[i] = null
            values[i] = null
        }
        size = newSize
    }

    override fun hashCode(): Int {
        val keys: Array<K> = keys
        val values: Array<V> = values
        var h = 0
        var i = 0
        val n = size
        while (i < n) {
            val key: K? = keys[i]
            val value: V? = values[i]
            if (key != null) h += key.hashCode() * 31
            if (value != null) h += value.hashCode()
            i++
        }
        return h
    }

    override fun equals(obj: Any?): Boolean {
        if (obj === this) return true
        if (obj !is ArrayMap<*, *>) return false
        val other = obj
        if (other.size != size) return false
        val keys: Array<K> = keys
        val values: Array<V> = values
        var i = 0
        val n = size
        while (i < n) {
            val key = keys[i]
            val value: V? = values[i]
            if (value == null) {
                if (other.get(key, ObjectMap.dummy) != null) return false
            } else {
                if (value != other.get(key)) return false
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
        if (obj !is ArrayMap<*, *>) return false
        val other = obj
        if (other.size != size) return false
        val keys: Array<K> = keys
        val values: Array<V> = values
        var i = 0
        val n = size
        while (i < n) {
            if (values[i] !== other.get(keys[i], ObjectMap.dummy)) return false
            i++
        }
        return true
    }

    override fun toString(): String {
        if (size == 0) return "{}"
        val keys: Array<K> = keys
        val values: Array<V> = values
        val buffer = StringBuilder(32)
        buffer.append('{')
        buffer.append(keys[0])
        buffer.append('=')
        buffer.append(values[0])
        for (i in 1 until size) {
            buffer.append(", ")
            buffer.append(keys[i])
            buffer.append('=')
            buffer.append(values[i])
        }
        buffer.append('}')
        return buffer.toString()
    }

    override fun iterator(): Iterator<Entry<K, V>> {
        return entries()!!
    }

    /**
     * Returns an iterator for the entries in the map. Remove is supported.
     *
     *
     * If [Collections.allocateIterators] is false, the same iterator instance is returned each time this method is called. Use the
     * [Entries] constructor for nested or multithreaded iteration.
     *
     * @see Collections.allocateIterators
     */
    fun entries(): Entries<K, V?>? {
        if (Collections.allocateIterators) return Entries<Any?, Any?>(this)
        if (entries1 == null) {
            entries1 = Entries<Any?, Any?>(this)
            entries2 = Entries<Any?, Any?>(this)
        }
        if (!entries1.valid) {
            entries1.index = 0
            entries1.valid = true
            entries2!!.valid = false
            return entries1
        }
        entries2!!.index = 0
        entries2!!.valid = true
        entries1.valid = false
        return entries2
    }

    /**
     * Returns an iterator for the values in the map. Remove is supported.
     *
     *
     * If [Collections.allocateIterators] is false, the same iterator instance is returned each time this method is called. Use the
     * [Entries] constructor for nested or multithreaded iteration.
     *
     * @see Collections.allocateIterators
     */
    fun values(): Values<V?>? {
        if (Collections.allocateIterators) return Values<Any?>(this)
        if (values1 == null) {
            values1 = Values<Any?>(this)
            values2 = Values<Any?>(this)
        }
        if (!values1.valid) {
            values1.index = 0
            values1.valid = true
            values2!!.valid = false
            return values1
        }
        values2!!.index = 0
        values2!!.valid = true
        values1.valid = false
        return values2
    }

    /**
     * Returns an iterator for the keys in the map. Remove is supported.
     *
     *
     * If [Collections.allocateIterators] is false, the same iterator instance is returned each time this method is called. Use the
     * [Entries] constructor for nested or multithreaded iteration.
     *
     * @see Collections.allocateIterators
     */
    fun keys(): Keys<K?>? {
        if (Collections.allocateIterators) return Keys<Any?>(this)
        if (keys1 == null) {
            keys1 = Keys<Any?>(this)
            keys2 = Keys<Any?>(this)
        }
        if (!keys1.valid) {
            keys1.index = 0
            keys1.valid = true
            keys2!!.valid = false
            return keys1
        }
        keys2!!.index = 0
        keys2!!.valid = true
        keys1.valid = false
        return keys2
    }

    class Entries<K, V>(private val map: ArrayMap<K, V>) : Iterable<Entry<K, V>?>, Iterator<Entry<K, V>?> {
        var entry: Entry<K, V> = Entry()
        var index = 0
        var valid = true
        override fun hasNext(): Boolean {
            if (!valid) throw GdxRuntimeException("#iterator() cannot be used nested.")
            return index < map.size
        }

        override fun iterator(): Iterator<Entry<K, V>> {
            return this
        }

        /**
         * Note the same entry instance is returned each time this method is called.
         */
        override fun next(): Entry<K, V> {
            if (index >= map.size) throw NoSuchElementException(index.toString())
            if (!valid) throw GdxRuntimeException("#iterator() cannot be used nested.")
            entry.key = map.keys[index]
            entry.value = map.values[index++]
            return entry
        }

        fun remove() {
            index--
            map.removeIndex(index)
        }

        fun reset() {
            index = 0
        }
    }

    class Values<V>(private val map: ArrayMap<Any, V>) : Iterable<V>, Iterator<V> {
        var index = 0
        var valid = true
        override fun hasNext(): Boolean {
            if (!valid) throw GdxRuntimeException("#iterator() cannot be used nested.")
            return index < map.size
        }

        override fun iterator(): Iterator<V> {
            return this
        }

        override fun next(): V {
            if (index >= map.size) throw NoSuchElementException(index.toString())
            if (!valid) throw GdxRuntimeException("#iterator() cannot be used nested.")
            return map.values[index++]
        }

        fun remove() {
            index--
            map.removeIndex(index)
        }

        fun reset() {
            index = 0
        }

        fun toArray(): Array<V> {
            return Array(true, map.values, index, map.size - index)
        }

        fun toArray(array: Array): Array<V> {
            array.addAll(map.values, index, map.size - index)
            return array
        }
    }

    class Keys<K>(private val map: ArrayMap<K, Any>) : Iterable<K>, Iterator<K> {
        var index = 0
        var valid = true
        override fun hasNext(): Boolean {
            if (!valid) throw GdxRuntimeException("#iterator() cannot be used nested.")
            return index < map.size
        }

        override fun iterator(): Iterator<K> {
            return this
        }

        override fun next(): K {
            if (index >= map.size) throw NoSuchElementException(index.toString())
            if (!valid) throw GdxRuntimeException("#iterator() cannot be used nested.")
            return map.keys[index++]
        }

        fun remove() {
            index--
            map.removeIndex(index)
        }

        fun reset() {
            index = 0
        }

        fun toArray(): Array<K> {
            return Array(true, map.keys, index, map.size - index)
        }

        fun toArray(array: Array): Array<K> {
            array.addAll(map.keys, index, map.size - index)
            return array
        }
    }
}
