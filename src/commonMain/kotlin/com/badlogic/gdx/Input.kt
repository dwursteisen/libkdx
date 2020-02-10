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
import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.InputEventQueue
import com.badlogic.gdx.utils.ObjectIntMap

/**
 *
 *
 * Interface to the input facilities. This allows polling the state of the keyboard, the touch screen and the accelerometer. On
 * some backends (desktop, gwt, etc) the touch screen is replaced by mouse input. The accelerometer is of course not available on
 * all backends.
 *
 *
 *
 *
 * Instead of polling for events, one can process all input events with an [InputProcessor]. You can set the InputProcessor
 * via the [.setInputProcessor] method. It will be called before the [ApplicationListener.render]
 * method in each frame.
 *
 *
 *
 *
 * Keyboard keys are translated to the constants in [Keys] transparently on all systems. Do not use system specific key
 * constants.
 *
 *
 *
 *
 * The class also offers methods to use (and test for the presence of) other input systems like vibration, compass, on-screen
 * keyboards, and cursor capture. Support for simple input dialogs is also provided.
 *
 *
 * @author mzechner
 */
interface Input {

    /** Callback interface for [Input.getTextInput]
     *
     * @author mzechner
     */
    interface TextInputListener {

        fun input(text: String?)
        fun canceled()
    }

    /** Mouse buttons.
     * @author mzechner
     */
    object Buttons {

        const val LEFT = 0
        const val RIGHT = 1
        const val MIDDLE = 2
        const val BACK = 3
        const val FORWARD = 4
    }

    /** Keys.
     *
     * @author mzechner
     */
    object Keys {

        const val ANY_KEY = -1
        const val NUM_0 = 7
        const val NUM_1 = 8
        const val NUM_2 = 9
        const val NUM_3 = 10
        const val NUM_4 = 11
        const val NUM_5 = 12
        const val NUM_6 = 13
        const val NUM_7 = 14
        const val NUM_8 = 15
        const val NUM_9 = 16
        const val A = 29
        const val ALT_LEFT = 57
        const val ALT_RIGHT = 58
        const val APOSTROPHE = 75
        const val AT = 77
        const val B = 30
        const val BACK = 4
        const val BACKSLASH = 73
        const val C = 31
        const val CALL = 5
        const val CAMERA = 27
        const val CLEAR = 28
        const val COMMA = 55
        const val D = 32
        const val DEL = 67
        const val BACKSPACE = 67
        const val FORWARD_DEL = 112
        const val DPAD_CENTER = 23
        const val DPAD_DOWN = 20
        const val DPAD_LEFT = 21
        const val DPAD_RIGHT = 22
        const val DPAD_UP = 19
        const val CENTER = 23
        const val DOWN = 20
        const val LEFT = 21
        const val RIGHT = 22
        const val UP = 19
        const val E = 33
        const val ENDCALL = 6
        const val ENTER = 66
        const val ENVELOPE = 65
        const val EQUALS = 70
        const val EXPLORER = 64
        const val F = 34
        const val FOCUS = 80
        const val G = 35
        const val GRAVE = 68
        const val H = 36
        const val HEADSETHOOK = 79
        const val HOME = 3
        const val I = 37
        const val J = 38
        const val K = 39
        const val L = 40
        const val LEFT_BRACKET = 71
        const val M = 41
        const val MEDIA_FAST_FORWARD = 90
        const val MEDIA_NEXT = 87
        const val MEDIA_PLAY_PAUSE = 85
        const val MEDIA_PREVIOUS = 88
        const val MEDIA_REWIND = 89
        const val MEDIA_STOP = 86
        const val MENU = 82
        const val MINUS = 69
        const val MUTE = 91
        const val N = 42
        const val NOTIFICATION = 83
        const val NUM = 78
        const val O = 43
        const val P = 44
        const val PERIOD = 56
        const val PLUS = 81
        const val POUND = 18
        const val POWER = 26
        const val Q = 45
        const val R = 46
        const val RIGHT_BRACKET = 72
        const val S = 47
        const val SEARCH = 84
        const val SEMICOLON = 74
        const val SHIFT_LEFT = 59
        const val SHIFT_RIGHT = 60
        const val SLASH = 76
        const val SOFT_LEFT = 1
        const val SOFT_RIGHT = 2
        const val SPACE = 62
        const val STAR = 17
        const val SYM = 63
        const val T = 48
        const val TAB = 61
        const val U = 49
        const val UNKNOWN = 0
        const val V = 50
        const val VOLUME_DOWN = 25
        const val VOLUME_UP = 24
        const val W = 51
        const val X = 52
        const val Y = 53
        const val Z = 54
        const val META_ALT_LEFT_ON = 16
        const val META_ALT_ON = 2
        const val META_ALT_RIGHT_ON = 32
        const val META_SHIFT_LEFT_ON = 64
        const val META_SHIFT_ON = 1
        const val META_SHIFT_RIGHT_ON = 128
        const val META_SYM_ON = 4
        const val CONTROL_LEFT = 129
        const val CONTROL_RIGHT = 130
        const val ESCAPE = 131
        const val END = 132
        const val INSERT = 133
        const val PAGE_UP = 92
        const val PAGE_DOWN = 93
        const val PICTSYMBOLS = 94
        const val SWITCH_CHARSET = 95
        const val BUTTON_CIRCLE = 255
        const val BUTTON_A = 96
        const val BUTTON_B = 97
        const val BUTTON_C = 98
        const val BUTTON_X = 99
        const val BUTTON_Y = 100
        const val BUTTON_Z = 101
        const val BUTTON_L1 = 102
        const val BUTTON_R1 = 103
        const val BUTTON_L2 = 104
        const val BUTTON_R2 = 105
        const val BUTTON_THUMBL = 106
        const val BUTTON_THUMBR = 107
        const val BUTTON_START = 108
        const val BUTTON_SELECT = 109
        const val BUTTON_MODE = 110
        const val NUMPAD_0 = 144
        const val NUMPAD_1 = 145
        const val NUMPAD_2 = 146
        const val NUMPAD_3 = 147
        const val NUMPAD_4 = 148
        const val NUMPAD_5 = 149
        const val NUMPAD_6 = 150
        const val NUMPAD_7 = 151
        const val NUMPAD_8 = 152
        const val NUMPAD_9 = 153

