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

/** [AssetLoader] for [Music] instances. The Music instance is loaded synchronously.
 * @author mzechner
 */
class MusicLoader(resolver: com.badlogic.gdx.assets.loaders.FileHandleResolver?) : com.badlogic.gdx.assets.loaders.AsynchronousAssetLoader<com.badlogic.gdx.audio.Music?, MusicLoader.MusicParameter?>(resolver) {

    private var music: com.badlogic.gdx.audio.Music? = null
    /** Returns the [Music] instance currently loaded by this
     * [MusicLoader].
     *
     * @return the currently loaded [Music], otherwise `null` if
     * no [Music] has been loaded yet.
     */
    protected val loadedMusic: com.badlogic.gdx.audio.Music?
        protected get() = music

    override fun loadAsync(manager: com.badlogic.gdx.assets.AssetManager?, fileName: String?, file: com.badlogic.gdx.files.FileHandle?, parameter: MusicParameter?) {
        music = com.badlogic.gdx.Gdx.audio.newMusic(file)
    }

    override fun loadSync(manager: com.badlogic.gdx.assets.AssetManager?, fileName: String?, file: com.badlogic.gdx.files.FileHandle?, parameter: MusicParameter?): com.badlogic.gdx.audio.Music? {
        val music: com.badlogic.gdx.audio.Music? = music
        this.music = null
        return music
    }

    override fun getDependencies(fileName: String?, file: com.badlogic.gdx.files.FileHandle?, parameter: MusicParameter?): com.badlogic.gdx.utils.Array<com.badlogic.gdx.assets.AssetDescriptor<*>?>? {
        return null
    }

    class MusicParameter : AssetLoaderParameters<com.badlogic.gdx.audio.Music?>()
}
