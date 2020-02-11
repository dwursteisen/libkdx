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
import com.badlogic.gdx.graphics.g3d.particles.ParticleSystem

/**
 * This class is used by particle batches to sort the particles before rendering.
 *
 * @author Inferno
 */
abstract class ParticleSorter {

    /**
     * Using this class will not apply sorting
     */
    class None : ParticleSorter() {

        var currentCapacity = 0
        var indices: IntArray
        override fun ensureCapacity(capacity: Int) {
            if (currentCapacity < capacity) {
                indices = IntArray(capacity)
                for (i in 0 until capacity) indices[i] = i
                currentCapacity = capacity
            }
        }

        override fun <T : ParticleControllerRenderData?> sort(renderData: Array<T>): IntArray {
            return indices
        }
    }

    /**
     * This class will sort all the particles using the distance from camera.
     */
    class Distance : ParticleSorter() {

        private var distances: FloatArray
        private var particleIndices: IntArray
        private var particleOffsets: IntArray
        private var currentSize = 0
        override fun ensureCapacity(capacity: Int) {
            if (currentSize < capacity) {
                distances = FloatArray(capacity)
                particleIndices = IntArray(capacity)
                particleOffsets = IntArray(capacity)
                currentSize = capacity
            }
        }

        override fun <T : ParticleControllerRenderData?> sort(renderData: Array<T>): IntArray {
            val `val`: FloatArray = camera.view.`val`
            val cx = `val`[Matrix4.M20]
            val cy = `val`[Matrix4.M21]
            val cz = `val`[Matrix4.M22]
            var count = 0
            var i = 0
            for (data in renderData) {
                var k = 0
                val c: Int = i + data.controller.particles.size
                while (i < c) {
                    distances[i] = cx * data.positionChannel.data.get(k + ParticleChannels.XOffset) + (cy
                        * data.positionChannel.data.get(k + ParticleChannels.YOffset)) + (cz
                        * data.positionChannel.data.get(k + ParticleChannels.ZOffset))
                    particleIndices[i] = i
                    ++i
                    k += data.positionChannel.strideSize
                }
                count += data.controller.particles.size
            }
            qsort(0, count - 1)
            i = 0
            while (i < count) {
                particleOffsets[particleIndices[i]] = i
                ++i
            }
            return particleOffsets
        }

        fun qsort(si: Int, ei: Int) {
            // base case
            if (si < ei) {
                var tmp: Float
                var tmpIndex: Int
                val particlesPivotIndex: Int
                // insertion
                if (ei - si <= 8) {
                    for (i in si..ei) {
                        var j = i
                        while (j > si && distances[j - 1] > distances[j]) {
                            tmp = distances[j]
                            distances[j] = distances[j - 1]
                            distances[j - 1] = tmp

                            // Swap indices
                            tmpIndex = particleIndices[j]
                            particleIndices[j] = particleIndices[j - 1]
                            particleIndices[j - 1] = tmpIndex
                            j--
                        }
                    }
                    return
                }

                // Quick
                val pivot = distances[si]
                var i = si + 1
                particlesPivotIndex = particleIndices[si]

                // partition array
                for (j in si + 1..ei) {
                    if (pivot > distances[j]) {
                        if (j > i) {
                            // Swap distances
                            tmp = distances[j]
                            distances[j] = distances[i]
                            distances[i] = tmp

                            // Swap indices
                            tmpIndex = particleIndices[j]
                            particleIndices[j] = particleIndices[i]
                            particleIndices[i] = tmpIndex
                        }
                        i++
                    }
                }

                // put pivot in right position
                distances[si] = distances[i - 1]
                distances[i - 1] = pivot
                particleIndices[si] = particleIndices[i - 1]
                particleIndices[i - 1] = particlesPivotIndex

                // call qsort on right and left sides of pivot
                qsort(si, i - 2)
                qsort(i, ei)
            }
        }
    }

    protected var camera: Camera? = null

    /**
     * @return an array of offsets where each particle should be put in the resulting mesh (also if more than one mesh will be
     * generated, this is an absolute offset considering a BIG output array).
     */
    abstract fun <T : ParticleControllerRenderData?> sort(renderData: Array<T>): IntArray
    fun setCamera(camera: Camera?) {
        this.camera = camera
    }

    /**
     * This method is called when the batch has increased the underlying particle buffer. In this way the sorter can increase the
     * data structures used to sort the particles (i.e increase backing array size)
     */
    fun ensureCapacity(capacity: Int) {}

    companion object {
        val TMP_V1: Vector3 = Vector3()
    }
}
