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

/**
 * Modification of the [VertexBufferObjectSubData] class.
 * Sets the glVertexAttribDivisor for every [VertexAttribute] automatically.
 *
 * @author mrdlink
 */
class InstanceBufferObjectSubData(val isStatic: Boolean, numInstances: Int, instanceAttributes: com.badlogic.gdx.graphics.VertexAttributes?) : InstanceData {

    val attributes: com.badlogic.gdx.graphics.VertexAttributes?
    val buffer: FloatBuffer?
    val byteBuffer: java.nio.ByteBuffer?
    var bufferHandle: Int
    val isDirect: Boolean
    val usage: Int
    var isDirty = false
    var isBound = false

    /**
     * Constructs a new interleaved InstanceBufferObject.
     *
     * @param isStatic           whether the vertex data is static.
     * @param numInstances       the maximum number of vertices
     * @param instanceAttributes the [VertexAttributes].
     */
    constructor(isStatic: Boolean, numInstances: Int, vararg instanceAttributes: com.badlogic.gdx.graphics.VertexAttribute?) : this(isStatic, numInstances, com.badlogic.gdx.graphics.VertexAttributes(*instanceAttributes)) {}

    private fun createBufferObject(): Int {
        val result: Int = com.badlogic.gdx.Gdx.gl20.glGenBuffer()
        com.badlogic.gdx.Gdx.gl20.glBindBuffer(com.badlogic.gdx.graphics.GL20.GL_ARRAY_BUFFER, result)
        com.badlogic.gdx.Gdx.gl20.glBufferData(com.badlogic.gdx.graphics.GL20.GL_ARRAY_BUFFER, byteBuffer.capacity(), null, usage)
        com.badlogic.gdx.Gdx.gl20.glBindBuffer(com.badlogic.gdx.graphics.GL20.GL_ARRAY_BUFFER, 0)
        return result
    }

    override fun getAttributes(): com.badlogic.gdx.graphics.VertexAttributes? {
        return attributes
    }

    /**
     * Effectively returns [.getNumInstances].
     *
     * @return number of instances in this buffer
     */
    override fun getNumInstances(): Int {
        return buffer.limit() * 4 / attributes.vertexSize
    }

    /**
     * Effectively returns [.getNumMaxInstances].
     *
     * @return maximum number of instances in this buffer
     */
    override fun getNumMaxInstances(): Int {
        return byteBuffer.capacity() / attributes.vertexSize
    }

    override fun getBuffer(): FloatBuffer? {
        isDirty = true
        return buffer
    }

    private fun bufferChanged() {
        if (isBound) {
            com.badlogic.gdx.Gdx.gl20.glBufferData(com.badlogic.gdx.graphics.GL20.GL_ARRAY_BUFFER, byteBuffer.limit(), null, usage)
            com.badlogic.gdx.Gdx.gl20.glBufferSubData(com.badlogic.gdx.graphics.GL20.GL_ARRAY_BUFFER, 0, byteBuffer.limit(), byteBuffer)
            isDirty = false
        }
    }

    override fun setInstanceData(data: FloatArray?, offset: Int, count: Int) {
        isDirty = true
        if (isDirect) {
            com.badlogic.gdx.utils.BufferUtils.copy(data, byteBuffer, count, offset)
            buffer.position(0)
            buffer.limit(count)
        } else {
            buffer.clear()
            buffer.put(data, offset, count)
            buffer.flip()
            byteBuffer.position(0)
            byteBuffer.limit(buffer.limit() shl 2)
        }
        bufferChanged()
    }

    override fun setInstanceData(data: FloatBuffer?, count: Int) {
        isDirty = true
        if (isDirect) {
            com.badlogic.gdx.utils.BufferUtils.copy(data, byteBuffer, count)
            buffer.position(0)
            buffer.limit(count)
        } else {
            buffer.clear()
            buffer.put(data)
            buffer.flip()
            byteBuffer.position(0)
            byteBuffer.limit(buffer.limit() shl 2)
        }
        bufferChanged()
    }

    override fun updateInstanceData(targetOffset: Int, data: FloatArray?, sourceOffset: Int, count: Int) {
        isDirty = true
        if (isDirect) {
            val pos: Int = byteBuffer.position()
            byteBuffer.position(targetOffset * 4)
            com.badlogic.gdx.utils.BufferUtils.copy(data, sourceOffset, count, byteBuffer)
            byteBuffer.position(pos)
        } else throw com.badlogic.gdx.utils.GdxRuntimeException("Buffer must be allocated direct.") // Should never happen
        bufferChanged()
    }

    override fun updateInstanceData(targetOffset: Int, data: FloatBuffer?, sourceOffset: Int, count: Int) {
        isDirty = true
        if (isDirect) {
            val pos: Int = byteBuffer.position()
            byteBuffer.position(targetOffset * 4)
            data.position(sourceOffset * 4)
            com.badlogic.gdx.utils.BufferUtils.copy(data, byteBuffer, count)
            byteBuffer.position(pos)
        } else throw com.badlogic.gdx.utils.GdxRuntimeException("Buffer must be allocated direct.") // Should never happen
        bufferChanged()
    }

