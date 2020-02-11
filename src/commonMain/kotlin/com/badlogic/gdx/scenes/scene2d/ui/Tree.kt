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

import com.badlogic.gdx.scenes.scene2d.ui.TooltipManager
import com.badlogic.gdx.scenes.scene2d.ui.Touchpad.TouchpadStyle
import com.badlogic.gdx.scenes.scene2d.ui.Value.Fixed
import com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup

/**
 * A tree widget where each node has an icon, actor, and child nodes.
 *
 *
 * The preferred size of the tree is determined by the preferred size of the actors for the expanded nodes.
 *
 *
 * [ChangeEvent] is fired when the selected node changes.
 *
 * @param <N> The type of nodes in the tree.
 * @param <V> The type of values for each node.
 * @author Nathan Sweet
</V></N> */
class Tree<N : Node?, V>(style: TreeStyle?) : WidgetGroup() {

    var style: TreeStyle? = null
    val nodes: Array<N> = Array()
        get() = field
    val selection: Selection<N>

    /**
     * Sets the amount of vertical space between nodes.
     */
    var ySpacing = 4f
    var iconSpacingLeft = 2f
    var iconSpacingRight = 2f
    var paddingLeft = 0f
    var paddingRight = 0f

    /**
     * Returns the amount of horizontal space for indentation level.
     */
    var indentSpacing = 0f
    private var prefWidth = 0f
    private var prefHeight = 0f
    private var sizeInvalid = true
    private var foundNode: N? = null
    /**
     * @return May be null.
     */
    /**
     * @param overNode May be null.
     */
    var overNode: N? = null
    var rangeStart: N? = null
    private var clickListener: ClickListener? = null

    constructor(skin: Skin) : this(skin.get(TreeStyle::class.java)) {}
    constructor(skin: Skin, styleName: String?) : this(skin.get(styleName, TreeStyle::class.java)) {}

    private fun initialize() {
        addListener(object : ClickListener() {
            fun clicked(event: InputEvent?, x: Float, y: Float) {
                val node = getNodeAt(y) ?: return
                if (node !== getNodeAt(getTouchDownY())) return
                if (selection.getMultiple() && selection.notEmpty() && UIUtils.shift()) {
                    // Select range (shift).
                    if (rangeStart == null) rangeStart = node
                    val rangeStart: N = rangeStart
                    if (!UIUtils.ctrl()) selection.clear()
                    val start: Float = rangeStart.actor.getY()
                    val end: Float = node.actor.getY()
                    if (start > end) selectNodes(nodes, end, start) else {
                        selectNodes(nodes, start, end)
                        selection.items().orderedItems().reverse()
                    }
                    selection.fireChangeEvent()
                    this@Tree.rangeStart = rangeStart
                    return
                }
                if (node.children.size > 0 && (!selection.getMultiple() || !UIUtils.ctrl())) {
                    // Toggle expanded if left of icon.
                    var rowX: Float = node.actor.getX()
                    if (node.icon != null) rowX -= iconSpacingRight + node.icon.getMinWidth()
                    if (x < rowX) {
                        node.setExpanded(!node.expanded)
                        return
                    }
                }
                if (!node.isSelectable()) return
                selection.choose(node)
                if (!selection.isEmpty()) rangeStart = node
            }

            fun mouseMoved(event: InputEvent?, x: Float, y: Float): Boolean {
                overNode = getNodeAt(y)
                return false
            }

            fun enter(event: InputEvent?, x: Float, y: Float, pointer: Int, fromActor: Actor?) {
                super.enter(event, x, y, pointer, fromActor)
                overNode = getNodeAt(y)
            }

            fun exit(event: InputEvent?, x: Float, y: Float, pointer: Int, toActor: Actor?) {
                super.exit(event, x, y, pointer, toActor)
                if (toActor == null || !toActor.isDescendantOf(this@Tree)) overNode = null
            }
        }.also { clickListener = it })
    }

    fun setStyle(style: TreeStyle?) {
        this.style = style

        // Reasonable default.
        if (indentSpacing == 0f) indentSpacing = plusMinusWidth()
    }

    fun add(node: N) {
        insert(nodes.size, node)
    }

