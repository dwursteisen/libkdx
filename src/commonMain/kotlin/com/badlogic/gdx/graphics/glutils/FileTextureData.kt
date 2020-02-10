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

class FileTextureData(val file: FileHandle?, preloadedPixmap: com.badlogic.gdx.graphics.Pixmap?, format: com.badlogic.gdx.graphics.Pixmap.Format?, useMipMaps: Boolean) : com.badlogic.gdx.graphics.TextureData {
    var width = 0
    var height = 0
    var format: com.badlogic.gdx.graphics.Pixmap.Format?
    var pixmap: com.badlogic.gdx.graphics.Pixmap?
    var useMipMaps: Boolean
    var isPrepared = false
    override fun isPrepared(): Boolean {
        return isPrepared
    }

    override fun prepare() {
        if (isPrepared) throw com.badlogic.gdx.utils.GdxRuntimeException("Already prepared")
        if (pixmap == null) {
            if (file!!.extension() == "cim") pixmap = com.badlogic.gdx.graphics.PixmapIO.readCIM(file) else pixmap = com.badlogic.gdx.graphics.Pixmap(file)
            width = pixmap.getWidth()
            height = pixmap.getHeight()
            if (format == null) format = pixmap.getFormat()
        }
        isPrepared = true
    }

    override fun consumePixmap(): com.badlogic.gdx.graphics.Pixmap? {
        if (!isPrepared) throw com.badlogic.gdx.utils.GdxRuntimeException("Call prepare() before calling getPixmap()")
        isPrepared = false
        val pixmap: com.badlogic.gdx.graphics.Pixmap? = pixmap
        this.pixmap = null
        return pixmap
    }

    override fun disposePixmap(): Boolean {
        return true
    }

    override fun getWidth(): Int {
        return width
    }

    override fun getHeight(): Int {
        return height
    }

    override fun getFormat(): com.badlogic.gdx.graphics.Pixmap.Format? {
        return format
    }

    override fun useMipMaps(): Boolean {
        return useMipMaps
    }

    override fun isManaged(): Boolean {
        return true
    }

    fun getFileHandle(): FileHandle? {
        return file
    }

    override fun getType(): com.badlogic.gdx.graphics.TextureData.TextureDataType? {
        return com.badlogic.gdx.graphics.TextureData.TextureDataType.Pixmap
    }

    override fun consumeCustomData(target: Int) {
        throw com.badlogic.gdx.utils.GdxRuntimeException("This TextureData implementation does not upload data itself")
    }

    override fun toString(): String {
        return file.toString()
    }

    init {
        pixmap = preloadedPixmap
        this.format = format
        this.useMipMaps = useMipMaps
        if (pixmap != null) {
            width = pixmap.getWidth()
            height = pixmap.getHeight()
            if (format == null) this.format = pixmap.getFormat()
        }
    }
}
