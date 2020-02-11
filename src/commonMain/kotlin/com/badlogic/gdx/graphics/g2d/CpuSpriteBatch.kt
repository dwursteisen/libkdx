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

import java.lang.IllegalStateException

/**
 * CpuSpriteBatch behaves like SpriteBatch, except it doesn't flush automatically whenever the transformation matrix changes.
 * Instead, the vertices get adjusted on subsequent draws to match the running batch. This can improve performance through longer
 * batches, for example when drawing Groups with transform enabled.
 *
 * @author Valentin Milea
 * @see SpriteBatch.renderCalls
 *
 * @see com.badlogic.gdx.scenes.scene2d.Group.setTransform
 */
class CpuSpriteBatch
/**
 * Constructs a CpuSpriteBatch with a custom shader.
 *
 * @see SpriteBatch.SpriteBatch
 */
/**
 * Constructs a CpuSpriteBatch with the default shader.
 *
 * @see SpriteBatch.SpriteBatch
 */
/**
 * Constructs a CpuSpriteBatch with a size of 1000 and the default shader.
 *
 * @see SpriteBatch.SpriteBatch
 */
@JvmOverloads constructor(size: Int = 1000, defaultShader: ShaderProgram? = null) : SpriteBatch(size, defaultShader) {

    private val virtualMatrix: Matrix4 = Matrix4()
    private val adjustAffine: Affine2 = Affine2()
    private var adjustNeeded = false
    private var haveIdentityRealMatrix = true
    private val tmpAffine: Affine2 = Affine2()

    /**
     *
     *
     * Flushes the batch and realigns the real matrix on the GPU. Subsequent draws won't need adjustment and will be slightly
     * faster as long as the transform matrix is not [changed][.setTransformMatrix].
     *
     *
     *
     * Note: The real transform matrix *must* be invertible. If a singular matrix is detected, GdxRuntimeException will be
     * thrown.
     *
     *
     * @see SpriteBatch.flush
     */
    fun flushAndSyncTransformMatrix() {
        flush()
        if (adjustNeeded) {
            // vertices flushed, safe now to replace matrix
            haveIdentityRealMatrix = checkIdt(virtualMatrix)
            if (!haveIdentityRealMatrix && virtualMatrix.det() === 0) throw GdxRuntimeException("Transform matrix is singular, can't sync")
            adjustNeeded = false
            super.setTransformMatrix(virtualMatrix)
        }
    }

    override fun getTransformMatrix(): Matrix4? {
        return if (adjustNeeded) virtualMatrix else super.getTransformMatrix()
    }

    /**
     * Sets the transform matrix to be used by this Batch. Even if this is called inside a [.begin]/[.end] block,
     * the current batch is *not* flushed to the GPU. Instead, for every subsequent draw() the vertices will be transformed
     * on the CPU to match the original batch matrix. This adjustment must be performed until the matrices are realigned by
     * restoring the original matrix, or by calling [.flushAndSyncTransformMatrix].
     */
    fun setTransformMatrix(transform: Matrix4?) {
        val realMatrix: Matrix4? = super.getTransformMatrix()
        if (checkEqual(realMatrix, transform)) {
            adjustNeeded = false
        } else {
            if (isDrawing) {
                virtualMatrix.setAsAffine(transform)
                adjustNeeded = true

                // adjust = inverse(real) x virtual
                // real x adjust x vertex = virtual x vertex
                if (haveIdentityRealMatrix) {
                    adjustAffine.set(transform)
                } else {
                    tmpAffine.set(transform)
                    adjustAffine.set(realMatrix).inv().mul(tmpAffine)
                }
            } else {
                realMatrix.setAsAffine(transform)
                haveIdentityRealMatrix = checkIdt(realMatrix)
            }
        }
    }

    /**
     * Sets the transform matrix to be used by this Batch. Even if this is called inside a [.begin]/[.end] block,
     * the current batch is *not* flushed to the GPU. Instead, for every subsequent draw() the vertices will be transformed
     * on the CPU to match the original batch matrix. This adjustment must be performed until the matrices are realigned by
     * restoring the original matrix, or by calling [.flushAndSyncTransformMatrix] or [.end].
     */
    fun setTransformMatrix(transform: Affine2?) {
        val realMatrix: Matrix4? = super.getTransformMatrix()
        if (checkEqual(realMatrix, transform)) {
            adjustNeeded = false
        } else {
            virtualMatrix.setAsAffine(transform)
            if (isDrawing) {
                adjustNeeded = true

                // adjust = inverse(real) x virtual
                // real x adjust x vertex = virtual x vertex
                if (haveIdentityRealMatrix) {
                    adjustAffine.set(transform)
                } else {
                    adjustAffine.set(realMatrix).inv().mul(transform)
                }
            } else {
                realMatrix.setAsAffine(transform)
                haveIdentityRealMatrix = checkIdt(realMatrix)
            }
        }
    }

    fun draw(texture: Texture, x: Float, y: Float, originX: Float, originY: Float, width: Float, height: Float, scaleX: Float,
             scaleY: Float, rotation: Float, srcX: Int, srcY: Int, srcWidth: Int, srcHeight: Int, flipX: Boolean, flipY: Boolean) {
        if (!adjustNeeded) {
            super.draw(texture, x, y, originX, originY, width, height, scaleX, scaleY, rotation, srcX, srcY, srcWidth, srcHeight,
                flipX, flipY)
        } else {
            drawAdjusted(texture, x, y, originX, originY, width, height, scaleX, scaleY, rotation, srcX, srcY, srcWidth, srcHeight,
                flipX, flipY)
        }
    }

    fun draw(texture: Texture, x: Float, y: Float, width: Float, height: Float, srcX: Int, srcY: Int, srcWidth: Int,
             srcHeight: Int, flipX: Boolean, flipY: Boolean) {
        if (!adjustNeeded) {
            super.draw(texture, x, y, width, height, srcX, srcY, srcWidth, srcHeight, flipX, flipY)
        } else {
            drawAdjusted(texture, x, y, 0f, 0f, width, height, 1f, 1f, 0f, srcX, srcY, srcWidth, srcHeight, flipX, flipY)
        }
    }

    fun draw(texture: Texture, x: Float, y: Float, srcX: Int, srcY: Int, srcWidth: Int, srcHeight: Int) {
        if (!adjustNeeded) {
            super.draw(texture, x, y, srcX, srcY, srcWidth, srcHeight)
        } else {
            drawAdjusted(texture, x, y, 0f, 0f, srcWidth.toFloat(), srcHeight.toFloat(), 1f, 1f, 0f, srcX, srcY, srcWidth, srcHeight,
                false, false)
        }
    }

    fun draw(texture: Texture?, x: Float, y: Float, width: Float, height: Float, u: Float, v: Float, u2: Float, v2: Float) {
        if (!adjustNeeded) {
            super.draw(texture, x, y, width, height, u, v, u2, v2)
        } else {
            drawAdjustedUV(texture, x, y, 0f, 0f, width, height, 1f, 1f, 0f, u, v, u2, v2, false, false)
        }
    }

    fun draw(texture: Texture, x: Float, y: Float) {
        if (!adjustNeeded) {
            super.draw(texture, x, y)
        } else {
            drawAdjusted(texture, x, y, 0f, 0f, texture.getWidth(), texture.getHeight(), 1f, 1f, 0f, 0, 1, 1, 0, false, false)
        }
    }

    fun draw(texture: Texture, x: Float, y: Float, width: Float, height: Float) {
        if (!adjustNeeded) {
            super.draw(texture, x, y, width, height)
        } else {
            drawAdjusted(texture, x, y, 0f, 0f, width, height, 1f, 1f, 0f, 0, 1, 1, 0, false, false)
        }
    }

    override fun draw(region: TextureRegion, x: Float, y: Float) {
        if (!adjustNeeded) {
            super.draw(region, x, y)
        } else {
            drawAdjusted(region, x, y, 0f, 0f, region.getRegionWidth().toFloat(), region.getRegionHeight().toFloat(), 1f, 1f, 0f)
        }
    }

    override fun draw(region: TextureRegion, x: Float, y: Float, width: Float, height: Float) {
        if (!adjustNeeded) {
            super.draw(region, x, y, width, height)
        } else {
            drawAdjusted(region, x, y, 0f, 0f, width, height, 1f, 1f, 0f)
        }
    }

    override fun draw(region: TextureRegion, x: Float, y: Float, originX: Float, originY: Float, width: Float, height: Float,
                      scaleX: Float, scaleY: Float, rotation: Float) {
        if (!adjustNeeded) {
            super.draw(region, x, y, originX, originY, width, height, scaleX, scaleY, rotation)
        } else {
            drawAdjusted(region, x, y, originX, originY, width, height, scaleX, scaleY, rotation)
        }
    }

    override fun draw(region: TextureRegion, x: Float, y: Float, originX: Float, originY: Float, width: Float, height: Float,
                      scaleX: Float, scaleY: Float, rotation: Float, clockwise: Boolean) {
        if (!adjustNeeded) {
            super.draw(region, x, y, originX, originY, width, height, scaleX, scaleY, rotation, clockwise)
        } else {
            drawAdjusted(region, x, y, originX, originY, width, height, scaleX, scaleY, rotation, clockwise)
        }
    }

    fun draw(texture: Texture, spriteVertices: FloatArray, offset: Int, count: Int) {
        if (count % Sprite.SPRITE_SIZE != 0) throw GdxRuntimeException("invalid vertex count")
        if (!adjustNeeded) {
            super.draw(texture, spriteVertices, offset, count)
        } else {
            drawAdjusted(texture, spriteVertices, offset, count)
        }
    }

    fun draw(region: TextureRegion, width: Float, height: Float, transform: Affine2) {
        if (!adjustNeeded) {
            super.draw(region, width, height, transform)
        } else {
            drawAdjusted(region, width, height, transform)
        }
    }

    private fun drawAdjusted(region: TextureRegion, x: Float, y: Float, originX: Float, originY: Float, width: Float, height: Float,
                             scaleX: Float, scaleY: Float, rotation: Float) {
        // v must be flipped
        drawAdjustedUV(region.texture, x, y, originX, originY, width, height, scaleX, scaleY, rotation, region.u, region.v2,
            region.u2, region.v, false, false)
    }

    private fun drawAdjusted(texture: Texture, x: Float, y: Float, originX: Float, originY: Float, width: Float, height: Float,
                             scaleX: Float, scaleY: Float, rotation: Float, srcX: Int, srcY: Int, srcWidth: Int, srcHeight: Int, flipX: Boolean, flipY: Boolean) {
        val invTexWidth: Float = 1.0f / texture.getWidth()
        val invTexHeight: Float = 1.0f / texture.getHeight()
        val u = srcX * invTexWidth
        val v = (srcY + srcHeight) * invTexHeight
        val u2 = (srcX + srcWidth) * invTexWidth
        val v2 = srcY * invTexHeight
        drawAdjustedUV(texture, x, y, originX, originY, width, height, scaleX, scaleY, rotation, u, v, u2, v2, flipX, flipY)
    }

    private fun drawAdjustedUV(texture: Texture?, x: Float, y: Float, originX: Float, originY: Float, width: Float, height: Float,
                               scaleX: Float, scaleY: Float, rotation: Float, u: Float, v: Float, u2: Float, v2: Float, flipX: Boolean, flipY: Boolean) {
        var u = u
        var v = v
        var u2 = u2
        var v2 = v2
        check(drawing) { "CpuSpriteBatch.begin must be called before draw." }
        if (texture !== lastTexture) switchTexture(texture) else if (idx == vertices.length) super.flush()

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
        val t: Affine2 = adjustAffine
        vertices.get(idx + 0) = t.m00 * x1 + t.m01 * y1 + t.m02
        vertices.get(idx + 1) = t.m10 * x1 + t.m11 * y1 + t.m12
        vertices.get(idx + 2) = colorPacked
        vertices.get(idx + 3) = u
        vertices.get(idx + 4) = v
        vertices.get(idx + 5) = t.m00 * x2 + t.m01 * y2 + t.m02
        vertices.get(idx + 6) = t.m10 * x2 + t.m11 * y2 + t.m12
        vertices.get(idx + 7) = colorPacked
        vertices.get(idx + 8) = u
        vertices.get(idx + 9) = v2
        vertices.get(idx + 10) = t.m00 * x3 + t.m01 * y3 + t.m02
        vertices.get(idx + 11) = t.m10 * x3 + t.m11 * y3 + t.m12
        vertices.get(idx + 12) = colorPacked
        vertices.get(idx + 13) = u2
        vertices.get(idx + 14) = v2
        vertices.get(idx + 15) = t.m00 * x4 + t.m01 * y4 + t.m02
        vertices.get(idx + 16) = t.m10 * x4 + t.m11 * y4 + t.m12
        vertices.get(idx + 17) = colorPacked
        vertices.get(idx + 18) = u2
        vertices.get(idx + 19) = v
        idx += Sprite.SPRITE_SIZE
    }

    private fun drawAdjusted(region: TextureRegion, x: Float, y: Float, originX: Float, originY: Float, width: Float, height: Float,
                             scaleX: Float, scaleY: Float, rotation: Float, clockwise: Boolean) {
        check(drawing) { "CpuSpriteBatch.begin must be called before draw." }
        if (region.texture != lastTexture) switchTexture(region.texture) else if (idx == vertices.length) super.flush()

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
        val t: Affine2 = adjustAffine
        vertices.get(idx + 0) = t.m00 * x1 + t.m01 * y1 + t.m02
        vertices.get(idx + 1) = t.m10 * x1 + t.m11 * y1 + t.m12
        vertices.get(idx + 2) = colorPacked
        vertices.get(idx + 3) = u1
        vertices.get(idx + 4) = v1
        vertices.get(idx + 5) = t.m00 * x2 + t.m01 * y2 + t.m02
        vertices.get(idx + 6) = t.m10 * x2 + t.m11 * y2 + t.m12
        vertices.get(idx + 7) = colorPacked
        vertices.get(idx + 8) = u2
        vertices.get(idx + 9) = v2
        vertices.get(idx + 10) = t.m00 * x3 + t.m01 * y3 + t.m02
        vertices.get(idx + 11) = t.m10 * x3 + t.m11 * y3 + t.m12
        vertices.get(idx + 12) = colorPacked
        vertices.get(idx + 13) = u3
        vertices.get(idx + 14) = v3
        vertices.get(idx + 15) = t.m00 * x4 + t.m01 * y4 + t.m02
        vertices.get(idx + 16) = t.m10 * x4 + t.m11 * y4 + t.m12
        vertices.get(idx + 17) = colorPacked
        vertices.get(idx + 18) = u4
        vertices.get(idx + 19) = v4
        idx += Sprite.SPRITE_SIZE
    }

    private fun drawAdjusted(region: TextureRegion, width: Float, height: Float, transform: Affine2) {
        check(drawing) { "CpuSpriteBatch.begin must be called before draw." }
        if (region.texture != lastTexture) switchTexture(region.texture) else if (idx == vertices.length) super.flush()
        var t: Affine2 = transform

        // construct corner points
        val x1: Float = t.m02
        val y1: Float = t.m12
        val x2: Float = t.m01 * height + t.m02
        val y2: Float = t.m11 * height + t.m12
        val x3: Float = t.m00 * width + t.m01 * height + t.m02
        val y3: Float = t.m10 * width + t.m11 * height + t.m12
        val x4: Float = t.m00 * width + t.m02
        val y4: Float = t.m10 * width + t.m12

        // v must be flipped
        val u = region.u
        val v = region.v2
        val u2 = region.u2
        val v2 = region.v
        t = adjustAffine
        vertices.get(idx + 0) = t.m00 * x1 + t.m01 * y1 + t.m02
        vertices.get(idx + 1) = t.m10 * x1 + t.m11 * y1 + t.m12
        vertices.get(idx + 2) = colorPacked
        vertices.get(idx + 3) = u
        vertices.get(idx + 4) = v
        vertices.get(idx + 5) = t.m00 * x2 + t.m01 * y2 + t.m02
        vertices.get(idx + 6) = t.m10 * x2 + t.m11 * y2 + t.m12
        vertices.get(idx + 7) = colorPacked
        vertices.get(idx + 8) = u
        vertices.get(idx + 9) = v2
        vertices.get(idx + 10) = t.m00 * x3 + t.m01 * y3 + t.m02
        vertices.get(idx + 11) = t.m10 * x3 + t.m11 * y3 + t.m12
        vertices.get(idx + 12) = colorPacked
        vertices.get(idx + 13) = u2
        vertices.get(idx + 14) = v2
        vertices.get(idx + 15) = t.m00 * x4 + t.m01 * y4 + t.m02
        vertices.get(idx + 16) = t.m10 * x4 + t.m11 * y4 + t.m12
        vertices.get(idx + 17) = colorPacked
        vertices.get(idx + 18) = u2
        vertices.get(idx + 19) = v
        idx += Sprite.SPRITE_SIZE
    }

    private fun drawAdjusted(texture: Texture, spriteVertices: FloatArray, offset: Int, count: Int) {
        var offset = offset
        var count = count
        check(drawing) { "CpuSpriteBatch.begin must be called before draw." }
        if (texture !== lastTexture) switchTexture(texture)
        val t: Affine2 = adjustAffine
        var copyCount: Int = java.lang.Math.min(vertices.length - idx, count)
        do {
            count -= copyCount
            while (copyCount > 0) {
                val x = spriteVertices[offset]
                val y = spriteVertices[offset + 1]
                vertices.get(idx) = t.m00 * x + t.m01 * y + t.m02 // x
                vertices.get(idx + 1) = t.m10 * x + t.m11 * y + t.m12 // y
                vertices.get(idx + 2) = spriteVertices[offset + 2] // color
                vertices.get(idx + 3) = spriteVertices[offset + 3] // u
                vertices.get(idx + 4) = spriteVertices[offset + 4] // v
                idx += Sprite.VERTEX_SIZE
                offset += Sprite.VERTEX_SIZE
                copyCount -= Sprite.VERTEX_SIZE
            }
            if (count > 0) {
                super.flush()
                copyCount = java.lang.Math.min(vertices.length, count)
            }
        } while (count > 0)
    }

    companion object {
        private fun checkEqual(a: Matrix4, b: Matrix4): Boolean {
            return if (a === b) true else a.`val`.get(Matrix4.M00) === b.`val`.get(Matrix4.M00) && a.`val`.get(Matrix4.M10) === b.`val`.get(Matrix4.M10) && a.`val`.get(Matrix4.M01) === b.`val`.get(Matrix4.M01) && a.`val`.get(Matrix4.M11) === b.`val`.get(Matrix4.M11) && a.`val`.get(Matrix4.M03) === b.`val`.get(Matrix4.M03) && a.`val`.get(Matrix4.M13) === b.`val`.get(Matrix4.M13)

            // matrices are assumed to be 2D transformations
        }

        private fun checkEqual(matrix: Matrix4, affine: Affine2): Boolean {
            val `val`: FloatArray = matrix.getValues()

            // matrix is assumed to be 2D transformation
            return `val`[Matrix4.M00] == affine.m00 && `val`[Matrix4.M10] == affine.m10 && `val`[Matrix4.M01] == affine.m01 && `val`[Matrix4.M11] == affine.m11 && `val`[Matrix4.M03] == affine.m02 && `val`[Matrix4.M13] == affine.m12
        }

        private fun checkIdt(matrix: Matrix4?): Boolean {
            val `val`: FloatArray = matrix.getValues()

            // matrix is assumed to be 2D transformation
            return `val`[Matrix4.M00] == 1 && `val`[Matrix4.M10] == 0 && `val`[Matrix4.M01] == 0 && `val`[Matrix4.M11] == 1 && `val`[Matrix4.M03] == 0 && `val`[Matrix4.M13] == 0
        }
    }
}
