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
 * A simple implementation of the ear cutting algorithm to triangulate simple polygons without holes. For more information:
 *
 *  * [http://cgm.cs.mcgill.ca/~godfried/
 * teaching/cg-projects/97/Ian/algorithm2.html](http://cgm.cs.mcgill.ca/~godfried/teaching/cg-projects/97/Ian/algorithm2.html)
 *  * [http://www.geometrictools.com/Documentation
 * /TriangulationByEarClipping.pdf](http://www.geometrictools.com/Documentation/TriangulationByEarClipping.pdf)
 *
 * If the input polygon is not simple (self-intersects), there will be output but it is of unspecified quality (garbage in,
 * garbage out).
 *
 *
 * If the polygon vertices are very large or very close together then [GeometryUtils.isClockwise] may not
 * be able to properly assess the winding (because it uses floats). In that case the vertices should be adjusted, eg by finding
 * the smallest X and Y values and subtracting that from each vertex.
 *
 * @author badlogicgames@gmail.com
 * @author Nicolas Gramlich (optimizations, collinear edge support)
 * @author Eric Spitz
 * @author Thomas ten Cate (bugfixes, optimizations)
 * @author Nathan Sweet (rewrite, return indices, no allocation, optimizations)
 */
class EarClippingTriangulator {

    private val indicesArray: ShortArray = ShortArray()
    private var indices: ShortArray
    private var vertices: FloatArray
    private var vertexCount = 0
    private val vertexTypes: IntArray = IntArray()
    private val triangles: ShortArray = ShortArray()

    /**
     * @see .computeTriangles
     */
    fun computeTriangles(vertices: FloatArray): ShortArray {
        return computeTriangles(vertices.items, 0, vertices.size)
    }

    /**
     * @see .computeTriangles
     */
    fun computeTriangles(vertices: FloatArray): ShortArray {
        return computeTriangles(vertices, 0, vertices.size)
    }

    /**
     * Triangulates the given (convex or concave) simple polygon to a list of triangle vertices.
     *
     * @param vertices pairs describing vertices of the polygon, in either clockwise or counterclockwise order.
     * @return triples of triangle indices in clockwise order. Note the returned array is reused for later calls to the same
     * method.
     */
    fun computeTriangles(vertices: FloatArray, offset: Int, count: Int): ShortArray {
        this.vertices = vertices
        vertexCount = count / 2
        val vertexCount = vertexCount
        val vertexOffset = offset / 2
        val indicesArray = indicesArray
        indicesArray.clear()
        indicesArray.ensureCapacity(vertexCount)
        indicesArray.size = vertexCount
        this.indices = indicesArray.items
        val indices = this.indices
        if (GeometryUtils.isClockwise(vertices, offset, count)) {
            for (i in 0 until vertexCount) indices[i] = (vertexOffset + i).toShort()
        } else {
            var i = 0
            val n = vertexCount - 1
            while (i < vertexCount) {
                indices[i] = (vertexOffset + n - i).toShort() // Reversed.
                i++
            }
        }
        val vertexTypes = vertexTypes
        vertexTypes.clear()
        vertexTypes.ensureCapacity(vertexCount)
        var i = 0
        val n = vertexCount
        while (i < n) {
            vertexTypes.add(classifyVertex(i))
            ++i
        }

        // A polygon with n vertices has a triangulation of n-2 triangles.
        val triangles = triangles
        triangles.clear()
        triangles.ensureCapacity(java.lang.Math.max(0, vertexCount - 2) * 3)
        triangulate()
        return triangles
    }

    private fun triangulate() {
        val vertexTypes: IntArray = vertexTypes.items
        while (vertexCount > 3) {
            val earTipIndex = findEarTip()
            cutEarTip(earTipIndex)

            // The type of the two vertices adjacent to the clipped vertex may have changed.
            val previousIndex = previousIndex(earTipIndex)
            val nextIndex = if (earTipIndex == vertexCount) 0 else earTipIndex
            vertexTypes[previousIndex] = classifyVertex(previousIndex)
            vertexTypes[nextIndex] = classifyVertex(nextIndex)
        }
        if (vertexCount == 3) {
            val triangles = triangles
            val indices = this.indices
            triangles.add(indices[0])
            triangles.add(indices[1])
            triangles.add(indices[2])
        }
    }

    /**
     * @return [.CONCAVE] or [.CONVEX]
     */
    private fun classifyVertex(index: Int): Int {
        val indices = this.indices
        val previous = indices[previousIndex(index)] * 2
        val current = indices[index] * 2
        val next = indices[nextIndex(index)] * 2
        val vertices = vertices
        return computeSpannedAreaSign(vertices[previous], vertices[previous + 1], vertices[current], vertices[current + 1],
            vertices[next], vertices[next + 1])
    }

    private fun findEarTip(): Int {
        val vertexCount = vertexCount
        for (i in 0 until vertexCount) if (isEarTip(i)) return i

        // Desperate mode: if no vertex is an ear tip, we are dealing with a degenerate polygon (e.g. nearly collinear).
        // Note that the input was not necessarily degenerate, but we could have made it so by clipping some valid ears.

        // Idea taken from Martin Held, "FIST: Fast industrial-strength triangulation of polygons", Algorithmica (1998),
        // http://citeseerx.ist.psu.edu/viewdoc/summary?doi=10.1.1.115.291

        // Return a convex or tangential vertex if one exists.
        val vertexTypes: IntArray = vertexTypes.items
        for (i in 0 until vertexCount) if (vertexTypes[i] != CONCAVE) return i
        return 0 // If all vertices are concave, just return the first one.
    }

    private fun isEarTip(earTipIndex: Int): Boolean {
        val vertexTypes: IntArray = vertexTypes.items
        if (vertexTypes[earTipIndex] == CONCAVE) return false
        val previousIndex = previousIndex(earTipIndex)
        val nextIndex = nextIndex(earTipIndex)
        val indices = this.indices
        val p1 = indices[previousIndex] * 2
        val p2 = indices[earTipIndex] * 2
        val p3 = indices[nextIndex] * 2
        val vertices = vertices
        val p1x = vertices[p1]
        val p1y = vertices[p1 + 1]
        val p2x = vertices[p2]
        val p2y = vertices[p2 + 1]
        val p3x = vertices[p3]
        val p3y = vertices[p3 + 1]

        // Check if any point is inside the triangle formed by previous, current and next vertices.
        // Only consider vertices that are not part of this triangle, or else we'll always find one inside.
        var i = nextIndex(nextIndex)
        while (i != previousIndex) {

            // Concave vertices can obviously be inside the candidate ear, but so can tangential vertices
            // if they coincide with one of the triangle's vertices.
            if (vertexTypes[i] != CONVEX) {
                val v = indices[i] * 2
                val vx = vertices[v]
                val vy = vertices[v + 1]
                // Because the polygon has clockwise winding order, the area sign will be positive if the point is strictly inside.
                // It will be 0 on the edge, which we want to include as well.
                // note: check the edge defined by p1->p3 first since this fails _far_ more then the other 2 checks.
                if (computeSpannedAreaSign(p3x, p3y, p1x, p1y, vx, vy) >= 0) {
                    if (computeSpannedAreaSign(p1x, p1y, p2x, p2y, vx, vy) >= 0) {
                        if (computeSpannedAreaSign(p2x, p2y, p3x, p3y, vx, vy) >= 0) return false
                    }
                }
            }
            i = nextIndex(i)
        }
        return true
    }

    private fun cutEarTip(earTipIndex: Int) {
        val indices = this.indices
        val triangles = triangles
        triangles.add(indices[previousIndex(earTipIndex)])
        triangles.add(indices[earTipIndex])
        triangles.add(indices[nextIndex(earTipIndex)])
        indicesArray.removeIndex(earTipIndex)
        vertexTypes.removeIndex(earTipIndex)
        vertexCount--
    }

    private fun previousIndex(index: Int): Int {
        return (if (index == 0) vertexCount else index) - 1
    }

    private fun nextIndex(index: Int): Int {
        return (index + 1) % vertexCount
    }

    companion object {
        private const val CONCAVE = -1
        private const val CONVEX = 1
        private fun computeSpannedAreaSign(p1x: Float, p1y: Float, p2x: Float, p2y: Float, p3x: Float, p3y: Float): Int {
            var area = p1x * (p3y - p2y)
            area += p2x * (p1y - p3y)
            area += p3x * (p2y - p1y)
            return java.lang.Math.signum(area)
        }
    }
}
