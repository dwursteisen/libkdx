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

/** Encapsulate the formulas to spawn a particle on a ellipse shape.
 * @author Inferno
 */
class EllipseSpawnShapeValue : com.badlogic.gdx.graphics.g3d.particles.values.PrimitiveSpawnShapeValue {

    var side: com.badlogic.gdx.graphics.g3d.particles.values.PrimitiveSpawnShapeValue.SpawnSide = com.badlogic.gdx.graphics.g3d.particles.values.PrimitiveSpawnShapeValue.SpawnSide.both

    constructor(value: EllipseSpawnShapeValue) : super(value) {
        load(value)
    }

    constructor() {}

    override fun spawnAux(vector: com.badlogic.gdx.math.Vector3, percent: Float) { // Generate the point on the surface of the sphere
        val width: Float = spawnWidth + spawnWidthDiff * spawnWidthValue.getScale(percent)
        val height: Float = spawnHeight + spawnHeightDiff * spawnHeightValue.getScale(percent)
        val depth: Float = spawnDepth + spawnDepthDiff * spawnDepthValue.getScale(percent)
        val radiusX: Float
        val radiusY: Float
        val radiusZ: Float
        // Where generate the point, on edges or inside ?
        val minT = 0f
        var maxT: Float = com.badlogic.gdx.math.MathUtils.PI2
        if (side == com.badlogic.gdx.graphics.g3d.particles.values.PrimitiveSpawnShapeValue.SpawnSide.top) {
            maxT = com.badlogic.gdx.math.MathUtils.PI
        } else if (side == com.badlogic.gdx.graphics.g3d.particles.values.PrimitiveSpawnShapeValue.SpawnSide.bottom) {
            maxT = -com.badlogic.gdx.math.MathUtils.PI
        }
        val t: Float = com.badlogic.gdx.math.MathUtils.random(minT, maxT)
        // Where generate the point, on edges or inside ?
        if (edges) {
            if (width == 0f) {
                vector.set(0f, height / 2 * com.badlogic.gdx.math.MathUtils.sin(t), depth / 2 * com.badlogic.gdx.math.MathUtils.cos(t))
                return
            }
            if (height == 0f) {
                vector.set(width / 2 * com.badlogic.gdx.math.MathUtils.cos(t), 0f, depth / 2 * com.badlogic.gdx.math.MathUtils.sin(t))
                return
            }
            if (depth == 0f) {
                vector.set(width / 2 * com.badlogic.gdx.math.MathUtils.cos(t), height / 2 * com.badlogic.gdx.math.MathUtils.sin(t), 0f)
                return
            }
            radiusX = width / 2
            radiusY = height / 2
            radiusZ = depth / 2
        } else {
            radiusX = com.badlogic.gdx.math.MathUtils.random(width / 2)
            radiusY = com.badlogic.gdx.math.MathUtils.random(height / 2)
            radiusZ = com.badlogic.gdx.math.MathUtils.random(depth / 2)
        }
        val z: Float = com.badlogic.gdx.math.MathUtils.random(-1f, 1f)
        val r = java.lang.Math.sqrt(1f - z * z.toDouble()) as Float
        vector.set(radiusX * r * com.badlogic.gdx.math.MathUtils.cos(t), radiusY * r * com.badlogic.gdx.math.MathUtils.sin(t), radiusZ * z)
    }

    fun getSide(): com.badlogic.gdx.graphics.g3d.particles.values.PrimitiveSpawnShapeValue.SpawnSide {
        return side
    }

    fun setSide(side: com.badlogic.gdx.graphics.g3d.particles.values.PrimitiveSpawnShapeValue.SpawnSide) {
        this.side = side
    }

    override fun load(value: com.badlogic.gdx.graphics.g3d.particles.values.ParticleValue) {
        super.load(value)
        val shape = value as EllipseSpawnShapeValue
        side = shape.side
    }

    override fun copy(): com.badlogic.gdx.graphics.g3d.particles.values.SpawnShapeValue {
        return EllipseSpawnShapeValue(this)
    }

    override fun write(json: com.badlogic.gdx.utils.Json) {
        super.write(json)
        json.writeValue("side", side)
    }

    override fun read(json: com.badlogic.gdx.utils.Json, jsonData: com.badlogic.gdx.utils.JsonValue) {
        super.read(json, jsonData)
        side = json.readValue("side", com.badlogic.gdx.graphics.g3d.particles.values.PrimitiveSpawnShapeValue.SpawnSide::class.java, jsonData)
    }
}
