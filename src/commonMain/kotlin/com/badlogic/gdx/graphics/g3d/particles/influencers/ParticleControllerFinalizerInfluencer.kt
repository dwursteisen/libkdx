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
package com.badlogic.gdx.graphics.g3d.particles.influencers

import com.badlogic.gdx.graphics.g3d.particles.influencers.ColorInfluencer
import com.badlogic.gdx.graphics.g3d.particles.influencers.DynamicsInfluencer
import com.badlogic.gdx.graphics.g3d.particles.influencers.DynamicsModifier
import com.badlogic.gdx.graphics.g3d.particles.influencers.DynamicsModifier.Angular
import com.badlogic.gdx.graphics.g3d.particles.influencers.DynamicsModifier.BrownianAcceleration
import com.badlogic.gdx.graphics.g3d.particles.influencers.DynamicsModifier.CentripetalAcceleration
import com.badlogic.gdx.graphics.g3d.particles.influencers.DynamicsModifier.FaceDirection
import com.badlogic.gdx.graphics.g3d.particles.influencers.DynamicsModifier.PolarAcceleration
import com.badlogic.gdx.graphics.g3d.particles.influencers.DynamicsModifier.Rotational2D
import com.badlogic.gdx.graphics.g3d.particles.influencers.DynamicsModifier.Rotational3D
import com.badlogic.gdx.graphics.g3d.particles.influencers.DynamicsModifier.Strength
import com.badlogic.gdx.graphics.g3d.particles.influencers.DynamicsModifier.TangentialAcceleration
import com.badlogic.gdx.graphics.g3d.particles.influencers.ModelInfluencer
import com.badlogic.gdx.graphics.g3d.particles.influencers.ModelInfluencer.Random.ModelInstancePool
import com.badlogic.gdx.graphics.g3d.particles.influencers.ParticleControllerInfluencer
import com.badlogic.gdx.graphics.g3d.particles.influencers.ParticleControllerInfluencer.Random.ParticleControllerPool
import com.badlogic.gdx.graphics.g3d.particles.influencers.RegionInfluencer
import com.badlogic.gdx.graphics.g3d.particles.influencers.RegionInfluencer.Animated
import com.badlogic.gdx.graphics.g3d.particles.influencers.RegionInfluencer.AspectTextureRegion
import com.badlogic.gdx.graphics.g3d.particles.influencers.ScaleInfluencer
import com.badlogic.gdx.graphics.g3d.particles.influencers.SimpleInfluencer
import com.badlogic.gdx.graphics.g3d.particles.influencers.SpawnInfluencer
import java.lang.RuntimeException

/**
 * It's an [Influencer] which updates the simulation of particles containing a [ParticleController]. Must be the last
 * influencer to be updated, so it has to be placed at the end of the influencers list when creating a [ParticleController].
 *
 * @author Inferno
 */
class ParticleControllerFinalizerInfluencer : Influencer() {

    var positionChannel: FloatChannel? = null
    var scaleChannel: FloatChannel? = null
    var rotationChannel: FloatChannel? = null
    var controllerChannel: ObjectChannel<ParticleController>? = null
    var hasScale = false
    var hasRotation = false
    fun init() {
        controllerChannel = controller.particles.getChannel(ParticleChannels.ParticleController)
        if (controllerChannel == null) throw GdxRuntimeException(
            "ParticleController channel not found, specify an influencer which will allocate it please.")
        scaleChannel = controller.particles.getChannel(ParticleChannels.Scale)
        rotationChannel = controller.particles.getChannel(ParticleChannels.Rotation3D)
        hasScale = scaleChannel != null
        hasRotation = rotationChannel != null
    }

    fun allocateChannels() {
        positionChannel = controller.particles.addChannel(ParticleChannels.Position)
    }

    fun update() {
        var i = 0
        var positionOffset = 0
        val c: Int = controller.particles.size
        while (i < c) {
            val particleController: ParticleController = controllerChannel.data.get(i)
            val scale = if (hasScale) scaleChannel.data.get(i) else 1.toFloat()
            var qx = 0f
            var qy = 0f
            var qz = 0f
            var qw = 1f
            if (hasRotation) {
                val rotationOffset: Int = i * rotationChannel.strideSize
                qx = rotationChannel.data.get(rotationOffset + ParticleChannels.XOffset)
                qy = rotationChannel.data.get(rotationOffset + ParticleChannels.YOffset)
                qz = rotationChannel.data.get(rotationOffset + ParticleChannels.ZOffset)
                qw = rotationChannel.data.get(rotationOffset + ParticleChannels.WOffset)
            }
            particleController.setTransform(positionChannel.data.get(positionOffset + ParticleChannels.XOffset),
                positionChannel.data.get(positionOffset + ParticleChannels.YOffset), positionChannel.data.get(positionOffset
                + ParticleChannels.ZOffset), qx, qy, qz, qw, scale)
            particleController.update()
            ++i
            positionOffset += positionChannel.strideSize
        }
    }

    fun copy(): ParticleControllerFinalizerInfluencer {
        return ParticleControllerFinalizerInfluencer()
    }
}
