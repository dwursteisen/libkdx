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

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.badlogic.gdx.scenes.scene2d.utils.DragScrollListener
import com.badlogic.gdx.scenes.scene2d.utils.FocusListener.FocusEvent
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack

/**
 * Listener for [FocusEvent].
 *
 * @author Nathan Sweet
 */
abstract class FocusListener : EventListener {

    fun handle(event: Event): Boolean {
        if (event !is FocusEvent) return false
        val focusEvent = event as FocusEvent
        when (focusEvent.type) {
            FocusEvent.Type.keyboard -> keyboardFocusChanged(focusEvent, event.getTarget(), focusEvent.isFocused)
            FocusEvent.Type.scroll -> scrollFocusChanged(focusEvent, event.getTarget(), focusEvent.isFocused)
        }
        return false
    }

    /**
     * @param actor The event target, which is the actor that emitted the focus event.
     */
    fun keyboardFocusChanged(event: FocusEvent?, actor: Actor?, focused: Boolean) {}

    /**
     * @param actor The event target, which is the actor that emitted the focus event.
     */
    fun scrollFocusChanged(event: FocusEvent?, actor: Actor?, focused: Boolean) {}

    /**
     * Fired when an actor gains or loses keyboard or scroll focus. Can be cancelled to prevent losing or gaining focus.
     *
     * @author Nathan Sweet
     */
    class FocusEvent : Event() {

        var isFocused = false
        var type: Type? = null
        private var relatedActor: Actor? = null
        fun reset() {
            super.reset()
            relatedActor = null
        }

        /**
         * The actor related to the event. When focus is lost, this is the new actor being focused, or null. When focus is gained,
         * this is the previous actor that was focused, or null.
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
         * @author Nathan Sweet
         */
        enum class Type {

            keyboard, scroll
        }
    }
}
