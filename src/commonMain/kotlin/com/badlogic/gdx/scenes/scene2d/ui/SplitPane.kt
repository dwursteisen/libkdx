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

import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.SplitPane.SplitPaneStyle

/**
 * A container that contains two widgets and is divided either horizontally or vertically. The user may resize the widgets. The
 * child widgets are always sized to fill their side of the SplitPane.
 *
 *
 * Minimum and maximum split amounts can be set to limit the motion of the resizing handle. The handle position is also prevented
 * from shrinking the children below their minimum sizes. If these limits over-constrain the handle, it will be locked and placed
 * at an averaged location, resulting in cropped children. The minimum child size can be ignored (allowing dynamic cropping) by
 * wrapping the child in a [Container] with a minimum size of 0 and [fill()][Container.fill] set, or by
 * overriding [.clampSplitAmount].
 *
 *
 * The preferred size of a SplitPane is that of the child widgets and the size of the [SplitPaneStyle.handle]. The widgets
 * are sized depending on the SplitPane size and the [split position][.setSplitAmount].
 *
 * @author mzechner
 * @author Nathan Sweet
 */
class SplitPane(firstWidget: Actor?, secondWidget: Actor?, vertical: Boolean, style: SplitPaneStyle?) : WidgetGroup() {

    var style: SplitPaneStyle? = null
    private var firstWidget: Actor? = null
    private var secondWidget: Actor? = null
    var vertical = false
    var splitAmount = 0.5f
    var minAmount = 0f
    var maxAmount = 1f
    private val firstWidgetBounds: Rectangle = Rectangle()
    private val secondWidgetBounds: Rectangle = Rectangle()
    var handleBounds: Rectangle = Rectangle()
    var isCursorOverHandle = false
    private val tempScissors: Rectangle = Rectangle()
    var lastPoint: Vector2 = Vector2()
    var handlePosition: Vector2 = Vector2()

    /**
     * @param firstWidget  May be null.
     * @param secondWidget May be null.
     */
    constructor(firstWidget: Actor?, secondWidget: Actor?, vertical: Boolean, skin: Skin) : this(firstWidget, secondWidget, vertical, skin, "default-" + if (vertical) "vertical" else "horizontal") {}

    /**
     * @param firstWidget  May be null.
     * @param secondWidget May be null.
     */
    constructor(firstWidget: Actor?, secondWidget: Actor?, vertical: Boolean, skin: Skin, styleName: String?) : this(firstWidget, secondWidget, vertical, skin.get(styleName, SplitPaneStyle::class.java)) {}

    private fun initialize() {
        addListener(object : InputListener() {
            var draggingPointer = -1
            fun touchDown(event: InputEvent?, x: Float, y: Float, pointer: Int, button: Int): Boolean {
                if (draggingPointer != -1) return false
                if (pointer == 0 && button != 0) return false
                if (handleBounds.contains(x, y)) {
                    draggingPointer = pointer
                    lastPoint.set(x, y)
                    handlePosition.set(handleBounds.x, handleBounds.y)
                    return true
                }
                return false
            }

            fun touchUp(event: InputEvent?, x: Float, y: Float, pointer: Int, button: Int) {
                if (pointer == draggingPointer) draggingPointer = -1
            }

            fun touchDragged(event: InputEvent?, x: Float, y: Float, pointer: Int) {
                if (pointer != draggingPointer) return
                val handle: Drawable? = style!!.handle
                if (!vertical) {
                    val delta: Float = x - lastPoint.x
                    val availWidth: Float = getWidth() - handle.getMinWidth()
                    var dragX: Float = handlePosition.x + delta
                    handlePosition.x = dragX
                    dragX = java.lang.Math.max(0f, dragX)
                    dragX = java.lang.Math.min(availWidth, dragX)
                    splitAmount = dragX / availWidth
                    lastPoint.set(x, y)
                } else {
                    val delta: Float = y - lastPoint.y
                    val availHeight: Float = getHeight() - handle.getMinHeight()
                    var dragY: Float = handlePosition.y + delta
                    handlePosition.y = dragY
                    dragY = java.lang.Math.max(0f, dragY)
                    dragY = java.lang.Math.min(availHeight, dragY)
                    splitAmount = 1 - dragY / availHeight
                    lastPoint.set(x, y)
                }
                invalidate()
            }

            fun mouseMoved(event: InputEvent?, x: Float, y: Float): Boolean {
                isCursorOverHandle = handleBounds.contains(x, y)
                return false
            }
        })
    }

    fun setStyle(style: SplitPaneStyle?) {
        this.style = style
        invalidateHierarchy()
    }

    /**
     * Returns the split pane's style. Modifying the returned style may not have an effect until [.setStyle]
     * is called.
     */
    fun getStyle(): SplitPaneStyle? {
        return style
    }

