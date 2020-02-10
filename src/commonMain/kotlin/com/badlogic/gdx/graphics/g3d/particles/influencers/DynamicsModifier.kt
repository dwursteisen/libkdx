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
 * It's the base class for any kind of influencer which operates on angular velocity and acceleration of the particles. All the
 * classes that will inherit this base class can and should be used only as sub-influencer of an instance of
 * [DynamicsInfluencer] .
 *
 * @author Inferno
 */
abstract class DynamicsModifier : Influencer {

    class FaceDirection : DynamicsModifier {
        var rotationChannel: FloatChannel? = null
        var accellerationChannel: FloatChannel? = null

        constructor() {}
        constructor(rotation: FaceDirection) : super(rotation) {}

        override fun allocateChannels() {
            rotationChannel = controller.particles.addChannel(ParticleChannels.Rotation3D)
            accellerationChannel = controller.particles.addChannel(ParticleChannels.Acceleration)
        }

        fun update() {
            var i = 0
            var accelOffset = 0
            val c: Int = i + controller.particles.size * rotationChannel.strideSize
            while (i < c) {
                val axisZ: Vector3 = TMP_V1.set(accellerationChannel.data.get(accelOffset + ParticleChannels.XOffset),
                    accellerationChannel.data.get(accelOffset + ParticleChannels.YOffset),
                    accellerationChannel.data.get(accelOffset + ParticleChannels.ZOffset)).nor()
                val axisY: Vector3 = TMP_V2.set(TMP_V1)
                    .crs(Vector3.Y).nor().crs(TMP_V1).nor()
                val axisX: Vector3 = TMP_V3.set(axisY).crs(axisZ).nor()
                TMP_Q.setFromAxes(false, axisX.x, axisY.x, axisZ.x, axisX.y, axisY.y, axisZ.y, axisX.z, axisY.z, axisZ.z)
                rotationChannel.data.get(i + ParticleChannels.XOffset) = TMP_Q.x
                rotationChannel.data.get(i + ParticleChannels.YOffset) = TMP_Q.y
                rotationChannel.data.get(i + ParticleChannels.ZOffset) = TMP_Q.z
                rotationChannel.data.get(i + ParticleChannels.WOffset) = TMP_Q.w
                i += rotationChannel.strideSize
                accelOffset += accellerationChannel.strideSize
            }
        }

        fun copy(): ParticleControllerComponent {
            return FaceDirection(this)
        }
    }

    abstract class Strength : DynamicsModifier {
        protected var strengthChannel: FloatChannel? = null
        var strengthValue: ScaledNumericValue

        constructor() {
            strengthValue = ScaledNumericValue()
        }

        constructor(rotation: Strength) : super(rotation) {
            strengthValue = ScaledNumericValue()
            strengthValue.load(rotation.strengthValue)
        }

        override fun allocateChannels() {
            super.allocateChannels()
            ParticleChannels.Interpolation.id = controller.particleChannels.newId()
            strengthChannel = controller.particles.addChannel(ParticleChannels.Interpolation)
        }

        fun activateParticles(startIndex: Int, count: Int) {
            var start: Float
            var diff: Float
            var i: Int = startIndex * strengthChannel.strideSize
            val c: Int = i + count * strengthChannel.strideSize
            while (i < c) {
                start = strengthValue.newLowValue()
                diff = strengthValue.newHighValue()
                if (!strengthValue.isRelative()) diff -= start
                strengthChannel.data.get(i + ParticleChannels.VelocityStrengthStartOffset) = start
                strengthChannel.data.get(i + ParticleChannels.VelocityStrengthDiffOffset) = diff
                i += strengthChannel.strideSize
            }
        }

        override fun write(json: Json) {
            super.write(json)
            json.writeValue("strengthValue", strengthValue)
        }

        override fun read(json: Json, jsonData: JsonValue?) {
            super.read(json, jsonData)
            strengthValue = json.readValue("strengthValue", ScaledNumericValue::class.java, jsonData)
        }
    }

    abstract class Angular : Strength {
        protected var angularChannel: FloatChannel? = null

        /**
         * Polar angle, XZ plane
         */
        var thetaValue: ScaledNumericValue

        /**
         * Azimuth, Y
         */
        var phiValue: ScaledNumericValue

