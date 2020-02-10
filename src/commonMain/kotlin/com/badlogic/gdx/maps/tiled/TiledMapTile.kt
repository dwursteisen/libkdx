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
package com.badlogic.gdx.maps.tiled

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
import com.badlogic.gdx.maps.tiled.tiles.AnimatedTiledMapTile
import com.badlogic.gdx.maps.tiled.tiles.StaticTiledMapTile
import kotlin.jvm.Throws

/**
 * @brief Generalises the concept of tile in a TiledMap
 */
interface TiledMapTile {

    enum class BlendMode {
        NONE, ALPHA
    }

    var id: Int

    /**
     * @return the [BlendMode] to use for rendering the tile
     */
    fun getBlendMode(): BlendMode?

    /**
     * Sets the [BlendMode] to use for rendering the tile
     *
     * @param blendMode the blend mode to use for rendering the tile
     */
    fun setBlendMode(blendMode: BlendMode?)

    /**
     * @return texture region used to render the tile
     */
    fun getTextureRegion(): TextureRegion?

    /**
     * Sets the texture region used to render the tile
     */
    fun setTextureRegion(textureRegion: TextureRegion?)

    /**
     * @return the amount to offset the x position when rendering the tile
     */
    /**
     * Set the amount to offset the x position when rendering the tile
     */
    var offsetX: Float

    /**
     * @return the amount to offset the y position when rendering the tile
     */
    /**
     * Set the amount to offset the y position when rendering the tile
     */
    var offsetY: Float

    /**
     * @return tile's properties set
     */
    val properties: com.badlogic.gdx.maps.MapProperties?

    /**
     * @return collection of objects contained in the tile
     */
    val objects: MapObjects?
}
