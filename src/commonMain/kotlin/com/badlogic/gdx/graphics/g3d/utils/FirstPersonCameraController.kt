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

/**
 * Takes a [Camera] instance and controls it via w,a,s,d and mouse panning.
 *
 * @author badlogic
 */
class FirstPersonCameraController(camera: Camera) : InputAdapter() {

    private val camera: Camera
    private val keys: IntIntMap = IntIntMap()
    private val STRAFE_LEFT: Int = Keys.A
    private val STRAFE_RIGHT: Int = Keys.D
    private val FORWARD: Int = Keys.W
    private val BACKWARD: Int = Keys.S
    private val UP: Int = Keys.Q
    private val DOWN: Int = Keys.E
    private var velocity = 5f
    private var degreesPerPixel = 0.5f
    private val tmp: Vector3 = Vector3()
    fun keyDown(keycode: Int): Boolean {
        keys.put(keycode, keycode)
        return true
    }

    fun keyUp(keycode: Int): Boolean {
        keys.remove(keycode, 0)
        return true
    }

    /**
     * Sets the velocity in units per second for moving forward, backward and strafing left/right.
     *
     * @param velocity the velocity in units per second
     */
    fun setVelocity(velocity: Float) {
        this.velocity = velocity
    }

    /**
     * Sets how many degrees to rotate per pixel the mouse moved.
     *
     * @param degreesPerPixel
     */
    fun setDegreesPerPixel(degreesPerPixel: Float) {
        this.degreesPerPixel = degreesPerPixel
    }

    fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        val deltaX: Float = -Gdx.input.getDeltaX() * degreesPerPixel
        val deltaY: Float = -Gdx.input.getDeltaY() * degreesPerPixel
        camera.direction.rotate(camera.up, deltaX)
        tmp.set(camera.direction).crs(camera.up).nor()
        camera.direction.rotate(tmp, deltaY)
        // camera.up.rotate(tmp, deltaY);
        return true
    }

    @JvmOverloads
    fun update(deltaTime: Float = Gdx.graphics.getDeltaTime()) {
        if (keys.containsKey(FORWARD)) {
            tmp.set(camera.direction).nor().scl(deltaTime * velocity)
            camera.position.add(tmp)
        }
        if (keys.containsKey(BACKWARD)) {
            tmp.set(camera.direction).nor().scl(-deltaTime * velocity)
            camera.position.add(tmp)
        }
        if (keys.containsKey(STRAFE_LEFT)) {
            tmp.set(camera.direction).crs(camera.up).nor().scl(-deltaTime * velocity)
            camera.position.add(tmp)
        }
        if (keys.containsKey(STRAFE_RIGHT)) {
            tmp.set(camera.direction).crs(camera.up).nor().scl(deltaTime * velocity)
            camera.position.add(tmp)
        }
        if (keys.containsKey(UP)) {
            tmp.set(camera.up).nor().scl(deltaTime * velocity)
            camera.position.add(tmp)
        }
        if (keys.containsKey(DOWN)) {
            tmp.set(camera.up).nor().scl(-deltaTime * velocity)
            camera.position.add(tmp)
        }
        camera.update(true)
    }

    init {
        this.camera = camera
    }
}
