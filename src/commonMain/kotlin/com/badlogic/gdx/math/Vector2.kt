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
package com.badlogic.gdx.math

import com.badlogic.gdx.utils.GdxRuntimeException
import com.badlogic.gdx.utils.NumberUtils
import java.lang.NumberFormatException

/**
 * Encapsulates a 2D vector. Allows chaining methods by returning a reference to itself
 *
 * @author badlogicgames@gmail.com
 */
class Vector2 : java.io.Serializable, Vector<Vector2?> {

    /**
     * the x-component of this vector
     */
    var x = 0f

    /**
     * the y-component of this vector
     */
    var y = 0f

    /**
     * Constructs a new vector at (0,0)
     */
    constructor() {}

    /**
     * Constructs a vector with the given components
     *
     * @param x The x-component
     * @param y The y-component
     */
    constructor(x: Float, y: Float) {
        this.x = x
        this.y = y
    }

    /**
     * Constructs a vector from the given vector
     *
     * @param v The vector
     */
    constructor(v: Vector2) {
        set(v)
    }

    fun cpy(): Vector2 {
        return Vector2(this)
    }

    fun len(): Float {
        return java.lang.Math.sqrt(x * x + y * y.toDouble())
    }

    fun len2(): Float {
        return x * x + y * y
    }

    fun set(v: Vector2): Vector2 {
        x = v.x
        y = v.y
        return this
    }

    /**
     * Sets the components of this vector
     *
     * @param x The x-component
     * @param y The y-component
     * @return This vector for chaining
     */
    operator fun set(x: Float, y: Float): Vector2 {
        this.x = x
        this.y = y
        return this
    }

    fun sub(v: Vector2): Vector2 {
        x -= v.x
        y -= v.y
        return this
    }

    /**
     * Substracts the other vector from this vector.
     *
     * @param x The x-component of the other vector
     * @param y The y-component of the other vector
     * @return This vector for chaining
     */
    fun sub(x: Float, y: Float): Vector2 {
        this.x -= x
        this.y -= y
        return this
    }

    fun nor(): Vector2 {
        val len = len()
        if (len != 0f) {
            x /= len
            y /= len
        }
        return this
    }

    fun add(v: Vector2): Vector2 {
        x += v.x
        y += v.y
        return this
    }

    /**
     * Adds the given components to this vector
     *
     * @param x The x-component
     * @param y The y-component
     * @return This vector for chaining
     */
    fun add(x: Float, y: Float): Vector2 {
        this.x += x
        this.y += y
        return this
    }

    fun dot(v: Vector2): Float {
        return x * v.x + y * v.y
    }

    fun dot(ox: Float, oy: Float): Float {
        return x * ox + y * oy
    }

    fun scl(scalar: Float): Vector2 {
        x *= scalar
        y *= scalar
        return this
    }

    /**
     * Multiplies this vector by a scalar
     *
     * @return This vector for chaining
     */
    fun scl(x: Float, y: Float): Vector2 {
        this.x *= x
        this.y *= y
        return this
    }

    fun scl(v: Vector2): Vector2 {
        x *= v.x
        y *= v.y
        return this
    }

    fun mulAdd(vec: Vector2, scalar: Float): Vector2 {
        x += vec.x * scalar
        y += vec.y * scalar
        return this
    }

    fun mulAdd(vec: Vector2, mulVec: Vector2): Vector2 {
        x += vec.x * mulVec.x
        y += vec.y * mulVec.y
        return this
    }

    fun dst(v: Vector2): Float {
        val x_d = v.x - x
        val y_d = v.y - y
        return java.lang.Math.sqrt(x_d * x_d + y_d * y_d.toDouble())
    }

    /**
     * @param x The x-component of the other vector
     * @param y The y-component of the other vector
     * @return the distance between this and the other vector
     */
    fun dst(x: Float, y: Float): Float {
        val x_d = x - this.x
        val y_d = y - this.y
        return java.lang.Math.sqrt(x_d * x_d + y_d * y_d.toDouble())
    }

    fun dst2(v: Vector2): Float {
        val x_d = v.x - x
        val y_d = v.y - y
        return x_d * x_d + y_d * y_d
    }

