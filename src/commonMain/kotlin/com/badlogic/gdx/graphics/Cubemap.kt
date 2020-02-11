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
package com.badlogic.gdx.graphics

import com.badlogic.gdx.graphics.Cubemap
import com.badlogic.gdx.graphics.TextureData.Factory.loadFromFile

/**
 * Wraps a standard OpenGL ES Cubemap. Must be disposed when it is no longer used.
 *
 * @author Xoppa
 */
class Cubemap : GLTexture {

    /**
     * Enum to identify each side of a Cubemap
     */
    enum class CubemapSide(
        /**
         * The zero based index of the side in the cubemap
         */
        val index: Int,
        /**
         * The OpenGL target (used for glTexImage2D) of the side.
         */
        val gLEnum: Int, upX: Float, upY: Float, upZ: Float, directionX: Float, directionY: Float, directionZ: Float) {

        /**
         * The positive X and first side of the cubemap
         */
        PositiveX(0, GL20.GL_TEXTURE_CUBE_MAP_POSITIVE_X, 0, -1, 0, 1, 0, 0),

        /**
         * The negative X and second side of the cubemap
         */
        NegativeX(1, GL20.GL_TEXTURE_CUBE_MAP_NEGATIVE_X, 0, -1, 0, -1, 0, 0),

        /**
         * The positive Y and third side of the cubemap
         */
        PositiveY(2, GL20.GL_TEXTURE_CUBE_MAP_POSITIVE_Y, 0, 0, 1, 0, 1, 0),

        /**
         * The negative Y and fourth side of the cubemap
         */
        NegativeY(3, GL20.GL_TEXTURE_CUBE_MAP_NEGATIVE_Y, 0, 0, -1, 0, -1, 0),

        /**
         * The positive Z and fifth side of the cubemap
         */
        PositiveZ(4, GL20.GL_TEXTURE_CUBE_MAP_POSITIVE_Z, 0, -1, 0, 0, 0, 1),

        /**
         * The negative Z and sixth side of the cubemap
         */
        NegativeZ(5, GL20.GL_TEXTURE_CUBE_MAP_NEGATIVE_Z, 0, -1, 0, 0, 0, -1);

        /**
         * @return The OpenGL target (used for glTexImage2D) of the side.
         */

        /**
         * The up vector to target the side.
         */
        val up: Vector3

        /**
         * The direction vector to target the side.
         */
        val direction: Vector3

        /**
         * @return The up vector of the side.
         */
        fun getUp(out: Vector3): Vector3 {
            return out.set(up)
        }

        /**
         * @return The direction vector of the side.
         */
        fun getDirection(out: Vector3): Vector3 {
            return out.set(direction)
        }

        init {
            up = Vector3(upX, upY, upZ)
            direction = Vector3(directionX, directionY, directionZ)
        }
    }

    protected var data: CubemapData? = null

    /**
     * Construct a Cubemap based on the given CubemapData.
     */
    constructor(data: CubemapData?) : super(GL20.GL_TEXTURE_CUBE_MAP) {
        this.data = data
        load(data)
    }

    /**
     * Construct a Cubemap with the specified texture files for the sides, does not generate mipmaps.
     */
    constructor(positiveX: FileHandle?, negativeX: FileHandle?, positiveY: FileHandle?, negativeY: FileHandle?, positiveZ: FileHandle?,
                negativeZ: FileHandle?) : this(positiveX, negativeX, positiveY, negativeY, positiveZ, negativeZ, false) {
    }

