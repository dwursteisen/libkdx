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
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.files.FileHandle

/** [AssetLoader] for [Music] instances. The Music instance is loaded synchronously.
 * @author mzechner
 */
class MusicLoader(resolver: FileHandleResolver) : AsynchronousAssetLoader<Music?, MusicLoader.MusicParameter>(resolver) {

    private var music: Music? = null

    /** Returns the [Music] instance currently loaded by this
     * [MusicLoader].
     *
     * @return the currently loaded [Music], otherwise `null` if
     * no [Music] has been loaded yet.
     */
    protected val loadedMusic: Music?
        protected get() = music

    override fun loadAsync(
        manager: AssetManager,
        fileName: String,
        file: FileHandle,
        parameter: MusicParameter?
    ) {
        music = com.badlogic.gdx.Gdx.audio.newMusic(file)
    }

    override fun loadSync(
        manager: AssetManager,
        fileName: String,
        file: FileHandle,
        parameter: MusicParameter?
    ): Music? {
        val music: Music? = music
        this.music = null
        return music
    }

    override fun getDependencies(
        fileName: String,
        file: FileHandle,
        parameter: MusicParameter?
    ): com.badlogic.gdx.utils.Array<com.badlogic.gdx.assets.AssetDescriptor<*>>? {
        return null
    }

    class MusicParameter : AssetLoaderParameters<Music?>()
}
