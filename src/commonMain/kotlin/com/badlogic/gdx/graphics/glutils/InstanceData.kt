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
package com.badlogic.gdx.graphics.glutils

import com.badlogic.gdx.graphics.glutils.HdpiMode
import java.io.BufferedInputStream
import java.lang.IllegalStateException
import java.lang.NumberFormatException
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import java.util.HashMap
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
 * A InstanceData instance holds instance data for rendering with OpenGL. It is implemented as either a [InstanceBufferObject] or a
 * [InstanceBufferObjectSubData]. Both require Open GL 3.3+.
 *
 * @author mrdlink
 */
interface InstanceData : com.badlogic.gdx.utils.Disposable {

    /**
     * @return the number of vertices this InstanceData stores
     */
    fun getNumInstances(): Int

    /**
     * @return the number of vertices this InstanceData can store
     */
    fun getNumMaxInstances(): Int

    /**
     * @return the [VertexAttributes] as specified during construction.
     */
    fun getAttributes(): com.badlogic.gdx.graphics.VertexAttributes?

    /**
     * Sets the vertices of this InstanceData, discarding the old vertex data. The count must equal the number of floats per vertex
     * times the number of vertices to be copied to this VertexData. The order of the vertex attributes must be the same as
     * specified at construction time via [VertexAttributes].
     *
     *
     * This can be called in between calls to bind and unbind. The vertex data will be updated instantly.
     *
     * @param data   the instance data
     * @param offset the offset to start copying the data from
     * @param count  the number of floats to copy
     */
    fun setInstanceData(data: FloatArray?, offset: Int, count: Int)

    /**
     * Update (a portion of) the vertices. Does not resize the backing buffer.
     *
     * @param data         the instance data
     * @param sourceOffset the offset to start copying the data from
     * @param count        the number of floats to copy
     */
    fun updateInstanceData(targetOffset: Int, data: FloatArray?, sourceOffset: Int, count: Int)

    /**
     * Sets the vertices of this InstanceData, discarding the old vertex data. The count must equal the number of floats per vertex
     * times the number of vertices to be copied to this InstanceData. The order of the vertex attributes must be the same as
     * specified at construction time via [VertexAttributes].
     *
     *
     * This can be called in between calls to bind and unbind. The vertex data will be updated instantly.
     *
     * @param data  the instance data
     * @param count the number of floats to copy
     */
    fun setInstanceData(data: FloatBuffer?, count: Int)

    /**
     * Update (a portion of) the vertices. Does not resize the backing buffer.
     *
     * @param data         the vertex data
     * @param sourceOffset the offset to start copying the data from
     * @param count        the number of floats to copy
     */
    fun updateInstanceData(targetOffset: Int, data: FloatBuffer?, sourceOffset: Int, count: Int)

    /**
     * Returns the underlying FloatBuffer and marks it as dirty, causing the buffer contents to be uploaded on the next call to
     * bind. If you need immediate uploading use [.setInstanceData]; Any modifications made to the Buffer
     * *after* the call to bind will not automatically be uploaded.
     *
     * @return the underlying FloatBuffer holding the vertex data.
     */
    fun getBuffer(): FloatBuffer?

    /**
     * Binds this InstanceData for rendering via glDrawArraysInstanced or glDrawElementsInstanced.
     */
    fun bind(shader: com.badlogic.gdx.graphics.glutils.ShaderProgram?)

    /**
     * Binds this InstanceData for rendering via glDrawArraysInstanced or glDrawElementsInstanced.
     *
     * @param locations array containing the attribute locations.
     */
    fun bind(shader: com.badlogic.gdx.graphics.glutils.ShaderProgram?, locations: IntArray?)

    /**
     * Unbinds this InstanceData.
     */
    fun unbind(shader: com.badlogic.gdx.graphics.glutils.ShaderProgram?)

    /**
     * Unbinds this InstanceData.
     *
     * @param locations array containing the attribute locations.
     */
    fun unbind(shader: com.badlogic.gdx.graphics.glutils.ShaderProgram?, locations: IntArray?)

    /**
     * Invalidates the InstanceData if applicable. Use this in case of a context loss.
     */
    fun invalidate()

    /**
     * Disposes this InstanceData and all its associated OpenGL resources.
     */
    override fun dispose()
}
