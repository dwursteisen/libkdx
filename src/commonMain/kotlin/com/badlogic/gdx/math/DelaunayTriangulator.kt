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
 * Delaunay triangulation. Adapted from Paul Bourke's triangulate: http://paulbourke.net/papers/triangulate/
 *
 * @author Nathan Sweet
 */
class DelaunayTriangulator {

    private val quicksortStack: IntArray = IntArray()
    private var sortedPoints: FloatArray?
    private val triangles = ShortArray(false, 16)
    private val originalIndices = ShortArray(false, 0)
    private val edges: IntArray = IntArray()
    private val complete = BooleanArray(false, 16)
    private val superTriangle = FloatArray(6)
    private val centroid = Vector2()

    /**
     * @see .computeTriangles
     */
    fun computeTriangles(points: FloatArray, sorted: Boolean): ShortArray {
        return computeTriangles(points.items, 0, points.size, sorted)
    }

    /**
     * @see .computeTriangles
     */
    fun computeTriangles(polygon: FloatArray, sorted: Boolean): ShortArray {
        return computeTriangles(polygon, 0, polygon.size, sorted)
    }

    /**
     * Triangulates the given point cloud to a list of triangle indices that make up the Delaunay triangulation.
     *
     * @param points x,y pairs describing points. Duplicate points will result in undefined behavior.
     * @param sorted If false, the points will be sorted by the x coordinate, which is required by the triangulation algorithm. If
     * sorting is done the input array is not modified, the returned indices are for the input array, and count*2
     * additional working memory is needed.
     * @return triples of indices into the points that describe the triangles in clockwise order. Note the returned array is reused
     * for later calls to the same method.
     */
    fun computeTriangles(points: FloatArray, offset: Int, count: Int, sorted: Boolean): ShortArray {
        var points = points
        var offset = offset
        if (count > 32767) throw java.lang.IllegalArgumentException("count must be <= " + 32767)
        val triangles = triangles
        triangles.clear()
        if (count < 6) return triangles
        triangles.ensureCapacity(count)
        if (!sorted) {
            if (sortedPoints == null || sortedPoints!!.size < count) sortedPoints = FloatArray(count)
            java.lang.System.arraycopy(points, offset, sortedPoints, 0, count)
            points = sortedPoints
            offset = 0
            sort(points, count)
        }
        val end = offset + count

        // Determine bounds for super triangle.
        var xmin = points[0]
        var ymin = points[1]
        var xmax = xmin
        var ymax = ymin
        run {
            var i = offset + 2
            while (i < end) {
                var value = points[i]
                if (value < xmin) xmin = value
                if (value > xmax) xmax = value
                i++
                value = points[i]
                if (value < ymin) ymin = value
                if (value > ymax) ymax = value
                i++
            }
        }
        val dx = xmax - xmin
        val dy = ymax - ymin
        val dmax = (if (dx > dy) dx else dy) * 20f
        val xmid = (xmax + xmin) / 2f
        val ymid = (ymax + ymin) / 2f

        // Setup the super triangle, which contains all points.
        val superTriangle = superTriangle
        superTriangle[0] = xmid - dmax
        superTriangle[1] = ymid - dmax
        superTriangle[2] = xmid
        superTriangle[3] = ymid + dmax
        superTriangle[4] = xmid + dmax
        superTriangle[5] = ymid - dmax
        val edges = edges
        edges.ensureCapacity(count / 2)
        val complete = complete
        complete.clear()
        complete.ensureCapacity(count)

        // Add super triangle.
        triangles.add(end)
        triangles.add(end + 2)
        triangles.add(end + 4)
        complete.add(false)

        // Include each point one at a time into the existing mesh.
        var pointIndex = offset
        while (pointIndex < end) {
            val x = points[pointIndex]
            val y = points[pointIndex + 1]

            // If x,y lies inside the circumcircle of a triangle, the edges are stored and the triangle removed.
            val trianglesArray: ShortArray = triangles.items
            val completeArray: BooleanArray = complete.items
            var triangleIndex = triangles.size - 1
            while (triangleIndex >= 0) {
                val completeIndex = triangleIndex / 3
                if (completeArray[completeIndex]) {
                    triangleIndex -= 3
                    continue
                }
                val p1 = trianglesArray[triangleIndex - 2].toInt()
                val p2 = trianglesArray[triangleIndex - 1].toInt()
                val p3 = trianglesArray[triangleIndex].toInt()
                var x1: Float
                var y1: Float
                var x2: Float
                var y2: Float
                var x3: Float
                var y3: Float
                if (p1 >= end) {
                    val i = p1 - end
                    x1 = superTriangle[i]
                    y1 = superTriangle[i + 1]
                } else {
                    x1 = points[p1]
                    y1 = points[p1 + 1]
                }
                if (p2 >= end) {
                    val i = p2 - end
                    x2 = superTriangle[i]
                    y2 = superTriangle[i + 1]
                } else {
                    x2 = points[p2]
                    y2 = points[p2 + 1]
                }
                if (p3 >= end) {
                    val i = p3 - end
                    x3 = superTriangle[i]
                    y3 = superTriangle[i + 1]
                } else {
                    x3 = points[p3]
                    y3 = points[p3 + 1]
                }
                when (circumCircle(x, y, x1, y1, x2, y2, x3, y3)) {
                    COMPLETE -> completeArray[completeIndex] = true
                    INSIDE -> {
                        edges.add(p1, p2, p2, p3)
                        edges.add(p3, p1)
                        triangles.removeRange(triangleIndex - 2, triangleIndex)
                        complete.removeIndex(completeIndex)
                    }
                }
                triangleIndex -= 3
            }
            val edgesArray: IntArray = edges.items
            var i = 0
            val n = edges.size
            while (i < n) {

                // Skip multiple edges. If all triangles are anticlockwise then all interior edges are opposite pointing in direction.
                val p1 = edgesArray[i]
                if (p1 == -1) {
                    i += 2
                    continue
                }
                val p2 = edgesArray[i + 1]
                var skip = false
                var ii = i + 2
                while (ii < n) {
                    if (p1 == edgesArray[ii + 1] && p2 == edgesArray[ii]) {
                        skip = true
                        edgesArray[ii] = -1
                    }
                    ii += 2
                }
                if (skip) {
                    i += 2
                    continue
                }

                // Form new triangles for the current point. Edges are arranged in clockwise order.
                triangles.add(p1)
                triangles.add(edgesArray[i + 1])
                triangles.add(pointIndex)
                complete.add(false)
                i += 2
            }
            edges.clear()
            pointIndex += 2
        }

        // Remove triangles with super triangle vertices.
        val trianglesArray: ShortArray = triangles.items
        var i = triangles.size - 1
        while (i >= 0) {
            if (trianglesArray[i] >= end || trianglesArray[i - 1] >= end || trianglesArray[i - 2] >= end) {
                triangles.removeIndex(i)
                triangles.removeIndex(i - 1)
                triangles.removeIndex(i - 2)
            }
            i -= 3
        }

        // Convert sorted to unsorted indices.
        if (!sorted) {
            val originalIndicesArray: ShortArray = originalIndices.items
            var i = 0
            val n = triangles.size
            while (i < n) {
                trianglesArray[i] = (originalIndicesArray[trianglesArray[i] / 2] * 2).toShort()
                i++
            }
        }

        // Adjust triangles to start from zero and count by 1, not by vertex x,y coordinate pairs.
        if (offset == 0) {
            var i = 0
            val n = triangles.size
            while (i < n) {
                trianglesArray[i] = (trianglesArray[i] / 2).toShort()
                i++
            }
        } else {
            var i = 0
            val n = triangles.size
            while (i < n) {
                trianglesArray[i] = ((trianglesArray[i] - offset) / 2).toShort()
                i++
            }
        }
        return triangles
    }

