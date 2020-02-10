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

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.math.Affine2
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.utils.Disposable

/** A Batch is used to draw 2D rectangles that reference a texture (region). The class will batch the drawing commands and optimize
 * them for processing by the GPU.
 *
 *
 * To draw something with a Batch one has to first call the [Batch.begin] method which will setup appropriate render
 * states. When you are done with drawing you have to call [Batch.end] which will actually draw the things you specified.
 *
 *
 * All drawing commands of the Batch operate in screen coordinates. The screen coordinate system has an x-axis pointing to the
 * right, an y-axis pointing upwards and the origin is in the lower left corner of the screen. You can also provide your own
 * transformation and projection matrices if you so wish.
 *
 *
 * A Batch is managed. In case the OpenGL context is lost all OpenGL resources a Batch uses internally get invalidated. A context
 * is lost when a user switches to another application or receives an incoming call on Android. A Batch will be automatically
 * reloaded after the OpenGL context is restored.
 *
 *
 * A Batch is a pretty heavy object so you should only ever have one in your program.
 *
 *
 * A Batch works with OpenGL ES 2.0. It will use its own custom shader to draw all provided
 * sprites. You can set your own custom shader via [.setShader].
 *
 *
 * A Batch has to be disposed if it is no longer used.
 * @author mzechner
 * @author Nathan Sweet
 */
interface Batch : Disposable {

    /** Sets up the Batch for drawing. This will disable depth buffer writing. It enables blending and texturing. If you have more
     * texture units enabled than the first one you have to disable them before calling this. Uses a screen coordinate system by
     * default where everything is given in pixels. You can specify your own projection and modelview matrices via
     * [.setProjectionMatrix] and [.setTransformMatrix].  */
    fun begin()

    /** Finishes off rendering. Enables depth writes, disables blending and texturing. Must always be called after a call to
     * [.begin]  */
    fun end()

    /** Sets the color used to tint images when they are added to the Batch. Default is [Color.WHITE].  */
    fun setColor(tint: Color?)

    /** @see .setColor
     */
    fun setColor(r: Float, g: Float, b: Float, a: Float)

    /** @return the rendering color of this Batch. If the returned instance is manipulated, [.setColor] must be called
     * afterward.
     */
    fun getColor(): Color?

    /** @return the rendering color of this Batch in vertex format (alpha compressed to 0-254)
     * @see Color.toFloatBits
     */
    /** Sets the rendering color of this Batch, expanding the alpha from 0-254 to 0-255.
     * @see .setColor
     * @see Color.toFloatBits
     */
    var packedColor: Float

    /** Draws a rectangle with the bottom left corner at x,y having the given width and height in pixels. The rectangle is offset by
     * originX, originY relative to the origin. Scale specifies the scaling factor by which the rectangle should be scaled around
     * originX, originY. Rotation specifies the angle of counter clockwise rotation of the rectangle around originX, originY. The
     * portion of the [Texture] given by srcX, srcY and srcWidth, srcHeight is used. These coordinates and sizes are given in
     * texels. FlipX and flipY specify whether the texture portion should be flipped horizontally or vertically.
     * @param x the x-coordinate in screen space
     * @param y the y-coordinate in screen space
     * @param originX the x-coordinate of the scaling and rotation origin relative to the screen space coordinates
     * @param originY the y-coordinate of the scaling and rotation origin relative to the screen space coordinates
     * @param width the width in pixels
     * @param height the height in pixels
     * @param scaleX the scale of the rectangle around originX/originY in x
     * @param scaleY the scale of the rectangle around originX/originY in y
     * @param rotation the angle of counter clockwise rotation of the rectangle around originX/originY
     * @param srcX the x-coordinate in texel space
     * @param srcY the y-coordinate in texel space
     * @param srcWidth the source with in texels
     * @param srcHeight the source height in texels
     * @param flipX whether to flip the sprite horizontally
     * @param flipY whether to flip the sprite vertically
     */
    fun draw(texture: Texture?, x: Float, y: Float, originX: Float, originY: Float, width: Float, height: Float, scaleX: Float,
             scaleY: Float, rotation: Float, srcX: Int, srcY: Int, srcWidth: Int, srcHeight: Int, flipX: Boolean, flipY: Boolean)

