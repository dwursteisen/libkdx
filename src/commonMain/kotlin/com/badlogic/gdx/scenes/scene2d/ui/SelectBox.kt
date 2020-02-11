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

import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar.ProgressBarStyle
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane.ScrollPaneStyle
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox.SelectBoxStyle
import java.lang.UnsupportedOperationException

/**
 * A select box (aka a drop-down list) allows a user to choose one of a number of values from a list. When inactive, the selected
 * value is displayed. When activated, it shows the list of values that may be selected.
 *
 *
 * [ChangeEvent] is fired when the selectbox selection changes.
 *
 *
 * The preferred size of the select box is determined by the maximum text bounds of the items and the size of the
 * [SelectBoxStyle.background].
 *
 * @author mzechner
 * @author Nathan Sweet
 */
class SelectBox<T>(style: SelectBoxStyle?) : Widget(), Disableable {

    var style: SelectBoxStyle? = null

    /**
     * Returns the internal items array. If modified, [.setItems] must be called to reflect the changes.
     */
    val items: Array<T?>? = Array()
    val selection: ArraySelection<T?>? = ArraySelection(items)
    var selectBoxList: SelectBoxList<T?>? = null
    private var prefWidth = 0f
    private var prefHeight = 0f
    private var clickListener: ClickListener? = null
    var disabled = false
    private var alignment: Int = Align.left

    constructor(skin: Skin?) : this(skin.get(SelectBoxStyle::class.java)) {}
    constructor(skin: Skin?, styleName: String?) : this(skin.get(styleName, SelectBoxStyle::class.java)) {}

    /**
     * @return Max number of items to display when the box is opened, or <= 0 to display them all.
     */
    /**
     * Set the max number of items to display when the select box is opened. Set to 0 (the default) to display as many as fit in
     * the stage height.
     */
    var maxListCount: Int
        get() = selectBoxList!!.maxListCount
        set(maxListCount) {
            selectBoxList!!.maxListCount = maxListCount
        }

    protected fun setStage(stage: Stage?) {
        if (stage == null) selectBoxList!!.hide()
        super.setStage(stage)
    }

    fun setStyle(style: SelectBoxStyle?) {
        if (style == null) throw java.lang.IllegalArgumentException("style cannot be null.")
        this.style = style
        if (selectBoxList != null) {
            selectBoxList.setStyle(style.scrollStyle)
            selectBoxList.list.setStyle(style.listStyle)
        }
        invalidateHierarchy()
    }

    /**
     * Returns the select box's style. Modifying the returned style may not have an effect until [.setStyle]
     * is called.
     */
    fun getStyle(): SelectBoxStyle? {
        return style
    }

    /**
     * Set the backing Array that makes up the choices available in the SelectBox
     */
    fun setItems(vararg newItems: T?) {
        if (newItems == null) throw java.lang.IllegalArgumentException("newItems cannot be null.")
        val oldPrefWidth = getPrefWidth()
        items.clear()
        items.addAll(newItems)
        selection.validate()
        selectBoxList!!.list.setItems(items)
        invalidate()
        if (oldPrefWidth != getPrefWidth()) invalidateHierarchy()
    }

    /**
     * Sets the items visible in the select box.
     */
    fun setItems(newItems: Array<T?>?) {
        if (newItems == null) throw java.lang.IllegalArgumentException("newItems cannot be null.")
        val oldPrefWidth = getPrefWidth()
        if (newItems !== items) {
            items.clear()
            items.addAll(newItems)
        }
        selection.validate()
        selectBoxList!!.list.setItems(items)
        invalidate()
        if (oldPrefWidth != getPrefWidth()) invalidateHierarchy()
    }

    fun clearItems() {
        if (items!!.size === 0) return
        items.clear()
        selection.clear()
        invalidateHierarchy()
    }

