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
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.BitmapFont.BitmapFontData
import com.badlogic.gdx.graphics.g2d.BitmapFont.Glyph
import com.badlogic.gdx.graphics.g2d.GlyphLayout.GlyphRun
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.FloatArray
import com.badlogic.gdx.utils.IntArray
import com.badlogic.gdx.utils.NumberUtils
import com.badlogic.gdx.utils.Pools

/** Caches glyph geometry for a BitmapFont, providing a fast way to render static text. This saves needing to compute the glyph
 * geometry each frame.
 * @author Nathan Sweet
 * @author davebaol
 * @author Alexander Dorokhov
 */
class BitmapFontCache @JvmOverloads constructor(font: BitmapFont, integer: Boolean = font.usesIntegerPositions()) {

    private val font: BitmapFont
    private var integer: Boolean
    private val layouts: Array<com.badlogic.gdx.graphics.g2d.GlyphLayout> = Array()
    private val pooledLayouts: Array<com.badlogic.gdx.graphics.g2d.GlyphLayout> = Array()
    private var glyphCount = 0

    /** Returns the x position of the cached string, relative to the position when the string was cached.  */
    var x = 0f
        private set

    /** Returns the y position of the cached string, relative to the position when the string was cached.  */
    var y = 0f
        private set
    private val color: Color = Color(1, 1, 1, 1)
    private var currentTint = 0f

    /** Vertex data per page.  */
    private var pageVertices: Array<FloatArray?>

    /** Number of vertex data entries per page.  */
    private var idx: IntArray

    /** For each page, an array with a value for each glyph from that page, where the value is the index of the character in the
     * full text being cached.  */
    private var pageGlyphIndices: Array<IntArray?>?

    /** Used internally to ensure a correct capacity for multi-page font vertex data.  */
    private var tempGlyphCount: IntArray

    /** Sets the position of the text, relative to the position when the cached text was created.
     * @param x The x coordinate
     * @param y The y coordinate
     */
    fun setPosition(x: Float, y: Float) {
        translate(x - this.x, y - this.y)
    }

    /** Sets the position of the text, relative to its current position.
     * @param xAmount The amount in x to move the text
     * @param yAmount The amount in y to move the text
     */
    fun translate(xAmount: Float, yAmount: Float) {
        var xAmount = xAmount
        var yAmount = yAmount
        if (xAmount == 0f && yAmount == 0f) return
        if (integer) {
            xAmount = java.lang.Math.round(xAmount).toFloat()
            yAmount = java.lang.Math.round(yAmount).toFloat()
        }
        x += xAmount
        y += yAmount
        val pageVertices = pageVertices
        var i = 0
        val n = pageVertices.size
        while (i < n) {
            val vertices = pageVertices[i]
            var ii = 0
            val nn = idx[i]
            while (ii < nn) {
                vertices!![ii] += xAmount
                vertices!![ii + 1] += yAmount
                ii += 5
            }
            i++
        }
    }

    /** Tints all text currently in the cache. Does not affect subsequently added text.  */
    fun tint(tint: Color) {
        val newTint: Float = tint.toFloatBits()
        if (currentTint == newTint) return
        currentTint = newTint
        val tempGlyphCount = tempGlyphCount
        run {
            var i = 0
            val n = tempGlyphCount.size
            while (i < n) {
                tempGlyphCount[i] = 0
                i++
            }
        }
        var i = 0
        val n = layouts.size
        while (i < n) {
            val layout: com.badlogic.gdx.graphics.g2d.GlyphLayout = layouts[i]
            var ii = 0
            val nn: Int = layout.runs.size
            while (ii < nn) {
                val run: GlyphRun = layout.runs.get(ii)
                val glyphs: Array<Glyph> = run.glyphs
                val colorFloat: Float = tempColor.set(run.color).mul(tint).toFloatBits()
                var iii = 0
                val nnn = glyphs.size
                while (iii < nnn) {
                    val glyph: Glyph = glyphs[iii]
                    val page: Int = glyph.page
                    val offset = tempGlyphCount[page] * 20 + 2
                    tempGlyphCount[page]++
                    val vertices = pageVertices[page]
                    var v = 0
                    while (v < 20) {
                        vertices!![offset + v] = colorFloat
                        v += 5
                    }
                    iii++
                }
                ii++
            }
            i++
        }
    }

