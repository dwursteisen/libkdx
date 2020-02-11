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
package com.badlogic.gdx.utils

import com.badlogic.gdx.utils.BooleanArray
import com.badlogic.gdx.utils.ByteArray
import java.lang.IllegalStateException
import java.lang.IndexOutOfBoundsException

/**
 * Class with static helper methods to increase the speed of array/direct buffer and direct buffer/direct buffer transfers
 *
 * @author mzechner, xoppa
 */
object BufferUtils {

    var unsafeBuffers: Array<ByteBuffer> = Array<ByteBuffer>()

    /**
     * @return the number of bytes allocated with [.newUnsafeByteBuffer]
     */
    var allocatedBytesUnsafe = 0

    /**
     * Copies numFloats floats from src starting at offset to dst. Dst is assumed to be a direct [Buffer]. The method will
     * crash if that is not the case. The position and limit of the buffer are ignored, the copy is placed at position 0 in the
     * buffer. After the copying process the position of the buffer is set to 0 and its limit is set to numFloats * 4 if it is a
     * ByteBuffer and numFloats if it is a FloatBuffer. In case the Buffer is neither a ByteBuffer nor a FloatBuffer the limit is
     * not set. This is an expert method, use at your own risk.
     *
     * @param src       the source array
     * @param dst       the destination buffer, has to be a direct Buffer
     * @param numFloats the number of floats to copy
     * @param offset    the offset in src to start copying from
     */
    fun copy(src: FloatArray, dst: Buffer, numFloats: Int, offset: Int) {
        if (dst is ByteBuffer) dst.limit(numFloats shl 2) else if (dst is FloatBuffer) dst.limit(numFloats)
        copyJni(src, dst, numFloats, offset)
        dst.position(0)
    }

    /**
     * Copies the contents of src to dst, starting from src[srcOffset], copying numElements elements. The [Buffer]
     * instance's [Buffer.position] is used to define the offset into the Buffer itself. The position will stay the same,
     * the limit will be set to position + numElements. **The Buffer must be a direct Buffer with native byte order. No error
     * checking is performed**.
     *
     * @param src         the source array.
     * @param srcOffset   the offset into the source array.
     * @param dst         the destination Buffer, its position is used as an offset.
     * @param numElements the number of elements to copy.
     */
    fun copy(src: ByteArray?, srcOffset: Int, dst: Buffer, numElements: Int) {
        dst.limit(dst.position() + bytesToElements(dst, numElements))
        copyJni(src, srcOffset, dst, positionInBytes(dst), numElements)
    }

    /**
     * Copies the contents of src to dst, starting from src[srcOffset], copying numElements elements. The [Buffer]
     * instance's [Buffer.position] is used to define the offset into the Buffer itself. The position will stay the same,
     * the limit will be set to position + numElements. **The Buffer must be a direct Buffer with native byte order. No error
     * checking is performed**.
     *
     * @param src         the source array.
     * @param srcOffset   the offset into the source array.
     * @param dst         the destination Buffer, its position is used as an offset.
     * @param numElements the number of elements to copy.
     */
    fun copy(src: ShortArray?, srcOffset: Int, dst: Buffer, numElements: Int) {
        dst.limit(dst.position() + bytesToElements(dst, numElements shl 1))
        copyJni(src, srcOffset, dst, positionInBytes(dst), numElements shl 1)
    }

    /**
     * Copies the contents of src to dst, starting from src[srcOffset], copying numElements elements. The [Buffer]
     * instance's [Buffer.position] is used to define the offset into the Buffer itself. The position and limit will stay
     * the same. **The Buffer must be a direct Buffer with native byte order. No error checking is performed**.
     *
     * @param src         the source array.
     * @param srcOffset   the offset into the source array.
     * @param numElements the number of elements to copy.
     * @param dst         the destination Buffer, its position is used as an offset.
     */
    fun copy(src: CharArray?, srcOffset: Int, numElements: Int, dst: Buffer) {
        copyJni(src, srcOffset, dst, positionInBytes(dst), numElements shl 1)
    }

    /**
     * Copies the contents of src to dst, starting from src[srcOffset], copying numElements elements. The [Buffer]
     * instance's [Buffer.position] is used to define the offset into the Buffer itself. The position and limit will stay
     * the same. **The Buffer must be a direct Buffer with native byte order. No error checking is performed**.
     *
     * @param src         the source array.
     * @param srcOffset   the offset into the source array.
     * @param numElements the number of elements to copy.
     * @param dst         the destination Buffer, its position is used as an offset.
     */
    fun copy(src: IntArray?, srcOffset: Int, numElements: Int, dst: Buffer) {
        copyJni(src, srcOffset, dst, positionInBytes(dst), numElements shl 2)
    }

