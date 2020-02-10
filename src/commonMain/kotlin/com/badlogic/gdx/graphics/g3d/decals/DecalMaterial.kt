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
package com.badlogic.gdx.graphics.g3d.decals

import Mesh.VertexDataType
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.g3d.decals.CameraGroupStrategy
import com.badlogic.gdx.graphics.g3d.decals.Decal
import com.badlogic.gdx.graphics.g3d.decals.DecalBatch
import com.badlogic.gdx.graphics.g3d.decals.SimpleOrthoGroupStrategy

/**
 * Material used by the [Decal] class
 */
class DecalMaterial {

    var textureRegion: TextureRegion? = null
    var srcBlendFactor = 0
        protected set
    var dstBlendFactor = 0
        protected set

    /**
     * Binds the material's texture to the OpenGL context and changes the glBlendFunc to the values used by it.
     */
    fun set() {
        textureRegion!!.getTexture().bind(0)
        if (!isOpaque) {
            Gdx.gl.glBlendFunc(srcBlendFactor, dstBlendFactor)
        }
    }

    /**
     * @return true if the material is completely opaque, false if it is not and therefor requires blending
     */
    val isOpaque: Boolean
        get() = srcBlendFactor == NO_BLEND

    override fun equals(o: Any?): Boolean {
        if (o == null) return false
        val material = o as DecalMaterial?
        return dstBlendFactor == material!!.dstBlendFactor && srcBlendFactor == material.srcBlendFactor && textureRegion!!.getTexture() === material.textureRegion!!.getTexture()
    }

    override fun hashCode(): Int {
        var result = if (textureRegion!!.getTexture() != null) textureRegion!!.getTexture().hashCode() else 0
        result = 31 * result + srcBlendFactor
        result = 31 * result + dstBlendFactor
        return result
    }

    companion object {
        const val NO_BLEND = -1
    }
}
