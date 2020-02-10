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
package com.badlogic.gdx.scenes.scene2d

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.Stage.TouchFocus
import java.lang.RuntimeException

/**
 * 2D scene graph node that may contain other actors.
 *
 *
 * Actors have a z-order equal to the order they were inserted into the group. Actors inserted later will be drawn on top of
 * actors added earlier. Touch events that hit more than one actor are distributed to topmost actors first.
 *
 * @author mzechner
 * @author Nathan Sweet
 */
class Group : Actor(), Cullable {

    val children: SnapshotArray<Actor?>? = SnapshotArray(true, 4, Actor::class.java)
    private val worldTransform: Affine2? = Affine2()
    private val computedTransform: Matrix4? = Matrix4()
    private val oldTransform: Matrix4? = Matrix4()

    /**
     * When true (the default), the Batch is transformed so children are drawn in their parent's coordinate system. This has a
     * performance impact because [Batch.flush] must be done before and after the transform. If the actors in a group are
     * not rotated or scaled, then the transform for the group can be set to false. In this case, each child's position will be
     * offset by the group's position for drawing, causing the children to appear in the correct location even though the Batch has
     * not been transformed.
     */
    var isTransform = true
    private var cullingArea: Rectangle? = null
    override fun act(delta: Float) {
        super.act(delta)
        val actors: Array<Actor?> = children.begin()
        var i = 0
        val n: Int = children.size
        while (i < n) {
            actors[i]!!.act(delta)
            i++
        }
        children.end()
    }

    /**
     * Draws the group and its children. The default implementation calls [.applyTransform] if needed, then
     * [.drawChildren], then [.resetTransform] if needed.
     */
    override fun draw(batch: Batch?, parentAlpha: Float) {
        if (isTransform) applyTransform(batch, computeTransform())
        drawChildren(batch, parentAlpha)
        if (isTransform) resetTransform(batch)
    }

    /**
     * Draws all children. [.applyTransform] should be called before and [.resetTransform]
     * after this method if [transform][.setTransform] is true. If [transform][.setTransform] is false
     * these methods don't need to be called, children positions are temporarily offset by the group position when drawn. This
     * method avoids drawing children completely outside the [culling area][.setCullingArea], if set.
     */
    protected fun drawChildren(batch: Batch?, parentAlpha: Float) {
        var parentAlpha = parentAlpha
        parentAlpha *= this.color.a
        val children: SnapshotArray<Actor?>? = children
        val actors: Array<Actor?> = children.begin()
        val cullingArea: Rectangle? = cullingArea
        if (cullingArea != null) {
            // Draw children only if inside culling area.
            val cullLeft: Float = cullingArea.x
            val cullRight: Float = cullLeft + cullingArea.width
            val cullBottom: Float = cullingArea.y
            val cullTop: Float = cullBottom + cullingArea.height
            if (isTransform) {
                var i = 0
                val n: Int = children.size
                while (i < n) {
                    val child: Actor? = actors[i]
                    if (!child!!.isVisible()) {
                        i++
                        continue
                    }
                    val cx: Float = child!!.x
                    val cy: Float = child!!.y
                    if (cx <= cullRight && cy <= cullTop && cx + child!!.width >= cullLeft && cy + child!!.height >= cullBottom) child!!.draw(batch, parentAlpha)
                    i++
                }
            } else {
                // No transform for this group, offset each child.
                val offsetX: Float = x
                val offsetY: Float = y
                x = 0
                y = 0
                var i = 0
                val n: Int = children.size
                while (i < n) {
                    val child: Actor? = actors[i]
                    if (!child!!.isVisible()) {
                        i++
                        continue
                    }
                    val cx: Float = child!!.x
                    val cy: Float = child!!.y
                    if (cx <= cullRight && cy <= cullTop && cx + child!!.width >= cullLeft && cy + child!!.height >= cullBottom) {
                        child!!.x = cx + offsetX
                        child!!.y = cy + offsetY
                        child!!.draw(batch, parentAlpha)
                        child!!.x = cx
                        child!!.y = cy
                    }
                    i++
                }
                x = offsetX
                y = offsetY
            }
        } else {
            // No culling, draw all children.
            if (isTransform) {
                var i = 0
                val n: Int = children.size
                while (i < n) {
                    val child: Actor? = actors[i]
                    if (!child!!.isVisible()) {
                        i++
                        continue
                    }
                    child!!.draw(batch, parentAlpha)
                    i++
                }
            } else {
                // No transform for this group, offset each child.
                val offsetX: Float = x
                val offsetY: Float = y
                x = 0
                y = 0
                var i = 0
                val n: Int = children.size
                while (i < n) {
                    val child: Actor? = actors[i]
                    if (!child!!.isVisible()) {
                        i++
                        continue
                    }
                    val cx: Float = child!!.x
                    val cy: Float = child!!.y
                    child!!.x = cx + offsetX
                    child!!.y = cy + offsetY
                    child!!.draw(batch, parentAlpha)
                    child!!.x = cx
                    child!!.y = cy
                    i++
                }
                x = offsetX
                y = offsetY
            }
        }
        children.end()
    }

