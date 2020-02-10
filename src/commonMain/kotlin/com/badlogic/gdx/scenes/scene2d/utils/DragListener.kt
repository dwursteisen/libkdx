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
package com.badlogic.gdx.scenes.scene2d.utils

import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop.Payload

/**
 * Detects mouse or finger touch drags on an actor. A touch must go down over the actor and a drag won't start until it is moved
 * outside the [tap square][.setTapSquareSize]. Any touch (not just the first) will trigger this listener. While
 * pressed, other touch downs are ignored.
 *
 * @author Nathan Sweet
 */
class DragListener : InputListener() {

    var tapSquareSize = 14f
    var touchDownX = -1f
        private set
    var touchDownY = -1f
        private set
    var stageTouchDownX = -1f
        private set
    var stageTouchDownY = -1f
        private set
    var dragStartX = 0f
    var dragStartY = 0f
    private var dragLastX = 0f
    private var dragLastY = 0f
    var dragX = 0f
        private set
    var dragY = 0f
        private set
    private var pressedPointer = -1

    /**
     * Sets the button to listen for, all other buttons are ignored. Default is [Buttons.LEFT]. Use -1 for any button.
     */
    var button = 0

    /**
     * Returns true if a touch has been dragged outside the tap square.
     */
    var isDragging = false
        private set

    fun touchDown(event: InputEvent, x: Float, y: Float, pointer: Int, button: Int): Boolean {
        if (pressedPointer != -1) return false
        if (pointer == 0 && this.button != -1 && button != this.button) return false
        pressedPointer = pointer
        touchDownX = x
        touchDownY = y
        stageTouchDownX = event.getStageX()
        stageTouchDownY = event.getStageY()
        return true
    }

    fun touchDragged(event: InputEvent?, x: Float, y: Float, pointer: Int) {
        if (pointer != pressedPointer) return
        if (!isDragging && (java.lang.Math.abs(touchDownX - x) > tapSquareSize || java.lang.Math.abs(touchDownY - y) > tapSquareSize)) {
            isDragging = true
            dragStartX = x
            dragStartY = y
            dragStart(event, x, y, pointer)
            dragX = x
            dragY = y
        }
        if (isDragging) {
            dragLastX = dragX
            dragLastY = dragY
            dragX = x
            dragY = y
            drag(event, x, y, pointer)
        }
    }

    fun touchUp(event: InputEvent?, x: Float, y: Float, pointer: Int, button: Int) {
        if (pointer == pressedPointer) {
            if (isDragging) dragStop(event, x, y, pointer)
            cancel()
        }
    }

    fun dragStart(event: InputEvent?, x: Float, y: Float, pointer: Int) {}
    fun drag(event: InputEvent?, x: Float, y: Float, pointer: Int) {}
    fun dragStop(event: InputEvent?, x: Float, y: Float, pointer: Int) {}

    /* If a drag is in progress, no further drag methods will be called until a new drag is started. */
    fun cancel() {
        isDragging = false
        pressedPointer = -1
    }

    /**
     * The distance from drag start to the current drag position.
     */
    val dragDistance: Float
        get() = Vector2.len(dragX - dragStartX, dragY - dragStartY)

    /**
     * Returns the amount on the x axis that the touch has been dragged since the last drag event.
     */
    val deltaX: Float
        get() = dragX - dragLastX

    /**
     * Returns the amount on the y axis that the touch has been dragged since the last drag event.
     */
    val deltaY: Float
        get() = dragY - dragLastY
}
