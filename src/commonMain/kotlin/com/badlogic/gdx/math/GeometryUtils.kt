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
 * @author Nathan Sweet
 */
object GeometryUtils {

    private val tmp1 = Vector2()
    private val tmp2 = Vector2()
    private val tmp3 = Vector2()

    /**
     * Computes the barycentric coordinates v,w for the specified point in the triangle.
     *
     *
     * If barycentric.x >= 0 && barycentric.y >= 0 && barycentric.x + barycentric.y <= 1 then the point is inside the triangle.
     *
     *
     * If vertices a,b,c have values aa,bb,cc then to get an interpolated value at point p:
     *
     * <pre>
     * GeometryUtils.barycentric(p, a, b, c, barycentric);
     * float u = 1.f - barycentric.x - barycentric.y;
     * float x = u * aa.x + barycentric.x * bb.x + barycentric.y * cc.x;
     * float y = u * aa.y + barycentric.x * bb.y + barycentric.y * cc.y;
    </pre> *
     *
     * @return barycentricOut
     */
    fun toBarycoord(p: Vector2?, a: Vector2, b: Vector2?, c: Vector2?, barycentricOut: Vector2): Vector2 {
        val v0 = tmp1.set(b)!!.sub(a)
        val v1 = tmp2.set(c)!!.sub(a)
        val v2 = tmp3.set(p)!!.sub(a)
        val d00 = v0.dot(v0)
        val d01 = v0.dot(v1)
        val d11 = v1.dot(v1)
        val d20 = v2.dot(v0)
        val d21 = v2.dot(v1)
        val denom = d00 * d11 - d01 * d01
        barycentricOut.x = (d11 * d20 - d01 * d21) / denom
        barycentricOut.y = (d00 * d21 - d01 * d20) / denom
        return barycentricOut
    }

    /**
     * Returns true if the barycentric coordinates are inside the triangle.
     */
    fun barycoordInsideTriangle(barycentric: Vector2): Boolean {
        return barycentric.x >= 0 && barycentric.y >= 0 && barycentric.x + barycentric.y <= 1
    }

    /**
     * Returns interpolated values given the barycentric coordinates of a point in a triangle and the values at each vertex.
     *
     * @return interpolatedOut
     */
    fun fromBarycoord(barycentric: Vector2, a: Vector2, b: Vector2, c: Vector2, interpolatedOut: Vector2): Vector2 {
        val u = 1 - barycentric.x - barycentric.y
        interpolatedOut.x = u * a.x + barycentric.x * b.x + barycentric.y * c.x
        interpolatedOut.y = u * a.y + barycentric.x * b.y + barycentric.y * c.y
        return interpolatedOut
    }

    /**
     * Returns an interpolated value given the barycentric coordinates of a point in a triangle and the values at each vertex.
     *
     * @return interpolatedOut
     */
    fun fromBarycoord(barycentric: Vector2, a: Float, b: Float, c: Float): Float {
        val u = 1 - barycentric.x - barycentric.y
        return u * a + barycentric.x * b + barycentric.y * c
    }

    /**
     * Returns the lowest positive root of the quadric equation given by a* x * x + b * x + c = 0. If no solution is given
     * Float.Nan is returned.
     *
     * @param a the first coefficient of the quadric equation
     * @param b the second coefficient of the quadric equation
     * @param c the third coefficient of the quadric equation
     * @return the lowest positive root or Float.Nan
     */
    fun lowestPositiveRoot(a: Float, b: Float, c: Float): Float {
        val det = b * b - 4 * a * c
        if (det < 0) return Float.NaN
        val sqrtD = java.lang.Math.sqrt(det.toDouble()) as Float
        val invA = 1 / (2 * a)
        var r1 = (-b - sqrtD) * invA
        var r2 = (-b + sqrtD) * invA
        if (r1 > r2) {
            val tmp = r2
            r2 = r1
            r1 = tmp
        }
        if (r1 > 0) return r1
        return if (r2 > 0) r2 else Float.NaN
    }

    fun colinear(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float): Boolean {
        val dx21 = x2 - x1
        val dy21 = y2 - y1
        val dx32 = x3 - x2
        val dy32 = y3 - y2
        val det = dx32 * dy21 - dx21 * dy32
        return java.lang.Math.abs(det) < MathUtils.FLOAT_ROUNDING_ERROR
    }

    fun triangleCentroid(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float, centroid: Vector2): Vector2 {
        centroid.x = (x1 + x2 + x3) / 3
        centroid.y = (y1 + y2 + y3) / 3
        return centroid
    }

    /**
     * Returns the circumcenter of the triangle. The input points must not be colinear.
     */
    fun triangleCircumcenter(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float, circumcenter: Vector2): Vector2 {
        val dx21 = x2 - x1
        val dy21 = y2 - y1
        val dx32 = x3 - x2
        val dy32 = y3 - y2
        val dx13 = x1 - x3
        val dy13 = y1 - y3
        var det = dx32 * dy21 - dx21 * dy32
        if (java.lang.Math.abs(det) < MathUtils.FLOAT_ROUNDING_ERROR) throw java.lang.IllegalArgumentException("Triangle points must not be colinear.")
        det *= 2f
        val sqr1 = x1 * x1 + y1 * y1
        val sqr2 = x2 * x2 + y2 * y2
        val sqr3 = x3 * x3 + y3 * y3
        circumcenter[(sqr1 * dy32 + sqr2 * dy13 + sqr3 * dy21) / det] = -(sqr1 * dx32 + sqr2 * dx13 + sqr3 * dx21) / det
        return circumcenter
    }

