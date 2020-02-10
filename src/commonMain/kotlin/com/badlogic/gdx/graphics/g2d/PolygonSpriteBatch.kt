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
import com.badlogic.gdx.graphics.g2d.Sprite.*
import com.badlogic.gdx.graphics.g2d.SpriteBatch
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
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.lang.IllegalStateException
import java.nio.FloatBuffer
import kotlin.jvm.Throws

/** A PolygonSpriteBatch is used to draw 2D polygons that reference a texture (region). The class will batch the drawing commands
 * and optimize them for processing by the GPU.
 *
 *
 * To draw something with a PolygonSpriteBatch one has to first call the [PolygonSpriteBatch.begin] method which will
 * setup appropriate render states. When you are done with drawing you have to call [PolygonSpriteBatch.end] which will
 * actually draw the things you specified.
 *
 *
 * All drawing commands of the PolygonSpriteBatch operate in screen coordinates. The screen coordinate system has an x-axis
 * pointing to the right, an y-axis pointing upwards and the origin is in the lower left corner of the screen. You can also
 * provide your own transformation and projection matrices if you so wish.
 *
 *
 * A PolygonSpriteBatch is managed. In case the OpenGL context is lost all OpenGL resources a PolygonSpriteBatch uses internally
 * get invalidated. A context is lost when a user switches to another application or receives an incoming call on Android. A
 * SpritPolygonSpriteBatcheBatch will be automatically reloaded after the OpenGL context is restored.
 *
 *
 * A PolygonSpriteBatch is a pretty heavy object so you should only ever have one in your program.
 *
 *
 * A PolygonSpriteBatch works with OpenGL ES 1.x and 2.0. In the case of a 2.0 context it will use its own custom shader to draw
 * all provided sprites. You can set your own custom shader via [.setShader].
 *
 *
 * A PolygonSpriteBatch has to be disposed if it is no longer used.
 * @author mzechner
 * @author Stefan Bachmann
 * @author Nathan Sweet
 */
class PolygonSpriteBatch(maxVertices: Int, maxTriangles: Int, defaultShader: ShaderProgram?) : PolygonBatch {

    private val mesh: Mesh
    private val vertices: FloatArray
    private val triangles: ShortArray
    private var vertexIndex = 0
    private var triangleIndex = 0
    private var lastTexture: Texture? = null
    private var invTexWidth = 0f
    private var invTexHeight = 0f
    var isDrawing = false
        private set
    private val transformMatrix: Matrix4 = Matrix4()
    private val projectionMatrix: Matrix4 = Matrix4()
    private val combinedMatrix: Matrix4 = Matrix4()
    private var blendingDisabled = false
    var blendSrcFunc: Int = GL20.GL_SRC_ALPHA
        private set
    var blendDstFunc: Int = GL20.GL_ONE_MINUS_SRC_ALPHA
        private set
    var blendSrcFuncAlpha: Int = GL20.GL_SRC_ALPHA
        private set
    var blendDstFuncAlpha: Int = GL20.GL_ONE_MINUS_SRC_ALPHA
        private set
    private var shader: ShaderProgram? = null
    private var customShader: ShaderProgram? = null
    private var ownsShader = false
    private val color: Color = Color(1, 1, 1, 1)
    var colorPacked: Float = Color.WHITE_FLOAT_BITS

    /** Number of render calls since the last [.begin].  */
    var renderCalls = 0

    /** Number of rendering calls, ever. Will not be reset unless set manually.  */
    var totalRenderCalls = 0

    /** The maximum number of triangles rendered in one batch so far.  */
    var maxTrianglesInBatch = 0

    /** Constructs a PolygonSpriteBatch with the default shader, size vertices, and size * 2 triangles.
     * @param size The max number of vertices and number of triangles in a single batch. Max of 32767.
     * @see .PolygonSpriteBatch
     */
    constructor(size: Int) : this(size, size * 2, null) {}
    /** Constructs a PolygonSpriteBatch with the specified shader, size vertices and size * 2 triangles.
     * @param size The max number of vertices and number of triangles in a single batch. Max of 32767.
     * @see .PolygonSpriteBatch
     */
    /** Constructs a PolygonSpriteBatch with the default shader, 2000 vertices, and 4000 triangles.
     * @see .PolygonSpriteBatch
     */
    @JvmOverloads
    constructor(size: Int = 2000, defaultShader: ShaderProgram? = null) : this(size, size * 2, defaultShader) {
    }

