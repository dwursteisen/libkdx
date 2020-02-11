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

import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar.ProgressBarStyle
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox.SelectBoxList
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox.SelectBoxStyle
import java.lang.UnsupportedOperationException

/**
 * A group that scrolls a child widget using scrollbars and/or mouse or touch dragging.
 *
 *
 * The widget is sized to its preferred size. If the widget's preferred width or height is less than the size of this scroll pane,
 * it is set to the size of this scroll pane. Scrollbars appear when the widget is larger than the scroll pane.
 *
 *
 * The scroll pane's preferred size is that of the child widget. At this size, the child widget will not need to scroll, so the
 * scroll pane is typically sized by ignoring the preferred size in one or both directions.
 *
 * @author mzechner
 * @author Nathan Sweet
 */
class ScrollPane(widget: Actor?, style: ScrollPaneStyle?) : WidgetGroup() {

    private var style: ScrollPaneStyle? = null
    private var widget: Actor? = null
    val hScrollBounds: Rectangle? = Rectangle()
    val vScrollBounds: Rectangle? = Rectangle()
    val hKnobBounds: Rectangle? = Rectangle()
    val vKnobBounds: Rectangle? = Rectangle()
    private val widgetAreaBounds: Rectangle? = Rectangle()
    private val widgetCullingArea: Rectangle? = Rectangle()
    private var flickScrollListener: ActorGestureListener? = null
    var scrollX = false
    var scrollY = false
    var vScrollOnRight = true
    var hScrollOnBottom = true
    var amountX = 0f
    var amountY = 0f
    var visualAmountX = 0f
    var visualAmountY = 0f

    /**
     * Returns the maximum scroll value in the x direction.
     */
    var maxX = 0f

    /**
     * Returns the maximum scroll value in the y direction.
     */
    var maxY = 0f
    var touchScrollH = false
    var touchScrollV = false
    val lastPoint: Vector2? = Vector2()

    /**
     * Returns the width of the scrolled viewport.
     */
    var scrollWidth = 0f

    /**
     * Returns the height of the scrolled viewport.
     */
    var scrollHeight = 0f
    var fadeScrollBars = true
    var smoothScrolling = true
    var scrollBarTouch = true
    var fadeAlpha = 0f
    var fadeAlphaSeconds = 1f
    var fadeDelay = 0f
    var fadeDelaySeconds = 1f
    var cancelTouchFocus = true
    var flickScroll = true

    /**
     * Gets the flick scroll x velocity.
     */
    var velocityX = 0f

    /**
     * Gets the flick scroll y velocity.
     */
    var velocityY = 0f
    var flingTimer = 0f
    private var overscrollX = true
    private var overscrollY = true
    var flingTime = 1f
    var overscrollDistance = 50f
        private set
    private var overscrollSpeedMin = 30f
    private var overscrollSpeedMax = 200f
    var isForceScrollX = false
        private set
    var isForceScrollY = false
        private set
    var isScrollingDisabledX = false
    var isScrollingDisabledY = false
    private var clamp = true
    private var scrollbarsOnTop = false

    /**
     * If true, the scroll knobs are sized based on [.getMaxX] or [.getMaxY]. If false, the scroll knobs are sized
     * based on [Drawable.getMinWidth] or [Drawable.getMinHeight]. Default is true.
     */
    var variableSizeKnobs = true
    var draggingPointer = -1

    /**
     * @param widget May be null.
     */
    constructor(widget: Actor?) : this(widget, ScrollPaneStyle()) {}

    /**
     * @param widget May be null.
     */
    constructor(widget: Actor?, skin: Skin?) : this(widget, skin.get(ScrollPaneStyle::class.java)) {}

    /**
     * @param widget May be null.
     */
    constructor(widget: Actor?, skin: Skin?, styleName: String?) : this(widget, skin.get(styleName, ScrollPaneStyle::class.java)) {}

    /**
     * Shows or hides the scrollbars for when using [.setFadeScrollBars].
     */
    fun setScrollbarsVisible(visible: Boolean) {
        if (visible) {
            fadeAlpha = fadeAlphaSeconds
            fadeDelay = fadeDelaySeconds
        } else {
            fadeAlpha = 0f
            fadeDelay = 0f
        }
    }

    /**
     * Cancels the stage's touch focus for all listeners except this scroll pane's flick scroll listener. This causes any widgets
     * inside the scrollpane that have received touchDown to receive touchUp.
     *
     * @see .setCancelTouchFocus
     */
    fun cancelTouchFocus() {
        val stage: Stage = getStage()
        if (stage != null) stage.cancelTouchFocusExcept(flickScrollListener, this)
    }

    /**
     * If currently scrolling by tracking a touch down, stop scrolling.
     */
    fun cancel() {
        draggingPointer = -1
        touchScrollH = false
        touchScrollV = false
        flickScrollListener.getGestureDetector().cancel()
    }

    fun clamp() {
        if (!clamp) return
        scrollX(if (overscrollX) MathUtils.clamp(amountX, -overscrollDistance, maxX + overscrollDistance) else MathUtils.clamp(amountX, 0, maxX))
        scrollY(if (overscrollY) MathUtils.clamp(amountY, -overscrollDistance, maxY + overscrollDistance) else MathUtils.clamp(amountY, 0, maxY))
    }

    fun setStyle(style: ScrollPaneStyle?) {
        if (style == null) throw java.lang.IllegalArgumentException("style cannot be null.")
        this.style = style
        invalidateHierarchy()
    }

