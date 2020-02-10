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

/** A value which has a defined minimum and maximum upper and lower bounds. Defines the variations of the value on a time line.
 * @author Inferno
 */
class ScaledNumericValue : com.badlogic.gdx.graphics.g3d.particles.values.RangedNumericValue() {

    var scaling = floatArrayOf(1f)
    var timeline = floatArrayOf(0f)
    var highMin = 0f
    var highMax = 0f
    var isRelative = false
    fun newHighValue(): Float {
        return highMin + (highMax - highMin) * com.badlogic.gdx.math.MathUtils.random()
    }

    fun setHigh(value: Float) {
        highMin = value
        highMax = value
    }

    fun setHigh(min: Float, max: Float) {
        highMin = min
        highMax = max
    }

    fun getScale(percent: Float): Float {
        var endIndex = -1
        val n = timeline.size
        // if (percent >= timeline[n-1])
// return scaling[n - 1];
        for (i in 1 until n) {
            val t = timeline[i]
            if (t > percent) {
                endIndex = i
                break
            }
        }
        if (endIndex == -1) return scaling[n - 1]
        val startIndex = endIndex - 1
        val startValue = scaling[startIndex]
        val startTime = timeline[startIndex]
        return startValue + (scaling[endIndex] - startValue) * ((percent - startTime) / (timeline[endIndex] - startTime))
    }

    fun load(value: ScaledNumericValue) {
        super.load(value)
        highMax = value.highMax
        highMin = value.highMin
        scaling = FloatArray(value.scaling.size)
        java.lang.System.arraycopy(value.scaling, 0, scaling, 0, scaling.size)
        timeline = FloatArray(value.timeline.size)
        java.lang.System.arraycopy(value.timeline, 0, timeline, 0, timeline.size)
        isRelative = value.isRelative
    }

    override fun write(json: com.badlogic.gdx.utils.Json) {
        super.write(json)
        json.writeValue("highMin", highMin)
        json.writeValue("highMax", highMax)
        json.writeValue("relative", isRelative)
        json.writeValue("scaling", scaling)
        json.writeValue("timeline", timeline)
    }

    override fun read(json: com.badlogic.gdx.utils.Json, jsonData: com.badlogic.gdx.utils.JsonValue) {
        super.read(json, jsonData)
        highMin = json.readValue("highMin", Float::class.javaPrimitiveType, jsonData)
        highMax = json.readValue("highMax", Float::class.javaPrimitiveType, jsonData)
        isRelative = json.readValue("relative", Boolean::class.javaPrimitiveType, jsonData)
        scaling = json.readValue("scaling", FloatArray::class.java, jsonData)
        timeline = json.readValue("timeline", FloatArray::class.java, jsonData)
    }
}