    fun begin() {
        check(!isDrawing) { "PolygonSpriteBatch.end must be called before begin." }
        renderCalls = 0
        Gdx.gl.glDepthMask(false)
        if (customShader != null) customShader!!.begin() else shader!!.begin()
        setupMatrices()
        isDrawing = true
    }

    fun end() {
        check(isDrawing) { "PolygonSpriteBatch.begin must be called before end." }
        if (vertexIndex > 0) flush()
        lastTexture = null
        isDrawing = false
        val gl: GL20 = Gdx.gl
        gl.glDepthMask(true)
        if (isBlendingEnabled) gl.glDisable(GL20.GL_BLEND)
        if (customShader != null) customShader!!.end() else shader!!.end()
    }

    fun setColor(tint: Color) {
        color.set(tint)
        colorPacked = tint.toFloatBits()
    }

    fun setColor(r: Float, g: Float, b: Float, a: Float) {
        color.set(r, g, b, a)
        colorPacked = color.toFloatBits()
    }

    fun getColor(): Color {
        return color
    }

    var packedColor: Float
        get() = colorPacked
        set(packedColor) {
            Color.abgr8888ToColor(color, packedColor)
            colorPacked = packedColor
        }

    fun draw(region: PolygonRegion, x: Float, y: Float) {
        check(isDrawing) { "PolygonSpriteBatch.begin must be called before draw." }
        val triangles = triangles
        val regionTriangles: ShortArray = region.triangles
        val regionTrianglesLength = regionTriangles.size
        val regionVertices: FloatArray = region.vertices
        val regionVerticesLength = regionVertices.size
        val texture: Texture = region.region.texture
        if (texture !== lastTexture) switchTexture(texture) else if (triangleIndex + regionTrianglesLength > triangles.size
            || vertexIndex + regionVerticesLength * VERTEX_SIZE / 2 > vertices.size) flush()
        var triangleIndex = triangleIndex
        var vertexIndex = vertexIndex
        val startVertex: Int = vertexIndex / VERTEX_SIZE
        for (i in 0 until regionTrianglesLength) triangles[triangleIndex++] = (regionTriangles[i] + startVertex).toShort()
        this.triangleIndex = triangleIndex
        val vertices = vertices
        val color = colorPacked
        val textureCoords: FloatArray = region.textureCoords
        var i = 0
        while (i < regionVerticesLength) {
            vertices[vertexIndex++] = regionVertices[i] + x
            vertices[vertexIndex++] = regionVertices[i + 1] + y
            vertices[vertexIndex++] = color
            vertices[vertexIndex++] = textureCoords[i]
            vertices[vertexIndex++] = textureCoords[i + 1]
            i += 2
        }
        this.vertexIndex = vertexIndex
    }

    fun draw(region: PolygonRegion, x: Float, y: Float, width: Float, height: Float) {
        check(isDrawing) { "PolygonSpriteBatch.begin must be called before draw." }
        val triangles = triangles
        val regionTriangles: ShortArray = region.triangles
        val regionTrianglesLength = regionTriangles.size
        val regionVertices: FloatArray = region.vertices
        val regionVerticesLength = regionVertices.size
        val textureRegion: TextureRegion = region.region
        val texture: Texture = textureRegion.texture
        if (texture !== lastTexture) switchTexture(texture) else if (triangleIndex + regionTrianglesLength > triangles.size
            || vertexIndex + regionVerticesLength * VERTEX_SIZE / 2 > vertices.size) flush()
        var triangleIndex = triangleIndex
        var vertexIndex = vertexIndex
        val startVertex: Int = vertexIndex / VERTEX_SIZE
        run {
            var i = 0
            val n = regionTriangles.size
            while (i < n) {
                triangles[triangleIndex++] = (regionTriangles[i] + startVertex).toShort()
                i++
            }
        }
        this.triangleIndex = triangleIndex
        val vertices = vertices
        val color = colorPacked
        val textureCoords: FloatArray = region.textureCoords
        val sX: Float = width / textureRegion.regionWidth
        val sY: Float = height / textureRegion.regionHeight
        var i = 0
        while (i < regionVerticesLength) {
            vertices[vertexIndex++] = regionVertices[i] * sX + x
            vertices[vertexIndex++] = regionVertices[i + 1] * sY + y
            vertices[vertexIndex++] = color
            vertices[vertexIndex++] = textureCoords[i]
            vertices[vertexIndex++] = textureCoords[i + 1]
            i += 2
        }
        this.vertexIndex = vertexIndex
    }

