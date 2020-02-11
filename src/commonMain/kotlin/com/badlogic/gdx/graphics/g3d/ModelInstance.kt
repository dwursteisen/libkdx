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
package com.badlogic.gdx.graphics.g3d

import com.badlogic.gdx.graphics.g3d.ModelCache.MeshPool
import com.badlogic.gdx.graphics.g3d.ModelCache.SimpleMeshPool

/**
 * An instance of a [Model], allows to specify global transform and modify the materials, as it has a copy of the model's
 * materials. Multiple instances can be created from the same Model, all sharing the meshes and textures of the Model. The Model
 * owns the meshes and textures, to dispose of these, the Model has to be disposed. Therefor, the Model must outlive all its
 * ModelInstances
 *
 *
 * The ModelInstance creates a full copy of all materials, nodes and animations.
 *
 * @author badlogic, xoppa
 */
class ModelInstance : RenderableProvider {

    /**
     * the materials of the model, used by nodes that have a graphical representation FIXME not sure if superfluous, allows
     * modification of materials without having to traverse the nodes
     */
    val materials: Array<Material> = Array()

    /**
     * root nodes of the model
     */
    val nodes: Array<Node> = Array()

    /**
     * animations of the model, modifying node transformations
     */
    val animations: Array<Animation> = Array()

    /**
     * the [Model] this instances derives from
     */
    val model: Model

    /**
     * the world transform
     */
    var transform: Matrix4? = null

    /**
     * user definable value, which is passed to the [Shader].
     */
    var userData: Any? = null

    /**
     * Constructs a new ModelInstance with all nodes and materials of the given model.
     *
     * @param model The [Model] to create an instance of.
     */
    constructor(model: Model?) : this(model, *(null as Array<String?>?)!!) {}

    /**
     * @param model          The source [Model]
     * @param nodeId         The ID of the root [Node] of the [Model] for the instance to contain
     * @param mergeTransform True to apply the source node transform to the instance transform, resetting the node transform.
     */
    constructor(model: Model, nodeId: String?, mergeTransform: Boolean) : this(model, null, nodeId, false, false, mergeTransform) {}

    /**
     * @param model          The source [Model]
     * @param transform      The [Matrix4] instance for this ModelInstance to reference or null to create a new matrix.
     * @param nodeId         The ID of the root [Node] of the [Model] for the instance to contain
     * @param mergeTransform True to apply the source node transform to the instance transform, resetting the node transform.
     */
    constructor(model: Model, transform: Matrix4?, nodeId: String?, mergeTransform: Boolean) : this(model, transform, nodeId, false, false, mergeTransform) {}

    /**
     * Recursively searches the mode for the specified node.
     *
     * @param model           The source [Model]
     * @param nodeId          The ID of the [Node] within the [Model] for the instance to contain
     * @param parentTransform True to apply the parent's node transform to the instance (only applicable if recursive is true).
     * @param mergeTransform  True to apply the source node transform to the instance transform, resetting the node transform.
     */
    constructor(model: Model, nodeId: String?, parentTransform: Boolean, mergeTransform: Boolean) : this(model, null, nodeId, true, parentTransform, mergeTransform) {}

    /**
     * Recursively searches the mode for the specified node.
     *
     * @param model           The source [Model]
     * @param transform       The [Matrix4] instance for this ModelInstance to reference or null to create a new matrix.
     * @param nodeId          The ID of the [Node] within the [Model] for the instance to contain
     * @param parentTransform True to apply the parent's node transform to the instance (only applicable if recursive is true).
     * @param mergeTransform  True to apply the source node transform to the instance transform, resetting the node transform.
     */
    constructor(model: Model, transform: Matrix4?, nodeId: String?, parentTransform: Boolean,
                mergeTransform: Boolean) : this(model, transform, nodeId, true, parentTransform, mergeTransform) {
    }

    /**
     * @param model           The source [Model]
     * @param nodeId          The ID of the [Node] within the [Model] for the instance to contain
     * @param recursive       True to recursively search the Model's node tree, false to only search for a root node
     * @param parentTransform True to apply the parent's node transform to the instance (only applicable if recursive is true).
     * @param mergeTransform  True to apply the source node transform to the instance transform, resetting the node transform.
     */
    constructor(model: Model, nodeId: String?, recursive: Boolean, parentTransform: Boolean,
                mergeTransform: Boolean) : this(model, null, nodeId, recursive, parentTransform, mergeTransform) {
    }

