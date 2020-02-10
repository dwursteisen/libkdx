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
package com.badlogic.gdx.graphics.g3d.model

import kotlin.jvm.JvmField
import kotlin.jvm.JvmStatic

/** A node is part of a hierarchy of Nodes in a [Model]. A Node encodes a transform relative to its parents. A Node can have
 * child nodes. Optionally a node can specify a [MeshPart] and a [Material] to be applied to the mesh part.
 * @author badlogic
 */
class Node {

    /** the id, may be null, FIXME is this unique?  */
    @JvmField
    var id: String? = null
    /** Whether this node should inherit the transformation of its parent node, defaults to true. When this flag is false the value
     * of [.globalTransform] will be the same as the value of [.localTransform] causing the transform to be independent
     * of its parent transform.  */
    var inheritTransform = true
    /** Whether this node is currently being animated, if so the translation, rotation and scale values are not used.  */
    @JvmField
    var isAnimated = false
    /** the translation, relative to the parent, not modified by animations  */
    @JvmField
    val translation: com.badlogic.gdx.math.Vector3? = com.badlogic.gdx.math.Vector3()
    /** the rotation, relative to the parent, not modified by animations  */
    @JvmField
    val rotation: com.badlogic.gdx.math.Quaternion? = com.badlogic.gdx.math.Quaternion(0, 0, 0, 1)
    /** the scale, relative to the parent, not modified by animations  */
    @JvmField
    val scale: com.badlogic.gdx.math.Vector3? = com.badlogic.gdx.math.Vector3(1, 1, 1)
    /** the local transform, based on translation/rotation/scale ([.calculateLocalTransform]) or any applied animation  */
    @JvmField
    val localTransform: com.badlogic.gdx.math.Matrix4? = com.badlogic.gdx.math.Matrix4()
    /** the global transform, product of local transform and transform of the parent node, calculated via
     * [.calculateWorldTransform]  */
    @JvmField
    val globalTransform: com.badlogic.gdx.math.Matrix4? = com.badlogic.gdx.math.Matrix4()
    @JvmField
    var parts: com.badlogic.gdx.utils.Array<com.badlogic.gdx.graphics.g3d.model.NodePart?>? = com.badlogic.gdx.utils.Array<com.badlogic.gdx.graphics.g3d.model.NodePart?>(2)
    /** @return The parent node that holds this node as child node, may be null.
     */
    var parent: Node? = null
        protected set
    private val children: com.badlogic.gdx.utils.Array<Node?>? = com.badlogic.gdx.utils.Array<Node?>(2)
    /** Calculates the local transform based on the translation, scale and rotation
     * @return the local transform
     */
    fun calculateLocalTransform(): com.badlogic.gdx.math.Matrix4? {
        if (!isAnimated) localTransform.set(translation, rotation, scale)
        return localTransform
    }

    /** Calculates the world transform; the product of local transform and the parent's world transform.
     * @return the world transform
     */
    fun calculateWorldTransform(): com.badlogic.gdx.math.Matrix4? {
        if (inheritTransform && parent != null) globalTransform.set(parent!!.globalTransform).mul(localTransform) else globalTransform.set(localTransform)
        return globalTransform
    }

    /** Calculates the local and world transform of this node and optionally all its children.
     *
     * @param recursive whether to calculate the local/world transforms for children.
     */
    fun calculateTransforms(recursive: Boolean) {
        calculateLocalTransform()
        calculateWorldTransform()
        if (recursive) {
            for (child in children) {
                child!!.calculateTransforms(true)
            }
        }
    }

    fun calculateBoneTransforms(recursive: Boolean) {
        for (part in parts) {
            if (part!!.invBoneBindTransforms == null || part!!.bones == null || part!!.invBoneBindTransforms.size != part!!.bones.size) continue
            val n: Int = part!!.invBoneBindTransforms.size
            for (i in 0 until n) part!!.bones.get(i).set(part!!.invBoneBindTransforms.keys.get(i).globalTransform).mul(part!!.invBoneBindTransforms.values.get(i))
        }
        if (recursive) {
            for (child in children) {
                child!!.calculateBoneTransforms(true)
            }
        }
    }