    fun draw(region: PolygonRegion, x: Float, y: Float, originX: Float, originY: Float, width: Float, height: Float,
             scaleX: Float, scaleY: Float, rotation: Float) {
        check(isDrawing) { "PolygonSpriteBatch.begin must be called before draw." }
        val triangles = triangles
        val regionTriangles: ShortArray = region.triangles
        val regionTrianglesLength = regionTriangles.size
        val regionVertices: FloatArray = region.vertices
        val regionVerticesLength = regionVertices.size
        val textureRegion: TextureRegion = region.region
        val texture: Texture = textureRegion.texture
        if (texture !== lastTexture) switchTexture(texture) else if (triangleIndex + regionTrianglesLength > triangles.size
            || vertexIndex + regionVerticesLength * VERTEX_SIZE / 2 > vertices.size) flush()
        var triangleIndex = triangleIndex
        var vertexIndex = vertexIndex
        val startVertex: Int = vertexIndex / VERTEX_SIZE
        for (i in 0 until regionTrianglesLength) triangles[triangleIndex++] = (regionTriangles[i] + startVertex).toShort()
        this.triangleIndex = triangleIndex
        val vertices = vertices
        val color = colorPacked
        val textureCoords: FloatArray = region.textureCoords
        val worldOriginX = x + originX
        val worldOriginY = y + originY
        val sX: Float = width / textureRegion.regionWidth
        val sY: Float = height / textureRegion.regionHeight
        val cos: Float = MathUtils.cosDeg(rotation)
        val sin: Float = MathUtils.sinDeg(rotation)
        var fx: Float
        var fy: Float
        var i = 0
        while (i < regionVerticesLength) {
            fx = (regionVertices[i] * sX - originX) * scaleX
            fy = (regionVertices[i + 1] * sY - originY) * scaleY
            vertices[vertexIndex++] = cos * fx - sin * fy + worldOriginX
            vertices[vertexIndex++] = sin * fx + cos * fy + worldOriginY
            vertices[vertexIndex++] = color
            vertices[vertexIndex++] = textureCoords[i]
            vertices[vertexIndex++] = textureCoords[i + 1]
            i += 2
        }
        this.vertexIndex = vertexIndex
    }

    fun draw(texture: Texture?, polygonVertices: FloatArray?, verticesOffset: Int, verticesCount: Int, polygonTriangles: ShortArray,
             trianglesOffset: Int, trianglesCount: Int) {
        check(isDrawing) { "PolygonSpriteBatch.begin must be called before draw." }
        val triangles = triangles
        val vertices = vertices
        if (texture !== lastTexture) switchTexture(texture) else if (triangleIndex + trianglesCount > triangles.size || vertexIndex + verticesCount > vertices.size) //
            flush()
        var triangleIndex = triangleIndex
        val vertexIndex = vertexIndex
        val startVertex: Int = vertexIndex / VERTEX_SIZE
        var i = trianglesOffset
        val n = i + trianglesCount
        while (i < n) {
            triangles[triangleIndex++] = (polygonTriangles[i] + startVertex).toShort()
            i++
        }
        this.triangleIndex = triangleIndex
        java.lang.System.arraycopy(polygonVertices, verticesOffset, vertices, vertexIndex, verticesCount)
        this.vertexIndex += verticesCount
    }

    fun draw(texture: Texture, x: Float, y: Float, originX: Float, originY: Float, width: Float, height: Float, scaleX: Float,
             scaleY: Float, rotation: Float, srcX: Int, srcY: Int, srcWidth: Int, srcHeight: Int, flipX: Boolean, flipY: Boolean) {
        check(isDrawing) { "PolygonSpriteBatch.begin must be called before draw." }
        val triangles = triangles
        val vertices = vertices
        if (texture !== lastTexture) switchTexture(texture) else if (triangleIndex + 6 > triangles.size || vertexIndex + SPRITE_SIZE > vertices.size) //
            flush()
        var triangleIndex = triangleIndex
        val startVertex: Int = vertexIndex / VERTEX_SIZE
        triangles[triangleIndex++] = startVertex.toShort()
        triangles[triangleIndex++] = (startVertex + 1).toShort()
        triangles[triangleIndex++] = (startVertex + 2).toShort()
        triangles[triangleIndex++] = (startVertex + 2).toShort()
        triangles[triangleIndex++] = (startVertex + 3).toShort()
        triangles[triangleIndex++] = startVertex.toShort()
        this.triangleIndex = triangleIndex

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
        var idx = vertexIndex
        vertices[idx++] = x1
        vertices[idx++] = y1
        vertices[idx++] = color
        vertices[idx++] = u
        vertices[idx++] = v
        vertices[idx++] = x2
        vertices[idx++] = y2
        vertices[idx++] = color
        vertices[idx++] = u
        vertices[idx++] = v2
        vertices[idx++] = x3
        vertices[idx++] = y3
        vertices[idx++] = color
        vertices[idx++] = u2
        vertices[idx++] = v2
        vertices[idx++] = x4
        vertices[idx++] = y4
        vertices[idx++] = color
        vertices[idx++] = u2
        vertices[idx++] = v
        vertexIndex = idx
    }

