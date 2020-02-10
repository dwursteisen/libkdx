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
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.SpriteBatch.*
import com.badlogic.gdx.graphics.g2d.SpriteCache
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasSprite
import com.badlogic.gdx.graphics.g2d.TextureAtlas.TextureAtlasData
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.utils.NumberUtils
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.lang.IllegalStateException
import java.nio.FloatBuffer
import kotlin.jvm.Throws

/** Holds the geometry, color, and texture information for drawing 2D sprites using [Batch]. A Sprite has a position and a
 * size given as width and height. The position is relative to the origin of the coordinate system specified via
 * [Batch.begin] and the respective matrices. A Sprite is always rectangular and its position (x, y) are located in the
 * bottom left corner of that rectangle. A Sprite also has an origin around which rotations and scaling are performed (that is,
 * the origin is not modified by rotation and scaling). The origin is given relative to the bottom left corner of the Sprite, its
 * position.
 * @author mzechner
 * @author Nathan Sweet
 */
class Sprite : TextureRegion {

    val vertices = FloatArray(SPRITE_SIZE)
    private val color: Color = Color(1, 1, 1, 1)
    private var x = 0f
    private var y = 0f

    /** @return the width of the sprite, not accounting for scale.
     */
    var width = 0f

    /** @return the height of the sprite, not accounting for scale.
     */
    var height = 0f

    /** The origin influences [.setPosition], [.setRotation] and the expansion direction of scaling
     * [.setScale]  */
    var originX = 0f
        private set

    /** The origin influences [.setPosition], [.setRotation] and the expansion direction of scaling
     * [.setScale]  */
    var originY = 0f
        private set
    private var rotation = 0f

    /** X scale of the sprite, independent of size set by [.setSize]  */
    var scaleX = 1f
        private set

    /** Y scale of the sprite, independent of size set by [.setSize]  */
    var scaleY = 1f
        private set
    private var dirty = true
    private var bounds: Rectangle? = null

    /** Creates an uninitialized sprite. The sprite will need a texture region and bounds set before it can be drawn.  */
    constructor() {
        setColor(1f, 1f, 1f, 1f)
    }

    /** Creates a sprite with width, height, and texture region equal to the size of the texture.  */
    constructor(texture: Texture) : this(texture, 0, 0, texture.getWidth(), texture.getHeight()) {}

    /** Creates a sprite with width, height, and texture region equal to the specified size. The texture region's upper left corner
     * will be 0,0.
     * @param srcWidth The width of the texture region. May be negative to flip the sprite when drawn.
     * @param srcHeight The height of the texture region. May be negative to flip the sprite when drawn.
     */
    constructor(texture: Texture?, srcWidth: Int, srcHeight: Int) : this(texture, 0, 0, srcWidth, srcHeight) {}

    /** Creates a sprite with width, height, and texture region equal to the specified size.
     * @param srcWidth The width of the texture region. May be negative to flip the sprite when drawn.
     * @param srcHeight The height of the texture region. May be negative to flip the sprite when drawn.
     */
    constructor(texture: Texture?, srcX: Int, srcY: Int, srcWidth: Int, srcHeight: Int) {
        if (texture == null) throw IllegalArgumentException("texture cannot be null.")
        texture = texture
        setRegion(srcX.toFloat(), srcY.toFloat(), srcWidth.toFloat(), srcHeight.toFloat())
        setColor(1f, 1f, 1f, 1f)
        setSize(java.lang.Math.abs(srcWidth).toFloat(), java.lang.Math.abs(srcHeight).toFloat())
        setOrigin(width / 2, height / 2)
    }
    // Note the region is copied.
    /** Creates a sprite based on a specific TextureRegion, the new sprite's region is a copy of the parameter region - altering one
     * does not affect the other  */
    constructor(region: TextureRegion) {
        setRegion(region)
        setColor(1f, 1f, 1f, 1f)
        setSize(region.getRegionWidth().toFloat(), region.getRegionHeight().toFloat())
        setOrigin(width / 2, height / 2)
    }

    /** Creates a sprite with width, height, and texture region equal to the specified size, relative to specified sprite's texture
     * region.
     * @param srcWidth The width of the texture region. May be negative to flip the sprite when drawn.
     * @param srcHeight The height of the texture region. May be negative to flip the sprite when drawn.
     */
    constructor(region: TextureRegion, srcX: Int, srcY: Int, srcWidth: Int, srcHeight: Int) {
        setRegion(region, srcX.toFloat(), srcY.toFloat(), srcWidth.toFloat(), srcHeight.toFloat())
        setColor(1f, 1f, 1f, 1f)
        setSize(java.lang.Math.abs(srcWidth).toFloat(), java.lang.Math.abs(srcHeight).toFloat())
        setOrigin(width / 2, height / 2)
    }

