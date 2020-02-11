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
package com.badlogic.gdx.scenes.scene2d.ui

/**
 * Displays a [Drawable], scaled various way within the widgets bounds. The preferred size is the min size of the drawable.
 * Only when using a [TextureRegionDrawable] will the actor's scale, rotation, and origin be used when drawing.
 *
 * @author Nathan Sweet
 */
class Image(drawable: Drawable?, scaling: Scaling?, align: Int) : Widget() {

    private var scaling: Scaling? = null
    private var align: Int = Align.center
    var imageX = 0f
        private set
    var imageY = 0f
        private set
    var imageWidth = 0f
        private set
    var imageHeight = 0f
        private set
    private var drawable: Drawable? = null

    /**
     * Creates an image stretched, and aligned center.
     *
     * @param patch May be null.
     */
    constructor(patch: NinePatch?) : this(NinePatchDrawable(patch), Scaling.stretch, Align.center) {}

    /**
     * Creates an image stretched, and aligned center.
     *
     * @param region May be null.
     */
    constructor(region: TextureRegion?) : this(TextureRegionDrawable(region), Scaling.stretch, Align.center) {}

    /**
     * Creates an image stretched, and aligned center.
     */
    constructor(texture: Texture?) : this(TextureRegionDrawable(TextureRegion(texture))) {}

    /**
     * Creates an image stretched, and aligned center.
     */
    constructor(skin: Skin, drawableName: String?) : this(skin.getDrawable(drawableName), Scaling.stretch, Align.center) {}
    /**
     * Creates an image stretched, and aligned center.
     *
     * @param drawable May be null.
     */
    /**
     * Creates an image with no drawable, stretched, and aligned center.
     */
    @JvmOverloads
    constructor(drawable: Drawable? = null as Drawable?) : this(drawable, Scaling.stretch, Align.center) {
    }

    /**
     * Creates an image aligned center.
     *
     * @param drawable May be null.
     */
    constructor(drawable: Drawable?, scaling: Scaling?) : this(drawable, scaling, Align.center) {}

    fun layout() {
        if (drawable == null) return
        val regionWidth: Float = drawable.getMinWidth()
        val regionHeight: Float = drawable.getMinHeight()
        val width: Float = getWidth()
        val height: Float = getHeight()
        val size: Vector2 = scaling.apply(regionWidth, regionHeight, width, height)
        imageWidth = size.x
        imageHeight = size.y
        if (align and Align.left !== 0) imageX = 0f else if (align and Align.right !== 0) imageX = (width - imageWidth) as Int.toFloat() else imageX = (width / 2-imageWidth / 2) as Int.toFloat()
        if (align and Align.top !== 0) imageY = (height - imageHeight) as Int.toFloat() else if ((align and Align.bottom) !== 0)imageY = 0f else imageY = (height / 2-imageHeight / 2) as Int.toFloat()
    }

    fun draw(batch: Batch, parentAlpha: Float) {
        validate()
        val color: Color = getColor()
        batch.setColor(color.r, color.g, color.b, color.a * parentAlpha)
        val x: Float = getX()
        val y: Float = getY()
        val scaleX: Float = getScaleX()
        val scaleY: Float = getScaleY()
        if (drawable is TransformDrawable) {
            val rotation: Float = getRotation()
            if (scaleX != 1f || scaleY != 1f || rotation != 0f) {
                (drawable as TransformDrawable?).draw(batch, x + imageX, y + imageY, getOriginX() - imageX, getOriginY() - imageY,
                    imageWidth, imageHeight, scaleX, scaleY, rotation)
                return
            }
        }
        if (drawable != null) drawable.draw(batch, x + imageX, y + imageY, imageWidth * scaleX, imageHeight * scaleY)
    }

    fun setDrawable(skin: Skin, drawableName: String?) {
        setDrawable(skin.getDrawable(drawableName))
    }

    /**
     * Sets a new drawable for the image. The image's pref size is the drawable's min size. If using the image actor's size rather
     * than the pref size, [.pack] can be used to size the image to its pref size.
     *
     * @param drawable May be null.
     */
    fun setDrawable(drawable: Drawable?) {
        if (this.drawable === drawable) return
        if (drawable != null) {
            if (prefWidth != drawable.getMinWidth() || prefHeight != drawable.getMinHeight()) invalidateHierarchy()
        } else invalidateHierarchy()
        this.drawable = drawable
    }

    /**
     * @return May be null.
     */
    fun getDrawable(): Drawable? {
        return drawable
    }

    fun setScaling(scaling: Scaling?) {
        if (scaling == null) throw java.lang.IllegalArgumentException("scaling cannot be null.")
        this.scaling = scaling
        invalidate()
    }

    fun setAlign(align: Int) {
        this.align = align
        invalidate()
    }

    val minWidth: Float
        get() = 0

    val minHeight: Float
        get() = 0

    val prefWidth: Float
        get() = if (drawable != null) drawable.getMinWidth() else 0

    val prefHeight: Float
        get() = if (drawable != null) drawable.getMinHeight() else 0

    override fun toString(): String {
        val name: String = getName()
        if (name != null) return name
        var className: String = javaClass.getName()
        val dotIndex = className.lastIndexOf('.')
        if (dotIndex != -1) className = className.substring(dotIndex + 1)
        return (if (className.indexOf('$') != -1) "Image " else "") + className + ": " + drawable
    }

    /**
     * @param drawable May be null.
     */
    init {
        setDrawable(drawable)
        this.scaling = scaling
        this.align = align
        setSize(prefWidth, prefHeight)
    }
}
