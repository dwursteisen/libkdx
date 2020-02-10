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
 * Ordered list of [MapLayer] instances owned by a [Map]
 */
class MapLayers : Iterable<MapLayer?> {

    private val layers: Array<MapLayer?>? = Array<MapLayer?>()

    /**
     * @param index
     * @return the MapLayer at the specified index
     */
    operator fun get(index: Int): MapLayer? {
        return layers!![index]
    }

    /**
     * @param name
     * @return the first layer having the specified name, if one exists, otherwise null
     */
    operator fun get(name: String?): MapLayer? {
        var i = 0
        val n = layers!!.size
        while (i < n) {
            val layer: MapLayer? = layers[i]
            if (name == layer.getName()) {
                return layer
            }
            i++
        }
        return null
    }

    /**
     * Get the index of the layer having the specified name, or -1 if no such layer exists.
     */
    fun getIndex(name: String?): Int {
        return getIndex(get(name))
    }

    /**
     * Get the index of the layer in the collection, or -1 if no such layer exists.
     */
    fun getIndex(layer: MapLayer?): Int {
        return layers!!.indexOf(layer, true)
    }

    /**
     * @return number of layers in the collection
     */
    val count: Int
        get() = layers!!.size

    /**
     * @param layer layer to be added to the set
     */
    fun add(layer: MapLayer?) {
        layers.add(layer)
    }

    /**
     * @param index removes layer at index
     */
    fun remove(index: Int) {
        layers.removeIndex(index)
    }

    /**
     * @param layer layer to be removed
     */
    fun remove(layer: MapLayer?) {
        layers.removeValue(layer, true)
    }

    /**
     * @return the number of map layers
     */
    fun size(): Int {
        return layers!!.size
    }

    /**
     * @param type
     * @return array with all the layers matching type
     */
    fun <T : MapLayer?> getByType(type: java.lang.Class<T?>?): Array<T?>? {
        return getByType<T?>(type, Array())
    }

    /**
     * @param type
     * @param fill array to be filled with the matching layers
     * @return array with all the layers matching type
     */
    fun <T : MapLayer?> getByType(type: java.lang.Class<T?>?, fill: Array<T?>?): Array<T?>? {
        fill.clear()
        var i = 0
        val n = layers!!.size
        while (i < n) {
            val layer: MapLayer? = layers[i]
            if (ClassReflection.isInstance(type, layer)) {
                fill.add(layer as T?)
            }
            i++
        }
        return fill
    }

    /**
     * @return iterator to set of layers
     */
    override fun iterator(): Iterator<Any?> {
        return layers!!.iterator()
    }
}
