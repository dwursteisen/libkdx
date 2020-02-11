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
package com.badlogic.gdx.graphics

import java.lang.IndexOutOfBoundsException

/**
 *
 *
 * A Mesh holds vertices composed of attributes specified by a [VertexAttributes] instance. The vertices are held either in
 * VRAM in form of vertex buffer objects or in RAM in form of vertex arrays. The former variant is more performant and is
 * preferred over vertex arrays if hardware supports it.
 *
 *
 *
 *
 * Meshes are automatically managed. If the OpenGL context is lost all vertex buffer objects get invalidated and must be reloaded
 * when the context is recreated. This only happens on Android when a user switches to another application or receives an incoming
 * call. A managed Mesh will be reloaded automagically so you don't have to do this manually.
 *
 *
 *
 *
 * A Mesh consists of vertices and optionally indices which specify which vertices define a triangle. Each vertex is composed of
 * attributes such as position, normal, color or texture coordinate. Note that not all of this attributes must be given, except
 * for position which is non-optional. Each attribute has an alias which is used when rendering a Mesh in OpenGL ES 2.0. The alias
 * is used to bind a specific vertex attribute to a shader attribute. The shader source and the alias of the attribute must match
 * exactly for this to work.
 *
 *
 * @author mzechner, Dave Clayton <contact></contact>@redskyforge.com>, Xoppa
 */
class Mesh : Disposable {

    enum class VertexDataType {
        VertexArray, VertexBufferObject, VertexBufferObjectSubData, VertexBufferObjectWithVAO
    }

    val vertices: VertexData
    val indices: IndexData
    var autoBind = true
    val isVertexArray: Boolean
    var instances: InstanceData? = null
    var isInstanced = false

    protected constructor(vertices: VertexData, indices: IndexData, isVertexArray: Boolean) {
        this.vertices = vertices
        this.indices = indices
        this.isVertexArray = isVertexArray
        addManagedMesh(Gdx.app, this)
    }

    /**
     * Creates a new Mesh with the given attributes.
     *
     * @param isStatic    whether this mesh is static or not. Allows for internal optimizations.
     * @param maxVertices the maximum number of vertices this mesh can hold
     * @param maxIndices  the maximum number of indices this mesh can hold
     * @param attributes  the [VertexAttribute]s. Each vertex attribute defines one property of a vertex such as position,
     * normal or texture coordinate
     */
    constructor(isStatic: Boolean, maxVertices: Int, maxIndices: Int, vararg attributes: VertexAttribute?) {
        vertices = makeVertexBuffer(isStatic, maxVertices, VertexAttributes(*attributes))
        indices = IndexBufferObject(isStatic, maxIndices)
        isVertexArray = false
        addManagedMesh(Gdx.app, this)
    }

    /**
     * Creates a new Mesh with the given attributes.
     *
     * @param isStatic    whether this mesh is static or not. Allows for internal optimizations.
     * @param maxVertices the maximum number of vertices this mesh can hold
     * @param maxIndices  the maximum number of indices this mesh can hold
     * @param attributes  the [VertexAttributes]. Each vertex attribute defines one property of a vertex such as position,
     * normal or texture coordinate
     */
    constructor(isStatic: Boolean, maxVertices: Int, maxIndices: Int, attributes: VertexAttributes) {
        vertices = makeVertexBuffer(isStatic, maxVertices, attributes)
        indices = IndexBufferObject(isStatic, maxIndices)
        isVertexArray = false
        addManagedMesh(Gdx.app, this)
    }

    /**
     * Creates a new Mesh with the given attributes. Adds extra optimizations for dynamic (frequently modified) meshes.
     *
     * @param staticVertices whether vertices of this mesh are static or not. Allows for internal optimizations.
     * @param staticIndices  whether indices of this mesh are static or not. Allows for internal optimizations.
     * @param maxVertices    the maximum number of vertices this mesh can hold
     * @param maxIndices     the maximum number of indices this mesh can hold
     * @param attributes     the [VertexAttributes]. Each vertex attribute defines one property of a vertex such as position,
     * normal or texture coordinate
     * @author Jaroslaw Wisniewski <j.wisniewski></j.wisniewski>@appsisle.com>
     */
    constructor(staticVertices: Boolean, staticIndices: Boolean, maxVertices: Int, maxIndices: Int, attributes: VertexAttributes) {
        vertices = makeVertexBuffer(staticVertices, maxVertices, attributes)
        indices = IndexBufferObject(staticIndices, maxIndices)
        isVertexArray = false
        addManagedMesh(Gdx.app, this)
    }

    private fun makeVertexBuffer(isStatic: Boolean, maxVertices: Int, vertexAttributes: VertexAttributes): VertexData {
        return if (Gdx.gl30 != null) {
            VertexBufferObjectWithVAO(isStatic, maxVertices, vertexAttributes)
        } else {
            VertexBufferObject(isStatic, maxVertices, vertexAttributes)
        }
    }

    /**
     * Creates a new Mesh with the given attributes. This is an expert method with no error checking. Use at your own risk.
     *
     * @param type        the [VertexDataType] to be used, VBO or VA.
     * @param isStatic    whether this mesh is static or not. Allows for internal optimizations.
     * @param maxVertices the maximum number of vertices this mesh can hold
     * @param maxIndices  the maximum number of indices this mesh can hold
     * @param attributes  the [VertexAttribute]s. Each vertex attribute defines one property of a vertex such as position,
     * normal or texture coordinate
     */
    constructor(type: VertexDataType?, isStatic: Boolean, maxVertices: Int, maxIndices: Int, vararg attributes: VertexAttribute?) : this(type, isStatic, maxVertices, maxIndices, VertexAttributes(*attributes)) {}

