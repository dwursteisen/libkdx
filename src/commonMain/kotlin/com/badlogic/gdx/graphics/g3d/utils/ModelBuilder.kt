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
package com.badlogic.gdx.graphics.g3d.utils

/**
 * Helper class to create [Model]s from code. To start building use the [.begin] method, when finished building use
 * the [.end] method. The end method returns the model just build. Building cannot be nested, only one model (per
 * ModelBuilder) can be build at the time. The same ModelBuilder can be used to build multiple models sequential. Use the
 * [.node] method to start a new node. Use one of the #part(...) methods to add a part within a node. The
 * [.part] method will return a [MeshPartBuilder] which can be used to build
 * the node part.
 *
 * @author Xoppa
 */
class ModelBuilder {

    /**
     * The model currently being build
     */
    private var model: Model? = null

    /**
     * The node currently being build
     */
    private var node: Node? = null

    /**
     * The mesh builders created between begin and end
     */
    private val builders: Array<MeshBuilder> = Array<MeshBuilder>()
    private val tmpTransform: Matrix4 = Matrix4()
    private fun getBuilder(attributes: VertexAttributes): MeshBuilder {
        for (mb in builders) if (mb.getAttributes().equals(attributes) && mb.lastIndex() < Short.MAX_VALUE / 2) return mb
        val result = MeshBuilder()
        result.begin(attributes)
        builders.add(result)
        return result
    }

    /**
     * Begin building a new model
     */
    fun begin() {
        if (model != null) throw GdxRuntimeException("Call end() first")
        node = null
        model = Model()
        builders.clear()
    }

    /**
     * End building the model.
     *
     * @return The newly created model. Call the [Model.dispose] method when no longer used.
     */
    fun end(): Model {
        if (model == null) throw GdxRuntimeException("Call begin() first")
        val result: Model = model
        endnode()
        model = null
        for (mb in builders) mb.end()
        builders.clear()
        rebuildReferences(result)
        return result
    }

    private fun endnode() {
        if (node != null) {
            node = null
        }
    }

    /**
     * Adds the [Node] to the model and sets it active for building. Use any of the part(...) method to add a NodePart.
     */
    protected fun node(node: Node): Node {
        if (model == null) throw GdxRuntimeException("Call begin() first")
        endnode()
        model.nodes.add(node)
        this.node = node
        return node
    }

    /**
     * Add a node to the model. Use any of the part(...) method to add a NodePart.
     *
     * @return The node being created.
     */
    fun node(): Node {
        val node = Node()
        node(node)
        node.id = "node" + model.nodes.size
        return node
    }

    /**
     * Adds the nodes of the specified model to a new node of the model being build. After this method the given model can no
     * longer be used. Do not call the [Model.dispose] method on that model.
     *
     * @return The newly created node containing the nodes of the given model.
     */
    fun node(id: String?, model: Model): Node {
        val node = Node()
        node.id = id
        node.addChildren(model.nodes)
        node(node)
        for (disposable in model.getManagedDisposables()) manage(disposable)
        return node
    }

    /**
     * Add the [Disposable] object to the model, causing it to be disposed when the model is disposed.
     */
    fun manage(disposable: Disposable?) {
        if (model == null) throw GdxRuntimeException("Call begin() first")
        model.manageDisposable(disposable)
    }

    /**
     * Adds the specified MeshPart to the current Node. The Mesh will be managed by the model and disposed when the model is
     * disposed. The resources the Material might contain are not managed, use [.manage] to add those to the
     * model.
     */
    fun part(meshpart: MeshPart?, material: Material?) {
        if (node == null) node()
        node.parts.add(NodePart(meshpart, material))
    }