    fun triangleCircumradius(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float): Float {
        val m1: Float
        val m2: Float
        val mx1: Float
        val mx2: Float
        val my1: Float
        val my2: Float
        val x: Float
        val y: Float
        if (java.lang.Math.abs(y2 - y1) < MathUtils.FLOAT_ROUNDING_ERROR) {
            m2 = -(x3 - x2) / (y3 - y2)
            mx2 = (x2 + x3) / 2
            my2 = (y2 + y3) / 2
            x = (x2 + x1) / 2
            y = m2 * (x - mx2) + my2
        } else if (java.lang.Math.abs(y3 - y2) < MathUtils.FLOAT_ROUNDING_ERROR) {
            m1 = -(x2 - x1) / (y2 - y1)
            mx1 = (x1 + x2) / 2
            my1 = (y1 + y2) / 2
            x = (x3 + x2) / 2
            y = m1 * (x - mx1) + my1
        } else {
            m1 = -(x2 - x1) / (y2 - y1)
            m2 = -(x3 - x2) / (y3 - y2)
            mx1 = (x1 + x2) / 2
            mx2 = (x2 + x3) / 2
            my1 = (y1 + y2) / 2
            my2 = (y2 + y3) / 2
            x = (m1 * mx1 - m2 * mx2 + my2 - my1) / (m1 - m2)
            y = m1 * (x - mx1) + my1
        }
        val dx = x1 - x
        val dy = y1 - y
        return java.lang.Math.sqrt(dx * dx + dy * dy.toDouble())
    }

    /**
     * Ratio of circumradius to shortest edge as a measure of triangle quality.
     *
     *
     * Gary L. Miller, Dafna Talmor, Shang-Hua Teng, and Noel Walkington. A Delaunay Based Numerical Method for Three Dimensions:
     * Generation, Formulation, and Partition.
     */
    fun triangleQuality(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float): Float {
        val length1 = java.lang.Math.sqrt(x1 * x1 + y1 * y1.toDouble()) as Float
        val length2 = java.lang.Math.sqrt(x2 * x2 + y2 * y2.toDouble()) as Float
        val length3 = java.lang.Math.sqrt(x3 * x3 + y3 * y3.toDouble()) as Float
        return java.lang.Math.min(length1, java.lang.Math.min(length2, length3)) / triangleCircumradius(x1, y1, x2, y2, x3, y3)
    }

    fun triangleArea(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float): Float {
        return java.lang.Math.abs((x1 - x3) * (y2 - y1) - (x1 - x2) * (y3 - y1)) * 0.5f
    }

    fun quadrilateralCentroid(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float, x4: Float, y4: Float,
                              centroid: Vector2): Vector2 {
        val avgX1 = (x1 + x2 + x3) / 3
        val avgY1 = (y1 + y2 + y3) / 3
        val avgX2 = (x1 + x4 + x3) / 3
        val avgY2 = (y1 + y4 + y3) / 3
        centroid.x = avgX1 - (avgX1 - avgX2) / 2
        centroid.y = avgY1 - (avgY1 - avgY2) / 2
        return centroid
    }

    /**
     * Returns the centroid for the specified non-self-intersecting polygon.
     */
    fun polygonCentroid(polygon: FloatArray, offset: Int, count: Int, centroid: Vector2): Vector2 {
        if (count < 6) throw java.lang.IllegalArgumentException("A polygon must have 3 or more coordinate pairs.")
        var area = 0f
        var x = 0f
        var y = 0f
        val last = offset + count - 2
        var x1 = polygon[last]
        var y1 = polygon[last + 1]
        var i = offset
        while (i <= last) {
            val x2 = polygon[i]
            val y2 = polygon[i + 1]
            val a = x1 * y2 - x2 * y1
            area += a
            x += (x1 + x2) * a
            y += (y1 + y2) * a
            x1 = x2
            y1 = y2
            i += 2
        }
        if (area == 0f) {
            centroid.x = 0
            centroid.y = 0
        } else {
            area *= 0.5f
            centroid.x = x / (6 * area)
            centroid.y = y / (6 * area)
        }
        return centroid
    }

    /**
     * Computes the area for a convex polygon.
     */
    fun polygonArea(polygon: FloatArray?, offset: Int, count: Int): Float {
        var area = 0f
        val last = offset + count - 2
        var x1 = polygon!![last]
        var y1 = polygon[last + 1]
        var i = offset
        while (i <= last) {
            val x2 = polygon[i]
            val y2 = polygon[i + 1]
            area += x1 * y2 - x2 * y1
            x1 = x2
            y1 = y2
            i += 2
        }
        return area * 0.5f
    }

    @JvmOverloads
    fun ensureCCW(polygon: FloatArray, offset: Int = 0, count: Int = polygon.size) {
        if (!isClockwise(polygon, offset, count)) return
        val lastX = offset + count - 2
        var i = offset
        val n = offset + count / 2
        while (i < n) {
            val other = lastX - i
            val x = polygon[i]
            val y = polygon[i + 1]
            polygon[i] = polygon[other]
            polygon[i + 1] = polygon[other + 1]
            polygon[other] = x
            polygon[other + 1] = y
            i += 2
        }
    }

    fun isClockwise(polygon: FloatArray, offset: Int, count: Int): Boolean {
        if (count <= 2) return false
        var area = 0f
        val last = offset + count - 2
        var x1 = polygon[last]
        var y1 = polygon[last + 1]
        var i = offset
        while (i <= last) {
            val x2 = polygon[i]
            val y2 = polygon[i + 1]
            area += x1 * y2 - x2 * y1
            x1 = x2
            y1 = y2
            i += 2
        }
        return area < 0
    }
}