    /** Sets the alpha component of all text currently in the cache. Does not affect subsequently added text.  */
    fun setAlphas(alpha: Float) {
        val alphaBits = (254 * alpha).toInt() shl 24
        var prev = 0f
        var newColor = 0f
        var j = 0
        val length = pageVertices.size
        while (j < length) {
            val vertices = pageVertices[j]
            var i = 2
            val n = idx[j]
            while (i < n) {
                val c = vertices!![i]
                if (c == prev && i != 2) {
                    vertices[i] = newColor
                } else {
                    prev = c
                    var rgba: Int = NumberUtils.floatToIntColor(c)
                    rgba = rgba and 0x00FFFFFF or alphaBits
                    newColor = NumberUtils.intToFloatColor(rgba)
                    vertices[i] = newColor
                }
                i += 5
            }
            j++
        }
    }

    /** Sets the color of all text currently in the cache. Does not affect subsequently added text.  */
    fun setColors(color: Float) {
        var j = 0
        val length = pageVertices.size
        while (j < length) {
            val vertices = pageVertices[j]
            var i = 2
            val n = idx[j]
            while (i < n) {
                vertices!![i] = color
                i += 5
            }
            j++
        }
    }

    /** Sets the color of all text currently in the cache. Does not affect subsequently added text.  */
    fun setColors(tint: Color) {
        setColors(tint.toFloatBits())
    }

    /** Sets the color of all text currently in the cache. Does not affect subsequently added text.  */
    fun setColors(r: Float, g: Float, b: Float, a: Float) {
        val intBits = (255 * a).toInt() shl 24 or ((255 * b).toInt() shl 16) or ((255 * g).toInt() shl 8) or (255 * r).toInt()
        setColors(NumberUtils.intToFloatColor(intBits))
    }

    /** Sets the color of the specified characters. This may only be called after [.setText] and
     * is reset every time setText is called.  */
    fun setColors(tint: Color, start: Int, end: Int) {
        setColors(tint.toFloatBits(), start, end)
    }

    /** Sets the color of the specified characters. This may only be called after [.setText] and
     * is reset every time setText is called.  */
    fun setColors(color: Float, start: Int, end: Int) {
        if (pageVertices.size == 1) { // One page.
            val vertices = pageVertices[0]
            var i = start * 20 + 2
            val n = end * 20
            while (i < n) {
                vertices!![i] = color
                i += 5
            }
            return
        }
        val pageCount = pageVertices.size
        for (i in 0 until pageCount) {
            val vertices = pageVertices[i]
            val glyphIndices = pageGlyphIndices!![i]
            // Loop through the indices and determine whether the glyph is inside begin/end.
            var j = 0
            val n = glyphIndices!!.size
            while (j < n) {
                val glyphIndex: Int = glyphIndices.items.get(j)

                // Break early if the glyph is out of bounds.
                if (glyphIndex >= end) break

                // If inside start and end, change its colour.
                if (glyphIndex >= start) { // && glyphIndex < end
                    var off = 0
                    while (off < 20) {
                        vertices!![off + (j * 20 + 2)] = color
                        off += 5
                    }
                }
                j++
            }
        }
    }

    /** Returns the color used for subsequently added text. Modifying the color affects text subsequently added to the cache, but
     * does not affect existing text currently in the cache.  */
    fun getColor(): Color {
        return color
    }

    /** A convenience method for setting the cache color. The color can also be set by modifying [.getColor].  */
    fun setColor(color: Color?) {
        this.color.set(color)
    }

    /** A convenience method for setting the cache color. The color can also be set by modifying [.getColor].  */
    fun setColor(r: Float, g: Float, b: Float, a: Float) {
        color.set(r, g, b, a)
    }

    fun draw(spriteBatch: Batch) {
        val regions: Array<TextureRegion> = font.getRegions()
        var j = 0
        val n = pageVertices.size
        while (j < n) {
            if (idx[j] > 0) { // ignore if this texture has no glyphs
                val vertices = pageVertices[j]
                spriteBatch.draw(regions[j].getTexture(), vertices, 0, idx[j])
            }
            j++
        }
    }

