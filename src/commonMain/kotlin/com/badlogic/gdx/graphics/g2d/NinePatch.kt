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

/**
 * A 3x3 grid of texture regions. Any of the regions may be omitted. Padding may be set as a hint on how to inset content on top
 * of the ninepatch (by default the eight "edge" textures of the nine-patch define the padding). When drawn the eight "edge"
 * patches will not be scaled, only the interior patch will be scaled.
 *
 *
 * **NOTE**: This class expects a "post-processed" nine-patch, and not a raw ".9.png" texture. That is, the textures given to
 * this class should *not* include the meta-data pixels from a ".9.png" that describe the layout of the ninepatch over the
 * interior of the graphic. That information should be passed into the constructor either implicitly as the size of the individual
 * patch textures, or via the `left, right, top, bottom` parameters to [.NinePatch]
 * or [.NinePatch].
 *
 *
 * [TextureAtlas] is one way to generate a post-processed nine-patch from a ".9.png" file.
 */
class NinePatch {

    private var texture: Texture? = null
    private var bottomLeft = -1
    private var bottomCenter = -1
    private var bottomRight = -1
    private var middleLeft = -1
    private var middleCenter = -1
    private var middleRight = -1
    private var topLeft = -1
    private var topCenter = -1
    private var topRight = -1

    /**
     * Set the draw-time width of the three left edge patches
     */
    var leftWidth = 0f

    /**
     * Set the draw-time width of the three right edge patches
     */
    var rightWidth = 0f

    /**
     * Set the width of the middle column of the patch. At render time, this is implicitly the requested render-width of the
     * entire nine patch, minus the left and right width. This value is only used for computing the [default][.getTotalWidth].
     */
    var middleWidth = 0f

    /**
     * Set the height of the middle row of the patch. At render time, this is implicitly the requested render-height of the entire
     * nine patch, minus the top and bottom height. This value is only used for computing the [default][.getTotalHeight].
     */
    var middleHeight = 0f

    /**
     * Set the draw-time height of the three top edge patches
     */
    var topHeight = 0f

    /**
     * Set the draw-time height of the three bottom edge patches
     */
    var bottomHeight = 0f
    private var vertices = FloatArray(9 * 4 * 5)
    private var idx = 0
    private val color: Color = Color(Color.WHITE)
    private var padLeft = -1f
    private var padRight = -1f
    private var padTop = -1f
    private var padBottom = -1f

    /**
     * Create a ninepatch by cutting up the given texture into nine patches. The subsequent parameters define the 4 lines that
     * will cut the texture region into 9 pieces.
     *
     * @param left   Pixels from left edge.
     * @param right  Pixels from right edge.
     * @param top    Pixels from top edge.
     * @param bottom Pixels from bottom edge.
     */
    constructor(texture: Texture?, left: Int, right: Int, top: Int, bottom: Int) : this(TextureRegion(texture), left, right, top, bottom) {}

    /**
     * Create a ninepatch by cutting up the given texture region into nine patches. The subsequent parameters define the 4 lines
     * that will cut the texture region into 9 pieces.
     *
     * @param left   Pixels from left edge.
     * @param right  Pixels from right edge.
     * @param top    Pixels from top edge.
     * @param bottom Pixels from bottom edge.
     */
    constructor(region: TextureRegion?, left: Int, right: Int, top: Int, bottom: Int) {
        if (region == null) throw java.lang.IllegalArgumentException("region cannot be null.")
        val middleWidth = region.getRegionWidth() - left - right
        val middleHeight = region.getRegionHeight() - top - bottom
        val patches = arrayOfNulls<TextureRegion>(9)
        if (top > 0) {
            if (left > 0) patches[TOP_LEFT] = TextureRegion(region, 0, 0, left, top)
            if (middleWidth > 0) patches[TOP_CENTER] = TextureRegion(region, left, 0, middleWidth, top)
            if (right > 0) patches[TOP_RIGHT] = TextureRegion(region, left + middleWidth, 0, right, top)
        }
        if (middleHeight > 0) {
            if (left > 0) patches[MIDDLE_LEFT] = TextureRegion(region, 0, top, left, middleHeight)
            if (middleWidth > 0) patches[MIDDLE_CENTER] = TextureRegion(region, left, top, middleWidth, middleHeight)
            if (right > 0) patches[MIDDLE_RIGHT] = TextureRegion(region, left + middleWidth, top, right, middleHeight)
        }
        if (bottom > 0) {
            if (left > 0) patches[BOTTOM_LEFT] = TextureRegion(region, 0, top + middleHeight, left, bottom)
            if (middleWidth > 0) patches[BOTTOM_CENTER] = TextureRegion(region, left, top + middleHeight, middleWidth, bottom)
            if (right > 0) patches[BOTTOM_RIGHT] = TextureRegion(region, left + middleWidth, top + middleHeight, right, bottom)
        }

        // If split only vertical, move splits from right to center.
        if (left == 0 && middleWidth == 0) {
            patches[TOP_CENTER] = patches[TOP_RIGHT]
            patches[MIDDLE_CENTER] = patches[MIDDLE_RIGHT]
            patches[BOTTOM_CENTER] = patches[BOTTOM_RIGHT]
            patches[TOP_RIGHT] = null
            patches[MIDDLE_RIGHT] = null
            patches[BOTTOM_RIGHT] = null
        }
        // If split only horizontal, move splits from bottom to center.
        if (top == 0 && middleHeight == 0) {
            patches[MIDDLE_LEFT] = patches[BOTTOM_LEFT]
            patches[MIDDLE_CENTER] = patches[BOTTOM_CENTER]
            patches[MIDDLE_RIGHT] = patches[BOTTOM_RIGHT]
            patches[BOTTOM_LEFT] = null
            patches[BOTTOM_CENTER] = null
            patches[BOTTOM_RIGHT] = null
        }
        load(patches)
    }

