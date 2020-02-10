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
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Mesh
import com.badlogic.gdx.graphics.Mesh.VertexDataType
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.VertexAttribute
import com.badlogic.gdx.graphics.VertexAttributes.Usage
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.graphics.g2d.SpriteCache
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasSprite
import com.badlogic.gdx.graphics.g2d.TextureAtlas.TextureAtlasData
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.math.Affine2
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.utils.NumberUtils
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.lang.IllegalStateException
import java.nio.FloatBuffer
import kotlin.jvm.Throws

/** Draws batched quads using indices.
 * @see Batch
 *
 * @author mzechner
 * @author Nathan Sweet
 */
class SpriteBatch @JvmOverloads constructor(size: Int = 1000, defaultShader: ShaderProgram? = null) : Batch {

    private val mesh: Mesh
    val vertices: FloatArray
    var idx = 0
    var lastTexture: Texture? = null
    var invTexWidth = 0f
    var invTexHeight = 0f
    override var isDrawing: Boolean = false public get() {
        return field
    }
    private val transformMatrix: Matrix4 = Matrix4()
    private val projectionMatrix: Matrix4 = Matrix4()
    private val combinedMatrix: Matrix4 = Matrix4()
    private var blendingDisabled = false
    override var blendSrcFunc: Int = GL20.GL_SRC_ALPHA
        private set
    override var blendDstFunc: Int = GL20.GL_ONE_MINUS_SRC_ALPHA
        private set
    override var blendSrcFuncAlpha: Int = GL20.GL_SRC_ALPHA
        private set
    override var blendDstFuncAlpha: Int = GL20.GL_ONE_MINUS_SRC_ALPHA
        private set
    private override var shader: ShaderProgram? = null
    private var customShader: ShaderProgram? = null
    private var ownsShader = false
    private val color: Color = Color(1, 1, 1, 1)
    var colorPacked: Float = Color.WHITE_FLOAT_BITS

    /** Number of render calls since the last [.begin].  */
    var renderCalls = 0

    /** Number of rendering calls, ever. Will not be reset unless set manually.  */
    var totalRenderCalls = 0

    /** The maximum number of sprites rendered in one batch so far.  */
    var maxSpritesInBatch = 0
    override fun begin() {
        check(!isDrawing) { "SpriteBatch.end must be called before begin." }
        renderCalls = 0
        Gdx.gl.glDepthMask(false)
        if (customShader != null) customShader!!.begin() else shader!!.begin()
        setupMatrices()
        isDrawing = true
    }

    override fun end() {
        check(isDrawing) { "SpriteBatch.begin must be called before end." }
        if (idx > 0) flush()
        lastTexture = null
        isDrawing = false
        val gl: GL20 = Gdx.gl
        gl.glDepthMask(true)
        if (isBlendingEnabled) gl.glDisable(GL20.GL_BLEND)
        if (customShader != null) customShader!!.end() else shader!!.end()
    }

    override fun setColor(tint: Color) {
        color.set(tint)
        colorPacked = tint.toFloatBits()
    }

    override fun setColor(r: Float, g: Float, b: Float, a: Float) {
        color.set(r, g, b, a)
        colorPacked = color.toFloatBits()
    }

    override fun getColor(): Color? {
        return color
    }

    override var packedColor: Float
        get() = colorPacked
        set(packedColor) {
            Color.abgr8888ToColor(color, packedColor)
            colorPacked = packedColor
        }

    override fun draw(texture: Texture, x: Float, y: Float, originX: Float, originY: Float, width: Float, height: Float, scaleX: Float,
                      scaleY: Float, rotation: Float, srcX: Int, srcY: Int, srcWidth: Int, srcHeight: Int, flipX: Boolean, flipY: Boolean) {
        check(isDrawing) { "SpriteBatch.begin must be called before draw." }
        val vertices = vertices
        if (texture !== lastTexture) switchTexture(texture) else if (idx == vertices.size) //
            flush()

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
        val color = colorPacked
        val idx = idx
        vertices[idx] = x1
        vertices[idx + 1] = y1
        vertices[idx + 2] = color
        vertices[idx + 3] = u
        vertices[idx + 4] = v
        vertices[idx + 5] = x2
        vertices[idx + 6] = y2
        vertices[idx + 7] = color
        vertices[idx + 8] = u
        vertices[idx + 9] = v2
        vertices[idx + 10] = x3
        vertices[idx + 11] = y3
        vertices[idx + 12] = color
        vertices[idx + 13] = u2
        vertices[idx + 14] = v2
        vertices[idx + 15] = x4
        vertices[idx + 16] = y4
        vertices[idx + 17] = color
        vertices[idx + 18] = u2
        vertices[idx + 19] = v
        this.idx = idx + 20
    }

