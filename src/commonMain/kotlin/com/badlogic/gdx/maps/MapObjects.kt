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
 * @brief Collection of MapObject instances
 */
class MapObjects : Iterable<MapObject?> {

    private val objects: Array<MapObject?>?

    /**
     * @param index
     * @return the MapObject at the specified index
     */
    operator fun get(index: Int): MapObject? {
        return objects!![index]
    }

    /**
     * @param name
     * @return the first object having the specified name, if one exists, otherwise null
     */
    operator fun get(name: String?): MapObject? {
        var i = 0
        val n = objects!!.size
        while (i < n) {
            val `object`: MapObject? = objects[i]
            if (name == `object`.getName()) {
                return `object`
            }
            i++
        }
        return null
    }

    /**
     * Get the index of the object having the specified name, or -1 if no such object exists.
     */
    fun getIndex(name: String?): Int {
        return getIndex(get(name))
    }

    /**
     * Get the index of the object in the collection, or -1 if no such object exists.
     */
    fun getIndex(`object`: MapObject?): Int {
        return objects!!.indexOf(`object`, true)
    }

    /**
     * @return number of objects in the collection
     */
    val count: Int
        get() = objects!!.size

    /**
     * @param object instance to be added to the collection
     */
    fun add(`object`: MapObject?) {
        objects.add(`object`)
    }

    /**
     * @param index removes MapObject instance at index
     */
    fun remove(index: Int) {
        objects.removeIndex(index)
    }

    /**
     * @param object instance to be removed
     */
    fun remove(`object`: MapObject?) {
        objects.removeValue(`object`, true)
    }

    /**
     * @param type class of the objects we want to retrieve
     * @return array filled with all the objects in the collection matching type
     */
    fun <T : MapObject?> getByType(type: java.lang.Class<T?>?): Array<T?>? {
        return getByType<T?>(type, Array())
    }

    /**
     * @param type class of the objects we want to retrieve
     * @param fill collection to put the returned objects in
     * @return array filled with all the objects in the collection matching type
     */
    fun <T : MapObject?> getByType(type: java.lang.Class<T?>?, fill: Array<T?>?): Array<T?>? {
        fill.clear()
        var i = 0
        val n = objects!!.size
        while (i < n) {
            val `object`: MapObject? = objects[i]
            if (ClassReflection.isInstance(type, `object`)) {
                fill.add(`object` as T?)
            }
            i++
        }
        return fill
    }

    /**
     * @return iterator for the objects within the collection
     */
    override fun iterator(): Iterator<Any?> {
        return objects!!.iterator()
    }

    /**
     * Creates an empty set of MapObject instances
     */
    init {
        objects = Array<MapObject?>()
    }
}
