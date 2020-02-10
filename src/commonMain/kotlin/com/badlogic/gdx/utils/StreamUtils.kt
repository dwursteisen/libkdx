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

import kotlin.jvm.Throws

/**
 * Provides utility methods to copy streams.
 */
object StreamUtils {

    const val DEFAULT_BUFFER_SIZE = 4096
    val EMPTY_BYTES = ByteArray(0)

    /**
     * Allocates a {@value #DEFAULT_BUFFER_SIZE} byte[] for use as a temporary buffer and calls
     * [.copyStream].
     */
    @Throws(IOException::class)
    fun copyStream(input: InputStream?, output: OutputStream?) {
        copyStream(input, output, ByteArray(DEFAULT_BUFFER_SIZE))
    }

    /**
     * Allocates a byte[] of the specified size for use as a temporary buffer and calls
     * [.copyStream].
     */
    @Throws(IOException::class)
    fun copyStream(input: InputStream?, output: OutputStream?, bufferSize: Int) {
        copyStream(input, output, ByteArray(bufferSize))
    }

    /**
     * Copy the data from an [InputStream] to an [OutputStream], using the specified byte[] as a temporary buffer. The
     * stream is not closed.
     */
    @Throws(IOException::class)
    fun copyStream(input: InputStream, output: OutputStream, buffer: ByteArray?) {
        var bytesRead: Int
        while (input.read(buffer).also({ bytesRead = it }) != -1) {
            output.write(buffer, 0, bytesRead)
        }
    }

    /**
     * Allocates a {@value #DEFAULT_BUFFER_SIZE} byte[] for use as a temporary buffer and calls
     * [.copyStream].
     */
    @Throws(IOException::class)
    fun copyStream(input: InputStream?, output: ByteBuffer?) {
        copyStream(input, output, ByteArray(DEFAULT_BUFFER_SIZE))
    }

    /**
     * Allocates a byte[] of the specified size for use as a temporary buffer and calls
     * [.copyStream].
     */
    @Throws(IOException::class)
    fun copyStream(input: InputStream?, output: ByteBuffer?, bufferSize: Int) {
        copyStream(input, output, ByteArray(bufferSize))
    }

    /**
     * Copy the data from an [InputStream] to a [ByteBuffer], using the specified byte[] as a temporary buffer. The
     * buffer's limit is increased by the number of bytes copied, the position is left unchanged. The stream is not closed.
     *
     * @param output Must be a direct Buffer with native byte order and the buffer MUST be large enough to hold all the bytes in
     * the stream. No error checking is performed.
     * @return the number of bytes copied.
     */
    @Throws(IOException::class)
    fun copyStream(input: InputStream, output: ByteBuffer, buffer: ByteArray?): Int {
        val startPosition: Int = output.position()
        var total = 0
        var bytesRead: Int
        while (input.read(buffer).also({ bytesRead = it }) != -1) {
            BufferUtils.copy(buffer, 0, output, bytesRead)
            total += bytesRead
            output.position(startPosition + total)
        }
        output.position(startPosition)
        return total
    }

    /**
     * Copy the data from an [InputStream] to a byte array. The stream is not closed.
     */
    @Throws(IOException::class)
    fun copyStreamToByteArray(input: InputStream): ByteArray {
        return copyStreamToByteArray(input, input.available())
    }

    /**
     * Copy the data from an [InputStream] to a byte array. The stream is not closed.
     *
     * @param estimatedSize Used to allocate the output byte[] to possibly avoid an array copy.
     */
    @Throws(IOException::class)
    fun copyStreamToByteArray(input: InputStream?, estimatedSize: Int): ByteArray {
        val baos: ByteArrayOutputStream = OptimizedByteArrayOutputStream(max(0, estimatedSize))
        copyStream(input, baos)
        return baos.toByteArray()
    }

    /**
     * Calls [.copyStreamToString] using the input's [available][InputStream.available] size
     * and the platform's default charset.
     */
    @Throws(IOException::class)
    fun copyStreamToString(input: InputStream): String {
        return copyStreamToString(input, input.available(), null)
    }

    /**
     * Calls [.copyStreamToString] using the platform's default charset.
     */
    @Throws(IOException::class)
    fun copyStreamToString(input: InputStream?, estimatedSize: Int): String {
        return copyStreamToString(input, estimatedSize, null)
    }

    /**
     * Copy the data from an [InputStream] to a string using the specified charset.
     *
     * @param estimatedSize Used to allocate the output buffer to possibly avoid an array copy.
     * @param charset       May be null to use the platform's default charset.
     */
    @Throws(IOException::class)
    fun copyStreamToString(input: InputStream?, estimatedSize: Int, charset: String?): String {
        val reader: InputStreamReader = charset?.let { InputStreamReader(input, it) } ?: InputStreamReader(input)
        val writer = StringWriter(max(0, estimatedSize))
        val buffer = CharArray(DEFAULT_BUFFER_SIZE)
        var charsRead: Int
        while (reader.read(buffer).also({ charsRead = it }) != -1) {
            writer.write(buffer, 0, charsRead)
        }
        return writer.toString()
    }

    /**
     * Close and ignore all errors.
     */
    fun closeQuietly(c: Closeable?) {
        if (c != null) {
            try {
                c.close()
            } catch (ignored: Throwable) {
            }
        }
    }

    /**
     * A ByteArrayOutputStream which avoids copying of the byte array if possible.
     */
    class OptimizedByteArrayOutputStream(initialSize: Int) : ByteArrayOutputStream(initialSize) {

        @Synchronized
        fun toByteArray(): ByteArray {
            return if (count === buf.length) buf else super.toByteArray()
        }

        val buffer: ByteArray
            get() = buf
    }
}
