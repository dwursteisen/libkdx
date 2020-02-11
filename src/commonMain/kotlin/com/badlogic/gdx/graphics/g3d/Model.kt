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

/**
 * A model represents a 3D assets. It stores a hierarchy of nodes. A node has a transform and optionally a graphical part in form
 * of a [MeshPart] and [Material]. Mesh parts reference subsets of vertices in one of the meshes of the model.
 * Animations can be applied to nodes, to modify their transform (translation, rotation, scale) over time.
 *
 *
 * A model can be rendered by creating a [ModelInstance] from it. That instance has an additional transform to position the
 * model in the world, and allows modification of materials and nodes without destroying the original model. The original model is
 * the owner of any meshes and textures, all instances created from the model share these resources. Disposing the model will
 * automatically make all instances invalid!
 *
 *
 * A model is created from [ModelData], which in turn is loaded by a [ModelLoader].
 *
 * @author badlogic, xoppa
 */
class Model : Disposable {

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
     * the meshes of the model
     */
    val meshes: Array<Mesh> = Array()

    /**
     * parts of meshes, used by nodes that have a graphical representation FIXME not sure if superfluous, stored in Nodes as well,
     * could be useful to create bullet meshes
     */
    val meshParts: Array<MeshPart> = Array()

    /**
     * Array of disposable resources like textures or meshes the Model is responsible for disposing
     */
    protected val disposables: Array<Disposable> = Array()

    /**
     * Constructs an empty model. Manual created models do not manage their resources by default. Use
     * [.manageDisposable] to add resources to be managed by this model.
     */
    constructor() {}

    /**
     * Constructs a new Model based on the [ModelData]. Texture files will be loaded from the internal file storage via an
     * [FileTextureProvider].
     *
     * @param modelData the [ModelData] got from e.g. [ModelLoader]
     */
    constructor(modelData: ModelData) : this(modelData, FileTextureProvider()) {}

    /**
     * Constructs a new Model based on the [ModelData].
     *
     * @param modelData       the [ModelData] got from e.g. [ModelLoader]
     * @param textureProvider the [TextureProvider] to use for loading the textures
     */
    constructor(modelData: ModelData, textureProvider: TextureProvider) {
        load(modelData, textureProvider)
    }

    protected fun load(modelData: ModelData, textureProvider: TextureProvider) {
        loadMeshes(modelData.meshes)
        loadMaterials(modelData.materials, textureProvider)
        loadNodes(modelData.nodes)
        loadAnimations(modelData.animations)
        calculateTransforms()
    }

    protected fun loadAnimations(modelAnimations: Iterable<ModelAnimation>) {
        for (anim in modelAnimations) {
            val animation = Animation()
            animation.id = anim.id
            for (nanim in anim.nodeAnimations) {
                val node: Node = getNode(nanim.nodeId) ?: continue
                val nodeAnim = NodeAnimation()
                nodeAnim.node = node
                if (nanim.translation != null) {
                    nodeAnim.translation = Array<NodeKeyframe<Vector3>>()
                    nodeAnim.translation.ensureCapacity(nanim.translation.size)
                    for (kf in nanim.translation) {
                        if (kf.keytime > animation.duration) animation.duration = kf.keytime
                        nodeAnim.translation.add(NodeKeyframe<Vector3>(kf.keytime, Vector3(if (kf.value == null) node.translation else kf.value)))
                    }
                }
                if (nanim.rotation != null) {
                    nodeAnim.rotation = Array<NodeKeyframe<Quaternion>>()
                    nodeAnim.rotation.ensureCapacity(nanim.rotation.size)
                    for (kf in nanim.rotation) {
                        if (kf.keytime > animation.duration) animation.duration = kf.keytime
                        nodeAnim.rotation.add(NodeKeyframe<Quaternion>(kf.keytime, Quaternion(if (kf.value == null) node.rotation else kf.value)))
                    }
                }
                if (nanim.scaling != null) {
                    nodeAnim.scaling = Array<NodeKeyframe<Vector3>>()
                    nodeAnim.scaling.ensureCapacity(nanim.scaling.size)
                    for (kf in nanim.scaling) {
                        if (kf.keytime > animation.duration) animation.duration = kf.keytime
                        nodeAnim.scaling.add(NodeKeyframe<Vector3>(kf.keytime,
                            Vector3(if (kf.value == null) node.scale else kf.value)))
                    }
                }
                if (nodeAnim.translation != null && nodeAnim.translation.size > 0
                    || nodeAnim.rotation != null && nodeAnim.rotation.size > 0
                    || nodeAnim.scaling != null && nodeAnim.scaling.size > 0) animation.nodeAnimations.add(nodeAnim)
            }
            if (animation.nodeAnimations.size > 0) animations.add(animation)
        }
    }