    /**
     * Copies the contents of src to dst, starting from src[srcOffset], copying numElements elements. The [Buffer]
     * instance's [Buffer.position] is used to define the offset into the Buffer itself. The position and limit will stay
     * the same. **The Buffer must be a direct Buffer with native byte order. No error checking is performed**.
     *
     * @param src         the source array.
     * @param srcOffset   the offset into the source array.
     * @param numElements the number of elements to copy.
     * @param dst         the destination Buffer, its position is used as an offset.
     */
    fun copy(src: LongArray?, srcOffset: Int, numElements: Int, dst: Buffer) {
        copyJni(src, srcOffset, dst, positionInBytes(dst), numElements shl 3)
    }

    /**
     * Copies the contents of src to dst, starting from src[srcOffset], copying numElements elements. The [Buffer]
     * instance's [Buffer.position] is used to define the offset into the Buffer itself. The position and limit will stay
     * the same. **The Buffer must be a direct Buffer with native byte order. No error checking is performed**.
     *
     * @param src         the source array.
     * @param srcOffset   the offset into the source array.
     * @param numElements the number of elements to copy.
     * @param dst         the destination Buffer, its position is used as an offset.
     */
    fun copy(src: FloatArray?, srcOffset: Int, numElements: Int, dst: Buffer) {
        copyJni(src, srcOffset, dst, positionInBytes(dst), numElements shl 2)
    }

    /**
     * Copies the contents of src to dst, starting from src[srcOffset], copying numElements elements. The [Buffer]
     * instance's [Buffer.position] is used to define the offset into the Buffer itself. The position and limit will stay
     * the same. **The Buffer must be a direct Buffer with native byte order. No error checking is performed**.
     *
     * @param src         the source array.
     * @param srcOffset   the offset into the source array.
     * @param numElements the number of elements to copy.
     * @param dst         the destination Buffer, its position is used as an offset.
     */
    fun copy(src: DoubleArray?, srcOffset: Int, numElements: Int, dst: Buffer) {
        copyJni(src, srcOffset, dst, positionInBytes(dst), numElements shl 3)
    }

    /**
     * Copies the contents of src to dst, starting from src[srcOffset], copying numElements elements. The [Buffer]
     * instance's [Buffer.position] is used to define the offset into the Buffer itself. The position will stay the same,
     * the limit will be set to position + numElements. **The Buffer must be a direct Buffer with native byte order. No error
     * checking is performed**.
     *
     * @param src         the source array.
     * @param srcOffset   the offset into the source array.
     * @param dst         the destination Buffer, its position is used as an offset.
     * @param numElements the number of elements to copy.
     */
    fun copy(src: CharArray?, srcOffset: Int, dst: Buffer, numElements: Int) {
        dst.limit(dst.position() + bytesToElements(dst, numElements shl 1))
        copyJni(src, srcOffset, dst, positionInBytes(dst), numElements shl 1)
    }

    /**
     * Copies the contents of src to dst, starting from src[srcOffset], copying numElements elements. The [Buffer]
     * instance's [Buffer.position] is used to define the offset into the Buffer itself. The position will stay the same,
     * the limit will be set to position + numElements. **The Buffer must be a direct Buffer with native byte order. No error
     * checking is performed**.
     *
     * @param src         the source array.
     * @param srcOffset   the offset into the source array.
     * @param dst         the destination Buffer, its position is used as an offset.
     * @param numElements the number of elements to copy.
     */
    fun copy(src: IntArray?, srcOffset: Int, dst: Buffer, numElements: Int) {
        dst.limit(dst.position() + bytesToElements(dst, numElements shl 2))
        copyJni(src, srcOffset, dst, positionInBytes(dst), numElements shl 2)
    }

    /**
     * Copies the contents of src to dst, starting from src[srcOffset], copying numElements elements. The [Buffer]
     * instance's [Buffer.position] is used to define the offset into the Buffer itself. The position will stay the same,
     * the limit will be set to position + numElements. **The Buffer must be a direct Buffer with native byte order. No error
     * checking is performed**.
     *
     * @param src         the source array.
     * @param srcOffset   the offset into the source array.
     * @param dst         the destination Buffer, its position is used as an offset.
     * @param numElements the number of elements to copy.
     */
    fun copy(src: LongArray?, srcOffset: Int, dst: Buffer, numElements: Int) {
        dst.limit(dst.position() + bytesToElements(dst, numElements shl 3))
        copyJni(src, srcOffset, dst, positionInBytes(dst), numElements shl 3)
    }

    /**
     * Copies the contents of src to dst, starting from src[srcOffset], copying numElements elements. The [Buffer]
     * instance's [Buffer.position] is used to define the offset into the Buffer itself. The position will stay the same,
     * the limit will be set to position + numElements. **The Buffer must be a direct Buffer with native byte order. No error
     * checking is performed**.
     *
     * @param src         the source array.
     * @param srcOffset   the offset into the source array.
     * @param dst         the destination Buffer, its position is used as an offset.
     * @param numElements the number of elements to copy.
     */
    fun copy(src: FloatArray?, srcOffset: Int, dst: Buffer, numElements: Int) {
        dst.limit(dst.position() + bytesToElements(dst, numElements shl 2))
        copyJni(src, srcOffset, dst, positionInBytes(dst), numElements shl 2)
    }