    /**
     * Returns the scroll pane's style. Modifying the returned style may not have an effect until
     * [.setStyle] is called.
     */
    fun getStyle(): ScrollPaneStyle? {
        return style
    }

    fun act(delta: Float) {
        super.act(delta)
        val panning: Boolean = flickScrollListener.getGestureDetector().isPanning()
        var animating = false
        if (fadeAlpha > 0 && fadeScrollBars && !panning && !touchScrollH && !touchScrollV) {
            fadeDelay -= delta
            if (fadeDelay <= 0) fadeAlpha = java.lang.Math.max(0f, fadeAlpha - delta)
            animating = true
        }
        if (flingTimer > 0) {
            setScrollbarsVisible(true)
            val alpha = flingTimer / flingTime
            amountX -= velocityX * alpha * delta
            amountY -= velocityY * alpha * delta
            clamp()

            // Stop fling if hit overscroll distance.
            if (amountX == -overscrollDistance) velocityX = 0f
            if (amountX >= maxX + overscrollDistance) velocityX = 0f
            if (amountY == -overscrollDistance) velocityY = 0f
            if (amountY >= maxY + overscrollDistance) velocityY = 0f
            flingTimer -= delta
            if (flingTimer <= 0) {
                velocityX = 0f
                velocityY = 0f
            }
            animating = true
        }
        if (smoothScrolling && flingTimer <= 0 && !panning &&  //
            // Scroll smoothly when grabbing the scrollbar if one pixel of scrollbar movement is > 10% of the scroll area.
            ((!touchScrollH || scrollX && maxX / (hScrollBounds.width - hKnobBounds.width) > scrollWidth * 0.1f) //
                && (!touchScrollV || scrollY && maxY / (vScrollBounds.height - vKnobBounds.height) > scrollHeight * 0.1f)) //
        ) {
            if (visualAmountX != amountX) {
                if (visualAmountX < amountX) visualScrollX(java.lang.Math.min(amountX, visualAmountX + java.lang.Math.max(200 * delta, (amountX - visualAmountX) * 7 * delta))) else visualScrollX(java.lang.Math.max(amountX, visualAmountX - java.lang.Math.max(200 * delta, (visualAmountX - amountX) * 7 * delta)))
                animating = true
            }
            if (visualAmountY != amountY) {
                if (visualAmountY < amountY) visualScrollY(java.lang.Math.min(amountY, visualAmountY + java.lang.Math.max(200 * delta, (amountY - visualAmountY) * 7 * delta))) else visualScrollY(java.lang.Math.max(amountY, visualAmountY - java.lang.Math.max(200 * delta, (visualAmountY - amountY) * 7 * delta)))
                animating = true
            }
        } else {
            if (visualAmountX != amountX) visualScrollX(amountX)
            if (visualAmountY != amountY) visualScrollY(amountY)
        }
        if (!panning) {
            if (overscrollX && scrollX) {
                if (amountX < 0) {
                    setScrollbarsVisible(true)
                    amountX += ((overscrollSpeedMin + (overscrollSpeedMax - overscrollSpeedMin) * -amountX / overscrollDistance)
                        * delta)
                    if (amountX > 0) scrollX(0f)
                    animating = true
                } else if (amountX > maxX) {
                    setScrollbarsVisible(true)
                    amountX -= (overscrollSpeedMin
                        + (overscrollSpeedMax - overscrollSpeedMin) * -(maxX - amountX) / overscrollDistance) * delta
                    if (amountX < maxX) scrollX(maxX)
                    animating = true
                }
            }
            if (overscrollY && scrollY) {
                if (amountY < 0) {
                    setScrollbarsVisible(true)
                    amountY += ((overscrollSpeedMin + (overscrollSpeedMax - overscrollSpeedMin) * -amountY / overscrollDistance)
                        * delta)
                    if (amountY > 0) scrollY(0f)
                    animating = true
                } else if (amountY > maxY) {
                    setScrollbarsVisible(true)
                    amountY -= (overscrollSpeedMin
                        + (overscrollSpeedMax - overscrollSpeedMin) * -(maxY - amountY) / overscrollDistance) * delta
                    if (amountY < maxY) scrollY(maxY)
                    animating = true
                }
            }
        }
        if (animating) {
            val stage: Stage = getStage()
            if (stage != null && stage.getActionsRequestRendering()) Gdx.graphics.requestRendering()
        }
    }

