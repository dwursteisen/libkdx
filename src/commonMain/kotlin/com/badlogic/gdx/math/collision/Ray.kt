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
package com.badlogic.gdx.math.collision

/**
 * Encapsulates a ray having a starting position and a unit length direction.
 *
 * @author badlogicgames@gmail.com
 */
class Ray : Serializable {

    val origin: Vector3 = Vector3()
    val direction: Vector3 = Vector3()

    constructor() {}

    /**
     * Constructor, sets the starting position of the ray and the direction.
     *
     * @param origin    The starting position
     * @param direction The direction
     */
    constructor(origin: Vector3?, direction: Vector3?) {
        this.origin.set(origin)
        this.direction.set(direction).nor()
    }

    /**
     * @return a copy of this ray.
     */
    fun cpy(): Ray {
        return Ray(origin, direction)
    }

    /**
     * Returns the endpoint given the distance. This is calculated as startpoint + distance * direction.
     *
     * @param out      The vector to set to the result
     * @param distance The distance from the end point to the start point.
     * @return The out param
     */
    fun getEndPoint(out: Vector3, distance: Float): Vector3 {
        return out.set(direction).scl(distance).add(origin)
    }

    /**
     * Multiplies the ray by the given matrix. Use this to transform a ray into another coordinate system.
     *
     * @param matrix The matrix
     * @return This ray for chaining.
     */
    fun mul(matrix: Matrix4?): Ray {
        tmp.set(origin).add(direction)
        tmp.mul(matrix)
        origin.mul(matrix)
        direction.set(tmp.sub(origin))
        return this
    }

    /**
     * {@inheritDoc}
     */
    override fun toString(): String {
        return "ray [$origin:$direction]"
    }

    /**
     * Sets the starting position and the direction of this ray.
     *
     * @param origin    The starting position
     * @param direction The direction
     * @return this ray for chaining
     */
    operator fun set(origin: Vector3?, direction: Vector3?): Ray {
        this.origin.set(origin)
        this.direction.set(direction)
        return this
    }

    /**
     * Sets this ray from the given starting position and direction.
     *
     * @param x  The x-component of the starting position
     * @param y  The y-component of the starting position
     * @param z  The z-component of the starting position
     * @param dx The x-component of the direction
     * @param dy The y-component of the direction
     * @param dz The z-component of the direction
     * @return this ray for chaining
     */
    operator fun set(x: Float, y: Float, z: Float, dx: Float, dy: Float, dz: Float): Ray {
        origin.set(x, y, z)
        direction.set(dx, dy, dz)
        return this
    }

    /**
     * Sets the starting position and direction from the given ray
     *
     * @param ray The ray
     * @return This ray for chaining
     */
    fun set(ray: Ray): Ray {
        origin.set(ray.origin)
        direction.set(ray.direction)
        return this
    }

    override fun equals(o: Any?): Boolean {
        if (o === this) return true
        if (o == null || o.javaClass != this.javaClass) return false
        val r = o as Ray
        return direction.equals(r.direction) && origin.equals(r.origin)
    }

    override fun hashCode(): Int {
        val prime = 73
        var result = 1
        result = prime * result + direction.hashCode()
        result = prime * result + origin.hashCode()
        return result
    }

    companion object {
        private const val serialVersionUID = -620692054835390878L
        var tmp: Vector3 = Vector3()
    }
}
