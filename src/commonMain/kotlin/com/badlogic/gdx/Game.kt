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
import java.lang.NullPointerException
import java.lang.RuntimeException

/**
 *
 *
 * An [ApplicationListener] that delegates to a [Screen]. This allows an application to easily have multiple screens.
 *
 *
 *
 * Screens are not disposed automatically. You must handle whether you want to keep screens around or dispose of them when another
 * screen is set.
 *   */
abstract class Game : ApplicationListener {

    protected var screen: Screen? = null
    fun dispose() {
        if (screen != null) screen!!.hide()
    }

    fun pause() {
        if (screen != null) screen!!.pause()
    }

    fun resume() {
        if (screen != null) screen!!.resume()
    }

    fun render() {
        if (screen != null) screen!!.render(Gdx.graphics.getDeltaTime())
    }

    fun resize(width: Int, height: Int) {
        if (screen != null) screen!!.resize(width, height)
    }

    /** Sets the current screen. [Screen.hide] is called on any old screen, and [Screen.show] is called on the new
     * screen, if any.
     * @param screen may be `null`
     */
    fun setScreen(screen: Screen?) {
        if (this.screen != null) this.screen!!.hide()
        this.screen = screen
        if (this.screen != null) {
            this.screen!!.show()
            this.screen!!.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight())
        }
    }

    /** @return the currently active [Screen].
     */
    fun getScreen(): Screen? {
        return screen
    }
}
