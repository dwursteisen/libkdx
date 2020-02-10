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

object MipMapGenerator {
    private var useHWMipMap = true
    fun setUseHardwareMipMap(useHWMipMap: Boolean) {
        MipMapGenerator.useHWMipMap = useHWMipMap
    }

    /** Sets the image data of the [Texture] based on the [Pixmap]. The texture must be bound for this to work. If
     * `disposePixmap` is true, the pixmap will be disposed at the end of the method.
     * @param pixmap the Pixmap
     */
    fun generateMipMap(pixmap: com.badlogic.gdx.graphics.Pixmap?, textureWidth: Int, textureHeight: Int) {
        generateMipMap(com.badlogic.gdx.graphics.GL20.GL_TEXTURE_2D, pixmap, textureWidth, textureHeight)
    }

    /** Sets the image data of the [Texture] based on the [Pixmap]. The texture must be bound for this to work. If
     * `disposePixmap` is true, the pixmap will be disposed at the end of the method.  */
    @kotlin.jvm.JvmStatic
    fun generateMipMap(target: Int, pixmap: com.badlogic.gdx.graphics.Pixmap?, textureWidth: Int, textureHeight: Int) {
        if (!useHWMipMap) {
            generateMipMapCPU(target, pixmap, textureWidth, textureHeight)
            return
        }
        if (com.badlogic.gdx.Gdx.app.getType() == com.badlogic.gdx.Application.ApplicationType.Android || com.badlogic.gdx.Gdx.app.getType() == com.badlogic.gdx.Application.ApplicationType.WebGL || com.badlogic.gdx.Gdx.app.getType() == com.badlogic.gdx.Application.ApplicationType.iOS) {
            generateMipMapGLES20(target, pixmap)
        } else {
            generateMipMapDesktop(target, pixmap, textureWidth, textureHeight)
        }
    }

    private fun generateMipMapGLES20(target: Int, pixmap: com.badlogic.gdx.graphics.Pixmap?) {
        com.badlogic.gdx.Gdx.gl.glTexImage2D(target, 0, pixmap.getGLInternalFormat(), pixmap.getWidth(), pixmap.getHeight(), 0,
            pixmap.getGLFormat(), pixmap.getGLType(), pixmap.getPixels())
        com.badlogic.gdx.Gdx.gl20.glGenerateMipmap(target)
    }

    private fun generateMipMapDesktop(target: Int, pixmap: com.badlogic.gdx.graphics.Pixmap?, textureWidth: Int, textureHeight: Int) {
        if (com.badlogic.gdx.Gdx.graphics.supportsExtension("GL_ARB_framebuffer_object")
            || com.badlogic.gdx.Gdx.graphics.supportsExtension("GL_EXT_framebuffer_object") || com.badlogic.gdx.Gdx.gl30 != null) {
            com.badlogic.gdx.Gdx.gl.glTexImage2D(target, 0, pixmap.getGLInternalFormat(), pixmap.getWidth(), pixmap.getHeight(), 0,
                pixmap.getGLFormat(), pixmap.getGLType(), pixmap.getPixels())
            com.badlogic.gdx.Gdx.gl20.glGenerateMipmap(target)
        } else {
            generateMipMapCPU(target, pixmap, textureWidth, textureHeight)
        }
    }

    private fun generateMipMapCPU(target: Int, pixmap: com.badlogic.gdx.graphics.Pixmap?, textureWidth: Int, textureHeight: Int) {
        var pixmap: com.badlogic.gdx.graphics.Pixmap? = pixmap
        com.badlogic.gdx.Gdx.gl.glTexImage2D(target, 0, pixmap.getGLInternalFormat(), pixmap.getWidth(), pixmap.getHeight(), 0,
            pixmap.getGLFormat(), pixmap.getGLType(), pixmap.getPixels())
        if (com.badlogic.gdx.Gdx.gl20 == null && textureWidth != textureHeight) throw com.badlogic.gdx.utils.GdxRuntimeException("texture width and height must be square when using mipmapping.")
        var width: Int = pixmap.getWidth() / 2
        var height: Int = pixmap.getHeight() / 2
        var level = 1
        while (width > 0 && height > 0) {
            val tmp: com.badlogic.gdx.graphics.Pixmap = com.badlogic.gdx.graphics.Pixmap(width, height, pixmap.getFormat())
            tmp.setBlending(com.badlogic.gdx.graphics.Pixmap.Blending.None)
            tmp.drawPixmap(pixmap, 0, 0, pixmap.getWidth(), pixmap.getHeight(), 0, 0, width, height)
            if (level > 1) pixmap.dispose()
            pixmap = tmp
            com.badlogic.gdx.Gdx.gl.glTexImage2D(target, level, pixmap.getGLInternalFormat(), pixmap.getWidth(), pixmap.getHeight(), 0,
                pixmap.getGLFormat(), pixmap.getGLType(), pixmap.getPixels())
            width = pixmap.getWidth() / 2
            height = pixmap.getHeight() / 2
            level++
        }
    }
}
