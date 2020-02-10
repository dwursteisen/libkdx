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
 * It's an [Influencer] which controls the particles dynamics (movement, rotations).
 *
 * @author Inferno
 */
class DynamicsInfluencer : Influencer {

    var velocities: Array<DynamicsModifier>
    private var accellerationChannel: FloatChannel? = null
    private var positionChannel: FloatChannel? = null
    private var previousPositionChannel: FloatChannel? = null
    private var rotationChannel: FloatChannel? = null
    private var angularVelocityChannel: FloatChannel? = null
    var hasAcceleration = false
    var has2dAngularVelocity = false
    var has3dAngularVelocity = false

    constructor() {
        velocities = Array<DynamicsModifier>(true, 3, DynamicsModifier::class.java)
    }

    constructor(vararg velocities: DynamicsModifier) {
        this.velocities = Array<DynamicsModifier>(true, velocities.size, DynamicsModifier::class.java)
        for (value in velocities) {
            this.velocities.add(value.copy() as DynamicsModifier)
        }
    }

    constructor(velocityInfluencer: DynamicsInfluencer) : this(*velocityInfluencer.velocities.toArray(DynamicsModifier::class.java) as Array<DynamicsModifier?>) {}

    fun allocateChannels() {
        for (k in 0 until velocities.size) {
            velocities.items.get(k).allocateChannels()
        }

        // Hack, shouldn't be done but after all the modifiers allocated their channels
        // it's possible to check if we need to allocate previous position channel
        accellerationChannel = controller.particles.getChannel(ParticleChannels.Acceleration)
        hasAcceleration = accellerationChannel != null
        if (hasAcceleration) {
            positionChannel = controller.particles.addChannel(ParticleChannels.Position)
            previousPositionChannel = controller.particles.addChannel(ParticleChannels.PreviousPosition)
        }

        // Angular velocity check
        angularVelocityChannel = controller.particles.getChannel(ParticleChannels.AngularVelocity2D)
        has2dAngularVelocity = angularVelocityChannel != null
        if (has2dAngularVelocity) {
            rotationChannel = controller.particles.addChannel(ParticleChannels.Rotation2D)
            has3dAngularVelocity = false
        } else {
            angularVelocityChannel = controller.particles.getChannel(ParticleChannels.AngularVelocity3D)
            has3dAngularVelocity = angularVelocityChannel != null
            if (has3dAngularVelocity) rotationChannel = controller.particles.addChannel(ParticleChannels.Rotation3D)
        }
    }

    fun set(particleController: ParticleController?) {
        super.set(particleController)
        for (k in 0 until velocities.size) {
            velocities.items.get(k).set(particleController)
        }
    }

    fun init() {
        for (k in 0 until velocities.size) {
            velocities.items.get(k).init()
        }
    }

    fun activateParticles(startIndex: Int, count: Int) {
        if (hasAcceleration) {
            // Previous position is the current position
            // Attention, this requires that some other influencer setting the position channel must execute before this influencer.
            var i: Int = startIndex * positionChannel.strideSize
            val c: Int = i + count * positionChannel.strideSize
            while (i < c) {
                previousPositionChannel.data.get(i + ParticleChannels.XOffset) = positionChannel.data.get(i + ParticleChannels.XOffset)
                previousPositionChannel.data.get(i + ParticleChannels.YOffset) = positionChannel.data.get(i + ParticleChannels.YOffset)
                previousPositionChannel.data.get(i + ParticleChannels.ZOffset) = positionChannel.data.get(i + ParticleChannels.ZOffset)
                i += positionChannel.strideSize
            }
        }
        if (has2dAngularVelocity) {
            // Rotation back to 0
            var i: Int = startIndex * rotationChannel.strideSize
            val c: Int = i + count * rotationChannel.strideSize
            while (i < c) {
                rotationChannel.data.get(i + ParticleChannels.CosineOffset) = 1
                rotationChannel.data.get(i + ParticleChannels.SineOffset) = 0
                i += rotationChannel.strideSize
            }
        } else if (has3dAngularVelocity) {
            // Rotation back to 0
            var i: Int = startIndex * rotationChannel.strideSize
            val c: Int = i + count * rotationChannel.strideSize
            while (i < c) {
                rotationChannel.data.get(i + ParticleChannels.XOffset) = 0
                rotationChannel.data.get(i + ParticleChannels.YOffset) = 0
                rotationChannel.data.get(i + ParticleChannels.ZOffset) = 0
                rotationChannel.data.get(i + ParticleChannels.WOffset) = 1
                i += rotationChannel.strideSize
            }
        }
        for (k in 0 until velocities.size) {
            velocities.items.get(k).activateParticles(startIndex, count)
        }
    }

