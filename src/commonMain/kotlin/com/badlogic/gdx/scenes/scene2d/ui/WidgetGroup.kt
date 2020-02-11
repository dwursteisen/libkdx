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

import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.utils.Layout

/**
 * A [Group] that participates in layout and provides a minimum, preferred, and maximum size.
 *
 *
 * The default preferred size of a widget group is 0 and this is almost always overridden by a subclass. The default minimum size
 * returns the preferred size, so a subclass may choose to return 0 for minimum size if it wants to allow itself to be sized
 * smaller than the preferred size. The default maximum size is 0, which means no maximum size.
 *
 *
 * See [Layout] for details on how a widget group should participate in layout. A widget group's mutator methods should call
 * [.invalidate] or [.invalidateHierarchy] as needed. By default, invalidateHierarchy is called when child widgets
 * are added and removed.
 *
 * @author Nathan Sweet
 */
open class WidgetGroup : Group, Layout {

    private var needsLayout = true
    private var fillParent = false
    private var layoutEnabled = true

    constructor() {}

    /**
     * Creates a new widget group containing the specified actors.
     */
    constructor(vararg actors: Actor?) {
        for (actor in actors) addActor(actor)
    }

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
        setLayoutEnabled(this, enabled)
    }

    private fun setLayoutEnabled(parent: Group, enabled: Boolean) {
        val children: SnapshotArray<Actor> = parent.getChildren()
        var i = 0
        val n: Int = children.size
        while (i < n) {
            val actor: Actor = children.get(i)
            if (actor is Layout) (actor as Layout).setLayoutEnabled(enabled) else if (actor is Group) //
                setLayoutEnabled(actor as Group, enabled)
            i++
        }
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
            if (getWidth() !== parentWidth || getHeight() !== parentHeight) {
                setWidth(parentWidth)
                setHeight(parentHeight)
                invalidate()
            }
        }
        if (!needsLayout) return
        needsLayout = false
        layout()

        // Widgets may call invalidateHierarchy during layout (eg, a wrapped label). The root-most widget group retries layout a
        // reasonable number of times.
        if (needsLayout) {
            if (parent is WidgetGroup) return  // The parent widget will layout again.
            for (i in 0..4) {
                needsLayout = false
                layout()
                if (!needsLayout) break
            }
        }
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
        invalidate()
        val parent: Group = getParent()
        if (parent is Layout) (parent as Layout).invalidateHierarchy()
    }

    protected fun childrenChanged() {
        invalidateHierarchy()
    }

    protected fun sizeChanged() {
        invalidate()
    }

    fun pack() {
        setSize(prefWidth, prefHeight)
        validate()
        // Validating the layout may change the pref size. Eg, a wrapped label doesn't know its pref height until it knows its
        // width, so it calls invalidateHierarchy() in layout() if its pref height has changed.
        setSize(prefWidth, prefHeight)
        validate()
    }

    fun setFillParent(fillParent: Boolean) {
        this.fillParent = fillParent
    }

    fun layout() {}

    /**
     * If this method is overridden, the super method or [.validate] should be called to ensure the widget group is laid
     * out.
     */
    fun draw(batch: Batch?, parentAlpha: Float) {
        validate()
        super.draw(batch, parentAlpha)
    }
}
