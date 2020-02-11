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

import java.lang.NullPointerException

/**
 * A button with a child [Label] to display text.
 *
 * @author Nathan Sweet
 */
class TextButton(text: String?, style: TextButtonStyle?) : Button() {

    private var label: Label? = null
    private var style: TextButtonStyle? = null

    constructor(text: String?, skin: Skin?) : this(text, skin.get(TextButtonStyle::class.java)) {
        setSkin(skin)
    }

    constructor(text: String?, skin: Skin?, styleName: String?) : this(text, skin.get(styleName, TextButtonStyle::class.java)) {
        setSkin(skin)
    }

    fun setStyle(style: ButtonStyle?) {
        if (style == null) throw NullPointerException("style cannot be null")
        if (style !is TextButtonStyle) throw java.lang.IllegalArgumentException("style must be a TextButtonStyle.")
        super.setStyle(style)
        this.style = style
        if (label != null) {
            val textButtonStyle = style as TextButtonStyle?
            val labelStyle: LabelStyle = label.getStyle()
            labelStyle.font = textButtonStyle!!.font
            labelStyle.fontColor = textButtonStyle.fontColor
            label.setStyle(labelStyle)
        }
    }

    fun getStyle(): TextButtonStyle? {
        return style
    }

    fun draw(batch: Batch?, parentAlpha: Float) {
        val fontColor: Color
        fontColor = if (isDisabled() && style!!.disabledFontColor != null) style!!.disabledFontColor else if (isPressed() && style!!.downFontColor != null) style!!.downFontColor else if (isChecked && style!!.checkedFontColor != null) if (isOver() && style!!.checkedOverFontColor != null) style!!.checkedOverFontColor else style!!.checkedFontColor else if (isOver() && style!!.overFontColor != null) style!!.overFontColor else style!!.fontColor
        if (fontColor != null) label.getStyle().fontColor = fontColor
        super.draw(batch, parentAlpha)
    }

    fun setLabel(label: Label?) {
        labelCell.setActor(label)
        this.label = label
    }

    fun getLabel(): Label? {
        return label
    }

    val labelCell: Cell<Label?>?
        get() = getCell(label)

    fun setText(text: String?) {
        label.setText(text)
    }

    val text: CharSequence?
        get() = label.getText()

    override fun toString(): String {
        val name: String = getName()
        if (name != null) return name
        var className: String? = javaClass.getName()
        val dotIndex = className!!.lastIndexOf('.')
        if (dotIndex != -1) className = className.substring(dotIndex + 1)
        return (if (className.indexOf('$') != -1) "TextButton " else "") + className + ": " + label.getText()
    }

    /**
     * The style for a text button, see [TextButton].
     *
     * @author Nathan Sweet
     */
    class TextButtonStyle : ButtonStyle {

        var font: BitmapFont? = null

        /**
         * Optional.
         */
        var fontColor: Color? = null
        var downFontColor: Color? = null
        var overFontColor: Color? = null
        var checkedFontColor: Color? = null
        var checkedOverFontColor: Color? = null
        var disabledFontColor: Color? = null

        constructor() {}
        constructor(up: Drawable?, down: Drawable?, checked: Drawable?, font: BitmapFont?) : super(up, down, checked) {
            this.font = font
        }

        constructor(style: TextButtonStyle?) : super(style) {
            font = style!!.font
            if (style.fontColor != null) fontColor = Color(style.fontColor)
            if (style.downFontColor != null) downFontColor = Color(style.downFontColor)
            if (style.overFontColor != null) overFontColor = Color(style.overFontColor)
            if (style.checkedFontColor != null) checkedFontColor = Color(style.checkedFontColor)
            if (style.checkedOverFontColor != null) checkedOverFontColor = Color(style.checkedOverFontColor)
            if (style.disabledFontColor != null) disabledFontColor = Color(style.disabledFontColor)
        }
    }

    init {
        setStyle(style)
        this.style = style
        label = Label(text, LabelStyle(style!!.font, style.fontColor))
        label.setAlignment(Align.center)
        add(label).expand().fill()
        setSize(getPrefWidth(), getPrefHeight())
    }
}
