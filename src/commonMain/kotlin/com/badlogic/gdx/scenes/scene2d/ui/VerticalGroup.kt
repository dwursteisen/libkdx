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
package com.badlogic.gdx.scenes.scene2d.ui

import com.badlogic.gdx.scenes.scene2d.ui.TooltipManager
import com.badlogic.gdx.scenes.scene2d.ui.Touchpad.TouchpadStyle
import com.badlogic.gdx.scenes.scene2d.ui.Tree.TreeStyle
import com.badlogic.gdx.scenes.scene2d.ui.Value.Fixed

/**
 * A group that lays out its children top to bottom vertically, with optional wrapping. [.getChildren] can be sorted to
 * change the order of the actors (eg [Actor.setZIndex]). This can be easier than using [Table] when actors need
 * to be inserted into or removed from the middle of the group. [.invalidate] must be called after changing the children
 * order.
 *
 *
 * The preferred width is the largest preferred width of any child. The preferred height is the sum of the children's preferred
 * heights plus spacing. The preferred size is slightly different when [wrap][.wrap] is enabled. The min size is the
 * preferred size and the max size is 0.
 *
 *
 * Widgets are sized using their [preferred height][Layout.getPrefWidth], so widgets which return 0 as their preferred
 * height will be given a height of 0.
 *
 * @author Nathan Sweet
 */
class VerticalGroup : WidgetGroup() {

    private var prefWidth = 0f
    private var prefHeight = 0f
    private var lastPrefWidth = 0f
    private var sizeInvalid = true
    private var columnSizes // column height, column width, ...
        : FloatArray? = null
    var align: Int = Align.top
        private set
    private var columnAlign = 0
    var reverse = false
        private set
    private var round = true
    var wrap = false
        private set
    var expand = false
        private set
    var space = 0f
        private set
    var wrapSpace = 0f
        private set
    var fill = 0f
        private set
    var padTop = 0f
        private set
    var padLeft = 0f
        private set
    var padBottom = 0f
        private set
    var padRight = 0f
        private set

    fun invalidate() {
        super.invalidate()
        sizeInvalid = true
    }

    private fun computeSize() {
        sizeInvalid = false
        val children: SnapshotArray<Actor> = getChildren()
        var n: Int = children.size
        prefWidth = 0f
        if (wrap) {
            prefHeight = 0f
            if (columnSizes == null) columnSizes = FloatArray() else columnSizes.clear()
            val columnSizes = columnSizes
            val space = space
            val wrapSpace = wrapSpace
            val pad = padTop + padBottom
            val groupHeight: Float = getHeight() - pad
            var x = 0f
            var y = 0f
            var columnWidth = 0f
            var i = 0
            var incr = 1
            if (reverse) {
                i = n - 1
                n = -1
                incr = -1
            }
            while (i != n) {
                val child: Actor = children.get(i)
                var width: Float
                var height: Float
                if (child is Layout) {
                    val layout: Layout = child as Layout
                    width = layout.getPrefWidth()
                    height = layout.getPrefHeight()
                    if (height > groupHeight) height = java.lang.Math.max(groupHeight, layout.getMinHeight())
                } else {
                    width = child.getWidth()
                    height = child.getHeight()
                }
                var incrY: Float = height + if (y > 0) space else 0
                if (y + incrY > groupHeight && y > 0) {
                    columnSizes.add(y)
                    columnSizes.add(columnWidth)
                    prefHeight = java.lang.Math.max(prefHeight, y + pad)
                    if (x > 0) x += wrapSpace
                    x += columnWidth
                    columnWidth = 0f
                    y = 0f
                    incrY = height
                }
                y += incrY
                columnWidth = java.lang.Math.max(columnWidth, width)
                i += incr
            }
            columnSizes.add(y)
            columnSizes.add(columnWidth)
            prefHeight = java.lang.Math.max(prefHeight, y + pad)
            if (x > 0) x += wrapSpace
            prefWidth = java.lang.Math.max(prefWidth, x + columnWidth)
        } else {
            prefHeight = padTop + padBottom + space * (n - 1)
            for (i in 0 until n) {
                val child: Actor = children.get(i)
                if (child is Layout) {
                    val layout: Layout = child as Layout
                    prefWidth = java.lang.Math.max(prefWidth, layout.getPrefWidth())
                    prefHeight += layout.getPrefHeight()
                } else {
                    prefWidth = java.lang.Math.max(prefWidth, child.getWidth())
                    prefHeight += child.getHeight()
                }
            }
        }
        prefWidth += padLeft + padRight
        if (round) {
            prefWidth = java.lang.Math.round(prefWidth).toFloat()
            prefHeight = java.lang.Math.round(prefHeight).toFloat()
        }
    }

