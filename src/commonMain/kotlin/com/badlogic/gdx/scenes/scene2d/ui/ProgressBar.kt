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

import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane.ScrollPaneStyle
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox.SelectBoxList
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox.SelectBoxStyle
import java.lang.UnsupportedOperationException

/**
 * A progress bar is a widget that visually displays the progress of some activity or a value within given range. The progress
 * bar has a range (min, max) and a stepping between each value it represents. The percentage of completeness typically starts out
 * as an empty progress bar and gradually becomes filled in as the task or variable value progresses.
 *
 *
 * [ChangeEvent] is fired when the progress bar knob is moved. Cancelling the event will move the knob to where it was
 * previously.
 *
 *
 * For a horizontal progress bar, its preferred height is determined by the larger of the knob and background, and the preferred
 * width is 140, a relatively arbitrary size. These parameters are reversed for a vertical progress bar.
 *
 * @author mzechner
 * @author Nathan Sweet
 */
class ProgressBar(min: Float, max: Float, stepSize: Float, vertical: Boolean, style: ProgressBarStyle?) : Widget(), Disableable {

    private var style: ProgressBarStyle? = null
    var minValue = 0f
        private set
    var maxValue = 0f
        private set
    private var stepSize = 0f
    var value = 0f
        private set
    private var animateFromValue = 0f

    /**
     * Returns progress bar visual position within the range.
     */
    protected var knobPosition = 0f

    /**
     * True if the progress bar is vertical, false if it is horizontal.
     */
    val isVertical: Boolean
    private var animateDuration = 0f
    private var animateTime = 0f
    private var animateInterpolation: Interpolation? = Interpolation.linear
    var isDisabled = false
    private var visualInterpolation: Interpolation? = Interpolation.linear
    private var round = true

    constructor(min: Float, max: Float, stepSize: Float, vertical: Boolean, skin: Skin?) : this(min, max, stepSize, vertical, skin.get("default-" + if (vertical) "vertical" else "horizontal", ProgressBarStyle::class.java)) {}
    constructor(min: Float, max: Float, stepSize: Float, vertical: Boolean, skin: Skin?, styleName: String?) : this(min, max, stepSize, vertical, skin.get(styleName, ProgressBarStyle::class.java)) {}

    fun setStyle(style: ProgressBarStyle?) {
        if (style == null) throw java.lang.IllegalArgumentException("style cannot be null.")
        this.style = style
        invalidateHierarchy()
    }

    /**
     * Returns the progress bar's style. Modifying the returned style may not have an effect until
     * [.setStyle] is called.
     */
    fun getStyle(): ProgressBarStyle? {
        return style
    }

    fun act(delta: Float) {
        super.act(delta)
        if (animateTime > 0) {
            animateTime -= delta
            val stage: Stage = getStage()
            if (stage != null && stage.getActionsRequestRendering()) Gdx.graphics.requestRendering()
        }
    }

