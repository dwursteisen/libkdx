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

import java.lang.RuntimeException

/**
 * Class to construct a mesh, optionally splitting it into one or more mesh parts. Before you can call any other method you must
 * call [.begin] or [.begin]. To use mesh parts you must call
 * [.part] before you start building the part. The MeshPart itself is only valid after the call to
 * [.end].
 *
 * @author Xoppa
 */
class MeshBuilder : MeshPartBuilder {

    private val vertTmp1 = MeshPartBuilder.VertexInfo()
    private val vertTmp2 = MeshPartBuilder.VertexInfo()
    private val vertTmp3 = MeshPartBuilder.VertexInfo()
    private val vertTmp4 = MeshPartBuilder.VertexInfo()
    private val tempC1: Color = Color()

    /**
     * The vertex attributes of the resulting mesh
     */
    private override var attributes: VertexAttributes? = null

    /**
     * The vertices to construct, no size checking is done
     */
    private val vertices: FloatArray = FloatArray()

    /**
     * The indices to construct, no size checking is done
     */
    private val indices: ShortArray = ShortArray()

    /**
     * @return the size in number of floats of one vertex, multiply by four to get the size in bytes.
     */
    /**
     * The size (in number of floats) of each vertex
     */
    var floatsPerVertex = 0
        private set

    /**
     * The current vertex index, used for indexing
     */
    private var vindex = 0

    /**
     * The offset in the indices array when begin() was called, used to define a meshpart.
     */
    private var istart = 0

    /**
     * The offset within an vertex to position
     */
    private var posOffset = 0

    /**
     * The size (in number of floats) of the position attribute
     */
    private var posSize = 0

    /**
     * The offset within an vertex to normal, or -1 if not available
     */
    private var norOffset = 0

    /**
     * The offset within a vertex to binormal, or -1 if not available
     */
    private var biNorOffset = 0

    /**
     * The offset within a vertex to tangent, or -1 if not available
     */
    private var tangentOffset = 0

    /**
     * The offset within an vertex to color, or -1 if not available
     */
    private var colOffset = 0

    /**
     * The size (in number of floats) of the color attribute
     */
    private var colSize = 0

    /**
     * The offset within an vertex to packed color, or -1 if not available
     */
    private var cpOffset = 0

    /**
     * The offset within an vertex to texture coordinates, or -1 if not available
     */
    private var uvOffset = 0

    /**
     * The meshpart currently being created
     */
    private var part: MeshPart? = null

    /**
     * The parts created between begin and end
     */
    private val parts: Array<MeshPart> = Array<MeshPart>()

    /**
     * The color used if no vertex color is specified.
     */
    private val color: Color = Color(Color.WHITE)
    private var hasColor = false

    /**
     * The current primitiveType
     */
    override var primitiveType = 0
        private set

    /**
     * The UV range used when building
     */
    private var uOffset = 0f
    private var uScale = 1f
    private var vOffset = 0f
    private var vScale = 1f
    private var hasUVTransform = false
    private var vertex: FloatArray?
    override var isVertexTransformationEnabled: Boolean = false public get() {
        return field
    }
    private val positionTransform: Matrix4 = Matrix4()
    private val normalTransform: Matrix3 = Matrix3()
    private val bounds: BoundingBox = BoundingBox()

    /**
     * Begin building a mesh. Call [.part] to start a [MeshPart].
     *
     * @param attributes bitwise mask of the [com.badlogic.gdx.graphics.VertexAttributes.Usage], only Position, Color, Normal
     * and TextureCoordinates is supported.
     */
    fun begin(attributes: Long) {
        begin(createAttributes(attributes), -1)
    }

    /**
     * Begin building a mesh. Call [.part] to start a [MeshPart].
     */
    fun begin(attributes: VertexAttributes?) {
        begin(attributes, -1)
    }

    /**
     * Begin building a mesh.
     *
     * @param attributes bitwise mask of the [com.badlogic.gdx.graphics.VertexAttributes.Usage], only Position, Color, Normal
     * and TextureCoordinates is supported.
     */
    fun begin(attributes: Long, primitiveType: Int) {
        begin(createAttributes(attributes), primitiveType)
    }

    /**
     * Begin building a mesh
     */
    fun begin(attributes: VertexAttributes, primitiveType: Int) {
        if (this.attributes != null) throw RuntimeException("Call end() first")
        this.attributes = attributes
        vertices.clear()
        this.indices.clear()
        parts.clear()
        vindex = 0
        this.lastIndex = -1
        istart = 0
        part = null
        floatsPerVertex = attributes.vertexSize / 4
        if (vertex == null || vertex!!.size < floatsPerVertex) vertex = FloatArray(floatsPerVertex)
        var a: VertexAttribute = attributes.findByUsage(Usage.Position)
            ?: throw GdxRuntimeException("Cannot build mesh without position attribute")
        posOffset = a.offset / 4
        posSize = a.numComponents
        a = attributes.findByUsage(Usage.Normal)
        norOffset = if (a == null) -1 else a.offset / 4
        a = attributes.findByUsage(Usage.BiNormal)
        biNorOffset = if (a == null) -1 else a.offset / 4
        a = attributes.findByUsage(Usage.Tangent)
        tangentOffset = if (a == null) -1 else a.offset / 4
        a = attributes.findByUsage(Usage.ColorUnpacked)
        colOffset = if (a == null) -1 else a.offset / 4
        colSize = if (a == null) 0 else a.numComponents
        a = attributes.findByUsage(Usage.ColorPacked)
        cpOffset = if (a == null) -1 else a.offset / 4
        a = attributes.findByUsage(Usage.TextureCoordinates)
        uvOffset = if (a == null) -1 else a.offset / 4
        setColor(null)
        setVertexTransform(null)
        setUVRange(null)
        this.primitiveType = primitiveType
        bounds.inf()
    }

