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
package com.badlogic.gdx.graphics.g3d.particles.values

/** Defines a variation of red, green and blue on a given time line.
 * @author Inferno
 */
class GradientColorValue : com.badlogic.gdx.graphics.g3d.particles.values.ParticleValue() {

    var colors = floatArrayOf(1f, 1f, 1f)
    var timeline = floatArrayOf(0f)

    fun getColor(percent: Float): FloatArray {
        getColor(percent, temp, 0)
        return temp
    }

    fun getColor(percent: Float, out: FloatArray, index: Int) {
        var startIndex = 0
        var endIndex = -1
        val timeline = timeline
        val n = timeline.size
        for (i in 1 until n) {
            val t = timeline[i]
            if (t > percent) {
                endIndex = i
                break
            }
            startIndex = i
        }
        val startTime = timeline[startIndex]
        startIndex *= 3
        val r1 = colors[startIndex]
        val g1 = colors[startIndex + 1]
        val b1 = colors[startIndex + 2]
        if (endIndex == -1) {
            out[index] = r1
            out[index + 1] = g1
            out[index + 2] = b1
            return
        }
        val factor = (percent - startTime) / (timeline[endIndex] - startTime)
        endIndex *= 3
        out[index] = r1 + (colors[endIndex] - r1) * factor
        out[index + 1] = g1 + (colors[endIndex + 1] - g1) * factor
        out[index + 2] = b1 + (colors[endIndex + 2] - b1) * factor
    }

    override fun write(json: com.badlogic.gdx.utils.Json) {
        super.write(json)
        json.writeValue("colors", colors)
        json.writeValue("timeline", timeline)
    }

    override fun read(json: com.badlogic.gdx.utils.Json, jsonData: com.badlogic.gdx.utils.JsonValue) {
        super.read(json, jsonData)
        colors = json.readValue("colors", FloatArray::class.java, jsonData)
        timeline = json.readValue("timeline", FloatArray::class.java, jsonData)
    }

    fun load(value: GradientColorValue) {
        super.load(value)
        colors = FloatArray(value.colors.size)
        java.lang.System.arraycopy(value.colors, 0, colors, 0, colors.size)
        timeline = FloatArray(value.timeline.size)
        java.lang.System.arraycopy(value.timeline, 0, timeline, 0, timeline.size)
    }

    companion object {
        private val temp = FloatArray(3)
    }
}
