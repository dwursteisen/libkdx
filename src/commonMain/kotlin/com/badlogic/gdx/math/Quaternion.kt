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

import com.badlogic.gdx.math.Affine2
import com.badlogic.gdx.math.BSpline
import com.badlogic.gdx.math.Bezier
import com.badlogic.gdx.math.CatmullRomSpline
import com.badlogic.gdx.math.CumulativeDistribution.CumulativeValue
import com.badlogic.gdx.math.DelaunayTriangulator
import com.badlogic.gdx.math.EarClippingTriangulator
import com.badlogic.gdx.math.Frustum
import com.badlogic.gdx.math.GeometryUtils
import com.badlogic.gdx.math.GridPoint2
import com.badlogic.gdx.math.GridPoint3
import com.badlogic.gdx.math.Intersector
import com.badlogic.gdx.math.Intersector.MinimumTranslationVector
import com.badlogic.gdx.math.Intersector.SplitTriangle
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.MathUtils.Sin
import com.badlogic.gdx.math.Matrix3
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Plane
import com.badlogic.gdx.math.Plane.PlaneSide
import com.badlogic.gdx.math.RandomXS128
import java.lang.RuntimeException

/**
 * A simple quaternion class.
 *
 * @author badlogicgames@gmail.com
 * @author vesuvio
 * @author xoppa
 * @see [http://en.wikipedia.org/wiki/Quaternion](http://en.wikipedia.org/wiki/Quaternion)
 */
class Quaternion : Serializable {

    var x = 0f
    var y = 0f
    var z = 0f
    var w = 0f

    /**
     * Constructor, sets the four components of the quaternion.
     *
     * @param x The x-component
     * @param y The y-component
     * @param z The z-component
     * @param w The w-component
     */
    constructor(x: Float, y: Float, z: Float, w: Float) {
        this[x, y, z] = w
    }

    constructor() {
        idt()
    }

    /**
     * Constructor, sets the quaternion components from the given quaternion.
     *
     * @param quaternion The quaternion to copy.
     */
    constructor(quaternion: Quaternion) {
        this.set(quaternion)
    }

    /**
     * Constructor, sets the quaternion from the given axis vector and the angle around that axis in degrees.
     *
     * @param axis  The axis
     * @param angle The angle in degrees.
     */
    constructor(axis: Vector3, angle: Float) {
        this[axis] = angle
    }

    /**
     * Sets the components of the quaternion
     *
     * @param x The x-component
     * @param y The y-component
     * @param z The z-component
     * @param w The w-component
     * @return This quaternion for chaining
     */
    operator fun set(x: Float, y: Float, z: Float, w: Float): Quaternion {
        this.x = x
        this.y = y
        this.z = z
        this.w = w
        return this
    }

    /**
     * Sets the quaternion components from the given quaternion.
     *
     * @param quaternion The quaternion.
     * @return This quaternion for chaining.
     */
    fun set(quaternion: Quaternion): Quaternion {
        return this.set(quaternion.x, quaternion.y, quaternion.z, quaternion.w)
    }

    /**
     * Sets the quaternion components from the given axis and angle around that axis.
     *
     * @param axis  The axis
     * @param angle The angle in degrees
     * @return This quaternion for chaining.
     */
    operator fun set(axis: Vector3, angle: Float): Quaternion {
        return setFromAxis(axis.x, axis.y, axis.z, angle)
    }

    /**
     * @return a copy of this quaternion
     */
    fun cpy(): Quaternion {
        return Quaternion(this)
    }

    /**
     * @return the euclidean length of this quaternion
     */
    fun len(): Float {
        return java.lang.Math.sqrt(x * x + y * y + z * z + (w * w).toDouble())
    }

    override fun toString(): String {
        return "[$x|$y|$z|$w]"
    }

    /**
     * Sets the quaternion to the given euler angles in degrees.
     *
     * @param yaw   the rotation around the y axis in degrees
     * @param pitch the rotation around the x axis in degrees
     * @param roll  the rotation around the z axis degrees
     * @return this quaternion
     */
    fun setEulerAngles(yaw: Float, pitch: Float, roll: Float): Quaternion {
        return setEulerAnglesRad(yaw * MathUtils.degreesToRadians, pitch * MathUtils.degreesToRadians, roll
            * MathUtils.degreesToRadians)
    }

