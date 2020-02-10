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
package com.badlogic.gdx.maps.objects

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
 * @brief Represents a map object containing a texture (region)
 */
class TextureMapObject @JvmOverloads constructor(textureRegion: TextureRegion? = null) : MapObject() {

    /**
     * @return x axis coordinate
     */
    /**
     * @param x new x axis coordinate
     */
    var x = 0.0f
    /**
     * @return y axis coordinate
     */
    /**
     * @param y new y axis coordinate
     */
    var y = 0.0f
    /**
     * @return x axis origin
     */
    /**
     * @param x new x axis origin
     */
    var originX = 0.0f
    /**
     * @return y axis origin
     */
    /**
     * @param y new axis origin
     */
    var originY = 0.0f
    /**
     * @return x axis scale
     */
    /**
     * @param x new x axis scale
     */
    var scaleX = 1.0f
    /**
     * @return y axis scale
     */
    /**
     * @param y new y axis scale
     */
    var scaleY = 1.0f
    /**
     * @return texture's rotation in radians
     */
    /**
     * @param rotation new texture's rotation in radians
     */
    var rotation = 0.0f
    private var textureRegion: TextureRegion? = null

    /**
     * @return region
     */
    fun getTextureRegion(): TextureRegion? {
        return textureRegion
    }

    /**
     * @param region new texture region
     */
    fun setTextureRegion(region: TextureRegion?) {
        textureRegion = region
    }
    /**
     * Creates texture map object with the given region
     *
     * @param textureRegion the [TextureRegion] to use.
     */
    /**
     * Creates an empty texture map object
     */
    init {
        this.textureRegion = textureRegion
    }
}