    /** Creates a sprite that is a copy in every way of the specified sprite.  */
    constructor(sprite: Sprite?) {
        set(sprite)
    }

    /** Make this sprite a copy in every way of the specified sprite  */
    fun set(sprite: Sprite?) {
        if (sprite == null) throw IllegalArgumentException("sprite cannot be null.")
        java.lang.System.arraycopy(sprite.vertices, 0, vertices, 0, SPRITE_SIZE)
        texture = sprite.texture
        u = sprite.u
        v = sprite.v
        u2 = sprite.u2
        v2 = sprite.v2
        x = sprite.x
        y = sprite.y
        width = sprite.width
        height = sprite.height
        regionWidth = sprite.regionWidth
        regionHeight = sprite.regionHeight
        originX = sprite.originX
        originY = sprite.originY
        rotation = sprite.rotation
        scaleX = sprite.scaleX
        scaleY = sprite.scaleY
        color.set(sprite.color)
        dirty = sprite.dirty
    }

    /** Sets the position and size of the sprite when drawn, before scaling and rotation are applied. If origin, rotation, or scale
     * are changed, it is slightly more efficient to set the bounds after those operations.  */
    fun setBounds(x: Float, y: Float, width: Float, height: Float) {
        this.x = x
        this.y = y
        this.width = width
        this.height = height
        if (dirty) return
        val x2 = x + width
        val y2 = y + height
        val vertices = vertices
        vertices[X1] = x
        vertices[Y1] = y
        vertices[X2] = x
        vertices[Y2] = y2
        vertices[X3] = x2
        vertices[Y3] = y2
        vertices[X4] = x2
        vertices[Y4] = y
        if (rotation != 0f || scaleX != 1f || scaleY != 1f) dirty = true
    }

    /** Sets the size of the sprite when drawn, before scaling and rotation are applied. If origin, rotation, or scale are changed,
     * it is slightly more efficient to set the size after those operations. If both position and size are to be changed, it is
     * better to use [.setBounds].  */
    fun setSize(width: Float, height: Float) {
        this.width = width
        this.height = height
        if (dirty) return
        val x2 = x + width
        val y2 = y + height
        val vertices = vertices
        vertices[X1] = x
        vertices[Y1] = y
        vertices[X2] = x
        vertices[Y2] = y2
        vertices[X3] = x2
        vertices[Y3] = y2
        vertices[X4] = x2
        vertices[Y4] = y
        if (rotation != 0f || scaleX != 1f || scaleY != 1f) dirty = true
    }

    /** Sets the position where the sprite will be drawn. If origin, rotation, or scale are changed, it is slightly more efficient
     * to set the position after those operations. If both position and size are to be changed, it is better to use
     * [.setBounds].  */
    fun setPosition(x: Float, y: Float) {
        translate(x - this.x, y - this.y)
    }

    /** Sets the position where the sprite will be drawn, relative to its current origin.   */
    fun setOriginBasedPosition(x: Float, y: Float) {
        setPosition(x - originX, y - originY)
    }

    /** Sets the x position where the sprite will be drawn. If origin, rotation, or scale are changed, it is slightly more efficient
     * to set the position after those operations. If both position and size are to be changed, it is better to use
     * [.setBounds].  */
    fun setX(x: Float) {
        translateX(x - this.x)
    }

    /** Sets the y position where the sprite will be drawn. If origin, rotation, or scale are changed, it is slightly more efficient
     * to set the position after those operations. If both position and size are to be changed, it is better to use
     * [.setBounds].  */
    fun setY(y: Float) {
        translateY(y - this.y)
    }

    /** Sets the x position so that it is centered on the given x parameter  */
    fun setCenterX(x: Float) {
        setX(x - width / 2)
    }

    /** Sets the y position so that it is centered on the given y parameter  */
    fun setCenterY(y: Float) {
        setY(y - height / 2)
    }

    /** Sets the position so that the sprite is centered on (x, y)  */
    fun setCenter(x: Float, y: Float) {
        setCenterX(x)
        setCenterY(y)
    }

