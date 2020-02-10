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

import com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable
import java.lang.UnsupportedOperationException

/**
 * Drawable for a [TextureRegion].
 *
 * @author Nathan Sweet
 */
class TextureRegionDrawable : BaseDrawable, TransformDrawable {

    private var region: TextureRegion? = null

    /**
     * Creates an uninitialized TextureRegionDrawable. The texture region must be set before use.
     */
    constructor() {}
    constructor(texture: Texture?) {
        setRegion(TextureRegion(texture))
    }

    constructor(region: TextureRegion?) {
        setRegion(region)
    }

    constructor(drawable: TextureRegionDrawable) : super(drawable) {
        setRegion(drawable.region)
    }

    override fun draw(batch: Batch?, x: Float, y: Float, width: Float, height: Float) {
        batch.draw(region, x, y, width, height)
    }

    fun draw(batch: Batch, x: Float, y: Float, originX: Float, originY: Float, width: Float, height: Float, scaleX: Float,
             scaleY: Float, rotation: Float) {
        batch.draw(region, x, y, originX, originY, width, height, scaleX, scaleY, rotation)
    }

    fun setRegion(region: TextureRegion?) {
        this.region = region
        if (region != null) {
            minWidth = region.getRegionWidth()
            minHeight = region.getRegionHeight()
        }
    }

    fun getRegion(): TextureRegion? {
        return region
    }

    /**
     * Creates a new drawable that renders the same as this drawable tinted the specified color.
     */
    fun tint(tint: Color?): Drawable {
        val sprite: Sprite
        if (region is AtlasRegion) sprite = AtlasSprite(region as AtlasRegion?) else sprite = Sprite(region)
        sprite.setColor(tint)
        sprite.setSize(minWidth, minHeight)
        val drawable = SpriteDrawable(sprite)
        drawable.leftWidth = leftWidth
        drawable.rightWidth = rightWidth
        drawable.topHeight = topHeight
        drawable.bottomHeight = bottomHeight
        return drawable
    }
}