    override fun draw(texture: Texture, x: Float, y: Float, width: Float, height: Float, srcX: Int, srcY: Int, srcWidth: Int,
                      srcHeight: Int, flipX: Boolean, flipY: Boolean) {
        check(isDrawing) { "SpriteBatch.begin must be called before draw." }
        val vertices = vertices
        if (texture !== lastTexture) switchTexture(texture) else if (idx == vertices.size) //
            flush()
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
        val color = colorPacked
        val idx = idx
        vertices[idx] = x
        vertices[idx + 1] = y
        vertices[idx + 2] = color
        vertices[idx + 3] = u
        vertices[idx + 4] = v
        vertices[idx + 5] = x
        vertices[idx + 6] = fy2
        vertices[idx + 7] = color
        vertices[idx + 8] = u
        vertices[idx + 9] = v2
        vertices[idx + 10] = fx2
        vertices[idx + 11] = fy2
        vertices[idx + 12] = color
        vertices[idx + 13] = u2
        vertices[idx + 14] = v2
        vertices[idx + 15] = fx2
        vertices[idx + 16] = y
        vertices[idx + 17] = color
        vertices[idx + 18] = u2
        vertices[idx + 19] = v
        this.idx = idx + 20
    }

    override fun draw(texture: Texture, x: Float, y: Float, srcX: Int, srcY: Int, srcWidth: Int, srcHeight: Int) {
        check(isDrawing) { "SpriteBatch.begin must be called before draw." }
        val vertices = vertices
        if (texture !== lastTexture) switchTexture(texture) else if (idx == vertices.size) //
            flush()
        val u = srcX * invTexWidth
        val v = (srcY + srcHeight) * invTexHeight
        val u2 = (srcX + srcWidth) * invTexWidth
        val v2 = srcY * invTexHeight
        val fx2 = x + srcWidth
        val fy2 = y + srcHeight
        val color = colorPacked
        val idx = idx
        vertices[idx] = x
        vertices[idx + 1] = y
        vertices[idx + 2] = color
        vertices[idx + 3] = u
        vertices[idx + 4] = v
        vertices[idx + 5] = x
        vertices[idx + 6] = fy2
        vertices[idx + 7] = color
        vertices[idx + 8] = u
        vertices[idx + 9] = v2
        vertices[idx + 10] = fx2
        vertices[idx + 11] = fy2
        vertices[idx + 12] = color
        vertices[idx + 13] = u2
        vertices[idx + 14] = v2
        vertices[idx + 15] = fx2
        vertices[idx + 16] = y
        vertices[idx + 17] = color
        vertices[idx + 18] = u2
        vertices[idx + 19] = v
        this.idx = idx + 20
    }

    override fun draw(texture: Texture, x: Float, y: Float, width: Float, height: Float, u: Float, v: Float, u2: Float, v2: Float) {
        check(isDrawing) { "SpriteBatch.begin must be called before draw." }
        val vertices = vertices
        if (texture !== lastTexture) switchTexture(texture) else if (idx == vertices.size) //
            flush()
        val fx2 = x + width
        val fy2 = y + height
        val color = colorPacked
        val idx = idx
        vertices[idx] = x
        vertices[idx + 1] = y
        vertices[idx + 2] = color
        vertices[idx + 3] = u
        vertices[idx + 4] = v
        vertices[idx + 5] = x
        vertices[idx + 6] = fy2
        vertices[idx + 7] = color
        vertices[idx + 8] = u
        vertices[idx + 9] = v2
        vertices[idx + 10] = fx2
        vertices[idx + 11] = fy2
        vertices[idx + 12] = color
        vertices[idx + 13] = u2
        vertices[idx + 14] = v2
        vertices[idx + 15] = fx2
        vertices[idx + 16] = y
        vertices[idx + 17] = color
        vertices[idx + 18] = u2
        vertices[idx + 19] = v
        this.idx = idx + 20
    }

