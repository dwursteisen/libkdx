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
import com.badlogic.gdx.graphics.g3d.particles.ParticleEffectLoader.ParticleEffectLoadParameter
import com.badlogic.gdx.graphics.g3d.particles.ParticleEffectLoader.ParticleEffectSaveParameter
import kotlin.jvm.Throws

/**
 * It's the base class of every [ParticleController] component. A component duty is to participate in one or some events
 * during the simulation. (i.e it can handle the particles emission or modify particle properties, etc.).
 *
 * @author inferno
 */
abstract class ParticleControllerComponent : Disposable, Json.Serializable, ResourceData.Configurable<Any?> {

    protected var controller: ParticleController? = null

    /**
     * Called to initialize new emitted particles.
     */
    fun activateParticles(startIndex: Int, count: Int) {}

    /**
     * Called to notify which particles have been killed.
     */
    fun killParticles(startIndex: Int, count: Int) {}

    /**
     * Called to execute the component behavior.
     */
    fun update() {}

    /**
     * Called once during intialization
     */
    fun init() {}

    /**
     * Called at the start of the simulation.
     */
    fun start() {}

    /**
     * Called at the end of the simulation.
     */
    fun end() {}
    fun dispose() {}
    abstract fun copy(): ParticleControllerComponent?

    /**
     * Called during initialization to allocate additional particles channels
     */
    fun allocateChannels() {}
    fun set(particleController: ParticleController?) {
        controller = particleController
    }

    fun save(manager: AssetManager?, data: ResourceData<*>?) {}
    fun load(manager: AssetManager?, data: ResourceData<*>?) {}
    fun write(json: Json?) {}
    fun read(json: Json?, jsonData: JsonValue?) {}

    companion object {
        protected val TMP_V1: Vector3? = Vector3()
        protected val TMP_V2: Vector3? = Vector3()
        protected val TMP_V3: Vector3? = Vector3()
        protected val TMP_V4: Vector3? = Vector3()
        protected val TMP_V5: Vector3? = Vector3()
        protected val TMP_V6: Vector3? = Vector3()
        protected val TMP_Q: Quaternion? = Quaternion()
        protected val TMP_Q2: Quaternion? = Quaternion()
        protected val TMP_M3: Matrix3? = Matrix3()
        protected val TMP_M4: Matrix4? = Matrix4()
    }
}
