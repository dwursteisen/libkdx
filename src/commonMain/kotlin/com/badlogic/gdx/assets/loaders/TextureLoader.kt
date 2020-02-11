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
package com.badlogic.gdx.assets.loaders

import com.badlogic.gdx.assets.AssetLoaderParameters
import com.badlogic.gdx.assets.loaders.TextureLoader.TextureParameter
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Pixmap.Format
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.TextureData

/** [AssetLoader] for [Texture] instances. The pixel data is loaded asynchronously. The texture is then created on the
 * rendering thread, synchronously. Passing a [TextureParameter] to
 * [AssetManager.load] allows one to specify parameters as can be passed to the
 * various Texture constructors, e.g. filtering, whether to generate mipmaps and so on.
 * @author mzechner
 */
class TextureLoader(resolver: FileHandleResolver) : AsynchronousAssetLoader<Texture, TextureParameter>(resolver) {

    class TextureLoaderInfo {
        var filename: String? = null
        var data: TextureData? = null
        var texture: Texture? = null
    }

    var info: TextureLoaderInfo? = TextureLoaderInfo()

    override fun loadAsync(
        manager: com.badlogic.gdx.assets.AssetManager,
        fileName: String,
        file: FileHandle,
        parameter: TextureParameter?
    ) {
        info!!.filename = fileName
        if (parameter == null || parameter.textureData == null) {
            var format: Format? = null
            var genMipMaps = false
            info!!.texture = null
            if (parameter != null) {
                format = parameter.format
                genMipMaps = parameter.genMipMaps
                info!!.texture = parameter.texture
            }
            info!!.data = TextureData.Factory.loadFromFile(file, format, genMipMaps)
        } else {
            info!!.data = parameter.textureData
            info!!.texture = parameter.texture
        }
        if (!info!!.data.isPrepared()) info!!.data.prepare()
    }

    override fun loadSync(
        manager: com.badlogic.gdx.assets.AssetManager,
        fileName: String,
        file: FileHandle,
        parameter: TextureParameter?
    ): Texture? {
        if (info == null) return null
        var texture: Texture? = info!!.texture
        if (texture != null) {
            texture.load(info!!.data)
        } else {
            texture = Texture(info!!.data)
        }
        if (parameter != null) {
            texture.setFilter(parameter.minFilter, parameter.magFilter)
            texture.setWrap(parameter.wrapU, parameter.wrapV)
        }
        return texture
    }

    override fun getDependencies(
        fileName: String,
        file: FileHandle,
        parameter: TextureParameter?
    ): com.badlogic.gdx.utils.Array<com.badlogic.gdx.assets.AssetDescriptor<*>>? {
        return null
    }

    class TextureParameter : AssetLoaderParameters<Texture>() {
        /** the format of the final Texture. Uses the source images format if null  */
        var format: Format? = null

        /** whether to generate mipmaps  */
        var genMipMaps = false

        /** The texture to put the [TextureData] in, optional.  */
        var texture: Texture? = null

        /** TextureData for textures created on the fly, optional. When set, all format and genMipMaps are ignored  */
        var textureData: TextureData? = null

        var minFilter: Texture.TextureFilter? = Texture.TextureFilter.Nearest

        var magFilter: Texture.TextureFilter? = Texture.TextureFilter.Nearest

        var wrapU: Texture.TextureWrap? = Texture.TextureWrap.ClampToEdge

        var wrapV: Texture.TextureWrap? = Texture.TextureWrap.ClampToEdge
    }
}
