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

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.utils.DragScrollListener
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack

/**
 * A drawable knows how to draw itself at a given rectangular size. It provides padding sizes and a minimum size so that other
 * code can determine how to size and position content.
 *
 * @author Nathan Sweet
 */
interface Drawable {

    /**
     * Draws this drawable at the specified bounds. The drawable should be tinted with [Batch.getColor], possibly by
     * mixing its own color.
     */
    fun draw(batch: Batch?, x: Float, y: Float, width: Float, height: Float)
    var leftWidth: Float
    var rightWidth: Float
    var topHeight: Float
    var bottomHeight: Float
    var minWidth: Float
    var minHeight: Float
}
