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
package com.badlogic.gdx.graphics.g3d.utils

import Texture.TextureFilter
import Texture.TextureWrap
import com.badlogic.gdx.graphics.g3d.utils.TextureDescriptor

/**
 * Used by [Model] to load textures from [ModelData].
 *
 * @author badlogic
 */
interface TextureProvider {

    fun load(fileName: String?): Texture
    class FileTextureProvider : TextureProvider {
        private var minFilter: TextureFilter
        private var magFilter: TextureFilter
        private var uWrap: TextureWrap
        private var vWrap: TextureWrap
        private var useMipMaps: Boolean

        constructor() {
            magFilter = Texture.TextureFilter.Linear
            minFilter = magFilter
            vWrap = Texture.TextureWrap.Repeat
            uWrap = vWrap
            useMipMaps = false
        }

        constructor(minFilter: TextureFilter, magFilter: TextureFilter, uWrap: TextureWrap,
                    vWrap: TextureWrap, useMipMaps: Boolean) {
            this.minFilter = minFilter
            this.magFilter = magFilter
            this.uWrap = uWrap
            this.vWrap = vWrap
            this.useMipMaps = useMipMaps
        }

        override fun load(fileName: String?): Texture {
            val result = Texture(Gdx.files.internal(fileName), useMipMaps)
            result.setFilter(minFilter, magFilter)
            result.setWrap(uWrap, vWrap)
            return result
        }
    }

    class AssetTextureProvider(assetManager: AssetManager) : TextureProvider {
        val assetManager: AssetManager
        override fun load(fileName: String?): Texture {
            return assetManager.get(fileName, Texture::class.java)
        }

        init {
            this.assetManager = assetManager
        }
    }
}
