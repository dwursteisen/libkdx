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

import com.badlogic.gdx.graphics.glutils.HdpiUtils.glViewport

/** Manages a [Camera] and determines how world coordinates are mapped to and from the screen.
 * @author Daniel Holderbaum
 * @author Nathan Sweet
 */
abstract class Viewport {

    private var camera: com.badlogic.gdx.graphics.Camera? = null
    /** The virtual width of this viewport in world coordinates. This width is scaled to the viewport's screen width.  */
    var worldWidth = 0f
    /** The virtual height of this viewport in world coordinates. This height is scaled to the viewport's screen height.  */
    var worldHeight = 0f
    /** Sets the viewport's offset from the left edge of the screen. This is typically set by [.update].  */
    /** Returns the left gutter (black bar) width in screen coordinates.  */
    var leftGutterWidth = 0
        get() = field
        set
    /** Sets the viewport's offset from the bottom edge of the screen. This is typically set by [.update].  */
    /** Returns the bottom gutter (black bar) height in screen coordinates.  */
    var bottomGutterHeight = 0
        get() = field
        set
    /** Sets the viewport's width in screen coordinates. This is typically set by [.update].  */
    var screenWidth = 0
    /** Sets the viewport's height in screen coordinates. This is typically set by [.update].  */
    var screenHeight = 0
    private val tmp: com.badlogic.gdx.math.Vector3 = com.badlogic.gdx.math.Vector3()
    /** Applies the viewport to the camera and sets the glViewport.
     * @param centerCamera If true, the camera position is set to the center of the world.
     */
    /** Calls [.apply] with false.  */
    @JvmOverloads
    fun apply(centerCamera: Boolean = false) {
        glViewport(leftGutterWidth, bottomGutterHeight, screenWidth, screenHeight)
        camera.viewportWidth = worldWidth
        camera.viewportHeight = worldHeight
        if (centerCamera) camera.position.set(worldWidth / 2, worldHeight / 2, 0f)
        camera.update()
    }

    /** Calls [.update] with false.  */
    fun update(screenWidth: Int, screenHeight: Int) {
        update(screenWidth, screenHeight, false)
    }

    /** Configures this viewport's screen bounds using the specified screen size and calls [.apply]. Typically called
     * from [ApplicationListener.resize] or [Screen.resize].
     *
     *
     * The default implementation only calls [.apply].  */
    open fun update(screenWidth: Int, screenHeight: Int, centerCamera: Boolean) {
        apply(centerCamera)
    }

    /** Transforms the specified screen coordinate to world coordinates.
     * @return The vector that was passed in, transformed to world coordinates.
     * @see Camera.unproject
     */
    fun unproject(screenCoords: com.badlogic.gdx.math.Vector2): com.badlogic.gdx.math.Vector2 {
        tmp.set(screenCoords.x, screenCoords.y, 1f)
        camera.unproject(tmp, leftGutterWidth.toFloat(), bottomGutterHeight.toFloat(), screenWidth.toFloat(), screenHeight.toFloat())
        screenCoords.set(tmp.x, tmp.y)
        return screenCoords
    }

    /** Transforms the specified world coordinate to screen coordinates.
     * @return The vector that was passed in, transformed to screen coordinates.
     * @see Camera.project
     */
    fun project(worldCoords: com.badlogic.gdx.math.Vector2): com.badlogic.gdx.math.Vector2 {
        tmp.set(worldCoords.x, worldCoords.y, 1f)
        camera.project(tmp, leftGutterWidth.toFloat(), bottomGutterHeight.toFloat(), screenWidth.toFloat(), screenHeight.toFloat())
        worldCoords.set(tmp.x, tmp.y)
        return worldCoords
    }

    /** Transforms the specified screen coordinate to world coordinates.
     * @return The vector that was passed in, transformed to world coordinates.
     * @see Camera.unproject
     */
    fun unproject(screenCoords: com.badlogic.gdx.math.Vector3): com.badlogic.gdx.math.Vector3 {
        camera.unproject(screenCoords, leftGutterWidth.toFloat(), bottomGutterHeight.toFloat(), screenWidth.toFloat(), screenHeight.toFloat())
        return screenCoords
    }

