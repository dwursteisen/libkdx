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

/** A FacedCubemapData holds a cubemap data definition based on a [TextureData] per face.
 *
 * @author Vincent Nousquet
 */
class FacedCubemapData @JvmOverloads constructor(positiveX: com.badlogic.gdx.graphics.TextureData? = null as com.badlogic.gdx.graphics.TextureData?, negativeX: com.badlogic.gdx.graphics.TextureData? = null as com.badlogic.gdx.graphics.TextureData?, positiveY: com.badlogic.gdx.graphics.TextureData? = null as com.badlogic.gdx.graphics.TextureData?, negativeY: com.badlogic.gdx.graphics.TextureData? = null as com.badlogic.gdx.graphics.TextureData?,
                                                 positiveZ: com.badlogic.gdx.graphics.TextureData? = null as com.badlogic.gdx.graphics.TextureData?, negativeZ: com.badlogic.gdx.graphics.TextureData? = null as com.badlogic.gdx.graphics.TextureData?) : com.badlogic.gdx.graphics.CubemapData {

    protected val data: Array<com.badlogic.gdx.graphics.TextureData?>? = arrayOfNulls<com.badlogic.gdx.graphics.TextureData?>(6)

    /** Construct a Cubemap with the specified texture files for the sides, optionally generating mipmaps.  */
    constructor(positiveX: FileHandle?, negativeX: FileHandle?, positiveY: FileHandle?, negativeY: FileHandle?,
                positiveZ: FileHandle?, negativeZ: FileHandle?) : this(com.badlogic.gdx.graphics.TextureData.Factory.loadFromFile(positiveX, false), com.badlogic.gdx.graphics.TextureData.Factory.loadFromFile(negativeX,
        false), com.badlogic.gdx.graphics.TextureData.Factory.loadFromFile(positiveY, false), com.badlogic.gdx.graphics.TextureData.Factory.loadFromFile(
        negativeY, false), com.badlogic.gdx.graphics.TextureData.Factory.loadFromFile(positiveZ, false), com.badlogic.gdx.graphics.TextureData.Factory
        .loadFromFile(negativeZ, false)) {
    }

    /** Construct a Cubemap with the specified texture files for the sides, optionally generating mipmaps.  */
    constructor(positiveX: FileHandle?, negativeX: FileHandle?, positiveY: FileHandle?, negativeY: FileHandle?,
                positiveZ: FileHandle?, negativeZ: FileHandle?, useMipMaps: Boolean) : this(com.badlogic.gdx.graphics.TextureData.Factory.loadFromFile(positiveX, useMipMaps), com.badlogic.gdx.graphics.TextureData.Factory.loadFromFile(
        negativeX, useMipMaps), com.badlogic.gdx.graphics.TextureData.Factory.loadFromFile(positiveY, useMipMaps), com.badlogic.gdx.graphics.TextureData.Factory
        .loadFromFile(negativeY, useMipMaps), com.badlogic.gdx.graphics.TextureData.Factory.loadFromFile(positiveZ, useMipMaps),
        com.badlogic.gdx.graphics.TextureData.Factory.loadFromFile(negativeZ, useMipMaps)) {
    }
    /** Construct a Cubemap with the specified [Pixmap]s for the sides, optionally generating mipmaps.  */
    /** Construct a Cubemap with the specified [Pixmap]s for the sides, does not generate mipmaps.  */
    @JvmOverloads
    constructor(positiveX: com.badlogic.gdx.graphics.Pixmap?, negativeX: com.badlogic.gdx.graphics.Pixmap?, positiveY: com.badlogic.gdx.graphics.Pixmap?, negativeY: com.badlogic.gdx.graphics.Pixmap?, positiveZ: com.badlogic.gdx.graphics.Pixmap?,
                negativeZ: com.badlogic.gdx.graphics.Pixmap?, useMipMaps: Boolean = false) : this(if (positiveX == null) null else com.badlogic.gdx.graphics.glutils.PixmapTextureData(positiveX, null, useMipMaps, false), if (negativeX == null) null else com.badlogic.gdx.graphics.glutils.PixmapTextureData(negativeX, null, useMipMaps, false), if (positiveY == null) null else com.badlogic.gdx.graphics.glutils.PixmapTextureData(positiveY,
        null, useMipMaps, false), if (negativeY == null) null else com.badlogic.gdx.graphics.glutils.PixmapTextureData(negativeY, null, useMipMaps, false),
        if (positiveZ == null) null else com.badlogic.gdx.graphics.glutils.PixmapTextureData(positiveZ, null, useMipMaps, false), if (negativeZ == null) null else com.badlogic.gdx.graphics.glutils.PixmapTextureData(negativeZ, null, useMipMaps, false)) {
    }

    /** Construct a Cubemap with [Pixmap]s for each side of the specified size.  */
    constructor(width: Int, height: Int, depth: Int, format: com.badlogic.gdx.graphics.Pixmap.Format?) : this(com.badlogic.gdx.graphics.glutils.PixmapTextureData(com.badlogic.gdx.graphics.Pixmap(depth, height, format), null, false, true), com.badlogic.gdx.graphics.glutils.PixmapTextureData(com.badlogic.gdx.graphics.Pixmap(depth,
        height, format), null, false, true), com.badlogic.gdx.graphics.glutils.PixmapTextureData(com.badlogic.gdx.graphics.Pixmap(width, depth, format), null, false, true),
        com.badlogic.gdx.graphics.glutils.PixmapTextureData(com.badlogic.gdx.graphics.Pixmap(width, depth, format), null, false, true), com.badlogic.gdx.graphics.glutils.PixmapTextureData(com.badlogic.gdx.graphics.Pixmap(width,
        height, format), null, false, true), com.badlogic.gdx.graphics.glutils.PixmapTextureData(com.badlogic.gdx.graphics.Pixmap(width, height, format), null, false, true)) {
    }

    override fun isManaged(): Boolean {
        for (data in data!!) if (!data.isManaged()) return false
        return true
    }

    /** Loads the texture specified using the [FileHandle] and sets it to specified side, overwriting any previous data set to
     * that side. Note that you need to reload through [Cubemap.load] any cubemap using this data for the change
     * to be taken in account.
     * @param side The [CubemapSide]
     * @param file The texture [FileHandle]
     */
    fun load(side: com.badlogic.gdx.graphics.Cubemap.CubemapSide?, file: FileHandle?) {
        data!![side.index] = com.badlogic.gdx.graphics.TextureData.Factory.loadFromFile(file, false)
    }

    /** Sets the specified side of this cubemap to the specified [Pixmap], overwriting any previous data set to that side.
     * Note that you need to reload through [Cubemap.load] any cubemap using this data for the change to be
     * taken in account.
     * @param side The [CubemapSide]
     * @param pixmap The [Pixmap]
     */
    fun load(side: com.badlogic.gdx.graphics.Cubemap.CubemapSide?, pixmap: com.badlogic.gdx.graphics.Pixmap?) {
        data!![side.index] = if (pixmap == null) null else com.badlogic.gdx.graphics.glutils.PixmapTextureData(pixmap, null, false, false)
    }

    /** @return True if all sides of this cubemap are set, false otherwise.
     */
    fun isComplete(): Boolean {
        for (i in data!!.indices) if (data[i] == null) return false
        return true
    }

    /** @return The [TextureData] for the specified side, can be null if the cubemap is incomplete.
     */
    fun getTextureData(side: com.badlogic.gdx.graphics.Cubemap.CubemapSide?): com.badlogic.gdx.graphics.TextureData? {
        return data!![side.index]
    }

    override fun getWidth(): Int {
        var tmp: Int
        var width = 0
        if (data!![com.badlogic.gdx.graphics.Cubemap.CubemapSide.PositiveZ.index] != null && data[com.badlogic.gdx.graphics.Cubemap.CubemapSide.PositiveZ.index].getWidth().also({ tmp = it }) > width) width = tmp
        if (data[com.badlogic.gdx.graphics.Cubemap.CubemapSide.NegativeZ.index] != null && data[com.badlogic.gdx.graphics.Cubemap.CubemapSide.NegativeZ.index].getWidth().also({ tmp = it }) > width) width = tmp
        if (data[com.badlogic.gdx.graphics.Cubemap.CubemapSide.PositiveY.index] != null && data[com.badlogic.gdx.graphics.Cubemap.CubemapSide.PositiveY.index].getWidth().also({ tmp = it }) > width) width = tmp
        if (data[com.badlogic.gdx.graphics.Cubemap.CubemapSide.NegativeY.index] != null && data[com.badlogic.gdx.graphics.Cubemap.CubemapSide.NegativeY.index].getWidth().also({ tmp = it }) > width) width = tmp
        return width
    }

    override fun getHeight(): Int {
        var tmp: Int
        var height = 0
        if (data!![com.badlogic.gdx.graphics.Cubemap.CubemapSide.PositiveZ.index] != null && data[com.badlogic.gdx.graphics.Cubemap.CubemapSide.PositiveZ.index].getHeight().also({ tmp = it }) > height) height = tmp
        if (data[com.badlogic.gdx.graphics.Cubemap.CubemapSide.NegativeZ.index] != null && data[com.badlogic.gdx.graphics.Cubemap.CubemapSide.NegativeZ.index].getHeight().also({ tmp = it }) > height) height = tmp
        if (data[com.badlogic.gdx.graphics.Cubemap.CubemapSide.PositiveX.index] != null && data[com.badlogic.gdx.graphics.Cubemap.CubemapSide.PositiveX.index].getHeight().also({ tmp = it }) > height) height = tmp
        if (data[com.badlogic.gdx.graphics.Cubemap.CubemapSide.NegativeX.index] != null && data[com.badlogic.gdx.graphics.Cubemap.CubemapSide.NegativeX.index].getHeight().also({ tmp = it }) > height) height = tmp
        return height
    }

    override fun isPrepared(): Boolean {
        return false
    }

    override fun prepare() {
        if (!isComplete()) throw com.badlogic.gdx.utils.GdxRuntimeException("You need to complete your cubemap data before using it")
        for (i in data!!.indices) if (!data[i].isPrepared()) data[i].prepare()
    }

    override fun consumeCubemapData() {
        for (i in data!!.indices) {
            if (data[i].getType() == com.badlogic.gdx.graphics.TextureData.TextureDataType.Custom) {
                data[i].consumeCustomData(com.badlogic.gdx.graphics.GL20.GL_TEXTURE_CUBE_MAP_POSITIVE_X + i)
            } else {
                var pixmap: com.badlogic.gdx.graphics.Pixmap? = data[i].consumePixmap()
                var disposePixmap: Boolean = data[i].disposePixmap()
                if (data[i].getFormat() != pixmap.getFormat()) {
                    val tmp: com.badlogic.gdx.graphics.Pixmap = com.badlogic.gdx.graphics.Pixmap(pixmap.getWidth(), pixmap.getHeight(), data[i].getFormat())
                    tmp.setBlending(com.badlogic.gdx.graphics.Pixmap.Blending.None)
                    tmp.drawPixmap(pixmap, 0, 0, 0, 0, pixmap.getWidth(), pixmap.getHeight())
                    if (data[i].disposePixmap()) pixmap.dispose()
                    pixmap = tmp
                    disposePixmap = true
                }
                com.badlogic.gdx.Gdx.gl.glPixelStorei(com.badlogic.gdx.graphics.GL20.GL_UNPACK_ALIGNMENT, 1)
                com.badlogic.gdx.Gdx.gl.glTexImage2D(com.badlogic.gdx.graphics.GL20.GL_TEXTURE_CUBE_MAP_POSITIVE_X + i, 0, pixmap.getGLInternalFormat(), pixmap.getWidth(),
                    pixmap.getHeight(), 0, pixmap.getGLFormat(), pixmap.getGLType(), pixmap.getPixels())
                if (disposePixmap) pixmap.dispose()
            }
        }
    }
    /** Construct a Cubemap with the specified [TextureData]'s for the sides  */
    /** Construct an empty Cubemap. Use the load(...) methods to set the texture of each side. Every side of the cubemap must be set
     * before it can be used.  */
    init {
        data!![0] = positiveX
        data[1] = negativeX
        data[2] = positiveY
        data[3] = negativeY
        data[4] = positiveZ
        data[5] = negativeZ
    }
}
