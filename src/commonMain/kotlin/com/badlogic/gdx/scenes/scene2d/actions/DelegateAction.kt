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

/**
 * Base class for an action that wraps another action.
 *
 * @author Nathan Sweet
 */
abstract class DelegateAction : Action() {

    protected var action: Action? = null

    /**
     * Sets the wrapped action.
     */
    fun setAction(action: Action?) {
        this.action = action
    }

    fun getAction(): Action? {
        return action
    }

    protected abstract fun delegate(delta: Float): Boolean
    fun act(delta: Float): Boolean {
        val pool: Pool = getPool()
        setPool(null) // Ensure this action can't be returned to the pool inside the delegate action.
        return try {
            delegate(delta)
        } finally {
            setPool(pool)
        }
    }

    fun restart() {
        if (action != null) action.restart()
    }

    fun reset() {
        super.reset()
        action = null
    }

    fun setActor(actor: Actor?) {
        if (action != null) action.setActor(actor)
        super.setActor(actor)
    }

    fun setTarget(target: Actor?) {
        if (action != null) action.setTarget(target)
        super.setTarget(target)
    }

    override fun toString(): String {
        return super.toString() + if (action == null) "" else "($action)"
    }
}
