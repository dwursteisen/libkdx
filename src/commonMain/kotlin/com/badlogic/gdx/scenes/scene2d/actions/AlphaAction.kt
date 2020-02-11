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

import com.badlogic.gdx.graphics.Color

/**
 * Sets the alpha for an actor's color (or a specified color), from the current alpha to the new alpha. Note this action
 * transitions from the alpha at the time the action starts to the specified alpha.
 *
 * @author Nathan Sweet
 */
class AlphaAction : TemporalAction() {

    private var start = 0f
    var alpha = 0f
    var color: Color? = null
    override fun begin() {
        if (color == null) color = target!!.getColor()
        start = color!!.a
    }

    override fun update(percent: Float) {
        if (percent == 0f) color!!.a = start else if (percent == 1f) color!!.a = alpha else color!!.a = start + (alpha - start) * percent
    }

    override fun reset() {
        super.reset()
        color = null
    }

    fun getColor(): Color? {
        return color
    }

    /**
     * Sets the color to modify. If null (the default), the [actor&#39;s][.getActor] [color][Actor.getColor] will be
     * used.
     */
    fun setColor(color: Color?) {
        this.color = color
    }
}
