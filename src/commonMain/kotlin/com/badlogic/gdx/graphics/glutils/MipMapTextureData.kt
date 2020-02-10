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

/** This class will load each contained TextureData to the chosen mipmap level.
 * All the mipmap levels must be defined and cannot be null.  */
class MipMapTextureData(vararg mipMapData: com.badlogic.gdx.graphics.TextureData?) : com.badlogic.gdx.graphics.TextureData {

    var mips: Array<com.badlogic.gdx.graphics.TextureData?>?
    override fun getType(): com.badlogic.gdx.graphics.TextureData.TextureDataType? {
        return com.badlogic.gdx.graphics.TextureData.TextureDataType.Custom
    }

    override fun isPrepared(): Boolean {
        return true
    }

    override fun prepare() {}
    override fun consumePixmap(): com.badlogic.gdx.graphics.Pixmap? {
        throw com.badlogic.gdx.utils.GdxRuntimeException("It's compressed, use the compressed method")
    }

    override fun disposePixmap(): Boolean {
        return false
    }

    override fun consumeCustomData(target: Int) {
        for (i in mips!!.indices) {
            com.badlogic.gdx.graphics.GLTexture.uploadImageData(target, mips!![i], i)
        }
    }

    override fun getWidth(): Int {
        return mips!![0].getWidth()
    }

    override fun getHeight(): Int {
        return mips!![0].getHeight()
    }

    override fun getFormat(): com.badlogic.gdx.graphics.Pixmap.Format? {
        return mips!![0].getFormat()
    }

    override fun useMipMaps(): Boolean {
        return false
    }

    override fun isManaged(): Boolean {
        return true
    }

    /** @param mipMapData must be != null and its length must be >= 1
     */
    init {
        mips = arrayOfNulls<com.badlogic.gdx.graphics.TextureData?>(mipMapData.size)
        java.lang.System.arraycopy(mipMapData, 0, mips, 0, mipMapData.size)
    }
}
