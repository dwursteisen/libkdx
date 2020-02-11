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
package com.badlogic.gdx.graphics.g2d

import com.badlogic.gdx.graphics.g2d.ParticleEffectPool.PooledEffect

class ParticleEffectPool(effect: ParticleEffect, initialCapacity: Int, max: Int) : Pool<PooledEffect?>(initialCapacity, max) {
    private val effect: ParticleEffect
    protected fun newObject(): PooledEffect {
        val pooledEffect = PooledEffect(effect)
        pooledEffect.start()
        return pooledEffect
    }

    fun free(effect: PooledEffect) {
        super.free(effect)
        effect.reset(false) // copy parameters exactly to avoid introducing error
        if (effect.xSizeScale !== this.effect.xSizeScale || effect.ySizeScale !== this.effect.ySizeScale || effect.motionScale !== this.effect.motionScale) {
            val emitters: Array<ParticleEmitter> = effect.getEmitters()
            val templateEmitters: Array<ParticleEmitter> = this.effect.getEmitters()
            for (i in 0 until emitters.size) {
                val emitter = emitters[i]
                val templateEmitter = templateEmitters[i]
                emitter.matchSize(templateEmitter)
                emitter.matchMotion(templateEmitter)
            }
            effect.xSizeScale = this.effect.xSizeScale
            effect.ySizeScale = this.effect.ySizeScale
            effect.motionScale = this.effect.motionScale
        }
    }

    inner class PooledEffect internal constructor(effect: ParticleEffect?) : ParticleEffect(effect) {
        fun free() {
            this@ParticleEffectPool.free(this)
        }
    }

    init {
        this.effect = effect
    }
}
