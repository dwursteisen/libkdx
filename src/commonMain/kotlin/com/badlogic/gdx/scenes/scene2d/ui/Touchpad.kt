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
import com.badlogic.gdx.scenes.scene2d.ui.Tree.TreeStyle
import com.badlogic.gdx.scenes.scene2d.ui.Value.Fixed
import com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup

/**
 * An on-screen joystick. The movement area of the joystick is circular, centered on the touchpad, and its size determined by the
 * smaller touchpad dimension.
 *
 *
 * The preferred size of the touchpad is determined by the background.
 *
 *
 * [ChangeEvent] is fired when the touchpad knob is moved. Cancelling the event will move the knob to where it was
 * previously.
 *
 * @author Josh Street
 */
class Touchpad(deadzoneRadius: Float, style: TouchpadStyle?) : Widget() {

    private var style: TouchpadStyle? = null
    var isTouched = false

    /**
     * @param reset Whether to reset the knob to the center on touch up.
     */
    var resetOnTouchUp = true
    private var deadzoneRadius = 0f
    private val knobBounds: Circle = Circle(0, 0, 0)
    private val touchBounds: Circle = Circle(0, 0, 0)
    private val deadzoneBounds: Circle = Circle(0, 0, 0)
    private val knobPosition: Vector2 = Vector2()
    private val knobPercent: Vector2 = Vector2()

    /**
     * @param deadzoneRadius The distance in pixels from the center of the touchpad required for the knob to be moved.
     */
    constructor(deadzoneRadius: Float, skin: Skin) : this(deadzoneRadius, skin.get(TouchpadStyle::class.java)) {}

    /**
     * @param deadzoneRadius The distance in pixels from the center of the touchpad required for the knob to be moved.
     */
    constructor(deadzoneRadius: Float, skin: Skin, styleName: String?) : this(deadzoneRadius, skin.get(styleName, TouchpadStyle::class.java)) {}

    fun calculatePositionAndValue(x: Float, y: Float, isTouchUp: Boolean) {
        val oldPositionX: Float = knobPosition.x
        val oldPositionY: Float = knobPosition.y
        val oldPercentX: Float = knobPercent.x
        val oldPercentY: Float = knobPercent.y
        val centerX: Float = knobBounds.x
        val centerY: Float = knobBounds.y
        knobPosition.set(centerX, centerY)
        knobPercent.set(0f, 0f)
        if (!isTouchUp) {
            if (!deadzoneBounds.contains(x, y)) {
                knobPercent.set((x - centerX) / knobBounds.radius, (y - centerY) / knobBounds.radius)
                val length: Float = knobPercent.len()
                if (length > 1) knobPercent.scl(1 / length)
                if (knobBounds.contains(x, y)) {
                    knobPosition.set(x, y)
                } else {
                    knobPosition.set(knobPercent).nor().scl(knobBounds.radius).add(knobBounds.x, knobBounds.y)
                }
            }
        }
        if (oldPercentX != knobPercent.x || oldPercentY != knobPercent.y) {
            val changeEvent: ChangeEvent = Pools.obtain(ChangeEvent::class.java)
            if (fire(changeEvent)) {
                knobPercent.set(oldPercentX, oldPercentY)
                knobPosition.set(oldPositionX, oldPositionY)
            }
            Pools.free(changeEvent)
        }
    }

    fun setStyle(style: TouchpadStyle?) {
        if (style == null) throw java.lang.IllegalArgumentException("style cannot be null")
        this.style = style
        invalidateHierarchy()
    }

    /**
     * Returns the touchpad's style. Modifying the returned style may not have an effect until [.setStyle] is
     * called.
     */
    fun getStyle(): TouchpadStyle? {
        return style
    }

    fun hit(x: Float, y: Float, touchable: Boolean): Actor? {
        if (touchable && this.getTouchable() !== Touchable.enabled) return null
        if (!isVisible()) return null
        return if (touchBounds.contains(x, y)) this else null
    }

