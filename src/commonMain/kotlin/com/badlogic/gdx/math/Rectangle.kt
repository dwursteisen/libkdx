/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package com.badlogic.gdx.math

import com.badlogic.gdx.utils.GdxRuntimeException
import com.badlogic.gdx.utils.NumberUtils
import com.badlogic.gdx.utils.Scaling
import java.lang.NumberFormatException

/**
 * Encapsulates a 2D rectangle defined by its corner point in the bottom left and its extents in x (width) and y (height).
 *
 * @author badlogicgames@gmail.com
 */
class Rectangle : java.io.Serializable, Shape2D {

    /**
     * @return the x-coordinate of the bottom left corner
     */
    var x = 0f

    /**
     * @return the y-coordinate of the bottom left corner
     */
    var y = 0f

    /**
     * @return the width
     */
    var width = 0f

    /**
     * @return the height
     */
    var height = 0f

    /**
     * Constructs a new rectangle with all values set to zero
     */
    constructor() {}

    /**
     * Constructs a new rectangle with the given corner point in the bottom left and dimensions.
     *
     * @param x      The corner point x-coordinate
     * @param y      The corner point y-coordinate
     * @param width  The width
     * @param height The height
     */
    constructor(x: Float, y: Float, width: Float, height: Float) {
        this.x = x
        this.y = y
        this.width = width
        this.height = height
    }

    /**
     * Constructs a rectangle based on the given rectangle
     *
     * @param rect The rectangle
     */
    constructor(rect: Rectangle) {
        x = rect.x
        y = rect.y
        width = rect.width
        height = rect.height
    }

    /**
     * @param x      bottom-left x coordinate
     * @param y      bottom-left y coordinate
     * @param width  width
     * @param height height
     * @return this rectangle for chaining
     */
    operator fun set(x: Float, y: Float, width: Float, height: Float): Rectangle {
        this.x = x
        this.y = y
        this.width = width
        this.height = height
        return this
    }

    /**
     * Sets the x-coordinate of the bottom left corner
     *
     * @param x The x-coordinate
     * @return this rectangle for chaining
     */
    fun setX(x: Float): Rectangle {
        this.x = x
        return this
    }

    /**
     * Sets the y-coordinate of the bottom left corner
     *
     * @param y The y-coordinate
     * @return this rectangle for chaining
     */
    fun setY(y: Float): Rectangle {
        this.y = y
        return this
    }

    /**
     * Sets the width of this rectangle
     *
     * @param width The width
     * @return this rectangle for chaining
     */
    fun setWidth(width: Float): Rectangle {
        this.width = width
        return this
    }

    /**
     * Sets the height of this rectangle
     *
     * @param height The height
     * @return this rectangle for chaining
     */
    fun setHeight(height: Float): Rectangle {
        this.height = height
        return this
    }

    /**
     * return the Vector2 with coordinates of this rectangle
     *
     * @param position The Vector2
     */
    fun getPosition(position: Vector2): Vector2 {
        return position.set(x, y)
    }

    /**
     * Sets the x and y-coordinates of the bottom left corner from vector
     *
     * @param position The position vector
     * @return this rectangle for chaining
     */
    fun setPosition(position: Vector2): Rectangle {
        x = position.x
        y = position.y
        return this
    }

    /**
     * Sets the x and y-coordinates of the bottom left corner
     *
     * @param x The x-coordinate
     * @param y The y-coordinate
     * @return this rectangle for chaining
     */
    fun setPosition(x: Float, y: Float): Rectangle {
        this.x = x
        this.y = y
        return this
    }

    /**
     * Sets the width and height of this rectangle
     *
     * @param width  The width
     * @param height The height
     * @return this rectangle for chaining
     */
    fun setSize(width: Float, height: Float): Rectangle {
        this.width = width
        this.height = height
        return this
    }

    /**
     * Sets the squared size of this rectangle
     *
     * @param sizeXY The size
     * @return this rectangle for chaining
     */
    fun setSize(sizeXY: Float): Rectangle {
        width = sizeXY
        height = sizeXY
        return this
    }

    /**
     * @param size The Vector2
     * @return the Vector2 with size of this rectangle
     */
    fun getSize(size: Vector2): Vector2 {
        return size.set(width, height)
    }

    /**
     * @param x point x coordinate
     * @param y point y coordinate
     * @return whether the point is contained in the rectangle
     */
    fun contains(x: Float, y: Float): Boolean {
        return this.x <= x && this.x + width >= x && this.y <= y && this.y + height >= y
    }

