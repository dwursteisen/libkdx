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

import Mesh.VertexDataType
import com.badlogic.gdx.ApplicationListener
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Mesh
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.VertexAttribute
import com.badlogic.gdx.graphics.VertexAttributes.Usage
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.graphics.g2d.Sprite.SPRITE_SIZE
import com.badlogic.gdx.graphics.g2d.Sprite.VERTEX_SIZE
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasSprite
import com.badlogic.gdx.graphics.g2d.TextureAtlas.TextureAtlasData
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.GdxRuntimeException
import com.badlogic.gdx.utils.IntArray
import com.badlogic.gdx.utils.NumberUtils
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.lang.IllegalStateException
import java.nio.FloatBuffer
import kotlin.jvm.Throws

/** Draws 2D images, optimized for geometry that does not change. Sprites and/or textures are cached and given an ID, which can
 * later be used for drawing. The size, color, and texture region for each cached image cannot be modified. This information is
 * stored in video memory and does not have to be sent to the GPU each time it is drawn.<br></br>
 * <br></br>
 * To cache [sprites][Sprite] or [textures][Texture], first call [SpriteCache.beginCache], then call the
 * appropriate add method to define the images. To complete the cache, call [SpriteCache.endCache] and store the returned
 * cache ID.<br></br>
 * <br></br>
 * To draw with SpriteCache, first call [.begin], then call [.draw] with a cache ID. When SpriteCache drawing
 * is complete, call [.end].<br></br>
 * <br></br>
 * By default, SpriteCache draws using screen coordinates and uses an x-axis pointing to the right, an y-axis pointing upwards and
 * the origin is the bottom left corner of the screen. The default transformation and projection matrices can be changed. If the
 * screen is [resized][ApplicationListener.resize], the SpriteCache's matrices must be updated. For example:<br></br>
 * `cache.getProjectionMatrix().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());`<br></br>
 * <br></br>
 * Note that SpriteCache does not manage blending. You will need to enable blending (*Gdx.gl.glEnable(GL10.GL_BLEND);*) and
 * set the blend func as needed before or between calls to [.draw].<br></br>
 * <br></br>
 * SpriteCache is managed. If the OpenGL context is lost and the restored, all OpenGL resources a SpriteCache uses internally are
 * restored.<br></br>
 * <br></br>
 * SpriteCache is a reasonably heavyweight object. Typically only one instance should be used for an entire application.<br></br>
 * <br></br>
 * SpriteCache works with OpenGL ES 1.x and 2.0. For 2.0, it uses its own custom shader to draw.<br></br>
 * <br></br>
 * SpriteCache must be disposed once it is no longer needed.
 * @author Nathan Sweet
 */
class SpriteCache(size: Int, private val shader: ShaderProgram?, useIndices: Boolean) : Disposable {

    private val mesh: Mesh
    var isDrawing = false
        private set
    private val transformMatrix: Matrix4 = Matrix4()
    private val projectionMatrix: Matrix4 = Matrix4()
    private val caches: Array<Cache> = Array()
    private val combinedMatrix: Matrix4 = Matrix4()
    private var currentCache: Cache? = null
    private val textures: Array<Texture> = Array(8)
    private val counts = IntArray(8)
    private val color: Color = Color(1, 1, 1, 1)
    private var colorPacked: Float = Color.WHITE_FLOAT_BITS

    /** Returns the custom shader, or null if the default shader is being used.  */
    var customShader: ShaderProgram? = null
        private set

    /** Number of render calls since the last [.begin].  */
    var renderCalls = 0

    /** Number of rendering calls, ever. Will not be reset unless set manually.  */
    var totalRenderCalls = 0
    /** Creates a cache with the specified size, using a default shader if OpenGL ES 2.0 is being used.
     * @param size The maximum number of images this cache can hold. The memory required to hold the images is allocated up front.
     * Max of 8191 if indices are used.
     * @param useIndices If true, indexed geometry will be used.
     */
    /** Creates a cache that uses indexed geometry and can contain up to 1000 images.  */
    @JvmOverloads
    constructor(size: Int = 1000, useIndices: Boolean = false) : this(size, createDefaultShader(), useIndices) {
    }

    /** Sets the color used to tint images when they are added to the SpriteCache. Default is [Color.WHITE].  */
    fun setColor(tint: Color) {
        color.set(tint)
        colorPacked = tint.toFloatBits()
    }