    private fun endpart() {
        if (part != null) {
            bounds.getCenter(part.center)
            bounds.getDimensions(part.halfExtents).scl(0.5f)
            part.radius = part.halfExtents.len()
            bounds.inf()
            part.offset = istart
            part.size = indices.size - istart
            istart = indices.size
            part = null
        }
    }

    /**
     * Starts a new MeshPart. The mesh part is not usable until end() is called. This will reset the current color and vertex
     * transformation.
     *
     * @see .part
     */
    fun part(id: String?, primitiveType: Int): MeshPart {
        return part(id, primitiveType, MeshPart())
    }

    /**
     * Starts a new MeshPart. The mesh part is not usable until end() is called. This will reset the current color and vertex
     * transformation.
     *
     * @param id            The id (name) of the part
     * @param primitiveType e.g. [GL20.GL_TRIANGLES] or [GL20.GL_LINES]
     * @param meshPart      The part to receive the result
     */
    fun part(id: String?, primitiveType: Int, meshPart: MeshPart?): MeshPart {
        if (attributes == null) throw RuntimeException("Call begin() first")
        endpart()
        part = meshPart
        part.id = id
        part.primitiveType = primitiveType
        this.primitiveType = part.primitiveType
        parts.add(part)
        setColor(null)
        setVertexTransform(null)
        setUVRange(null)
        return part
    }

    /**
     * End building the mesh and returns the mesh
     *
     * @param mesh The mesh to receive the built vertices and indices, must have the same attributes and must be big enough to hold
     * the data, any existing data will be overwritten.
     */
    fun end(mesh: Mesh): Mesh {
        endpart()
        if (attributes == null) throw GdxRuntimeException("Call begin() first")
        if (!attributes.equals(mesh.getVertexAttributes())) throw GdxRuntimeException("Mesh attributes don't match")
        if (mesh.getMaxVertices() * floatsPerVertex < vertices.size) throw GdxRuntimeException("Mesh can't hold enough vertices: " + mesh.getMaxVertices().toString() + " * " + floatsPerVertex.toString() + " < "
            + vertices.size)
        if (mesh.getMaxIndices() < indices.size) throw GdxRuntimeException("Mesh can't hold enough indices: " + mesh.getMaxIndices().toString() + " < " + indices.size)
        mesh.setVertices(vertices.items, 0, vertices.size)
        mesh.setIndices(indices.items, 0, indices.size)
        for (p in parts) p.mesh = mesh
        parts.clear()
        attributes = null
        vertices.clear()
        indices.clear()
        return mesh
    }

    /**
     * End building the mesh and returns the mesh
     */
    fun end(): Mesh {
        return end(Mesh(true, vertices.size / floatsPerVertex, indices.size, attributes))
    }

    /**
     * Clears the data being built up until now, including the vertices, indices and all parts. Must be called in between the call
     * to #begin and #end. Any builder calls made from the last call to #begin up until now are practically discarded. The state
     * (e.g. UV region, color, vertex transform) will remain unchanged.
     */
    fun clear() {
        vertices.clear()
        this.indices.clear()
        parts.clear()
        vindex = 0
        this.lastIndex = -1
        istart = 0
        part = null
    }

    /**
     * @return The number of vertices built up until now, only valid in between the call to begin() and end().
     */
    val numVertices: Int
        get() = vertices.size / floatsPerVertex

    /**
     * Get a copy of the vertices built so far.
     *
     * @param out        The float array to receive the copy of the vertices, must be at least `destOffset` + [.getNumVertices] *
     * [.getFloatsPerVertex] in size.
     * @param destOffset The offset (number of floats) in the out array where to start copying
     */
    fun getVertices(out: FloatArray, destOffset: Int) {
        if (attributes == null) throw GdxRuntimeException("Must be called in between #begin and #end")
        if (destOffset < 0 || destOffset > out.size - vertices.size) throw GdxRuntimeException("Array to small or offset out of range")
        java.lang.System.arraycopy(vertices.items, 0, out, destOffset, vertices.size)
    }

    /**
     * Provides direct access to the vertices array being built, use with care. The size of the array might be bigger, do not rely
     * on the length of the array. Instead use [.getFloatsPerVertex] * [.getNumVertices] to calculate the usable
     * size of the array. Must be called in between the call to #begin and #end.
     */
    protected fun getVertices(): FloatArray {
        return vertices.items
    }

    /**
     * @return The number of indices built up until now, only valid in between the call to begin() and end().
     */
    val numIndices: Int
        get() = indices.size

    /**
     * Get a copy of the indices built so far.
     *
     * @param out        The short array to receive the copy of the indices, must be at least `destOffset` + [.getNumIndices] in
     * size.
     * @param destOffset The offset (number of shorts) in the out array where to start copying
     */
    fun getIndices(out: ShortArray, destOffset: Int) {
        if (attributes == null) throw GdxRuntimeException("Must be called in between #begin and #end")
        if (destOffset < 0 || destOffset > out.size - indices.size) throw GdxRuntimeException("Array to small or offset out of range")
        java.lang.System.arraycopy(indices.items, 0, out, destOffset, indices.size)
    }

    /**
     * Provides direct access to the indices array being built, use with care. The size of the array might be bigger, do not rely
     * on the length of the array. Instead use [.getNumIndices] to calculate the usable size of the array. Must be called
     * in between the call to #begin and #end.
     */
    protected fun getIndices(): ShortArray {
        return indices.items
    }