    override fun draw(texture: Texture, x: Float, y: Float) {
        draw(texture, x, y, texture.getWidth(), texture.getHeight())
    }

    override fun draw(texture: Texture, x: Float, y: Float, width: Float, height: Float) {
        check(isDrawing) { "SpriteBatch.begin must be called before draw." }
        val vertices = vertices
        if (texture !== lastTexture) switchTexture(texture) else if (idx == vertices.size) //
            flush()
        val fx2 = x + width
        val fy2 = y + height
        val u = 0f
        val v = 1f
        val u2 = 1f
        val v2 = 0f
        val color = colorPacked
        val idx = idx
        vertices[idx] = x
        vertices[idx + 1] = y
        vertices[idx + 2] = color
        vertices[idx + 3] = u
        vertices[idx + 4] = v
        vertices[idx + 5] = x
        vertices[idx + 6] = fy2
        vertices[idx + 7] = color
        vertices[idx + 8] = u
        vertices[idx + 9] = v2
        vertices[idx + 10] = fx2
        vertices[idx + 11] = fy2
        vertices[idx + 12] = color
        vertices[idx + 13] = u2
        vertices[idx + 14] = v2
        vertices[idx + 15] = fx2
        vertices[idx + 16] = y
        vertices[idx + 17] = color
        vertices[idx + 18] = u2
        vertices[idx + 19] = v
        this.idx = idx + 20
    }

    override fun draw(texture: Texture, spriteVertices: FloatArray?, offset: Int, count: Int) {
        var offset = offset
        var count = count
        check(isDrawing) { "SpriteBatch.begin must be called before draw." }
        val verticesLength = vertices.size
        var remainingVertices = verticesLength
        if (texture !== lastTexture) switchTexture(texture) else {
            remainingVertices -= idx
            if (remainingVertices == 0) {
                flush()
                remainingVertices = verticesLength
            }
        }
        var copyCount: Int = java.lang.Math.min(remainingVertices, count)
        java.lang.System.arraycopy(spriteVertices, offset, vertices, idx, copyCount)
        idx += copyCount
        count -= copyCount
        while (count > 0) {
            offset += copyCount
            flush()
            copyCount = java.lang.Math.min(verticesLength, count)
            java.lang.System.arraycopy(spriteVertices, offset, vertices, 0, copyCount)
            idx += copyCount
            count -= copyCount
        }
    }

    fun draw(region: TextureRegion, x: Float, y: Float) {
        draw(region, x, y, region.getRegionWidth(), region.getRegionHeight())
    }

    fun draw(region: TextureRegion, x: Float, y: Float, width: Float, height: Float) {
        check(isDrawing) { "SpriteBatch.begin must be called before draw." }
        val vertices = vertices
        val texture: Texture = region.texture
        if (texture !== lastTexture) {
            switchTexture(texture)
        } else if (idx == vertices.size) //
            flush()
        val fx2 = x + width
        val fy2 = y + height
        val u: Float = region.u
        val v: Float = region.v2
        val u2: Float = region.u2
        val v2: Float = region.v
        val color = colorPacked
        val idx = idx
        vertices[idx] = x
        vertices[idx + 1] = y
        vertices[idx + 2] = color
        vertices[idx + 3] = u
        vertices[idx + 4] = v
        vertices[idx + 5] = x
        vertices[idx + 6] = fy2
        vertices[idx + 7] = color
        vertices[idx + 8] = u
        vertices[idx + 9] = v2
        vertices[idx + 10] = fx2
        vertices[idx + 11] = fy2
        vertices[idx + 12] = color
        vertices[idx + 13] = u2
        vertices[idx + 14] = v2
        vertices[idx + 15] = fx2
        vertices[idx + 16] = y
        vertices[idx + 17] = color
        vertices[idx + 18] = u2
        vertices[idx + 19] = v
        this.idx = idx + 20
    }

