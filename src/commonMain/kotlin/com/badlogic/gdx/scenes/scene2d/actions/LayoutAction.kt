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
package com.badlogic.gdx.scenes.scene2d.actions

/**
 * Sets an actor's [layout][Layout.setLayoutEnabled] to enabled or disabled. The actor must implements
 * [Layout].
 *
 * @author Nathan Sweet
 */
class LayoutAction : Action() {

    var isEnabled = false
        private set

    fun setTarget(actor: Actor?) {
        if (actor != null && actor !is Layout) throw GdxRuntimeException("Actor must implement layout: $actor")
        super.setTarget(actor)
    }

    fun act(delta: Float): Boolean {
        (target as Layout?).setLayoutEnabled(isEnabled)
        return true
    }

    fun setLayoutEnabled(enabled: Boolean) {
        isEnabled = enabled
    }
}
