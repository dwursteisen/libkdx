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
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Plane
import com.badlogic.gdx.math.Plane.PlaneSide
import com.badlogic.gdx.math.RandomXS128
import java.lang.RuntimeException

/**
 * A 3x3 [column major](http://en.wikipedia.org/wiki/Row-major_order#Column-major_order) matrix; useful for 2D
 * transforms.
 *
 * @author mzechner
 */
class Matrix3 : Serializable {

    /**
     * Get the values in this matrix.
     *
     * @return The float values that make up this matrix in column-major order.
     */
    var values = FloatArray(9)
    private val tmp = FloatArray(9)

    constructor() {
        idt()
    }

    constructor(matrix: Matrix3) {
        set(matrix)
    }

    /**
     * Constructs a matrix from the given float array. The array must have at least 9 elements; the first 9 will be copied.
     *
     * @param values The float array to copy. Remember that this matrix is in [column major](http://en.wikipedia.org/wiki/Row-major_order#Column-major_order) order. (The float array is
     * not modified.)
     */
    constructor(values: FloatArray?) {
        this.set(values)
    }

    /**
     * Sets this matrix to the identity matrix
     *
     * @return This matrix for the purpose of chaining operations.
     */
    fun idt(): Matrix3 {
        val `val` = values
        `val`[M00] = 1
        `val`[M10] = 0
        `val`[M20] = 0
        `val`[M01] = 0
        `val`[M11] = 1
        `val`[M21] = 0
        `val`[M02] = 0
        `val`[M12] = 0
        `val`[M22] = 1
        return this
    }

    /**
     * Postmultiplies this matrix with the provided matrix and stores the result in this matrix. For example:
     *
     * <pre>
     * A.mul(B) results in A := AB
    </pre> *
     *
     * @param m Matrix to multiply by.
     * @return This matrix for the purpose of chaining operations together.
     */
    fun mul(m: Matrix3): Matrix3 {
        val `val` = values
        val v00 = `val`[M00] * m.values[M00] + `val`[M01] * m.values[M10] + `val`[M02] * m.values[M20]
        val v01 = `val`[M00] * m.values[M01] + `val`[M01] * m.values[M11] + `val`[M02] * m.values[M21]
        val v02 = `val`[M00] * m.values[M02] + `val`[M01] * m.values[M12] + `val`[M02] * m.values[M22]
        val v10 = `val`[M10] * m.values[M00] + `val`[M11] * m.values[M10] + `val`[M12] * m.values[M20]
        val v11 = `val`[M10] * m.values[M01] + `val`[M11] * m.values[M11] + `val`[M12] * m.values[M21]
        val v12 = `val`[M10] * m.values[M02] + `val`[M11] * m.values[M12] + `val`[M12] * m.values[M22]
        val v20 = `val`[M20] * m.values[M00] + `val`[M21] * m.values[M10] + `val`[M22] * m.values[M20]
        val v21 = `val`[M20] * m.values[M01] + `val`[M21] * m.values[M11] + `val`[M22] * m.values[M21]
        val v22 = `val`[M20] * m.values[M02] + `val`[M21] * m.values[M12] + `val`[M22] * m.values[M22]
        `val`[M00] = v00
        `val`[M10] = v10
        `val`[M20] = v20
        `val`[M01] = v01
        `val`[M11] = v11
        `val`[M21] = v21
        `val`[M02] = v02
        `val`[M12] = v12
        `val`[M22] = v22
        return this
    }

