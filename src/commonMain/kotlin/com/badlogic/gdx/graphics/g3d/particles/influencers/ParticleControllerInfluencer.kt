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
import com.badlogic.gdx.graphics.g3d.particles.influencers.ModelInfluencer
import com.badlogic.gdx.graphics.g3d.particles.influencers.ModelInfluencer.Random.ModelInstancePool
import com.badlogic.gdx.graphics.g3d.particles.influencers.ParticleControllerFinalizerInfluencer
import com.badlogic.gdx.graphics.g3d.particles.influencers.RegionInfluencer
import com.badlogic.gdx.graphics.g3d.particles.influencers.RegionInfluencer.Animated
import com.badlogic.gdx.graphics.g3d.particles.influencers.RegionInfluencer.AspectTextureRegion
import com.badlogic.gdx.graphics.g3d.particles.influencers.ScaleInfluencer
import com.badlogic.gdx.graphics.g3d.particles.influencers.SimpleInfluencer
import com.badlogic.gdx.graphics.g3d.particles.influencers.SpawnInfluencer
import java.lang.RuntimeException

/**
 * It's an [Influencer] which controls which [ParticleController] will be assigned to a particle.
 *
 * @author Inferno
 */
abstract class ParticleControllerInfluencer : Influencer {

    /**
     * Assigns the first controller of [ParticleControllerInfluencer.templates] to the particles.
     */
    class Single : ParticleControllerInfluencer {

        constructor(vararg templates: ParticleController?) : super(*templates) {}
        constructor() : super() {}
        constructor(particleControllerSingle: Single) : super(particleControllerSingle) {}

        fun init() {
            val first: ParticleController = templates.first()
            var i = 0
            val c: Int = controller.particles.capacity
            while (i < c) {
                val copy: ParticleController = first.copy()
                copy.init()
                particleControllerChannel.data.get(i) = copy
                ++i
            }
        }

        fun activateParticles(startIndex: Int, count: Int) {
            var i = startIndex
            val c = startIndex + count
            while (i < c) {
                particleControllerChannel.data.get(i).start()
                ++i
            }
        }

        fun killParticles(startIndex: Int, count: Int) {
            var i = startIndex
            val c = startIndex + count
            while (i < c) {
                particleControllerChannel.data.get(i).end()
                ++i
            }
        }

        fun copy(): Single {
            return Single(this)
        }
    }

    /**
     * Assigns a random controller of [ParticleControllerInfluencer.templates] to the particles.
     */
    class Random : ParticleControllerInfluencer {

        inner class ParticleControllerPool : Pool<ParticleController?>() {
            fun newObject(): ParticleController {
                val controller: ParticleController = templates.random().copy()
                controller.init()
                return controller
            }

            fun clear() {
                // Dispose every allocated instance because the templates may be changed
                var i = 0
                val free: Int = pool.getFree()
                while (i < free) {
                    pool.obtain().dispose()
                    ++i
                }
                super.clear()
            }
        }

        var pool: ParticleControllerPool

        constructor() : super() {
            pool = ParticleControllerPool()
        }

        constructor(vararg templates: ParticleController?) : super(*templates) {
            pool = ParticleControllerPool()
        }

        constructor(particleControllerRandom: Random) : super(particleControllerRandom) {
            pool = ParticleControllerPool()
        }

        fun init() {
            pool.clear()
            // Allocate the new instances
            for (i in 0 until controller.emitter.maxParticleCount) {
                pool.free(pool.newObject())
            }
        }

        override fun dispose() {
            pool.clear()
            super.dispose()
        }

        fun activateParticles(startIndex: Int, count: Int) {
            var i = startIndex
            val c = startIndex + count
            while (i < c) {
                val controller: ParticleController = pool.obtain()
                controller.start()
                particleControllerChannel.data.get(i) = controller
                ++i
            }
        }

        fun killParticles(startIndex: Int, count: Int) {
            var i = startIndex
            val c = startIndex + count
            while (i < c) {
                val controller: ParticleController = particleControllerChannel.data.get(i)
                controller.end()
                pool.free(controller)
                particleControllerChannel.data.get(i) = null
                ++i
            }
        }

        fun copy(): Random {
            return Random(this)
        }
    }

    var templates: Array<ParticleController>? = null
    var particleControllerChannel: ObjectChannel<ParticleController>? = null

    constructor() {
        templates = Array<ParticleController>(true, 1, ParticleController::class.java)
    }

    constructor(vararg templates: ParticleController?) {
        this.templates = Array<ParticleController>(templates)
    }

    constructor(influencer: ParticleControllerInfluencer) : this(influencer.templates.items) {}

    fun allocateChannels() {
        particleControllerChannel = controller.particles.addChannel(ParticleChannels.ParticleController)
    }

    fun end() {
        for (i in 0 until controller.particles.size) {
            particleControllerChannel.data.get(i).end()
        }
    }

    fun dispose() {
        if (controller != null) {
            for (i in 0 until controller.particles.size) {
                val controller: ParticleController = particleControllerChannel.data.get(i)
                if (controller != null) {
                    controller.dispose()
                    particleControllerChannel.data.get(i) = null
                }
            }
        }
    }

    fun save(manager: AssetManager, resources: ResourceData) {
        val data: SaveData = resources.createSaveData()
        val effects: Array<ParticleEffect> = manager.getAll(ParticleEffect::class.java, Array<ParticleEffect>())
        val controllers: Array<ParticleController> = Array<ParticleController>(templates)
        val effectsIndices = Array<IntArray>()
        var i = 0
        while (i < effects.size && controllers.size > 0) {
            val effect: ParticleEffect = effects[i]
            val effectControllers: Array<ParticleController> = effect.getControllers()
            val iterator: Iterator<ParticleController> = controllers.iterator()
            var indices: IntArray? = null
            while (iterator.hasNext()) {
                val controller: ParticleController = iterator.next()
                var index = -1
                if (effectControllers.indexOf(controller, true).also { index = it } > -1) {
                    if (indices == null) {
                        indices = IntArray()
                    }
                    iterator.remove()
                    indices.add(index)
                }
            }
            if (indices != null) {
                data.saveAsset(manager.getAssetFileName(effect), ParticleEffect::class.java)
                effectsIndices.add(indices)
            }
            ++i
        }
        data.save("indices", effectsIndices)
    }

    fun load(manager: AssetManager, resources: ResourceData) {
        val data: SaveData = resources.getSaveData()
        val effectsIndices: Array<IntArray> = data.load("indices")
        var descriptor: AssetDescriptor?
        val iterator = effectsIndices.iterator()
        while (data.loadAsset().also({ descriptor = it }) != null) {
            val effect: ParticleEffect = manager.get(descriptor) as ParticleEffect
                ?: throw RuntimeException("Template is null")
            val effectControllers: Array<ParticleController> = effect.getControllers()
            val effectIndices = iterator.next()
            var i = 0
            val n = effectIndices.size
            while (i < n) {
                templates.add(effectControllers[effectIndices[i]])
                i++
            }
        }
    }
}
