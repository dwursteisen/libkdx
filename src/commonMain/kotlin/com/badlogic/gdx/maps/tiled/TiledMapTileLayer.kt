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
 * @brief Layer for a TiledMap
 */
class TiledMapTileLayer(
    /**
     * @return layer's width in tiles
     */
    val width: Int,
    /**
     * @return layer's height in tiles
     */
    val height: Int, tileWidth: Int, tileHeight: Int) : MapLayer() {

    /**
     * @return tiles' width in pixels
     */
    val tileWidth: Float

    /**
     * @return tiles' height in pixels
     */
    val tileHeight: Float
    private val cells: Array<Array<Cell?>?>?

    /**
     * @param x X coordinate
     * @param y Y coordinate
     * @return [Cell] at (x, y)
     */
    fun getCell(x: Int, y: Int): Cell? {
        if (x < 0 || x >= width) return null
        return if (y < 0 || y >= height) null else cells!![x]!![y]
    }

    /**
     * Sets the [Cell] at the given coordinates.
     *
     * @param x    X coordinate
     * @param y    Y coordinate
     * @param cell the [Cell] to set at the given coordinates.
     */
    fun setCell(x: Int, y: Int, cell: Cell?) {
        if (x < 0 || x >= width) return
        if (y < 0 || y >= height) return
        cells!![x]!![y] = cell
    }

    /**
     * @brief represents a cell in a TiledLayer: TiledMapTile, flip and rotation properties.
     */
    class Cell {

        private var tile: TiledMapTile? = null

        /**
         * @return Whether the tile should be flipped horizontally.
         */
        var flipHorizontally = false
            private set

        /**
         * @return Whether the tile should be flipped vertically.
         */
        var flipVertically = false
            private set

        /**
         * @return The rotation of this cell, in 90 degree increments.
         */
        var rotation = 0
            private set

        /**
         * @return The tile currently assigned to this cell.
         */
        fun getTile(): TiledMapTile? {
            return tile
        }

        /**
         * Sets the tile to be used for this cell.
         *
         * @param tile the [TiledMapTile] to use for this cell.
         * @return this, for method chaining
         */
        fun setTile(tile: TiledMapTile?): Cell? {
            this.tile = tile
            return this
        }

        /**
         * Sets whether to flip the tile horizontally.
         *
         * @param flipHorizontally whether or not to flip the tile horizontally.
         * @return this, for method chaining
         */
        fun setFlipHorizontally(flipHorizontally: Boolean): Cell? {
            this.flipHorizontally = flipHorizontally
            return this
        }

        /**
         * Sets whether to flip the tile vertically.
         *
         * @param flipVertically whether or not this tile should be flipped vertically.
         * @return this, for method chaining
         */
        fun setFlipVertically(flipVertically: Boolean): Cell? {
            this.flipVertically = flipVertically
            return this
        }

        /**
         * Sets the rotation of this cell, in 90 degree increments.
         *
         * @param rotation the rotation in 90 degree increments (see ints below).
         * @return this, for method chaining
         */
        fun setRotation(rotation: Int): Cell? {
            this.rotation = rotation
            return this
        }

        companion object {
            const val ROTATE_0 = 0
            const val ROTATE_90 = 1
            const val ROTATE_180 = 2
            const val ROTATE_270 = 3
        }
    }

    /**
     * Creates TiledMap layer
     *
     * @param width      layer width in tiles
     * @param height     layer height in tiles
     * @param tileWidth  tile width in pixels
     * @param tileHeight tile height in pixels
     */
    init {
        this.tileWidth = tileWidth.toFloat()
        this.tileHeight = tileHeight.toFloat()
        cells = Array(width) { arrayOfNulls<Cell?>(height) }
    }
}