    /**
     * Construct a Cubemap with the specified texture files for the sides, optionally generating mipmaps.
     */
    constructor(positiveX: FileHandle?, negativeX: FileHandle?, positiveY: FileHandle?, negativeY: FileHandle?, positiveZ: FileHandle?,
                negativeZ: FileHandle?, useMipMaps: Boolean) : this(loadFromFile(positiveX, useMipMaps), loadFromFile(negativeX, useMipMaps),
        loadFromFile(positiveY, useMipMaps), loadFromFile(negativeY, useMipMaps),
        loadFromFile(positiveZ, useMipMaps), loadFromFile(negativeZ, useMipMaps)) {
    }
    /**
     * Construct a Cubemap with the specified [Pixmap]s for the sides, optionally generating mipmaps.
     */
    /**
     * Construct a Cubemap with the specified [Pixmap]s for the sides, does not generate mipmaps.
     */
    @JvmOverloads
    constructor(positiveX: Pixmap?, negativeX: Pixmap?, positiveY: Pixmap?, negativeY: Pixmap?, positiveZ: Pixmap?, negativeZ: Pixmap?,
                useMipMaps: Boolean = false) : this(if (positiveX == null) null else PixmapTextureData(positiveX, null, useMipMaps, false), if (negativeX == null) null else PixmapTextureData(negativeX, null, useMipMaps, false), if (positiveY == null) null else PixmapTextureData(positiveY,
        null, useMipMaps, false), if (negativeY == null) null else PixmapTextureData(negativeY, null, useMipMaps, false),
        if (positiveZ == null) null else PixmapTextureData(positiveZ, null, useMipMaps, false), if (negativeZ == null) null else PixmapTextureData(negativeZ, null, useMipMaps, false)) {
    }

    /**
     * Construct a Cubemap with [Pixmap]s for each side of the specified size.
     */
    constructor(width: Int, height: Int, depth: Int, format: Format?) : this(PixmapTextureData(Pixmap(depth, height, format), null, false, true), PixmapTextureData(Pixmap(depth,
        height, format), null, false, true), PixmapTextureData(Pixmap(width, depth, format), null, false, true),
        PixmapTextureData(Pixmap(width, depth, format), null, false, true), PixmapTextureData(Pixmap(width,
        height, format), null, false, true), PixmapTextureData(Pixmap(width, height, format), null, false, true)) {
    }

    /**
     * Construct a Cubemap with the specified [TextureData]'s for the sides
     */
    constructor(positiveX: TextureData?, negativeX: TextureData?, positiveY: TextureData?, negativeY: TextureData?,
                positiveZ: TextureData?, negativeZ: TextureData?) : super(GL20.GL_TEXTURE_CUBE_MAP) {
        minFilter = TextureFilter.Nearest
        magFilter = TextureFilter.Nearest
        uWrap = TextureWrap.ClampToEdge
        vWrap = TextureWrap.ClampToEdge
        data = FacedCubemapData(positiveX, negativeX, positiveY, negativeY, positiveZ, negativeZ)
        load(data)
    }

    /**
     * Sets the sides of this cubemap to the specified [CubemapData].
     */
    fun load(data: CubemapData?) {
        if (!data.isPrepared()) data.prepare()
        bind()
        unsafeSetFilter(minFilter, magFilter, true)
        unsafeSetWrap(uWrap, vWrap, true)
        unsafeSetAnisotropicFilter(anisotropicFilterLevel, true)
        data.consumeCubemapData()
        Gdx.gl.glBindTexture(glTarget, 0)
    }

    val cubemapData: CubemapData?
        get() = data

    val isManaged: Boolean
        get() = data.isManaged()

    protected fun reload() {
        if (!isManaged) throw GdxRuntimeException("Tried to reload an unmanaged Cubemap")
        glHandle = Gdx.gl.glGenTexture()
        load(data)
    }

    val width: Int
        get() = data.getWidth()

    val height: Int
        get() = data.getHeight()

    val depth: Int
        get() = 0

    /**
     * Disposes all resources associated with the cubemap
     */
    fun dispose() {
        // this is a hack. reason: we have to set the glHandle to 0 for textures that are
        // reloaded through the asset manager as we first remove (and thus dispose) the texture
        // and then reload it. the glHandle is set to 0 in invalidateAllTextures prior to
        // removal from the asset manager.
        if (glHandle === 0) return
        delete()
        if (data.isManaged()) if (managedCubemaps[Gdx.app] != null) managedCubemaps[Gdx.app].removeValue(this, true)
    }

