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

import com.badlogic.gdx.graphics.g3d.particles.ParticleShader
import com.badlogic.gdx.graphics.g3d.particles.ParticleShader.AlignMode
import com.badlogic.gdx.graphics.g3d.particles.ParticleShader.ParticleType
import com.badlogic.gdx.graphics.g3d.particles.ParticleSorter

/**
 * Singleton class which manages the particle effects. It's a utility class to ease particle batches management and particle
 * effects update.
 *
 * @author inferno
 */
class ParticleSystem : RenderableProvider {

    private val batches: Array<ParticleBatch<*>>
    private val effects: Array<ParticleEffect>
    fun add(batch: ParticleBatch<*>?) {
        batches.add(batch)
    }

    fun add(effect: ParticleEffect?) {
        effects.add(effect)
    }

    fun remove(effect: ParticleEffect?) {
        effects.removeValue(effect, true)
    }

    /**
     * Removes all the effects added to the system
     */
    fun removeAll() {
        effects.clear()
    }

    /**
     * Updates the simulation of all effects
     */
    fun update() {
        for (effect in effects) {
            effect.update()
        }
    }

    fun updateAndDraw() {
        for (effect in effects) {
            effect.update()
            effect.draw()
        }
    }

    fun update(deltaTime: Float) {
        for (effect in effects) {
            effect.update(deltaTime)
        }
    }

    fun updateAndDraw(deltaTime: Float) {
        for (effect in effects) {
            effect.update(deltaTime)
            effect.draw()
        }
    }

    /**
     * Must be called one time per frame before any particle effect drawing operation will occur.
     */
    fun begin() {
        for (batch in batches) batch.begin()
    }

    /**
     * Draws all the particle effects. Call [.begin] before this method and [.end] after.
     */
    fun draw() {
        for (effect in effects) {
            effect.draw()
        }
    }

    /**
     * Must be called one time per frame at the end of all drawing operations.
     */
    fun end() {
        for (batch in batches) batch.end()
    }

    fun getRenderables(renderables: Array<Renderable?>?, pool: Pool<Renderable?>?) {
        for (batch in batches) batch.getRenderables(renderables, pool)
    }

    fun getBatches(): Array<ParticleBatch<*>> {
        return batches
    }

    companion object {
        private var instance: ParticleSystem? = null

        @Deprecated("Please directly use the constructor")
        fun get(): ParticleSystem? {
            if (instance == null) instance = ParticleSystem()
            return instance
        }
    }

    init {
        batches = Array<ParticleBatch<*>>()
        effects = Array()
    }
}