    /** @see .setColor
     */
    fun setColor(r: Float, g: Float, b: Float, a: Float) {
        color.set(r, g, b, a)
        colorPacked = color.toFloatBits()
    }

    fun getColor(): Color {
        return color
    }

    /** Sets the color of this sprite cache, expanding the alpha from 0-254 to 0-255.
     * @see Color.toFloatBits
     */
    var packedColor: Float
        get() = colorPacked
        set(packedColor) {
            Color.abgr8888ToColor(color, packedColor)
            colorPacked = packedColor
        }

    /** Starts the definition of a new cache, allowing the add and [.endCache] methods to be called.  */
    fun beginCache() {
        check(!isDrawing) { "end must be called before beginCache" }
        check(currentCache == null) { "endCache must be called before begin." }
        val verticesPerImage = if (mesh.getNumIndices() > 0) 4 else 6
        currentCache = Cache(caches.size, mesh.getVerticesBuffer().limit())
        caches.add(currentCache)
        mesh.getVerticesBuffer().compact()
    }

    /** Starts the redefinition of an existing cache, allowing the add and [.endCache] methods to be called. If this is not
     * the last cache created, it cannot have more entries added to it than when it was first created. To do that, use
     * [.clear] and then [.begin].  */
    fun beginCache(cacheID: Int) {
        check(!isDrawing) { "end must be called before beginCache" }
        check(currentCache == null) { "endCache must be called before begin." }
        if (cacheID == caches.size - 1) {
            val oldCache: Cache = caches.removeIndex(cacheID)
            mesh.getVerticesBuffer().limit(oldCache.offset)
            beginCache()
            return
        }
        currentCache = caches[cacheID]
        mesh.getVerticesBuffer().position(currentCache!!.offset)
    }

    /** Ends the definition of a cache, returning the cache ID to be used with [.draw].  */
    fun endCache(): Int {
        checkNotNull(currentCache) { "beginCache must be called before endCache." }
        val cache: Cache = currentCache
        val cacheCount: Int = mesh.getVerticesBuffer().position() - cache.offset
        if (cache.textures == null) {
            // New cache.
            cache.maxCount = cacheCount
            cache.textureCount = textures.size
            cache.textures = textures.toArray(Texture::class.java)
            cache.counts = IntArray(cache.textureCount)
            var i = 0
            val n = counts.size
            while (i < n) {
                cache.counts[i] = counts[i]
                i++
            }
            mesh.getVerticesBuffer().flip()
        } else {
            // Redefine existing cache.
            if (cacheCount > cache.maxCount) {
                throw GdxRuntimeException(
                    "If a cache is not the last created, it cannot be redefined with more entries than when it was first created: "
                        + cacheCount + " (" + cache.maxCount + " max)")
            }
            cache.textureCount = textures.size
            if (cache.textures!!.size < cache.textureCount) cache.textures = arrayOfNulls<Texture>(cache.textureCount)
            run {
                var i = 0
                val n = cache.textureCount
                while (i < n) {
                    cache.textures!![i] = textures[i]
                    i++
                }
            }
            if (cache.counts.size < cache.textureCount) cache.counts = IntArray(cache.textureCount)
            var i = 0
            val n = cache.textureCount
            while (i < n) {
                cache.counts[i] = counts[i]
                i++
            }
            val vertices: FloatBuffer = mesh.getVerticesBuffer()
            vertices.position(0)
            val lastCache = caches[caches.size - 1]
            vertices.limit(lastCache.offset + lastCache.maxCount)
        }
        currentCache = null
        textures.clear()
        counts.clear()
        return cache.id
    }

    /** Invalidates all cache IDs and resets the SpriteCache so new caches can be added.  */
    fun clear() {
        caches.clear()
        mesh.getVerticesBuffer().clear().flip()
    }

    /** Adds the specified vertices to the cache. Each vertex should have 5 elements, one for each of the attributes: x, y, color,
     * u, and v. If indexed geometry is used, each image should be specified as 4 vertices, otherwise each image should be
     * specified as 6 vertices.  */
    fun add(texture: Texture?, vertices: FloatArray?, offset: Int, length: Int) {
        checkNotNull(currentCache) { "beginCache must be called before add." }
        val verticesPerImage = if (mesh.getNumIndices() > 0) 4 else 6
        val count: Int = length / (verticesPerImage * VERTEX_SIZE) * 6
        val lastIndex = textures.size - 1
        if (lastIndex < 0 || textures[lastIndex] !== texture) {
            textures.add(texture)
            counts.add(count)
        } else counts.incr(lastIndex, count)
        mesh.getVerticesBuffer().put(vertices, offset, length)
    }