    /**
     * Adds the specified mesh part to the current node. The Mesh will be managed by the model and disposed when the model is
     * disposed. The resources the Material might contain are not managed, use [.manage] to add those to the
     * model.
     *
     * @return The added MeshPart.
     */
    fun part(id: String?, mesh: Mesh?, primitiveType: Int, offset: Int, size: Int, material: Material?): MeshPart {
        val meshPart = MeshPart()
        meshPart.id = id
        meshPart.primitiveType = primitiveType
        meshPart.mesh = mesh
        meshPart.offset = offset
        meshPart.size = size
        part(meshPart, material)
        return meshPart
    }

    /**
     * Adds the specified mesh part to the current node. The Mesh will be managed by the model and disposed when the model is
     * disposed. The resources the Material might contain are not managed, use [.manage] to add those to the
     * model.
     *
     * @return The added MeshPart.
     */
    fun part(id: String?, mesh: Mesh, primitiveType: Int, material: Material?): MeshPart {
        return part(id, mesh, primitiveType, 0, mesh.getNumIndices(), material)
    }

    /**
     * Creates a new MeshPart within the current Node and returns a [MeshPartBuilder] which can be used to build the shape of
     * the part. If possible a previously used [MeshPartBuilder] will be reused, to reduce the number of mesh binds.
     * Therefore you can only build one part at a time. The resources the Material might contain are not managed, use
     * [.manage] to add those to the model.
     *
     * @return The [MeshPartBuilder] you can use to build the MeshPart.
     */
    fun part(id: String?, primitiveType: Int, attributes: VertexAttributes, material: Material?): MeshPartBuilder {
        val builder: MeshBuilder = getBuilder(attributes)
        part(builder.part(id, primitiveType), material)
        return builder
    }

    /**
     * Creates a new MeshPart within the current Node and returns a [MeshPartBuilder] which can be used to build the shape of
     * the part. If possible a previously used [MeshPartBuilder] will be reused, to reduce the number of mesh binds.
     * Therefore you can only build one part at a time. The resources the Material might contain are not managed, use
     * [.manage] to add those to the model.
     *
     * @param attributes bitwise mask of the [com.badlogic.gdx.graphics.VertexAttributes.Usage], only Position, Color, Normal
     * and TextureCoordinates is supported.
     * @return The [MeshPartBuilder] you can use to build the MeshPart.
     */
    fun part(id: String?, primitiveType: Int, attributes: Long, material: Material?): MeshPartBuilder {
        return part(id, primitiveType, MeshBuilder.createAttributes(attributes), material)
    }

    /**
     * Convenience method to create a model with a single node containing a box shape. The resources the Material might contain are
     * not managed, use [Model.manageDisposable] to add those to the model.
     *
     * @param attributes bitwise mask of the [com.badlogic.gdx.graphics.VertexAttributes.Usage], only Position, Color, Normal
     * and TextureCoordinates is supported.
     */
    fun createBox(width: Float, height: Float, depth: Float, material: Material?, attributes: Long): Model {
        return createBox(width, height, depth, GL20.GL_TRIANGLES, material, attributes)
    }

    /**
     * Convenience method to create a model with a single node containing a box shape. The resources the Material might contain are
     * not managed, use [Model.manageDisposable] to add those to the model.
     *
     * @param attributes bitwise mask of the [com.badlogic.gdx.graphics.VertexAttributes.Usage], only Position, Color, Normal
     * and TextureCoordinates is supported.
     */
    fun createBox(width: Float, height: Float, depth: Float, primitiveType: Int, material: Material?,
                  attributes: Long): Model {
        begin()
        part("box", primitiveType, attributes, material).box(width, height, depth)
        return end()
    }

    /**
     * Convenience method to create a model with a single node containing a rectangle shape. The resources the Material might
     * contain are not managed, use [Model.manageDisposable] to add those to the model.
     *
     * @param attributes bitwise mask of the [com.badlogic.gdx.graphics.VertexAttributes.Usage], only Position, Color, Normal
     * and TextureCoordinates is supported.
     */
    fun createRect(x00: Float, y00: Float, z00: Float, x10: Float, y10: Float, z10: Float, x11: Float, y11: Float, z11: Float,
                   x01: Float, y01: Float, z01: Float, normalX: Float, normalY: Float, normalZ: Float, material: Material?, attributes: Long): Model {
        return createRect(x00, y00, z00, x10, y10, z10, x11, y11, z11, x01, y01, z01, normalX, normalY, normalZ, GL20.GL_TRIANGLES,
            material, attributes)
    }