    /**
     * Sets the quaternion to the given euler angles in radians.
     *
     * @param yaw   the rotation around the y axis in radians
     * @param pitch the rotation around the x axis in radians
     * @param roll  the rotation around the z axis in radians
     * @return this quaternion
     */
    fun setEulerAnglesRad(yaw: Float, pitch: Float, roll: Float): Quaternion {
        val hr = roll * 0.5f
        val shr = java.lang.Math.sin(hr.toDouble()) as Float
        val chr = java.lang.Math.cos(hr.toDouble()) as Float
        val hp = pitch * 0.5f
        val shp = java.lang.Math.sin(hp.toDouble()) as Float
        val chp = java.lang.Math.cos(hp.toDouble()) as Float
        val hy = yaw * 0.5f
        val shy = java.lang.Math.sin(hy.toDouble()) as Float
        val chy = java.lang.Math.cos(hy.toDouble()) as Float
        val chy_shp = chy * shp
        val shy_chp = shy * chp
        val chy_chp = chy * chp
        val shy_shp = shy * shp
        x = chy_shp * chr + shy_chp * shr // cos(yaw/2) * sin(pitch/2) * cos(roll/2) + sin(yaw/2) * cos(pitch/2) * sin(roll/2)
        y = shy_chp * chr - chy_shp * shr // sin(yaw/2) * cos(pitch/2) * cos(roll/2) - cos(yaw/2) * sin(pitch/2) * sin(roll/2)
        z = chy_chp * shr - shy_shp * chr // cos(yaw/2) * cos(pitch/2) * sin(roll/2) - sin(yaw/2) * sin(pitch/2) * cos(roll/2)
        w = chy_chp * chr + shy_shp * shr // cos(yaw/2) * cos(pitch/2) * cos(roll/2) + sin(yaw/2) * sin(pitch/2) * sin(roll/2)
        return this
    }

    /**
     * Get the pole of the gimbal lock, if any.
     *
     * @return positive (+1) for north pole, negative (-1) for south pole, zero (0) when no gimbal lock
     */
    val gimbalPole: Int
        get() {
            val t = y * x + z * w
            return if (t > 0.499f) 1 else if (t < -0.499f) -1 else 0
        }

    /**
     * Get the roll euler angle in radians, which is the rotation around the z axis. Requires that this quaternion is normalized.
     *
     * @return the rotation around the z axis in radians (between -PI and +PI)
     */
    val rollRad: Float
        get() {
            val pole = gimbalPole
            return if (pole == 0) MathUtils.atan2(2f * (w * z + y * x), 1f - 2f * (x * x + z * z)) else pole.toFloat() * 2f
                * MathUtils.atan2(y, w)
        }

    /**
     * Get the roll euler angle in degrees, which is the rotation around the z axis. Requires that this quaternion is normalized.
     *
     * @return the rotation around the z axis in degrees (between -180 and +180)
     */
    val roll: Float
        get() = rollRad * MathUtils.radiansToDegrees

    /**
     * Get the pitch euler angle in radians, which is the rotation around the x axis. Requires that this quaternion is normalized.
     *
     * @return the rotation around the x axis in radians (between -(PI/2) and +(PI/2))
     */
    val pitchRad: Float
        get() {
            val pole = gimbalPole
            return if (pole == 0) java.lang.Math.asin(MathUtils.clamp(2f * (w * x - z * y), -1f, 1f)) else pole.toFloat() * MathUtils.PI * 0.5f
        }

    /**
     * Get the pitch euler angle in degrees, which is the rotation around the x axis. Requires that this quaternion is normalized.
     *
     * @return the rotation around the x axis in degrees (between -90 and +90)
     */
    val pitch: Float
        get() = pitchRad * MathUtils.radiansToDegrees

    /**
     * Get the yaw euler angle in radians, which is the rotation around the y axis. Requires that this quaternion is normalized.
     *
     * @return the rotation around the y axis in radians (between -PI and +PI)
     */
    val yawRad: Float
        get() = if (gimbalPole == 0) MathUtils.atan2(2f * (y * w + x * z), 1f - 2f * (y * y + x * x)) else 0f

    /**
     * Get the yaw euler angle in degrees, which is the rotation around the y axis. Requires that this quaternion is normalized.
     *
     * @return the rotation around the y axis in degrees (between -180 and +180)
     */
    val yaw: Float
        get() = yawRad * MathUtils.radiansToDegrees

    /**
     * @return the length of this quaternion without square root
     */
    fun len2(): Float {
        return x * x + y * y + z * z + w * w
    }

    /**
     * Normalizes this quaternion to unit length
     *
     * @return the quaternion for chaining
     */
    fun nor(): Quaternion {
        var len = len2()
        if (len != 0f && !MathUtils.isEqual(len, 1f)) {
            len = java.lang.Math.sqrt(len.toDouble())
            w /= len
            x /= len
            y /= len
            z /= len
        }
        return this
    }

    /**
     * Conjugate the quaternion.
     *
     * @return This quaternion for chaining
     */
    fun conjugate(): Quaternion {
        x = -x
        y = -y
        z = -z
        return this
    }
    // TODO : this would better fit into the vector3 class
    /**
     * Transforms the given vector using this quaternion
     *
     * @param v Vector to transform
     */
    fun transform(v: Vector3): Vector3 {
        tmp2.set(this)
        tmp2.conjugate()
        tmp2.mulLeft(tmp1.set(v.x, v.y, v.z, 0f)).mulLeft(this)
        v.x = tmp2.x
        v.y = tmp2.y
        v.z = tmp2.z
        return v
    }

