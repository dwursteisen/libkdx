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
 * @brief Set of [TiledMapTile] instances used to compose a TiledMapLayer
 */
class TiledMapTileSet : Iterable<TiledMapTile?> {

    /**
     * @return tileset's name
     */
    /**
     * @param name new name for the tileset
     */
    var name: String? = null
    private val tiles: IntMap<TiledMapTile?>?
    private val properties: MapProperties?

    /**
     * @return tileset's properties set
     */
    fun getProperties(): MapProperties? {
        return properties
    }

    /**
     * Gets the [TiledMapTile] that has the given id.
     *
     * @param id the id of the [TiledMapTile] to retrieve.
     * @return tile matching id, null if it doesn't exist
     */
    fun getTile(id: Int): TiledMapTile? {
        return tiles.get(id)
    }

    /**
     * @return iterator to tiles in this tileset
     */
    override fun iterator(): Iterator<Any?> {
        return tiles.values().iterator()
    }

    /**
     * Adds or replaces tile with that id
     *
     * @param id   the id of the [TiledMapTile] to add or replace.
     * @param tile the [TiledMapTile] to add or replace.
     */
    fun putTile(id: Int, tile: TiledMapTile?) {
        tiles.put(id, tile)
    }

    /**
     * @param id tile's id to be removed
     */
    fun removeTile(id: Int) {
        tiles.remove(id)
    }

    /**
     * @return the size of this TiledMapTileSet.
     */
    fun size(): Int {
        return tiles.size
    }

    /**
     * Creates empty tileset
     */
    init {
        tiles = IntMap<TiledMapTile?>()
        properties = MapProperties()
    }
}
