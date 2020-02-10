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
package com.badlogic.gdx.graphics

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Pixmap.Format
import com.badlogic.gdx.graphics.g2d.Gdx2DPixmap
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.GdxRuntimeException

/**
 *
 *
 * A Pixmap represents an image in memory. It has a width and height expressed in pixels as well as a [Format] specifying
 * the number and order of color components per pixel. Coordinates of pixels are specified with respect to the top left corner of
 * the image, with the x-axis pointing to the right and the y-axis pointing downwards.
 *
 *
 * By default all methods use blending. You can disable blending with [Pixmap.setBlending], which may reduce
 * blitting time by ~30%. The [Pixmap.drawPixmap] method will scale and
 * stretch the source image to a target image. There either nearest neighbour or bilinear filtering can be used.
 *
 *
 * A Pixmap stores its data in native heap memory. It is mandatory to call [Pixmap.dispose] when the pixmap is no longer
 * needed, otherwise memory leaks will result
 *
 * @author badlogicgames@gmail.com
 */
class Pixmap : Disposable {

    /**
     * Different pixel formats.
     *
     * @author mzechner
     */
    enum class Format {

        Alpha, Intensity, LuminanceAlpha, RGB565, RGBA4444, RGB888, RGBA8888;

        companion object {
            fun toGdx2DPixmapFormat(format: Format): Int {
                if (format == Alpha) return Gdx2DPixmap.GDX2D_FORMAT_ALPHA
                if (format == Intensity) return Gdx2DPixmap.GDX2D_FORMAT_ALPHA
                if (format == LuminanceAlpha) return Gdx2DPixmap.GDX2D_FORMAT_LUMINANCE_ALPHA
                if (format == RGB565) return Gdx2DPixmap.GDX2D_FORMAT_RGB565
                if (format == RGBA4444) return Gdx2DPixmap.GDX2D_FORMAT_RGBA4444
                if (format == RGB888) return Gdx2DPixmap.GDX2D_FORMAT_RGB888
                if (format == RGBA8888) return Gdx2DPixmap.GDX2D_FORMAT_RGBA8888
                throw GdxRuntimeException("Unknown Format: $format")
            }

            fun fromGdx2DPixmapFormat(format: Int): Format {
                if (format == Gdx2DPixmap.GDX2D_FORMAT_ALPHA) return Alpha
                if (format == Gdx2DPixmap.GDX2D_FORMAT_LUMINANCE_ALPHA) return LuminanceAlpha
                if (format == Gdx2DPixmap.GDX2D_FORMAT_RGB565) return RGB565
                if (format == Gdx2DPixmap.GDX2D_FORMAT_RGBA4444) return RGBA4444
                if (format == Gdx2DPixmap.GDX2D_FORMAT_RGB888) return RGB888
                if (format == Gdx2DPixmap.GDX2D_FORMAT_RGBA8888) return RGBA8888
                throw GdxRuntimeException("Unknown Gdx2DPixmap Format: $format")
            }

            fun toGlFormat(format: Format): Int {
                return Gdx2DPixmap.toGlFormat(toGdx2DPixmapFormat(format))
            }

            fun toGlType(format: Format): Int {
                return Gdx2DPixmap.toGlType(toGdx2DPixmapFormat(format))
            }
        }
    }

    /**
     * Blending functions to be set with [Pixmap.setBlending].
     *
     * @author mzechner
     */
    enum class Blending {

        None, SourceOver
    }

    /**
     * Filters to be used with [Pixmap.drawPixmap].
     *
     * @author mzechner
     */
    enum class Filter {

        NearestNeighbour, BiLinear
    }

    /**
     * @return the currently set [Blending]
     */
    /**
     * Sets the type of [Blending] to be used for all operations. Default is [Blending.SourceOver].
     *
     * @param blending the blending type
     */
    var blending = Blending.SourceOver
        set(blending) {
            field = blending
            pixmap.setBlend(if (blending == Blending.None) 0 else 1)
        }
    /**
     * @return the currently set [Filter]
     */
    /**
     * Sets the type of interpolation [Filter] to be used in conjunction with
     * [Pixmap.drawPixmap].
     *
     * @param filter the filter.
     */
    var filter = Filter.BiLinear
        set(filter) {
            field = filter
            pixmap.setScale(if (filter == Filter.NearestNeighbour) Gdx2DPixmap.GDX2D_SCALE_NEAREST else Gdx2DPixmap.GDX2D_SCALE_LINEAR)
        }
    val pixmap: Gdx2DPixmap
    var color = 0
    var isDisposed = false
        private set

    /**
     * Creates a new Pixmap instance with the given width, height and format.
     *
     * @param width  the width in pixels
     * @param height the height in pixels
     * @param format the [Format]
     */
    constructor(width: Int, height: Int, format: Format) {
        pixmap = Gdx2DPixmap(width, height, Format.toGdx2DPixmapFormat(format))
        setColor(0f, 0f, 0f, 0f)
        fill()
    }