    /**
     * Returns INSIDE if point xp,yp is inside the circumcircle made up of the points x1,y1, x2,y2, x3,y3. Returns COMPLETE if xp
     * is to the right of the entire circumcircle. Otherwise returns INCOMPLETE. Note: a point on the circumcircle edge is
     * considered inside.
     */
    private fun circumCircle(xp: Float, yp: Float, x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float): Int {
        val xc: Float
        val yc: Float
        val y1y2: Float = java.lang.Math.abs(y1 - y2)
        val y2y3: Float = java.lang.Math.abs(y2 - y3)
        if (y1y2 < EPSILON) {
            if (y2y3 < EPSILON) return INCOMPLETE
            val m2 = -(x3 - x2) / (y3 - y2)
            val mx2 = (x2 + x3) / 2f
            val my2 = (y2 + y3) / 2f
            xc = (x2 + x1) / 2f
            yc = m2 * (xc - mx2) + my2
        } else {
            val m1 = -(x2 - x1) / (y2 - y1)
            val mx1 = (x1 + x2) / 2f
            val my1 = (y1 + y2) / 2f
            if (y2y3 < EPSILON) {
                xc = (x3 + x2) / 2f
                yc = m1 * (xc - mx1) + my1
            } else {
                val m2 = -(x3 - x2) / (y3 - y2)
                val mx2 = (x2 + x3) / 2f
                val my2 = (y2 + y3) / 2f
                xc = (m1 * mx1 - m2 * mx2 + my2 - my1) / (m1 - m2)
                yc = m1 * (xc - mx1) + my1
            }
        }
        var dx = x2 - xc
        var dy = y2 - yc
        val rsqr = dx * dx + dy * dy
        dx = xp - xc
        dx *= dx
        dy = yp - yc
        if (dx + dy * dy - rsqr <= EPSILON) return INSIDE
        return if (xp > xc && dx > rsqr) COMPLETE else INCOMPLETE
    }