    /**
     * Creates a new Mesh with the given attributes. This is an expert method with no error checking. Use at your own risk.
     *
     * @param type        the [VertexDataType] to be used, VBO or VA.
     * @param isStatic    whether this mesh is static or not. Allows for internal optimizations.
     * @param maxVertices the maximum number of vertices this mesh can hold
     * @param maxIndices  the maximum number of indices this mesh can hold
     * @param attributes  the [VertexAttributes].
     */
    constructor(type: VertexDataType?, isStatic: Boolean, maxVertices: Int, maxIndices: Int, attributes: VertexAttributes?) {
        when (type) {
            VertexDataType.VertexBufferObject -> {
                vertices = VertexBufferObject(isStatic, maxVertices, attributes)
                indices = IndexBufferObject(isStatic, maxIndices)
                isVertexArray = false
            }
            VertexDataType.VertexBufferObjectSubData -> {
                vertices = VertexBufferObjectSubData(isStatic, maxVertices, attributes)
                indices = IndexBufferObjectSubData(isStatic, maxIndices)
                isVertexArray = false
            }
            VertexDataType.VertexBufferObjectWithVAO -> {
                vertices = VertexBufferObjectWithVAO(isStatic, maxVertices, attributes)
                indices = IndexBufferObjectSubData(isStatic, maxIndices)
                isVertexArray = false
            }
            VertexDataType.VertexArray -> {
                vertices = VertexArray(maxVertices, attributes)
                indices = IndexArray(maxIndices)
                isVertexArray = true
            }
            else -> {
                vertices = VertexArray(maxVertices, attributes)
                indices = IndexArray(maxIndices)
                isVertexArray = true
            }
        }
        addManagedMesh(Gdx.app, this)
    }

    fun enableInstancedRendering(isStatic: Boolean, maxInstances: Int, vararg attributes: VertexAttribute?): Mesh {
        if (!isInstanced) {
            isInstanced = true
            instances = InstanceBufferObject(isStatic, maxInstances, attributes)
        } else {
            throw GdxRuntimeException("Trying to enable InstancedRendering on same Mesh instance twice."
                + " Use disableInstancedRendering to clean up old InstanceData first")
        }
        return this
    }

    fun disableInstancedRendering(): Mesh {
        if (isInstanced) {
            isInstanced = false
            instances.dispose()
            instances = null
        }
        return this
    }

    /**
     * Sets the instance data of this Mesh. The attributes are assumed to be given in float format.
     *
     * @param instanceData the instance data.
     * @param offset       the offset into the vertices array
     * @param count        the number of floats to use
     * @return the mesh for invocation chaining.
     */
    fun setInstanceData(instanceData: FloatArray?, offset: Int, count: Int): Mesh {
        if (instances != null) {
            instances.setInstanceData(instanceData, offset, count)
        } else {
            throw GdxRuntimeException("An InstanceBufferObject must be set before setting instance data!")
        }
        return this
    }

    /**
     * Sets the instance data of this Mesh. The attributes are assumed to be given in float format.
     *
     * @param instanceData the instance data.
     * @return the mesh for invocation chaining.
     */
    fun setInstanceData(instanceData: FloatArray): Mesh {
        if (instances != null) {
            instances.setInstanceData(instanceData, 0, instanceData.size)
        } else {
            throw GdxRuntimeException("An InstanceBufferObject must be set before setting instance data!")
        }
        return this
    }

    /**
     * Sets the instance data of this Mesh. The attributes are assumed to be given in float format.
     *
     * @param instanceData the instance data.
     * @param count        the number of floats to use
     * @return the mesh for invocation chaining.
     */
    fun setInstanceData(instanceData: FloatBuffer?, count: Int): Mesh {
        if (instances != null) {
            instances.setInstanceData(instanceData, count)
        } else {
            throw GdxRuntimeException("An InstanceBufferObject must be set before setting instance data!")
        }
        return this
    }

    /**
     * Sets the instance data of this Mesh. The attributes are assumed to be given in float format.
     *
     * @param instanceData the instance data.
     * @return the mesh for invocation chaining.
     */
    fun setInstanceData(instanceData: FloatBuffer): Mesh {
        if (instances != null) {
            instances.setInstanceData(instanceData, instanceData.limit())
        } else {
            throw GdxRuntimeException("An InstanceBufferObject must be set before setting instance data!")
        }
        return this
    }
    /**
     * Update (a portion of) the instance data. Does not resize the backing buffer.
     *
     * @param targetOffset the offset in number of floats of the mesh part.
     * @param source       the instance data to update the mesh part with
     * @param sourceOffset the offset in number of floats within the source array
     * @param count        the number of floats to update
     */
    /**
     * Update (a portion of) the instance data. Does not resize the backing buffer.
     *
     * @param targetOffset the offset in number of floats of the mesh part.
     * @param source       the instance data to update the mesh part with
     */
    @JvmOverloads
    fun updateInstanceData(targetOffset: Int, source: FloatArray, sourceOffset: Int = 0, count: Int = source.size): Mesh {
        instances.updateInstanceData(targetOffset, source, sourceOffset, count)
        return this
    }

    /**
     * Update (a portion of) the instance data. Does not resize the backing buffer.
     *
     * @param targetOffset the offset in number of floats of the mesh part.
     * @param source       the instance data to update the mesh part with
     */
    fun updateInstanceData(targetOffset: Int, source: FloatBuffer): Mesh {
        return updateInstanceData(targetOffset, source, 0, source.limit())
    }

    /**
     * Update (a portion of) the instance data. Does not resize the backing buffer.
     *
     * @param targetOffset the offset in number of floats of the mesh part.
     * @param source       the instance data to update the mesh part with
     * @param sourceOffset the offset in number of floats within the source array
     * @param count        the number of floats to update
     */
    fun updateInstanceData(targetOffset: Int, source: FloatBuffer?, sourceOffset: Int, count: Int): Mesh {
        instances.updateInstanceData(targetOffset, source, sourceOffset, count)
        return this
    }

    /**
     * Sets the vertices of this Mesh. The attributes are assumed to be given in float format.
     *
     * @param vertices the vertices.
     * @return the mesh for invocation chaining.
     */
    fun setVertices(vertices: FloatArray): Mesh {
        this.vertices.setVertices(vertices, 0, vertices.size)
        return this
    }

