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
import java.util.Locale

/** [AssetLoader] for [Pixmap] instances. The Pixmap is loaded asynchronously.
 * @author mzechner
 */
class PixmapLoader(resolver: com.badlogic.gdx.assets.loaders.FileHandleResolver?) : com.badlogic.gdx.assets.loaders.AsynchronousAssetLoader<com.badlogic.gdx.graphics.Pixmap?, PixmapLoader.PixmapParameter?>(resolver) {

    var pixmap: com.badlogic.gdx.graphics.Pixmap? = null
    override fun loadAsync(manager: com.badlogic.gdx.assets.AssetManager?, fileName: String?, file: com.badlogic.gdx.files.FileHandle?, parameter: PixmapParameter?) {
        pixmap = null
        pixmap = com.badlogic.gdx.graphics.Pixmap(file)
    }

    override fun loadSync(manager: com.badlogic.gdx.assets.AssetManager?, fileName: String?, file: com.badlogic.gdx.files.FileHandle?, parameter: PixmapParameter?): com.badlogic.gdx.graphics.Pixmap? {
        val pixmap: com.badlogic.gdx.graphics.Pixmap? = pixmap
        this.pixmap = null
        return pixmap
    }

    override fun getDependencies(fileName: String?, file: com.badlogic.gdx.files.FileHandle?, parameter: PixmapParameter?): com.badlogic.gdx.utils.Array<com.badlogic.gdx.assets.AssetDescriptor<*>?>? {
        return null
    }

    class PixmapParameter : AssetLoaderParameters<com.badlogic.gdx.graphics.Pixmap?>()
}