    fun layout() {
        if (sizeInvalid) computeSize()
        if (wrap) {
            layoutWrapped()
            return
        }
        val round = round
        var align = align
        val space = space
        val padLeft = padLeft
        val fill = fill
        val columnWidth = (if (expand) getWidth() else prefWidth) - padLeft - padRight
        var y = prefHeight - padTop + space
        if (align and Align.top !== 0) y += getHeight() - prefHeight else if (align and Align.bottom === 0) // center
            y += (getHeight() - prefHeight) / 2
        val startX: Float
        if (align and Align.left !== 0) startX = padLeft else if (align and Align.right !== 0) startX = getWidth() - padRight - columnWidth else startX = padLeft + (getWidth() - padLeft - padRight - columnWidth) / 2
        align = columnAlign
        val children: SnapshotArray<Actor> = getChildren()
        var i = 0
        var n: Int = children.size
        var incr = 1
        if (reverse) {
            i = n - 1
            n = -1
            incr = -1
        }
        val r = 0
        while (i != n) {
            val child: Actor = children.get(i)
            var width: Float
            var height: Float
            var layout: Layout? = null
            if (child is Layout) {
                layout = child as Layout
                width = layout.getPrefWidth()
                height = layout.getPrefHeight()
            } else {
                width = child.getWidth()
                height = child.getHeight()
            }
            if (fill > 0) width = columnWidth * fill
            if (layout != null) {
                width = java.lang.Math.max(width, layout.getMinWidth())
                val maxWidth: Float = layout.getMaxWidth()
                if (maxWidth > 0 && width > maxWidth) width = maxWidth
            }
            var x = startX
            if (align and Align.right !== 0) x += columnWidth - width else if (align and Align.left === 0) // center
                x += (columnWidth - width) / 2
            y -= height + space
            if (round) child.setBounds(java.lang.Math.round(x), java.lang.Math.round(y), java.lang.Math.round(width), java.lang.Math.round(height)) else child.setBounds(x, y, width, height)
            if (layout != null) layout.validate()
            i += incr
        }
    }

    private fun layoutWrapped() {
        val prefWidth = getPrefWidth()
        if (prefWidth != lastPrefWidth) {
            lastPrefWidth = prefWidth
            invalidateHierarchy()
        }
        var align = align
        val round = round
        val space = space
        val padLeft = padLeft
        val fill = fill
        val wrapSpace = wrapSpace
        val maxHeight = prefHeight - padTop - padBottom
        var columnX = padLeft
        var groupHeight: Float = getHeight()
        var yStart = prefHeight - padTop + space
        var y = 0f
        var columnWidth = 0f
        if (align and Align.right !== 0) columnX += getWidth() - prefWidth else if (align and Align.left === 0) // center
            columnX += (getWidth() - prefWidth) / 2
        if (align and Align.top !== 0) yStart += groupHeight - prefHeight else if (align and Align.bottom === 0) // center
            yStart += (groupHeight - prefHeight) / 2
        groupHeight -= padTop
        align = columnAlign
        val columnSizes = columnSizes
        val children: SnapshotArray<Actor> = getChildren()
        var i = 0
        var n: Int = children.size
        var incr = 1
        if (reverse) {
            i = n - 1
            n = -1
            incr = -1
        }
        var r = 0
        while (i != n) {
            val child: Actor = children.get(i)
            var width: Float
            var height: Float
            var layout: Layout? = null
            if (child is Layout) {
                layout = child as Layout
                width = layout.getPrefWidth()
                height = layout.getPrefHeight()
                if (height > groupHeight) height = java.lang.Math.max(groupHeight, layout.getMinHeight())
            } else {
                width = child.getWidth()
                height = child.getHeight()
            }
            if (y - height - space < padBottom || r == 0) {
                y = yStart
                if (align and Align.bottom !== 0) y -= maxHeight - columnSizes!![r] else if (align and Align.top === 0) // center
                    y -= (maxHeight - columnSizes!![r]) / 2
                if (r > 0) {
                    columnX += wrapSpace
                    columnX += columnWidth
                }
                columnWidth = columnSizes!![r + 1]
                r += 2
            }
            if (fill > 0) width = columnWidth * fill
            if (layout != null) {
                width = java.lang.Math.max(width, layout.getMinWidth())
                val maxWidth: Float = layout.getMaxWidth()
                if (maxWidth > 0 && width > maxWidth) width = maxWidth
            }
            var x = columnX
            if (align and Align.right !== 0) x += columnWidth - width else if (align and Align.left === 0) // center
                x += (columnWidth - width) / 2
            y -= height + space
            if (round) child.setBounds(java.lang.Math.round(x), java.lang.Math.round(y), java.lang.Math.round(width), java.lang.Math.round(height)) else child.setBounds(x, y, width, height)
            if (layout != null) layout.validate()
            i += incr
        }
    }

