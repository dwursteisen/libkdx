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
 * Moves an actor from its current size to a specific size.
 *
 * @author Nathan Sweet
 */
class SizeToAction : TemporalAction() {

    private var startWidth = 0f
    private var startHeight = 0f
    var width = 0f
    var height = 0f
    protected override fun begin() {
        startWidth = target.getWidth()
        startHeight = target.getHeight()
    }

    protected override fun update(percent: Float) {
        val width: Float
        val height: Float
        if (percent == 0f) {
            width = startWidth
            height = startHeight
        } else if (percent == 1f) {
            width = this.width
            height = this.height
        } else {
            width = startWidth + (this.width - startWidth) * percent
            height = startHeight + (this.height - startHeight) * percent
        }
        target.setSize(width, height)
    }

    fun setSize(width: Float, height: Float) {
        this.width = width
        this.height = height
    }
}
