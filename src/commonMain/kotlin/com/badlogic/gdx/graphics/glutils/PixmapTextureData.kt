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

class PixmapTextureData @JvmOverloads constructor(pixmap: com.badlogic.gdx.graphics.Pixmap?, format: com.badlogic.gdx.graphics.Pixmap.Format?, useMipMaps: Boolean, disposePixmap: Boolean, managed: Boolean = false) : com.badlogic.gdx.graphics.TextureData {
    val pixmap: com.badlogic.gdx.graphics.Pixmap?
    val format: com.badlogic.gdx.graphics.Pixmap.Format?
    val useMipMaps: Boolean
    val disposePixmap: Boolean
    val managed: Boolean
    override fun disposePixmap(): Boolean {
        return disposePixmap
    }

    override fun consumePixmap(): com.badlogic.gdx.graphics.Pixmap? {
        return pixmap
    }

    override fun getWidth(): Int {
        return pixmap.getWidth()
    }

    override fun getHeight(): Int {
        return pixmap.getHeight()
    }

    override fun getFormat(): com.badlogic.gdx.graphics.Pixmap.Format? {
        return format
    }

    override fun useMipMaps(): Boolean {
        return useMipMaps
    }

    override fun isManaged(): Boolean {
        return managed
    }

    override fun getType(): com.badlogic.gdx.graphics.TextureData.TextureDataType? {
        return com.badlogic.gdx.graphics.TextureData.TextureDataType.Pixmap
    }

    override fun consumeCustomData(target: Int) {
        throw com.badlogic.gdx.utils.GdxRuntimeException("This TextureData implementation does not upload data itself")
    }

    override fun isPrepared(): Boolean {
        return true
    }

    override fun prepare() {
        throw com.badlogic.gdx.utils.GdxRuntimeException("prepare() must not be called on a PixmapTextureData instance as it is already prepared.")
    }

    init {
        this.pixmap = pixmap
        this.format = if (format == null) pixmap.getFormat() else format
        this.useMipMaps = useMipMaps
        this.disposePixmap = disposePixmap
        this.managed = managed
    }
}
