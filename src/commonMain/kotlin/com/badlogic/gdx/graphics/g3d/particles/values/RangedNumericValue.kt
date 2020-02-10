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

/** A value which has a defined minimum and maximum bounds.
 * @author Inferno
 */
open class RangedNumericValue : com.badlogic.gdx.graphics.g3d.particles.values.ParticleValue() {

    var lowMin = 0f
    var lowMax = 0f
    fun newLowValue(): Float {
        return lowMin + (lowMax - lowMin) * com.badlogic.gdx.math.MathUtils.random()
    }

    fun setLow(value: Float) {
        lowMin = value
        lowMax = value
    }

    fun setLow(min: Float, max: Float) {
        lowMin = min
        lowMax = max
    }

    fun load(value: RangedNumericValue) {
        super.load(value)
        lowMax = value.lowMax
        lowMin = value.lowMin
    }

    override fun write(json: com.badlogic.gdx.utils.Json) {
        super.write(json)
        json.writeValue("lowMin", lowMin)
        json.writeValue("lowMax", lowMax)
    }

    override fun read(json: com.badlogic.gdx.utils.Json, jsonData: com.badlogic.gdx.utils.JsonValue) {
        super.read(json, jsonData)
        lowMin = json.readValue("lowMin", Float::class.javaPrimitiveType, jsonData)
        lowMax = json.readValue("lowMax", Float::class.javaPrimitiveType, jsonData)
    }
}
