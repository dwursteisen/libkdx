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
package com.badlogic.gdx.utils

import com.badlogic.gdx.utils.TimeUtils.nanoTime

/**
 * Class to keep track of the time and load (percentage of total time) a specific task takes. Call [.start] just before
 * starting the task and [.stop] right after. You can do this multiple times if required. Every render or update call
 * [.tick] to update the values. The [.time] [FloatCounter] provides access to the minimum, maximum, average,
 * total and current time (in seconds) the task takes. Likewise for the [.load] value, which is the percentage of the total time.
 *
 * @author xoppa
 */
class PerformanceCounter @JvmOverloads constructor(
    /**
     * The name of this counter
     */
    val name: String, windowSize: Int = 5) {

    private var startTime = 0L
    private var lastTick = 0L

    /**
     * The time value of this counter (seconds)
     */
    val time: FloatCounter

    /**
     * The load value of this counter
     */
    val load: FloatCounter

    /**
     * The current value in seconds, you can manually increase this using your own timing mechanism if needed, if you do so, you also need to
     * update [.valid].
     */
    var current = 0f

    /**
     * Flag to indicate that the current value is valid, you need to set this to true if using your own timing mechanism
     */
    var valid = false

    /**
     * Updates the time and load counters and resets the time. Call [.start] to begin a new count. The values are only
     * valid after at least two calls to this method.
     */
    fun tick() {
        val t = nanoTime()
        if (lastTick > 0L) tick((t - lastTick) * nano2seconds)
        lastTick = t
    }

    /**
     * Updates the time and load counters and resets the time. Call [.start] to begin a new count.
     *
     * @param delta The time since the last call to this method
     */
    fun tick(delta: Float) {
        if (!valid) {
            Gdx.app.error("PerformanceCounter", "Invalid data, check if you called PerformanceCounter#stop()")
            return
        }
        time.put(current)
        val currentLoad = if (delta == 0f) 0f else current / delta
        load.put(if (delta > 1f) currentLoad else delta * currentLoad + (1f - delta) * load.latest)
        current = 0f
        valid = false
    }

    /**
     * Start counting, call this method just before performing the task you want to keep track of. Call [.stop] when done.
     */
    fun start() {
        startTime = nanoTime()
        valid = false
    }

    /**
     * Stop counting, call this method right after you performed the task you want to keep track of. Call [.start] again
     * when you perform more of that task.
     */
    fun stop() {
        if (startTime > 0L) {
            current += (nanoTime() - startTime) * nano2seconds
            startTime = 0L
            valid = true
        }
    }

    /**
     * Resets this performance counter to its defaults values.
     */
    fun reset() {
        time.reset()
        load.reset()
        startTime = 0L
        lastTick = 0L
        current = 0f
        valid = false
    }

    /**
     * {@inheritDoc}
     */
    override fun toString(): String {
        val sb = StringBuilder()
        return toString(sb).toString()
    }

    /**
     * Creates a string in the form of "name [time: value, load: value]"
     */
    fun toString(sb: StringBuilder): StringBuilder {
        sb.append(name).append(": [time: ").append(time.value).append(", load: ").append(load.value).append("]")
        return sb
    }

    companion object {
        private const val nano2seconds = 1f / 1000000000.0f
    }

    init {
        time = FloatCounter(windowSize)
        load = FloatCounter(1)
    }
}