    /**
     * Construct a degenerate "nine" patch with only a center component.
     */
    constructor(texture: Texture?, color: Color?) : this(texture) {
        setColor(color)
    }

    /**
     * Construct a degenerate "nine" patch with only a center component.
     */
    constructor(texture: Texture?) : this(TextureRegion(texture)) {}

    /**
     * Construct a degenerate "nine" patch with only a center component.
     */
    constructor(region: TextureRegion?, color: Color?) : this(region) {
        setColor(color)
    }

    /**
     * Construct a degenerate "nine" patch with only a center component.
     */
    constructor(region: TextureRegion?) {
        load(arrayOf( //
            null, null, null,  //
            null, region, null,  //
            null, null, null //
        ))
    }

    /**
     * Construct a nine patch from the given nine texture regions. The provided patches must be consistently sized (e.g., any left
     * edge textures must have the same width, etc). Patches may be `null`. Patch indices are specified via the public
     * members [.TOP_LEFT], [.TOP_CENTER], etc.
     */
    constructor(vararg patches: TextureRegion?) {
        if (patches == null || patches.size != 9) throw java.lang.IllegalArgumentException("NinePatch needs nine TextureRegions")
        load(patches)
        val leftWidth = leftWidth
        if (patches[TOP_LEFT] != null && patches[TOP_LEFT]!!.getRegionWidth() != leftWidth
            || patches[MIDDLE_LEFT] != null && patches[MIDDLE_LEFT]!!.getRegionWidth() != leftWidth
            || patches[BOTTOM_LEFT] != null && patches[BOTTOM_LEFT]!!.getRegionWidth() != leftWidth) {
            throw GdxRuntimeException("Left side patches must have the same width")
        }
        val rightWidth = rightWidth
        if (patches[TOP_RIGHT] != null && patches[TOP_RIGHT]!!.getRegionWidth() != rightWidth
            || patches[MIDDLE_RIGHT] != null && patches[MIDDLE_RIGHT]!!.getRegionWidth() != rightWidth
            || patches[BOTTOM_RIGHT] != null && patches[BOTTOM_RIGHT]!!.getRegionWidth() != rightWidth) {
            throw GdxRuntimeException("Right side patches must have the same width")
        }
        val bottomHeight = bottomHeight
        if (patches[BOTTOM_LEFT] != null && patches[BOTTOM_LEFT]!!.getRegionHeight() != bottomHeight
            || patches[BOTTOM_CENTER] != null && patches[BOTTOM_CENTER]!!.getRegionHeight() != bottomHeight
            || patches[BOTTOM_RIGHT] != null && patches[BOTTOM_RIGHT]!!.getRegionHeight() != bottomHeight) {
            throw GdxRuntimeException("Bottom side patches must have the same height")
        }
        val topHeight = topHeight
        if (patches[TOP_LEFT] != null && patches[TOP_LEFT]!!.getRegionHeight() != topHeight
            || patches[TOP_CENTER] != null && patches[TOP_CENTER]!!.getRegionHeight() != topHeight
            || patches[TOP_RIGHT] != null && patches[TOP_RIGHT]!!.getRegionHeight() != topHeight) {
            throw GdxRuntimeException("Top side patches must have the same height")
        }
    }