    fun draw(texture: Texture, x: Float, y: Float, width: Float, height: Float, srcX: Int, srcY: Int, srcWidth: Int,
             srcHeight: Int, flipX: Boolean, flipY: Boolean) {
        check(isDrawing) { "PolygonSpriteBatch.begin must be called before draw." }
        val triangles = triangles
        val vertices = vertices
        if (texture !== lastTexture) switchTexture(texture) else if (triangleIndex + 6 > triangles.size || vertexIndex + SPRITE_SIZE > vertices.size) //
            flush()
        var triangleIndex = triangleIndex
        val startVertex: Int = vertexIndex / VERTEX_SIZE
        triangles[triangleIndex++] = startVertex.toShort()
        triangles[triangleIndex++] = (startVertex + 1).toShort()
        triangles[triangleIndex++] = (startVertex + 2).toShort()
        triangles[triangleIndex++] = (startVertex + 2).toShort()
        triangles[triangleIndex++] = (startVertex + 3).toShort()
        triangles[triangleIndex++] = startVertex.toShort()
        this.triangleIndex = triangleIndex
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
        var idx = vertexIndex
        vertices[idx++] = x
        vertices[idx++] = y
        vertices[idx++] = color
        vertices[idx++] = u
        vertices[idx++] = v
        vertices[idx++] = x
        vertices[idx++] = fy2
        vertices[idx++] = color
        vertices[idx++] = u
        vertices[idx++] = v2
        vertices[idx++] = fx2
        vertices[idx++] = fy2
        vertices[idx++] = color
        vertices[idx++] = u2
        vertices[idx++] = v2
        vertices[idx++] = fx2
        vertices[idx++] = y
        vertices[idx++] = color
        vertices[idx++] = u2
        vertices[idx++] = v
        vertexIndex = idx
    }

    fun draw(texture: Texture, x: Float, y: Float, srcX: Int, srcY: Int, srcWidth: Int, srcHeight: Int) {
        check(isDrawing) { "PolygonSpriteBatch.begin must be called before draw." }
        val triangles = triangles
        val vertices = vertices
        if (texture !== lastTexture) switchTexture(texture) else if (triangleIndex + 6 > triangles.size || vertexIndex + SPRITE_SIZE > vertices.size) //
            flush()
        var triangleIndex = triangleIndex
        val startVertex: Int = vertexIndex / VERTEX_SIZE
        triangles[triangleIndex++] = startVertex.toShort()
        triangles[triangleIndex++] = (startVertex + 1).toShort()
        triangles[triangleIndex++] = (startVertex + 2).toShort()
        triangles[triangleIndex++] = (startVertex + 2).toShort()
        triangles[triangleIndex++] = (startVertex + 3).toShort()
        triangles[triangleIndex++] = startVertex.toShort()
        this.triangleIndex = triangleIndex
        val u = srcX * invTexWidth
        val v = (srcY + srcHeight) * invTexHeight
        val u2 = (srcX + srcWidth) * invTexWidth
        val v2 = srcY * invTexHeight
        val fx2 = x + srcWidth
        val fy2 = y + srcHeight
        val color = colorPacked
        var idx = vertexIndex
        vertices[idx++] = x
        vertices[idx++] = y
        vertices[idx++] = color
        vertices[idx++] = u
        vertices[idx++] = v
        vertices[idx++] = x
        vertices[idx++] = fy2
        vertices[idx++] = color
        vertices[idx++] = u
        vertices[idx++] = v2
        vertices[idx++] = fx2
        vertices[idx++] = fy2
        vertices[idx++] = color
        vertices[idx++] = u2
        vertices[idx++] = v2
        vertices[idx++] = fx2
        vertices[idx++] = y
        vertices[idx++] = color
        vertices[idx++] = u2
        vertices[idx++] = v
        vertexIndex = idx
    }