    override fun getAttributes(): VertexAttributes? {
        return attributes
    }

    override val meshPart: MeshPart?
        get() = part

    override fun setColor(r: Float, g: Float, b: Float, a: Float) {
        color.set(r, g, b, a)
        hasColor = !color.equals(Color.WHITE)
    }

    fun setColor(color: Color?) {
        this.color.set(if (!(color != null).also { hasColor = it }) Color.WHITE else color)
    }

    override fun setUVRange(u1: Float, v1: Float, u2: Float, v2: Float) {
        uOffset = u1
        vOffset = v1
        uScale = u2 - u1
        vScale = v2 - v1
        hasUVTransform = !(MathUtils.isZero(u1) && MathUtils.isZero(v1) && MathUtils.isEqual(u2, 1f) && MathUtils.isEqual(v2, 1f))
    }

    fun setUVRange(region: TextureRegion?) {
        if (!(region != null).also { hasUVTransform = it }) {
            vOffset = 0f
            uOffset = vOffset
            vScale = 1f
            uScale = vScale
        } else setUVRange(region.getU(), region.getV(), region.getU2(), region.getV2())
    }

    override fun getVertexTransform(out: Matrix4?): Matrix4? {
        return out.set(positionTransform)
    }

    override fun setVertexTransform(transform: Matrix4?) {
        isVertexTransformationEnabled = transform != null
        if (isVertexTransformationEnabled) {
            positionTransform.set(transform)
            normalTransform.set(transform).inv().transpose()
        } else {
            positionTransform.idt()
            normalTransform.idt()
        }
    }

    override fun ensureVertices(numVertices: Int) {
        vertices.ensureCapacity(floatsPerVertex * numVertices)
    }

    override fun ensureIndices(numIndices: Int) {
        indices.ensureCapacity(numIndices)
    }

    override fun ensureCapacity(numVertices: Int, numIndices: Int) {
        ensureVertices(numVertices)
        ensureIndices(numIndices)
    }

    override fun ensureTriangleIndices(numTriangles: Int) {
        if (primitiveType == GL20.GL_LINES) ensureIndices(6 * numTriangles) else if (primitiveType == GL20.GL_TRIANGLES || primitiveType == GL20.GL_POINTS) ensureIndices(3 * numTriangles) else throw GdxRuntimeException("Incorrect primtive type")
    }

    @Deprecated("use {@link #ensureVertices(int)} followed by {@link #ensureTriangleIndices(int)} instead.")
    fun ensureTriangles(numVertices: Int, numTriangles: Int) {
        ensureVertices(numVertices)
        ensureTriangleIndices(numTriangles)
    }

    @Deprecated("use {@link #ensureVertices(int)} followed by {@link #ensureTriangleIndices(int)} instead.")
    fun ensureTriangles(numTriangles: Int) {
        ensureVertices(3 * numTriangles)
        ensureTriangleIndices(numTriangles)
    }

    override fun ensureRectangleIndices(numRectangles: Int) {
        if (primitiveType == GL20.GL_POINTS) ensureIndices(4 * numRectangles) else if (primitiveType == GL20.GL_LINES) ensureIndices(8 * numRectangles) else  // GL_TRIANGLES
            ensureIndices(6 * numRectangles)
    }

    @Deprecated("use {@link #ensureVertices(int)} followed by {@link #ensureRectangleIndices(int)} instead.")
    fun ensureRectangles(numVertices: Int, numRectangles: Int) {
        ensureVertices(numVertices)
        ensureRectangleIndices(numRectangles)
    }

    @Deprecated("use {@link #ensureVertices(int)} followed by {@link #ensureRectangleIndices(int)} instead.")
    fun ensureRectangles(numRectangles: Int) {
        ensureVertices(4 * numRectangles)
        ensureRectangleIndices(numRectangles)
    }

    private var lastIndex: Short = -1
    override fun lastIndex(): Short {
        return lastIndex
    }

    private fun addVertex(values: FloatArray?, offset: Int) {
        val o = vertices.size
        vertices.addAll(values, offset, floatsPerVertex)
        lastIndex = vindex++.toShort()
        if (isVertexTransformationEnabled) {
            transformPosition(vertices.items, o + posOffset, posSize, positionTransform)
            if (norOffset >= 0) transformNormal(vertices.items, o + norOffset, 3, normalTransform)
            if (biNorOffset >= 0) transformNormal(vertices.items, o + biNorOffset, 3, normalTransform)
            if (tangentOffset >= 0) transformNormal(vertices.items, o + tangentOffset, 3, normalTransform)
        }
        val x: Float = vertices.items.get(o + posOffset)
        val y = if (posSize > 1) vertices.items.get(o + posOffset + 1) else 0f
        val z = if (posSize > 2) vertices.items.get(o + posOffset + 2) else 0f
        bounds.ext(x, y, z)
        if (hasColor) {
            if (colOffset >= 0) {
                vertices.items.get(o + colOffset) *= color.r
                vertices.items.get(o + colOffset + 1) *= color.g
                vertices.items.get(o + colOffset + 2) *= color.b
                if (colSize > 3) vertices.items.get(o + colOffset + 3) *= color.a
            } else if (cpOffset >= 0) {
                Color.abgr8888ToColor(tempC1, vertices.items.get(o + cpOffset))
                vertices.items.get(o + cpOffset) = tempC1.mul(color).toFloatBits()
            }
        }
        if (hasUVTransform && uvOffset >= 0) {
            vertices.items.get(o + uvOffset) = uOffset + uScale * vertices.items.get(o + uvOffset)
            vertices.items.get(o + uvOffset + 1) = vOffset + vScale * vertices.items.get(o + uvOffset + 1)
        }
    }