    /**
     * Multiplies this quaternion with another one in the form of this = this * other
     *
     * @param other Quaternion to multiply with
     * @return This quaternion for chaining
     */
    fun mul(other: Quaternion): Quaternion {
        val newX = w * other.x + x * other.w + y * other.z - z * other.y
        val newY = w * other.y + y * other.w + z * other.x - x * other.z
        val newZ = w * other.z + z * other.w + x * other.y - y * other.x
        val newW = w * other.w - x * other.x - y * other.y - z * other.z
        x = newX
        y = newY
        z = newZ
        w = newW
        return this
    }

    /**
     * Multiplies this quaternion with another one in the form of this = this * other
     *
     * @param x the x component of the other quaternion to multiply with
     * @param y the y component of the other quaternion to multiply with
     * @param z the z component of the other quaternion to multiply with
     * @param w the w component of the other quaternion to multiply with
     * @return This quaternion for chaining
     */
    fun mul(x: Float, y: Float, z: Float, w: Float): Quaternion {
        val newX = this.w * x + this.x * w + this.y * z - this.z * y
        val newY = this.w * y + this.y * w + this.z * x - this.x * z
        val newZ = this.w * z + this.z * w + this.x * y - this.y * x
        val newW = this.w * w - this.x * x - this.y * y - this.z * z
        this.x = newX
        this.y = newY
        this.z = newZ
        this.w = newW
        return this
    }

    /**
     * Multiplies this quaternion with another one in the form of this = other * this
     *
     * @param other Quaternion to multiply with
     * @return This quaternion for chaining
     */
    fun mulLeft(other: Quaternion): Quaternion {
        val newX = other.w * x + other.x * w + other.y * z - other.z * y
        val newY = other.w * y + other.y * w + other.z * x - other.x * z
        val newZ = other.w * z + other.z * w + other.x * y - other.y * x
        val newW = other.w * w - other.x * x - other.y * y - other.z * z
        x = newX
        y = newY
        z = newZ
        w = newW
        return this
    }

    /**
     * Multiplies this quaternion with another one in the form of this = other * this
     *
     * @param x the x component of the other quaternion to multiply with
     * @param y the y component of the other quaternion to multiply with
     * @param z the z component of the other quaternion to multiply with
     * @param w the w component of the other quaternion to multiply with
     * @return This quaternion for chaining
     */
    fun mulLeft(x: Float, y: Float, z: Float, w: Float): Quaternion {
        val newX = w * this.x + x * this.w + y * this.z - z * this.y
        val newY = w * this.y + y * this.w + z * this.x - x * this.z
        val newZ = w * this.z + z * this.w + x * this.y - y * this.x
        val newW = w * this.w - x * this.x - y * this.y - z * this.z
        this.x = newX
        this.y = newY
        this.z = newZ
        this.w = newW
        return this
    }

    /**
     * Add the x,y,z,w components of the passed in quaternion to the ones of this quaternion
     */
    fun add(quaternion: Quaternion): Quaternion {
        x += quaternion.x
        y += quaternion.y
        z += quaternion.z
        w += quaternion.w
        return this
    }

    /**
     * Add the x,y,z,w components of the passed in quaternion to the ones of this quaternion
     */
    fun add(qx: Float, qy: Float, qz: Float, qw: Float): Quaternion {
        x += qx
        y += qy
        z += qz
        w += qw
        return this
    }
    // TODO : the matrix4 set(quaternion) doesnt set the last row+col of the matrix to 0,0,0,1 so... that's why there is this
    // method
    /**
     * Fills a 4x4 matrix with the rotation matrix represented by this quaternion.
     *
     * @param matrix Matrix to fill
     */
    fun toMatrix(matrix: FloatArray) {
        val xx = x * x
        val xy = x * y
        val xz = x * z
        val xw = x * w
        val yy = y * y
        val yz = y * z
        val yw = y * w
        val zz = z * z
        val zw = z * w
        // Set matrix from quaternion
        matrix[Matrix4.M00] = 1 - 2 * (yy + zz)
        matrix[Matrix4.M01] = 2 * (xy - zw)
        matrix[Matrix4.M02] = 2 * (xz + yw)
        matrix[Matrix4.M03] = 0
        matrix[Matrix4.M10] = 2 * (xy + zw)
        matrix[Matrix4.M11] = 1 - 2 * (xx + zz)
        matrix[Matrix4.M12] = 2 * (yz - xw)
        matrix[Matrix4.M13] = 0
        matrix[Matrix4.M20] = 2 * (xz - yw)
        matrix[Matrix4.M21] = 2 * (yz + xw)
        matrix[Matrix4.M22] = 1 - 2 * (xx + yy)
        matrix[Matrix4.M23] = 0
        matrix[Matrix4.M30] = 0
        matrix[Matrix4.M31] = 0
        matrix[Matrix4.M32] = 0
        matrix[Matrix4.M33] = 1
    }

    /**
     * Sets the quaternion to an identity Quaternion
     *
     * @return this quaternion for chaining
     */
    fun idt(): Quaternion {
        return this.set(0f, 0f, 0f, 1f)
    }

    /**
     * @return If this quaternion is an identity Quaternion
     */
    val isIdentity: Boolean
        get() = MathUtils.isZero(x) && MathUtils.isZero(y) && MathUtils.isZero(z) && MathUtils.isEqual(w, 1f)