        // public static final int BACKTICK = 0;
        // public static final int TILDE = 0;
        // public static final int UNDERSCORE = 0;
        // public static final int DOT = 0;
        // public static final int BREAK = 0;
        // public static final int PIPE = 0;
        // public static final int EXCLAMATION = 0;
        // public static final int QUESTIONMARK = 0;
        // ` | VK_BACKTICK
        // ~ | VK_TILDE
        // : | VK_COLON
        // _ | VK_UNDERSCORE
        // . | VK_DOT
        // (break) | VK_BREAK
        // | | VK_PIPE
        // ! | VK_EXCLAMATION
        // ? | VK_QUESTION
        const val COLON = 243
        const val F1 = 244
        const val F2 = 245
        const val F3 = 246
        const val F4 = 247
        const val F5 = 248
        const val F6 = 249
        const val F7 = 250
        const val F8 = 251
        const val F9 = 252
        const val F10 = 253
        const val F11 = 254
        const val F12 = 255

        /** @return a human readable representation of the keycode. The returned value can be used in
         * [Input.Keys.valueOf]
         */
        fun toString(keycode: Int): String? {
            if (keycode < 0) throw IllegalArgumentException("keycode cannot be negative, keycode: $keycode")
            if (keycode > 255) throw IllegalArgumentException("keycode cannot be greater than 255, keycode: $keycode")
            return when (keycode) {
                UNKNOWN -> "Unknown"
                SOFT_LEFT -> "Soft Left"
                SOFT_RIGHT -> "Soft Right"
                HOME -> "Home"
                BACK -> "Back"
                CALL -> "Call"
                ENDCALL -> "End Call"
                NUM_0 -> "0"
                NUM_1 -> "1"
                NUM_2 -> "2"
                NUM_3 -> "3"
                NUM_4 -> "4"
                NUM_5 -> "5"
                NUM_6 -> "6"
                NUM_7 -> "7"
                NUM_8 -> "8"
                NUM_9 -> "9"
                STAR -> "*"
                POUND -> "#"
                UP -> "Up"
                DOWN -> "Down"
                LEFT -> "Left"
                RIGHT -> "Right"
                CENTER -> "Center"
                VOLUME_UP -> "Volume Up"
                VOLUME_DOWN -> "Volume Down"
                POWER -> "Power"
                CAMERA -> "Camera"
                CLEAR -> "Clear"
                A -> "A"
                B -> "B"
                C -> "C"
                D -> "D"
                E -> "E"
                F -> "F"
                G -> "G"
                H -> "H"
                I -> "I"
                J -> "J"
                K -> "K"
                L -> "L"
                M -> "M"
                N -> "N"
                O -> "O"
                P -> "P"
                Q -> "Q"
                R -> "R"
                S -> "S"
                T -> "T"
                U -> "U"
                V -> "V"
                W -> "W"
                X -> "X"
                Y -> "Y"
                Z -> "Z"
                COMMA -> ","
                PERIOD -> "."
                ALT_LEFT -> "L-Alt"
                ALT_RIGHT -> "R-Alt"
                SHIFT_LEFT -> "L-Shift"
                SHIFT_RIGHT -> "R-Shift"
                TAB -> "Tab"
                SPACE -> "Space"
                SYM -> "SYM"
                EXPLORER -> "Explorer"
                ENVELOPE -> "Envelope"
                ENTER -> "Enter"
                DEL -> "Delete" // also BACKSPACE
                GRAVE -> "`"
                MINUS -> "-"
                EQUALS -> "="
                LEFT_BRACKET -> "["
                RIGHT_BRACKET -> "]"
                BACKSLASH -> "\\"
                SEMICOLON -> ";"
                APOSTROPHE -> "'"
                SLASH -> "/"
                AT -> "@"
                NUM -> "Num"
                HEADSETHOOK -> "Headset Hook"
                FOCUS -> "Focus"
                PLUS -> "Plus"
                MENU -> "Menu"
                NOTIFICATION -> "Notification"
                SEARCH -> "Search"
                MEDIA_PLAY_PAUSE -> "Play/Pause"
                MEDIA_STOP -> "Stop Media"
                MEDIA_NEXT -> "Next Media"
                MEDIA_PREVIOUS -> "Prev Media"
                MEDIA_REWIND -> "Rewind"
                MEDIA_FAST_FORWARD -> "Fast Forward"
                MUTE -> "Mute"
                PAGE_UP -> "Page Up"
                PAGE_DOWN -> "Page Down"
                PICTSYMBOLS -> "PICTSYMBOLS"
                SWITCH_CHARSET -> "SWITCH_CHARSET"
                BUTTON_A -> "A Button"
                BUTTON_B -> "B Button"
                BUTTON_C -> "C Button"
                BUTTON_X -> "X Button"
                BUTTON_Y -> "Y Button"
                BUTTON_Z -> "Z Button"
                BUTTON_L1 -> "L1 Button"
                BUTTON_R1 -> "R1 Button"
                BUTTON_L2 -> "L2 Button"
                BUTTON_R2 -> "R2 Button"
                BUTTON_THUMBL -> "Left Thumb"
                BUTTON_THUMBR -> "Right Thumb"
                BUTTON_START -> "Start"
                BUTTON_SELECT -> "Select"
                BUTTON_MODE -> "Button Mode"
                FORWARD_DEL -> "Forward Delete"
                CONTROL_LEFT -> "L-Ctrl"
                CONTROL_RIGHT -> "R-Ctrl"
                ESCAPE -> "Escape"
                END -> "End"
                INSERT -> "Insert"
                NUMPAD_0 -> "Numpad 0"
                NUMPAD_1 -> "Numpad 1"
                NUMPAD_2 -> "Numpad 2"
                NUMPAD_3 -> "Numpad 3"
                NUMPAD_4 -> "Numpad 4"
                NUMPAD_5 -> "Numpad 5"
                NUMPAD_6 -> "Numpad 6"
                NUMPAD_7 -> "Numpad 7"
                NUMPAD_8 -> "Numpad 8"
                NUMPAD_9 -> "Numpad 9"
                COLON -> ":"
                F1 -> "F1"
                F2 -> "F2"
                F3 -> "F3"
                F4 -> "F4"
                F5 -> "F5"
                F6 -> "F6"
                F7 -> "F7"
                F8 -> "F8"
                F9 -> "F9"
                F10 -> "F10"
                F11 -> "F11"
                F12 -> "F12"
                else ->                // key name not found
                    null
            }
        }

