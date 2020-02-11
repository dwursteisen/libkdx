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
import com.badlogic.gdx.graphics.g3d.particles.renderers.ParticleControllerControllerRenderer
import com.badlogic.gdx.graphics.g3d.particles.renderers.PointSpriteRenderer

/**
 * A [ParticleControllerRenderer] which will render particles as [ModelInstance] to a
 * [ModelInstanceParticleBatch].
 *
 * @author Inferno
 */
class ModelInstanceRenderer() : ParticleControllerRenderer<ModelInstanceControllerRenderData?, ModelInstanceParticleBatch?>(ModelInstanceControllerRenderData()) {

    private var hasColor = false
    private var hasScale = false
    private var hasRotation = false

    constructor(batch: ModelInstanceParticleBatch?) : this() {
        setBatch(batch)
    }

    fun allocateChannels() {
        renderData!!.positionChannel = controller.particles.addChannel(ParticleChannels.Position)
    }

    fun init() {
        renderData!!.modelInstanceChannel = controller.particles.getChannel(ParticleChannels.ModelInstance)
        renderData!!.colorChannel = controller.particles.getChannel(ParticleChannels.Color)
        renderData!!.scaleChannel = controller.particles.getChannel(ParticleChannels.Scale)
        renderData!!.rotationChannel = controller.particles.getChannel(ParticleChannels.Rotation3D)
        hasColor = renderData!!.colorChannel != null
        hasScale = renderData!!.scaleChannel != null
        hasRotation = renderData!!.rotationChannel != null
    }

    override fun update() {
        var i = 0
        var positionOffset = 0
        val c: Int = controller.particles.size
        while (i < c) {
            val instance: ModelInstance = renderData!!.modelInstanceChannel.data.get(i)
            val scale = if (hasScale) renderData!!.scaleChannel.data.get(i) else 1.toFloat()
            var qx = 0f
            var qy = 0f
            var qz = 0f
            var qw = 1f
            if (hasRotation) {
                val rotationOffset: Int = i * renderData!!.rotationChannel.strideSize
                qx = renderData!!.rotationChannel.data.get(rotationOffset + ParticleChannels.XOffset)
                qy = renderData!!.rotationChannel.data.get(rotationOffset + ParticleChannels.YOffset)
                qz = renderData!!.rotationChannel.data.get(rotationOffset + ParticleChannels.ZOffset)
                qw = renderData!!.rotationChannel.data.get(rotationOffset + ParticleChannels.WOffset)
            }
            instance.transform.set(renderData!!.positionChannel.data.get(positionOffset + ParticleChannels.XOffset),
                renderData!!.positionChannel.data.get(positionOffset + ParticleChannels.YOffset),
                renderData!!.positionChannel.data.get(positionOffset + ParticleChannels.ZOffset), qx, qy, qz, qw, scale, scale, scale)
            if (hasColor) {
                val colorOffset: Int = i * renderData!!.colorChannel.strideSize
                val colorAttribute: ColorAttribute = instance.materials.get(0).get(ColorAttribute.Diffuse) as ColorAttribute
                val blendingAttribute: BlendingAttribute = instance.materials.get(0).get(BlendingAttribute.Type) as BlendingAttribute
                colorAttribute.color.r = renderData!!.colorChannel.data.get(colorOffset + ParticleChannels.RedOffset)
                colorAttribute.color.g = renderData!!.colorChannel.data.get(colorOffset + ParticleChannels.GreenOffset)
                colorAttribute.color.b = renderData!!.colorChannel.data.get(colorOffset + ParticleChannels.BlueOffset)
                if (blendingAttribute != null) blendingAttribute.opacity = renderData!!.colorChannel.data.get(colorOffset + ParticleChannels.AlphaOffset)
            }
            ++i
            positionOffset += renderData!!.positionChannel.strideSize
        }
        super.update()
    }

    fun copy(): ParticleControllerComponent? {
        return ModelInstanceRenderer(batch)
    }

    override fun isCompatible(batch: ParticleBatch<*>?): Boolean {
        return batch is ModelInstanceParticleBatch
    }
}