    /**
     * Draws this actor's debug lines if [.getDebug] is true and, regardless of [.getDebug], calls
     * [Actor.drawDebug] on each child.
     */
    override fun drawDebug(shapes: ShapeRenderer?) {
        drawDebugBounds(shapes)
        if (isTransform) applyTransform(shapes, computeTransform())
        drawDebugChildren(shapes)
        if (isTransform) resetTransform(shapes)
    }

    /**
     * Draws all children. [.applyTransform] should be called before and [.resetTransform]
     * after this method if [transform][.setTransform] is true. If [transform][.setTransform] is false
     * these methods don't need to be called, children positions are temporarily offset by the group position when drawn. This
     * method avoids drawing children completely outside the [culling area][.setCullingArea], if set.
     */
    protected fun drawDebugChildren(shapes: ShapeRenderer?) {
        val children: SnapshotArray<Actor?>? = children
        val actors: Array<Actor?> = children.begin()
        // No culling, draw all children.
        if (isTransform) {
            var i = 0
            val n: Int = children.size
            while (i < n) {
                val child: Actor? = actors[i]
                if (!child!!.isVisible()) {
                    i++
                    continue
                }
                if (!child!!.getDebug() && child !is Group) {
                    i++
                    continue
                }
                child!!.drawDebug(shapes)
                i++
            }
            shapes.flush()
        } else {
            // No transform for this group, offset each child.
            val offsetX: Float = x
            val offsetY: Float = y
            x = 0
            y = 0
            var i = 0
            val n: Int = children.size
            while (i < n) {
                val child: Actor? = actors[i]
                if (!child!!.isVisible()) {
                    i++
                    continue
                }
                if (!child!!.getDebug() && child !is Group) {
                    i++
                    continue
                }
                val cx: Float = child!!.x
                val cy: Float = child!!.y
                child!!.x = cx + offsetX
                child!!.y = cy + offsetY
                child!!.drawDebug(shapes)
                child!!.x = cx
                child!!.y = cy
                i++
            }
            x = offsetX
            y = offsetY
        }
        children.end()
    }

    /**
     * Returns the transform for this group's coordinate system.
     */
    protected fun computeTransform(): Matrix4? {
        val worldTransform: Affine2? = worldTransform
        val originX: Float = this.originX
        val originY: Float = this.originY
        worldTransform.setToTrnRotScl(x + originX, y + originY, rotation, scaleX, scaleY)
        if (originX != 0f || originY != 0f) worldTransform.translate(-originX, -originY)

        // Find the first parent that transforms.
        var parentGroup: Group? = parent
        while (parentGroup != null) {
            if (parentGroup.isTransform) break
            parentGroup = parentGroup.parent
        }
        if (parentGroup != null) worldTransform.preMul(parentGroup.worldTransform)
        computedTransform.set(worldTransform)
        return computedTransform
    }

    /**
     * Set the batch's transformation matrix, often with the result of [.computeTransform]. Note this causes the batch to
     * be flushed. [.resetTransform] will restore the transform to what it was before this call.
     */
    protected fun applyTransform(batch: Batch?, transform: Matrix4?) {
        oldTransform.set(batch.getTransformMatrix())
        batch.setTransformMatrix(transform)
    }

    /**
     * Restores the batch transform to what it was before [.applyTransform]. Note this causes the batch to
     * be flushed.
     */
    protected fun resetTransform(batch: Batch?) {
        batch.setTransformMatrix(oldTransform)
    }

    /**
     * Set the shape renderer transformation matrix, often with the result of [.computeTransform]. Note this causes the
     * shape renderer to be flushed. [.resetTransform] will restore the transform to what it was before this
     * call.
     */
    protected fun applyTransform(shapes: ShapeRenderer?, transform: Matrix4?) {
        oldTransform.set(shapes.getTransformMatrix())
        shapes.setTransformMatrix(transform)
        shapes.flush()
    }

