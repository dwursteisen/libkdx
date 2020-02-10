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

class PointLight : com.badlogic.gdx.graphics.g3d.environment.BaseLight<PointLight?>() {
    @JvmField
    val position: com.badlogic.gdx.math.Vector3 = com.badlogic.gdx.math.Vector3()
    @JvmField
    var intensity = 0f
    fun setPosition(positionX: Float, positionY: Float, positionZ: Float): PointLight {
        position.set(positionX, positionY, positionZ)
        return this
    }

    fun setPosition(position: com.badlogic.gdx.math.Vector3?): PointLight {
        this.position.set(position)
        return this
    }

    fun setIntensity(intensity: Float): PointLight {
        this.intensity = intensity
        return this
    }

    fun set(copyFrom: PointLight): PointLight {
        return set(copyFrom.color, copyFrom.position, copyFrom.intensity)
    }

    operator fun set(color: com.badlogic.gdx.graphics.Color?, position: com.badlogic.gdx.math.Vector3?, intensity: Float): PointLight {
        if (color != null) color.set(color)
        if (position != null) this.position.set(position)
        this.intensity = intensity
        return this
    }

    operator fun set(r: Float, g: Float, b: Float, position: com.badlogic.gdx.math.Vector3?, intensity: Float): PointLight {
        this.color.set(r, g, b, 1f)
        if (position != null) this.position.set(position)
        this.intensity = intensity
        return this
    }

    operator fun set(color: com.badlogic.gdx.graphics.Color?, x: Float, y: Float, z: Float, intensity: Float): PointLight {
        if (color != null) color.set(color)
        position.set(x, y, z)
        this.intensity = intensity
        return this
    }

    operator fun set(r: Float, g: Float, b: Float, x: Float, y: Float, z: Float,
                     intensity: Float): PointLight {
        this.color.set(r, g, b, 1f)
        position.set(x, y, z)
        this.intensity = intensity
        return this
    }

    override fun equals(obj: Any?): Boolean {
        return obj is PointLight && equals(obj as PointLight?)
    }

    fun equals(other: PointLight?): Boolean {
        return other != null && (other === this || color == other.color && position == other.position && intensity == other.intensity)
    }
}
