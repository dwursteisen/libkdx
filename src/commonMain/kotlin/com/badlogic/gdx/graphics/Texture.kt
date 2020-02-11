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

import com.badlogic.gdx.graphics.TextureData.Factory.loadFromFile

/**
 * A Texture wraps a standard OpenGL ES texture.
 *
 *
 * A Texture can be managed. If the OpenGL context is lost all managed textures get invalidated. This happens when a user switches
 * to another application or receives an incoming call. Managed textures get reloaded automatically.
 *
 *
 * A Texture has to be bound via the [Texture.bind] method in order for it to be applied to geometry. The texture will be
 * bound to the currently active texture unit specified via [GL20.glActiveTexture].
 *
 *
 * You can draw [Pixmap]s to a texture at any time. The changes will be automatically uploaded to texture memory. This is of
 * course not extremely fast so use it with care. It also only works with unmanaged textures.
 *
 *
 * A Texture must be disposed when it is no longer used
 *
 * @author badlogicgames@gmail.com
 */
class Texture protected constructor(glTarget: Int, glHandle: Int, data: TextureData) : GLTexture(glTarget, glHandle) {

    enum class TextureFilter(val gLEnum: Int) {
        /**
         * Fetch the nearest texel that best maps to the pixel on screen.
         */
        Nearest(GL20.GL_NEAREST),

        /**
         * Fetch four nearest texels that best maps to the pixel on screen.
         */
        Linear(GL20.GL_LINEAR),

        /**
         * @see TextureFilter.MipMapLinearLinear
         */
        MipMap(GL20.GL_LINEAR_MIPMAP_LINEAR),

        /**
         * Fetch the best fitting image from the mip map chain based on the pixel/texel ratio and then sample the texels with a
         * nearest filter.
         */
        MipMapNearestNearest(GL20.GL_NEAREST_MIPMAP_NEAREST),

        /**
         * Fetch the best fitting image from the mip map chain based on the pixel/texel ratio and then sample the texels with a
         * linear filter.
         */
        MipMapLinearNearest(GL20.GL_LINEAR_MIPMAP_NEAREST),

        /**
         * Fetch the two best fitting images from the mip map chain and then sample the nearest texel from each of the two images,
         * combining them to the final output pixel.
         */
        MipMapNearestLinear(GL20.GL_NEAREST_MIPMAP_LINEAR),

        /**
         * Fetch the two best fitting images from the mip map chain and then sample the four nearest texels from each of the two
         * images, combining them to the final output pixel.
         */
        MipMapLinearLinear(GL20.GL_LINEAR_MIPMAP_LINEAR);

        val isMipMap: Boolean
            get() = gLEnum != GL20.GL_NEAREST && gLEnum != GL20.GL_LINEAR
    }

    enum class TextureWrap(val gLEnum: Int) {
        MirroredRepeat(GL20.GL_MIRRORED_REPEAT), ClampToEdge(GL20.GL_CLAMP_TO_EDGE), Repeat(GL20.GL_REPEAT);
    }

    var textureData: TextureData? = null

    constructor(internalPath: String?) : this(Gdx.files.internal(internalPath)) {}
    constructor(file: FileHandle?) : this(file, null, false) {}
    constructor(file: FileHandle?, useMipMaps: Boolean) : this(file, null, useMipMaps) {}
    constructor(file: FileHandle?, format: Format?, useMipMaps: Boolean) : this(loadFromFile(file, format, useMipMaps)) {}
    constructor(pixmap: Pixmap?) : this(PixmapTextureData(pixmap, null, false, false)) {}
    constructor(pixmap: Pixmap?, useMipMaps: Boolean) : this(PixmapTextureData(pixmap, null, useMipMaps, false)) {}
    constructor(pixmap: Pixmap?, format: Format?, useMipMaps: Boolean) : this(PixmapTextureData(pixmap, format, useMipMaps, false)) {}
    constructor(width: Int, height: Int, format: Format?) : this(PixmapTextureData(Pixmap(width, height, format), null, false, true)) {}
    constructor(data: TextureData?) : this(GL20.GL_TEXTURE_2D, Gdx.gl.glGenTexture(), data) {}

    fun load(data: TextureData?) {
        if (textureData != null && data!!.isManaged != textureData!!.isManaged) throw GdxRuntimeException("New data must have the same managed status as the old data")
        textureData = data
        if (!data!!.isPrepared) data.prepare()
        bind()
        uploadImageData(GL20.GL_TEXTURE_2D, data)
        unsafeSetFilter(minFilter, magFilter, true)
        unsafeSetWrap(uWrap, vWrap, true)
        unsafeSetAnisotropicFilter(anisotropicFilterLevel, true)
        Gdx.gl.glBindTexture(glTarget, 0)
    }

    /**
     * Used internally to reload after context loss. Creates a new GL handle then calls [.load]. Use this only
     * if you know what you do!
     */
    protected fun reload() {
        if (!isManaged) throw GdxRuntimeException("Tried to reload unmanaged Texture")
        glHandle = Gdx.gl.glGenTexture()
        load(textureData)
    }

