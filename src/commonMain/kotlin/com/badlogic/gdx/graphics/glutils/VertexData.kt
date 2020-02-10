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
import com.badlogic.gdx.graphics.glutils.InstanceData
import java.io.BufferedInputStream
import java.lang.IllegalStateException
import java.lang.NumberFormatException
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import java.util.HashMap
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/** A VertexData instance holds vertices for rendering with OpenGL. It is implemented as either a [VertexArray] or a
 * [VertexBufferObject]. Only the later supports OpenGL ES 2.0.
 *
 * @author mzechner
 */
interface VertexData : com.badlogic.gdx.utils.Disposable {

    /** @return the number of vertices this VertexData stores
     */
    val numVertices: Int

    /** @return the number of vertices this VertedData can store
     */
    val numMaxVertices: Int

    /** @return the [VertexAttributes] as specified during construction.
     */
    val attributes: com.badlogic.gdx.graphics.VertexAttributes?

    /** Sets the vertices of this VertexData, discarding the old vertex data. The count must equal the number of floats per vertex
     * times the number of vertices to be copied to this VertexData. The order of the vertex attributes must be the same as
     * specified at construction time via [VertexAttributes].
     *
     *
     * This can be called in between calls to bind and unbind. The vertex data will be updated instantly.
     * @param vertices the vertex data
     * @param offset the offset to start copying the data from
     * @param count the number of floats to copy
     */
    fun setVertices(vertices: FloatArray?, offset: Int, count: Int)

    /** Update (a portion of) the vertices. Does not resize the backing buffer.
     * @param vertices the vertex data
     * @param sourceOffset the offset to start copying the data from
     * @param count the number of floats to copy
     */
    fun updateVertices(targetOffset: Int, vertices: FloatArray?, sourceOffset: Int, count: Int)

    /** Returns the underlying FloatBuffer and marks it as dirty, causing the buffer contents to be uploaded on the next call to
     * bind. If you need immediate uploading use [.setVertices]; Any modifications made to the Buffer
     * *after* the call to bind will not automatically be uploaded.
     * @return the underlying FloatBuffer holding the vertex data.
     */
    val buffer: FloatBuffer?

    /** Binds this VertexData for rendering via glDrawArrays or glDrawElements.  */
    fun bind(shader: com.badlogic.gdx.graphics.glutils.ShaderProgram?)

    /** Binds this VertexData for rendering via glDrawArrays or glDrawElements.
     * @param locations array containing the attribute locations.
     */
    fun bind(shader: com.badlogic.gdx.graphics.glutils.ShaderProgram?, locations: IntArray?)

    /** Unbinds this VertexData.  */
    fun unbind(shader: com.badlogic.gdx.graphics.glutils.ShaderProgram?)

    /** Unbinds this VertexData.
     * @param locations array containing the attribute locations.
     */
    fun unbind(shader: com.badlogic.gdx.graphics.glutils.ShaderProgram?, locations: IntArray?)

    /** Invalidates the VertexData if applicable. Use this in case of a context loss.  */
    fun invalidate()

    /** Disposes this VertexData and all its associated OpenGL resources.  */
    override fun dispose()
}
