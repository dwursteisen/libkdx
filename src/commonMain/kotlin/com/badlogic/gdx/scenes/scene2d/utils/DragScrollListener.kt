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
package com.badlogic.gdx.scenes.scene2d.utils

import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack
import com.badlogic.gdx.utils.Timer
import com.badlogic.gdx.utils.Timer.Task

/**
 * Causes a scroll pane to scroll when a drag goes outside the bounds of the scroll pane. Attach the listener to the actor which
 * will cause scrolling when dragged, usually the scroll pane or the scroll pane's actor.
 *
 *
 * If [ScrollPane.setFlickScroll] is true, the scroll pane must have
 * [ScrollPane.setCancelTouchFocus] false. When a drag starts that should drag rather than flick scroll, cancel the
 * scroll pane's touch focus using `stage.cancelTouchFocus(gloom.monstersScroll);`. In this case the drag scroll
 * listener must not be attached to the scroll pane, else it would also lose touch focus. Instead it can be attached to the scroll
 * pane's actor.
 *
 *
 * If using drag and drop, [DragAndDrop.setCancelTouchFocus] must be false.
 *
 * @author Nathan Sweet
 */
class DragScrollListener(scroll: ScrollPane) : DragListener() {

    private val scroll: ScrollPane
    private val scrollUp: Task
    private val scrollDown: Task
    var interpolation: Interpolation = Interpolation.exp5In
    var minSpeed = 15f
    var maxSpeed = 75f
    var tickSecs = 0.05f
    var startTime: Long = 0
    var rampTime: Long = 1750
    var padTop = 0f
    var padBottom = 0f
    fun setup(minSpeedPixels: Float, maxSpeedPixels: Float, tickSecs: Float, rampSecs: Float) {
        minSpeed = minSpeedPixels
        maxSpeed = maxSpeedPixels
        this.tickSecs = tickSecs
        rampTime = (rampSecs * 1000).toLong()
    }

    val scrollPixels: Float
        get() = interpolation.apply(minSpeed, maxSpeed, java.lang.Math.min(1f, (java.lang.System.currentTimeMillis() - startTime) / rampTime.toFloat()))

    fun drag(event: InputEvent, x: Float, y: Float, pointer: Int) {
        event.getListenerActor().localToActorCoordinates(scroll, tmpCoords.set(x, y))
        if (isAbove(tmpCoords.y)) {
            scrollDown.cancel()
            if (!scrollUp.isScheduled()) {
                startTime = java.lang.System.currentTimeMillis()
                Timer.schedule(scrollUp, tickSecs, tickSecs)
            }
            return
        } else if (isBelow(tmpCoords.y)) {
            scrollUp.cancel()
            if (!scrollDown.isScheduled()) {
                startTime = java.lang.System.currentTimeMillis()
                Timer.schedule(scrollDown, tickSecs, tickSecs)
            }
            return
        }
        scrollUp.cancel()
        scrollDown.cancel()
    }

    fun dragStop(event: InputEvent?, x: Float, y: Float, pointer: Int) {
        scrollUp.cancel()
        scrollDown.cancel()
    }

    protected fun isAbove(y: Float): Boolean {
        return y >= scroll.getHeight() - padTop
    }

    protected fun isBelow(y: Float): Boolean {
        return y < padBottom
    }

    protected fun scroll(y: Float) {
        scroll.setScrollY(y)
    }

    fun setPadding(padTop: Float, padBottom: Float) {
        this.padTop = padTop
        this.padBottom = padBottom
    }

    companion object {
        val tmpCoords: Vector2 = Vector2()
    }

    init {
        this.scroll = scroll
        scrollUp = object : Task() {
            fun run() {
                scroll(scroll.getScrollY() - scrollPixels)
            }
        }
        scrollDown = object : Task() {
            fun run() {
                scroll(scroll.getScrollY() + scrollPixels)
            }
        }
    }
}