    private val tmpNormal: Vector3 = Vector3()
    fun vertex(pos: Vector3, nor: Vector3?, col: Color?, uv: Vector2?): Short {
        var nor: Vector3? = nor
        var col: Color? = col
        if (vindex > Short.MAX_VALUE) throw GdxRuntimeException("Too many vertices used")
        vertex!![posOffset] = pos.x
        if (posSize > 1) vertex!![posOffset + 1] = pos.y
        if (posSize > 2) vertex!![posOffset + 2] = pos.z
        if (norOffset >= 0) {
            if (nor == null) nor = tmpNormal.set(pos).nor()
            vertex!![norOffset] = nor.x
            vertex!![norOffset + 1] = nor.y
            vertex!![norOffset + 2] = nor.z
        }
        if (colOffset >= 0) {
            if (col == null) col = Color.WHITE
            vertex!![colOffset] = col.r
            vertex!![colOffset + 1] = col.g
            vertex!![colOffset + 2] = col.b
            if (colSize > 3) vertex!![colOffset + 3] = col.a
        } else if (cpOffset > 0) {
            if (col == null) col = Color.WHITE
            vertex!![cpOffset] = col.toFloatBits() // FIXME cache packed color?
        }
        if (uv != null && uvOffset >= 0) {
            vertex!![uvOffset] = uv.x
            vertex!![uvOffset + 1] = uv.y
        }
        addVertex(vertex, 0)
        return lastIndex
    }

    override fun vertex(vararg values: Float): Short {
        val n = values.size - floatsPerVertex
        var i = 0
        while (i <= n) {
            addVertex(values, i)
            i += floatsPerVertex
        }
        return lastIndex
    }

    override fun vertex(info: MeshPartBuilder.VertexInfo?): Short {
        return vertex(if (info!!.hasPosition) info.position else null, if (info.hasNormal) info.normal else null, if (info.hasColor) info.color else null, if (info.hasUV) info.uv else null)
    }

    override fun index(value: Short) {
        indices.add(value)
    }

    override fun index(value1: Short, value2: Short) {
        ensureIndices(2)
        indices.add(value1)
        indices.add(value2)
    }

    override fun index(value1: Short, value2: Short, value3: Short) {
        ensureIndices(3)
        indices.add(value1)
        indices.add(value2)
        indices.add(value3)
    }

    override fun index(value1: Short, value2: Short, value3: Short, value4: Short) {
        ensureIndices(4)
        indices.add(value1)
        indices.add(value2)
        indices.add(value3)
        indices.add(value4)
    }

    override fun index(value1: Short, value2: Short, value3: Short, value4: Short, value5: Short, value6: Short) {
        ensureIndices(6)
        indices.add(value1)
        indices.add(value2)
        indices.add(value3)
        indices.add(value4)
        indices.add(value5)
        indices.add(value6)
    }

    override fun index(value1: Short, value2: Short, value3: Short, value4: Short, value5: Short, value6: Short, value7: Short,
                       value8: Short) {
        ensureIndices(8)
        indices.add(value1)
        indices.add(value2)
        indices.add(value3)
        indices.add(value4)
        indices.add(value5)
        indices.add(value6)
        indices.add(value7)
        indices.add(value8)
    }

    override fun line(index1: Short, index2: Short) {
        if (primitiveType != GL20.GL_LINES) throw GdxRuntimeException("Incorrect primitive type")
        index(index1, index2)
    }

    override fun line(p1: MeshPartBuilder.VertexInfo?, p2: MeshPartBuilder.VertexInfo?) {
        ensureVertices(2)
        line(vertex(p1), vertex(p2))
    }

    fun line(p1: Vector3?, p2: Vector3?) {
        line(vertTmp1.set(p1, null, null, null), vertTmp2.set(p2, null, null, null))
    }

    override fun line(x1: Float, y1: Float, z1: Float, x2: Float, y2: Float, z2: Float) {
        line(vertTmp1.set(null, null, null, null).setPos(x1, y1, z1), vertTmp2.set(null, null, null, null).setPos(x2, y2, z2))
    }

    fun line(p1: Vector3?, c1: Color?, p2: Vector3?, c2: Color?) {
        line(vertTmp1.set(p1, null, c1, null), vertTmp2.set(p2, null, c2, null))
    }

    override fun triangle(index1: Short, index2: Short, index3: Short) {
        if (primitiveType == GL20.GL_TRIANGLES || primitiveType == GL20.GL_POINTS) {
            index(index1, index2, index3)
        } else if (primitiveType == GL20.GL_LINES) {
            index(index1, index2, index2, index3, index3, index1)
        } else throw GdxRuntimeException("Incorrect primitive type")
    }

    override fun triangle(p1: MeshPartBuilder.VertexInfo?, p2: MeshPartBuilder.VertexInfo?, p3: MeshPartBuilder.VertexInfo?) {
        ensureVertices(3)
        triangle(vertex(p1), vertex(p2), vertex(p3))
    }

    fun triangle(p1: Vector3?, p2: Vector3?, p3: Vector3?) {
        triangle(vertTmp1.set(p1, null, null, null), vertTmp2.set(p2, null, null, null), vertTmp3.set(p3, null, null, null))
    }