    /** Adds the specified texture to the cache.  */
    fun add(texture: Texture, x: Float, y: Float) {
        val fx2: Float = x + texture.getWidth()
        val fy2: Float = y + texture.getHeight()
        tempVertices[0] = x
        tempVertices[1] = y
        tempVertices[2] = colorPacked
        tempVertices[3] = 0
        tempVertices[4] = 1
        tempVertices[5] = x
        tempVertices[6] = fy2
        tempVertices[7] = colorPacked
        tempVertices[8] = 0
        tempVertices[9] = 0
        tempVertices[10] = fx2
        tempVertices[11] = fy2
        tempVertices[12] = colorPacked
        tempVertices[13] = 1
        tempVertices[14] = 0
        if (mesh.getNumIndices() > 0) {
            tempVertices[15] = fx2
            tempVertices[16] = y
            tempVertices[17] = colorPacked
            tempVertices[18] = 1
            tempVertices[19] = 1
            add(texture, tempVertices, 0, 20)
        } else {
            tempVertices[15] = fx2
            tempVertices[16] = fy2
            tempVertices[17] = colorPacked
            tempVertices[18] = 1
            tempVertices[19] = 0
            tempVertices[20] = fx2
            tempVertices[21] = y
            tempVertices[22] = colorPacked
            tempVertices[23] = 1
            tempVertices[24] = 1
            tempVertices[25] = x
            tempVertices[26] = y
            tempVertices[27] = colorPacked
            tempVertices[28] = 0
            tempVertices[29] = 1
            add(texture, tempVertices, 0, 30)
        }
    }

    /** Adds the specified texture to the cache.  */
    fun add(texture: Texture?, x: Float, y: Float, srcWidth: Int, srcHeight: Int, u: Float, v: Float, u2: Float, v2: Float,
            color: Float) {
        val fx2 = x + srcWidth
        val fy2 = y + srcHeight
        tempVertices[0] = x
        tempVertices[1] = y
        tempVertices[2] = color
        tempVertices[3] = u
        tempVertices[4] = v
        tempVertices[5] = x
        tempVertices[6] = fy2
        tempVertices[7] = color
        tempVertices[8] = u
        tempVertices[9] = v2
        tempVertices[10] = fx2
        tempVertices[11] = fy2
        tempVertices[12] = color
        tempVertices[13] = u2
        tempVertices[14] = v2
        if (mesh.getNumIndices() > 0) {
            tempVertices[15] = fx2
            tempVertices[16] = y
            tempVertices[17] = color
            tempVertices[18] = u2
            tempVertices[19] = v
            add(texture, tempVertices, 0, 20)
        } else {
            tempVertices[15] = fx2
            tempVertices[16] = fy2
            tempVertices[17] = color
            tempVertices[18] = u2
            tempVertices[19] = v2
            tempVertices[20] = fx2
            tempVertices[21] = y
            tempVertices[22] = color
            tempVertices[23] = u2
            tempVertices[24] = v
            tempVertices[25] = x
            tempVertices[26] = y
            tempVertices[27] = color
            tempVertices[28] = u
            tempVertices[29] = v
            add(texture, tempVertices, 0, 30)
        }
    }

