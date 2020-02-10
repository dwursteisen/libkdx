/*******************************************************************************
 * Copyright 2013 See AUTHORS file.
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
package com.badlogic.gdx.maps.tiled.renderers

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

class HexagonalTiledMapRenderer : BatchTiledMapRenderer {
    /**
     * true for X-Axis, false for Y-Axis
     */
    private var staggerAxisX = true

    /**
     * true for even StaggerIndex, false for odd
     */
    private var staggerIndexEven = false

    /**
     * the parameter defining the shape of the hexagon from tiled. more specifically it represents the length of the sides that
     * are parallel to the stagger axis. e.g. with respect to the stagger axis a value of 0 results in a rhombus shape, while a
     * value equal to the tile length/height represents a square shape and a value of 0.5 represents a regular hexagon if tile
     * length equals tile height
     */
    private var hexSideLength = 0f

    constructor(map: TiledMap?) : super(map) {
        init(map)
    }

    constructor(map: TiledMap?, unitScale: Float) : super(map, unitScale) {
        init(map)
    }

    constructor(map: TiledMap?, batch: Batch?) : super(map, batch) {
        init(map)
    }

    constructor(map: TiledMap?, unitScale: Float, batch: Batch?) : super(map, unitScale, batch) {
        init(map)
    }

    private fun init(map: TiledMap?) {
        val axis: String = map.getProperties().get("staggeraxis", String::class.java)
        if (axis != null) {
            staggerAxisX = if (axis == "x") {
                true
            } else {
                false
            }
        }
        val index: String = map.getProperties().get("staggerindex", String::class.java)
        if (index != null) {
            staggerIndexEven = if (index == "even") {
                true
            } else {
                false
            }
        }
        var length: Int = map.getProperties().get("hexsidelength", Int::class.java)
        if (length != null) {
            hexSideLength = length.toFloat()
        } else {
            if (staggerAxisX) {
                length = map.getProperties().get("tilewidth", Int::class.java)
                if (length != null) {
                    hexSideLength = 0.5f * length
                } else {
                    val tmtl: TiledMapTileLayer = map.getLayers().get(0) as TiledMapTileLayer
                    hexSideLength = 0.5f * tmtl.getTileWidth()
                }
            } else {
                length = map.getProperties().get("tileheight", Int::class.java)
                if (length != null) {
                    hexSideLength = 0.5f * length
                } else {
                    val tmtl: TiledMapTileLayer = map.getLayers().get(0) as TiledMapTileLayer
                    hexSideLength = 0.5f * tmtl.getTileHeight()
                }
            }
        }
    }

    fun renderTileLayer(layer: TiledMapTileLayer?) {
        val batchColor: Color = batch.getColor()
        val color: Float = Color.toFloatBits(batchColor.r, batchColor.g, batchColor.b, batchColor.a * layer.getOpacity())
        val layerWidth: Int = layer.getWidth()
        val layerHeight: Int = layer.getHeight()
        val layerTileWidth: Float = layer.getTileWidth() * unitScale
        val layerTileHeight: Float = layer.getTileHeight() * unitScale
        val layerOffsetX: Float = layer.getRenderOffsetX() * unitScale
        // offset in tiled is y down, so we flip it
        val layerOffsetY: Float = -layer.getRenderOffsetY() * unitScale
        val layerHexLength: Float = hexSideLength * unitScale
        if (staggerAxisX) {
            val tileWidthLowerCorner = (layerTileWidth - layerHexLength) / 2
            val tileWidthUpperCorner = (layerTileWidth + layerHexLength) / 2
            val layerTileHeight50 = layerTileHeight * 0.50f
            val row1: Int = java.lang.Math.max(0, ((viewBounds.y - layerTileHeight50 - layerOffsetX) / layerTileHeight) as Int)
            val row2: Int = java.lang.Math.min(layerHeight,
                ((viewBounds.y + viewBounds.height + layerTileHeight - layerOffsetX) / layerTileHeight) as Int)
            val col1: Int = java.lang.Math.max(0, ((viewBounds.x - tileWidthLowerCorner - layerOffsetY) / tileWidthUpperCorner) as Int)
            val col2: Int = java.lang.Math.min(layerWidth,
                ((viewBounds.x + viewBounds.width + tileWidthUpperCorner - layerOffsetY) / tileWidthUpperCorner) as Int)

            // depending on the stagger index either draw all even before the odd or vice versa
            val colA = if (staggerIndexEven == (col1 % 2 == 0)) col1 + 1 else col1
            val colB = if (staggerIndexEven == (col1 % 2 == 0)) col1 else col1 + 1
            for (row in row2 - 1 downTo row1) {
                run {
                    var col = colA
                    while (col < col2) {
                        renderCell(layer.getCell(col, row), tileWidthUpperCorner * col + layerOffsetX,
                            layerTileHeight50 + layerTileHeight * row + layerOffsetY, color)
                        col += 2
                    }
                }
                var col = colB
                while (col < col2) {
                    renderCell(layer.getCell(col, row), tileWidthUpperCorner * col + layerOffsetX,
                        layerTileHeight * row + layerOffsetY, color)
                    col += 2
                }
            }
        } else {
            val tileHeightLowerCorner = (layerTileHeight - layerHexLength) / 2
            val tileHeightUpperCorner = (layerTileHeight + layerHexLength) / 2
            val layerTileWidth50 = layerTileWidth * 0.50f
            val row1: Int = java.lang.Math.max(0, ((viewBounds.y - tileHeightLowerCorner - layerOffsetX) / tileHeightUpperCorner) as Int)
            val row2: Int = java.lang.Math.min(layerHeight,
                ((viewBounds.y + viewBounds.height + tileHeightUpperCorner - layerOffsetX) / tileHeightUpperCorner) as Int)
            val col1: Int = java.lang.Math.max(0, ((viewBounds.x - layerTileWidth50 - layerOffsetY) / layerTileWidth) as Int)
            val col2: Int = java.lang.Math.min(layerWidth,
                ((viewBounds.x + viewBounds.width + layerTileWidth - layerOffsetY) / layerTileWidth) as Int)
            var shiftX = 0f
            for (row in row2 - 1 downTo row1) {
                // depending on the stagger index either shift for even or uneven indexes
                shiftX = if (row % 2 == 0 == staggerIndexEven) layerTileWidth50 else 0f
                for (col in col1 until col2) {
                    renderCell(layer.getCell(col, row), layerTileWidth * col + shiftX + layerOffsetX,
                        tileHeightUpperCorner * row + layerOffsetY, color)
                }
            }
        }
    }

    /**
     * render a single cell
     */
    private fun renderCell(cell: TiledMapTileLayer.Cell?, x: Float, y: Float, color: Float) {
        if (cell != null) {
            val tile: TiledMapTile = cell.getTile()
            if (tile != null) {
                if (tile is AnimatedTiledMapTile) return
                val flipX: Boolean = cell.getFlipHorizontally()
                val flipY: Boolean = cell.getFlipVertically()
                val rotations: Int = cell.getRotation()
                val region: TextureRegion = tile.getTextureRegion()
                val x1: Float = x + tile.getOffsetX() * unitScale
                val y1: Float = y + tile.getOffsetY() * unitScale
                val x2: Float = x1 + region.getRegionWidth() * unitScale
                val y2: Float = y1 + region.getRegionHeight() * unitScale
                val u1: Float = region.getU()
                val v1: Float = region.getV2()
                val u2: Float = region.getU2()
                val v2: Float = region.getV()
                vertices!!.get(X1) = x1
                vertices!!.get(Y1) = y1
                vertices!!.get(C1) = color
                vertices!!.get(U1) = u1
                vertices!!.get(V1) = v1
                vertices!!.get(X2) = x1
                vertices!!.get(Y2) = y2
                vertices!!.get(C2) = color
                vertices!!.get(U2) = u1
                vertices!!.get(V2) = v2
                vertices!!.get(X3) = x2
                vertices!!.get(Y3) = y2
                vertices!!.get(C3) = color
                vertices!!.get(U3) = u2
                vertices!!.get(V3) = v2
                vertices!!.get(X4) = x2
                vertices!!.get(Y4) = y1
                vertices!!.get(C4) = color
                vertices!!.get(U4) = u2
                vertices!!.get(V4) = v1
                if (flipX) {
                    var temp: Float = vertices!!.get(U1)
                    vertices!!.get(U1) = vertices!!.get(U3)
                    vertices!!.get(U3) = temp
                    temp = vertices!!.get(U2)
                    vertices!!.get(U2) = vertices!!.get(U4)
                    vertices!!.get(U4) = temp
                }
                if (flipY) {
                    var temp: Float = vertices!!.get(V1)
                    vertices!!.get(V1) = vertices!!.get(V3)
                    vertices!!.get(V3) = temp
                    temp = vertices!!.get(V2)
                    vertices!!.get(V2) = vertices!!.get(V4)
                    vertices!!.get(V4) = temp
                }
                if (rotations == 2) {
                    var tempU: Float = vertices!!.get(U1)
                    vertices!!.get(U1) = vertices!!.get(U3)
                    vertices!!.get(U3) = tempU
                    tempU = vertices!!.get(U2)
                    vertices!!.get(U2) = vertices!!.get(U4)
                    vertices!!.get(U4) = tempU
                    var tempV: Float = vertices!!.get(V1)
                    vertices!!.get(V1) = vertices!!.get(V3)
                    vertices!!.get(V3) = tempV
                    tempV = vertices!!.get(V2)
                    vertices!!.get(V2) = vertices!!.get(V4)
                    vertices!!.get(V4) = tempV
                }
                batch.draw(region.getTexture(), vertices, 0, NUM_VERTICES)
            }
        }
    }
}