    fun insert(index: Int, node: N) {
        var index = index
        val existingIndex = nodes.indexOf(node, true)
        if (existingIndex != -1 && existingIndex < index) index--
        remove(node)
        node.parent = null
        nodes.insert(index, node)
        node.addToTree(this)
        invalidateHierarchy()
    }

    fun remove(node: N) {
        if (node.parent != null) {
            node.parent.remove(node)
            return
        }
        nodes.removeValue(node, true)
        node.removeFromTree(this)
        invalidateHierarchy()
    }

    /**
     * Removes all tree nodes.
     */
    fun clearChildren() {
        super.clearChildren()
        overNode = null
        nodes.clear()
        selection.clear()
    }

    fun invalidate() {
        super.invalidate()
        sizeInvalid = true
    }

    private fun plusMinusWidth(): Float {
        var width: Float = java.lang.Math.max(style!!.plus.getMinWidth(), style!!.minus.getMinWidth())
        if (style!!.plusOver != null) width = java.lang.Math.max(width, style!!.plusOver.getMinWidth())
        if (style!!.minusOver != null) width = java.lang.Math.max(width, style!!.minusOver.getMinWidth())
        return width
    }

    private fun computeSize() {
        sizeInvalid = false
        prefWidth = plusMinusWidth()
        prefHeight = 0f
        computeSize(nodes, 0f, prefWidth)
        prefWidth += paddingLeft + paddingRight
    }

    private fun computeSize(nodes: Array<N>, indent: Float, plusMinusWidth: Float) {
        val ySpacing = ySpacing
        val spacing = iconSpacingLeft + iconSpacingRight
        var i = 0
        val n = nodes.size
        while (i < n) {
            val node = nodes[i]
            var rowWidth = indent + plusMinusWidth
            val actor: Actor = node.actor
            if (actor is Layout) {
                val layout: Layout = actor as Layout
                rowWidth += layout.getPrefWidth()
                node.height = layout.getPrefHeight()
            } else {
                rowWidth += actor.getWidth()
                node.height = actor.getHeight()
            }
            if (node.icon != null) {
                rowWidth += spacing + node.icon.getMinWidth()
                node.height = java.lang.Math.max(node.height, node.icon.getMinHeight())
            }
            prefWidth = java.lang.Math.max(prefWidth, rowWidth)
            prefHeight += node.height + ySpacing
            if (node.expanded) computeSize(node.children, indent + indentSpacing, plusMinusWidth)
            i++
        }
    }

    fun layout() {
        if (sizeInvalid) computeSize()
        layout(nodes, paddingLeft, getHeight() - ySpacing / 2, plusMinusWidth())
    }

    private fun layout(nodes: Array<N>, indent: Float, y: Float, plusMinusWidth: Float): Float {
        var y = y
        val ySpacing = ySpacing
        val spacing = iconSpacingLeft + iconSpacingRight
        var i = 0
        val n = nodes.size
        while (i < n) {
            val node = nodes[i]
            var x = indent + plusMinusWidth
            if (node.icon != null) x += spacing + node.icon.getMinWidth()
            if (node.actor is Layout) (node.actor as Layout).pack()
            y -= node.getHeight()
            node.actor.setPosition(x, y)
            y -= ySpacing
            if (node.expanded) y = layout(node.children, indent + indentSpacing, y, plusMinusWidth)
            i++
        }
        return y
    }

    fun draw(batch: Batch, parentAlpha: Float) {
        drawBackground(batch, parentAlpha)
        val color: Color = getColor()
        batch.setColor(color.r, color.g, color.b, color.a * parentAlpha)
        draw(batch, nodes, paddingLeft, plusMinusWidth())
        super.draw(batch, parentAlpha) // Draw node actors.
    }

    /**
     * Called to draw the background. Default implementation draws the style background drawable.
     */
    protected fun drawBackground(batch: Batch, parentAlpha: Float) {
        if (style!!.background != null) {
            val color: Color = getColor()
            batch.setColor(color.r, color.g, color.b, color.a * parentAlpha)
            style!!.background.draw(batch, getX(), getY(), getWidth(), getHeight())
        }
    }