    /**
     * Copies the contents of src to dst, starting from src[srcOffset], copying numElements elements. The [Buffer]
     * instance's [Buffer.position] is used to define the offset into the Buffer itself. The position will stay the same,
     * the limit will be set to position + numElements. **The Buffer must be a direct Buffer with native byte order. No error
     * checking is performed**.
     *
     * @param src         the source array.
     * @param srcOffset   the offset into the source array.
     * @param dst         the destination Buffer, its position is used as an offset.
     * @param numElements the number of elements to copy.
     */
    fun copy(src: DoubleArray?, srcOffset: Int, dst: Buffer, numElements: Int) {
        dst.limit(dst.position() + bytesToElements(dst, numElements shl 3))
        copyJni(src, srcOffset, dst, positionInBytes(dst), numElements shl 3)
    }

    /**
     * Copies the contents of src to dst, starting from the current position of src, copying numElements elements (using the data
     * type of src, no matter the datatype of dst). The dst [Buffer.position] is used as the writing offset. The position
     * of both Buffers will stay the same. The limit of the src Buffer will stay the same. The limit of the dst Buffer will be set
     * to dst.position() + numElements, where numElements are translated to the number of elements appropriate for the dst Buffer
     * data type. **The Buffers must be direct Buffers with native byte order. No error checking is performed**.
     *
     * @param src         the source Buffer.
     * @param dst         the destination Buffer.
     * @param numElements the number of elements to copy.
     */
    fun copy(src: Buffer, dst: Buffer, numElements: Int) {
        val numBytes = elementsToBytes(src, numElements)
        dst.limit(dst.position() + bytesToElements(dst, numBytes))
        copyJni(src, positionInBytes(src), dst, positionInBytes(dst), numBytes)
    }

    /**
     * Multiply float vector components within the buffer with the specified matrix. The [Buffer.position] is used as the
     * offset.
     *
     * @param data          The buffer to transform.
     * @param dimensions    The number of components of the vector (2 for xy, 3 for xyz or 4 for xyzw)
     * @param strideInBytes The offset between the first and the second vector to transform
     * @param count         The number of vectors to transform
     * @param matrix        The matrix to multiply the vector with
     */
    fun transform(data: Buffer?, dimensions: Int, strideInBytes: Int, count: Int, matrix: Matrix4?) {
        transform(data, dimensions, strideInBytes, count, matrix, 0)
    }

    /**
     * Multiply float vector components within the buffer with the specified matrix. The [Buffer.position] is used as the
     * offset.
     *
     * @param data          The buffer to transform.
     * @param dimensions    The number of components of the vector (2 for xy, 3 for xyz or 4 for xyzw)
     * @param strideInBytes The offset between the first and the second vector to transform
     * @param count         The number of vectors to transform
     * @param matrix        The matrix to multiply the vector with
     */
    fun transform(data: FloatArray?, dimensions: Int, strideInBytes: Int, count: Int, matrix: Matrix4?) {
        transform(data, dimensions, strideInBytes, count, matrix, 0)
    }

    /**
     * Multiply float vector components within the buffer with the specified matrix. The specified offset value is added to the
     * [Buffer.position] and used as the offset.
     *
     * @param data          The buffer to transform.
     * @param dimensions    The number of components of the vector (2 for xy, 3 for xyz or 4 for xyzw)
     * @param strideInBytes The offset between the first and the second vector to transform
     * @param count         The number of vectors to transform
     * @param matrix        The matrix to multiply the vector with
     * @param offset        The offset within the buffer (in bytes relative to the current position) to the vector
     */
    fun transform(data: Buffer, dimensions: Int, strideInBytes: Int, count: Int, matrix: Matrix4, offset: Int) {
        when (dimensions) {
            4 -> transformV4M4Jni(data, strideInBytes, count, matrix.`val`, positionInBytes(data) + offset)
            3 -> transformV3M4Jni(data, strideInBytes, count, matrix.`val`, positionInBytes(data) + offset)
            2 -> transformV2M4Jni(data, strideInBytes, count, matrix.`val`, positionInBytes(data) + offset)
            else -> throw java.lang.IllegalArgumentException()
        }
    }

    /**
     * Multiply float vector components within the buffer with the specified matrix. The specified offset value is added to the
     * [Buffer.position] and used as the offset.
     *
     * @param data          The buffer to transform.
     * @param dimensions    The number of components of the vector (2 for xy, 3 for xyz or 4 for xyzw)
     * @param strideInBytes The offset between the first and the second vector to transform
     * @param count         The number of vectors to transform
     * @param matrix        The matrix to multiply the vector with
     * @param offset        The offset within the buffer (in bytes relative to the current position) to the vector
     */
    fun transform(data: FloatArray?, dimensions: Int, strideInBytes: Int, count: Int, matrix: Matrix4, offset: Int) {
        when (dimensions) {
            4 -> transformV4M4Jni(data, strideInBytes, count, matrix.`val`, offset)
            3 -> transformV3M4Jni(data, strideInBytes, count, matrix.`val`, offset)
            2 -> transformV2M4Jni(data, strideInBytes, count, matrix.`val`, offset)
            else -> throw java.lang.IllegalArgumentException()
        }
    }

