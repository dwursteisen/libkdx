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
 * @author Xoppa
 */
class BSpline<T : Vector<T>?> : Path<T> {

    var controlPoints: Array<T>
    var knots: Array<T>? = null
    var degree = 0
    var continuous = false
    var spanCount = 0
    private var tmp: T? = null
    private var tmp2: T? = null
    private var tmp3: T? = null

    constructor() {}
    constructor(controlPoints: Array<T?>, degree: Int, continuous: Boolean) {
        set(controlPoints, degree, continuous)
    }

    operator fun set(controlPoints: Array<T?>, degree: Int, continuous: Boolean): BSpline<*> {
        if (tmp == null) tmp = controlPoints[0]!!.cpy()
        if (tmp2 == null) tmp2 = controlPoints[0]!!.cpy()
        if (tmp3 == null) tmp3 = controlPoints[0]!!.cpy()
        this.controlPoints = controlPoints
        this.degree = degree
        this.continuous = continuous
        spanCount = if (continuous) controlPoints.size else controlPoints.size - degree
        if (knots == null) knots = Array(spanCount) else {
            knots.clear()
            knots.ensureCapacity(spanCount)
        }
        for (i in 0 until spanCount) knots.add(calculate<T?>(controlPoints[0]!!.cpy(), if (continuous) i else (i + 0.5f * degree).toInt(), 0f, controlPoints, degree,
            continuous, tmp))
        return this
    }

    fun valueAt(out: T?, t: Float): T {
        val n = spanCount
        var u = t * n
        val i = if (t >= 1f) n - 1 else u.toInt()
        u -= i.toFloat()
        return valueAt(out, i, u)
    }

    /**
     * @return The value of the spline at position u of the specified span
     */
    fun valueAt(out: T?, span: Int, u: Float): T {
        return calculate(out, if (continuous) span else span + (degree * 0.5f).toInt(), u, controlPoints, degree, continuous, tmp)
    }

    override fun derivativeAt(out: T, t: Float): T {
        val n = spanCount
        var u = t * n
        val i = if (t >= 1f) n - 1 else u.toInt()
        u -= i.toFloat()
        return derivativeAt(out, i, u)
    }

    /**
     * @return The derivative of the spline at position u of the specified span
     */
    fun derivativeAt(out: T, span: Int, u: Float): T {
        return derivative(out, if (continuous) span else span + (degree * 0.5f).toInt(), u, controlPoints, degree, continuous, tmp)
    }
    /**
     * @return The span closest to the specified value, restricting to the specified spans.
     */
    /**
     * @return The span closest to the specified value
     */
    @JvmOverloads
    fun nearest(`in`: T, start: Int = 0, count: Int = spanCount): Int {
        var start = start
        while (start < 0) start += spanCount
        var result = start % spanCount
        var dst = `in`!!.dst2(knots!![result])
        for (i in 1 until count) {
            val idx = (start + i) % spanCount
            val d = `in`.dst2(knots!![idx])
            if (d < dst) {
                dst = d
                result = idx
            }
        }
        return result
    }

    override fun approximate(v: T): Float {
        return approximate(v, nearest(v))
    }

    fun approximate(`in`: T, start: Int, count: Int): Float {
        return approximate(`in`, nearest(`in`, start, count))
    }

    fun approximate(`in`: T, near: Int): Float {
        var n = near
        val nearest = knots!![n]
        val previous = knots!![if (n > 0) n - 1 else spanCount - 1]
        val next = knots!![(n + 1) % spanCount]
        val dstPrev2 = `in`!!.dst2(previous)
        val dstNext2 = `in`.dst2(next)
        val P1: T
        val P2: T
        val P3: T
        if (dstNext2 < dstPrev2) {
            P1 = nearest
            P2 = next
            P3 = `in`
        } else {
            P1 = previous
            P2 = nearest
            P3 = `in`
            n = if (n > 0) n - 1 else spanCount - 1
        }
        val L1Sqr = P1!!.dst2(P2)
        val L2Sqr = P3!!.dst2(P2)
        val L3Sqr = P3.dst2(P1)
        val L1 = java.lang.Math.sqrt(L1Sqr.toDouble()) as Float
        val s = (L2Sqr + L1Sqr - L3Sqr) / (2 * L1)
        val u: Float = MathUtils.clamp((L1 - s) / L1, 0f, 1f)
        return (n + u) / spanCount
    }

