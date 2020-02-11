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

import com.badlogic.gdx.graphics.g2d.GlyphLayout.GlyphRun

/**
 * Stores [runs][GlyphRun] of glyphs for a piece of text. The text may contain newlines and color markup tags.
 *
 * @author Nathan Sweet
 * @author davebaol
 * @author Alexander Dorokhov
 */
class GlyphLayout : Poolable {

    val runs: Array<GlyphRun> = Array()
    var width = 0f
    var height = 0f
    private val colorStack: Array<Color> = Array(4)

    /**
     * Creates an empty GlyphLayout.
     */
    constructor() {}

    /**
     * @see .setText
     */
    constructor(font: BitmapFont, str: CharSequence) {
        setText(font, str)
    }

    /**
     * @see .setText
     */
    constructor(font: BitmapFont, str: CharSequence, color: Color?, targetWidth: Float, halign: Int, wrap: Boolean) {
        setText(font, str, color, targetWidth, halign, wrap)
    }

    /**
     * @see .setText
     */
    constructor(font: BitmapFont, str: CharSequence, start: Int, end: Int, color: Color?, targetWidth: Float, halign: Int,
                wrap: Boolean, truncate: String?) {
        setText(font, str, start, end, color, targetWidth, halign, wrap, truncate)
    }

    /**
     * Calls [setText][.setText] with the whole
     * string, the font's current color, and no alignment or wrapping.
     */
    fun setText(font: BitmapFont, str: CharSequence) {
        setText(font, str, 0, str.length, font.getColor(), 0f, Align.left, false, null)
    }

    /**
     * Calls [setText][.setText] with the whole
     * string and no truncation.
     */
    fun setText(font: BitmapFont, str: CharSequence, color: Color?, targetWidth: Float, halign: Int, wrap: Boolean) {
        setText(font, str, 0, str.length, color, targetWidth, halign, wrap, null)
    }