    /**
     * @return If this quaternion is an identity Quaternion
     */
    fun isIdentity(tolerance: Float): Boolean {
        return (MathUtils.isZero(x, tolerance) && MathUtils.isZero(y, tolerance) && MathUtils.isZero(z, tolerance)
            && MathUtils.isEqual(w, 1f, tolerance))
    }
    // todo : the setFromAxis(v3,float) method should replace the set(v3,float) method
    /**
     * Sets the quaternion components from the given axis and angle around that axis.
     *
     * @param axis    The axis
     * @param degrees The angle in degrees
     * @return This quaternion for chaining.
     */
    fun setFromAxis(axis: Vector3, degrees: Float): Quaternion {
        return setFromAxis(axis.x, axis.y, axis.z, degrees)
    }

    /**
     * Sets the quaternion components from the given axis and angle around that axis.
     *
     * @param axis    The axis
     * @param radians The angle in radians
     * @return This quaternion for chaining.
     */
    fun setFromAxisRad(axis: Vector3, radians: Float): Quaternion {
        return setFromAxisRad(axis.x, axis.y, axis.z, radians)
    }

    /**
     * Sets the quaternion components from the given axis and angle around that axis.
     *
     * @param x       X direction of the axis
     * @param y       Y direction of the axis
     * @param z       Z direction of the axis
     * @param degrees The angle in degrees
     * @return This quaternion for chaining.
     */
    fun setFromAxis(x: Float, y: Float, z: Float, degrees: Float): Quaternion {
        return setFromAxisRad(x, y, z, degrees * MathUtils.degreesToRadians)
    }

    /**
     * Sets the quaternion components from the given axis and angle around that axis.
     *
     * @param x       X direction of the axis
     * @param y       Y direction of the axis
     * @param z       Z direction of the axis
     * @param radians The angle in radians
     * @return This quaternion for chaining.
     */
    fun setFromAxisRad(x: Float, y: Float, z: Float, radians: Float): Quaternion {
        var d = Vector3.len(x, y, z)
        if (d == 0f) return idt()
        d = 1f / d
        val l_ang: Float = if (radians < 0) MathUtils.PI2 - -radians % MathUtils.PI2 else radians % MathUtils.PI2
        val l_sin = java.lang.Math.sin(l_ang / 2.toDouble()) as Float
        val l_cos = java.lang.Math.cos(l_ang / 2.toDouble()) as Float
        return this.set(d * x * l_sin, d * y * l_sin, d * z * l_sin, l_cos).nor()
    }

    /**
     * Sets the Quaternion from the given matrix, optionally removing any scaling.
     */
    fun setFromMatrix(normalizeAxes: Boolean, matrix: Matrix4): Quaternion {
        return setFromAxes(normalizeAxes, matrix.`val`.get(Matrix4.M00), matrix.`val`.get(Matrix4.M01), matrix.`val`.get(Matrix4.M02),
            matrix.`val`.get(Matrix4.M10), matrix.`val`.get(Matrix4.M11), matrix.`val`.get(Matrix4.M12), matrix.`val`.get(Matrix4.M20),
            matrix.`val`.get(Matrix4.M21), matrix.`val`.get(Matrix4.M22))
    }

    /**
     * Sets the Quaternion from the given rotation matrix, which must not contain scaling.
     */
    fun setFromMatrix(matrix: Matrix4): Quaternion {
        return setFromMatrix(false, matrix)
    }

    /**
     * Sets the Quaternion from the given matrix, optionally removing any scaling.
     */
    fun setFromMatrix(normalizeAxes: Boolean, matrix: Matrix3): Quaternion {
        return setFromAxes(normalizeAxes, matrix.`val`.get(Matrix3.M00), matrix.`val`.get(Matrix3.M01), matrix.`val`.get(Matrix3.M02),
            matrix.`val`.get(Matrix3.M10), matrix.`val`.get(Matrix3.M11), matrix.`val`.get(Matrix3.M12), matrix.`val`.get(Matrix3.M20),
            matrix.`val`.get(Matrix3.M21), matrix.`val`.get(Matrix3.M22))
    }

    /**
     * Sets the Quaternion from the given rotation matrix, which must not contain scaling.
     */
    fun setFromMatrix(matrix: Matrix3): Quaternion {
        return setFromMatrix(false, matrix)
    }

    /**
     *
     *
     * Sets the Quaternion from the given x-, y- and z-axis which have to be orthonormal.
     *
     *
     *
     *
     * Taken from Bones framework for JPCT, see http://www.aptalkarga.com/bones/ which in turn took it from Graphics Gem code at
     * ftp://ftp.cis.upenn.edu/pub/graphics/shoemake/quatut.ps.Z.
     *
     *
     * @param xx x-axis x-coordinate
     * @param xy x-axis y-coordinate
     * @param xz x-axis z-coordinate
     * @param yx y-axis x-coordinate
     * @param yy y-axis y-coordinate
     * @param yz y-axis z-coordinate
     * @param zx z-axis x-coordinate
     * @param zy z-axis y-coordinate
     * @param zz z-axis z-coordinate
     */
    fun setFromAxes(xx: Float, xy: Float, xz: Float, yx: Float, yy: Float, yz: Float, zx: Float, zy: Float, zz: Float): Quaternion {
        return setFromAxes(false, xx, xy, xz, yx, yy, yz, zx, zy, zz)
    }