    /** Adds the specified texture to the cache.  */
    fun add(texture: Texture, x: Float, y: Float, srcX: Int, srcY: Int, srcWidth: Int, srcHeight: Int) {
        val invTexWidth: Float = 1.0f / texture.getWidth()
        val invTexHeight: Float = 1.0f / texture.getHeight()
        val u = srcX * invTexWidth
        val v = (srcY + srcHeight) * invTexHeight
        val u2 = (srcX + srcWidth) * invTexWidth
        val v2 = srcY * invTexHeight
        val fx2 = x + srcWidth
        val fy2 = y + srcHeight
        tempVertices[0] = x
        tempVertices[1] = y
        tempVertices[2] = colorPacked
        tempVertices[3] = u
        tempVertices[4] = v
        tempVertices[5] = x
        tempVertices[6] = fy2
        tempVertices[7] = colorPacked
        tempVertices[8] = u
        tempVertices[9] = v2
        tempVertices[10] = fx2
        tempVertices[11] = fy2
        tempVertices[12] = colorPacked
        tempVertices[13] = u2
        tempVertices[14] = v2
        if (mesh.getNumIndices() > 0) {
            tempVertices[15] = fx2
            tempVertices[16] = y
            tempVertices[17] = colorPacked
            tempVertices[18] = u2
            tempVertices[19] = v
            add(texture, tempVertices, 0, 20)
        } else {
            tempVertices[15] = fx2
            tempVertices[16] = fy2
            tempVertices[17] = colorPacked
            tempVertices[18] = u2
            tempVertices[19] = v2
            tempVertices[20] = fx2
            tempVertices[21] = y
            tempVertices[22] = colorPacked
            tempVertices[23] = u2
            tempVertices[24] = v
            tempVertices[25] = x
            tempVertices[26] = y
            tempVertices[27] = colorPacked
            tempVertices[28] = u
            tempVertices[29] = v
            add(texture, tempVertices, 0, 30)
        }
    }

    /** Adds the specified texture to the cache.  */
    fun add(texture: Texture, x: Float, y: Float, width: Float, height: Float, srcX: Int, srcY: Int, srcWidth: Int,
            srcHeight: Int, flipX: Boolean, flipY: Boolean) {
        val invTexWidth: Float = 1.0f / texture.getWidth()
        val invTexHeight: Float = 1.0f / texture.getHeight()
        var u = srcX * invTexWidth
        var v = (srcY + srcHeight) * invTexHeight
        var u2 = (srcX + srcWidth) * invTexWidth
        var v2 = srcY * invTexHeight
        val fx2 = x + width
        val fy2 = y + height
        if (flipX) {
            val tmp = u
            u = u2
            u2 = tmp
        }
        if (flipY) {
            val tmp = v
            v = v2
            v2 = tmp
        }
        tempVertices[0] = x
        tempVertices[1] = y
        tempVertices[2] = colorPacked
        tempVertices[3] = u
        tempVertices[4] = v
        tempVertices[5] = x
        tempVertices[6] = fy2
        tempVertices[7] = colorPacked
        tempVertices[8] = u
        tempVertices[9] = v2
        tempVertices[10] = fx2
        tempVertices[11] = fy2
        tempVertices[12] = colorPacked
        tempVertices[13] = u2
        tempVertices[14] = v2
        if (mesh.getNumIndices() > 0) {
            tempVertices[15] = fx2
            tempVertices[16] = y
            tempVertices[17] = colorPacked
            tempVertices[18] = u2
            tempVertices[19] = v
            add(texture, tempVertices, 0, 20)
        } else {
            tempVertices[15] = fx2
            tempVertices[16] = fy2
            tempVertices[17] = colorPacked
            tempVertices[18] = u2
            tempVertices[19] = v2
            tempVertices[20] = fx2
            tempVertices[21] = y
            tempVertices[22] = colorPacked
            tempVertices[23] = u2
            tempVertices[24] = v
            tempVertices[25] = x
            tempVertices[26] = y
            tempVertices[27] = colorPacked
            tempVertices[28] = u
            tempVertices[29] = v
            add(texture, tempVertices, 0, 30)
        }
    }

