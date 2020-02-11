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
import com.badlogic.gdx.files.FileHandle

/** Base class for asynchronous [AssetLoader] instances. Such loaders try to load parts of an OpenGL resource, like the
 * Pixmap, on a separate thread to then load the actual resource on the thread the OpenGL context is active on.
 * @author mzechner
 *
 * @param <T>
 * @param <P>
</P></T> */
abstract class AsynchronousAssetLoader<T, P : AssetLoaderParameters<T>>(resolver: FileHandleResolver) : AssetLoader<T, P>(resolver) {

    /** Loads the non-OpenGL part of the asset and injects any dependencies of the asset into the AssetManager.
     * @param manager
     * @param fileName the name of the asset to load
     * @param file the resolved file to load
     * @param parameter the parameters to use for loading the asset
     */
    abstract fun loadAsync(
        manager: com.badlogic.gdx.assets.AssetManager,
        fileName: String,
        file: FileHandle,
        parameter: P?
    )

    /** Loads the OpenGL part of the asset.
     * @param manager
     * @param fileName
     * @param file the resolved file to load
     * @param parameter
     */
    abstract fun loadSync(
        manager: com.badlogic.gdx.assets.AssetManager,
        fileName: String,
        file: FileHandle,
        parameter: P?
    ): T?
}
