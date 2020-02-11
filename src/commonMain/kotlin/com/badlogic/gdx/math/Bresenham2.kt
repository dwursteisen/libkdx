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
 * Returns a list of points at integer coordinates for a line on a 2D grid, using the Bresenham algorithm.
 *
 *
 *
 *
 * Instances of this class own the returned array of points and the points themselves to avoid garbage collection as much as
 * possible. Calling any of the methods will result in the reuse of the previously returned array and vectors, expect
 *
 * @author badlogic
 */
class Bresenham2 {

    private val points: Array<GridPoint2> = Array<GridPoint2>()
    private val pool: Pool<GridPoint2?> = object : Pool<GridPoint2?>() {
        protected fun newObject(): GridPoint2 {
            return GridPoint2()
        }
    }

    /**
     * Returns a list of [GridPoint2] instances along the given line, at integer coordinates.
     *
     * @param start the start of the line
     * @param end   the end of the line
     * @return the list of points on the line at integer coordinates
     */
    fun line(start: GridPoint2, end: GridPoint2): Array<GridPoint2> {
        return line(start.x, start.y, end.x, end.y)
    }

    /**
     * Returns a list of [GridPoint2] instances along the given line, at integer coordinates.
     *
     * @param startX the start x coordinate of the line
     * @param startY the start y coordinate of the line
     * @param endX   the end x coordinate of the line
     * @param endY   the end y coordinate of the line
     * @return the list of points on the line at integer coordinates
     */
    fun line(startX: Int, startY: Int, endX: Int, endY: Int): Array<GridPoint2> {
        pool.freeAll(points)
        points.clear()
        return line(startX, startY, endX, endY, pool, points)
    }

    /**
     * Returns a list of [GridPoint2] instances along the given line, at integer coordinates.
     *
     * @param startX the start x coordinate of the line
     * @param startY the start y coordinate of the line
     * @param endX   the end x coordinate of the line
     * @param endY   the end y coordinate of the line
     * @param pool   the pool from which GridPoint2 instances are fetched
     * @param output the output array, will be cleared in this method
     * @return the list of points on the line at integer coordinates
     */
    fun line(startX: Int, startY: Int, endX: Int, endY: Int, pool: Pool<GridPoint2?>, output: Array<GridPoint2>): Array<GridPoint2> {
        var startX = startX
        var startY = startY
        val w = endX - startX
        val h = endY - startY
        var dx1 = 0
        var dy1 = 0
        var dx2 = 0
        var dy2 = 0
        if (w < 0) {
            dx1 = -1
            dx2 = -1
        } else if (w > 0) {
            dx1 = 1
            dx2 = 1
        }
        if (h < 0) dy1 = -1 else if (h > 0) dy1 = 1
        var longest: Int = java.lang.Math.abs(w)
        var shortest: Int = java.lang.Math.abs(h)
        if (longest <= shortest) {
            longest = java.lang.Math.abs(h)
            shortest = java.lang.Math.abs(w)
            if (h < 0) dy2 = -1 else if (h > 0) dy2 = 1
            dx2 = 0
        }
        var numerator = longest shr 1
        for (i in 0..longest) {
            val point: GridPoint2 = pool.obtain()
            point.set(startX, startY)
            output.add(point)
            numerator += shortest
            if (numerator > longest) {
                numerator -= longest
                startX += dx1
                startY += dy1
            } else {
                startX += dx2
                startY += dy2
            }
        }
        return output
    }
}
