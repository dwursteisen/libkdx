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
package com.badlogic.gdx.utils

import com.badlogic.gdx.utils.Pools
import com.badlogic.gdx.utils.Predicate.PredicateIterator
import com.badlogic.gdx.utils.PropertiesUtils
import com.badlogic.gdx.utils.Queue.QueueIterable
import com.badlogic.gdx.utils.Queue.QueueIterator
import com.badlogic.gdx.utils.Scaling
import java.lang.IndexOutOfBoundsException
import java.lang.NullPointerException
import java.lang.RuntimeException
import kotlin.jvm.Throws

/**
 * Class with static helper methods that provide access to the default OpenGL FrameBuffer. These methods can be used to get the
 * entire screen content or a portion thereof.
 *
 * @author espitz
 */
object ScreenUtils {

    /**
     * Returns the default framebuffer contents as a [TextureRegion] with a width and height equal to the current screen
     * size. The base [Texture] always has [MathUtils.nextPowerOfTwo] dimensions and RGBA8888 [Format]. It can be
     * accessed via [TextureRegion.getTexture]. The texture is not managed and has to be reloaded manually on a context loss.
     * The returned TextureRegion is flipped along the Y axis by default.
     */
    val frameBufferTexture: TextureRegion
        get() {
            val w: Int = Gdx.graphics.getBackBufferWidth()
            val h: Int = Gdx.graphics.getBackBufferHeight()
            return getFrameBufferTexture(0, 0, w, h)
        }

    /**
     * Returns a portion of the default framebuffer contents specified by x, y, width and height as a [TextureRegion] with
     * the same dimensions. The base [Texture] always has [MathUtils.nextPowerOfTwo] dimensions and RGBA8888
     * [Format]. It can be accessed via [TextureRegion.getTexture]. This texture is not managed and has to be reloaded
     * manually on a context loss. If the width and height specified are larger than the framebuffer dimensions, the Texture will
     * be padded accordingly. Pixels that fall outside of the current screen will have RGBA values of 0.
     *
     * @param x the x position of the framebuffer contents to capture
     * @param y the y position of the framebuffer contents to capture
     * @param w the width of the framebuffer contents to capture
     * @param h the height of the framebuffer contents to capture
     */
    fun getFrameBufferTexture(x: Int, y: Int, w: Int, h: Int): TextureRegion {
        val potW: Int = MathUtils.nextPowerOfTwo(w)
        val potH: Int = MathUtils.nextPowerOfTwo(h)
        val pixmap: Pixmap = getFrameBufferPixmap(x, y, w, h)
        val potPixmap = Pixmap(potW, potH, Format.RGBA8888)
        potPixmap.setBlending(Blending.None)
        potPixmap.drawPixmap(pixmap, 0, 0)
        val texture = Texture(potPixmap)
        val textureRegion = TextureRegion(texture, 0, h, w, -h)
        potPixmap.dispose()
        pixmap.dispose()
        return textureRegion
    }

    fun getFrameBufferPixmap(x: Int, y: Int, w: Int, h: Int): Pixmap {
        Gdx.gl.glPixelStorei(GL20.GL_PACK_ALIGNMENT, 1)
        val pixmap = Pixmap(w, h, Format.RGBA8888)
        val pixels: ByteBuffer = pixmap.getPixels()
        Gdx.gl.glReadPixels(x, y, w, h, GL20.GL_RGBA, GL20.GL_UNSIGNED_BYTE, pixels)
        return pixmap
    }

    /**
     * Returns the default framebuffer contents as a byte[] array with a length equal to screen width * height * 4. The byte[] will
     * always contain RGBA8888 data. Because of differences in screen and image origins the framebuffer contents should be flipped
     * along the Y axis if you intend save them to disk as a bitmap. Flipping is not a cheap operation, so use this functionality
     * wisely.
     *
     * @param flipY whether to flip pixels along Y axis
     */
    fun getFrameBufferPixels(flipY: Boolean): ByteArray {
        val w: Int = Gdx.graphics.getBackBufferWidth()
        val h: Int = Gdx.graphics.getBackBufferHeight()
        return getFrameBufferPixels(0, 0, w, h, flipY)
    }

    /**
     * Returns a portion of the default framebuffer contents specified by x, y, width and height, as a byte[] array with a length
     * equal to the specified width * height * 4. The byte[] will always contain RGBA8888 data. If the width and height specified
     * are larger than the framebuffer dimensions, the Texture will be padded accordingly. Pixels that fall outside of the current
     * screen will have RGBA values of 0. Because of differences in screen and image origins the framebuffer contents should be
     * flipped along the Y axis if you intend save them to disk as a bitmap. Flipping is not a cheap operation, so use this
     * functionality wisely.
     *
     * @param flipY whether to flip pixels along Y axis
     */
    fun getFrameBufferPixels(x: Int, y: Int, w: Int, h: Int, flipY: Boolean): ByteArray {
        Gdx.gl.glPixelStorei(GL20.GL_PACK_ALIGNMENT, 1)
        val pixels: ByteBuffer = BufferUtils.newByteBuffer(w * h * 4)
        Gdx.gl.glReadPixels(x, y, w, h, GL20.GL_RGBA, GL20.GL_UNSIGNED_BYTE, pixels)
        val numBytes = w * h * 4
        val lines = ByteArray(numBytes)
        if (flipY) {
            val numBytesPerLine = w * 4
            for (i in 0 until h) {
                pixels.position((h - i - 1) * numBytesPerLine)
                pixels.get(lines, i * numBytesPerLine, numBytesPerLine)
            }
        } else {
            pixels.clear()
            pixels.get(lines)
        }
        return lines
    }
}