    fun getPrefWidth(): Float {
        if (sizeInvalid) computeSize()
        return prefWidth
    }

    fun getPrefHeight(): Float {
        if (wrap) return 0
        if (sizeInvalid) computeSize()
        return prefHeight
    }

    /**
     * If true (the default), positions and sizes are rounded to integers.
     */
    fun setRound(round: Boolean) {
        this.round = round
    }

    /**
     * The children will be displayed last to first.
     */
    fun reverse(): VerticalGroup {
        reverse = true
        return this
    }

    /**
     * If true, the children will be displayed last to first.
     */
    fun reverse(reverse: Boolean): VerticalGroup {
        this.reverse = reverse
        return this
    }

    /**
     * Sets the vertical space between children.
     */
    fun space(space: Float): VerticalGroup {
        this.space = space
        return this
    }

    /**
     * Sets the horizontal space between columns when wrap is enabled.
     */
    fun wrapSpace(wrapSpace: Float): VerticalGroup {
        this.wrapSpace = wrapSpace
        return this
    }

    /**
     * Sets the padTop, padLeft, padBottom, and padRight to the specified value.
     */
    fun pad(pad: Float): VerticalGroup {
        padTop = pad
        padLeft = pad
        padBottom = pad
        padRight = pad
        return this
    }

    fun pad(top: Float, left: Float, bottom: Float, right: Float): VerticalGroup {
        padTop = top
        padLeft = left
        padBottom = bottom
        padRight = right
        return this
    }

    fun padTop(padTop: Float): VerticalGroup {
        this.padTop = padTop
        return this
    }

    fun padLeft(padLeft: Float): VerticalGroup {
        this.padLeft = padLeft
        return this
    }

    fun padBottom(padBottom: Float): VerticalGroup {
        this.padBottom = padBottom
        return this
    }

    fun padRight(padRight: Float): VerticalGroup {
        this.padRight = padRight
        return this
    }

    /**
     * Sets the alignment of all widgets within the vertical group. Set to [Align.center], [Align.top],
     * [Align.bottom], [Align.left], [Align.right], or any combination of those.
     */
    fun align(align: Int): VerticalGroup {
        this.align = align
        return this
    }

    /**
     * Sets the alignment of all widgets within the vertical group to [Align.center]. This clears any other alignment.
     */
    fun center(): VerticalGroup {
        align = Align.center
        return this
    }

    /**
     * Sets [Align.top] and clears [Align.bottom] for the alignment of all widgets within the vertical group.
     */
    fun top(): VerticalGroup {
        align = align or Align.top
        align = align and Align.bottom.inv()
        return this
    }

    /**
     * Adds [Align.left] and clears [Align.right] for the alignment of all widgets within the vertical group.
     */
    fun left(): VerticalGroup {
        align = align or Align.left
        align = align and Align.right.inv()
        return this
    }