    /**
     * Draws the given [Pixmap] to the texture at position x, y. No clipping is performed so you have to make sure that you
     * draw only inside the texture region. Note that this will only draw to mipmap level 0!
     *
     * @param pixmap The Pixmap
     * @param x      The x coordinate in pixels
     * @param y      The y coordinate in pixels
     */
    fun draw(pixmap: Pixmap, x: Int, y: Int) {
        if (textureData!!.isManaged) throw GdxRuntimeException("can't draw to a managed texture")
        bind()
        Gdx.gl.glTexSubImage2D(glTarget, 0, x, y, pixmap.width, pixmap.height, pixmap.gLFormat, pixmap.gLType,
            pixmap.pixels)
    }

    val width: Int
        get() = textureData!!.width

    val height: Int
        get() = textureData!!.height

    val depth: Int
        get() = 0

    /**
     * @return whether this texture is managed or not.
     */
    val isManaged: Boolean
        get() = textureData!!.isManaged

    /**
     * Disposes all resources associated with the texture
     */
    fun dispose() {
        // this is a hack. reason: we have to set the glHandle to 0 for textures that are
        // reloaded through the asset manager as we first remove (and thus dispose) the texture
        // and then reload it. the glHandle is set to 0 in invalidateAllTextures prior to
        // removal from the asset manager.
        if (glHandle === 0) return
        delete()
        if (textureData!!.isManaged) if (managedTextures[Gdx.app] != null) managedTextures[Gdx.app].removeValue(this, true)
    }

    override fun toString(): String {
        return if (textureData is FileTextureData) textureData.toString() else super.toString()
    }

    companion object {
        private var assetManager: AssetManager? = null
        val managedTextures: Map<Application, Array<Texture>?> = HashMap<Application, Array<Texture>?>()
        private fun addManagedTexture(app: Application, texture: Texture) {
            var managedTextureArray = managedTextures[app]
            if (managedTextureArray == null) managedTextureArray = Array()
            managedTextureArray.add(texture)
            managedTextures.put(app, managedTextureArray)
        }

        /**
         * Clears all managed textures. This is an internal method. Do not use it!
         */
        fun clearAllTextures(app: Application?) {
            managedTextures.remove(app)
        }

        /**
         * Invalidate all managed textures. This is an internal method. Do not use it!
         */
        fun invalidateAllTextures(app: Application) {
            val managedTextureArray = managedTextures[app] ?: return
            if (assetManager == null) {
                for (i in 0 until managedTextureArray.size) {
                    val texture = managedTextureArray[i]
                    texture.reload()
                }
            } else {
                // first we have to make sure the AssetManager isn't loading anything anymore,
                // otherwise the ref counting trick below wouldn't work (when a texture is
                // currently on the task stack of the manager.)
                assetManager.finishLoading()

                // next we go through each texture and reload either directly or via the
                // asset manager.
                val textures = Array<Texture>(managedTextureArray)
                for (texture in textures) {
                    val fileName: String = assetManager.getAssetFileName(texture)
                    if (fileName == null) {
                        texture.reload()
                    } else {
                        // get the ref count of the texture, then set it to 0 so we
                        // can actually remove it from the assetmanager. Also set the
                        // handle to zero, otherwise we might accidentially dispose
                        // already reloaded textures.
                        val refCount: Int = assetManager.getReferenceCount(fileName)
                        assetManager.setReferenceCount(fileName, 0)
                        texture.glHandle = 0

                        // create the parameters, passing the reference to the texture as
                        // well as a callback that sets the ref count.
                        val params = TextureParameter()
                        params.textureData = texture.textureData
                        params.minFilter = texture.getMinFilter()
                        params.magFilter = texture.getMagFilter()
                        params.wrapU = texture.getUWrap()
                        params.wrapV = texture.getVWrap()
                        params.genMipMaps = texture.textureData!!.useMipMaps() // not sure about this?
                        params.texture = texture // special parameter which will ensure that the references stay the same.
                        params.loadedCallback = object : LoadedCallback() {
                            fun finishedLoading(assetManager: AssetManager, fileName: String?, type: java.lang.Class?) {
                                assetManager.setReferenceCount(fileName, refCount)
                            }
                        }

                        // unload the texture, create a new gl handle then reload it.
                        assetManager.unload(fileName)
                        texture.glHandle = Gdx.gl.glGenTexture()
                        assetManager.load(fileName, Texture::class.java, params)
                    }
                }
                managedTextureArray.clear()
                managedTextureArray.addAll(textures)
            }
        }

        /**
         * Sets the [AssetManager]. When the context is lost, textures managed by the asset manager are reloaded by the manager
         * on a separate thread (provided that a suitable [AssetLoader] is registered with the manager). Textures not managed by
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
                builder.append("Managed textures/app: { ")
                for (app in managedTextures.keySet()) {
                    builder.append(managedTextures[app]!!.size)
                    builder.append(" ")
                }
                builder.append("}")
                return builder.toString()
            }

        /**
         * @return the number of managed textures currently loaded
         */
        val numManagedTextures: Int
            get() = managedTextures[Gdx.app]!!.size
    }

    init {
        load(data)
        if (data.isManaged) addManagedTexture(Gdx.app, this)
    }
}