    /**
     * @param color       The default color to use for the text (the BitmapFont [color][BitmapFont.getColor] is not used). If
     * [BitmapFontData.markupEnabled] is true, color markup tags in the specified string may change the color for
     * portions of the text.
     * @param halign      Horizontal alignment of the text, see [Align].
     * @param targetWidth The width used for alignment, line wrapping, and truncation. May be zero if those features are not used.
     * @param truncate    If not null and the width of the glyphs exceed targetWidth, the glyphs are truncated and the glyphs for the
     * specified truncate string are placed at the end. Empty string can be used to truncate without adding glyphs.
     * Truncate should not be used with text that contains multiple lines. Wrap is ignored if truncate is not null.
     */
    fun setText(font: BitmapFont, str: CharSequence, start: Int, end: Int, color: Color?, targetWidth: Float, halign: Int,
                wrap: Boolean, truncate: String?) {
        var start = start
        var color: Color? = color
        var wrap = wrap
        val fontData: BitmapFontData = font.data
        if (truncate != null) wrap = true // Causes truncate code to run, doesn't actually cause wrapping.
        else if (targetWidth <= fontData.spaceXadvance * 3) //
            wrap = false // Avoid one line per character, which is very inefficient.
        val markupEnabled: Boolean = fontData.markupEnabled
        val glyphRunPool: Pool<GlyphRun> = Pools.get(GlyphRun::class.java)
        val runs = runs
        glyphRunPool.freeAll(runs)
        runs.clear()
        var x = 0f
        var y = 0f
        var width = 0f
        var lines = 0
        var blankLines = 0
        var lastGlyph: Glyph? = null
        val colorStack: Array<Color> = colorStack
        var nextColor: Color? = color
        colorStack.add(color)
        val colorPool: Pool<Color> = Pools.get(Color::class.java)
        var runStart = start
        outer@ while (true) {
            // Each run is delimited by newline or left square bracket.
            var runEnd = -1
            var newline = false
            if (start == end) {
                if (runStart == end) break // End of string with no run to process, we're done.
                runEnd = end // End of string, process last run.
            } else {
                when (str[start++]) {
                    '\n' -> {
                        // End of line.
                        runEnd = start - 1
                        newline = true
                    }
                    '[' ->                         // Possible color tag.
                        if (markupEnabled) {
                            val length = parseColorMarkup(str, start, end, colorPool)
                            if (length >= 0) {
                                runEnd = start - 1
                                start += length + 1
                                nextColor = colorStack.peek()
                            } else if (length == -2) {
                                start++ // Skip first of "[[" escape sequence.
                                continue@outer
                            }
                        }
                }
            }
            if (runEnd != -1) {
                runEnded@ if (runEnd != runStart) { // Eg, when a color tag is at text start or a line is "\n".
                    // Store the run that has ended.
                    var run: GlyphRun? = glyphRunPool.obtain()
                    run!!.color.set(color)
                    fontData.getGlyphs(run, str, runStart, runEnd, lastGlyph)
                    if (run.glyphs.size === 0) {
                        glyphRunPool.free(run)
                        break@runEnded
                    }
                    if (lastGlyph != null) { // Move back the width of the last glyph from the previous run.
                        x -= if (lastGlyph.fixedWidth) lastGlyph.xadvance * fontData.scaleX else (lastGlyph.width + lastGlyph.xoffset) * fontData.scaleX - fontData.padRight
                    }
                    lastGlyph = run.glyphs.peek()
                    run.x = x
                    run.y = y
                    if (newline || runEnd == end) adjustLastGlyph(fontData, run)
                    runs.add(run)
                    var xAdvances: FloatArray = run.xAdvances.items
                    var n = run.xAdvances.size
                    if (!wrap) { // No wrap or truncate.
                        var runWidth = 0f
                        for (i in 0 until n) runWidth += xAdvances[i]
                        x += runWidth
                        run.width = runWidth
                        break@runEnded
                    }

                    // Wrap or truncate.
                    x += xAdvances[0]
                    run.width = xAdvances[0]
                    if (n < 1) break@runEnded
                    x += xAdvances[1]
                    run.width += xAdvances[1]
                    var i = 2
                    while (i < n) {
                        val glyph: Glyph = run!!.glyphs[i - 1]
                        val glyphWidth: Float = (glyph.width + glyph.xoffset) * fontData.scaleX - fontData.padRight
                        if (x + glyphWidth <= targetWidth) {
                            // Glyph fits.
                            x += xAdvances[i]
                            run.width += xAdvances[i]
                            i++
                            continue
                        }
                        if (truncate != null) {
                            // Truncate.
                            truncate(fontData, run, targetWidth, truncate, i, glyphRunPool)
                            x = run.x + run.width
                            break@outer
                        }

                        // Wrap.
                        var wrapIndex: Int = fontData.getWrapIndex(run.glyphs, i)
                        if (run.x == 0f && wrapIndex == 0 // Require at least one glyph per line.
                            || wrapIndex >= run.glyphs.size) { // Wrap at least the glyph that didn't fit.
                            wrapIndex = i - 1
                        }
                        var next: GlyphRun?
                        if (wrapIndex == 0) { // Move entire run to next line.
                            next = run
                            run.width = 0f

                            // Remove leading whitespace.
                            val glyphCount = run.glyphs.size
                            while (wrapIndex < glyphCount) {
                                if (!fontData.isWhitespace(run.glyphs[wrapIndex].id as Char)) break
                                wrapIndex++
                            }
                            if (wrapIndex > 0) {
                                run.glyphs.removeRange(0, wrapIndex - 1)
                                run.xAdvances.removeRange(1, wrapIndex)
                            }
                            run.xAdvances[0] = -run.glyphs.first().xoffset * fontData.scaleX - fontData.padLeft
                            if (runs.size > 1) { // Previous run is now at the end of a line.
                                // Remove trailing whitespace and adjust last glyph.
                                val previous = runs[runs.size - 2]
                                var lastIndex = previous.glyphs.size - 1
                                while (lastIndex > 0) {
                                    val g: Glyph = previous.glyphs[lastIndex]
                                    if (!fontData.isWhitespace(g.id as Char)) break
                                    previous.width -= previous.xAdvances[lastIndex + 1]
                                    lastIndex--
                                }
                                previous.glyphs.truncate(lastIndex + 1)
                                previous.xAdvances.truncate(lastIndex + 2)
                                adjustLastGlyph(fontData, previous)
                                width = java.lang.Math.max(width, previous.x + previous.width)
                            }
                        } else {
                            next = wrap(fontData, run, glyphRunPool, wrapIndex, i)
                            width = java.lang.Math.max(width, run.x + run.width)
                            if (next == null) { // All wrapped glyphs were whitespace.
                                x = 0f
                                y += fontData.down
                                lines++
                                lastGlyph = null
                                break
                            }
                            runs.add(next)
                        }

                        // Start the loop over with the new run on the next line.
                        n = next!!.xAdvances.size
                        xAdvances = next.xAdvances.items
                        x = xAdvances[0]
                        if (n > 1) x += xAdvances[1]
                        next.width += x
                        y += fontData.down
                        lines++
                        next.x = 0f
                        next.y = y
                        i = 1
                        run = next
                        lastGlyph = null
                        i++
                    }
                }
                if (newline) {
                    // Next run will be on the next line.
                    width = java.lang.Math.max(width, x)
                    x = 0f
                    var down: Float = fontData.down
                    if (runEnd == runStart) { // Blank line.
                        down *= fontData.blankLineScale
                        blankLines++
                    } else lines++
                    y += down
                    lastGlyph = null
                }
                runStart = start
                color = nextColor
            }
        }
        width = java.lang.Math.max(width, x)
        var i = 1
        val n = colorStack.size
        while (i < n) {
            colorPool.free(colorStack[i])
            i++
        }
        colorStack.clear()

        // Align runs to center or right of targetWidth.
        if (halign and Align.left === 0) { // Not left aligned, so must be center or right aligned.
            val center = halign and Align.center !== 0
            var lineWidth = 0f
            var lineY = Int.MIN_VALUE.toFloat()
            var lineStart = 0
            val n = runs.size
            for (i in 0 until n) {
                val run = runs[i]
                if (run.y != lineY) {
                    lineY = run.y
                    var shift = targetWidth - lineWidth
                    if (center) shift /= 2f
                    while (lineStart < i) runs[lineStart++].x += shift
                    lineWidth = 0f
                }
                lineWidth = java.lang.Math.max(lineWidth, run.x + run.width)
            }
            var shift = targetWidth - lineWidth
            if (center) shift /= 2f
            while (lineStart < n) runs[lineStart++].x += shift
        }
        this.width = width
        if (fontData.flipped) height = fontData.capHeight + lines * fontData.down + blankLines * fontData.down * fontData.blankLineScale else height = fontData.capHeight + lines * -fontData.down + blankLines * -fontData.down * fontData.blankLineScale
    }

