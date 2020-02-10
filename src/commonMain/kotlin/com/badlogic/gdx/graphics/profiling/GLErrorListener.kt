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

/** Listener for GL errors detected by [GLProfiler].
 *
 * @see GLProfiler
 *
 * @author Jan Polák
 */
interface GLErrorListener {

    /** Put your error logging code here.
     * @see GLInterceptor.resolveErrorNumber
     */
    fun onError(error: Int)

    companion object {
        // Basic implementations
        /** Listener that will log using Gdx.app.error GL error name and GL function.  */
        val LOGGING_LISTENER: GLErrorListener = object : GLErrorListener {
            override fun onError(error: Int) {
                var place: String? = null
                try {
                    val stack: Array<StackTraceElement> = java.lang.Thread.currentThread().getStackTrace()
                    for (i in stack.indices) {
                        if ("check" == stack[i].getMethodName()) {
                            if (i + 1 < stack.size) {
                                val glMethod: StackTraceElement = stack[i + 1]
                                place = glMethod.getMethodName()
                            }
                            break
                        }
                    }
                } catch (ignored: java.lang.Exception) {
                }
                if (place != null) {
                    com.badlogic.gdx.Gdx.app.error("GLProfiler", "Error " + com.badlogic.gdx.graphics.profiling.GLInterceptor.Companion.resolveErrorNumber(error) + " from " + place)
                } else {
                    com.badlogic.gdx.Gdx.app.error("GLProfiler", "Error " + com.badlogic.gdx.graphics.profiling.GLInterceptor.Companion.resolveErrorNumber(error) + " at: ", java.lang.Exception())
                    // This will capture current stack trace for logging, if possible
                }
            }
        }
        /** Listener that will throw a GdxRuntimeException with error name.  */
        val THROWING_LISTENER: GLErrorListener = object : GLErrorListener {
            override fun onError(error: Int) {
                throw com.badlogic.gdx.utils.GdxRuntimeException("GLProfiler: Got GL error " + com.badlogic.gdx.graphics.profiling.GLInterceptor.Companion.resolveErrorNumber(error))
            }
        }
    }
}