        constructor() {
            thetaValue = ScaledNumericValue()
            phiValue = ScaledNumericValue()
        }

        constructor(value: Angular) : super(value) {
            thetaValue = ScaledNumericValue()
            phiValue = ScaledNumericValue()
            thetaValue.load(value.thetaValue)
            phiValue.load(value.phiValue)
        }

        override fun allocateChannels() {
            super.allocateChannels()
            ParticleChannels.Interpolation4.id = controller.particleChannels.newId()
            angularChannel = controller.particles.addChannel(ParticleChannels.Interpolation4)
        }

        override fun activateParticles(startIndex: Int, count: Int) {
            super.activateParticles(startIndex, count)
            var start: Float
            var diff: Float
            var i: Int = startIndex * angularChannel.strideSize
            val c: Int = i + count * angularChannel.strideSize
            while (i < c) {

                // Theta
                start = thetaValue.newLowValue()
                diff = thetaValue.newHighValue()
                if (!thetaValue.isRelative()) diff -= start
                angularChannel.data.get(i + ParticleChannels.VelocityThetaStartOffset) = start
                angularChannel.data.get(i + ParticleChannels.VelocityThetaDiffOffset) = diff

                // Phi
                start = phiValue.newLowValue()
                diff = phiValue.newHighValue()
                if (!phiValue.isRelative()) diff -= start
                angularChannel.data.get(i + ParticleChannels.VelocityPhiStartOffset) = start
                angularChannel.data.get(i + ParticleChannels.VelocityPhiDiffOffset) = diff
                i += angularChannel.strideSize
            }
        }

        override fun write(json: Json) {
            super.write(json)
            json.writeValue("thetaValue", thetaValue)
            json.writeValue("phiValue", phiValue)
        }

        override fun read(json: Json, jsonData: JsonValue?) {
            super.read(json, jsonData)
            thetaValue = json.readValue("thetaValue", ScaledNumericValue::class.java, jsonData)
            phiValue = json.readValue("phiValue", ScaledNumericValue::class.java, jsonData)
        }
    }

    class Rotational2D : Strength {
        var rotationalVelocity2dChannel: FloatChannel? = null

        constructor() {}
        constructor(rotation: Rotational2D) : super(rotation) {}

        override fun allocateChannels() {
            super.allocateChannels()
            rotationalVelocity2dChannel = controller.particles.addChannel(ParticleChannels.AngularVelocity2D)
        }

        fun update() {
            var i = 0
            var l: Int = ParticleChannels.LifePercentOffset
            var s = 0
            val c: Int = i + controller.particles.size
            * rotationalVelocity2dChannel.strideSize
            while (i < c) {
                rotationalVelocity2dChannel.data.get(i) += (strengthChannel.data.get(s + ParticleChannels.VelocityStrengthStartOffset)
                    + strengthChannel.data.get(s + ParticleChannels.VelocityStrengthDiffOffset)
                    * strengthValue.getScale(lifeChannel.data.get(l)))
                s += strengthChannel.strideSize
                i += rotationalVelocity2dChannel.strideSize
                l += lifeChannel.strideSize
            }
        }

        fun copy(): Rotational2D {
            return Rotational2D(this)
        }
    }

    class Rotational3D : Angular {
        var rotationChannel: FloatChannel? = null
        var rotationalForceChannel: FloatChannel? = null

        constructor() {}
        constructor(rotation: Rotational3D) : super(rotation) {}

        override fun allocateChannels() {
            super.allocateChannels()
            rotationChannel = controller.particles.addChannel(ParticleChannels.Rotation3D)
            rotationalForceChannel = controller.particles.addChannel(ParticleChannels.AngularVelocity3D)
        }

