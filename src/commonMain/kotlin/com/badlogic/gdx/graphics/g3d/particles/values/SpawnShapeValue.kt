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

/** Encapsulate the formulas to spawn a particle on a shape.
 * @author Inferno
 */
abstract class SpawnShapeValue() : com.badlogic.gdx.graphics.g3d.particles.values.ParticleValue(), com.badlogic.gdx.graphics.g3d.particles.ResourceData.Configurable<Any?>, com.badlogic.gdx.utils.Json.Serializable {

    var xOffsetValue: com.badlogic.gdx.graphics.g3d.particles.values.RangedNumericValue
    var yOffsetValue: com.badlogic.gdx.graphics.g3d.particles.values.RangedNumericValue
    var zOffsetValue: com.badlogic.gdx.graphics.g3d.particles.values.RangedNumericValue

    constructor(spawnShapeValue: SpawnShapeValue?) : this() {}

    abstract fun spawnAux(vector: com.badlogic.gdx.math.Vector3, percent: Float)
    fun spawn(vector: com.badlogic.gdx.math.Vector3, percent: Float): com.badlogic.gdx.math.Vector3 {
        spawnAux(vector, percent)
        if (xOffsetValue.active) vector.x += xOffsetValue.newLowValue()
        if (yOffsetValue.active) vector.y += yOffsetValue.newLowValue()
        if (zOffsetValue.active) vector.z += zOffsetValue.newLowValue()
        return vector
    }

    open fun init() {}
    open fun start() {}
    override fun load(value: com.badlogic.gdx.graphics.g3d.particles.values.ParticleValue) {
        super.load(value)
        val shape = value as SpawnShapeValue
        xOffsetValue.load(shape.xOffsetValue)
        yOffsetValue.load(shape.yOffsetValue)
        zOffsetValue.load(shape.zOffsetValue)
    }

    abstract fun copy(): SpawnShapeValue
    override fun write(json: com.badlogic.gdx.utils.Json) {
        super.write(json)
        json.writeValue("xOffsetValue", xOffsetValue)
        json.writeValue("yOffsetValue", yOffsetValue)
        json.writeValue("zOffsetValue", zOffsetValue)
    }

    override fun read(json: com.badlogic.gdx.utils.Json, jsonData: com.badlogic.gdx.utils.JsonValue) {
        super.read(json, jsonData)
        xOffsetValue = json.readValue("xOffsetValue", com.badlogic.gdx.graphics.g3d.particles.values.RangedNumericValue::class.java, jsonData)
        yOffsetValue = json.readValue("yOffsetValue", com.badlogic.gdx.graphics.g3d.particles.values.RangedNumericValue::class.java, jsonData)
        zOffsetValue = json.readValue("zOffsetValue", com.badlogic.gdx.graphics.g3d.particles.values.RangedNumericValue::class.java, jsonData)
    }

    override fun save(manager: com.badlogic.gdx.assets.AssetManager, data: com.badlogic.gdx.graphics.g3d.particles.ResourceData<*>?) {}
    override fun load(manager: com.badlogic.gdx.assets.AssetManager, data: com.badlogic.gdx.graphics.g3d.particles.ResourceData<*>?) {}

    init {
        xOffsetValue = com.badlogic.gdx.graphics.g3d.particles.values.RangedNumericValue()
        yOffsetValue = com.badlogic.gdx.graphics.g3d.particles.values.RangedNumericValue()
        zOffsetValue = com.badlogic.gdx.graphics.g3d.particles.values.RangedNumericValue()
    }
}
