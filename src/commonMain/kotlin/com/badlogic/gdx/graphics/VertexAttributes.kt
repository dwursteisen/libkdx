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
package com.badlogic.gdx.graphics

import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.Collections
import com.badlogic.gdx.utils.GdxRuntimeException
import java.util.NoSuchElementException

/**
 * Instances of this class specify the vertex attributes of a mesh. VertexAttributes are used by [Mesh] instances to define
 * its vertex structure. Vertex attributes have an order. The order is specified by the order they are added to this class.
 *
 * @author mzechner, Xoppa
 */
class VertexAttributes(vararg attributes: VertexAttribute?) : Iterable<VertexAttribute?>, Comparable<VertexAttributes?> {

    /**
     * The usage of a vertex attribute.
     *
     * @author mzechner
     */
    object Usage {

        const val Position = 1
        const val ColorUnpacked = 2
        const val ColorPacked = 4
        const val Normal = 8
        const val TextureCoordinates = 16
        const val Generic = 32
        const val BoneWeight = 64
        const val Tangent = 128
        const val BiNormal = 256
    }

    /**
     * the attributes in the order they were specified
     */
    private val attributes: Array<VertexAttribute?>

    /**
     * the size of a single vertex in bytes
     */
    val vertexSize: Int

    /**
     * cache of the value calculated by [.getMask]
     */
    private var mask: Long = -1
        get() {
            if (field == -1L) {
                var result: Long = 0
                for (i in attributes.indices) {
                    result = result or attributes[i].usage
                }
                field = result
            }
            return field
        }
    private var iterable: ReadonlyIterable<VertexAttribute>? = null

    /**
     * Returns the offset for the first VertexAttribute with the specified usage.
     *
     * @param usage The usage of the VertexAttribute.
     */
    fun getOffset(usage: Int, defaultIfNotFound: Int): Int {
        val vertexAttribute: VertexAttribute = findByUsage(usage) ?: return defaultIfNotFound
        return vertexAttribute.offset / 4
    }

    /**
     * Returns the offset for the first VertexAttribute with the specified usage.
     *
     * @param usage The usage of the VertexAttribute.
     */
    fun getOffset(usage: Int): Int {
        return getOffset(usage, 0)
    }

    /**
     * Returns the first VertexAttribute for the given usage.
     *
     * @param usage The usage of the VertexAttribute to find.
     */
    fun findByUsage(usage: Int): VertexAttribute? {
        val len = size()
        for (i in 0 until len) if (get(i).usage === usage) return get(i)
        return null
    }

    private fun calculateOffsets(): Int {
        var count = 0
        for (i in attributes.indices) {
            val attribute: VertexAttribute? = attributes[i]
            attribute.offset = count
            count += attribute.getSizeInBytes()
        }
        return count
    }

    /**
     * @return the number of attributes
     */
    fun size(): Int {
        return attributes.size
    }

    /**
     * @param index the index
     * @return the VertexAttribute at the given index
     */
    operator fun get(index: Int): VertexAttribute? {
        return attributes[index]
    }

    override fun toString(): String {
        val builder: java.lang.StringBuilder = java.lang.StringBuilder()
        builder.append("[")
        for (i in attributes.indices) {
            builder.append("(")
            builder.append(attributes[i].alias)
            builder.append(", ")
            builder.append(attributes[i].usage)
            builder.append(", ")
            builder.append(attributes[i].numComponents)
            builder.append(", ")
            builder.append(attributes[i].offset)
            builder.append(")")
            builder.append("\n")
        }
        builder.append("]")
        return builder.toString()
    }

    override fun equals(obj: Any?): Boolean {
        if (obj === this) return true
        if (obj !is VertexAttributes) return false
        val other = obj
        if (attributes.size != other.attributes.size) return false
        for (i in attributes.indices) {
            if (!attributes[i].equals(other.attributes[i])) return false
        }
        return true
    }

    override fun hashCode(): Int {
        var result = 61 * attributes.size.toLong()
        for (i in attributes.indices) result = result * 61 + attributes[i].hashCode()
        return (result xor (result shr 32)).toInt()
    }

    /**
     * Calculates a mask based on the contained [VertexAttribute] instances. The mask is a bit-wise or of each attributes
     * [VertexAttribute.usage].
     *
     * @return the mask
     */
    fun getMask(): Long {
        if (field == -1L) {
            var result: Long = 0
            for (i in attributes.indices) {
                result = result or attributes[i].usage
            }
            field = result
        }
        return field
    }

    /**
     * Calculates the mask based on [VertexAttributes.getMask] and packs the attributes count into the last 32 bits.
     *
     * @return the mask with attributes count packed into the last 32 bits.
     */
    fun getMaskWithSizePacked(): Long {
        return getMask() or (attributes.size.toLong() shl 32)
    }

    override operator fun compareTo(o: VertexAttributes): Int {
        if (attributes.size != o.attributes.size) return attributes.size - o.attributes.size
        val m1 = getMask()
        val m2 = o.getMask()
        if (m1 != m2) return if (m1 < m2) -1 else 1
        for (i in attributes.indices.reversed()) {
            val va0: VertexAttribute? = attributes[i]
            val va1: VertexAttribute? = o.attributes[i]
            if (va0.usage !== va1.usage) return va0.usage - va1.usage
            if (va0.unit !== va1.unit) return va0.unit - va1.unit
            if (va0.numComponents !== va1.numComponents) return va0.numComponents - va1.numComponents
            if (va0.normalized !== va1.normalized) return if (va0.normalized) 1 else -1
            if (va0.type !== va1.type) return va0.type - va1.type
        }
        return 0
    }

    /**
     * @see Collections.allocateIterators
     */
    override fun iterator(): MutableIterator<VertexAttribute> {
        if (iterable == null) iterable = ReadonlyIterable<VertexAttribute>(attributes)
        return iterable!!.iterator()
    }

    private class ReadonlyIterator<T>(private val array: Array<T>) : MutableIterator<T>, Iterable<T> {
        var index = 0
        var valid = true
        override fun hasNext(): Boolean {
            if (!valid) throw GdxRuntimeException("#iterator() cannot be used nested.")
            return index < array.size
        }

        override fun next(): T {
            if (index >= array.size) throw NoSuchElementException(index.toString())
            if (!valid) throw GdxRuntimeException("#iterator() cannot be used nested.")
            return array[index++]
        }

        override fun remove() {
            throw GdxRuntimeException("Remove not allowed.")
        }

        fun reset() {
            index = 0
        }

        override fun iterator(): Iterator<T> {
            return this
        }
    }

    private class ReadonlyIterable<T>(array: Array<T>) : Iterable<T> {
        private val array: Array<T?>
        private var iterator1: ReadonlyIterator<*>? = null
        private var iterator2: ReadonlyIterator<*>? = null
        override fun iterator(): MutableIterator<T> {
            if (Collections.allocateIterators) return ReadonlyIterator<Any?>(array)
            if (iterator1 == null) {
                iterator1 = ReadonlyIterator<Any?>(array)
                iterator2 = ReadonlyIterator<Any?>(array)
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

        init {
            this.array = array
        }
    }

    /**
     * Constructor, sets the vertex attributes in a specific order
     */
    init {
        if (attributes.size == 0) throw java.lang.IllegalArgumentException("attributes must be >= 1")
        val list: Array<VertexAttribute?> = arrayOfNulls<VertexAttribute>(attributes.size)
        for (i in 0 until attributes.size) list[i] = attributes[i]
        this.attributes = list
        vertexSize = calculateOffsets()
    }
}
