package com.badlogic.gdx.scenes.scene2d.utils

import com.badlogic.gdx.graphics.g2d.Batch

/**
 * A drawable that supports scale and rotation.
 */
interface TransformDrawable : Drawable {

    fun draw(batch: Batch?, x: Float, y: Float, originX: Float, originY: Float, width: Float, height: Float, scaleX: Float,
             scaleY: Float, rotation: Float)
}
