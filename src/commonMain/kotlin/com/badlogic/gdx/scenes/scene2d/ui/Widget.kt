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
 * An [Actor] that participates in layout and provides a minimum, preferred, and maximum size.
 *
 *
 * The default preferred size of a widget is 0 and this is almost always overridden by a subclass. The default minimum size
 * returns the preferred size, so a subclass may choose to return 0 if it wants to allow itself to be sized smaller. The default
 * maximum size is 0, which means no maximum size.
 *
 *
 * See [Layout] for details on how a widget should participate in layout. A widget's mutator methods should call
 * [.invalidate] or [.invalidateHierarchy] as needed.
 *
 * @author mzechner
 * @author Nathan Sweet
 */
class Widget : Actor(), Layout {

    private var needsLayout = true
    private var fillParent = false
    private var layoutEnabled = true
    val minWidth: Float
        get() = prefWidth

    val minHeight: Float
        get() = prefHeight

    val prefWidth: Float
        get() = 0

    val prefHeight: Float
        get() = 0

    val maxWidth: Float
        get() = 0

    val maxHeight: Float
        get() = 0

    fun setLayoutEnabled(enabled: Boolean) {
        layoutEnabled = enabled
        if (enabled) invalidateHierarchy()
    }

    fun validate() {
        if (!layoutEnabled) return
        val parent: Group = getParent()
        if (fillParent && parent != null) {
            val parentWidth: Float
            val parentHeight: Float
            val stage: Stage = getStage()
            if (stage != null && parent === stage.getRoot()) {
                parentWidth = stage.getWidth()
                parentHeight = stage.getHeight()
            } else {
                parentWidth = parent.getWidth()
                parentHeight = parent.getHeight()
            }
            setSize(parentWidth, parentHeight)
        }
        if (!needsLayout) return
        needsLayout = false
        layout()
    }

    /**
     * Returns true if the widget's layout has been [invalidated][.invalidate].
     */
    fun needsLayout(): Boolean {
        return needsLayout
    }

    fun invalidate() {
        needsLayout = true
    }

    fun invalidateHierarchy() {
        if (!layoutEnabled) return
        invalidate()
        val parent: Group = getParent()
        if (parent is Layout) (parent as Layout).invalidateHierarchy()
    }

    protected fun sizeChanged() {
        invalidate()
    }

    fun pack() {
        setSize(prefWidth, prefHeight)
        validate()
    }

    fun setFillParent(fillParent: Boolean) {
        this.fillParent = fillParent
    }

    /**
     * If this method is overridden, the super method or [.validate] should be called to ensure the widget is laid out.
     */
    fun draw(batch: Batch?, parentAlpha: Float) {
        validate()
    }

    fun layout() {}
}
