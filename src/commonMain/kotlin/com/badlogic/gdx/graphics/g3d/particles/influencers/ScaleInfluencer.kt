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
import com.badlogic.gdx.graphics.g3d.particles.influencers.ParticleControllerFinalizerInfluencer
import com.badlogic.gdx.graphics.g3d.particles.influencers.ParticleControllerInfluencer
import com.badlogic.gdx.graphics.g3d.particles.influencers.ParticleControllerInfluencer.Random.ParticleControllerPool
import com.badlogic.gdx.graphics.g3d.particles.influencers.RegionInfluencer
import com.badlogic.gdx.graphics.g3d.particles.influencers.RegionInfluencer.Animated
import com.badlogic.gdx.graphics.g3d.particles.influencers.RegionInfluencer.AspectTextureRegion
import com.badlogic.gdx.graphics.g3d.particles.influencers.SimpleInfluencer
import com.badlogic.gdx.graphics.g3d.particles.influencers.SpawnInfluencer
import java.lang.RuntimeException

/**
 * It's an [Influencer] which controls the scale of the particles.
 *
 * @author Inferno
 */
class ScaleInfluencer : SimpleInfluencer {

    constructor() : super() {
        valueChannelDescriptor = ParticleChannels.Scale
    }

    override fun activateParticles(startIndex: Int, count: Int) {
        if (value.isRelative()) {
            var i: Int = startIndex * valueChannel.strideSize
            var a: Int = startIndex * interpolationChannel.strideSize
            val c = i + count
            * valueChannel.strideSize
            while (i < c) {
                val start: Float = value.newLowValue() * controller.scale.x
                val diff: Float = value.newHighValue() * controller.scale.x
                interpolationChannel.data.get(a + ParticleChannels.InterpolationStartOffset) = start
                interpolationChannel.data.get(a + ParticleChannels.InterpolationDiffOffset) = diff
                valueChannel.data.get(i) = start + diff * value.getScale(0)
                i += valueChannel.strideSize
                a += interpolationChannel.strideSize
            }
        } else {
            var i: Int = startIndex * valueChannel.strideSize
            var a: Int = startIndex * interpolationChannel.strideSize
            val c = i + count
            * valueChannel.strideSize
            while (i < c) {
                val start: Float = value.newLowValue() * controller.scale.x
                val diff: Float = value.newHighValue() * controller.scale.x - start
                interpolationChannel.data.get(a + ParticleChannels.InterpolationStartOffset) = start
                interpolationChannel.data.get(a + ParticleChannels.InterpolationDiffOffset) = diff
                valueChannel.data.get(i) = start + diff * value.getScale(0)
                i += valueChannel.strideSize
                a += interpolationChannel.strideSize
            }
        }
    }

    constructor(scaleInfluencer: ScaleInfluencer) : super(scaleInfluencer) {}

    fun copy(): ParticleControllerComponent {
        return ScaleInfluencer(this)
    }
}
