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
package com.badlogic.gdx.scenes.scene2d

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.Stage.TouchFocus
import java.lang.RuntimeException

/**
 * Event for actor input: touch, mouse, keyboard, and scroll.
 *
 * @see InputListener
 */
class InputEvent : Event() {

    /**
     * The type of input event.
     */
    var type: Type? = null

    /**
     * The stage x coordinate where the event occurred. Valid for: touchDown, touchDragged, touchUp, mouseMoved, enter, and exit.
     */
    var stageX = 0f

    /**
     * The stage x coordinate where the event occurred. Valid for: touchDown, touchDragged, touchUp, mouseMoved, enter, and exit.
     */
    var stageY = 0f

    /**
     * The pointer index for the event. The first touch is index 0, second touch is index 1, etc. Always -1 on desktop. Valid for:
     * touchDown, touchDragged, touchUp, enter, and exit.
     */
    var pointer = 0

    /**
     * The index for the mouse button pressed. Always 0 on Android. Valid for: touchDown and touchUp.
     *
     * @see Buttons
     */
    var button = 0

    /**
     * The key code of the key that was pressed. Valid for: keyDown and keyUp.
     */
    var keyCode = 0

    /**
     * The amount the mouse was scrolled. Valid for: scrolled.
     */
    var scrollAmount = 0

    /**
     * The character for the key that was type. Valid for: keyTyped.
     */
    var character = 0.toChar()
    private var relatedActor: Actor? = null
    override fun reset() {
        super.reset()
        relatedActor = null
        button = -1
    }

    /**
     * The actor related to the event. Valid for: enter and exit. For enter, this is the actor being exited, or null. For exit,
     * this is the actor being entered, or null.
     */
    fun getRelatedActor(): Actor? {
        return relatedActor
    }

    /**
     * @param relatedActor May be null.
     */
    fun setRelatedActor(relatedActor: Actor?) {
        this.relatedActor = relatedActor
    }

    /**
     * Sets actorCoords to this event's coordinates relative to the specified actor.
     *
     * @param actorCoords Output for resulting coordinates.
     */
    fun toCoordinates(actor: Actor?, actorCoords: Vector2?): Vector2? {
        actorCoords.set(stageX, stageY)
        actor!!.stageToLocalCoordinates(actorCoords)
        return actorCoords
    }

    /**
     * Returns true of this event is a touchUp triggered by [Stage.cancelTouchFocus].
     */
    val isTouchFocusCancel: Boolean
        get() = stageX == Int.MIN_VALUE.toFloat() || stageY == Int.MIN_VALUE.toFloat()

    override fun toString(): String {
        return type.toString()
    }

    /**
     * Types of low-level input events supported by scene2d.
     */
    enum class Type {

        /**
         * A new touch for a pointer on the stage was detected
         */
        touchDown,

        /**
         * A pointer has stopped touching the stage.
         */
        touchUp,

        /**
         * A pointer that is touching the stage has moved.
         */
        touchDragged,

        /**
         * The mouse pointer has moved (without a mouse button being active).
         */
        mouseMoved,

        /**
         * The mouse pointer or an active touch have entered (i.e., [hit][Actor.hit]) an actor.
         */
        enter,

        /**
         * The mouse pointer or an active touch have exited an actor.
         */
        exit,

        /**
         * The mouse scroll wheel has changed.
         */
        scrolled,

        /**
         * A keyboard key has been pressed.
         */
        keyDown,

        /**
         * A keyboard key has been released.
         */
        keyUp,

        /**
         * A keyboard key has been pressed and released.
         */
        keyTyped
    }
}
