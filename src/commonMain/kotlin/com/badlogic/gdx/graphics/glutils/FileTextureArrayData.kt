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

import com.badlogic.gdx.files.FileHandle
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

/** @author Tomski
 */
class FileTextureArrayData(format: com.badlogic.gdx.graphics.Pixmap.Format?, useMipMaps: Boolean, files: Array<FileHandle?>?) : com.badlogic.gdx.graphics.TextureArrayData {

    private val textureDatas: Array<com.badlogic.gdx.graphics.TextureData?>?
    private var prepared = false
    private val format: com.badlogic.gdx.graphics.Pixmap.Format?
    private val depth: Int
    var useMipMaps: Boolean
    override fun isPrepared(): Boolean {
        return prepared
    }

    override fun prepare() {
        var width = -1
        var height = -1
        for (data in textureDatas!!) {
            data.prepare()
            if (width == -1) {
                width = data.getWidth()
                height = data.getHeight()
                continue
            }
            if (width != data.getWidth() || height != data.getHeight()) {
                throw com.badlogic.gdx.utils.GdxRuntimeException("Error whilst preparing TextureArray: TextureArray Textures must have equal dimensions.")
            }
        }
        prepared = true
    }

    override fun consumeTextureArrayData() {
        for (i in textureDatas!!.indices) {
            if (textureDatas[i].getType() == com.badlogic.gdx.graphics.TextureData.TextureDataType.Custom) {
                textureDatas[i].consumeCustomData(com.badlogic.gdx.graphics.GL30.GL_TEXTURE_2D_ARRAY)
            } else {
                val texData: com.badlogic.gdx.graphics.TextureData? = textureDatas[i]
                var pixmap: com.badlogic.gdx.graphics.Pixmap? = texData.consumePixmap()
                var disposePixmap: Boolean = texData.disposePixmap()
                if (texData.getFormat() != pixmap.getFormat()) {
                    val temp: com.badlogic.gdx.graphics.Pixmap = com.badlogic.gdx.graphics.Pixmap(pixmap.getWidth(), pixmap.getHeight(), texData.getFormat())
                    temp.setBlending(com.badlogic.gdx.graphics.Pixmap.Blending.None)
                    temp.drawPixmap(pixmap, 0, 0, 0, 0, pixmap.getWidth(), pixmap.getHeight())
                    if (texData.disposePixmap()) {
                        pixmap.dispose()
                    }
                    pixmap = temp
                    disposePixmap = true
                }
                com.badlogic.gdx.Gdx.gl30.glTexSubImage3D(com.badlogic.gdx.graphics.GL30.GL_TEXTURE_2D_ARRAY, 0, 0, 0, i, pixmap.getWidth(), pixmap.getHeight(), 1, pixmap.getGLInternalFormat(), pixmap.getGLType(), pixmap.getPixels())
                if (disposePixmap) pixmap.dispose()
            }
        }
    }

    override fun getWidth(): Int {
        return textureDatas!![0].getWidth()
    }

    override fun getHeight(): Int {
        return textureDatas!![0].getHeight()
    }

    override fun getDepth(): Int {
        return depth
    }

    override fun getInternalFormat(): Int {
        return com.badlogic.gdx.graphics.Pixmap.Format.toGlFormat(format)
    }

    override fun getGLType(): Int {
        return com.badlogic.gdx.graphics.Pixmap.Format.toGlType(format)
    }

    override fun isManaged(): Boolean {
        for (data in textureDatas!!) {
            if (!data.isManaged()) {
                return false
            }
        }
        return true
    }

    init {
        this.format = format
        this.useMipMaps = useMipMaps
        depth = files!!.size
        textureDatas = arrayOfNulls<com.badlogic.gdx.graphics.TextureData?>(files.size)
        for (i in files.indices) {
            textureDatas[i] = com.badlogic.gdx.graphics.TextureData.Factory.loadFromFile(files[i], format, useMipMaps)
        }
    }
}