    fun draw(spriteBatch: Batch, start: Int, end: Int) {
        if (pageVertices.size == 1) { // 1 page.
            spriteBatch.draw(font.getRegion().getTexture(), pageVertices[0], start * 20, (end - start) * 20)
            return
        }

        // Determine vertex offset and count to render for each page. Some pages might not need to be rendered at all.
        val regions: Array<TextureRegion> = font.getRegions()
        var i = 0
        val pageCount = pageVertices.size
        while (i < pageCount) {
            var offset = -1
            var count = 0

            // For each set of glyph indices, determine where to begin within the start/end bounds.
            val glyphIndices = pageGlyphIndices!![i]
            var ii = 0
            val n = glyphIndices!!.size
            while (ii < n) {
                val glyphIndex = glyphIndices[ii]

                // Break early if the glyph is out of bounds.
                if (glyphIndex >= end) break

                // Determine if this glyph is within bounds. Use the first match of that for the offset.
                if (offset == -1 && glyphIndex >= start) offset = ii

                // Determine the vertex count by counting glyphs within bounds.
                if (glyphIndex >= start) // && gInd < end
                    count++
                ii++
            }

            // Page doesn't need to be rendered.
            if (offset == -1 || count == 0) {
                i++
                continue
            }

            // Render the page vertex data with the offset and count.
            spriteBatch.draw(regions[i].getTexture(), pageVertices[i], offset * 20, count * 20)
            i++
        }
    }

    fun draw(spriteBatch: Batch, alphaModulation: Float) {
        if (alphaModulation == 1f) {
            draw(spriteBatch)
            return
        }
        val color: Color = getColor()
        val oldAlpha: Float = color.a
        color.a *= alphaModulation
        setColors(color)
        draw(spriteBatch)
        color.a = oldAlpha
        setColors(color)
    }

    /** Removes all glyphs in the cache.  */
    fun clear() {
        x = 0f
        y = 0f
        Pools.freeAll(pooledLayouts, true)
        pooledLayouts.clear()
        layouts.clear()
        var i = 0
        val n = idx.size
        while (i < n) {
            if (pageGlyphIndices != null) pageGlyphIndices!![i].clear()
            idx[i] = 0
            i++
        }
    }

    private fun requireGlyphs(layout: com.badlogic.gdx.graphics.g2d.GlyphLayout) {
        if (pageVertices.size == 1) {
            // Simpler counting if we just have one page.
            var newGlyphCount = 0
            var i = 0
            val n: Int = layout.runs.size
            while (i < n) {
                newGlyphCount += layout.runs.get(i).glyphs.size
                i++
            }
            requirePageGlyphs(0, newGlyphCount)
        } else {
            val tempGlyphCount = tempGlyphCount
            run {
                var i = 0
                val n = tempGlyphCount.size
                while (i < n) {
                    tempGlyphCount[i] = 0
                    i++
                }
            }
            // Determine # of glyphs in each page.
            run {
                var i = 0
                val n: Int = layout.runs.size
                while (i < n) {
                    val glyphs: Array<Glyph> = layout.runs.get(i).glyphs
                    var ii = 0
                    val nn = glyphs.size
                    while (ii < nn) {
                        tempGlyphCount[glyphs[ii].page]++
                        ii++
                    }
                    i++
                }
            }
            // Require that many for each page.
            var i = 0
            val n = tempGlyphCount.size
            while (i < n) {
                requirePageGlyphs(i, tempGlyphCount[i])
                i++
            }
        }
    }

    private fun requirePageGlyphs(page: Int, glyphCount: Int) {
        if (pageGlyphIndices != null) {
            if (glyphCount > pageGlyphIndices!![page].items.length) pageGlyphIndices!![page].ensureCapacity(glyphCount - pageGlyphIndices!![page].items.length)
        }
        val vertexCount = idx[page] + glyphCount * 20
        val vertices = pageVertices[page]
        if (vertices == null) {
            pageVertices[page] = FloatArray(vertexCount)
        } else if (vertices.size < vertexCount) {
            val newVertices = FloatArray(vertexCount)
            java.lang.System.arraycopy(vertices, 0, newVertices, 0, idx[page])
            pageVertices[page] = newVertices
        }
    }

