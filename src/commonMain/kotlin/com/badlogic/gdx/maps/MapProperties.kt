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
 * @brief Set of string indexed values representing map elements' properties, allowing to retrieve, modify and add properties to
 * the set.
 */
class MapProperties {

    private val properties: ObjectMap<String?, Any?>?

    /**
     * @param key property name
     * @return true if and only if the property exists
     */
    fun containsKey(key: String?): Boolean {
        return properties.containsKey(key)
    }

    /**
     * @param key property name
     * @return the value for that property if it exists, otherwise, null
     */
    operator fun get(key: String?): Any? {
        return properties.get(key)
    }

    /**
     * Returns the object for the given key, casting it to clazz.
     *
     * @param key   the key of the object
     * @param clazz the class of the object
     * @return the object or null if the object is not in the map
     * @throws ClassCastException if the object with the given key is not of type clazz
     */
    operator fun <T> get(key: String?, clazz: java.lang.Class<T?>?): T? {
        return get(key) as T?
    }

    /**
     * Returns the object for the given key, casting it to clazz.
     *
     * @param key          the key of the object
     * @param defaultValue the default value
     * @param clazz        the class of the object
     * @return the object or the defaultValue if the object is not in the map
     * @throws ClassCastException if the object with the given key is not of type clazz
     */
    operator fun <T> get(key: String?, defaultValue: T?, clazz: java.lang.Class<T?>?): T? {
        val `object` = get(key)
        return if (`object` == null) defaultValue else `object` as T?
    }

    /**
     * @param key   property name
     * @param value value to be inserted or modified (if it already existed)
     */
    fun put(key: String?, value: Any?) {
        properties.put(key, value)
    }

    /**
     * @param properties set of properties to be added
     */
    fun putAll(properties: MapProperties?) {
        this.properties.putAll(properties!!.properties)
    }

    /**
     * @param key property name to be removed
     */
    fun remove(key: String?) {
        properties.remove(key)
    }

    /**
     * Removes all properties
     */
    fun clear() {
        properties.clear()
    }

    /**
     * @return iterator for the property names
     */
    val keys: Iterator<String?>?
        get() = properties.keys()

    /**
     * @return iterator to properties' values
     */
    val values: Iterator<Any?>?
        get() = properties.values()

    /**
     * Creates an empty properties set
     */
    init {
        properties = ObjectMap<String?, Any?>()
    }
}
