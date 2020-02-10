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
package com.badlogic.gdx.input

import java.io.IOException
import java.lang.Runnable

/** Sends all inputs from touch, key, accelerometer and compass to a [RemoteInput] at the given ip/port. Instantiate this and
 * call sendUpdate() periodically.
 *
 * @author mzechner
 */
class RemoteSender(ip: String, port: Int) : com.badlogic.gdx.InputProcessor {

    private var out: java.io.DataOutputStream? = null
    private var connected = false
    fun sendUpdate() {
        synchronized(this) { if (!connected) return }
        try {
            out.writeInt(ACCEL)
            out.writeFloat(com.badlogic.gdx.Gdx.input.getAccelerometerX())
            out.writeFloat(com.badlogic.gdx.Gdx.input.getAccelerometerY())
            out.writeFloat(com.badlogic.gdx.Gdx.input.getAccelerometerZ())
            out.writeInt(COMPASS)
            out.writeFloat(com.badlogic.gdx.Gdx.input.getAzimuth())
            out.writeFloat(com.badlogic.gdx.Gdx.input.getPitch())
            out.writeFloat(com.badlogic.gdx.Gdx.input.getRoll())
            out.writeInt(SIZE)
            out.writeFloat(com.badlogic.gdx.Gdx.graphics.getWidth().toFloat())
            out.writeFloat(com.badlogic.gdx.Gdx.graphics.getHeight().toFloat())
            out.writeInt(GYRO)
            out.writeFloat(com.badlogic.gdx.Gdx.input.getGyroscopeX())
            out.writeFloat(com.badlogic.gdx.Gdx.input.getGyroscopeY())
            out.writeFloat(com.badlogic.gdx.Gdx.input.getGyroscopeZ())
        } catch (t: Throwable) {
            out = null
            connected = false
        }
    }

    override fun keyDown(keycode: Int): Boolean {
        synchronized(this) { if (!connected) return false }
        try {
            out.writeInt(KEY_DOWN)
            out.writeInt(keycode)
        } catch (t: Throwable) {
            synchronized(this) { connected = false }
        }
        return false
    }

    override fun keyUp(keycode: Int): Boolean {
        synchronized(this) { if (!connected) return false }
        try {
            out.writeInt(KEY_UP)
            out.writeInt(keycode)
        } catch (t: Throwable) {
            synchronized(this) { connected = false }
        }
        return false
    }

    override fun keyTyped(character: Char): Boolean {
        synchronized(this) { if (!connected) return false }
        try {
            out.writeInt(KEY_TYPED)
            out.writeChar(character.toInt())
        } catch (t: Throwable) {
            synchronized(this) { connected = false }
        }
        return false
    }

    override fun touchDown(x: Int, y: Int, pointer: Int, button: Int): Boolean {
        synchronized(this) { if (!connected) return false }
        try {
            out.writeInt(TOUCH_DOWN)
            out.writeInt(x)
            out.writeInt(y)
            out.writeInt(pointer)
        } catch (t: Throwable) {
            synchronized(this) { connected = false }
        }
        return false
    }

    override fun touchUp(x: Int, y: Int, pointer: Int, button: Int): Boolean {
        synchronized(this) { if (!connected) return false }
        try {
            out.writeInt(TOUCH_UP)
            out.writeInt(x)
            out.writeInt(y)
            out.writeInt(pointer)
        } catch (t: Throwable) {
            synchronized(this) { connected = false }
        }
        return false
    }

    override fun touchDragged(x: Int, y: Int, pointer: Int): Boolean {
        synchronized(this) { if (!connected) return false }
        try {
            out.writeInt(TOUCH_DRAGGED)
            out.writeInt(x)
            out.writeInt(y)
            out.writeInt(pointer)
        } catch (t: Throwable) {
            synchronized(this) { connected = false }
        }
        return false
    }

    override fun mouseMoved(x: Int, y: Int): Boolean {
        return false
    }

    override fun scrolled(amount: Int): Boolean {
        return false
    }

    fun isConnected(): Boolean {
        synchronized(this) { return connected }
    }

    companion object {
        const val KEY_DOWN = 0
        const val KEY_UP = 1
        const val KEY_TYPED = 2
        const val TOUCH_DOWN = 3
        const val TOUCH_UP = 4
        const val TOUCH_DRAGGED = 5
        const val ACCEL = 6
        const val COMPASS = 7
        const val SIZE = 8
        const val GYRO = 9
    }

    init {
        try {
            val socket: java.net.Socket = java.net.Socket(ip, port)
            socket.setTcpNoDelay(true)
            socket.setSoTimeout(3000)
            out = java.io.DataOutputStream(socket.getOutputStream())
            out.writeBoolean(com.badlogic.gdx.Gdx.input.isPeripheralAvailable(com.badlogic.gdx.Input.Peripheral.MultitouchScreen))
            connected = true
            com.badlogic.gdx.Gdx.input.setInputProcessor(this)
        } catch (e: java.lang.Exception) {
            com.badlogic.gdx.Gdx.app.log("RemoteSender", "couldn't connect to $ip:$port")
        }
    }
}
