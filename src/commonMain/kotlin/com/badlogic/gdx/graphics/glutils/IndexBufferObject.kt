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
 * In IndexBufferObject wraps OpenGL's index buffer functionality to be used in conjunction with VBOs. This class can be
 * seamlessly used with OpenGL ES 1.x and 2.0.
 *
 *
 *
 *
 * Uses indirect Buffers on Android 1.5/1.6 to fix GC invocation due to leaking PlatformAddress instances.
 *
 *
 *
 *
 * You can also use this to store indices for vertex arrays. Do not call [.bind] or [.unbind] in this case but
 * rather use [.getBuffer] to use the buffer directly with glDrawElements. You must also create the IndexBufferObject with
 * the second constructor and specify isDirect as true as glDrawElements in conjunction with vertex arrays needs direct buffers.
 *
 *
 *
 *
 * VertexBufferObjects must be disposed via the [.dispose] method when no longer needed
 *
 *
 * @author mzechner, Thorsten Schleinzer
 */
class IndexBufferObject : com.badlogic.gdx.graphics.glutils.IndexData {

    override val buffer: ShortBuffer?
    val byteBuffer: java.nio.ByteBuffer?
    val ownsBuffer: Boolean
    var bufferHandle: Int
    val isDirect: Boolean
    var isDirty = true
    var isBound = false
    val usage: Int
    // used to work around bug: https://android-review.googlesource.com/#/c/73175/
    private val empty: Boolean

    /** Creates a new static IndexBufferObject to be used with vertex arrays.
     *
     * @param maxIndices the maximum number of indices this buffer can hold
     */
    constructor(maxIndices: Int) : this(true, maxIndices) {}

    /** Creates a new IndexBufferObject.
     *
     * @param isStatic whether the index buffer is static
     * @param maxIndices the maximum number of indices this buffer can hold
     */
    constructor(isStatic: Boolean, maxIndices: Int) {
        var maxIndices = maxIndices
        empty = maxIndices == 0
        if (empty) {
            maxIndices = 1 // avoid allocating a zero-sized buffer because of bug in Android's ART < Android 5.0
        }
        byteBuffer = com.badlogic.gdx.utils.BufferUtils.newUnsafeByteBuffer(maxIndices * 2)
        isDirect = true
        buffer = byteBuffer.asShortBuffer()
        ownsBuffer = true
        buffer.flip()
        byteBuffer.flip()
        bufferHandle = com.badlogic.gdx.Gdx.gl20.glGenBuffer()
        usage = if (isStatic) com.badlogic.gdx.graphics.GL20.GL_STATIC_DRAW else com.badlogic.gdx.graphics.GL20.GL_DYNAMIC_DRAW
    }

    constructor(isStatic: Boolean, data: java.nio.ByteBuffer?) {
        empty = data.limit() == 0
        byteBuffer = data
        isDirect = true
        buffer = byteBuffer.asShortBuffer()
        ownsBuffer = false
        bufferHandle = com.badlogic.gdx.Gdx.gl20.glGenBuffer()
        usage = if (isStatic) com.badlogic.gdx.graphics.GL20.GL_STATIC_DRAW else com.badlogic.gdx.graphics.GL20.GL_DYNAMIC_DRAW
    }

    /** @return the number of indices currently stored in this buffer
     */
    override fun getNumIndices(): Int {
        return if (empty) 0 else buffer.limit()
    }

    /** @return the maximum number of indices this IndexBufferObject can store.
     */
    override fun getNumMaxIndices(): Int {
        return if (empty) 0 else buffer.capacity()
    }

