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
package com.badlogic.gdx.graphics.g3d.particles

import com.badlogic.gdx.graphics.g3d.particles.ParallelArray.ChannelDescriptor
import com.badlogic.gdx.graphics.g3d.particles.ParallelArray.ChannelInitializer
import com.badlogic.gdx.graphics.g3d.particles.ParallelArray.FloatChannel
import com.badlogic.gdx.graphics.g3d.particles.ParallelArray.IntChannel
import com.badlogic.gdx.graphics.g3d.particles.ParallelArray.ObjectChannel
import com.badlogic.gdx.graphics.g3d.particles.ParticleChannels.Companion.ParticleController
import com.badlogic.gdx.graphics.g3d.particles.ParticleController
import com.badlogic.gdx.graphics.g3d.particles.ParticleControllerComponent
import com.badlogic.gdx.graphics.g3d.particles.ParticleEffectLoader.ParticleEffectLoadParameter
import com.badlogic.gdx.graphics.g3d.particles.ParticleEffectLoader.ParticleEffectSaveParameter
import kotlin.jvm.Throws

/**
 * This contains all the definitions of particle related channels and channel initializers. It is also used by the
 * [ParticleController] to handle temporary channels allocated by influencers.
 *
 * @author inferno
 */
class ParticleChannels {

    // Initializers
    class TextureRegionInitializer : ChannelInitializer<FloatChannel?> {

        override fun init(channel: FloatChannel?) {
            var i = 0
            val c: Int = channel!!.data.length
            while (i < c) {
                channel!!.data!!.get(i + UOffset) = 0
                channel!!.data!!.get(i + VOffset) = 0
                channel!!.data!!.get(i + U2Offset) = 1
                channel!!.data!!.get(i + V2Offset) = 1
                channel!!.data!!.get(i + HalfWidthOffset) = 0.5f
                channel!!.data!!.get(i + HalfHeightOffset) = 0.5f
                i += channel!!.strideSize
            }
        }

        companion object {
            private var instance: TextureRegionInitializer? = null
            fun get(): TextureRegionInitializer? {
                if (instance == null) instance = TextureRegionInitializer()
                return instance
            }
        }
    }

    class ColorInitializer : ChannelInitializer<FloatChannel?> {
        override fun init(channel: FloatChannel?) {
            Arrays.fill(channel!!.data, 0, channel!!.data.length, 1)
        }

        companion object {
            private var instance: ColorInitializer? = null
            fun get(): ColorInitializer? {
                if (instance == null) instance = ColorInitializer()
                return instance
            }
        }
    }

    class ScaleInitializer : ChannelInitializer<FloatChannel?> {
        override fun init(channel: FloatChannel?) {
            Arrays.fill(channel!!.data, 0, channel!!.data.length, 1)
        }

        companion object {
            private var instance: ScaleInitializer? = null
            fun get(): ScaleInitializer? {
                if (instance == null) instance = ScaleInitializer()
                return instance
            }
        }
    }

    class Rotation2dInitializer : ChannelInitializer<FloatChannel?> {
        override fun init(channel: FloatChannel?) {
            var i = 0
            val c: Int = channel!!.data.length
            while (i < c) {
                channel!!.data!!.get(i + CosineOffset) = 1
                channel!!.data!!.get(i + SineOffset) = 0
                i += channel!!.strideSize
            }
        }

        companion object {
            private var instance: Rotation2dInitializer? = null
            fun get(): Rotation2dInitializer? {
                if (instance == null) instance = Rotation2dInitializer()
                return instance
            }
        }
    }

    class Rotation3dInitializer : ChannelInitializer<FloatChannel?> {
        override fun init(channel: FloatChannel?) {
            var i = 0
            val c: Int = channel!!.data.length
            while (i < c) {
                channel!!.data!!.get(i
                    + ZOffset) = 0
                channel!!.data!!.get(i + YOffset) = channel!!.data!!.get(i
                    + ZOffset)
                channel!!.data!!.get(i + XOffset) = channel!!.data!!.get(i + YOffset)
                channel!!.data!!.get(i + WOffset) = 1
                i += channel!!.strideSize
            }
        }