    /** Adds the specified texture to the cache.  */
    fun add(texture: Texture, x: Float, y: Float, originX: Float, originY: Float, width: Float, height: Float, scaleX: Float,
            scaleY: Float, rotation: Float, srcX: Int, srcY: Int, srcWidth: Int, srcHeight: Int, flipX: Boolean, flipY: Boolean) {

        // bottom left and top right corner points relative to origin
        val worldOriginX = x + originX
        val worldOriginY = y + originY
        var fx = -originX
        var fy = -originY
        var fx2 = width - originX
        var fy2 = height - originY

        // scale
        if (scaleX != 1f || scaleY != 1f) {
            fx *= scaleX
            fy *= scaleY
            fx2 *= scaleX
            fy2 *= scaleY
        }

        // construct corner points, start from top left and go counter clockwise
        val p1x = fx
        val p1y = fy
        val p2x = fx
        val p2y = fy2
        val p3x = fx2
        val p3y = fy2
        val p4x = fx2
        val p4y = fy
        var x1: Float
        var y1: Float
        var x2: Float
        var y2: Float
        var x3: Float
        var y3: Float
        var x4: Float
        var y4: Float

        // rotate
        if (rotation != 0f) {
            val cos: Float = MathUtils.cosDeg(rotation)
            val sin: Float = MathUtils.sinDeg(rotation)
            x1 = cos * p1x - sin * p1y
            y1 = sin * p1x + cos * p1y
            x2 = cos * p2x - sin * p2y
            y2 = sin * p2x + cos * p2y
            x3 = cos * p3x - sin * p3y
            y3 = sin * p3x + cos * p3y
            x4 = x1 + (x3 - x2)
            y4 = y3 - (y2 - y1)
        } else {
            x1 = p1x
            y1 = p1y
            x2 = p2x
            y2 = p2y
            x3 = p3x
            y3 = p3y
            x4 = p4x
            y4 = p4y
        }
        x1 += worldOriginX
        y1 += worldOriginY
        x2 += worldOriginX
        y2 += worldOriginY
        x3 += worldOriginX
        y3 += worldOriginY
        x4 += worldOriginX
        y4 += worldOriginY
        val invTexWidth: Float = 1.0f / texture.getWidth()
        val invTexHeight: Float = 1.0f / texture.getHeight()
        var u = srcX * invTexWidth
        var v = (srcY + srcHeight) * invTexHeight
        var u2 = (srcX + srcWidth) * invTexWidth
        var v2 = srcY * invTexHeight
        if (flipX) {
            val tmp = u
            u = u2
            u2 = tmp
        }
        if (flipY) {
            val tmp = v
            v = v2
            v2 = tmp
        }
        tempVertices[0] = x1
        tempVertices[1] = y1
        tempVertices[2] = colorPacked
        tempVertices[3] = u
        tempVertices[4] = v
        tempVertices[5] = x2
        tempVertices[6] = y2
        tempVertices[7] = colorPacked
        tempVertices[8] = u
        tempVertices[9] = v2
        tempVertices[10] = x3
        tempVertices[11] = y3
        tempVertices[12] = colorPacked
        tempVertices[13] = u2
        tempVertices[14] = v2
        if (mesh.getNumIndices() > 0) {
            tempVertices[15] = x4
            tempVertices[16] = y4
            tempVertices[17] = colorPacked
            tempVertices[18] = u2
            tempVertices[19] = v
            add(texture, tempVertices, 0, 20)
        } else {
            tempVertices[15] = x3
            tempVertices[16] = y3
            tempVertices[17] = colorPacked
            tempVertices[18] = u2
            tempVertices[19] = v2
            tempVertices[20] = x4
            tempVertices[21] = y4
            tempVertices[22] = colorPacked
            tempVertices[23] = u2
            tempVertices[24] = v
            tempVertices[25] = x1
            tempVertices[26] = y1
            tempVertices[27] = colorPacked
            tempVertices[28] = u
            tempVertices[29] = v
            add(texture, tempVertices, 0, 30)
        }
    }

    /** Adds the specified region to the cache.  */
    fun add(region: TextureRegion, x: Float, y: Float) {
        add(region, x, y, region.getRegionWidth().toFloat(), region.getRegionHeight().toFloat())
    }

    /** Adds the specified region to the cache.  */
    fun add(region: TextureRegion, x: Float, y: Float, width: Float, height: Float) {
        val fx2 = x + width
        val fy2 = y + height
        val u: Float = region.u
        val v: Float = region.v2
        val u2: Float = region.u2
        val v2: Float = region.v
        tempVertices[0] = x
        tempVertices[1] = y
        tempVertices[2] = colorPacked
        tempVertices[3] = u
        tempVertices[4] = v
        tempVertices[5] = x
        tempVertices[6] = fy2
        tempVertices[7] = colorPacked
        tempVertices[8] = u
        tempVertices[9] = v2
        tempVertices[10] = fx2
        tempVertices[11] = fy2
        tempVertices[12] = colorPacked
        tempVertices[13] = u2
        tempVertices[14] = v2
        if (mesh.getNumIndices() > 0) {
            tempVertices[15] = fx2
            tempVertices[16] = y
            tempVertices[17] = colorPacked
            tempVertices[18] = u2
            tempVertices[19] = v
            add(region.texture, tempVertices, 0, 20)
        } else {
            tempVertices[15] = fx2
            tempVertices[16] = fy2
            tempVertices[17] = colorPacked
            tempVertices[18] = u2
            tempVertices[19] = v2
            tempVertices[20] = fx2
            tempVertices[21] = y
            tempVertices[22] = colorPacked
            tempVertices[23] = u2
            tempVertices[24] = v
            tempVertices[25] = x
            tempVertices[26] = y
            tempVertices[27] = colorPacked
            tempVertices[28] = u
            tempVertices[29] = v
            add(region.texture, tempVertices, 0, 30)
        }
    }

