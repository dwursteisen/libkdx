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
package com.badlogic.gdx.scenes.scene2d.actions

import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.scenes.scene2d.Action
import com.badlogic.gdx.utils.Pool
import java.lang.Runnable

/**
 * Base class for actions that transition over time using the percent complete.
 *
 * @author Nathan Sweet
 */
abstract class TemporalAction : Action {

    /**
     * Sets the length of the transition in seconds.
     */
    var duration = 0f
    /**
     * Gets the transition time so far.
     */
    /**
     * Sets the transition time so far.
     */
    var time = 0f
    var interpolation: Interpolation? = null

    /**
     * When true, the action's progress will go from 100% to 0%.
     */
    var isReverse = false
    private var began = false

    /**
     * Returns true after [.act] has been called where time >= duration.
     */
    var isComplete = false
        private set

    constructor() {}
    constructor(duration: Float) {
        this.duration = duration
    }

    constructor(duration: Float, interpolation: Interpolation?) {
        this.duration = duration
        this.interpolation = interpolation
    }

    fun act(delta: Float): Boolean {
        if (isComplete) return true
        val pool: Pool = getPool()
        setPool(null) // Ensure this action can't be returned to the pool while executing.
        return try {
            if (!began) {
                begin()
                began = true
            }
            time += delta
            isComplete = time >= duration
            var percent: Float = if (isComplete) 1 else time / duration
            if (interpolation != null) percent = interpolation!!.apply(percent)
            update(if (isReverse) 1 - percent else percent)
            if (isComplete) end()
            isComplete
        } finally {
            setPool(pool)
        }
    }

    /**
     * Called the first time [.act] is called. This is a good place to query the [actor&#39;s][.actor] starting
     * state.
     */
    protected fun begin() {}

    /**
     * Called the last time [.act] is called.
     */
    protected fun end() {}

    /**
     * Called each frame.
     *
     * @param percent The percentage of completion for this action, growing from 0 to 1 over the duration. If
     * [reversed][.setReverse], this will shrink from 1 to 0.
     */
    protected abstract fun update(percent: Float)

    /**
     * Skips to the end of the transition.
     */
    fun finish() {
        time = duration
    }

    fun restart() {
        time = 0f
        began = false
        isComplete = false
    }

    fun reset() {
        super.reset()
        isReverse = false
        interpolation = null
    }
}