    /**
     * @param truncate May be empty string.
     */
    private fun truncate(fontData: BitmapFontData, run: GlyphRun?, targetWidth: Float, truncate: String, widthIndex: Int,
                         glyphRunPool: Pool<GlyphRun>) {

        // Determine truncate string size.
        var targetWidth = targetWidth
        val truncateRun: GlyphRun = glyphRunPool.obtain()
        fontData.getGlyphs(truncateRun, truncate, 0, truncate.length, null)
        var truncateWidth = 0f
        if (truncateRun.xAdvances.size > 0) {
            adjustLastGlyph(fontData, truncateRun)
            var i = 1
            val n = truncateRun.xAdvances.size
            while (i < n) {
                // Skip first for tight bounds.
                truncateWidth += truncateRun.xAdvances[i]
                i++
            }
        }
        targetWidth -= truncateWidth

        // Determine visible glyphs.
        var count = 0
        var width = run!!.x
        while (count < run.xAdvances.size) {
            val xAdvance = run.xAdvances[count]
            width += xAdvance
            if (width > targetWidth) {
                run.width = width - run.x - xAdvance
                break
            }
            count++
        }
        if (count > 1) {
            // Some run glyphs fit, append truncate glyphs.
            run.glyphs.truncate(count - 1)
            run.xAdvances.truncate(count)
            adjustLastGlyph(fontData, run)
            if (truncateRun.xAdvances.size > 0) run.xAdvances.addAll(truncateRun.xAdvances, 1, truncateRun.xAdvances.size - 1)
        } else {
            // No run glyphs fit, use only truncate glyphs.
            run.glyphs.clear()
            run.xAdvances.clear()
            run.xAdvances.addAll(truncateRun.xAdvances)
            if (truncateRun.xAdvances.size > 0) run.width += truncateRun.xAdvances[0]
        }
        run.glyphs.addAll(truncateRun.glyphs)
        run.width += truncateWidth
        glyphRunPool.free(truncateRun)
    }