    fun draw(texture: Texture, x: Float, y: Float, width: Float, height: Float, u: Float, v: Float, u2: Float, v2: Float) {
        check(isDrawing) { "PolygonSpriteBatch.begin must be called before draw." }
        val triangles = triangles
        val vertices = vertices
        if (texture !== lastTexture) switchTexture(texture) else if (triangleIndex + 6 > triangles.size || vertexIndex + SPRITE_SIZE > vertices.size) //
            flush()
        var triangleIndex = triangleIndex
        val startVertex: Int = vertexIndex / VERTEX_SIZE
        triangles[triangleIndex++] = startVertex.toShort()
        triangles[triangleIndex++] = (startVertex + 1).toShort()
        triangles[triangleIndex++] = (startVertex + 2).toShort()
        triangles[triangleIndex++] = (startVertex + 2).toShort()
        triangles[triangleIndex++] = (startVertex + 3).toShort()
        triangles[triangleIndex++] = startVertex.toShort()
        this.triangleIndex = triangleIndex
        val fx2 = x + width
        val fy2 = y + height
        val color = colorPacked
        var idx = vertexIndex
        vertices[idx++] = x
        vertices[idx++] = y
        vertices[idx++] = color
        vertices[idx++] = u
        vertices[idx++] = v
        vertices[idx++] = x
        vertices[idx++] = fy2
        vertices[idx++] = color
        vertices[idx++] = u
        vertices[idx++] = v2
        vertices[idx++] = fx2
        vertices[idx++] = fy2
        vertices[idx++] = color
        vertices[idx++] = u2
        vertices[idx++] = v2
        vertices[idx++] = fx2
        vertices[idx++] = y
        vertices[idx++] = color
        vertices[idx++] = u2
        vertices[idx++] = v
        vertexIndex = idx
    }

    fun draw(texture: Texture, x: Float, y: Float) {
        draw(texture, x, y, texture.getWidth(), texture.getHeight())
    }

    fun draw(texture: Texture, x: Float, y: Float, width: Float, height: Float) {
        check(isDrawing) { "PolygonSpriteBatch.begin must be called before draw." }
        val triangles = triangles
        val vertices = vertices
        if (texture !== lastTexture) switchTexture(texture) else if (triangleIndex + 6 > triangles.size || vertexIndex + SPRITE_SIZE > vertices.size) //
            flush()
        var triangleIndex = triangleIndex
        val startVertex: Int = vertexIndex / VERTEX_SIZE
        triangles[triangleIndex++] = startVertex.toShort()
        triangles[triangleIndex++] = (startVertex + 1).toShort()
        triangles[triangleIndex++] = (startVertex + 2).toShort()
        triangles[triangleIndex++] = (startVertex + 2).toShort()
        triangles[triangleIndex++] = (startVertex + 3).toShort()
        triangles[triangleIndex++] = startVertex.toShort()
        this.triangleIndex = triangleIndex
        val fx2 = x + width
        val fy2 = y + height
        val u = 0f
        val v = 1f
        val u2 = 1f
        val v2 = 0f
        val color = colorPacked
        var idx = vertexIndex
        vertices[idx++] = x
        vertices[idx++] = y
        vertices[idx++] = color
        vertices[idx++] = u
        vertices[idx++] = v
        vertices[idx++] = x
        vertices[idx++] = fy2
        vertices[idx++] = color
        vertices[idx++] = u
        vertices[idx++] = v2
        vertices[idx++] = fx2
        vertices[idx++] = fy2
        vertices[idx++] = color
        vertices[idx++] = u2
        vertices[idx++] = v2
        vertices[idx++] = fx2
        vertices[idx++] = y
        vertices[idx++] = color
        vertices[idx++] = u2
        vertices[idx++] = v
        vertexIndex = idx
    }