    fun layout() {
        var bg: Drawable? = style!!.background
        val font: BitmapFont? = style!!.font
        prefHeight = if (bg != null) {
            java.lang.Math.max(bg.getTopHeight() + bg.getBottomHeight() + font.getCapHeight() - font.getDescent() * 2,
                bg.getMinHeight())
        } else font.getCapHeight() - font.getDescent() * 2
        var maxItemWidth = 0f
        val layoutPool: Pool<GlyphLayout?> = Pools.get(GlyphLayout::class.java)
        val layout: GlyphLayout = layoutPool.obtain()
        for (i in 0 until items!!.size) {
            layout.setText(font, toString(items[i]))
            maxItemWidth = java.lang.Math.max(layout.width, maxItemWidth)
        }
        layoutPool.free(layout)
        prefWidth = maxItemWidth
        if (bg != null) prefWidth = java.lang.Math.max(prefWidth + bg.getLeftWidth() + bg.getRightWidth(), bg.getMinWidth())
        val listStyle: ListStyle? = style!!.listStyle
        val scrollStyle: ScrollPaneStyle? = style!!.scrollStyle
        var listWidth: Float = maxItemWidth + listStyle.selection.getLeftWidth() + listStyle.selection.getRightWidth()
        bg = scrollStyle!!.background
        if (bg != null) listWidth = java.lang.Math.max(listWidth + bg.getLeftWidth() + bg.getRightWidth(), bg.getMinWidth())
        if (selectBoxList == null || !selectBoxList.disableY) {
            listWidth += java.lang.Math.max(if (style!!.scrollStyle!!.vScroll != null) style!!.scrollStyle!!.vScroll.getMinWidth() else 0,
                if (style!!.scrollStyle!!.vScrollKnob != null) style!!.scrollStyle!!.vScrollKnob.getMinWidth() else 0).toFloat()
        }
        prefWidth = java.lang.Math.max(prefWidth, listWidth)
    }

    fun draw(batch: Batch?, parentAlpha: Float) {
        validate()
        val background: Drawable?
        background = if (disabled && style!!.backgroundDisabled != null) style!!.backgroundDisabled else if (selectBoxList.hasParent() && style!!.backgroundOpen != null) style!!.backgroundOpen else if (clickListener.isOver() && style!!.backgroundOver != null) style!!.backgroundOver else if (style!!.background != null) style!!.background else null
        val font: BitmapFont? = style!!.font
        val fontColor: Color = if (disabled && style!!.disabledFontColor != null) style!!.disabledFontColor else style!!.fontColor
        val color: Color = getColor()
        var x: Float = getX()
        var y: Float = getY()
        var width: Float = getWidth()
        var height: Float = getHeight()
        batch.setColor(color.r, color.g, color.b, color.a * parentAlpha)
        if (background != null) background.draw(batch, x, y, width, height)
        val selected: T = selection.first()
        if (selected != null) {
            if (background != null) {
                width -= background.getLeftWidth() + background.getRightWidth()
                height -= background.getBottomHeight() + background.getTopHeight()
                x += background.getLeftWidth()
                y += (height / 2 + background.getBottomHeight() + font.getData().capHeight / 2) as Int.toFloat()
            } else {
                y += (height / 2 + font.getData().capHeight / 2) as Int.toFloat()
            }
            font.setColor(fontColor.r, fontColor.g, fontColor.b, fontColor.a * parentAlpha)
            drawItem(batch, font, selected, x, y, width)
        }
    }

    protected fun drawItem(batch: Batch?, font: BitmapFont?, item: T?, x: Float, y: Float, width: Float): GlyphLayout? {
        val string = toString(item)
        return font.draw(batch, string, x, y, 0, string!!.length, width, alignment, false, "...")
    }

    /**
     * Sets the alignment of the selected item in the select box. See [.getList] and [List.setAlignment] to set
     * the alignment in the list shown when the select box is open.
     *
     * @param alignment See [Align].
     */
    fun setAlignment(alignment: Int) {
        this.alignment = alignment
    }

    /**
     * Get the set of selected items, useful when multiple items are selected
     *
     * @return a Selection object containing the selected elements
     */
    fun getSelection(): ArraySelection<T?>? {
        return selection
    }

    /**
     * Returns the first selected item, or null. For multiple selections use [SelectBox.getSelection].
     */
    /**
     * Sets the selection to only the passed item, if it is a possible choice, else selects the first item.
     */
    var selected: T?
        get() = selection.first()
        set(item) {
            if (items!!.contains(item, false)) selection.set(item) else if (items.size > 0) selection.set(items.first()) else selection.clear()
        }

    /**
     * @return The index of the first selected item. The top item has an index of 0. Nothing selected has an index of -1.
     */
    /**
     * Sets the selection to only the selected index.
     */
    var selectedIndex: Int
        get() {
            val selected: ObjectSet<T?> = selection.items()
            return if (selected.size === 0) -1 else items!!.indexOf(selected.first(), false)
        }
        set(index) {
            selection.set(items!![index])
        }

    fun setDisabled(disabled: Boolean) {
        if (disabled && !this.disabled) hideList()
        this.disabled = disabled
    }

    fun isDisabled(): Boolean {
        return disabled
    }

    fun getPrefWidth(): Float {
        validate()
        return prefWidth
    }

