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
import com.badlogic.gdx.assets.loaders.SkinLoader.SkinParameter
import java.util.Locale

/** [AssetLoader] for [Skin] instances. All [Texture] and [BitmapFont] instances will be loaded as
 * dependencies. Passing a [SkinParameter] allows the exact name of the texture associated with the skin to be specified.
 * Otherwise the skin texture is looked up just as with a call to [Skin.Skin]. A
 * [SkinParameter] also allows named resources to be set that will be added to the skin before loading the json file,
 * meaning that they can be referenced from inside the json file itself. This is useful for dynamic resources such as a BitmapFont
 * generated through FreeTypeFontGenerator.
 * @author Nathan Sweet
 */
class SkinLoader(resolver: com.badlogic.gdx.assets.loaders.FileHandleResolver?) : com.badlogic.gdx.assets.loaders.AsynchronousAssetLoader<com.badlogic.gdx.scenes.scene2d.ui.Skin?, SkinParameter?>(resolver) {

    override fun getDependencies(fileName: String?, file: com.badlogic.gdx.files.FileHandle?, parameter: SkinParameter?): com.badlogic.gdx.utils.Array<com.badlogic.gdx.assets.AssetDescriptor<*>?>? {
        val deps: com.badlogic.gdx.utils.Array<com.badlogic.gdx.assets.AssetDescriptor<*>?> = com.badlogic.gdx.utils.Array()
        if (parameter == null || parameter.textureAtlasPath == null) deps.add(com.badlogic.gdx.assets.AssetDescriptor<Any?>(file.pathWithoutExtension() + ".atlas", com.badlogic.gdx.graphics.g2d.TextureAtlas::class.java)) else if (parameter.textureAtlasPath != null) deps.add(com.badlogic.gdx.assets.AssetDescriptor<Any?>(parameter.textureAtlasPath, com.badlogic.gdx.graphics.g2d.TextureAtlas::class.java))
        return deps
    }

    override fun loadAsync(manager: com.badlogic.gdx.assets.AssetManager?, fileName: String?, file: com.badlogic.gdx.files.FileHandle?, parameter: SkinParameter?) {}
    override fun loadSync(manager: com.badlogic.gdx.assets.AssetManager?, fileName: String?, file: com.badlogic.gdx.files.FileHandle?, parameter: SkinParameter?): com.badlogic.gdx.scenes.scene2d.ui.Skin? {
        var textureAtlasPath: String? = file.pathWithoutExtension() + ".atlas"
        var resources: com.badlogic.gdx.utils.ObjectMap<String?, Any?>? = null
        if (parameter != null) {
            if (parameter.textureAtlasPath != null) {
                textureAtlasPath = parameter.textureAtlasPath
            }
            if (parameter.resources != null) {
                resources = parameter.resources
            }
        }
        val atlas: com.badlogic.gdx.graphics.g2d.TextureAtlas = manager.get(textureAtlasPath, com.badlogic.gdx.graphics.g2d.TextureAtlas::class.java)
        val skin: com.badlogic.gdx.scenes.scene2d.ui.Skin? = newSkin(atlas)
        if (resources != null) {
            for (entry in resources.entries()) {
                skin.add(entry.key, entry.value)
            }
        }
        skin.load(file)
        return skin
    }

    /** Override to allow subclasses of Skin to be loaded or the skin instance to be configured.
     * @param atlas The TextureAtlas that the skin will use.
     * @return A new Skin (or subclass of Skin) instance based on the provided TextureAtlas.
     */
    protected fun newSkin(atlas: com.badlogic.gdx.graphics.g2d.TextureAtlas?): com.badlogic.gdx.scenes.scene2d.ui.Skin? {
        return com.badlogic.gdx.scenes.scene2d.ui.Skin(atlas)
    }

    class SkinParameter @JvmOverloads constructor(val textureAtlasPath: String? = null, resources: com.badlogic.gdx.utils.ObjectMap<String?, Any?>? = null) : AssetLoaderParameters<com.badlogic.gdx.scenes.scene2d.ui.Skin?>() {
        val resources: com.badlogic.gdx.utils.ObjectMap<String?, Any?>?

        constructor(resources: com.badlogic.gdx.utils.ObjectMap<String?, Any?>?) : this(null, resources) {}

        init {
            this.resources = resources
        }
    }
}