    fun draw(texture: Texture, spriteVertices: FloatArray?, offset: Int, count: Int) {
        var offset = offset
        var count = count
        check(isDrawing) { "PolygonSpriteBatch.begin must be called before draw." }
        val triangles = triangles
        val vertices = vertices
        var triangleCount: Int = count / SPRITE_SIZE * 6
        var batch: Int
        if (texture !== lastTexture) {
            switchTexture(texture)
            batch = java.lang.Math.min(java.lang.Math.min(count, vertices.size - vertices.size % SPRITE_SIZE), triangles.size / 6 * SPRITE_SIZE)
            triangleCount = batch / SPRITE_SIZE * 6
        } else if (triangleIndex + triangleCount > triangles.size || vertexIndex + count > vertices.size) {
            flush()
            batch = java.lang.Math.min(java.lang.Math.min(count, vertices.size - vertices.size % SPRITE_SIZE), triangles.size / 6 * SPRITE_SIZE)
            triangleCount = batch / SPRITE_SIZE * 6
        } else batch = count
        var vertexIndex = vertexIndex
        var vertex = (vertexIndex / VERTEX_SIZE) as Short
        var triangleIndex = triangleIndex
        val n = triangleIndex + triangleCount
        while (triangleIndex < n) {
            triangles[triangleIndex] = vertex
            triangles[triangleIndex + 1] = (vertex + 1).toShort()
            triangles[triangleIndex + 2] = (vertex + 2).toShort()
            triangles[triangleIndex + 3] = (vertex + 2).toShort()
            triangles[triangleIndex + 4] = (vertex + 3).toShort()
            triangles[triangleIndex + 5] = vertex
            triangleIndex += 6
            (vertex += 4).toShort()
        }
        while (true) {
            java.lang.System.arraycopy(spriteVertices, offset, vertices, vertexIndex, batch)
            this.vertexIndex = vertexIndex + batch
            this.triangleIndex = triangleIndex
            count -= batch
            if (count == 0) break
            offset += batch
            flush()
            vertexIndex = 0
            if (batch > count) {
                batch = java.lang.Math.min(count, triangles.size / 6 * SPRITE_SIZE)
                triangleIndex = batch / SPRITE_SIZE * 6
            }
        }
    }

    fun draw(region: TextureRegion, x: Float, y: Float) {
        draw(region, x, y, region.getRegionWidth(), region.getRegionHeight())
    }

    fun draw(region: TextureRegion, x: Float, y: Float, width: Float, height: Float) {
        check(isDrawing) { "PolygonSpriteBatch.begin must be called before draw." }
        val triangles = triangles
        val vertices = vertices
        val texture: Texture = region.texture
        if (texture !== lastTexture) switchTexture(texture) else if (triangleIndex + 6 > triangles.size || vertexIndex + SPRITE_SIZE > vertices.size) //
            flush()
        var triangleIndex = triangleIndex
        val startVertex: Int = vertexIndex / VERTEX_SIZE
        triangles[triangleIndex++] = startVertex.toShort()
        triangles[triangleIndex++] = (startVertex + 1).toShort()
        triangles[triangleIndex++] = (startVertex + 2).toShort()
        triangles[triangleIndex++] = (startVertex + 2).toShort()
        triangles[triangleIndex++] = (startVertex + 3).toShort()
        triangles[triangleIndex++] = startVertex.toShort()
        this.triangleIndex = triangleIndex
        val fx2 = x + width
        val fy2 = y + height
        val u: Float = region.u
        val v: Float = region.v2
        val u2: Float = region.u2
        val v2: Float = region.v
        val color = colorPacked
        var idx = vertexIndex
        vertices[idx++] = x
        vertices[idx++] = y
        vertices[idx++] = color
        vertices[idx++] = u
        vertices[idx++] = v
        vertices[idx++] = x
        vertices[idx++] = fy2
        vertices[idx++] = color
        vertices[idx++] = u
        vertices[idx++] = v2
        vertices[idx++] = fx2
        vertices[idx++] = fy2
        vertices[idx++] = color
        vertices[idx++] = u2
        vertices[idx++] = v2
        vertices[idx++] = fx2
        vertices[idx++] = y
        vertices[idx++] = color
        vertices[idx++] = u2
        vertices[idx++] = v
        vertexIndex = idx
    }

