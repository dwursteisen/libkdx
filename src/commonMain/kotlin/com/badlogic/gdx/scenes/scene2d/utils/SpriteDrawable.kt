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
 * Drawable for a [Sprite].
 *
 * @author Nathan Sweet
 */
class SpriteDrawable : BaseDrawable, TransformDrawable {

    private var sprite: Sprite? = null

    /**
     * Creates an uninitialized SpriteDrawable. The sprite must be set before use.
     */
    constructor() {}
    constructor(sprite: Sprite?) {
        setSprite(sprite)
    }

    constructor(drawable: SpriteDrawable) : super(drawable) {
        setSprite(drawable.sprite)
    }

    override fun draw(batch: Batch?, x: Float, y: Float, width: Float, height: Float) {
        val spriteColor: Color = sprite.getColor()
        temp.set(spriteColor)
        sprite.setColor(spriteColor.mul(batch.getColor()))
        sprite.setRotation(0)
        sprite.setScale(1, 1)
        sprite.setBounds(x, y, width, height)
        sprite.draw(batch)
        sprite.setColor(temp)
    }

    fun draw(batch: Batch, x: Float, y: Float, originX: Float, originY: Float, width: Float, height: Float, scaleX: Float,
             scaleY: Float, rotation: Float) {
        val spriteColor: Color = sprite.getColor()
        temp.set(spriteColor)
        sprite.setColor(spriteColor.mul(batch.getColor()))
        sprite.setOrigin(originX, originY)
        sprite.setRotation(rotation)
        sprite.setScale(scaleX, scaleY)
        sprite.setBounds(x, y, width, height)
        sprite.draw(batch)
        sprite.setColor(temp)
    }

    fun setSprite(sprite: Sprite?) {
        this.sprite = sprite
        minWidth = sprite.getWidth()
        minHeight = sprite.getHeight()
    }

    fun getSprite(): Sprite? {
        return sprite
    }

    /**
     * Creates a new drawable that renders the same as this drawable tinted the specified color.
     */
    fun tint(tint: Color?): SpriteDrawable {
        val newSprite: Sprite
        if (sprite is AtlasSprite) newSprite = AtlasSprite(sprite as AtlasSprite?) else newSprite = Sprite(sprite)
        newSprite.setColor(tint)
        newSprite.setSize(minWidth, minHeight)
        val drawable = SpriteDrawable(newSprite)
        drawable.leftWidth = leftWidth
        drawable.rightWidth = rightWidth
        drawable.topHeight = topHeight
        drawable.bottomHeight = bottomHeight
        return drawable
    }

    companion object {
        private val temp: Color = Color()
    }
}
