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
package com.badlogic.gdx.graphics.g3d

class Attributes : Iterable<Attribute?>, Comparator<Attribute?>, Comparable<Attributes?> {
    /**
     * @return Bitwise mask of the ID's of all the containing attributes
     */
    var mask: Long = 0
        protected set
    protected val attributes: Array<Attribute> = Array<Attribute>()
    protected var sorted = true

    /**
     * Sort the attributes by their ID
     */
    fun sort() {
        if (!sorted) {
            attributes.sort(this)
            sorted = true
        }
    }

    /**
     * Example usage: ((BlendingAttribute)material.get(BlendingAttribute.ID)).sourceFunction;
     *
     * @return The attribute (which can safely be cast) if any, otherwise null
     */
    operator fun get(type: Long): Attribute? {
        if (has(type)) for (i in 0 until attributes.size) if (attributes[i].type === type) return attributes[i]
        return null
    }

    /**
     * Example usage: ((BlendingAttribute)material.get(BlendingAttribute.ID)).sourceFunction;
     *
     * @return The attribute if any, otherwise null
     */
    operator fun <T : Attribute?> get(clazz: java.lang.Class<T>?, type: Long): T? {
        return get(type)
    }

    /**
     * Get multiple attributes at once. Example: material.get(out, ColorAttribute.Diffuse | ColorAttribute.Specular |
     * TextureAttribute.Diffuse);
     */
    operator fun get(out: Array<Attribute>, type: Long): Array<Attribute> {
        for (i in 0 until attributes.size) if (attributes[i].type and type !== 0) out.add(attributes[i])
        return out
    }

    /**
     * Removes all attributes
     */
    fun clear() {
        mask = 0
        attributes.clear()
    }

    /**
     * @return The amount of attributes this material contains.
     */
    fun size(): Int {
        return attributes.size
    }

    private fun enable(mask: Long) {
        this.mask = this.mask or mask
    }

    private fun disable(mask: Long) {
        this.mask = this.mask and mask.inv()
    }

    /**
     * Add a attribute to this material. If the material already contains an attribute of the same type it is overwritten.
     */
    fun set(attribute: Attribute) {
        val idx = indexOf(attribute.type)
        if (idx < 0) {
            enable(attribute.type)
            attributes.add(attribute)
            sorted = false
        } else {
            attributes[idx] = attribute
        }
        sort() //FIXME: See #4186
    }

    /**
     * Add multiple attributes to this material. If the material already contains an attribute of the same type it is overwritten.
     */
    operator fun set(attribute1: Attribute?, attribute2: Attribute?) {
        set(attribute1)
        set(attribute2)
    }

    /**
     * Add multiple attributes to this material. If the material already contains an attribute of the same type it is overwritten.
     */
    operator fun set(attribute1: Attribute?, attribute2: Attribute?, attribute3: Attribute?) {
        set(attribute1)
        set(attribute2)
        set(attribute3)
    }

    /**
     * Add multiple attributes to this material. If the material already contains an attribute of the same type it is overwritten.
     */
    operator fun set(attribute1: Attribute?, attribute2: Attribute?, attribute3: Attribute?,
                     attribute4: Attribute?) {
        set(attribute1)
        set(attribute2)
        set(attribute3)
        set(attribute4)
    }

    /**
     * Add an array of attributes to this material. If the material already contains an attribute of the same type it is
     * overwritten.
     */
    fun set(vararg attributes: Attribute?) {
        for (attr in attributes) set(attr)
    }

    /**
     * Add an array of attributes to this material. If the material already contains an attribute of the same type it is
     * overwritten.
     */
    fun set(attributes: Iterable<Attribute?>) {
        for (attr in attributes) set(attr)
    }

    /**
     * Removes the attribute from the material, i.e.: material.remove(BlendingAttribute.ID); Can also be used to remove multiple
     * attributes also, i.e. remove(AttributeA.ID | AttributeB.ID);
     */
    fun remove(mask: Long) {
        for (i in attributes.size - 1 downTo 0) {
            val type: Long = attributes[i].type
            if (mask and type == type) {
                attributes.removeIndex(i)
                disable(type)
                sorted = false
            }
        }
        sort() //FIXME: See #4186
    }

    /**
     * @return True if this collection has the specified attribute, i.e. attributes.has(ColorAttribute.Diffuse); Or when multiple
     * attribute types are specified, true if this collection has all specified attributes, i.e. attributes.has(out,
     * ColorAttribute.Diffuse | ColorAttribute.Specular | TextureAttribute.Diffuse);
     */
    fun has(type: Long): Boolean {
        return type != 0L && mask and type == type
    }

    /**
     * @return the index of the attribute with the specified type or negative if not available.
     */
    protected fun indexOf(type: Long): Int {
        if (has(type)) for (i in 0 until attributes.size) if (attributes[i].type === type) return i
        return -1
    }
    /**
     * Check if this collection has the same attributes as the other collection. If compareValues is true, it also compares the
     * values of each attribute.
     *
     * @param compareValues True to compare attribute values, false to only compare attribute types
     * @return True if this collection contains the same attributes (and optionally attribute values) as the other.
     */
    /**
     * See [.same]
     *
     * @return True if this collection contains the same attributes (but not values) as the other.
     */
    @JvmOverloads
    fun same(other: Attributes?, compareValues: Boolean = false): Boolean {
        if (other === this) return true
        if (other == null || mask != other.mask) return false
        if (!compareValues) return true
        sort()
        other.sort()
        for (i in 0 until attributes.size) if (!attributes[i].equals(other.attributes[i])) return false
        return true
    }

    /**
     * Used for sorting attributes by type (not by value)
     */
    override fun compare(arg0: Attribute, arg1: Attribute): Int {
        return (arg0.type - arg1.type)
    }

    /**
     * Used for iterating through the attributes
     */
    override fun iterator(): Iterator<Attribute> {
        return attributes.iterator()
    }

    /**
     * @return A hash code based on only the attribute values, which might be different compared to [.hashCode] because the latter
     * might include other properties as well, i.e. the material id.
     */
    fun attributesHash(): Int {
        sort()
        val n = attributes.size
        var result = 71 + mask
        var m = 1
        for (i in 0 until n) result += mask * attributes[i].hashCode() * (m * 7 and 0xFFFF.also { m = it })
        return (result xor (result shr 32)).toInt()
    }

    override fun hashCode(): Int {
        return attributesHash()
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Attributes) return false
        return if (other === this) true else same(other as Attributes?, true)
    }

    override operator fun compareTo(other: Attributes): Int {
        if (other === this) return 0
        if (mask != other.mask) return if (mask < other.mask) -1 else 1
        sort()
        other.sort()
        for (i in 0 until attributes.size) {
            val c: Int = attributes[i].compareTo(other.attributes[i])
            if (c != 0) return if (c < 0) -1 else if (c > 0) 1 else 0
        }
        return 0
    }
}
