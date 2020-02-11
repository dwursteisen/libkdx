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
 * Moves an actor from its current position to a specific position.
 *
 * @author Nathan Sweet
 */
class MoveToAction : TemporalAction() {

    /**
     * Gets the starting X value, set in [.begin].
     */
    var startX = 0f
        private set

    /**
     * Gets the starting Y value, set in [.begin].
     */
    var startY = 0f
        private set
    var x = 0f
    var y = 0f
    var alignment: Int = Align.bottomLeft
    override fun begin() {
        startX = target!!.getX(alignment)
        startY = target!!.getY(alignment)
    }

    override fun update(percent: Float) {
        val x: Float
        val y: Float
        if (percent == 0f) {
            x = startX
            y = startY
        } else if (percent == 1f) {
            x = this.x
            y = this.y
        } else {
            x = startX + (this.x - startX) * percent
            y = startY + (this.y - startY) * percent
        }
        target!!.setPosition(x, y, alignment)
    }

    override fun reset() {
        super.reset()
        alignment = Align.bottomLeft
    }

    fun setStartPosition(x: Float, y: Float) {
        startX = x
        startY = y
    }

    fun setPosition(x: Float, y: Float) {
        this.x = x
        this.y = y
    }

    fun setPosition(x: Float, y: Float, alignment: Int) {
        this.x = x
        this.y = y
        this.alignment = alignment
    }
}