    /**
     * Sets the vertices of this Mesh. The attributes are assumed to be given in float format.
     *
     * @param vertices the vertices.
     * @param offset   the offset into the vertices array
     * @param count    the number of floats to use
     * @return the mesh for invocation chaining.
     */
    fun setVertices(vertices: FloatArray?, offset: Int, count: Int): Mesh {
        this.vertices.setVertices(vertices, offset, count)
        return this
    }
    /**
     * Update (a portion of) the vertices. Does not resize the backing buffer.
     *
     * @param targetOffset the offset in number of floats of the mesh part.
     * @param source       the vertex data to update the mesh part with
     * @param sourceOffset the offset in number of floats within the source array
     * @param count        the number of floats to update
     */
    /**
     * Update (a portion of) the vertices. Does not resize the backing buffer.
     *
     * @param targetOffset the offset in number of floats of the mesh part.
     * @param source       the vertex data to update the mesh part with
     */
    @JvmOverloads
    fun updateVertices(targetOffset: Int, source: FloatArray, sourceOffset: Int = 0, count: Int = source.size): Mesh {
        vertices.updateVertices(targetOffset, source, sourceOffset, count)
        return this
    }

    /**
     * Copies the vertices from the Mesh to the float array. The float array must be large enough to hold all the Mesh's vertices.
     *
     * @param vertices the array to copy the vertices to
     */
    fun getVertices(vertices: FloatArray): FloatArray {
        return getVertices(0, -1, vertices)
    }

    /**
     * Copies the the remaining vertices from the Mesh to the float array. The float array must be large enough to hold the
     * remaining vertices.
     *
     * @param srcOffset the offset (in number of floats) of the vertices in the mesh to copy
     * @param vertices  the array to copy the vertices to
     */
    fun getVertices(srcOffset: Int, vertices: FloatArray): FloatArray {
        return getVertices(srcOffset, -1, vertices)
    }

    /**
     * Copies the specified vertices from the Mesh to the float array. The float array must be large enough to hold count vertices.
     *
     * @param srcOffset the offset (in number of floats) of the vertices in the mesh to copy
     * @param count     the amount of floats to copy
     * @param vertices  the array to copy the vertices to
     */
    fun getVertices(srcOffset: Int, count: Int, vertices: FloatArray): FloatArray {
        return getVertices(srcOffset, count, vertices, 0)
    }

    /**
     * Copies the specified vertices from the Mesh to the float array. The float array must be large enough to hold
     * destOffset+count vertices.
     *
     * @param srcOffset  the offset (in number of floats) of the vertices in the mesh to copy
     * @param count      the amount of floats to copy
     * @param vertices   the array to copy the vertices to
     * @param destOffset the offset (in floats) in the vertices array to start copying
     */
    fun getVertices(srcOffset: Int, count: Int, vertices: FloatArray, destOffset: Int): FloatArray {
        // TODO: Perhaps this method should be vertexSize aware??
        var count = count
        val max = numVertices * vertexSize / 4
        if (count == -1) {
            count = max - srcOffset
            if (count > vertices.size - destOffset) count = vertices.size - destOffset
        }
        if (srcOffset < 0 || count <= 0 || srcOffset + count > max || destOffset < 0 || destOffset >= vertices.size) throw IndexOutOfBoundsException()
        if (vertices.size - destOffset < count) throw java.lang.IllegalArgumentException("not enough room in vertices array, has " + vertices.size + " floats, needs "
            + count)
        val pos: Int = verticesBuffer.position()
        verticesBuffer.position(srcOffset)
        verticesBuffer.get(vertices, destOffset, count)
        verticesBuffer.position(pos)
        return vertices
    }

    /**
     * Sets the indices of this Mesh
     *
     * @param indices the indices
     * @return the mesh for invocation chaining.
     */
    fun setIndices(indices: ShortArray): Mesh {
        this.indices.setIndices(indices, 0, indices.size)
        return this
    }

    /**
     * Sets the indices of this Mesh.
     *
     * @param indices the indices
     * @param offset  the offset into the indices array
     * @param count   the number of indices to copy
     * @return the mesh for invocation chaining.
     */
    fun setIndices(indices: ShortArray?, offset: Int, count: Int): Mesh {
        this.indices.setIndices(indices, offset, count)
        return this
    }

    /**
     * Copies the indices from the Mesh to the short array. The short array must be large enough to hold all the Mesh's indices.
     *
     * @param indices the array to copy the indices to
     */
    fun getIndices(indices: ShortArray) {
        getIndices(indices, 0)
    }

    /**
     * Copies the indices from the Mesh to the short array. The short array must be large enough to hold destOffset + all the
     * Mesh's indices.
     *
     * @param indices    the array to copy the indices to
     * @param destOffset the offset in the indices array to start copying
     */
    fun getIndices(indices: ShortArray, destOffset: Int) {
        getIndices(0, indices, destOffset)
    }

    /**
     * Copies the remaining indices from the Mesh to the short array. The short array must be large enough to hold destOffset + all
     * the remaining indices.
     *
     * @param srcOffset  the zero-based offset of the first index to fetch
     * @param indices    the array to copy the indices to
     * @param destOffset the offset in the indices array to start copying
     */
    fun getIndices(srcOffset: Int, indices: ShortArray, destOffset: Int) {
        getIndices(srcOffset, -1, indices, destOffset)
    }

    /**
     * Copies the indices from the Mesh to the short array. The short array must be large enough to hold destOffset + count
     * indices.
     *
     * @param srcOffset  the zero-based offset of the first index to fetch
     * @param count      the total amount of indices to copy
     * @param indices    the array to copy the indices to
     * @param destOffset the offset in the indices array to start copying
     */
    fun getIndices(srcOffset: Int, count: Int, indices: ShortArray, destOffset: Int) {
        var count = count
        val max = numIndices
        if (count < 0) count = max - srcOffset
        if (srcOffset < 0 || srcOffset >= max || srcOffset + count > max) throw java.lang.IllegalArgumentException("Invalid range specified, offset: " + srcOffset + ", count: " + count + ", max: "
            + max)
        if (indices.size - destOffset < count) throw java.lang.IllegalArgumentException("not enough room in indices array, has " + indices.size + " shorts, needs " + count)
        val pos: Int = indicesBuffer.position()
        indicesBuffer.position(srcOffset)
        indicesBuffer.get(indices, destOffset, count)
        indicesBuffer.position(pos)
    }

    /**
     * @return the number of defined indices
     */
    val numIndices: Int
        get() = indices.getNumIndices()

    /**
     * @return the number of defined vertices
     */
    val numVertices: Int
        get() = vertices.getNumVertices()

