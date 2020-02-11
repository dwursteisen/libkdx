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

/**
 * A [ParticleControllerRenderer] which will render particles as point sprites to a [PointSpriteParticleBatch] .
 *
 * @author Inferno
 */
class PointSpriteRenderer() : ParticleControllerRenderer<PointSpriteControllerRenderData?, PointSpriteParticleBatch?>(PointSpriteControllerRenderData()) {

    constructor(batch: PointSpriteParticleBatch?) : this() {
        setBatch(batch)
    }

    fun allocateChannels() {
        renderData!!.positionChannel = controller.particles.addChannel(ParticleChannels.Position)
        renderData!!.regionChannel = controller.particles.addChannel(ParticleChannels.TextureRegion, TextureRegionInitializer.get())
        renderData!!.colorChannel = controller.particles.addChannel(ParticleChannels.Color, ColorInitializer.get())
        renderData!!.scaleChannel = controller.particles.addChannel(ParticleChannels.Scale, ScaleInitializer.get())
        renderData!!.rotationChannel = controller.particles.addChannel(ParticleChannels.Rotation2D, Rotation2dInitializer.get())
    }

    override fun isCompatible(batch: ParticleBatch<*>?): Boolean {
        return batch is PointSpriteParticleBatch
    }

    fun copy(): ParticleControllerComponent? {
        return PointSpriteRenderer(batch)
    }
}