    /** Sets the x position relative to the current position where the sprite will be drawn. If origin, rotation, or scale are
     * changed, it is slightly more efficient to translate after those operations.  */
    fun translateX(xAmount: Float) {
        x += xAmount
        if (dirty) return
        val vertices = vertices
        vertices[X1] += xAmount
        vertices[X2] += xAmount
        vertices[X3] += xAmount
        vertices[X4] += xAmount
    }

    /** Sets the y position relative to the current position where the sprite will be drawn. If origin, rotation, or scale are
     * changed, it is slightly more efficient to translate after those operations.  */
    fun translateY(yAmount: Float) {
        y += yAmount
        if (dirty) return
        val vertices = vertices
        vertices[Y1] += yAmount
        vertices[Y2] += yAmount
        vertices[Y3] += yAmount
        vertices[Y4] += yAmount
    }

    /** Sets the position relative to the current position where the sprite will be drawn. If origin, rotation, or scale are
     * changed, it is slightly more efficient to translate after those operations.  */
    fun translate(xAmount: Float, yAmount: Float) {
        x += xAmount
        y += yAmount
        if (dirty) return
        val vertices = vertices
        vertices[X1] += xAmount
        vertices[Y1] += yAmount
        vertices[X2] += xAmount
        vertices[Y2] += yAmount
        vertices[X3] += xAmount
        vertices[Y3] += yAmount
        vertices[X4] += xAmount
        vertices[Y4] += yAmount
    }

    /** Sets the color used to tint this sprite. Default is [Color.WHITE].  */
    fun setColor(tint: Color) {
        color.set(tint)
        val color: Float = tint.toFloatBits()
        val vertices = vertices
        vertices[C1] = color
        vertices[C2] = color
        vertices[C3] = color
        vertices[C4] = color
    }

    /** Sets the alpha portion of the color used to tint this sprite.  */
    fun setAlpha(a: Float) {
        color.a = a
        val color: Float = color.toFloatBits()
        vertices[C1] = color
        vertices[C2] = color
        vertices[C3] = color
        vertices[C4] = color
    }

    /** @see .setColor
     */
    fun setColor(r: Float, g: Float, b: Float, a: Float) {
        color.set(r, g, b, a)
        val color: Float = color.toFloatBits()
        val vertices = vertices
        vertices[C1] = color
        vertices[C2] = color
        vertices[C3] = color
        vertices[C4] = color
    }

    /** Sets the color of this sprite, expanding the alpha from 0-254 to 0-255.
     * @see .setColor
     * @see Color.toFloatBits
     */
    fun setPackedColor(packedColor: Float) {
        Color.abgr8888ToColor(color, packedColor)
        val vertices = vertices
        vertices[C1] = packedColor
        vertices[C2] = packedColor
        vertices[C3] = packedColor
        vertices[C4] = packedColor
    }

    /** Sets the origin in relation to the sprite's position for scaling and rotation.  */
    fun setOrigin(originX: Float, originY: Float) {
        this.originX = originX
        this.originY = originY
        dirty = true
    }

    /** Place origin in the center of the sprite  */
    fun setOriginCenter() {
        originX = width / 2
        originY = height / 2
        dirty = true
    }

    /** Sets the rotation of the sprite in degrees. Rotation is centered on the origin set in [.setOrigin]  */
    fun setRotation(degrees: Float) {
        rotation = degrees
        dirty = true
    }

    /** @return the rotation of the sprite in degrees
     */
    fun getRotation(): Float {
        return rotation
    }

    /** Sets the sprite's rotation in degrees relative to the current rotation. Rotation is centered on the origin set in
     * [.setOrigin]  */
    fun rotate(degrees: Float) {
        if (degrees == 0f) return
        rotation += degrees
        dirty = true
    }

    /** Rotates this sprite 90 degrees in-place by rotating the texture coordinates. This rotation is unaffected by
     * [.setRotation] and [.rotate].  */
    fun rotate90(clockwise: Boolean) {
        val vertices = vertices
        if (clockwise) {
            var temp = vertices[V1]
            vertices[V1] = vertices[V4]
            vertices[V4] = vertices[V3]
            vertices[V3] = vertices[V2]
            vertices[V2] = temp
            temp = vertices[U1]
            vertices[U1] = vertices[U4]
            vertices[U4] = vertices[U3]
            vertices[U3] = vertices[U2]
            vertices[U2] = temp
        } else {
            var temp = vertices[V1]
            vertices[V1] = vertices[V2]
            vertices[V2] = vertices[V3]
            vertices[V3] = vertices[V4]
            vertices[V4] = temp
            temp = vertices[U1]
            vertices[U1] = vertices[U2]
            vertices[U2] = vertices[U3]
            vertices[U3] = vertices[U4]
            vertices[U4] = temp
        }
    }