    fun layout() {
        val bg: Drawable? = style!!.background
        val hScrollKnob: Drawable? = style!!.hScrollKnob
        val vScrollKnob: Drawable? = style!!.vScrollKnob
        var bgLeftWidth = 0f
        var bgRightWidth = 0f
        var bgTopHeight = 0f
        var bgBottomHeight = 0f
        if (bg != null) {
            bgLeftWidth = bg.getLeftWidth()
            bgRightWidth = bg.getRightWidth()
            bgTopHeight = bg.getTopHeight()
            bgBottomHeight = bg.getBottomHeight()
        }
        val width: Float = getWidth()
        val height: Float = getHeight()
        var scrollbarHeight = 0f
        if (hScrollKnob != null) scrollbarHeight = hScrollKnob.getMinHeight()
        if (style!!.hScroll != null) scrollbarHeight = java.lang.Math.max(scrollbarHeight, style!!.hScroll.getMinHeight())
        var scrollbarWidth = 0f
        if (vScrollKnob != null) scrollbarWidth = vScrollKnob.getMinWidth()
        if (style!!.vScroll != null) scrollbarWidth = java.lang.Math.max(scrollbarWidth, style!!.vScroll.getMinWidth())

        // Get available space size by subtracting background's padded area.
        scrollWidth = width - bgLeftWidth - bgRightWidth
        scrollHeight = height - bgTopHeight - bgBottomHeight
        if (widget == null) return

        // Get widget's desired width.
        var widgetWidth: Float
        var widgetHeight: Float
        if (widget is Layout) {
            val layout: Layout? = widget as Layout?
            widgetWidth = layout.getPrefWidth()
            widgetHeight = layout.getPrefHeight()
        } else {
            widgetWidth = widget.getWidth()
            widgetHeight = widget.getHeight()
        }

        // Determine if horizontal/vertical scrollbars are needed.
        scrollX = isForceScrollX || widgetWidth > scrollWidth && !isScrollingDisabledX
        scrollY = isForceScrollY || widgetHeight > scrollHeight && !isScrollingDisabledY
        val fade = fadeScrollBars
        if (!fade) {
            // Check again, now taking into account the area that's taken up by any enabled scrollbars.
            if (scrollY) {
                scrollWidth -= scrollbarWidth
                if (!scrollX && widgetWidth > scrollWidth && !isScrollingDisabledX) scrollX = true
            }
            if (scrollX) {
                scrollHeight -= scrollbarHeight
                if (!scrollY && widgetHeight > scrollHeight && !isScrollingDisabledY) {
                    scrollY = true
                    scrollWidth -= scrollbarWidth
                }
            }
        }

        // The bounds of the scrollable area for the widget.
        widgetAreaBounds.set(bgLeftWidth, bgBottomHeight, scrollWidth, scrollHeight)
        if (fade) {
            // Make sure widget is drawn under fading scrollbars.
            if (scrollX && scrollY) {
                scrollHeight -= scrollbarHeight
                scrollWidth -= scrollbarWidth
            }
        } else {
            if (scrollbarsOnTop) {
                // Make sure widget is drawn under non-fading scrollbars.
                if (scrollX) widgetAreaBounds.height += scrollbarHeight
                if (scrollY) widgetAreaBounds.width += scrollbarWidth
            } else {
                // Offset widget area y for horizontal scrollbar at bottom.
                if (scrollX && hScrollOnBottom) widgetAreaBounds.y += scrollbarHeight
                // Offset widget area x for vertical scrollbar at left.
                if (scrollY && !vScrollOnRight) widgetAreaBounds.x += scrollbarWidth
            }
        }

        // If the widget is smaller than the available space, make it take up the available space.
        widgetWidth = if (isScrollingDisabledX) scrollWidth else java.lang.Math.max(scrollWidth, widgetWidth)
        widgetHeight = if (isScrollingDisabledY) scrollHeight else java.lang.Math.max(scrollHeight, widgetHeight)
        maxX = widgetWidth - scrollWidth
        maxY = widgetHeight - scrollHeight
        if (fade) {
            // Make sure widget is drawn under fading scrollbars.
            if (scrollX && scrollY) {
                maxY -= scrollbarHeight
                maxX -= scrollbarWidth
            }
        }
        scrollX(MathUtils.clamp(amountX, 0, maxX))
        scrollY(MathUtils.clamp(amountY, 0, maxY))

        // Set the bounds and scroll knob sizes if scrollbars are needed.
        if (scrollX) {
            if (hScrollKnob != null) {
                val hScrollHeight: Float = if (style!!.hScroll != null) style!!.hScroll.getMinHeight() else hScrollKnob.getMinHeight()
                // The corner gap where the two scroll bars intersect might have to flip from right to left.
                val boundsX = if (vScrollOnRight) bgLeftWidth else bgLeftWidth + scrollbarWidth
                // Scrollbar on the top or bottom.
                val boundsY = if (hScrollOnBottom) bgBottomHeight else height - bgTopHeight - hScrollHeight
                hScrollBounds.set(boundsX, boundsY, scrollWidth, hScrollHeight)
                if (variableSizeKnobs) hKnobBounds.width = java.lang.Math.max(hScrollKnob.getMinWidth(), (hScrollBounds.width * scrollWidth / widgetWidth) as Int) else hKnobBounds.width = hScrollKnob.getMinWidth()
                if (hKnobBounds.width > widgetWidth) hKnobBounds.width = 0
                hKnobBounds.height = hScrollKnob.getMinHeight()
                hKnobBounds.x = hScrollBounds.x + ((hScrollBounds.width - hKnobBounds.width) * scrollPercentX) as Int
                hKnobBounds.y = hScrollBounds.y
            } else {
                hScrollBounds.set(0, 0, 0, 0)
                hKnobBounds.set(0, 0, 0, 0)
            }
        }
        if (scrollY) {
            if (vScrollKnob != null) {
                val vScrollWidth: Float = if (style!!.vScroll != null) style!!.vScroll.getMinWidth() else vScrollKnob.getMinWidth()
                // the small gap where the two scroll bars intersect might have to flip from bottom to top
                val boundsX: Float
                val boundsY: Float
                boundsY = if (hScrollOnBottom) {
                    height - bgTopHeight - scrollHeight
                } else {
                    bgBottomHeight
                }
                // bar on the left or right
                boundsX = if (vScrollOnRight) {
                    width - bgRightWidth - vScrollWidth
                } else {
                    bgLeftWidth
                }
                vScrollBounds.set(boundsX, boundsY, vScrollWidth, scrollHeight)
                vKnobBounds.width = vScrollKnob.getMinWidth()
                if (variableSizeKnobs) vKnobBounds.height = java.lang.Math.max(vScrollKnob.getMinHeight(), (vScrollBounds.height * scrollHeight / widgetHeight) as Int) else vKnobBounds.height = vScrollKnob.getMinHeight()
                if (vKnobBounds.height > widgetHeight) vKnobBounds.height = 0
                if (vScrollOnRight) {
                    vKnobBounds.x = width - bgRightWidth - vScrollKnob.getMinWidth()
                } else {
                    vKnobBounds.x = bgLeftWidth
                }
                vKnobBounds.y = vScrollBounds.y + ((vScrollBounds.height - vKnobBounds.height) * (1 - scrollPercentY)) as Int
            } else {
                vScrollBounds.set(0, 0, 0, 0)
                vKnobBounds.set(0, 0, 0, 0)
            }
        }
        updateWidgetPosition()
        if (widget is Layout) {
            widget.setSize(widgetWidth, widgetHeight)
            (widget as Layout?).validate()
        }
    }