    /**
     * @return the maximum number of vertices this mesh can hold
     */
    val maxVertices: Int
        get() = vertices.getNumMaxVertices()

    /**
     * @return the maximum number of indices this mesh can hold
     */
    val maxIndices: Int
        get() = indices.getNumMaxIndices()

    /**
     * @return the size of a single vertex in bytes
     */
    val vertexSize: Int
        get() = vertices.getAttributes().vertexSize

    /**
     * Sets whether to bind the underlying [VertexArray] or [VertexBufferObject] automatically on a call to one of the
     * render methods. Usually you want to use autobind. Manual binding is an expert functionality. There is a driver bug on the
     * MSM720xa chips that will fuck up memory if you manipulate the vertices and indices of a Mesh multiple times while it is
     * bound. Keep this in mind.
     *
     * @param autoBind whether to autobind meshes.
     */
    fun setAutoBind(autoBind: Boolean) {
        this.autoBind = autoBind
    }

    /**
     * Binds the underlying [VertexBufferObject] and [IndexBufferObject] if indices where given. Use this with OpenGL
     * ES 2.0 and when auto-bind is disabled.
     *
     * @param shader the shader (does not bind the shader)
     */
    fun bind(shader: ShaderProgram?) {
        bind(shader, null)
    }

    /**
     * Binds the underlying [VertexBufferObject] and [IndexBufferObject] if indices where given. Use this with OpenGL
     * ES 2.0 and when auto-bind is disabled.
     *
     * @param shader    the shader (does not bind the shader)
     * @param locations array containing the attribute locations.
     */
    fun bind(shader: ShaderProgram?, locations: IntArray?) {
        vertices.bind(shader, locations)
        if (instances != null && instances.getNumInstances() > 0) instances.bind(shader, locations)
        if (indices.getNumIndices() > 0) indices.bind()
    }

    /**
     * Unbinds the underlying [VertexBufferObject] and [IndexBufferObject] is indices were given. Use this with OpenGL
     * ES 1.x and when auto-bind is disabled.
     *
     * @param shader the shader (does not unbind the shader)
     */
    fun unbind(shader: ShaderProgram?) {
        unbind(shader, null)
    }

    /**
     * Unbinds the underlying [VertexBufferObject] and [IndexBufferObject] is indices were given. Use this with OpenGL
     * ES 1.x and when auto-bind is disabled.
     *
     * @param shader    the shader (does not unbind the shader)
     * @param locations array containing the attribute locations.
     */
    fun unbind(shader: ShaderProgram?, locations: IntArray?) {
        vertices.unbind(shader, locations)
        if (instances != null && instances.getNumInstances() > 0) instances.unbind(shader, locations)
        if (indices.getNumIndices() > 0) indices.unbind()
    }

    /**
     *
     *
     * Renders the mesh using the given primitive type. If indices are set for this mesh then getNumIndices() / #vertices per
     * primitive primitives are rendered. If no indices are set then getNumVertices() / #vertices per primitive are rendered.
     *
     *
     *
     *
     * This method will automatically bind each vertex attribute as specified at construction time via [VertexAttributes] to
     * the respective shader attributes. The binding is based on the alias defined for each VertexAttribute.
     *
     *
     *
     *
     * This method must only be called after the [ShaderProgram.begin] method has been called!
     *
     *
     *
     *
     * This method is intended for use with OpenGL ES 2.0 and will throw an IllegalStateException when OpenGL ES 1.x is used.
     *
     *
     * @param primitiveType the primitive type
     */
    fun render(shader: ShaderProgram?, primitiveType: Int) {
        render(shader, primitiveType, 0, if (indices.getNumMaxIndices() > 0) numIndices else numVertices, autoBind)
    }

    /**
     *
     *
     * Renders the mesh using the given primitive type. offset specifies the offset into either the vertex buffer or the index
     * buffer depending on whether indices are defined. count specifies the number of vertices or indices to use thus count /
     * #vertices per primitive primitives are rendered.
     *
     *
     *
     *
     * This method will automatically bind each vertex attribute as specified at construction time via [VertexAttributes] to
     * the respective shader attributes. The binding is based on the alias defined for each VertexAttribute.
     *
     *
     *
     *
     * This method must only be called after the [ShaderProgram.begin] method has been called!
     *
     *
     *
     *
     * This method is intended for use with OpenGL ES 2.0 and will throw an IllegalStateException when OpenGL ES 1.x is used.
     *
     *
     * @param shader        the shader to be used
     * @param primitiveType the primitive type
     * @param offset        the offset into the vertex or index buffer
     * @param count         number of vertices or indices to use
     */
    fun render(shader: ShaderProgram?, primitiveType: Int, offset: Int, count: Int) {
        render(shader, primitiveType, offset, count, autoBind)
    }

    /**
     *
     *
     * Renders the mesh using the given primitive type. offset specifies the offset into either the vertex buffer or the index
     * buffer depending on whether indices are defined. count specifies the number of vertices or indices to use thus count /
     * #vertices per primitive primitives are rendered.
     *
     *
     *
     *
     * This method will automatically bind each vertex attribute as specified at construction time via [VertexAttributes] to
     * the respective shader attributes. The binding is based on the alias defined for each VertexAttribute.
     *
     *
     *
     *
     * This method must only be called after the [ShaderProgram.begin] method has been called!
     *
     *
     *
     *
     * This method is intended for use with OpenGL ES 2.0 and will throw an IllegalStateException when OpenGL ES 1.x is used.
     *
     *
     * @param shader        the shader to be used
     * @param primitiveType the primitive type
     * @param offset        the offset into the vertex or index buffer
     * @param count         number of vertices or indices to use
     * @param autoBind      overrides the autoBind member of this Mesh
     */
    fun render(shader: ShaderProgram?, primitiveType: Int, offset: Int, count: Int, autoBind: Boolean) {
        if (count == 0) return
        if (autoBind) bind(shader)
        if (isVertexArray) {
            if (indices.getNumIndices() > 0) {
                val buffer: ShortBuffer = indices.getBuffer()
                val oldPosition: Int = buffer.position()
                val oldLimit: Int = buffer.limit()
                buffer.position(offset)
                buffer.limit(offset + count)
                Gdx.gl20.glDrawElements(primitiveType, count, GL20.GL_UNSIGNED_SHORT, buffer)
                buffer.position(oldPosition)
                buffer.limit(oldLimit)
            } else {
                Gdx.gl20.glDrawArrays(primitiveType, offset, count)
            }
        } else {
            var numInstances = 0
            if (isInstanced) numInstances = instances.getNumInstances()
            if (indices.getNumIndices() > 0) {
                if (count + offset > indices.getNumMaxIndices()) {
                    throw GdxRuntimeException("Mesh attempting to access memory outside of the index buffer (count: "
                        + count + ", offset: " + offset + ", max: " + indices.getNumMaxIndices() + ")")
                }
                if (isInstanced && numInstances > 0) {
                    Gdx.gl30.glDrawElementsInstanced(primitiveType, count, GL20.GL_UNSIGNED_SHORT, offset * 2, numInstances)
                } else {
                    Gdx.gl20.glDrawElements(primitiveType, count, GL20.GL_UNSIGNED_SHORT, offset * 2)
                }
            } else {
                if (isInstanced && numInstances > 0) {
                    Gdx.gl30.glDrawArraysInstanced(primitiveType, offset, count, numInstances)
                } else {
                    Gdx.gl20.glDrawArrays(primitiveType, offset, count)
                }
            }
        }
        if (autoBind) unbind(shader)
    }

