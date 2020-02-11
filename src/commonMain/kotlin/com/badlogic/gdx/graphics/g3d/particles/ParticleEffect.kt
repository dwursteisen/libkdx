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
 * It's a set of particles controllers. It can be updated, rendered, transformed which means the changes will be applied on all
 * the particles controllers.
 *
 * @author inferno
 */
class ParticleEffect : Disposable, ResourceData.Configurable<Any?> {

    private var controllers: Array<ParticleController?>?
    private var bounds: BoundingBox? = null

    constructor() {
        controllers = Array<ParticleController?>(true, 3, ParticleController::class.java)
    }

    constructor(effect: ParticleEffect?) {
        controllers = Array<ParticleController?>(true, effect!!.controllers!!.size)
        var i = 0
        val n = effect!!.controllers!!.size
        while (i < n) {
            controllers.add(effect.controllers!![i]!!.copy())
            i++
        }
    }

    constructor(vararg emitters: ParticleController?) {
        controllers = Array<ParticleController?>(emitters)
    }

    fun init() {
        var i = 0
        val n = controllers!!.size
        while (i < n) {
            controllers!![i]!!.init()
            i++
        }
    }

    fun start() {
        var i = 0
        val n = controllers!!.size
        while (i < n) {
            controllers!![i]!!.start()
            i++
        }
    }

    fun end() {
        var i = 0
        val n = controllers!!.size
        while (i < n) {
            controllers!![i]!!.end()
            i++
        }
    }

    fun reset() {
        var i = 0
        val n = controllers!!.size
        while (i < n) {
            controllers!![i]!!.reset()
            i++
        }
    }

    fun update() {
        var i = 0
        val n = controllers!!.size
        while (i < n) {
            controllers!![i]!!.update()
            i++
        }
    }

    fun update(deltaTime: Float) {
        var i = 0
        val n = controllers!!.size
        while (i < n) {
            controllers!![i]!!.update(deltaTime)
            i++
        }
    }

    fun draw() {
        var i = 0
        val n = controllers!!.size
        while (i < n) {
            controllers!![i]!!.draw()
            i++
        }
    }

    val isComplete: Boolean
        get() {
            var i = 0
            val n = controllers!!.size
            while (i < n) {
                if (!controllers!![i]!!.isComplete()) {
                    return false
                }
                i++
            }
            return true
        }

    /**
     * Sets the given transform matrix on each controller.
     */
    fun setTransform(transform: Matrix4?) {
        var i = 0
        val n = controllers!!.size
        while (i < n) {
            controllers!![i]!!.setTransform(transform)
            i++
        }
    }

    /**
     * Applies the rotation to the current transformation matrix of each controller.
     */
    fun rotate(rotation: Quaternion?) {
        var i = 0
        val n = controllers!!.size
        while (i < n) {
            controllers!![i]!!.rotate(rotation)
            i++
        }
    }

    /**
     * Applies the rotation by the given angle around the given axis to the current transformation matrix of each controller.
     *
     * @param axis  the rotation axis
     * @param angle the rotation angle in degrees
     */
    fun rotate(axis: Vector3?, angle: Float) {
        var i = 0
        val n = controllers!!.size
        while (i < n) {
            controllers!![i]!!.rotate(axis, angle)
            i++
        }
    }

    /**
     * Applies the translation to the current transformation matrix of each controller.
     */
    fun translate(translation: Vector3?) {
        var i = 0
        val n = controllers!!.size
        while (i < n) {
            controllers!![i]!!.translate(translation)
            i++
        }
    }

    /**
     * Applies the scale to the current transformation matrix of each controller.
     */
    fun scale(scaleX: Float, scaleY: Float, scaleZ: Float) {
        var i = 0
        val n = controllers!!.size
        while (i < n) {
            controllers!![i]!!.scale(scaleX, scaleY, scaleZ)
            i++
        }
    }

    /**
     * Applies the scale to the current transformation matrix of each controller.
     */
    fun scale(scale: Vector3?) {
        var i = 0
        val n = controllers!!.size
        while (i < n) {
            controllers!![i]!!.scale(scale.x, scale.y, scale.z)
            i++
        }
    }

    /**
     * @return all particle controllers.
     */
    fun getControllers(): Array<ParticleController?>? {
        return controllers
    }

    /**
     * Returns the controller with the specified name, or null.
     */
    fun findController(name: String?): ParticleController? {
        var i = 0
        val n = controllers!!.size
        while (i < n) {
            val emitter: ParticleController? = controllers!![i]
            if (emitter!!.name.equals(name)) return emitter
            i++
        }
        return null
    }

    fun dispose() {
        var i = 0
        val n = controllers!!.size
        while (i < n) {
            controllers!![i]!!.dispose()
            i++
        }
    }

    /**
     * @return the merged bounding box of all controllers.
     */
    val boundingBox: BoundingBox?
        get() {
            if (bounds == null) bounds = BoundingBox()
            val bounds: BoundingBox? = bounds
            bounds.inf()
            for (emitter in controllers!!) bounds.ext(emitter!!.getBoundingBox())
            return bounds
        }

    /**
     * Assign one batch, among those passed in, to each controller. The batch must be compatible with the controller to be
     * assigned.
     */
    fun setBatch(batches: Array<ParticleBatch<*>?>?) {
        for (controller in controllers!!) {
            for (batch in batches!!) if (controller!!.renderer.setBatch(batch)) break
        }
    }

    /**
     * @return a copy of this effect, should be used after the particle effect has been loaded.
     */
    fun copy(): ParticleEffect? {
        return ParticleEffect(this)
    }

    /**
     * Saves all the assets required by all the controllers inside this effect.
     */
    fun save(assetManager: AssetManager?, data: ResourceData<*>?) {
        for (controller in controllers!!) {
            controller!!.save(assetManager, data)
        }
    }

    /**
     * Loads all the assets required by all the controllers inside this effect.
     */
    fun load(assetManager: AssetManager?, data: ResourceData<*>?) {
        val i = 0
        for (controller in controllers!!) {
            controller!!.load(assetManager, data)
        }
    }
}