    fun update() {
        // Clean previouse frame velocities
        if (hasAcceleration) Arrays.fill(accellerationChannel.data, 0, controller.particles.size * accellerationChannel.strideSize, 0)
        if (has2dAngularVelocity || has3dAngularVelocity) Arrays.fill(angularVelocityChannel.data, 0, controller.particles.size * angularVelocityChannel.strideSize, 0)

        // Sum all the forces/accelerations
        for (k in 0 until velocities.size) {
            velocities.items.get(k).update()
        }

        // Apply the forces
        if (hasAcceleration) {
            /*
             * //Euler Integration for(int i=0, offset = 0; i < controller.particles.size; ++i, offset +=positionChannel.strideSize){
             * previousPositionChannel.data[offset + ParticleChannels.XOffset] += accellerationChannel.data[offset +
             * ParticleChannels.XOffset]*controller.deltaTime; previousPositionChannel.data[offset + ParticleChannels.YOffset] +=
             * accellerationChannel.data[offset + ParticleChannels.YOffset]*controller.deltaTime; previousPositionChannel.data[offset
             * + ParticleChannels.ZOffset] += accellerationChannel.data[offset + ParticleChannels.ZOffset]*controller.deltaTime;
             *
             * positionChannel.data[offset + ParticleChannels.XOffset] += previousPositionChannel.data[offset +
             * ParticleChannels.XOffset]*controller.deltaTime; positionChannel.data[offset + ParticleChannels.YOffset] +=
             * previousPositionChannel.data[offset + ParticleChannels.YOffset]*controller.deltaTime; positionChannel.data[offset +
             * ParticleChannels.ZOffset] += previousPositionChannel.data[offset + ParticleChannels.ZOffset]*controller.deltaTime; }
             */
            // Verlet integration
            var i = 0
            var offset = 0
            while (i < controller.particles.size) {
                val x: Float = positionChannel.data.get(offset + ParticleChannels.XOffset)
                val y: Float = positionChannel.data.get(offset
                    + ParticleChannels.YOffset)
                val z: Float = positionChannel.data.get(offset + ParticleChannels.ZOffset)
                positionChannel.data.get(offset + ParticleChannels.XOffset) = (2 * x
                    - previousPositionChannel.data.get(offset + ParticleChannels.XOffset)
                    + accellerationChannel.data.get(offset + ParticleChannels.XOffset) * controller.deltaTimeSqr)
                positionChannel.data.get(offset + ParticleChannels.YOffset) = (2 * y
                    - previousPositionChannel.data.get(offset + ParticleChannels.YOffset)
                    + accellerationChannel.data.get(offset + ParticleChannels.YOffset) * controller.deltaTimeSqr)
                positionChannel.data.get(offset + ParticleChannels.ZOffset) = (2 * z
                    - previousPositionChannel.data.get(offset + ParticleChannels.ZOffset)
                    + accellerationChannel.data.get(offset + ParticleChannels.ZOffset) * controller.deltaTimeSqr)
                previousPositionChannel.data.get(offset + ParticleChannels.XOffset) = x
                previousPositionChannel.data.get(offset + ParticleChannels.YOffset) = y
                previousPositionChannel.data.get(offset + ParticleChannels.ZOffset) = z
                ++i
                offset += positionChannel.strideSize
            }
        }
        if (has2dAngularVelocity) {
            var i = 0
            var offset = 0
            while (i < controller.particles.size) {
                val rotation: Float = angularVelocityChannel.data.get(i) * controller.deltaTime
                if (rotation != 0f) {
                    val cosBeta: Float = MathUtils.cosDeg(rotation)
                    val sinBeta: Float = MathUtils.sinDeg(rotation)
                    val currentCosine: Float = rotationChannel.data.get(offset + ParticleChannels.CosineOffset)
                    val currentSine: Float = rotationChannel.data.get(offset + ParticleChannels.SineOffset)
                    val newCosine = currentCosine * cosBeta - currentSine * sinBeta
                    val newSine = currentSine * cosBeta + currentCosine
                    * sinBeta
                    rotationChannel.data.get(offset + ParticleChannels.CosineOffset) = newCosine
                    rotationChannel.data.get(offset + ParticleChannels.SineOffset) = newSine
                }
                ++i
                offset += rotationChannel.strideSize
            }
        } else if (has3dAngularVelocity) {
            var i = 0
            var offset = 0
            var angularOffset = 0
            while (i < controller.particles.size) {
                val wx: Float = angularVelocityChannel.data.get(angularOffset + ParticleChannels.XOffset)
                val wy: Float = angularVelocityChannel.data.get(angularOffset
                    + ParticleChannels.YOffset)
                val wz: Float = angularVelocityChannel.data.get(angularOffset + ParticleChannels.ZOffset)
                val qx: Float = rotationChannel.data.get(offset
                    + ParticleChannels.XOffset)
                val qy: Float = rotationChannel.data.get(offset + ParticleChannels.YOffset)
                val qz: Float = rotationChannel.data.get(offset
                    + ParticleChannels.ZOffset)
                val qw: Float = rotationChannel.data.get(offset + ParticleChannels.WOffset)
                TMP_Q.set(wx, wy, wz, 0).mul(qx, qy, qz, qw).mul(0.5f * controller.deltaTime).add(qx, qy, qz, qw).nor()
                rotationChannel.data.get(offset + ParticleChannels.XOffset) = TMP_Q.x
                rotationChannel.data.get(offset + ParticleChannels.YOffset) = TMP_Q.y
                rotationChannel.data.get(offset + ParticleChannels.ZOffset) = TMP_Q.z
                rotationChannel.data.get(offset + ParticleChannels.WOffset) = TMP_Q.w
                ++i
                offset += rotationChannel.strideSize
                angularOffset += angularVelocityChannel.strideSize
            }
        }
    }

    fun copy(): DynamicsInfluencer {
        return DynamicsInfluencer(this)
    }

    fun write(json: Json) {
        json.writeValue("velocities", velocities, Array::class.java, DynamicsModifier::class.java)
    }

    fun read(json: Json, jsonData: JsonValue?) {
        velocities.addAll(json.readValue("velocities", Array::class.java, DynamicsModifier::class.java, jsonData))
    }
}
