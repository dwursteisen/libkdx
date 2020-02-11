/*******************************************************************************
 * Copyright 2015 See AUTHORS file.
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

import java.lang.NullPointerException

/**
 * A tooltip that shows a label.
 *
 * @author Nathan Sweet
 */
class TextTooltip(text: String?, manager: TooltipManager, style: TextTooltipStyle) : Tooltip<Label?>(null, manager) {

    constructor(text: String?, skin: Skin) : this(text, TooltipManager.getInstance(), skin.get(TextTooltipStyle::class.java)) {}
    constructor(text: String?, skin: Skin, styleName: String?) : this(text, TooltipManager.getInstance(), skin.get(styleName, TextTooltipStyle::class.java)) {}
    constructor(text: String?, style: TextTooltipStyle?) : this(text, TooltipManager.getInstance(), style) {}
    constructor(text: String?, manager: TooltipManager?, skin: Skin) : this(text, manager, skin.get(TextTooltipStyle::class.java)) {}
    constructor(text: String?, manager: TooltipManager?, skin: Skin, styleName: String?) : this(text, manager, skin.get(styleName, TextTooltipStyle::class.java)) {}

    fun setStyle(style: TextTooltipStyle?) {
        if (style == null) throw NullPointerException("style cannot be null")
        container.getActor().setStyle(style.label)
        container.setBackground(style.background)
        container.maxWidth(style.wrapWidth)
    }

    /**
     * The style for a text tooltip, see [TextTooltip].
     *
     * @author Nathan Sweet
     */
    class TextTooltipStyle {

        var label: LabelStyle? = null

        /**
         * Optional.
         */
        var background: Drawable? = null

        /**
         * Optional, 0 means don't wrap.
         */
        var wrapWidth = 0f

        constructor() {}
        constructor(label: LabelStyle?, background: Drawable?) {
            this.label = label
            this.background = background
        }

        constructor(style: TextTooltipStyle) {
            label = LabelStyle(style.label)
            background = style.background
            wrapWidth = style.wrapWidth
        }
    }

    init {
        val label = Label(text, style.label)
        label.setWrap(true)
        container.setActor(label)
        container.width(object : Value() {
            override fun get(context: Actor?): Float {
                return java.lang.Math.min(manager.maxWidth, container.getActor().getGlyphLayout().width)
            }
        })
        setStyle(style)
    }
}