    /**
     * Restores the shape renderer transform to what it was before [.applyTransform]. Note this causes the
     * shape renderer to be flushed.
     */
    protected fun resetTransform(shapes: ShapeRenderer?) {
        shapes.setTransformMatrix(oldTransform)
    }

    /**
     * Children completely outside of this rectangle will not be drawn. This is only valid for use with unrotated and unscaled
     * actors.
     *
     * @param cullingArea May be null.
     */
    fun setCullingArea(cullingArea: Rectangle?) {
        this.cullingArea = cullingArea
    }

    /**
     * @return May be null.
     * @see .setCullingArea
     */
    fun getCullingArea(): Rectangle? {
        return cullingArea
    }

    override fun hit(x: Float, y: Float, touchable: Boolean): Actor? {
        if (touchable && getTouchable() === Touchable.disabled) return null
        if (!isVisible()) return null
        val point: Vector2? = tmp
        val childrenArray: Array<Actor?> = children.items
        for (i in children.size - 1 downTo 0) {
            val child: Actor? = childrenArray[i]
            child!!.parentToLocalCoordinates(point.set(x, y))
            val hit: Actor = child!!.hit(point.x, point.y, touchable)
            if (hit != null) return hit
        }
        return super.hit(x, y, touchable)
    }

    /**
     * Called when actors are added to or removed from the group.
     */
    fun childrenChanged() {}

    /**
     * Adds an actor as a child of this group, removing it from its previous parent. If the actor is already a child of this
     * group, no changes are made.
     */
    fun addActor(actor: Actor?) {
        if (actor!!.parent != null) {
            if (actor!!.parent === this) return
            actor!!.parent.removeActor(actor, false)
        }
        children.add(actor)
        actor!!.setParent(this)
        actor!!.setStage(getStage())
        childrenChanged()
    }

    /**
     * Adds an actor as a child of this group at a specific index, removing it from its previous parent. If the actor is already a
     * child of this group, no changes are made.
     *
     * @param index May be greater than the number of children.
     */
    fun addActorAt(index: Int, actor: Actor?) {
        if (actor!!.parent != null) {
            if (actor!!.parent === this) return
            actor!!.parent.removeActor(actor, false)
        }
        if (index >= children.size) children.add(actor) else children.insert(index, actor)
        actor!!.setParent(this)
        actor!!.setStage(getStage())
        childrenChanged()
    }

    /**
     * Adds an actor as a child of this group immediately before another child actor, removing it from its previous parent. If the
     * actor is already a child of this group, no changes are made.
     */
    fun addActorBefore(actorBefore: Actor?, actor: Actor?) {
        if (actor!!.parent != null) {
            if (actor!!.parent === this) return
            actor!!.parent.removeActor(actor, false)
        }
        val index: Int = children.indexOf(actorBefore, true)
        children.insert(index, actor)
        actor!!.setParent(this)
        actor!!.setStage(getStage())
        childrenChanged()
    }

    /**
     * Adds an actor as a child of this group immediately after another child actor, removing it from its previous parent. If the
     * actor is already a child of this group, no changes are made.
     */
    fun addActorAfter(actorAfter: Actor?, actor: Actor?) {
        if (actor!!.parent != null) {
            if (actor!!.parent === this) return
            actor!!.parent.removeActor(actor, false)
        }
        val index: Int = children.indexOf(actorAfter, true)
        if (index == children.size) children.add(actor) else children.insert(index + 1, actor)
        actor!!.setParent(this)
        actor!!.setStage(getStage())
        childrenChanged()
    }

    /**
     * Removes an actor from this group and unfocuses it. Calls [.removeActor] with true.
     */
    fun removeActor(actor: Actor?): Boolean {
        return removeActor(actor, true)
    }

    /**
     * Removes an actor from this group. If the actor will not be used again and has actions, they should be
     * [cleared][Actor.clearActions] so the actions will be returned to their
     * [pool][Action.setPool], if any. This is not done automatically.
     *
     * @param unfocus If true, [Stage.unfocus] is called.
     * @return true if the actor was removed from this group.
     */
    fun removeActor(actor: Actor?, unfocus: Boolean): Boolean {
        if (!children.removeValue(actor, true)) return false
        if (unfocus) {
            val stage: Stage = getStage()
            if (stage != null) stage.unfocus(actor)
        }
        actor!!.setParent(null)
        actor!!.setStage(null)
        childrenChanged()
        return true
    }

    /**
     * Removes all actors from this group.
     */
    fun clearChildren() {
        val actors: Array<Actor?> = children.begin()
        var i = 0
        val n: Int = children.size
        while (i < n) {
            val child: Actor? = actors[i]
            child!!.setStage(null)
            child!!.setParent(null)
            i++
        }
        children.end()
        children.clear()
        childrenChanged()
    }

