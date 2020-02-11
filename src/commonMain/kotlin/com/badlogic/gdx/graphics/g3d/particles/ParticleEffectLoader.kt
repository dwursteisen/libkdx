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
package com.badlogic.gdx.graphics.g3d.particles

import com.badlogic.gdx.graphics.g3d.particles.ParallelArray.ChannelDescriptor
import com.badlogic.gdx.graphics.g3d.particles.ParallelArray.ChannelInitializer
import com.badlogic.gdx.graphics.g3d.particles.ParallelArray.FloatChannel
import com.badlogic.gdx.graphics.g3d.particles.ParallelArray.IntChannel
import com.badlogic.gdx.graphics.g3d.particles.ParallelArray.ObjectChannel
import com.badlogic.gdx.graphics.g3d.particles.ParticleChannels
import com.badlogic.gdx.graphics.g3d.particles.ParticleChannels.ColorInitializer
import com.badlogic.gdx.graphics.g3d.particles.ParticleChannels.Rotation2dInitializer
import com.badlogic.gdx.graphics.g3d.particles.ParticleChannels.Rotation3dInitializer
import com.badlogic.gdx.graphics.g3d.particles.ParticleChannels.ScaleInitializer
import com.badlogic.gdx.graphics.g3d.particles.ParticleChannels.TextureRegionInitializer
import com.badlogic.gdx.graphics.g3d.particles.ParticleController
import com.badlogic.gdx.graphics.g3d.particles.ParticleControllerComponent
import com.badlogic.gdx.graphics.g3d.particles.ParticleEffectLoader.ParticleEffectLoadParameter
import com.badlogic.gdx.graphics.g3d.particles.ParticleEffectLoader.ParticleEffectSaveParameter
import kotlin.jvm.Throws

/**
 * This class can save and load a [ParticleEffect]. It should be added as [AsynchronousAssetLoader] to the
 * [AssetManager] so it will be able to load the effects. It's important to note that the two classes
 * [ParticleEffectLoadParameter] and [ParticleEffectSaveParameter] should be passed in whenever possible, because when
 * present the batches settings will be loaded automatically. When the load and save parameters are absent, once the effect will
 * be created, one will have to set the required batches manually otherwise the [ParticleController] instances contained
 * inside the effect will not be able to render themselves.
 *
 * @author inferno
 */
class ParticleEffectLoader(resolver: FileHandleResolver?) : AsynchronousAssetLoader<ParticleEffect?, ParticleEffectLoadParameter?>(resolver) {

    protected var items: Array<ObjectMap.Entry<String?, ResourceData<ParticleEffect?>?>?>? = Array<ObjectMap.Entry<String?, ResourceData<ParticleEffect?>?>?>()
    fun loadAsync(manager: AssetManager?, fileName: String?, file: FileHandle?, parameter: ParticleEffectLoadParameter?) {}
    fun getDependencies(fileName: String?, file: FileHandle?, parameter: ParticleEffectLoadParameter?): Array<AssetDescriptor?>? {
        val json = Json()
        val data: ResourceData<ParticleEffect?> = json.fromJson(ResourceData::class.java, file)
        var assets: Array<AssetData?>? = null
        synchronized(items!!) {
            val entry: ObjectMap.Entry<String?, ResourceData<ParticleEffect?>?> = Entry<String?, ResourceData<ParticleEffect?>?>()
            entry.key = fileName
            entry.value = data
            items.add(entry)
            assets = data.assets
        }
        val descriptors: Array<AssetDescriptor?> = Array<AssetDescriptor?>()
        for (assetData in assets!!) {

            // If the asset doesn't exist try to load it from loading effect directory
            if (!resolve(assetData.filename).exists()) {
                assetData.filename = file.parent().child(Gdx.files.internal(assetData.filename).name()).path()
            }
            if (assetData.type === ParticleEffect::class.java) {
                descriptors.add(AssetDescriptor(assetData.filename, assetData.type, parameter))
            } else descriptors.add(AssetDescriptor(assetData.filename, assetData.type))
        }
        return descriptors
    }

    /**
     * Saves the effect to the given file contained in the passed in parameter.
     */
    @Throws(IOException::class)
    fun save(effect: ParticleEffect?, parameter: ParticleEffectSaveParameter?) {
        val data: ResourceData<ParticleEffect?> = ResourceData<ParticleEffect?>(effect)

        // effect assets
        effect!!.save(parameter!!.manager, data)

        // Batches configurations
        if (parameter.batches != null) {
            for (batch in parameter.batches!!) {
                var save = false
                for (controller in effect!!.getControllers()) {
                    if (controller!!.renderer.isCompatible(batch)) {
                        save = true
                        break
                    }
                }
                if (save) batch.save(parameter.manager, data)
            }
        }

        // save
        val json = Json()
        json.toJson(data, parameter.file)
    }

    fun loadSync(manager: AssetManager?, fileName: String?, file: FileHandle?, parameter: ParticleEffectLoadParameter?): ParticleEffect? {
        var effectData: ResourceData<ParticleEffect?>? = null
        synchronized(items!!) {
            for (i in 0 until items!!.size) {
                val entry: ObjectMap.Entry<String?, ResourceData<ParticleEffect?>?>? = items!![i]
                if (entry.key.equals(fileName)) {
                    effectData = entry.value
                    items.removeIndex(i)
                    break
                }
            }
        }
        effectData!!.resource!!.load(manager, effectData)
        if (parameter != null) {
            if (parameter.batches != null) {
                for (batch in parameter.batches!!) {
                    batch.load(manager, effectData)
                }
            }
            effectData!!.resource!!.setBatch(parameter.batches)
        }
        return effectData!!.resource
    }

    private fun <T> find(array: Array<*>?, type: java.lang.Class<T?>?): T? {
        for (`object` in array!!) {
            if (ClassReflection.isAssignableFrom(type, `object`.javaClass)) return `object` as T?
        }
        return null
    }

    class ParticleEffectLoadParameter(batches: Array<ParticleBatch<*>?>?) : AssetLoaderParameters<ParticleEffect?>() {
        var batches: Array<ParticleBatch<*>?>?

        init {
            this.batches = batches
        }
    }

    class ParticleEffectSaveParameter(file: FileHandle?, manager: AssetManager?, batches: Array<ParticleBatch<*>?>?) : AssetLoaderParameters<ParticleEffect?>() {
        /**
         * Optional parameters, but should be present to correctly load the settings
         */
        var batches: Array<ParticleBatch<*>?>?

        /**
         * Required parameters
         */
        var file: FileHandle?
        var manager: AssetManager?

        init {
            this.batches = batches
            this.file = file
            this.manager = manager
        }
    }
}