    /**
     * Sets [Align.bottom] and clears [Align.top] for the alignment of all widgets within the vertical group.
     */
    fun bottom(): VerticalGroup {
        align = align or Align.bottom
        align = align and Align.top.inv()
        return this
    }

    /**
     * Adds [Align.right] and clears [Align.left] for the alignment of all widgets within the vertical group.
     */
    fun right(): VerticalGroup {
        align = align or Align.right
        align = align and Align.left.inv()
        return this
    }

    fun fill(): VerticalGroup {
        fill = 1f
        return this
    }

    /**
     * @param fill 0 will use preferred height.
     */
    fun fill(fill: Float): VerticalGroup {
        this.fill = fill
        return this
    }

    fun expand(): VerticalGroup {
        expand = true
        return this
    }

    /**
     * When true and wrap is false, the columns will take up the entire vertical group width.
     */
    fun expand(expand: Boolean): VerticalGroup {
        this.expand = expand
        return this
    }

    /**
     * Sets fill to 1 and expand to true.
     */
    fun grow(): VerticalGroup {
        expand = true
        fill = 1f
        return this
    }

    /**
     * If false, the widgets are arranged in a single column and the preferred height is the widget heights plus spacing.
     *
     *
     * If true, the widgets will wrap using the height of the vertical group. The preferred height of the group will be 0 as it is
     * expected that something external will set the height of the group. Widgets are sized to their preferred height unless it is
     * larger than the group's height, in which case they are sized to the group's height but not less than their minimum height.
     * Default is false.
     *
     *
     * When wrap is enabled, the group's preferred width depends on the height of the group. In some cases the parent of the group
     * will need to layout twice: once to set the height of the group and a second time to adjust to the group's new preferred
     * width.
     */
    fun wrap(): VerticalGroup {
        wrap = true
        return this
    }

    fun wrap(wrap: Boolean): VerticalGroup {
        this.wrap = wrap
        return this
    }

    /**
     * Sets the vertical alignment of each column of widgets when [wrapping][.wrap] is enabled and sets the horizontal
     * alignment of widgets within each column. Set to [Align.center], [Align.top], [Align.bottom],
     * [Align.left], [Align.right], or any combination of those.
     */
    fun columnAlign(columnAlign: Int): VerticalGroup {
        this.columnAlign = columnAlign
        return this
    }

    /**
     * Sets the alignment of widgets within each column to [Align.center]. This clears any other alignment.
     */
    fun columnCenter(): VerticalGroup {
        columnAlign = Align.center
        return this
    }

    /**
     * Adds [Align.top] and clears [Align.bottom] for the alignment of each column of widgets when [ wrapping][.wrap] is enabled.
     */
    fun columnTop(): VerticalGroup {
        columnAlign = columnAlign or Align.top
        columnAlign = columnAlign and Align.bottom.inv()
        return this
    }

    /**
     * Adds [Align.left] and clears [Align.right] for the alignment of widgets within each column.
     */
    fun columnLeft(): VerticalGroup {
        columnAlign = columnAlign or Align.left
        columnAlign = columnAlign and Align.right.inv()
        return this
    }

    /**
     * Adds [Align.bottom] and clears [Align.top] for the alignment of each column of widgets when [ wrapping][.wrap] is enabled.
     */
    fun columnBottom(): VerticalGroup {
        columnAlign = columnAlign or Align.bottom
        columnAlign = columnAlign and Align.top.inv()
        return this
    }

    /**
     * Adds [Align.right] and clears [Align.left] for the alignment of widgets within each column.
     */
    fun columnRight(): VerticalGroup {
        columnAlign = columnAlign or Align.right
        columnAlign = columnAlign and Align.left.inv()
        return this
    }

    protected fun drawDebugBounds(shapes: ShapeRenderer) {
        super.drawDebugBounds(shapes)
        if (!getDebug()) return
        shapes.set(ShapeType.Line)
        if (getStage() != null) shapes.setColor(getStage().getDebugColor())
        shapes.rect(getX() + padLeft, getY() + padBottom, getOriginX(), getOriginY(), getWidth() - padLeft - padRight,
            getHeight() - padBottom - padTop, getScaleX(), getScaleY(), getRotation())
    }

    init {
        setTouchable(Touchable.childrenOnly)
    }
}
