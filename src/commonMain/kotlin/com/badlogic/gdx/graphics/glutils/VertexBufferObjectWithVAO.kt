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
 * A [VertexData] implementation that uses vertex buffer objects and vertex array objects.
 * (This is required for OpenGL 3.0+ core profiles. In particular, the default VAO has been
 * deprecated, as has the use of client memory for passing vertex attributes.) Use of VAOs should
 * give a slight performance benefit since you don't have to bind the attributes on every draw
 * anymore.
 *
 *
 *
 *
 * If the OpenGL ES context was lost you can call [.invalidate] to recreate a new OpenGL vertex buffer object.
 *
 *
 *
 *
 * VertexBufferObjectWithVAO objects must be disposed via the [.dispose] method when no longer needed
 *
 *
 * Code adapted from [VertexBufferObject].
 * @author mzechner, Dave Clayton <contact></contact>@redskyforge.com>, Nate Austin <nate.austin gmail>
</nate.austin> */
class VertexBufferObjectWithVAO : com.badlogic.gdx.graphics.glutils.VertexData {

    override val attributes: com.badlogic.gdx.graphics.VertexAttributes?
    override val buffer: FloatBuffer?
    val byteBuffer: java.nio.ByteBuffer?
    val ownsBuffer: Boolean
    var bufferHandle: Int
    val isStatic: Boolean
    val usage: Int
    var isDirty = false
    var isBound = false
    var vaoHandle = -1
    var cachedLocations: com.badlogic.gdx.utils.IntArray? = com.badlogic.gdx.utils.IntArray()

    /**
     * Constructs a new interleaved VertexBufferObjectWithVAO.
     *
     * @param isStatic    whether the vertex data is static.
     * @param numVertices the maximum number of vertices
     * @param attributes  the [com.badlogic.gdx.graphics.VertexAttribute]s.
     */
    constructor(isStatic: Boolean, numVertices: Int, vararg attributes: com.badlogic.gdx.graphics.VertexAttribute?) : this(isStatic, numVertices, com.badlogic.gdx.graphics.VertexAttributes(*attributes)) {}

    /**
     * Constructs a new interleaved VertexBufferObjectWithVAO.
     *
     * @param isStatic    whether the vertex data is static.
     * @param numVertices the maximum number of vertices
     * @param attributes  the [VertexAttributes].
     */
    constructor(isStatic: Boolean, numVertices: Int, attributes: com.badlogic.gdx.graphics.VertexAttributes?) {
        this.isStatic = isStatic
        this.attributes = attributes
        byteBuffer = com.badlogic.gdx.utils.BufferUtils.newUnsafeByteBuffer(this.attributes.vertexSize * numVertices)
        buffer = byteBuffer.asFloatBuffer()
        ownsBuffer = true
        buffer.flip()
        byteBuffer.flip()
        bufferHandle = com.badlogic.gdx.Gdx.gl20.glGenBuffer()
        usage = if (isStatic) com.badlogic.gdx.graphics.GL20.GL_STATIC_DRAW else com.badlogic.gdx.graphics.GL20.GL_DYNAMIC_DRAW
        createVAO()
    }

    constructor(isStatic: Boolean, unmanagedBuffer: java.nio.ByteBuffer?, attributes: com.badlogic.gdx.graphics.VertexAttributes?) {
        this.isStatic = isStatic
        this.attributes = attributes
        byteBuffer = unmanagedBuffer
        ownsBuffer = false
        buffer = byteBuffer.asFloatBuffer()
        buffer.flip()
        byteBuffer.flip()
        bufferHandle = com.badlogic.gdx.Gdx.gl20.glGenBuffer()
        usage = if (isStatic) com.badlogic.gdx.graphics.GL20.GL_STATIC_DRAW else com.badlogic.gdx.graphics.GL20.GL_DYNAMIC_DRAW
        createVAO()
    }

    override fun getAttributes(): com.badlogic.gdx.graphics.VertexAttributes? {
        return attributes
    }

    override fun getNumVertices(): Int {
        return buffer.limit() * 4 / attributes.vertexSize
    }

    override fun getNumMaxVertices(): Int {
        return byteBuffer.capacity() / attributes.vertexSize
    }

    override fun getBuffer(): FloatBuffer? {
        isDirty = true
        return buffer
    }

    private fun bufferChanged() {
        if (isBound) {
            com.badlogic.gdx.Gdx.gl20.glBufferData(com.badlogic.gdx.graphics.GL20.GL_ARRAY_BUFFER, byteBuffer.limit(), byteBuffer, usage)
            isDirty = false
        }
    }

    override fun setVertices(vertices: FloatArray?, offset: Int, count: Int) {
        isDirty = true
        com.badlogic.gdx.utils.BufferUtils.copy(vertices, byteBuffer, count, offset)
        buffer.position(0)
        buffer.limit(count)
        bufferChanged()
    }

    override fun updateVertices(targetOffset: Int, vertices: FloatArray?, sourceOffset: Int, count: Int) {
        isDirty = true
        val pos: Int = byteBuffer.position()
        byteBuffer.position(targetOffset * 4)
        com.badlogic.gdx.utils.BufferUtils.copy(vertices, sourceOffset, count, byteBuffer)
        byteBuffer.position(pos)
        buffer.position(0)
        bufferChanged()
    }