    /**
     * Breaks a run into two runs at the specified wrapIndex.
     *
     * @return May be null if second run is all whitespace.
     */
    private fun wrap(fontData: BitmapFontData, first: GlyphRun?, glyphRunPool: Pool<GlyphRun>, wrapIndex: Int, widthIndex: Int): GlyphRun? {
        var widthIndex = widthIndex
        val glyphs2: Array<Glyph> = first!!.glyphs // Starts with all the glyphs.
        val glyphCount = first.glyphs.size
        val xAdvances2 = first.xAdvances // Starts with all the xAdvances.

        // Skip whitespace before the wrap index.
        var firstEnd = wrapIndex
        while (firstEnd > 0) {
            if (!fontData.isWhitespace(glyphs2[firstEnd - 1].id as Char)) break
            firstEnd--
        }

        // Skip whitespace after the wrap index.
        var secondStart = wrapIndex
        while (secondStart < glyphCount) {
            if (!fontData.isWhitespace(glyphs2[secondStart].id as Char)) break
            secondStart++
        }

        // Increase first run width up to the end index.
        while (widthIndex < firstEnd) first.width += xAdvances2[widthIndex++]

        // Reduce first run width by the wrapped glyphs that have contributed to the width.
        val n = firstEnd + 1
        while (widthIndex > n) {
            first.width -= xAdvances2[--widthIndex]
        }

        // Copy wrapped glyphs and xAdvances to second run.
        // The second run will contain the remaining glyph data, so swap instances rather than copying.
        var second: GlyphRun? = null
        if (secondStart < glyphCount) {
            second = glyphRunPool.obtain()
            second.color.set(first.color)
            val glyphs1: Array<Glyph> = second.glyphs // Starts empty.
            glyphs1.addAll(glyphs2, 0, firstEnd)
            glyphs2.removeRange(0, secondStart - 1)
            first.glyphs = glyphs1
            second.glyphs = glyphs2
            val xAdvances1 = second.xAdvances // Starts empty.
            xAdvances1.addAll(xAdvances2, 0, firstEnd + 1)
            xAdvances2.removeRange(1, secondStart) // Leave first entry to be overwritten by next line.
            xAdvances2[0] = -glyphs2.first().xoffset * fontData.scaleX - fontData.padLeft
            first.xAdvances = xAdvances1
            second.xAdvances = xAdvances2
        } else {
            // Second run is empty, just trim whitespace glyphs from end of first run.
            glyphs2.truncate(firstEnd)
            xAdvances2.truncate(firstEnd + 1)
        }
        if (firstEnd == 0) {
            // If the first run is now empty, remove it.
            glyphRunPool.free(first)
            runs.pop()
        } else adjustLastGlyph(fontData, first)
        return second
    }

    /**
     * Adjusts the xadvance of the last glyph to use its width instead of xadvance.
     */
    private fun adjustLastGlyph(fontData: BitmapFontData, run: GlyphRun?) {
        val last: Glyph = run!!.glyphs.peek()
        if (last.fixedWidth) return
        val width: Float = (last.width + last.xoffset) * fontData.scaleX - fontData.padRight
        run.width += width - run.xAdvances.peek() // Can cause the run width to be > targetWidth, but the problem is minimal.
        run.xAdvances[run.xAdvances.size - 1] = width
    }

