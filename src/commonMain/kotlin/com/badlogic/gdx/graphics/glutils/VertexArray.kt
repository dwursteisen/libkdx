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
 *
 *
 * Convenience class for working with OpenGL vertex arrays. It interleaves all data in the order you specified in the constructor
 * via [VertexAttribute].
 *
 *
 *
 *
 * This class is not compatible with OpenGL 3+ core profiles. For this [VertexBufferObject]s are needed.
 *
 *
 * @author mzechner, Dave Clayton <contact></contact>@redskyforge.com>
 */
class VertexArray(numVertices: Int, attributes: com.badlogic.gdx.graphics.VertexAttributes?) : com.badlogic.gdx.graphics.glutils.VertexData {

    override val attributes: com.badlogic.gdx.graphics.VertexAttributes?
    override val buffer: FloatBuffer?
    val byteBuffer: java.nio.ByteBuffer?
    var isBound = false

    /** Constructs a new interleaved VertexArray
     *
     * @param numVertices the maximum number of vertices
     * @param attributes the [VertexAttribute]s
     */
    constructor(numVertices: Int, vararg attributes: com.badlogic.gdx.graphics.VertexAttribute?) : this(numVertices, com.badlogic.gdx.graphics.VertexAttributes(*attributes)) {}

    override fun dispose() {
        com.badlogic.gdx.utils.BufferUtils.disposeUnsafeByteBuffer(byteBuffer)
    }

    override fun getBuffer(): FloatBuffer? {
        return buffer
    }

    override fun getNumVertices(): Int {
        return buffer.limit() * 4 / attributes.vertexSize
    }

    override fun getNumMaxVertices(): Int {
        return byteBuffer.capacity() / attributes.vertexSize
    }

    override fun setVertices(vertices: FloatArray?, offset: Int, count: Int) {
        com.badlogic.gdx.utils.BufferUtils.copy(vertices, byteBuffer, count, offset)
        buffer.position(0)
        buffer.limit(count)
    }

    override fun updateVertices(targetOffset: Int, vertices: FloatArray?, sourceOffset: Int, count: Int) {
        val pos: Int = byteBuffer.position()
        byteBuffer.position(targetOffset * 4)
        com.badlogic.gdx.utils.BufferUtils.copy(vertices, sourceOffset, count, byteBuffer)
        byteBuffer.position(pos)
    }

    override fun bind(shader: com.badlogic.gdx.graphics.glutils.ShaderProgram?) {
        bind(shader, null)
    }

    override fun bind(shader: com.badlogic.gdx.graphics.glutils.ShaderProgram?, locations: IntArray?) {
        val numAttributes: Int = attributes.size()
        byteBuffer.limit(buffer.limit() * 4)
        if (locations == null) {
            for (i in 0 until numAttributes) {
                val attribute: com.badlogic.gdx.graphics.VertexAttribute = attributes.get(i)
                val location: Int = shader!!.getAttributeLocation(attribute.alias)
                if (location < 0) continue
                shader!!.enableVertexAttribute(location)
                if (attribute.type == com.badlogic.gdx.graphics.GL20.GL_FLOAT) {
                    buffer.position(attribute.offset / 4)
                    shader.setVertexAttribute(location, attribute.numComponents, attribute.type, attribute.normalized,
                        attributes.vertexSize, buffer)
                } else {
                    byteBuffer.position(attribute.offset)
                    shader.setVertexAttribute(location, attribute.numComponents, attribute.type, attribute.normalized,
                        attributes.vertexSize, byteBuffer)
                }
            }
        } else {
            for (i in 0 until numAttributes) {
                val attribute: com.badlogic.gdx.graphics.VertexAttribute = attributes.get(i)
                val location = locations[i]
                if (location < 0) continue
                shader!!.enableVertexAttribute(location)
                if (attribute.type == com.badlogic.gdx.graphics.GL20.GL_FLOAT) {
                    buffer.position(attribute.offset / 4)
                    shader.setVertexAttribute(location, attribute.numComponents, attribute.type, attribute.normalized,
                        attributes.vertexSize, buffer)
                } else {
                    byteBuffer.position(attribute.offset)
                    shader.setVertexAttribute(location, attribute.numComponents, attribute.type, attribute.normalized,
                        attributes.vertexSize, byteBuffer)
                }
            }
        }
        isBound = true
    }

    /** Unbinds this VertexBufferObject.
     *
     * @param shader the shader
     */
    override fun unbind(shader: com.badlogic.gdx.graphics.glutils.ShaderProgram?) {
        unbind(shader, null)
    }

    override fun unbind(shader: com.badlogic.gdx.graphics.glutils.ShaderProgram?, locations: IntArray?) {
        val numAttributes: Int = attributes.size()
        if (locations == null) {
            for (i in 0 until numAttributes) {
                shader.disableVertexAttribute(attributes.get(i).alias)
            }
        } else {
            for (i in 0 until numAttributes) {
                val location = locations[i]
                if (location >= 0) shader!!.disableVertexAttribute(location)
            }
        }
        isBound = false
    }

    override fun getAttributes(): com.badlogic.gdx.graphics.VertexAttributes? {
        return attributes
    }

    override fun invalidate() {}

    /** Constructs a new interleaved VertexArray
     *
     * @param numVertices the maximum number of vertices
     * @param attributes the [VertexAttributes]
     */
    init {
        this.attributes = attributes
        byteBuffer = com.badlogic.gdx.utils.BufferUtils.newUnsafeByteBuffer(this.attributes.vertexSize * numVertices)
        buffer = byteBuffer.asFloatBuffer()
        buffer.flip()
        byteBuffer.flip()
    }
}