    /** Adds the specified region to the cache.  */
    fun add(region: TextureRegion, x: Float, y: Float, originX: Float, originY: Float, width: Float, height: Float,
            scaleX: Float, scaleY: Float, rotation: Float) {

        // bottom left and top right corner points relative to origin
        val worldOriginX = x + originX
        val worldOriginY = y + originY
        var fx = -originX
        var fy = -originY
        var fx2 = width - originX
        var fy2 = height - originY

        // scale
        if (scaleX != 1f || scaleY != 1f) {
            fx *= scaleX
            fy *= scaleY
            fx2 *= scaleX
            fy2 *= scaleY
        }

        // construct corner points, start from top left and go counter clockwise
        val p1x = fx
        val p1y = fy
        val p2x = fx
        val p2y = fy2
        val p3x = fx2
        val p3y = fy2
        val p4x = fx2
        val p4y = fy
        var x1: Float
        var y1: Float
        var x2: Float
        var y2: Float
        var x3: Float
        var y3: Float
        var x4: Float
        var y4: Float

        // rotate
        if (rotation != 0f) {
            val cos: Float = MathUtils.cosDeg(rotation)
            val sin: Float = MathUtils.sinDeg(rotation)
            x1 = cos * p1x - sin * p1y
            y1 = sin * p1x + cos * p1y
            x2 = cos * p2x - sin * p2y
            y2 = sin * p2x + cos * p2y
            x3 = cos * p3x - sin * p3y
            y3 = sin * p3x + cos * p3y
            x4 = x1 + (x3 - x2)
            y4 = y3 - (y2 - y1)
        } else {
            x1 = p1x
            y1 = p1y
            x2 = p2x
            y2 = p2y
            x3 = p3x
            y3 = p3y
            x4 = p4x
            y4 = p4y
        }
        x1 += worldOriginX
        y1 += worldOriginY
        x2 += worldOriginX
        y2 += worldOriginY
        x3 += worldOriginX
        y3 += worldOriginY
        x4 += worldOriginX
        y4 += worldOriginY
        val u: Float = region.u
        val v: Float = region.v2
        val u2: Float = region.u2
        val v2: Float = region.v
        tempVertices[0] = x1
        tempVertices[1] = y1
        tempVertices[2] = colorPacked
        tempVertices[3] = u
        tempVertices[4] = v
        tempVertices[5] = x2
        tempVertices[6] = y2
        tempVertices[7] = colorPacked
        tempVertices[8] = u
        tempVertices[9] = v2
        tempVertices[10] = x3
        tempVertices[11] = y3
        tempVertices[12] = colorPacked
        tempVertices[13] = u2
        tempVertices[14] = v2
        if (mesh.getNumIndices() > 0) {
            tempVertices[15] = x4
            tempVertices[16] = y4
            tempVertices[17] = colorPacked
            tempVertices[18] = u2
            tempVertices[19] = v
            add(region.texture, tempVertices, 0, 20)
        } else {
            tempVertices[15] = x3
            tempVertices[16] = y3
            tempVertices[17] = colorPacked
            tempVertices[18] = u2
            tempVertices[19] = v2
            tempVertices[20] = x4
            tempVertices[21] = y4
            tempVertices[22] = colorPacked
            tempVertices[23] = u2
            tempVertices[24] = v
            tempVertices[25] = x1
            tempVertices[26] = y1
            tempVertices[27] = colorPacked
            tempVertices[28] = u
            tempVertices[29] = v
            add(region.texture, tempVertices, 0, 30)
        }
    }