    /** Calculate the bounding box of this Node. This is a potential slow operation, it is advised to cache the result.  */
    fun calculateBoundingBox(out: com.badlogic.gdx.math.collision.BoundingBox?): com.badlogic.gdx.math.collision.BoundingBox? {
        out.inf()
        return extendBoundingBox(out)
    }

    /** Calculate the bounding box of this Node. This is a potential slow operation, it is advised to cache the result.  */
    fun calculateBoundingBox(out: com.badlogic.gdx.math.collision.BoundingBox?, transform: Boolean): com.badlogic.gdx.math.collision.BoundingBox? {
        out.inf()
        return extendBoundingBox(out, transform)
    }
    /** Extends the bounding box with the bounds of this Node. This is a potential slow operation, it is advised to cache the
     * result.  */
    /** Extends the bounding box with the bounds of this Node. This is a potential slow operation, it is advised to cache the
     * result.  */
    @JvmOverloads
    fun extendBoundingBox(out: com.badlogic.gdx.math.collision.BoundingBox?, transform: Boolean = true): com.badlogic.gdx.math.collision.BoundingBox? {
        val partCount: Int = parts.size
        for (i in 0 until partCount) {
            val part: com.badlogic.gdx.graphics.g3d.model.NodePart = parts.get(i)
            if (part!!.enabled) {
                val meshPart: com.badlogic.gdx.graphics.g3d.model.MeshPart = part!!.meshPart
                if (transform) meshPart!!.mesh.extendBoundingBox(out, meshPart!!.offset, meshPart!!.size, globalTransform) else meshPart!!.mesh.extendBoundingBox(out, meshPart!!.offset, meshPart!!.size)
            }
        }
        val childCount: Int = children.size
        for (i in 0 until childCount) children.get(i).extendBoundingBox(out)
        return out
    }

    /** Adds this node as child to specified parent Node, synonym for: `parent.addChild(this)`
     * @param parent The Node to attach this Node to.
     */
    fun <T : Node?> attachTo(parent: T?) {
        parent!!.addChild<Node?>(this)
    }

    /** Removes this node from its current parent, if any. Short for: `this.getParent().removeChild(this)`  */
    fun detach() {
        if (parent != null) {
            parent!!.removeChild<Node?>(this)
            parent = null
        }
    }

    /** @return whether this Node has one or more children (true) or not (false)
     */
    fun hasChildren(): Boolean {
        return children != null && children.size > 0
    }

    /** @return The number of child nodes that this Node current contains.
     * @see .getChild
     */
    val childCount: Int
        get() = children.size

    /** @param index The zero-based index of the child node to get, must be: 0 <= index < [.getChildCount].
     * @return The child node at the specified index
     */
    fun getChild(index: Int): Node? {
        return children.get(index)
    }

    /** @param recursive false to fetch a root child only, true to search the entire node tree for the specified node.
     * @return The node with the specified id, or null if not found.
     */
    fun getChild(id: String?, recursive: Boolean, ignoreCase: Boolean): Node? {
        return getNode(children, id, recursive, ignoreCase)
    }

    /** Adds the specified node as the currently last child of this node. If the node is already a child of another node, then it is
     * removed from its current parent.
     * @param child The Node to add as child of this Node
     * @return the zero-based index of the child
     */
    fun <T : Node?> addChild(child: T?): Int {
        return insertChild(-1, child)
    }

    /** Adds the specified nodes as the currently last child of this node. If the node is already a child of another node, then it
     * is removed from its current parent.
     * @param nodes The Node to add as child of this Node
     * @return the zero-based index of the first added child
     */
    fun <T : Node?> addChildren(nodes: Iterable<T?>?): Int {
        return insertChildren(-1, nodes)
    }

    /** Insert the specified node as child of this node at the specified index. If the node is already a child of another node, then
     * it is removed from its current parent. If the specified index is less than zero or equal or greater than
     * [.getChildCount] then the Node is added as the currently last child.
     * @param index The zero-based index at which to add the child
     * @param child The Node to add as child of this Node
     * @return the zero-based index of the child
     */
    fun <T : Node?> insertChild(index: Int, child: T?): Int {
        var index = index
        run {
            var p: Node? = this
            while (p != null) {
                if (p === child) throw com.badlogic.gdx.utils.GdxRuntimeException("Cannot add a parent as a child")
                p = p.parent
            }
        }
        val p = child!!.parent
        if (p != null && !p.removeChild(child)) throw com.badlogic.gdx.utils.GdxRuntimeException("Could not remove child from its current parent")
        if (index < 0 || index >= children.size) {
            index = children.size
            children.add(child)
        } else children.insert(index, child)
        child.parent = this
        return index
    }