    /**
     * Frees all resources associated with this Mesh
     */
    fun dispose() {
        if (meshes[Gdx.app] != null) meshes[Gdx.app].removeValue(this, true)
        vertices.dispose()
        if (instances != null) instances.dispose()
        indices.dispose()
    }

    /**
     * Returns the first [VertexAttribute] having the given [Usage].
     *
     * @param usage the Usage.
     * @return the VertexAttribute or null if no attribute with that usage was found.
     */
    fun getVertexAttribute(usage: Int): VertexAttribute? {
        val attributes: VertexAttributes = vertices.getAttributes()
        val len = attributes.size()
        for (i in 0 until len) if (attributes[i]!!.usage == usage) return attributes[i]
        return null
    }

    /**
     * @return the vertex attributes of this Mesh
     */
    val vertexAttributes: VertexAttributes
        get() = vertices.getAttributes()

    /**
     * @return the backing FloatBuffer holding the vertices. Does not have to be a direct buffer on Android!
     */
    val verticesBuffer: FloatBuffer
        get() = vertices.getBuffer()

    /**
     * Calculates the [BoundingBox] of the vertices contained in this mesh. In case no vertices are defined yet a
     * [GdxRuntimeException] is thrown. This method creates a new BoundingBox instance.
     *
     * @return the bounding box.
     */
    fun calculateBoundingBox(): BoundingBox {
        val bbox = BoundingBox()
        calculateBoundingBox(bbox)
        return bbox
    }

    /**
     * Calculates the [BoundingBox] of the vertices contained in this mesh. In case no vertices are defined yet a
     * [GdxRuntimeException] is thrown.
     *
     * @param bbox the bounding box to store the result in.
     */
    fun calculateBoundingBox(bbox: BoundingBox) {
        val numVertices = numVertices
        if (numVertices == 0) throw GdxRuntimeException("No vertices defined")
        val verts: FloatBuffer = vertices.getBuffer()
        bbox.inf()
        val posAttrib = getVertexAttribute(Usage.Position)
        val offset = posAttrib!!.offset / 4
        val vertexSize: Int = vertices.getAttributes().vertexSize / 4
        var idx = offset
        when (posAttrib.numComponents) {
            1 -> {
                var i = 0
                while (i < numVertices) {
                    bbox.ext(verts.get(idx), 0, 0)
                    idx += vertexSize
                    i++
                }
            }
            2 -> {
                var i = 0
                while (i < numVertices) {
                    bbox.ext(verts.get(idx), verts.get(idx + 1), 0)
                    idx += vertexSize
                    i++
                }
            }
            3 -> {
                var i = 0
                while (i < numVertices) {
                    bbox.ext(verts.get(idx), verts.get(idx + 1), verts.get(idx + 2))
                    idx += vertexSize
                    i++
                }
            }
        }
    }

    /**
     * Calculate the [BoundingBox] of the specified part.
     *
     * @param out    the bounding box to store the result in.
     * @param offset the start index of the part.
     * @param count  the amount of indices the part contains.
     * @return the value specified by out.
     */
    fun calculateBoundingBox(out: BoundingBox, offset: Int, count: Int): BoundingBox {
        return extendBoundingBox(out.inf(), offset, count)
    }

    /**
     * Calculate the [BoundingBox] of the specified part.
     *
     * @param out    the bounding box to store the result in.
     * @param offset the start index of the part.
     * @param count  the amount of indices the part contains.
     * @return the value specified by out.
     */
    fun calculateBoundingBox(out: BoundingBox, offset: Int, count: Int, transform: Matrix4?): BoundingBox {
        return extendBoundingBox(out.inf(), offset, count, transform)
    }

    /**
     * Extends the specified [BoundingBox] with the specified part.
     *
     * @param out    the bounding box to store the result in.
     * @param offset the start index of the part.
     * @param count  the amount of indices the part contains.
     * @return the value specified by out.
     */
    fun extendBoundingBox(out: BoundingBox, offset: Int, count: Int): BoundingBox {
        return extendBoundingBox(out, offset, count, null)
    }

    private val tmpV: Vector3 = Vector3()

