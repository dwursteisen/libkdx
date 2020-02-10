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
package com.badlogic.gdx.graphics.g2d

import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.GdxRuntimeException
import java.io.ByteArrayOutputStream

/**
 * @author mzechner
 */
class Gdx2DPixmap : Disposable {

    var basePtr: Long
    var width: Int
    var height: Int
    var format: Int
    var pixelPtr: java.nio.ByteBuffer?
    var nativeData = LongArray(4)

    constructor(encodedData: ByteArray, offset: Int, len: Int, requestedFormat: Int) {
        pixelPtr = load(nativeData, encodedData, offset, len)
        if (pixelPtr == null) throw IOException("Error loading pixmap: $failureReason")
        basePtr = nativeData[0]
        width = nativeData[1].toInt()
        height = nativeData[2].toInt()
        format = nativeData[3].toInt()
        if (requestedFormat != 0 && requestedFormat != format) {
            convert(requestedFormat)
        }
    }

    constructor(`in`: java.io.InputStream, requestedFormat: Int) {
        val bytes = ByteArrayOutputStream(1024)
        var buffer = ByteArray(1024)
        var readBytes = 0
        while (`in`.read(buffer).also({ readBytes = it }) != -1) {
            bytes.write(buffer, 0, readBytes)
        }
        buffer = bytes.toByteArray()
        pixelPtr = load(nativeData, buffer, 0, buffer.size)
        if (pixelPtr == null) throw IOException("Error loading pixmap: $failureReason")
        basePtr = nativeData[0]
        width = nativeData[1].toInt()
        height = nativeData[2].toInt()
        format = nativeData[3].toInt()
        if (requestedFormat != 0 && requestedFormat != format) {
            convert(requestedFormat)
        }
    }

    /**
     * @throws GdxRuntimeException if allocation failed.
     */
    constructor(width: Int, height: Int, format: Int) {
        pixelPtr = newPixmap(nativeData, width, height, format)
        if (pixelPtr == null) throw GdxRuntimeException(
            "Unable to allocate memory for pixmap: " + width + "x" + height + ", " + getFormatString(format))
        basePtr = nativeData[0]
        this.width = nativeData[1].toInt()
        this.height = nativeData[2].toInt()
        this.format = nativeData[3].toInt()
    }

    constructor(pixelPtr: java.nio.ByteBuffer?, nativeData: LongArray) {
        this.pixelPtr = pixelPtr
        basePtr = nativeData[0]
        width = nativeData[1].toInt()
        height = nativeData[2].toInt()
        format = nativeData[3].toInt()
    }

    private fun convert(requestedFormat: Int) {
        val pixmap = Gdx2DPixmap(width, height, requestedFormat)
        pixmap.setBlend(GDX2D_BLEND_NONE)
        pixmap.drawPixmap(this, 0, 0, 0, 0, width, height)
        dispose()
        basePtr = pixmap.basePtr
        format = pixmap.format
        height = pixmap.height
        nativeData = pixmap.nativeData
        pixelPtr = pixmap.pixelPtr
        width = pixmap.width
    }

    override fun dispose() {
        free(basePtr)
    }

    fun clear(color: Int) {
        clear(basePtr, color)
    }

    fun setPixel(x: Int, y: Int, color: Int) {
        setPixel(basePtr, x, y, color)
    }

    fun getPixel(x: Int, y: Int): Int {
        return getPixel(basePtr, x, y)
    }

    fun drawLine(x: Int, y: Int, x2: Int, y2: Int, color: Int) {
        drawLine(basePtr, x, y, x2, y2, color)
    }

    fun drawRect(x: Int, y: Int, width: Int, height: Int, color: Int) {
        drawRect(basePtr, x, y, width, height, color)
    }

    fun drawCircle(x: Int, y: Int, radius: Int, color: Int) {
        drawCircle(basePtr, x, y, radius, color)
    }

    fun fillRect(x: Int, y: Int, width: Int, height: Int, color: Int) {
        fillRect(basePtr, x, y, width, height, color)
    }

    fun fillCircle(x: Int, y: Int, radius: Int, color: Int) {
        fillCircle(basePtr, x, y, radius, color)
    }

    fun fillTriangle(x1: Int, y1: Int, x2: Int, y2: Int, x3: Int, y3: Int, color: Int) {
        fillTriangle(basePtr, x1, y1, x2, y2, x3, y3, color)
    }