    /**
     * Draws selection, icons, and expand icons.
     */
    private fun draw(batch: Batch, nodes: Array<N>, indent: Float, plusMinusWidth: Float) {
        val cullingArea: Rectangle = getCullingArea()
        var cullBottom = 0f
        var cullTop = 0f
        if (cullingArea != null) {
            cullBottom = cullingArea.y
            cullTop = cullBottom + cullingArea.height
        }
        val style = style
        val x: Float = getX()
        val y: Float = getY()
        val expandX = x + indent
        val iconX = expandX + plusMinusWidth + iconSpacingLeft
        var i = 0
        val n = nodes.size
        while (i < n) {
            val node = nodes[i]
            val actor: Actor = node.actor
            val actorY: Float = actor.getY()
            val height: Float = node.height
            if (cullingArea == null || actorY + height >= cullBottom && actorY <= cullTop) {
                if (selection.contains(node) && style!!.selection != null) {
                    drawSelection(node, style.selection, batch, x, y + actorY - ySpacing / 2, getWidth(), height + ySpacing)
                } else if (node === overNode && style!!.over != null) {
                    drawOver(node, style.over, batch, x, y + actorY - ySpacing / 2, getWidth(), height + ySpacing)
                }
                if (node.icon != null) {
                    val iconY: Float = y + actorY + java.lang.Math.round((height - node.icon.getMinHeight()) / 2)
                    batch.setColor(actor.getColor())
                    drawIcon(node, node.icon, batch, iconX, iconY)
                    batch.setColor(1, 1, 1, 1)
                }
                if (node.children.size > 0) {
                    val expandIcon: Drawable? = getExpandIcon(node, iconX)
                    val iconY: Float = y + actorY + java.lang.Math.round((height - expandIcon.getMinHeight()) / 2)
                    drawExpandIcon(node, expandIcon, batch, expandX, iconY)
                }
            } else if (actorY < cullBottom) {
                return
            }
            if (node.expanded && node.children.size > 0) draw(batch, node.children, indent + indentSpacing, plusMinusWidth)
            i++
        }
    }

    protected fun drawSelection(node: N, selection: Drawable?, batch: Batch?, x: Float, y: Float, width: Float, height: Float) {
        selection.draw(batch, x, y, width, height)
    }

    protected fun drawOver(node: N, over: Drawable?, batch: Batch?, x: Float, y: Float, width: Float, height: Float) {
        over.draw(batch, x, y, width, height)
    }

    protected fun drawExpandIcon(node: N, expandIcon: Drawable?, batch: Batch?, x: Float, y: Float) {
        expandIcon.draw(batch, x, y, expandIcon.getMinWidth(), expandIcon.getMinHeight())
    }

    protected fun drawIcon(node: N, icon: Drawable, batch: Batch?, x: Float, y: Float) {
        icon.draw(batch, x, y, icon.getMinWidth(), icon.getMinHeight())
    }

    /**
     * Returns the drawable for the expand icon. The default implementation returns [TreeStyle.plusOver] or
     * [TreeStyle.minusOver] on the desktop if the node is the [over node][.getOverNode], the mouse is left of
     * `iconX`, and clicking would expand the node.
     *
     * @param iconX The X coordinate of the over node's icon.
     */
    protected fun getExpandIcon(node: N, iconX: Float): Drawable? {
        var over = false
        if (node === overNode //
            && Gdx.app.getType() === ApplicationType.Desktop //
            && (!selection.getMultiple() || !UIUtils.ctrl() && !UIUtils.shift()) //
        ) {
            val mouseX: Float = screenToLocalCoordinates(tmp.set(Gdx.input.getX(), 0)).x
            if (mouseX >= 0 && mouseX < iconX) over = true
        }
        if (over) {
            val icon: Drawable = if (node.expanded) style!!.minusOver else style!!.plusOver
            if (icon != null) return icon
        }
        return if (node.expanded) style!!.minus else style!!.plus
    }

    /**
     * @return May be null.
     */
    fun getNodeAt(y: Float): N? {
        foundNode = null
        getNodeAt(nodes, y, getHeight())
        return foundNode
    }

    private fun getNodeAt(nodes: Array<N>, y: Float, rowY: Float): Float {
        var rowY = rowY
        var i = 0
        val n = nodes.size
        while (i < n) {
            val node = nodes[i]
            val height: Float = node.height
            rowY -= node.getHeight() - height // Node subclass may increase getHeight.
            if (y >= rowY - height - ySpacing && y < rowY) {
                foundNode = node
                return (-1).toFloat()
            }
            rowY -= height + ySpacing
            if (node.expanded) {
                rowY = getNodeAt(node.children, y, rowY)
                if (rowY == -1f) return (-1).toFloat()
            }
            i++
        }
        return rowY
    }