    /**
     * Convenience method to create a model with a single node containing a rectangle shape. The resources the Material might
     * contain are not managed, use [Model.manageDisposable] to add those to the model.
     *
     * @param attributes bitwise mask of the [com.badlogic.gdx.graphics.VertexAttributes.Usage], only Position, Color, Normal
     * and TextureCoordinates is supported.
     */
    fun createRect(x00: Float, y00: Float, z00: Float, x10: Float, y10: Float, z10: Float, x11: Float, y11: Float, z11: Float,
                   x01: Float, y01: Float, z01: Float, normalX: Float, normalY: Float, normalZ: Float, primitiveType: Int, material: Material?,
                   attributes: Long): Model {
        begin()
        part("rect", primitiveType, attributes, material).rect(x00, y00, z00, x10, y10, z10, x11, y11, z11, x01, y01, z01, normalX,
            normalY, normalZ)
        return end()
    }

    /**
     * Convenience method to create a model with a single node containing a cylinder shape. The resources the Material might
     * contain are not managed, use [Model.manageDisposable] to add those to the model.
     *
     * @param attributes bitwise mask of the [com.badlogic.gdx.graphics.VertexAttributes.Usage], only Position, Color, Normal
     * and TextureCoordinates is supported.
     */
    fun createCylinder(width: Float, height: Float, depth: Float, divisions: Int, material: Material?,
                       attributes: Long): Model {
        return createCylinder(width, height, depth, divisions, GL20.GL_TRIANGLES, material, attributes)
    }

    /**
     * Convenience method to create a model with a single node containing a cylinder shape. The resources the Material might
     * contain are not managed, use [Model.manageDisposable] to add those to the model.
     *
     * @param attributes bitwise mask of the [com.badlogic.gdx.graphics.VertexAttributes.Usage], only Position, Color, Normal
     * and TextureCoordinates is supported.
     */
    fun createCylinder(width: Float, height: Float, depth: Float, divisions: Int, primitiveType: Int,
                       material: Material?, attributes: Long): Model {
        return createCylinder(width, height, depth, divisions, primitiveType, material, attributes, 0f, 360f)
    }

    /**
     * Convenience method to create a model with a single node containing a cylinder shape. The resources the Material might
     * contain are not managed, use [Model.manageDisposable] to add those to the model.
     *
     * @param attributes bitwise mask of the [com.badlogic.gdx.graphics.VertexAttributes.Usage], only Position, Color, Normal
     * and TextureCoordinates is supported.
     */
    fun createCylinder(width: Float, height: Float, depth: Float, divisions: Int, material: Material?,
                       attributes: Long, angleFrom: Float, angleTo: Float): Model {
        return createCylinder(width, height, depth, divisions, GL20.GL_TRIANGLES, material, attributes, angleFrom, angleTo)
    }

    /**
     * Convenience method to create a model with a single node containing a cylinder shape. The resources the Material might
     * contain are not managed, use [Model.manageDisposable] to add those to the model.
     *
     * @param attributes bitwise mask of the [com.badlogic.gdx.graphics.VertexAttributes.Usage], only Position, Color, Normal
     * and TextureCoordinates is supported.
     */
    fun createCylinder(width: Float, height: Float, depth: Float, divisions: Int, primitiveType: Int,
                       material: Material?, attributes: Long, angleFrom: Float, angleTo: Float): Model {
        begin()
        part("cylinder", primitiveType, attributes, material).cylinder(width, height, depth, divisions, angleFrom, angleTo)
        return end()
    }