    /**
     * Binds this VertexBufferObject for rendering via glDrawArrays or glDrawElements
     *
     * @param shader the shader
     */
    override fun bind(shader: com.badlogic.gdx.graphics.glutils.ShaderProgram?) {
        bind(shader, null)
    }

    override fun bind(shader: com.badlogic.gdx.graphics.glutils.ShaderProgram?, locations: IntArray?) {
        val gl: com.badlogic.gdx.graphics.GL30 = com.badlogic.gdx.Gdx.gl30
        gl.glBindVertexArray(vaoHandle)
        bindAttributes(shader, locations)
        //if our data has changed upload it:
        bindData(gl)
        isBound = true
    }

    private fun bindAttributes(shader: com.badlogic.gdx.graphics.glutils.ShaderProgram?, locations: IntArray?) {
        var stillValid = cachedLocations.size != 0
        val numAttributes: Int = attributes.size()
        if (stillValid) {
            if (locations == null) {
                var i = 0
                while (stillValid && i < numAttributes) {
                    val attribute: com.badlogic.gdx.graphics.VertexAttribute = attributes.get(i)
                    val location: Int = shader!!.getAttributeLocation(attribute.alias)
                    stillValid = location == cachedLocations.get(i)
                    i++
                }
            } else {
                stillValid = locations.size == cachedLocations.size
                var i = 0
                while (stillValid && i < numAttributes) {
                    stillValid = locations[i] == cachedLocations.get(i)
                    i++
                }
            }
        }
        if (!stillValid) {
            com.badlogic.gdx.Gdx.gl.glBindBuffer(com.badlogic.gdx.graphics.GL20.GL_ARRAY_BUFFER, bufferHandle)
            unbindAttributes(shader)
            cachedLocations.clear()
            for (i in 0 until numAttributes) {
                val attribute: com.badlogic.gdx.graphics.VertexAttribute = attributes.get(i)
                if (locations == null) {
                    cachedLocations.add(shader!!.getAttributeLocation(attribute.alias))
                } else {
                    cachedLocations.add(locations[i])
                }
                val location: Int = cachedLocations.get(i)
                if (location < 0) {
                    continue
                }
                shader!!.enableVertexAttribute(location)
                shader.setVertexAttribute(location, attribute.numComponents, attribute.type, attribute.normalized, attributes.vertexSize, attribute.offset)
            }
        }
    }

    private fun unbindAttributes(shaderProgram: com.badlogic.gdx.graphics.glutils.ShaderProgram?) {
        if (cachedLocations.size == 0) {
            return
        }
        val numAttributes: Int = attributes.size()
        for (i in 0 until numAttributes) {
            val location: Int = cachedLocations.get(i)
            if (location < 0) {
                continue
            }
            shaderProgram!!.disableVertexAttribute(location)
        }
    }

    private fun bindData(gl: com.badlogic.gdx.graphics.GL20?) {
        if (isDirty) {
            gl.glBindBuffer(com.badlogic.gdx.graphics.GL20.GL_ARRAY_BUFFER, bufferHandle)
            byteBuffer.limit(buffer.limit() * 4)
            gl.glBufferData(com.badlogic.gdx.graphics.GL20.GL_ARRAY_BUFFER, byteBuffer.limit(), byteBuffer, usage)
            isDirty = false
        }
    }

    /**
     * Unbinds this VertexBufferObject.
     *
     * @param shader the shader
     */
    override fun unbind(shader: com.badlogic.gdx.graphics.glutils.ShaderProgram?) {
        unbind(shader, null)
    }

    override fun unbind(shader: com.badlogic.gdx.graphics.glutils.ShaderProgram?, locations: IntArray?) {
        val gl: com.badlogic.gdx.graphics.GL30 = com.badlogic.gdx.Gdx.gl30
        gl.glBindVertexArray(0)
        isBound = false
    }

    /**
     * Invalidates the VertexBufferObject so a new OpenGL buffer handle is created. Use this in case of a context loss.
     */
    override fun invalidate() {
        bufferHandle = com.badlogic.gdx.Gdx.gl30.glGenBuffer()
        createVAO()
        isDirty = true
    }

    /**
     * Disposes of all resources this VertexBufferObject uses.
     */
    override fun dispose() {
        val gl: com.badlogic.gdx.graphics.GL30 = com.badlogic.gdx.Gdx.gl30
        gl.glBindBuffer(com.badlogic.gdx.graphics.GL20.GL_ARRAY_BUFFER, 0)
        gl.glDeleteBuffer(bufferHandle)
        bufferHandle = 0
        if (ownsBuffer) {
            com.badlogic.gdx.utils.BufferUtils.disposeUnsafeByteBuffer(byteBuffer)
        }
        deleteVAO()
    }

    private fun createVAO() {
        tmpHandle.clear()
        com.badlogic.gdx.Gdx.gl30.glGenVertexArrays(1, tmpHandle)
        vaoHandle = tmpHandle.get()
    }

    private fun deleteVAO() {
        if (vaoHandle != -1) {
            tmpHandle.clear()
            tmpHandle.put(vaoHandle)
            tmpHandle.flip()
            com.badlogic.gdx.Gdx.gl30.glDeleteVertexArrays(1, tmpHandle)
            vaoHandle = -1
        }
    }

    companion object {
        val tmpHandle: java.nio.IntBuffer? = com.badlogic.gdx.utils.BufferUtils.newIntBuffer(1)
    }
}
