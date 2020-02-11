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
package com.badlogic.gdx.graphics.g3d.particles.renderers

import com.badlogic.gdx.graphics.g3d.particles.renderers.BillboardRenderer
import com.badlogic.gdx.graphics.g3d.particles.renderers.ModelInstanceRenderer
import com.badlogic.gdx.graphics.g3d.particles.renderers.ParticleControllerControllerRenderer
import com.badlogic.gdx.graphics.g3d.particles.renderers.PointSpriteRenderer

/**
 * It's a [ParticleControllerComponent] which determines how the particles are rendered. It's the base class of every
 * particle renderer.
 *
 * @author Inferno
 */
abstract class ParticleControllerRenderer<D : ParticleControllerRenderData?, T : ParticleBatch<D?>?> : ParticleControllerComponent {

    protected var batch: T? = null
    protected var renderData: D? = null

    protected constructor() {}
    protected constructor(renderData: D?) {
        this.renderData = renderData
    }

    fun update() {
        batch.draw(renderData)
    }

    fun setBatch(batch: ParticleBatch<*>?): Boolean {
        if (isCompatible(batch)) {
            this.batch = batch
            return true
        }
        return false
    }

    abstract fun isCompatible(batch: ParticleBatch<*>?): Boolean
    fun set(particleController: ParticleController?) {
        super.set(particleController)
        if (renderData != null) renderData.controller = controller
    }
}