    private fun updateWidgetPosition() {
        // Calculate the widget's position depending on the scroll state and available widget area.
        var y: Float = widgetAreaBounds.y
        if (!scrollY) y -= maxY as Int.toFloat() else y -= (maxY-visualAmountY) as Int.toFloat()
        var x: Float = widgetAreaBounds.x
        if (scrollX) x -= visualAmountX as Int.toFloat()
        if (!fadeScrollBars && scrollbarsOnTop) {
            if (scrollX && hScrollOnBottom) {
                var scrollbarHeight = 0f
                if (style!!.hScrollKnob != null) scrollbarHeight = style!!.hScrollKnob.getMinHeight()
                if (style!!.hScroll != null) scrollbarHeight = java.lang.Math.max(scrollbarHeight, style!!.hScroll.getMinHeight())
                y += scrollbarHeight
            }
            if (scrollY && !vScrollOnRight) {
                var scrollbarWidth = 0f
                if (style!!.hScrollKnob != null) scrollbarWidth = style!!.hScrollKnob.getMinWidth()
                if (style!!.hScroll != null) scrollbarWidth = java.lang.Math.max(scrollbarWidth, style!!.hScroll.getMinWidth())
                x += scrollbarWidth
            }
        }
        widget.setPosition(x, y)
        if (widget is Cullable) {
            widgetCullingArea.x = widgetAreaBounds.x - x
            widgetCullingArea.y = widgetAreaBounds.y - y
            widgetCullingArea.width = widgetAreaBounds.width
            widgetCullingArea.height = widgetAreaBounds.height
            (widget as Cullable?).setCullingArea(widgetCullingArea)
        }
    }

    fun draw(batch: Batch?, parentAlpha: Float) {
        if (widget == null) return
        validate()

        // Setup transform for this group.
        applyTransform(batch, computeTransform())
        if (scrollX) hKnobBounds.x = hScrollBounds.x + ((hScrollBounds.width - hKnobBounds.width) * visualScrollPercentX) as Int
        if (scrollY) vKnobBounds.y = vScrollBounds.y + ((vScrollBounds.height - vKnobBounds.height) * (1 - visualScrollPercentY)) as Int
        updateWidgetPosition()

        // Draw the background ninepatch.
        val color: Color = getColor()
        batch.setColor(color.r, color.g, color.b, color.a * parentAlpha)
        if (style!!.background != null) style!!.background.draw(batch, 0, 0, getWidth(), getHeight())
        batch.flush()
        if (clipBegin(widgetAreaBounds.x, widgetAreaBounds.y, widgetAreaBounds.width, widgetAreaBounds.height)) {
            drawChildren(batch, parentAlpha)
            batch.flush()
            clipEnd()
        }

        // Render scrollbars and knobs on top if they will be visible
        var alpha: Float = color.a * parentAlpha
        if (fadeScrollBars) alpha *= Interpolation.fade.apply(fadeAlpha / fadeAlphaSeconds)
        batch.setColor(color.r, color.g, color.b, color.a * parentAlpha)
        drawScrollBars(batch, color.r, color.g, color.b, alpha)
        resetTransform(batch)
    }

    /**
     * Renders the scrollbars after the children have been drawn. If the scrollbars faded out, a is zero and rendering can be
     * skipped.
     */
    protected fun drawScrollBars(batch: Batch?, r: Float, g: Float, b: Float, a: Float) {
        if (a <= 0) return
        batch.setColor(r, g, b, a)
        val x = scrollX && hKnobBounds.width > 0
        val y = scrollY && vKnobBounds.height > 0
        if (x && y) {
            if (style!!.corner != null) {
                style!!.corner.draw(batch, hScrollBounds.x + hScrollBounds.width, hScrollBounds.y, vScrollBounds.width,
                    vScrollBounds.y)
            }
        }
        if (x) {
            if (style!!.hScroll != null) style!!.hScroll.draw(batch, hScrollBounds.x, hScrollBounds.y, hScrollBounds.width, hScrollBounds.height)
            if (style!!.hScrollKnob != null) style!!.hScrollKnob.draw(batch, hKnobBounds.x, hKnobBounds.y, hKnobBounds.width, hKnobBounds.height)
        }
        if (y) {
            if (style!!.vScroll != null) style!!.vScroll.draw(batch, vScrollBounds.x, vScrollBounds.y, vScrollBounds.width, vScrollBounds.height)
            if (style!!.vScrollKnob != null) style!!.vScrollKnob.draw(batch, vKnobBounds.x, vKnobBounds.y, vKnobBounds.width, vKnobBounds.height)
        }
    }

