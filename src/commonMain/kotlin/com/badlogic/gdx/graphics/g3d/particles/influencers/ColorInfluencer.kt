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
import com.badlogic.gdx.graphics.g3d.particles.influencers.SimpleInfluencer
import com.badlogic.gdx.graphics.g3d.particles.influencers.SpawnInfluencer
import java.lang.RuntimeException

/**
 * It's an [Influencer] which controls particles color and transparency.
 *
 * @author Inferno
 */
abstract class ColorInfluencer : Influencer() {

    /**
     * It's an [Influencer] which assigns a random color when a particle is activated.
     */
    class Random : ColorInfluencer() {

        override var colorChannel: FloatChannel? = null
        override fun allocateChannels() {
            colorChannel = controller.particles.addChannel(ParticleChannels.Color)
        }

        fun activateParticles(startIndex: Int, count: Int) {
            var i: Int = startIndex * colorChannel.strideSize
            val c: Int = i + count * colorChannel.strideSize
            while (i < c) {
                colorChannel.data.get(i + ParticleChannels.RedOffset) = MathUtils.random()
                colorChannel.data.get(i + ParticleChannels.GreenOffset) = MathUtils.random()
                colorChannel.data.get(i + ParticleChannels.BlueOffset) = MathUtils.random()
                colorChannel.data.get(i + ParticleChannels.AlphaOffset) = MathUtils.random()
                i += colorChannel.strideSize
            }
        }

        fun copy(): Random {
            return Random()
        }
    }

    /**
     * It's an [Influencer] which manages the particle color during its life time.
     */
    class Single() : ColorInfluencer() {

        var alphaInterpolationChannel: FloatChannel? = null
        var lifeChannel: FloatChannel? = null
        var alphaValue: ScaledNumericValue
        var colorValue: GradientColorValue

        constructor(billboardColorInfluencer: Single) : this() {
            set(billboardColorInfluencer)
        }

        fun set(colorInfluencer: Single) {
            colorValue.load(colorInfluencer.colorValue)
            alphaValue.load(colorInfluencer.alphaValue)
        }

        override fun allocateChannels() {
            super.allocateChannels()
            // Hack this allows to share the channel descriptor structure but using a different id temporary
            ParticleChannels.Interpolation.id = controller.particleChannels.newId()
            alphaInterpolationChannel = controller.particles.addChannel(ParticleChannels.Interpolation)
            lifeChannel = controller.particles.addChannel(ParticleChannels.Life)
        }

        fun activateParticles(startIndex: Int, count: Int) {
            var i: Int = startIndex * colorChannel.strideSize
            var a: Int = startIndex * alphaInterpolationChannel.strideSize
            var l = startIndex
            * lifeChannel.strideSize + ParticleChannels.LifePercentOffset
            val c: Int = i + count * colorChannel.strideSize
            while (i < c) {
                val alphaStart: Float = alphaValue.newLowValue()
                val alphaDiff: Float = alphaValue.newHighValue() - alphaStart
                colorValue.getColor(0, colorChannel.data, i)
                colorChannel.data.get(i + ParticleChannels.AlphaOffset) = alphaStart + alphaDiff
                * alphaValue.getScale(lifeChannel.data.get(l))
                alphaInterpolationChannel.data.get(a + ParticleChannels.InterpolationStartOffset) = alphaStart
                alphaInterpolationChannel.data.get(a + ParticleChannels.InterpolationDiffOffset) = alphaDiff
                i += colorChannel.strideSize
                a += alphaInterpolationChannel.strideSize
                l += lifeChannel.strideSize
            }
        }

        fun update() {
            var i = 0
            var a = 0
            var l: Int = ParticleChannels.LifePercentOffset
            val c: Int = i + controller.particles.size
            * colorChannel.strideSize
            while (i < c) {
                val lifePercent: Float = lifeChannel.data.get(l)
                colorValue.getColor(lifePercent, colorChannel.data, i)
                colorChannel.data.get(i + ParticleChannels.AlphaOffset) = (alphaInterpolationChannel.data.get(a
                    + ParticleChannels.InterpolationStartOffset)
                    + alphaInterpolationChannel.data.get(a + ParticleChannels.InterpolationDiffOffset) * alphaValue.getScale(lifePercent))
                i += colorChannel.strideSize
                a += alphaInterpolationChannel.strideSize
                l += lifeChannel.strideSize
            }
        }

        fun copy(): Single {
            return Single(this)
        }

        fun write(json: Json) {
            json.writeValue("alpha", alphaValue)
            json.writeValue("color", colorValue)
        }

        fun read(json: Json, jsonData: JsonValue?) {
            alphaValue = json.readValue("alpha", ScaledNumericValue::class.java, jsonData)
            colorValue = json.readValue("color", GradientColorValue::class.java, jsonData)
        }

        init {
            colorValue = GradientColorValue()
            alphaValue = ScaledNumericValue()
            alphaValue.setHigh(1)
        }
    }

    var colorChannel: FloatChannel? = null
    fun allocateChannels() {
        colorChannel = controller.particles.addChannel(ParticleChannels.Color)
    }
}
