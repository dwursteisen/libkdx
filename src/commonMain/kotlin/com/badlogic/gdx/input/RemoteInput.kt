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

/**
 *
 *
 * An [Input] implementation that receives touch, key, accelerometer and compass events from a remote Android device. Just
 * instantiate it and specify the port it should listen on for incoming connections (default 8190). Then store the new RemoteInput
 * instance in Gdx.input. That's it.
 *
 *
 *
 *
 * On your Android device you can use the gdx-remote application available on the Google Code page as an APK or in SVN
 * (extensions/gdx-remote). Open it, specify the IP address and the port of the PC your libgdx app is running on and then tap
 * away.
 *
 *
 *
 *
 * The touch coordinates will be translated to the desktop window's coordinate system, no matter the orientation of the device
 *
 *
 * @author mzechner
 */
class RemoteInput @JvmOverloads constructor(port: Int = DEFAULT_PORT, private val listener: RemoteInputListener? = null) : Runnable, com.badlogic.gdx.Input {

    interface RemoteInputListener {
        fun onConnected()
        fun onDisconnected()
    }

    internal inner class KeyEvent {
        var timeStamp: Long = 0
        var type = 0
        var keyCode = 0
        var keyChar = 0.toChar()

        companion object {
            const val KEY_DOWN = 0
            const val KEY_UP = 1
            const val KEY_TYPED = 2
        }
    }

    internal inner class TouchEvent {
        var timeStamp: Long = 0
        var type = 0
        var x = 0
        var y = 0
        var pointer = 0

        companion object {
            const val TOUCH_DOWN = 0
            const val TOUCH_UP = 1
            const val TOUCH_DRAGGED = 2
        }
    }

    internal inner class EventTrigger(var touchEvent: TouchEvent?, var keyEvent: KeyEvent?) : Runnable {
        override fun run() {
            justTouched = false
            if (keyJustPressed) {
                keyJustPressed = false
                for (i in justPressedKeys.indices) {
                    justPressedKeys[i] = false
                }
            }
            if (processor != null) {
                if (touchEvent != null) {
                    when (touchEvent!!.type) {
                        TouchEvent.Companion.TOUCH_DOWN -> {
                            deltaX[touchEvent!!.pointer] = 0
                            deltaY[touchEvent!!.pointer] = 0
                            processor.touchDown(touchEvent!!.x, touchEvent!!.y, touchEvent!!.pointer, com.badlogic.gdx.Input.Buttons.LEFT)
                            isTouched[touchEvent!!.pointer] = true
                            justTouched = true
                        }
                        TouchEvent.Companion.TOUCH_UP -> {
                            deltaX[touchEvent!!.pointer] = 0
                            deltaY[touchEvent!!.pointer] = 0
                            processor.touchUp(touchEvent!!.x, touchEvent!!.y, touchEvent!!.pointer, com.badlogic.gdx.Input.Buttons.LEFT)
                            isTouched[touchEvent!!.pointer] = false
                        }
                        TouchEvent.Companion.TOUCH_DRAGGED -> {
                            deltaX[touchEvent!!.pointer] = touchEvent!!.x - touchX[touchEvent!!.pointer]
                            deltaY[touchEvent!!.pointer] = touchEvent!!.y - touchY[touchEvent!!.pointer]
                            processor.touchDragged(touchEvent!!.x, touchEvent!!.y, touchEvent!!.pointer)
                        }
                    }
                    touchX[touchEvent!!.pointer] = touchEvent!!.x
                    touchY[touchEvent!!.pointer] = touchEvent!!.y
                }
                if (keyEvent != null) {
                    when (keyEvent!!.type) {
                        KeyEvent.Companion.KEY_DOWN -> {
                            processor.keyDown(keyEvent!!.keyCode)
                            if (!keys[keyEvent!!.keyCode]) {
                                keyCount++
                                keys[keyEvent!!.keyCode] = true
                            }
                            keyJustPressed = true
                            justPressedKeys[keyEvent!!.keyCode] = true
                        }
                        KeyEvent.Companion.KEY_UP -> {
                            processor.keyUp(keyEvent!!.keyCode)
                            if (keys[keyEvent!!.keyCode]) {
                                keyCount--
                                keys[keyEvent!!.keyCode] = false
                            }
                        }
                        KeyEvent.Companion.KEY_TYPED -> processor.keyTyped(keyEvent!!.keyChar)
                    }
                }
            } else {
                if (touchEvent != null) {
                    when (touchEvent!!.type) {
                        TouchEvent.Companion.TOUCH_DOWN -> {
                            deltaX[touchEvent!!.pointer] = 0
                            deltaY[touchEvent!!.pointer] = 0
                            isTouched[touchEvent!!.pointer] = true
                            justTouched = true
                        }
                        TouchEvent.Companion.TOUCH_UP -> {
                            deltaX[touchEvent!!.pointer] = 0
                            deltaY[touchEvent!!.pointer] = 0
                            isTouched[touchEvent!!.pointer] = false
                        }
                        TouchEvent.Companion.TOUCH_DRAGGED -> {
                            deltaX[touchEvent!!.pointer] = touchEvent!!.x - touchX[touchEvent!!.pointer]
                            deltaY[touchEvent!!.pointer] = touchEvent!!.y - touchY[touchEvent!!.pointer]
                        }
                    }
                    touchX[touchEvent!!.pointer] = touchEvent!!.x
                    touchY[touchEvent!!.pointer] = touchEvent!!.y
                }
                if (keyEvent != null) {
                    if (keyEvent!!.type == KeyEvent.Companion.KEY_DOWN) {
                        if (!keys[keyEvent!!.keyCode]) {
                            keyCount++
                            keys[keyEvent!!.keyCode] = true
                        }
                        keyJustPressed = true
                        justPressedKeys[keyEvent!!.keyCode] = true
                    }
                    if (keyEvent!!.type == KeyEvent.Companion.KEY_UP) {
                        if (keys[keyEvent!!.keyCode]) {
                            keyCount--
                            keys[keyEvent!!.keyCode] = false
                        }
                    }
                }
            }
        }
    }

