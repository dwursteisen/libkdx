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
 * Executes a number of actions one at a time.
 *
 * @author Nathan Sweet
 */
class SequenceAction : ParallelAction {

    private var index = 0

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
        if (index >= actions.size) return true
        val pool: Pool = getPool()
        setPool(null) // Ensure this action can't be returned to the pool while executings.
        return try {
            if (actions.get(index).act(delta)) {
                if (actor == null) return true // This action was removed.
                index++
                if (index >= actions.size) return true
            }
            false
        } finally {
            setPool(pool)
        }
    }

    fun restart() {
        super.restart()
        index = 0
    }
}
