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
package com.badlogic.gdx.graphics.g3d.particles.emitters

import com.badlogic.gdx.graphics.g3d.particles.values.RangedNumericValue
import com.badlogic.gdx.graphics.g3d.particles.values.ScaledNumericValue

/** It's a generic use [Emitter] which fits most of the particles simulation scenarios.
 * @author Inferno
 */
class RegularEmitter() : com.badlogic.gdx.graphics.g3d.particles.emitters.Emitter(), com.badlogic.gdx.utils.Json.Serializable {

    /** Possible emission modes. Emission mode does not affect already emitted particles.  */
    enum class EmissionMode {

        /** New particles can be emitted.  */
        Enabled,
        /** Only valid for continuous emitters. It will only emit particles until the end of the effect duration. After that emission
         * cycle will not be restarted.  */
        EnabledUntilCycleEnd,
        /** Prevents new particle emission.  */
        Disabled
    }

    var delayValue: RangedNumericValue
    var durationValue: RangedNumericValue
    var lifeOffsetValue: ScaledNumericValue
    var lifeValue: ScaledNumericValue
    var emissionValue: ScaledNumericValue
    protected var emission = 0
    protected var emissionDiff = 0
    protected var emissionDelta = 0
    protected var lifeOffset = 0
    protected var lifeOffsetDiff = 0
    protected var life = 0
    protected var lifeDiff = 0
    protected var duration = 0f
    protected var delay = 0f
    protected var durationTimer = 0f
    protected var delayTimer = 0f
    var isContinuous: Boolean
    /** Gets current emission mode.
     * @return Current emission mode.
     */
    /** Sets emission mode. Emission mode does not affect already emitted particles.
     * @param emissionMode Emission mode to set.
     */
    var emissionMode: EmissionMode
    private var lifeChannel: com.badlogic.gdx.graphics.g3d.particles.ParallelArray.FloatChannel? = null

    constructor(regularEmitter: RegularEmitter) : this() {
        set(regularEmitter)
    }

    override fun allocateChannels() {
        lifeChannel = controller.particles.addChannel(com.badlogic.gdx.graphics.g3d.particles.ParticleChannels.Life)
    }

    override fun start() {
        delay = if (delayValue.isActive) delayValue.newLowValue() else 0
        delayTimer = 0f
        durationTimer = 0f
        duration = durationValue.newLowValue()
        percent = durationTimer / duration
        emission = emissionValue.newLowValue().toInt()
        emissionDiff = emissionValue.newHighValue().toInt()
        if (!emissionValue.isRelative) emissionDiff -= emission
        life = lifeValue.newLowValue().toInt()
        lifeDiff = lifeValue.newHighValue().toInt()
        if (!lifeValue.isRelative) lifeDiff -= life
        lifeOffset = if (lifeOffsetValue.isActive) lifeOffsetValue.newLowValue().toInt() else 0
        lifeOffsetDiff = lifeOffsetValue.newHighValue().toInt()
        if (!lifeOffsetValue.isRelative) lifeOffsetDiff -= lifeOffset
    }

    override fun init() {
        super.init()
        emissionDelta = 0
        durationTimer = duration
    }

    override fun activateParticles(startIndex: Int, count: Int) {
        val currentTotaLife = life + (lifeDiff * lifeValue.getScale(percent)).toInt()
        var currentLife = currentTotaLife
        var offsetTime = (lifeOffset + lifeOffsetDiff * lifeOffsetValue.getScale(percent)).toInt()
        if (offsetTime > 0) {
            if (offsetTime >= currentLife) offsetTime = currentLife - 1
            currentLife -= offsetTime
        }
        val lifePercent = 1 - currentLife / currentTotaLife.toFloat()
        var i: Int = startIndex * lifeChannel.strideSize
        val c: Int = i + count * lifeChannel.strideSize
        while (i < c) {
            lifeChannel.data.get(i + com.badlogic.gdx.graphics.g3d.particles.ParticleChannels.CurrentLifeOffset) = currentLife
            lifeChannel.data.get(i + com.badlogic.gdx.graphics.g3d.particles.ParticleChannels.TotalLifeOffset) = currentTotaLife
            lifeChannel.data.get(i + com.badlogic.gdx.graphics.g3d.particles.ParticleChannels.LifePercentOffset) = lifePercent
            i += lifeChannel.strideSize
        }
    }