    /** Sets the sprite's scale for both X and Y uniformly. The sprite scales out from the origin. This will not affect the values
     * returned by [.getWidth] and [.getHeight]  */
    fun setScale(scaleXY: Float) {
        scaleX = scaleXY
        scaleY = scaleXY
        dirty = true
    }

    /** Sets the sprite's scale for both X and Y. The sprite scales out from the origin. This will not affect the values returned by
     * [.getWidth] and [.getHeight]  */
    fun setScale(scaleX: Float, scaleY: Float) {
        this.scaleX = scaleX
        this.scaleY = scaleY
        dirty = true
    }

    /** Sets the sprite's scale relative to the current scale. for example: original scale 2 -> sprite.scale(4) -> final scale 6.
     * The sprite scales out from the origin. This will not affect the values returned by [.getWidth] and
     * [.getHeight]  */
    fun scale(amount: Float) {
        scaleX += amount
        scaleY += amount
        dirty = true
    }

    /** Returns the packed vertices, colors, and texture coordinates for this sprite.  */
    fun getVertices(): FloatArray {
        if (dirty) {
            dirty = false
            val vertices = vertices
            var localX = -originX
            var localY = -originY
            var localX2 = localX + width
            var localY2 = localY + height
            val worldOriginX = x - localX
            val worldOriginY = y - localY
            if (scaleX != 1f || scaleY != 1f) {
                localX *= scaleX
                localY *= scaleY
                localX2 *= scaleX
                localY2 *= scaleY
            }
            if (rotation != 0f) {
                val cos: Float = MathUtils.cosDeg(rotation)
                val sin: Float = MathUtils.sinDeg(rotation)
                val localXCos = localX * cos
                val localXSin = localX * sin
                val localYCos = localY * cos
                val localYSin = localY * sin
                val localX2Cos = localX2 * cos
                val localX2Sin = localX2 * sin
                val localY2Cos = localY2 * cos
                val localY2Sin = localY2 * sin
                val x1 = localXCos - localYSin + worldOriginX
                val y1 = localYCos + localXSin + worldOriginY
                vertices[X1] = x1
                vertices[Y1] = y1
                val x2 = localXCos - localY2Sin + worldOriginX
                val y2 = localY2Cos + localXSin + worldOriginY
                vertices[X2] = x2
                vertices[Y2] = y2
                val x3 = localX2Cos - localY2Sin + worldOriginX
                val y3 = localY2Cos + localX2Sin + worldOriginY
                vertices[X3] = x3
                vertices[Y3] = y3
                vertices[X4] = x1 + (x3 - x2)
                vertices[Y4] = y3 - (y2 - y1)
            } else {
                val x1 = localX + worldOriginX
                val y1 = localY + worldOriginY
                val x2 = localX2 + worldOriginX
                val y2 = localY2 + worldOriginY
                vertices[X1] = x1
                vertices[Y1] = y1
                vertices[X2] = x1
                vertices[Y2] = y2
                vertices[X3] = x2
                vertices[Y3] = y2
                vertices[X4] = x2
                vertices[Y4] = y1
            }
        }
        return vertices
    }

    /** Returns the bounding axis aligned [Rectangle] that bounds this sprite. The rectangles x and y coordinates describe its
     * bottom left corner. If you change the position or size of the sprite, you have to fetch the triangle again for it to be
     * recomputed.
     *
     * @return the bounding Rectangle
     */
    val boundingRectangle: Rectangle
        get() {
            val vertices = getVertices()
            var minx = vertices[X1]
            var miny = vertices[Y1]
            var maxx = vertices[X1]
            var maxy = vertices[Y1]
            minx = if (minx > vertices[X2]) vertices[X2] else minx
            minx = if (minx > vertices[X3]) vertices[X3] else minx
            minx = if (minx > vertices[X4]) vertices[X4] else minx
            maxx = if (maxx < vertices[X2]) vertices[X2] else maxx
            maxx = if (maxx < vertices[X3]) vertices[X3] else maxx
            maxx = if (maxx < vertices[X4]) vertices[X4] else maxx
            miny = if (miny > vertices[Y2]) vertices[Y2] else miny
            miny = if (miny > vertices[Y3]) vertices[Y3] else miny
            miny = if (miny > vertices[Y4]) vertices[Y4] else miny
            maxy = if (maxy < vertices[Y2]) vertices[Y2] else maxy
            maxy = if (maxy < vertices[Y3]) vertices[Y3] else maxy
            maxy = if (maxy < vertices[Y4]) vertices[Y4] else maxy
            if (bounds == null) bounds = Rectangle()
            bounds.x = minx
            bounds.y = miny
            bounds.width = maxx - minx
            bounds.height = maxy - miny
            return bounds
        }

