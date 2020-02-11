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
 * Implementation of the Bezier curve.
 *
 * @author Xoppa
 */
class Bezier<T : Vector<T>?> : Path<T> {

    var points = Array<T>()
    private var tmp: T? = null
    private var tmp2: T? = null
    private var tmp3: T? = null

    constructor() {}
    constructor(vararg points: T) {
        set(*points)
    }

    constructor(points: Array<T>?, offset: Int, length: Int) {
        set(points, offset, length)
    }

    constructor(points: Array<T>?, offset: Int, length: Int) {
        set(points, offset, length)
    }

    fun set(vararg points: T): Bezier<*> {
        return set(points, 0, points.size)
    }

    operator fun set(points: Array<T>, offset: Int, length: Int): Bezier<*> {
        if (length < 2 || length > 4) throw GdxRuntimeException("Only first, second and third degree Bezier curves are supported.")
        if (tmp == null) tmp = points[0]!!.cpy()
        if (tmp2 == null) tmp2 = points[0]!!.cpy()
        if (tmp3 == null) tmp3 = points[0]!!.cpy()
        this.points.clear()
        this.points.addAll(points, offset, length)
        return this
    }

    operator fun set(points: Array<T>, offset: Int, length: Int): Bezier<*> {
        if (length < 2 || length > 4) throw GdxRuntimeException("Only first, second and third degree Bezier curves are supported.")
        if (tmp == null) tmp = points[0]!!.cpy()
        if (tmp2 == null) tmp2 = points[0]!!.cpy()
        if (tmp3 == null) tmp3 = points[0]!!.cpy()
        this.points.clear()
        this.points.addAll(points, offset, length)
        return this
    }

    fun valueAt(out: T?, t: Float): T? {
        val n = points.size
        if (n == 2) linear<T?>(out, t, points[0], points[1], tmp) else if (n == 3) quadratic<T?>(out, t, points[0], points[1], points[2], tmp) else if (n == 4) cubic<T?>(out, t, points[0], points[1], points[2], points[3], tmp)
        return out
    }

    override fun derivativeAt(out: T, t: Float): T {
        val n = points.size
        if (n == 2) linear_derivative<T?>(out, t, points[0], points[1], tmp) else if (n == 3) quadratic_derivative<T?>(out, t, points[0], points[1], points[2], tmp) else if (n == 4) cubic_derivative<T?>(out, t, points[0], points[1], points[2], points[3], tmp)
        return out
    }

    override fun approximate(v: T): Float {
        // TODO: make a real approximate method
        val p1 = points[0]
        val p2 = points[points.size - 1]
        val l1Sqr = p1!!.dst2(p2)
        val l2Sqr = v!!.dst2(p2)
        val l3Sqr = v.dst2(p1)
        val l1 = java.lang.Math.sqrt(l1Sqr.toDouble()) as Float
        val s = (l2Sqr + l1Sqr - l3Sqr) / (2 * l1)
        return MathUtils.clamp((l1 - s) / l1, 0f, 1f)
    }

    override fun locate(v: T): Float {
        // TODO implement a precise method
        return approximate(v)
    }

    override fun approxLength(samples: Int): Float {
        var tempLength = 0f
        for (i in 0 until samples) {
            tmp2!!.set(tmp3)
            valueAt(tmp3, i / (samples.toFloat() - 1))
            if (i > 0) tempLength += tmp2!!.dst(tmp3)
        }
        return tempLength
    }