    fun triangle(p1: Vector3?, c1: Color?, p2: Vector3?, c2: Color?, p3: Vector3?, c3: Color?) {
        triangle(vertTmp1.set(p1, null, c1, null), vertTmp2.set(p2, null, c2, null), vertTmp3.set(p3, null, c3, null))
    }

    override fun rect(corner00: Short, corner10: Short, corner11: Short, corner01: Short) {
        if (primitiveType == GL20.GL_TRIANGLES) {
            index(corner00, corner10, corner11, corner11, corner01, corner00)
        } else if (primitiveType == GL20.GL_LINES) {
            index(corner00, corner10, corner10, corner11, corner11, corner01, corner01, corner00)
        } else if (primitiveType == GL20.GL_POINTS) {
            index(corner00, corner10, corner11, corner01)
        } else throw GdxRuntimeException("Incorrect primitive type")
    }

    override fun rect(corner00: MeshPartBuilder.VertexInfo?, corner10: MeshPartBuilder.VertexInfo?, corner11: MeshPartBuilder.VertexInfo?, corner01: MeshPartBuilder.VertexInfo?) {
        ensureVertices(4)
        rect(vertex(corner00), vertex(corner10), vertex(corner11), vertex(corner01))
    }

    fun rect(corner00: Vector3?, corner10: Vector3?, corner11: Vector3?, corner01: Vector3?, normal: Vector3?) {
        rect(vertTmp1.set(corner00, normal, null, null).setUV(0f, 1f), vertTmp2.set(corner10, normal, null, null).setUV(1f, 1f),
            vertTmp3.set(corner11, normal, null, null).setUV(1f, 0f), vertTmp4.set(corner01, normal, null, null).setUV(0f, 0f))
    }

    override fun rect(x00: Float, y00: Float, z00: Float, x10: Float, y10: Float, z10: Float, x11: Float, y11: Float, z11: Float,
                      x01: Float, y01: Float, z01: Float, normalX: Float, normalY: Float, normalZ: Float) {
        rect(vertTmp1.set(null, null, null, null).setPos(x00, y00, z00).setNor(normalX, normalY, normalZ).setUV(0f, 1f), vertTmp2
            .set(null, null, null, null).setPos(x10, y10, z10).setNor(normalX, normalY, normalZ).setUV(1f, 1f),
            vertTmp3.set(null, null, null, null).setPos(x11, y11, z11).setNor(normalX, normalY, normalZ).setUV(1f, 0f), vertTmp4
            .set(null, null, null, null).setPos(x01, y01, z01).setNor(normalX, normalY, normalZ).setUV(0f, 0f))
    }

    override fun addMesh(mesh: Mesh?) {
        addMesh(mesh, 0, mesh.getNumIndices())
    }

    override fun addMesh(meshpart: MeshPart?) {
        if (meshpart.primitiveType !== primitiveType) throw GdxRuntimeException("Primitive type doesn't match")
        addMesh(meshpart.mesh, meshpart.offset, meshpart.size)
    }

    override fun addMesh(mesh: Mesh?, indexOffset: Int, numIndices: Int) {
        if (!attributes.equals(mesh.getVertexAttributes())) throw GdxRuntimeException("Vertex attributes do not match")
        if (numIndices <= 0) return  // silently ignore an empty mesh part

        // FIXME don't triple copy, instead move the copy to jni
        val numFloats: Int = mesh.getNumVertices() * floatsPerVertex
        tmpVertices.clear()
        tmpVertices.ensureCapacity(numFloats)
        tmpVertices.size = numFloats
        mesh.getVertices(tmpVertices.items)
        tmpIndices.clear()
        tmpIndices.ensureCapacity(numIndices)
        tmpIndices.size = numIndices
        mesh.getIndices(indexOffset, numIndices, tmpIndices.items, 0)
        addMesh(tmpVertices.items, tmpIndices.items, 0, numIndices)
    }

    override fun addMesh(vertices: FloatArray?, indices: ShortArray?, indexOffset: Int, numIndices: Int) {
        if (indicesMap == null) indicesMap = IntIntMap(numIndices) else {
            indicesMap.clear()
            indicesMap.ensureCapacity(numIndices)
        }
        ensureIndices(numIndices)
        val numVertices = vertices!!.size / floatsPerVertex
        ensureVertices(if (numVertices < numIndices) numVertices else numIndices)
        for (i in 0 until numIndices) {
            val sidx = indices!![indexOffset + i].toInt()
            var didx: Int = indicesMap.get(sidx, -1)
            if (didx < 0) {
                addVertex(vertices, sidx * floatsPerVertex)
                indicesMap.put(sidx, lastIndex.also { didx = it.toInt() })
            }
            index(didx.toShort())
        }
    }

    override fun addMesh(vertices: FloatArray?, indices: ShortArray?) {
        val offset = (lastIndex + 1).toShort()
        val numVertices = vertices!!.size / floatsPerVertex
        ensureVertices(numVertices)
        var v = 0
        while (v < vertices.size) {
            addVertex(vertices, v)
            v += floatsPerVertex
        }
        ensureIndices(indices!!.size)
        for (i in indices.indices) index((indices[i] + offset).toShort())
    }

    // TODO: The following methods are deprecated and will be removed in a future release
    @Deprecated("")
    override fun patch(corner00: MeshPartBuilder.VertexInfo?, corner10: MeshPartBuilder.VertexInfo?, corner11: MeshPartBuilder.VertexInfo?, corner01: MeshPartBuilder.VertexInfo?, divisionsU: Int,
                       divisionsV: Int) {
        PatchShapeBuilder.build(this, corner00, corner10, corner11, corner01, divisionsU, divisionsV)
    }