    fun getPrefHeight(): Float {
        validate()
        return prefHeight
    }

    protected fun toString(item: T?): String? {
        return item.toString()
    }

    fun showList() {
        if (items!!.size === 0) return
        if (getStage() != null) selectBoxList!!.show(getStage())
    }

    fun hideList() {
        selectBoxList!!.hide()
    }

    /**
     * Returns the list shown when the select box is open.
     */
    val list: List<T?>?
        get() = selectBoxList!!.list

    /**
     * Disables scrolling of the list shown when the select box is open.
     */
    fun setScrollingDisabled(y: Boolean) {
        selectBoxList.setScrollingDisabled(true, y)
        invalidateHierarchy()
    }

    /**
     * Returns the scroll pane containing the list that is shown when the select box is open.
     */
    val scrollPane: com.badlogic.gdx.scenes.scene2d.ui.ScrollPane?
        get() = selectBoxList

    protected fun onShow(selectBoxList: Actor?, below: Boolean) {
        selectBoxList.getColor().a = 0
        selectBoxList.addAction(fadeIn(0.3f, Interpolation.fade))
    }

    protected fun onHide(selectBoxList: Actor?) {
        selectBoxList.getColor().a = 1
        selectBoxList.addAction(sequence(fadeOut(0.15f, Interpolation.fade), removeActor()))
    }

    /**
     * @author Nathan Sweet
     */
    class SelectBoxList<T>(private val selectBox: SelectBox<T?>?) : ScrollPane(null, selectBox!!.style!!.scrollStyle) {

        var maxListCount = 0
        private val screenPosition: Vector2? = Vector2()
        val list: List<T?>?
        private val hideListener: InputListener?
        private var previousScrollFocus: Actor? = null
        fun show(stage: Stage?) {
            if (list.isTouchable()) return
            stage.addActor(this)
            stage.addCaptureListener(hideListener)
            stage.addListener(list.getKeyListener())
            selectBox.localToStageCoordinates(screenPosition.set(0, 0))

            // Show the list above or below the select box, limited to a number of items and the available height in the stage.
            val itemHeight: Float = list.getItemHeight()
            var height = itemHeight * if (maxListCount <= 0) selectBox!!.items!!.size else java.lang.Math.min(maxListCount, selectBox!!.items!!.size)
            val scrollPaneBackground: Drawable = getStyle().background
            if (scrollPaneBackground != null) height += scrollPaneBackground.getTopHeight() + scrollPaneBackground.getBottomHeight()
            val listBackground: Drawable = list.getStyle().background
            if (listBackground != null) height += listBackground.getTopHeight() + listBackground.getBottomHeight()
            val heightBelow: Float = screenPosition.y
            val heightAbove: Float = stage.getCamera().viewportHeight - screenPosition.y - selectBox.getHeight()
            var below = true
            if (height > heightBelow) {
                if (heightAbove > heightBelow) {
                    below = false
                    height = java.lang.Math.min(height, heightAbove)
                } else height = heightBelow
            }
            if (below) setY(screenPosition.y - height) else setY(screenPosition.y + selectBox.getHeight())
            setX(screenPosition.x)
            setHeight(height)
            validate()
            var width: Float = java.lang.Math.max(getPrefWidth(), selectBox.getWidth())
            if (getPrefHeight() > height && !disableY) width += getScrollBarWidth()
            setWidth(width)
            validate()
            scrollTo(0, list.getHeight() - selectBox.selectedIndex * itemHeight - itemHeight / 2, 0, 0, true, true)
            updateVisualScroll()
            previousScrollFocus = null
            val actor: Actor = stage.getScrollFocus()
            if (actor != null && !actor.isDescendantOf(this)) previousScrollFocus = actor
            stage.setScrollFocus(this)
            list.selection.set(selectBox.selected)
            list.setTouchable(Touchable.enabled)
            clearActions()
            selectBox.onShow(this, below)
        }

        fun hide() {
            if (!list.isTouchable() || !hasParent()) return
            list.setTouchable(Touchable.disabled)
            val stage: Stage = getStage()
            if (stage != null) {
                stage.removeCaptureListener(hideListener)
                stage.removeListener(list.getKeyListener())
                if (previousScrollFocus != null && previousScrollFocus.getStage() == null) previousScrollFocus = null
                val actor: Actor = stage.getScrollFocus()
                if (actor == null || isAscendantOf(actor)) stage.setScrollFocus(previousScrollFocus)
            }
            clearActions()
            selectBox!!.onHide(this)
        }

