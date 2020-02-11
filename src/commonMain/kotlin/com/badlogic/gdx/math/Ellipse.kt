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
 * A convenient 2D ellipse class, based on the circle class
 *
 * @author tonyp7
 */
class Ellipse : Serializable, Shape2D {

    var x = 0f
    var y = 0f
    var width = 0f
    var height = 0f

    /**
     * Construct a new ellipse with all values set to zero
     */
    constructor() {}

    /**
     * Copy constructor
     *
     * @param ellipse Ellipse to construct a copy of.
     */
    constructor(ellipse: Ellipse) {
        x = ellipse.x
        y = ellipse.y
        width = ellipse.width
        height = ellipse.height
    }

    /**
     * Constructs a new ellipse
     *
     * @param x      X coordinate
     * @param y      Y coordinate
     * @param width  the width of the ellipse
     * @param height the height of the ellipse
     */
    constructor(x: Float, y: Float, width: Float, height: Float) {
        this.x = x
        this.y = y
        this.width = width
        this.height = height
    }

    /**
     * Costructs a new ellipse
     *
     * @param position Position vector
     * @param width    the width of the ellipse
     * @param height   the height of the ellipse
     */
    constructor(position: Vector2, width: Float, height: Float) {
        x = position.x
        y = position.y
        this.width = width
        this.height = height
    }

    constructor(position: Vector2, size: Vector2) {
        x = position.x
        y = position.y
        width = size.x
        height = size.y
    }

    /**
     * Constructs a new [Ellipse] from the position and radius of a [Circle] (since circles are special cases of
     * ellipses).
     *
     * @param circle The circle to take the values of
     */
    constructor(circle: Circle) {
        x = circle.x
        y = circle.y
        width = circle.radius * 2f
        height = circle.radius * 2f
    }

    /**
     * Checks whether or not this ellipse contains the given point.
     *
     * @param x X coordinate
     * @param y Y coordinate
     * @return true if this ellipse contains the given point; false otherwise.
     */
    override fun contains(x: Float, y: Float): Boolean {
        var x = x
        var y = y
        x = x - this.x
        y = y - this.y
        return x * x / (width * 0.5f * width * 0.5f) + y * y / (height * 0.5f * height * 0.5f) <= 1.0f
    }

    /**
     * Checks whether or not this ellipse contains the given point.
     *
     * @param point Position vector
     * @return true if this ellipse contains the given point; false otherwise.
     */
    operator fun contains(point: Vector2): Boolean {
        return contains(point.x, point.y)
    }

    /**
     * Sets a new position and size for this ellipse.
     *
     * @param x      X coordinate
     * @param y      Y coordinate
     * @param width  the width of the ellipse
     * @param height the height of the ellipse
     */
    operator fun set(x: Float, y: Float, width: Float, height: Float) {
        this.x = x
        this.y = y
        this.width = width
        this.height = height
    }

    /**
     * Sets a new position and size for this ellipse based upon another ellipse.
     *
     * @param ellipse The ellipse to copy the position and size of.
     */
    fun set(ellipse: Ellipse) {
        x = ellipse.x
        y = ellipse.y
        width = ellipse.width
        height = ellipse.height
    }

    fun set(circle: Circle) {
        x = circle.x
        y = circle.y
        width = circle.radius * 2f
        height = circle.radius * 2f
    }

    operator fun set(position: Vector2, size: Vector2) {
        x = position.x
        y = position.y
        width = size.x
        height = size.y
    }

    /**
     * Sets the x and y-coordinates of ellipse center from a [Vector2].
     *
     * @param position The position vector
     * @return this ellipse for chaining
     */
    fun setPosition(position: Vector2): Ellipse {
        x = position.x
        y = position.y
        return this
    }

    /**
     * Sets the x and y-coordinates of ellipse center
     *
     * @param x The x-coordinate
     * @param y The y-coordinate
     * @return this ellipse for chaining
     */
    fun setPosition(x: Float, y: Float): Ellipse {
        this.x = x
        this.y = y
        return this
    }

    /**
     * Sets the width and height of this ellipse
     *
     * @param width  The width
     * @param height The height
     * @return this ellipse for chaining
     */
    fun setSize(width: Float, height: Float): Ellipse {
        this.width = width
        this.height = height
        return this
    }

    /**
     * @return The area of this [Ellipse] as [MathUtils.PI] * [Ellipse.width] * [Ellipse.height]
     */
    fun area(): Float {
        return MathUtils.PI * (width * height) / 4
    }

    /**
     * Approximates the circumference of this [Ellipse]. Oddly enough, the circumference of an ellipse is actually difficult
     * to compute exactly.
     *
     * @return The Ramanujan approximation to the circumference of an ellipse if one dimension is at least three times longer than
     * the other, else the simpler approximation
     */
    fun circumference(): Float {
        val a = width / 2
        val b = height / 2
        return if (a * 3 > b || b * 3 > a) {
            // If one dimension is three times as long as the other...
            (MathUtils.PI * (3 * (a + b) - java.lang.Math.sqrt((3 * a + b) * (a + 3 * b).toDouble()))) as Float
        } else {
            // We can use the simpler approximation, then
            (MathUtils.PI2 * java.lang.Math.sqrt((a * a + b * b) / 2.toDouble())) as Float
        }
    }

    override fun equals(o: Any?): Boolean {
        if (o === this) return true
        if (o == null || o.javaClass != this.javaClass) return false
        val e = o as Ellipse
        return x == e.x && y == e.y && width == e.width && height == e.height
    }

    override fun hashCode(): Int {
        val prime = 53
        var result = 1
        result = prime * result + NumberUtils.floatToRawIntBits(height)
        result = prime * result + NumberUtils.floatToRawIntBits(width)
        result = prime * result + NumberUtils.floatToRawIntBits(x)
        result = prime * result + NumberUtils.floatToRawIntBits(y)
        return result
    }

    companion object {
        private const val serialVersionUID = 7381533206532032099L
    }
}
