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

import com.badlogic.gdx.graphics.g2d.PolygonRegionLoader.PolygonRegionParameters

/**
 * @author Stefan Bachmann
 * @author Nathan Sweet
 */
class PolygonSprite {

    var region: PolygonRegion? = null
    private var x = 0f
    private var y = 0f
    var width = 0f
        private set
    var height = 0f
        private set
    var scaleX = 1f
        private set
    var scaleY = 1f
        private set
    private var rotation = 0f
    var originX = 0f
        private set
    var originY = 0f
        private set
    private var vertices: FloatArray?
    private var dirty = false
    private val bounds: Rectangle = Rectangle()
    private val color: Color = Color(1f, 1f, 1f, 1f)

    constructor(region: PolygonRegion) {
        setRegion(region)
        setSize(region.region.regionWidth.toFloat(), region.region.regionHeight.toFloat())
        setOrigin(width / 2, height / 2)
    }

    /**
     * Creates a sprite that is a copy in every way of the specified sprite.
     */
    constructor(sprite: PolygonSprite?) {
        set(sprite)
    }

    fun set(sprite: PolygonSprite?) {
        if (sprite == null) throw java.lang.IllegalArgumentException("sprite cannot be null.")
        setRegion(sprite.region)
        x = sprite.x
        y = sprite.y
        width = sprite.width
        height = sprite.height
        originX = sprite.originX
        originY = sprite.originY
        rotation = sprite.rotation
        scaleX = sprite.scaleX
        scaleY = sprite.scaleY
        color.set(sprite.color)
    }

    /**
     * Sets the position and size of the sprite when drawn, before scaling and rotation are applied. If origin, rotation, or scale
     * are changed, it is slightly more efficient to set the bounds after those operations.
     */
    fun setBounds(x: Float, y: Float, width: Float, height: Float) {
        this.x = x
        this.y = y
        this.width = width
        this.height = height
        dirty = true
    }

    /**
     * Sets the size of the sprite when drawn, before scaling and rotation are applied. If origin, rotation, or scale are changed,
     * it is slightly more efficient to set the size after those operations. If both position and size are to be changed, it is
     * better to use [.setBounds].
     */
    fun setSize(width: Float, height: Float) {
        this.width = width
        this.height = height
        dirty = true
    }

    /**
     * Sets the position where the sprite will be drawn. If origin, rotation, or scale are changed, it is slightly more efficient
     * to set the position after those operations. If both position and size are to be changed, it is better to use
     * [.setBounds].
     */
    fun setPosition(x: Float, y: Float) {
        translate(x - this.x, y - this.y)
    }

    /**
     * Sets the x position where the sprite will be drawn. If origin, rotation, or scale are changed, it is slightly more efficient
     * to set the position after those operations. If both position and size are to be changed, it is better to use
     * [.setBounds].
     */
    fun setX(x: Float) {
        translateX(x - this.x)
    }

    /**
     * Sets the y position where the sprite will be drawn. If origin, rotation, or scale are changed, it is slightly more efficient
     * to set the position after those operations. If both position and size are to be changed, it is better to use
     * [.setBounds].
     */
    fun setY(y: Float) {
        translateY(y - this.y)
    }

    /**
     * Sets the x position relative to the current position where the sprite will be drawn. If origin, rotation, or scale are
     * changed, it is slightly more efficient to translate after those operations.
     */
    fun translateX(xAmount: Float) {
        x += xAmount
        if (dirty) return
        val vertices = vertices
        var i = 0
        while (i < vertices!!.size) {
            vertices[i] += xAmount
            i += Sprite.VERTEX_SIZE
        }
    }

    /**
     * Sets the y position relative to the current position where the sprite will be drawn. If origin, rotation, or scale are
     * changed, it is slightly more efficient to translate after those operations.
     */
    fun translateY(yAmount: Float) {
        y += yAmount
        if (dirty) return
        val vertices = vertices
        var i = 1
        while (i < vertices!!.size) {
            vertices[i] += yAmount
            i += Sprite.VERTEX_SIZE
        }
    }

    /**
     * Sets the position relative to the current position where the sprite will be drawn. If origin, rotation, or scale are
     * changed, it is slightly more efficient to translate after those operations.
     */
    fun translate(xAmount: Float, yAmount: Float) {
        x += xAmount
        y += yAmount
        if (dirty) return
        val vertices = vertices
        var i = 0
        while (i < vertices!!.size) {
            vertices[i] += xAmount
            vertices[i + 1] += yAmount
            i += Sprite.VERTEX_SIZE
        }
    }

    fun setColor(tint: Color) {
        color.set(tint)
        val color: Float = tint.toFloatBits()
        val vertices = vertices
        var i = 2
        while (i < vertices!!.size) {
            vertices[i] = color
            i += Sprite.VERTEX_SIZE
        }
    }

    fun setColor(r: Float, g: Float, b: Float, a: Float) {
        color.set(r, g, b, a)
        val packedColor: Float = color.toFloatBits()
        val vertices = vertices
        var i = 2
        while (i < vertices!!.size) {
            vertices[i] = packedColor
            i += Sprite.VERTEX_SIZE
        }
    }