    companion object {
        private var assetManager: AssetManager? = null
        val managedCubemaps: Map<Application, Array<Cubemap>?> = HashMap<Application, Array<Cubemap>?>()
        private fun addManagedCubemap(app: Application, cubemap: Cubemap) {
            var managedCubemapArray = managedCubemaps[app]
            if (managedCubemapArray == null) managedCubemapArray = Array()
            managedCubemapArray.add(cubemap)
            managedCubemaps.put(app, managedCubemapArray)
        }

        /**
         * Clears all managed cubemaps. This is an internal method. Do not use it!
         */
        fun clearAllCubemaps(app: Application?) {
            managedCubemaps.remove(app)
        }

        /**
         * Invalidate all managed cubemaps. This is an internal method. Do not use it!
         */
        fun invalidateAllCubemaps(app: Application) {
            val managedCubemapArray = managedCubemaps[app] ?: return
            if (assetManager == null) {
                for (i in 0 until managedCubemapArray.size) {
                    val cubemap = managedCubemapArray[i]
                    cubemap.reload()
                }
            } else {
                // first we have to make sure the AssetManager isn't loading anything anymore,
                // otherwise the ref counting trick below wouldn't work (when a cubemap is
                // currently on the task stack of the manager.)
                assetManager.finishLoading()

                // next we go through each cubemap and reload either directly or via the
                // asset manager.
                val cubemaps = Array<Cubemap>(managedCubemapArray)
                for (cubemap in cubemaps) {
                    val fileName: String = assetManager.getAssetFileName(cubemap)
                    if (fileName == null) {
                        cubemap.reload()
                    } else {
                        // get the ref count of the cubemap, then set it to 0 so we
                        // can actually remove it from the assetmanager. Also set the
                        // handle to zero, otherwise we might accidentially dispose
                        // already reloaded cubemaps.
                        val refCount: Int = assetManager.getReferenceCount(fileName)
                        assetManager.setReferenceCount(fileName, 0)
                        cubemap.glHandle = 0

                        // create the parameters, passing the reference to the cubemap as
                        // well as a callback that sets the ref count.
                        val params = CubemapParameter()
                        params.cubemapData = cubemap.cubemapData
                        params.minFilter = cubemap.getMinFilter()
                        params.magFilter = cubemap.getMagFilter()
                        params.wrapU = cubemap.getUWrap()
                        params.wrapV = cubemap.getVWrap()
                        params.cubemap = cubemap // special parameter which will ensure that the references stay the same.
                        params.loadedCallback = object : LoadedCallback() {
                            fun finishedLoading(assetManager: AssetManager, fileName: String?, type: java.lang.Class?) {
                                assetManager.setReferenceCount(fileName, refCount)
                            }
                        }

                        // unload the c, create a new gl handle then reload it.
                        assetManager.unload(fileName)
                        cubemap.glHandle = Gdx.gl.glGenTexture()
                        assetManager.load(fileName, Cubemap::class.java, params)
                    }
                }
                managedCubemapArray.clear()
                managedCubemapArray.addAll(cubemaps)
            }
        }

        /**
         * Sets the [AssetManager]. When the context is lost, cubemaps managed by the asset manager are reloaded by the manager
         * on a separate thread (provided that a suitable [AssetLoader] is registered with the manager). Cubemaps not managed by
         * the AssetManager are reloaded via the usual means on the rendering thread.
         *
         * @param manager the asset manager.
         */
        fun setAssetManager(manager: AssetManager?) {
            assetManager = manager
        }

        val managedStatus: String
            get() {
                val builder: java.lang.StringBuilder = java.lang.StringBuilder()
                builder.append("Managed cubemap/app: { ")
                for (app in managedCubemaps.keySet()) {
                    builder.append(managedCubemaps[app]!!.size)
                    builder.append(" ")
                }
                builder.append("}")
                return builder.toString()
            }

        /**
         * @return the number of managed cubemaps currently loaded
         */
        val numManagedCubemaps: Int
            get() = managedCubemaps[Gdx.app]!!.size
    }
}
