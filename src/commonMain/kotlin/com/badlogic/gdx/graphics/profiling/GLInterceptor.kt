/*******************************************************************************
 * Copyright 2015 See AUTHORS file.
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
package com.badlogic.gdx.graphics.profiling

import java.lang.StackTraceElement
import java.nio.FloatBuffer
import java.nio.LongBuffer

abstract class GLInterceptor protected constructor(profiler: com.badlogic.gdx.graphics.profiling.GLProfiler) : com.badlogic.gdx.graphics.GL20 {
    var calls = 0
        protected set
    var textureBindings = 0
        protected set
    var drawCalls = 0
        protected set
    var shaderSwitches = 0
        protected set
    protected val vertexCount: com.badlogic.gdx.math.FloatCounter = com.badlogic.gdx.math.FloatCounter(0)
    protected var glProfiler: com.badlogic.gdx.graphics.profiling.GLProfiler

    fun getVertexCount(): com.badlogic.gdx.math.FloatCounter {
        return vertexCount
    }

    fun reset() {
        calls = 0
        textureBindings = 0
        drawCalls = 0
        shaderSwitches = 0
        vertexCount.reset()
    }

    companion object {
        fun resolveErrorNumber(error: Int): String {
            return when (error) {
                com.badlogic.gdx.graphics.GL20.GL_INVALID_VALUE -> "GL_INVALID_VALUE"
                com.badlogic.gdx.graphics.GL20.GL_INVALID_OPERATION -> "GL_INVALID_OPERATION"
                com.badlogic.gdx.graphics.GL20.GL_INVALID_FRAMEBUFFER_OPERATION -> "GL_INVALID_FRAMEBUFFER_OPERATION"
                com.badlogic.gdx.graphics.GL20.GL_INVALID_ENUM -> "GL_INVALID_ENUM"
                com.badlogic.gdx.graphics.GL20.GL_OUT_OF_MEMORY -> "GL_OUT_OF_MEMORY"
                else -> "number $error"
            }
        }
    }

    init {
        glProfiler = profiler
    }
}