    fun selectNodes(nodes: Array<N>, low: Float, high: Float) {
        var i = 0
        val n = nodes.size
        while (i < n) {
            val node = nodes[i]
            if (node.actor.getY() < low) break
            if (!node.isSelectable()) {
                i++
                continue
            }
            if (node.actor.getY() <= high) selection.add(node)
            if (node.expanded) selectNodes(node.children, low, high)
            i++
        }
    }

    fun getSelection(): Selection<N> {
        return selection
    }

    /**
     * Returns the first selected node, or null.
     */
    val selectedNode: N
        get() = selection.first()

    /**
     * Returns the first selected value, or null.
     */
    val selectedValue: V?
        get() {
            val node: N = selection.first()
            return if (node == null) null else node.getValue() as V
        }

    fun getStyle(): TreeStyle? {
        return style
    }

    /**
     * Removes the root node actors from the tree and adds them again. This is useful after changing the order of
     * [.getRootNodes].
     *
     * @see Node.updateChildren
     */
    fun updateRootNodes() {
        for (i in nodes.size - 1 downTo 0) nodes[i].removeFromTree(this)
        var i = 0
        val n = nodes.size
        while (i < n) {
            nodes[i].addToTree(this)
            i++
        }
    }

    /**
     * @return May be null.
     */
    val overValue: V?
        get() = if (overNode == null) null else overNode.getValue() as V

    /**
     * Sets the amount of horizontal space between the nodes and the left/right edges of the tree.
     */
    fun setPadding(padding: Float) {
        paddingLeft = padding
        paddingRight = padding
    }

    /**
     * Sets the amount of horizontal space between the nodes and the left/right edges of the tree.
     */
    fun setPadding(left: Float, right: Float) {
        paddingLeft = left
        paddingRight = right
    }

    /**
     * Sets the amount of horizontal space left and right of the node's icon.
     */
    fun setIconSpacing(left: Float, right: Float) {
        iconSpacingLeft = left
        iconSpacingRight = right
    }

    fun getPrefWidth(): Float {
        if (sizeInvalid) computeSize()
        return prefWidth
    }

    fun getPrefHeight(): Float {
        if (sizeInvalid) computeSize()
        return prefHeight
    }

    fun findExpandedValues(values: Array<V>) {
        findExpandedValues(nodes, values)
    }

    fun restoreExpandedValues(values: Array<V>) {
        var i = 0
        val n = values.size
        while (i < n) {
            val node = findNode(values[i])
            if (node != null) {
                node.setExpanded(true)
                node.expandTo()
            }
            i++
        }
    }

    /**
     * Returns the node with the specified value, or null.
     */
    fun findNode(value: V?): N? {
        if (value == null) throw java.lang.IllegalArgumentException("value cannot be null.")
        return findNode(nodes, value) as N?
    }

    fun collapseAll() {
        collapseAll(nodes)
    }

    fun expandAll() {
        expandAll(nodes)
    }

    /**
     * Returns the click listener the tree uses for clicking on nodes and the over node.
     */
    fun getClickListener(): ClickListener? {
        return clickListener
    }

    /**
     * A [Tree] node which has an actor and value.
     *
     *
     * A subclass can be used so the generic type parameters don't need to be specified repeatedly.
     *
     * @param <N> The type for the node's parent and child nodes.
     * @param <V> The type for the node's value.
     * @param <A> The type for the node's actor.
     * @author Nathan Sweet
    </A></V></N> */
    abstract class Node<N : Node<*, *, *>?, V, A : Actor?> {

        var actor: A? = null

        /**
         * @return May be null.
         */
        var parent: N? = null

        /**
         * If the children order is changed, [.updateChildren] must be called.
         */
        val children: Array<N> = Array(0)
        var isSelectable = true
        var expanded = false
        var icon: Drawable? = null

        /**
         * Returns the height of the node as calculated for layout. A subclass may override and increase the returned height to
         * create a blank space in the tree above the node, eg for a separator.
         */
        var height = 0f
        var value: V? = null

