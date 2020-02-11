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
import com.badlogic.gdx.math.RandomXS128
import java.lang.RuntimeException

/**
 * A plane defined via a unit length normal and the distance from the origin, as you learned in your math class.
 *
 * @author badlogicgames@gmail.com
 */
class Plane : Serializable {

    /**
     * Enum specifying on which side a point lies respective to the plane and it's normal. [PlaneSide.Front] is the side to
     * which the normal points.
     *
     * @author mzechner
     */
    enum class PlaneSide {

        OnPlane, Back, Front
    }

    /**
     * @return The normal
     */
    val normal = Vector3()

    /**
     * @return The distance to the origin
     */
    var d = 0f

    /**
     * Constructs a new plane with all values set to 0
     */
    constructor() {}

    /**
     * Constructs a new plane based on the normal and distance to the origin.
     *
     * @param normal The plane normal
     * @param d      The distance to the origin
     */
    constructor(normal: Vector3?, d: Float) {
        this.normal.set(normal)!!.nor()
        this.d = d
    }

    /**
     * Constructs a new plane based on the normal and a point on the plane.
     *
     * @param normal The normal
     * @param point  The point on the plane
     */
    constructor(normal: Vector3?, point: Vector3?) {
        this.normal.set(normal)!!.nor()
        d = -this.normal.dot(point)
    }

    /**
     * Constructs a new plane out of the three given points that are considered to be on the plane. The normal is calculated via a
     * cross product between (point1-point2)x(point2-point3)
     *
     * @param point1 The first point
     * @param point2 The second point
     * @param point3 The third point
     */
    constructor(point1: Vector3, point2: Vector3, point3: Vector3) {
        set(point1, point2, point3)
    }

    /**
     * Sets the plane normal and distance to the origin based on the three given points which are considered to be on the plane.
     * The normal is calculated via a cross product between (point1-point2)x(point2-point3)
     *
     * @param point1
     * @param point2
     * @param point3
     */
    operator fun set(point1: Vector3, point2: Vector3, point3: Vector3) {
        normal.set(point1).sub(point2).crs(point2.x - point3.x, point2.y - point3.y, point2.z - point3.z).nor()
        d = -point1.dot(normal)
    }

    /**
     * Sets the plane normal and distance
     *
     * @param nx normal x-component
     * @param ny normal y-component
     * @param nz normal z-component
     * @param d  distance to origin
     */
    operator fun set(nx: Float, ny: Float, nz: Float, d: Float) {
        normal[nx, ny] = nz
        this.d = d
    }

    /**
     * Calculates the shortest signed distance between the plane and the given point.
     *
     * @param point The point
     * @return the shortest signed distance between the plane and the point
     */
    fun distance(point: Vector3?): Float {
        return normal.dot(point) + d
    }

    /**
     * Returns on which side the given point lies relative to the plane and its normal. PlaneSide.Front refers to the side the
     * plane normal points to.
     *
     * @param point The point
     * @return The side the point lies relative to the plane
     */
    fun testPoint(point: Vector3?): PlaneSide {
        val dist = normal.dot(point) + d
        return if (dist == 0f) PlaneSide.OnPlane else if (dist < 0) PlaneSide.Back else PlaneSide.Front
    }

    /**
     * Returns on which side the given point lies relative to the plane and its normal. PlaneSide.Front refers to the side the
     * plane normal points to.
     *
     * @param x
     * @param y
     * @param z
     * @return The side the point lies relative to the plane
     */
    fun testPoint(x: Float, y: Float, z: Float): PlaneSide {
        val dist = normal.dot(x, y, z) + d
        return if (dist == 0f) PlaneSide.OnPlane else if (dist < 0) PlaneSide.Back else PlaneSide.Front
    }

    /**
     * Returns whether the plane is facing the direction vector. Think of the direction vector as the direction a camera looks in.
     * This method will return true if the front side of the plane determined by its normal faces the camera.
     *
     * @param direction the direction
     * @return whether the plane is front facing
     */
    fun isFrontFacing(direction: Vector3?): Boolean {
        val dot = normal.dot(direction)
        return dot <= 0
    }

    /**
     * Sets the plane to the given point and normal.
     *
     * @param point  the point on the plane
     * @param normal the normal of the plane
     */
    operator fun set(point: Vector3, normal: Vector3?) {
        this.normal.set(normal)
        d = -point.dot(normal)
    }

    operator fun set(pointX: Float, pointY: Float, pointZ: Float, norX: Float, norY: Float, norZ: Float) {
        normal[norX, norY] = norZ
        d = -(pointX * norX + pointY * norY + pointZ * norZ)
    }

    /**
     * Sets this plane from the given plane
     *
     * @param plane the plane
     */
    fun set(plane: Plane) {
        normal.set(plane.normal)
        d = plane.d
    }

    override fun toString(): String {
        return "$normal, $d"
    }

    companion object {
        private const val serialVersionUID = -1240652082930747866L
    }
}