    private fun parseColorMarkup(str: CharSequence, start: Int, end: Int, colorPool: Pool<Color>): Int {
        if (start == end) return -1 // String ended with "[".
        when (str[start]) {
            '#' -> {
                // Parse hex color RRGGBBAA where AA is optional and defaults to 0xFF if less than 6 chars are used.
                var colorInt = 0
                var i = start + 1
                while (i < end) {
                    val ch = str[i]
                    if (ch == ']') {
                        if (i < start + 2 || i > start + 9) break // Illegal number of hex digits.
                        if (i - start <= 7) { // RRGGBB or fewer chars.
                            var ii = 0
                            val nn = 9 - (i - start)
                            while (ii < nn) {
                                colorInt = colorInt shl 4
                                ii++
                            }
                            colorInt = colorInt or 0xff
                        }
                        val color: Color = colorPool.obtain()
                        colorStack.add(color)
                        Color.rgba8888ToColor(color, colorInt)
                        return i - start
                    }
                    colorInt = if (ch >= '0' && ch <= '9') colorInt * 16 + (ch - '0') else if (ch >= 'a' && ch <= 'f') colorInt * 16 + (ch.toInt() - ('a'.toInt() - 10)) else if (ch >= 'A' && ch <= 'F') colorInt * 16 + (ch.toInt() - ('A'.toInt() - 10)) else break // Unexpected character in hex color.
                    i++
                }
                return -1
            }
            '[' -> return -2
            ']' -> {
                if (colorStack.size > 1) colorPool.free(colorStack.pop())
                return 0
            }
        }
        // Parse named color.
        for (i in start + 1 until end) {
            val ch = str[i]
            if (ch != ']') continue
            val namedColor: Color = Colors.get(str.subSequence(start, i).toString()) ?: return -1
            // Unknown color name.
            val color: Color = colorPool.obtain()
            colorStack.add(color)
            color.set(namedColor)
            return i - start
        }
        return -1 // Unclosed color tag.
    }

    fun reset() {
        Pools.get(GlyphRun::class.java).freeAll(runs)
        runs.clear()
        width = 0f
        height = 0f
    }

    override fun toString(): String {
        if (runs.size === 0) return ""
        val buffer: java.lang.StringBuilder = java.lang.StringBuilder(128)
        buffer.append(width)
        buffer.append('x')
        buffer.append(height)
        buffer.append('\n')
        var i = 0
        val n = runs.size
        while (i < n) {
            buffer.append(runs[i].toString())
            buffer.append('\n')
            i++
        }
        buffer.setLength(buffer.length - 1)
        return buffer.toString()
    }

    /**
     * Stores glyphs and positions for a piece of text which is a single color and does not span multiple lines.
     *
     * @author Nathan Sweet
     */
    class GlyphRun : Poolable {

        var glyphs: Array<Glyph> = Array()

        /**
         * Contains glyphs.size+1 entries: First entry is X offset relative to the drawing position. Subsequent entries are the X
         * advance relative to previous glyph position. Last entry is the width of the last glyph.
         */
        var xAdvances: FloatArray = FloatArray()
        var x = 0f
        var y = 0f
        var width = 0f
        val color: Color = Color()
        fun reset() {
            glyphs.clear()
            xAdvances.clear()
            width = 0f
        }

        override fun toString(): String {
            val buffer: java.lang.StringBuilder = java.lang.StringBuilder(glyphs.size)
            val glyphs: Array<Glyph> = glyphs
            var i = 0
            val n = glyphs.size
            while (i < n) {
                val g: Glyph = glyphs[i]
                buffer.append(g.id as Char)
                i++
            }
            buffer.append(", #")
            buffer.append(color)
            buffer.append(", ")
            buffer.append(x)
            buffer.append(", ")
            buffer.append(y)
            buffer.append(", ")
            buffer.append(width)
            return buffer.toString()
        }
    }
}