    /**
     *
     *
     * Sets the Quaternion from the given x-, y- and z-axis.
     *
     *
     *
     *
     * Taken from Bones framework for JPCT, see http://www.aptalkarga.com/bones/ which in turn took it from Graphics Gem code at
     * ftp://ftp.cis.upenn.edu/pub/graphics/shoemake/quatut.ps.Z.
     *
     *
     * @param normalizeAxes whether to normalize the axes (necessary when they contain scaling)
     * @param xx            x-axis x-coordinate
     * @param xy            x-axis y-coordinate
     * @param xz            x-axis z-coordinate
     * @param yx            y-axis x-coordinate
     * @param yy            y-axis y-coordinate
     * @param yz            y-axis z-coordinate
     * @param zx            z-axis x-coordinate
     * @param zy            z-axis y-coordinate
     * @param zz            z-axis z-coordinate
     */
    fun setFromAxes(normalizeAxes: Boolean, xx: Float, xy: Float, xz: Float, yx: Float, yy: Float, yz: Float, zx: Float,
                    zy: Float, zz: Float): Quaternion {
        var xx = xx
        var xy = xy
        var xz = xz
        var yx = yx
        var yy = yy
        var yz = yz
        var zx = zx
        var zy = zy
        var zz = zz
        if (normalizeAxes) {
            val lx = 1f / Vector3.len(xx, xy, xz)
            val ly = 1f / Vector3.len(yx, yy, yz)
            val lz = 1f / Vector3.len(zx, zy, zz)
            xx *= lx
            xy *= lx
            xz *= lx
            yx *= ly
            yy *= ly
            yz *= ly
            zx *= lz
            zy *= lz
            zz *= lz
        }
        // the trace is the sum of the diagonal elements; see
        // http://mathworld.wolfram.com/MatrixTrace.html
        val t = xx + yy + zz

        // we protect the division by s by ensuring that s>=1
        if (t >= 0) { // |w| >= .5
            var s = java.lang.Math.sqrt(t + 1.toDouble()) as Float // |s|>=1 ...
            w = 0.5f * s
            s = 0.5f / s // so this division isn't bad
            x = (zy - yz) * s
            y = (xz - zx) * s
            z = (yx - xy) * s
        } else if (xx > yy && xx > zz) {
            var s = java.lang.Math.sqrt(1.0 + xx - yy - zz) as Float // |s|>=1
            x = s * 0.5f // |x| >= .5
            s = 0.5f / s
            y = (yx + xy) * s
            z = (xz + zx) * s
            w = (zy - yz) * s
        } else if (yy > zz) {
            var s = java.lang.Math.sqrt(1.0 + yy - xx - zz) as Float // |s|>=1
            y = s * 0.5f // |y| >= .5
            s = 0.5f / s
            x = (yx + xy) * s
            z = (zy + yz) * s
            w = (xz - zx) * s
        } else {
            var s = java.lang.Math.sqrt(1.0 + zz - xx - yy) as Float // |s|>=1
            z = s * 0.5f // |z| >= .5
            s = 0.5f / s
            x = (xz + zx) * s
            y = (zy + yz) * s
            w = (yx - xy) * s
        }
        return this
    }

    /**
     * Set this quaternion to the rotation between two vectors.
     *
     * @param v1 The base vector, which should be normalized.
     * @param v2 The target vector, which should be normalized.
     * @return This quaternion for chaining
     */
    fun setFromCross(v1: Vector3, v2: Vector3): Quaternion {
        val dot: Float = MathUtils.clamp(v1.dot(v2), -1f, 1f)
        val angle = java.lang.Math.acos(dot.toDouble()) as Float
        return setFromAxisRad(v1.y * v2.z - v1.z * v2.y, v1.z * v2.x - v1.x * v2.z, v1.x * v2.y - v1.y * v2.x, angle)
    }

    /**
     * Set this quaternion to the rotation between two vectors.
     *
     * @param x1 The base vectors x value, which should be normalized.
     * @param y1 The base vectors y value, which should be normalized.
     * @param z1 The base vectors z value, which should be normalized.
     * @param x2 The target vector x value, which should be normalized.
     * @param y2 The target vector y value, which should be normalized.
     * @param z2 The target vector z value, which should be normalized.
     * @return This quaternion for chaining
     */
    fun setFromCross(x1: Float, y1: Float, z1: Float, x2: Float, y2: Float, z2: Float): Quaternion {
        val dot: Float = MathUtils.clamp(Vector3.dot(x1, y1, z1, x2, y2, z2), -1f, 1f)
        val angle = java.lang.Math.acos(dot.toDouble()) as Float
        return setFromAxisRad(y1 * z2 - z1 * y2, z1 * x2 - x1 * z2, x1 * y2 - y1 * x2, angle)
    }

