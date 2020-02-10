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
package com.badlogic.gdx.graphics.g3d.environment

import kotlin.jvm.JvmField

open class DirectionalLight : com.badlogic.gdx.graphics.g3d.environment.BaseLight<DirectionalLight?>() {
    @JvmField
    val direction: com.badlogic.gdx.math.Vector3 = com.badlogic.gdx.math.Vector3()
    fun setDirection(directionX: Float, directionY: Float, directionZ: Float): DirectionalLight {
        direction.set(directionX, directionY, directionZ)
        return this
    }

    fun setDirection(direction: com.badlogic.gdx.math.Vector3?): DirectionalLight {
        this.direction.set(direction)
        return this
    }

    fun set(copyFrom: DirectionalLight): DirectionalLight {
        return set(copyFrom.color, copyFrom.direction)
    }

    operator fun set(color: com.badlogic.gdx.graphics.Color?, direction: com.badlogic.gdx.math.Vector3?): DirectionalLight {
        if (color != null) color.set(color)
        if (direction != null) this.direction.set(direction).nor()
        return this
    }

    operator fun set(r: Float, g: Float, b: Float, direction: com.badlogic.gdx.math.Vector3?): DirectionalLight {
        this.color.set(r, g, b, 1f)
        if (direction != null) this.direction.set(direction).nor()
        return this
    }

    operator fun set(color: com.badlogic.gdx.graphics.Color?, dirX: Float, dirY: Float, dirZ: Float): DirectionalLight {
        if (color != null) color.set(color)
        direction.set(dirX, dirY, dirZ).nor()
        return this
    }

    operator fun set(r: Float, g: Float, b: Float, dirX: Float, dirY: Float, dirZ: Float): DirectionalLight {
        this.color.set(r, g, b, 1f)
        direction.set(dirX, dirY, dirZ).nor()
        return this
    }

    override fun equals(arg0: Any?): Boolean {
        return arg0 is DirectionalLight && equals(arg0 as DirectionalLight?)
    }

    fun equals(other: DirectionalLight?): Boolean {
        return other != null && (other === this || color == other.color && direction == other.direction)
    }
}