    /**
     * Extends the specified [BoundingBox] with the specified part.
     *
     * @param out    the bounding box to store the result in.
     * @param offset the start of the part.
     * @param count  the size of the part.
     * @return the value specified by out.
     */
    fun extendBoundingBox(out: BoundingBox, offset: Int, count: Int, transform: Matrix4?): BoundingBox {
        val numIndices = numIndices
        val numVertices = numVertices
        val max = if (numIndices == 0) numVertices else numIndices
        if (offset < 0 || count < 1 || offset + count > max) throw GdxRuntimeException("Invalid part specified ( offset=$offset, count=$count, max=$max )")
        val verts: FloatBuffer = vertices.getBuffer()
        val index: ShortBuffer = indices.getBuffer()
        val posAttrib = getVertexAttribute(Usage.Position)
        val posoff = posAttrib!!.offset / 4
        val vertexSize: Int = vertices.getAttributes().vertexSize / 4
        val end = offset + count
        when (posAttrib.numComponents) {
            1 -> if (numIndices > 0) {
                var i = offset
                while (i < end) {
                    val idx: Int = index.get(i) * vertexSize + posoff
                    tmpV.set(verts.get(idx), 0, 0)
                    if (transform != null) tmpV.mul(transform)
                    out.ext(tmpV)
                    i++
                }
            } else {
                var i = offset
                while (i < end) {
                    val idx = i * vertexSize + posoff
                    tmpV.set(verts.get(idx), 0, 0)
                    if (transform != null) tmpV.mul(transform)
                    out.ext(tmpV)
                    i++
                }
            }
            2 -> if (numIndices > 0) {
                var i = offset
                while (i < end) {
                    val idx: Int = index.get(i) * vertexSize + posoff
                    tmpV.set(verts.get(idx), verts.get(idx + 1), 0)
                    if (transform != null) tmpV.mul(transform)
                    out.ext(tmpV)
                    i++
                }
            } else {
                var i = offset
                while (i < end) {
                    val idx = i * vertexSize + posoff
                    tmpV.set(verts.get(idx), verts.get(idx + 1), 0)
                    if (transform != null) tmpV.mul(transform)
                    out.ext(tmpV)
                    i++
                }
            }
            3 -> if (numIndices > 0) {
                var i = offset
                while (i < end) {
                    val idx: Int = index.get(i) * vertexSize + posoff
                    tmpV.set(verts.get(idx), verts.get(idx + 1), verts.get(idx + 2))
                    if (transform != null) tmpV.mul(transform)
                    out.ext(tmpV)
                    i++
                }
            } else {
                var i = offset
                while (i < end) {
                    val idx = i * vertexSize + posoff
                    tmpV.set(verts.get(idx), verts.get(idx + 1), verts.get(idx + 2))
                    if (transform != null) tmpV.mul(transform)
                    out.ext(tmpV)
                    i++
                }
            }
        }
        return out
    }

    /**
     * Calculates the squared radius of the bounding sphere around the specified center for the specified part.
     *
     * @param centerX The X coordinate of the center of the bounding sphere
     * @param centerY The Y coordinate of the center of the bounding sphere
     * @param centerZ The Z coordinate of the center of the bounding sphere
     * @param offset  the start index of the part.
     * @param count   the amount of indices the part contains.
     * @return the squared radius of the bounding sphere.
     */
    fun calculateRadiusSquared(centerX: Float, centerY: Float, centerZ: Float, offset: Int, count: Int,
                               transform: Matrix4?): Float {
        val numIndices = numIndices
        if (offset < 0 || count < 1 || offset + count > numIndices) throw GdxRuntimeException("Not enough indices")
        val verts: FloatBuffer = vertices.getBuffer()
        val index: ShortBuffer = indices.getBuffer()
        val posAttrib = getVertexAttribute(Usage.Position)
        val posoff = posAttrib!!.offset / 4
        val vertexSize: Int = vertices.getAttributes().vertexSize / 4
        val end = offset + count
        var result = 0f
        when (posAttrib.numComponents) {
            1 -> {
                var i = offset
                while (i < end) {
                    val idx: Int = index.get(i) * vertexSize + posoff
                    tmpV.set(verts.get(idx), 0, 0)
                    if (transform != null) tmpV.mul(transform)
                    val r: Float = tmpV.sub(centerX, centerY, centerZ).len2()
                    if (r > result) result = r
                    i++
                }
            }
            2 -> {
                var i = offset
                while (i < end) {
                    val idx: Int = index.get(i) * vertexSize + posoff
                    tmpV.set(verts.get(idx), verts.get(idx + 1), 0)
                    if (transform != null) tmpV.mul(transform)
                    val r: Float = tmpV.sub(centerX, centerY, centerZ).len2()
                    if (r > result) result = r
                    i++
                }
            }
            3 -> {
                var i = offset
                while (i < end) {
                    val idx: Int = index.get(i) * vertexSize + posoff
                    tmpV.set(verts.get(idx), verts.get(idx + 1), verts.get(idx + 2))
                    if (transform != null) tmpV.mul(transform)
                    val r: Float = tmpV.sub(centerX, centerY, centerZ).len2()
                    if (r > result) result = r
                    i++
                }
            }
        }
        return result
    }
    /**
     * Calculates the radius of the bounding sphere around the specified center for the specified part.
     *
     * @param centerX The X coordinate of the center of the bounding sphere
     * @param centerY The Y coordinate of the center of the bounding sphere
     * @param centerZ The Z coordinate of the center of the bounding sphere
     * @param offset  the start index of the part.
     * @param count   the amount of indices the part contains.
     * @return the radius of the bounding sphere.
     */
    /**
     * Calculates the squared radius of the bounding sphere around the specified center for the specified part.
     *
     * @param centerX The X coordinate of the center of the bounding sphere
     * @param centerY The Y coordinate of the center of the bounding sphere
     * @param centerZ The Z coordinate of the center of the bounding sphere
     * @return the squared radius of the bounding sphere.
     */
    /**
     * Calculates the squared radius of the bounding sphere around the specified center for the specified part.
     *
     * @param centerX The X coordinate of the center of the bounding sphere
     * @param centerY The Y coordinate of the center of the bounding sphere
     * @param centerZ The Z coordinate of the center of the bounding sphere
     * @param offset  the start index of the part.
     * @param count   the amount of indices the part contains.
     * @return the squared radius of the bounding sphere.
     */
    @JvmOverloads
    fun calculateRadius(centerX: Float, centerY: Float, centerZ: Float, offset: Int = 0, count: Int = numIndices,
                        transform: Matrix4? = null): Float {
        return java.lang.Math.sqrt(calculateRadiusSquared(centerX, centerY, centerZ, offset, count, transform).toDouble())
    }

    /**
     * Calculates the squared radius of the bounding sphere around the specified center for the specified part.
     *
     * @param center The center of the bounding sphere
     * @param offset the start index of the part.
     * @param count  the amount of indices the part contains.
     * @return the squared radius of the bounding sphere.
     */
    fun calculateRadius(center: Vector3, offset: Int, count: Int, transform: Matrix4?): Float {
        return calculateRadius(center.x, center.y, center.z, offset, count, transform)
    }