    @JvmOverloads
    constructor(ninePatch: NinePatch, color: Color? = ninePatch.this.color) {
        texture = ninePatch.texture
        bottomLeft = ninePatch.bottomLeft
        bottomCenter = ninePatch.bottomCenter
        bottomRight = ninePatch.bottomRight
        middleLeft = ninePatch.middleLeft
        middleCenter = ninePatch.middleCenter
        middleRight = ninePatch.middleRight
        topLeft = ninePatch.topLeft
        topCenter = ninePatch.topCenter
        topRight = ninePatch.topRight
        leftWidth = ninePatch.leftWidth
        rightWidth = ninePatch.rightWidth
        middleWidth = ninePatch.middleWidth
        middleHeight = ninePatch.middleHeight
        topHeight = ninePatch.topHeight
        bottomHeight = ninePatch.bottomHeight
        padLeft = ninePatch.padLeft
        padTop = ninePatch.padTop
        padBottom = ninePatch.padBottom
        padRight = ninePatch.padRight
        vertices = FloatArray(ninePatch.vertices.size)
        java.lang.System.arraycopy(ninePatch.vertices, 0, vertices, 0, ninePatch.vertices.size)
        idx = ninePatch.idx
        this.color.set(color)
    }

    private fun load(patches: Array<TextureRegion?>) {
        val color: Float = Color.WHITE_FLOAT_BITS // placeholder color, overwritten at draw time
        if (patches[BOTTOM_LEFT] != null) {
            bottomLeft = add(patches[BOTTOM_LEFT], color, false, false)
            leftWidth = patches[BOTTOM_LEFT]!!.getRegionWidth().toFloat()
            bottomHeight = patches[BOTTOM_LEFT]!!.getRegionHeight().toFloat()
        }
        if (patches[BOTTOM_CENTER] != null) {
            bottomCenter = add(patches[BOTTOM_CENTER], color, true, false)
            middleWidth = java.lang.Math.max(middleWidth, patches[BOTTOM_CENTER]!!.getRegionWidth())
            bottomHeight = java.lang.Math.max(bottomHeight, patches[BOTTOM_CENTER]!!.getRegionHeight())
        }
        if (patches[BOTTOM_RIGHT] != null) {
            bottomRight = add(patches[BOTTOM_RIGHT], color, false, false)
            rightWidth = java.lang.Math.max(rightWidth, patches[BOTTOM_RIGHT]!!.getRegionWidth())
            bottomHeight = java.lang.Math.max(bottomHeight, patches[BOTTOM_RIGHT]!!.getRegionHeight())
        }
        if (patches[MIDDLE_LEFT] != null) {
            middleLeft = add(patches[MIDDLE_LEFT], color, false, true)
            leftWidth = java.lang.Math.max(leftWidth, patches[MIDDLE_LEFT]!!.getRegionWidth())
            middleHeight = java.lang.Math.max(middleHeight, patches[MIDDLE_LEFT]!!.getRegionHeight())
        }
        if (patches[MIDDLE_CENTER] != null) {
            middleCenter = add(patches[MIDDLE_CENTER], color, true, true)
            middleWidth = java.lang.Math.max(middleWidth, patches[MIDDLE_CENTER]!!.getRegionWidth())
            middleHeight = java.lang.Math.max(middleHeight, patches[MIDDLE_CENTER]!!.getRegionHeight())
        }
        if (patches[MIDDLE_RIGHT] != null) {
            middleRight = add(patches[MIDDLE_RIGHT], color, false, true)
            rightWidth = java.lang.Math.max(rightWidth, patches[MIDDLE_RIGHT]!!.getRegionWidth())
            middleHeight = java.lang.Math.max(middleHeight, patches[MIDDLE_RIGHT]!!.getRegionHeight())
        }
        if (patches[TOP_LEFT] != null) {
            topLeft = add(patches[TOP_LEFT], color, false, false)
            leftWidth = java.lang.Math.max(leftWidth, patches[TOP_LEFT]!!.getRegionWidth())
            topHeight = java.lang.Math.max(topHeight, patches[TOP_LEFT]!!.getRegionHeight())
        }
        if (patches[TOP_CENTER] != null) {
            topCenter = add(patches[TOP_CENTER], color, true, false)
            middleWidth = java.lang.Math.max(middleWidth, patches[TOP_CENTER]!!.getRegionWidth())
            topHeight = java.lang.Math.max(topHeight, patches[TOP_CENTER]!!.getRegionHeight())
        }
        if (patches[TOP_RIGHT] != null) {
            topRight = add(patches[TOP_RIGHT], color, false, false)
            rightWidth = java.lang.Math.max(rightWidth, patches[TOP_RIGHT]!!.getRegionWidth())
            topHeight = java.lang.Math.max(topHeight, patches[TOP_RIGHT]!!.getRegionHeight())
        }
        if (idx < vertices.size) {
            val newVertices = FloatArray(idx)
            java.lang.System.arraycopy(vertices, 0, newVertices, 0, idx)
            vertices = newVertices
        }
    }

