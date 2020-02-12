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

    /** @return the currently active [Screen].
     */
    var screen: Screen? = null

    override fun dispose() {
        screen?.run { hide() }
    }

    override fun pause() {
        screen?.run { pause() }
    }

    override fun resume() {
        screen?.run { resume() }
    }

    override fun render() {
        screen?.run { render(Gdx.graphics.deltaTime) }
    }

    override fun resize(width: Int, height: Int) {
        screen?.run { resize(width, height) }
    }

    /** Sets the current screen. [Screen.hide] is called on any old screen, and [Screen.show] is called on the new
     * screen, if any.
     * @param screen may be `null`
     */
    fun setScreen(screen: Screen?) {
        screen?.run { hide() }
        this.screen = screen
        screen?.run {
            show()
            resize(Gdx.graphics.width, Gdx.graphics.height)
        }
    }
}
