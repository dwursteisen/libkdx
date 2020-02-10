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
 * @brief Represents a rectangle shaped map object
 */
class RectangleMapObject @JvmOverloads constructor(x: Float = 0.0f, y: Float = 0.0f, width: Float = 1.0f, height: Float = 1.0f) : MapObject() {

    private val rectangle: Rectangle?

    /**
     * @return rectangle shape
     */
    fun getRectangle(): Rectangle? {
        return rectangle
    }
    /**
     * Creates a [Rectangle] object with the given X and Y coordinates along with a given width and height.
     *
     * @param x      X coordinate
     * @param y      Y coordinate
     * @param width  Width of the [Rectangle] to be created.
     * @param height Height of the [Rectangle] to be created.
     */
    /**
     * Creates a rectangle object which lower left corner is at (0, 0) with width=1 and height=1
     */
    init {
        rectangle = Rectangle(x, y, width, height)
    }
}
