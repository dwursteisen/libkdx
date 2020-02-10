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

/** An IndexData instance holds index data. Can be either a plain short buffer or an OpenGL buffer object.
 * @author mzechner
 */
interface IndexData : com.badlogic.gdx.utils.Disposable {

    /** @return the number of indices currently stored in this buffer
     */
    val numIndices: Int

    /** @return the maximum number of indices this IndexBufferObject can store.
     */
    val numMaxIndices: Int

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
     * @param indices the index data
     * @param offset the offset to start copying the data from
     * @param count the number of shorts to copy
     */
    fun setIndices(indices: ShortArray?, offset: Int, count: Int)

    /** Copies the specified indices to the indices of this IndexBufferObject, discarding the old indices. Copying start at the
     * current [ShortBuffer.position] of the specified buffer and copied the [ShortBuffer.remaining] amount of
     * indices. This can be called in between calls to [.bind] and [.unbind]. The index data will be updated
     * instantly.
     * @param indices the index data to copy
     */
    fun setIndices(indices: ShortBuffer?)

    /** Update (a portion of) the indices.
     * @param targetOffset offset in indices buffer
     * @param indices the index data
     * @param offset the offset to start copying the data from
     * @param count the number of shorts to copy
     */
    fun updateIndices(targetOffset: Int, indices: ShortArray?, offset: Int, count: Int)

    /**
     *
     *
     * Returns the underlying ShortBuffer. If you modify the buffer contents they wil be uploaded on the call to [.bind].
     * If you need immediate uploading use [.setIndices].
     *
     *
     * @return the underlying short buffer.
     */
    val buffer: ShortBuffer?

    /** Binds this IndexBufferObject for rendering with glDrawElements.  */
    fun bind()

    /** Unbinds this IndexBufferObject.  */
    fun unbind()

    /** Invalidates the IndexBufferObject so a new OpenGL buffer handle is created. Use this in case of a context loss.  */
    fun invalidate()

    /** Disposes this IndexDatat and all its associated OpenGL resources.  */
    override fun dispose()
}
