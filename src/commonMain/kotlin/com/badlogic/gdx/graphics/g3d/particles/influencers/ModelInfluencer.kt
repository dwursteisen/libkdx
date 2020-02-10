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
package com.badlogic.gdx.graphics.g3d.particles.influencers

import com.badlogic.gdx.graphics.g3d.particles.influencers.ColorInfluencer
import com.badlogic.gdx.graphics.g3d.particles.influencers.DynamicsInfluencer
import com.badlogic.gdx.graphics.g3d.particles.influencers.DynamicsModifier
import com.badlogic.gdx.graphics.g3d.particles.influencers.DynamicsModifier.Angular
import com.badlogic.gdx.graphics.g3d.particles.influencers.DynamicsModifier.BrownianAcceleration
import com.badlogic.gdx.graphics.g3d.particles.influencers.DynamicsModifier.CentripetalAcceleration
import com.badlogic.gdx.graphics.g3d.particles.influencers.DynamicsModifier.FaceDirection
import com.badlogic.gdx.graphics.g3d.particles.influencers.DynamicsModifier.PolarAcceleration
import com.badlogic.gdx.graphics.g3d.particles.influencers.DynamicsModifier.Rotational2D
import com.badlogic.gdx.graphics.g3d.particles.influencers.DynamicsModifier.Rotational3D
import com.badlogic.gdx.graphics.g3d.particles.influencers.DynamicsModifier.Strength
import com.badlogic.gdx.graphics.g3d.particles.influencers.DynamicsModifier.TangentialAcceleration
import com.badlogic.gdx.graphics.g3d.particles.influencers.ParticleControllerFinalizerInfluencer
import com.badlogic.gdx.graphics.g3d.particles.influencers.ParticleControllerInfluencer
import com.badlogic.gdx.graphics.g3d.particles.influencers.ParticleControllerInfluencer.Random.ParticleControllerPool
import com.badlogic.gdx.graphics.g3d.particles.influencers.RegionInfluencer
import com.badlogic.gdx.graphics.g3d.particles.influencers.RegionInfluencer.Animated
import com.badlogic.gdx.graphics.g3d.particles.influencers.RegionInfluencer.AspectTextureRegion
import com.badlogic.gdx.graphics.g3d.particles.influencers.ScaleInfluencer
import com.badlogic.gdx.graphics.g3d.particles.influencers.SimpleInfluencer
import com.badlogic.gdx.graphics.g3d.particles.influencers.SpawnInfluencer
import java.lang.RuntimeException

/**
 * It's an [Influencer] which controls which [Model] will be assigned to the particles as [ModelInstance].
 *
 * @author Inferno
 */
abstract class ModelInfluencer : Influencer {

    /**
     * Assigns the first model of [ModelInfluencer.models] to the particles.
     */
    class Single : ModelInfluencer {

        constructor() : super() {}
        constructor(influencer: Single) : super(influencer) {}
        constructor(vararg models: Model?) : super(*models) {}

        fun init() {
            val first: Model = models.first()
            var i = 0
            val c: Int = controller.emitter.maxParticleCount
            while (i < c) {
                modelChannel.data.get(i) = ModelInstance(first)
                ++i
            }
        }

        fun copy(): Single {
            return Single(this)
        }
    }

    /**
     * Assigns a random model of [ModelInfluencer.models] to the particles.
     */
    class Random : ModelInfluencer {

        inner class ModelInstancePool : Pool<ModelInstance?>() {
            fun newObject(): ModelInstance {
                return ModelInstance(models.random())
            }
        }

        var pool: ModelInstancePool

        constructor() : super() {
            pool = ModelInstancePool()
        }

        constructor(influencer: Random) : super(influencer) {
            pool = ModelInstancePool()
        }

        constructor(vararg models: Model?) : super(*models) {
            pool = ModelInstancePool()
        }

        fun init() {
            pool.clear()
        }

        fun activateParticles(startIndex: Int, count: Int) {
            var i = startIndex
            val c = startIndex + count
            while (i < c) {
                modelChannel.data.get(i) = pool.obtain()
                ++i
            }
        }

        fun killParticles(startIndex: Int, count: Int) {
            var i = startIndex
            val c = startIndex + count
            while (i < c) {
                pool.free(modelChannel.data.get(i))
                modelChannel.data.get(i) = null
                ++i
            }
        }

        fun copy(): Random {
            return Random(this)
        }
    }

    var models: Array<Model>
    var modelChannel: ObjectChannel<ModelInstance>? = null

    constructor() {
        models = Array<Model>(true, 1, Model::class.java)
    }

    constructor(vararg models: Model?) {
        this.models = Array<Model>(models)
    }

    constructor(influencer: ModelInfluencer) : this(*influencer.models.toArray(Model::class.java) as Array<Model?>) {}

    fun allocateChannels() {
        modelChannel = controller.particles.addChannel(ParticleChannels.ModelInstance)
    }

    fun save(manager: AssetManager, resources: ResourceData) {
        val data: SaveData = resources.createSaveData()
        for (model in models) data.saveAsset(manager.getAssetFileName(model), Model::class.java)
    }

    fun load(manager: AssetManager, resources: ResourceData) {
        val data: SaveData = resources.getSaveData()
        var descriptor: AssetDescriptor?
        while (data.loadAsset().also({ descriptor = it }) != null) {
            val model: Model = manager.get(descriptor) as Model ?: throw RuntimeException("Model is null")
            models.add(model)
        }
    }
}