    fun drawPixmap(src: Gdx2DPixmap, srcX: Int, srcY: Int, dstX: Int, dstY: Int, width: Int, height: Int) {
        drawPixmap(src.basePtr, basePtr, srcX, srcY, width, height, dstX, dstY, width, height)
    }

    fun drawPixmap(src: Gdx2DPixmap, srcX: Int, srcY: Int, srcWidth: Int, srcHeight: Int, dstX: Int, dstY: Int, dstWidth: Int,
                   dstHeight: Int) {
        drawPixmap(src.basePtr, basePtr, srcX, srcY, srcWidth, srcHeight, dstX, dstY, dstWidth, dstHeight)
    }

    fun setBlend(blend: Int) {
        setBlend(basePtr, blend)
    }

    fun setScale(scale: Int) {
        setScale(basePtr, scale)
    }

    val pixels: java.nio.ByteBuffer?
        get() = pixelPtr

    val gLInternalFormat: Int
        get() = toGlFormat(format)

    val gLFormat: Int
        get() = gLInternalFormat

    val gLType: Int
        get() = toGlType(format)

    val formatString: String
        get() = getFormatString(format)

    companion object {
        const val GDX2D_FORMAT_ALPHA = 1
        const val GDX2D_FORMAT_LUMINANCE_ALPHA = 2
        const val GDX2D_FORMAT_RGB888 = 3
        const val GDX2D_FORMAT_RGBA8888 = 4
        const val GDX2D_FORMAT_RGB565 = 5
        const val GDX2D_FORMAT_RGBA4444 = 6
        const val GDX2D_SCALE_NEAREST = 0
        const val GDX2D_SCALE_LINEAR = 1
        const val GDX2D_BLEND_NONE = 0
        const val GDX2D_BLEND_SRC_OVER = 1
        fun toGlFormat(format: Int): Int {
            return when (format) {
                GDX2D_FORMAT_ALPHA -> GL20.GL_ALPHA
                GDX2D_FORMAT_LUMINANCE_ALPHA -> GL20.GL_LUMINANCE_ALPHA
                GDX2D_FORMAT_RGB888, GDX2D_FORMAT_RGB565 -> GL20.GL_RGB
                GDX2D_FORMAT_RGBA8888, GDX2D_FORMAT_RGBA4444 -> GL20.GL_RGBA
                else -> throw GdxRuntimeException("unknown format: $format")
            }
        }

        fun toGlType(format: Int): Int {
            return when (format) {
                GDX2D_FORMAT_ALPHA, GDX2D_FORMAT_LUMINANCE_ALPHA, GDX2D_FORMAT_RGB888, GDX2D_FORMAT_RGBA8888 -> GL20.GL_UNSIGNED_BYTE
                GDX2D_FORMAT_RGB565 -> GL20.GL_UNSIGNED_SHORT_5_6_5
                GDX2D_FORMAT_RGBA4444 -> GL20.GL_UNSIGNED_SHORT_4_4_4_4
                else -> throw GdxRuntimeException("unknown format: $format")
            }
        }

        fun newPixmap(`in`: java.io.InputStream, requestedFormat: Int): Gdx2DPixmap? {
            return try {
                Gdx2DPixmap(`in`, requestedFormat)
            } catch (e: IOException) {
                null
            }
        }

        fun newPixmap(width: Int, height: Int, format: Int): Gdx2DPixmap? {
            return try {
                Gdx2DPixmap(width, height, format)
            } catch (e: java.lang.IllegalArgumentException) {
                null
            }
        }

        private fun getFormatString(format: Int): String {
            return when (format) {
                GDX2D_FORMAT_ALPHA -> "alpha"
                GDX2D_FORMAT_LUMINANCE_ALPHA -> "luminance alpha"
                GDX2D_FORMAT_RGB888 -> "rgb888"
                GDX2D_FORMAT_RGBA8888 -> "rgba8888"
                GDX2D_FORMAT_RGB565 -> "rgb565"
                GDX2D_FORMAT_RGBA4444 -> "rgba4444"
                else -> "unknown"
            }
        }

        // @off
        /*JNI
	#include <gdx2d/gdx2d.h>
	#include <stdlib.h>
	 */
        private external fun load(nativeData: LongArray, buffer: ByteArray, offset: Int, len: Int): java.nio.ByteBuffer /*MANUAL
		const unsigned char* p_buffer = (const unsigned char*)env->GetPrimitiveArrayCritical(buffer, 0);
		gdx2d_pixmap* pixmap = gdx2d_load(p_buffer + offset, len);
		env->ReleasePrimitiveArrayCritical(buffer, (char*)p_buffer, 0);

		if(pixmap==0)
			return 0;

		jobject pixel_buffer = env->NewDirectByteBuffer((void*)pixmap->pixels, pixmap->width * pixmap->height * gdx2d_bytes_per_pixel(pixmap->format));
		jlong* p_native_data = (jlong*)env->GetPrimitiveArrayCritical(nativeData, 0);
		p_native_data[0] = (jlong)pixmap;
		p_native_data[1] = pixmap->width;
		p_native_data[2] = pixmap->height;
		p_native_data[3] = pixmap->format;
		env->ReleasePrimitiveArrayCritical(nativeData, p_native_data, 0);

		return pixel_buffer;
	 */
        private external fun newPixmap(nativeData: LongArray, width: Int, height: Int, format: Int): java.nio.ByteBuffer? /*MANUAL
		gdx2d_pixmap* pixmap = gdx2d_new(width, height, format);
		if(pixmap==0)
			return 0;

		jobject pixel_buffer = env->NewDirectByteBuffer((void*)pixmap->pixels, pixmap->width * pixmap->height * gdx2d_bytes_per_pixel(pixmap->format));
		jlong* p_native_data = (jlong*)env->GetPrimitiveArrayCritical(nativeData, 0);
		p_native_data[0] = (jlong)pixmap;
		p_native_data[1] = pixmap->width;
		p_native_data[2] = pixmap->height;
		p_native_data[3] = pixmap->format;
		env->ReleasePrimitiveArrayCritical(nativeData, p_native_data, 0);

		return pixel_buffer;
	 */
        private external fun free(pixmap: Long) /*
		gdx2d_free((gdx2d_pixmap*)pixmap);
	 */
        private external fun clear(pixmap: Long, color: Int) /*
		gdx2d_clear((gdx2d_pixmap*)pixmap, color);
	 */
        private external fun setPixel(pixmap: Long, x: Int, y: Int, color: Int) /*
		gdx2d_set_pixel((gdx2d_pixmap*)pixmap, x, y, color);
	 */
        private external fun getPixel(pixmap: Long, x: Int, y: Int): Int /*
		return gdx2d_get_pixel((gdx2d_pixmap*)pixmap, x, y);
	 */
        private external fun drawLine(pixmap: Long, x: Int, y: Int, x2: Int, y2: Int, color: Int) /*
		gdx2d_draw_line((gdx2d_pixmap*)pixmap, x, y, x2, y2, color);
	 */
        private external fun drawRect(pixmap: Long, x: Int, y: Int, width: Int, height: Int, color: Int) /*
		gdx2d_draw_rect((gdx2d_pixmap*)pixmap, x, y, width, height, color);
	 */
        private external fun drawCircle(pixmap: Long, x: Int, y: Int, radius: Int, color: Int) /*
		gdx2d_draw_circle((gdx2d_pixmap*)pixmap, x, y, radius, color);
	 */
        private external fun fillRect(pixmap: Long, x: Int, y: Int, width: Int, height: Int, color: Int) /*
		gdx2d_fill_rect((gdx2d_pixmap*)pixmap, x, y, width, height, color);
	 */
        private external fun fillCircle(pixmap: Long, x: Int, y: Int, radius: Int, color: Int) /*
		gdx2d_fill_circle((gdx2d_pixmap*)pixmap, x, y, radius, color);
	 */
        private external fun fillTriangle(pixmap: Long, x1: Int, y1: Int, x2: Int, y2: Int, x3: Int, y3: Int, color: Int) /*
		gdx2d_fill_triangle((gdx2d_pixmap*)pixmap, x1, y1, x2, y2, x3, y3, color);
	 */
        private external fun drawPixmap(src: Long, dst: Long, srcX: Int, srcY: Int, srcWidth: Int, srcHeight: Int, dstX: Int,
                                        dstY: Int, dstWidth: Int, dstHeight: Int) /*
		gdx2d_draw_pixmap((gdx2d_pixmap*)src, (gdx2d_pixmap*)dst, srcX, srcY, srcWidth, srcHeight, dstX, dstY, dstWidth, dstHeight);
		 */

        private external fun setBlend(src: Long, blend: Int) /*
		gdx2d_set_blend((gdx2d_pixmap*)src, blend);
	 */
        private external fun setScale(src: Long, scale: Int) /*
		gdx2d_set_scale((gdx2d_pixmap*)src, scale);
	 */

        /*
     return env->NewStringUTF(gdx2d_get_failure_reason());
	 */
        val failureReason: String external get
    }
}
