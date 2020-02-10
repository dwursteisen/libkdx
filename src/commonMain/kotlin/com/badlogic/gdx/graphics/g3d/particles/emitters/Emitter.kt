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
package com.badlogic.gdx.graphics.g3d.particles.emitters

import kotlin.jvm.JvmField

/** An [Emitter] is a [ParticleControllerComponent] which will handle the particles emission. It must update the
 * [Emitter.percent] to reflect the current percentage of the current emission cycle. It should consider
 * [Emitter.minParticleCount] and [Emitter.maxParticleCount] to rule particle emission. It should notify the particle
 * controller when particles are activated, killed, or when an emission cycle begins.
 * @author Inferno
 */
abstract class Emitter : com.badlogic.gdx.graphics.g3d.particles.ParticleControllerComponent, com.badlogic.gdx.utils.Json.Serializable {

    /** The min/max quantity of particles  */
    var minParticleCount = 0
    @JvmField
    var maxParticleCount = 4
    /** Current state of the emission, should be currentTime/ duration Must be updated on each update  */
    @JvmField
    var percent = 0f

    constructor(regularEmitter: Emitter) {
        set(regularEmitter)
    }

    constructor() {}

    override fun init() {
        controller.particles.size = 0
    }

    override fun end() {
        controller.particles.size = 0
    }

    open val isComplete: Boolean
        get() = percent >= 1.0f

    fun setParticleCount(aMin: Int, aMax: Int) {
        minParticleCount = aMin
        maxParticleCount = aMax
    }

    fun set(emitter: Emitter) {
        minParticleCount = emitter.minParticleCount
        maxParticleCount = emitter.maxParticleCount
    }

    override fun write(json: com.badlogic.gdx.utils.Json) {
        json.writeValue("minParticleCount", minParticleCount)
        json.writeValue("maxParticleCount", maxParticleCount)
    }

    override fun read(json: com.badlogic.gdx.utils.Json, jsonData: com.badlogic.gdx.utils.JsonValue) {
        minParticleCount = json.readValue("minParticleCount", Int::class.javaPrimitiveType, jsonData)
        maxParticleCount = json.readValue("maxParticleCount", Int::class.javaPrimitiveType, jsonData)
    }
}