    /**
     * Premultiplies this matrix with the provided matrix and stores the result in this matrix. For example:
     *
     * <pre>
     * A.mulLeft(B) results in A := BA
    </pre> *
     *
     * @param m The other Matrix to multiply by
     * @return This matrix for the purpose of chaining operations.
     */
    fun mulLeft(m: Matrix3): Matrix3 {
        val `val` = values
        val v00 = m.values[M00] * `val`[M00] + m.values[M01] * `val`[M10] + m.values[M02] * `val`[M20]
        val v01 = m.values[M00] * `val`[M01] + m.values[M01] * `val`[M11] + m.values[M02] * `val`[M21]
        val v02 = m.values[M00] * `val`[M02] + m.values[M01] * `val`[M12] + m.values[M02] * `val`[M22]
        val v10 = m.values[M10] * `val`[M00] + m.values[M11] * `val`[M10] + m.values[M12] * `val`[M20]
        val v11 = m.values[M10] * `val`[M01] + m.values[M11] * `val`[M11] + m.values[M12] * `val`[M21]
        val v12 = m.values[M10] * `val`[M02] + m.values[M11] * `val`[M12] + m.values[M12] * `val`[M22]
        val v20 = m.values[M20] * `val`[M00] + m.values[M21] * `val`[M10] + m.values[M22] * `val`[M20]
        val v21 = m.values[M20] * `val`[M01] + m.values[M21] * `val`[M11] + m.values[M22] * `val`[M21]
        val v22 = m.values[M20] * `val`[M02] + m.values[M21] * `val`[M12] + m.values[M22] * `val`[M22]
        `val`[M00] = v00
        `val`[M10] = v10
        `val`[M20] = v20
        `val`[M01] = v01
        `val`[M11] = v11
        `val`[M21] = v21
        `val`[M02] = v02
        `val`[M12] = v12
        `val`[M22] = v22
        return this
    }

    /**
     * Sets this matrix to a rotation matrix that will rotate any vector in counter-clockwise direction around the z-axis.
     *
     * @param degrees the angle in degrees.
     * @return This matrix for the purpose of chaining operations.
     */
    fun setToRotation(degrees: Float): Matrix3 {
        return setToRotationRad(MathUtils.degreesToRadians * degrees)
    }

    /**
     * Sets this matrix to a rotation matrix that will rotate any vector in counter-clockwise direction around the z-axis.
     *
     * @param radians the angle in radians.
     * @return This matrix for the purpose of chaining operations.
     */
    fun setToRotationRad(radians: Float): Matrix3 {
        val cos = java.lang.Math.cos(radians.toDouble()) as Float
        val sin = java.lang.Math.sin(radians.toDouble()) as Float
        val `val` = values
        `val`[M00] = cos
        `val`[M10] = sin
        `val`[M20] = 0
        `val`[M01] = -sin
        `val`[M11] = cos
        `val`[M21] = 0
        `val`[M02] = 0
        `val`[M12] = 0
        `val`[M22] = 1
        return this
    }

    fun setToRotation(axis: Vector3, degrees: Float): Matrix3 {
        return setToRotation(axis, MathUtils.cosDeg(degrees), MathUtils.sinDeg(degrees))
    }

    fun setToRotation(axis: Vector3, cos: Float, sin: Float): Matrix3 {
        val `val` = values
        val oc = 1.0f - cos
        `val`[M00] = oc * axis.x * axis.x + cos
        `val`[M10] = oc * axis.x * axis.y - axis.z * sin
        `val`[M20] = oc * axis.z * axis.x + axis.y * sin
        `val`[M01] = oc * axis.x * axis.y + axis.z * sin
        `val`[M11] = oc * axis.y * axis.y + cos
        `val`[M21] = oc * axis.y * axis.z - axis.x * sin
        `val`[M02] = oc * axis.z * axis.x - axis.y * sin
        `val`[M12] = oc * axis.y * axis.z + axis.x * sin
        `val`[M22] = oc * axis.z * axis.z + cos
        return this
    }

    /**
     * Sets this matrix to a translation matrix.
     *
     * @param x the translation in x
     * @param y the translation in y
     * @return This matrix for the purpose of chaining operations.
     */
    fun setToTranslation(x: Float, y: Float): Matrix3 {
        val `val` = values
        `val`[M00] = 1
        `val`[M10] = 0
        `val`[M20] = 0
        `val`[M01] = 0
        `val`[M11] = 1
        `val`[M21] = 0
        `val`[M02] = x
        `val`[M12] = y
        `val`[M22] = 1
        return this
    }