    /**
     * @param point The coordinates vector
     * @return whether the point is contained in the rectangle
     */
    operator fun contains(point: Vector2): Boolean {
        return contains(point.x, point.y)
    }

    /**
     * @param circle the circle
     * @return whether the circle is contained in the rectangle
     */
    operator fun contains(circle: Circle): Boolean {
        return (circle.x - circle.radius >= x && circle.x + circle.radius <= x + width
            && circle.y - circle.radius >= y && circle.y + circle.radius <= y + height)
    }

    /**
     * @param rectangle the other [Rectangle].
     * @return whether the other rectangle is contained in this rectangle.
     */
    operator fun contains(rectangle: Rectangle): Boolean {
        val xmin = rectangle.x
        val xmax = xmin + rectangle.width
        val ymin = rectangle.y
        val ymax = ymin + rectangle.height
        return (xmin > x && xmin < x + width && xmax > x && xmax < x + width
            && ymin > y && ymin < y + height && ymax > y && ymax < y + height)
    }

    /**
     * @param r the other [Rectangle]
     * @return whether this rectangle overlaps the other rectangle.
     */
    fun overlaps(r: Rectangle): Boolean {
        return x < r.x + r.width && x + width > r.x && y < r.y + r.height && y + height > r.y
    }

    /**
     * Sets the values of the given rectangle to this rectangle.
     *
     * @param rect the other rectangle
     * @return this rectangle for chaining
     */
    fun set(rect: Rectangle): Rectangle {
        x = rect.x
        y = rect.y
        width = rect.width
        height = rect.height
        return this
    }

    /**
     * Merges this rectangle with the other rectangle. The rectangle should not have negative width or negative height.
     *
     * @param rect the other rectangle
     * @return this rectangle for chaining
     */
    fun merge(rect: Rectangle): Rectangle {
        val minX: Float = java.lang.Math.min(x, rect.x)
        val maxX: Float = max(x + width, rect.x + rect.width)
        x = minX
        width = maxX - minX
        val minY: Float = java.lang.Math.min(y, rect.y)
        val maxY: Float = max(y + height, rect.y + rect.height)
        y = minY
        height = maxY - minY
        return this
    }

    /**
     * Merges this rectangle with a point. The rectangle should not have negative width or negative height.
     *
     * @param x the x coordinate of the point
     * @param y the y coordinate of the point
     * @return this rectangle for chaining
     */
    fun merge(x: Float, y: Float): Rectangle {
        val minX: Float = java.lang.Math.min(this.x, x)
        val maxX: Float = max(this.x + width, x)
        this.x = minX
        width = maxX - minX
        val minY: Float = java.lang.Math.min(this.y, y)
        val maxY: Float = max(this.y + height, y)
        this.y = minY
        height = maxY - minY
        return this
    }

    /**
     * Merges this rectangle with a point. The rectangle should not have negative width or negative height.
     *
     * @param vec the vector describing the point
     * @return this rectangle for chaining
     */
    fun merge(vec: Vector2): Rectangle {
        return merge(vec.x, vec.y)
    }

    /**
     * Merges this rectangle with a list of points. The rectangle should not have negative width or negative height.
     *
     * @param vecs the vectors describing the points
     * @return this rectangle for chaining
     */
    fun merge(vecs: Array<Vector2>): Rectangle {
        var minX = x
        var maxX = x + width
        var minY = y
        var maxY = y + height
        for (i in vecs.indices) {
            val v = vecs[i]
            minX = java.lang.Math.min(minX, v.x)
            maxX = max(maxX, v.x)
            minY = java.lang.Math.min(minY, v.y)
            maxY = max(maxY, v.y)
        }
        x = minX
        width = maxX - minX
        y = minY
        height = maxY - minY
        return this
    }

    /**
     * Calculates the aspect ratio ( width / height ) of this rectangle
     *
     * @return the aspect ratio of this rectangle. Returns Float.NaN if height is 0 to avoid ArithmeticException
     */
    val aspectRatio: Float
        get() = if (height == 0f) Float.NaN else width / height

    /**
     * Calculates the center of the rectangle. Results are located in the given Vector2
     *
     * @param vector the Vector2 to use
     * @return the given vector with results stored inside
     */
    fun getCenter(vector: Vector2): Vector2 {
        vector.x = x + width / 2
        vector.y = y + height / 2
        return vector
    }

    /**
     * Moves this rectangle so that its center point is located at a given position
     *
     * @param x the position's x
     * @param y the position's y
     * @return this for chaining
     */
    fun setCenter(x: Float, y: Float): Rectangle {
        setPosition(x - width / 2, y - height / 2)
        return this
    }