    /**
     * Generate fling gesture.
     *
     * @param flingTime Time in seconds for which you want to fling last.
     * @param velocityX Velocity for horizontal direction.
     * @param velocityY Velocity for vertical direction.
     */
    fun fling(flingTime: Float, velocityX: Float, velocityY: Float) {
        flingTimer = flingTime
        this.velocityX = velocityX
        this.velocityY = velocityY
    }

    //
    val prefWidth: Float
        get() {
            var width = 0f
            if (widget is Layout) width = (widget as Layout?).getPrefWidth() else if (widget != null) //
                width = widget.getWidth()
            val background: Drawable? = style!!.background
            if (background != null) width = java.lang.Math.max(width + background.getLeftWidth() + background.getRightWidth(), background.getMinWidth())
            if (scrollY) {
                var scrollbarWidth = 0f
                if (style!!.vScrollKnob != null) scrollbarWidth = style!!.vScrollKnob.getMinWidth()
                if (style!!.vScroll != null) scrollbarWidth = java.lang.Math.max(scrollbarWidth, style!!.vScroll.getMinWidth())
                width += scrollbarWidth
            }
            return width
        }

    //
    val prefHeight: Float
        get() {
            var height = 0f
            if (widget is Layout) height = (widget as Layout?).getPrefHeight() else if (widget != null) //
                height = widget.getHeight()
            val background: Drawable? = style!!.background
            if (background != null) height = java.lang.Math.max(height + background.getTopHeight() + background.getBottomHeight(), background.getMinHeight())
            if (scrollX) {
                var scrollbarHeight = 0f
                if (style!!.hScrollKnob != null) scrollbarHeight = style!!.hScrollKnob.getMinHeight()
                if (style!!.hScroll != null) scrollbarHeight = java.lang.Math.max(scrollbarHeight, style!!.hScroll.getMinHeight())
                height += scrollbarHeight
            }
            return height
        }

    val minWidth: Float
        get() = 0

    val minHeight: Float
        get() = 0

    /**
     * Returns the actor embedded in this scroll pane, or null.
     */
    /**
     * Sets the [Actor] embedded in this scroll pane.
     *
     * @param actor May be null to remove any current actor.
     */
    var actor: Actor?
        get() = widget
        set(actor) {
            if (widget === this) throw java.lang.IllegalArgumentException("widget cannot be the ScrollPane.")
            if (widget != null) super.removeActor(widget)
            widget = actor
            if (widget != null) super.addActor(widget)
        }

    @Deprecated("Use {@link #setActor(Actor)}.")
    fun setWidget(actor: Actor?) {
        actor = actor
    }

    @Deprecated("Use {@link #getActor()}.")
    fun getWidget(): Actor? {
        return widget
    }

    /**
     * @see .setWidget
     */
    @Deprecated("ScrollPane may have only a single child.")
    fun addActor(actor: Actor?) {
        throw UnsupportedOperationException("Use ScrollPane#setWidget.")
    }

    /**
     * @see .setWidget
     */
    @Deprecated("ScrollPane may have only a single child.")
    fun addActorAt(index: Int, actor: Actor?) {
        throw UnsupportedOperationException("Use ScrollPane#setWidget.")
    }

    /**
     * @see .setWidget
     */
    @Deprecated("ScrollPane may have only a single child.")
    fun addActorBefore(actorBefore: Actor?, actor: Actor?) {
        throw UnsupportedOperationException("Use ScrollPane#setWidget.")
    }

    /**
     * @see .setWidget
     */
    @Deprecated("ScrollPane may have only a single child.")
    fun addActorAfter(actorAfter: Actor?, actor: Actor?) {
        throw UnsupportedOperationException("Use ScrollPane#setWidget.")
    }

    fun removeActor(actor: Actor?): Boolean {
        if (actor == null) throw java.lang.IllegalArgumentException("actor cannot be null.")
        if (actor !== widget) return false
        actor = null
        return true
    }

    fun removeActor(actor: Actor?, unfocus: Boolean): Boolean {
        if (actor == null) throw java.lang.IllegalArgumentException("actor cannot be null.")
        if (actor !== widget) return false
        widget = null
        return super.removeActor(actor, unfocus)
    }

    fun hit(x: Float, y: Float, touchable: Boolean): Actor? {
        if (x < 0 || x >= getWidth() || y < 0 || y >= getHeight()) return null
        if (touchable && getTouchable() === Touchable.enabled && isVisible()) {
            if (scrollX && touchScrollH && hScrollBounds.contains(x, y)) return this
            if (scrollY && touchScrollV && vScrollBounds.contains(x, y)) return this
        }
        return super.hit(x, y, touchable)
    }

    /**
     * Called whenever the x scroll amount is changed.
     */
    protected fun scrollX(pixelsX: Float) {
        amountX = pixelsX
    }

    /**
     * Called whenever the y scroll amount is changed.
     */
    protected fun scrollY(pixelsY: Float) {
        amountY = pixelsY
    }

