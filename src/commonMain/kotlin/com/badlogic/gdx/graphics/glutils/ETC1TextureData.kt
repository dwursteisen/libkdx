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

class ETC1TextureData : com.badlogic.gdx.graphics.TextureData {
    var file: FileHandle? = null
    var data: com.badlogic.gdx.graphics.glutils.ETC1.ETC1Data? = null
    var useMipMaps: Boolean
    var width = 0
    var height = 0
    var isPrepared = false

    @JvmOverloads
    constructor(file: FileHandle?, useMipMaps: Boolean = false) {
        this.file = file
        this.useMipMaps = useMipMaps
    }

    constructor(encodedImage: com.badlogic.gdx.graphics.glutils.ETC1.ETC1Data?, useMipMaps: Boolean) {
        data = encodedImage
        this.useMipMaps = useMipMaps
    }

    override fun getType(): com.badlogic.gdx.graphics.TextureData.TextureDataType? {
        return com.badlogic.gdx.graphics.TextureData.TextureDataType.Custom
    }

    override fun isPrepared(): Boolean {
        return isPrepared
    }

    override fun prepare() {
        if (isPrepared) throw com.badlogic.gdx.utils.GdxRuntimeException("Already prepared")
        if (file == null && data == null) throw com.badlogic.gdx.utils.GdxRuntimeException("Can only load once from ETC1Data")
        if (file != null) {
            data = com.badlogic.gdx.graphics.glutils.ETC1.ETC1Data(file)
        }
        width = data!!.width
        height = data!!.height
        isPrepared = true
    }

    override fun consumeCustomData(target: Int) {
        if (!isPrepared) throw com.badlogic.gdx.utils.GdxRuntimeException("Call prepare() before calling consumeCompressedData()")
        if (!com.badlogic.gdx.Gdx.graphics.supportsExtension("GL_OES_compressed_ETC1_RGB8_texture")) {
            val pixmap: com.badlogic.gdx.graphics.Pixmap = com.badlogic.gdx.graphics.glutils.ETC1.decodeImage(data, com.badlogic.gdx.graphics.Pixmap.Format.RGB565)
            com.badlogic.gdx.Gdx.gl.glTexImage2D(target, 0, pixmap.getGLInternalFormat(), pixmap.getWidth(), pixmap.getHeight(), 0,
                pixmap.getGLFormat(), pixmap.getGLType(), pixmap.getPixels())
            if (useMipMaps) com.badlogic.gdx.graphics.glutils.MipMapGenerator.generateMipMap(target, pixmap, pixmap.getWidth(), pixmap.getHeight())
            pixmap.dispose()
            useMipMaps = false
        } else {
            com.badlogic.gdx.Gdx.gl.glCompressedTexImage2D(target, 0, com.badlogic.gdx.graphics.glutils.ETC1.ETC1_RGB8_OES, width, height, 0, data!!.compressedData.capacity()
                - data!!.dataOffset, data!!.compressedData)
            if (useMipMaps()) com.badlogic.gdx.Gdx.gl20.glGenerateMipmap(com.badlogic.gdx.graphics.GL20.GL_TEXTURE_2D)
        }
        data!!.dispose()
        data = null
        isPrepared = false
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
        return com.badlogic.gdx.graphics.Pixmap.Format.RGB565
    }

    override fun useMipMaps(): Boolean {
        return useMipMaps
    }

    override fun isManaged(): Boolean {
        return true
    }
}
