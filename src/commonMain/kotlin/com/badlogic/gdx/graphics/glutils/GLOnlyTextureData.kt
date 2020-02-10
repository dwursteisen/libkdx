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

/** A [TextureData] implementation which should be used to create gl only textures. This TextureData fits perfectly for
 * FrameBuffer. The data is not managed.  */
class GLOnlyTextureData(width: Int, height: Int, mipMapLevel: Int, internalFormat: Int, format: Int, type: Int) : com.badlogic.gdx.graphics.TextureData {

    /** width and height  */
    var width = 0
    var height = 0
    var isPrepared = false
    /** properties of opengl texture  */
    var mipLevel = 0
    var internalFormat: Int
    var format: Int
    var type: Int
    override fun getType(): com.badlogic.gdx.graphics.TextureData.TextureDataType? {
        return com.badlogic.gdx.graphics.TextureData.TextureDataType.Custom
    }

    override fun isPrepared(): Boolean {
        return isPrepared
    }

    override fun prepare() {
        if (isPrepared) throw com.badlogic.gdx.utils.GdxRuntimeException("Already prepared")
        isPrepared = true
    }

    override fun consumeCustomData(target: Int) {
        com.badlogic.gdx.Gdx.gl.glTexImage2D(target, mipLevel, internalFormat, width, height, 0, format, type, null)
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
        return com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888
    }

    override fun useMipMaps(): Boolean {
        return false
    }

    override fun isManaged(): Boolean {
        return false
    }

    /** @see "https://www.khronos.org/opengles/sdk/docs/man/xhtml/glTexImage2D.xml"
     *
     * @param internalFormat Specifies the internal format of the texture. Must be one of the following symbolic constants:
     * [GL20.GL_ALPHA], [GL20.GL_LUMINANCE], [GL20.GL_LUMINANCE_ALPHA], [GL20.GL_RGB],
     * [GL20.GL_RGBA].
     * @param format Specifies the format of the texel data. Must match internalformat. The following symbolic values are accepted:
     * [GL20.GL_ALPHA], [GL20.GL_RGB], [GL20.GL_RGBA], [GL20.GL_LUMINANCE], and
     * [GL20.GL_LUMINANCE_ALPHA].
     * @param type Specifies the data type of the texel data. The following symbolic values are accepted:
     * [GL20.GL_UNSIGNED_BYTE], [GL20.GL_UNSIGNED_SHORT_5_6_5], [GL20.GL_UNSIGNED_SHORT_4_4_4_4], and
     * [GL20.GL_UNSIGNED_SHORT_5_5_5_1].
     */
    init {
        this.width = width
        this.height = height
        mipLevel = mipMapLevel
        this.internalFormat = internalFormat
        this.format = format
        this.type = type
    }
}