    /**
     * @param model           The source [Model]
     * @param transform       The [Matrix4] instance for this ModelInstance to reference or null to create a new matrix.
     * @param nodeId          The ID of the [Node] within the [Model] for the instance to contain
     * @param recursive       True to recursively search the Model's node tree, false to only search for a root node
     * @param parentTransform True to apply the parent's node transform to the instance (only applicable if recursive is true).
     * @param mergeTransform  True to apply the source node transform to the instance transform, resetting the node transform.
     */
    constructor(model: Model, transform: Matrix4?, nodeId: String?, recursive: Boolean,
                parentTransform: Boolean, mergeTransform: Boolean) : this(model, transform, nodeId, recursive, parentTransform, mergeTransform, defaultShareKeyframes) {
    }

    /**
     * @param model           The source [Model]
     * @param transform       The [Matrix4] instance for this ModelInstance to reference or null to create a new matrix.
     * @param nodeId          The ID of the [Node] within the [Model] for the instance to contain
     * @param recursive       True to recursively search the Model's node tree, false to only search for a root node
     * @param parentTransform True to apply the parent's node transform to the instance (only applicable if recursive is true).
     * @param mergeTransform  True to apply the source node transform to the instance transform, resetting the node transform.
     */
    constructor(model: Model, transform: Matrix4?, nodeId: String?, recursive: Boolean,
                parentTransform: Boolean, mergeTransform: Boolean, shareKeyframes: Boolean) {
        this.model = model
        this.transform = if (transform == null) Matrix4() else transform
        var copy: Node
        val node: Node = model.getNode(nodeId, recursive)
        nodes.add(node.copy().also({ copy = it }))
        if (mergeTransform) {
            this.transform.mul(if (parentTransform) node.globalTransform else node.localTransform)
            copy.translation.set(0, 0, 0)
            copy.rotation.idt()
            copy.scale.set(1, 1, 1)
        } else if (parentTransform && copy.hasParent()) this.transform.mul(node.getParent().globalTransform)
        invalidate()
        copyAnimations(model.animations, shareKeyframes)
        calculateTransforms()
    }

    /**
     * Constructs a new ModelInstance with only the specified nodes and materials of the given model.
     */
    constructor(model: Model?, vararg rootNodeIds: String?) : this(model, null, *rootNodeIds) {}

    /**
     * Constructs a new ModelInstance with only the specified nodes and materials of the given model.
     */
    constructor(model: Model, transform: Matrix4?, vararg rootNodeIds: String?) {
        this.model = model
        this.transform = if (transform == null) Matrix4() else transform
        if (rootNodeIds == null) copyNodes(model.nodes) else copyNodes(model.nodes, rootNodeIds)
        copyAnimations(model.animations, defaultShareKeyframes)
        calculateTransforms()
    }

    /**
     * Constructs a new ModelInstance with only the specified nodes and materials of the given model.
     */
    constructor(model: Model, rootNodeIds: Array<String>) : this(model, null, rootNodeIds) {}

    /**
     * Constructs a new ModelInstance with only the specified nodes and materials of the given model.
     */
    constructor(model: Model, transform: Matrix4?, rootNodeIds: Array<String>) : this(model, transform, rootNodeIds, defaultShareKeyframes) {}

    /**
     * Constructs a new ModelInstance with only the specified nodes and materials of the given model.
     */
    constructor(model: Model, transform: Matrix4?, rootNodeIds: Array<String>, shareKeyframes: Boolean) {
        this.model = model
        this.transform = if (transform == null) Matrix4() else transform
        copyNodes(model.nodes, rootNodeIds)
        copyAnimations(model.animations, shareKeyframes)
        calculateTransforms()
    }

    /**
     * Constructs a new ModelInstance at the specified position.
     */
    constructor(model: Model?, position: Vector3?) : this(model) {
        transform.setToTranslation(position)
    }

