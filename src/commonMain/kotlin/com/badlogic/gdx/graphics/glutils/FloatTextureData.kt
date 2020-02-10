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
package com.badlogic.gdx.graphics.glutils

import com.badlogic.gdx.graphics.glutils.HdpiMode
import com.badlogic.gdx.graphics.glutils.InstanceData
import java.io.BufferedInputStream
import java.lang.IllegalStateException
import java.lang.NumberFormatException
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import java.util.HashMap
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/** A [TextureData] implementation which should be used to create float textures.  */
class FloatTextureData(w: Int, h: Int, internalFormat: Int, format: Int, type: Int, isGpuOnly: Boolean) : com.badlogic.gdx.graphics.TextureData {

    var width = 0
    var height = 0
    var internalFormat: Int
    var format: Int
    var type: Int
    var isGpuOnly: Boolean
    var isPrepared = false
    var buffer: FloatBuffer? = null
    override fun getType(): com.badlogic.gdx.graphics.TextureData.TextureDataType? {
        return com.badlogic.gdx.graphics.TextureData.TextureDataType.Custom
    }

    override fun isPrepared(): Boolean {
        return isPrepared
    }

    override fun prepare() {
        if (isPrepared) throw com.badlogic.gdx.utils.GdxRuntimeException("Already prepared")
        if (!isGpuOnly) {
            var amountOfFloats = 4
            if (com.badlogic.gdx.Gdx.graphics.getGLVersion().getType() == com.badlogic.gdx.graphics.glutils.GLVersion.Type.OpenGL) {
                if (internalFormat == com.badlogic.gdx.graphics.GL30.GL_RGBA16F || internalFormat == com.badlogic.gdx.graphics.GL30.GL_RGBA32F) amountOfFloats = 4
                if (internalFormat == com.badlogic.gdx.graphics.GL30.GL_RGB16F || internalFormat == com.badlogic.gdx.graphics.GL30.GL_RGB32F) amountOfFloats = 3
                if (internalFormat == com.badlogic.gdx.graphics.GL30.GL_RG16F || internalFormat == com.badlogic.gdx.graphics.GL30.GL_RG32F) amountOfFloats = 2
                if (internalFormat == com.badlogic.gdx.graphics.GL30.GL_R16F || internalFormat == com.badlogic.gdx.graphics.GL30.GL_R32F) amountOfFloats = 1
            }
            buffer = com.badlogic.gdx.utils.BufferUtils.newFloatBuffer(width * height * amountOfFloats)
        }
        isPrepared = true
    }

    override fun consumeCustomData(target: Int) {
        if (com.badlogic.gdx.Gdx.app.getType() == com.badlogic.gdx.Application.ApplicationType.Android || com.badlogic.gdx.Gdx.app.getType() == com.badlogic.gdx.Application.ApplicationType.iOS || com.badlogic.gdx.Gdx.app.getType() == com.badlogic.gdx.Application.ApplicationType.WebGL) {
            if (!com.badlogic.gdx.Gdx.graphics.supportsExtension("OES_texture_float")) throw com.badlogic.gdx.utils.GdxRuntimeException("Extension OES_texture_float not supported!")
            // GLES and WebGL defines texture format by 3rd and 8th argument,
// so to get a float texture one needs to supply GL_RGBA and GL_FLOAT there.
            com.badlogic.gdx.Gdx.gl.glTexImage2D(target, 0, com.badlogic.gdx.graphics.GL20.GL_RGBA, width, height, 0, com.badlogic.gdx.graphics.GL20.GL_RGBA, com.badlogic.gdx.graphics.GL20.GL_FLOAT, buffer)
        } else {
            if (!com.badlogic.gdx.Gdx.graphics.isGL30Available()) {
                if (!com.badlogic.gdx.Gdx.graphics.supportsExtension("GL_ARB_texture_float")) throw com.badlogic.gdx.utils.GdxRuntimeException("Extension GL_ARB_texture_float not supported!")
            }
            // in desktop OpenGL the texture format is defined only by the third argument,
// hence we need to use GL_RGBA32F there (this constant is unavailable in GLES/WebGL)
            com.badlogic.gdx.Gdx.gl.glTexImage2D(target, 0, internalFormat, width, height, 0, format, com.badlogic.gdx.graphics.GL20.GL_FLOAT, buffer)
        }
    }

    override fun consumePixmap(): com.badlogic.gdx.graphics.Pixmap? {
        throw com.badlogic.gdx.utils.GdxRuntimeException("This TextureData implementation does not return a Pixmap")
    }

    override fun disposePixmap(): Boolean {
        throw com.badlogic.gdx.utils.GdxRuntimeException("This TextureData implementation does not return a Pixmap")
    }

    override fun getWidth(): Int {
        return width
    }

    override fun getHeight(): Int {
        return height
    }

    override fun getFormat(): com.badlogic.gdx.graphics.Pixmap.Format? {
        return com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888 // it's not true, but FloatTextureData.getFormat() isn't used anywhere
    }

    override fun useMipMaps(): Boolean {
        return false
    }

    override fun isManaged(): Boolean {
        return true
    }

    fun getBuffer(): FloatBuffer? {
        return buffer
    }

    init {
        width = w
        height = h
        this.internalFormat = internalFormat
        this.format = format
        this.type = type
        this.isGpuOnly = isGpuOnly
    }
}