    /**
     * Removes all children, actions, and listeners from this group.
     */
    override fun clear() {
        super.clear()
        clearChildren()
    }

    /**
     * Returns the first actor found with the specified name. Note this recursively compares the name of every actor in the
     * group.
     */
    fun <T : Actor?> findActor(name: String?): T? {
        val children: Array<Actor?>? = children
        run {
            var i = 0
            val n = children!!.size
            while (i < n) {
                if (name == children[i].getName()) return children[i] as T?
                i++
            }
        }
        var i = 0
        val n = children!!.size
        while (i < n) {
            val child: Actor? = children[i]
            if (child is Group) {
                val actor: Actor = (child as Group?)!!.findActor<Actor?>(name)
                if (actor != null) return actor as T
            }
            i++
        }
        return null
    }

    override fun setStage(stage: Stage?) {
        super.setStage(stage)
        val childrenArray: Array<Actor?> = children.items
        var i = 0
        val n: Int = children.size
        while (i < n) {
            childrenArray[i]!!.setStage(stage) // StackOverflowError here means the group is its own ancestor.
            i++
        }
    }

    /**
     * Swaps two actors by index. Returns false if the swap did not occur because the indexes were out of bounds.
     */
    fun swapActor(first: Int, second: Int): Boolean {
        val maxIndex: Int = children.size
        if (first < 0 || first >= maxIndex) return false
        if (second < 0 || second >= maxIndex) return false
        children.swap(first, second)
        return true
    }

    /**
     * Swaps two actors. Returns false if the swap did not occur because the actors are not children of this group.
     */
    fun swapActor(first: Actor?, second: Actor?): Boolean {
        val firstIndex: Int = children.indexOf(first, true)
        val secondIndex: Int = children.indexOf(second, true)
        if (firstIndex == -1 || secondIndex == -1) return false
        children.swap(firstIndex, secondIndex)
        return true
    }

    /**
     * Returns the child at the specified index.
     */
    fun getChild(index: Int): Actor? {
        return children.get(index)
    }

    /**
     * Returns an ordered list of child actors in this group.
     */
    fun getChildren(): SnapshotArray<Actor?>? {
        return children
    }

    fun hasChildren(): Boolean {
        return children.size > 0
    }

    /**
     * Converts coordinates for this group to those of a descendant actor. The descendant does not need to be a direct child.
     *
     * @throws IllegalArgumentException if the specified actor is not a descendant of this group.
     */
    fun localToDescendantCoordinates(descendant: Actor?, localCoords: Vector2?): Vector2? {
        val parent: Group = descendant!!.parent
            ?: throw java.lang.IllegalArgumentException("Child is not a descendant: $descendant")
        // First convert to the actor's parent coordinates.
        if (parent !== this) localToDescendantCoordinates(parent, localCoords)
        // Then from each parent down to the descendant.
        descendant!!.parentToLocalCoordinates(localCoords)
        return localCoords
    }

    /**
     * If true, [.drawDebug] will be called for this group and, optionally, all children recursively.
     */
    fun setDebug(enabled: Boolean, recursively: Boolean) {
        setDebug(enabled)
        if (recursively) {
            for (child in children) {
                if (child is Group) {
                    (child as Group).setDebug(enabled, recursively)
                } else {
                    child!!.setDebug(enabled)
                }
            }
        }
    }

    /**
     * Calls [.setDebug] with `true, true`.
     */
    fun debugAll(): Group? {
        setDebug(true, true)
        return this
    }

    /**
     * Returns a description of the actor hierarchy, recursively.
     */
    override fun toString(): String {
        val buffer: java.lang.StringBuilder = java.lang.StringBuilder(128)
        toString(buffer, 1)
        buffer.setLength(buffer.length - 1)
        return buffer.toString()
    }

    fun toString(buffer: java.lang.StringBuilder?, indent: Int) {
        buffer.append(super.toString())
        buffer.append('\n')
        val actors: Array<Actor?> = children.begin()
        var i = 0
        val n: Int = children.size
        while (i < n) {
            for (ii in 0 until indent) buffer.append("|  ")
            val actor: Actor? = actors[i]
            if (actor is Group) (actor as Group?)!!.toString(buffer, indent + 1) else {
                buffer.append(actor)
                buffer.append('\n')
            }
            i++
        }
        children.end()
    }

    companion object {
        private val tmp: Vector2? = Vector2()
    }
}