    /**
     * Constructs a new ModelInstance at the specified position.
     */
    constructor(model: Model?, x: Float, y: Float, z: Float) : this(model) {
        transform.setToTranslation(x, y, z)
    }

    /**
     * Constructs a new ModelInstance with the specified transform.
     */
    constructor(model: Model?, transform: Matrix4?) : this(model, transform, *null as Array<String?>?) {}

    /**
     * Constructs a new ModelInstance which is an copy of the specified ModelInstance.
     */
    constructor(copyFrom: ModelInstance) : this(copyFrom, copyFrom.transform.cpy()) {}

    /**
     * Constructs a new ModelInstance which is an copy of the specified ModelInstance.
     */
    constructor(copyFrom: ModelInstance?, transform: Matrix4?) : this(copyFrom, transform, defaultShareKeyframes) {}

    /**
     * Constructs a new ModelInstance which is an copy of the specified ModelInstance.
     */
    constructor(copyFrom: ModelInstance, transform: Matrix4?, shareKeyframes: Boolean) {
        model = copyFrom.model
        this.transform = if (transform == null) Matrix4() else transform
        copyNodes(copyFrom.nodes)
        copyAnimations(copyFrom.animations, shareKeyframes)
        calculateTransforms()
    }

    /**
     * @return A newly created ModelInstance which is a copy of this ModelInstance
     */
    fun copy(): ModelInstance {
        return ModelInstance(this)
    }

    private fun copyNodes(nodes: Array<Node>) {
        var i = 0
        val n = nodes.size
        while (i < n) {
            val node: Node = nodes[i]
            this.nodes.add(node.copy())
            ++i
        }
        invalidate()
    }

    private fun copyNodes(nodes: Array<Node>, vararg nodeIds: String) {
        var i = 0
        val n = nodes.size
        while (i < n) {
            val node: Node = nodes[i]
            for (nodeId in nodeIds) {
                if (nodeId == node.id) {
                    this.nodes.add(node.copy())
                    break
                }
            }
            ++i
        }
        invalidate()
    }

    private fun copyNodes(nodes: Array<Node>, nodeIds: Array<String>) {
        var i = 0
        val n = nodes.size
        while (i < n) {
            val node: Node = nodes[i]
            for (nodeId in nodeIds) {
                if (nodeId == node.id) {
                    this.nodes.add(node.copy())
                    break
                }
            }
            ++i
        }
        invalidate()
    }

    /**
     * Makes sure that each [NodePart] of the [Node] and its sub-nodes, doesn't reference a node outside this node
     * tree and that all materials are listed in the [.materials] array.
     */
    private fun invalidate(node: Node) {
        run {
            var i = 0
            val n: Int = node.parts.size
            while (i < n) {
                val part: NodePart = node.parts.get(i)
                val bindPose: ArrayMap<Node, Matrix4> = part.invBoneBindTransforms
                if (bindPose != null) {
                    for (j in 0 until bindPose.size) {
                        bindPose.keys.get(j) = getNode(bindPose.keys.get(j).id)
                    }
                }
                if (!materials.contains(part.material, true)) {
                    val midx = materials.indexOf(part.material, false)
                    if (midx < 0) materials.add(part.material.copy().also({ part.material = it })) else part.material = materials[midx]
                }
                ++i
            }
        }
        var i = 0
        val n: Int = node.getChildCount()
        while (i < n) {
            invalidate(node.getChild(i))
            ++i
        }
    }

    /**
     * Makes sure that each [NodePart] of each [Node] doesn't reference a node outside this node tree and that all
     * materials are listed in the [.materials] array.
     */
    private fun invalidate() {
        var i = 0
        val n = nodes.size
        while (i < n) {
            invalidate(nodes[i])
            ++i
        }
    }

    /**
     * Copy source animations to this ModelInstance
     *
     * @param source Iterable collection of source animations [Animation]
     */
    fun copyAnimations(source: Iterable<Animation>) {
        for (anim in source) {
            copyAnimation(anim, defaultShareKeyframes)
        }
    }