    /**
     * Multiply float vector components within the buffer with the specified matrix. The [Buffer.position] is used as the
     * offset.
     *
     * @param data          The buffer to transform.
     * @param dimensions    The number of components (x, y, z) of the vector (2 for xy or 3 for xyz)
     * @param strideInBytes The offset between the first and the second vector to transform
     * @param count         The number of vectors to transform
     * @param matrix        The matrix to multiply the vector with
     */
    fun transform(data: Buffer?, dimensions: Int, strideInBytes: Int, count: Int, matrix: Matrix3?) {
        transform(data, dimensions, strideInBytes, count, matrix, 0)
    }

    /**
     * Multiply float vector components within the buffer with the specified matrix. The [Buffer.position] is used as the
     * offset.
     *
     * @param data          The buffer to transform.
     * @param dimensions    The number of components (x, y, z) of the vector (2 for xy or 3 for xyz)
     * @param strideInBytes The offset between the first and the second vector to transform
     * @param count         The number of vectors to transform
     * @param matrix        The matrix to multiply the vector with
     */
    fun transform(data: FloatArray?, dimensions: Int, strideInBytes: Int, count: Int, matrix: Matrix3?) {
        transform(data, dimensions, strideInBytes, count, matrix, 0)
    }

    /**
     * Multiply float vector components within the buffer with the specified matrix. The specified offset value is added to the
     * [Buffer.position] and used as the offset.
     *
     * @param data          The buffer to transform.
     * @param dimensions    The number of components (x, y, z) of the vector (2 for xy or 3 for xyz)
     * @param strideInBytes The offset between the first and the second vector to transform
     * @param count         The number of vectors to transform
     * @param matrix        The matrix to multiply the vector with,
     * @param offset        The offset within the buffer (in bytes relative to the current position) to the vector
     */
    fun transform(data: Buffer, dimensions: Int, strideInBytes: Int, count: Int, matrix: Matrix3, offset: Int) {
        when (dimensions) {
            3 -> transformV3M3Jni(data, strideInBytes, count, matrix.`val`, positionInBytes(data) + offset)
            2 -> transformV2M3Jni(data, strideInBytes, count, matrix.`val`, positionInBytes(data) + offset)
            else -> throw java.lang.IllegalArgumentException()
        }
    }

    /**
     * Multiply float vector components within the buffer with the specified matrix. The specified offset value is added to the
     * [Buffer.position] and used as the offset.
     *
     * @param data          The buffer to transform.
     * @param dimensions    The number of components (x, y, z) of the vector (2 for xy or 3 for xyz)
     * @param strideInBytes The offset between the first and the second vector to transform
     * @param count         The number of vectors to transform
     * @param matrix        The matrix to multiply the vector with,
     * @param offset        The offset within the buffer (in bytes relative to the current position) to the vector
     */
    fun transform(data: FloatArray?, dimensions: Int, strideInBytes: Int, count: Int, matrix: Matrix3, offset: Int) {
        when (dimensions) {
            3 -> transformV3M3Jni(data, strideInBytes, count, matrix.`val`, offset)
            2 -> transformV2M3Jni(data, strideInBytes, count, matrix.`val`, offset)
            else -> throw java.lang.IllegalArgumentException()
        }
    }

    fun findFloats(vertex: Buffer, strideInBytes: Int, vertices: Buffer, numVertices: Int): Long {
        return find(vertex, positionInBytes(vertex), strideInBytes, vertices, positionInBytes(vertices), numVertices)
    }

    fun findFloats(vertex: FloatArray?, strideInBytes: Int, vertices: Buffer, numVertices: Int): Long {
        return find(vertex, 0, strideInBytes, vertices, positionInBytes(vertices), numVertices)
    }

    fun findFloats(vertex: Buffer, strideInBytes: Int, vertices: FloatArray?, numVertices: Int): Long {
        return find(vertex, positionInBytes(vertex), strideInBytes, vertices, 0, numVertices)
    }

    fun findFloats(vertex: FloatArray?, strideInBytes: Int, vertices: FloatArray?, numVertices: Int): Long {
        return find(vertex, 0, strideInBytes, vertices, 0, numVertices)
    }

    fun findFloats(vertex: Buffer, strideInBytes: Int, vertices: Buffer, numVertices: Int, epsilon: Float): Long {
        return find(vertex, positionInBytes(vertex), strideInBytes, vertices, positionInBytes(vertices), numVertices, epsilon)
    }

    fun findFloats(vertex: FloatArray?, strideInBytes: Int, vertices: Buffer, numVertices: Int, epsilon: Float): Long {
        return find(vertex, 0, strideInBytes, vertices, positionInBytes(vertices), numVertices, epsilon)
    }

