/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
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
 * A convenient 2D circle class.
 *
 * @author mzechner
 */
class Circle : Serializable, Shape2D {

    var x = 0f
    var y = 0f
    var radius = 0f

    /**
     * Constructs a new circle with all values set to zero
     */
    constructor() {}

    /**
     * Constructs a new circle with the given X and Y coordinates and the given radius.
     *
     * @param x      X coordinate
     * @param y      Y coordinate
     * @param radius The radius of the circle
     */
    constructor(x: Float, y: Float, radius: Float) {
        this.x = x
        this.y = y
        this.radius = radius
    }

    /**
     * Constructs a new circle using a given [Vector2] that contains the desired X and Y coordinates, and a given radius.
     *
     * @param position The position [Vector2].
     * @param radius   The radius
     */
    constructor(position: Vector2, radius: Float) {
        x = position.x
        y = position.y
        this.radius = radius
    }

    /**
     * Copy constructor
     *
     * @param circle The circle to construct a copy of.
     */
    constructor(circle: Circle) {
        x = circle.x
        y = circle.y
        radius = circle.radius
    }

    /**
     * Creates a new [Circle] in terms of its center and a point on its edge.
     *
     * @param center The center of the new circle
     * @param edge   Any point on the edge of the given circle
     */
    constructor(center: Vector2, edge: Vector2) {
        x = center.x
        y = center.y
        radius = Vector2.len(center.x - edge.x, center.y - edge.y)
    }

    /**
     * Sets a new location and radius for this circle.
     *
     * @param x      X coordinate
     * @param y      Y coordinate
     * @param radius Circle radius
     */
    operator fun set(x: Float, y: Float, radius: Float) {
        this.x = x
        this.y = y
        this.radius = radius
    }

    /**
     * Sets a new location and radius for this circle.
     *
     * @param position Position [Vector2] for this circle.
     * @param radius   Circle radius
     */
    operator fun set(position: Vector2, radius: Float) {
        x = position.x
        y = position.y
        this.radius = radius
    }

    /**
     * Sets a new location and radius for this circle, based upon another circle.
     *
     * @param circle The circle to copy the position and radius of.
     */
    fun set(circle: Circle) {
        x = circle.x
        y = circle.y
        radius = circle.radius
    }

    /**
     * Sets this [Circle]'s values in terms of its center and a point on its edge.
     *
     * @param center The new center of the circle
     * @param edge   Any point on the edge of the given circle
     */
    operator fun set(center: Vector2, edge: Vector2) {
        x = center.x
        y = center.y
        radius = Vector2.len(center.x - edge.x, center.y - edge.y)
    }

    /**
     * Sets the x and y-coordinates of circle center from vector
     *
     * @param position The position vector
     */
    fun setPosition(position: Vector2) {
        x = position.x
        y = position.y
    }

    /**
     * Sets the x and y-coordinates of circle center
     *
     * @param x The x-coordinate
     * @param y The y-coordinate
     */
    fun setPosition(x: Float, y: Float) {
        this.x = x
        this.y = y
    }

    /**
     * Sets the x-coordinate of circle center
     *
     * @param x The x-coordinate
     */
    fun setX(x: Float) {
        this.x = x
    }

    /**
     * Sets the y-coordinate of circle center
     *
     * @param y The y-coordinate
     */
    fun setY(y: Float) {
        this.y = y
    }

    /**
     * Sets the radius of circle
     *
     * @param radius The radius
     */
    fun setRadius(radius: Float) {
        this.radius = radius
    }

    /**
     * Checks whether or not this circle contains a given point.
     *
     * @param x X coordinate
     * @param y Y coordinate
     * @return true if this circle contains the given point.
     */
    override fun contains(x: Float, y: Float): Boolean {
        var x = x
        var y = y
        x = this.x - x
        y = this.y - y
        return x * x + y * y <= radius * radius
    }

    /**
     * Checks whether or not this circle contains a given point.
     *
     * @param point The [Vector2] that contains the point coordinates.
     * @return true if this circle contains this point; false otherwise.
     */
    operator fun contains(point: Vector2): Boolean {
        val dx = x - point.x
        val dy = y - point.y
        return dx * dx + dy * dy <= radius * radius
    }

    /**
     * @param c the other [Circle]
     * @return whether this circle contains the other circle.
     */
    operator fun contains(c: Circle): Boolean {
        val radiusDiff = radius - c.radius
        if (radiusDiff < 0f) return false // Can't contain bigger circle
        val dx = x - c.x
        val dy = y - c.y
        val dst = dx * dx + dy * dy
        val radiusSum = radius + c.radius
        return radiusDiff * radiusDiff >= dst && dst < radiusSum * radiusSum
    }

    /**
     * @param c the other [Circle]
     * @return whether this circle overlaps the other circle.
     */
    fun overlaps(c: Circle): Boolean {
        val dx = x - c.x
        val dy = y - c.y
        val distance = dx * dx + dy * dy
        val radiusSum = radius + c.radius
        return distance < radiusSum * radiusSum
    }

    /**
     * Returns a [String] representation of this [Circle] of the form `x,y,radius`.
     */
    override fun toString(): String {
        return "$x,$y,$radius"
    }

    /**
     * @return The circumference of this circle (as 2 * [MathUtils.PI2]) * `radius`
     */
    fun circumference(): Float {
        return radius * MathUtils.PI2
    }

    /**
     * @return The area of this circle (as [MathUtils.PI] * radius * radius).
     */
    fun area(): Float {
        return radius * radius * MathUtils.PI
    }

    override fun equals(o: Any?): Boolean {
        if (o === this) return true
        if (o == null || o.javaClass != this.javaClass) return false
        val c = o as Circle
        return x == c.x && y == c.y && radius == c.radius
    }

    override fun hashCode(): Int {
        val prime = 41
        var result = 1
        result = prime * result + NumberUtils.floatToRawIntBits(radius)
        result = prime * result + NumberUtils.floatToRawIntBits(x)
        result = prime * result + NumberUtils.floatToRawIntBits(y)
        return result
    }
}
