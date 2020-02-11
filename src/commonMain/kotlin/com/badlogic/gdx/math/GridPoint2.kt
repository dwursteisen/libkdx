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
 * A point in a 2D grid, with integer x and y coordinates
 *
 * @author badlogic
 */
class GridPoint2 : Serializable {

    var x = 0
    var y = 0

    /**
     * Constructs a new 2D grid point.
     */
    constructor() {}

    /**
     * Constructs a new 2D grid point.
     *
     * @param x X coordinate
     * @param y Y coordinate
     */
    constructor(x: Int, y: Int) {
        this.x = x
        this.y = y
    }

    /**
     * Copy constructor
     *
     * @param point The 2D grid point to make a copy of.
     */
    constructor(point: GridPoint2) {
        x = point.x
        y = point.y
    }

    /**
     * Sets the coordinates of this 2D grid point to that of another.
     *
     * @param point The 2D grid point to copy the coordinates of.
     * @return this 2D grid point for chaining.
     */
    fun set(point: GridPoint2): GridPoint2 {
        x = point.x
        y = point.y
        return this
    }

    /**
     * Sets the coordinates of this 2D grid point.
     *
     * @param x X coordinate
     * @param y Y coordinate
     * @return this 2D grid point for chaining.
     */
    operator fun set(x: Int, y: Int): GridPoint2 {
        this.x = x
        this.y = y
        return this
    }

    /**
     * @param other The other point
     * @return the squared distance between this point and the other point.
     */
    fun dst2(other: GridPoint2): Float {
        val xd = other.x - x
        val yd = other.y - y
        return (xd * xd + yd * yd).toFloat()
    }

    /**
     * @param x The x-coordinate of the other point
     * @param y The y-coordinate of the other point
     * @return the squared distance between this point and the other point.
     */
    fun dst2(x: Int, y: Int): Float {
        val xd = x - this.x
        val yd = y - this.y
        return (xd * xd + yd * yd).toFloat()
    }

    /**
     * @param other The other point
     * @return the distance between this point and the other vector.
     */
    fun dst(other: GridPoint2): Float {
        val xd = other.x - x
        val yd = other.y - y
        return java.lang.Math.sqrt(xd * xd + yd * yd.toDouble())
    }

    /**
     * @param x The x-coordinate of the other point
     * @param y The y-coordinate of the other point
     * @return the distance between this point and the other point.
     */
    fun dst(x: Int, y: Int): Float {
        val xd = x - this.x
        val yd = y - this.y
        return java.lang.Math.sqrt(xd * xd + yd * yd.toDouble())
    }

    /**
     * Adds another 2D grid point to this point.
     *
     * @param other The other point
     * @return this 2d grid point for chaining.
     */
    fun add(other: GridPoint2): GridPoint2 {
        x += other.x
        y += other.y
        return this
    }

    /**
     * Adds another 2D grid point to this point.
     *
     * @param x The x-coordinate of the other point
     * @param y The y-coordinate of the other point
     * @return this 2d grid point for chaining.
     */
    fun add(x: Int, y: Int): GridPoint2 {
        this.x += x
        this.y += y
        return this
    }

    /**
     * Subtracts another 2D grid point from this point.
     *
     * @param other The other point
     * @return this 2d grid point for chaining.
     */
    fun sub(other: GridPoint2): GridPoint2 {
        x -= other.x
        y -= other.y
        return this
    }

    /**
     * Subtracts another 2D grid point from this point.
     *
     * @param x The x-coordinate of the other point
     * @param y The y-coordinate of the other point
     * @return this 2d grid point for chaining.
     */
    fun sub(x: Int, y: Int): GridPoint2 {
        this.x -= x
        this.y -= y
        return this
    }

    /**
     * @return a copy of this grid point
     */
    fun cpy(): GridPoint2 {
        return GridPoint2(this)
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || o.javaClass != this.javaClass) return false
        val g = o as GridPoint2
        return x == g.x && y == g.y
    }

    override fun hashCode(): Int {
        val prime = 53
        var result = 1
        result = prime * result + x
        result = prime * result + y
        return result
    }

    override fun toString(): String {
        return "($x, $y)"
    }

    companion object {
        private const val serialVersionUID = -4019969926331717380L
    }
}