    /**
     * Calculates the squared radius of the bounding sphere around the specified center for the specified part.
     *
     * @param center The center of the bounding sphere
     * @param offset the start index of the part.
     * @param count  the amount of indices the part contains.
     * @return the squared radius of the bounding sphere.
     */
    fun calculateRadius(center: Vector3, offset: Int, count: Int): Float {
        return calculateRadius(center.x, center.y, center.z, offset, count, null)
    }

    /**
     * Calculates the squared radius of the bounding sphere around the specified center for the specified part.
     *
     * @param center The center of the bounding sphere
     * @return the squared radius of the bounding sphere.
     */
    fun calculateRadius(center: Vector3): Float {
        return calculateRadius(center.x, center.y, center.z, 0, numIndices, null)
    }

    /**
     * @return the backing shortbuffer holding the indices. Does not have to be a direct buffer on Android!
     */
    val indicesBuffer: ShortBuffer
        get() = indices.getBuffer()

    /**
     * Method to scale the positions in the mesh. Normals will be kept as is. This is a potentially slow operation, use with care.
     * It will also create a temporary float[] which will be garbage collected.
     *
     * @param scaleX scale on x
     * @param scaleY scale on y
     * @param scaleZ scale on z
     */
    fun scale(scaleX: Float, scaleY: Float, scaleZ: Float) {
        val posAttr = getVertexAttribute(Usage.Position)
        val offset = posAttr!!.offset / 4
        val numComponents = posAttr.numComponents
        val numVertices = numVertices
        val vertexSize = vertexSize / 4
        val vertices = FloatArray(numVertices * vertexSize)
        getVertices(vertices)
        var idx = offset
        when (numComponents) {
            1 -> {
                var i = 0
                while (i < numVertices) {
                    vertices[idx] *= scaleX
                    idx += vertexSize
                    i++
                }
            }
            2 -> {
                var i = 0
                while (i < numVertices) {
                    vertices[idx] *= scaleX
                    vertices[idx + 1] *= scaleY
                    idx += vertexSize
                    i++
                }
            }
            3 -> {
                var i = 0
                while (i < numVertices) {
                    vertices[idx] *= scaleX
                    vertices[idx + 1] *= scaleY
                    vertices[idx + 2] *= scaleZ
                    idx += vertexSize
                    i++
                }
            }
        }
        setVertices(vertices)
    }

    /**
     * Method to transform the positions in the mesh. Normals will be kept as is. This is a potentially slow operation, use with
     * care. It will also create a temporary float[] which will be garbage collected.
     *
     * @param matrix the transformation matrix
     */
    fun transform(matrix: Matrix4?) {
        transform(matrix, 0, numVertices)
    }

    // TODO: Protected for now, because transforming a portion works but still copies all vertices
    fun transform(matrix: Matrix4?, start: Int, count: Int) {
        val posAttr = getVertexAttribute(Usage.Position)
        val posOffset = posAttr!!.offset / 4
        val stride = vertexSize / 4
        val numComponents = posAttr.numComponents
        val numVertices = numVertices
        val vertices = FloatArray(count * stride)
        getVertices(start * stride, count * stride, vertices)
        // getVertices(0, vertices.length, vertices);
        transform(matrix, vertices, stride, posOffset, numComponents, 0, count)
        // setVertices(vertices, 0, vertices.length);
        updateVertices(start * stride, vertices)
    }

    /**
     * Method to transform the texture coordinates in the mesh. This is a potentially slow operation, use with care. It will also
     * create a temporary float[] which will be garbage collected.
     *
     * @param matrix the transformation matrix
     */
    fun transformUV(matrix: Matrix3?) {
        transformUV(matrix, 0, numVertices)
    }

    // TODO: Protected for now, because transforming a portion works but still copies all vertices
    protected fun transformUV(matrix: Matrix3?, start: Int, count: Int) {
        val posAttr = getVertexAttribute(Usage.TextureCoordinates)
        val offset = posAttr!!.offset / 4
        val vertexSize = vertexSize / 4
        val numVertices = numVertices
        val vertices = FloatArray(numVertices * vertexSize)
        // TODO: getVertices(vertices, start * vertexSize, count * vertexSize);
        getVertices(0, vertices.size, vertices)
        transformUV(matrix, vertices, vertexSize, offset, start, count)
        setVertices(vertices, 0, vertices.size)
        // TODO: setVertices(start * vertexSize, vertices, 0, vertices.length);
    }
    /**
     * Copies this mesh optionally removing duplicate vertices and/or reducing the amount of attributes.
     *
     * @param isStatic         whether the new mesh is static or not. Allows for internal optimizations.
     * @param removeDuplicates whether to remove duplicate vertices if possible. Only the vertices specified by usage are checked.
     * @param usage            which attributes (if available) to copy
     * @return the copy of this mesh
     */
    /**
     * Copies this mesh.
     *
     * @param isStatic whether the new mesh is static or not. Allows for internal optimizations.
     * @return the copy of this mesh
     */
    @JvmOverloads
    fun copy(isStatic: Boolean, removeDuplicates: Boolean = false, usage: IntArray? = null): Mesh {
        // TODO move this to a copy constructor?
        // TODO duplicate the buffers without double copying the data if possible.
        // TODO perhaps move this code to JNI if it turns out being too slow.
        val vertexSize = vertexSize / 4
        var numVertices = numVertices
        var vertices = FloatArray(numVertices * vertexSize)
        getVertices(0, vertices.size, vertices)
        var checks: ShortArray? = null
        var attrs: Array<VertexAttribute?>? = null
        var newVertexSize = 0
        if (usage != null) {
            var size = 0
            var `as` = 0
            for (i in usage.indices) if (getVertexAttribute(usage[i]) != null) {
                size += getVertexAttribute(usage[i])!!.numComponents
                `as`++
            }
            if (size > 0) {
                attrs = arrayOfNulls(`as`)
                checks = ShortArray(size)
                var idx = -1
                var ai = -1
                for (i in usage.indices) {
                    val a = getVertexAttribute(usage[i]) ?: continue
                    for (j in 0 until a.numComponents) checks[++idx] = (a.offset + j).toShort()
                    attrs[++ai] = a.copy()
                    newVertexSize += a.numComponents
                }
            }
        }
        if (checks == null) {
            checks = ShortArray(vertexSize)
            for (i in 0 until vertexSize) checks[i] = i.toShort()
            newVertexSize = vertexSize
        }
        val numIndices = numIndices
        var indices: ShortArray? = null
        if (numIndices > 0) {
            indices = ShortArray(numIndices)
            getIndices(indices)
            if (removeDuplicates || newVertexSize != vertexSize) {
                val tmp = FloatArray(vertices.size)
                var size = 0
                for (i in 0 until numIndices) {
                    val idx1 = indices[i] * vertexSize
                    var newIndex: Short = -1
                    if (removeDuplicates) {
                        var j: Short = 0
                        while (j < size && newIndex < 0) {
                            val idx2 = j * newVertexSize
                            var found = true
                            var k = 0
                            while (k < checks.size && found) {
                                if (tmp[idx2 + k] != vertices[idx1 + checks[k]]) found = false
                                k++
                            }
                            if (found) newIndex = j
                            j++
                        }
                    }
                    if (newIndex > 0) indices[i] = newIndex else {
                        val idx = size * newVertexSize
                        for (j in checks.indices) tmp[idx + j] = vertices[idx1 + checks[j]]
                        indices[i] = size.toShort()
                        size++
                    }
                }
                vertices = tmp
                numVertices = size
            }
        }
        val result: Mesh
        result = if (attrs == null) Mesh(isStatic, numVertices, indices?.size
            ?: 0, vertexAttributes) else Mesh(isStatic, numVertices, indices?.size ?: 0, *attrs)
        result.setVertices(vertices, 0, numVertices * newVertexSize)
        if (indices != null) result.setIndices(indices)
        return result
    }