        constructor(actor: A?) {
            if (actor == null) throw java.lang.IllegalArgumentException("actor cannot be null.")
            this.actor = actor
        }

        /**
         * Creates a node without an actor. An actor must be set using [.setActor] before this node can be used.
         */
        constructor() {}

        fun setExpanded(expanded: Boolean) {
            if (expanded == this.expanded) return
            this.expanded = expanded
            if (children.size === 0) return
            val tree = tree ?: return
            if (expanded) {
                var i = 0
                val n = children.size
                while (i < n) {
                    children[i]!!.addToTree(tree)
                    i++
                }
            } else {
                for (i in children.size - 1 downTo 0) children[i]!!.removeFromTree(tree)
            }
            tree.invalidateHierarchy()
        }

        /**
         * Called to add the actor to the tree when the node's parent is expanded.
         */
        protected fun addToTree(tree: Tree<N, V>) {
            tree.addActor(actor)
            if (!expanded) return
            val children: Array<Any> = children.items
            for (i in this.children.size - 1 downTo 0) (children[i] as N)!!.addToTree(tree)
        }

        /**
         * Called to remove the actor from the tree when the node's parent is collapsed.
         */
        protected fun removeFromTree(tree: Tree<N, V>) {
            tree.removeActor(actor)
            if (!expanded) return
            val children: Array<Any> = children.items
            for (i in this.children.size - 1 downTo 0) (children[i] as N)!!.removeFromTree(tree)
        }

        fun add(node: N) {
            insert(children.size, node)
        }

        fun addAll(nodes: Array<N>) {
            var i = 0
            val n = nodes.size
            while (i < n) {
                insert(children.size, nodes[i])
                i++
            }
        }

        fun insert(index: Int, node: N) {
            node!!.parent = this
            children.insert(index, node)
            updateChildren()
        }

        fun remove() {
            val tree: Tree<*, *>? = tree
            if (tree != null) tree.remove(this) else if (parent != null) //
                parent.remove(this)
        }

        fun remove(node: N) {
            children.removeValue(node, true)
            if (!expanded) return
            val tree: Tree<*, *>? = tree
            if (tree != null) node!!.removeFromTree(tree)
        }

        fun removeAll() {
            val tree: Tree<*, *>? = tree
            if (tree != null) {
                val children: Array<Any> = children.items
                for (i in this.children.size - 1 downTo 0) (children[i] as N)!!.removeFromTree(tree)
            }
            children.clear()
        }

        /**
         * Returns the tree this node's actor is currently in, or null. The actor is only in the tree when all of its parent nodes
         * are expanded.
         */
        val tree: Tree<N, V>?
            get() {
                val parent: Group = actor.getParent()
                return if (parent is Tree<*, *>) parent else null
            }

        fun setActor(newActor: A) {
            if (actor != null) {
                val tree = tree
                if (tree != null) {
                    actor.remove()
                    tree.addActor(newActor)
                }
            }
            actor = newActor
        }

        fun isExpanded(): Boolean {
            return expanded
        }

        fun hasChildren(): Boolean {
            return children.size > 0
        }

        /**
         * Removes the child node actors from the tree and adds them again. This is useful after changing the order of
         * [.getChildren].
         *
         * @see Tree.updateRootNodes
         */
        fun updateChildren() {
            if (!expanded) return
            val tree = tree ?: return
            for (i in children.size - 1 downTo 0) children[i]!!.removeFromTree(tree)
            var i = 0
            val n = children.size
            while (i < n) {
                children[i]!!.addToTree(tree)
                i++
            }
        }

        /**
         * Sets an icon that will be drawn to the left of the actor.
         */
        fun setIcon(icon: Drawable?) {
            this.icon = icon
        }

        /**
         * Sets an application specific value for this node.
         */
        fun setValue(value: V) {
            this.value = value
        }

        fun getIcon(): Drawable? {
            return icon
        }

        val level: Int
            get() {
                var level = 0
                var current: Node<*, *, *>? = this
                do {
                    level++
                    current = current!!.parent
                } while (current != null)
                return level
            }

