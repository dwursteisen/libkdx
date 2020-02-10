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
package com.badlogic.gdx

import com.badlogic.gdx.Graphics.BufferFormat
import com.badlogic.gdx.Graphics.GraphicsType
import com.badlogic.gdx.Input.Peripheral
import com.badlogic.gdx.Input.TextInputListener
import com.badlogic.gdx.InputEventQueue
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.SnapshotArray
import java.lang.NullPointerException
import java.lang.RuntimeException

/** An [InputProcessor] that delegates to an ordered list of other InputProcessors. Delegation for an event stops if a
 * processor returns true, which indicates that the event was handled.
 * @author Nathan Sweet
 */
class InputMultiplexer : InputProcessor {

    private val processors: SnapshotArray<InputProcessor?>? = SnapshotArray(4)

    constructor() {}
    constructor(vararg processors: InputProcessor?) {
        this.processors.addAll(processors)
    }

    fun addProcessor(index: Int, processor: InputProcessor?) {
        if (processor == null) throw NullPointerException("processor cannot be null")
        processors.insert(index, processor)
    }

    fun removeProcessor(index: Int) {
        processors.removeIndex(index)
    }

    fun addProcessor(processor: InputProcessor?) {
        if (processor == null) throw NullPointerException("processor cannot be null")
        processors.add(processor)
    }

    fun removeProcessor(processor: InputProcessor?) {
        processors.removeValue(processor, true)
    }

    /** @return the number of processors in this multiplexer
     */
    fun size(): Int {
        return processors.size
    }

    fun clear() {
        processors.clear()
    }

    fun setProcessors(vararg processors: InputProcessor?) {
        this.processors.clear()
        this.processors.addAll(processors)
    }

    fun setProcessors(processors: Array<InputProcessor?>?) {
        this.processors.clear()
        this.processors.addAll(processors)
    }

    fun getProcessors(): SnapshotArray<InputProcessor?>? {
        return processors
    }

    override fun keyDown(keycode: Int): Boolean {
        val items: Array<Any?> = processors.begin()
        try {
            var i = 0
            val n: Int = processors.size
            while (i < n) {
                if ((items[i] as InputProcessor?)!!.keyDown(keycode)) return true
                i++
            }
        } finally {
            processors.end()
        }
        return false
    }

    override fun keyUp(keycode: Int): Boolean {
        val items: Array<Any?> = processors.begin()
        try {
            var i = 0
            val n: Int = processors.size
            while (i < n) {
                if ((items[i] as InputProcessor?)!!.keyUp(keycode)) return true
                i++
            }
        } finally {
            processors.end()
        }
        return false
    }

    override fun keyTyped(character: Char): Boolean {
        val items: Array<Any?> = processors.begin()
        try {
            var i = 0
            val n: Int = processors.size
            while (i < n) {
                if ((items[i] as InputProcessor?)!!.keyTyped(character)) return true
                i++
            }
        } finally {
            processors.end()
        }
        return false
    }

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        val items: Array<Any?> = processors.begin()
        try {
            var i = 0
            val n: Int = processors.size
            while (i < n) {
                if ((items[i] as InputProcessor?)!!.touchDown(screenX, screenY, pointer, button)) return true
                i++
            }
        } finally {
            processors.end()
        }
        return false
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        val items: Array<Any?> = processors.begin()
        try {
            var i = 0
            val n: Int = processors.size
            while (i < n) {
                if ((items[i] as InputProcessor?)!!.touchUp(screenX, screenY, pointer, button)) return true
                i++
            }
        } finally {
            processors.end()
        }
        return false
    }

    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        val items: Array<Any?> = processors.begin()
        try {
            var i = 0
            val n: Int = processors.size
            while (i < n) {
                if ((items[i] as InputProcessor?)!!.touchDragged(screenX, screenY, pointer)) return true
                i++
            }
        } finally {
            processors.end()
        }
        return false
    }

    override fun mouseMoved(screenX: Int, screenY: Int): Boolean {
        val items: Array<Any?> = processors.begin()
        try {
            var i = 0
            val n: Int = processors.size
            while (i < n) {
                if ((items[i] as InputProcessor?)!!.mouseMoved(screenX, screenY)) return true
                i++
            }
        } finally {
            processors.end()
        }
        return false
    }

    override fun scrolled(amount: Int): Boolean {
        val items: Array<Any?> = processors.begin()
        try {
            var i = 0
            val n: Int = processors.size
            while (i < n) {
                if ((items[i] as InputProcessor?)!!.scrolled(amount)) return true
                i++
            }
        } finally {
            processors.end()
        }
        return false
    }
}