    fun findFloats(vertex: Buffer, strideInBytes: Int, vertices: FloatArray?, numVertices: Int, epsilon: Float): Long {
        return find(vertex, positionInBytes(vertex), strideInBytes, vertices, 0, numVertices, epsilon)
    }

    fun findFloats(vertex: FloatArray?, strideInBytes: Int, vertices: FloatArray?, numVertices: Int, epsilon: Float): Long {
        return find(vertex, 0, strideInBytes, vertices, 0, numVertices, epsilon)
    }

    private fun positionInBytes(dst: Buffer): Int {
        return if (dst is ByteBuffer) dst.position() else if (dst is ShortBuffer) dst.position() shl 1 else if (dst is CharBuffer) dst.position() shl 1 else if (dst is IntBuffer) dst.position() shl 2 else if (dst is LongBuffer) dst.position() shl 3 else if (dst is FloatBuffer) dst.position() shl 2 else if (dst is DoubleBuffer) dst.position() shl 3 else throw GdxRuntimeException("Can't copy to a " + dst.getClass().getName().toString() + " instance")
    }

    private fun bytesToElements(dst: Buffer, bytes: Int): Int {
        return if (dst is ByteBuffer) bytes else if (dst is ShortBuffer) bytes ushr 1 else if (dst is CharBuffer) bytes ushr 1 else if (dst is IntBuffer) bytes ushr 2 else if (dst is LongBuffer) bytes ushr 3 else if (dst is FloatBuffer) bytes ushr 2 else if (dst is DoubleBuffer) bytes ushr 3 else throw GdxRuntimeException("Can't copy to a " + dst.getClass().getName().toString() + " instance")
    }

    private fun elementsToBytes(dst: Buffer, elements: Int): Int {
        return if (dst is ByteBuffer) elements else if (dst is ShortBuffer) elements shl 1 else if (dst is CharBuffer) elements shl 1 else if (dst is IntBuffer) elements shl 2 else if (dst is LongBuffer) elements shl 3 else if (dst is FloatBuffer) elements shl 2 else if (dst is DoubleBuffer) elements shl 3 else throw GdxRuntimeException("Can't copy to a " + dst.getClass().getName().toString() + " instance")
    }

    fun newFloatBuffer(numFloats: Int): FloatBuffer {
        val buffer: ByteBuffer = ByteBuffer.allocateDirect(numFloats * 4)
        buffer.order(ByteOrder.nativeOrder())
        return buffer.asFloatBuffer()
    }

    fun newDoubleBuffer(numDoubles: Int): DoubleBuffer {
        val buffer: ByteBuffer = ByteBuffer.allocateDirect(numDoubles * 8)
        buffer.order(ByteOrder.nativeOrder())
        return buffer.asDoubleBuffer()
    }

    fun newByteBuffer(numBytes: Int): ByteBuffer {
        val buffer: ByteBuffer = ByteBuffer.allocateDirect(numBytes)
        buffer.order(ByteOrder.nativeOrder())
        return buffer
    }

    fun newShortBuffer(numShorts: Int): ShortBuffer {
        val buffer: ByteBuffer = ByteBuffer.allocateDirect(numShorts * 2)
        buffer.order(ByteOrder.nativeOrder())
        return buffer.asShortBuffer()
    }

    fun newCharBuffer(numChars: Int): CharBuffer {
        val buffer: ByteBuffer = ByteBuffer.allocateDirect(numChars * 2)
        buffer.order(ByteOrder.nativeOrder())
        return buffer.asCharBuffer()
    }

    fun newIntBuffer(numInts: Int): IntBuffer {
        val buffer: ByteBuffer = ByteBuffer.allocateDirect(numInts * 4)
        buffer.order(ByteOrder.nativeOrder())
        return buffer.asIntBuffer()
    }

    fun newLongBuffer(numLongs: Int): LongBuffer {
        val buffer: ByteBuffer = ByteBuffer.allocateDirect(numLongs * 8)
        buffer.order(ByteOrder.nativeOrder())
        return buffer.asLongBuffer()
    }

    fun disposeUnsafeByteBuffer(buffer: ByteBuffer) {
        val size: Int = buffer.capacity()
        synchronized(unsafeBuffers) { if (!unsafeBuffers.removeValue(buffer, true)) throw java.lang.IllegalArgumentException("buffer not allocated with newUnsafeByteBuffer or already disposed") }
        allocatedBytesUnsafe -= size
        freeMemory(buffer)
    }

    fun isUnsafeByteBuffer(buffer: ByteBuffer?): Boolean {
        synchronized(unsafeBuffers) { return unsafeBuffers.contains(buffer, true) }
    }

    /**
     * Allocates a new direct ByteBuffer from native heap memory using the native byte order. Needs to be disposed with
     * [.disposeUnsafeByteBuffer].
     */
    fun newUnsafeByteBuffer(numBytes: Int): ByteBuffer {
        val buffer: ByteBuffer = newDisposableByteBuffer(numBytes)
        buffer.order(ByteOrder.nativeOrder())
        allocatedBytesUnsafe += numBytes
        synchronized(unsafeBuffers) { unsafeBuffers.add(buffer) }
        return buffer
    }