    fun draw(region: TextureRegion, x: Float, y: Float, originX: Float, originY: Float, width: Float, height: Float,
             scaleX: Float, scaleY: Float, rotation: Float) {
        check(isDrawing) { "PolygonSpriteBatch.begin must be called before draw." }
        val triangles = triangles
        val vertices = vertices
        val texture: Texture = region.texture
        if (texture !== lastTexture) switchTexture(texture) else if (triangleIndex + 6 > triangles.size || vertexIndex + SPRITE_SIZE > vertices.size) //
            flush()
        var triangleIndex = triangleIndex
        val startVertex: Int = vertexIndex / VERTEX_SIZE
        triangles[triangleIndex++] = startVertex.toShort()
        triangles[triangleIndex++] = (startVertex + 1).toShort()
        triangles[triangleIndex++] = (startVertex + 2).toShort()
        triangles[triangleIndex++] = (startVertex + 2).toShort()
        triangles[triangleIndex++] = (startVertex + 3).toShort()
        triangles[triangleIndex++] = startVertex.toShort()
        this.triangleIndex = triangleIndex

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
        var idx = vertexIndex
        vertices[idx++] = x1
        vertices[idx++] = y1
        vertices[idx++] = color
        vertices[idx++] = u
        vertices[idx++] = v
        vertices[idx++] = x2
        vertices[idx++] = y2
        vertices[idx++] = color
        vertices[idx++] = u
        vertices[idx++] = v2
        vertices[idx++] = x3
        vertices[idx++] = y3
        vertices[idx++] = color
        vertices[idx++] = u2
        vertices[idx++] = v2
        vertices[idx++] = x4
        vertices[idx++] = y4
        vertices[idx++] = color
        vertices[idx++] = u2
        vertices[idx++] = v
        vertexIndex = idx
    }

    fun draw(region: TextureRegion, x: Float, y: Float, originX: Float, originY: Float, width: Float, height: Float,
             scaleX: Float, scaleY: Float, rotation: Float, clockwise: Boolean) {
        check(isDrawing) { "PolygonSpriteBatch.begin must be called before draw." }
        val triangles = triangles
        val vertices = vertices
        val texture: Texture = region.texture
        if (texture !== lastTexture) switchTexture(texture) else if (triangleIndex + 6 > triangles.size || vertexIndex + SPRITE_SIZE > vertices.size) //
            flush()
        var triangleIndex = triangleIndex
        val startVertex: Int = vertexIndex / VERTEX_SIZE
        triangles[triangleIndex++] = startVertex.toShort()
        triangles[triangleIndex++] = (startVertex + 1).toShort()
        triangles[triangleIndex++] = (startVertex + 2).toShort()
        triangles[triangleIndex++] = (startVertex + 2).toShort()
        triangles[triangleIndex++] = (startVertex + 3).toShort()
        triangles[triangleIndex++] = startVertex.toShort()
        this.triangleIndex = triangleIndex

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
        var idx = vertexIndex
        vertices[idx++] = x1
        vertices[idx++] = y1
        vertices[idx++] = color
        vertices[idx++] = u1
        vertices[idx++] = v1
        vertices[idx++] = x2
        vertices[idx++] = y2
        vertices[idx++] = color
        vertices[idx++] = u2
        vertices[idx++] = v2
        vertices[idx++] = x3
        vertices[idx++] = y3
        vertices[idx++] = color
        vertices[idx++] = u3
        vertices[idx++] = v3
        vertices[idx++] = x4
        vertices[idx++] = y4
        vertices[idx++] = color
        vertices[idx++] = u4
        vertices[idx++] = v4
        vertexIndex = idx
    }

    fun draw(region: TextureRegion, width: Float, height: Float, transform: Affine2) {
        check(isDrawing) { "PolygonSpriteBatch.begin must be called before draw." }
        val triangles = triangles
        val vertices = vertices
        val texture: Texture = region.texture
        if (texture !== lastTexture) switchTexture(texture) else if (triangleIndex + 6 > triangles.size || vertexIndex + SPRITE_SIZE > vertices.size) //
            flush()
        var triangleIndex = triangleIndex
        val startVertex: Int = vertexIndex / VERTEX_SIZE
        triangles[triangleIndex++] = startVertex.toShort()
        triangles[triangleIndex++] = (startVertex + 1).toShort()
        triangles[triangleIndex++] = (startVertex + 2).toShort()
        triangles[triangleIndex++] = (startVertex + 2).toShort()
        triangles[triangleIndex++] = (startVertex + 3).toShort()
        triangles[triangleIndex++] = startVertex.toShort()
        this.triangleIndex = triangleIndex

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
        var idx = vertexIndex
        vertices[idx++] = x1
        vertices[idx++] = y1
        vertices[idx++] = color
        vertices[idx++] = u
        vertices[idx++] = v
        vertices[idx++] = x2
        vertices[idx++] = y2
        vertices[idx++] = color
        vertices[idx++] = u
        vertices[idx++] = v2
        vertices[idx++] = x3
        vertices[idx++] = y3
        vertices[idx++] = color
        vertices[idx++] = u2
        vertices[idx++] = v2
        vertices[idx++] = x4
        vertices[idx++] = y4
        vertices[idx++] = color
        vertices[idx++] = u2
        vertices[idx++] = v
        vertexIndex = idx
    }

