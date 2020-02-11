/*******************************************************************************
 * Copyright (c) 2011, Nathan Sweet <nathan.sweet></nathan.sweet>@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * * Neither the name of the <organization> nor the
 * names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
</COPYRIGHT></organization> */
package com.badlogic.gdx.scenes.scene2d.ui

/**
 * A stack is a container that sizes its children to its size and positions them at 0,0 on top of each other.
 *
 *
 * The preferred and min size of the stack is the largest preferred and min size of any children. The max size of the stack is the
 * smallest max size of any children.
 *
 * @author Nathan Sweet
 */
class Stack() : WidgetGroup() {

    private override var prefWidth = 0f
    private override var prefHeight = 0f
    private override var minWidth = 0f
    private override var minHeight = 0f
    private override var maxWidth = 0f
    private override var maxHeight = 0f
    private var sizeInvalid = true

    constructor(vararg actors: Actor?) : this() {
        for (actor in actors) addActor(actor)
    }

    override fun invalidate() {
        super.invalidate()
        sizeInvalid = true
    }

    private fun computeSize() {
        sizeInvalid = false
        prefWidth = 0f
        prefHeight = 0f
        minWidth = 0f
        minHeight = 0f
        maxWidth = 0f
        maxHeight = 0f
        val children: SnapshotArray<Actor?>? = getChildren()
        var i = 0
        val n: Int = children.size
        while (i < n) {
            val child: Actor = children.get(i)
            var childMaxWidth: Float
            var childMaxHeight: Float
            if (child is Layout) {
                val layout: Layout = child as Layout
                prefWidth = java.lang.Math.max(prefWidth, layout.getPrefWidth())
                prefHeight = java.lang.Math.max(prefHeight, layout.getPrefHeight())
                minWidth = java.lang.Math.max(minWidth, layout.getMinWidth())
                minHeight = java.lang.Math.max(minHeight, layout.getMinHeight())
                childMaxWidth = layout.getMaxWidth()
                childMaxHeight = layout.getMaxHeight()
            } else {
                prefWidth = java.lang.Math.max(prefWidth, child.getWidth())
                prefHeight = java.lang.Math.max(prefHeight, child.getHeight())
                minWidth = java.lang.Math.max(minWidth, child.getWidth())
                minHeight = java.lang.Math.max(minHeight, child.getHeight())
                childMaxWidth = 0f
                childMaxHeight = 0f
            }
            if (childMaxWidth > 0) maxWidth = if (maxWidth == 0f) childMaxWidth else java.lang.Math.min(maxWidth, childMaxWidth)
            if (childMaxHeight > 0) maxHeight = if (maxHeight == 0f) childMaxHeight else java.lang.Math.min(maxHeight, childMaxHeight)
            i++
        }
    }

    fun add(actor: Actor?) {
        addActor(actor)
    }

    override fun layout() {
        if (sizeInvalid) computeSize()
        val width = getWidth()
        val height = getHeight()
        val children: Array<Actor?>? = getChildren()
        var i = 0
        val n = children!!.size
        while (i < n) {
            val child: Actor? = children[i]
            child.setBounds(0, 0, width, height)
            if (child is Layout) (child as Layout?).validate()
            i++
        }
    }

    override fun getPrefWidth(): Float {
        if (sizeInvalid) computeSize()
        return prefWidth
    }

    override fun getPrefHeight(): Float {
        if (sizeInvalid) computeSize()
        return prefHeight
    }

    override fun getMinWidth(): Float {
        if (sizeInvalid) computeSize()
        return minWidth
    }

    override fun getMinHeight(): Float {
        if (sizeInvalid) computeSize()
        return minHeight
    }

    override fun getMaxWidth(): Float {
        if (sizeInvalid) computeSize()
        return maxWidth
    }

    override fun getMaxHeight(): Float {
        if (sizeInvalid) computeSize()
        return maxHeight
    }

    init {
        isTransform = false
        setWidth(150)
        setHeight(150)
        setTouchable(Touchable.childrenOnly)
    }
}