    /**
     * Returns the address of the Buffer, it assumes it is an unsafe buffer.
     *
     * @param buffer The Buffer to ask the address for.
     * @return the address of the Buffer.
     */
    fun getUnsafeBufferAddress(buffer: Buffer): Long {
        return getBufferAddress(buffer) + buffer.position()
    }

    /**
     * Registers the given ByteBuffer as an unsafe ByteBuffer. The ByteBuffer must have been allocated in native code, pointing to
     * a memory region allocated via malloc. Needs to be disposed with [.disposeUnsafeByteBuffer].
     *
     * @param buffer the [ByteBuffer] to register
     * @return the ByteBuffer passed to the method
     */
    fun newUnsafeByteBuffer(buffer: ByteBuffer): ByteBuffer {
        allocatedBytesUnsafe += buffer.capacity()
        synchronized(unsafeBuffers) { unsafeBuffers.add(buffer) }
        return buffer
    }

    // @off
    /*JNI 
	#include <stdio.h>
	#include <stdlib.h>
	#include <string.h>
	*/
    /**
     * Frees the memory allocated for the ByteBuffer, which MUST have been allocated via [.newUnsafeByteBuffer]
     * or in native code.
     */
    private external fun freeMemory(buffer: ByteBuffer) /*
		free(buffer);
	 */
    private external fun newDisposableByteBuffer(numBytes: Int): ByteBuffer /*
		return env->NewDirectByteBuffer((char*)malloc(numBytes), numBytes);
	*/
    private external fun getBufferAddress(buffer: Buffer): Long /*
	    return (jlong) buffer;
	*/

    /**
     * Writes the specified number of zeros to the buffer. This is generally faster than reallocating a new buffer.
     */
    external fun clear(buffer: ByteBuffer?, numBytes: Int) /*
		memset(buffer, 0, numBytes);
	*/
    private external fun copyJni(src: FloatArray, dst: Buffer, numFloats: Int, offset: Int) /*
		memcpy(dst, src + offset, numFloats << 2 );
	*/
    private external fun copyJni(src: ByteArray, srcOffset: Int, dst: Buffer, dstOffset: Int, numBytes: Int) /*
		memcpy(dst + dstOffset, src + srcOffset, numBytes);
	*/
    private external fun copyJni(src: CharArray, srcOffset: Int, dst: Buffer, dstOffset: Int, numBytes: Int) /*
		memcpy(dst + dstOffset, src + srcOffset, numBytes);
	*/
    private external fun copyJni(src: ShortArray, srcOffset: Int, dst: Buffer, dstOffset: Int, numBytes: Int) /*
		memcpy(dst + dstOffset, src + srcOffset, numBytes);
	 */
    private external fun copyJni(src: IntArray, srcOffset: Int, dst: Buffer, dstOffset: Int, numBytes: Int) /*
		memcpy(dst + dstOffset, src + srcOffset, numBytes);
	*/
    private external fun copyJni(src: LongArray, srcOffset: Int, dst: Buffer, dstOffset: Int, numBytes: Int) /*
		memcpy(dst + dstOffset, src + srcOffset, numBytes);
	*/
    private external fun copyJni(src: FloatArray, srcOffset: Int, dst: Buffer, dstOffset: Int, numBytes: Int) /*
		memcpy(dst + dstOffset, src + srcOffset, numBytes);
	*/
    private external fun copyJni(src: DoubleArray, srcOffset: Int, dst: Buffer, dstOffset: Int, numBytes: Int) /*
		memcpy(dst + dstOffset, src + srcOffset, numBytes);
	*/
    private external fun copyJni(src: Buffer, srcOffset: Int, dst: Buffer, dstOffset: Int, numBytes: Int) /*
		memcpy(dst + dstOffset, src + srcOffset, numBytes);
	*/