        private val keyNames by lazy { initializeKeyNames() }

        /** @param keyname the keyname returned by the [Keys.toString] method
         * @return the int keycode
         */
        fun valueOf(keyname: String?): Int {
            return keyNames.get(keyname, -1)
        }

        /** lazily intialized in [Keys.valueOf]  */
        private fun initializeKeyNames(): ObjectIntMap<String?> {
            val result = ObjectIntMap<String?>()
            for (i in 0..255) {
                val name = toString(i)
                if (name != null) result.put(name, i)
            }
            return result
        }
    }

    /** Enumeration of potentially available peripherals. Use with [Input.isPeripheralAvailable].
     * @author mzechner
     */
    enum class Peripheral {

        HardwareKeyboard, OnscreenKeyboard, MultitouchScreen, Accelerometer, Compass, Vibrator, Gyroscope, RotationVector, Pressure
    }

    /** @return The acceleration force in m/s^2 applied to the device in the X axis, including the force of gravity
     */
    val accelerometerX: Float

    /** @return The acceleration force in m/s^2 applied to the device in the Y axis, including the force of gravity
     */
    val accelerometerY: Float

    /** @return The acceleration force in m/s^2 applied to the device in the Z axis, including the force of gravity
     */
    val accelerometerZ: Float

    /** @return The rate of rotation in rad/s around the X axis
     */
    val gyroscopeX: Float

