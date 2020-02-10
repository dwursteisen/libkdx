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

import java.lang.Runnable

/**
 * Repeats an action a number of times or forever.
 *
 * @author Nathan Sweet
 */
class RepeatAction : DelegateAction() {

    /**
     * Sets the number of times to repeat. Can be set to [.FOREVER].
     */
    var count = 0
    private var executedCount = 0
    private var finished = false
    protected fun delegate(delta: Float): Boolean {
        if (executedCount == count) return true
        if (action.act(delta)) {
            if (finished) return true
            if (count > 0) executedCount++
            if (executedCount == count) return true
            if (action != null) action.restart()
        }
        return false
    }

    /**
     * Causes the action to not repeat again.
     */
    fun finish() {
        finished = true
    }

    fun restart() {
        super.restart()
        executedCount = 0
        finished = false
    }

    companion object {
        const val FOREVER = -1
    }
}
