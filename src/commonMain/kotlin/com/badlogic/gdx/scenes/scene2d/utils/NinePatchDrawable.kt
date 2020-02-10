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

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.NinePatch
import com.badlogic.gdx.scenes.scene2d.utils.DragScrollListener
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack

/**
 * Drawable for a [NinePatch].
 *
 *
 * The drawable sizes are set when the ninepatch is set, but they are separate values. Eg, [Drawable.getLeftWidth] could
 * be set to more than [NinePatch.getLeftWidth] in order to provide more space on the left than actually exists in the
 * ninepatch.
 *
 *
 * The min size is set to the ninepatch total size by default. It could be set to the left+right and top+bottom, excluding the
 * middle size, to allow the drawable to be sized down as small as possible.
 *
 * @author Nathan Sweet
 */
class NinePatchDrawable : BaseDrawable, TransformDrawable {

    private var patch: NinePatch? = null

    /**
     * Creates an uninitialized NinePatchDrawable. The ninepatch must be [set][.setPatch] before use.
     */
    constructor() {}
    constructor(patch: NinePatch) {
        setPatch(patch)
    }

    constructor(drawable: NinePatchDrawable) : super(drawable) {
        patch = drawable.patch
    }

    fun draw(batch: Batch?, x: Float, y: Float, width: Float, height: Float) {
        patch.draw(batch, x, y, width, height)
    }

    fun draw(batch: Batch?, x: Float, y: Float, originX: Float, originY: Float, width: Float, height: Float, scaleX: Float,
             scaleY: Float, rotation: Float) {
        patch.draw(batch, x, y, originX, originY, width, height, scaleX, scaleY, rotation)
    }

    /**
     * Sets this drawable's ninepatch and set the min width, min height, top height, right width, bottom height, and left width to
     * the patch's padding.
     */
    fun setPatch(patch: NinePatch) {
        this.patch = patch
        setMinWidth(patch.getTotalWidth())
        setMinHeight(patch.getTotalHeight())
        setTopHeight(patch.getPadTop())
        setRightWidth(patch.getPadRight())
        setBottomHeight(patch.getPadBottom())
        setLeftWidth(patch.getPadLeft())
    }

    fun getPatch(): NinePatch? {
        return patch
    }

    /**
     * Creates a new drawable that renders the same as this drawable tinted the specified color.
     */
    fun tint(tint: Color?): NinePatchDrawable {
        val drawable = NinePatchDrawable(this)
        drawable.patch = NinePatch(drawable.getPatch(), tint)
        return drawable
    }
}