        fun update() {

            // Matrix3 I_t = defined by the shape, it's the inertia tensor
            // Vector3 r = position vector
            // Vector3 L = r.cross(v.mul(m)), It's the angular momentum, where mv it's the linear momentum
            // Inverse(I_t) = a diagonal matrix where the diagonal is IyIz, IxIz, IxIy
            // Vector3 w = L/I_t = inverse(I_t)*L, It's the angular velocity
            // Quaternion spin = 0.5f*Quaternion(w, 0)*currentRotation
            // currentRotation += spin*dt
            // normalize(currentRotation)

            // Algorithm 1
            // Consider a simple channel which represent an angular velocity w
            // Sum each w for each rotation
            // Update rotation

            // Algorithm 2
            // Consider a channel which represent a sort of angular momentum L (r, v)
            // Sum each L for each rotation
            // Multiply sum by constant quantity k = m*I_to(-1) , m could be optional while I is constant and can be calculated at
// start
            // Update rotation

            // Algorithm 3
            // Consider a channel which represent a simple angular momentum L
            // Proceed as Algorithm 2
            var i = 0
            var l: Int = ParticleChannels.LifePercentOffset
            var s = 0
            var a = 0
            val c: Int = (controller.particles.size
                * rotationalForceChannel.strideSize)
            while (i < c) {
                val lifePercent: Float = lifeChannel.data.get(l)
                val strength: Float = (strengthChannel.data.get(s
                    + ParticleChannels.VelocityStrengthStartOffset)
                    + strengthChannel.data.get(s + ParticleChannels.VelocityStrengthDiffOffset) * strengthValue.getScale(lifePercent))
                val phi: Float = (angularChannel.data.get(a
                    + ParticleChannels.VelocityPhiStartOffset)
                    + angularChannel.data.get(a + ParticleChannels.VelocityPhiDiffOffset) * phiValue.getScale(lifePercent))
                val theta: Float = (angularChannel.data.get(a
                    + ParticleChannels.VelocityThetaStartOffset)
                    + angularChannel.data.get(a + ParticleChannels.VelocityThetaDiffOffset) * thetaValue.getScale(lifePercent))
                val cosTheta: Float = MathUtils.cosDeg(theta)
                val sinTheta: Float = MathUtils.sinDeg(theta)
                val cosPhi: Float = MathUtils.cosDeg(phi)
                val sinPhi: Float = MathUtils
                    .sinDeg(phi)
                TMP_V3.set(cosTheta * sinPhi, cosPhi, sinTheta * sinPhi)
                TMP_V3.scl(strength * MathUtils.degreesToRadians)
                rotationalForceChannel.data.get(i + ParticleChannels.XOffset) += TMP_V3.x
                rotationalForceChannel.data.get(i + ParticleChannels.YOffset) += TMP_V3.y
                rotationalForceChannel.data.get(i + ParticleChannels.ZOffset) += TMP_V3.z
                s += strengthChannel.strideSize
                i += rotationalForceChannel.strideSize
                a += angularChannel.strideSize
                l += lifeChannel.strideSize
            }
        }

        fun copy(): Rotational3D {
            return Rotational3D(this)
        }
    }

    class CentripetalAcceleration : Strength {
        var accelerationChannel: FloatChannel? = null
        var positionChannel: FloatChannel? = null

        constructor() {}
        constructor(rotation: CentripetalAcceleration) : super(rotation) {}

        override fun allocateChannels() {
            super.allocateChannels()
            accelerationChannel = controller.particles.addChannel(ParticleChannels.Acceleration)
            positionChannel = controller.particles.addChannel(ParticleChannels.Position)
        }

        fun update() {
            var cx = 0f
            var cy = 0f
            var cz = 0f
            if (!isGlobal) {
                val `val`: FloatArray = controller.transform.`val`
                cx = `val`[Matrix4.M03]
                cy = `val`[Matrix4.M13]
                cz = `val`[Matrix4.M23]
            }
            var lifeOffset: Int = ParticleChannels.LifePercentOffset
            var strengthOffset = 0
            var positionOffset = 0
            var forceOffset = 0
            var i = 0
            val c: Int = controller.particles.size
            while (i < c) {
                val strength: Float = (strengthChannel.data.get(strengthOffset + ParticleChannels.VelocityStrengthStartOffset)
                    + strengthChannel.data.get(strengthOffset + ParticleChannels.VelocityStrengthDiffOffset)
                    * strengthValue.getScale(lifeChannel.data.get(lifeOffset)))
                TMP_V3
                    .set(positionChannel.data.get(positionOffset + ParticleChannels.XOffset) - cx,
                        positionChannel.data.get(positionOffset + ParticleChannels.YOffset) - cy,
                        positionChannel.data.get(positionOffset + ParticleChannels.ZOffset) - cz).nor().scl(strength)
                accelerationChannel.data.get(forceOffset + ParticleChannels.XOffset) += TMP_V3.x
                accelerationChannel.data.get(forceOffset + ParticleChannels.YOffset) += TMP_V3.y
                accelerationChannel.data.get(forceOffset + ParticleChannels.ZOffset) += TMP_V3.z
                ++i
                positionOffset += positionChannel.strideSize
                strengthOffset += strengthChannel.strideSize
                forceOffset += accelerationChannel.strideSize
                lifeOffset += lifeChannel.strideSize
            }
        }

