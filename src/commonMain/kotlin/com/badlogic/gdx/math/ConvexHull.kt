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
 * Computes the convex hull of a set of points using the monotone chain convex hull algorithm (aka Andrew's algorithm).
 *
 * @author Nathan Sweet
 */
class ConvexHull {

    private val quicksortStack: IntArray = IntArray()
    private var sortedPoints: FloatArray?
    private val hull: FloatArray = FloatArray()
    private val indices: IntArray = IntArray()
    private val originalIndices = ShortArray(false, 0)

    /**
     * @see .computePolygon
     */
    fun computePolygon(points: FloatArray, sorted: Boolean): FloatArray {
        return computePolygon(points.items, 0, points.size, sorted)
    }

    /**
     * @see .computePolygon
     */
    fun computePolygon(polygon: FloatArray, sorted: Boolean): FloatArray {
        return computePolygon(polygon, 0, polygon.size, sorted)
    }
    /** Returns a list of points on the convex hull in counter-clockwise order. Note: the last point in the returned list is the
     * same as the first one.  */
    /**
     * Returns the convex hull polygon for the given point cloud.
     *
     * @param points x,y pairs describing points. Duplicate points will result in undefined behavior.
     * @param sorted If false, the points will be sorted by the x coordinate then the y coordinate, which is required by the convex
     * hull algorithm. If sorting is done the input array is not modified and count additional working memory is needed.
     * @return pairs of coordinates that describe the convex hull polygon in counterclockwise order. Note the returned array is
     * reused for later calls to the same method.
     */
    fun computePolygon(points: FloatArray, offset: Int, count: Int, sorted: Boolean): FloatArray {
        var points = points
        var offset = offset
        val end = offset + count
        if (!sorted) {
            if (sortedPoints == null || sortedPoints!!.size < count) sortedPoints = FloatArray(count)
            java.lang.System.arraycopy(points, offset, sortedPoints, 0, count)
            points = sortedPoints
            offset = 0
            sort(points, count)
        }
        val hull = hull
        hull.clear()

        // Lower hull.
        run {
            var i = offset
            while (i < end) {
                val x = points[i]
                val y = points[i + 1]
                while (hull.size >= 4 && ccw(x, y) <= 0) hull.size -= 2
                hull.add(x)
                hull.add(y)
                i += 2
            }
        }

        // Upper hull.
        var i = end - 4
        val t = hull.size + 2
        while (i >= offset) {
            val x = points[i]
            val y = points[i + 1]
            while (hull.size >= t && ccw(x, y) <= 0) hull.size -= 2
            hull.add(x)
            hull.add(y)
            i -= 2
        }
        return hull
    }

    /**
     * @see .computeIndices
     */
    fun computeIndices(points: FloatArray, sorted: Boolean, yDown: Boolean): IntArray {
        return computeIndices(points.items, 0, points.size, sorted, yDown)
    }

    /**
     * @see .computeIndices
     */
    fun computeIndices(polygon: FloatArray, sorted: Boolean, yDown: Boolean): IntArray {
        return computeIndices(polygon, 0, polygon.size, sorted, yDown)
    }

    /**
     * Computes a hull the same as [.computePolygon] but returns indices of the specified points.
     */
    fun computeIndices(points: FloatArray, offset: Int, count: Int, sorted: Boolean, yDown: Boolean): IntArray {
        var points = points
        var offset = offset
        if (count > 32767) throw java.lang.IllegalArgumentException("count must be <= " + 32767)
        val end = offset + count
        if (!sorted) {
            if (sortedPoints == null || sortedPoints!!.size < count) sortedPoints = FloatArray(count)
            java.lang.System.arraycopy(points, offset, sortedPoints, 0, count)
            points = sortedPoints
            offset = 0
            sortWithIndices(points, count, yDown)
        }
        val indices = this.indices
        indices.clear()
        val hull = hull
        hull.clear()

        // Lower hull.
        run {
            var i = offset
            var index = i / 2
            while (i < end) {
                val x = points[i]
                val y = points[i + 1]
                while (hull.size >= 4 && ccw(x, y) <= 0) {
                    hull.size -= 2
                    indices.size--
                }
                hull.add(x)
                hull.add(y)
                indices.add(index)
                i += 2
                index++
            }
        }

        // Upper hull.
        var i = end - 4
        var index = i / 2
        val t = hull.size + 2
        while (i >= offset) {
            val x = points[i]
            val y = points[i + 1]
            while (hull.size >= t && ccw(x, y) <= 0) {
                hull.size -= 2
                indices.size--
            }
            hull.add(x)
            hull.add(y)
            indices.add(index)
            i -= 2
            index--
        }

        // Convert sorted to unsorted indices.
        if (!sorted) {
            val originalIndicesArray: ShortArray = originalIndices.items
            val indicesArray: IntArray = indices.items
            var i = 0
            val n = indices.size
            while (i < n) {
                indicesArray[i] = originalIndicesArray[indicesArray[i]].toInt()
                i++
            }
        }
        return indices
    }

    /**
     * Returns > 0 if the points are a counterclockwise turn, < 0 if clockwise, and 0 if colinear.
     */
    private fun ccw(p3x: Float, p3y: Float): Float {
        val hull = hull
        val size = hull.size
        val p1x = hull[size - 4]
        val p1y = hull[size - 3]
        val p2x = hull[size - 2]
        val p2y: Float = hull.peek()
        return (p2x - p1x) * (p3y - p1y) - (p2y - p1y) * (p3x - p1x)
    }

    /**
     * Sorts x,y pairs of values by the x value, then the y value.
     *
     * @param count Number of indices, must be even.
     */
    private fun sort(values: FloatArray, count: Int) {
        var lower = 0
        var upper = count - 1
        val stack = quicksortStack
        stack.add(lower)
        stack.add(upper - 1)
        while (stack.size > 0) {
            upper = stack.pop()
            lower = stack.pop()
            if (upper <= lower) continue
            val i = quicksortPartition(values, lower, upper)
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

    private fun quicksortPartition(values: FloatArray, lower: Int, upper: Int): Int {
        val x = values[lower]
        val y = values[lower + 1]
        var up = upper
        var down = lower
        var temp: Float
        while (down < up) {
            while (down < up && values[down] <= x) down = down + 2
            while (values[up] > x || values[up] == x && values[up + 1] < y) up = up - 2
            if (down < up) {
                temp = values[down]
                values[down] = values[up]
                values[up] = temp
                temp = values[down + 1]
                values[down + 1] = values[up + 1]
                values[up + 1] = temp
            }
        }
        values[lower] = values[up]
        values[up] = x
        values[lower + 1] = values[up + 1]
        values[up + 1] = y
        return up
    }

    /**
     * Sorts x,y pairs of values by the x value, then the y value and stores unsorted original indices.
     *
     * @param count Number of indices, must be even.
     */
    private fun sortWithIndices(values: FloatArray, count: Int, yDown: Boolean) {
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
            val i = quicksortPartitionWithIndices(values, lower, upper, yDown, originalIndicesArray)
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

    private fun quicksortPartitionWithIndices(values: FloatArray, lower: Int, upper: Int, yDown: Boolean, originalIndices: ShortArray): Int {
        val x = values[lower]
        val y = values[lower + 1]
        var up = upper
        var down = lower
        var temp: Float
        var tempIndex: Short
        while (down < up) {
            while (down < up && values[down] <= x) down = down + 2
            if (yDown) {
                while (values[up] > x || values[up] == x && values[up + 1] < y) up = up - 2
            } else {
                while (values[up] > x || values[up] == x && values[up + 1] > y) up = up - 2
            }
            if (down < up) {
                temp = values[down]
                values[down] = values[up]
                values[up] = temp
                temp = values[down + 1]
                values[down + 1] = values[up + 1]
                values[up + 1] = temp
                tempIndex = originalIndices[down / 2]
                originalIndices[down / 2] = originalIndices[up / 2]
                originalIndices[up / 2] = tempIndex
            }
        }
        values[lower] = values[up]
        values[up] = x
        values[lower + 1] = values[up + 1]
        values[up + 1] = y
        tempIndex = originalIndices[lower / 2]
        originalIndices[lower / 2] = originalIndices[up / 2]
        originalIndices[up / 2] = tempIndex
        return up
    }
}