    /**
     * Copy source animations to this ModelInstance
     *
     * @param source         Iterable collection of source animations [Animation]
     * @param shareKeyframes Shallow copy of [NodeKeyframe]'s if it's true, otherwise make a deep copy.
     */
    fun copyAnimations(source: Iterable<Animation>, shareKeyframes: Boolean) {
        for (anim in source) {
            copyAnimation(anim, shareKeyframes)
        }
    }

    /**
     * Copy the source animation to this ModelInstance
     *
     * @param sourceAnim The source animation [Animation]
     */
    fun copyAnimation(sourceAnim: Animation) {
        copyAnimation(sourceAnim, defaultShareKeyframes)
    }

    /**
     * Copy the source animation to this ModelInstance
     *
     * @param sourceAnim     The source animation [Animation]
     * @param shareKeyframes Shallow copy of [NodeKeyframe]'s if it's true, otherwise make a deep copy.
     */
    fun copyAnimation(sourceAnim: Animation, shareKeyframes: Boolean) {
        val animation = Animation()
        animation.id = sourceAnim.id
        animation.duration = sourceAnim.duration
        for (nanim in sourceAnim.nodeAnimations) {
            val node: Node = getNode(nanim.node.id) ?: continue
            val nodeAnim = NodeAnimation()
            nodeAnim.node = node
            if (shareKeyframes) {
                nodeAnim.translation = nanim.translation
                nodeAnim.rotation = nanim.rotation
                nodeAnim.scaling = nanim.scaling
            } else {
                if (nanim.translation != null) {
                    nodeAnim.translation = Array<NodeKeyframe<Vector3>>()
                    for (kf in nanim.translation) nodeAnim.translation.add(NodeKeyframe<Vector3>(kf.keytime, kf.value))
                }
                if (nanim.rotation != null) {
                    nodeAnim.rotation = Array<NodeKeyframe<Quaternion>>()
                    for (kf in nanim.rotation) nodeAnim.rotation.add(NodeKeyframe<Quaternion>(kf.keytime, kf.value))
                }
                if (nanim.scaling != null) {
                    nodeAnim.scaling = Array<NodeKeyframe<Vector3>>()
                    for (kf in nanim.scaling) nodeAnim.scaling.add(NodeKeyframe<Vector3>(kf.keytime, kf.value))
                }
            }
            if (nodeAnim.translation != null || nodeAnim.rotation != null || nodeAnim.scaling != null) animation.nodeAnimations.add(nodeAnim)
        }
        if (animation.nodeAnimations.size > 0) animations.add(animation)
    }

    /**
     * Traverses the Node hierarchy and collects [Renderable] instances for every node with a graphical representation.
     * Renderables are obtained from the provided pool. The resulting array can be rendered via a [ModelBatch].
     *
     * @param renderables the output array
     * @param pool        the pool to obtain Renderables from
     */
    fun getRenderables(renderables: Array<Renderable?>, pool: Pool<Renderable?>) {
        for (node in nodes) {
            getRenderables(node, renderables, pool)
        }
    }

    /**
     * @return The renderable of the first node's first part.
     */
    fun getRenderable(out: Renderable): Renderable {
        return getRenderable(out, nodes[0])
    }

    /**
     * @return The renderable of the node's first part.
     */
    fun getRenderable(out: Renderable, node: Node): Renderable {
        return getRenderable(out, node, node.parts.get(0))
    }

    fun getRenderable(out: Renderable, node: Node, nodePart: NodePart): Renderable {
        nodePart.setRenderable(out)
        if (nodePart.bones == null && transform != null) out.worldTransform.set(transform).mul(node.globalTransform) else if (transform != null) out.worldTransform.set(transform) else out.worldTransform.idt()
        out.userData = userData
        return out
    }

    protected fun getRenderables(node: Node, renderables: Array<Renderable?>, pool: Pool<Renderable?>) {
        if (node.parts.size > 0) {
            for (nodePart in node.parts) {
                if (nodePart.enabled) renderables.add(getRenderable(pool.obtain(), node, nodePart))
            }
        }
        for (child in node.getChildren()) {
            getRenderables(child, renderables, pool)
        }
    }

