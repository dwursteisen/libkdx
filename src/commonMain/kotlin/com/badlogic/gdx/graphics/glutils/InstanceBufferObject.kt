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
 * Modification of the [VertexBufferObject] class.
 * Sets the glVertexAttribDivisor for every [VertexAttribute] automatically.
 *
 * @author mrdlink
 */
class InstanceBufferObject(isStatic: Boolean, numVertices: Int, instanceAttributes: com.badlogic.gdx.graphics.VertexAttributes?) : InstanceData {

    private var attributes: com.badlogic.gdx.graphics.VertexAttributes? = null
    private var buffer: FloatBuffer? = null
    private var byteBuffer: java.nio.ByteBuffer? = null
    private var ownsBuffer = false
    private var bufferHandle: Int
    private var usage = 0
    var isDirty = false
    var isBound = false

    constructor(isStatic: Boolean, numVertices: Int, vararg attributes: com.badlogic.gdx.graphics.VertexAttribute?) : this(isStatic, numVertices, com.badlogic.gdx.graphics.VertexAttributes(*attributes)) {}

    override fun getAttributes(): com.badlogic.gdx.graphics.VertexAttributes? {
        return attributes
    }

    override fun getNumInstances(): Int {
        return buffer.limit() * 4 / attributes.vertexSize
    }

    override fun getNumMaxInstances(): Int {
        return byteBuffer.capacity() / attributes.vertexSize
    }

    override fun getBuffer(): FloatBuffer? {
        isDirty = true
        return buffer
    }

    /**
     * Low level method to reset the buffer and attributes to the specified values. Use with care!
     *
     * @param data
     * @param ownsBuffer
     * @param value
     */
    protected fun setBuffer(data: java.nio.Buffer?, ownsBuffer: Boolean, value: com.badlogic.gdx.graphics.VertexAttributes?) {
        if (isBound) throw com.badlogic.gdx.utils.GdxRuntimeException("Cannot change attributes while VBO is bound")
        if (this.ownsBuffer && byteBuffer != null) com.badlogic.gdx.utils.BufferUtils.disposeUnsafeByteBuffer(byteBuffer)
        attributes = value
        byteBuffer = if (data is java.nio.ByteBuffer) data as java.nio.ByteBuffer? else throw com.badlogic.gdx.utils.GdxRuntimeException("Only ByteBuffer is currently supported")
        this.ownsBuffer = ownsBuffer
        val l: Int = byteBuffer.limit()
        byteBuffer.limit(byteBuffer.capacity())
        buffer = byteBuffer.asFloatBuffer()
        byteBuffer.limit(l)
        buffer.limit(l / 4)
    }

    private fun bufferChanged() {
        if (isBound) {
            com.badlogic.gdx.Gdx.gl20.glBufferData(com.badlogic.gdx.graphics.GL20.GL_ARRAY_BUFFER, byteBuffer.limit(), null, usage)
            com.badlogic.gdx.Gdx.gl20.glBufferData(com.badlogic.gdx.graphics.GL20.GL_ARRAY_BUFFER, byteBuffer.limit(), byteBuffer, usage)
            isDirty = false
        }
    }

    override fun setInstanceData(data: FloatArray?, offset: Int, count: Int) {
        isDirty = true
        com.badlogic.gdx.utils.BufferUtils.copy(data, byteBuffer, count, offset)
        buffer.position(0)
        buffer.limit(count)
        bufferChanged()
    }

    override fun setInstanceData(data: FloatBuffer?, count: Int) {
        isDirty = true
        com.badlogic.gdx.utils.BufferUtils.copy(data, byteBuffer, count)
        buffer.position(0)
        buffer.limit(count)
        bufferChanged()
    }

    override fun updateInstanceData(targetOffset: Int, data: FloatArray?, sourceOffset: Int, count: Int) {
        isDirty = true
        val pos: Int = byteBuffer.position()
        byteBuffer.position(targetOffset * 4)
        com.badlogic.gdx.utils.BufferUtils.copy(data, sourceOffset, count, byteBuffer)
        byteBuffer.position(pos)
        buffer.position(0)
        bufferChanged()
    }

    override fun updateInstanceData(targetOffset: Int, data: FloatBuffer?, sourceOffset: Int, count: Int) {
        isDirty = true
        val pos: Int = byteBuffer.position()
        byteBuffer.position(targetOffset * 4)
        data.position(sourceOffset * 4)
        com.badlogic.gdx.utils.BufferUtils.copy(data, byteBuffer, count)
        byteBuffer.position(pos)
        buffer.position(0)
        bufferChanged()
    }

    /**
     * @return The GL enum used in the call to [GL20.glBufferData], e.g. GL_STATIC_DRAW or
     * GL_DYNAMIC_DRAW
     */
    protected fun getUsage(): Int {
        return usage
    }

    /**
     * Set the GL enum used in the call to [GL20.glBufferData], can only be called when the
     * VBO is not bound.
     */
    protected fun setUsage(value: Int) {
        if (isBound) throw com.badlogic.gdx.utils.GdxRuntimeException("Cannot change usage while VBO is bound")
        usage = value
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
        bufferHandle = com.badlogic.gdx.Gdx.gl20.glGenBuffer()
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
        if (ownsBuffer) com.badlogic.gdx.utils.BufferUtils.disposeUnsafeByteBuffer(byteBuffer)
    }

    init {
        if (com.badlogic.gdx.Gdx.gl30 == null) throw com.badlogic.gdx.utils.GdxRuntimeException("InstanceBufferObject requires a device running with GLES 3.0 compatibilty")
        bufferHandle = com.badlogic.gdx.Gdx.gl20.glGenBuffer()
        val data: java.nio.ByteBuffer = com.badlogic.gdx.utils.BufferUtils.newUnsafeByteBuffer(instanceAttributes.vertexSize * numVertices)
        data.limit(0)
        setBuffer(data, true, instanceAttributes)
        setUsage(if (isStatic) com.badlogic.gdx.graphics.GL20.GL_STATIC_DRAW else com.badlogic.gdx.graphics.GL20.GL_DYNAMIC_DRAW)
    }
}