    /**
     * Spherical linear interpolation between this quaternion and the other quaternion, based on the alpha value in the range
     * [0,1]. Taken from Bones framework for JPCT, see http://www.aptalkarga.com/bones/
     *
     * @param end   the end quaternion
     * @param alpha alpha in the range [0,1]
     * @return this quaternion for chaining
     */
    fun slerp(end: Quaternion, alpha: Float): Quaternion {
        val d = x * end.x + y * end.y + z * end.z + w * end.w
        val absDot = if (d < 0f) -d else d

        // Set the first and second scale for the interpolation
        var scale0 = 1f - alpha
        var scale1 = alpha

        // Check if the angle between the 2 quaternions was big enough to
        // warrant such calculations
        if (1 - absDot > 0.1) { // Get the angle between the 2 quaternions,
            // and then store the sin() of that angle
            val angle = java.lang.Math.acos(absDot.toDouble()) as Float
            val invSinTheta = 1f / java.lang.Math.sin(angle.toDouble()) as Float

            // Calculate the scale for q1 and q2, according to the angle and
            // it's sine value
            scale0 = java.lang.Math.sin((1f - alpha) * angle.toDouble()) as Float * invSinTheta
            scale1 = java.lang.Math.sin((alpha * angle).toDouble()) as Float * invSinTheta
        }
        if (d < 0f) scale1 = -scale1

        // Calculate the x, y, z and w values for the quaternion by using a
        // special form of linear interpolation for quaternions.
        x = scale0 * x + scale1 * end.x
        y = scale0 * y + scale1 * end.y
        z = scale0 * z + scale1 * end.z
        w = scale0 * w + scale1 * end.w

        // Return the interpolated quaternion
        return this
    }

    /**
     * Spherical linearly interpolates multiple quaternions and stores the result in this Quaternion. Will not destroy the data
     * previously inside the elements of q. result = (q_1^w_1)*(q_2^w_2)* ... *(q_n^w_n) where w_i=1/n.
     *
     * @param q List of quaternions
     * @return This quaternion for chaining
     */
    fun slerp(q: Array<Quaternion>): Quaternion {

        // Calculate exponents and multiply everything from left to right
        val w = 1.0f / q.size
        set(q[0]).exp(w)
        for (i in 1 until q.size) mul(tmp1.set(q[i]).exp(w))
        nor()
        return this
    }

    /**
     * Spherical linearly interpolates multiple quaternions by the given weights and stores the result in this Quaternion. Will not
     * destroy the data previously inside the elements of q or w. result = (q_1^w_1)*(q_2^w_2)* ... *(q_n^w_n) where the sum of w_i
     * is 1. Lists must be equal in length.
     *
     * @param q List of quaternions
     * @param w List of weights
     * @return This quaternion for chaining
     */
    fun slerp(q: Array<Quaternion>, w: FloatArray): Quaternion {

        // Calculate exponents and multiply everything from left to right
        set(q[0]).exp(w[0])
        for (i in 1 until q.size) mul(tmp1.set(q[i]).exp(w[i]))
        nor()
        return this
    }

    /**
     * Calculates (this quaternion)^alpha where alpha is a real number and stores the result in this quaternion. See
     * http://en.wikipedia.org/wiki/Quaternion#Exponential.2C_logarithm.2C_and_power
     *
     * @param alpha Exponent
     * @return This quaternion for chaining
     */
    fun exp(alpha: Float): Quaternion {

        // Calculate |q|^alpha
        val norm = len()
        val normExp = java.lang.Math.pow(norm.toDouble(), alpha.toDouble()) as Float

        // Calculate theta
        val theta = java.lang.Math.acos(w / norm.toDouble()) as Float

        // Calculate coefficient of basis elements
        var coeff = 0f
        coeff = if (java.lang.Math.abs(theta) < 0.001) // If theta is small enough, use the limit of sin(alpha*theta) / sin(theta) instead of actual
        // value
            normExp * alpha / norm else (normExp * java.lang.Math.sin(alpha * theta.toDouble()) / (norm * java.lang.Math.sin(theta.toDouble())))

        // Write results
        w = (normExp * java.lang.Math.cos(alpha * theta.toDouble())) as Float
        x *= coeff
        y *= coeff
        z *= coeff

        // Fix any possible discrepancies
        nor()
        return this
    }

    override fun hashCode(): Int {
        val prime = 31
        var result = 1
        result = prime * result + NumberUtils.floatToRawIntBits(w)
        result = prime * result + NumberUtils.floatToRawIntBits(x)
        result = prime * result + NumberUtils.floatToRawIntBits(y)
        result = prime * result + NumberUtils.floatToRawIntBits(z)
        return result
    }

    override fun equals(obj: Any?): Boolean {
        if (this === obj) {
            return true
        }
        if (obj == null) {
            return false
        }
        if (obj !is Quaternion) {
            return false
        }
        val other = obj
        return (NumberUtils.floatToRawIntBits(w) === NumberUtils.floatToRawIntBits(other.w)
            && NumberUtils.floatToRawIntBits(x) === NumberUtils.floatToRawIntBits(other.x)
            && NumberUtils.floatToRawIntBits(y) === NumberUtils.floatToRawIntBits(other.y)
            && NumberUtils.floatToRawIntBits(z) === NumberUtils.floatToRawIntBits(other.z))
    }