    /**
     * @param x The x-component of the other vector
     * @param y The y-component of the other vector
     * @return the squared distance between this and the other vector
     */
    fun dst2(x: Float, y: Float): Float {
        val x_d = x - this.x
        val y_d = y - this.y
        return x_d * x_d + y_d * y_d
    }

    fun limit(limit: Float): Vector2 {
        return limit2(limit * limit)
    }

    fun limit2(limit2: Float): Vector2 {
        val len2 = len2()
        return if (len2 > limit2) {
            scl(java.lang.Math.sqrt(limit2 / len2.toDouble()) as Float)
        } else this
    }

    fun clamp(min: Float, max: Float): Vector2 {
        val len2 = len2()
        if (len2 == 0f) return this
        val max2 = max * max
        if (len2 > max2) return scl(java.lang.Math.sqrt(max2 / len2.toDouble()) as Float)
        val min2 = min * min
        return if (len2 < min2) scl(java.lang.Math.sqrt(min2 / len2.toDouble()) as Float) else this
    }

    fun setLength(len: Float): Vector2 {
        return setLength2(len * len)
    }

    fun setLength2(len2: Float): Vector2 {
        val oldLen2 = len2()
        return if (oldLen2 == 0f || oldLen2 == len2) this else scl(java.lang.Math.sqrt(len2 / oldLen2.toDouble()) as Float)
    }

    /**
     * Converts this `Vector2` to a string in the format `(x,y)`.
     *
     * @return a string representation of this object.
     */
    override fun toString(): String {
        return "($x,$y)"
    }

    /**
     * Sets this `Vector2` to the value represented by the specified string according to the format of [.toString].
     *
     * @param v the string.
     * @return this vector for chaining
     */
    fun fromString(v: String): Vector2 {
        val s = v.indexOf(',', 1)
        if (s != -1 && v[0] == '(' && v[v.length - 1] == ')') {
            try {
                val x = v.substring(1, s).toFloat()
                val y = v.substring(s + 1, v.length - 1).toFloat()
                return this.set(x, y)
            } catch (ex: NumberFormatException) {
                // Throw a GdxRuntimeException
            }
        }
        throw GdxRuntimeException("Malformed Vector2: $v")
    }

    /**
     * Left-multiplies this vector by the given matrix
     *
     * @param mat the matrix
     * @return this vector
     */
    fun mul(mat: Matrix3): Vector2 {
        val x: Float = x * mat.`val`.get(0) + y * mat.`val`.get(3) + mat.`val`.get(6)
        val y: Float = this.x * mat.`val`.get(1) + y * mat.`val`.get(4) + mat.`val`.get(7)
        this.x = x
        this.y = y
        return this
    }

    /**
     * Calculates the 2D cross product between this and the given vector.
     *
     * @param v the other vector
     * @return the cross product
     */
    fun crs(v: Vector2): Float {
        return x * v.y - y * v.x
    }

    /**
     * Calculates the 2D cross product between this and the given vector.
     *
     * @param x the x-coordinate of the other vector
     * @param y the y-coordinate of the other vector
     * @return the cross product
     */
    fun crs(x: Float, y: Float): Float {
        return this.x * y - this.y * x
    }

    /**
     * @return the angle in degrees of this vector (point) relative to the x-axis. Angles are towards the positive y-axis
     * (typically counter-clockwise) and between 0 and 360.
     */
    fun angle(): Float {
        var angle: Float = java.lang.Math.atan2(y.toDouble(), x.toDouble()) as Float * MathUtils.radiansToDegrees
        if (angle < 0) angle += 360f
        return angle
    }

    /**
     * @return the angle in degrees of this vector (point) relative to the given vector. Angles are towards the positive y-axis
     * (typically counter-clockwise.) between -180 and +180
     */
    fun angle(reference: Vector2): Float {
        return java.lang.Math.atan2(crs(reference).toDouble(), dot(reference).toDouble()) as Float * MathUtils.radiansToDegrees
    }

    /**
     * @return the angle in radians of this vector (point) relative to the x-axis. Angles are towards the positive y-axis.
     * (typically counter-clockwise)
     */
    fun angleRad(): Float {
        return java.lang.Math.atan2(y.toDouble(), x.toDouble())
    }

