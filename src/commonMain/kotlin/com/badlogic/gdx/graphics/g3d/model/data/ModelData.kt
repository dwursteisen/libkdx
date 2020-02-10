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
package com.badlogic.gdx.graphics.g3d.model.data

import kotlin.jvm.JvmField

/** Returned by a [ModelLoader], contains meshes, materials, nodes and animations. OpenGL resources like textures or vertex
 * buffer objects are not stored. Instead, a ModelData instance needs to be converted to a Model first.
 * @author badlogic
 */
class ModelData {

    var id: String? = null
    val version: ShortArray? = ShortArray(2)
    @JvmField
    val meshes: com.badlogic.gdx.utils.Array<com.badlogic.gdx.graphics.g3d.model.data.ModelMesh?>? = com.badlogic.gdx.utils.Array<com.badlogic.gdx.graphics.g3d.model.data.ModelMesh?>()
    @JvmField
    val materials: com.badlogic.gdx.utils.Array<com.badlogic.gdx.graphics.g3d.model.data.ModelMaterial?>? = com.badlogic.gdx.utils.Array<com.badlogic.gdx.graphics.g3d.model.data.ModelMaterial?>()
    @JvmField
    val nodes: com.badlogic.gdx.utils.Array<com.badlogic.gdx.graphics.g3d.model.data.ModelNode?>? = com.badlogic.gdx.utils.Array<com.badlogic.gdx.graphics.g3d.model.data.ModelNode?>()
    @JvmField
    val animations: com.badlogic.gdx.utils.Array<com.badlogic.gdx.graphics.g3d.model.data.ModelAnimation?>? = com.badlogic.gdx.utils.Array<com.badlogic.gdx.graphics.g3d.model.data.ModelAnimation?>()
    fun addMesh(mesh: com.badlogic.gdx.graphics.g3d.model.data.ModelMesh?) {
        for (other in meshes) {
            if (other!!.id == mesh!!.id) {
                throw com.badlogic.gdx.utils.GdxRuntimeException("Mesh with id '" + other!!.id + "' already in model")
            }
        }
        meshes.add(mesh)
    }
}