        companion object {
            private var instance: Rotation3dInitializer? = null
            fun get(): Rotation3dInitializer? {
                if (instance == null) instance = Rotation3dInitializer()
                return instance
            }
        }
    }

    private var currentId = 0
    fun newId(): Int {
        return currentId++
    }

    fun resetIds() {
        currentId = currentGlobalId
    }

    companion object {
        private var currentGlobalId = 0
        fun newGlobalId(): Int {
            return currentGlobalId++
        }
        // Channels
        /**
         * Channels of common use like position, life, color, etc...
         */
        val Life: ChannelDescriptor? = ChannelDescriptor(newGlobalId(), Float::class.javaPrimitiveType, 3)
        val Position: ChannelDescriptor? = ChannelDescriptor(newGlobalId(), Float::class.javaPrimitiveType, 3) // gl units
        val PreviousPosition: ChannelDescriptor? = ChannelDescriptor(newGlobalId(), Float::class.javaPrimitiveType, 3)
        val Color: ChannelDescriptor? = ChannelDescriptor(newGlobalId(), Float::class.javaPrimitiveType, 4)
        val TextureRegion: ChannelDescriptor? = ChannelDescriptor(newGlobalId(), Float::class.javaPrimitiveType, 6)
        val Rotation2D: ChannelDescriptor? = ChannelDescriptor(newGlobalId(), Float::class.javaPrimitiveType, 2)
        val Rotation3D: ChannelDescriptor? = ChannelDescriptor(newGlobalId(), Float::class.javaPrimitiveType, 4)
        val Scale: ChannelDescriptor? = ChannelDescriptor(newGlobalId(), Float::class.javaPrimitiveType, 1)
        val ModelInstance: ChannelDescriptor? = ChannelDescriptor(newGlobalId(), ModelInstance::class.java, 1)
        val ParticleController: ChannelDescriptor? = ChannelDescriptor(newGlobalId(), ParticleController::class.java, 1)
        val Acceleration: ChannelDescriptor? = ChannelDescriptor(newGlobalId(), Float::class.javaPrimitiveType, 3) // gl units/s2
        val AngularVelocity2D: ChannelDescriptor? = ChannelDescriptor(newGlobalId(), Float::class.javaPrimitiveType, 1)
        val AngularVelocity3D: ChannelDescriptor? = ChannelDescriptor(newGlobalId(), Float::class.javaPrimitiveType, 3)
        val Interpolation: ChannelDescriptor? = ChannelDescriptor(-1, Float::class.javaPrimitiveType, 2)
        val Interpolation4: ChannelDescriptor? = ChannelDescriptor(-1, Float::class.javaPrimitiveType, 4)
        val Interpolation6: ChannelDescriptor? = ChannelDescriptor(-1, Float::class.javaPrimitiveType, 6)
        // Offsets
        /**
         * Offsets to acess a particular value inside a stride of a given channel
         */
        const val CurrentLifeOffset = 0
        const val TotalLifeOffset = 1
        const val LifePercentOffset = 2
        const val RedOffset = 0
        const val GreenOffset = 1
        const val BlueOffset = 2
        const val AlphaOffset = 3
        const val InterpolationStartOffset = 0
        const val InterpolationDiffOffset = 1
        const val VelocityStrengthStartOffset = 0
        const val VelocityStrengthDiffOffset = 1
        const val VelocityThetaStartOffset = 0
        const val VelocityThetaDiffOffset = 1
        const val VelocityPhiStartOffset = 2
        const val VelocityPhiDiffOffset = 3
        const val XOffset = 0
        const val YOffset = 1
        const val ZOffset = 2
        const val WOffset = 3
        const val UOffset = 0
        const val VOffset = 1
        const val U2Offset = 2
        const val V2Offset = 3
        const val HalfWidthOffset = 4
        const val HalfHeightOffset = 5
        const val CosineOffset = 0
        const val SineOffset = 1
    }

    init {
        resetIds()
    }
}
