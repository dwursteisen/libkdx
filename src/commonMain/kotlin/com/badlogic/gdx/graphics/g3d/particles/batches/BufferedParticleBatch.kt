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
package com.badlogic.gdx.graphics.g3d.particles.batches

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.g3d.particles.ParticleSorter
import com.badlogic.gdx.graphics.g3d.particles.renderers.ParticleControllerRenderData
import com.badlogic.gdx.utils.Array

/**
 * Base class of all the batches requiring to buffer [ParticleControllerRenderData]
 *
 * @author Inferno
 */
abstract class BufferedParticleBatch<T : ParticleControllerRenderData?> protected constructor(type: java.lang.Class<T>?) : ParticleBatch<T> {

    protected var renderData: Array<T>
    var bufferedCount = 0
        protected set
    protected var currentCapacity = 0
    protected var sorter: ParticleSorter
    protected var camera: Camera? = null
    override fun begin() {
        renderData.clear()
        bufferedCount = 0
    }

    override fun draw(data: T) {
        if (data.controller.particles.size > 0) {
            renderData.add(data)
            bufferedCount += data.controller.particles.size
        }
    }

    /**
     *
     */
    override fun end() {
        if (bufferedCount > 0) {
            ensureCapacity(bufferedCount)
            flush(sorter.sort(renderData))
        }
    }

    /**
     * Ensure the batch can contain the passed in amount of particles
     */
    fun ensureCapacity(capacity: Int) {
        if (currentCapacity >= capacity) return
        sorter.ensureCapacity(capacity)
        allocParticlesData(capacity)
        currentCapacity = capacity
    }

    fun resetCapacity() {
        bufferedCount = 0
        currentCapacity = bufferedCount
    }

    protected abstract fun allocParticlesData(capacity: Int)
    fun setCamera(camera: Camera?) {
        this.camera = camera
        sorter.setCamera(camera)
    }

    fun getSorter(): ParticleSorter {
        return sorter
    }

    fun setSorter(sorter: ParticleSorter) {
        this.sorter = sorter
        sorter.setCamera(camera)
        sorter.ensureCapacity(currentCapacity)
    }

    /**
     * Sends the data to the gpu. This method must use the calculated offsets to build the particles meshes. The offsets represent
     * the position at which a particle should be placed into the vertex array.
     *
     * @param offsets the calculated offsets
     */
    protected abstract fun flush(offsets: IntArray?)

    init {
        sorter = Distance()
        renderData = Array(false, 10, type)
    }
}