    /** @return The rate of rotation in rad/s around the Y axis
     */
    val gyroscopeY: Float

    /** @return The rate of rotation in rad/s around the Z axis
     */
    val gyroscopeZ: Float

    /** @return The maximum number of pointers supported
     */
    val maxPointers: Int

    /** @return The x coordinate of the last touch on touch screen devices and the current mouse position on desktop for the first
     * pointer in screen coordinates. The screen origin is the top left corner.
     */
    val x: Int

    /** Returns the x coordinate in screen coordinates of the given pointer. Pointers are indexed from 0 to n. The pointer id
     * identifies the order in which the fingers went down on the screen, e.g. 0 is the first finger, 1 is the second and so on.
     * When two fingers are touched down and the first one is lifted the second one keeps its index. If another finger is placed on
     * the touch screen the first free index will be used.
     *
     * @param pointer the pointer id.
     * @return the x coordinate
     */
    fun getX(pointer: Int): Int

    /** @return the different between the current pointer location and the last pointer location on the x-axis.
     */
    val deltaX: Int

    /** @return the different between the current pointer location and the last pointer location on the x-axis.
     */
    fun getDeltaX(pointer: Int): Int

    /** @return The y coordinate of the last touch on touch screen devices and the current mouse position on desktop for the first
     * pointer in screen coordinates. The screen origin is the top left corner.
     */
    val y: Int

    /** Returns the y coordinate in screen coordinates of the given pointer. Pointers are indexed from 0 to n. The pointer id
     * identifies the order in which the fingers went down on the screen, e.g. 0 is the first finger, 1 is the second and so on.
     * When two fingers are touched down and the first one is lifted the second one keeps its index. If another finger is placed on
     * the touch screen the first free index will be used.
     *
     * @param pointer the pointer id.
     * @return the y coordinate
     */
    fun getY(pointer: Int): Int

    /** @return the different between the current pointer location and the last pointer location on the y-axis.
     */
    val deltaY: Int

    /** @return the different between the current pointer location and the last pointer location on the y-axis.
     */
    fun getDeltaY(pointer: Int): Int