    override fun locate(v: T): Float {
        // TODO Add a precise method
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
        private const val d6 = 1f / 6f

        /**
         * Calculates the cubic b-spline value for the given position (t).
         *
         * @param out        The Vector to set to the result.
         * @param t          The position (0<=t<=1) on the spline
         * @param points     The control points
         * @param continuous If true the b-spline restarts at 0 when reaching 1
         * @param tmp        A temporary vector used for the calculation
         * @return The value of out
         */
        fun <T : Vector<T>?> cubic(out: T, t: Float, points: Array<T>, continuous: Boolean,
                                   tmp: T): T {
            val n = if (continuous) points.size else points.size - 3
            var u = t * n
            val i = if (t >= 1f) n - 1 else u.toInt()
            u -= i.toFloat()
            return cubic(out, i, u, points, continuous, tmp)
        }

        /**
         * Calculates the cubic b-spline derivative for the given position (t).
         *
         * @param out        The Vector to set to the result.
         * @param t          The position (0<=t<=1) on the spline
         * @param points     The control points
         * @param continuous If true the b-spline restarts at 0 when reaching 1
         * @param tmp        A temporary vector used for the calculation
         * @return The value of out
         */
        fun <T : Vector<T>?> cubic_derivative(out: T, t: Float, points: Array<T>,
                                              continuous: Boolean, tmp: T): T {
            val n = if (continuous) points.size else points.size - 3
            var u = t * n
            val i = if (t >= 1f) n - 1 else u.toInt()
            u -= i.toFloat()
            return cubic(out, i, u, points, continuous, tmp)
        }

        /**
         * Calculates the cubic b-spline value for the given span (i) at the given position (u).
         *
         * @param out        The Vector to set to the result.
         * @param i          The span (0<=i<spanCount></spanCount>) spanCount = continuous ? points.length : points.length - 3 (cubic degree)
         * @param u          The position (0<=u<=1) on the span
         * @param points     The control points
         * @param continuous If true the b-spline restarts at 0 when reaching 1
         * @param tmp        A temporary vector used for the calculation
         * @return The value of out
         */
        fun <T : Vector<T>?> cubic(out: T?, i: Int, u: Float, points: Array<T>,
                                   continuous: Boolean, tmp: T?): T? {
            val n = points.size
            val dt = 1f - u
            val t2 = u * u
            val t3 = t2 * u
            out!!.set(points[i])!!.scl((3f * t3 - 6f * t2 + 4f) * d6)
            if (continuous || i > 0) out.add(tmp!!.set(points[(n + i - 1) % n])!!.scl(dt * dt * dt * d6))
            if (continuous || i < n - 1) out.add(tmp!!.set(points[(i + 1) % n])!!.scl((-3f * t3 + 3f * t2 + 3f * u + 1f) * d6))
            if (continuous || i < n - 2) out.add(tmp!!.set(points[(i + 2) % n])!!.scl(t3 * d6))
            return out
        }

        /**
         * Calculates the cubic b-spline derivative for the given span (i) at the given position (u).
         *
         * @param out        The Vector to set to the result.
         * @param i          The span (0<=i<spanCount></spanCount>) spanCount = continuous ? points.length : points.length - 3 (cubic degree)
         * @param u          The position (0<=u<=1) on the span
         * @param points     The control points
         * @param continuous If true the b-spline restarts at 0 when reaching 1
         * @param tmp        A temporary vector used for the calculation
         * @return The value of out
         */
        fun <T : Vector<T>?> cubic_derivative(out: T, i: Int, u: Float, points: Array<T>,
                                              continuous: Boolean, tmp: T?): T {
            val n = points.size
            val dt = 1f - u
            val t2 = u * u
            val t3 = t2 * u
            out!!.set(points[i])!!.scl(1.5f * t2 - 2 * u)
            if (continuous || i > 0) out.add(tmp!!.set(points[(n + i - 1) % n])!!.scl(-0.5f * dt * dt))
            if (continuous || i < n - 1) out.add(tmp!!.set(points[(i + 1) % n])!!.scl(-1.5f * t2 + u + 0.5f))
            if (continuous || i < n - 2) out.add(tmp!!.set(points[(i + 2) % n])!!.scl(0.5f * t2))
            return out
        }

        /**
         * Calculates the n-degree b-spline value for the given position (t).
         *
         * @param out        The Vector to set to the result.
         * @param t          The position (0<=t<=1) on the spline
         * @param points     The control points
         * @param degree     The degree of the b-spline
         * @param continuous If true the b-spline restarts at 0 when reaching 1
         * @param tmp        A temporary vector used for the calculation
         * @return The value of out
         */
        fun <T : Vector<T>?> calculate(out: T, t: Float, points: Array<T>, degree: Int,
                                       continuous: Boolean, tmp: T): T {
            val n = if (continuous) points.size else points.size - degree
            var u = t * n
            val i = if (t >= 1f) n - 1 else u.toInt()
            u -= i.toFloat()
            return calculate(out, i, u, points, degree, continuous, tmp)
        }

        /**
         * Calculates the n-degree b-spline derivative for the given position (t).
         *
         * @param out        The Vector to set to the result.
         * @param t          The position (0<=t<=1) on the spline
         * @param points     The control points
         * @param degree     The degree of the b-spline
         * @param continuous If true the b-spline restarts at 0 when reaching 1
         * @param tmp        A temporary vector used for the calculation
         * @return The value of out
         */
        fun <T : Vector<T>?> derivative(out: T, t: Float, points: Array<T>, degree: Int,
                                        continuous: Boolean, tmp: T): T {
            val n = if (continuous) points.size else points.size - degree
            var u = t * n
            val i = if (t >= 1f) n - 1 else u.toInt()
            u -= i.toFloat()
            return derivative(out, i, u, points, degree, continuous, tmp)
        }

        /**
         * Calculates the n-degree b-spline value for the given span (i) at the given position (u).
         *
         * @param out        The Vector to set to the result.
         * @param i          The span (0<=i<spanCount></spanCount>) spanCount = continuous ? points.length : points.length - degree
         * @param u          The position (0<=u<=1) on the span
         * @param points     The control points
         * @param degree     The degree of the b-spline
         * @param continuous If true the b-spline restarts at 0 when reaching 1
         * @param tmp        A temporary vector used for the calculation
         * @return The value of out
         */
        fun <T : Vector<T>?> calculate(out: T?, i: Int, u: Float, points: Array<T>, degree: Int,
                                       continuous: Boolean, tmp: T?): T? {
            when (degree) {
                3 -> return cubic(out, i, u, points, continuous, tmp)
            }
            return out
        }

        /**
         * Calculates the n-degree b-spline derivative for the given span (i) at the given position (u).
         *
         * @param out        The Vector to set to the result.
         * @param i          The span (0<=i<spanCount></spanCount>) spanCount = continuous ? points.length : points.length - degree
         * @param u          The position (0<=u<=1) on the span
         * @param points     The control points
         * @param degree     The degree of the b-spline
         * @param continuous If true the b-spline restarts at 0 when reaching 1
         * @param tmp        A temporary vector used for the calculation
         * @return The value of out
         */
        fun <T : Vector<T>?> derivative(out: T, i: Int, u: Float, points: Array<T>, degree: Int,
                                        continuous: Boolean, tmp: T?): T {
            when (degree) {
                3 -> return cubic_derivative(out, i, u, points, continuous, tmp)
            }
            return out
        }
    }
}
