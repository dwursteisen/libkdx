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
import com.badlogic.gdx.assets.loaders.ParticleEffectLoader.ParticleEffectParameter
import java.util.Locale

/** [AssetLoader] to load [ParticleEffect] instances. Passing a [ParticleEffectParameter] to
 * [AssetManager.load] allows to specify an atlas file or an image directory to be
 * used for the effect's images. Per default images are loaded from the directory in which the effect file is found.  */
class ParticleEffectLoader(resolver: com.badlogic.gdx.assets.loaders.FileHandleResolver?) : com.badlogic.gdx.assets.loaders.SynchronousAssetLoader<com.badlogic.gdx.graphics.g2d.ParticleEffect?, ParticleEffectParameter?>(resolver) {

    override fun load(am: com.badlogic.gdx.assets.AssetManager?, fileName: String?, file: com.badlogic.gdx.files.FileHandle?, param: ParticleEffectParameter?): com.badlogic.gdx.graphics.g2d.ParticleEffect? {
        val effect: com.badlogic.gdx.graphics.g2d.ParticleEffect = com.badlogic.gdx.graphics.g2d.ParticleEffect()
        if (param != null && param.atlasFile != null) effect.load(file, am.get(param.atlasFile, com.badlogic.gdx.graphics.g2d.TextureAtlas::class.java), param.atlasPrefix) else if (param != null && param.imagesDir != null) effect.load(file, param.imagesDir) else effect.load(file, file.parent())
        return effect
    }

    override fun getDependencies(fileName: String?, file: com.badlogic.gdx.files.FileHandle?, param: ParticleEffectParameter?): com.badlogic.gdx.utils.Array<com.badlogic.gdx.assets.AssetDescriptor<*>?>? {
        var deps: com.badlogic.gdx.utils.Array<com.badlogic.gdx.assets.AssetDescriptor<*>?>? = null
        if (param != null && param.atlasFile != null) {
            deps = com.badlogic.gdx.utils.Array()
            deps.add(com.badlogic.gdx.assets.AssetDescriptor<com.badlogic.gdx.graphics.g2d.TextureAtlas?>(param.atlasFile, com.badlogic.gdx.graphics.g2d.TextureAtlas::class.java))
        }
        return deps
    }

    /** Parameter to be passed to [AssetManager.load] if additional configuration is
     * necessary for the [ParticleEffect].  */
    class ParticleEffectParameter : AssetLoaderParameters<com.badlogic.gdx.graphics.g2d.ParticleEffect?>() {

        /** Atlas file name.  */
        var atlasFile: String? = null
        /** Optional prefix to image names  */
        var atlasPrefix: String? = null
        /** Image directory.  */
        var imagesDir: com.badlogic.gdx.files.FileHandle? = null
    }
}