    companion object {
        // TODO implement Serializable
        /**
         * Simple linear interpolation
         *
         * @param out The [Vector] to set to the result.
         * @param t   The location (ranging 0..1) on the line.
         * @param p0  The start point.
         * @param p1  The end point.
         * @param tmp A temporary vector to be used by the calculation.
         * @return The value specified by out for chaining
         */
        fun <T : Vector<T>?> linear(out: T, t: Float, p0: T, p1: T, tmp: T): T {
            // B1(t) = p0 + (p1-p0)*t
            return out!!.set(p0)!!.scl(1f - t)!!.add(tmp!!.set(p1)!!.scl(t)) // Could just use lerp...
        }

        /**
         * Simple linear interpolation derivative
         *
         * @param out The [Vector] to set to the result.
         * @param t   The location (ranging 0..1) on the line.
         * @param p0  The start point.
         * @param p1  The end point.
         * @param tmp A temporary vector to be used by the calculation.
         * @return The value specified by out for chaining
         */
        fun <T : Vector<T>?> linear_derivative(out: T, t: Float, p0: T, p1: T, tmp: T): T {
            // B1'(t) = p1-p0
            return out!!.set(p1)!!.sub(p0)
        }

        /**
         * Quadratic Bezier curve
         *
         * @param out The [Vector] to set to the result.
         * @param t   The location (ranging 0..1) on the curve.
         * @param p0  The first bezier point.
         * @param p1  The second bezier point.
         * @param p2  The third bezier point.
         * @param tmp A temporary vector to be used by the calculation.
         * @return The value specified by out for chaining
         */
        fun <T : Vector<T>?> quadratic(out: T, t: Float, p0: T, p1: T, p2: T, tmp: T): T {
            // B2(t) = (1 - t) * (1 - t) * p0 + 2 * (1-t) * t * p1 + t*t*p2
            val dt = 1f - t
            return out!!.set(p0)!!.scl(dt * dt)!!.add(tmp!!.set(p1)!!.scl(2 * dt * t))!!.add(tmp.set(p2)!!.scl(t * t))
        }

        /**
         * Quadratic Bezier curve derivative
         *
         * @param out The [Vector] to set to the result.
         * @param t   The location (ranging 0..1) on the curve.
         * @param p0  The first bezier point.
         * @param p1  The second bezier point.
         * @param p2  The third bezier point.
         * @param tmp A temporary vector to be used by the calculation.
         * @return The value specified by out for chaining
         */
        fun <T : Vector<T>?> quadratic_derivative(out: T, t: Float, p0: T, p1: T, p2: T,
                                                  tmp: T): T {
            // B2'(t) = 2 * (1 - t) * (p1 - p0) + 2 * t * (p2 - p1)
            val dt = 1f - t
            return out!!.set(p1)!!.sub(p0)!!.scl(2f)!!.scl(1 - t)!!.add(tmp!!.set(p2)!!.sub(p1)!!.scl(t)!!.scl(2f))
        }

        /**
         * Cubic Bezier curve
         *
         * @param out The [Vector] to set to the result.
         * @param t   The location (ranging 0..1) on the curve.
         * @param p0  The first bezier point.
         * @param p1  The second bezier point.
         * @param p2  The third bezier point.
         * @param p3  The fourth bezier point.
         * @param tmp A temporary vector to be used by the calculation.
         * @return The value specified by out for chaining
         */
        fun <T : Vector<T>?> cubic(out: T, t: Float, p0: T, p1: T, p2: T, p3: T,
                                   tmp: T): T {
            // B3(t) = (1-t) * (1-t) * (1-t) * p0 + 3 * (1-t) * (1-t) * t * p1 + 3 * (1-t) * t * t * p2 + t * t * t * p3
            val dt = 1f - t
            val dt2 = dt * dt
            val t2 = t * t
            return out!!.set(p0)!!.scl(dt2 * dt)!!.add(tmp!!.set(p1)!!.scl(3 * dt2 * t))!!.add(tmp.set(p2)!!.scl(3 * dt * t2))
                .add(tmp.set(p3)!!.scl(t2 * t))
        }

        /**
         * Cubic Bezier curve derivative
         *
         * @param out The [Vector] to set to the result.
         * @param t   The location (ranging 0..1) on the curve.
         * @param p0  The first bezier point.
         * @param p1  The second bezier point.
         * @param p2  The third bezier point.
         * @param p3  The fourth bezier point.
         * @param tmp A temporary vector to be used by the calculation.
         * @return The value specified by out for chaining
         */
        fun <T : Vector<T>?> cubic_derivative(out: T, t: Float, p0: T, p1: T, p2: T,
                                              p3: T, tmp: T): T {
            // B3'(t) = 3 * (1-t) * (1-t) * (p1 - p0) + 6 * (1 - t) * t * (p2 - p1) + 3 * t * t * (p3 - p2)
            val dt = 1f - t
            val dt2 = dt * dt
            val t2 = t * t
            return out!!.set(p1)!!.sub(p0)!!.scl(dt2 * 3)!!.add(tmp!!.set(p2)!!.sub(p1)!!.scl(dt * t * 6))!!.add(tmp.set(p3)!!.sub(p2)!!.scl(t2 * 3))
        }
    }
}
