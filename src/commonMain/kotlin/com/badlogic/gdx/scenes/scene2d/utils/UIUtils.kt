package com.badlogic.gdx.scenes.scene2d.utils

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input

object UIUtils {
    var isMac: Boolean = java.lang.System.getProperty("os.name").contains("OS X")
    var isWindows: Boolean = java.lang.System.getProperty("os.name").contains("Windows")
    var isLinux: Boolean = java.lang.System.getProperty("os.name").contains("Linux")
    fun left(): Boolean {
        return Gdx.input.isButtonPressed(Input.Buttons.LEFT)
    }

    fun left(button: Int): Boolean {
        return button == Input.Buttons.LEFT
    }

    fun right(): Boolean {
        return Gdx.input.isButtonPressed(Input.Buttons.RIGHT)
    }

    fun right(button: Int): Boolean {
        return button == Input.Buttons.RIGHT
    }

    fun middle(): Boolean {
        return Gdx.input.isButtonPressed(Input.Buttons.MIDDLE)
    }

    fun middle(button: Int): Boolean {
        return button == Input.Buttons.MIDDLE
    }

    fun shift(): Boolean {
        return Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT)
    }

    fun shift(keycode: Int): Boolean {
        return keycode == Input.Keys.SHIFT_LEFT || keycode == Input.Keys.SHIFT_RIGHT
    }

    fun ctrl(): Boolean {
        return if (isMac) Gdx.input.isKeyPressed(Input.Keys.SYM) else Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT) || Gdx.input.isKeyPressed(Input.Keys.CONTROL_RIGHT)
    }

    fun ctrl(keycode: Int): Boolean {
        return if (isMac) keycode == Input.Keys.SYM else keycode == Input.Keys.CONTROL_LEFT || keycode == Input.Keys.CONTROL_RIGHT
    }

    fun alt(): Boolean {
        return Gdx.input.isKeyPressed(Input.Keys.ALT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.ALT_RIGHT)
    }

    fun alt(keycode: Int): Boolean {
        return keycode == Input.Keys.ALT_LEFT || keycode == Input.Keys.ALT_RIGHT
    }
}