    fun draw(batch: Batch) {
        batch.draw(texture, getVertices(), 0, SPRITE_SIZE)
    }

    fun draw(batch: Batch, alphaModulation: Float) {
        val oldAlpha: Float = getColor().a
        setAlpha(oldAlpha * alphaModulation)
        draw(batch)
        setAlpha(oldAlpha)
    }

    fun getX(): Float {
        return x
    }

    fun getY(): Float {
        return y
    }

    /** Returns the color of this sprite. If the returned instance is manipulated, [.setColor] must be called
     * afterward.  */
    fun getColor(): Color {
        val intBits: Int = NumberUtils.floatToIntColor(vertices[C1])
        val color: Color = color
        color.r = (intBits and 0xff) / 255f
        color.g = (intBits ushr 8 and 0xff) / 255f
        color.b = (intBits ushr 16 and 0xff) / 255f
        color.a = (intBits ushr 24 and 0xff) / 255f
        return color
    }

    override fun setRegion(u: Float, v: Float, u2: Float, v2: Float) {
        super.setRegion(u, v, u2, v2)
        val vertices = vertices
        vertices[U1] = u
        vertices[V1] = v2
        vertices[U2] = u
        vertices[V2] = v
        vertices[U3] = u2
        vertices[V3] = v
        vertices[U4] = u2
        vertices[V4] = v2
    }

    override fun setU(u: Float) {
        super.setU(u)
        vertices[U1] = u
        vertices[U2] = u
    }

    override fun setV(v: Float) {
        super.setV(v)
        vertices[V2] = v
        vertices[V3] = v
    }

    override fun setU2(u2: Float) {
        super.setU2(u2)
        vertices[U3] = u2
        vertices[U4] = u2
    }

    override fun setV2(v2: Float) {
        super.setV2(v2)
        vertices[V1] = v2
        vertices[V4] = v2
    }

    /** Set the sprite's flip state regardless of current condition
     * @param x the desired horizontal flip state
     * @param y the desired vertical flip state
     */
    fun setFlip(x: Boolean, y: Boolean) {
        var performX = false
        var performY = false
        if (isFlipX() !== x) {
            performX = true
        }
        if (isFlipY() !== y) {
            performY = true
        }
        flip(performX, performY)
    }

    /** boolean parameters x,y are not setting a state, but performing a flip
     * @param x perform horizontal flip
     * @param y perform vertical flip
     */
    override fun flip(x: Boolean, y: Boolean) {
        super.flip(x, y)
        val vertices = vertices
        if (x) {
            var temp = vertices[U1]
            vertices[U1] = vertices[U3]
            vertices[U3] = temp
            temp = vertices[U2]
            vertices[U2] = vertices[U4]
            vertices[U4] = temp
        }
        if (y) {
            var temp = vertices[V1]
            vertices[V1] = vertices[V3]
            vertices[V3] = temp
            temp = vertices[V2]
            vertices[V2] = vertices[V4]
            vertices[V4] = temp
        }
    }

    override fun scroll(xAmount: Float, yAmount: Float) {
        val vertices = vertices
        if (xAmount != 0f) {
            var u = (vertices[U1] + xAmount) % 1
            var u2: Float = u + width / texture.getWidth()
            u = u
            u2 = u2
            vertices[U1] = u
            vertices[U2] = u
            vertices[U3] = u2
            vertices[U4] = u2
        }
        if (yAmount != 0f) {
            var v = (vertices[V2] + yAmount) % 1
            var v2: Float = v + height / texture.getHeight()
            v = v
            v2 = v2
            vertices[V1] = v2
            vertices[V2] = v
            vertices[V3] = v
            vertices[V4] = v2
        }
    }

    companion object {
        const val VERTEX_SIZE = 2 + 1 + 2
        const val SPRITE_SIZE = 4 * VERTEX_SIZE
    }
}
