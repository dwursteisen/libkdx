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
package com.badlogic.gdx.maps

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
 * A generic level map implementation.
 *
 *
 * A map has [MapProperties] which describe general attributes. Availability of properties depends on the type of map, e.g.
 * what format is was loaded from etc.
 *
 *
 * A map has [MapLayers]. Map layers are ordered and indexed. A [MapLayer] contains [MapObjects] which represent
 * things within the layer. Different types of [MapObject] are available, e.g. [CircleMapObject],
 * [TextureMapObject], and so on.
 *
 *
 * A map can be rendered by a [MapRenderer]. A MapRenderer implementation may chose to only render specific MapObject or
 * MapLayer types.
 *
 *
 * There are more specialized implementations of Map for specific use cases. e.g. the [TiledMap] class and its associated
 * classes add functionality specifically for tile maps on top of the basic map functionality.
 *
 *
 * Maps must be disposed through a call to [.dispose] when no longer used.
 */
class Map
/**
 * Creates empty map
 */
    : Disposable {

    /**
     * @return the map's layers
     */
    val layers: MapLayers? = MapLayers()

    /**
     * @return the map's properties
     */
    val properties: MapProperties? = MapProperties()

    /**
     * Disposes all resources like [Texture] instances that the map may own.
     */
    fun dispose() {}
}
