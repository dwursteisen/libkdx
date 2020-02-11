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
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.model.data.ModelData
import com.badlogic.gdx.graphics.g3d.utils.TextureProvider
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.ObjectMap
import kotlin.jvm.JvmOverloads
import kotlin.reflect.KClass

abstract class ModelLoader(resolver: FileHandleResolver) : AsynchronousAssetLoader<Model, ModelLoader.ModelParameters>(resolver) {

    protected var items: Array<ObjectMap.Entry<String?, ModelData?>> = Array()
    protected var defaultParameters: ModelParameters = ModelParameters()

    /** Directly load the raw model data on the calling thread.  */
    abstract fun loadModelData(
        fileHandle: FileHandle,
        parameters: ModelParameters?
    ): ModelData?

    /** Directly load the raw model data on the calling thread.  */
    fun loadModelData(fileHandle: FileHandle): ModelData? {
        return loadModelData(
            fileHandle,
            null
        )
    }
    /** Directly load the model on the calling thread. The model with not be managed by an [AssetManager].  */
    /** Directly load the model on the calling thread. The model with not be managed by an [AssetManager].  */
    /** Directly load the model on the calling thread. The model with not be managed by an [AssetManager].  */
    @JvmOverloads
    fun loadModel(
        fileHandle: FileHandle,
        textureProvider: TextureProvider = TextureProvider.FileTextureProvider(),
        parameters: ModelParameters? = null
    ): Model? {
        val data: ModelData? = loadModelData(fileHandle, parameters)
        return if (data == null) null else Model(data, textureProvider)
    }

    /** Directly load the model on the calling thread. The model with not be managed by an [AssetManager].  */
    fun loadModel(fileHandle: FileHandle, parameters: ModelParameters?): Model? {
        return loadModel(fileHandle, TextureProvider.FileTextureProvider(), parameters)
    }

    override fun getDependencies(fileName: String, file: FileHandle, parameter: ModelParameters?): Array<AssetDescriptor<*>>? {
        val deps: Array<AssetDescriptor<*>> = Array()
        val data: ModelData = loadModelData(file, parameter) ?: return deps
        val item: ObjectMap.Entry<String?, ModelData?> = ObjectMap.Entry()
        item.key = fileName
        item.value = data
        synchronized(items) { items.add(item) }
        val textureParameter: TextureLoader.TextureParameter = parameter?.textureParameter ?: defaultParameters.textureParameter
        for (modelMaterial in data.materials!!) {
            if (modelMaterial!!.textures != null) {
                for (modelTexture in modelMaterial.textures!!) {
                    val kClass: KClass<*> = Texture::class
                    deps.add(AssetDescriptor(
                        modelTexture!!.fileName!!,
                        kClass,
                        textureParameter
                    ))
                }
            }
        }
        return deps
    }

    override fun loadAsync(
        manager: com.badlogic.gdx.assets.AssetManager,
        fileName: String,
        file: FileHandle,
        parameter: ModelParameters?
    ) {}

    override fun loadSync(
        manager: com.badlogic.gdx.assets.AssetManager,
        fileName: String,
        file: FileHandle,
        parameter: ModelParameters?
    ): Model? {
        var data: ModelData? = null
        synchronized(items) {
            for (i in 0 until items.size) {
                if (items.get(i).key == fileName) {
                    data = items.get(i).value
                    items.removeIndex(i)
                }
            }
        }
        if (data == null) return null
        val result: Model = Model(data, TextureProvider.AssetTextureProvider(manager))
        // need to remove the textures from the managed disposables, or else ref counting
// doesn't work!
        val disposables: MutableIterator<Disposable> = result.managedDisposables.iterator()
        while (disposables.hasNext()) {
            val disposable: Disposable? = disposables.next()
            if (disposable is Texture) {
                disposables.remove()
            }
        }
        data = null
        return result
    }

    class ModelParameters : AssetLoaderParameters<Model>() {

        var textureParameter: TextureLoader.TextureParameter = TextureLoader.TextureParameter()

        init {
            textureParameter.magFilter = Texture.TextureFilter.Linear
            textureParameter.minFilter = textureParameter.magFilter
            textureParameter.wrapV = Texture.TextureWrap.Repeat
            textureParameter.wrapU = textureParameter.wrapV
        }
    }
}