    /**
     * Sorts x,y pairs of values by the x value.
     *
     * @param count Number of indices, must be even.
     */
    private fun sort(values: FloatArray, count: Int) {
        val pointCount = count / 2
        originalIndices.clear()
        originalIndices.ensureCapacity(pointCount)
        val originalIndicesArray: ShortArray = originalIndices.items
        for (i in 0 until pointCount) originalIndicesArray[i] = i.toShort()
        var lower = 0
        var upper = count - 1
        val stack = quicksortStack
        stack.add(lower)
        stack.add(upper - 1)
        while (stack.size > 0) {
            upper = stack.pop()
            lower = stack.pop()
            if (upper <= lower) continue
            val i = quicksortPartition(values, lower, upper, originalIndicesArray)
            if (i - lower > upper - i) {
                stack.add(lower)
                stack.add(i - 2)
            }
            stack.add(i + 2)
            stack.add(upper)
            if (upper - i >= i - lower) {
                stack.add(lower)
                stack.add(i - 2)
            }
        }
    }

    private fun quicksortPartition(values: FloatArray, lower: Int, upper: Int, originalIndices: ShortArray): Int {
        val value = values[lower]
        var up = upper
        var down = lower + 2
        var tempValue: Float
        var tempIndex: Short
        while (down < up) {
            while (down < up && values[down] <= value) down = down + 2
            while (values[up] > value) up = up - 2
            if (down < up) {
                tempValue = values[down]
                values[down] = values[up]
                values[up] = tempValue
                tempValue = values[down + 1]
                values[down + 1] = values[up + 1]
                values[up + 1] = tempValue
                tempIndex = originalIndices[down / 2]
                originalIndices[down / 2] = originalIndices[up / 2]
                originalIndices[up / 2] = tempIndex
            }
        }
        values[lower] = values[up]
        values[up] = value
        tempValue = values[lower + 1]
        values[lower + 1] = values[up + 1]
        values[up + 1] = tempValue
        tempIndex = originalIndices[lower / 2]
        originalIndices[lower / 2] = originalIndices[up / 2]
        originalIndices[up / 2] = tempIndex
        return up
    }

    /**
     * Removes all triangles with a centroid outside the specified hull, which may be concave. Note some triangulations may have
     * triangles whose centroid is inside the hull but a portion is outside.
     */
    fun trim(triangles: ShortArray, points: FloatArray, hull: FloatArray, offset: Int, count: Int) {
        val trianglesArray: ShortArray = triangles.items
        var i = triangles.size - 1
        while (i >= 0) {
            val p1 = trianglesArray[i - 2] * 2
            val p2 = trianglesArray[i - 1] * 2
            val p3 = trianglesArray[i] * 2
            GeometryUtils.triangleCentroid(points[p1], points[p1 + 1], points[p2], points[p2 + 1], points[p3], points[p3 + 1],
                centroid)
            if (!Intersector.isPointInPolygon(hull, offset, count, centroid.x, centroid.y)) {
                triangles.removeIndex(i)
                triangles.removeIndex(i - 1)
                triangles.removeIndex(i - 2)
            }
            i -= 3
        }
    }

    companion object {
        private const val EPSILON = 0.000001f
        private const val INSIDE = 0
        private const val COMPLETE = 1
        private const val INCOMPLETE = 2
    }
}