    /** @return whether the screen is currently touched.
     */
    val isTouched: Boolean

    /** @return whether a new touch down event just occurred.
     */
    fun justTouched(): Boolean

    /** Whether the screen is currently touched by the pointer with the given index. Pointers are indexed from 0 to n. The pointer
     * id identifies the order in which the fingers went down on the screen, e.g. 0 is the first finger, 1 is the second and so on.
     * When two fingers are touched down and the first one is lifted the second one keeps its index. If another finger is placed on
     * the touch screen the first free index will be used.
     *
     * @param pointer the pointer
     * @return whether the screen is touched by the pointer
     */
    fun isTouched(pointer: Int): Boolean

    /** @return the pressure of the first pointer
     */
    val pressure: Float

    /** Returns the pressure of the given pointer, where 0 is untouched. On Android it should be
     * up to 1.0, but it can go above that slightly and its not consistent between devices. On iOS 1.0 is the normal touch
     * and significantly more of hard touch. Check relevant manufacturer documentation for details.
     * Check availability with [Input.isPeripheralAvailable]. If not supported, returns 1.0 when touched.
     *
     * @param pointer the pointer id.
     * @return the pressure
     */
    fun getPressure(pointer: Int): Float

    /** Whether a given button is pressed or not. Button constants can be found in [Buttons]. On Android only the Buttons#LEFT
     * constant is meaningful before version 4.0.
     * @param button the button to check.
     * @return whether the button is down or not.
     */
    fun isButtonPressed(button: Int): Boolean

    /** Returns whether a given button has just been pressed. Button constants can be found in [Buttons]. On Android only the Buttons#LEFT
     * constant is meaningful before version 4.0.
     *
     * @param button the button to check.
     * @return true or false.
     */
    fun isButtonJustPressed(button: Int): Boolean

    /** Returns whether the key is pressed.
     *
     * @param key The key code as found in [Input.Keys].
     * @return true or false.
     */
    fun isKeyPressed(key: Int): Boolean

    /** Returns whether the key has just been pressed.
     *
     * @param key The key code as found in [Input.Keys].
     * @return true or false.
     */
    fun isKeyJustPressed(key: Int): Boolean

    /** System dependent method to input a string of text. A dialog box will be created with the given title and the given text as a
     * message for the user. Once the dialog has been closed the provided [TextInputListener] will be called on the rendering
     * thread.
     *
     * @param listener The TextInputListener.
     * @param title The title of the text input dialog.
     * @param text The message presented to the user.
     */
    fun getTextInput(listener: TextInputListener?, title: String?, text: String?, hint: String?)

    /** Sets the on-screen keyboard visible if available.
     *
     * @param visible visible or not
     */
    fun setOnscreenKeyboardVisible(visible: Boolean)

    /** Vibrates for the given amount of time. Note that you'll need the permission
     * ` <uses-permission android:name="android.permission.VIBRATE"></uses-permission>` in your manifest file in order for this to work.
     *
     * @param milliseconds the number of milliseconds to vibrate.
     */
    fun vibrate(milliseconds: Int)

    /** Vibrate with a given pattern. Pass in an array of ints that are the times at which to turn on or off the vibrator. The first
     * one is how long to wait before turning it on, and then after that it alternates. If you want to repeat, pass the index into
     * the pattern at which to start the repeat.
     * @param pattern an array of longs of times to turn the vibrator on or off.
     * @param repeat the index into pattern at which to repeat, or -1 if you don't want to repeat.
     */
    fun vibrate(pattern: LongArray?, repeat: Int)

    /** Stops the vibrator  */
    fun cancelVibrate()

    /** The azimuth is the angle of the device's orientation around the z-axis. The positive z-axis points towards the earths
     * center.
     *
     * @see [](http://developer.android.com/reference/android/hardware/SensorManager.html.getRotationMatrix
    @return the azimuth in degrees
    ) */
    val azimuth: Float

