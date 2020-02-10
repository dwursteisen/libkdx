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

class IndexArray(maxIndices: Int) : com.badlogic.gdx.graphics.glutils.IndexData {
    override val buffer: ShortBuffer?
    val byteBuffer: java.nio.ByteBuffer?
    // used to work around bug: https://android-review.googlesource.com/#/c/73175/
    private val empty: Boolean

    /** @return the number of indices currently stored in this buffer
     */
    override val numIndices: Int
        get() = if (empty) 0 else buffer.limit()

    /** @return the maximum number of indices this IndexArray can store.
     */
    override val numMaxIndices: Int
        get() = if (empty) 0 else buffer.capacity()

    /**
     *
     *
     * Sets the indices of this IndexArray, discarding the old indices. The count must equal the number of indices to be copied to
     * this IndexArray.
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
        buffer.clear()
        buffer.put(indices, offset, count)
        buffer.flip()
        byteBuffer.position(0)
        byteBuffer.limit(count shl 1)
    }

    override fun setIndices(indices: ShortBuffer?) {
        val pos: Int = indices.position()
        buffer.clear()
        buffer.limit(indices.remaining())
        buffer.put(indices)
        buffer.flip()
        indices.position(pos)
        byteBuffer.position(0)
        byteBuffer.limit(buffer.limit() shl 1)
    }

    override fun updateIndices(targetOffset: Int, indices: ShortArray?, offset: Int, count: Int) {
        val pos: Int = byteBuffer.position()
        byteBuffer.position(targetOffset * 2)
        com.badlogic.gdx.utils.BufferUtils.copy(indices, offset, byteBuffer, count)
        byteBuffer.position(pos)
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
        return buffer
    }

    /** Binds this IndexArray for rendering with glDrawElements.  */
    override fun bind() {}

    /** Unbinds this IndexArray.  */
    override fun unbind() {}

    /** Invalidates the IndexArray so a new OpenGL buffer handle is created. Use this in case of a context loss.  */
    override fun invalidate() {}

    /** Disposes this IndexArray and all its associated OpenGL resources.  */
    override fun dispose() {
        com.badlogic.gdx.utils.BufferUtils.disposeUnsafeByteBuffer(byteBuffer)
    }

    /** Creates a new IndexArray to be used with vertex arrays.
     *
     * @param maxIndices the maximum number of indices this buffer can hold
     */
    init {
        var maxIndices = maxIndices
        empty = maxIndices == 0
        if (empty) {
            maxIndices = 1 // avoid allocating a zero-sized buffer because of bug in Android's ART < Android 5.0
        }
        byteBuffer = com.badlogic.gdx.utils.BufferUtils.newUnsafeByteBuffer(maxIndices * 2)
        buffer = byteBuffer.asShortBuffer()
        buffer.flip()
        byteBuffer.flip()
    }
}
