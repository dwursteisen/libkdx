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
import com.badlogic.gdx.assets.loaders.BitmapFontLoader.BitmapFontParameter
import java.util.Locale

/** [AssetLoader] for [BitmapFont] instances. Loads the font description file (.fnt) asynchronously, loads the
 * [Texture] containing the glyphs as a dependency. The [BitmapFontParameter] allows you to set things like texture
 * filters or whether to flip the glyphs vertically.
 * @author mzechner
 */
class BitmapFontLoader(resolver: com.badlogic.gdx.assets.loaders.FileHandleResolver?) : com.badlogic.gdx.assets.loaders.AsynchronousAssetLoader<com.badlogic.gdx.graphics.g2d.BitmapFont?, BitmapFontParameter?>(resolver) {

    var data: com.badlogic.gdx.graphics.g2d.BitmapFont.BitmapFontData? = null
    override fun getDependencies(fileName: String?, file: com.badlogic.gdx.files.FileHandle?, parameter: BitmapFontParameter?): com.badlogic.gdx.utils.Array<com.badlogic.gdx.assets.AssetDescriptor<*>?>? {
        val deps: com.badlogic.gdx.utils.Array<com.badlogic.gdx.assets.AssetDescriptor<*>?> = com.badlogic.gdx.utils.Array()
        if (parameter != null && parameter.bitmapFontData != null) {
            data = parameter.bitmapFontData
            return deps
        }
        data = com.badlogic.gdx.graphics.g2d.BitmapFont.BitmapFontData(file, parameter != null && parameter.flip)
        if (parameter != null && parameter.atlasName != null) {
            deps.add(com.badlogic.gdx.assets.AssetDescriptor<Any?>(parameter.atlasName, com.badlogic.gdx.graphics.g2d.TextureAtlas::class.java))
        } else {
            for (i in data.getImagePaths().indices) {
                val path: String = data.getImagePath(i)
                val resolved: com.badlogic.gdx.files.FileHandle = resolve(path)
                val textureParams: com.badlogic.gdx.assets.loaders.TextureLoader.TextureParameter = com.badlogic.gdx.assets.loaders.TextureLoader.TextureParameter()
                if (parameter != null) {
                    textureParams!!.genMipMaps = parameter.genMipMaps
                    textureParams!!.minFilter = parameter.minFilter
                    textureParams!!.magFilter = parameter.magFilter
                }
                val descriptor: com.badlogic.gdx.assets.AssetDescriptor<*> = com.badlogic.gdx.assets.AssetDescriptor<Any?>(resolved, com.badlogic.gdx.graphics.Texture::class.java, textureParams)
                deps.add(descriptor)
            }
        }
        return deps
    }

    override fun loadAsync(manager: com.badlogic.gdx.assets.AssetManager?, fileName: String?, file: com.badlogic.gdx.files.FileHandle?, parameter: BitmapFontParameter?) {}
    override fun loadSync(manager: com.badlogic.gdx.assets.AssetManager?, fileName: String?, file: com.badlogic.gdx.files.FileHandle?, parameter: BitmapFontParameter?): com.badlogic.gdx.graphics.g2d.BitmapFont? {
        return if (parameter != null && parameter.atlasName != null) {
            val atlas: com.badlogic.gdx.graphics.g2d.TextureAtlas = manager.get(parameter.atlasName, com.badlogic.gdx.graphics.g2d.TextureAtlas::class.java)
            val name: String = file.sibling(data.imagePaths.get(0)).nameWithoutExtension().toString()
            val region: com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion = atlas.findRegion(name)
                ?: throw com.badlogic.gdx.utils.GdxRuntimeException("Could not find font region " + name + " in atlas " + parameter.atlasName)
            com.badlogic.gdx.graphics.g2d.BitmapFont(file, region)
        } else {
            val n: Int = data.getImagePaths().size
            val regs: com.badlogic.gdx.utils.Array<com.badlogic.gdx.graphics.g2d.TextureRegion?> = com.badlogic.gdx.utils.Array(n)
            for (i in 0 until n) {
                regs.add(com.badlogic.gdx.graphics.g2d.TextureRegion(manager.get(data.getImagePath(i), com.badlogic.gdx.graphics.Texture::class.java)))
            }
            com.badlogic.gdx.graphics.g2d.BitmapFont(data, regs, true)
        }
    }

    /** Parameter to be passed to [AssetManager.load] if additional configuration is
     * necessary for the [BitmapFont].
     * @author mzechner
     */
    class BitmapFontParameter : AssetLoaderParameters<com.badlogic.gdx.graphics.g2d.BitmapFont?>() {

        /** Flips the font vertically if `true`. Defaults to `false`.  */
        var flip = false
        /** Generates mipmaps for the font if `true`. Defaults to `false`.  */
        var genMipMaps = false
        /** The [TextureFilter] to use when scaling down the [BitmapFont]. Defaults to [TextureFilter.Nearest].  */
        var minFilter: com.badlogic.gdx.graphics.Texture.TextureFilter? = com.badlogic.gdx.graphics.Texture.TextureFilter.Nearest
        /** The [TextureFilter] to use when scaling up the [BitmapFont]. Defaults to [TextureFilter.Nearest].  */
        var magFilter: com.badlogic.gdx.graphics.Texture.TextureFilter? = com.badlogic.gdx.graphics.Texture.TextureFilter.Nearest
        /** optional [BitmapFontData] to be used instead of loading the [Texture] directly. Use this if your font is
         * embedded in a [Skin].  */
        var bitmapFontData: com.badlogic.gdx.graphics.g2d.BitmapFont.BitmapFontData? = null
        /** The name of the [TextureAtlas] to load the [BitmapFont] itself from. Optional; if `null`, will look for
         * a separate image  */
        var atlasName: String? = null
    }
}
