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

/** [AssetLoader] to load [Sound] instances.
 * @author mzechner
 */
class SoundLoader(resolver: com.badlogic.gdx.assets.loaders.FileHandleResolver?) : com.badlogic.gdx.assets.loaders.AsynchronousAssetLoader<com.badlogic.gdx.audio.Sound?, SoundLoader.SoundParameter?>(resolver) {

    private var sound: com.badlogic.gdx.audio.Sound? = null
    /** Returns the [Sound] instance currently loaded by this
     * [SoundLoader].
     *
     * @return the currently loaded [Sound], otherwise `null` if
     * no [Sound] has been loaded yet.
     */
    protected val loadedSound: com.badlogic.gdx.audio.Sound?
        protected get() = sound

    override fun loadAsync(manager: com.badlogic.gdx.assets.AssetManager?, fileName: String?, file: com.badlogic.gdx.files.FileHandle?, parameter: SoundParameter?) {
        sound = com.badlogic.gdx.Gdx.audio.newSound(file)
    }

    override fun loadSync(manager: com.badlogic.gdx.assets.AssetManager?, fileName: String?, file: com.badlogic.gdx.files.FileHandle?, parameter: SoundParameter?): com.badlogic.gdx.audio.Sound? {
        val sound: com.badlogic.gdx.audio.Sound? = sound
        this.sound = null
        return sound
    }

    override fun getDependencies(fileName: String?, file: com.badlogic.gdx.files.FileHandle?, parameter: SoundParameter?): com.badlogic.gdx.utils.Array<com.badlogic.gdx.assets.AssetDescriptor<*>?>? {
        return null
    }

    class SoundParameter : AssetLoaderParameters<com.badlogic.gdx.audio.Sound?>()
}