    /**
     * Moves this rectangle so that its center point is located at a given position
     *
     * @param position the position
     * @return this for chaining
     */
    fun setCenter(position: Vector2): Rectangle {
        setPosition(position.x - width / 2, position.y - height / 2)
        return this
    }

    /**
     * Fits this rectangle around another rectangle while maintaining aspect ratio. This scales and centers the rectangle to the
     * other rectangle (e.g. Having a camera translate and scale to show a given area)
     *
     * @param rect the other rectangle to fit this rectangle around
     * @return this rectangle for chaining
     * @see Scaling
     */
    fun fitOutside(rect: Rectangle): Rectangle {
        val ratio = aspectRatio
        if (ratio > rect.aspectRatio) {
            // Wider than tall
            setSize(rect.height * ratio, rect.height)
        } else {
            // Taller than wide
            setSize(rect.width, rect.width / ratio)
        }
        setPosition(rect.x + rect.width / 2 - width / 2, rect.y + rect.height / 2 - height / 2)
        return this
    }

    /**
     * Fits this rectangle into another rectangle while maintaining aspect ratio. This scales and centers the rectangle to the
     * other rectangle (e.g. Scaling a texture within a arbitrary cell without squeezing)
     *
     * @param rect the other rectangle to fit this rectangle inside
     * @return this rectangle for chaining
     * @see Scaling
     */
    fun fitInside(rect: Rectangle): Rectangle {
        val ratio = aspectRatio
        if (ratio < rect.aspectRatio) {
            // Taller than wide
            setSize(rect.height * ratio, rect.height)
        } else {
            // Wider than tall
            setSize(rect.width, rect.width / ratio)
        }
        setPosition(rect.x + rect.width / 2 - width / 2, rect.y + rect.height / 2 - height / 2)
        return this
    }

    /**
     * Converts this `Rectangle` to a string in the format `[x,y,width,height]`.
     *
     * @return a string representation of this object.
     */
    override fun toString(): String {
        return "[$x,$y,$width,$height]"
    }

    /**
     * Sets this `Rectangle` to the value represented by the specified string according to the format of [.toString]
     * .
     *
     * @param v the string.
     * @return this rectangle for chaining
     */
    fun fromString(v: String): Rectangle {
        val s0 = v.indexOf(',', 1)
        val s1 = v.indexOf(',', s0 + 1)
        val s2 = v.indexOf(',', s1 + 1)
        if (s0 != -1 && s1 != -1 && s2 != -1 && v[0] == '[' && v[v.length - 1] == ']') {
            try {
                val x = v.substring(1, s0).toFloat()
                val y = v.substring(s0 + 1, s1).toFloat()
                val width = v.substring(s1 + 1, s2).toFloat()
                val height = v.substring(s2 + 1, v.length - 1).toFloat()
                return this.set(x, y, width, height)
            } catch (ex: NumberFormatException) {
                // Throw a GdxRuntimeException
            }
        }
        throw GdxRuntimeException("Malformed Rectangle: $v")
    }

    fun area(): Float {
        return width * height
    }

    fun perimeter(): Float {
        return 2 * (width + height)
    }

    override fun hashCode(): Int {
        val prime = 31
        var result = 1
        result = prime * result + NumberUtils.floatToRawIntBits(height)
        result = prime * result + NumberUtils.floatToRawIntBits(width)
        result = prime * result + NumberUtils.floatToRawIntBits(x)
        result = prime * result + NumberUtils.floatToRawIntBits(y)
        return result
    }

    override fun equals(obj: Any?): Boolean {
        if (this === obj) return true
        if (obj == null) return false
        if (javaClass != obj.javaClass) return false
        val other = obj as Rectangle
        if (NumberUtils.floatToRawIntBits(height) !== NumberUtils.floatToRawIntBits(other.height)) return false
        if (NumberUtils.floatToRawIntBits(width) !== NumberUtils.floatToRawIntBits(other.width)) return false
        if (NumberUtils.floatToRawIntBits(x) !== NumberUtils.floatToRawIntBits(other.x)) return false
        return if (NumberUtils.floatToRawIntBits(y) !== NumberUtils.floatToRawIntBits(other.y)) false else true
    }

    companion object {
        /**
         * Static temporary rectangle. Use with care! Use only when sure other code will not also use this.
         */
        val tmp = Rectangle()

        /**
         * Static temporary rectangle. Use with care! Use only when sure other code will not also use this.
         */
        val tmp2 = Rectangle()
        private const val serialVersionUID = 5733252015138115702L
    }
}
