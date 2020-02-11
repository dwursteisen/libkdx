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

import com.badlogic.gdx.assets.AssetDescriptor
import com.badlogic.gdx.assets.AssetLoaderParameters
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.assets.loaders.CubemapLoader.CubemapParameter
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Cubemap
import com.badlogic.gdx.graphics.CubemapData
import com.badlogic.gdx.graphics.Texture.TextureFilter
import com.badlogic.gdx.graphics.Texture.TextureWrap

/** [AssetLoader] for [Cubemap] instances. The pixel data is loaded asynchronously. The texture is then created on the
 * rendering thread, synchronously. Passing a [CubemapParameter] to
 * [AssetManager.load] allows one to specify parameters as can be passed to the
 * various Cubemap constructors, e.g. filtering and so on.
 * @author mzechner, Vincent Bousquet
 */
class CubemapLoader(resolver: FileHandleResolver) : AsynchronousAssetLoader<Cubemap, CubemapParameter>(resolver) {

    class CubemapLoaderInfo {
        var filename: String? = null
        var data: CubemapData? = null
        var cubemap: Cubemap? = null
    }

    var info: CubemapLoaderInfo? = CubemapLoaderInfo()
    override fun loadAsync(manager: AssetManager, fileName: String, file: FileHandle, parameter: CubemapParameter?) {
        info!!.filename = fileName
        if (parameter == null || parameter.cubemapData == null) {
            var format: com.badlogic.gdx.graphics.Pixmap.Format? = null
            val genMipMaps = false
            info!!.cubemap = null
            if (parameter != null) {
                format = parameter.format
                info!!.cubemap = parameter.cubemap
            }
            if (fileName.contains(".ktx") || fileName.contains(".zktx")) {
                info!!.data = com.badlogic.gdx.graphics.glutils.KTXTextureData(file, genMipMaps)
            }
        } else {
            info!!.data = parameter.cubemapData
            info!!.cubemap = parameter.cubemap
        }
        if (!info!!.data!!.isPrepared) info!!.data!!.prepare()
    }

    override fun loadSync(manager: AssetManager, fileName: String, file: FileHandle, parameter: CubemapParameter?): Cubemap? {
        if (info == null) return null
        var cubemap: Cubemap? = info!!.cubemap
        if (cubemap != null) {
            cubemap.load(info!!.data!!)
        } else {
            cubemap = Cubemap(info!!.data!!)
        }
        parameter?.run {
            cubemap.setFilter(this.minFilter, this.magFilter)
            cubemap.setWrap(this.wrapU, this.wrapV)
        }

        return cubemap
    }

    override fun getDependencies(fileName: String, file: FileHandle, parameter: CubemapParameter?): com.badlogic.gdx.utils.Array<AssetDescriptor<*>>? {
        return null
    }

    class CubemapParameter : AssetLoaderParameters<Cubemap>() {
        /** the format of the final Texture. Uses the source images format if null  */
        var format: com.badlogic.gdx.graphics.Pixmap.Format? = null
        /** The texture to put the [TextureData] in, optional.  */
        @kotlin.jvm.JvmField
        var cubemap: Cubemap? = null
        /** CubemapData for textures created on the fly, optional. When set, all format and genMipMaps are ignored  */
        @kotlin.jvm.JvmField
        var cubemapData: CubemapData? = null
        @kotlin.jvm.JvmField
        var minFilter: TextureFilter = TextureFilter.Nearest
        @kotlin.jvm.JvmField
        var magFilter: TextureFilter = TextureFilter.Nearest
        @kotlin.jvm.JvmField
        var wrapU: TextureWrap = TextureWrap.ClampToEdge
        @kotlin.jvm.JvmField
        var wrapV: TextureWrap = TextureWrap.ClampToEdge
    }
}
