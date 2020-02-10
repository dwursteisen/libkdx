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

import com.badlogic.gdx.assets.AssetDescriptor

/** The base class of all the [ParticleValue] values which spawn a particle on a mesh shape.
 * @author Inferno
 */
abstract class MeshSpawnShapeValue : com.badlogic.gdx.graphics.g3d.particles.values.SpawnShapeValue {

    class Triangle(var x1: Float, var y1: Float, var z1: Float, var x2: Float, var y2: Float, var z2: Float, var x3: Float, var y3: Float, var z3: Float) {
        fun pick(vector: com.badlogic.gdx.math.Vector3): com.badlogic.gdx.math.Vector3 {
            val a: Float = com.badlogic.gdx.math.MathUtils.random()
            val b: Float = com.badlogic.gdx.math.MathUtils.random()
            return vector.set(x1 + a * (x2 - x1) + b * (x3 - x1), y1 + a * (y2 - y1) + b * (y3 - y1), z1 + a * (z2 - z1) + (b
                * (z3 - z1)))
        }

        companion object {
            fun pick(x1: Float, y1: Float, z1: Float, x2: Float, y2: Float, z2: Float, x3: Float, y3: Float, z3: Float,
                     vector: com.badlogic.gdx.math.Vector3): com.badlogic.gdx.math.Vector3 {
                val a: Float = com.badlogic.gdx.math.MathUtils.random()
                val b: Float = com.badlogic.gdx.math.MathUtils.random()
                return vector.set(x1 + a * (x2 - x1) + b * (x3 - x1), y1 + a * (y2 - y1) + b * (y3 - y1), z1 + a * (z2 - z1) + (b
                    * (z3 - z1)))
            }
        }
    }

    protected var mesh: com.badlogic.gdx.graphics.Mesh? = null
    /** the model this mesh belongs to. It can be null, but this means the mesh will not be able to be serialized correctly.  */
    protected var model: com.badlogic.gdx.graphics.g3d.Model? = null

    constructor(value: MeshSpawnShapeValue?) : super(value) {}
    constructor() {}

    override fun load(value: com.badlogic.gdx.graphics.g3d.particles.values.ParticleValue) {
        super.load(value)
        val spawnShapeValue = value as MeshSpawnShapeValue
        setMesh(spawnShapeValue.mesh, spawnShapeValue.model)
    }

    open fun setMesh(mesh: com.badlogic.gdx.graphics.Mesh?, model: com.badlogic.gdx.graphics.g3d.Model?) {
        if (mesh.getVertexAttribute(com.badlogic.gdx.graphics.VertexAttributes.Usage.Position) == null) throw com.badlogic.gdx.utils.GdxRuntimeException("Mesh vertices must have Usage.Position")
        this.model = model
        this.mesh = mesh
    }

    fun setMesh(mesh: com.badlogic.gdx.graphics.Mesh?) {
        this.setMesh(mesh, null)
    }

    override fun save(manager: com.badlogic.gdx.assets.AssetManager, data: com.badlogic.gdx.graphics.g3d.particles.ResourceData<*>) {
        if (model != null) {
            val saveData: com.badlogic.gdx.graphics.g3d.particles.ResourceData.SaveData = data.createSaveData()
            saveData.saveAsset(manager.getAssetFileName(model), com.badlogic.gdx.graphics.g3d.Model::class.java)
            saveData.save("index", model.meshes.indexOf(mesh, true))
        }
    }

    override fun load(manager: com.badlogic.gdx.assets.AssetManager, data: com.badlogic.gdx.graphics.g3d.particles.ResourceData<*>) {
        val saveData: com.badlogic.gdx.graphics.g3d.particles.ResourceData.SaveData = data.getSaveData()
        val descriptor: AssetDescriptor<*> = saveData.loadAsset()
        if (descriptor != null) {
            val model: com.badlogic.gdx.graphics.g3d.Model = manager.get(descriptor) as com.badlogic.gdx.graphics.g3d.Model
            setMesh(model.meshes.get(saveData.load("index") as Int), model)
        }
    }
}
