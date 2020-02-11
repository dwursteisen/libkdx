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
 * Executes a number of actions at the same time.
 *
 * @author Nathan Sweet
 */
class ParallelAction : Action {

    var actions: Array<Action?>? = Array(4)
    private var complete = false

    constructor() {}
    constructor(action1: Action?) {
        addAction(action1)
    }

    constructor(action1: Action?, action2: Action?) {
        addAction(action1)
        addAction(action2)
    }

    constructor(action1: Action?, action2: Action?, action3: Action?) {
        addAction(action1)
        addAction(action2)
        addAction(action3)
    }

    constructor(action1: Action?, action2: Action?, action3: Action?, action4: Action?) {
        addAction(action1)
        addAction(action2)
        addAction(action3)
        addAction(action4)
    }

    constructor(action1: Action?, action2: Action?, action3: Action?, action4: Action?, action5: Action?) {
        addAction(action1)
        addAction(action2)
        addAction(action3)
        addAction(action4)
        addAction(action5)
    }

    fun act(delta: Float): Boolean {
        if (complete) return true
        complete = true
        val pool: Pool = getPool()
        setPool(null) // Ensure this action can't be returned to the pool while executing.
        return try {
            val actions: Array<Action?>? = actions
            var i = 0
            val n = actions!!.size
            while (i < n && actor != null) {
                val currentAction: Action? = actions[i]
                if (currentAction.getActor() != null && !currentAction.act(delta)) complete = false
                if (actor == null) return true // This action was removed.
                i++
            }
            complete
        } finally {
            setPool(pool)
        }
    }

    fun restart() {
        complete = false
        val actions: Array<Action?>? = actions
        var i = 0
        val n = actions!!.size
        while (i < n) {
            actions[i].restart()
            i++
        }
    }

    fun reset() {
        super.reset()
        actions.clear()
    }

    fun addAction(action: Action?) {
        actions.add(action)
        if (actor != null) action.setActor(actor)
    }

    fun setActor(actor: Actor?) {
        val actions: Array<Action?>? = actions
        var i = 0
        val n = actions!!.size
        while (i < n) {
            actions[i].setActor(actor)
            i++
        }
        super.setActor(actor)
    }

    fun getActions(): Array<Action?>? {
        return actions
    }

    override fun toString(): String {
        val buffer: java.lang.StringBuilder = java.lang.StringBuilder(64)
        buffer.append(super.toString())
        buffer.append('(')
        val actions: Array<Action?>? = actions
        var i = 0
        val n = actions!!.size
        while (i < n) {
            if (i > 0) buffer.append(", ")
            buffer.append(actions[i])
            i++
        }
        buffer.append(')')
        return buffer.toString()
    }
}
