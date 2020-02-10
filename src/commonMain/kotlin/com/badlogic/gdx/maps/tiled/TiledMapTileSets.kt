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
 * @brief Collection of [TiledMapTileSet]
 */
class TiledMapTileSets : Iterable<TiledMapTileSet?> {

    private val tilesets: Array<TiledMapTileSet?>?

    /**
     * @param index index to get the desired [TiledMapTileSet] at.
     * @return tileset at index
     */
    fun getTileSet(index: Int): TiledMapTileSet? {
        return tilesets!![index]
    }

    /**
     * @param name Name of the [TiledMapTileSet] to retrieve.
     * @return tileset with matching name, null if it doesn't exist
     */
    fun getTileSet(name: String?): TiledMapTileSet? {
        for (tileset in tilesets!!) {
            if (name == tileset.getName()) {
                return tileset
            }
        }
        return null
    }

    /**
     * @param tileset set to be added to the collection
     */
    fun addTileSet(tileset: TiledMapTileSet?) {
        tilesets.add(tileset)
    }

    /**
     * Removes tileset at index
     *
     * @param index index at which to remove a tileset.
     */
    fun removeTileSet(index: Int) {
        tilesets.removeIndex(index)
    }

    /**
     * @param tileset set to be removed
     */
    fun removeTileSet(tileset: TiledMapTileSet?) {
        tilesets.removeValue(tileset, true)
    }

    /**
     * @param id id of the [TiledMapTile] to get.
     * @return tile with matching id, null if it doesn't exist
     */
    fun getTile(id: Int): TiledMapTile? {
        // The purpose of backward iteration here is to maintain backwards compatibility
        // with maps created with earlier versions of a shared tileset.  The assumption
        // is that the tilesets are in order of ascending firstgid, and by backward
        // iterating precedence for conflicts is given to later tilesets in the list,
        // which are likely to be the earlier version of any given gid.
        // See TiledMapModifiedExternalTilesetTest for example of this issue.
        for (i in tilesets!!.size - 1 downTo 0) {
            val tileset: TiledMapTileSet? = tilesets[i]
            val tile: TiledMapTile = tileset!!.getTile(id)
            if (tile != null) {
                return tile
            }
        }
        return null
    }

    /**
     * @return iterator to tilesets
     */
    override fun iterator(): Iterator<Any?> {
        return tilesets!!.iterator()
    }

    /**
     * Creates an empty collection of tilesets.
     */
    init {
        tilesets = Array<TiledMapTileSet?>()
    }
}
