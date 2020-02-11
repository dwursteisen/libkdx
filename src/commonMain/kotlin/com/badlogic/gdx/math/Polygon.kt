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
 * Encapsulates a 2D polygon defined by it's vertices relative to an origin point (default of 0, 0).
 */
class Polygon : Shape2D {

    private var localVertices: FloatArray
    private var worldVertices: FloatArray?

    /**
     * Returns the x-coordinate of the polygon's position within the world.
     */
    var x = 0f
        private set

    /**
     * Returns the y-coordinate of the polygon's position within the world.
     */
    var y = 0f
        private set

    /**
     * Returns the x-coordinate of the polygon's origin point.
     */
    var originX = 0f
        private set

    /**
     * Returns the y-coordinate of the polygon's origin point.
     */
    var originY = 0f
        private set
    private var rotation = 0f

    /**
     * Returns the total horizontal scaling applied to the polygon.
     */
    var scaleX = 1f
        private set

    /**
     * Returns the total vertical scaling applied to the polygon.
     */
    var scaleY = 1f
        private set
    private var dirty = true
    private var bounds: Rectangle? = null

    /**
     * Constructs a new polygon with no vertices.
     */
    constructor() {
        localVertices = FloatArray(0)
    }

    /**
     * Constructs a new polygon from a float array of parts of vertex points.
     *
     * @param vertices an array where every even element represents the horizontal part of a point, and the following element
     * representing the vertical part
     * @throws IllegalArgumentException if less than 6 elements, representing 3 points, are provided
     */
    constructor(vertices: FloatArray) {
        if (vertices.size < 6) throw java.lang.IllegalArgumentException("polygons must contain at least 3 points.")
        localVertices = vertices
    }

    /**
     * Returns the polygon's local vertices without scaling or rotation and without being offset by the polygon position.
     */
    /**
     * Sets the polygon's local vertices relative to the origin point, without any scaling, rotating or translations being applied.
     *
     * @param vertices float array where every even element represents the x-coordinate of a vertex, and the proceeding element
     * representing the y-coordinate.
     * @throws IllegalArgumentException if less than 6 elements, representing 3 points, are provided
     */
    var vertices: FloatArray
        get() = localVertices
        set(vertices) {
            if (vertices.size < 6) throw java.lang.IllegalArgumentException("polygons must contain at least 3 points.")
            localVertices = vertices
            dirty = true
        }// scale if needed

    // rotate if needed

    /**
     * Calculates and returns the vertices of the polygon after scaling, rotation, and positional translations have been applied,
     * as they are position within the world.
     *
     * @return vertices scaled, rotated, and offset by the polygon position.
     */
    val transformedVertices: FloatArray?
        get() {
            if (!dirty) return worldVertices
            dirty = false
            val localVertices = localVertices
            if (worldVertices == null || worldVertices!!.size != localVertices.size) worldVertices = FloatArray(localVertices.size)
            val worldVertices = worldVertices
            val positionX = x
            val positionY = y
            val originX = originX
            val originY = originY
            val scaleX = scaleX
            val scaleY = scaleY
            val scale = scaleX != 1f || scaleY != 1f
            val rotation = rotation
            val cos: Float = MathUtils.cosDeg(rotation)
            val sin: Float = MathUtils.sinDeg(rotation)
            var i = 0
            val n = localVertices.size
            while (i < n) {
                var x = localVertices[i] - originX
                var y = localVertices[i + 1] - originY

                // scale if needed
                if (scale) {
                    x *= scaleX
                    y *= scaleY
                }

                // rotate if needed
                if (rotation != 0f) {
                    val oldX = x
                    x = cos * x - sin * y
                    y = sin * oldX + cos * y
                }
                worldVertices!![i] = positionX + x + originX
                worldVertices[i + 1] = positionY + y + originY
                i += 2
            }
            return worldVertices
        }

    /**
     * Sets the origin point to which all of the polygon's local vertices are relative to.
     */
    fun setOrigin(originX: Float, originY: Float) {
        this.originX = originX
        this.originY = originY
        dirty = true
    }

    /**
     * Sets the polygon's position within the world.
     */
    fun setPosition(x: Float, y: Float) {
        this.x = x
        this.y = y
        dirty = true
    }

    /**
     * Translates the polygon's position by the specified horizontal and vertical amounts.
     */
    fun translate(x: Float, y: Float) {
        this.x += x
        this.y += y
        dirty = true
    }

    /**
     * Sets the polygon to be rotated by the supplied degrees.
     */
    fun setRotation(degrees: Float) {
        rotation = degrees
        dirty = true
    }

    /**
     * Applies additional rotation to the polygon by the supplied degrees.
     */
    fun rotate(degrees: Float) {
        rotation += degrees
        dirty = true
    }

    /**
     * Sets the amount of scaling to be applied to the polygon.
     */
    fun setScale(scaleX: Float, scaleY: Float) {
        this.scaleX = scaleX
        this.scaleY = scaleY
        dirty = true
    }

    /**
     * Applies additional scaling to the polygon by the supplied amount.
     */
    fun scale(amount: Float) {
        scaleX += amount
        scaleY += amount
        dirty = true
    }

    /**
     * Sets the polygon's world vertices to be recalculated when calling [getTransformedVertices][.getTransformedVertices].
     */
    fun dirty() {
        dirty = true
    }

    /**
     * Returns the area contained within the polygon.
     */
    fun area(): Float {
        val vertices = transformedVertices
        return GeometryUtils.polygonArea(vertices, 0, vertices!!.size)
    }

    /**
     * Returns an axis-aligned bounding box of this polygon.
     *
     *
     * Note the returned Rectangle is cached in this polygon, and will be reused if this Polygon is changed.
     *
     * @return this polygon's bounding box [Rectangle]
     */
    val boundingRectangle: Rectangle
        get() {
            val vertices = transformedVertices
            var minX = vertices!![0]
            var minY = vertices[1]
            var maxX = vertices[0]
            var maxY = vertices[1]
            val numFloats = vertices.size
            var i = 2
            while (i < numFloats) {
                minX = if (minX > vertices[i]) vertices[i] else minX
                minY = if (minY > vertices[i + 1]) vertices[i + 1] else minY
                maxX = if (maxX < vertices[i]) vertices[i] else maxX
                maxY = if (maxY < vertices[i + 1]) vertices[i + 1] else maxY
                i += 2
            }
            if (bounds == null) bounds = Rectangle()
            bounds!!.x = minX
            bounds!!.y = minY
            bounds!!.width = maxX - minX
            bounds!!.height = maxY - minY
            return bounds
        }

    /**
     * Returns whether an x, y pair is contained within the polygon.
     */
    override fun contains(x: Float, y: Float): Boolean {
        val vertices = transformedVertices
        val numFloats = vertices!!.size
        var intersects = 0
        var i = 0
        while (i < numFloats) {
            val x1 = vertices[i]
            val y1 = vertices[i + 1]
            val x2 = vertices[(i + 2) % numFloats]
            val y2 = vertices[(i + 3) % numFloats]
            if ((y1 <= y && y < y2 || y2 <= y && y < y1) && x < (x2 - x1) / (y2 - y1) * (y - y1) + x1) intersects++
            i += 2
        }
        return intersects and 1 == 1
    }

    operator fun contains(point: Vector2): Boolean {
        return contains(point.x, point.y)
    }

    /**
     * Returns the total rotation applied to the polygon.
     */
    fun getRotation(): Float {
        return rotation
    }
}