    /**
     * Get the dot product between this and the other quaternion (commutative).
     *
     * @param other the other quaternion.
     * @return the dot product of this and the other quaternion.
     */
    fun dot(other: Quaternion): Float {
        return x * other.x + y * other.y + z * other.z + w * other.w
    }

    /**
     * Get the dot product between this and the other quaternion (commutative).
     *
     * @param x the x component of the other quaternion
     * @param y the y component of the other quaternion
     * @param z the z component of the other quaternion
     * @param w the w component of the other quaternion
     * @return the dot product of this and the other quaternion.
     */
    fun dot(x: Float, y: Float, z: Float, w: Float): Float {
        return this.x * x + this.y * y + this.z * z + this.w * w
    }

    /**
     * Multiplies the components of this quaternion with the given scalar.
     *
     * @param scalar the scalar.
     * @return this quaternion for chaining.
     */
    fun mul(scalar: Float): Quaternion {
        x *= scalar
        y *= scalar
        z *= scalar
        w *= scalar
        return this
    }

    /**
     * Get the axis angle representation of the rotation in degrees. The supplied vector will receive the axis (x, y and z values)
     * of the rotation and the value returned is the angle in degrees around that axis. Note that this method will alter the
     * supplied vector, the existing value of the vector is ignored.  This will normalize this quaternion if needed. The
     * received axis is a unit vector. However, if this is an identity quaternion (no rotation), then the length of the axis may be
     * zero.
     *
     * @param axis vector which will receive the axis
     * @return the angle in degrees
     * @see [wikipedia](http://en.wikipedia.org/wiki/Axis%E2%80%93angle_representation)
     *
     * @see [calculation](http://www.euclideanspace.com/maths/geometry/rotations/conversions/quaternionToAngle)
     */
    fun getAxisAngle(axis: Vector3): Float {
        return getAxisAngleRad(axis) * MathUtils.radiansToDegrees
    }

    /**
     * Get the axis-angle representation of the rotation in radians. The supplied vector will receive the axis (x, y and z values)
     * of the rotation and the value returned is the angle in radians around that axis. Note that this method will alter the
     * supplied vector, the existing value of the vector is ignored.  This will normalize this quaternion if needed. The
     * received axis is a unit vector. However, if this is an identity quaternion (no rotation), then the length of the axis may be
     * zero.
     *
     * @param axis vector which will receive the axis
     * @return the angle in radians
     * @see [wikipedia](http://en.wikipedia.org/wiki/Axis%E2%80%93angle_representation)
     *
     * @see [calculation](http://www.euclideanspace.com/maths/geometry/rotations/conversions/quaternionToAngle)
     */
    fun getAxisAngleRad(axis: Vector3): Float {
        if (w > 1) nor() // if w>1 acos and sqrt will produce errors, this cant happen if quaternion is normalised
        val angle = (2.0 * java.lang.Math.acos(w.toDouble())) as Float
        val s: Double = java.lang.Math.sqrt(1 - w * w.toDouble()) // assuming quaternion normalised then w is less than 1, so term always positive.
        if (s < MathUtils.FLOAT_ROUNDING_ERROR) { // test to avoid divide by zero, s is always positive due to sqrt
            // if s close to zero then direction of axis not important
            axis.x = x // if it is important that axis is normalised then replace with x=1; y=z=0;
            axis.y = y
            axis.z = z
        } else {
            axis.x = (x / s).toFloat() // normalise axis
            axis.y = (y / s).toFloat()
            axis.z = (z / s).toFloat()
        }
        return angle
    }

    /**
     * Get the angle in radians of the rotation this quaternion represents. Does not normalize the quaternion. Use
     * [.getAxisAngleRad] to get both the axis and the angle of this rotation. Use
     * [.getAngleAroundRad] to get the angle around a specific axis.
     *
     * @return the angle in radians of the rotation
     */
    val angleRad: Float
        get() = (2.0 * java.lang.Math.acos(if (w > 1) w / len() else w.toDouble())) as Float

    /**
     * Get the angle in degrees of the rotation this quaternion represents. Use [.getAxisAngle] to get both the axis
     * and the angle of this rotation. Use [.getAngleAround] to get the angle around a specific axis.
     *
     * @return the angle in degrees of the rotation
     */
    val angle: Float
        get() = angleRad * MathUtils.radiansToDegrees

