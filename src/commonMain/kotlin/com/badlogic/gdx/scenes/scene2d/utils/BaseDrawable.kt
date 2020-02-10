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

/**
 * Drawable that stores the size information but doesn't draw anything.
 *
 * @author Nathan Sweet
 */
class BaseDrawable : Drawable {

    var name: String? = null
    override var leftWidth = 0f
    override var rightWidth = 0f
    override var topHeight = 0f
    override var bottomHeight = 0f
    override var minWidth = 0f
    override var minHeight = 0f

    constructor() {}

    /**
     * Creates a new empty drawable with the same sizing information as the specified drawable.
     */
    constructor(drawable: Drawable) {
        if (drawable is BaseDrawable) name = drawable.name
        leftWidth = drawable.leftWidth
        rightWidth = drawable.rightWidth
        topHeight = drawable.topHeight
        bottomHeight = drawable.bottomHeight
        minWidth = drawable.minWidth
        minHeight = drawable.minHeight
    }

    fun draw(batch: Batch?, x: Float, y: Float, width: Float, height: Float) {}

    fun setPadding(topHeight: Float, leftWidth: Float, bottomHeight: Float, rightWidth: Float) {
        topHeight = topHeight
        leftWidth = leftWidth
        bottomHeight = bottomHeight
        rightWidth = rightWidth
    }

    fun setMinSize(minWidth: Float, minHeight: Float) {
        minWidth = minWidth
        minHeight = minHeight
    }

    override fun toString(): String {
        return if (name == null) ClassReflection.getSimpleName(javaClass) else name
    }
}
