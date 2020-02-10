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
 * Represents one of many application screens, such as a main menu, a settings menu, the game screen and so on.
 *
 *
 *
 * Note that [.dispose] is not called automatically.
 *
 * @see Game
 */
interface Screen {

    /** Called when this screen becomes the current screen for a [Game].  */
    fun show()

    /** Called when the screen should render itself.
     * @param delta The time in seconds since the last render.
     */
    fun render(delta: Float)

    /** @see ApplicationListener.resize
     */
    fun resize(width: Int, height: Int)

    /** @see ApplicationListener.pause
     */
    fun pause()

    /** @see ApplicationListener.resume
     */
    fun resume()

    /** Called when this screen is no longer the current screen for a [Game].  */
    fun hide()

    /** Called when this screen should release all resources.  */
    fun dispose()
}
