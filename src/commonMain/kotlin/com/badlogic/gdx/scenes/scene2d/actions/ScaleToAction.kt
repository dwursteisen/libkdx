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

import java.lang.Runnable

/**
 * Sets the actor's scale from its current value to a specific value.
 *
 * @author Nathan Sweet
 */
class ScaleToAction : TemporalAction() {

    private var startX = 0f
    private var startY = 0f
    var x = 0f
    var y = 0f
    protected override fun begin() {
        startX = target.getScaleX()
        startY = target.getScaleY()
    }

    protected override fun update(percent: Float) {
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
        target.setScale(x, y)
    }

    fun setScale(x: Float, y: Float) {
        this.x = x
        this.y = y
    }

    fun setScale(scale: Float) {
        x = scale
        y = scale
    }
}
