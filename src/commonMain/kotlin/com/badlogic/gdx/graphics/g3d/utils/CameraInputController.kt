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
package com.badlogic.gdx.graphics.g3d.utils

import com.badlogic.gdx.graphics.g3d.utils.AnimationController.AnimationDesc
import com.badlogic.gdx.graphics.g3d.utils.AnimationController.AnimationListener
import com.badlogic.gdx.graphics.g3d.utils.BaseAnimationController
import com.badlogic.gdx.graphics.g3d.utils.DefaultTextureBinder

class CameraInputController protected constructor(protected val gestureListener: CameraGestureListener?, camera: Camera?) : GestureDetector(gestureListener) {
    /**
     * The button for rotating the camera.
     */
    var rotateButton: Int = Buttons.LEFT

    /**
     * The angle to rotate when moved the full width or height of the screen.
     */
    var rotateAngle = 360f

    /**
     * The button for translating the camera along the up/right plane
     */
    var translateButton: Int = Buttons.RIGHT

    /**
     * The units to translate the camera when moved the full width or height of the screen.
     */
    var translateUnits = 10f // FIXME auto calculate this based on the target

    /**
     * The button for translating the camera along the direction axis
     */
    var forwardButton: Int = Buttons.MIDDLE

    /**
     * The key which must be pressed to activate rotate, translate and forward or 0 to always activate.
     */
    var activateKey = 0

    /**
     * Indicates if the activateKey is currently being pressed.
     */
    protected var activatePressed = false

    /**
     * Whether scrolling requires the activeKey to be pressed (false) or always allow scrolling (true).
     */
    var alwaysScroll = true

    /**
     * The weight for each scrolled amount.
     */
    var scrollFactor = -0.1f

    /**
     * World units per screen size
     */
    var pinchZoomFactor = 10f

    /**
     * Whether to update the camera after it has been changed.
     */
    var autoUpdate = true

    /**
     * The target to rotate around.
     */
    var target: Vector3? = Vector3()

    /**
     * Whether to update the target on translation
     */
    var translateTarget = true

    /**
     * Whether to update the target on forward
     */
    var forwardTarget = true

    /**
     * Whether to update the target on scroll
     */
    var scrollTarget = false
    var forwardKey: Int = Keys.W
    protected var forwardPressed = false
    var backwardKey: Int = Keys.S
    protected var backwardPressed = false
    var rotateRightKey: Int = Keys.A
    protected var rotateRightPressed = false
    var rotateLeftKey: Int = Keys.D
    protected var rotateLeftPressed = false

    /**
     * The camera.
     */
    var camera: Camera?

    /**
     * The current (first) button being pressed.
     */
    protected var button = -1
    private var startX = 0f
    private var startY = 0f
    private val tmpV1: Vector3? = Vector3()
    private val tmpV2: Vector3? = Vector3()

    protected class CameraGestureListener : GestureAdapter() {
        var controller: CameraInputController? = null
        private var previousZoom = 0f
        fun touchDown(x: Float, y: Float, pointer: Int, button: Int): Boolean {
            previousZoom = 0f
            return false
        }

        fun tap(x: Float, y: Float, count: Int, button: Int): Boolean {
            return false
        }

        fun longPress(x: Float, y: Float): Boolean {
            return false
        }

        fun fling(velocityX: Float, velocityY: Float, button: Int): Boolean {
            return false
        }

        fun pan(x: Float, y: Float, deltaX: Float, deltaY: Float): Boolean {
            return false
        }

        fun zoom(initialDistance: Float, distance: Float): Boolean {
            val newZoom = distance - initialDistance
            val amount = newZoom - previousZoom
            previousZoom = newZoom
            val w: Float = Gdx.graphics.getWidth()
            val h: Float = Gdx.graphics.getHeight()
            return controller!!.pinchZoom(amount / if (w > h) h else w)
        }

        fun pinch(initialPointer1: Vector2?, initialPointer2: Vector2?, pointer1: Vector2?, pointer2: Vector2?): Boolean {
            return false
        }
    }

    constructor(camera: Camera?) : this(CameraGestureListener(), camera) {}

