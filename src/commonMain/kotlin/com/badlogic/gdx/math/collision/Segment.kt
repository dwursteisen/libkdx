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
 * A Segment is a line in 3-space having a staring and an ending position.
 *
 * @author mzechner
 */
class Segment : Serializable {

    /**
     * the starting position
     */
    val a: Vector3 = Vector3()

    /**
     * the ending position
     */
    val b: Vector3 = Vector3()

    /**
     * Constructs a new Segment from the two points given.
     *
     * @param a the first point
     * @param b the second point
     */
    constructor(a: Vector3?, b: Vector3?) {
        this.a.set(a)
        this.b.set(b)
    }

    /**
     * Constructs a new Segment from the two points given.
     *
     * @param aX the x-coordinate of the first point
     * @param aY the y-coordinate of the first point
     * @param aZ the z-coordinate of the first point
     * @param bX the x-coordinate of the second point
     * @param bY the y-coordinate of the second point
     * @param bZ the z-coordinate of the second point
     */
    constructor(aX: Float, aY: Float, aZ: Float, bX: Float, bY: Float, bZ: Float) {
        a.set(aX, aY, aZ)
        b.set(bX, bY, bZ)
    }

    fun len(): Float {
        return a.dst(b)
    }

    fun len2(): Float {
        return a.dst2(b)
    }

    override fun equals(o: Any?): Boolean {
        if (o === this) return true
        if (o == null || o.javaClass != this.javaClass) return false
        val s = o as Segment
        return a.equals(s.a) && b.equals(s.b)
    }

    override fun hashCode(): Int {
        val prime = 71
        var result = 1
        result = prime * result + a.hashCode()
        result = prime * result + b.hashCode()
        return result
    }

    companion object {
        private const val serialVersionUID = 2739667069736519602L
    }
}
