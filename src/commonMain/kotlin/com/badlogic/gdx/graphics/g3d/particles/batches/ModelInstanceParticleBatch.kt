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
package com.badlogic.gdx.graphics.g3d.particles.batches

import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.graphics.g3d.Renderable
import com.badlogic.gdx.graphics.g3d.particles.ResourceData
import com.badlogic.gdx.graphics.g3d.particles.renderers.ModelInstanceControllerRenderData
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.Pool

/*** This class is used to render particles having a model instance channel.
 * @author Inferno
 */
class ModelInstanceParticleBatch : ParticleBatch<ModelInstanceControllerRenderData?> {

    var controllersRenderData: Array<ModelInstanceControllerRenderData>
    var bufferedCount = 0
    fun getRenderables(renderables: Array<Renderable?>?, pool: Pool<Renderable?>?) {
        for (data in controllersRenderData) {
            var i = 0
            val count: Int = data.controller.particles.size
            while (i < count) {
                data.modelInstanceChannel.data.get(i).getRenderables(renderables, pool)
                ++i
            }
        }
    }

    override fun begin() {
        controllersRenderData.clear()
        bufferedCount = 0
    }

    override fun end() {}
    override fun draw(data: ModelInstanceControllerRenderData) {
        controllersRenderData.add(data)
        bufferedCount += data.controller.particles.size
    }

    override fun save(manager: AssetManager?, assetDependencyData: ResourceData<*>?) {}
    override fun load(manager: AssetManager?, assetDependencyData: ResourceData<*>?) {}

    init {
        controllersRenderData = Array<ModelInstanceControllerRenderData>(false, 5)
    }
}
