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
package com.badlogic.gdx.assets

import com.badlogic.gdx.assets.AssetManager
import java.lang.Void
import kotlin.jvm.Throws

/**
 * Responsible for loading an asset through an [AssetLoader] based on an [AssetDescriptor].
 *
 * @author mzechner
 */
internal class AssetLoadingTask(manager: AssetManager?, assetDesc: AssetDescriptor<*>?, loader: AssetLoader?, threadPool: AsyncExecutor?) : AsyncTask<Void?> {

    var manager: AssetManager?
    val assetDesc: AssetDescriptor<*>?
    val loader: AssetLoader?
    val executor: AsyncExecutor?
    val startTime: Long

    @Volatile
    var asyncDone = false

    @Volatile
    var dependenciesLoaded = false

    @Volatile
    var dependencies: Array<AssetDescriptor<*>?>? = null

    @Volatile
    var depsFuture: AsyncResult<Void?>? = null

    @Volatile
    var loadFuture: AsyncResult<Void?>? = null

    @Volatile
    var asset: Any? = null
    var ticks = 0

    @Volatile
    var cancel = false

    /**
     * Loads parts of the asset asynchronously if the loader is an [AsynchronousAssetLoader].
     */
    @Throws(java.lang.Exception::class)
    fun call(): Void? {
        val asyncLoader: AsynchronousAssetLoader? = loader as AsynchronousAssetLoader?
        if (!dependenciesLoaded) {
            dependencies = asyncLoader.getDependencies(assetDesc!!.fileName, resolve(loader, assetDesc), assetDesc.params)
            if (dependencies != null) {
                removeDuplicates(dependencies)
                manager.injectDependencies(assetDesc.fileName, dependencies)
            } else {
                // if we have no dependencies, we load the async part of the task immediately.
                asyncLoader.loadAsync(manager, assetDesc.fileName, resolve(loader, assetDesc), assetDesc.params)
                asyncDone = true
            }
        } else {
            asyncLoader.loadAsync(manager, assetDesc!!.fileName, resolve(loader, assetDesc), assetDesc.params)
        }
        return null
    }

    /**
     * Updates the loading of the asset. In case the asset is loaded with an [AsynchronousAssetLoader], the loaders
     * [AsynchronousAssetLoader.loadAsync] method is first called on
     * a worker thread. Once this method returns, the rest of the asset is loaded on the rendering thread via
     * [AsynchronousAssetLoader.loadSync].
     *
     * @return true in case the asset was fully loaded, false otherwise
     * @throws GdxRuntimeException
     */
    fun update(): Boolean {
        ticks++
        if (loader is SynchronousAssetLoader) {
            handleSyncLoader()
        } else {
            handleAsyncLoader()
        }
        return asset != null
    }

    private fun handleSyncLoader() {
        val syncLoader: SynchronousAssetLoader? = loader as SynchronousAssetLoader?
        if (!dependenciesLoaded) {
            dependenciesLoaded = true
            dependencies = syncLoader.getDependencies(assetDesc!!.fileName, resolve(loader, assetDesc), assetDesc.params)
            if (dependencies == null) {
                asset = syncLoader.load(manager, assetDesc.fileName, resolve(loader, assetDesc), assetDesc.params)
                return
            }
            removeDuplicates(dependencies)
            manager.injectDependencies(assetDesc.fileName, dependencies)
        } else {
            asset = syncLoader.load(manager, assetDesc!!.fileName, resolve(loader, assetDesc), assetDesc.params)
        }
    }

    private fun handleAsyncLoader() {
        val asyncLoader: AsynchronousAssetLoader? = loader as AsynchronousAssetLoader?
        if (!dependenciesLoaded) {
            if (depsFuture == null) {
                depsFuture = executor.submit(this)
            } else {
                if (depsFuture.isDone()) {
                    try {
                        depsFuture.get()
                    } catch (e: java.lang.Exception) {
                        throw GdxRuntimeException("Couldn't load dependencies of asset: " + assetDesc!!.fileName, e)
                    }
                    dependenciesLoaded = true
                    if (asyncDone) {
                        asset = asyncLoader.loadSync(manager, assetDesc!!.fileName, resolve(loader, assetDesc), assetDesc.params)
                    }
                }
            }
        } else {
            if (loadFuture == null && !asyncDone) {
                loadFuture = executor.submit(this)
            } else {
                if (asyncDone) {
                    asset = asyncLoader.loadSync(manager, assetDesc!!.fileName, resolve(loader, assetDesc), assetDesc.params)
                } else if (loadFuture.isDone()) {
                    try {
                        loadFuture.get()
                    } catch (e: java.lang.Exception) {
                        throw GdxRuntimeException("Couldn't load asset: " + assetDesc!!.fileName, e)
                    }
                    asset = asyncLoader.loadSync(manager, assetDesc!!.fileName, resolve(loader, assetDesc), assetDesc.params)
                }
            }
        }
    }

    private fun resolve(loader: AssetLoader?, assetDesc: AssetDescriptor<*>?): FileHandle? {
        if (assetDesc!!.file == null) assetDesc.file = loader.resolve(assetDesc.fileName)
        return assetDesc.file
    }

    private fun removeDuplicates(array: Array<AssetDescriptor<*>?>?) {
        val ordered: Boolean = array.ordered
        array.ordered = true
        for (i in 0 until array!!.size) {
            val fn = array[i]!!.fileName
            val type: java.lang.Class? = array[i]!!.type
            for (j in array.size - 1 downTo i + 1) {
                if (type == array[j]!!.type && fn == array[j]!!.fileName) array.removeIndex(j)
            }
        }
        array.ordered = ordered
    }

    init {
        this.manager = manager
        this.assetDesc = assetDesc
        this.loader = loader
        executor = threadPool
        startTime = if (manager.log.getLevel() === Logger.DEBUG) TimeUtils.nanoTime() else 0.toLong()
    }
}