    /**
     * Calculates the local and world transform of all [Node] instances in this model, recursively. First each
     * [Node.localTransform] transform is calculated based on the translation, rotation and scale of each Node. Then each
     * [Node.calculateWorldTransform] is calculated, based on the parent's world transform and the local transform of each
     * Node. Finally, the animation bone matrices are updated accordingly.
     *
     *
     * This method can be used to recalculate all transforms if any of the Node's local properties (translation, rotation, scale)
     * was modified.
     */
    fun calculateTransforms() {
        val n = nodes.size
        for (i in 0 until n) {
            nodes[i].calculateTransforms(true)
        }
        for (i in 0 until n) {
            nodes[i].calculateBoneTransforms(true)
        }
    }

    /**
     * Calculate the bounding box of this model instance. This is a potential slow operation, it is advised to cache the result.
     *
     * @param out the [BoundingBox] that will be set with the bounds.
     * @return the out parameter for chaining
     */
    fun calculateBoundingBox(out: BoundingBox): BoundingBox {
        out.inf()
        return extendBoundingBox(out)
    }

    /**
     * Extends the bounding box with the bounds of this model instance. This is a potential slow operation, it is advised to cache
     * the result.
     *
     * @param out the [BoundingBox] that will be extended with the bounds.
     * @return the out parameter for chaining
     */
    fun extendBoundingBox(out: BoundingBox): BoundingBox {
        val n = nodes.size
        for (i in 0 until n) nodes[i].extendBoundingBox(out)
        return out
    }

    /**
     * @param id The ID of the animation to fetch (case sensitive).
     * @return The [Animation] with the specified id, or null if not available.
     */
    fun getAnimation(id: String?): Animation? {
        return getAnimation(id, false)
    }

    /**
     * @param id         The ID of the animation to fetch.
     * @param ignoreCase whether to use case sensitivity when comparing the animation id.
     * @return The [Animation] with the specified id, or null if not available.
     */
    fun getAnimation(id: String?, ignoreCase: Boolean): Animation? {
        val n = animations.size
        var animation: Animation?
        if (ignoreCase) {
            for (i in 0 until n) if (animations[i].also({ animation = it }).id.equalsIgnoreCase(id)) return animation
        } else {
            for (i in 0 until n) if (animations[i].also({ animation = it }).id.equals(id)) return animation
        }
        return null
    }

    /**
     * @param id The ID of the material to fetch.
     * @return The [Material] with the specified id, or null if not available.
     */
    fun getMaterial(id: String?): Material? {
        return getMaterial(id, true)
    }

    /**
     * @param id         The ID of the material to fetch.
     * @param ignoreCase whether to use case sensitivity when comparing the material id.
     * @return The [Material] with the specified id, or null if not available.
     */
    fun getMaterial(id: String?, ignoreCase: Boolean): Material? {
        val n = materials.size
        var material: Material?
        if (ignoreCase) {
            for (i in 0 until n) if (materials[i].also({ material = it }).id.equalsIgnoreCase(id)) return material
        } else {
            for (i in 0 until n) if (materials[i].also({ material = it }).id.equals(id)) return material
        }
        return null
    }

    /**
     * @param id The ID of the node to fetch.
     * @return The [Node] with the specified id, or null if not found.
     */
    fun getNode(id: String?): Node {
        return getNode(id, true)
    }

    /**
     * @param id        The ID of the node to fetch.
     * @param recursive false to fetch a root node only, true to search the entire node tree for the specified node.
     * @return The [Node] with the specified id, or null if not found.
     */
    fun getNode(id: String?, recursive: Boolean): Node {
        return getNode(id, recursive, false)
    }

    /**
     * @param id         The ID of the node to fetch.
     * @param recursive  false to fetch a root node only, true to search the entire node tree for the specified node.
     * @param ignoreCase whether to use case sensitivity when comparing the node id.
     * @return The [Node] with the specified id, or null if not found.
     */
    fun getNode(id: String?, recursive: Boolean, ignoreCase: Boolean): Node {
        return Node.getNode(nodes, id, recursive, ignoreCase)
    }

    companion object {
        /**
         * Whether, by default, [NodeKeyframe]'s are shared amongst [Model] and ModelInstance. Can be overridden per
         * ModelInstance using the constructor argument.
         */
        var defaultShareKeyframes = true
    }
}
