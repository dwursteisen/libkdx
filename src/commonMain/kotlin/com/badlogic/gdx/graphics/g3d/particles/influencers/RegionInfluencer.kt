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
import com.badlogic.gdx.graphics.g3d.particles.influencers.ScaleInfluencer
import com.badlogic.gdx.graphics.g3d.particles.influencers.SimpleInfluencer
import com.badlogic.gdx.graphics.g3d.particles.influencers.SpawnInfluencer
import java.lang.RuntimeException

/**
 * It's an [Influencer] which assigns a region of a [Texture] to the particles.
 *
 * @author Inferno
 */
abstract class RegionInfluencer : Influencer {

    /**
     * Assigns the first region of [RegionInfluencer.regions] to the particles.
     */
    class Single : RegionInfluencer {

        constructor() {}
        constructor(regionInfluencer: Single?) : super(regionInfluencer) {}
        constructor(textureRegion: TextureRegion?) : super(textureRegion) {}
        constructor(texture: Texture?) : super(texture) {}

        fun init() {
            val region: AspectTextureRegion = regions.items.get(0)
            var i = 0
            val c: Int = controller.emitter.maxParticleCount * regionChannel.strideSize
            while (i < c) {
                regionChannel.data.get(i + ParticleChannels.UOffset) = region.u
                regionChannel.data.get(i + ParticleChannels.VOffset) = region.v
                regionChannel.data.get(i + ParticleChannels.U2Offset) = region.u2
                regionChannel.data.get(i + ParticleChannels.V2Offset) = region.v2
                regionChannel.data.get(i + ParticleChannels.HalfWidthOffset) = 0.5f
                regionChannel.data.get(i + ParticleChannels.HalfHeightOffset) = region.halfInvAspectRatio
                i += regionChannel.strideSize
            }
        }

        fun copy(): Single {
            return Single(this)
        }
    }

    /**
     * Assigns a random region of [RegionInfluencer.regions] to the particles.
     */
    class Random : RegionInfluencer {

        constructor() {}
        constructor(regionInfluencer: Random?) : super(regionInfluencer) {}
        constructor(textureRegion: TextureRegion?) : super(textureRegion) {}
        constructor(texture: Texture?) : super(texture) {}

        fun activateParticles(startIndex: Int, count: Int) {
            var i: Int = startIndex * regionChannel.strideSize
            val c: Int = i + count * regionChannel.strideSize
            while (i < c) {
                val region = regions!!.random()
                regionChannel.data.get(i + ParticleChannels.UOffset) = region.u
                regionChannel.data.get(i + ParticleChannels.VOffset) = region.v
                regionChannel.data.get(i + ParticleChannels.U2Offset) = region.u2
                regionChannel.data.get(i + ParticleChannels.V2Offset) = region.v2
                regionChannel.data.get(i + ParticleChannels.HalfWidthOffset) = 0.5f
                regionChannel.data.get(i + ParticleChannels.HalfHeightOffset) = region.halfInvAspectRatio
                i += regionChannel.strideSize
            }
        }

        fun copy(): Random {
            return Random(this)
        }
    }

    /**
     * Assigns a region to the particles using the particle life percent to calculate the current index in the
     * [RegionInfluencer.regions] array.
     */
    class Animated : RegionInfluencer {

        var lifeChannel: FloatChannel? = null

        constructor() {}
        constructor(regionInfluencer: Animated?) : super(regionInfluencer) {}
        constructor(textureRegion: TextureRegion?) : super(textureRegion) {}
        constructor(texture: Texture?) : super(texture) {}

        override fun allocateChannels() {
            super.allocateChannels()
            lifeChannel = controller.particles.addChannel(ParticleChannels.Life)
        }

        fun update() {
            var i = 0
            var l: Int = ParticleChannels.LifePercentOffset
            val c: Int = controller.particles.size * regionChannel.strideSize
            while (i < c) {
                val region = regions!![(lifeChannel.data.get(l) * (regions!!.size - 1))]
                regionChannel.data.get(i + ParticleChannels.UOffset) = region.u
                regionChannel.data.get(i + ParticleChannels.VOffset) = region.v
                regionChannel.data.get(i + ParticleChannels.U2Offset) = region.u2
                regionChannel.data.get(i + ParticleChannels.V2Offset) = region.v2
                regionChannel.data.get(i + ParticleChannels.HalfWidthOffset) = 0.5f
                regionChannel.data.get(i + ParticleChannels.HalfHeightOffset) = region.halfInvAspectRatio
                i += regionChannel.strideSize
                l += lifeChannel.strideSize
            }
        }

        fun copy(): Animated {
            return Animated(this)
        }
    }

    /**
     * It's a class used internally by the [RegionInfluencer] to represent a texture region. It contains the uv coordinates
     * of the region and the region inverse aspect ratio.
     */
    class AspectTextureRegion {

        var u = 0f
        var v = 0f
        var u2 = 0f
        var v2 = 0f
        var halfInvAspectRatio = 0f

        constructor() {}
        constructor(aspectTextureRegion: AspectTextureRegion?) {
            set(aspectTextureRegion)
        }

        constructor(region: TextureRegion?) {
            set(region)
        }

        fun set(region: TextureRegion) {
            u = region.getU()
            v = region.getV()
            u2 = region.getU2()
            v2 = region.getV2()
            halfInvAspectRatio = 0.5f * (region.getRegionHeight() as Float / region.getRegionWidth())
        }

        fun set(aspectTextureRegion: AspectTextureRegion) {
            u = aspectTextureRegion.u
            v = aspectTextureRegion.v
            u2 = aspectTextureRegion.u2
            v2 = aspectTextureRegion.v2
            halfInvAspectRatio = aspectTextureRegion.halfInvAspectRatio
        }
    }

    var regions: Array<AspectTextureRegion>? = null
    var regionChannel: FloatChannel? = null

    constructor(regionsCount: Int) {
        regions = Array(false, regionsCount, AspectTextureRegion::class.java)
    }

    constructor() : this(1) {
        val aspectRegion = AspectTextureRegion()
        aspectRegion.v = 0f
        aspectRegion.u = aspectRegion.v
        aspectRegion.v2 = 1f
        aspectRegion.u2 = aspectRegion.v2
        aspectRegion.halfInvAspectRatio = 0.5f
        regions.add(aspectRegion)
    }

    /**
     * All the regions must be defined on the same Texture
     */
    constructor(vararg regions: TextureRegion?) {
        this.regions = Array(false, regions.size, AspectTextureRegion::class.java)
        add(*regions)
    }

    constructor(texture: Texture?) : this(TextureRegion(texture)) {}
    constructor(regionInfluencer: RegionInfluencer) : this(regionInfluencer.regions!!.size) {
        regions.ensureCapacity(regionInfluencer.regions!!.size)
        for (i in 0 until regionInfluencer.regions!!.size) {
            regions.add(AspectTextureRegion(regionInfluencer.regions!![i]))
        }
    }

    fun add(vararg regions: TextureRegion?) {
        this.regions.ensureCapacity(regions.size)
        for (region in regions) {
            this.regions.add(AspectTextureRegion(region))
        }
    }

    fun clear() {
        regions.clear()
    }

    fun allocateChannels() {
        regionChannel = controller.particles.addChannel(ParticleChannels.TextureRegion)
    }

    fun write(json: Json) {
        json.writeValue("regions", regions, Array::class.java, AspectTextureRegion::class.java)
    }

    fun read(json: Json, jsonData: JsonValue?) {
        regions.clear()
        regions.addAll(json.readValue("regions", Array::class.java, AspectTextureRegion::class.java, jsonData))
    }
}
