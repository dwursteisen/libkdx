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

/** Encapsulate the formulas to spawn a particle on a point shape.
 * @author Inferno
 */
class PointSpawnShapeValue : com.badlogic.gdx.graphics.g3d.particles.values.PrimitiveSpawnShapeValue {

    constructor(value: PointSpawnShapeValue?) : super(value) {
        load(value!!)
    }

    constructor() {}

    override fun spawnAux(vector: com.badlogic.gdx.math.Vector3, percent: Float) {
        vector.x = spawnWidth + spawnWidthDiff * spawnWidthValue.getScale(percent)
        vector.y = spawnHeight + spawnHeightDiff * spawnHeightValue.getScale(percent)
        vector.z = spawnDepth + spawnDepthDiff * spawnDepthValue.getScale(percent)
    }

    override fun copy(): com.badlogic.gdx.graphics.g3d.particles.values.SpawnShapeValue {
        return PointSpawnShapeValue(this)
    }
}