    /**
     * Convenience method to create a model with a single node containing a cone shape. The resources the Material might contain
     * are not managed, use [Model.manageDisposable] to add those to the model.
     *
     * @param attributes bitwise mask of the [com.badlogic.gdx.graphics.VertexAttributes.Usage], only Position, Color, Normal
     * and TextureCoordinates is supported.
     */
    fun createCone(width: Float, height: Float, depth: Float, divisions: Int, material: Material?, attributes: Long): Model {
        return createCone(width, height, depth, divisions, GL20.GL_TRIANGLES, material, attributes)
    }

    /**
     * Convenience method to create a model with a single node containing a cone shape. The resources the Material might contain
     * are not managed, use [Model.manageDisposable] to add those to the model.
     *
     * @param attributes bitwise mask of the [com.badlogic.gdx.graphics.VertexAttributes.Usage], only Position, Color, Normal
     * and TextureCoordinates is supported.
     */
    fun createCone(width: Float, height: Float, depth: Float, divisions: Int, primitiveType: Int, material: Material?,
                   attributes: Long): Model {
        return createCone(width, height, depth, divisions, primitiveType, material, attributes, 0f, 360f)
    }

    /**
     * Convenience method to create a model with a single node containing a cone shape. The resources the Material might contain
     * are not managed, use [Model.manageDisposable] to add those to the model.
     *
     * @param attributes bitwise mask of the [com.badlogic.gdx.graphics.VertexAttributes.Usage], only Position, Color, Normal
     * and TextureCoordinates is supported.
     */
    fun createCone(width: Float, height: Float, depth: Float, divisions: Int, material: Material?,
                   attributes: Long, angleFrom: Float, angleTo: Float): Model {
        return createCone(width, height, depth, divisions, GL20.GL_TRIANGLES, material, attributes, angleFrom, angleTo)
    }

    /**
     * Convenience method to create a model with a single node containing a cone shape. The resources the Material might contain
     * are not managed, use [Model.manageDisposable] to add those to the model.
     *
     * @param attributes bitwise mask of the [com.badlogic.gdx.graphics.VertexAttributes.Usage], only Position, Color, Normal
     * and TextureCoordinates is supported.
     */
    fun createCone(width: Float, height: Float, depth: Float, divisions: Int, primitiveType: Int, material: Material?,
                   attributes: Long, angleFrom: Float, angleTo: Float): Model {
        begin()
        part("cone", primitiveType, attributes, material).cone(width, height, depth, divisions, angleFrom, angleTo)
        return end()
    }

    /**
     * Convenience method to create a model with a single node containing a sphere shape. The resources the Material might contain
     * are not managed, use [Model.manageDisposable] to add those to the model.
     *
     * @param attributes bitwise mask of the [com.badlogic.gdx.graphics.VertexAttributes.Usage], only Position, Color, Normal
     * and TextureCoordinates is supported.
     */
    fun createSphere(width: Float, height: Float, depth: Float, divisionsU: Int, divisionsV: Int, material: Material?,
                     attributes: Long): Model {
        return createSphere(width, height, depth, divisionsU, divisionsV, GL20.GL_TRIANGLES, material, attributes)
    }

    /**
     * Convenience method to create a model with a single node containing a sphere shape. The resources the Material might contain
     * are not managed, use [Model.manageDisposable] to add those to the model.
     *
     * @param attributes bitwise mask of the [com.badlogic.gdx.graphics.VertexAttributes.Usage], only Position, Color, Normal
     * and TextureCoordinates is supported.
     */
    fun createSphere(width: Float, height: Float, depth: Float, divisionsU: Int, divisionsV: Int, primitiveType: Int,
                     material: Material?, attributes: Long): Model {
        return createSphere(width, height, depth, divisionsU, divisionsV, primitiveType, material, attributes, 0f, 360f, 0f, 180f)
    }