    /**
     * Sets this matrix to a translation matrix.
     *
     * @param translation The translation vector.
     * @return This matrix for the purpose of chaining operations.
     */
    fun setToTranslation(translation: Vector2): Matrix3 {
        val `val` = values
        `val`[M00] = 1
        `val`[M10] = 0
        `val`[M20] = 0
        `val`[M01] = 0
        `val`[M11] = 1
        `val`[M21] = 0
        `val`[M02] = translation.x
        `val`[M12] = translation.y
        `val`[M22] = 1
        return this
    }

    /**
     * Sets this matrix to a scaling matrix.
     *
     * @param scaleX the scale in x
     * @param scaleY the scale in y
     * @return This matrix for the purpose of chaining operations.
     */
    fun setToScaling(scaleX: Float, scaleY: Float): Matrix3 {
        val `val` = values
        `val`[M00] = scaleX
        `val`[M10] = 0
        `val`[M20] = 0
        `val`[M01] = 0
        `val`[M11] = scaleY
        `val`[M21] = 0
        `val`[M02] = 0
        `val`[M12] = 0
        `val`[M22] = 1
        return this
    }

    /**
     * Sets this matrix to a scaling matrix.
     *
     * @param scale The scale vector.
     * @return This matrix for the purpose of chaining operations.
     */
    fun setToScaling(scale: Vector2): Matrix3 {
        val `val` = values
        `val`[M00] = scale.x
        `val`[M10] = 0
        `val`[M20] = 0
        `val`[M01] = 0
        `val`[M11] = scale.y
        `val`[M21] = 0
        `val`[M02] = 0
        `val`[M12] = 0
        `val`[M22] = 1
        return this
    }

    override fun toString(): String {
        val `val` = values
        return """
            [${`val`[M00]}|${`val`[M01]}|${`val`[M02]}]
            [${`val`[M10]}|${`val`[M11]}|${`val`[M12]}]
            [${`val`[M20]}|${`val`[M21]}|${`val`[M22]}]
            """.trimIndent()
    }

    /**
     * @return The determinant of this matrix
     */
    fun det(): Float {
        val `val` = values
        return `val`[M00] * `val`[M11] * `val`[M22] + `val`[M01] * `val`[M12] * `val`[M20] + `val`[M02] * `val`[M10] * `val`[M21] - (`val`[M00]
            * `val`[M12] * `val`[M21]) - `val`[M01] * `val`[M10] * `val`[M22] - `val`[M02] * `val`[M11] * `val`[M20]
    }

    /**
     * Inverts this matrix given that the determinant is != 0.
     *
     * @return This matrix for the purpose of chaining operations.
     * @throws GdxRuntimeException if the matrix is singular (not invertible)
     */
    fun inv(): Matrix3 {
        val det = det()
        if (det == 0f) throw GdxRuntimeException("Can't invert a singular matrix")
        val inv_det = 1.0f / det
        val tmp = tmp
        val `val` = values
        tmp[M00] = `val`[M11] * `val`[M22] - `val`[M21] * `val`[M12]
        tmp[M10] = `val`[M20] * `val`[M12] - `val`[M10] * `val`[M22]
        tmp[M20] = `val`[M10] * `val`[M21] - `val`[M20] * `val`[M11]
        tmp[M01] = `val`[M21] * `val`[M02] - `val`[M01] * `val`[M22]
        tmp[M11] = `val`[M00] * `val`[M22] - `val`[M20] * `val`[M02]
        tmp[M21] = `val`[M20] * `val`[M01] - `val`[M00] * `val`[M21]
        tmp[M02] = `val`[M01] * `val`[M12] - `val`[M11] * `val`[M02]
        tmp[M12] = `val`[M10] * `val`[M02] - `val`[M00] * `val`[M12]
        tmp[M22] = `val`[M00] * `val`[M11] - `val`[M10] * `val`[M01]
        `val`[M00] = inv_det * tmp[M00]
        `val`[M10] = inv_det * tmp[M10]
        `val`[M20] = inv_det * tmp[M20]
        `val`[M01] = inv_det * tmp[M01]
        `val`[M11] = inv_det * tmp[M11]
        `val`[M21] = inv_det * tmp[M21]
        `val`[M02] = inv_det * tmp[M02]
        `val`[M12] = inv_det * tmp[M12]
        `val`[M22] = inv_det * tmp[M22]
        return this
    }

