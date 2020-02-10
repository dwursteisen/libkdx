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
import java.util.Locale

/** [AssetLoader] for [Texture] instances. The pixel data is loaded asynchronously. The texture is then created on the
 * rendering thread, synchronously. Passing a [TextureParameter] to
 * [AssetManager.load] allows one to specify parameters as can be passed to the
 * various Texture constructors, e.g. filtering, whether to generate mipmaps and so on.
 * @author mzechner
 */
class TextureLoader(resolver: com.badlogic.gdx.assets.loaders.FileHandleResolver?) : com.badlogic.gdx.assets.loaders.AsynchronousAssetLoader<com.badlogic.gdx.graphics.Texture?, TextureParameter?>(resolver) {

    class TextureLoaderInfo {
        var filename: String? = null
        var data: com.badlogic.gdx.graphics.TextureData? = null
        var texture: com.badlogic.gdx.graphics.Texture? = null
    }

    var info: TextureLoaderInfo? = TextureLoaderInfo()
    override fun loadAsync(manager: com.badlogic.gdx.assets.AssetManager?, fileName: String?, file: com.badlogic.gdx.files.FileHandle?, parameter: TextureParameter?) {
        info!!.filename = fileName
        if (parameter == null || parameter.textureData == null) {
            var format: com.badlogic.gdx.graphics.Pixmap.Format? = null
            var genMipMaps = false
            info!!.texture = null
            if (parameter != null) {
                format = parameter.format
                genMipMaps = parameter.genMipMaps
                info!!.texture = parameter.texture
            }
            info!!.data = com.badlogic.gdx.graphics.TextureData.Factory.loadFromFile(file, format, genMipMaps)
        } else {
            info!!.data = parameter.textureData
            info!!.texture = parameter.texture
        }
        if (!info!!.data.isPrepared()) info!!.data.prepare()
    }

    override fun loadSync(manager: com.badlogic.gdx.assets.AssetManager?, fileName: String?, file: com.badlogic.gdx.files.FileHandle?, parameter: TextureParameter?): com.badlogic.gdx.graphics.Texture? {
        if (info == null) return null
        var texture: com.badlogic.gdx.graphics.Texture? = info!!.texture
        if (texture != null) {
            texture.load(info!!.data)
        } else {
            texture = com.badlogic.gdx.graphics.Texture(info!!.data)
        }
        if (parameter != null) {
            texture.setFilter(parameter.minFilter, parameter.magFilter)
            texture.setWrap(parameter.wrapU, parameter.wrapV)
        }
        return texture
    }

    override fun getDependencies(fileName: String?, file: com.badlogic.gdx.files.FileHandle?, parameter: TextureParameter?): com.badlogic.gdx.utils.Array<com.badlogic.gdx.assets.AssetDescriptor<*>?>? {
        return null
    }

    class TextureParameter : AssetLoaderParameters<com.badlogic.gdx.graphics.Texture?>() {
        /** the format of the final Texture. Uses the source images format if null  */
        var format: com.badlogic.gdx.graphics.Pixmap.Format? = null
        /** whether to generate mipmaps  */
        @kotlin.jvm.JvmField
        var genMipMaps = false
        /** The texture to put the [TextureData] in, optional.  */
        @kotlin.jvm.JvmField
        var texture: com.badlogic.gdx.graphics.Texture? = null
        /** TextureData for textures created on the fly, optional. When set, all format and genMipMaps are ignored  */
        @kotlin.jvm.JvmField
        var textureData: com.badlogic.gdx.graphics.TextureData? = null
        @kotlin.jvm.JvmField
        var minFilter: com.badlogic.gdx.graphics.Texture.TextureFilter? = com.badlogic.gdx.graphics.Texture.TextureFilter.Nearest
        @kotlin.jvm.JvmField
        var magFilter: com.badlogic.gdx.graphics.Texture.TextureFilter? = com.badlogic.gdx.graphics.Texture.TextureFilter.Nearest
        @kotlin.jvm.JvmField
        var wrapU: com.badlogic.gdx.graphics.Texture.TextureWrap? = com.badlogic.gdx.graphics.Texture.TextureWrap.ClampToEdge
        @kotlin.jvm.JvmField
        var wrapV: com.badlogic.gdx.graphics.Texture.TextureWrap? = com.badlogic.gdx.graphics.Texture.TextureWrap.ClampToEdge
    }
}
