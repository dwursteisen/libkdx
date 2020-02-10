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
import com.badlogic.gdx.assets.loaders.CubemapLoader.CubemapParameter
import java.util.Locale

/** [AssetLoader] for [Cubemap] instances. The pixel data is loaded asynchronously. The texture is then created on the
 * rendering thread, synchronously. Passing a [CubemapParameter] to
 * [AssetManager.load] allows one to specify parameters as can be passed to the
 * various Cubemap constructors, e.g. filtering and so on.
 * @author mzechner, Vincent Bousquet
 */
class CubemapLoader(resolver: com.badlogic.gdx.assets.loaders.FileHandleResolver?) : com.badlogic.gdx.assets.loaders.AsynchronousAssetLoader<com.badlogic.gdx.graphics.Cubemap?, CubemapParameter?>(resolver) {

    class CubemapLoaderInfo {
        var filename: String? = null
        var data: com.badlogic.gdx.graphics.CubemapData? = null
        var cubemap: com.badlogic.gdx.graphics.Cubemap? = null
    }

    var info: CubemapLoaderInfo? = CubemapLoaderInfo()
    override fun loadAsync(manager: com.badlogic.gdx.assets.AssetManager?, fileName: String?, file: com.badlogic.gdx.files.FileHandle?, parameter: CubemapParameter?) {
        info!!.filename = fileName
        if (parameter == null || parameter.cubemapData == null) {
            var format: com.badlogic.gdx.graphics.Pixmap.Format? = null
            val genMipMaps = false
            info!!.cubemap = null
            if (parameter != null) {
                format = parameter.format
                info!!.cubemap = parameter.cubemap
            }
            if (fileName!!.contains(".ktx") || fileName.contains(".zktx")) {
                info!!.data = com.badlogic.gdx.graphics.glutils.KTXTextureData(file, genMipMaps)
            }
        } else {
            info!!.data = parameter.cubemapData
            info!!.cubemap = parameter.cubemap
        }
        if (!info!!.data.isPrepared()) info!!.data.prepare()
    }

    override fun loadSync(manager: com.badlogic.gdx.assets.AssetManager?, fileName: String?, file: com.badlogic.gdx.files.FileHandle?, parameter: CubemapParameter?): com.badlogic.gdx.graphics.Cubemap? {
        if (info == null) return null
        var cubemap: com.badlogic.gdx.graphics.Cubemap? = info!!.cubemap
        if (cubemap != null) {
            cubemap.load(info!!.data)
        } else {
            cubemap = com.badlogic.gdx.graphics.Cubemap(info!!.data)
        }
        if (parameter != null) {
            cubemap.setFilter(parameter.minFilter, parameter.magFilter)
            cubemap.setWrap(parameter.wrapU, parameter.wrapV)
        }
        return cubemap
    }

    override fun getDependencies(fileName: String?, file: com.badlogic.gdx.files.FileHandle?, parameter: CubemapParameter?): com.badlogic.gdx.utils.Array<com.badlogic.gdx.assets.AssetDescriptor<*>?>? {
        return null
    }

    class CubemapParameter : AssetLoaderParameters<com.badlogic.gdx.graphics.Cubemap?>() {
        /** the format of the final Texture. Uses the source images format if null  */
        var format: com.badlogic.gdx.graphics.Pixmap.Format? = null
        /** The texture to put the [TextureData] in, optional.  */
        @kotlin.jvm.JvmField
        var cubemap: com.badlogic.gdx.graphics.Cubemap? = null
        /** CubemapData for textures created on the fly, optional. When set, all format and genMipMaps are ignored  */
        @kotlin.jvm.JvmField
        var cubemapData: com.badlogic.gdx.graphics.CubemapData? = null
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