        fun copy(): CentripetalAcceleration {
            return CentripetalAcceleration(this)
        }
    }

    class PolarAcceleration : Angular {
        var directionalVelocityChannel: FloatChannel? = null

        constructor() {}
        constructor(rotation: PolarAcceleration) : super(rotation) {}

        override fun allocateChannels() {
            super.allocateChannels()
            directionalVelocityChannel = controller.particles.addChannel(ParticleChannels.Acceleration)
        }

        fun update() {
            var i = 0
            var l: Int = ParticleChannels.LifePercentOffset
            var s = 0
            var a = 0
            val c: Int = i + controller.particles.size
            * directionalVelocityChannel.strideSize
            while (i < c) {
                val lifePercent: Float = lifeChannel.data.get(l)
                val strength: Float = (strengthChannel.data.get(s
                    + ParticleChannels.VelocityStrengthStartOffset)
                    + strengthChannel.data.get(s + ParticleChannels.VelocityStrengthDiffOffset) * strengthValue.getScale(lifePercent))
                val phi: Float = (angularChannel.data.get(a
                    + ParticleChannels.VelocityPhiStartOffset)
                    + angularChannel.data.get(a + ParticleChannels.VelocityPhiDiffOffset) * phiValue.getScale(lifePercent))
                val theta: Float = (angularChannel.data.get(a
                    + ParticleChannels.VelocityThetaStartOffset)
                    + angularChannel.data.get(a + ParticleChannels.VelocityThetaDiffOffset) * thetaValue.getScale(lifePercent))
                val cosTheta: Float = MathUtils.cosDeg(theta)
                val sinTheta: Float = MathUtils.sinDeg(theta)
                val cosPhi: Float = MathUtils.cosDeg(phi)
                val sinPhi: Float = MathUtils
                    .sinDeg(phi)
                TMP_V3.set(cosTheta * sinPhi, cosPhi, sinTheta * sinPhi).nor().scl(strength)
                directionalVelocityChannel.data.get(i + ParticleChannels.XOffset) += TMP_V3.x
                directionalVelocityChannel.data.get(i + ParticleChannels.YOffset) += TMP_V3.y
                directionalVelocityChannel.data.get(i + ParticleChannels.ZOffset) += TMP_V3.z
                s += strengthChannel.strideSize
                i += directionalVelocityChannel.strideSize
                a += angularChannel.strideSize
                l += lifeChannel.strideSize
            }
        }

        fun copy(): PolarAcceleration {
            return PolarAcceleration(this)
        }
    }

    class TangentialAcceleration : Angular {
        var directionalVelocityChannel: FloatChannel? = null
        var positionChannel: FloatChannel? = null

        constructor() {}
        constructor(rotation: TangentialAcceleration) : super(rotation) {}

        override fun allocateChannels() {
            super.allocateChannels()
            directionalVelocityChannel = controller.particles.addChannel(ParticleChannels.Acceleration)
            positionChannel = controller.particles.addChannel(ParticleChannels.Position)
        }