    fun draw(batch: Batch?, parentAlpha: Float) {
        val style = style
        val disabled = isDisabled
        val knob: Drawable? = knobDrawable
        val bg: Drawable = if (disabled && style!!.disabledBackground != null) style.disabledBackground else style!!.background
        val knobBefore: Drawable = if (disabled && style.disabledKnobBefore != null) style.disabledKnobBefore else style.knobBefore
        val knobAfter: Drawable = if (disabled && style.disabledKnobAfter != null) style.disabledKnobAfter else style.knobAfter
        val color: Color = getColor()
        val x: Float = getX()
        val y: Float = getY()
        val width: Float = getWidth()
        val height: Float = getHeight()
        val knobHeight = (if (knob == null) 0 else knob.getMinHeight().toFloat()).toFloat()
        val knobWidth = (if (knob == null) 0 else knob.getMinWidth().toFloat()).toFloat()
        val percent = visualPercent
        batch.setColor(color.r, color.g, color.b, color.a * parentAlpha)
        if (isVertical) {
            var positionHeight = height
            var bgTopHeight = 0f
            var bgBottomHeight = 0f
            if (bg != null) {
                if (round) bg.draw(batch, java.lang.Math.round(x + (width - bg.getMinWidth()) * 0.5f), y, java.lang.Math.round(bg.getMinWidth()), height) else bg.draw(batch, x + width - bg.getMinWidth() * 0.5f, y, bg.getMinWidth(), height)
                bgTopHeight = bg.getTopHeight()
                bgBottomHeight = bg.getBottomHeight()
                positionHeight -= bgTopHeight + bgBottomHeight
            }
            var knobHeightHalf = 0f
            if (knob == null) {
                knobHeightHalf = (if (knobBefore == null) 0 else knobBefore.getMinHeight() * 0.5f).toFloat()
                knobPosition = (positionHeight - knobHeightHalf) * percent
                knobPosition = java.lang.Math.min(positionHeight - knobHeightHalf, knobPosition)
            } else {
                knobHeightHalf = knobHeight * 0.5f
                knobPosition = (positionHeight - knobHeight) * percent
                knobPosition = java.lang.Math.min(positionHeight - knobHeight, knobPosition) + bgBottomHeight
            }
            knobPosition = java.lang.Math.max(java.lang.Math.min(0f, bgBottomHeight), knobPosition)
            if (knobBefore != null) {
                if (round) {
                    knobBefore.draw(batch, java.lang.Math.round(x + (width - knobBefore.getMinWidth()) * 0.5f), java.lang.Math.round(y + bgTopHeight),
                        java.lang.Math.round(knobBefore.getMinWidth()), java.lang.Math.round(knobPosition + knobHeightHalf))
                } else {
                    knobBefore.draw(batch, x + (width - knobBefore.getMinWidth()) * 0.5f, y + bgTopHeight, knobBefore.getMinWidth(),
                        knobPosition + knobHeightHalf)
                }
            }
            if (knobAfter != null) {
                if (round) {
                    knobAfter.draw(batch, java.lang.Math.round(x + (width - knobAfter.getMinWidth()) * 0.5f),
                        java.lang.Math.round(y + knobPosition + knobHeightHalf), java.lang.Math.round(knobAfter.getMinWidth()),
                        java.lang.Math.round(height - knobPosition - knobHeightHalf))
                } else {
                    knobAfter.draw(batch, x + (width - knobAfter.getMinWidth()) * 0.5f, y + knobPosition + knobHeightHalf,
                        knobAfter.getMinWidth(), height - knobPosition - knobHeightHalf)
                }
            }
            if (knob != null) {
                if (round) {
                    knob.draw(batch, java.lang.Math.round(x + (width - knobWidth) * 0.5f), java.lang.Math.round(y + knobPosition), java.lang.Math.round(knobWidth),
                        java.lang.Math.round(knobHeight))
                } else knob.draw(batch, x + (width - knobWidth) * 0.5f, y + knobPosition, knobWidth, knobHeight)
            }
        } else {
            var positionWidth = width
            var bgLeftWidth = 0f
            var bgRightWidth = 0f
            if (bg != null) {
                if (round) bg.draw(batch, x, java.lang.Math.round(y + (height - bg.getMinHeight()) * 0.5f), width, java.lang.Math.round(bg.getMinHeight())) else bg.draw(batch, x, y + (height - bg.getMinHeight()) * 0.5f, width, bg.getMinHeight())
                bgLeftWidth = bg.getLeftWidth()
                bgRightWidth = bg.getRightWidth()
                positionWidth -= bgLeftWidth + bgRightWidth
            }
            var knobWidthHalf = 0f
            if (knob == null) {
                knobWidthHalf = (if (knobBefore == null) 0 else knobBefore.getMinWidth() * 0.5f).toFloat()
                knobPosition = (positionWidth - knobWidthHalf) * percent
                knobPosition = java.lang.Math.min(positionWidth - knobWidthHalf, knobPosition)
            } else {
                knobWidthHalf = knobWidth * 0.5f
                knobPosition = (positionWidth - knobWidth) * percent
                knobPosition = java.lang.Math.min(positionWidth - knobWidth, knobPosition) + bgLeftWidth
            }
            knobPosition = java.lang.Math.max(java.lang.Math.min(0f, bgLeftWidth), knobPosition)
            if (knobBefore != null) {
                if (round) {
                    knobBefore.draw(batch, java.lang.Math.round(x + bgLeftWidth), java.lang.Math.round(y + (height - knobBefore.getMinHeight()) * 0.5f),
                        java.lang.Math.round(knobPosition + knobWidthHalf), java.lang.Math.round(knobBefore.getMinHeight()))
                } else {
                    knobBefore.draw(batch, x + bgLeftWidth, y + (height - knobBefore.getMinHeight()) * 0.5f, knobPosition + knobWidthHalf,
                        knobBefore.getMinHeight())
                }
            }
            if (knobAfter != null) {
                if (round) {
                    knobAfter.draw(batch, java.lang.Math.round(x + knobPosition + knobWidthHalf),
                        java.lang.Math.round(y + (height - knobAfter.getMinHeight()) * 0.5f), java.lang.Math.round(width - knobPosition - knobWidthHalf),
                        java.lang.Math.round(knobAfter.getMinHeight()))
                } else {
                    knobAfter.draw(batch, x + knobPosition + knobWidthHalf, y + (height - knobAfter.getMinHeight()) * 0.5f,
                        width - knobPosition - knobWidthHalf, knobAfter.getMinHeight())
                }
            }
            if (knob != null) {
                if (round) {
                    knob.draw(batch, java.lang.Math.round(x + knobPosition), java.lang.Math.round(y + (height - knobHeight) * 0.5f), java.lang.Math.round(knobWidth),
                        java.lang.Math.round(knobHeight))
                } else knob.draw(batch, x + knobPosition, y + (height - knobHeight) * 0.5f, knobWidth, knobHeight)
            }
        }
    }