    /**
     * Sets the origin in relation to the sprite's position for scaling and rotation.
     */
    fun setOrigin(originX: Float, originY: Float) {
        this.originX = originX
        this.originY = originY
        dirty = true
    }

    fun setRotation(degrees: Float) {
        rotation = degrees
        dirty = true
    }

    /**
     * Sets the sprite's rotation relative to the current rotation.
     */
    fun rotate(degrees: Float) {
        rotation += degrees
        dirty = true
    }

    fun setScale(scaleXY: Float) {
        scaleX = scaleXY
        scaleY = scaleXY
        dirty = true
    }

    fun setScale(scaleX: Float, scaleY: Float) {
        this.scaleX = scaleX
        this.scaleY = scaleY
        dirty = true
    }

    /**
     * Sets the sprite's scale relative to the current scale.
     */
    fun scale(amount: Float) {
        scaleX += amount
        scaleY += amount
        dirty = true
    }

    /**
     * Returns the packed vertices, colors, and texture coordinates for this sprite.
     */
    fun getVertices(): FloatArray? {
        if (!dirty) return vertices
        dirty = false
        val originX = originX
        val originY = originY
        val scaleX = scaleX
        val scaleY = scaleY
        val region: PolygonRegion? = region
        val vertices = vertices
        val regionVertices: FloatArray = region!!.vertices
        val worldOriginX = x + originX
        val worldOriginY = y + originY
        val sX: Float = width / region!!.region.getRegionWidth()
        val sY: Float = height / region!!.region.getRegionHeight()
        val cos: Float = MathUtils.cosDeg(rotation)
        val sin: Float = MathUtils.sinDeg(rotation)
        var fx: Float
        var fy: Float
        var i = 0
        var v = 0
        val n = regionVertices.size
        while (i < n) {
            fx = (regionVertices[i] * sX - originX) * scaleX
            fy = (regionVertices[i + 1] * sY - originY) * scaleY
            vertices!![v] = cos * fx - sin * fy + worldOriginX
            vertices[v + 1] = sin * fx + cos * fy + worldOriginY
            i += 2
            v += 5
        }
        return vertices
    }

    /**
     * Returns the bounding axis aligned [Rectangle] that bounds this sprite. The rectangles x and y coordinates describe its
     * bottom left corner. If you change the position or size of the sprite, you have to fetch the triangle again for it to be
     * recomputed.
     *
     * @return the bounding Rectangle
     */
    val boundingRectangle: Rectangle
        get() {
            val vertices = getVertices()
            var minx = vertices!![0]
            var miny = vertices[1]
            var maxx = vertices[0]
            var maxy = vertices[1]
            var i = 5
            while (i < vertices.size) {
                val x = vertices[i]
                val y = vertices[i + 1]
                minx = if (minx > x) x else minx
                maxx = if (maxx < x) x else maxx
                miny = if (miny > y) y else miny
                maxy = if (maxy < y) y else maxy
                i += 5
            }
            bounds.x = minx
            bounds.y = miny
            bounds.width = maxx - minx
            bounds.height = maxy - miny
            return bounds
        }

    fun draw(spriteBatch: PolygonSpriteBatch) {
        val region: PolygonRegion? = region
        spriteBatch.draw(region!!.region.texture, getVertices(), 0, vertices!!.size, region!!.triangles, 0, region!!.triangles.length)
    }

    fun draw(spriteBatch: PolygonSpriteBatch, alphaModulation: Float) {
        val color: Color = getColor()
        val oldAlpha: Float = color.a
        color.a *= alphaModulation
        setColor(color)
        draw(spriteBatch)
        color.a = oldAlpha
        setColor(color)
    }

    fun getX(): Float {
        return x
    }

    fun getY(): Float {
        return y
    }

    fun getRotation(): Float {
        return rotation
    }

    /**
     * Returns the color of this sprite. Modifying the returned color will have unexpected effects unless [.setColor]
     * or [.setColor] is subsequently called before drawing this sprite.
     */
    fun getColor(): Color {
        return color
    }

    /**
     * Returns the actual color used in the vertices of this sprite. Modifying the returned color will have unexpected effects
     * unless [.setColor] or [.setColor] is subsequently called before drawing this
     * sprite.
     */
    val packedColor: Color
        get() {
            Color.abgr8888ToColor(color, vertices!![2])
            return color
        }

    fun setRegion(region: PolygonRegion?) {
        this.region = region
        val regionVertices: FloatArray = region!!.vertices
        val textureCoords: FloatArray = region!!.textureCoords
        val verticesLength = regionVertices.size / 2 * 5
        if (vertices == null || vertices!!.size != verticesLength) vertices = FloatArray(verticesLength)

        // Set the color and UVs in this sprite's vertices.
        val floatColor: Float = color.toFloatBits()
        val vertices = vertices
        var i = 0
        var v = 2
        while (v < verticesLength) {
            vertices!![v] = floatColor
            vertices[v + 1] = textureCoords[i]
            vertices[v + 2] = textureCoords[i + 1]
            i += 2
            v += 5
        }
        dirty = true
    }

    fun getRegion(): PolygonRegion? {
        return region
    }
}
