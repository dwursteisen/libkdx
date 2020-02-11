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
package com.badlogic.gdx.graphics.g3d.utils

import com.badlogic.gdx.graphics.g3d.utils.AnimationController.AnimationDesc
import com.badlogic.gdx.graphics.g3d.utils.AnimationController.AnimationListener
import com.badlogic.gdx.graphics.g3d.utils.BaseAnimationController
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController.CameraGestureListener

/**
 * Class that you assign a range of texture units and binds textures for you within that range. It does some basic usage tracking
 * to avoid unnecessary bind calls.
 *
 * @author xoppa
 */
class DefaultTextureBinder @JvmOverloads constructor(method: Int, offset: Int = 0, count: Int = -1, reuseWeight: Int = 10) : TextureBinder {

    /**
     * The index of the first exclusive texture unit
     */
    private val offset: Int

    /**
     * The amount of exclusive textures that may be used
     */
    private val count: Int

    /**
     * The weight added to a texture when its reused
     */
    private val reuseWeight: Int

    /**
     * The textures currently exclusive bound
     */
    private val textures: Array<GLTexture?>?

    /**
     * The weight (reuseWeight * reused - discarded) of the textures
     */
    private val weights: IntArray?

    /**
     * The method of binding to use
     */
    private val method: Int

    /**
     * Flag to indicate the current texture is reused
     */
    private var reused = false
    var reuseCount = 0 // TODO remove debug code
        private set
    var bindCount = 0 // TODO remove debug code
        private set

    fun begin() {
        for (i in 0 until count) {
            textures!![i] = null
            if (weights != null) weights[i] = 0
        }
    }

    fun end() {
        /*
         * No need to unbind and textures are set to null in begin() for(int i = 0; i < count; i++) { if (textures[i] != null) {
         * Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0 + offset + i); Gdx.gl.glBindTexture(GL20.GL_TEXTURE_2D, 0); textures[i] = null; }
         * }
         */
        Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0)
    }

    fun bind(textureDesc: TextureDescriptor?): Int {
        return bindTexture(textureDesc, false)
    }

    private val tempDesc: TextureDescriptor? = TextureDescriptor()
    fun bind(texture: GLTexture?): Int {
        tempDesc.set(texture, null, null, null, null)
        return bindTexture(tempDesc, false)
    }

    private fun bindTexture(textureDesc: TextureDescriptor?, rebind: Boolean): Int {
        var idx: Int
        val result: Int
        val texture: GLTexture = textureDesc.texture
        reused = false
        when (method) {
            ROUNDROBIN -> result = offset + bindTextureRoundRobin(texture).also { idx = it }
            WEIGHTED -> result = offset + bindTextureWeighted(texture).also { idx = it }
            else -> return -1
        }
        if (reused) {
            reuseCount++
            if (rebind) texture.bind(result) else Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0 + result)
        } else bindCount++
        texture.unsafeSetWrap(textureDesc.uWrap, textureDesc.vWrap)
        texture.unsafeSetFilter(textureDesc.minFilter, textureDesc.magFilter)
        return result
    }

    private var currentTexture = 0
    private fun bindTextureRoundRobin(texture: GLTexture?): Int {
        for (i in 0 until count) {
            val idx = (currentTexture + i) % count
            if (textures!![idx] === texture) {
                reused = true
                return idx
            }
        }
        currentTexture = (currentTexture + 1) % count
        textures!![currentTexture] = texture
        texture.bind(offset + currentTexture)
        return currentTexture
    }

    private fun bindTextureWeighted(texture: GLTexture?): Int {
        var result = -1
        var weight = weights!![0]
        var windex = 0
        for (i in 0 until count) {
            if (textures!![i] === texture) {
                result = i
                weights[i] += reuseWeight
            } else if (weights[i] < 0 || --weights[i] < weight) {
                weight = weights[i]
                windex = i
            }
        }
        if (result < 0) {
            textures!![windex] = texture
            weights[windex] = 100
            texture.bind(offset + windex.also { result = it })
        } else reused = true
        return result
    }

    fun resetCounts() {
        reuseCount = 0
        bindCount = reuseCount
    }

    companion object {
        const val ROUNDROBIN = 0
        const val WEIGHTED = 1

        /**
         * GLES only supports up to 32 textures
         */
        const val MAX_GLES_UNITS = 32
        private val maxTextureUnits: Int
            private get() {
                val buffer: IntBuffer = BufferUtils.newIntBuffer(16)
                Gdx.gl.glGetIntegerv(GL20.GL_MAX_TEXTURE_IMAGE_UNITS, buffer)
                return buffer.get(0)
            }
    }
    /**
     * Uses reuse weight of 10
     */
    /**
     * Uses all remaining texture units and reuse weight of 3
     */
    /**
     * Uses all available texture units and reuse weight of 3
     */
    init {
        var count = count
        val max: Int = java.lang.Math.min(maxTextureUnits, MAX_GLES_UNITS)
        if (count < 0) count = max - offset
        if (offset < 0 || count < 0 || offset + count > max || reuseWeight < 1) throw GdxRuntimeException("Illegal arguments")
        this.method = method
        this.offset = offset
        this.count = count
        textures = arrayOfNulls<GLTexture?>(count)
        this.reuseWeight = reuseWeight
        weights = if (method == WEIGHTED) IntArray(count) else null
    }
}
