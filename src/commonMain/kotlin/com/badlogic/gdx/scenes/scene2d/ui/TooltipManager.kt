/*******************************************************************************
 * Copyright 2015 See AUTHORS file.
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

import com.badlogic.gdx.scenes.scene2d.ui.Touchpad.TouchpadStyle
import com.badlogic.gdx.scenes.scene2d.ui.Tree.TreeStyle
import com.badlogic.gdx.scenes.scene2d.ui.Value.Fixed
import com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup

/**
 * Keeps track of an application's tooltips.
 *
 * @author Nathan Sweet
 */
class TooltipManager {

    /**
     * Seconds from when an actor is hovered to when the tooltip is shown. Default is 2. Call [.hideAll] after changing to
     * reset internal state.
     */
    var initialTime = 2f

    /**
     * Once a tooltip is shown, this is used instead of [.initialTime]. Default is 0.
     */
    var subsequentTime = 0f

    /**
     * Seconds to use [.subsequentTime]. Default is 1.5.
     */
    var resetTime = 1.5f

    /**
     * If false, tooltips will not be shown. Default is true.
     */
    var enabled = true

    /**
     * If false, tooltips will be shown without animations. Default is true.
     */
    var animations = true

    /**
     * The maximum width of a [TextTooltip]. The label will wrap if needed. Default is Integer.MAX_VALUE.
     */
    var maxWidth = Int.MAX_VALUE.toFloat()

    /**
     * The distance from the mouse position to offset the tooltip actor. Default is 15,19.
     */
    var offsetX = 15f
    var offsetY = 19f

    /**
     * The distance from the tooltip actor position to the edge of the screen where the actor will be shown on the other side of
     * the mouse cursor. Default is 7.
     */
    var edgeDistance = 7f
    val shown: Array<Tooltip> = Array()
    var time = initialTime
    val resetTask: Task = object : Task() {
        fun run() {
            time = initialTime
        }
    }
    var showTooltip: Tooltip? = null
    val showTask: Task = object : Task() {
        fun run() {
            if (showTooltip == null || showTooltip.targetActor == null) return
            val stage: Stage = showTooltip.targetActor.getStage() ?: return
            stage.addActor(showTooltip.container)
            showTooltip.container.toFront()
            shown.add(showTooltip)
            showTooltip.container.clearActions()
            showAction(showTooltip)
            if (!showTooltip.instant) {
                time = subsequentTime
                resetTask.cancel()
            }
        }
    }

    fun touchDown(tooltip: Tooltip) {
        showTask.cancel()
        if (tooltip.container.remove()) resetTask.cancel()
        resetTask.run()
        if (enabled || tooltip.always) {
            showTooltip = tooltip
            Timer.schedule(showTask, time)
        }
    }

    fun enter(tooltip: Tooltip) {
        showTooltip = tooltip
        showTask.cancel()
        if (enabled || tooltip.always) {
            if (time == 0f || tooltip.instant) showTask.run() else Timer.schedule(showTask, time)
        }
    }

    fun hide(tooltip: Tooltip) {
        showTooltip = null
        showTask.cancel()
        if (tooltip.container.hasParent()) {
            shown.removeValue(tooltip, true)
            hideAction(tooltip)
            resetTask.cancel()
            Timer.schedule(resetTask, resetTime)
        }
    }

    /**
     * Called when tooltip is shown. Default implementation sets actions to animate showing.
     */
    protected fun showAction(tooltip: Tooltip) {
        val actionTime = if (animations) if (time > 0) 0.5f else 0.15f else 0.1f
        tooltip.container.setTransform(true)
        tooltip.container.getColor().a = 0.2f
        tooltip.container.setScale(0.05f)
        tooltip.container.addAction(parallel(fadeIn(actionTime, fade), scaleTo(1, 1, actionTime, Interpolation.fade)))
    }

    /**
     * Called when tooltip is hidden. Default implementation sets actions to animate hiding and to remove the actor from the stage
     * when the actions are complete. A subclass must at least remove the actor.
     */
    protected fun hideAction(tooltip: Tooltip) {
        tooltip.container
            .addAction(sequence(parallel(alpha(0.2f, 0.2f, fade), scaleTo(0.05f, 0.05f, 0.2f, Interpolation.fade)), removeActor()))
    }

    fun hideAll() {
        resetTask.cancel()
        showTask.cancel()
        time = initialTime
        showTooltip = null
        for (tooltip in shown) tooltip.hide()
        shown.clear()
    }

    /**
     * Shows all tooltips on hover without a delay for [.resetTime] seconds.
     */
    fun instant() {
        time = 0f
        showTask.run()
        showTask.cancel()
    }

    companion object {
        var instance: TooltipManager? = null
            get() {
                if (files == null || files !== Gdx.files) {
                    files = Gdx.files
                    field = TooltipManager()
                }
                return field
            }
            private set
        private var files: Files? = null
    }
}