    /**
     * Convenience method to create a model with a single node containing a sphere shape. The resources the Material might contain
     * are not managed, use [Model.manageDisposable] to add those to the model.
     *
     * @param attributes bitwise mask of the [com.badlogic.gdx.graphics.VertexAttributes.Usage], only Position, Color, Normal
     * and TextureCoordinates is supported.
     */
    fun createSphere(width: Float, height: Float, depth: Float, divisionsU: Int, divisionsV: Int, material: Material?,
                     attributes: Long, angleUFrom: Float, angleUTo: Float, angleVFrom: Float, angleVTo: Float): Model {
        return createSphere(width, height, depth, divisionsU, divisionsV, GL20.GL_TRIANGLES, material, attributes, angleUFrom,
            angleUTo, angleVFrom, angleVTo)
    }

    /**
     * Convenience method to create a model with a single node containing a sphere shape. The resources the Material might contain
     * are not managed, use [Model.manageDisposable] to add those to the model.
     *
     * @param attributes bitwise mask of the [com.badlogic.gdx.graphics.VertexAttributes.Usage], only Position, Color, Normal
     * and TextureCoordinates is supported.
     */
    fun createSphere(width: Float, height: Float, depth: Float, divisionsU: Int, divisionsV: Int, primitiveType: Int,
                     material: Material?, attributes: Long, angleUFrom: Float, angleUTo: Float, angleVFrom: Float, angleVTo: Float): Model {
        begin()
        part("cylinder", primitiveType, attributes, material).sphere(width, height, depth, divisionsU, divisionsV, angleUFrom,
            angleUTo, angleVFrom, angleVTo)
        return end()
    }

    /**
     * Convenience method to create a model with a single node containing a capsule shape. The resources the Material might contain
     * are not managed, use [Model.manageDisposable] to add those to the model.
     *
     * @param attributes bitwise mask of the [com.badlogic.gdx.graphics.VertexAttributes.Usage], only Position, Color, Normal
     * and TextureCoordinates is supported.
     */
    fun createCapsule(radius: Float, height: Float, divisions: Int, material: Material?, attributes: Long): Model {
        return createCapsule(radius, height, divisions, GL20.GL_TRIANGLES, material, attributes)
    }

    /**
     * Convenience method to create a model with a single node containing a capsule shape. The resources the Material might contain
     * are not managed, use [Model.manageDisposable] to add those to the model.
     *
     * @param attributes bitwise mask of the [com.badlogic.gdx.graphics.VertexAttributes.Usage], only Position, Color, Normal
     * and TextureCoordinates is supported.
     */
    fun createCapsule(radius: Float, height: Float, divisions: Int, primitiveType: Int, material: Material?,
                      attributes: Long): Model {
        begin()
        part("capsule", primitiveType, attributes, material).capsule(radius, height, divisions)
        return end()
    }

    /**
     * Convenience method to create a model with three orthonormal vectors shapes. The resources the Material might contain are not
     * managed, use [Model.manageDisposable] to add those to the model.
     *
     * @param axisLength    Length of each axis.
     * @param capLength     is the height of the cap in percentage, must be in (0,1)
     * @param stemThickness is the percentage of stem diameter compared to cap diameter, must be in (0,1]
     * @param divisions     the amount of vertices used to generate the cap and stem ellipsoidal bases
     */
    fun createXYZCoordinates(axisLength: Float, capLength: Float, stemThickness: Float, divisions: Int, primitiveType: Int,
                             material: Material?, attributes: Long): Model {
        begin()
        val partBuilder: MeshPartBuilder
        val node: Node = node()
        partBuilder = part("xyz", primitiveType, attributes, material)
        partBuilder.setColor(Color.RED)
        partBuilder.arrow(0, 0, 0, axisLength, 0, 0, capLength, stemThickness, divisions)
        partBuilder.setColor(Color.GREEN)
        partBuilder.arrow(0, 0, 0, 0, axisLength, 0, capLength, stemThickness, divisions)
        partBuilder.setColor(Color.BLUE)
        partBuilder.arrow(0, 0, 0, 0, 0, axisLength, capLength, stemThickness, divisions)
        return end()
    }

