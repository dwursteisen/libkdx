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
package com.badlogic.gdx.math.collision

import com.badlogic.gdx.math.collision.Ray

/**
 * Encapsulates an axis aligned bounding box represented by a minimum and a maximum Vector. Additionally you can query for the
 * bounding box's center, dimensions and corner points.
 *
 * @author badlogicgames@gmail.com, Xoppa
 */
class BoundingBox : Serializable {

    val min: Vector3 = Vector3()
    val max: Vector3 = Vector3()
    private val cnt: Vector3 = Vector3()
    private val dim: Vector3 = Vector3()

    /**
     * @param out The [Vector3] to receive the center of the bounding box.
     * @return The vector specified with the out argument.
     */
    fun getCenter(out: Vector3): Vector3 {
        return out.set(cnt)
    }

    val centerX: Float
        get() = cnt.x

    val centerY: Float
        get() = cnt.y

    val centerZ: Float
        get() = cnt.z

    fun getCorner000(out: Vector3): Vector3 {
        return out.set(min.x, min.y, min.z)
    }

    fun getCorner001(out: Vector3): Vector3 {
        return out.set(min.x, min.y, max.z)
    }

    fun getCorner010(out: Vector3): Vector3 {
        return out.set(min.x, max.y, min.z)
    }

    fun getCorner011(out: Vector3): Vector3 {
        return out.set(min.x, max.y, max.z)
    }

    fun getCorner100(out: Vector3): Vector3 {
        return out.set(max.x, min.y, min.z)
    }

    fun getCorner101(out: Vector3): Vector3 {
        return out.set(max.x, min.y, max.z)
    }

    fun getCorner110(out: Vector3): Vector3 {
        return out.set(max.x, max.y, min.z)
    }

    fun getCorner111(out: Vector3): Vector3 {
        return out.set(max.x, max.y, max.z)
    }

    /**
     * @param out The [Vector3] to receive the dimensions of this bounding box on all three axis.
     * @return The vector specified with the out argument
     */
    fun getDimensions(out: Vector3): Vector3 {
        return out.set(dim)
    }

    val width: Float
        get() = dim.x

    val height: Float
        get() = dim.y

    val depth: Float
        get() = dim.z

    /**
     * @param out The [Vector3] to receive the minimum values.
     * @return The vector specified with the out argument
     */
    fun getMin(out: Vector3): Vector3 {
        return out.set(min)
    }

    /**
     * @param out The [Vector3] to receive the maximum values.
     * @return The vector specified with the out argument
     */
    fun getMax(out: Vector3): Vector3 {
        return out.set(max)
    }

    /**
     * Constructs a new bounding box with the minimum and maximum vector set to zeros.
     */
    constructor() {
        clr()
    }

    /**
     * Constructs a new bounding box from the given bounding box.
     *
     * @param bounds The bounding box to copy
     */
    constructor(bounds: BoundingBox) {
        this.set(bounds)
    }

    /**
     * Constructs the new bounding box using the given minimum and maximum vector.
     *
     * @param minimum The minimum vector
     * @param maximum The maximum vector
     */
    constructor(minimum: Vector3, maximum: Vector3) {
        this[minimum] = maximum
    }

    /**
     * Sets the given bounding box.
     *
     * @param bounds The bounds.
     * @return This bounding box for chaining.
     */
    fun set(bounds: BoundingBox): BoundingBox {
        return this.set(bounds.min, bounds.max)
    }

    /**
     * Sets the given minimum and maximum vector.
     *
     * @param minimum The minimum vector
     * @param maximum The maximum vector
     * @return This bounding box for chaining.
     */
    operator fun set(minimum: Vector3, maximum: Vector3): BoundingBox {
        min.set(if (minimum.x < maximum.x) minimum.x else maximum.x, if (minimum.y < maximum.y) minimum.y else maximum.y,
            if (minimum.z < maximum.z) minimum.z else maximum.z)
        max.set(if (minimum.x > maximum.x) minimum.x else maximum.x, if (minimum.y > maximum.y) minimum.y else maximum.y,
            if (minimum.z > maximum.z) minimum.z else maximum.z)
        cnt.set(min).add(max).scl(0.5f)
        dim.set(max).sub(min)
        return this
    }

