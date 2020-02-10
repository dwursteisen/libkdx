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

import com.badlogic.gdx.files.FileHandle
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

/** Class for encoding and decoding ETC1 compressed images. Also provides methods to add a PKM header.
 * @author mzechner
 */
object ETC1 {

    /** The PKM header size in bytes  */
    var PKM_HEADER_SIZE = 16
    var ETC1_RGB8_OES = 0x00008d64
    private fun getPixelSize(format: com.badlogic.gdx.graphics.Pixmap.Format?): Int {
        if (format == com.badlogic.gdx.graphics.Pixmap.Format.RGB565) return 2
        if (format == com.badlogic.gdx.graphics.Pixmap.Format.RGB888) return 3
        throw com.badlogic.gdx.utils.GdxRuntimeException("Can only handle RGB565 or RGB888 images")
    }

    /** Encodes the image via the ETC1 compression scheme. Only [Format.RGB565] and [Format.RGB888] are supported.
     * @param pixmap the [Pixmap]
     * @return the [ETC1Data]
     */
    fun encodeImage(pixmap: com.badlogic.gdx.graphics.Pixmap?): ETC1Data? {
        val pixelSize = getPixelSize(pixmap.getFormat())
        val compressedData: java.nio.ByteBuffer? = encodeImage(pixmap.getPixels(), 0, pixmap.getWidth(), pixmap.getHeight(), pixelSize)
        com.badlogic.gdx.utils.BufferUtils.newUnsafeByteBuffer(compressedData)
        return ETC1Data(pixmap.getWidth(), pixmap.getHeight(), compressedData, 0)
    }

    /** Encodes the image via the ETC1 compression scheme. Only [Format.RGB565] and [Format.RGB888] are supported. Adds
     * a PKM header in front of the compressed image data.
     * @param pixmap the [Pixmap]
     * @return the [ETC1Data]
     */
    fun encodeImagePKM(pixmap: com.badlogic.gdx.graphics.Pixmap?): ETC1Data? {
        val pixelSize = getPixelSize(pixmap.getFormat())
        val compressedData: java.nio.ByteBuffer? = encodeImagePKM(pixmap.getPixels(), 0, pixmap.getWidth(), pixmap.getHeight(), pixelSize)
        com.badlogic.gdx.utils.BufferUtils.newUnsafeByteBuffer(compressedData)
        return ETC1Data(pixmap.getWidth(), pixmap.getHeight(), compressedData, 16)
    }

    /** Takes ETC1 compressed image data and converts it to a [Format.RGB565] or [Format.RGB888] [Pixmap]. Does
     * not modify the ByteBuffer's position or limit.
     * @param etc1Data the [ETC1Data] instance
     * @param format either [Format.RGB565] or [Format.RGB888]
     * @return the Pixmap
     */
    fun decodeImage(etc1Data: ETC1Data?, format: com.badlogic.gdx.graphics.Pixmap.Format?): com.badlogic.gdx.graphics.Pixmap? {
        var dataOffset = 0
        var width = 0
        var height = 0
        if (etc1Data!!.hasPKMHeader()) {
            dataOffset = 16
            width = getWidthPKM(etc1Data.compressedData, 0)
            height = getHeightPKM(etc1Data.compressedData, 0)
        } else {
            dataOffset = 0
            width = etc1Data.width
            height = etc1Data.height
        }
        val pixelSize = getPixelSize(format)
        val pixmap: com.badlogic.gdx.graphics.Pixmap = com.badlogic.gdx.graphics.Pixmap(width, height, format)
        decodeImage(etc1Data.compressedData, dataOffset, pixmap.getPixels(), 0, width, height, pixelSize)
        return pixmap
    }
    // @off
/*JNI
	#include <etc1/etc1_utils.h>
	#include <stdlib.h>
	 */
    /** @param width the width in pixels
     * @param height the height in pixels
     * @return the number of bytes needed to store the compressed data
     */
    external fun getCompressedDataSize(width: Int, height: Int): Int /*
		return etc1_get_encoded_data_size(width, height);
	*/

    /** Writes a PKM header to the [ByteBuffer]. Does not modify the position or limit of the ByteBuffer.
     * @param header the direct native order [ByteBuffer]
     * @param offset the offset to the header in bytes
     * @param width the width in pixels
     * @param height the height in pixels
     */
    external fun formatHeader(header: java.nio.ByteBuffer?, offset: Int, width: Int, height: Int) /*
		etc1_pkm_format_header((etc1_byte*)header + offset, width, height);
	*/