    /** Insert the specified nodes as children of this node at the specified index. If the node is already a child of another node,
     * then it is removed from its current parent. If the specified index is less than zero or equal or greater than
     * [.getChildCount] then the Node is added as the currently last child.
     * @param index The zero-based index at which to add the child
     * @param nodes The nodes to add as child of this Node
     * @return the zero-based index of the first inserted child
     */
    fun <T : Node?> insertChildren(index: Int, nodes: Iterable<T?>?): Int {
        var index = index
        if (index < 0 || index > children.size) index = children.size
        var i = index
        for (child in nodes!!) insertChild(i++, child)
        return index
    }

    /** Removes the specified node as child of this node. On success, the child node will be not attached to any parent node (its
     * [.getParent] method will return null). If the specified node currently isn't a child of this node then the removal
     * is considered to be unsuccessful and the method will return false.
     * @param child The child node to remove.
     * @return Whether the removal was successful.
     */
    fun <T : Node?> removeChild(child: T?): Boolean {
        if (!children.removeValue(child, true)) return false
        child!!.parent = null
        return true
    }

    /** @return An [Iterable] to all child nodes that this node contains.
     */
    fun getChildren(): Iterable<Node?>? {
        return children
    }

    /** @return Whether (true) is this Node is a child node of another node or not (false).
     */
    fun hasParent(): Boolean {
        return parent != null
    }

    /** Creates a nested copy of this Node, any child nodes are copied using this method as well. The [.parts] are copied
     * using the [NodePart.copy] method. Note that that method copies the material and nodes (bones) by reference. If you
     * intend to use the copy in a different node tree (e.g. a different Model or ModelInstance) then you will need to update these
     * references afterwards.
     *
     * Override this method in your custom Node class to instantiate that class, in that case you should override the
     * [.set] method as well.  */
    fun copy(): Node? {
        return Node().set(this)
    }

    /** Creates a nested copy of this Node, any child nodes are copied using the [.copy] method. This will detach this node
     * from its parent, but does not attach it to the parent of node being copied. The [.parts] are copied using the
     * [NodePart.copy] method. Note that that method copies the material and nodes (bones) by reference. If you intend to
     * use this node in a different node tree (e.g. a different Model or ModelInstance) then you will need to update these
     * references afterwards.
     *
     * Override this method in your custom Node class to copy any additional fields you've added.
     * @return This Node for chaining
     */
    protected fun set(other: Node?): Node? {
        detach()
        id = other!!.id
        isAnimated = other.isAnimated
        inheritTransform = other.inheritTransform
        translation.set(other.translation)
        rotation.set(other.rotation)
        scale.set(other.scale)
        localTransform.set(other.localTransform)
        globalTransform.set(other.globalTransform)
        parts.clear()
        for (nodePart in other.parts) {
            parts.add(nodePart!!.copy())
        }
        children.clear()
        for (child in other.getChildren()!!) {
            addChild<Node?>(child!!.copy())
        }
        return this
    }

    companion object {
        /** Helper method to recursive fetch a node from an array
         * @param recursive false to fetch a root node only, true to search the entire node tree for the specified node.
         * @return The node with the specified id, or null if not found.
         */
        @JvmStatic
        fun getNode(nodes: com.badlogic.gdx.utils.Array<Node?>?, id: String?, recursive: Boolean, ignoreCase: Boolean): Node? {
            val n: Int = nodes.size
            var node: Node?
            if (ignoreCase) {
                for (i in 0 until n) if (nodes.get(i).also({ node = it }).id.equals(id, ignoreCase = true)) return node
            } else {
                for (i in 0 until n) if (nodes.get(i).also({ node = it }).id == id) return node
            }
            if (recursive) {
                for (i in 0 until n) if (getNode(nodes.get(i).children, id, true, ignoreCase).also { node = it } != null) return node
            }
            return null
        }
    }
}