    /**
     * Sets the bounding box minimum and maximum vector from the given points.
     *
     * @param points The points.
     * @return This bounding box for chaining.
     */
    fun set(points: Array<Vector3?>): BoundingBox {
        inf()
        for (l_point in points) this.ext(l_point)
        return this
    }

    /**
     * Sets the bounding box minimum and maximum vector from the given points.
     *
     * @param points The points.
     * @return This bounding box for chaining.
     */
    fun set(points: List<Vector3?>): BoundingBox {
        inf()
        for (l_point in points) this.ext(l_point)
        return this
    }

    /**
     * Sets the minimum and maximum vector to positive and negative infinity.
     *
     * @return This bounding box for chaining.
     */
    fun inf(): BoundingBox {
        min.set(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        max.set(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY)
        cnt.set(0, 0, 0)
        dim.set(0, 0, 0)
        return this
    }

    /**
     * Extends the bounding box to incorporate the given [Vector3].
     *
     * @param point The vector
     * @return This bounding box for chaining.
     */
    fun ext(point: Vector3): BoundingBox {
        return this.set(min.set(Companion.min(min.x, point.x), Companion.min(min.y, point.y), Companion.min(min.z, point.z)),
            max.set(java.lang.Math.max(max.x, point.x), java.lang.Math.max(max.y, point.y), java.lang.Math.max(max.z, point.z)))
    }

    /**
     * Sets the minimum and maximum vector to zeros.
     *
     * @return This bounding box for chaining.
     */
    fun clr(): BoundingBox {
        return this.set(min.set(0, 0, 0), max.set(0, 0, 0))
    }

    /**
     * Returns whether this bounding box is valid. This means that [.max] is greater than or equal to [.min].
     *
     * @return True in case the bounding box is valid, false otherwise
     */
    val isValid: Boolean
        get() = min.x <= max.x && min.y <= max.y && min.z <= max.z

    /**
     * Extends this bounding box by the given bounding box.
     *
     * @param a_bounds The bounding box
     * @return This bounding box for chaining.
     */
    fun ext(a_bounds: BoundingBox): BoundingBox {
        return this.set(min.set(Companion.min(min.x, a_bounds.min.x), Companion.min(min.y, a_bounds.min.y), Companion.min(min.z, a_bounds.min.z)),
            max.set(Companion.max(max.x, a_bounds.max.x), Companion.max(max.y, a_bounds.max.y), Companion.max(max.z, a_bounds.max.z)))
    }

    /**
     * Extends this bounding box by the given sphere.
     *
     * @param center Sphere center
     * @param radius Sphere radius
     * @return This bounding box for chaining.
     */
    fun ext(center: Vector3, radius: Float): BoundingBox {
        return this.set(min.set(Companion.min(min.x, center.x - radius), Companion.min(min.y, center.y - radius), Companion.min(min.z, center.z - radius)),
            max.set(Companion.max(max.x, center.x + radius), Companion.max(max.y, center.y + radius), Companion.max(max.z, center.z + radius)))
    }

    /**
     * Extends this bounding box by the given transformed bounding box.
     *
     * @param bounds    The bounding box
     * @param transform The transformation matrix to apply to bounds, before using it to extend this bounding box.
     * @return This bounding box for chaining.
     */
    fun ext(bounds: BoundingBox, transform: Matrix4?): BoundingBox {
        ext(tmpVector.set(bounds.min.x, bounds.min.y, bounds.min.z).mul(transform))
        ext(tmpVector.set(bounds.min.x, bounds.min.y, bounds.max.z).mul(transform))
        ext(tmpVector.set(bounds.min.x, bounds.max.y, bounds.min.z).mul(transform))
        ext(tmpVector.set(bounds.min.x, bounds.max.y, bounds.max.z).mul(transform))
        ext(tmpVector.set(bounds.max.x, bounds.min.y, bounds.min.z).mul(transform))
        ext(tmpVector.set(bounds.max.x, bounds.min.y, bounds.max.z).mul(transform))
        ext(tmpVector.set(bounds.max.x, bounds.max.y, bounds.min.z).mul(transform))
        ext(tmpVector.set(bounds.max.x, bounds.max.y, bounds.max.z).mul(transform))
        return this
    }

    /**
     * Multiplies the bounding box by the given matrix. This is achieved by multiplying the 8 corner points and then calculating
     * the minimum and maximum vectors from the transformed points.
     *
     * @param transform The matrix
     * @return This bounding box for chaining.
     */
    fun mul(transform: Matrix4?): BoundingBox {
        val x0: Float = min.x
        val y0: Float = min.y
        val z0: Float = min.z
        val x1: Float = max.x
        val y1: Float = max.y
        val z1: Float = max.z
        inf()
        ext(tmpVector.set(x0, y0, z0).mul(transform))
        ext(tmpVector.set(x0, y0, z1).mul(transform))
        ext(tmpVector.set(x0, y1, z0).mul(transform))
        ext(tmpVector.set(x0, y1, z1).mul(transform))
        ext(tmpVector.set(x1, y0, z0).mul(transform))
        ext(tmpVector.set(x1, y0, z1).mul(transform))
        ext(tmpVector.set(x1, y1, z0).mul(transform))
        ext(tmpVector.set(x1, y1, z1).mul(transform))
        return this
    }

    /**
     * Returns whether the given bounding box is contained in this bounding box.
     *
     * @param b The bounding box
     * @return Whether the given bounding box is contained
     */
    operator fun contains(b: BoundingBox): Boolean {
        return (!isValid
            || min.x <= b.min.x && min.y <= b.min.y && min.z <= b.min.z && max.x >= b.max.x && max.y >= b.max.y && max.z >= b.max.z)
    }

    /**
     * Returns whether the given bounding box is intersecting this bounding box (at least one point in).
     *
     * @param b The bounding box
     * @return Whether the given bounding box is intersected
     */
    fun intersects(b: BoundingBox): Boolean {
        if (!isValid) return false

        // test using SAT (separating axis theorem)
        val lx: Float = java.lang.Math.abs(cnt.x - b.cnt.x)
        val sumx: Float = dim.x / 2.0f + b.dim.x / 2.0f
        val ly: Float = java.lang.Math.abs(cnt.y - b.cnt.y)
        val sumy: Float = dim.y / 2.0f + b.dim.y / 2.0f
        val lz: Float = java.lang.Math.abs(cnt.z - b.cnt.z)
        val sumz: Float = dim.z / 2.0f + b.dim.z / 2.0f
        return lx <= sumx && ly <= sumy && lz <= sumz
    }

    /**
     * Returns whether the given vector is contained in this bounding box.
     *
     * @param v The vector
     * @return Whether the vector is contained or not.
     */
    operator fun contains(v: Vector3): Boolean {
        return min.x <= v.x && max.x >= v.x && min.y <= v.y && max.y >= v.y && min.z <= v.z && max.z >= v.z
    }

    override fun toString(): String {
        return "[$min|$max]"
    }

    /**
     * Extends the bounding box by the given vector.
     *
     * @param x The x-coordinate
     * @param y The y-coordinate
     * @param z The z-coordinate
     * @return This bounding box for chaining.
     */
    fun ext(x: Float, y: Float, z: Float): BoundingBox {
        return this.set(min.set(Companion.min(min.x, x), Companion.min(min.y, y), Companion.min(min.z, z)), max.set(Companion.max(max.x, x), Companion.max(max.y, y), Companion.max(max.z, z)))
    }

    companion object {
        private const val serialVersionUID = -1286036817192127343L
        private val tmpVector: Vector3 = Vector3()
        fun min(a: Float, b: Float): Float {
            return if (a > b) b else a
        }

        fun max(a: Float, b: Float): Float {
            return if (a > b) a else b
        }
    }
}