    /** @param header direct native order [ByteBuffer] holding the PKM header
     * @param offset the offset in bytes to the PKM header from the ByteBuffer's start
     * @return the width stored in the PKM header
     */
    external fun getWidthPKM(header: java.nio.ByteBuffer?, offset: Int): Int /*
		return etc1_pkm_get_width((etc1_byte*)header + offset);
	*/

    /** @param header direct native order [ByteBuffer] holding the PKM header
     * @param offset the offset in bytes to the PKM header from the ByteBuffer's start
     * @return the height stored in the PKM header
     */
    external fun getHeightPKM(header: java.nio.ByteBuffer?, offset: Int): Int /*
		return etc1_pkm_get_height((etc1_byte*)header + offset);
	*/

    /** @param header direct native order [ByteBuffer] holding the PKM header
     * @param offset the offset in bytes to the PKM header from the ByteBuffer's start
     * @return the width stored in the PKM header
     */
    external fun isValidPKM(header: java.nio.ByteBuffer?, offset: Int): Boolean /*
		return etc1_pkm_is_valid((etc1_byte*)header + offset) != 0?true:false;
	*/

    /** Decodes the compressed image data to RGB565 or RGB888 pixel data. Does not modify the position or limit of the
     * [ByteBuffer] instances.
     * @param compressedData the compressed image data in a direct native order [ByteBuffer]
     * @param offset the offset in bytes to the image data from the start of the buffer
     * @param decodedData the decoded data in a direct native order ByteBuffer, must hold width * height * pixelSize bytes.
     * @param offsetDec the offset in bytes to the decoded image data.
     * @param width the width in pixels
     * @param height the height in pixels
     * @param pixelSize the pixel size, either 2 (RBG565) or 3 (RGB888)
     */
    private external fun decodeImage(compressedData: java.nio.ByteBuffer?, offset: Int, decodedData: java.nio.ByteBuffer?, offsetDec: Int,
                                     width: Int, height: Int, pixelSize: Int) /*
		etc1_decode_image((etc1_byte*)compressedData + offset, (etc1_byte*)decodedData + offsetDec, width, height, pixelSize, width * pixelSize);
	*/

    /** Encodes the image data given as RGB565 or RGB888. Does not modify the position or limit of the [ByteBuffer].
     * @param imageData the image data in a direct native order [ByteBuffer]
     * @param offset the offset in bytes to the image data from the start of the buffer
     * @param width the width in pixels
     * @param height the height in pixels
     * @param pixelSize the pixel size, either 2 (RGB565) or 3 (RGB888)
     * @return a new direct native order ByteBuffer containing the compressed image data
     */
    private external fun encodeImage(imageData: java.nio.ByteBuffer?, offset: Int, width: Int, height: Int, pixelSize: Int): java.nio.ByteBuffer? /*
		int compressedSize = etc1_get_encoded_data_size(width, height);
		etc1_byte* compressedData = (etc1_byte*)malloc(compressedSize);
		etc1_encode_image((etc1_byte*)imageData + offset, width, height, pixelSize, width * pixelSize, compressedData);
		return env->NewDirectByteBuffer(compressedData, compressedSize);
	*/

    /** Encodes the image data given as RGB565 or RGB888. Does not modify the position or limit of the [ByteBuffer].
     * @param imageData the image data in a direct native order [ByteBuffer]
     * @param offset the offset in bytes to the image data from the start of the buffer
     * @param width the width in pixels
     * @param height the height in pixels
     * @param pixelSize the pixel size, either 2 (RGB565) or 3 (RGB888)
     * @return a new direct native order ByteBuffer containing the compressed image data
     */
    private external fun encodeImagePKM(imageData: java.nio.ByteBuffer?, offset: Int, width: Int, height: Int, pixelSize: Int): java.nio.ByteBuffer? /*
		int compressedSize = etc1_get_encoded_data_size(width, height);
		etc1_byte* compressed = (etc1_byte*)malloc(compressedSize + ETC_PKM_HEADER_SIZE);
		etc1_pkm_format_header(compressed, width, height);
		etc1_encode_image((etc1_byte*)imageData + offset, width, height, pixelSize, width * pixelSize, compressed + ETC_PKM_HEADER_SIZE);
		return env->NewDirectByteBuffer(compressed, compressedSize + ETC_PKM_HEADER_SIZE);
	*/