    /**
     * Copies the values from the provided matrix to this matrix.
     *
     * @param mat The matrix to copy.
     * @return This matrix for the purposes of chaining.
     */
    fun set(mat: Matrix3): Matrix3 {
        java.lang.System.arraycopy(mat.values, 0, values, 0, values.size)
        return this
    }

    /**
     * Copies the values from the provided affine matrix to this matrix. The last row is set to (0, 0, 1).
     *
     * @param affine The affine matrix to copy.
     * @return This matrix for the purposes of chaining.
     */
    fun set(affine: Affine2): Matrix3 {
        val `val` = values
        `val`[M00] = affine.m00
        `val`[M10] = affine.m10
        `val`[M20] = 0
        `val`[M01] = affine.m01
        `val`[M11] = affine.m11
        `val`[M21] = 0
        `val`[M02] = affine.m02
        `val`[M12] = affine.m12
        `val`[M22] = 1
        return this
    }

    /**
     * Sets this 3x3 matrix to the top left 3x3 corner of the provided 4x4 matrix.
     *
     * @param mat The matrix whose top left corner will be copied. This matrix will not be modified.
     * @return This matrix for the purpose of chaining operations.
     */
    fun set(mat: Matrix4): Matrix3 {
        val `val` = values
        `val`[M00] = mat.`val`.get(Matrix4.M00)
        `val`[M10] = mat.`val`.get(Matrix4.M10)
        `val`[M20] = mat.`val`.get(Matrix4.M20)
        `val`[M01] = mat.`val`.get(Matrix4.M01)
        `val`[M11] = mat.`val`.get(Matrix4.M11)
        `val`[M21] = mat.`val`.get(Matrix4.M21)
        `val`[M02] = mat.`val`.get(Matrix4.M02)
        `val`[M12] = mat.`val`.get(Matrix4.M12)
        `val`[M22] = mat.`val`.get(Matrix4.M22)
        return this
    }

    /**
     * Sets the matrix to the given matrix as a float array. The float array must have at least 9 elements; the first 9 will be
     * copied.
     *
     * @param values The matrix, in float form, that is to be copied. Remember that this matrix is in [column major](http://en.wikipedia.org/wiki/Row-major_order#Column-major_order) order.
     * @return This matrix for the purpose of chaining methods together.
     */
    fun set(values: FloatArray?): Matrix3 {
        java.lang.System.arraycopy(values, 0, this.values, 0, values!!.size)
        return this
    }

    /**
     * Adds a translational component to the matrix in the 3rd column. The other columns are untouched.
     *
     * @param vector The translation vector.
     * @return This matrix for the purpose of chaining.
     */
    fun trn(vector: Vector2): Matrix3 {
        values[M02] += vector.x
        values[M12] += vector.y
        return this
    }

    /**
     * Adds a translational component to the matrix in the 3rd column. The other columns are untouched.
     *
     * @param x The x-component of the translation vector.
     * @param y The y-component of the translation vector.
     * @return This matrix for the purpose of chaining.
     */
    fun trn(x: Float, y: Float): Matrix3 {
        values[M02] += x
        values[M12] += y
        return this
    }

    /**
     * Adds a translational component to the matrix in the 3rd column. The other columns are untouched.
     *
     * @param vector The translation vector. (The z-component of the vector is ignored because this is a 3x3 matrix)
     * @return This matrix for the purpose of chaining.
     */
    fun trn(vector: Vector3): Matrix3 {
        values[M02] += vector.x
        values[M12] += vector.y
        return this
    }

    /**
     * Postmultiplies this matrix by a translation matrix. Postmultiplication is also used by OpenGL ES' 1.x
     * glTranslate/glRotate/glScale.
     *
     * @param x The x-component of the translation vector.
     * @param y The y-component of the translation vector.
     * @return This matrix for the purpose of chaining.
     */
    fun translate(x: Float, y: Float): Matrix3 {
        val `val` = values
        tmp[M00] = 1
        tmp[M10] = 0
        tmp[M20] = 0
        tmp[M01] = 0
        tmp[M11] = 1
        tmp[M21] = 0
        tmp[M02] = x
        tmp[M12] = y
        tmp[M22] = 1
        mul(`val`, tmp)
        return this
    }