    fun layout() {
        // Recalc pad and deadzone bounds
        val halfWidth: Float = getWidth() / 2
        val halfHeight: Float = getHeight() / 2
        var radius: Float = java.lang.Math.min(halfWidth, halfHeight)
        touchBounds.set(halfWidth, halfHeight, radius)
        if (style!!.knob != null) radius -= java.lang.Math.max(style!!.knob.getMinWidth(), style!!.knob.getMinHeight()) / 2
        knobBounds.set(halfWidth, halfHeight, radius)
        deadzoneBounds.set(halfWidth, halfHeight, deadzoneRadius)
        // Recalc pad values and knob position
        knobPosition.set(halfWidth, halfHeight)
        knobPercent.set(0, 0)
    }

    fun draw(batch: Batch, parentAlpha: Float) {
        validate()
        val c: Color = getColor()
        batch.setColor(c.r, c.g, c.b, c.a * parentAlpha)
        var x: Float = getX()
        var y: Float = getY()
        val w: Float = getWidth()
        val h: Float = getHeight()
        val bg: Drawable? = style!!.background
        if (bg != null) bg.draw(batch, x, y, w, h)
        val knob: Drawable? = style!!.knob
        if (knob != null) {
            x += knobPosition.x - knob.getMinWidth() / 2f
            y += knobPosition.y - knob.getMinHeight() / 2f
            knob.draw(batch, x, y, knob.getMinWidth(), knob.getMinHeight())
        }
    }

    val prefWidth: Float
        get() = (if (style!!.background != null) style!!.background.getMinWidth() else 0).toFloat()

    val prefHeight: Float
        get() = (if (style!!.background != null) style!!.background.getMinHeight() else 0).toFloat()

    /**
     * @param deadzoneRadius The distance in pixels from the center of the touchpad required for the knob to be moved.
     */
    fun setDeadzone(deadzoneRadius: Float) {
        if (deadzoneRadius < 0) throw java.lang.IllegalArgumentException("deadzoneRadius must be > 0")
        this.deadzoneRadius = deadzoneRadius
        invalidate()
    }

    /**
     * Returns the x-position of the knob relative to the center of the widget. The positive direction is right.
     */
    val knobX: Float
        get() = knobPosition.x

    /**
     * Returns the y-position of the knob relative to the center of the widget. The positive direction is up.
     */
    val knobY: Float
        get() = knobPosition.y

    /**
     * Returns the x-position of the knob as a percentage from the center of the touchpad to the edge of the circular movement
     * area. The positive direction is right.
     */
    val knobPercentX: Float
        get() = knobPercent.x

    /**
     * Returns the y-position of the knob as a percentage from the center of the touchpad to the edge of the circular movement
     * area. The positive direction is up.
     */
    val knobPercentY: Float
        get() = knobPercent.y

    /**
     * The style for a [Touchpad].
     *
     * @author Josh Street
     */
    class TouchpadStyle {

        /**
         * Stretched in both directions. Optional.
         */
        var background: Drawable? = null

        /**
         * Optional.
         */
        var knob: Drawable? = null

        constructor() {}
        constructor(background: Drawable?, knob: Drawable?) {
            this.background = background
            this.knob = knob
        }

        constructor(style: TouchpadStyle) {
            background = style.background
            knob = style.knob
        }
    }

    /**
     * @param deadzoneRadius The distance in pixels from the center of the touchpad required for the knob to be moved.
     */
    init {
        if (deadzoneRadius < 0) throw java.lang.IllegalArgumentException("deadzoneRadius must be > 0")
        this.deadzoneRadius = deadzoneRadius
        knobPosition.set(getWidth() / 2f, getHeight() / 2f)
        setStyle(style)
        setSize(prefWidth, prefHeight)
        addListener(object : InputListener() {
            fun touchDown(event: InputEvent?, x: Float, y: Float, pointer: Int, button: Int): Boolean {
                if (isTouched) return false
                isTouched = true
                calculatePositionAndValue(x, y, false)
                return true
            }

            fun touchDragged(event: InputEvent?, x: Float, y: Float, pointer: Int) {
                calculatePositionAndValue(x, y, false)
            }

            fun touchUp(event: InputEvent?, x: Float, y: Float, pointer: Int, button: Int) {
                isTouched = false
                calculatePositionAndValue(x, y, resetOnTouchUp)
            }
        })
    }
}