    fun flush() {
        if (vertexIndex == 0) return
        renderCalls++
        totalRenderCalls++
        val trianglesInBatch = triangleIndex
        if (trianglesInBatch > maxTrianglesInBatch) maxTrianglesInBatch = trianglesInBatch
        lastTexture.bind()
        val mesh: Mesh = mesh
        mesh.setVertices(vertices, 0, vertexIndex)
        mesh.setIndices(triangles, 0, trianglesInBatch)
        if (blendingDisabled) {
            Gdx.gl.glDisable(GL20.GL_BLEND)
        } else {
            Gdx.gl.glEnable(GL20.GL_BLEND)
            if (blendSrcFunc != -1) Gdx.gl.glBlendFuncSeparate(blendSrcFunc, blendDstFunc, blendSrcFuncAlpha, blendDstFuncAlpha)
        }
        mesh.render(if (customShader != null) customShader else shader, GL20.GL_TRIANGLES, 0, trianglesInBatch)
        vertexIndex = 0
        triangleIndex = 0
    }

    fun disableBlending() {
        flush()
        blendingDisabled = true
    }

    fun enableBlending() {
        flush()
        blendingDisabled = false
    }

    fun setBlendFunction(srcFunc: Int, dstFunc: Int) {
        setBlendFunctionSeparate(srcFunc, dstFunc, srcFunc, dstFunc)
    }

    fun setBlendFunctionSeparate(srcFuncColor: Int, dstFuncColor: Int, srcFuncAlpha: Int, dstFuncAlpha: Int) {
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

    fun getProjectionMatrix(): Matrix4 {
        return projectionMatrix
    }

    fun getTransformMatrix(): Matrix4 {
        return transformMatrix
    }

    fun setProjectionMatrix(projection: Matrix4?) {
        if (isDrawing) flush()
        projectionMatrix.set(projection)
        if (isDrawing) setupMatrices()
    }

    fun setTransformMatrix(transform: Matrix4?) {
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

    private fun switchTexture(texture: Texture?) {
        flush()
        lastTexture = texture
        invTexWidth = 1.0f / texture.getWidth()
        invTexHeight = 1.0f / texture.getHeight()
    }

    fun setShader(shader: ShaderProgram?) {
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

    fun getShader(): ShaderProgram? {
        return if (customShader == null) {
            shader
        } else customShader
    }

    val isBlendingEnabled: Boolean
        get() = !blendingDisabled

    /** Constructs a new PolygonSpriteBatch. Sets the projection matrix to an orthographic projection with y-axis point upwards,
     * x-axis point to the right and the origin being in the bottom left corner of the screen. The projection will be pixel perfect
     * with respect to the current screen resolution.
     *
     *
     * The defaultShader specifies the shader to use. Note that the names for uniforms for this default shader are different than
     * the ones expect for shaders set with [.setShader]. See [SpriteBatch.createDefaultShader].
     * @param maxVertices The max number of vertices in a single batch. Max of 32767.
     * @param maxTriangles The max number of triangles in a single batch.
     * @param defaultShader The default shader to use. This is not owned by the PolygonSpriteBatch and must be disposed separately.
     * May be null to use the default shader.
     */
    init {
        // 32767 is max vertex index.
        if (maxVertices > 32767) throw java.lang.IllegalArgumentException("Can't have more than 32767 vertices per batch: $maxVertices")
        var vertexDataType: VertexDataType = Mesh.VertexDataType.VertexArray
        if (Gdx.gl30 != null) {
            vertexDataType = VertexDataType.VertexBufferObjectWithVAO
        }
        mesh = Mesh(vertexDataType, false, maxVertices, maxTriangles * 3,
            VertexAttribute(Usage.Position, 2, ShaderProgram.POSITION_ATTRIBUTE),
            VertexAttribute(Usage.ColorPacked, 4, ShaderProgram.COLOR_ATTRIBUTE),
            VertexAttribute(Usage.TextureCoordinates, 2, ShaderProgram.TEXCOORD_ATTRIBUTE + "0"))
        vertices = FloatArray(maxVertices * VERTEX_SIZE)
        triangles = ShortArray(maxTriangles * 3)
        if (defaultShader == null) {
            shader = SpriteBatch.createDefaultShader()
            ownsShader = true
        } else shader = defaultShader
        projectionMatrix.setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight())
    }
}