    private fun add(region: TextureRegion?, color: Float, isStretchW: Boolean, isStretchH: Boolean): Int {
        if (texture == null) texture = region!!.getTexture() else if (texture !== region!!.getTexture()) throw java.lang.IllegalArgumentException("All regions must be from the same texture.")
        var u = region!!.u
        var v = region.v2
        var u2 = region.u2
        var v2 = region.v

        // Add half pixel offsets on stretchable dimensions to avoid color bleeding when GL_LINEAR
        // filtering is used for the texture. This nudges the texture coordinate to the center
        // of the texel where the neighboring pixel has 0% contribution in linear blending mode.
        if (texture.getMagFilter() === TextureFilter.Linear || texture.getMinFilter() === TextureFilter.Linear) {
            if (isStretchW) {
                val halfTexelWidth: Float = 0.5f * 1.0f / texture.getWidth()
                u += halfTexelWidth
                u2 -= halfTexelWidth
            }
            if (isStretchH) {
                val halfTexelHeight: Float = 0.5f * 1.0f / texture.getHeight()
                v -= halfTexelHeight
                v2 += halfTexelHeight
            }
        }
        val vertices = vertices
        vertices[idx + 2] = color
        vertices[idx + 3] = u
        vertices[idx + 4] = v
        vertices[idx + 7] = color
        vertices[idx + 8] = u
        vertices[idx + 9] = v2
        vertices[idx + 12] = color
        vertices[idx + 13] = u2
        vertices[idx + 14] = v2
        vertices[idx + 17] = color
        vertices[idx + 18] = u2
        vertices[idx + 19] = v
        idx += 20
        return idx - 20
    }

    /**
     * Set the coordinates and color of a ninth of the patch.
     */
    private operator fun set(idx: Int, x: Float, y: Float, width: Float, height: Float, color: Float) {
        val fx2 = x + width
        val fy2 = y + height
        val vertices = vertices
        vertices[idx] = x
        vertices[idx + 1] = y
        vertices[idx + 2] = color
        vertices[idx + 5] = x
        vertices[idx + 6] = fy2
        vertices[idx + 7] = color
        vertices[idx + 10] = fx2
        vertices[idx + 11] = fy2
        vertices[idx + 12] = color
        vertices[idx + 15] = fx2
        vertices[idx + 16] = y
        vertices[idx + 17] = color
    }

    private fun prepareVertices(batch: Batch, x: Float, y: Float, width: Float, height: Float) {
        val centerColumnX = x + leftWidth
        val rightColumnX = x + width - rightWidth
        val middleRowY = y + bottomHeight
        val topRowY = y + height - topHeight
        val c: Float = tmpDrawColor.set(color).mul(batch.getColor()).toFloatBits()
        if (bottomLeft != -1) set(bottomLeft, x, y, centerColumnX - x, middleRowY - y, c)
        if (bottomCenter != -1) set(bottomCenter, centerColumnX, y, rightColumnX - centerColumnX, middleRowY - y, c)
        if (bottomRight != -1) set(bottomRight, rightColumnX, y, x + width - rightColumnX, middleRowY - y, c)
        if (middleLeft != -1) set(middleLeft, x, middleRowY, centerColumnX - x, topRowY - middleRowY, c)
        if (middleCenter != -1) set(middleCenter, centerColumnX, middleRowY, rightColumnX - centerColumnX, topRowY - middleRowY, c)
        if (middleRight != -1) set(middleRight, rightColumnX, middleRowY, x + width - rightColumnX, topRowY - middleRowY, c)
        if (topLeft != -1) set(topLeft, x, topRowY, centerColumnX - x, y + height - topRowY, c)
        if (topCenter != -1) set(topCenter, centerColumnX, topRowY, rightColumnX - centerColumnX, y + height - topRowY, c)
        if (topRight != -1) set(topRight, rightColumnX, topRowY, x + width - rightColumnX, y + height - topRowY, c)
    }