        /**
         * Returns this node or the child node with the specified value, or null.
         */
        fun findNode(value: V?): N? {
            if (value == null) throw java.lang.IllegalArgumentException("value cannot be null.")
            return if (value == this.value) this as N else findNode(children, value) as N?
        }

        /**
         * Collapses all nodes under and including this node.
         */
        fun collapseAll() {
            setExpanded(false)
            collapseAll(children)
        }

        /**
         * Expands all nodes under and including this node.
         */
        fun expandAll() {
            setExpanded(true)
            if (children.size > 0) expandAll(children)
        }

        /**
         * Expands all parent nodes of this node.
         */
        fun expandTo() {
            var node: Node<*, *, *>? = parent
            while (node != null) {
                node.setExpanded(true)
                node = node.parent
            }
        }

        fun findExpandedValues(values: Array<V>) {
            if (expanded && !findExpandedValues(children, values)) values.add(value)
        }

        fun restoreExpandedValues(values: Array<V>) {
            var i = 0
            val n = values.size
            while (i < n) {
                val node = findNode(values[i])
                if (node != null) {
                    node.setExpanded(true)
                    node.expandTo()
                }
                i++
            }
        }

        /**
         * Returns true if the specified node is this node or an ascendant of this node.
         */
        fun isAscendantOf(node: N?): Boolean {
            if (node == null) throw java.lang.IllegalArgumentException("node cannot be null.")
            var current: Node<*, *, *>? = node
            do {
                if (current === this) return true
                current = current!!.parent
            } while (current != null)
            return false
        }

        /**
         * Returns true if the specified node is this node or an descendant of this node.
         */
        fun isDescendantOf(node: N?): Boolean {
            if (node == null) throw java.lang.IllegalArgumentException("node cannot be null.")
            var parent: Node<*, *, *>? = this
            do {
                if (parent === node) return true
                parent = parent!!.parent
            } while (parent != null)
            return false
        }
    }

    /**
     * The style for a [Tree].
     *
     * @author Nathan Sweet
     */
    class TreeStyle {

        var plus: Drawable? = null
        var minus: Drawable? = null

        /**
         * Optional.
         */
        var plusOver: Drawable? = null
        var minusOver: Drawable? = null
        var over: Drawable? = null
        var selection: Drawable? = null
        var background: Drawable? = null

        constructor() {}
        constructor(plus: Drawable?, minus: Drawable?, selection: Drawable?) {
            this.plus = plus
            this.minus = minus
            this.selection = selection
        }

        constructor(style: TreeStyle) {
            plus = style.plus
            minus = style.minus
            plusOver = style.plusOver
            minusOver = style.minusOver
            over = style.over
            selection = style.selection
            background = style.background
        }
    }

    companion object {
        private val tmp: Vector2 = Vector2()
        fun findExpandedValues(nodes: Array<out Node<*, *, *>>, values: Array): Boolean {
            val expanded = false
            var i = 0
            val n = nodes.size
            while (i < n) {
                val node = nodes[i]
                if (node.expanded && !findExpandedValues(node.children, values)) values.add(node.value)
                i++
            }
            return expanded
        }

        fun findNode(nodes: Array<out Node<*, *, *>>, value: Any): Node<*, *, *>? {
            run {
                var i = 0
                val n = nodes.size
                while (i < n) {
                    val node = nodes[i]
                    if (value == node.value) return node
                    i++
                }
            }
            var i = 0
            val n = nodes.size
            while (i < n) {
                val node = nodes[i]
                val found = findNode(node.children, value)
                if (found != null) return found
                i++
            }
            return null
        }

        fun collapseAll(nodes: Array<out Node<*, *, *>>) {
            var i = 0
            val n = nodes.size
            while (i < n) {
                val node = nodes[i]
                node.setExpanded(false)
                collapseAll(node.children)
                i++
            }
        }

        fun expandAll(nodes: Array<out Node<*, *, *>>) {
            var i = 0
            val n = nodes.size
            while (i < n) {
                nodes[i].expandAll()
                i++
            }
        }
    }

    init {
        selection = object : Selection<N>() {
            protected fun changed() {
                when (size()) {
                    0 -> rangeStart = null
                    1 -> rangeStart = first()
                }
            }
        }
        selection.setActor(this)
        selection.setMultiple(true)
        setStyle(style)
        initialize()
    }
}
