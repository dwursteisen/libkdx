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

import com.badlogic.gdx.utils.IntArray as GdxIntArray
import com.badlogic.gdx.utils.TimeUtils
import kotlin.jvm.Synchronized

/** Queues events that are later passed to the wrapped [InputProcessor].
 * @author Nathan Sweet
 */
class InputEventQueue(var processor: InputProcessor? = null) : InputProcessor {

    private val queue: GdxIntArray = GdxIntArray()
    private val processingQueue: GdxIntArray = GdxIntArray()

    private var currentEventTime: Long = 0

    fun drain() {
        if (processor == null) {
            queue.clear()
            return
        }
        processingQueue.addAll(queue)
        queue.clear()

        val q: IntArray = processingQueue.items
        val localProcessor = processor
        var i = 0
        val n = processingQueue.size
        while (i < n) {
            val type = q[i++]
            currentEventTime = q[i++].toLong() shl 32 or q[i++].toLong() and 0xFFFFFFFFL
            when (type) {
                SKIP -> i += q[i]
                KEY_DOWN -> localProcessor!!.keyDown(q[i++])
                KEY_UP -> localProcessor!!.keyUp(q[i++])
                KEY_TYPED -> localProcessor!!.keyTyped(q[i++].toChar())
                TOUCH_DOWN -> localProcessor!!.touchDown(q[i++], q[i++], q[i++], q[i++])
                TOUCH_UP -> localProcessor!!.touchUp(q[i++], q[i++], q[i++], q[i++])
                TOUCH_DRAGGED -> localProcessor!!.touchDragged(q[i++], q[i++], q[i++])
                MOUSE_MOVED -> localProcessor!!.mouseMoved(q[i++], q[i++])
                SCROLLED -> localProcessor!!.scrolled(q[i++])
                else -> throw RuntimeException()
            }
        }
        processingQueue.clear()
    }

    @Synchronized
    private fun next(nextType: Int, i: Int): Int {
        var i = i
        val q: IntArray = queue.items
        val n = queue!!.size
        while (i < n) {
            val type = q[i]
            if (type == nextType) return i
            i += 3
            when (type) {
                SKIP -> i += q[i]
                KEY_DOWN -> i++
                KEY_UP -> i++
                KEY_TYPED -> i++
                TOUCH_DOWN -> i += 4
                TOUCH_UP -> i += 4
                TOUCH_DRAGGED -> i += 3
                MOUSE_MOVED -> i += 2
                SCROLLED -> i++
                else -> throw RuntimeException()
            }
        }
        return -1
    }

    private fun queueTime() {
        val time: Long = TimeUtils.nanoTime()
        queue.add((time shr 32).toInt())
        queue.add(time.toInt())
    }

    @Synchronized
    override fun keyDown(keycode: Int): Boolean {
        queue.add(KEY_DOWN)
        queueTime()
        queue.add(keycode)
        return false
    }

    @Synchronized
    override fun keyUp(keycode: Int): Boolean {
        queue.add(KEY_UP)
        queueTime()
        queue.add(keycode)
        return false
    }

    @Synchronized
    override fun keyTyped(character: Char): Boolean {
        queue.add(KEY_TYPED)
        queueTime()
        queue.add(character.toInt())
        return false
    }

    @Synchronized
    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        queue.add(TOUCH_DOWN)
        queueTime()
        queue.add(screenX)
        queue.add(screenY)
        queue.add(pointer)
        queue.add(button)
        return false
    }

    @Synchronized
    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        queue.add(TOUCH_UP)
        queueTime()
        queue.add(screenX)
        queue.add(screenY)
        queue.add(pointer)
        queue.add(button)
        return false
    }

    @Synchronized
    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        // Skip any queued touch dragged events for the same pointer.
        var i = next(TOUCH_DRAGGED, 0)
        while (i >= 0) {
            if (queue!![i + 5] === pointer) {
                queue!![i] = SKIP
                queue[i + 3] = 3
            }
            i = next(TOUCH_DRAGGED, i + 6)
        }
        queue.add(TOUCH_DRAGGED)
        queueTime()
        queue.add(screenX)
        queue.add(screenY)
        queue.add(pointer)
        return false
    }

    @Synchronized
    override fun mouseMoved(screenX: Int, screenY: Int): Boolean {
        // Skip any queued mouse moved events.
        var i = next(MOUSE_MOVED, 0)
        while (i >= 0) {
            queue!![i] = SKIP
            queue[i + 3] = 2
            i = next(MOUSE_MOVED, i + 5)
        }
        queue.add(MOUSE_MOVED)
        queueTime()
        queue.add(screenX)
        queue.add(screenY)
        return false
    }

    @Synchronized
    override fun scrolled(amount: Int): Boolean {
        queue.add(SCROLLED)
        queueTime()
        queue.add(amount)
        return false
    }

    companion object {
        private const val SKIP = -1
        private const val KEY_DOWN = 0
        private const val KEY_UP = 1
        private const val KEY_TYPED = 2
        private const val TOUCH_DOWN = 3
        private const val TOUCH_UP = 4
        private const val TOUCH_DRAGGED = 5
        private const val MOUSE_MOVED = 6
        private const val SCROLLED = 7
    }
}