    /**
     * Called whenever the visual x scroll amount is changed.
     */
    protected fun visualScrollX(pixelsX: Float) {
        visualAmountX = pixelsX
    }

    /**
     * Called whenever the visual y scroll amount is changed.
     */
    protected fun visualScrollY(pixelsY: Float) {
        visualAmountY = pixelsY
    }

    /**
     * Returns the amount to scroll horizontally when the mouse wheel is scrolled.
     */
    protected val mouseWheelX: Float
        protected get() = java.lang.Math.min(scrollWidth, java.lang.Math.max(scrollWidth * 0.9f, maxX * 0.1f) / 4)

    /**
     * Returns the amount to scroll vertically when the mouse wheel is scrolled.
     */
    protected val mouseWheelY: Float
        protected get() = java.lang.Math.min(scrollHeight, java.lang.Math.max(scrollHeight * 0.9f, maxY * 0.1f) / 4)

    fun setScrollX(pixels: Float) {
        scrollX(MathUtils.clamp(pixels, 0, maxX))
    }

    /**
     * Returns the x scroll position in pixels, where 0 is the left of the scroll pane.
     */
    fun getScrollX(): Float {
        return amountX
    }

    fun setScrollY(pixels: Float) {
        scrollY(MathUtils.clamp(pixels, 0, maxY))
    }

    /**
     * Returns the y scroll position in pixels, where 0 is the top of the scroll pane.
     */
    fun getScrollY(): Float {
        return amountY
    }

    /**
     * Sets the visual scroll amount equal to the scroll amount. This can be used when setting the scroll amount without
     * animating.
     */
    fun updateVisualScroll() {
        visualAmountX = amountX
        visualAmountY = amountY
    }

    val visualScrollX: Float
        get() = if (!scrollX) 0 else visualAmountX

    val visualScrollY: Float
        get() = if (!scrollY) 0 else visualAmountY

    val visualScrollPercentX: Float
        get() = if (maxX == 0f) 0 else MathUtils.clamp(visualAmountX / maxX, 0, 1)

    val visualScrollPercentY: Float
        get() = if (maxY == 0f) 0 else MathUtils.clamp(visualAmountY / maxY, 0, 1)

    var scrollPercentX: Float
        get() = if (maxX == 0f) 0 else MathUtils.clamp(amountX / maxX, 0, 1)
        set(percentX) {
            scrollX(maxX * MathUtils.clamp(percentX, 0, 1))
        }

    var scrollPercentY: Float
        get() = if (maxY == 0f) 0 else MathUtils.clamp(amountY / maxY, 0, 1)
        set(percentY) {
            scrollY(maxY * MathUtils.clamp(percentY, 0, 1))
        }

    fun setFlickScroll(flickScroll: Boolean) {
        if (this.flickScroll == flickScroll) return
        this.flickScroll = flickScroll
        if (flickScroll) addListener(flickScrollListener) else removeListener(flickScrollListener)
        invalidate()
    }

    fun setFlickScrollTapSquareSize(halfTapSquareSize: Float) {
        flickScrollListener.getGestureDetector().setTapSquareSize(halfTapSquareSize)
    }
    /**
     * Sets the scroll offset so the specified rectangle is fully in view, and optionally centered vertically and/or horizontally,
     * if possible. Coordinates are in the scroll pane widget's coordinate system.
     */
    /**
     * Sets the scroll offset so the specified rectangle is fully in view, if possible. Coordinates are in the scroll pane
     * widget's coordinate system.
     */
    @JvmOverloads
    fun scrollTo(x: Float, y: Float, width: Float, height: Float, centerHorizontal: Boolean = false, centerVertical: Boolean = false) {
        validate()
        var amountX = amountX
        if (centerHorizontal) {
            amountX = x - scrollWidth / 2 + width / 2
        } else {
            if (x + width > amountX + scrollWidth) amountX = x + width - scrollWidth
            if (x < amountX) amountX = x
        }
        scrollX(MathUtils.clamp(amountX, 0, maxX))
        var amountY = amountY
        if (centerVertical) {
            amountY = maxY - y + scrollHeight / 2 - height / 2
        } else {
            if (amountY > maxY - y - height + scrollHeight) amountY = maxY - y - height + scrollHeight
            if (amountY < maxY - y) amountY = maxY - y
        }
        scrollY(MathUtils.clamp(amountY, 0, maxY))
    }

    val scrollBarHeight: Float
        get() {
            if (!scrollX) return 0
            var height = 0f
            if (style!!.hScrollKnob != null) height = style!!.hScrollKnob.getMinHeight()
            if (style!!.hScroll != null) height = java.lang.Math.max(height, style!!.hScroll.getMinHeight())
            return height
        }

    val scrollBarWidth: Float
        get() {
            if (!scrollY) return 0
            var width = 0f
            if (style!!.vScrollKnob != null) width = style!!.vScrollKnob.getMinWidth()
            if (style!!.vScroll != null) width = java.lang.Math.max(width, style!!.vScroll.getMinWidth())
            return width
        }

    /**
     * Returns true if the widget is larger than the scroll pane horizontally.
     */
    fun isScrollX(): Boolean {
        return scrollX
    }

    /**
     * Returns true if the widget is larger than the scroll pane vertically.
     */
    fun isScrollY(): Boolean {
        return scrollY
    }