    /**
     * Postmultiplies this matrix by a translation matrix. Postmultiplication is also used by OpenGL ES' 1.x
     * glTranslate/glRotate/glScale.
     *
     * @param translation The translation vector.
     * @return This matrix for the purpose of chaining.
     */
    fun translate(translation: Vector2): Matrix3 {
        val `val` = values
        tmp[M00] = 1
        tmp[M10] = 0
        tmp[M20] = 0
        tmp[M01] = 0
        tmp[M11] = 1
        tmp[M21] = 0
        tmp[M02] = translation.x
        tmp[M12] = translation.y
        tmp[M22] = 1
        mul(`val`, tmp)
        return this
    }

    /**
     * Postmultiplies this matrix with a (counter-clockwise) rotation matrix. Postmultiplication is also used by OpenGL ES' 1.x
     * glTranslate/glRotate/glScale.
     *
     * @param degrees The angle in degrees
     * @return This matrix for the purpose of chaining.
     */
    fun rotate(degrees: Float): Matrix3 {
        return rotateRad(MathUtils.degreesToRadians * degrees)
    }

    /**
     * Postmultiplies this matrix with a (counter-clockwise) rotation matrix. Postmultiplication is also used by OpenGL ES' 1.x
     * glTranslate/glRotate/glScale.
     *
     * @param radians The angle in radians
     * @return This matrix for the purpose of chaining.
     */
    fun rotateRad(radians: Float): Matrix3 {
        if (radians == 0f) return this
        val cos = java.lang.Math.cos(radians.toDouble()) as Float
        val sin = java.lang.Math.sin(radians.toDouble()) as Float
        val tmp = tmp
        tmp[M00] = cos
        tmp[M10] = sin
        tmp[M20] = 0
        tmp[M01] = -sin
        tmp[M11] = cos
        tmp[M21] = 0
        tmp[M02] = 0
        tmp[M12] = 0
        tmp[M22] = 1
        mul(values, tmp)
        return this
    }

    /**
     * Postmultiplies this matrix with a scale matrix. Postmultiplication is also used by OpenGL ES' 1.x
     * glTranslate/glRotate/glScale.
     *
     * @param scaleX The scale in the x-axis.
     * @param scaleY The scale in the y-axis.
     * @return This matrix for the purpose of chaining.
     */
    fun scale(scaleX: Float, scaleY: Float): Matrix3 {
        val tmp = tmp
        tmp[M00] = scaleX
        tmp[M10] = 0
        tmp[M20] = 0
        tmp[M01] = 0
        tmp[M11] = scaleY
        tmp[M21] = 0
        tmp[M02] = 0
        tmp[M12] = 0
        tmp[M22] = 1
        mul(values, tmp)
        return this
    }

    /**
     * Postmultiplies this matrix with a scale matrix. Postmultiplication is also used by OpenGL ES' 1.x
     * glTranslate/glRotate/glScale.
     *
     * @param scale The vector to scale the matrix by.
     * @return This matrix for the purpose of chaining.
     */
    fun scale(scale: Vector2): Matrix3 {
        val tmp = tmp
        tmp[M00] = scale.x
        tmp[M10] = 0
        tmp[M20] = 0
        tmp[M01] = 0
        tmp[M11] = scale.y
        tmp[M21] = 0
        tmp[M02] = 0
        tmp[M12] = 0
        tmp[M22] = 1
        mul(values, tmp)
        return this
    }

    fun getTranslation(position: Vector2): Vector2 {
        position.x = values[M02]
        position.y = values[M12]
        return position
    }

    fun getScale(scale: Vector2): Vector2 {
        val `val` = values
        scale.x = java.lang.Math.sqrt(`val`[M00] * `val`[M00] + `val`[M01] * `val`[M01].toDouble())
        scale.y = java.lang.Math.sqrt(`val`[M10] * `val`[M10] + `val`[M11] * `val`[M11].toDouble())
        return scale
    }

