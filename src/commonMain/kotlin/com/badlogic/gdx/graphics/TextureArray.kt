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

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.GdxRuntimeException
import java.util.HashMap

/**
 * Open GLES wrapper for TextureArray
 *
 * @author Tomski
 */
class TextureArray(data: TextureArrayData) : GLTexture(GL30.GL_TEXTURE_2D_ARRAY, Gdx.gl.glGenTexture()) {

    private var data: TextureArrayData? = null

    constructor(vararg internalPaths: String?) : this(*getInternalHandles(*internalPaths)) {}
    constructor(vararg files: FileHandle?) : this(false, *files) {}
    constructor(useMipMaps: Boolean, vararg files: FileHandle?) : this(useMipMaps, Pixmap.Format.RGBA8888, *files) {}
    constructor(useMipMaps: Boolean, format: Pixmap.Format?, vararg files: FileHandle?) : this(TextureArrayData.Factory.loadFromFiles(format, useMipMaps, files)) {}

    private fun load(data: TextureArrayData?) {
        if (this.data != null && data.isManaged() !== this.data.isManaged()) throw GdxRuntimeException("New data must have the same managed status as the old data")
        this.data = data
        bind()
        Gdx.gl30.glTexImage3D(GL30.GL_TEXTURE_2D_ARRAY, 0, data.getInternalFormat(), data.getWidth(), data.getHeight(), data.getDepth(), 0, data.getInternalFormat(), data.getGLType(), null)
        if (!data.isPrepared()) data.prepare()
        data.consumeTextureArrayData()
        setFilter(minFilter, magFilter)
        setWrap(uWrap, vWrap)
        Gdx.gl.glBindTexture(glTarget, 0)
    }

    val width: Int
        get() = data.getWidth()

    val height: Int
        get() = data.getHeight()

    val depth: Int
        get() = data.getDepth()

    val isManaged: Boolean
        get() = data.isManaged()

    protected fun reload() {
        if (!isManaged) throw GdxRuntimeException("Tried to reload an unmanaged TextureArray")
        glHandle = Gdx.gl.glGenTexture()
        load(data)
    }

    companion object {
        val managedTextureArrays: MutableMap<Application, Array<TextureArray>> = HashMap()
        private fun getInternalHandles(vararg internalPaths: String): Array<FileHandle?> {
            val handles = arrayOfNulls<FileHandle>(internalPaths.size)
            for (i in 0 until internalPaths.size) {
                handles[i] = Gdx.files.internal(internalPaths[i])
            }
            return handles
        }

        private fun addManagedTexture(app: Application, texture: TextureArray) {
            var managedTextureArray = managedTextureArrays[app]
            if (managedTextureArray == null) managedTextureArray = Array()
            managedTextureArray.add(texture)
            managedTextureArrays[app] = managedTextureArray
        }

        /**
         * Clears all managed TextureArrays. This is an internal method. Do not use it!
         */
        fun clearAllTextureArrays(app: Application) {
            managedTextureArrays.remove(app)
        }

        /**
         * Invalidate all managed TextureArrays. This is an internal method. Do not use it!
         */
        fun invalidateAllTextureArrays(app: Application) {
            val managedTextureArray = managedTextureArrays[app] ?: return
            for (i in 0 until managedTextureArray.size) {
                val textureArray = managedTextureArray[i]
                textureArray.reload()
            }
        }

        val managedStatus: String
            get() {
                val builder: java.lang.StringBuilder = java.lang.StringBuilder()
                builder.append("Managed TextureArrays/app: { ")
                for (app in managedTextureArrays.keys) {
                    builder.append(managedTextureArrays[app]!!.size)
                    builder.append(" ")
                }
                builder.append("}")
                return builder.toString()
            }

        /**
         * @return the number of managed TextureArrays currently loaded
         */
        val numManagedTextureArrays: Int
            get() = managedTextureArrays[Gdx.app]!!.size
    }

    init {
        if (Gdx.gl30 == null) {
            throw GdxRuntimeException("TextureArray requires a device running with GLES 3.0 compatibilty")
        }
        load(data)
        if (data.isManaged()) addManagedTexture(Gdx.app, this)
    }
}