    override fun update() {
        val deltaMillis: Float = controller.deltaTime * 1000
        if (delayTimer < delay) {
            delayTimer += deltaMillis
        } else {
            var emit = emissionMode != EmissionMode.Disabled
            // End check
            if (durationTimer < duration) {
                durationTimer += deltaMillis
                percent = durationTimer / duration
            } else {
                if (isContinuous && emit && emissionMode == EmissionMode.Enabled) controller.start() else emit = false
            }
            if (emit) { // Emit particles
                emissionDelta += deltaMillis.toInt()
                var emissionTime = emission + emissionDiff * emissionValue.getScale(percent)
                if (emissionTime > 0) {
                    emissionTime = 1000 / emissionTime
                    if (emissionDelta >= emissionTime) {
                        var emitCount = (emissionDelta / emissionTime).toInt()
                        emitCount = java.lang.Math.min(emitCount, maxParticleCount - controller.particles.size)
                        emissionDelta -= emitCount * emissionTime.toInt()
                        emissionDelta %= emissionTime.toInt()
                        addParticles(emitCount)
                    }
                }
                if (controller.particles.size < minParticleCount) addParticles(minParticleCount - controller.particles.size)
            }
        }
        // Update particles
        val activeParticles: Int = controller.particles.size
        var i = 0
        var k = 0
        while (i < controller.particles.size) {
            if (deltaMillis.let { lifeChannel.data.get(k + com.badlogic.gdx.graphics.g3d.particles.ParticleChannels.CurrentLifeOffset) -= it; lifeChannel.data.get(k + com.badlogic.gdx.graphics.g3d.particles.ParticleChannels.CurrentLifeOffset) } <= 0) {
                controller.particles.removeElement(i)
                continue
            } else {
                lifeChannel.data.get(k + com.badlogic.gdx.graphics.g3d.particles.ParticleChannels.LifePercentOffset) = (1
                    - lifeChannel.data.get(k + com.badlogic.gdx.graphics.g3d.particles.ParticleChannels.CurrentLifeOffset)
                    / lifeChannel.data.get(k + com.badlogic.gdx.graphics.g3d.particles.ParticleChannels.TotalLifeOffset))
            }
            ++i
            k += lifeChannel.strideSize
        }
        if (controller.particles.size < activeParticles) {
            controller.killParticles(controller.particles.size, activeParticles - controller.particles.size)
        }
    }

    private fun addParticles(count: Int) {
        var count = count
        count = java.lang.Math.min(count, maxParticleCount - controller.particles.size)
        if (count <= 0) return
        controller.activateParticles(controller.particles.size, count)
        controller.particles.size += count
    }

    fun getLife(): ScaledNumericValue {
        return lifeValue
    }

    fun getEmission(): ScaledNumericValue {
        return emissionValue
    }

    fun getDuration(): RangedNumericValue {
        return durationValue
    }

    fun getDelay(): RangedNumericValue {
        return delayValue
    }

    fun getLifeOffset(): ScaledNumericValue {
        return lifeOffsetValue
    }

    override val isComplete: Boolean
        get() = if (delayTimer < delay) false else durationTimer >= duration && controller.particles.size == 0

    val percentComplete: Float
        get() = if (delayTimer < delay) 0 else java.lang.Math.min(1f, durationTimer / duration)

    fun set(emitter: RegularEmitter) {
        super.set(emitter)
        delayValue.load(emitter.delayValue)
        durationValue.load(emitter.durationValue)
        lifeOffsetValue.load(emitter.lifeOffsetValue)
        lifeValue.load(emitter.lifeValue)
        emissionValue.load(emitter.emissionValue)
        emission = emitter.emission
        emissionDiff = emitter.emissionDiff
        emissionDelta = emitter.emissionDelta
        lifeOffset = emitter.lifeOffset
        lifeOffsetDiff = emitter.lifeOffsetDiff
        life = emitter.life
        lifeDiff = emitter.lifeDiff
        duration = emitter.duration
        delay = emitter.delay
        durationTimer = emitter.durationTimer
        delayTimer = emitter.delayTimer
        isContinuous = emitter.isContinuous
    }

    override fun copy(): com.badlogic.gdx.graphics.g3d.particles.ParticleControllerComponent {
        return RegularEmitter(this)
    }

    override fun write(json: com.badlogic.gdx.utils.Json) {
        super.write(json)
        json.writeValue("continous", isContinuous)
        json.writeValue("emission", emissionValue)
        json.writeValue("delay", delayValue)
        json.writeValue("duration", durationValue)
        json.writeValue("life", lifeValue)
        json.writeValue("lifeOffset", lifeOffsetValue)
    }

    override fun read(json: com.badlogic.gdx.utils.Json, jsonData: com.badlogic.gdx.utils.JsonValue) {
        super.read(json, jsonData)
        isContinuous = json.readValue("continous", Boolean::class.javaPrimitiveType, jsonData)
        emissionValue = json.readValue("emission", ScaledNumericValue::class.java, jsonData)
        delayValue = json.readValue("delay", RangedNumericValue::class.java, jsonData)
        durationValue = json.readValue("duration", RangedNumericValue::class.java, jsonData)
        lifeValue = json.readValue("life", ScaledNumericValue::class.java, jsonData)
        lifeOffsetValue = json.readValue("lifeOffset", ScaledNumericValue::class.java, jsonData)
    }

    init {
        delayValue = RangedNumericValue()
        durationValue = RangedNumericValue()
        lifeOffsetValue = ScaledNumericValue()
        lifeValue = ScaledNumericValue()
        emissionValue = ScaledNumericValue()
        durationValue.isActive = true
        emissionValue.isActive = true
        lifeValue.isActive = true
        isContinuous = true
        emissionMode = EmissionMode.Enabled
    }
}