    fun draw(batch: Batch, x: Float, y: Float, width: Float, height: Float) {
        prepareVertices(batch, x, y, width, height)
        batch.draw(texture, vertices, 0, idx)
    }

    fun draw(batch: Batch, x: Float, y: Float, originX: Float, originY: Float, width: Float, height: Float, scaleX: Float,
             scaleY: Float, rotation: Float) {
        prepareVertices(batch, x, y, width, height)
        val worldOriginX = x + originX
        val worldOriginY = y + originY
        val n = idx
        val vertices = vertices
        if (rotation != 0f) {
            var i = 0
            while (i < n) {
                val vx = (vertices[i] - worldOriginX) * scaleX
                val vy = (vertices[i + 1] - worldOriginY) * scaleY
                val cos: Float = MathUtils.cosDeg(rotation)
                val sin: Float = MathUtils.sinDeg(rotation)
                vertices[i] = cos * vx - sin * vy + worldOriginX
                vertices[i + 1] = sin * vx + cos * vy + worldOriginY
                i += 5
            }
        } else if (scaleX != 1f || scaleY != 1f) {
            var i = 0
            while (i < n) {
                vertices[i] = (vertices[i] - worldOriginX) * scaleX + worldOriginX
                vertices[i + 1] = (vertices[i + 1] - worldOriginY) * scaleY + worldOriginY
                i += 5
            }
        }
        batch.draw(texture, vertices, 0, n)
    }

    /**
     * Copy given color. The color will be blended with the batch color, then combined with the texture colors at
     * [draw][NinePatch.draw] time. Default is [Color.WHITE].
     */
    fun setColor(color: Color?) {
        this.color.set(color)
    }

    fun getColor(): Color {
        return color
    }

    val totalWidth: Float
        get() = leftWidth + middleWidth + rightWidth

    val totalHeight: Float
        get() = topHeight + middleHeight + bottomHeight

    /**
     * Set the padding for content inside this ninepatch. By default the padding is set to match the exterior of the ninepatch, so
     * the content should fit exactly within the middle patch.
     */
    fun setPadding(left: Float, right: Float, top: Float, bottom: Float) {
        padLeft = left
        padRight = right
        padTop = top
        padBottom = bottom
    }

    /**
     * Returns the left padding if set, else returns [.getLeftWidth].
     */
    fun getPadLeft(): Float {
        return if (padLeft == -1f) leftWidth else padLeft
    }

    /**
     * See [.setPadding]
     */
    fun setPadLeft(left: Float) {
        padLeft = left
    }

    /**
     * Returns the right padding if set, else returns [.getRightWidth].
     */
    fun getPadRight(): Float {
        return if (padRight == -1f) rightWidth else padRight
    }

    /**
     * See [.setPadding]
     */
    fun setPadRight(right: Float) {
        padRight = right
    }

    /**
     * Returns the top padding if set, else returns [.getTopHeight].
     */
    fun getPadTop(): Float {
        return if (padTop == -1f) topHeight else padTop
    }

    /**
     * See [.setPadding]
     */
    fun setPadTop(top: Float) {
        padTop = top
    }

    /**
     * Returns the bottom padding if set, else returns [.getBottomHeight].
     */
    fun getPadBottom(): Float {
        return if (padBottom == -1f) bottomHeight else padBottom
    }

    /**
     * See [.setPadding]
     */
    fun setPadBottom(bottom: Float) {
        padBottom = bottom
    }

    /**
     * Multiplies the top/left/bottom/right sizes and padding by the specified amount.
     */
    fun scale(scaleX: Float, scaleY: Float) {
        leftWidth *= scaleX
        rightWidth *= scaleX
        topHeight *= scaleY
        bottomHeight *= scaleY
        middleWidth *= scaleX
        middleHeight *= scaleY
        if (padLeft != -1f) padLeft *= scaleX
        if (padRight != -1f) padRight *= scaleX
        if (padTop != -1f) padTop *= scaleY
        if (padBottom != -1f) padBottom *= scaleY
    }

    fun getTexture(): Texture? {
        return texture
    }

    companion object {
        const val TOP_LEFT = 0
        const val TOP_CENTER = 1
        const val TOP_RIGHT = 2
        const val MIDDLE_LEFT = 3
        const val MIDDLE_CENTER = 4
        const val MIDDLE_RIGHT = 5
        const val BOTTOM_LEFT = 6

        /**
         * Indices for [.NinePatch] constructor
         */
        const val BOTTOM_CENTER = 7 // alphabetically first in javadoc
        const val BOTTOM_RIGHT = 8
        private val tmpDrawColor: Color = Color()
    }
}