    /**
     * Disables scrolling in a direction. The widget will be sized to the FlickScrollPane in the disabled direction.
     */
    fun setScrollingDisabled(x: Boolean, y: Boolean) {
        isScrollingDisabledX = x
        isScrollingDisabledY = y
        invalidate()
    }

    val isLeftEdge: Boolean
        get() = !scrollX || amountX <= 0

    val isRightEdge: Boolean
        get() = !scrollX || amountX >= maxX

    val isTopEdge: Boolean
        get() = !scrollY || amountY <= 0

    val isBottomEdge: Boolean
        get() = !scrollY || amountY >= maxY

    val isDragging: Boolean
        get() = draggingPointer != -1

    val isPanning: Boolean
        get() = flickScrollListener.getGestureDetector().isPanning()

    val isFlinging: Boolean
        get() = flingTimer > 0

    /**
     * For flick scroll, if true the widget can be scrolled slightly past its bounds and will animate back to its bounds when
     * scrolling is stopped. Default is true.
     */
    fun setOverscroll(overscrollX: Boolean, overscrollY: Boolean) {
        this.overscrollX = overscrollX
        this.overscrollY = overscrollY
    }

    /**
     * For flick scroll, sets the overscroll distance in pixels and the speed it returns to the widget's bounds in seconds.
     * Default is 50, 30, 200.
     */
    fun setupOverscroll(distance: Float, speedMin: Float, speedMax: Float) {
        overscrollDistance = distance
        overscrollSpeedMin = speedMin
        overscrollSpeedMax = speedMax
    }

    /**
     * Forces enabling scrollbars (for non-flick scroll) and overscrolling (for flick scroll) in a direction, even if the contents
     * do not exceed the bounds in that direction.
     */
    fun setForceScroll(x: Boolean, y: Boolean) {
        isForceScrollX = x
        isForceScrollY = y
    }

    /**
     * For flick scroll, sets the amount of time in seconds that a fling will continue to scroll. Default is 1.
     */
    fun setFlingTime(flingTime: Float) {
        this.flingTime = flingTime
    }

    /**
     * For flick scroll, prevents scrolling out of the widget's bounds. Default is true.
     */
    fun setClamp(clamp: Boolean) {
        this.clamp = clamp
    }

    /**
     * Set the position of the vertical and horizontal scroll bars.
     */
    fun setScrollBarPositions(bottom: Boolean, right: Boolean) {
        hScrollOnBottom = bottom
        vScrollOnRight = right
    }

    /**
     * When true the scrollbars don't reduce the scrollable size and fade out after some time of not being used.
     */
    fun setFadeScrollBars(fadeScrollBars: Boolean) {
        if (this.fadeScrollBars == fadeScrollBars) return
        this.fadeScrollBars = fadeScrollBars
        if (!fadeScrollBars) fadeAlpha = fadeAlphaSeconds
        invalidate()
    }

    fun setupFadeScrollBars(fadeAlphaSeconds: Float, fadeDelaySeconds: Float) {
        this.fadeAlphaSeconds = fadeAlphaSeconds
        this.fadeDelaySeconds = fadeDelaySeconds
    }

    fun getFadeScrollBars(): Boolean {
        return fadeScrollBars
    }

    /**
     * When false, the scroll bars don't respond to touch or mouse events. Default is true.
     */
    fun setScrollBarTouch(scrollBarTouch: Boolean) {
        this.scrollBarTouch = scrollBarTouch
    }

    fun setSmoothScrolling(smoothScrolling: Boolean) {
        this.smoothScrolling = smoothScrolling
    }

    /**
     * When false (the default), the widget is clipped so it is not drawn under the scrollbars. When true, the widget is clipped
     * to the entire scroll pane bounds and the scrollbars are drawn on top of the widget. If [.setFadeScrollBars]
     * is true, the scroll bars are always drawn on top.
     */
    fun setScrollbarsOnTop(scrollbarsOnTop: Boolean) {
        this.scrollbarsOnTop = scrollbarsOnTop
        invalidate()
    }

    /**
     * When true (default) and flick scrolling begins, [.cancelTouchFocus] is called. This causes any widgets inside the
     * scrollpane that have received touchDown to receive touchUp when flick scrolling begins.
     */
    fun setCancelTouchFocus(cancelTouchFocus: Boolean) {
        this.cancelTouchFocus = cancelTouchFocus
    }

    fun drawDebug(shapes: ShapeRenderer?) {
        drawDebugBounds(shapes)
        applyTransform(shapes, computeTransform())
        if (clipBegin(widgetAreaBounds.x, widgetAreaBounds.y, widgetAreaBounds.width, widgetAreaBounds.height)) {
            drawDebugChildren(shapes)
            shapes.flush()
            clipEnd()
        }
        resetTransform(shapes)
    }

    /**
     * The style for a scroll pane, see [ScrollPane].
     *
     * @author mzechner
     * @author Nathan Sweet
     */
    class ScrollPaneStyle {

        /**
         * Optional.
         */
        var background: Drawable? = null
        var corner: Drawable? = null

        /**
         * Optional.
         */
        var hScroll: Drawable? = null
        var hScrollKnob: Drawable? = null

        /**
         * Optional.
         */
        var vScroll: Drawable? = null
        var vScrollKnob: Drawable? = null

