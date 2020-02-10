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
import com.badlogic.gdx.graphics.g3d.particles.influencers.ScaleInfluencer
import com.badlogic.gdx.graphics.g3d.particles.influencers.SpawnInfluencer
import java.lang.RuntimeException

/**
 * It's an [Influencer] which controls a generic channel of the particles. It handles the interpolation through time using
 * [ScaledNumericValue].
 *
 * @author Inferno
 */
abstract class SimpleInfluencer() : Influencer() {

    var value: ScaledNumericValue
    var valueChannel: FloatChannel? = null
    var interpolationChannel: FloatChannel? = null
    var lifeChannel: FloatChannel? = null
    var valueChannelDescriptor: ChannelDescriptor? = null

    constructor(billboardScaleinfluencer: SimpleInfluencer) : this() {
        set(billboardScaleinfluencer)
    }

    private fun set(scaleInfluencer: SimpleInfluencer) {
        value.load(scaleInfluencer.value)
        valueChannelDescriptor = scaleInfluencer.valueChannelDescriptor
    }

    fun allocateChannels() {
        valueChannel = controller.particles.addChannel(valueChannelDescriptor)
        ParticleChannels.Interpolation.id = controller.particleChannels.newId()
        interpolationChannel = controller.particles.addChannel(ParticleChannels.Interpolation)
        lifeChannel = controller.particles.addChannel(ParticleChannels.Life)
    }

    fun activateParticles(startIndex: Int, count: Int) {
        if (!value.isRelative()) {
            var i: Int = startIndex * valueChannel.strideSize
            var a: Int = startIndex * interpolationChannel.strideSize
            val c = i + count
            * valueChannel.strideSize
            while (i < c) {
                val start: Float = value.newLowValue()
                val diff: Float = value.newHighValue() - start
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
                val start: Float = value.newLowValue()
                val diff: Float = value.newHighValue()
                interpolationChannel.data.get(a + ParticleChannels.InterpolationStartOffset) = start
                interpolationChannel.data.get(a + ParticleChannels.InterpolationDiffOffset) = diff
                valueChannel.data.get(i) = start + diff * value.getScale(0)
                i += valueChannel.strideSize
                a += interpolationChannel.strideSize
            }
        }
    }

    fun update() {
        var i = 0
        var a = 0
        var l: Int = ParticleChannels.LifePercentOffset
        val c: Int = i + controller.particles.size * valueChannel.strideSize
        while (i < c) {
            valueChannel.data.get(i) = (interpolationChannel.data.get(a + ParticleChannels.InterpolationStartOffset)
                + interpolationChannel.data.get(a + ParticleChannels.InterpolationDiffOffset) * value.getScale(lifeChannel.data.get(l)))
            i += valueChannel.strideSize
            a += interpolationChannel.strideSize
            l += lifeChannel.strideSize
        }
    }

    fun write(json: Json) {
        json.writeValue("value", value)
    }

    fun read(json: Json, jsonData: JsonValue?) {
        value = json.readValue("value", ScaledNumericValue::class.java, jsonData)
    }

    init {
        value = ScaledNumericValue()
        value.setHigh(1)
    }
}