    private var serverSocket: java.net.ServerSocket? = null
    private val accel = FloatArray(3)
    private val gyrate = FloatArray(3)
    private val compass = FloatArray(3)
    private var multiTouch = false
    private var remoteWidth = 0f
    private var remoteHeight = 0f
    var isConnected = false
        private set
    var keyCount = 0
    var keys = BooleanArray(256)
    var keyJustPressed = false
    var justPressedKeys = BooleanArray(256)
    var deltaX = IntArray(maxPointers)
    var deltaY = IntArray(maxPointers)
    var touchX = IntArray(maxPointers)
    var touchY = IntArray(maxPointers)
    var isTouched = BooleanArray(maxPointers)
    var justTouched = false
    var processor: com.badlogic.gdx.InputProcessor? = null
    private val port = 0
    /** @return the IP addresses [RemoteSender] or gdx-remote should connect to. Most likely the LAN addresses if behind a NAT.
     */
    val iPs: Array<String?>

    constructor(listener: RemoteInputListener?) : this(DEFAULT_PORT, listener) {}

    override fun run() {
        while (true) {
            try {
                isConnected = false
                listener?.onDisconnected()
                println("listening, port $port")
                var socket: java.net.Socket? = null
                socket = serverSocket.accept()
                socket.setTcpNoDelay(true)
                socket.setSoTimeout(3000)
                isConnected = true
                listener?.onConnected()
                val `in`: java.io.DataInputStream = java.io.DataInputStream(socket.getInputStream())
                multiTouch = `in`.readBoolean()
                while (true) {
                    val event: Int = `in`.readInt()
                    var keyEvent: KeyEvent? = null
                    var touchEvent: TouchEvent? = null
                    when (event) {
                        com.badlogic.gdx.input.RemoteSender.Companion.ACCEL -> {
                            accel[0] = `in`.readFloat()
                            accel[1] = `in`.readFloat()
                            accel[2] = `in`.readFloat()
                        }
                        com.badlogic.gdx.input.RemoteSender.Companion.COMPASS -> {
                            compass[0] = `in`.readFloat()
                            compass[1] = `in`.readFloat()
                            compass[2] = `in`.readFloat()
                        }
                        com.badlogic.gdx.input.RemoteSender.Companion.SIZE -> {
                            remoteWidth = `in`.readFloat()
                            remoteHeight = `in`.readFloat()
                        }
                        com.badlogic.gdx.input.RemoteSender.Companion.GYRO -> {
                            gyrate[0] = `in`.readFloat()
                            gyrate[1] = `in`.readFloat()
                            gyrate[2] = `in`.readFloat()
                        }
                        com.badlogic.gdx.input.RemoteSender.Companion.KEY_DOWN -> {
                            keyEvent = KeyEvent()
                            keyEvent.keyCode = `in`.readInt()
                            keyEvent.type = KeyEvent.Companion.KEY_DOWN
                        }
                        com.badlogic.gdx.input.RemoteSender.Companion.KEY_UP -> {
                            keyEvent = KeyEvent()
                            keyEvent.keyCode = `in`.readInt()
                            keyEvent.type = KeyEvent.Companion.KEY_UP
                        }
                        com.badlogic.gdx.input.RemoteSender.Companion.KEY_TYPED -> {
                            keyEvent = KeyEvent()
                            keyEvent.keyChar = `in`.readChar()
                            keyEvent.type = KeyEvent.Companion.KEY_TYPED
                        }
                        com.badlogic.gdx.input.RemoteSender.Companion.TOUCH_DOWN -> {
                            touchEvent = TouchEvent()
                            touchEvent.x = (`in`.readInt() / remoteWidth * com.badlogic.gdx.Gdx.graphics.getWidth())
                            touchEvent.y = (`in`.readInt() / remoteHeight * com.badlogic.gdx.Gdx.graphics.getHeight())
                            touchEvent.pointer = `in`.readInt()
                            touchEvent.type = TouchEvent.Companion.TOUCH_DOWN
                        }
                        com.badlogic.gdx.input.RemoteSender.Companion.TOUCH_UP -> {
                            touchEvent = TouchEvent()
                            touchEvent.x = (`in`.readInt() / remoteWidth * com.badlogic.gdx.Gdx.graphics.getWidth())
                            touchEvent.y = (`in`.readInt() / remoteHeight * com.badlogic.gdx.Gdx.graphics.getHeight())
                            touchEvent.pointer = `in`.readInt()
                            touchEvent.type = TouchEvent.Companion.TOUCH_UP
                        }
                        com.badlogic.gdx.input.RemoteSender.Companion.TOUCH_DRAGGED -> {
                            touchEvent = TouchEvent()
                            touchEvent.x = (`in`.readInt() / remoteWidth * com.badlogic.gdx.Gdx.graphics.getWidth())
                            touchEvent.y = (`in`.readInt() / remoteHeight * com.badlogic.gdx.Gdx.graphics.getHeight())
                            touchEvent.pointer = `in`.readInt()
                            touchEvent.type = TouchEvent.Companion.TOUCH_DRAGGED
                        }
                    }
                    com.badlogic.gdx.Gdx.app.postRunnable(EventTrigger(touchEvent, keyEvent))
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    val accelerometerX: Float
        get() = accel[0]

    val accelerometerY: Float
        get() = accel[1]

    val accelerometerZ: Float
        get() = accel[2]

    val gyroscopeX: Float
        get() = gyrate[0]

    val gyroscopeY: Float
        get() = gyrate[1]

    val gyroscopeZ: Float
        get() = gyrate[2]

    val x: Int
        get() = touchX[0]

    override fun getX(pointer: Int): Int {
        return touchX[pointer]
    }

    val y: Int
        get() = touchY[0]

    override fun getY(pointer: Int): Int {
        return touchY[pointer]
    }

    override fun isTouched(): Boolean {
        return isTouched[0]
    }

    override fun justTouched(): Boolean {
        return justTouched
    }

    override fun isTouched(pointer: Int): Boolean {
        return isTouched[pointer]
    }

    val pressure: Float
        get() = getPressure(0)

    override fun getPressure(pointer: Int): Float {
        return if (isTouched(pointer)) 1 else 0
    }

    override fun isButtonPressed(button: Int): Boolean {
        if (button != com.badlogic.gdx.Input.Buttons.LEFT) return false
        for (i in isTouched.indices) if (isTouched[i]) return true
        return false
    }

    override fun isButtonJustPressed(button: Int): Boolean {
        return button == com.badlogic.gdx.Input.Buttons.LEFT && justTouched
    }

    override fun isKeyPressed(key: Int): Boolean {
        if (key == com.badlogic.gdx.Input.Keys.ANY_KEY) {
            return keyCount > 0
        }
        return if (key < 0 || key > 255) {
            false
        } else keys[key]
    }

    override fun isKeyJustPressed(key: Int): Boolean {
        if (key == com.badlogic.gdx.Input.Keys.ANY_KEY) {
            return keyJustPressed
        }
        return if (key < 0 || key > 255) {
            false
        } else justPressedKeys[key]
    }

    override fun getTextInput(listener: com.badlogic.gdx.Input.TextInputListener, title: String, text: String, hint: String) {
        com.badlogic.gdx.Gdx.app.getInput().getTextInput(listener, title, text, hint)
    }

    override fun setOnscreenKeyboardVisible(visible: Boolean) {}
    override fun vibrate(milliseconds: Int) {}
    override fun vibrate(pattern: LongArray, repeat: Int) {}
    override fun cancelVibrate() {}
    val azimuth: Float
        get() = compass[0]

    val pitch: Float
        get() = compass[1]

    val roll: Float
        get() = compass[2]

    var isCatchBackKey: Boolean
        get() = false
        set(catchBack) {}

    var isCatchMenuKey: Boolean
        get() = false
        set(catchMenu) {}

    override fun setCatchKey(keycode: Int, catchKey: Boolean) {}
    override fun isCatchKey(keycode: Int): Boolean {
        return false
    }

    var inputProcessor: com.badlogic.gdx.InputProcessor
        get() = processor
        set(processor) {
            this.processor = processor
        }

    override fun isPeripheralAvailable(peripheral: com.badlogic.gdx.Input.Peripheral): Boolean {
        if (peripheral == com.badlogic.gdx.Input.Peripheral.Accelerometer) return true
        if (peripheral == com.badlogic.gdx.Input.Peripheral.Compass) return true
        return if (peripheral == com.badlogic.gdx.Input.Peripheral.MultitouchScreen) multiTouch else false
    }

    val rotation: Int
        get() = 0

    val nativeOrientation: com.badlogic.gdx.Input.Orientation
        get() = com.badlogic.gdx.Input.Orientation.Landscape

    var isCursorCatched: Boolean
        get() = false
        set(catched) {}

    override fun getDeltaX(): Int {
        return deltaX[0]
    }

    override fun getDeltaX(pointer: Int): Int {
        return deltaX[pointer]
    }

    override fun getDeltaY(): Int {
        return deltaY[0]
    }

    override fun getDeltaY(pointer: Int): Int {
        return deltaY[pointer]
    }

    override fun setCursorPosition(x: Int, y: Int) {}
    // TODO Auto-generated method stub
    val currentEventTime: Long
        get() =// TODO Auto-generated method stub
            0

    override fun getRotationMatrix(matrix: FloatArray) { // TODO Auto-generated method stub
    }

    companion object {
        const val maxPointers = 20
        var DEFAULT_PORT = 8190
    }

    init {
        try {
            this.port = port
            serverSocket = java.net.ServerSocket(port)
            val thread: java.lang.Thread = java.lang.Thread(this)
            thread.setDaemon(true)
            thread.start()
            val allByName: Array<java.net.InetAddress> = java.net.InetAddress.getAllByName(java.net.InetAddress.getLocalHost().getHostName())
            iPs = arrayOfNulls(allByName.size)
            for (i in allByName.indices) {
                iPs[i] = allByName[i].getHostAddress()
            }
        } catch (e: java.lang.Exception) {
            throw com.badlogic.gdx.utils.GdxRuntimeException("Couldn't open listening socket at port '$port'", e)
        }
    }
}