    /**
     * Binds this InstanceBufferObject for rendering via glDrawArraysInstanced or glDrawElementsInstanced
     *
     * @param shader the shader
     */
    override fun bind(shader: com.badlogic.gdx.graphics.glutils.ShaderProgram?) {
        bind(shader, null)
    }

    override fun bind(shader: com.badlogic.gdx.graphics.glutils.ShaderProgram?, locations: IntArray?) {
        val gl: com.badlogic.gdx.graphics.GL20 = com.badlogic.gdx.Gdx.gl20
        gl.glBindBuffer(com.badlogic.gdx.graphics.GL20.GL_ARRAY_BUFFER, bufferHandle)
        if (isDirty) {
            byteBuffer.limit(buffer.limit() * 4)
            gl.glBufferData(com.badlogic.gdx.graphics.GL20.GL_ARRAY_BUFFER, byteBuffer.limit(), byteBuffer, usage)
            isDirty = false
        }
        val numAttributes: Int = attributes.size()
        if (locations == null) {
            for (i in 0 until numAttributes) {
                val attribute: com.badlogic.gdx.graphics.VertexAttribute = attributes.get(i)
                val location: Int = shader!!.getAttributeLocation(attribute.alias)
                if (location < 0) continue
                val unitOffset: Int = +attribute.unit
                shader!!.enableVertexAttribute(location + unitOffset)
                shader.setVertexAttribute(location + unitOffset, attribute.numComponents, attribute.type, attribute.normalized, attributes.vertexSize, attribute.offset)
                com.badlogic.gdx.Gdx.gl30.glVertexAttribDivisor(location + unitOffset, 1)
            }
        } else {
            for (i in 0 until numAttributes) {
                val attribute: com.badlogic.gdx.graphics.VertexAttribute = attributes.get(i)
                val location = locations[i]
                if (location < 0) continue
                val unitOffset: Int = +attribute.unit
                shader!!.enableVertexAttribute(location + unitOffset)
                shader.setVertexAttribute(location + unitOffset, attribute.numComponents, attribute.type, attribute.normalized, attributes.vertexSize, attribute.offset)
                com.badlogic.gdx.Gdx.gl30.glVertexAttribDivisor(location + unitOffset, 1)
            }
        }
        isBound = true
    }

    /**
     * Unbinds this InstanceBufferObject.
     *
     * @param shader the shader
     */
    override fun unbind(shader: com.badlogic.gdx.graphics.glutils.ShaderProgram?) {
        unbind(shader, null)
    }

    override fun unbind(shader: com.badlogic.gdx.graphics.glutils.ShaderProgram?, locations: IntArray?) {
        val gl: com.badlogic.gdx.graphics.GL20 = com.badlogic.gdx.Gdx.gl20
        val numAttributes: Int = attributes.size()
        if (locations == null) {
            for (i in 0 until numAttributes) {
                val attribute: com.badlogic.gdx.graphics.VertexAttribute = attributes.get(i)
                val location: Int = shader!!.getAttributeLocation(attribute.alias)
                if (location < 0) continue
                val unitOffset: Int = +attribute.unit
                shader!!.disableVertexAttribute(location + unitOffset)
            }
        } else {
            for (i in 0 until numAttributes) {
                val attribute: com.badlogic.gdx.graphics.VertexAttribute = attributes.get(i)
                val location = locations[i]
                if (location < 0) continue
                val unitOffset: Int = +attribute.unit
                shader!!.enableVertexAttribute(location + unitOffset)
            }
        }
        gl.glBindBuffer(com.badlogic.gdx.graphics.GL20.GL_ARRAY_BUFFER, 0)
        isBound = false
    }

    /**
     * Invalidates the InstanceBufferObject so a new OpenGL buffer handle is created. Use this in case of a context loss.
     */
    override fun invalidate() {
        bufferHandle = createBufferObject()
        isDirty = true
    }

    /**
     * Disposes of all resources this InstanceBufferObject uses.
     */
    override fun dispose() {
        val gl: com.badlogic.gdx.graphics.GL20 = com.badlogic.gdx.Gdx.gl20
        gl.glBindBuffer(com.badlogic.gdx.graphics.GL20.GL_ARRAY_BUFFER, 0)
        gl.glDeleteBuffer(bufferHandle)
        bufferHandle = 0
    }

    /**
     * Returns the InstanceBufferObject handle
     *
     * @return the InstanceBufferObject handle
     */
    fun getBufferHandle(): Int {
        return bufferHandle
    }

    /**
     * Constructs a new interleaved InstanceBufferObject.
     *
     * @param isStatic           whether the vertex data is static.
     * @param numInstances       the maximum number of vertices
     * @param instanceAttributes the [VertexAttribute]s.
     */
    init {
        attributes = instanceAttributes
        byteBuffer = com.badlogic.gdx.utils.BufferUtils.newByteBuffer(attributes.vertexSize * numInstances)
        isDirect = true
        usage = if (isStatic) com.badlogic.gdx.graphics.GL20.GL_STATIC_DRAW else com.badlogic.gdx.graphics.GL20.GL_DYNAMIC_DRAW
        buffer = byteBuffer.asFloatBuffer()
        bufferHandle = createBufferObject()
        buffer.flip()
        byteBuffer.flip()
    }
}