    /**
     * @return the angle in radians of this vector (point) relative to the given vector. Angles are towards the positive y-axis.
     * (typically counter-clockwise.)
     */
    fun angleRad(reference: Vector2): Float {
        return java.lang.Math.atan2(crs(reference).toDouble(), dot(reference).toDouble())
    }

    /**
     * Sets the angle of the vector in degrees relative to the x-axis, towards the positive y-axis (typically counter-clockwise).
     *
     * @param degrees The angle in degrees to set.
     */
    fun setAngle(degrees: Float): Vector2 {
        return setAngleRad(degrees * MathUtils.degreesToRadians)
    }

    /**
     * Sets the angle of the vector in radians relative to the x-axis, towards the positive y-axis (typically counter-clockwise).
     *
     * @param radians The angle in radians to set.
     */
    fun setAngleRad(radians: Float): Vector2 {
        this[len()] = 0f
        rotateRad(radians)
        return this
    }

    /**
     * Rotates the Vector2 by the given angle, counter-clockwise assuming the y-axis points up.
     *
     * @param degrees the angle in degrees
     */
    fun rotate(degrees: Float): Vector2 {
        return rotateRad(degrees * MathUtils.degreesToRadians)
    }

    /**
     * Rotates the Vector2 by the given angle around reference vector, counter-clockwise assuming the y-axis points up.
     *
     * @param degrees   the angle in degrees
     * @param reference center Vector2
     */
    fun rotateAround(reference: Vector2, degrees: Float): Vector2 {
        return this.sub(reference).rotate(degrees).add(reference)
    }

    /**
     * Rotates the Vector2 by the given angle, counter-clockwise assuming the y-axis points up.
     *
     * @param radians the angle in radians
     */
    fun rotateRad(radians: Float): Vector2 {
        val cos = java.lang.Math.cos(radians.toDouble()) as Float
        val sin = java.lang.Math.sin(radians.toDouble()) as Float
        val newX = x * cos - y * sin
        val newY = x * sin + y * cos
        x = newX
        y = newY
        return this
    }

    /**
     * Rotates the Vector2 by the given angle around reference vector, counter-clockwise assuming the y-axis points up.
     *
     * @param radians   the angle in radians
     * @param reference center Vector2
     */
    fun rotateAroundRad(reference: Vector2, radians: Float): Vector2 {
        return this.sub(reference).rotateRad(radians).add(reference)
    }

    /**
     * Rotates the Vector2 by 90 degrees in the specified direction, where >= 0 is counter-clockwise and < 0 is clockwise.
     */
    fun rotate90(dir: Int): Vector2 {
        val x = x
        if (dir >= 0) {
            this.x = -y
            y = x
        } else {
            this.x = y
            y = -x
        }
        return this
    }

    fun lerp(target: Vector2, alpha: Float): Vector2 {
        val invAlpha = 1.0f - alpha
        x = x * invAlpha + target.x * alpha
        y = y * invAlpha + target.y * alpha
        return this
    }

    fun interpolate(target: Vector2, alpha: Float, interpolation: Interpolation): Vector2 {
        return lerp(target, interpolation.apply(alpha))
    }

    fun setToRandomDirection(): Vector2 {
        val theta: Float = MathUtils.random(0f, MathUtils.PI2)
        return this.set(MathUtils.cos(theta), MathUtils.sin(theta))
    }

    override fun hashCode(): Int {
        val prime = 31
        var result = 1
        result = prime * result + NumberUtils.floatToIntBits(x)
        result = prime * result + NumberUtils.floatToIntBits(y)
        return result
    }

    override fun equals(obj: Any?): Boolean {
        if (this === obj) return true
        if (obj == null) return false
        if (javaClass != obj.javaClass) return false
        val other = obj as Vector2
        if (NumberUtils.floatToIntBits(x) !== NumberUtils.floatToIntBits(other.x)) return false
        return if (NumberUtils.floatToIntBits(y) !== NumberUtils.floatToIntBits(other.y)) false else true
    }

