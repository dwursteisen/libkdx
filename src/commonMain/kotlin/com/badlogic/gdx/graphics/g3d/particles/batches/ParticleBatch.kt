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
import com.badlogic.gdx.graphics.g3d.RenderableProvider
import com.badlogic.gdx.graphics.g3d.particles.ResourceData
import com.badlogic.gdx.graphics.g3d.particles.renderers.ParticleControllerRenderData

/**
 * Common interface to all the batches that render particles.
 *
 * @author Inferno
 */
interface ParticleBatch<T : ParticleControllerRenderData?> : RenderableProvider, ResourceData.Configurable<Any?> {

    /**
     * Must be called once before any drawing operation
     */
    fun begin()
    fun draw(controller: T)

    /**
     * Must be called after all the drawing operations
     */
    fun end()
    fun save(manager: AssetManager?, assetDependencyData: ResourceData<*>?)
    fun load(manager: AssetManager?, assetDependencyData: ResourceData<*>?)
}
