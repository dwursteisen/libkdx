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

/** Note that the default shader doesn't support spot lights, you'll have to supply your own shader to use this class.
 * @author realitix
 */
class SpotLight : com.badlogic.gdx.graphics.g3d.environment.BaseLight<SpotLight?>() {

    @JvmField
    val position: com.badlogic.gdx.math.Vector3 = com.badlogic.gdx.math.Vector3()
    @JvmField
    val direction: com.badlogic.gdx.math.Vector3 = com.badlogic.gdx.math.Vector3()
    @JvmField
    var intensity = 0f
    @JvmField
    var cutoffAngle = 0f
    @JvmField
    var exponent = 0f
    fun setPosition(positionX: Float, positionY: Float, positionZ: Float): SpotLight {
        position.set(positionX, positionY, positionZ)
        return this
    }

    fun setPosition(position: com.badlogic.gdx.math.Vector3?): SpotLight {
        this.position.set(position)
        return this
    }

    fun setDirection(directionX: Float, directionY: Float, directionZ: Float): SpotLight {
        direction.set(directionX, directionY, directionZ)
        return this
    }

    fun setDirection(direction: com.badlogic.gdx.math.Vector3?): SpotLight {
        this.direction.set(direction)
        return this
    }

    fun setIntensity(intensity: Float): SpotLight {
        this.intensity = intensity
        return this
    }

    fun setCutoffAngle(cutoffAngle: Float): SpotLight {
        this.cutoffAngle = cutoffAngle
        return this
    }

    fun setExponent(exponent: Float): SpotLight {
        this.exponent = exponent
        return this
    }

    fun set(copyFrom: SpotLight): SpotLight {
        return set(copyFrom.color, copyFrom.position, copyFrom.direction, copyFrom.intensity, copyFrom.cutoffAngle, copyFrom.exponent)
    }

    operator fun set(color: com.badlogic.gdx.graphics.Color?, position: com.badlogic.gdx.math.Vector3?, direction: com.badlogic.gdx.math.Vector3?, intensity: Float,
                     cutoffAngle: Float, exponent: Float): SpotLight {
        if (color != null) color.set(color)
        if (position != null) this.position.set(position)
        if (direction != null) this.direction.set(direction).nor()
        this.intensity = intensity
        this.cutoffAngle = cutoffAngle
        this.exponent = exponent
        return this
    }

    operator fun set(r: Float, g: Float, b: Float, position: com.badlogic.gdx.math.Vector3?, direction: com.badlogic.gdx.math.Vector3?,
                     intensity: Float, cutoffAngle: Float, exponent: Float): SpotLight {
        this.color.set(r, g, b, 1f)
        if (position != null) this.position.set(position)
        if (direction != null) this.direction.set(direction).nor()
        this.intensity = intensity
        this.cutoffAngle = cutoffAngle
        this.exponent = exponent
        return this
    }

    operator fun set(color: com.badlogic.gdx.graphics.Color?, posX: Float, posY: Float, posZ: Float, dirX: Float,
                     dirY: Float, dirZ: Float, intensity: Float, cutoffAngle: Float, exponent: Float): SpotLight {
        if (color != null) color.set(color)
        position.set(posX, posY, posZ)
        direction.set(dirX, dirY, dirZ).nor()
        this.intensity = intensity
        this.cutoffAngle = cutoffAngle
        this.exponent = exponent
        return this
    }

    operator fun set(r: Float, g: Float, b: Float, posX: Float, posY: Float, posZ: Float,
                     dirX: Float, dirY: Float, dirZ: Float, intensity: Float, cutoffAngle: Float, exponent: Float): SpotLight {
        this.color.set(r, g, b, 1f)
        position.set(posX, posY, posZ)
        direction.set(dirX, dirY, dirZ).nor()
        this.intensity = intensity
        this.cutoffAngle = cutoffAngle
        this.exponent = exponent
        return this
    }

    fun setTarget(target: com.badlogic.gdx.math.Vector3?): SpotLight {
        direction.set(target).sub(position).nor()
        return this
    }

    override fun equals(obj: Any?): Boolean {
        return obj is SpotLight && equals(obj as SpotLight?)
    }

    fun equals(other: SpotLight?): Boolean {
        return other != null && (other === this || color == other.color && position == other.position && direction == other.direction && com.badlogic.gdx.math.MathUtils.isEqual(intensity, other.intensity) && com.badlogic.gdx.math.MathUtils.isEqual(cutoffAngle,
            other.cutoffAngle) && com.badlogic.gdx.math.MathUtils.isEqual(exponent, other.exponent))
    }
}