    fun draw(region: TextureRegion, x: Float, y: Float, originX: Float, originY: Float, width: Float, height: Float,
             scaleX: Float, scaleY: Float, rotation: Float) {
        check(isDrawing) { "SpriteBatch.begin must be called before draw." }
        val vertices = vertices
        val texture: Texture = region.texture
        if (texture !== lastTexture) {
            switchTexture(texture)
        } else if (idx == vertices.size) //
            flush()

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
        val color = colorPacked
        val idx = idx
        vertices[idx] = x1
        vertices[idx + 1] = y1
        vertices[idx + 2] = color
        vertices[idx + 3] = u
        vertices[idx + 4] = v
        vertices[idx + 5] = x2
        vertices[idx + 6] = y2
        vertices[idx + 7] = color
        vertices[idx + 8] = u
        vertices[idx + 9] = v2
        vertices[idx + 10] = x3
        vertices[idx + 11] = y3
        vertices[idx + 12] = color
        vertices[idx + 13] = u2
        vertices[idx + 14] = v2
        vertices[idx + 15] = x4
        vertices[idx + 16] = y4
        vertices[idx + 17] = color
        vertices[idx + 18] = u2
        vertices[idx + 19] = v
        this.idx = idx + 20
    }

    fun draw(region: TextureRegion, x: Float, y: Float, originX: Float, originY: Float, width: Float, height: Float,
             scaleX: Float, scaleY: Float, rotation: Float, clockwise: Boolean) {
        check(isDrawing) { "SpriteBatch.begin must be called before draw." }
        val vertices = vertices
        val texture: Texture = region.texture
        if (texture !== lastTexture) {
            switchTexture(texture)
        } else if (idx == vertices.size) //
            flush()

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
        val u1: Float
        val v1: Float
        val u2: Float
        val v2: Float
        val u3: Float
        val v3: Float
        val u4: Float
        val v4: Float
        if (clockwise) {
            u1 = region.u2
            v1 = region.v2
            u2 = region.u
            v2 = region.v2
            u3 = region.u
            v3 = region.v
            u4 = region.u2
            v4 = region.v
        } else {
            u1 = region.u
            v1 = region.v
            u2 = region.u2
            v2 = region.v
            u3 = region.u2
            v3 = region.v2
            u4 = region.u
            v4 = region.v2
        }
        val color = colorPacked
        val idx = idx
        vertices[idx] = x1
        vertices[idx + 1] = y1
        vertices[idx + 2] = color
        vertices[idx + 3] = u1
        vertices[idx + 4] = v1
        vertices[idx + 5] = x2
        vertices[idx + 6] = y2
        vertices[idx + 7] = color
        vertices[idx + 8] = u2
        vertices[idx + 9] = v2
        vertices[idx + 10] = x3
        vertices[idx + 11] = y3
        vertices[idx + 12] = color
        vertices[idx + 13] = u3
        vertices[idx + 14] = v3
        vertices[idx + 15] = x4
        vertices[idx + 16] = y4
        vertices[idx + 17] = color
        vertices[idx + 18] = u4
        vertices[idx + 19] = v4
        this.idx = idx + 20
    }

    fun draw(region: TextureRegion, width: Float, height: Float, transform: Affine2) {
        check(isDrawing) { "SpriteBatch.begin must be called before draw." }
        val vertices = vertices
        val texture: Texture = region.texture
        if (texture !== lastTexture) {
            switchTexture(texture)
        } else if (idx == vertices.size) {
            flush()
        }

        // construct corner points
        val x1: Float = transform.m02
        val y1: Float = transform.m12
        val x2: Float = transform.m01 * height + transform.m02
        val y2: Float = transform.m11 * height + transform.m12
        val x3: Float = transform.m00 * width + transform.m01 * height + transform.m02
        val y3: Float = transform.m10 * width + transform.m11 * height + transform.m12
        val x4: Float = transform.m00 * width + transform.m02
        val y4: Float = transform.m10 * width + transform.m12
        val u: Float = region.u
        val v: Float = region.v2
        val u2: Float = region.u2
        val v2: Float = region.v
        val color = colorPacked
        val idx = idx
        vertices[idx] = x1
        vertices[idx + 1] = y1
        vertices[idx + 2] = color
        vertices[idx + 3] = u
        vertices[idx + 4] = v
        vertices[idx + 5] = x2
        vertices[idx + 6] = y2
        vertices[idx + 7] = color
        vertices[idx + 8] = u
        vertices[idx + 9] = v2
        vertices[idx + 10] = x3
        vertices[idx + 11] = y3
        vertices[idx + 12] = color
        vertices[idx + 13] = u2
        vertices[idx + 14] = v2
        vertices[idx + 15] = x4
        vertices[idx + 16] = y4
        vertices[idx + 17] = color
        vertices[idx + 18] = u2
        vertices[idx + 19] = v
        this.idx = idx + 20
    }