    fun createXYZCoordinates(axisLength: Float, material: Material?, attributes: Long): Model {
        return createXYZCoordinates(axisLength, 0.1f, 0.1f, 5, GL20.GL_TRIANGLES, material, attributes)
    }

    /**
     * Convenience method to create a model with an arrow. The resources the Material might contain are not managed, use
     * [Model.manageDisposable] to add those to the model.
     *
     * @param material
     * @param capLength     is the height of the cap in percentage, must be in (0,1)
     * @param stemThickness is the percentage of stem diameter compared to cap diameter, must be in (0,1]
     * @param divisions     the amount of vertices used to generate the cap and stem ellipsoidal bases
     */
    fun createArrow(x1: Float, y1: Float, z1: Float, x2: Float, y2: Float, z2: Float, capLength: Float, stemThickness: Float,
                    divisions: Int, primitiveType: Int, material: Material?, attributes: Long): Model {
        begin()
        part("arrow", primitiveType, attributes, material).arrow(x1, y1, z1, x2, y2, z2, capLength, stemThickness, divisions)
        return end()
    }

    /**
     * Convenience method to create a model with an arrow. The resources the Material might contain are not managed, use
     * [Model.manageDisposable] to add those to the model.
     */
    fun createArrow(from: Vector3, to: Vector3, material: Material?, attributes: Long): Model {
        return createArrow(from.x, from.y, from.z, to.x, to.y, to.z, 0.1f, 0.1f, 5, GL20.GL_TRIANGLES, material, attributes)
    }

    /**
     * Convenience method to create a model which represents a grid of lines on the XZ plane. The resources the Material might
     * contain are not managed, use [Model.manageDisposable] to add those to the model.
     *
     * @param xDivisions row count along x axis.
     * @param zDivisions row count along z axis.
     * @param xSize      Length of a single row on x.
     * @param zSize      Length of a single row on z.
     */
    fun createLineGrid(xDivisions: Int, zDivisions: Int, xSize: Float, zSize: Float, material: Material?, attributes: Long): Model {
        begin()
        val partBuilder: MeshPartBuilder = part("lines", GL20.GL_LINES, attributes, material)
        val xlength = xDivisions * xSize
        val zlength = zDivisions * zSize
        val hxlength = xlength / 2
        val hzlength = zlength / 2
        var x1 = -hxlength
        var y1 = 0f
        var z1 = hzlength
        var x2 = -hxlength
        var y2 = 0f
        var z2 = -hzlength
        for (i in 0..xDivisions) {
            partBuilder.line(x1, y1, z1, x2, y2, z2)
            x1 += xSize
            x2 += xSize
        }
        x1 = -hxlength
        y1 = 0f
        z1 = -hzlength
        x2 = hxlength
        y2 = 0f
        z2 = -hzlength
        for (j in 0..zDivisions) {
            partBuilder.line(x1, y1, z1, x2, y2, z2)
            z1 += zSize
            z2 += zSize
        }
        return end()
    }

    companion object {
        /**
         * Resets the references to [Material]s, [Mesh]es and [MeshPart]s within the model to the ones used within
         * it's nodes. This will make the model responsible for disposing all referenced meshes.
         */
        fun rebuildReferences(model: Model) {
            model.materials.clear()
            model.meshes.clear()
            model.meshParts.clear()
            for (node in model.nodes) rebuildReferences(model, node)
        }

        private fun rebuildReferences(model: Model, node: Node) {
            for (mpm in node.parts) {
                if (!model.materials.contains(mpm.material, true)) model.materials.add(mpm.material)
                if (!model.meshParts.contains(mpm.meshPart, true)) {
                    model.meshParts.add(mpm.meshPart)
                    if (!model.meshes.contains(mpm.meshPart.mesh, true)) model.meshes.add(mpm.meshPart.mesh)
                    model.manageDisposable(mpm.meshPart.mesh)
                }
            }
            for (child in node.getChildren()) rebuildReferences(model, child)
        }
    }
}