    /** The pitch is the angle of the device's orientation around the x-axis. The positive x-axis roughly points to the west and is
     * orthogonal to the z- and y-axis.
     * @see [](http://developer.android.com/reference/android/hardware/SensorManager.html.getRotationMatrix
    @return the pitch in degrees
    ) */
    val pitch: Float

    /** The roll is the angle of the device's orientation around the y-axis. The positive y-axis points to the magnetic north pole
     * of the earth.
     * @see [](http://developer.android.com/reference/android/hardware/SensorManager.html.getRotationMatrix
    @return the roll in degrees
    ) */
    val roll: Float

    /** Returns the rotation matrix describing the devices rotation as per [SensorManager#getRotationMatrix(float[], float[], float[], float[])](http://developer.android.com/reference/android/hardware/SensorManager.html#getRotationMatrix(float[], float[], float[], float[])). Does not manipulate the matrix if the platform
     * does not have an accelerometer.
     * @param matrix
     */
    fun getRotationMatrix(matrix: FloatArray?)

    /** @return the time of the event currently reported to the [InputProcessor].
     */
    val currentEventTime: Long

    /**
     * @return whether the back button is currently being caught
     */
    /**
     * @param catchBack whether to catch the back button
     */
    @get:Deprecated("""use {@link Input#isCatchKey(int keycode)} instead
	  """)
    @set:Deprecated("""use {@link Input#setCatchKey(int keycode, boolean catchKey)} instead
	 
	  Sets whether the BACK button on Android should be caught. This will prevent the app from being paused. Will have no effect
	  on the desktop.
	 
	  """)
    var isCatchBackKey: Boolean

    /**
     * @return whether the menu button is currently being caught
     */
    /**
     * @param catchMenu whether to catch the menu button
     */
    @get:Deprecated("""use {@link Input#isCatchKey(int keycode)} instead
	  """)
    @set:Deprecated("""use {@link Input#setCatchKey(int keycode, boolean catchKey)} instead
	 
	  Sets whether the MENU button on Android should be caught. This will prevent the onscreen keyboard to show up. Will have no
	  effect on the desktop.
	  
	  """)
    var isCatchMenuKey: Boolean

    /**
     * Sets whether the given key on Android should be caught. No effect on other platforms.
     * All keys that are not caught may be handled by other apps or background processes. For example, media or volume
     * buttons are handled by background media players if present. If you use these keys to control your game, they
     * must be catched to prevent unintended behaviour.
     *
     * @param keycode  keycode to catch
     * @param catchKey whether to catch the given keycode
     */
    fun setCatchKey(keycode: Int, catchKey: Boolean)

    /**
     *
     * @param keycode keycode to check if caught
     * @return true if the given keycode is configured to be caught
     */
    fun isCatchKey(keycode: Int): Boolean

    /** @return the currently set [InputProcessor] or null.
     */
    /** Sets the [InputProcessor] that will receive all touch and key input events. It will be called before the
     * [ApplicationListener.render] method each frame.
     *
     * @param processor the InputProcessor
     */
    var inputProcessor: InputProcessor?

    /** Queries whether a [Peripheral] is currently available. In case of Android and the [Peripheral.HardwareKeyboard]
     * this returns the whether the keyboard is currently slid out or not.
     *
     * @param peripheral the [Peripheral]
     * @return whether the peripheral is available or not.
     */
    fun isPeripheralAvailable(peripheral: Peripheral?): Boolean

    /** @return the rotation of the device with respect to its native orientation.
     */
    val rotation: Int

    /** @return the native orientation of the device.
     */
    val nativeOrientation: Orientation?

    enum class Orientation {
        Landscape, Portrait
    }

    /** @return whether the mouse cursor is catched.
     */
    /** Only viable on the desktop. Will confine the mouse cursor location to the window and hide the mouse cursor. X and y
     * coordinates are still reported as if the mouse was not catched.
     * @param catched whether to catch or not to catch the mouse cursor
     */
    var isCursorCatched: Boolean

    /** Only viable on the desktop. Will set the mouse cursor location to the given window coordinates (origin top-left corner).
     * @param x the x-position
     * @param y the y-position
     */
    fun setCursorPosition(x: Int, y: Int)
}