    /** Adds the specified sprite to the cache.  */
    fun add(sprite: Sprite) {
        if (mesh.getNumIndices() > 0) {
            add(sprite.getTexture(), sprite.getVertices(), 0, SPRITE_SIZE)
            return
        }
        val spriteVertices: FloatArray = sprite.getVertices()
        java.lang.System.arraycopy(spriteVertices, 0, tempVertices, 0, 3 * VERTEX_SIZE) // temp0,1,2=sprite0,1,2
        java.lang.System.arraycopy(spriteVertices, 2 * VERTEX_SIZE, tempVertices, 3 * VERTEX_SIZE, VERTEX_SIZE) // temp3=sprite2
        java.lang.System.arraycopy(spriteVertices, 3 * VERTEX_SIZE, tempVertices, 4 * VERTEX_SIZE, VERTEX_SIZE) // temp4=sprite3
        java.lang.System.arraycopy(spriteVertices, 0, tempVertices, 5 * VERTEX_SIZE, VERTEX_SIZE) // temp5=sprite0
        add(sprite.getTexture(), tempVertices, 0, 30)
    }

    /** Prepares the OpenGL state for SpriteCache rendering.  */
    fun begin() {
        check(!isDrawing) { "end must be called before begin." }
        check(currentCache == null) { "endCache must be called before begin" }
        renderCalls = 0
        combinedMatrix.set(projectionMatrix).mul(transformMatrix)
        Gdx.gl20.glDepthMask(false)
        if (customShader != null) {
            customShader!!.begin()
            customShader.setUniformMatrix("u_proj", projectionMatrix)
            customShader.setUniformMatrix("u_trans", transformMatrix)
            customShader.setUniformMatrix("u_projTrans", combinedMatrix)
            customShader!!.setUniformi("u_texture", 0)
            mesh.bind(customShader)
        } else {
            shader!!.begin()
            shader.setUniformMatrix("u_projectionViewMatrix", combinedMatrix)
            shader.setUniformi("u_texture", 0)
            mesh.bind(shader)
        }
        isDrawing = true
    }

    /** Completes rendering for this SpriteCache.  */
    fun end() {
        check(isDrawing) { "begin must be called before end." }
        isDrawing = false
        shader!!.end()
        val gl: GL20 = Gdx.gl20
        gl.glDepthMask(true)
        if (customShader != null) mesh.unbind(customShader) else mesh.unbind(shader)
    }

    /** Draws all the images defined for the specified cache ID.  */
    fun draw(cacheID: Int) {
        check(isDrawing) { "SpriteCache.begin must be called before draw." }
        val cache = caches[cacheID]
        val verticesPerImage = if (mesh.getNumIndices() > 0) 4 else 6
        var offset: Int = cache.offset / (verticesPerImage * VERTEX_SIZE) * 6
        val textures: Array<Texture?>? = cache.textures
        val counts = cache.counts
        val textureCount = cache.textureCount
        for (i in 0 until textureCount) {
            val count = counts[i]
            textures!![i].bind()
            if (customShader != null) mesh.render(customShader, GL20.GL_TRIANGLES, offset, count) else mesh.render(shader, GL20.GL_TRIANGLES, offset, count)
            offset += count
        }
        renderCalls += textureCount
        totalRenderCalls += textureCount
    }

    /** Draws a subset of images defined for the specified cache ID.
     * @param offset The first image to render.
     * @param length The number of images from the first image (inclusive) to render.
     */
    fun draw(cacheID: Int, offset: Int, length: Int) {
        var offset = offset
        var length = length
        check(isDrawing) { "SpriteCache.begin must be called before draw." }
        val cache = caches[cacheID]
        offset = offset * 6 + cache.offset
        length *= 6
        val textures: Array<Texture?>? = cache.textures
        val counts = cache.counts
        val textureCount = cache.textureCount
        var i = 0
        while (i < textureCount) {
            textures!![i].bind()
            var count = counts[i]
            if (count > length) {
                i = textureCount
                count = length
            } else length -= count
            if (customShader != null) mesh.render(customShader, GL20.GL_TRIANGLES, offset, count) else mesh.render(shader, GL20.GL_TRIANGLES, offset, count)
            offset += count
            i++
        }
        renderCalls += cache.textureCount
        totalRenderCalls += textureCount
    }

    /** Releases all resources held by this SpriteCache.  */
    fun dispose() {
        mesh.dispose()
        shader?.dispose()
    }