    val rotation: Float
        get() = MathUtils.radiansToDegrees * java.lang.Math.atan2(values[M10], values[M00]) as Float

    val rotationRad: Float
        get() = java.lang.Math.atan2(values[M10], values[M00])

    /**
     * Scale the matrix in the both the x and y components by the scalar value.
     *
     * @param scale The single value that will be used to scale both the x and y components.
     * @return This matrix for the purpose of chaining methods together.
     */
    fun scl(scale: Float): Matrix3 {
        values[M00] *= scale
        values[M11] *= scale
        return this
    }

    /**
     * Scale this matrix using the x and y components of the vector but leave the rest of the matrix alone.
     *
     * @param scale The [Vector3] to use to scale this matrix.
     * @return This matrix for the purpose of chaining methods together.
     */
    fun scl(scale: Vector2): Matrix3 {
        values[M00] *= scale.x
        values[M11] *= scale.y
        return this
    }

    /**
     * Scale this matrix using the x and y components of the vector but leave the rest of the matrix alone.
     *
     * @param scale The [Vector3] to use to scale this matrix. The z component will be ignored.
     * @return This matrix for the purpose of chaining methods together.
     */
    fun scl(scale: Vector3): Matrix3 {
        values[M00] *= scale.x
        values[M11] *= scale.y
        return this
    }

    /**
     * Transposes the current matrix.
     *
     * @return This matrix for the purpose of chaining methods together.
     */
    fun transpose(): Matrix3 {
        // Where MXY you do not have to change MXX
        val `val` = values
        val v01 = `val`[M10]
        val v02 = `val`[M20]
        val v10 = `val`[M01]
        val v12 = `val`[M21]
        val v20 = `val`[M02]
        val v21 = `val`[M12]
        `val`[M01] = v01
        `val`[M02] = v02
        `val`[M10] = v10
        `val`[M12] = v12
        `val`[M20] = v20
        `val`[M21] = v21
        return this
    }

    companion object {
        private const val serialVersionUID = 7907569533774959788L
        const val M00 = 0
        const val M01 = 3
        const val M02 = 6
        const val M10 = 1
        const val M11 = 4
        const val M12 = 7
        const val M20 = 2
        const val M21 = 5
        const val M22 = 8

        /**
         * Multiplies matrix a with matrix b in the following manner:
         *
         * <pre>
         * mul(A, B) => A := AB
        </pre> *
         *
         * @param mata The float array representing the first matrix. Must have at least 9 elements.
         * @param matb The float array representing the second matrix. Must have at least 9 elements.
         */
        private fun mul(mata: FloatArray, matb: FloatArray) {
            val v00 = mata[M00] * matb[M00] + mata[M01] * matb[M10] + mata[M02] * matb[M20]
            val v01 = mata[M00] * matb[M01] + mata[M01] * matb[M11] + mata[M02] * matb[M21]
            val v02 = mata[M00] * matb[M02] + mata[M01] * matb[M12] + mata[M02] * matb[M22]
            val v10 = mata[M10] * matb[M00] + mata[M11] * matb[M10] + mata[M12] * matb[M20]
            val v11 = mata[M10] * matb[M01] + mata[M11] * matb[M11] + mata[M12] * matb[M21]
            val v12 = mata[M10] * matb[M02] + mata[M11] * matb[M12] + mata[M12] * matb[M22]
            val v20 = mata[M20] * matb[M00] + mata[M21] * matb[M10] + mata[M22] * matb[M20]
            val v21 = mata[M20] * matb[M01] + mata[M21] * matb[M11] + mata[M22] * matb[M21]
            val v22 = mata[M20] * matb[M02] + mata[M21] * matb[M12] + mata[M22] * matb[M22]
            mata[M00] = v00
            mata[M10] = v10
            mata[M20] = v20
            mata[M01] = v01
            mata[M11] = v11
            mata[M21] = v21
            mata[M02] = v02
            mata[M12] = v12
            mata[M22] = v22
        }
    }
}