    private val nodePartBones: ObjectMap<NodePart, ArrayMap<String, Matrix4>> = ObjectMap<NodePart, ArrayMap<String, Matrix4>>()
    protected fun loadNodes(modelNodes: Iterable<ModelNode>) {
        nodePartBones.clear()
        for (node in modelNodes) {
            nodes.add(loadNode(node))
        }
        for (e in nodePartBones.entries()) {
            if (e.key.invBoneBindTransforms == null) e.key.invBoneBindTransforms = ArrayMap<Node, Matrix4>(Node::class.java, Matrix4::class.java)
            e.key.invBoneBindTransforms.clear()
            for (b in e.value.entries()) e.key.invBoneBindTransforms.put(getNode(b.key), Matrix4(b.value).inv())
        }
    }

    protected fun loadNode(modelNode: ModelNode): Node {
        val node = Node()
        node.id = modelNode.id
        if (modelNode.translation != null) node.translation.set(modelNode.translation)
        if (modelNode.rotation != null) node.rotation.set(modelNode.rotation)
        if (modelNode.scale != null) node.scale.set(modelNode.scale)
        // FIXME create temporary maps for faster lookup?
        if (modelNode.parts != null) {
            for (modelNodePart in modelNode.parts) {
                var meshPart: MeshPart? = null
                var meshMaterial: Material? = null
                if (modelNodePart.meshPartId != null) {
                    for (part in meshParts) {
                        if (modelNodePart.meshPartId.equals(part.id)) {
                            meshPart = part
                            break
                        }
                    }
                }
                if (modelNodePart.materialId != null) {
                    for (material in materials) {
                        if (modelNodePart.materialId.equals(material.id)) {
                            meshMaterial = material
                            break
                        }
                    }
                }
                if (meshPart == null || meshMaterial == null) throw GdxRuntimeException("Invalid node: " + node.id)
                if (meshPart != null && meshMaterial != null) {
                    val nodePart = NodePart()
                    nodePart.meshPart = meshPart
                    nodePart.material = meshMaterial
                    node.parts.add(nodePart)
                    if (modelNodePart.bones != null) nodePartBones.put(nodePart, modelNodePart.bones)
                }
            }
        }
        if (modelNode.children != null) {
            for (child in modelNode.children) {
                node.addChild(loadNode(child))
            }
        }
        return node
    }

    protected fun loadMeshes(meshes: Iterable<ModelMesh>) {
        for (mesh in meshes) {
            convertMesh(mesh)
        }
    }

    protected fun convertMesh(modelMesh: ModelMesh) {
        var numIndices = 0
        for (part in modelMesh.parts) {
            numIndices += part.indices.length
        }
        val hasIndices = numIndices > 0
        val attributes = VertexAttributes(modelMesh.attributes)
        val numVertices: Int = modelMesh.vertices.length / (attributes.vertexSize / 4)
        val mesh = Mesh(true, numVertices, numIndices, attributes)
        meshes.add(mesh)
        disposables.add(mesh)
        BufferUtils.copy(modelMesh.vertices, mesh.getVerticesBuffer(), modelMesh.vertices.length, 0)
        var offset = 0
        mesh.getIndicesBuffer().clear()
        for (part in modelMesh.parts) {
            val meshPart = MeshPart()
            meshPart.id = part.id
            meshPart.primitiveType = part.primitiveType
            meshPart.offset = offset
            meshPart.size = if (hasIndices) part.indices.length else numVertices
            meshPart.mesh = mesh
            if (hasIndices) {
                mesh.getIndicesBuffer().put(part.indices)
            }
            offset += meshPart.size
            meshParts.add(meshPart)
        }
        mesh.getIndicesBuffer().position(0)
        for (part in meshParts) part.update()
    }

    protected fun loadMaterials(modelMaterials: Iterable<ModelMaterial>, textureProvider: TextureProvider) {
        for (mtl in modelMaterials) {
            materials.add(convertMaterial(mtl, textureProvider))
        }
    }

