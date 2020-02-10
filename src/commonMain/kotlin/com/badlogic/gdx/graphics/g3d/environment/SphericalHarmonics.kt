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

class SphericalHarmonics {
    val data: FloatArray

    constructor() {
        data = FloatArray(9 * 3)
    }

    constructor(copyFrom: FloatArray) {
        if (copyFrom.size != 9 * 3) throw com.badlogic.gdx.utils.GdxRuntimeException("Incorrect array size")
        data = copyFrom.clone()
    }

    fun set(values: FloatArray): SphericalHarmonics {
        for (i in data.indices) data[i] = values[i]
        return this
    }

    fun set(other: com.badlogic.gdx.graphics.g3d.environment.AmbientCubemap): SphericalHarmonics {
        return set(other.data)
    }

    fun set(color: com.badlogic.gdx.graphics.Color): SphericalHarmonics {
        return set(color.r, color.g, color.b)
    }

    operator fun set(r: Float, g: Float, b: Float): SphericalHarmonics {
        var idx = 0
        while (idx < data.size) {
            data[idx++] = r
            data[idx++] = g
            data[idx++] = b
        }
        return this
    }

    companion object {
        // <kalle_h> last term is no x*x * y*y but x*x - y*y
        private val coeff = floatArrayOf(0.282095f, 0.488603f, 0.488603f, 0.488603f, 1.092548f, 1.092548f, 1.092548f, 0.315392f,
            0.546274f)

        private fun clamp(v: Float): Float {
            return if (v < 0f) 0f else if (v > 1f) 1f else v
        }
    }
}
