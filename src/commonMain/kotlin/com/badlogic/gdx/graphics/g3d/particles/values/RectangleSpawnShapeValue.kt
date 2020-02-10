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

/** Encapsulate the formulas to spawn a particle on a rectangle shape.
 * @author Inferno
 */
class RectangleSpawnShapeValue : com.badlogic.gdx.graphics.g3d.particles.values.PrimitiveSpawnShapeValue {

    constructor(value: RectangleSpawnShapeValue?) : super(value) {
        load(value!!)
    }

    constructor() {}

    override fun spawnAux(vector: com.badlogic.gdx.math.Vector3, percent: Float) {
        val width: Float = spawnWidth + spawnWidthDiff * spawnWidthValue.getScale(percent)
        val height: Float = spawnHeight + spawnHeightDiff * spawnHeightValue.getScale(percent)
        val depth: Float = spawnDepth + spawnDepthDiff * spawnDepthValue.getScale(percent)
        // Where generate the point, on edges or inside ?
        if (edges) {
            val a: Int = com.badlogic.gdx.math.MathUtils.random(-1, 1)
            var tx = 0f
            var ty = 0f
            var tz = 0f
            if (a == -1) {
                tx = if (com.badlogic.gdx.math.MathUtils.random(1) == 0) -width / 2 else width / 2
                if (tx == 0f) {
                    ty = if (com.badlogic.gdx.math.MathUtils.random(1) == 0) -height / 2 else height / 2
                    tz = if (com.badlogic.gdx.math.MathUtils.random(1) == 0) -depth / 2 else depth / 2
                } else {
                    ty = com.badlogic.gdx.math.MathUtils.random(height) - height / 2
                    tz = com.badlogic.gdx.math.MathUtils.random(depth) - depth / 2
                }
            } else if (a == 0) { // Z
                tz = if (com.badlogic.gdx.math.MathUtils.random(1) == 0) -depth / 2 else depth / 2
                if (tz == 0f) {
                    ty = if (com.badlogic.gdx.math.MathUtils.random(1) == 0) -height / 2 else height / 2
                    tx = if (com.badlogic.gdx.math.MathUtils.random(1) == 0) -width / 2 else width / 2
                } else {
                    ty = com.badlogic.gdx.math.MathUtils.random(height) - height / 2
                    tx = com.badlogic.gdx.math.MathUtils.random(width) - width / 2
                }
            } else { // Y
                ty = if (com.badlogic.gdx.math.MathUtils.random(1) == 0) -height / 2 else height / 2
                if (ty == 0f) {
                    tx = if (com.badlogic.gdx.math.MathUtils.random(1) == 0) -width / 2 else width / 2
                    tz = if (com.badlogic.gdx.math.MathUtils.random(1) == 0) -depth / 2 else depth / 2
                } else {
                    tx = com.badlogic.gdx.math.MathUtils.random(width) - width / 2
                    tz = com.badlogic.gdx.math.MathUtils.random(depth) - depth / 2
                }
            }
            vector.x = tx
            vector.y = ty
            vector.z = tz
        } else {
            vector.x = com.badlogic.gdx.math.MathUtils.random(width) - width / 2
            vector.y = com.badlogic.gdx.math.MathUtils.random(height) - height / 2
            vector.z = com.badlogic.gdx.math.MathUtils.random(depth) - depth / 2
        }
    }

    override fun copy(): com.badlogic.gdx.graphics.g3d.particles.values.SpawnShapeValue {
        return RectangleSpawnShapeValue(this)
    }
}
