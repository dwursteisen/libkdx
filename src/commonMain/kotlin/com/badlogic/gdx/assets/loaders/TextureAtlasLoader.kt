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
import com.badlogic.gdx.assets.loaders.TextureAtlasLoader.TextureAtlasParameter
import java.util.Locale

/** [AssetLoader] to load [TextureAtlas] instances. Passing a [TextureAtlasParameter] to
 * [AssetManager.load] allows to specify whether the atlas regions should be flipped
 * on the y-axis or not.
 * @author mzechner
 */
class TextureAtlasLoader(resolver: com.badlogic.gdx.assets.loaders.FileHandleResolver?) : com.badlogic.gdx.assets.loaders.SynchronousAssetLoader<com.badlogic.gdx.graphics.g2d.TextureAtlas?, TextureAtlasParameter?>(resolver) {

    var data: com.badlogic.gdx.graphics.g2d.TextureAtlas.TextureAtlasData? = null
    override fun load(assetManager: com.badlogic.gdx.assets.AssetManager?, fileName: String?, file: com.badlogic.gdx.files.FileHandle?, parameter: TextureAtlasParameter?): com.badlogic.gdx.graphics.g2d.TextureAtlas? {
        for (page in data.getPages()) {
            val texture: com.badlogic.gdx.graphics.Texture = assetManager.get(page.textureFile.path().replace("\\\\".toRegex(), "/"), com.badlogic.gdx.graphics.Texture::class.java)
            page.texture = texture
        }
        val atlas: com.badlogic.gdx.graphics.g2d.TextureAtlas = com.badlogic.gdx.graphics.g2d.TextureAtlas(data)
        data = null
        return atlas
    }

    override fun getDependencies(fileName: String?, atlasFile: com.badlogic.gdx.files.FileHandle?, parameter: TextureAtlasParameter?): com.badlogic.gdx.utils.Array<com.badlogic.gdx.assets.AssetDescriptor<*>?>? {
        val imgDir: com.badlogic.gdx.files.FileHandle = atlasFile.parent()
        data = if (parameter != null) com.badlogic.gdx.graphics.g2d.TextureAtlas.TextureAtlasData(atlasFile, imgDir, parameter.flip) else {
            com.badlogic.gdx.graphics.g2d.TextureAtlas.TextureAtlasData(atlasFile, imgDir, false)
        }
        val dependencies: com.badlogic.gdx.utils.Array<com.badlogic.gdx.assets.AssetDescriptor<*>?> = com.badlogic.gdx.utils.Array()
        for (page in data.getPages()) {
            val params: com.badlogic.gdx.assets.loaders.TextureLoader.TextureParameter = com.badlogic.gdx.assets.loaders.TextureLoader.TextureParameter()
            params!!.format = page.format
            params!!.genMipMaps = page.useMipMaps
            params!!.minFilter = page.minFilter
            params!!.magFilter = page.magFilter
            dependencies.add(com.badlogic.gdx.assets.AssetDescriptor<Any?>(page.textureFile, com.badlogic.gdx.graphics.Texture::class.java, params))
        }
        return dependencies
    }

    class TextureAtlasParameter : AssetLoaderParameters<com.badlogic.gdx.graphics.g2d.TextureAtlas?> {
        /** whether to flip the texture atlas vertically  */
        var flip = false

        constructor() {}
        constructor(flip: Boolean) {
            this.flip = flip
        }
    }
}