    @Deprecated("")
    fun patch(corner00: Vector3?, corner10: Vector3?, corner11: Vector3?, corner01: Vector3?, normal: Vector3?, divisionsU: Int,
              divisionsV: Int) {
        PatchShapeBuilder.build(this, corner00, corner10, corner11, corner01, normal, divisionsU, divisionsV)
    }

    @Deprecated("")
    override fun patch(x00: Float, y00: Float, z00: Float, x10: Float, y10: Float, z10: Float, x11: Float, y11: Float, z11: Float,
                       x01: Float, y01: Float, z01: Float, normalX: Float, normalY: Float, normalZ: Float, divisionsU: Int, divisionsV: Int) {
        PatchShapeBuilder.build(this, x00, y00, z00, x10, y10, z10, x11, y11, z11, x01, y01, z01, normalX, normalY, normalZ, divisionsU, divisionsV)
    }

    @Deprecated("")
    override fun box(corner000: MeshPartBuilder.VertexInfo?, corner010: MeshPartBuilder.VertexInfo?, corner100: MeshPartBuilder.VertexInfo?, corner110: MeshPartBuilder.VertexInfo?, corner001: MeshPartBuilder.VertexInfo?,
                     corner011: MeshPartBuilder.VertexInfo?, corner101: MeshPartBuilder.VertexInfo?, corner111: MeshPartBuilder.VertexInfo?) {
        BoxShapeBuilder.build(this, corner000, corner010, corner100, corner110, corner001, corner011, corner101, corner111)
    }

    @Deprecated("")
    fun box(corner000: Vector3?, corner010: Vector3?, corner100: Vector3?, corner110: Vector3?, corner001: Vector3?,
            corner011: Vector3?, corner101: Vector3?, corner111: Vector3?) {
        BoxShapeBuilder.build(this, corner000, corner010, corner100, corner110, corner001, corner011, corner101, corner111)
    }

    @Deprecated("")
    override fun box(transform: Matrix4?) {
        BoxShapeBuilder.build(this, transform)
    }

    @Deprecated("")
    override fun box(width: Float, height: Float, depth: Float) {
        BoxShapeBuilder.build(this, width, height, depth)
    }

    @Deprecated("")
    override fun box(x: Float, y: Float, z: Float, width: Float, height: Float, depth: Float) {
        BoxShapeBuilder.build(this, x, y, z, width, height, depth)
    }

    @Deprecated("")
    override fun circle(radius: Float, divisions: Int, centerX: Float, centerY: Float, centerZ: Float, normalX: Float, normalY: Float,
                        normalZ: Float) {
        EllipseShapeBuilder.build(this, radius, divisions, centerX, centerY, centerZ, normalX, normalY, normalZ)
    }

    @Deprecated("")
    fun circle(radius: Float, divisions: Int, center: Vector3?, normal: Vector3?) {
        EllipseShapeBuilder.build(this, radius, divisions, center, normal)
    }

    @Deprecated("")
    fun circle(radius: Float, divisions: Int, center: Vector3?, normal: Vector3?, tangent: Vector3?,
               binormal: Vector3?) {
        EllipseShapeBuilder.build(this, radius, divisions, center, normal, tangent, binormal)
    }

    @Deprecated("")
    override fun circle(radius: Float, divisions: Int, centerX: Float, centerY: Float, centerZ: Float, normalX: Float, normalY: Float,
                        normalZ: Float, tangentX: Float, tangentY: Float, tangentZ: Float, binormalX: Float, binormalY: Float, binormalZ: Float) {
        EllipseShapeBuilder.build(this, radius, divisions, centerX, centerY, centerZ, normalX, normalY, normalZ, tangentX,
            tangentY, tangentZ, binormalX, binormalY, binormalZ)
    }

    @Deprecated("")
    override fun circle(radius: Float, divisions: Int, centerX: Float, centerY: Float, centerZ: Float, normalX: Float, normalY: Float,
                        normalZ: Float, angleFrom: Float, angleTo: Float) {
        EllipseShapeBuilder
            .build(this, radius, divisions, centerX, centerY, centerZ, normalX, normalY, normalZ, angleFrom, angleTo)
    }

    @Deprecated("")
    fun circle(radius: Float, divisions: Int, center: Vector3?, normal: Vector3?, angleFrom: Float, angleTo: Float) {
        EllipseShapeBuilder.build(this, radius, divisions, center, normal, angleFrom, angleTo)
    }

    @Deprecated("")
    fun circle(radius: Float, divisions: Int, center: Vector3, normal: Vector3, tangent: Vector3,
               binormal: Vector3, angleFrom: Float, angleTo: Float) {
        circle(radius, divisions, center.x, center.y, center.z, normal.x, normal.y, normal.z, tangent.x, tangent.y, tangent.z,
            binormal.x, binormal.y, binormal.z, angleFrom, angleTo)
    }

    @Deprecated("")
    override fun circle(radius: Float, divisions: Int, centerX: Float, centerY: Float, centerZ: Float, normalX: Float, normalY: Float,
                        normalZ: Float, tangentX: Float, tangentY: Float, tangentZ: Float, binormalX: Float, binormalY: Float, binormalZ: Float,
                        angleFrom: Float, angleTo: Float) {
        EllipseShapeBuilder.build(this, radius, divisions, centerX, centerY, centerZ, normalX, normalY, normalZ, tangentX,
            tangentY, tangentZ, binormalX, binormalY, binormalZ, angleFrom, angleTo)
    }

