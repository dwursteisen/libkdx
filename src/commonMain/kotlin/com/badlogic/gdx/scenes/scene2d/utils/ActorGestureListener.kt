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

import com.badlogic.gdx.input.GestureDetector
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.badlogic.gdx.scenes.scene2d.InputEvent

/**
 * Detects tap, long press, fling, pan, zoom, and pinch gestures on an actor. If there is only a need to detect tap, use
 * [ClickListener].
 *
 * @author Nathan Sweet
 * @see GestureDetector
 */
class ActorGestureListener @JvmOverloads constructor(halfTapSquareSize: Float = 20f, tapCountInterval: Float = 0.4f, longPressDuration: Float = 1.1f, maxFlingDelay: Float = 0.15f) : EventListener {

    val gestureDetector: GestureDetector
    var event: InputEvent? = null
    var actor: Actor? = null
    var touchDownTarget: Actor? = null
    fun handle(e: Event): Boolean {
        if (e !is InputEvent) return false
        val event: InputEvent = e as InputEvent
        when (event.getType()) {
            touchDown -> {
                actor = event.getListenerActor()
                touchDownTarget = event.getTarget()
                gestureDetector.touchDown(event.getStageX(), event.getStageY(), event.getPointer(), event.getButton())
                actor.stageToLocalCoordinates(tmpCoords.set(event.getStageX(), event.getStageY()))
                touchDown(event, tmpCoords.x, tmpCoords.y, event.getPointer(), event.getButton())
                return true
            }
            touchUp -> {
                if (event.isTouchFocusCancel()) {
                    gestureDetector.reset()
                    return false
                }
                this.event = event
                actor = event.getListenerActor()
                gestureDetector.touchUp(event.getStageX(), event.getStageY(), event.getPointer(), event.getButton())
                actor.stageToLocalCoordinates(tmpCoords.set(event.getStageX(), event.getStageY()))
                touchUp(event, tmpCoords.x, tmpCoords.y, event.getPointer(), event.getButton())
                return true
            }
            touchDragged -> {
                this.event = event
                actor = event.getListenerActor()
                gestureDetector.touchDragged(event.getStageX(), event.getStageY(), event.getPointer())
                return true
            }
        }
        return false
    }

    fun touchDown(event: InputEvent?, x: Float, y: Float, pointer: Int, button: Int) {}
    fun touchUp(event: InputEvent?, x: Float, y: Float, pointer: Int, button: Int) {}
    fun tap(event: InputEvent?, x: Float, y: Float, count: Int, button: Int) {}

    /**
     * If true is returned, additional gestures will not be triggered. No event is provided because this event is triggered by
     * time passing, not by an InputEvent.
     */
    fun longPress(actor: Actor?, x: Float, y: Float): Boolean {
        return false
    }

    fun fling(event: InputEvent?, velocityX: Float, velocityY: Float, button: Int) {}

    /**
     * The delta is the difference in stage coordinates since the last pan.
     */
    fun pan(event: InputEvent?, x: Float, y: Float, deltaX: Float, deltaY: Float) {}
    fun zoom(event: InputEvent?, initialDistance: Float, distance: Float) {}
    fun pinch(event: InputEvent?, initialPointer1: Vector2?, initialPointer2: Vector2?, pointer1: Vector2?, pointer2: Vector2?) {}

    fun getTouchDownTarget(): Actor? {
        return touchDownTarget
    }

    companion object {
        val tmpCoords: Vector2 = Vector2()
        val tmpCoords2: Vector2 = Vector2()
    }
    /**
     * @see GestureDetector.GestureDetector
     */
    /**
     * @see GestureDetector.GestureDetector
     */
    init {
        gestureDetector = GestureDetector(halfTapSquareSize, tapCountInterval, longPressDuration, maxFlingDelay, object : GestureDetector.GestureAdapter() {
            private val initialPointer1: Vector2 = Vector2()
            private val initialPointer2: Vector2 = Vector2()
            private val pointer1: Vector2 = Vector2()
            private val pointer2: Vector2 = Vector2()
            override fun tap(stageX: Float, stageY: Float, count: Int, button: Int): Boolean {
                actor.stageToLocalCoordinates(tmpCoords.set(stageX, stageY))
                this@ActorGestureListener.tap(event, tmpCoords.x, tmpCoords.y, count, button)
                return true
            }

            override fun longPress(stageX: Float, stageY: Float): Boolean {
                actor.stageToLocalCoordinates(tmpCoords.set(stageX, stageY))
                return this@ActorGestureListener.longPress(actor, tmpCoords.x, tmpCoords.y)
            }

            override fun fling(velocityX: Float, velocityY: Float, button: Int): Boolean {
                stageToLocalAmount(tmpCoords.set(velocityX, velocityY))
                this@ActorGestureListener.fling(event, tmpCoords.x, tmpCoords.y, button)
                return true
            }

            override fun pan(stageX: Float, stageY: Float, deltaX: Float, deltaY: Float): Boolean {
                var deltaX = deltaX
                var deltaY = deltaY
                stageToLocalAmount(tmpCoords.set(deltaX, deltaY))
                deltaX = tmpCoords.x
                deltaY = tmpCoords.y
                actor.stageToLocalCoordinates(tmpCoords.set(stageX, stageY))
                this@ActorGestureListener.pan(event, tmpCoords.x, tmpCoords.y, deltaX, deltaY)
                return true
            }

            override fun zoom(initialDistance: Float, distance: Float): Boolean {
                this@ActorGestureListener.zoom(event, initialDistance, distance)
                return true
            }

            override fun pinch(stageInitialPointer1: Vector2?, stageInitialPointer2: Vector2?, stagePointer1: Vector2?,
                               stagePointer2: Vector2?): Boolean {
                actor.stageToLocalCoordinates(initialPointer1.set(stageInitialPointer1))
                actor.stageToLocalCoordinates(initialPointer2.set(stageInitialPointer2))
                actor.stageToLocalCoordinates(pointer1.set(stagePointer1))
                actor.stageToLocalCoordinates(pointer2.set(stagePointer2))
                this@ActorGestureListener.pinch(event, initialPointer1, initialPointer2, pointer1, pointer2)
                return true
            }

            private fun stageToLocalAmount(amount: Vector2) {
                actor.stageToLocalCoordinates(amount)
                amount.sub(actor.stageToLocalCoordinates(tmpCoords2.set(0, 0)))
            }
        })
    }
}
