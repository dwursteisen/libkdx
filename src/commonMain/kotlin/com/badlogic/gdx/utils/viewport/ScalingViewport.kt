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
package com.badlogic.gdx.utils.viewport

/** A viewport that scales the world using [Scaling].
 *
 *
 * [Scaling.fit] keeps the aspect ratio by scaling the world up to fit the screen, adding black bars (letterboxing) for the
 * remaining space.
 *
 *
 * [Scaling.fill] keeps the aspect ratio by scaling the world up to take the whole screen (some of the world may be off
 * screen).
 *
 *
 * [Scaling.stretch] does not keep the aspect ratio, the world is scaled to take the whole screen.
 *
 *
 * [Scaling.none] keeps the aspect ratio by using a fixed size world (the world may not fill the screen or some of the world
 * may be off screen).
 * @author Daniel Holderbaum
 * @author Nathan Sweet
 */
open class ScalingViewport @JvmOverloads constructor(scaling: com.badlogic.gdx.utils.Scaling, worldWidth: Float, worldHeight: Float, camera: com.badlogic.gdx.graphics.Camera? = com.badlogic.gdx.graphics.OrthographicCamera()) : com.badlogic.gdx.utils.viewport.Viewport() {

    private var scaling: com.badlogic.gdx.utils.Scaling
    override fun update(screenWidth: Int, screenHeight: Int, centerCamera: Boolean) {
        val scaled: com.badlogic.gdx.math.Vector2 = scaling.apply(getWorldWidth(), getWorldHeight(), screenWidth.toFloat(), screenHeight.toFloat())
        val viewportWidth: Int = java.lang.Math.round(scaled.x)
        val viewportHeight: Int = java.lang.Math.round(scaled.y)
        // Center.
        setScreenBounds((screenWidth - viewportWidth) / 2, (screenHeight - viewportHeight) / 2, viewportWidth, viewportHeight)
        apply(centerCamera)
    }

    fun getScaling(): com.badlogic.gdx.utils.Scaling {
        return scaling
    }

    fun setScaling(scaling: com.badlogic.gdx.utils.Scaling) {
        this.scaling = scaling
    }

    /** Creates a new viewport using a new [OrthographicCamera].  */
    init {
        this.scaling = scaling
        setWorldSize(worldWidth, worldHeight)
        setCamera(camera)
    }
}