    @Deprecated("")
    override fun ellipse(width: Float, height: Float, divisions: Int, centerX: Float, centerY: Float, centerZ: Float, normalX: Float,
                         normalY: Float, normalZ: Float) {
        EllipseShapeBuilder.build(this, width, height, divisions, centerX, centerY, centerZ, normalX, normalY, normalZ)
    }

    @Deprecated("")
    fun ellipse(width: Float, height: Float, divisions: Int, center: Vector3?, normal: Vector3?) {
        EllipseShapeBuilder.build(this, width, height, divisions, center, normal)
    }

    @Deprecated("")
    fun ellipse(width: Float, height: Float, divisions: Int, center: Vector3?, normal: Vector3?,
                tangent: Vector3?, binormal: Vector3?) {
        EllipseShapeBuilder.build(this, width, height, divisions, center, normal, tangent, binormal)
    }

    @Deprecated("")
    override fun ellipse(width: Float, height: Float, divisions: Int, centerX: Float, centerY: Float, centerZ: Float, normalX: Float,
                         normalY: Float, normalZ: Float, tangentX: Float, tangentY: Float, tangentZ: Float, binormalX: Float, binormalY: Float,
                         binormalZ: Float) {
        EllipseShapeBuilder.build(this, width, height, divisions, centerX, centerY, centerZ, normalX, normalY, normalZ, tangentX,
            tangentY, tangentZ, binormalX, binormalY, binormalZ)
    }

    @Deprecated("")
    override fun ellipse(width: Float, height: Float, divisions: Int, centerX: Float, centerY: Float, centerZ: Float, normalX: Float,
                         normalY: Float, normalZ: Float, angleFrom: Float, angleTo: Float) {
        EllipseShapeBuilder.build(this, width, height, divisions, centerX, centerY, centerZ, normalX, normalY, normalZ, angleFrom,
            angleTo)
    }

    @Deprecated("")
    fun ellipse(width: Float, height: Float, divisions: Int, center: Vector3?, normal: Vector3?, angleFrom: Float,
                angleTo: Float) {
        EllipseShapeBuilder.build(this, width, height, divisions, center, normal, angleFrom, angleTo)
    }

    @Deprecated("")
    fun ellipse(width: Float, height: Float, divisions: Int, center: Vector3?, normal: Vector3?,
                tangent: Vector3?, binormal: Vector3?, angleFrom: Float, angleTo: Float) {
        EllipseShapeBuilder.build(this, width, height, divisions, center, normal, tangent, binormal, angleFrom, angleTo)
    }

    @Deprecated("")
    override fun ellipse(width: Float, height: Float, divisions: Int, centerX: Float, centerY: Float, centerZ: Float, normalX: Float,
                         normalY: Float, normalZ: Float, tangentX: Float, tangentY: Float, tangentZ: Float, binormalX: Float, binormalY: Float,
                         binormalZ: Float, angleFrom: Float, angleTo: Float) {
        EllipseShapeBuilder.build(this, width, height, divisions, centerX, centerY, centerZ, normalX, normalY, normalZ, tangentX,
            tangentY, tangentZ, binormalX, binormalY, binormalZ, angleFrom, angleTo)
    }

    @Deprecated("")
    fun ellipse(width: Float, height: Float, innerWidth: Float, innerHeight: Float, divisions: Int, center: Vector3?,
                normal: Vector3?) {
        EllipseShapeBuilder.build(this, width, height, innerWidth, innerHeight, divisions, center, normal)
    }

    @Deprecated("")
    override fun ellipse(width: Float, height: Float, innerWidth: Float, innerHeight: Float, divisions: Int, centerX: Float,
                         centerY: Float, centerZ: Float, normalX: Float, normalY: Float, normalZ: Float) {
        EllipseShapeBuilder.build(this, width, height, innerWidth, innerHeight, divisions, centerX, centerY, centerZ, normalX,
            normalY, normalZ)
    }

    @Deprecated("")
    override fun ellipse(width: Float, height: Float, innerWidth: Float, innerHeight: Float, divisions: Int, centerX: Float,
                         centerY: Float, centerZ: Float, normalX: Float, normalY: Float, normalZ: Float, angleFrom: Float, angleTo: Float) {
        EllipseShapeBuilder.build(this, width, height, innerWidth, innerHeight, divisions, centerX, centerY, centerZ, normalX,
            normalY, normalZ, angleFrom, angleTo)
    }

    @Deprecated("")
    override fun ellipse(width: Float, height: Float, innerWidth: Float, innerHeight: Float, divisions: Int, centerX: Float,
                         centerY: Float, centerZ: Float, normalX: Float, normalY: Float, normalZ: Float, tangentX: Float, tangentY: Float, tangentZ: Float,
                         binormalX: Float, binormalY: Float, binormalZ: Float, angleFrom: Float, angleTo: Float) {
        EllipseShapeBuilder.build(this, width, height, innerWidth, innerHeight, divisions, centerX, centerY, centerZ, normalX,
            normalY, normalZ, tangentX, tangentY, tangentZ, binormalX, binormalY, binormalZ, angleFrom, angleTo)
    }

    @Deprecated("")
    override fun cylinder(width: Float, height: Float, depth: Float, divisions: Int) {
        CylinderShapeBuilder.build(this, width, height, depth, divisions)
    }

    @Deprecated("")
    override fun cylinder(width: Float, height: Float, depth: Float, divisions: Int, angleFrom: Float, angleTo: Float) {
        CylinderShapeBuilder.build(this, width, height, depth, divisions, angleFrom, angleTo)
    }

