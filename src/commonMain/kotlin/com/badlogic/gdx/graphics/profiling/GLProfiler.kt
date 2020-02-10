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
package com.badlogic.gdx.graphics.profiling

import java.lang.StackTraceElement
import java.nio.FloatBuffer
import java.nio.LongBuffer

/** When enabled, collects statistics about GL calls and checks for GL errors.
 * Enabling will wrap Gdx.gl* instances with delegate classes which provide described functionality
 * and route GL calls to the actual GL instances.
 *
 * @see GL20Interceptor
 *
 * @see GL30Interceptor
 *
 *
 * @author Daniel Holderbaum
 * @author Jan Polák
 */
class GLProfiler(graphics: com.badlogic.gdx.Graphics) {

    private val graphics: com.badlogic.gdx.Graphics
    private var glInterceptor: com.badlogic.gdx.graphics.profiling.GLInterceptor? = null
    private var listener: com.badlogic.gdx.graphics.profiling.GLErrorListener
    /** @return true if the GLProfiler is currently profiling
     */
    var isEnabled = false
        private set

    /** Enables profiling by replacing the `GL20` and `GL30` instances with profiling ones.  */
    fun enable() {
        if (isEnabled) return
        val gl30: com.badlogic.gdx.graphics.GL30 = graphics.getGL30()
        if (gl30 != null) {
            graphics.setGL30(glInterceptor as com.badlogic.gdx.graphics.GL30?)
        } else {
            graphics.setGL20(glInterceptor)
        }
        isEnabled = true
    }

    /** Disables profiling by resetting the `GL20` and `GL30` instances with the original ones.  */
    fun disable() {
        if (!isEnabled) return
        val gl30: com.badlogic.gdx.graphics.GL30 = graphics.getGL30()
        if (gl30 != null) graphics.setGL30((graphics.getGL30() as com.badlogic.gdx.graphics.profiling.GL30Interceptor).gl30) else graphics.setGL20((graphics.getGL20() as com.badlogic.gdx.graphics.profiling.GL20Interceptor).gl20)
        isEnabled = false
    }

    /** Set the current listener for the [GLProfiler] to `errorListener`  */
    fun setListener(errorListener: com.badlogic.gdx.graphics.profiling.GLErrorListener) {
        listener = errorListener
    }

    /** @return the current [GLErrorListener]
     */
    fun getListener(): com.badlogic.gdx.graphics.profiling.GLErrorListener {
        return listener
    }

    /**
     *
     * @return the total gl calls made since the last reset
     */
    val calls: Int
        get() = glInterceptor.getCalls()

    /**
     *
     * @return the total amount of texture bindings made since the last reset
     */
    val textureBindings: Int
        get() = glInterceptor.getTextureBindings()

    /**
     *
     * @return the total amount of draw calls made since the last reset
     */
    val drawCalls: Int
        get() = glInterceptor.getDrawCalls()

    /**
     *
     * @return the total amount of shader switches made since the last reset
     */
    val shaderSwitches: Int
        get() = glInterceptor.getShaderSwitches()

    /**
     *
     * @return [FloatCounter] containing information about rendered vertices since the last reset
     */
    val vertexCount: com.badlogic.gdx.math.FloatCounter
        get() = glInterceptor!!.getVertexCount()

    /** Will reset the statistical information which has been collected so far. This should be called after every frame.
     * Error listener is kept as it is.  */
    fun reset() {
        glInterceptor!!.reset()
    }

    /**
     * Create a new instance of GLProfiler to monitor a [com.badlogic.gdx.Graphics] instance's gl calls
     * @param graphics instance to monitor with this instance, With Lwjgl 2.x you can pass in Gdx.graphics, with Lwjgl3 use
     * Lwjgl3Window.getGraphics()
     */
    init {
        this.graphics = graphics
        val gl30: com.badlogic.gdx.graphics.GL30 = graphics.getGL30()
        if (gl30 != null) {
            glInterceptor = com.badlogic.gdx.graphics.profiling.GL30Interceptor(this, graphics.getGL30())
        } else {
            glInterceptor = com.badlogic.gdx.graphics.profiling.GL20Interceptor(this, graphics.getGL20())
        }
        listener = com.badlogic.gdx.graphics.profiling.GLErrorListener.Companion.LOGGING_LISTENER
    }
}