    /**
     * Get the swing rotation and twist rotation for the specified axis. The twist rotation represents the rotation around the
     * specified axis. The swing rotation represents the rotation of the specified axis itself, which is the rotation around an
     * axis perpendicular to the specified axis.  The swing and twist rotation can be used to reconstruct the original
     * quaternion: this = swing * twist
     *
     * @param axisX the X component of the normalized axis for which to get the swing and twist rotation
     * @param axisY the Y component of the normalized axis for which to get the swing and twist rotation
     * @param axisZ the Z component of the normalized axis for which to get the swing and twist rotation
     * @param swing will receive the swing rotation: the rotation around an axis perpendicular to the specified axis
     * @param twist will receive the twist rotation: the rotation around the specified axis
     * @see [calculation](http://www.euclideanspace.com/maths/geometry/rotations/for/decomposition)
     */
    fun getSwingTwist(axisX: Float, axisY: Float, axisZ: Float, swing: Quaternion,
                      twist: Quaternion) {
        val d = Vector3.dot(x, y, z, axisX, axisY, axisZ)
        twist.set(axisX * d, axisY * d, axisZ * d, w).nor()
        if (d < 0) twist.mul(-1f)
        swing.set(twist).conjugate().mulLeft(this)
    }

    /**
     * Get the swing rotation and twist rotation for the specified axis. The twist rotation represents the rotation around the
     * specified axis. The swing rotation represents the rotation of the specified axis itself, which is the rotation around an
     * axis perpendicular to the specified axis.  The swing and twist rotation can be used to reconstruct the original
     * quaternion: this = swing * twist
     *
     * @param axis  the normalized axis for which to get the swing and twist rotation
     * @param swing will receive the swing rotation: the rotation around an axis perpendicular to the specified axis
     * @param twist will receive the twist rotation: the rotation around the specified axis
     * @see [calculation](http://www.euclideanspace.com/maths/geometry/rotations/for/decomposition)
     */
    fun getSwingTwist(axis: Vector3, swing: Quaternion, twist: Quaternion) {
        getSwingTwist(axis.x, axis.y, axis.z, swing, twist)
    }

    /**
     * Get the angle in radians of the rotation around the specified axis. The axis must be normalized.
     *
     * @param axisX the x component of the normalized axis for which to get the angle
     * @param axisY the y component of the normalized axis for which to get the angle
     * @param axisZ the z component of the normalized axis for which to get the angle
     * @return the angle in radians of the rotation around the specified axis
     */
    fun getAngleAroundRad(axisX: Float, axisY: Float, axisZ: Float): Float {
        val d = Vector3.dot(x, y, z, axisX, axisY, axisZ)
        val l2 = len2(axisX * d, axisY * d, axisZ * d, w)
        return if (MathUtils.isZero(l2)) 0f else (2.0 * java.lang.Math.acos(MathUtils.clamp(
            ((if (d < 0) -w else w) / java.lang.Math.sqrt(l2.toDouble())) as Float, -1f, 1f))) as Float
    }

    /**
     * Get the angle in radians of the rotation around the specified axis. The axis must be normalized.
     *
     * @param axis the normalized axis for which to get the angle
     * @return the angle in radians of the rotation around the specified axis
     */
    fun getAngleAroundRad(axis: Vector3): Float {
        return getAngleAroundRad(axis.x, axis.y, axis.z)
    }

    /**
     * Get the angle in degrees of the rotation around the specified axis. The axis must be normalized.
     *
     * @param axisX the x component of the normalized axis for which to get the angle
     * @param axisY the y component of the normalized axis for which to get the angle
     * @param axisZ the z component of the normalized axis for which to get the angle
     * @return the angle in degrees of the rotation around the specified axis
     */
    fun getAngleAround(axisX: Float, axisY: Float, axisZ: Float): Float {
        return getAngleAroundRad(axisX, axisY, axisZ) * MathUtils.radiansToDegrees
    }

    /**
     * Get the angle in degrees of the rotation around the specified axis. The axis must be normalized.
     *
     * @param axis the normalized axis for which to get the angle
     * @return the angle in degrees of the rotation around the specified axis
     */
    fun getAngleAround(axis: Vector3): Float {
        return getAngleAround(axis.x, axis.y, axis.z)
    }

    companion object {
        private const val serialVersionUID = -7661875440774897168L
        private val tmp1 = Quaternion(0, 0, 0, 0)
        private val tmp2 = Quaternion(0, 0, 0, 0)

        /**
         * @return the euclidean length of the specified quaternion
         */
        fun len(x: Float, y: Float, z: Float, w: Float): Float {
            return java.lang.Math.sqrt(x * x + y * y + z * z + (w * w).toDouble())
        }

        fun len2(x: Float, y: Float, z: Float, w: Float): Float {
            return x * x + y * y + z * z + w * w
        }

        /**
         * Get the dot product between the two quaternions (commutative).
         *
         * @param x1 the x component of the first quaternion
         * @param y1 the y component of the first quaternion
         * @param z1 the z component of the first quaternion
         * @param w1 the w component of the first quaternion
         * @param x2 the x component of the second quaternion
         * @param y2 the y component of the second quaternion
         * @param z2 the z component of the second quaternion
         * @param w2 the w component of the second quaternion
         * @return the dot product between the first and second quaternion.
         */
        fun dot(x1: Float, y1: Float, z1: Float, w1: Float, x2: Float, y2: Float,
                z2: Float, w2: Float): Float {
            return x1 * x2 + y1 * y2 + z1 * z2 + w1 * w2
        }
    }
}
