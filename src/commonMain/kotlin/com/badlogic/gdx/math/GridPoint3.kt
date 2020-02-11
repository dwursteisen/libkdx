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
 * A point in a 3D grid, with integer x and y coordinates
 *
 * @author badlogic
 */
class GridPoint3 : Serializable {

    var x = 0
    var y = 0
    var z = 0

    /**
     * Constructs a 3D grid point with all coordinates pointing to the origin (0, 0, 0).
     */
    constructor() {}

    /**
     * Constructs a 3D grid point.
     *
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     */
    constructor(x: Int, y: Int, z: Int) {
        this.x = x
        this.y = y
        this.z = z
    }

    /**
     * Copy constructor
     *
     * @param point The 3D grid point to make a copy of.
     */
    constructor(point: GridPoint3) {
        x = point.x
        y = point.y
        z = point.z
    }

    /**
     * Sets the coordinates of this 3D grid point to that of another.
     *
     * @param point The 3D grid point to copy coordinates of.
     * @return this GridPoint3 for chaining.
     */
    fun set(point: GridPoint3): GridPoint3 {
        x = point.x
        y = point.y
        z = point.z
        return this
    }

    /**
     * Sets the coordinates of this GridPoint3D.
     *
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     * @return this GridPoint3D for chaining.
     */
    operator fun set(x: Int, y: Int, z: Int): GridPoint3 {
        this.x = x
        this.y = y
        this.z = z
        return this
    }

    /**
     * @param other The other point
     * @return the squared distance between this point and the other point.
     */
    fun dst2(other: GridPoint3): Float {
        val xd = other.x - x
        val yd = other.y - y
        val zd = other.z - z
        return (xd * xd + yd * yd + zd * zd).toFloat()
    }

    /**
     * @param x The x-coordinate of the other point
     * @param y The y-coordinate of the other point
     * @param z The z-coordinate of the other point
     * @return the squared distance between this point and the other point.
     */
    fun dst2(x: Int, y: Int, z: Int): Float {
        val xd = x - this.x
        val yd = y - this.y
        val zd = z - this.z
        return (xd * xd + yd * yd + zd * zd).toFloat()
    }

    /**
     * @param other The other point
     * @return the distance between this point and the other vector.
     */
    fun dst(other: GridPoint3): Float {
        val xd = other.x - x
        val yd = other.y - y
        val zd = other.z - z
        return java.lang.Math.sqrt(xd * xd + yd * yd + (zd * zd).toDouble())
    }

    /**
     * @param x The x-coordinate of the other point
     * @param y The y-coordinate of the other point
     * @param z The z-coordinate of the other point
     * @return the distance between this point and the other point.
     */
    fun dst(x: Int, y: Int, z: Int): Float {
        val xd = x - this.x
        val yd = y - this.y
        val zd = z - this.z
        return java.lang.Math.sqrt(xd * xd + yd * yd + (zd * zd).toDouble())
    }

    /**
     * Adds another 3D grid point to this point.
     *
     * @param other The other point
     * @return this 3d grid point for chaining.
     */
    fun add(other: GridPoint3): GridPoint3 {
        x += other.x
        y += other.y
        z += other.z
        return this
    }

    /**
     * Adds another 3D grid point to this point.
     *
     * @param x The x-coordinate of the other point
     * @param y The y-coordinate of the other point
     * @param z The z-coordinate of the other point
     * @return this 3d grid point for chaining.
     */
    fun add(x: Int, y: Int, z: Int): GridPoint3 {
        this.x += x
        this.y += y
        this.z += z
        return this
    }

    /**
     * Subtracts another 3D grid point from this point.
     *
     * @param other The other point
     * @return this 3d grid point for chaining.
     */
    fun sub(other: GridPoint3): GridPoint3 {
        x -= other.x
        y -= other.y
        z -= other.z
        return this
    }

    /**
     * Subtracts another 3D grid point from this point.
     *
     * @param x The x-coordinate of the other point
     * @param y The y-coordinate of the other point
     * @param z The z-coordinate of the other point
     * @return this 3d grid point for chaining.
     */
    fun sub(x: Int, y: Int, z: Int): GridPoint3 {
        this.x -= x
        this.y -= y
        this.z -= z
        return this
    }

    /**
     * @return a copy of this grid point
     */
    fun cpy(): GridPoint3 {
        return GridPoint3(this)
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || o.javaClass != this.javaClass) return false
        val g = o as GridPoint3
        return x == g.x && y == g.y && z == g.z
    }

    override fun hashCode(): Int {
        val prime = 17
        var result = 1
        result = prime * result + x
        result = prime * result + y
        result = prime * result + z
        return result
    }

    override fun toString(): String {
        return "($x, $y, $z)"
    }

    companion object {
        private const val serialVersionUID = 5922187982746752830L
    }
}