    /** Draws a rectangle with the bottom left corner at x,y having the given width and height in pixels. The portion of the
     * [Texture] given by srcX, srcY and srcWidth, srcHeight is used. These coordinates and sizes are given in texels. FlipX
     * and flipY specify whether the texture portion should be flipped horizontally or vertically.
     * @param x the x-coordinate in screen space
     * @param y the y-coordinate in screen space
     * @param width the width in pixels
     * @param height the height in pixels
     * @param srcX the x-coordinate in texel space
     * @param srcY the y-coordinate in texel space
     * @param srcWidth the source with in texels
     * @param srcHeight the source height in texels
     * @param flipX whether to flip the sprite horizontally
     * @param flipY whether to flip the sprite vertically
     */
    fun draw(texture: Texture?, x: Float, y: Float, width: Float, height: Float, srcX: Int, srcY: Int, srcWidth: Int,
             srcHeight: Int, flipX: Boolean, flipY: Boolean)

    /** Draws a rectangle with the bottom left corner at x,y having the given width and height in pixels. The portion of the
     * [Texture] given by srcX, srcY and srcWidth, srcHeight are used. These coordinates and sizes are given in texels.
     * @param x the x-coordinate in screen space
     * @param y the y-coordinate in screen space
     * @param srcX the x-coordinate in texel space
     * @param srcY the y-coordinate in texel space
     * @param srcWidth the source with in texels
     * @param srcHeight the source height in texels
     */
    fun draw(texture: Texture?, x: Float, y: Float, srcX: Int, srcY: Int, srcWidth: Int, srcHeight: Int)

    /** Draws a rectangle with the bottom left corner at x,y having the given width and height in pixels. The portion of the
     * [Texture] given by u, v and u2, v2 are used. These coordinates and sizes are given in texture size percentage. The
     * rectangle will have the given tint [Color].
     * @param x the x-coordinate in screen space
     * @param y the y-coordinate in screen space
     * @param width the width in pixels
     * @param height the height in pixels
     */
    fun draw(texture: Texture?, x: Float, y: Float, width: Float, height: Float, u: Float, v: Float, u2: Float, v2: Float)

    /** Draws a rectangle with the bottom left corner at x,y having the width and height of the texture.
     * @param x the x-coordinate in screen space
     * @param y the y-coordinate in screen space
     */
    fun draw(texture: Texture?, x: Float, y: Float)

    /** Draws a rectangle with the bottom left corner at x,y and stretching the region to cover the given width and height.  */
    fun draw(texture: Texture?, x: Float, y: Float, width: Float, height: Float)

    /** Draws a rectangle using the given vertices. There must be 4 vertices, each made up of 5 elements in this order: x, y, color,
     * u, v. The [.getColor] from the Batch is not applied.  */
    fun draw(texture: Texture?, spriteVertices: FloatArray?, offset: Int, count: Int)

    /** Draws a rectangle with the bottom left corner at x,y having the width and height of the region.  */
    fun draw(region: TextureRegion?, x: Float, y: Float)

    /** Draws a rectangle with the bottom left corner at x,y and stretching the region to cover the given width and height.  */
    fun draw(region: TextureRegion?, x: Float, y: Float, width: Float, height: Float)

    /** Draws a rectangle with the bottom left corner at x,y and stretching the region to cover the given width and height. The
     * rectangle is offset by originX, originY relative to the origin. Scale specifies the scaling factor by which the rectangle
     * should be scaled around originX, originY. Rotation specifies the angle of counter clockwise rotation of the rectangle around
     * originX, originY.  */
    fun draw(region: TextureRegion?, x: Float, y: Float, originX: Float, originY: Float, width: Float, height: Float,
             scaleX: Float, scaleY: Float, rotation: Float)

    /** Draws a rectangle with the texture coordinates rotated 90 degrees. The bottom left corner at x,y and stretching the region
     * to cover the given width and height. The rectangle is offset by originX, originY relative to the origin. Scale specifies the
     * scaling factor by which the rectangle should be scaled around originX, originY. Rotation specifies the angle of counter
     * clockwise rotation of the rectangle around originX, originY.
     * @param clockwise If true, the texture coordinates are rotated 90 degrees clockwise. If false, they are rotated 90 degrees
     * counter clockwise.
     */
    fun draw(region: TextureRegion?, x: Float, y: Float, originX: Float, originY: Float, width: Float, height: Float,
             scaleX: Float, scaleY: Float, rotation: Float, clockwise: Boolean)