    fun getProjectionMatrix(): Matrix4 {
        return projectionMatrix
    }

    fun setProjectionMatrix(projection: Matrix4?) {
        check(!isDrawing) { "Can't set the matrix within begin/end." }
        projectionMatrix.set(projection)
    }

    fun getTransformMatrix(): Matrix4 {
        return transformMatrix
    }

    fun setTransformMatrix(transform: Matrix4?) {
        check(!isDrawing) { "Can't set the matrix within begin/end." }
        transformMatrix.set(transform)
    }

    private class Cache(val id: Int, val offset: Int) {
        var maxCount = 0
        var textureCount = 0
        var textures: Array<Texture?>?
        var counts: IntArray
    }

    /** Sets the shader to be used in a GLES 2.0 environment. Vertex position attribute is called "a_position", the texture
     * coordinates attribute is called called "a_texCoords", the color attribute is called "a_color". The projection matrix is
     * uploaded via a mat4 uniform called "u_proj", the transform matrix is uploaded via a uniform called "u_trans", the combined
     * transform and projection matrx is is uploaded via a mat4 uniform called "u_projTrans". The texture sampler is passed via a
     * uniform called "u_texture".
     *
     * Call this method with a null argument to use the default shader.
     *
     * @param shader the [ShaderProgram] or null to use the default shader.
     */
    fun setShader(shader: ShaderProgram?) {
        customShader = shader
    }

    companion object {
        private val tempVertices = FloatArray(VERTEX_SIZE * 6)
        fun createDefaultShader(): ShaderProgram {
            val vertexShader = """attribute vec4 ${ShaderProgram.POSITION_ATTRIBUTE};
attribute vec4 ${ShaderProgram.COLOR_ATTRIBUTE};
attribute vec2 ${ShaderProgram.TEXCOORD_ATTRIBUTE}0;
uniform mat4 u_projectionViewMatrix;
varying vec4 v_color;
varying vec2 v_texCoords;

void main()
{
   v_color = ${ShaderProgram.COLOR_ATTRIBUTE};
   v_color.a = v_color.a * (255.0/254.0);
   v_texCoords = ${ShaderProgram.TEXCOORD_ATTRIBUTE}0;
   gl_Position =  u_projectionViewMatrix * ${ShaderProgram.POSITION_ATTRIBUTE};
}
"""
            val fragmentShader = """#ifdef GL_ES
precision mediump float;
#endif
varying vec4 v_color;
varying vec2 v_texCoords;
uniform sampler2D u_texture;
void main()
{
  gl_FragColor = v_color * texture2D(u_texture, v_texCoords);
}"""
            val shader = ShaderProgram(vertexShader, fragmentShader)
            if (!shader.isCompiled()) throw IllegalArgumentException("Error compiling shader: " + shader.getLog())
            return shader
        }
    }

    /** Creates a cache with the specified size and OpenGL ES 2.0 shader.
     * @param size The maximum number of images this cache can hold. The memory required to hold the images is allocated up front.
     * Max of 8191 if indices are used.
     * @param useIndices If true, indexed geometry will be used.
     */
    init {
        if (useIndices && size > 8191) throw IllegalArgumentException("Can't have more than 8191 sprites per batch: $size")
        mesh = Mesh(true, size * if (useIndices) 4 else 6, if (useIndices) size * 6 else 0, VertexAttribute(Usage.Position, 2,
            ShaderProgram.POSITION_ATTRIBUTE), VertexAttribute(Usage.ColorPacked, 4, ShaderProgram.COLOR_ATTRIBUTE),
            VertexAttribute(Usage.TextureCoordinates, 2, ShaderProgram.TEXCOORD_ATTRIBUTE + "0"))
        mesh.setAutoBind(false)
        if (useIndices) {
            val length = size * 6
            val indices = ShortArray(length)
            var j: Short = 0
            var i = 0
            while (i < length) {
                indices[i + 0] = j
                indices[i + 1] = (j + 1).toShort()
                indices[i + 2] = (j + 2).toShort()
                indices[i + 3] = (j + 2).toShort()
                indices[i + 4] = (j + 3).toShort()
                indices[i + 5] = j
                i += 6
                (j += 4).toShort()
            }
            mesh.setIndices(indices)
        }
        projectionMatrix.setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight())
    }
}