    /**
     *
     *
     * Sets the indices of this IndexBufferObject, discarding the old indices. The count must equal the number of indices to be
     * copied to this IndexBufferObject.
     *
     *
     *
     *
     * This can be called in between calls to [.bind] and [.unbind]. The index data will be updated instantly.
     *
     *
     * @param indices the vertex data
     * @param offset the offset to start copying the data from
     * @param count the number of shorts to copy
     */
    override fun setIndices(indices: ShortArray?, offset: Int, count: Int) {
        isDirty = true
        buffer.clear()
        buffer.put(indices, offset, count)
        buffer.flip()
        byteBuffer.position(0)
        byteBuffer.limit(count shl 1)
        if (isBound) {
            com.badlogic.gdx.Gdx.gl20.glBufferData(com.badlogic.gdx.graphics.GL20.GL_ELEMENT_ARRAY_BUFFER, byteBuffer.limit(), byteBuffer, usage)
            isDirty = false
        }
    }

    override fun setIndices(indices: ShortBuffer?) {
        isDirty = true
        val pos: Int = indices.position()
        buffer.clear()
        buffer.put(indices)
        buffer.flip()
        indices.position(pos)
        byteBuffer.position(0)
        byteBuffer.limit(buffer.limit() shl 1)
        if (isBound) {
            com.badlogic.gdx.Gdx.gl20.glBufferData(com.badlogic.gdx.graphics.GL20.GL_ELEMENT_ARRAY_BUFFER, byteBuffer.limit(), byteBuffer, usage)
            isDirty = false
        }
    }

    override fun updateIndices(targetOffset: Int, indices: ShortArray?, offset: Int, count: Int) {
        isDirty = true
        val pos: Int = byteBuffer.position()
        byteBuffer.position(targetOffset * 2)
        com.badlogic.gdx.utils.BufferUtils.copy(indices, offset, byteBuffer, count)
        byteBuffer.position(pos)
        buffer.position(0)
        if (isBound) {
            com.badlogic.gdx.Gdx.gl20.glBufferData(com.badlogic.gdx.graphics.GL20.GL_ELEMENT_ARRAY_BUFFER, byteBuffer.limit(), byteBuffer, usage)
            isDirty = false
        }
    }

    /**
     *
     *
     * Returns the underlying ShortBuffer. If you modify the buffer contents they wil be uploaded on the call to [.bind].
     * If you need immediate uploading use [.setIndices].
     *
     *
     * @return the underlying short buffer.
     */
    override fun getBuffer(): ShortBuffer? {
        isDirty = true
        return buffer
    }

    /** Binds this IndexBufferObject for rendering with glDrawElements.  */
    override fun bind() {
        if (bufferHandle == 0) throw com.badlogic.gdx.utils.GdxRuntimeException("No buffer allocated!")
        com.badlogic.gdx.Gdx.gl20.glBindBuffer(com.badlogic.gdx.graphics.GL20.GL_ELEMENT_ARRAY_BUFFER, bufferHandle)
        if (isDirty) {
            byteBuffer.limit(buffer.limit() * 2)
            com.badlogic.gdx.Gdx.gl20.glBufferData(com.badlogic.gdx.graphics.GL20.GL_ELEMENT_ARRAY_BUFFER, byteBuffer.limit(), byteBuffer, usage)
            isDirty = false
        }
        isBound = true
    }

    /** Unbinds this IndexBufferObject.  */
    override fun unbind() {
        com.badlogic.gdx.Gdx.gl20.glBindBuffer(com.badlogic.gdx.graphics.GL20.GL_ELEMENT_ARRAY_BUFFER, 0)
        isBound = false
    }

    /** Invalidates the IndexBufferObject so a new OpenGL buffer handle is created. Use this in case of a context loss.  */
    override fun invalidate() {
        bufferHandle = com.badlogic.gdx.Gdx.gl20.glGenBuffer()
        isDirty = true
    }

    /** Disposes this IndexBufferObject and all its associated OpenGL resources.  */
    override fun dispose() {
        com.badlogic.gdx.Gdx.gl20.glBindBuffer(com.badlogic.gdx.graphics.GL20.GL_ELEMENT_ARRAY_BUFFER, 0)
        com.badlogic.gdx.Gdx.gl20.glDeleteBuffer(bufferHandle)
        bufferHandle = 0
        if (ownsBuffer) {
            com.badlogic.gdx.utils.BufferUtils.disposeUnsafeByteBuffer(byteBuffer)
        }
    }
}
