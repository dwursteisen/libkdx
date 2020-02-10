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
package com.badlogic.gdx.maps.tiled.tiles

import Texture.TextureFilter
import com.badlogic.gdx.maps.ImageResolver
import com.badlogic.gdx.maps.MapLayer
import com.badlogic.gdx.maps.MapProperties
import com.badlogic.gdx.maps.tiled.AtlasTmxMapLoader.AtlasResolver
import com.badlogic.gdx.maps.tiled.AtlasTmxMapLoader.AtlasResolver.AssetManagerAtlasResolver
import com.badlogic.gdx.maps.tiled.AtlasTmxMapLoader.AtlasResolver.DirectAtlasResolver
import com.badlogic.gdx.maps.tiled.AtlasTmxMapLoader.AtlasTiledMapLoaderParameters
import com.badlogic.gdx.maps.tiled.BaseTmxMapLoader
import com.badlogic.gdx.maps.tiled.TideMapLoader
import com.badlogic.gdx.maps.tiled.renderers.BatchTiledMapRenderer
import com.badlogic.gdx.maps.tiled.renderers.OrthoCachedTiledMapRenderer
import com.badlogic.gdx.maps.tiled.tiles.StaticTiledMapTile
import kotlin.jvm.Throws

/**
 * @brief Represents a changing [TiledMapTile].
 */
class AnimatedTiledMapTile : TiledMapTile {

    var id = 0
    var blendMode: BlendMode? = BlendMode.ALPHA
    var properties: MapProperties? = null
        get() {
            if (field == null) {
                field = MapProperties()
            }
            return field
        }
        private set
    var objects: MapObjects? = null
        get() {
            if (field == null) {
                field = MapObjects()
            }
            return field
        }
        private set
    private var frameTiles: Array<StaticTiledMapTile?>?
    private var animationIntervals: IntArray?
    private var frameCount = 0
    private var loopDuration: Int

    val currentFrameIndex: Int
        get() {
            var currentTime = (lastTiledMapRenderTime % loopDuration).toInt()
            for (i in animationIntervals!!.indices) {
                val animationInterval = animationIntervals!![i]
                if (currentTime <= animationInterval) return i
                currentTime -= animationInterval
            }
            throw GdxRuntimeException(
                "Could not determine current animation frame in AnimatedTiledMapTile.  This should never happen.")
        }

    val currentFrame: TiledMapTile?
        get() = frameTiles!![currentFrameIndex]

    var textureRegion: TextureRegion?
        get() = currentFrame.getTextureRegion()
        set(textureRegion) {
            throw GdxRuntimeException("Cannot set the texture region of AnimatedTiledMapTile.")
        }

    var offsetX: Float
        get() = currentFrame.getOffsetX()
        set(offsetX) {
            throw GdxRuntimeException("Cannot set offset of AnimatedTiledMapTile.")
        }

    var offsetY: Float
        get() = currentFrame.getOffsetY()
        set(offsetY) {
            throw GdxRuntimeException("Cannot set offset of AnimatedTiledMapTile.")
        }

    fun getAnimationIntervals(): IntArray? {
        return animationIntervals
    }

    fun setAnimationIntervals(intervals: IntArray?) {
        if (intervals!!.size == animationIntervals!!.size) {
            animationIntervals = intervals
            loopDuration = 0
            for (i in intervals.indices) {
                loopDuration += intervals[i]
            }
        } else {
            throw GdxRuntimeException("Cannot set " + intervals.size
                + " frame intervals. The given int[] must have a size of " + animationIntervals!!.size + ".")
        }
    }

    /**
     * Creates an animated tile with the given animation interval and frame tiles.
     *
     * @param interval   The interval between each individual frame tile.
     * @param frameTiles An array of [StaticTiledMapTile]s that make up the animation.
     */
    constructor(interval: Float, frameTiles: Array<StaticTiledMapTile?>?) {
        this.frameTiles = arrayOfNulls<StaticTiledMapTile?>(frameTiles!!.size)
        frameCount = frameTiles!!.size
        loopDuration = frameTiles.size * (interval * 1000f).toInt()
        animationIntervals = IntArray(frameTiles.size)
        for (i in 0 until frameTiles.size) {
            this.frameTiles!![i] = frameTiles[i]
            animationIntervals!![i] = (interval * 1000f).toInt()
        }
    }

    /**
     * Creates an animated tile with the given animation intervals and frame tiles.
     *
     * @param intervals  The intervals between each individual frame tile in milliseconds.
     * @param frameTiles An array of [StaticTiledMapTile]s that make up the animation.
     */
    constructor(intervals: IntArray?, frameTiles: Array<StaticTiledMapTile?>?) {
        this.frameTiles = arrayOfNulls<StaticTiledMapTile?>(frameTiles!!.size)
        frameCount = frameTiles!!.size
        animationIntervals = intervals.toArray()
        loopDuration = 0
        for (i in 0 until intervals!!.size) {
            this.frameTiles!![i] = frameTiles[i]
            loopDuration += intervals[i]
        }
    }

    fun getFrameTiles(): Array<StaticTiledMapTile?>? {
        return frameTiles
    }

    companion object {
        private var lastTiledMapRenderTime: Long = 0
        private val initialTimeOffset: Long = TimeUtils.millis()

        /**
         * Function is called by BatchTiledMapRenderer render(), lastTiledMapRenderTime is used to keep all of the tiles in lock-step
         * animation and avoids having to call TimeUtils.millis() in getTextureRegion()
         */
        fun updateAnimationBaseTime() {
            lastTiledMapRenderTime = TimeUtils.millis() - initialTimeOffset
        }
    }
}