    @Deprecated("")
    override fun cylinder(width: Float, height: Float, depth: Float, divisions: Int, angleFrom: Float, angleTo: Float, close: Boolean) {
        CylinderShapeBuilder.build(this, width, height, depth, divisions, angleFrom, angleTo, close)
    }

    @Deprecated("")
    override fun cone(width: Float, height: Float, depth: Float, divisions: Int) {
        cone(width, height, depth, divisions, 0f, 360f)
    }

    @Deprecated("")
    override fun cone(width: Float, height: Float, depth: Float, divisions: Int, angleFrom: Float, angleTo: Float) {
        ConeShapeBuilder.build(this, width, height, depth, divisions, angleFrom, angleTo)
    }

    @Deprecated("")
    override fun sphere(width: Float, height: Float, depth: Float, divisionsU: Int, divisionsV: Int) {
        SphereShapeBuilder.build(this, width, height, depth, divisionsU, divisionsV)
    }

    @Deprecated("")
    override fun sphere(transform: Matrix4?, width: Float, height: Float, depth: Float, divisionsU: Int, divisionsV: Int) {
        SphereShapeBuilder.build(this, transform, width, height, depth, divisionsU, divisionsV)
    }

    @Deprecated("")
    override fun sphere(width: Float, height: Float, depth: Float, divisionsU: Int, divisionsV: Int, angleUFrom: Float, angleUTo: Float,
                        angleVFrom: Float, angleVTo: Float) {
        SphereShapeBuilder.build(this, width, height, depth, divisionsU, divisionsV, angleUFrom, angleUTo, angleVFrom, angleVTo)
    }

    @Deprecated("")
    override fun sphere(transform: Matrix4?, width: Float, height: Float, depth: Float, divisionsU: Int, divisionsV: Int,
                        angleUFrom: Float, angleUTo: Float, angleVFrom: Float, angleVTo: Float) {
        SphereShapeBuilder.build(this, transform, width, height, depth, divisionsU, divisionsV, angleUFrom, angleUTo, angleVFrom,
            angleVTo)
    }

    @Deprecated("")
    override fun capsule(radius: Float, height: Float, divisions: Int) {
        CapsuleShapeBuilder.build(this, radius, height, divisions)
    }

    @Deprecated("")
    override fun arrow(x1: Float, y1: Float, z1: Float, x2: Float, y2: Float, z2: Float, capLength: Float, stemThickness: Float,
                       divisions: Int) {
        ArrowShapeBuilder.build(this, x1, y1, z1, x2, y2, z2, capLength, stemThickness, divisions)
    }

    companion object {
        private val tmpIndices: ShortArray = ShortArray()
        private val tmpVertices: FloatArray = FloatArray()

        /**
         * @param usage bitwise mask of the [com.badlogic.gdx.graphics.VertexAttributes.Usage], only Position, Color, Normal and
         * TextureCoordinates is supported.
         */
        fun createAttributes(usage: Long): VertexAttributes {
            val attrs: Array<VertexAttribute> = Array<VertexAttribute>()
            if (usage and Usage.Position === Usage.Position) attrs.add(VertexAttribute(Usage.Position, 3, ShaderProgram.POSITION_ATTRIBUTE))
            if (usage and Usage.ColorUnpacked === Usage.ColorUnpacked) attrs.add(VertexAttribute(Usage.ColorUnpacked, 4, ShaderProgram.COLOR_ATTRIBUTE))
            if (usage and Usage.ColorPacked === Usage.ColorPacked) attrs.add(VertexAttribute(Usage.ColorPacked, 4, ShaderProgram.COLOR_ATTRIBUTE))
            if (usage and Usage.Normal === Usage.Normal) attrs.add(VertexAttribute(Usage.Normal, 3, ShaderProgram.NORMAL_ATTRIBUTE))
            if (usage and Usage.TextureCoordinates === Usage.TextureCoordinates) attrs.add(VertexAttribute(Usage.TextureCoordinates, 2, ShaderProgram.TEXCOORD_ATTRIBUTE.toString() + "0"))
            val attributes: Array<VertexAttribute?> = arrayOfNulls<VertexAttribute>(attrs.size)
            for (i in attributes.indices) attributes[i] = attrs[i]
            return VertexAttributes(attributes)
        }

        private val vTmp: Vector3 = Vector3()
        private fun transformPosition(values: FloatArray, offset: Int, size: Int, transform: Matrix4) {
            if (size > 2) {
                vTmp.set(values[offset], values[offset + 1], values[offset + 2]).mul(transform)
                values[offset] = vTmp.x
                values[offset + 1] = vTmp.y
                values[offset + 2] = vTmp.z
            } else if (size > 1) {
                vTmp.set(values[offset], values[offset + 1], 0).mul(transform)
                values[offset] = vTmp.x
                values[offset + 1] = vTmp.y
            } else values[offset] = vTmp.set(values[offset], 0, 0).mul(transform).x
        }

        private fun transformNormal(values: FloatArray, offset: Int, size: Int, transform: Matrix3) {
            if (size > 2) {
                vTmp.set(values[offset], values[offset + 1], values[offset + 2]).mul(transform).nor()
                values[offset] = vTmp.x
                values[offset + 1] = vTmp.y
                values[offset + 2] = vTmp.z
            } else if (size > 1) {
                vTmp.set(values[offset], values[offset + 1], 0).mul(transform).nor()
                values[offset] = vTmp.x
                values[offset + 1] = vTmp.y
            } else values[offset] = vTmp.set(values[offset], 0, 0).mul(transform).nor().x
        }

        private var indicesMap: IntIntMap? = null
    }
}