    override fun flush() {
        if (idx == 0) return
        renderCalls++
        totalRenderCalls++
        val spritesInBatch = idx / 20
        if (spritesInBatch > maxSpritesInBatch) maxSpritesInBatch = spritesInBatch
        val count = spritesInBatch * 6
        lastTexture.bind()
        val mesh: Mesh = mesh
        mesh.setVertices(vertices, 0, idx)
        mesh.getIndicesBuffer().position(0)
        mesh.getIndicesBuffer().limit(count)
        if (blendingDisabled) {
            Gdx.gl.glDisable(GL20.GL_BLEND)
        } else {
            Gdx.gl.glEnable(GL20.GL_BLEND)
            if (blendSrcFunc != -1) Gdx.gl.glBlendFuncSeparate(blendSrcFunc, blendDstFunc, blendSrcFuncAlpha, blendDstFuncAlpha)
        }
        mesh.render(if (customShader != null) customShader else shader, GL20.GL_TRIANGLES, 0, count)
        idx = 0
    }

    override fun disableBlending() {
        if (blendingDisabled) return
        flush()
        blendingDisabled = true
    }

    override fun enableBlending() {
        if (!blendingDisabled) return
        flush()
        blendingDisabled = false
    }

    override fun setBlendFunction(srcFunc: Int, dstFunc: Int) {
        setBlendFunctionSeparate(srcFunc, dstFunc, srcFunc, dstFunc)
    }

    override fun setBlendFunctionSeparate(srcFuncColor: Int, dstFuncColor: Int, srcFuncAlpha: Int, dstFuncAlpha: Int) {
        if (blendSrcFunc == srcFuncColor && blendDstFunc == dstFuncColor && blendSrcFuncAlpha == srcFuncAlpha && blendDstFuncAlpha == dstFuncAlpha) return
        flush()
        blendSrcFunc = srcFuncColor
        blendDstFunc = dstFuncColor
        blendSrcFuncAlpha = srcFuncAlpha
        blendDstFuncAlpha = dstFuncAlpha
    }

    fun dispose() {
        mesh.dispose()
        if (ownsShader && shader != null) shader.dispose()
    }

    override fun getProjectionMatrix(): Matrix4? {
        return projectionMatrix
    }

    override fun getTransformMatrix(): Matrix4? {
        return transformMatrix
    }

    override fun setProjectionMatrix(projection: Matrix4?) {
        if (isDrawing) flush()
        projectionMatrix.set(projection)
        if (isDrawing) setupMatrices()
    }

    override fun setTransformMatrix(transform: Matrix4?) {
        if (isDrawing) flush()
        transformMatrix.set(transform)
        if (isDrawing) setupMatrices()
    }

    private fun setupMatrices() {
        combinedMatrix.set(projectionMatrix).mul(transformMatrix)
        if (customShader != null) {
            customShader.setUniformMatrix("u_projTrans", combinedMatrix)
            customShader!!.setUniformi("u_texture", 0)
        } else {
            shader.setUniformMatrix("u_projTrans", combinedMatrix)
            shader!!.setUniformi("u_texture", 0)
        }
    }

    protected fun switchTexture(texture: Texture?) {
        flush()
        lastTexture = texture
        invTexWidth = 1.0f / texture.getWidth()
        invTexHeight = 1.0f / texture.getHeight()
    }

