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

abstract class ModelLoader<P : ModelLoader.ModelParameters?>(resolver: com.badlogic.gdx.assets.loaders.FileHandleResolver?) : com.badlogic.gdx.assets.loaders.AsynchronousAssetLoader<com.badlogic.gdx.graphics.g3d.Model?, P?>(resolver) {
    protected var items: com.badlogic.gdx.utils.Array<com.badlogic.gdx.utils.ObjectMap.Entry<String?, com.badlogic.gdx.graphics.g3d.model.data.ModelData?>?>? = com.badlogic.gdx.utils.Array()
    protected var defaultParameters: ModelParameters? = ModelParameters()
    /** Directly load the raw model data on the calling thread.  */
    abstract fun loadModelData(fileHandle: com.badlogic.gdx.files.FileHandle?, parameters: P?): com.badlogic.gdx.graphics.g3d.model.data.ModelData?

    /** Directly load the raw model data on the calling thread.  */
    fun loadModelData(fileHandle: com.badlogic.gdx.files.FileHandle?): com.badlogic.gdx.graphics.g3d.model.data.ModelData? {
        return loadModelData(fileHandle, null)
    }
    /** Directly load the model on the calling thread. The model with not be managed by an [AssetManager].  */
    /** Directly load the model on the calling thread. The model with not be managed by an [AssetManager].  */
    /** Directly load the model on the calling thread. The model with not be managed by an [AssetManager].  */
    @JvmOverloads
    fun loadModel(fileHandle: com.badlogic.gdx.files.FileHandle?, textureProvider: com.badlogic.gdx.graphics.g3d.utils.TextureProvider? = com.badlogic.gdx.graphics.g3d.utils.TextureProvider.FileTextureProvider(), parameters: P? = null): com.badlogic.gdx.graphics.g3d.Model? {
        val data: com.badlogic.gdx.graphics.g3d.model.data.ModelData? = loadModelData(fileHandle, parameters)
        return if (data == null) null else com.badlogic.gdx.graphics.g3d.Model(data, textureProvider)
    }

    /** Directly load the model on the calling thread. The model with not be managed by an [AssetManager].  */
    fun loadModel(fileHandle: com.badlogic.gdx.files.FileHandle?, parameters: P?): com.badlogic.gdx.graphics.g3d.Model? {
        return loadModel(fileHandle, com.badlogic.gdx.graphics.g3d.utils.TextureProvider.FileTextureProvider(), parameters)
    }

    override fun getDependencies(fileName: String?, file: com.badlogic.gdx.files.FileHandle?, parameters: P?): com.badlogic.gdx.utils.Array<com.badlogic.gdx.assets.AssetDescriptor<*>?>? {
        val deps: com.badlogic.gdx.utils.Array<com.badlogic.gdx.assets.AssetDescriptor<*>?> = com.badlogic.gdx.utils.Array()
        val data: com.badlogic.gdx.graphics.g3d.model.data.ModelData = loadModelData(file, parameters) ?: return deps
        val item: com.badlogic.gdx.utils.ObjectMap.Entry<String?, com.badlogic.gdx.graphics.g3d.model.data.ModelData?> = com.badlogic.gdx.utils.ObjectMap.Entry()
        item.key = fileName
        item.value = data
        synchronized(items) { items.add(item) }
        val textureParameter: com.badlogic.gdx.assets.loaders.TextureLoader.TextureParameter = if (parameters != null) parameters.textureParameter else defaultParameters!!.textureParameter
        for (modelMaterial in data.materials) {
            if (modelMaterial.textures != null) {
                for (modelTexture in modelMaterial.textures) deps.add(com.badlogic.gdx.assets.AssetDescriptor<Any?>(modelTexture.fileName, com.badlogic.gdx.graphics.Texture::class.java, textureParameter))
            }
        }
        return deps
    }

    override fun loadAsync(manager: com.badlogic.gdx.assets.AssetManager?, fileName: String?, file: com.badlogic.gdx.files.FileHandle?, parameters: P?) {}
    override fun loadSync(manager: com.badlogic.gdx.assets.AssetManager?, fileName: String?, file: com.badlogic.gdx.files.FileHandle?, parameters: P?): com.badlogic.gdx.graphics.g3d.Model? {
        var data: com.badlogic.gdx.graphics.g3d.model.data.ModelData? = null
        synchronized(items) {
            for (i in 0 until items.size) {
                if (items.get(i).key == fileName) {
                    data = items.get(i).value
                    items.removeIndex(i)
                }
            }
        }
        if (data == null) return null
        val result: com.badlogic.gdx.graphics.g3d.Model = com.badlogic.gdx.graphics.g3d.Model(data, com.badlogic.gdx.graphics.g3d.utils.TextureProvider.AssetTextureProvider(manager))
        // need to remove the textures from the managed disposables, or else ref counting
// doesn't work!
        val disposables: MutableIterator<com.badlogic.gdx.utils.Disposable?> = result.getManagedDisposables().iterator()
        while (disposables.hasNext()) {
            val disposable: com.badlogic.gdx.utils.Disposable? = disposables.next()
            if (disposable is com.badlogic.gdx.graphics.Texture) {
                disposables.remove()
            }
        }
        data = null
        return result
    }

    open class ModelParameters : AssetLoaderParameters<com.badlogic.gdx.graphics.g3d.Model?>() {
        var textureParameter: com.badlogic.gdx.assets.loaders.TextureLoader.TextureParameter?

        init {
            textureParameter = com.badlogic.gdx.assets.loaders.TextureLoader.TextureParameter()
            textureParameter.magFilter = com.badlogic.gdx.graphics.Texture.TextureFilter.Linear
            textureParameter.minFilter = textureParameter.magFilter
            textureParameter.wrapV = com.badlogic.gdx.graphics.Texture.TextureWrap.Repeat
            textureParameter.wrapU = textureParameter.wrapV
        }
    }
}