    /** Draws a rectangle transformed by the given matrix.  */
    fun draw(region: TextureRegion?, width: Float, height: Float, transform: Affine2?)

    /** Causes any pending sprites to be rendered, without ending the Batch.  */
    fun flush()

    /** Disables blending for drawing sprites. Calling this within [.begin]/[.end] will flush the batch.  */
    fun disableBlending()

    /** Enables blending for drawing sprites. Calling this within [.begin]/[.end] will flush the batch.  */
    fun enableBlending()

    /** Sets the blending function to be used when rendering sprites.
     * @param srcFunc the source function, e.g. GL20.GL_SRC_ALPHA. If set to -1, Batch won't change the blending function.
     * @param dstFunc the destination function, e.g. GL20.GL_ONE_MINUS_SRC_ALPHA
     */
    fun setBlendFunction(srcFunc: Int, dstFunc: Int)

    /** Sets separate (color/alpha) blending function to be used when rendering sprites.
     * @param srcFuncColor the source color function, e.g. GL20.GL_SRC_ALPHA. If set to -1, Batch won't change the blending function.
     * @param dstFuncColor the destination color function, e.g. GL20.GL_ONE_MINUS_SRC_ALPHA.
     * @param srcFuncAlpha the source alpha function, e.g. GL20.GL_SRC_ALPHA.
     * @param dstFuncAlpha the destination alpha function, e.g. GL20.GL_ONE_MINUS_SRC_ALPHA.
     */
    fun setBlendFunctionSeparate(srcFuncColor: Int, dstFuncColor: Int, srcFuncAlpha: Int, dstFuncAlpha: Int)
    val blendSrcFunc: Int
    val blendDstFunc: Int
    val blendSrcFuncAlpha: Int
    val blendDstFuncAlpha: Int

    /** Returns the current projection matrix. Changing this within [.begin]/[.end] results in undefined behaviour.  */
    fun getProjectionMatrix(): Matrix4?

    /** Returns the current transform matrix. Changing this within [.begin]/[.end] results in undefined behaviour.  */
    fun getTransformMatrix(): Matrix4?

    /** Sets the projection matrix to be used by this Batch. If this is called inside a [.begin]/[.end] block, the
     * current batch is flushed to the gpu.  */
    fun setProjectionMatrix(projection: Matrix4?)

    /** Sets the transform matrix to be used by this Batch.  */
    fun setTransformMatrix(transform: Matrix4?)

    /** @return the current [ShaderProgram] set by [.setShader] or the defaultShader
     */
    /** Sets the shader to be used in a GLES 2.0 environment. Vertex position attribute is called "a_position", the texture
     * coordinates attribute is called "a_texCoord0", the color attribute is called "a_color". See
     * [ShaderProgram.POSITION_ATTRIBUTE], [ShaderProgram.COLOR_ATTRIBUTE] and [ShaderProgram.TEXCOORD_ATTRIBUTE]
     * which gets "0" appended to indicate the use of the first texture unit. The combined transform and projection matrx is
     * uploaded via a mat4 uniform called "u_projTrans". The texture sampler is passed via a uniform called "u_texture".
     *
     *
     * Call this method with a null argument to use the default shader.
     *
     *
     * This method will flush the batch before setting the new shader, you can call it in between [.begin] and
     * [.end].
     * @param shader the [ShaderProgram] or null to use the default shader.
     */
    var shader: ShaderProgram?

    /** @return true if blending for sprites is enabled
     */
    val isBlendingEnabled: Boolean

    /** @return true if currently between begin and end.
     */
    val isDrawing: Boolean

    companion object {
        const val X1 = 0
        const val Y1 = 1
        const val C1 = 2
        const val U1 = 3
        const val V1 = 4
        const val X2 = 5
        const val Y2 = 6
        const val C2 = 7
        const val U2 = 8
        const val V2 = 9
        const val X3 = 10
        const val Y3 = 11
        const val C3 = 12
        const val U3 = 13
        const val V3 = 14
        const val X4 = 15
        const val Y4 = 16
        const val C4 = 17
        const val U4 = 18
        const val V4 = 19
    }
}
