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

/** The base class of all the [SpawnShapeValue] values which spawn the particles on a geometric primitive.
 * @author Inferno
 */
abstract class PrimitiveSpawnShapeValue : com.badlogic.gdx.graphics.g3d.particles.values.SpawnShapeValue {

    enum class SpawnSide {
        both, top, bottom
    }

    var spawnWidthValue: com.badlogic.gdx.graphics.g3d.particles.values.ScaledNumericValue
    var spawnHeightValue: com.badlogic.gdx.graphics.g3d.particles.values.ScaledNumericValue
    var spawnDepthValue: com.badlogic.gdx.graphics.g3d.particles.values.ScaledNumericValue
    protected var spawnWidth = 0f
    protected var spawnWidthDiff = 0f
    protected var spawnHeight = 0f
    protected var spawnHeightDiff = 0f
    protected var spawnDepth = 0f
    protected var spawnDepthDiff = 0f
    var isEdges = false

    constructor() {
        spawnWidthValue = com.badlogic.gdx.graphics.g3d.particles.values.ScaledNumericValue()
        spawnHeightValue = com.badlogic.gdx.graphics.g3d.particles.values.ScaledNumericValue()
        spawnDepthValue = com.badlogic.gdx.graphics.g3d.particles.values.ScaledNumericValue()
    }

    constructor(value: PrimitiveSpawnShapeValue?) : super(value) {
        spawnWidthValue = com.badlogic.gdx.graphics.g3d.particles.values.ScaledNumericValue()
        spawnHeightValue = com.badlogic.gdx.graphics.g3d.particles.values.ScaledNumericValue()
        spawnDepthValue = com.badlogic.gdx.graphics.g3d.particles.values.ScaledNumericValue()
    }

    override fun setActive(active: Boolean) {
        super.setActive(active)
        spawnWidthValue.setActive(true)
        spawnHeightValue.setActive(true)
        spawnDepthValue.setActive(true)
    }

    fun getSpawnWidth(): com.badlogic.gdx.graphics.g3d.particles.values.ScaledNumericValue {
        return spawnWidthValue
    }

    fun getSpawnHeight(): com.badlogic.gdx.graphics.g3d.particles.values.ScaledNumericValue {
        return spawnHeightValue
    }

    fun getSpawnDepth(): com.badlogic.gdx.graphics.g3d.particles.values.ScaledNumericValue {
        return spawnDepthValue
    }

    fun setDimensions(width: Float, height: Float, depth: Float) {
        spawnWidthValue.setHigh(width)
        spawnHeightValue.setHigh(height)
        spawnDepthValue.setHigh(depth)
    }

    override fun start() {
        spawnWidth = spawnWidthValue.newLowValue()
        spawnWidthDiff = spawnWidthValue.newHighValue()
        if (!spawnWidthValue.isRelative()) spawnWidthDiff -= spawnWidth
        spawnHeight = spawnHeightValue.newLowValue()
        spawnHeightDiff = spawnHeightValue.newHighValue()
        if (!spawnHeightValue.isRelative()) spawnHeightDiff -= spawnHeight
        spawnDepth = spawnDepthValue.newLowValue()
        spawnDepthDiff = spawnDepthValue.newHighValue()
        if (!spawnDepthValue.isRelative()) spawnDepthDiff -= spawnDepth
    }

    override fun load(value: com.badlogic.gdx.graphics.g3d.particles.values.ParticleValue) {
        super.load(value)
        val shape = value as PrimitiveSpawnShapeValue
        isEdges = shape.isEdges
        spawnWidthValue.load(shape.spawnWidthValue)
        spawnHeightValue.load(shape.spawnHeightValue)
        spawnDepthValue.load(shape.spawnDepthValue)
    }

    override fun write(json: com.badlogic.gdx.utils.Json) {
        super.write(json)
        json.writeValue("spawnWidthValue", spawnWidthValue)
        json.writeValue("spawnHeightValue", spawnHeightValue)
        json.writeValue("spawnDepthValue", spawnDepthValue)
        json.writeValue("edges", isEdges)
    }

    override fun read(json: com.badlogic.gdx.utils.Json, jsonData: com.badlogic.gdx.utils.JsonValue) {
        super.read(json, jsonData)
        spawnWidthValue = json.readValue("spawnWidthValue", com.badlogic.gdx.graphics.g3d.particles.values.ScaledNumericValue::class.java, jsonData)
        spawnHeightValue = json.readValue("spawnHeightValue", com.badlogic.gdx.graphics.g3d.particles.values.ScaledNumericValue::class.java, jsonData)
        spawnDepthValue = json.readValue("spawnDepthValue", com.badlogic.gdx.graphics.g3d.particles.values.ScaledNumericValue::class.java, jsonData)
        isEdges = json.readValue("edges", Boolean::class.javaPrimitiveType, jsonData)
    }

    companion object {
        protected val TMP_V1: com.badlogic.gdx.math.Vector3 = com.badlogic.gdx.math.Vector3()
    }
}
