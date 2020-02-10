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
 * @brief Represents a tiled map, adds the concept of tiles and tilesets.
 * @see Map
 */
class TiledMap : Map() {

    private val tilesets: TiledMapTileSets?
    private var ownedResources: Array<out Disposable?>? = null

    /**
     * @return collection of tilesets for this map.
     */
    val tileSets: com.badlogic.gdx.maps.tiled.TiledMapTileSets?
        get() = tilesets

    /**
     * Used by loaders to set resources when loading the map directly, without [AssetManager]. To be disposed in
     * [.dispose].
     *
     * @param resources
     */
    fun setOwnedResources(resources: Array<out Disposable?>?) {
        ownedResources = resources
    }

    fun dispose() {
        if (ownedResources != null) {
            for (resource in ownedResources) {
                resource.dispose()
            }
        }
    }

    /**
     * Creates an empty TiledMap.
     */
    init {
        tilesets = TiledMapTileSets()
    }
}
