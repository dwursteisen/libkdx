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
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.SpriteCache
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasSprite
import com.badlogic.gdx.graphics.g2d.TextureAtlas.TextureAtlasData
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.lang.IllegalStateException
import java.nio.FloatBuffer
import kotlin.jvm.Throws

/** Defines a rectangular area of a texture. The coordinate system used has its origin in the upper left corner with the x-axis
 * pointing to the right and the y axis pointing downwards.
 * @author mzechner
 * @author Nathan Sweet
 */
class TextureRegion {

    var texture: Texture? = null
    var u = 0f
    var v = 0f
    var u2 = 0f
    var v2 = 0f
    var regionWidth = 0
    var regionHeight = 0

    /** Constructs a region that cannot be used until a texture and texture coordinates are set.  */
    constructor() {}

    /** Constructs a region the size of the specified texture.  */
    constructor(texture: Texture?) {
        if (texture == null) throw IllegalArgumentException("texture cannot be null.")
        this.texture = texture
        setRegion(0, 0, texture.getWidth(), texture.getHeight())
    }

    /** @param width The width of the texture region. May be negative to flip the sprite when drawn.
     * @param height The height of the texture region. May be negative to flip the sprite when drawn.
     */
    constructor(texture: Texture?, width: Int, height: Int) {
        this.texture = texture
        setRegion(0, 0, width, height)
    }

    /** @param width The width of the texture region. May be negative to flip the sprite when drawn.
     * @param height The height of the texture region. May be negative to flip the sprite when drawn.
     */
    constructor(texture: Texture?, x: Int, y: Int, width: Int, height: Int) {
        this.texture = texture
        setRegion(x, y, width, height)
    }

    constructor(texture: Texture?, u: Float, v: Float, u2: Float, v2: Float) {
        this.texture = texture
        setRegion(u, v, u2, v2)
    }

    /** Constructs a region with the same texture and coordinates of the specified region.  */
    constructor(region: TextureRegion?) {
        setRegion(region)
    }

    /** Constructs a region with the same texture as the specified region and sets the coordinates relative to the specified
     * region.
     * @param width The width of the texture region. May be negative to flip the sprite when drawn.
     * @param height The height of the texture region. May be negative to flip the sprite when drawn.
     */
    constructor(region: TextureRegion, x: Int, y: Int, width: Int, height: Int) {
        setRegion(region, x, y, width, height)
    }

    /** Sets the texture and sets the coordinates to the size of the specified texture.  */
    fun setRegion(texture: Texture) {
        this.texture = texture
        setRegion(0, 0, texture.getWidth(), texture.getHeight())
    }

    /** @param width The width of the texture region. May be negative to flip the sprite when drawn.
     * @param height The height of the texture region. May be negative to flip the sprite when drawn.
     */
    fun setRegion(x: Int, y: Int, width: Int, height: Int) {
        val invTexWidth: Float = 1f / texture.getWidth()
        val invTexHeight: Float = 1f / texture.getHeight()
        setRegion(x * invTexWidth, y * invTexHeight, (x + width) * invTexWidth, (y + height) * invTexHeight)
        regionWidth = java.lang.Math.abs(width)
        regionHeight = java.lang.Math.abs(height)
    }

    fun setRegion(u: Float, v: Float, u2: Float, v2: Float) {
        var u = u
        var v = v
        var u2 = u2
        var v2 = v2
        val texWidth: Int = texture.getWidth()
        val texHeight: Int = texture.getHeight()
        regionWidth = java.lang.Math.round(java.lang.Math.abs(u2 - u) * texWidth)
        regionHeight = java.lang.Math.round(java.lang.Math.abs(v2 - v) * texHeight)

        // For a 1x1 region, adjust UVs toward pixel center to avoid filtering artifacts on AMD GPUs when drawing very stretched.
        if (regionWidth == 1 && regionHeight == 1) {
            val adjustX = 0.25f / texWidth
            u += adjustX
            u2 -= adjustX
            val adjustY = 0.25f / texHeight
            v += adjustY
            v2 -= adjustY
        }
        this.u = u
        this.v = v
        this.u2 = u2
        this.v2 = v2
    }

    /** Sets the texture and coordinates to the specified region.  */
    fun setRegion(region: TextureRegion) {
        texture = region.texture
        setRegion(region.u, region.v, region.u2, region.v2)
    }

    /** Sets the texture to that of the specified region and sets the coordinates relative to the specified region.  */
    fun setRegion(region: TextureRegion, x: Int, y: Int, width: Int, height: Int) {
        texture = region.texture
        setRegion(region.regionX + x, region.regionY + y, width, height)
    }

    fun getTexture(): Texture? {
        return texture
    }

    fun setTexture(texture: Texture?) {
        this.texture = texture
    }

