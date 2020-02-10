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
import kotlin.jvm.JvmStatic

class AmbientCubemap {
    @JvmField
    val data: FloatArray

    constructor() {
        data = FloatArray(NUM_VALUES)
    }

    constructor(copyFrom: FloatArray) {
        if (copyFrom.size != NUM_VALUES) throw com.badlogic.gdx.utils.GdxRuntimeException("Incorrect array size")
        data = FloatArray(copyFrom.size)
        java.lang.System.arraycopy(copyFrom, 0, data, 0, data.size)
    }

    constructor(copyFrom: AmbientCubemap) : this(copyFrom.data) {}

    fun set(values: FloatArray): AmbientCubemap {
        for (i in data.indices) data[i] = values[i]
        return this
    }

    fun set(other: AmbientCubemap): AmbientCubemap {
        return set(other.data)
    }

    fun set(color: com.badlogic.gdx.graphics.Color): AmbientCubemap {
        return set(color.r, color.g, color.b)
    }

    operator fun set(r: Float, g: Float, b: Float): AmbientCubemap {
        var idx = 0
        while (idx < NUM_VALUES) {
            data[idx] = r
            data[idx + 1] = g
            data[idx + 2] = b
            idx += 3
        }
        return this
    }

    fun getColor(out: com.badlogic.gdx.graphics.Color, side: Int): com.badlogic.gdx.graphics.Color {
        var side = side
        side *= 3
        return out.set(data[side], data[side + 1], data[side + 2], 1f)
    }

    fun clear(): AmbientCubemap {
        for (i in data.indices) data[i] = 0f
        return this
    }

    fun clamp(): AmbientCubemap {
        for (i in data.indices) data[i] = clamp(data[i])
        return this
    }

    fun add(r: Float, g: Float, b: Float): AmbientCubemap {
        var idx = 0
        while (idx < data.size) {
            data[idx++] += r
            data[idx++] += g
            data[idx++] += b
        }
        return this
    }

    fun add(color: com.badlogic.gdx.graphics.Color): AmbientCubemap {
        return add(color.r, color.g, color.b)
    }

    fun add(r: Float, g: Float, b: Float, x: Float, y: Float, z: Float): AmbientCubemap {
        val x2 = x * x
        val y2 = y * y
        val z2 = z * z
        var d = x2 + y2 + z2
        if (d == 0f) return this
        d = 1f / d * (d + 1f)
        val rd = r * d
        val gd = g * d
        val bd = b * d
        var idx = if (x > 0) 0 else 3
        data[idx] += x2 * rd
        data[idx + 1] += x2 * gd
        data[idx + 2] += x2 * bd
        idx = if (y > 0) 6 else 9
        data[idx] += y2 * rd
        data[idx + 1] += y2 * gd
        data[idx + 2] += y2 * bd
        idx = if (z > 0) 12 else 15
        data[idx] += z2 * rd
        data[idx + 1] += z2 * gd
        data[idx + 2] += z2 * bd
        return this
    }

    fun add(color: com.badlogic.gdx.graphics.Color, direction: com.badlogic.gdx.math.Vector3): AmbientCubemap {
        return add(color.r, color.g, color.b, direction.x, direction.y, direction.z)
    }

    fun add(r: Float, g: Float, b: Float, direction: com.badlogic.gdx.math.Vector3): AmbientCubemap {
        return add(r, g, b, direction.x, direction.y, direction.z)
    }

    fun add(color: com.badlogic.gdx.graphics.Color, x: Float, y: Float, z: Float): AmbientCubemap {
        return add(color.r, color.g, color.b, x, y, z)
    }

    fun add(color: com.badlogic.gdx.graphics.Color, point: com.badlogic.gdx.math.Vector3, target: com.badlogic.gdx.math.Vector3): AmbientCubemap {
        return add(color.r, color.g, color.b, target.x - point.x, target.y - point.y, target.z - point.z)
    }

    fun add(color: com.badlogic.gdx.graphics.Color, point: com.badlogic.gdx.math.Vector3, target: com.badlogic.gdx.math.Vector3, intensity: Float): AmbientCubemap {
        val t: Float = intensity / (1f + target.dst(point))
        return add(color.r * t, color.g * t, color.b * t, target.x - point.x, target.y - point.y, target.z - point.z)
    }

    override fun toString(): String {
        var result = ""
        var i = 0
        while (i < data.size) {
            result += java.lang.Float.toString(data[i]) + ", " + java.lang.Float.toString(data[i + 1]) + ", " + java.lang.Float.toString(data[i + 2]) + "\n"
            i += 3
        }
        return result
    }

    companion object {
        private const val NUM_VALUES = 6 * 3
        @JvmStatic
        private fun clamp(v: Float): Float {
            return if (v < 0f) 0f else if (v > 1f) 1f else v
        }
    }
}