        override fun draw(batch: Batch?, parentAlpha: Float) {
            selectBox.localToStageCoordinates(temp.set(0, 0))
            if (!temp.equals(screenPosition)) hide()
            super.draw(batch, parentAlpha)
        }

        override fun act(delta: Float) {
            super.act(delta)
            toFront()
        }

        protected fun setStage(stage: Stage?) {
            val oldStage: Stage = getStage()
            if (oldStage != null) {
                oldStage.removeCaptureListener(hideListener)
                oldStage.removeListener(list.getKeyListener())
            }
            super.setStage(stage)
        }

        init {
            setOverscroll(false, false)
            setFadeScrollBars(false)
            setScrollingDisabled(true, false)
            list = object : List<T?>(selectBox!!.style!!.listStyle) {
                fun toString(obj: T?): String? {
                    return selectBox!!.toString(obj)
                }
            }
            list.setTouchable(Touchable.disabled)
            list.setTypeToSelect(true)
            setActor(list)
            list.addListener(object : ClickListener() {
                fun clicked(event: InputEvent?, x: Float, y: Float) {
                    selectBox!!.selection.choose(list.getSelected())
                    hide()
                }

                fun mouseMoved(event: InputEvent?, x: Float, y: Float): Boolean {
                    val index: Int = list.getItemIndexAt(y)
                    if (index != -1) list.setSelectedIndex(index)
                    return true
                }
            })
            addListener(object : InputListener() {
                fun exit(event: InputEvent?, x: Float, y: Float, pointer: Int, toActor: Actor?) {
                    if (toActor == null || !isAscendantOf(toActor)) list.selection.set(selectBox!!.selected)
                }
            })
            hideListener = object : InputListener() {
                fun touchDown(event: InputEvent?, x: Float, y: Float, pointer: Int, button: Int): Boolean {
                    val target: Actor = event.getTarget()
                    if (isAscendantOf(target)) return false
                    list.selection.set(selectBox!!.selected)
                    hide()
                    return false
                }

                fun keyDown(event: InputEvent?, keycode: Int): Boolean {
                    when (keycode) {
                        Keys.ENTER -> {
                            selectBox!!.selection.choose(list.getSelected())
                            hide()
                            event.stop()
                            return true
                        }
                        Keys.ESCAPE -> {
                            hide()
                            event.stop()
                            return true
                        }
                    }
                    return false
                }
            }
        }
    }

    /**
     * The style for a select box, see [SelectBox].
     *
     * @author mzechner
     * @author Nathan Sweet
     */
    class SelectBoxStyle {

        var font: BitmapFont? = null
        var fontColor: Color? = Color(1, 1, 1, 1)

        /**
         * Optional.
         */
        var disabledFontColor: Color? = null

        /**
         * Optional.
         */
        var background: Drawable? = null
        var scrollStyle: ScrollPaneStyle? = null
        var listStyle: ListStyle? = null

        /**
         * Optional.
         */
        var backgroundOver: Drawable? = null
        var backgroundOpen: Drawable? = null
        var backgroundDisabled: Drawable? = null

        constructor() {}
        constructor(font: BitmapFont?, fontColor: Color?, background: Drawable?, scrollStyle: ScrollPaneStyle?,
                    listStyle: ListStyle?) {
            this.font = font
            this.fontColor.set(fontColor)
            this.background = background
            this.scrollStyle = scrollStyle
            this.listStyle = listStyle
        }

        constructor(style: SelectBoxStyle?) {
            font = style!!.font
            fontColor.set(style.fontColor)
            if (style.disabledFontColor != null) disabledFontColor = Color(style.disabledFontColor)
            background = style.background
            backgroundOver = style.backgroundOver
            backgroundOpen = style.backgroundOpen
            backgroundDisabled = style.backgroundDisabled
            scrollStyle = ScrollPaneStyle(style.scrollStyle)
            listStyle = ListStyle(style.listStyle)
        }
    }

    companion object {
        val temp: Vector2? = Vector2()
    }

    init {
        setStyle(style)
        setSize(getPrefWidth(), getPrefHeight())
        selection.setActor(this)
        selection.setRequired(true)
        selectBoxList = SelectBoxList<Any?>(this)
        addListener(object : ClickListener() {
            fun touchDown(event: InputEvent?, x: Float, y: Float, pointer: Int, button: Int): Boolean {
                if (pointer == 0 && button != 0) return false
                if (disabled) return false
                if (selectBoxList.hasParent()) hideList() else showList()
                return true
            }
        }.also { clickListener = it })
    }
}