    private fun addToCache(layout: com.badlogic.gdx.graphics.g2d.GlyphLayout, x: Float, y: Float) {
        // Check if the number of font pages has changed.
        val pageCount: Int = font.regions.size
        if (pageVertices.size < pageCount) {
            val newPageVertices = arrayOfNulls<FloatArray>(pageCount)
            java.lang.System.arraycopy(pageVertices, 0, newPageVertices, 0, pageVertices.size)
            pageVertices = newPageVertices
            val newIdx = IntArray(pageCount)
            java.lang.System.arraycopy(idx, 0, newIdx, 0, idx.size)
            idx = newIdx
            val newPageGlyphIndices = arrayOfNulls<IntArray>(pageCount)
            var pageGlyphIndicesLength = 0
            if (pageGlyphIndices != null) {
                pageGlyphIndicesLength = pageGlyphIndices!!.size
                java.lang.System.arraycopy(pageGlyphIndices, 0, newPageGlyphIndices, 0, pageGlyphIndices!!.size)
            }
            for (i in pageGlyphIndicesLength until pageCount) newPageGlyphIndices[i] = IntArray()
            pageGlyphIndices = newPageGlyphIndices
            tempGlyphCount = IntArray(pageCount)
        }
        layouts.add(layout)
        requireGlyphs(layout)
        var i = 0
        val n: Int = layout.runs.size
        while (i < n) {
            val run: GlyphRun = layout.runs.get(i)
            val glyphs: Array<Glyph> = run.glyphs
            val xAdvances: FloatArray = run.xAdvances
            val color: Float = run.color.toFloatBits()
            var gx: Float = x + run.x
            val gy: Float = y + run.y
            var ii = 0
            val nn = glyphs.size
            while (ii < nn) {
                val glyph: Glyph = glyphs[ii]
                gx += xAdvances[ii]
                addGlyph(glyph, gx, gy, color)
                ii++
            }
            i++
        }
        currentTint = Color.WHITE_FLOAT_BITS // Cached glyphs have changed, reset the current tint.
    }

    private fun addGlyph(glyph: Glyph, x: Float, y: Float, color: Float) {
        var x = x
        var y = y
        val scaleX: Float = font.data.scaleX
        val scaleY: Float = font.data.scaleY
        x += glyph.xoffset * scaleX
        y += glyph.yoffset * scaleY
        var width: Float = glyph.width * scaleX
        var height: Float = glyph.height * scaleY
        val u: Float = glyph.u
        val u2: Float = glyph.u2
        val v: Float = glyph.v
        val v2: Float = glyph.v2
        if (integer) {
            x = java.lang.Math.round(x).toFloat()
            y = java.lang.Math.round(y).toFloat()
            width = java.lang.Math.round(width).toFloat()
            height = java.lang.Math.round(height).toFloat()
        }
        val x2 = x + width
        val y2 = y + height
        val page: Int = glyph.page
        var idx = idx[page]
        this.idx[page] += 20
        if (pageGlyphIndices != null) pageGlyphIndices!![page].add(glyphCount++)
        val vertices = pageVertices[page]
        vertices!![idx++] = x
        vertices[idx++] = y
        vertices[idx++] = color
        vertices[idx++] = u
        vertices[idx++] = v
        vertices[idx++] = x
        vertices[idx++] = y2
        vertices[idx++] = color
        vertices[idx++] = u
        vertices[idx++] = v2
        vertices[idx++] = x2
        vertices[idx++] = y2
        vertices[idx++] = color
        vertices[idx++] = u2
        vertices[idx++] = v2
        vertices[idx++] = x2
        vertices[idx++] = y
        vertices[idx++] = color
        vertices[idx++] = u2
        vertices[idx] = v
    }

    /** Clears any cached glyphs and adds glyphs for the specified text.
     * @see .addText
     */
    fun setText(str: CharSequence, x: Float, y: Float): com.badlogic.gdx.graphics.g2d.GlyphLayout {
        clear()
        return addText(str, x, y, 0, str.length, 0f, Align.left, false)
    }

    /** Clears any cached glyphs and adds glyphs for the specified text.
     * @see .addText
     */
    fun setText(str: CharSequence, x: Float, y: Float, targetWidth: Float, halign: Int, wrap: Boolean): com.badlogic.gdx.graphics.g2d.GlyphLayout {
        clear()
        return addText(str, x, y, 0, str.length, targetWidth, halign, wrap)
    }

    /** Clears any cached glyphs and adds glyphs for the specified text.
     * @see .addText
     */
    fun setText(str: CharSequence, x: Float, y: Float, start: Int, end: Int, targetWidth: Float, halign: Int,
                wrap: Boolean): com.badlogic.gdx.graphics.g2d.GlyphLayout {
        clear()
        return addText(str, x, y, start, end, targetWidth, halign, wrap)
    }