    /** Transforms the specified world coordinate to screen coordinates.
     * @return The vector that was passed in, transformed to screen coordinates.
     * @see Camera.project
     */
    fun project(worldCoords: com.badlogic.gdx.math.Vector3): com.badlogic.gdx.math.Vector3 {
        camera.project(worldCoords, leftGutterWidth.toFloat(), bottomGutterHeight.toFloat(), screenWidth.toFloat(), screenHeight.toFloat())
        return worldCoords
    }

    /** @see Camera.getPickRay
     */
    fun getPickRay(screenX: Float, screenY: Float): com.badlogic.gdx.math.collision.Ray {
        return camera.getPickRay(screenX, screenY, leftGutterWidth.toFloat(), bottomGutterHeight.toFloat(), screenWidth.toFloat(), screenHeight.toFloat())
    }

    /** @see ScissorStack.calculateScissors
     */
    fun calculateScissors(batchTransform: com.badlogic.gdx.math.Matrix4?, area: com.badlogic.gdx.math.Rectangle?, scissor: com.badlogic.gdx.math.Rectangle?) {
        com.badlogic.gdx.scenes.scene2d.utils.ScissorStack.calculateScissors(camera, leftGutterWidth.toFloat(), bottomGutterHeight.toFloat(), screenWidth.toFloat(), screenHeight.toFloat(), batchTransform, area, scissor)
    }

    /** Transforms a point to real screen coordinates (as opposed to OpenGL ES window coordinates), where the origin is in the top
     * left and the the y-axis is pointing downwards.  */
    fun toScreenCoordinates(worldCoords: com.badlogic.gdx.math.Vector2, transformMatrix: com.badlogic.gdx.math.Matrix4?): com.badlogic.gdx.math.Vector2 {
        tmp.set(worldCoords.x, worldCoords.y, 0f)
        tmp.mul(transformMatrix)
        camera.project(tmp)
        tmp.y = com.badlogic.gdx.Gdx.graphics.getHeight() - tmp.y
        worldCoords.x = tmp.x
        worldCoords.y = tmp.y
        return worldCoords
    }

    fun getCamera(): com.badlogic.gdx.graphics.Camera? {
        return camera
    }

    fun setCamera(camera: com.badlogic.gdx.graphics.Camera?) {
        this.camera = camera
    }

    fun setWorldSize(worldWidth: Float, worldHeight: Float) {
        this.worldWidth = worldWidth
        this.worldHeight = worldHeight
    }

    /** Sets the viewport's position in screen coordinates. This is typically set by [.update].  */
    fun setScreenPosition(screenX: Int, screenY: Int) {
        leftGutterWidth = screenX
        bottomGutterHeight = screenY
    }

    /** Sets the viewport's size in screen coordinates. This is typically set by [.update].  */
    fun setScreenSize(screenWidth: Int, screenHeight: Int) {
        this.screenWidth = screenWidth
        this.screenHeight = screenHeight
    }

    /** Sets the viewport's bounds in screen coordinates. This is typically set by [.update].  */
    fun setScreenBounds(screenX: Int, screenY: Int, screenWidth: Int, screenHeight: Int) {
        leftGutterWidth = screenX
        bottomGutterHeight = screenY
        this.screenWidth = screenWidth
        this.screenHeight = screenHeight
    }

    /** Returns the right gutter (black bar) x in screen coordinates.  */
    val rightGutterX: Int
        get() = leftGutterWidth + screenWidth

    /** Returns the right gutter (black bar) width in screen coordinates.  */
    val rightGutterWidth: Int
        get() = com.badlogic.gdx.Gdx.graphics.getWidth() - (leftGutterWidth + screenWidth)

    /** Returns the top gutter (black bar) y in screen coordinates.  */
    val topGutterY: Int
        get() = bottomGutterHeight + screenHeight

    /** Returns the top gutter (black bar) height in screen coordinates.  */
    val topGutterHeight: Int
        get() = com.badlogic.gdx.Gdx.graphics.getHeight() - (bottomGutterHeight + screenHeight)
}