    fun epsilonEquals(other: Vector2?, epsilon: Float): Boolean {
        if (other == null) return false
        if (java.lang.Math.abs(other.x - x) > epsilon) return false
        return if (java.lang.Math.abs(other.y - y) > epsilon) false else true
    }
    /**
     * Compares this vector with the other vector, using the supplied epsilon for fuzzy equality testing.
     *
     * @return whether the vectors are the same.
     */
    /**
     * Compares this vector with the other vector using MathUtils.FLOAT_ROUNDING_ERROR for fuzzy equality testing
     *
     * @param x x component of the other vector to compare
     * @param y y component of the other vector to compare
     * @return true if vector are equal, otherwise false
     */
    @JvmOverloads
    fun epsilonEquals(x: Float, y: Float, epsilon: Float = MathUtils.FLOAT_ROUNDING_ERROR): Boolean {
        if (java.lang.Math.abs(x - this.x) > epsilon) return false
        return if (java.lang.Math.abs(y - this.y) > epsilon) false else true
    }

    /**
     * Compares this vector with the other vector using MathUtils.FLOAT_ROUNDING_ERROR for fuzzy equality testing
     *
     * @param other other vector to compare
     * @return true if vector are equal, otherwise false
     */
    fun epsilonEquals(other: Vector2?): Boolean {
        return epsilonEquals(other, MathUtils.FLOAT_ROUNDING_ERROR)
    }

    val isUnit: Boolean
        get() = isUnit(0.000000001f)

    fun isUnit(margin: Float): Boolean {
        return java.lang.Math.abs(len2() - 1f) < margin
    }

    val isZero: Boolean
        get() = x == 0f && y == 0f

    fun isZero(margin: Float): Boolean {
        return len2() < margin
    }

    fun isOnLine(other: Vector2): Boolean {
        return MathUtils.isZero(x * other.y - y * other.x)
    }

    fun isOnLine(other: Vector2, epsilon: Float): Boolean {
        return MathUtils.isZero(x * other.y - y * other.x, epsilon)
    }

    fun isCollinear(other: Vector2, epsilon: Float): Boolean {
        return isOnLine(other, epsilon) && dot(other) > 0f
    }

    fun isCollinear(other: Vector2): Boolean {
        return isOnLine(other) && dot(other) > 0f
    }

    fun isCollinearOpposite(other: Vector2, epsilon: Float): Boolean {
        return isOnLine(other, epsilon) && dot(other) < 0f
    }

    fun isCollinearOpposite(other: Vector2): Boolean {
        return isOnLine(other) && dot(other) < 0f
    }

    fun isPerpendicular(vector: Vector2): Boolean {
        return MathUtils.isZero(dot(vector))
    }

    fun isPerpendicular(vector: Vector2, epsilon: Float): Boolean {
        return MathUtils.isZero(dot(vector), epsilon)
    }

    fun hasSameDirection(vector: Vector2): Boolean {
        return dot(vector) > 0
    }

    fun hasOppositeDirection(vector: Vector2): Boolean {
        return dot(vector) < 0
    }

    fun setZero(): Vector2 {
        x = 0f
        y = 0f
        return this
    }

    companion object {
        private const val serialVersionUID = 913902788239530931L
        val X = Vector2(1, 0)
        val Y = Vector2(0, 1)
        val Zero = Vector2(0, 0)
        fun len(x: Float, y: Float): Float {
            return java.lang.Math.sqrt(x * x + y * y.toDouble())
        }

        fun len2(x: Float, y: Float): Float {
            return x * x + y * y
        }

        fun dot(x1: Float, y1: Float, x2: Float, y2: Float): Float {
            return x1 * x2 + y1 * y2
        }

        fun dst(x1: Float, y1: Float, x2: Float, y2: Float): Float {
            val x_d = x2 - x1
            val y_d = y2 - y1
            return java.lang.Math.sqrt(x_d * x_d + y_d * y_d.toDouble())
        }

        fun dst2(x1: Float, y1: Float, x2: Float, y2: Float): Float {
            val x_d = x2 - x1
            val y_d = y2 - y1
            return x_d * x_d + y_d * y_d
        }
    }
}