        fun update() {
            var i = 0
            var l: Int = ParticleChannels.LifePercentOffset
            var s = 0
            var a = 0
            var positionOffset = 0
            val c: Int = (i
                + controller.particles.size * directionalVelocityChannel.strideSize)
            while (i < c) {
                val lifePercent: Float = lifeChannel.data.get(l)
                val strength: Float = (strengthChannel.data.get(s
                    + ParticleChannels.VelocityStrengthStartOffset)
                    + strengthChannel.data.get(s + ParticleChannels.VelocityStrengthDiffOffset) * strengthValue.getScale(lifePercent))
                val phi: Float = (angularChannel.data.get(a
                    + ParticleChannels.VelocityPhiStartOffset)
                    + angularChannel.data.get(a + ParticleChannels.VelocityPhiDiffOffset) * phiValue.getScale(lifePercent))
                val theta: Float = (angularChannel.data.get(a
                    + ParticleChannels.VelocityThetaStartOffset)
                    + angularChannel.data.get(a + ParticleChannels.VelocityThetaDiffOffset) * thetaValue.getScale(lifePercent))
                val cosTheta: Float = MathUtils.cosDeg(theta)
                val sinTheta: Float = MathUtils.sinDeg(theta)
                val cosPhi: Float = MathUtils.cosDeg(phi)
                val sinPhi: Float = MathUtils
                    .sinDeg(phi)
                TMP_V3
                    .set(cosTheta * sinPhi, cosPhi, sinTheta * sinPhi)
                    .crs(positionChannel.data.get(positionOffset + ParticleChannels.XOffset),
                        positionChannel.data.get(positionOffset + ParticleChannels.YOffset),
                        positionChannel.data.get(positionOffset + ParticleChannels.ZOffset)).nor().scl(strength)
                directionalVelocityChannel.data.get(i + ParticleChannels.XOffset) += TMP_V3.x
                directionalVelocityChannel.data.get(i + ParticleChannels.YOffset) += TMP_V3.y
                directionalVelocityChannel.data.get(i + ParticleChannels.ZOffset) += TMP_V3.z
                s += strengthChannel.strideSize
                i += directionalVelocityChannel.strideSize
                a += angularChannel.strideSize
                l += lifeChannel.strideSize
                positionOffset += positionChannel.strideSize
            }
        }

        fun copy(): TangentialAcceleration {
            return TangentialAcceleration(this)
        }
    }

    class BrownianAcceleration : Strength {
        var accelerationChannel: FloatChannel? = null

        constructor() {}
        constructor(rotation: BrownianAcceleration) : super(rotation) {}

        override fun allocateChannels() {
            super.allocateChannels()
            accelerationChannel = controller.particles.addChannel(ParticleChannels.Acceleration)
        }

        fun update() {
            var lifeOffset: Int = ParticleChannels.LifePercentOffset
            var strengthOffset = 0
            var forceOffset = 0
            var i = 0
            val c: Int = controller.particles.size
            while (i < c) {
                val strength: Float = (strengthChannel.data.get(strengthOffset + ParticleChannels.VelocityStrengthStartOffset)
                    + strengthChannel.data.get(strengthOffset + ParticleChannels.VelocityStrengthDiffOffset)
                    * strengthValue.getScale(lifeChannel.data.get(lifeOffset)))
                TMP_V3.set(MathUtils.random(-1, 1f), MathUtils.random(-1, 1f), MathUtils.random(-1, 1f)).nor().scl(strength)
                accelerationChannel.data.get(forceOffset + ParticleChannels.XOffset) += TMP_V3.x
                accelerationChannel.data.get(forceOffset + ParticleChannels.YOffset) += TMP_V3.y
                accelerationChannel.data.get(forceOffset + ParticleChannels.ZOffset) += TMP_V3.z
                ++i
                strengthOffset += strengthChannel.strideSize
                forceOffset += accelerationChannel.strideSize
                lifeOffset += lifeChannel.strideSize
            }
        }

        fun copy(): BrownianAcceleration {
            return BrownianAcceleration(this)
        }
    }

    var isGlobal = false
    protected var lifeChannel: FloatChannel? = null

    constructor() {}
    constructor(modifier: DynamicsModifier) {
        isGlobal = modifier.isGlobal
    }

    fun allocateChannels() {
        lifeChannel = controller.particles.addChannel(ParticleChannels.Life)
    }

    fun write(json: Json) {
        super.write(json)
        json.writeValue("isGlobal", isGlobal)
    }

    fun read(json: Json, jsonData: JsonValue?) {
        super.read(json, jsonData)
        isGlobal = json.readValue("isGlobal", Boolean::class.javaPrimitiveType, jsonData)
    }

    companion object {
        protected val TMP_V1: Vector3 = Vector3()
        protected val TMP_V2: Vector3 = Vector3()
        protected val TMP_V3: Vector3 = Vector3()
        protected val TMP_Q: Quaternion = Quaternion()
    }
}
