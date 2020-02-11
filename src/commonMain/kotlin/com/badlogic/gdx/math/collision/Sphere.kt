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

import com.badlogic.gdx.math.collision.Ray

/**
 * Encapsulates a 3D sphere with a center and a radius
 *
 * @author badlogicgames@gmail.com
 */
class Sphere(center: Vector3?, radius: Float) : Serializable {

    /**
     * the radius of the sphere
     */
    var radius: Float

    /**
     * the center of the sphere
     */
    val center: Vector3

    /**
     * @param sphere the other sphere
     * @return whether this and the other sphere overlap
     */
    fun overlaps(sphere: Sphere): Boolean {
        return center.dst2(sphere.center) < (radius + sphere.radius) * (radius + sphere.radius)
    }

    override fun hashCode(): Int {
        val prime = 71
        var result = 1
        result = prime * result + center.hashCode()
        result = prime * result + NumberUtils.floatToRawIntBits(radius)
        return result
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || o.javaClass != this.javaClass) return false
        val s = o as Sphere
        return radius == s.radius && center.equals(s.center)
    }

    fun volume(): Float {
        return PI_4_3 * radius * radius * radius
    }

    fun surfaceArea(): Float {
        return 4 * MathUtils.PI * radius * radius
    }

    companion object {
        private const val serialVersionUID = -6487336868908521596L
        private val PI_4_3: Float = MathUtils.PI * 4f / 3f
    }

    /**
     * Constructs a sphere with the given center and radius
     *
     * @param center The center
     * @param radius The radius
     */
    init {
        this.center = Vector3(center)
        this.radius = radius
    }
}
