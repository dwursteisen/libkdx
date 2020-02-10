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

import com.badlogic.gdx.scenes.scene2d.Action
import com.badlogic.gdx.utils.Pool
import java.lang.Runnable

/**
 * An action that runs a [Runnable]. Alternatively, the [.run] method can be overridden instead of setting a
 * runnable.
 *
 * @author Nathan Sweet
 */
class RunnableAction : Action() {

    private var runnable: Runnable? = null
    private var ran = false
    fun act(delta: Float): Boolean {
        if (!ran) {
            ran = true
            run()
        }
        return true
    }

    /**
     * Called to run the runnable.
     */
    fun run() {
        val pool: Pool = getPool()
        setPool(null) // Ensure this action can't be returned to the pool inside the runnable.
        try {
            runnable.run()
        } finally {
            setPool(pool)
        }
    }

    fun restart() {
        ran = false
    }

    fun reset() {
        super.reset()
        runnable = null
    }

    fun getRunnable(): Runnable? {
        return runnable
    }

    fun setRunnable(runnable: Runnable?) {
        this.runnable = runnable
    }
}