    /**
     * If [animating][.setAnimateDuration] the progress bar value, this returns the value current displayed.
     */
    val visualValue: Float
        get() = if (animateTime > 0) animateInterpolation.apply(animateFromValue, value, 1 - animateTime / animateDuration) else value

    val percent: Float
        get() = if (minValue == maxValue) 0 else (value - minValue) / (maxValue - minValue)

    val visualPercent: Float
        get() = if (minValue == maxValue) 0 else visualInterpolation.apply((visualValue - minValue) / (maxValue - minValue))

    protected val knobDrawable: Drawable?
        protected get() = if (isDisabled && style!!.disabledKnob != null) style!!.disabledKnob else style!!.knob

    /**
     * Sets the progress bar position, rounded to the nearest step size and clamped to the minimum and maximum values.
     * [.clamp] can be overridden to allow values outside of the progress bar's min/max range.
     *
     * @return false if the value was not changed because the progress bar already had the value or it was canceled by a
     * listener.
     */
    fun setValue(value: Float): Boolean {
        var value = value
        value = clamp(java.lang.Math.round(value / stepSize) * stepSize)
        val oldValue = this.value
        if (value == oldValue) return false
        val oldVisualValue = visualValue
        this.value = value
        val changeEvent: ChangeEvent = Pools.obtain(ChangeEvent::class.java)
        val cancelled: Boolean = fire(changeEvent)
        if (cancelled) this.value = oldValue else if (animateDuration > 0) {
            animateFromValue = oldVisualValue
            animateTime = animateDuration
        }
        Pools.free(changeEvent)
        return !cancelled
    }

    /**
     * Clamps the value to the progress bar's min/max range. This can be overridden to allow a range different from the progress
     * bar knob's range.
     */
    protected fun clamp(value: Float): Float {
        return MathUtils.clamp(value, minValue, maxValue)
    }

    /**
     * Sets the range of this progress bar. The progress bar's current value is clamped to the range.
     */
    fun setRange(min: Float, max: Float) {
        if (min > max) throw java.lang.IllegalArgumentException("min must be <= max: $min <= $max")
        minValue = min
        maxValue = max
        if (value < min) setValue(min) else if (value > max) setValue(max)
    }