    override fun setShader(shader: ShaderProgram?) {
        if (isDrawing) {
            flush()
            if (customShader != null) customShader!!.end() else this.shader!!.end()
        }
        customShader = shader
        if (isDrawing) {
            if (customShader != null) customShader!!.begin() else this.shader!!.begin()
            setupMatrices()
        }
    }

    override fun getShader(): ShaderProgram? {
        return if (customShader == null) {
            shader
        } else customShader
    }

    override val isBlendingEnabled: Boolean
        get() = !blendingDisabled

    companion object {

        @Deprecated("""Do not use, this field is for testing only and is likely to be removed. Sets the {@link VertexDataType} to be
	              used when gles 3 is not available, defaults to {@link VertexDataType#VertexArray}. """)
        var defaultVertexDataType: VertexDataType = VertexDataType.VertexArray

        /** Returns a new instance of the default shader used by SpriteBatch for GL2 when no shader is specified.  */
        fun createDefaultShader(): ShaderProgram {
            val vertexShader = """attribute vec4 ${ShaderProgram.POSITION_ATTRIBUTE};
attribute vec4 ${ShaderProgram.COLOR_ATTRIBUTE};
attribute vec2 ${ShaderProgram.TEXCOORD_ATTRIBUTE}0;
uniform mat4 u_projTrans;
varying vec4 v_color;
varying vec2 v_texCoords;

void main()
{
   v_color = ${ShaderProgram.COLOR_ATTRIBUTE};
   v_color.a = v_color.a * (255.0/254.0);
   v_texCoords = ${ShaderProgram.TEXCOORD_ATTRIBUTE}0;
   gl_Position =  u_projTrans * ${ShaderProgram.POSITION_ATTRIBUTE};
}
"""
            val fragmentShader = """#ifdef GL_ES
#define LOWP lowp
precision mediump float;
#else
#define LOWP 
#endif
varying LOWP vec4 v_color;
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
    /** Constructs a new SpriteBatch. Sets the projection matrix to an orthographic projection with y-axis point upwards, x-axis
     * point to the right and the origin being in the bottom left corner of the screen. The projection will be pixel perfect with
     * respect to the current screen resolution.
     *
     *
     * The defaultShader specifies the shader to use. Note that the names for uniforms for this default shader are different than
     * the ones expect for shaders set with [.setShader]. See [.createDefaultShader].
     * @param size The max number of sprites in a single batch. Max of 8191.
     * @param defaultShader The default shader to use. This is not owned by the SpriteBatch and must be disposed separately.
     */
    /** Constructs a new SpriteBatch with a size of 1000, one buffer, and the default shader.
     * @see SpriteBatch.SpriteBatch
     */
    /** Constructs a SpriteBatch with one buffer and the default shader.
     * @see SpriteBatch.SpriteBatch
     */
    init {
        // 32767 is max vertex index, so 32767 / 4 vertices per sprite = 8191 sprites max.
        if (size > 8191) throw IllegalArgumentException("Can't have more than 8191 sprites per batch: $size")
        val vertexDataType: VertexDataType = if (Gdx.gl30 != null) VertexDataType.VertexBufferObjectWithVAO else defaultVertexDataType
        mesh = Mesh(vertexDataType, false, size * 4, size * 6,
            VertexAttribute(Usage.Position, 2, ShaderProgram.POSITION_ATTRIBUTE),
            VertexAttribute(Usage.ColorPacked, 4, ShaderProgram.COLOR_ATTRIBUTE),
            VertexAttribute(Usage.TextureCoordinates, 2, ShaderProgram.TEXCOORD_ATTRIBUTE + "0"))
        projectionMatrix.setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight())
        vertices = FloatArray(size * Sprite.SPRITE_SIZE)
        val len = size * 6
        val indices = ShortArray(len)
        var j: Short = 0
        var i = 0
        while (i < len) {
            indices[i] = j
            indices[i + 1] = (j + 1).toShort()
            indices[i + 2] = (j + 2).toShort()
            indices[i + 3] = (j + 2).toShort()
            indices[i + 4] = (j + 3).toShort()
            indices[i + 5] = j
            i += 6
            (j += 4).toShort()
        }
        mesh.setIndices(indices)
        if (defaultShader == null) {
            shader = createDefaultShader()
            ownsShader = true
        } else shader = defaultShader
    }
}