    companion object {
        /**
         * list of all meshes
         */
        val meshes: Map<Application, Array<Mesh>?> = HashMap<Application, Array<Mesh>?>()
        private fun addManagedMesh(app: Application, mesh: Mesh) {
            var managedResources = meshes[app]
            if (managedResources == null) managedResources = Array()
            managedResources.add(mesh)
            meshes.put(app, managedResources)
        }

        /**
         * Invalidates all meshes so the next time they are rendered new VBO handles are generated.
         *
         * @param app
         */
        fun invalidateAllMeshes(app: Application) {
            val meshesArray = meshes[app] ?: return
            for (i in 0 until meshesArray.size) {
                meshesArray[i].vertices.invalidate()
                meshesArray[i].indices.invalidate()
            }
        }

        /**
         * Will clear the managed mesh cache. I wouldn't use this if i was you :)
         */
        fun clearAllMeshes(app: Application?) {
            meshes.remove(app)
        }

        val managedStatus: String
            get() {
                val builder: java.lang.StringBuilder = java.lang.StringBuilder()
                val i = 0
                builder.append("Managed meshes/app: { ")
                for (app in meshes.keySet()) {
                    builder.append(meshes[app]!!.size)
                    builder.append(" ")
                }
                builder.append("}")
                return builder.toString()
            }

        /**
         * Method to transform the positions in the float array. Normals will be kept as is. This is a potentially slow operation, use
         * with care.
         *
         * @param matrix     the transformation matrix
         * @param vertices   the float array
         * @param vertexSize the number of floats in each vertex
         * @param offset     the offset within a vertex to the position
         * @param dimensions the size of the position
         * @param start      the vertex to start with
         * @param count      the amount of vertices to transform
         */
        fun transform(matrix: Matrix4?, vertices: FloatArray, vertexSize: Int, offset: Int, dimensions: Int,
                      start: Int, count: Int) {
            if (offset < 0 || dimensions < 1 || offset + dimensions > vertexSize) throw IndexOutOfBoundsException()
            if (start < 0 || count < 1 || (start + count) * vertexSize > vertices.size) throw IndexOutOfBoundsException("start = " + start + ", count = " + count + ", vertexSize = " + vertexSize
                + ", length = " + vertices.size)
            val tmp = Vector3()
            var idx = offset + start * vertexSize
            when (dimensions) {
                1 -> {
                    var i = 0
                    while (i < count) {
                        tmp.set(vertices[idx], 0, 0).mul(matrix)
                        vertices[idx] = tmp.x
                        idx += vertexSize
                        i++
                    }
                }
                2 -> {
                    var i = 0
                    while (i < count) {
                        tmp.set(vertices[idx], vertices[idx + 1], 0).mul(matrix)
                        vertices[idx] = tmp.x
                        vertices[idx + 1] = tmp.y
                        idx += vertexSize
                        i++
                    }
                }
                3 -> {
                    var i = 0
                    while (i < count) {
                        tmp.set(vertices[idx], vertices[idx + 1], vertices[idx + 2]).mul(matrix)
                        vertices[idx] = tmp.x
                        vertices[idx + 1] = tmp.y
                        vertices[idx + 2] = tmp.z
                        idx += vertexSize
                        i++
                    }
                }
            }
        }

        /**
         * Method to transform the texture coordinates (UV) in the float array. This is a potentially slow operation, use with care.
         *
         * @param matrix     the transformation matrix
         * @param vertices   the float array
         * @param vertexSize the number of floats in each vertex
         * @param offset     the offset within a vertex to the texture location
         * @param start      the vertex to start with
         * @param count      the amount of vertices to transform
         */
        fun transformUV(matrix: Matrix3?, vertices: FloatArray, vertexSize: Int, offset: Int, start: Int, count: Int) {
            if (start < 0 || count < 1 || (start + count) * vertexSize > vertices.size) throw IndexOutOfBoundsException("start = " + start + ", count = " + count + ", vertexSize = " + vertexSize
                + ", length = " + vertices.size)
            val tmp = Vector2()
            var idx = offset + start * vertexSize
            for (i in 0 until count) {
                tmp.set(vertices[idx], vertices[idx + 1]).mul(matrix)
                vertices[idx] = tmp.x
                vertices[idx + 1] = tmp.y
                idx += vertexSize
            }
        }
    }
}
