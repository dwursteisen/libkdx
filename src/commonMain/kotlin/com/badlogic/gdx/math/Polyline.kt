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

class Polyline : Shape2D {
    private var localVertices: FloatArray
    private var worldVertices: FloatArray?
    var x = 0f
        private set
    var y = 0f
        private set
    var originX = 0f
        private set
    var originY = 0f
        private set
    private var rotation = 0f
    var scaleX = 1f
        private set
    var scaleY = 1f
        private set

    /**
     * Returns the euclidean length of the polyline without scaling
     */
    var length = 0f
        get() {
            if (!calculateLength) return field
            calculateLength = false
            field = 0f
            var i = 0
            val n = localVertices.size - 2
            while (i < n) {
                val x = localVertices[i + 2] - localVertices[i]
                val y = localVertices[i + 1] - localVertices[i + 3]
                field += java.lang.Math.sqrt(x * x + y * y.toDouble()) as Float
                i += 2
            }
            return field
        }
        private set

    /**
     * Returns the euclidean length of the polyline
     */
    var scaledLength = 0f
        get() {
            if (!calculateScaledLength) return field
            calculateScaledLength = false
            field = 0f
            var i = 0
            val n = localVertices.size - 2
            while (i < n) {
                val x = localVertices[i + 2] * scaleX - localVertices[i] * scaleX
                val y = localVertices[i + 1] * scaleY - localVertices[i + 3] * scaleY
                field += java.lang.Math.sqrt(x * x + y * y.toDouble()) as Float
                i += 2
            }
            return field
        }
        private set
    private var calculateScaledLength = true
    private var calculateLength = true
    private var dirty = true

    constructor() {
        localVertices = FloatArray(0)
    }

    constructor(vertices: FloatArray) {
        if (vertices.size < 4) throw java.lang.IllegalArgumentException("polylines must contain at least 2 points.")
        localVertices = vertices
    }

    /**
     * Returns vertices without scaling or rotation and without being offset by the polyline position.
     */
    var vertices: FloatArray
        get() = localVertices
        set(vertices) {
            if (vertices.size < 4) throw java.lang.IllegalArgumentException("polylines must contain at least 2 points.")
            localVertices = vertices
            dirty = true
        }// scale if needed

    // rotate if needed

    /**
     * Returns vertices scaled, rotated, and offset by the polygon position.
     */
    val transformedVertices: FloatArray?
        get() {
            if (!dirty) return worldVertices
            dirty = false
            val localVertices = localVertices
            if (worldVertices == null || worldVertices!!.size < localVertices.size) worldVertices = FloatArray(localVertices.size)
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

    fun getRotation(): Float {
        return rotation
    }

    fun setOrigin(originX: Float, originY: Float) {
        this.originX = originX
        this.originY = originY
        dirty = true
    }

    fun setPosition(x: Float, y: Float) {
        this.x = x
        this.y = y
        dirty = true
    }

    fun setRotation(degrees: Float) {
        rotation = degrees
        dirty = true
    }

    fun rotate(degrees: Float) {
        rotation += degrees
        dirty = true
    }

    fun setScale(scaleX: Float, scaleY: Float) {
        this.scaleX = scaleX
        this.scaleY = scaleY
        dirty = true
        calculateScaledLength = true
    }

    fun scale(amount: Float) {
        scaleX += amount
        scaleY += amount
        dirty = true
        calculateScaledLength = true
    }

    fun calculateLength() {
        calculateLength = true
    }

    fun calculateScaledLength() {
        calculateScaledLength = true
    }

    fun dirty() {
        dirty = true
    }

    fun translate(x: Float, y: Float) {
        this.x += x
        this.y += y
        dirty = true
    }

    override operator fun contains(point: Vector2?): Boolean {
        return false
    }

    override fun contains(x: Float, y: Float): Boolean {
        return false
    }
}
