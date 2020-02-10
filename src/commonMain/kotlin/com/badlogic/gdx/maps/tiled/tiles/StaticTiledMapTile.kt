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
import com.badlogic.gdx.maps.tiled.tiles.AnimatedTiledMapTile
import kotlin.jvm.Throws

/**
 * @brief Represents a non changing [TiledMapTile] (can be cached)
 */
class StaticTiledMapTile : TiledMapTile {

    var id = 0
    var blendMode: BlendMode? = BlendMode.ALPHA
    private var properties: MapProperties? = null
    private var objects: MapObjects? = null
    private var textureRegion: TextureRegion?
    var offsetX = 0f
    var offsetY = 0f

    fun getProperties(): MapProperties? {
        if (properties == null) {
            properties = MapProperties()
        }
        return properties
    }

    fun getObjects(): MapObjects? {
        if (objects == null) {
            objects = MapObjects()
        }
        return objects
    }

    fun getTextureRegion(): TextureRegion? {
        return textureRegion
    }

    fun setTextureRegion(textureRegion: TextureRegion?) {
        this.textureRegion = textureRegion
    }

    /**
     * Creates a static tile with the given region
     *
     * @param textureRegion the [TextureRegion] to use.
     */
    constructor(textureRegion: TextureRegion?) {
        this.textureRegion = textureRegion
    }

    /**
     * Copy constructor
     *
     * @param copy the StaticTiledMapTile to copy.
     */
    constructor(copy: StaticTiledMapTile?) {
        if (copy!!.properties != null) {
            getProperties()!!.putAll(copy.properties)
        }
        objects = copy.objects
        textureRegion = copy.textureRegion
        id = copy.id
    }
}
