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

/** Encapsulate the formulas to spawn a particle on a cylinder shape.
 * @author Inferno
 */
class CylinderSpawnShapeValue : com.badlogic.gdx.graphics.g3d.particles.values.PrimitiveSpawnShapeValue {

    constructor(cylinderSpawnShapeValue: CylinderSpawnShapeValue?) : super(cylinderSpawnShapeValue) {
        load(cylinderSpawnShapeValue!!)
    }

    constructor() {}

    override fun spawnAux(vector: com.badlogic.gdx.math.Vector3, percent: Float) { // Generate the point on the surface of the sphere
        val width: Float = spawnWidth + spawnWidthDiff * spawnWidthValue.getScale(percent)
        val height: Float = spawnHeight + spawnHeightDiff * spawnHeightValue.getScale(percent)
        val depth: Float = spawnDepth + spawnDepthDiff * spawnDepthValue.getScale(percent)
        val radiusX: Float
        val radiusZ: Float
        val hf = height / 2
        val ty: Float = com.badlogic.gdx.math.MathUtils.random(height) - hf
        // Where generate the point, on edges or inside ?
        if (edges) {
            radiusX = width / 2
            radiusZ = depth / 2
        } else {
            radiusX = com.badlogic.gdx.math.MathUtils.random(width) / 2
            radiusZ = com.badlogic.gdx.math.MathUtils.random(depth) / 2
        }
        var spawnTheta = 0f
        // Generate theta
        val isRadiusXZero = radiusX == 0f
        val isRadiusZZero = radiusZ == 0f
        if (!isRadiusXZero && !isRadiusZZero) spawnTheta = com.badlogic.gdx.math.MathUtils.random(360f) else {
            if (isRadiusXZero) spawnTheta = if (com.badlogic.gdx.math.MathUtils.random(1) == 0) (-90).toFloat() else 90.toFloat() else if (isRadiusZZero) spawnTheta = if (com.badlogic.gdx.math.MathUtils.random(1) == 0) 0 else 180.toFloat()
        }
        vector.set(radiusX * com.badlogic.gdx.math.MathUtils.cosDeg(spawnTheta), ty, radiusZ * com.badlogic.gdx.math.MathUtils.sinDeg(spawnTheta))
    }

    override fun copy(): com.badlogic.gdx.graphics.g3d.particles.values.SpawnShapeValue {
        return CylinderSpawnShapeValue(this)
    }
}
