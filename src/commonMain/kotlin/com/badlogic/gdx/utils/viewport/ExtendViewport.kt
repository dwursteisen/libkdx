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

/** A viewport that keeps the world aspect ratio by extending the world in one direction. The world is first scaled to fit within
 * the viewport, then the shorter dimension is lengthened to fill the viewport. A maximum size can be specified to limit how much
 * the world is extended and black bars (letterboxing) are used for any remaining space.
 * @author Nathan Sweet
 */
class ExtendViewport @JvmOverloads constructor(var minWorldWidth: Float, var minWorldHeight: Float, var maxWorldWidth: Float = 0f, var maxWorldHeight: Float = 0f, camera: com.badlogic.gdx.graphics.Camera? = com.badlogic.gdx.graphics.OrthographicCamera()) : com.badlogic.gdx.utils.viewport.Viewport() {

    /** Creates a new viewport with no maximum world size.  */
    constructor(minWorldWidth: Float, minWorldHeight: Float, camera: com.badlogic.gdx.graphics.Camera?) : this(minWorldWidth, minWorldHeight, 0f, 0f, camera) {}

    override fun update(screenWidth: Int, screenHeight: Int, centerCamera: Boolean) { // Fit min size to the screen.
        var worldWidth = minWorldWidth
        var worldHeight = minWorldHeight
        val scaled: com.badlogic.gdx.math.Vector2 = com.badlogic.gdx.utils.Scaling.fit.apply(worldWidth, worldHeight, screenWidth.toFloat(), screenHeight.toFloat())
        // Extend in the short direction.
        var viewportWidth: Int = java.lang.Math.round(scaled.x)
        var viewportHeight: Int = java.lang.Math.round(scaled.y)
        if (viewportWidth < screenWidth) {
            val toViewportSpace = viewportHeight / worldHeight
            val toWorldSpace = worldHeight / viewportHeight
            var lengthen = (screenWidth - viewportWidth) * toWorldSpace
            if (maxWorldWidth > 0) lengthen = java.lang.Math.min(lengthen, maxWorldWidth - minWorldWidth)
            worldWidth += lengthen
            viewportWidth += java.lang.Math.round(lengthen * toViewportSpace)
        } else if (viewportHeight < screenHeight) {
            val toViewportSpace = viewportWidth / worldWidth
            val toWorldSpace = worldWidth / viewportWidth
            var lengthen = (screenHeight - viewportHeight) * toWorldSpace
            if (maxWorldHeight > 0) lengthen = java.lang.Math.min(lengthen, maxWorldHeight - minWorldHeight)
            worldHeight += lengthen
            viewportHeight += java.lang.Math.round(lengthen * toViewportSpace)
        }
        setWorldSize(worldWidth, worldHeight)
        // Center.
        setScreenBounds((screenWidth - viewportWidth) / 2, (screenHeight - viewportHeight) / 2, viewportWidth, viewportHeight)
        apply(centerCamera)
    }

    /** Creates a new viewport with a maximum world size.
     * @param maxWorldWidth User 0 for no maximum width.
     * @param maxWorldHeight User 0 for no maximum height.
     */
    /** Creates a new viewport using a new [OrthographicCamera] with no maximum world size.  */
    /** Creates a new viewport using a new [OrthographicCamera] and a maximum world size.
     * @see ExtendViewport.ExtendViewport
     */
    init {
        setCamera(camera)
    }
}
