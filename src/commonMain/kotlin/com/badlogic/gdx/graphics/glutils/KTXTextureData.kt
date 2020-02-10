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

/** A KTXTextureData holds the data from a KTX (or zipped KTX file, aka ZKTX). That is to say an OpenGL ready texture data. The KTX
 * file format is just a thin wrapper around OpenGL textures and therefore is compatible with most OpenGL texture capabilities
 * like texture compression, cubemapping, mipmapping, etc.
 *
 * For example, KTXTextureData can be used for [Texture] or [Cubemap].
 *
 * @author Vincent Bousquet
 */
class KTXTextureData(// The file we are loading
    private val file: FileHandle?, // Whether to generate mipmaps if they are not included in the file
    private var useMipMaps: Boolean) : com.badlogic.gdx.graphics.TextureData, com.badlogic.gdx.graphics.CubemapData {

    // KTX header (only available after preparing)
    private var glType = 0
    private var glTypeSize = 0
    private var glFormat = 0
    private var glInternalFormat = 0
    private var glBaseInternalFormat = 0
    private var pixelWidth = -1
    private var pixelHeight = -1
    private var pixelDepth = -1
    private var numberOfArrayElements = 0
    private var numberOfFaces = 0
    private var numberOfMipmapLevels = 0
    private var imagePos = 0
    // KTX image data (only available after preparing and before consuming)
    private var compressedData: java.nio.ByteBuffer? = null

    override fun getType(): com.badlogic.gdx.graphics.TextureData.TextureDataType? {
        return com.badlogic.gdx.graphics.TextureData.TextureDataType.Custom
    }

    override fun isPrepared(): Boolean {
        return compressedData != null
    }

    override fun prepare() {
        if (compressedData != null) throw com.badlogic.gdx.utils.GdxRuntimeException("Already prepared")
        if (file == null) throw com.badlogic.gdx.utils.GdxRuntimeException("Need a file to load from")
        // We support normal ktx files as well as 'zktx' which are gzip ktx file with an int length at the beginning (like ETC1).
        if (file.name().endsWith(".zktx")) {
            val buffer = ByteArray(1024 * 10)
            var `in`: java.io.DataInputStream? = null
            try {
                `in` = java.io.DataInputStream(BufferedInputStream(GZIPInputStream(file.read())))
                val fileSize: Int = `in`.readInt()
                compressedData = com.badlogic.gdx.utils.BufferUtils.newUnsafeByteBuffer(fileSize)
                var readBytes = 0
                while (`in`.read(buffer).also({ readBytes = it }) != -1) compressedData.put(buffer, 0, readBytes)
                compressedData.position(0)
                compressedData.limit(compressedData.capacity())
            } catch (e: java.lang.Exception) {
                throw com.badlogic.gdx.utils.GdxRuntimeException("Couldn't load zktx file '$file'", e)
            } finally {
                com.badlogic.gdx.utils.StreamUtils.closeQuietly(`in`)
            }
        } else {
            compressedData = java.nio.ByteBuffer.wrap(file.readBytes())
        }
        if (compressedData.get() != 0x0AB.toByte()) throw com.badlogic.gdx.utils.GdxRuntimeException("Invalid KTX Header")
        if (compressedData.get() != 0x04B.toByte()) throw com.badlogic.gdx.utils.GdxRuntimeException("Invalid KTX Header")
        if (compressedData.get() != 0x054.toByte()) throw com.badlogic.gdx.utils.GdxRuntimeException("Invalid KTX Header")
        if (compressedData.get() != 0x058.toByte()) throw com.badlogic.gdx.utils.GdxRuntimeException("Invalid KTX Header")
        if (compressedData.get() != 0x020.toByte()) throw com.badlogic.gdx.utils.GdxRuntimeException("Invalid KTX Header")
        if (compressedData.get() != 0x031.toByte()) throw com.badlogic.gdx.utils.GdxRuntimeException("Invalid KTX Header")
        if (compressedData.get() != 0x031.toByte()) throw com.badlogic.gdx.utils.GdxRuntimeException("Invalid KTX Header")
        if (compressedData.get() != 0x0BB.toByte()) throw com.badlogic.gdx.utils.GdxRuntimeException("Invalid KTX Header")
        if (compressedData.get() != 0x00D.toByte()) throw com.badlogic.gdx.utils.GdxRuntimeException("Invalid KTX Header")
        if (compressedData.get() != 0x00A.toByte()) throw com.badlogic.gdx.utils.GdxRuntimeException("Invalid KTX Header")
        if (compressedData.get() != 0x01A.toByte()) throw com.badlogic.gdx.utils.GdxRuntimeException("Invalid KTX Header")
        if (compressedData.get() != 0x00A.toByte()) throw com.badlogic.gdx.utils.GdxRuntimeException("Invalid KTX Header")
        val endianTag: Int = compressedData.getInt()
        if (endianTag != 0x04030201 && endianTag != 0x01020304) throw com.badlogic.gdx.utils.GdxRuntimeException("Invalid KTX Header")
        if (endianTag != 0x04030201) compressedData.order(if (compressedData.order() == ByteOrder.BIG_ENDIAN) ByteOrder.LITTLE_ENDIAN else ByteOrder.BIG_ENDIAN)
        glType = compressedData.getInt()
        glTypeSize = compressedData.getInt()
        glFormat = compressedData.getInt()
        glInternalFormat = compressedData.getInt()
        glBaseInternalFormat = compressedData.getInt()
        pixelWidth = compressedData.getInt()
        pixelHeight = compressedData.getInt()
        pixelDepth = compressedData.getInt()
        numberOfArrayElements = compressedData.getInt()
        numberOfFaces = compressedData.getInt()
        numberOfMipmapLevels = compressedData.getInt()
        if (numberOfMipmapLevels == 0) {
            numberOfMipmapLevels = 1
            useMipMaps = true
        }
        val bytesOfKeyValueData: Int = compressedData.getInt()
        imagePos = compressedData.position() + bytesOfKeyValueData
        if (!compressedData.isDirect()) {
            var pos = imagePos
            for (level in 0 until numberOfMipmapLevels) {
                val faceLodSize: Int = compressedData.getInt(pos)
                val faceLodSizeRounded = faceLodSize + 3 and 3.inv()
                pos += faceLodSizeRounded * numberOfFaces + 4
            }
            compressedData.limit(pos)
            compressedData.position(0)
            val directBuffer: java.nio.ByteBuffer = com.badlogic.gdx.utils.BufferUtils.newUnsafeByteBuffer(pos)
            directBuffer.order(compressedData.order())
            directBuffer.put(compressedData)
            compressedData = directBuffer
        }
    }

    override fun consumeCubemapData() {
        consumeCustomData(com.badlogic.gdx.graphics.GL20.GL_TEXTURE_CUBE_MAP)
    }

    override fun consumeCustomData(target: Int) {
        var target = target
        if (compressedData == null) throw com.badlogic.gdx.utils.GdxRuntimeException("Call prepare() before calling consumeCompressedData()")
        val buffer: java.nio.IntBuffer = com.badlogic.gdx.utils.BufferUtils.newIntBuffer(16)
        // Check OpenGL type and format, detect compressed data format (no type & format)
        var compressed = false
        if (glType == 0 || glFormat == 0) {
            if (glType + glFormat != 0) throw com.badlogic.gdx.utils.GdxRuntimeException("either both or none of glType, glFormat must be zero")
            compressed = true
        }
        // find OpenGL texture target and dimensions
        var textureDimensions = 1
        var glTarget = GL_TEXTURE_1D
        if (pixelHeight > 0) {
            textureDimensions = 2
            glTarget = com.badlogic.gdx.graphics.GL20.GL_TEXTURE_2D
        }
        if (pixelDepth > 0) {
            textureDimensions = 3
            glTarget = GL_TEXTURE_3D
        }
        if (numberOfFaces == 6) {
            glTarget = if (textureDimensions == 2) com.badlogic.gdx.graphics.GL20.GL_TEXTURE_CUBE_MAP else throw com.badlogic.gdx.utils.GdxRuntimeException("cube map needs 2D faces")
        } else if (numberOfFaces != 1) {
            throw com.badlogic.gdx.utils.GdxRuntimeException("numberOfFaces must be either 1 or 6")
        }
        if (numberOfArrayElements > 0) {
            glTarget = if (glTarget == GL_TEXTURE_1D) GL_TEXTURE_1D_ARRAY_EXT else if (glTarget == com.badlogic.gdx.graphics.GL20.GL_TEXTURE_2D) GL_TEXTURE_2D_ARRAY_EXT else throw com.badlogic.gdx.utils.GdxRuntimeException("No API for 3D and cube arrays yet")
            textureDimensions++
        }
        if (glTarget == 0x1234) throw com.badlogic.gdx.utils.GdxRuntimeException("Unsupported texture format (only 2D texture are supported in LibGdx for the time being)")
        var singleFace = -1
        if (numberOfFaces == 6 && target != com.badlogic.gdx.graphics.GL20.GL_TEXTURE_CUBE_MAP) { // Load a single face of the cube (should be avoided since the data is unloaded afterwards)
            if (!(com.badlogic.gdx.graphics.GL20.GL_TEXTURE_CUBE_MAP_POSITIVE_X <= target && target <= com.badlogic.gdx.graphics.GL20.GL_TEXTURE_CUBE_MAP_NEGATIVE_Z)) throw com.badlogic.gdx.utils.GdxRuntimeException(
                "You must specify either GL_TEXTURE_CUBE_MAP to bind all 6 faces of the cube or the requested face GL_TEXTURE_CUBE_MAP_POSITIVE_X and followings.")
            singleFace = target - com.badlogic.gdx.graphics.GL20.GL_TEXTURE_CUBE_MAP_POSITIVE_X
            target = com.badlogic.gdx.graphics.GL20.GL_TEXTURE_CUBE_MAP_POSITIVE_X
        } else if (numberOfFaces == 6 && target == com.badlogic.gdx.graphics.GL20.GL_TEXTURE_CUBE_MAP) { // Load the 6 faces
            target = com.badlogic.gdx.graphics.GL20.GL_TEXTURE_CUBE_MAP_POSITIVE_X
        } else { // Load normal texture
            if (target != glTarget
                && !(com.badlogic.gdx.graphics.GL20.GL_TEXTURE_CUBE_MAP_POSITIVE_X <= target && target <= com.badlogic.gdx.graphics.GL20.GL_TEXTURE_CUBE_MAP_NEGATIVE_Z && target == com.badlogic.gdx.graphics.GL20.GL_TEXTURE_2D)) throw com.badlogic.gdx.utils.GdxRuntimeException("Invalid target requested : 0x" + java.lang.Integer.toHexString(target) + ", expecting : 0x"
                + java.lang.Integer.toHexString(glTarget))
        }
        // KTX files require an unpack alignment of 4
        com.badlogic.gdx.Gdx.gl.glGetIntegerv(com.badlogic.gdx.graphics.GL20.GL_UNPACK_ALIGNMENT, buffer)
        val previousUnpackAlignment: Int = buffer.get(0)
        if (previousUnpackAlignment != 4) com.badlogic.gdx.Gdx.gl.glPixelStorei(com.badlogic.gdx.graphics.GL20.GL_UNPACK_ALIGNMENT, 4)
        val glInternalFormat = glInternalFormat
        val glFormat = glFormat
        var pos = imagePos
        for (level in 0 until numberOfMipmapLevels) {
            val pixelWidth: Int = java.lang.Math.max(1, pixelWidth shr level)
            var pixelHeight: Int = java.lang.Math.max(1, pixelHeight shr level)
            var pixelDepth: Int = java.lang.Math.max(1, pixelDepth shr level)
            compressedData.position(pos)
            val faceLodSize: Int = compressedData.getInt()
            val faceLodSizeRounded = faceLodSize + 3 and 3.inv()
            pos += 4
            for (face in 0 until numberOfFaces) {
                compressedData.position(pos)
                pos += faceLodSizeRounded
                if (singleFace != -1 && singleFace != face) continue
                val data: java.nio.ByteBuffer = compressedData.slice()
                data.limit(faceLodSizeRounded)
                if (textureDimensions == 1) { // if (compressed)
// Gdx.gl.glCompressedTexImage1D(target + face, level, glInternalFormat, pixelWidth, 0, faceLodSize,
// data);
// else
// Gdx.gl.glTexImage1D(target + face, level, glInternalFormat, pixelWidth, 0, glFormat, glType, data);
                } else if (textureDimensions == 2) {
                    if (numberOfArrayElements > 0) pixelHeight = numberOfArrayElements
                    if (compressed) {
                        if (glInternalFormat == com.badlogic.gdx.graphics.glutils.ETC1.ETC1_RGB8_OES) {
                            if (!com.badlogic.gdx.Gdx.graphics.supportsExtension("GL_OES_compressed_ETC1_RGB8_texture")) {
                                val etcData: com.badlogic.gdx.graphics.glutils.ETC1.ETC1Data = com.badlogic.gdx.graphics.glutils.ETC1.ETC1Data(pixelWidth, pixelHeight, data, 0)
                                val pixmap: com.badlogic.gdx.graphics.Pixmap = com.badlogic.gdx.graphics.glutils.ETC1.decodeImage(etcData, com.badlogic.gdx.graphics.Pixmap.Format.RGB888)
                                com.badlogic.gdx.Gdx.gl.glTexImage2D(target + face, level, pixmap.getGLInternalFormat(), pixmap.getWidth(),
                                    pixmap.getHeight(), 0, pixmap.getGLFormat(), pixmap.getGLType(), pixmap.getPixels())
                                pixmap.dispose()
                            } else {
                                com.badlogic.gdx.Gdx.gl.glCompressedTexImage2D(target + face, level, glInternalFormat, pixelWidth, pixelHeight, 0,
                                    faceLodSize, data)
                            }
                        } else { // Try to load (no software unpacking fallback)
                            com.badlogic.gdx.Gdx.gl.glCompressedTexImage2D(target + face, level, glInternalFormat, pixelWidth, pixelHeight, 0,
                                faceLodSize, data)
                        }
                    } else com.badlogic.gdx.Gdx.gl.glTexImage2D(target + face, level, glInternalFormat, pixelWidth, pixelHeight, 0, glFormat, glType, data)
                } else if (textureDimensions == 3) {
                    if (numberOfArrayElements > 0) pixelDepth = numberOfArrayElements
                    // if (compressed)
// Gdx.gl.glCompressedTexImage3D(target + face, level, glInternalFormat, pixelWidth, pixelHeight, pixelDepth, 0,
// faceLodSize, data);
// else
// Gdx.gl.glTexImage3D(target + face, level, glInternalFormat, pixelWidth, pixelHeight, pixelDepth, 0, glFormat,
// glType, data);
                }
            }
        }
        if (previousUnpackAlignment != 4) com.badlogic.gdx.Gdx.gl.glPixelStorei(com.badlogic.gdx.graphics.GL20.GL_UNPACK_ALIGNMENT, previousUnpackAlignment)
        if (useMipMaps()) com.badlogic.gdx.Gdx.gl.glGenerateMipmap(target)
        // dispose data once transfered to GPU
        disposePreparedData()
    }

    fun disposePreparedData() {
        if (compressedData != null) com.badlogic.gdx.utils.BufferUtils.disposeUnsafeByteBuffer(compressedData)
        compressedData = null
    }

    override fun consumePixmap(): com.badlogic.gdx.graphics.Pixmap? {
        throw com.badlogic.gdx.utils.GdxRuntimeException("This TextureData implementation does not return a Pixmap")
    }

    override fun disposePixmap(): Boolean {
        throw com.badlogic.gdx.utils.GdxRuntimeException("This TextureData implementation does not return a Pixmap")
    }

    override fun getWidth(): Int {
        return pixelWidth
    }

    override fun getHeight(): Int {
        return pixelHeight
    }

    fun getNumberOfMipMapLevels(): Int {
        return numberOfMipmapLevels
    }

    fun getNumberOfFaces(): Int {
        return numberOfFaces
    }

    fun getGlInternalFormat(): Int {
        return glInternalFormat
    }

    fun getData(requestedLevel: Int, requestedFace: Int): java.nio.ByteBuffer? {
        var pos = imagePos
        for (level in 0 until numberOfMipmapLevels) {
            val faceLodSize: Int = compressedData.getInt(pos)
            val faceLodSizeRounded = faceLodSize + 3 and 3.inv()
            pos += 4
            if (level == requestedLevel) {
                for (face in 0 until numberOfFaces) {
                    if (face == requestedFace) {
                        compressedData.position(pos)
                        val data: java.nio.ByteBuffer = compressedData.slice()
                        data.limit(faceLodSizeRounded)
                        return data
                    }
                    pos += faceLodSizeRounded
                }
            } else {
                pos += faceLodSizeRounded * numberOfFaces
            }
        }
        return null
    }

    override fun getFormat(): com.badlogic.gdx.graphics.Pixmap.Format? {
        throw com.badlogic.gdx.utils.GdxRuntimeException("This TextureData implementation directly handles texture formats.")
    }

    override fun useMipMaps(): Boolean {
        return useMipMaps
    }

    override fun isManaged(): Boolean {
        return true
    }

    companion object {
        private const val GL_TEXTURE_1D = 0x1234
        private const val GL_TEXTURE_3D = 0x1234
        private const val GL_TEXTURE_1D_ARRAY_EXT = 0x1234
        private const val GL_TEXTURE_2D_ARRAY_EXT = 0x1234
    }
}