    fun layout() {
        clampSplitAmount()
        if (!vertical) calculateHorizBoundsAndPositions() else calculateVertBoundsAndPositions()
        val firstWidget: Actor? = firstWidget
        if (firstWidget != null) {
            val firstWidgetBounds: Rectangle = firstWidgetBounds
            firstWidget.setBounds(firstWidgetBounds.x, firstWidgetBounds.y, firstWidgetBounds.width, firstWidgetBounds.height)
            if (firstWidget is Layout) (firstWidget as Layout).validate()
        }
        val secondWidget: Actor? = secondWidget
        if (secondWidget != null) {
            val secondWidgetBounds: Rectangle = secondWidgetBounds
            secondWidget.setBounds(secondWidgetBounds.x, secondWidgetBounds.y, secondWidgetBounds.width, secondWidgetBounds.height)
            if (secondWidget is Layout) (secondWidget as Layout).validate()
        }
    }

    val prefWidth: Float
        get() {
            val first = (if (firstWidget == null) 0 else (if (firstWidget is Layout) (firstWidget as Layout).getPrefWidth() else firstWidget.getWidth()).toFloat()).toFloat()
            val second = (if (secondWidget == null) 0 else (if (secondWidget is Layout) (secondWidget as Layout).getPrefWidth() else secondWidget.getWidth()).toFloat()).toFloat()
            return if (vertical) java.lang.Math.max(first, second) else first + style!!.handle.getMinWidth() + second
        }

    val prefHeight: Float
        get() {
            val first = (if (firstWidget == null) 0 else (if (firstWidget is Layout) (firstWidget as Layout).getPrefHeight() else firstWidget.getHeight()).toFloat()).toFloat()
            val second = (if (secondWidget == null) 0 else (if (secondWidget is Layout) (secondWidget as Layout).getPrefHeight() else secondWidget.getHeight()).toFloat()).toFloat()
            return if (!vertical) java.lang.Math.max(first, second) else first + style!!.handle.getMinHeight() + second
        }

    val minWidth: Float
        get() {
            val first = if (firstWidget is Layout) (firstWidget as Layout?).getMinWidth() else 0.toFloat()
            val second = if (secondWidget is Layout) (secondWidget as Layout?).getMinWidth() else 0.toFloat()
            return if (vertical) java.lang.Math.max(first, second) else first + style!!.handle.getMinWidth() + second
        }

    val minHeight: Float
        get() {
            val first = if (firstWidget is Layout) (firstWidget as Layout?).getMinHeight() else 0.toFloat()
            val second = if (secondWidget is Layout) (secondWidget as Layout?).getMinHeight() else 0.toFloat()
            return if (!vertical) java.lang.Math.max(first, second) else first + style!!.handle.getMinHeight() + second
        }

    fun setVertical(vertical: Boolean) {
        if (this.vertical == vertical) return
        this.vertical = vertical
        invalidateHierarchy()
    }

    fun isVertical(): Boolean {
        return vertical
    }

    private fun calculateHorizBoundsAndPositions() {
        val handle: Drawable? = style!!.handle
        val height: Float = getHeight()
        val availWidth: Float = getWidth() - handle.getMinWidth()
        val leftAreaWidth: Float = (availWidth * splitAmount) as Int.toFloat()
        val rightAreaWidth = availWidth - leftAreaWidth
        val handleWidth: Float = handle.getMinWidth()
        firstWidgetBounds.set(0, 0, leftAreaWidth, height)
        secondWidgetBounds.set(leftAreaWidth + handleWidth, 0, rightAreaWidth, height)
        handleBounds.set(leftAreaWidth, 0, handleWidth, height)
    }

    private fun calculateVertBoundsAndPositions() {
        val handle: Drawable? = style!!.handle
        val width: Float = getWidth()
        val height: Float = getHeight()
        val availHeight: Float = height - handle.getMinHeight()
        val topAreaHeight: Float = (availHeight * splitAmount) as Int.toFloat()
        val bottomAreaHeight = availHeight - topAreaHeight
        val handleHeight: Float = handle.getMinHeight()
        firstWidgetBounds.set(0, height - topAreaHeight, width, topAreaHeight)
        secondWidgetBounds.set(0, 0, width, bottomAreaHeight)
        handleBounds.set(0, bottomAreaHeight, width, handleHeight)
    }

    fun draw(batch: Batch, parentAlpha: Float) {
        val stage: Stage = getStage() ?: return
        validate()
        val color: Color = getColor()
        val alpha: Float = color.a * parentAlpha
        applyTransform(batch, computeTransform())
        if (firstWidget != null && firstWidget.isVisible()) {
            batch.flush()
            stage.calculateScissors(firstWidgetBounds, tempScissors)
            if (ScissorStack.pushScissors(tempScissors)) {
                firstWidget.draw(batch, alpha)
                batch.flush()
                ScissorStack.popScissors()
            }
        }
        if (secondWidget != null && secondWidget.isVisible()) {
            batch.flush()
            stage.calculateScissors(secondWidgetBounds, tempScissors)
            if (ScissorStack.pushScissors(tempScissors)) {
                secondWidget.draw(batch, alpha)
                batch.flush()
                ScissorStack.popScissors()
            }
        }
        batch.setColor(color.r, color.g, color.b, alpha)
        style!!.handle.draw(batch, handleBounds.x, handleBounds.y, handleBounds.width, handleBounds.height)
        resetTransform(batch)
    }

