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

import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import java.lang.UnsupportedOperationException

/**
 * Draws a [TextureRegion] repeatedly to fill the area, instead of stretching it.
 *
 * @author Nathan Sweet
 */
class TiledDrawable : TextureRegionDrawable {

    private val color: Color = Color(1, 1, 1, 1)

    constructor() : super() {}
    constructor(region: TextureRegion?) : super(region) {}
    constructor(drawable: TextureRegionDrawable?) : super(drawable) {}

    override fun draw(batch: Batch, x: Float, y: Float, width: Float, height: Float) {
        var x = x
        var y = y
        val batchColor: Color = batch.getColor()
        temp.set(batchColor)
        batch.setColor(batchColor.mul(color))
        val region: TextureRegion = getRegion()
        val regionWidth: Float = region.getRegionWidth()
        val regionHeight: Float = region.getRegionHeight()
        val fullX = (width / regionWidth).toInt()
        val fullY = (height / regionHeight).toInt()
        val remainingX = width - regionWidth * fullX
        val remainingY = height - regionHeight * fullY
        val startX = x
        val startY = y
        val endX = x + width - remainingX
        val endY = y + height - remainingY
        for (i in 0 until fullX) {
            y = startY
            for (ii in 0 until fullY) {
                batch.draw(region, x, y, regionWidth, regionHeight)
                y += regionHeight
            }
            x += regionWidth
        }
        val texture: Texture = region.getTexture()
        val u: Float = region.getU()
        val v2: Float = region.getV2()
        if (remainingX > 0) {
            // Right edge.
            val u2: Float = u + remainingX / texture.getWidth()
            var v: Float = region.getV()
            y = startY
            for (ii in 0 until fullY) {
                batch.draw(texture, x, y, remainingX, regionHeight, u, v2, u2, v)
                y += regionHeight
            }
            // Upper right corner.
            if (remainingY > 0) {
                v = v2 - remainingY / texture.getHeight()
                batch.draw(texture, x, y, remainingX, remainingY, u, v2, u2, v)
            }
        }
        if (remainingY > 0) {
            // Top edge.
            val u2: Float = region.getU2()
            val v: Float = v2 - remainingY / texture.getHeight()
            x = startX
            for (i in 0 until fullX) {
                batch.draw(texture, x, y, regionWidth, remainingY, u, v2, u2, v)
                x += regionWidth
            }
        }
        batch.setColor(temp)
    }

    override fun draw(batch: Batch?, x: Float, y: Float, originX: Float, originY: Float, width: Float, height: Float, scaleX: Float,
                      scaleY: Float, rotation: Float) {
        throw UnsupportedOperationException()
    }

    fun getColor(): Color {
        return color
    }

    override fun tint(tint: Color?): TiledDrawable {
        val drawable = TiledDrawable(this)
        drawable.color.set(tint)
        drawable.setLeftWidth(getLeftWidth())
        drawable.setRightWidth(getRightWidth())
        drawable.setTopHeight(getTopHeight())
        drawable.setBottomHeight(getBottomHeight())
        return drawable
    }

    companion object {
        private val temp: Color = Color()
    }
}
