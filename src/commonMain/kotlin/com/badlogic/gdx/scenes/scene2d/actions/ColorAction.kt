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
 * Sets the actor's color (or a specified color), from the current to the new color. Note this action transitions from the color
 * at the time the action starts to the specified color.
 *
 * @author Nathan Sweet
 */
class ColorAction : TemporalAction() {

    private var startR = 0f
    private var startG = 0f
    private var startB = 0f
    private var startA = 0f
    private var color: Color? = null
    private val end: Color = Color()
    override fun begin() {
        if (color == null) color = target!!.getColor()
        startR = color.r
        startG = color.g
        startB = color.b
        startA = color.a
    }

    override fun update(percent: Float) {
        if (percent == 0f) color.set(startR, startG, startB, startA) else if (percent == 1f) color.set(end) else {
            val r: Float = startR + (end.r - startR) * percent
            val g: Float = startG + (end.g - startG) * percent
            val b: Float = startB + (end.b - startB) * percent
            val a: Float = startA + (end.a - startA) * percent
            color.set(r, g, b, a)
        }
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

    /**
     * Sets the color to transition to. Required.
     */
    var endColor: Color
        get() = end
        set(color) {
            end.set(color)
        }
}