    /** Class for storing ETC1 compressed image data.
     * @author mzechner
     */
    class ETC1Data : com.badlogic.gdx.utils.Disposable {

        /** the width in pixels  */
        val width: Int
        /** the height in pixels  */
        val height: Int
        /** the optional PKM header and compressed image data  */
        val compressedData: java.nio.ByteBuffer?
        /** the offset in bytes to the actual compressed data. Might be 16 if this contains a PKM header, 0 otherwise  */
        val dataOffset: Int

        constructor(width: Int, height: Int, compressedData: java.nio.ByteBuffer?, dataOffset: Int) {
            this.width = width
            this.height = height
            this.compressedData = compressedData
            this.dataOffset = dataOffset
            checkNPOT()
        }

        constructor(pkmFile: FileHandle?) {
            val buffer = ByteArray(1024 * 10)
            var `in`: java.io.DataInputStream? = null
            try {
                `in` = java.io.DataInputStream(BufferedInputStream(GZIPInputStream(pkmFile!!.read())))
                val fileSize: Int = `in`.readInt()
                compressedData = com.badlogic.gdx.utils.BufferUtils.newUnsafeByteBuffer(fileSize)
                var readBytes = 0
                while (`in`.read(buffer).also({ readBytes = it }) != -1) {
                    compressedData.put(buffer, 0, readBytes)
                }
                compressedData.position(0)
                compressedData.limit(compressedData.capacity())
            } catch (e: java.lang.Exception) {
                throw com.badlogic.gdx.utils.GdxRuntimeException("Couldn't load pkm file '$pkmFile'", e)
            } finally {
                com.badlogic.gdx.utils.StreamUtils.closeQuietly(`in`)
            }
            width = getWidthPKM(compressedData, 0)
            height = getHeightPKM(compressedData, 0)
            dataOffset = PKM_HEADER_SIZE
            compressedData.position(dataOffset)
            checkNPOT()
        }

        private fun checkNPOT() {
            if (!com.badlogic.gdx.math.MathUtils.isPowerOfTwo(width) || !com.badlogic.gdx.math.MathUtils.isPowerOfTwo(height)) {
                println("ETC1Data " + "warning: non-power-of-two ETC1 textures may crash the driver of PowerVR GPUs")
            }
        }

        /** @return whether this ETC1Data has a PKM header
         */
        fun hasPKMHeader(): Boolean {
            return dataOffset == 16
        }

        /** Writes the ETC1Data with a PKM header to the given file.
         * @param file the file.
         */
        fun write(file: FileHandle?) {
            var write: java.io.DataOutputStream? = null
            val buffer = ByteArray(10 * 1024)
            var writtenBytes = 0
            compressedData.position(0)
            compressedData.limit(compressedData.capacity())
            try {
                write = java.io.DataOutputStream(GZIPOutputStream(file!!.write(false)))
                write.writeInt(compressedData.capacity())
                while (writtenBytes != compressedData.capacity()) {
                    val bytesToWrite: Int = java.lang.Math.min(compressedData.remaining(), buffer.size)
                    compressedData.get(buffer, 0, bytesToWrite)
                    write.write(buffer, 0, bytesToWrite)
                    writtenBytes += bytesToWrite
                }
            } catch (e: java.lang.Exception) {
                throw com.badlogic.gdx.utils.GdxRuntimeException("Couldn't write PKM file to '$file'", e)
            } finally {
                com.badlogic.gdx.utils.StreamUtils.closeQuietly(write)
            }
            compressedData.position(dataOffset)
            compressedData.limit(compressedData.capacity())
        }

        /** Releases the native resources of the ETC1Data instance.  */
        override fun dispose() {
            com.badlogic.gdx.utils.BufferUtils.disposeUnsafeByteBuffer(compressedData)
        }

        override fun toString(): String {
            return if (hasPKMHeader()) {
                ((if (isValidPKM(compressedData, 0)) "valid" else "invalid") + " pkm [" + getWidthPKM(compressedData, 0)
                    + "x" + getHeightPKM(compressedData, 0) + "], compressed: "
                    + (compressedData.capacity() - PKM_HEADER_SIZE))
            } else {
                "raw [" + width + "x" + height + "], compressed: " + (compressedData.capacity() - PKM_HEADER_SIZE)
            }
        }
    }
}