    /*JNI
	template<size_t n1, size_t n2> void transform(float * const &src, float * const &m, float * const &dst) {}
	
	template<> inline void transform<4, 4>(float * const &src, float * const &m, float * const &dst) {
		const float x = src[0], y = src[1], z = src[2], w = src[3];
		dst[0] = x * m[ 0] + y * m[ 4] + z * m[ 8] + w * m[12]; 
		dst[1] = x * m[ 1] + y * m[ 5] + z * m[ 9] + w * m[13];
		dst[2] = x * m[ 2] + y * m[ 6] + z * m[10] + w * m[14];
		dst[3] = x * m[ 3] + y * m[ 7] + z * m[11] + w * m[15]; 
	}
	
	template<> inline void transform<3, 4>(float * const &src, float * const &m, float * const &dst) {
		const float x = src[0], y = src[1], z = src[2];
		dst[0] = x * m[ 0] + y * m[ 4] + z * m[ 8] + m[12]; 
		dst[1] = x * m[ 1] + y * m[ 5] + z * m[ 9] + m[13];
		dst[2] = x * m[ 2] + y * m[ 6] + z * m[10] + m[14]; 
	}
	
	template<> inline void transform<2, 4>(float * const &src, float * const &m, float * const &dst) {
		const float x = src[0], y = src[1];
		dst[0] = x * m[ 0] + y * m[ 4] + m[12]; 
		dst[1] = x * m[ 1] + y * m[ 5] + m[13]; 
	}
	
	template<> inline void transform<3, 3>(float * const &src, float * const &m, float * const &dst) {
		const float x = src[0], y = src[1], z = src[2];
		dst[0] = x * m[0] + y * m[3] + z * m[6]; 
		dst[1] = x * m[1] + y * m[4] + z * m[7];
		dst[2] = x * m[2] + y * m[5] + z * m[8]; 
	}
	
	template<> inline void transform<2, 3>(float * const &src, float * const &m, float * const &dst) {
		const float x = src[0], y = src[1];
		dst[0] = x * m[0] + y * m[3] + m[6]; 
		dst[1] = x * m[1] + y * m[4] + m[7]; 
	}
	
	template<size_t n1, size_t n2> void transform(float * const &v, int const &stride, int const &count, float * const &m, int offset) {
		for (int i = 0; i < count; i++) {
			transform<n1, n2>(&v[offset], m, &v[offset]);
			offset += stride;
		}
	}
	
	template<size_t n1, size_t n2> void transform(float * const &v, int const &stride, unsigned short * const &indices, int const &count, float * const &m, int offset) {
		for (int i = 0; i < count; i++) {
			transform<n1, n2>(&v[offset], m, &v[offset]);
			offset += stride;
		}
	}
	
	inline bool compare(float * const &lhs, float * const & rhs, const unsigned int &size, const float &epsilon) {
   	for (unsigned int i = 0; i < size; i++)
   		if ((*(unsigned int*)&lhs[i] != *(unsigned int*)&rhs[i]) && ((lhs[i] > rhs[i] ? lhs[i] - rhs[i] : rhs[i] - lhs[i]) > epsilon))
         	return false;
		return true;
	}
	
	long find(float * const &vertex, const unsigned int &size, float * const &vertices, const unsigned int &count, const float &epsilon) {
		for (unsigned int i = 0; i < count; i++)
			if (compare(&vertices[i*size], vertex, size, epsilon))
				return (long)i;
		return -1;
	}

	inline bool compare(float * const &lhs, float * const & rhs, const unsigned int &size) {
   	for (unsigned int i = 0; i < size; i++)
      	if ((*(unsigned int*)&lhs[i] != *(unsigned int*)&rhs[i]) && lhs[i] != rhs[i])
         	return false;
		return true;
	}
	
	long find(float * const &vertex, const unsigned int &size, float * const &vertices, const unsigned int &count) {
		for (unsigned int i = 0; i < count; i++)
			if (compare(&vertices[i*size], vertex, size))
				return (long)i;
		return -1;
	}

	inline unsigned int calcHash(float * const &vertex, const unsigned int &size) {
		unsigned int result = 0;
		for (unsigned int i = 0; i < size; ++i)
			result += ((*((unsigned int *)&vertex[i])) & 0xffffff80) >> (i & 0x7);
		return result & 0x7fffffff;
	}
	
	long find(float * const &vertex, const unsigned int &size, float * const &vertices, unsigned int * const &hashes, const unsigned int &count) {
		const unsigned int hash = calcHash(vertex, size);
		for (unsigned int i = 0; i < count; i++)
			if (hashes[i] == hash && compare(&vertices[i*size], vertex, size))
				return (long)i;
		return -1;
	}
	*/
    private external fun transformV4M4Jni(data: Buffer, strideInBytes: Int, count: Int, matrix: FloatArray, offsetInBytes: Int) /*
		transform<4, 4>((float*)data, strideInBytes / 4, count, (float*)matrix, offsetInBytes / 4);  
	*/
    private external fun transformV4M4Jni(data: FloatArray, strideInBytes: Int, count: Int, matrix: FloatArray, offsetInBytes: Int) /*
		transform<4, 4>((float*)data, strideInBytes / 4, count, (float*)matrix, offsetInBytes / 4);  
	*/
    private external fun transformV3M4Jni(data: Buffer, strideInBytes: Int, count: Int, matrix: FloatArray, offsetInBytes: Int) /*
		transform<3, 4>((float*)data, strideInBytes / 4, count, (float*)matrix, offsetInBytes / 4);
	*/
    private external fun transformV3M4Jni(data: FloatArray, strideInBytes: Int, count: Int, matrix: FloatArray, offsetInBytes: Int) /*
		transform<3, 4>((float*)data, strideInBytes / 4, count, (float*)matrix, offsetInBytes / 4);
	*/
    private external fun transformV2M4Jni(data: Buffer, strideInBytes: Int, count: Int, matrix: FloatArray, offsetInBytes: Int) /*
		transform<2, 4>((float*)data, strideInBytes / 4, count, (float*)matrix, offsetInBytes / 4);
	*/
    private external fun transformV2M4Jni(data: FloatArray, strideInBytes: Int, count: Int, matrix: FloatArray, offsetInBytes: Int) /*
		transform<2, 4>((float*)data, strideInBytes / 4, count, (float*)matrix, offsetInBytes / 4);
	*/
    private external fun transformV3M3Jni(data: Buffer, strideInBytes: Int, count: Int, matrix: FloatArray, offsetInBytes: Int) /*
		transform<3, 3>((float*)data, strideInBytes / 4, count, (float*)matrix, offsetInBytes / 4);
	*/
    private external fun transformV3M3Jni(data: FloatArray, strideInBytes: Int, count: Int, matrix: FloatArray, offsetInBytes: Int) /*
		transform<3, 3>((float*)data, strideInBytes / 4, count, (float*)matrix, offsetInBytes / 4);
	*/
    private external fun transformV2M3Jni(data: Buffer, strideInBytes: Int, count: Int, matrix: FloatArray, offsetInBytes: Int) /*
		transform<2, 3>((float*)data, strideInBytes / 4, count, (float*)matrix, offsetInBytes / 4);
	*/
    private external fun transformV2M3Jni(data: FloatArray, strideInBytes: Int, count: Int, matrix: FloatArray, offsetInBytes: Int) /*
		transform<2, 3>((float*)data, strideInBytes / 4, count, (float*)matrix, offsetInBytes / 4);
	*/
    private external fun find(vertex: Buffer, vertexOffsetInBytes: Int, strideInBytes: Int, vertices: Buffer, verticesOffsetInBytes: Int, numVertices: Int): Long /*
		return find((float *)&vertex[vertexOffsetInBytes / 4], (unsigned int)(strideInBytes / 4), (float*)&vertices[verticesOffsetInBytes / 4], (unsigned int)numVertices);
	*/
    private external fun find(vertex: FloatArray, vertexOffsetInBytes: Int, strideInBytes: Int, vertices: Buffer, verticesOffsetInBytes: Int, numVertices: Int): Long /*
		return find((float *)&vertex[vertexOffsetInBytes / 4], (unsigned int)(strideInBytes / 4), (float*)&vertices[verticesOffsetInBytes / 4], (unsigned int)numVertices);
	*/
    private external fun find(vertex: Buffer, vertexOffsetInBytes: Int, strideInBytes: Int, vertices: FloatArray, verticesOffsetInBytes: Int, numVertices: Int): Long /*
		return find((float *)&vertex[vertexOffsetInBytes / 4], (unsigned int)(strideInBytes / 4), (float*)&vertices[verticesOffsetInBytes / 4], (unsigned int)numVertices);
	*/
    private external fun find(vertex: FloatArray, vertexOffsetInBytes: Int, strideInBytes: Int, vertices: FloatArray, verticesOffsetInBytes: Int, numVertices: Int): Long /*
		return find((float *)&vertex[vertexOffsetInBytes / 4], (unsigned int)(strideInBytes / 4), (float*)&vertices[verticesOffsetInBytes / 4], (unsigned int)numVertices);
	*/
    private external fun find(vertex: Buffer, vertexOffsetInBytes: Int, strideInBytes: Int, vertices: Buffer, verticesOffsetInBytes: Int, numVertices: Int, epsilon: Float): Long /*
		return find((float *)&vertex[vertexOffsetInBytes / 4], (unsigned int)(strideInBytes / 4), (float*)&vertices[verticesOffsetInBytes / 4], (unsigned int)numVertices, epsilon);
	*/
    private external fun find(vertex: FloatArray, vertexOffsetInBytes: Int, strideInBytes: Int, vertices: Buffer, verticesOffsetInBytes: Int, numVertices: Int, epsilon: Float): Long /*
		return find((float *)&vertex[vertexOffsetInBytes / 4], (unsigned int)(strideInBytes / 4), (float*)&vertices[verticesOffsetInBytes / 4], (unsigned int)numVertices, epsilon);
	*/
    private external fun find(vertex: Buffer, vertexOffsetInBytes: Int, strideInBytes: Int, vertices: FloatArray, verticesOffsetInBytes: Int, numVertices: Int, epsilon: Float): Long /*
		return find((float *)&vertex[vertexOffsetInBytes / 4], (unsigned int)(strideInBytes / 4), (float*)&vertices[verticesOffsetInBytes / 4], (unsigned int)numVertices, epsilon);
	*/
    private external fun find(vertex: FloatArray, vertexOffsetInBytes: Int, strideInBytes: Int, vertices: FloatArray, verticesOffsetInBytes: Int, numVertices: Int, epsilon: Float): Long /*
		return find((float *)&vertex[vertexOffsetInBytes / 4], (unsigned int)(strideInBytes / 4), (float*)&vertices[verticesOffsetInBytes / 4], (unsigned int)numVertices, epsilon);
	*/
}