    protected fun convertMaterial(mtl: ModelMaterial, textureProvider: TextureProvider): Material {
        val result = Material()
        result.id = mtl.id
        if (mtl.ambient != null) result[] = ColorAttribute(ColorAttribute.Ambient, mtl.ambient)
        if (mtl.diffuse != null) result[] = ColorAttribute(ColorAttribute.Diffuse, mtl.diffuse)
        if (mtl.specular != null) result[] = ColorAttribute(ColorAttribute.Specular, mtl.specular)
        if (mtl.emissive != null) result[] = ColorAttribute(ColorAttribute.Emissive, mtl.emissive)
        if (mtl.reflection != null) result[] = ColorAttribute(ColorAttribute.Reflection, mtl.reflection)
        if (mtl.shininess > 0f) result[] = FloatAttribute(FloatAttribute.Shininess, mtl.shininess)
        if (mtl.opacity !== 1f) result[] = BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA, mtl.opacity)
        val textures: ObjectMap<String, Texture> = ObjectMap<String, Texture>()

        // FIXME uvScaling/uvTranslation totally ignored
        if (mtl.textures != null) {
            for (tex in mtl.textures) {
                var texture: Texture
                if (textures.containsKey(tex.fileName)) {
                    texture = textures.get(tex.fileName)
                } else {
                    texture = textureProvider.load(tex.fileName)
                    textures.put(tex.fileName, texture)
                    disposables.add(texture)
                }
                val descriptor = TextureDescriptor(texture)
                descriptor.minFilter = texture.getMinFilter()
                descriptor.magFilter = texture.getMagFilter()
                descriptor.uWrap = texture.getUWrap()
                descriptor.vWrap = texture.getVWrap()
                val offsetU = if (tex.uvTranslation == null) 0f else tex.uvTranslation.x
                val offsetV = if (tex.uvTranslation == null) 0f else tex.uvTranslation.y
                val scaleU = if (tex.uvScaling == null) 1f else tex.uvScaling.x
                val scaleV = if (tex.uvScaling == null) 1f else tex.uvScaling.y
                when (tex.usage) {
                    ModelTexture.USAGE_DIFFUSE -> result[] = TextureAttribute(TextureAttribute.Diffuse, descriptor, offsetU, offsetV, scaleU, scaleV)
                    ModelTexture.USAGE_SPECULAR -> result[] = TextureAttribute(TextureAttribute.Specular, descriptor, offsetU, offsetV, scaleU, scaleV)
                    ModelTexture.USAGE_BUMP -> result[] = TextureAttribute(TextureAttribute.Bump, descriptor, offsetU, offsetV, scaleU, scaleV)
                    ModelTexture.USAGE_NORMAL -> result[] = TextureAttribute(TextureAttribute.Normal, descriptor, offsetU, offsetV, scaleU, scaleV)
                    ModelTexture.USAGE_AMBIENT -> result[] = TextureAttribute(TextureAttribute.Ambient, descriptor, offsetU, offsetV, scaleU, scaleV)
                    ModelTexture.USAGE_EMISSIVE -> result[] = TextureAttribute(TextureAttribute.Emissive, descriptor, offsetU, offsetV, scaleU, scaleV)
                    ModelTexture.USAGE_REFLECTION -> result[] = TextureAttribute(TextureAttribute.Reflection, descriptor, offsetU, offsetV, scaleU, scaleV)
                }
            }
        }
        return result
    }

    /**
     * Adds a [Disposable] to be managed and disposed by this Model. Can be used to keep track of manually loaded textures
     * for [ModelInstance].
     *
     * @param disposable the Disposable
     */
    fun manageDisposable(disposable: Disposable?) {
        if (!disposables.contains(disposable, true)) disposables.add(disposable)
    }

    /**
     * @return the [Disposable] objects that will be disposed when the [.dispose] method is called.
     */
    val managedDisposables: Iterable<Any>
        get() = disposables

    fun dispose() {
        for (disposable in disposables) {
            disposable.dispose()
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
        return getAnimation(id, true)
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
    fun getMaterial(id: String): Material? {
        return getMaterial(id, true)
    }

    /**
     * @param id         The ID of the material to fetch.
     * @param ignoreCase whether to use case sensitivity when comparing the material id.
     * @return The [Material] with the specified id, or null if not available.
     */
    fun getMaterial(id: String, ignoreCase: Boolean): Material? {
        val n = materials.size
        var material: Material?
        if (ignoreCase) {
            for (i in 0 until n) if (materials[i].also { material = it }.id.equals(id, ignoreCase = true)) return material
        } else {
            for (i in 0 until n) if (materials[i].also { material = it }.id == id) return material
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
}