    fun getU(): Float {
        return u
    }

    fun setU(u: Float) {
        this.u = u
        regionWidth = java.lang.Math.round(java.lang.Math.abs(u2 - u) * texture.getWidth())
    }

    fun getV(): Float {
        return v
    }

    fun setV(v: Float) {
        this.v = v
        regionHeight = java.lang.Math.round(java.lang.Math.abs(v2 - v) * texture.getHeight())
    }

    fun getU2(): Float {
        return u2
    }

    fun setU2(u2: Float) {
        this.u2 = u2
        regionWidth = java.lang.Math.round(java.lang.Math.abs(u2 - u) * texture.getWidth())
    }

    fun getV2(): Float {
        return v2
    }

    fun setV2(v2: Float) {
        this.v2 = v2
        regionHeight = java.lang.Math.round(java.lang.Math.abs(v2 - v) * texture.getHeight())
    }

    var regionX: Int
        get() = java.lang.Math.round(u * texture.getWidth())
        set(x) {
            setU(x / texture.getWidth() as Float)
        }

    var regionY: Int
        get() = java.lang.Math.round(v * texture.getHeight())
        set(y) {
            setV(y / texture.getHeight() as Float)
        }

    /** Returns the region's width.  */
    fun getRegionWidth(): Int {
        return regionWidth
    }

    fun setRegionWidth(width: Int) {
        if (isFlipX) {
            setU(u2 + width / texture.getWidth() as Float)
        } else {
            setU2(u + width / texture.getWidth() as Float)
        }
    }

    /** Returns the region's height.  */
    fun getRegionHeight(): Int {
        return regionHeight
    }

    fun setRegionHeight(height: Int) {
        if (isFlipY) {
            setV(v2 + height / texture.getHeight() as Float)
        } else {
            setV2(v + height / texture.getHeight() as Float)
        }
    }

    fun flip(x: Boolean, y: Boolean) {
        if (x) {
            val temp = u
            u = u2
            u2 = temp
        }
        if (y) {
            val temp = v
            v = v2
            v2 = temp
        }
    }

    val isFlipX: Boolean
        get() = u > u2

    val isFlipY: Boolean
        get() = v > v2

    /** Offsets the region relative to the current region. Generally the region's size should be the entire size of the texture in
     * the direction(s) it is scrolled.
     * @param xAmount The percentage to offset horizontally.
     * @param yAmount The percentage to offset vertically. This is done in texture space, so up is negative.
     */
    fun scroll(xAmount: Float, yAmount: Float) {
        if (xAmount != 0f) {
            val width: Float = (u2 - u) * texture.getWidth()
            u = (u + xAmount) % 1
            u2 = u + width / texture.getWidth()
        }
        if (yAmount != 0f) {
            val height: Float = (v2 - v) * texture.getHeight()
            v = (v + yAmount) % 1
            v2 = v + height / texture.getHeight()
        }
    }

    /** Helper function to create tiles out of this TextureRegion starting from the top left corner going to the right and ending
     * at the bottom right corner. Only complete tiles will be returned so if the region's width or height are not a multiple of
     * the tile width and height not all of the region will be used. This will not work on texture regions returned form a
     * TextureAtlas that either have whitespace removed or where flipped before the region is split.
     *
     * @param tileWidth a tile's width in pixels
     * @param tileHeight a tile's height in pixels
     * @return a 2D array of TextureRegions indexed by [row][column].
     */
    fun split(tileWidth: Int, tileHeight: Int): Array<Array<TextureRegion?>> {
        var x = regionX
        var y = regionY
        val width = regionWidth
        val height = regionHeight
        val rows = height / tileHeight
        val cols = width / tileWidth
        val startX = x
        val tiles = Array(rows) { arrayOfNulls<TextureRegion>(cols) }
        var row = 0
        while (row < rows) {
            x = startX
            var col = 0
            while (col < cols) {
                tiles[row][col] = TextureRegion(texture, x, y, tileWidth, tileHeight)
                col++
                x += tileWidth
            }
            row++
            y += tileHeight
        }
        return tiles
    }

    companion object {
        /** Helper function to create tiles out of the given [Texture] starting from the top left corner going to the right and
         * ending at the bottom right corner. Only complete tiles will be returned so if the texture's width or height are not a
         * multiple of the tile width and height not all of the texture will be used.
         *
         * @param texture the Texture
         * @param tileWidth a tile's width in pixels
         * @param tileHeight a tile's height in pixels
         * @return a 2D array of TextureRegions indexed by [row][column].
         */
        fun split(texture: Texture?, tileWidth: Int, tileHeight: Int): Array<Array<TextureRegion?>> {
            val region = TextureRegion(texture)
            return region.split(tileWidth, tileHeight)
        }
    }
}