        constructor() {}
        constructor(background: Drawable?, hScroll: Drawable?, hScrollKnob: Drawable?, vScroll: Drawable?,
                    vScrollKnob: Drawable?) {
            this.background = background
            this.hScroll = hScroll
            this.hScrollKnob = hScrollKnob
            this.vScroll = vScroll
            this.vScrollKnob = vScrollKnob
        }

        constructor(style: ScrollPaneStyle?) {
            background = style!!.background
            corner = style.corner
            hScroll = style.hScroll
            hScrollKnob = style.hScrollKnob
            vScroll = style.vScroll
            vScrollKnob = style.vScrollKnob
        }
    }

    /**
     * @param widget May be null.
     */
    init {
        if (style == null) throw java.lang.IllegalArgumentException("style cannot be null.")
        this.style = style
        actor = widget
        setSize(150, 150)
        addCaptureListener(object : InputListener() {
            private var handlePosition = 0f
            fun touchDown(event: InputEvent?, x: Float, y: Float, pointer: Int, button: Int): Boolean {
                if (draggingPointer != -1) return false
                if (pointer == 0 && button != 0) return false
                if (getStage() != null) getStage().setScrollFocus(this@ScrollPane)
                if (!flickScroll) setScrollbarsVisible(true)
                if (fadeAlpha == 0f) return false
                if (scrollBarTouch && scrollX && hScrollBounds.contains(x, y)) {
                    event.stop()
                    setScrollbarsVisible(true)
                    if (hKnobBounds.contains(x, y)) {
                        lastPoint.set(x, y)
                        handlePosition = hKnobBounds.x
                        touchScrollH = true
                        draggingPointer = pointer
                        return true
                    }
                    setScrollX(amountX + scrollWidth * if (x < hKnobBounds.x) -1 else 1)
                    return true
                }
                if (scrollBarTouch && scrollY && vScrollBounds.contains(x, y)) {
                    event.stop()
                    setScrollbarsVisible(true)
                    if (vKnobBounds.contains(x, y)) {
                        lastPoint.set(x, y)
                        handlePosition = vKnobBounds.y
                        touchScrollV = true
                        draggingPointer = pointer
                        return true
                    }
                    setScrollY(amountY + scrollHeight * if (y < vKnobBounds.y) 1 else -1)
                    return true
                }
                return false
            }

            fun touchUp(event: InputEvent?, x: Float, y: Float, pointer: Int, button: Int) {
                if (pointer != draggingPointer) return
                cancel()
            }

            fun touchDragged(event: InputEvent?, x: Float, y: Float, pointer: Int) {
                if (pointer != draggingPointer) return
                if (touchScrollH) {
                    val delta: Float = x - lastPoint.x
                    var scrollH = handlePosition + delta
                    handlePosition = scrollH
                    scrollH = java.lang.Math.max(hScrollBounds.x, scrollH)
                    scrollH = java.lang.Math.min(hScrollBounds.x + hScrollBounds.width - hKnobBounds.width, scrollH)
                    val total: Float = hScrollBounds.width - hKnobBounds.width
                    if (total != 0f) scrollPercentX = (scrollH - hScrollBounds.x) / total
                    lastPoint.set(x, y)
                } else if (touchScrollV) {
                    val delta: Float = y - lastPoint.y
                    var scrollV = handlePosition + delta
                    handlePosition = scrollV
                    scrollV = java.lang.Math.max(vScrollBounds.y, scrollV)
                    scrollV = java.lang.Math.min(vScrollBounds.y + vScrollBounds.height - vKnobBounds.height, scrollV)
                    val total: Float = vScrollBounds.height - vKnobBounds.height
                    if (total != 0f) scrollPercentY = 1 - (scrollV - vScrollBounds.y) / total
                    lastPoint.set(x, y)
                }
            }

            fun mouseMoved(event: InputEvent?, x: Float, y: Float): Boolean {
                if (!flickScroll) setScrollbarsVisible(true)
                return false
            }
        })
        flickScrollListener = object : ActorGestureListener() {
            fun pan(event: InputEvent?, x: Float, y: Float, deltaX: Float, deltaY: Float) {
                setScrollbarsVisible(true)
                amountX -= deltaX
                amountY += deltaY
                clamp()
                if (cancelTouchFocus && (scrollX && deltaX != 0f || scrollY && deltaY != 0f)) cancelTouchFocus()
            }

            fun fling(event: InputEvent?, x: Float, y: Float, button: Int) {
                if (java.lang.Math.abs(x) > 150 && scrollX) {
                    flingTimer = flingTime
                    velocityX = x
                    if (cancelTouchFocus) cancelTouchFocus()
                }
                if (java.lang.Math.abs(y) > 150 && scrollY) {
                    flingTimer = flingTime
                    velocityY = -y
                    if (cancelTouchFocus) cancelTouchFocus()
                }
            }

            fun handle(event: Event?): Boolean {
                if (super.handle(event)) {
                    if ((event as InputEvent?).getType() === InputEvent.Type.touchDown) flingTimer = 0f
                    return true
                } else if (event is InputEvent && (event as InputEvent?).isTouchFocusCancel()) //
                    cancel()
                return false
            }
        }
        addListener(flickScrollListener)
        addListener(object : InputListener() {
            fun scrolled(event: InputEvent?, x: Float, y: Float, amount: Int): Boolean {
                setScrollbarsVisible(true)
                if (scrollY) setScrollY(amountY + mouseWheelY * amount) else if (scrollX) //
                    setScrollX(amountX + mouseWheelX * amount) else return false
                return true
            }
        })
    }
}