    /**
     * Creates a new Pixmap instance from the given encoded image data. The image can be encoded as JPEG, PNG or BMP.
     *
     * @param encodedData the encoded image data
     * @param offset      the offset
     * @param len         the length
     */
    constructor(encodedData: ByteArray?, offset: Int, len: Int) {
        try {
            pixmap = Gdx2DPixmap(encodedData, offset, len, 0)
        } catch (e: IOException) {
            throw GdxRuntimeException("Couldn't load pixmap from image data", e)
        }
    }

    /**
     * Creates a new Pixmap instance from the given file. The file must be a Png, Jpeg or Bitmap. Paletted formats are not
     * supported.
     *
     * @param file the [FileHandle]
     */
    constructor(file: FileHandle) {
        try {
            val bytes = file.readBytes()
            pixmap = Gdx2DPixmap(bytes, 0, bytes.size, 0)
        } catch (e: java.lang.Exception) {
            throw GdxRuntimeException("Couldn't load file: $file", e)
        }
    }

    /**
     * Constructs a new Pixmap from a [Gdx2DPixmap].
     *
     * @param pixmap
     */
    constructor(pixmap: Gdx2DPixmap) {
        this.pixmap = pixmap
    }

    /**
     * Sets the color for the following drawing operations
     *
     * @param color the color, encoded as RGBA8888
     */
    fun setColor(color: Int) {
        this.color = color
    }

    /**
     * Sets the color for the following drawing operations.
     *
     * @param r The red component.
     * @param g The green component.
     * @param b The blue component.
     * @param a The alpha component.
     */
    fun setColor(r: Float, g: Float, b: Float, a: Float) {
        color = Color.rgba8888(r, g, b, a)
    }

    /**
     * Sets the color for the following drawing operations.
     *
     * @param color The color.
     */
    fun setColor(color: Color) {
        this.color = Color.rgba8888(color.r, color.g, color.b, color.a)
    }

    /**
     * Fills the complete bitmap with the currently set color.
     */
    fun fill() {
        pixmap.clear(color)
    }
    // /**
    // * Sets the width in pixels of strokes.
    // *
    // * @param width The stroke width in pixels.
    // */
    // public void setStrokeWidth (int width);
    /**
     * Draws a line between the given coordinates using the currently set color.
     *
     * @param x  The x-coodinate of the first point
     * @param y  The y-coordinate of the first point
     * @param x2 The x-coordinate of the first point
     * @param y2 The y-coordinate of the first point
     */
    fun drawLine(x: Int, y: Int, x2: Int, y2: Int) {
        pixmap.drawLine(x, y, x2, y2, color)
    }

    /**
     * Draws a rectangle outline starting at x, y extending by width to the right and by height downwards (y-axis points downwards)
     * using the current color.
     *
     * @param x      The x coordinate
     * @param y      The y coordinate
     * @param width  The width in pixels
     * @param height The height in pixels
     */
    fun drawRectangle(x: Int, y: Int, width: Int, height: Int) {
        pixmap.drawRect(x, y, width, height, color)
    }
    /**
     * Draws an area from another Pixmap to this Pixmap.
     *
     * @param pixmap    The other Pixmap
     * @param x         The target x-coordinate (top left corner)
     * @param y         The target y-coordinate (top left corner)
     * @param srcx      The source x-coordinate (top left corner)
     * @param srcy      The source y-coordinate (top left corner);
     * @param srcWidth  The width of the area from the other Pixmap in pixels
     * @param srcHeight The height of the area from the other Pixmap in pixels
     */
    /**
     * Draws an area from another Pixmap to this Pixmap.
     *
     * @param pixmap The other Pixmap
     * @param x      The target x-coordinate (top left corner)
     * @param y      The target y-coordinate (top left corner)
     */
    @JvmOverloads
    fun drawPixmap(pixmap: Pixmap, x: Int, y: Int, srcx: Int = 0, srcy: Int = 0, srcWidth: Int = pixmap.width, srcHeight: Int = pixmap.height) {
        this.pixmap.drawPixmap(pixmap.pixmap, srcx, srcy, x, y, srcWidth, srcHeight)
    }

    /**
     * Draws an area from another Pixmap to this Pixmap. This will automatically scale and stretch the source image to the
     * specified target rectangle. Use [Pixmap.setFilter] to specify the type of filtering to be used (nearest
     * neighbour or bilinear).
     *
     * @param pixmap    The other Pixmap
     * @param srcx      The source x-coordinate (top left corner)
     * @param srcy      The source y-coordinate (top left corner);
     * @param srcWidth  The width of the area from the other Pixmap in pixels
     * @param srcHeight The height of the area from the other Pixmap in pixels
     * @param dstx      The target x-coordinate (top left corner)
     * @param dsty      The target y-coordinate (top left corner)
     * @param dstWidth  The target width
     * @param dstHeight the target height
     */
    fun drawPixmap(pixmap: Pixmap, srcx: Int, srcy: Int, srcWidth: Int, srcHeight: Int, dstx: Int, dsty: Int, dstWidth: Int,
                   dstHeight: Int) {
        this.pixmap.drawPixmap(pixmap.pixmap, srcx, srcy, srcWidth, srcHeight, dstx, dsty, dstWidth, dstHeight)
    }

