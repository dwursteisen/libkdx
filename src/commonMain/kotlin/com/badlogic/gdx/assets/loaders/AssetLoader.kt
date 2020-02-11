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
import com.badlogic.gdx.files.FileHandle

/** Abstract base class for asset loaders.
 * @author mzechner
 *
 * @param <T> the class of the asset the loader supports
 * @param <P> the class of the loading parameters the loader supports.
</P></T> */
abstract class AssetLoader<T, P : AssetLoaderParameters<T>>(
    /** [FileHandleResolver] used to map from plain asset names to [FileHandle] instances  */
    private val resolver: FileHandleResolver
) {

    /** @param fileName file name to resolve
     * @return handle to the file, as resolved by the [FileHandleResolver] set on the loader
     */
    fun resolve(fileName: String): FileHandle {
        return resolver.resolve(fileName)
    }

    /** Returns the assets this asset requires to be loaded first. This method may be called on a thread other than the GL thread.
     * @param fileName name of the asset to load
     * @param file the resolved file to load
     * @param parameter parameters for loading the asset
     * @return other assets that the asset depends on and need to be loaded first or null if there are no dependencies.
     */
    abstract fun getDependencies(
        fileName: String,
        file: FileHandle,
        parameter: P?
    ): com.badlogic.gdx.utils.Array<AssetDescriptor<*>>?
}