    /** Clears any cached glyphs and adds glyphs for the specified text.
     * @see .addText
     */
    fun setText(str: CharSequence, x: Float, y: Float, start: Int, end: Int, targetWidth: Float, halign: Int,
                wrap: Boolean, truncate: String?): com.badlogic.gdx.graphics.g2d.GlyphLayout {
        clear()
        return addText(str, x, y, start, end, targetWidth, halign, wrap, truncate)
    }

    /** Clears any cached glyphs and adds the specified glyphs.
     * @see .addText
     */
    fun setText(layout: com.badlogic.gdx.graphics.g2d.GlyphLayout, x: Float, y: Float) {
        clear()
        addText(layout, x, y)
    }

    /** Adds glyphs for the specified text.
     * @see .addText
     */
    fun addText(str: CharSequence, x: Float, y: Float, targetWidth: Float, halign: Int, wrap: Boolean): com.badlogic.gdx.graphics.g2d.GlyphLayout {
        return addText(str, x, y, 0, str.length, targetWidth, halign, wrap, null)
    }
    /** Adds glyphs for the the specified text.
     * @param x The x position for the left most character.
     * @param y The y position for the top of most capital letters in the font (the [cap height][BitmapFontData.capHeight]).
     * @param start The first character of the string to draw.
     * @param end The last character of the string to draw (exclusive).
     * @param targetWidth The width of the area the text will be drawn, for wrapping or truncation.
     * @param halign Horizontal alignment of the text, see [Align].
     * @param wrap If true, the text will be wrapped within targetWidth.
     * @param truncate If not null, the text will be truncated within targetWidth with this string appended. May be an empty
     * string.
     * @return The glyph layout for the cached string (the layout's height is the distance from y to the baseline).
     */
    /** Adds glyphs for the specified text.
     * @see .addText
     */
    /** Adds glyphs for the specified text.
     * @see .addText
     */
    @JvmOverloads
    fun addText(str: CharSequence, x: Float, y: Float, start: Int = 0, end: Int = str.length, targetWidth: Float = 0f, halign: Int = Align.left,
                wrap: Boolean = false, truncate: String? = null): com.badlogic.gdx.graphics.g2d.GlyphLayout {
        val layout: com.badlogic.gdx.graphics.g2d.GlyphLayout = Pools.obtain(com.badlogic.gdx.graphics.g2d.GlyphLayout::class.java)
        pooledLayouts.add(layout)
        layout.setText(font, str, start, end, color, targetWidth, halign, wrap, truncate)
        addText(layout, x, y)
        return layout
    }

    /** Adds the specified glyphs.  */
    fun addText(layout: com.badlogic.gdx.graphics.g2d.GlyphLayout, x: Float, y: Float) {
        addToCache(layout, x, y + font.data.ascent)
    }

    fun getFont(): BitmapFont {
        return font
    }

    /** Specifies whether to use integer positions or not. Default is to use them so filtering doesn't kick in as badly.
     * @param use
     */
    fun setUseIntegerPositions(use: Boolean) {
        integer = use
    }

    /** @return whether this font uses integer positions for drawing.
     */
    fun usesIntegerPositions(): Boolean {
        return integer
    }

    val vertices: FloatArray?
        get() = getVertices(0)

    fun getVertices(page: Int): FloatArray? {
        return pageVertices[page]
    }

    fun getVertexCount(page: Int): Int {
        return idx[page]
    }

    fun getLayouts(): Array<com.badlogic.gdx.graphics.g2d.GlyphLayout> {
        return layouts
    }

    companion object {
        private val tempColor: Color = Color(1, 1, 1, 1)
    }

    /** @param integer If true, rendering positions will be at integer values to avoid filtering artifacts.
     */
    init {
        this.font = font
        this.integer = integer
        val pageCount: Int = font.regions.size
        if (pageCount == 0) throw IllegalArgumentException("The specified font must contain at least one texture page.")
        pageVertices = arrayOfNulls(pageCount)
        idx = IntArray(pageCount)
        if (pageCount > 1) {
            // Contains the indices of the glyph in the cache as they are added.
            pageGlyphIndices = arrayOfNulls(pageCount)
            var i = 0
            val n = pageGlyphIndices!!.size
            while (i < n) {
                pageGlyphIndices!![i] = IntArray()
                i++
            }
        }
        tempGlyphCount = IntArray(pageCount)
    }
}