    /**
     * Fills a rectangle starting at x, y extending by width to the right and by height downwards (y-axis points downwards) using
     * the current color.
     *
     * @param x      The x coordinate
     * @param y      The y coordinate
     * @param width  The width in pixels
     * @param height The height in pixels
     */
    fun fillRectangle(x: Int, y: Int, width: Int, height: Int) {
        pixmap.fillRect(x, y, width, height, color)
    }

    /**
     * Draws a circle outline with the center at x,y and a radius using the current color and stroke width.
     *
     * @param x      The x-coordinate of the center
     * @param y      The y-coordinate of the center
     * @param radius The radius in pixels
     */
    fun drawCircle(x: Int, y: Int, radius: Int) {
        pixmap.drawCircle(x, y, radius, color)
    }

    /**
     * Fills a circle with the center at x,y and a radius using the current color.
     *
     * @param x      The x-coordinate of the center
     * @param y      The y-coordinate of the center
     * @param radius The radius in pixels
     */
    fun fillCircle(x: Int, y: Int, radius: Int) {
        pixmap.fillCircle(x, y, radius, color)
    }

    /**
     * Fills a triangle with vertices at x1,y1 and x2,y2 and x3,y3 using the current color.
     *
     * @param x1 The x-coordinate of vertex 1
     * @param y1 The y-coordinate of vertex 1
     * @param x2 The x-coordinate of vertex 2
     * @param y2 The y-coordinate of vertex 2
     * @param x3 The x-coordinate of vertex 3
     * @param y3 The y-coordinate of vertex 3
     */
    fun fillTriangle(x1: Int, y1: Int, x2: Int, y2: Int, x3: Int, y3: Int) {
        pixmap.fillTriangle(x1, y1, x2, y2, x3, y3, color)
    }

    /**
     * Returns the 32-bit RGBA8888 value of the pixel at x, y. For Alpha formats the RGB components will be one.
     *
     * @param x The x-coordinate
     * @param y The y-coordinate
     * @return The pixel color in RGBA8888 format.
     */
    fun getPixel(x: Int, y: Int): Int {
        return pixmap.getPixel(x, y)
    }

    /**
     * @return The width of the Pixmap in pixels.
     */
    val width: Int
        get() = pixmap.getWidth()

    /**
     * @return The height of the Pixmap in pixels.
     */
    val height: Int
        get() = pixmap.getHeight()

    /**
     * Releases all resources associated with this Pixmap.
     */
    fun dispose() {
        if (isDisposed) throw GdxRuntimeException("Pixmap already disposed!")
        pixmap.dispose()
        isDisposed = true
    }

    /**
     * Draws a pixel at the given location with the current color.
     *
     * @param x the x-coordinate
     * @param y the y-coordinate
     */
    fun drawPixel(x: Int, y: Int) {
        pixmap.setPixel(x, y, color)
    }

    /**
     * Draws a pixel at the given location with the given color.
     *
     * @param x     the x-coordinate
     * @param y     the y-coordinate
     * @param color the color in RGBA8888 format.
     */
    fun drawPixel(x: Int, y: Int, color: Int) {
        pixmap.setPixel(x, y, color)
    }

    /**
     * Returns the OpenGL ES format of this Pixmap. Used as the seventh parameter to
     * [GL20.glTexImage2D].
     *
     * @return one of GL_ALPHA, GL_RGB, GL_RGBA, GL_LUMINANCE, or GL_LUMINANCE_ALPHA.
     */
    val gLFormat: Int
        get() = pixmap.getGLFormat()

    /**
     * Returns the OpenGL ES format of this Pixmap. Used as the third parameter to
     * [GL20.glTexImage2D].
     *
     * @return one of GL_ALPHA, GL_RGB, GL_RGBA, GL_LUMINANCE, or GL_LUMINANCE_ALPHA.
     */
    val gLInternalFormat: Int
        get() = pixmap.getGLInternalFormat()

    /**
     * Returns the OpenGL ES type of this Pixmap. Used as the eighth parameter to
     * [GL20.glTexImage2D].
     *
     * @return one of GL_UNSIGNED_BYTE, GL_UNSIGNED_SHORT_5_6_5, GL_UNSIGNED_SHORT_4_4_4_4
     */
    val gLType: Int
        get() = pixmap.getGLType()

    /**
     * Returns the direct ByteBuffer holding the pixel data. For the format Alpha each value is encoded as a byte. For the format
     * LuminanceAlpha the luminance is the first byte and the alpha is the second byte of the pixel. For the formats RGB888 and
     * RGBA8888 the color components are stored in a single byte each in the order red, green, blue (alpha). For the formats RGB565
     * and RGBA4444 the pixel colors are stored in shorts in machine dependent order.
     *
     * @return the direct [ByteBuffer] holding the pixel data.
     */
    val pixels: java.nio.ByteBuffer
        get() {
            if (isDisposed) throw GdxRuntimeException("Pixmap already disposed")
            return pixmap.getPixels()
        }

    /**
     * @return the [Format] of this Pixmap.
     */
    val format: Format
        get() = Format.fromGdx2DPixmapFormat(pixmap.getFormat())
}