    fun setStepSize(stepSize: Float) {
        if (stepSize <= 0) throw java.lang.IllegalArgumentException("steps must be > 0: $stepSize")
        this.stepSize = stepSize
    }

    val prefWidth: Float
        get() = if (isVertical) {
            val knob: Drawable? = knobDrawable
            val bg: Drawable = if (isDisabled && style!!.disabledBackground != null) style!!.disabledBackground else style!!.background
            java.lang.Math.max(if (knob == null) 0 else knob.getMinWidth(), if (bg == null) 0 else bg.getMinWidth())
        } else 140

    val prefHeight: Float
        get() = if (isVertical) 140 else {
            val knob: Drawable? = knobDrawable
            val bg: Drawable = if (isDisabled && style!!.disabledBackground != null) style!!.disabledBackground else style!!.background
            java.lang.Math.max(if (knob == null) 0 else knob.getMinHeight(), if (bg == null) 0 else bg.getMinHeight())
        }

    fun getStepSize(): Float {
        return stepSize
    }

    /**
     * If > 0, changes to the progress bar value via [.setValue] will happen over this duration in seconds.
     */
    fun setAnimateDuration(duration: Float) {
        animateDuration = duration
    }

    /**
     * Sets the interpolation to use for [.setAnimateDuration].
     */
    fun setAnimateInterpolation(animateInterpolation: Interpolation?) {
        if (animateInterpolation == null) throw java.lang.IllegalArgumentException("animateInterpolation cannot be null.")
        this.animateInterpolation = animateInterpolation
    }

    /**
     * Sets the interpolation to use for display.
     */
    fun setVisualInterpolation(interpolation: Interpolation?) {
        visualInterpolation = interpolation
    }

    /**
     * If true (the default), inner Drawable positions and sizes are rounded to integers.
     */
    fun setRound(round: Boolean) {
        this.round = round
    }

    /**
     * The style for a progress bar, see [ProgressBar].
     *
     * @author mzechner
     * @author Nathan Sweet
     */
    class ProgressBarStyle {

        /**
         * The progress bar background, stretched only in one direction. Optional.
         */
        var background: Drawable? = null

        /**
         * Optional.
         */
        var disabledBackground: Drawable? = null

        /**
         * Optional, centered on the background.
         */
        var knob: Drawable? = null
        var disabledKnob: Drawable? = null

        /**
         * Optional.
         */
        var knobBefore: Drawable? = null
        var knobAfter: Drawable? = null
        var disabledKnobBefore: Drawable? = null
        var disabledKnobAfter: Drawable? = null

        constructor() {}
        constructor(background: Drawable?, knob: Drawable?) {
            this.background = background
            this.knob = knob
        }

        constructor(style: ProgressBarStyle?) {
            background = style!!.background
            disabledBackground = style.disabledBackground
            knob = style.knob
            disabledKnob = style.disabledKnob
            knobBefore = style.knobBefore
            knobAfter = style.knobAfter
            disabledKnobBefore = style.disabledKnobBefore
            disabledKnobAfter = style.disabledKnobAfter
        }
    }

    /**
     * Creates a new progress bar. If horizontal, its width is determined by the prefWidth parameter, and its height is determined
     * by the maximum of the height of either the progress bar [NinePatch] or progress bar handle [TextureRegion]. The
     * min and max values determine the range the values of this progress bar can take on, the stepSize parameter specifies the
     * distance between individual values.
     *
     *
     * E.g. min could be 4, max could be 10 and stepSize could be 0.2, giving you a total of 30 values, 4.0 4.2, 4.4 and so on.
     *
     * @param min      the minimum value
     * @param max      the maximum value
     * @param stepSize the step size between values
     * @param style    the [ProgressBarStyle]
     */
    init {
        if (min > max) throw java.lang.IllegalArgumentException("max must be > min. min,max: $min, $max")
        if (stepSize <= 0) throw java.lang.IllegalArgumentException("stepSize must be > 0: $stepSize")
        setStyle(style)
        minValue = min
        maxValue = max
        this.stepSize = stepSize
        isVertical = vertical
        value = min
        setSize(prefWidth, prefHeight)
    }
}
