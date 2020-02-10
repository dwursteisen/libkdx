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
import com.badlogic.gdx.graphics.g3d.particles.influencers.ParticleControllerInfluencer
import com.badlogic.gdx.graphics.g3d.particles.influencers.ParticleControllerInfluencer.Random.ParticleControllerPool
import com.badlogic.gdx.graphics.g3d.particles.influencers.RegionInfluencer
import com.badlogic.gdx.graphics.g3d.particles.influencers.RegionInfluencer.Animated
import com.badlogic.gdx.graphics.g3d.particles.influencers.RegionInfluencer.AspectTextureRegion
import com.badlogic.gdx.graphics.g3d.particles.influencers.ScaleInfluencer
import com.badlogic.gdx.graphics.g3d.particles.influencers.SimpleInfluencer
import java.lang.RuntimeException

/**
 * It's an [Influencer] which controls where the particles will be spawned.
 *
 * @author Inferno
 */
class SpawnInfluencer : Influencer {

    var spawnShapeValue: SpawnShapeValue
    var positionChannel: FloatChannel? = null

    constructor() {
        spawnShapeValue = PointSpawnShapeValue()
    }

    constructor(spawnShapeValue: SpawnShapeValue) {
        this.spawnShapeValue = spawnShapeValue
    }

    constructor(source: SpawnInfluencer) {
        spawnShapeValue = source.spawnShapeValue.copy()
    }

    fun init() {
        spawnShapeValue.init()
    }

    fun allocateChannels() {
        positionChannel = controller.particles.addChannel(ParticleChannels.Position)
    }

    fun start() {
        spawnShapeValue.start()
    }

    fun activateParticles(startIndex: Int, count: Int) {
        var i: Int = startIndex * positionChannel.strideSize
        val c: Int = i + count * positionChannel.strideSize
        while (i < c) {
            spawnShapeValue.spawn(TMP_V1, controller.emitter.percent)
            TMP_V1.mul(controller.transform)
            positionChannel.data.get(i + ParticleChannels.XOffset) = TMP_V1.x
            positionChannel.data.get(i + ParticleChannels.YOffset) = TMP_V1.y
            positionChannel.data.get(i + ParticleChannels.ZOffset) = TMP_V1.z
            i += positionChannel.strideSize
        }
    }

    fun copy(): SpawnInfluencer {
        return SpawnInfluencer(this)
    }

    fun write(json: Json) {
        json.writeValue("spawnShape", spawnShapeValue, SpawnShapeValue::class.java)
    }

    fun read(json: Json, jsonData: JsonValue?) {
        spawnShapeValue = json.readValue("spawnShape", SpawnShapeValue::class.java, jsonData)
    }

    fun save(manager: AssetManager?, data: ResourceData?) {
        spawnShapeValue.save(manager, data)
    }

    fun load(manager: AssetManager?, data: ResourceData?) {
        spawnShapeValue.load(manager, data)
    }
}
