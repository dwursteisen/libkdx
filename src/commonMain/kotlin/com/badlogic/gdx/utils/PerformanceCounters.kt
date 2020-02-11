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
 * @author xoppa
 */
class PerformanceCounters {

    private var lastTick = 0L
    val counters = Array<PerformanceCounter>()
    fun add(name: String?, windowSize: Int): PerformanceCounter {
        val result = PerformanceCounter(name!!, windowSize)
        counters.add(result)
        return result
    }

    fun add(name: String?): PerformanceCounter {
        val result = PerformanceCounter(name!!)
        counters.add(result)
        return result
    }

    fun tick() {
        val t = nanoTime()
        if (lastTick > 0L) tick((t - lastTick) * nano2seconds)
        lastTick = t
    }

    fun tick(deltaTime: Float) {
        for (i in 0 until counters.size) counters[i]!!.tick(deltaTime)
    }

    fun toString(sb: StringBuilder): StringBuilder {
        sb.setLength(0)
        for (i in 0 until counters.size) {
            if (i != 0) sb.append("; ")
            counters[i]!!.toString(sb)
        }
        return sb
    }

    companion object {
        private const val nano2seconds = 1f / 1000000000.0f
    }
}