    /**
     * @param splitAmount The split amount between the min and max amount. This parameter is clamped during
     * layout. See [.clampSplitAmount].
     */
    fun setSplitAmount(splitAmount: Float) {
        this.splitAmount = splitAmount // will be clamped during layout
        invalidate()
    }

    fun getSplitAmount(): Float {
        return splitAmount
    }

    /**
     * Called during layout to clamp the [.splitAmount] within the set limits. By default it imposes the limits of the
     * [min amount][.getMinSplitAmount], [max amount][.getMaxSplitAmount], and min sizes of the children. This
     * method is internally called in response to layout, so it should not call [.invalidate].
     */
    protected fun clampSplitAmount() {
        var effectiveMinAmount = minAmount
        var effectiveMaxAmount = maxAmount
        if (vertical) {
            val availableHeight: Float = getHeight() - style!!.handle.getMinHeight()
            if (firstWidget is Layout) effectiveMinAmount = java.lang.Math.max(effectiveMinAmount, java.lang.Math.min((firstWidget as Layout?).getMinHeight() / availableHeight, 1))
            if (secondWidget is Layout) effectiveMaxAmount = java.lang.Math.min(effectiveMaxAmount, 1 - java.lang.Math.min((secondWidget as Layout?).getMinHeight() / availableHeight, 1))
        } else {
            val availableWidth: Float = getWidth() - style!!.handle.getMinWidth()
            if (firstWidget is Layout) effectiveMinAmount = java.lang.Math.max(effectiveMinAmount, java.lang.Math.min((firstWidget as Layout?).getMinWidth() / availableWidth, 1))
            if (secondWidget is Layout) effectiveMaxAmount = java.lang.Math.min(effectiveMaxAmount, 1 - java.lang.Math.min((secondWidget as Layout?).getMinWidth() / availableWidth, 1))
        }
        splitAmount = if (effectiveMinAmount > effectiveMaxAmount) // Locked handle. Average the position.
            0.5f * (effectiveMinAmount + effectiveMaxAmount) else java.lang.Math.max(java.lang.Math.min(splitAmount, effectiveMaxAmount), effectiveMinAmount)
    }

    var minSplitAmount: Float
        get() = minAmount
        set(minAmount) {
            if (minAmount < 0 || minAmount > 1) throw GdxRuntimeException("minAmount has to be >= 0 and <= 1")
            this.minAmount = minAmount
        }

    var maxSplitAmount: Float
        get() = maxAmount
        set(maxAmount) {
            if (maxAmount < 0 || maxAmount > 1) throw GdxRuntimeException("maxAmount has to be >= 0 and <= 1")
            this.maxAmount = maxAmount
        }

    /**
     * @param widget May be null.
     */
    fun setFirstWidget(widget: Actor?) {
        if (firstWidget != null) super.removeActor(firstWidget)
        firstWidget = widget
        if (widget != null) super.addActor(widget)
        invalidate()
    }

    /**
     * @param widget May be null.
     */
    fun setSecondWidget(widget: Actor?) {
        if (secondWidget != null) super.removeActor(secondWidget)
        secondWidget = widget
        if (widget != null) super.addActor(widget)
        invalidate()
    }

    fun addActor(actor: Actor?) {
        throw UnsupportedOperationException("Use SplitPane#setWidget.")
    }

    fun addActorAt(index: Int, actor: Actor?) {
        throw UnsupportedOperationException("Use SplitPane#setWidget.")
    }

    fun addActorBefore(actorBefore: Actor?, actor: Actor?) {
        throw UnsupportedOperationException("Use SplitPane#setWidget.")
    }

    fun removeActor(actor: Actor?): Boolean {
        if (actor == null) throw java.lang.IllegalArgumentException("actor cannot be null.")
        if (actor === firstWidget) {
            setFirstWidget(null)
            return true
        }
        if (actor === secondWidget) {
            setSecondWidget(null)
            return true
        }
        return true
    }

    fun removeActor(actor: Actor?, unfocus: Boolean): Boolean {
        if (actor == null) throw java.lang.IllegalArgumentException("actor cannot be null.")
        if (actor === firstWidget) {
            super.removeActor(actor, unfocus)
            firstWidget = null
            invalidate()
            return true
        }
        if (actor === secondWidget) {
            super.removeActor(actor, unfocus)
            secondWidget = null
            invalidate()
            return true
        }
        return false
    }

    /**
     * The style for a splitpane, see [SplitPane].
     *
     * @author mzechner
     * @author Nathan Sweet
     */
    class SplitPaneStyle {

        var handle: Drawable? = null

        constructor() {}
        constructor(handle: Drawable?) {
            this.handle = handle
        }

        constructor(style: SplitPaneStyle) {
            handle = style.handle
        }
    }

    /**
     * @param firstWidget  May be null.
     * @param secondWidget May be null.
     */
    init {
        this.vertical = vertical
        setStyle(style)
        setFirstWidget(firstWidget)
        setSecondWidget(secondWidget)
        setSize(prefWidth, prefHeight)
        initialize()
    }
}