    fun update() {
        if (rotateRightPressed || rotateLeftPressed || forwardPressed || backwardPressed) {
            val delta: Float = Gdx.graphics.getDeltaTime()
            if (rotateRightPressed) camera.rotate(camera.up, -delta * rotateAngle)
            if (rotateLeftPressed) camera.rotate(camera.up, delta * rotateAngle)
            if (forwardPressed) {
                camera.translate(tmpV1.set(camera.direction).scl(delta * translateUnits))
                if (forwardTarget) target.add(tmpV1)
            }
            if (backwardPressed) {
                camera.translate(tmpV1.set(camera.direction).scl(-delta * translateUnits))
                if (forwardTarget) target.add(tmpV1)
            }
            if (autoUpdate) camera.update()
        }
    }

    private var touched = 0
    private var multiTouch = false
    fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        touched = touched or (1 shl pointer)
        multiTouch = !MathUtils.isPowerOfTwo(touched)
        if (multiTouch) this.button = -1 else if (this.button < 0 && (activateKey == 0 || activatePressed)) {
            startX = screenX.toFloat()
            startY = screenY.toFloat()
            this.button = button
        }
        return super.touchDown(screenX, screenY, pointer, button) || activateKey == 0 || activatePressed
    }

    fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        touched = touched and -1 xor (1 shl pointer)
        multiTouch = !MathUtils.isPowerOfTwo(touched)
        if (button == this.button) this.button = -1
        return super.touchUp(screenX, screenY, pointer, button) || activatePressed
    }

    protected fun process(deltaX: Float, deltaY: Float, button: Int): Boolean {
        if (button == rotateButton) {
            tmpV1.set(camera.direction).crs(camera.up).y = 0f
            camera.rotateAround(target, tmpV1.nor(), deltaY * rotateAngle)
            camera.rotateAround(target, Vector3.Y, deltaX * -rotateAngle)
        } else if (button == translateButton) {
            camera.translate(tmpV1.set(camera.direction).crs(camera.up).nor().scl(-deltaX * translateUnits))
            camera.translate(tmpV2.set(camera.up).scl(-deltaY * translateUnits))
            if (translateTarget) target.add(tmpV1).add(tmpV2)
        } else if (button == forwardButton) {
            camera.translate(tmpV1.set(camera.direction).scl(deltaY * translateUnits))
            if (forwardTarget) target.add(tmpV1)
        }
        if (autoUpdate) camera.update()
        return true
    }

    fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        val result: Boolean = super.touchDragged(screenX, screenY, pointer)
        if (result || button < 0) return result
        val deltaX: Float = (screenX - startX) / Gdx.graphics.getWidth()
        val deltaY: Float = (startY - screenY) / Gdx.graphics.getHeight()
        startX = screenX.toFloat()
        startY = screenY.toFloat()
        return process(deltaX, deltaY, button)
    }

    fun scrolled(amount: Int): Boolean {
        return zoom(amount * scrollFactor * translateUnits)
    }

    fun zoom(amount: Float): Boolean {
        if (!alwaysScroll && activateKey != 0 && !activatePressed) return false
        camera.translate(tmpV1.set(camera.direction).scl(amount))
        if (scrollTarget) target.add(tmpV1)
        if (autoUpdate) camera.update()
        return true
    }

    protected fun pinchZoom(amount: Float): Boolean {
        return zoom(pinchZoomFactor * amount)
    }

    fun keyDown(keycode: Int): Boolean {
        if (keycode == activateKey) activatePressed = true
        if (keycode == forwardKey) forwardPressed = true else if (keycode == backwardKey) backwardPressed = true else if (keycode == rotateRightKey) rotateRightPressed = true else if (keycode == rotateLeftKey) rotateLeftPressed = true
        return false
    }

    fun keyUp(keycode: Int): Boolean {
        if (keycode == activateKey) {
            activatePressed = false
            button = -1
        }
        if (keycode == forwardKey) forwardPressed = false else if (keycode == backwardKey) backwardPressed = false else if (keycode == rotateRightKey) rotateRightPressed = false else if (keycode == rotateLeftKey) rotateLeftPressed = false
        return false
    }

    init {
        gestureListener!!.controller = this
        this.camera = camera
    }
}
