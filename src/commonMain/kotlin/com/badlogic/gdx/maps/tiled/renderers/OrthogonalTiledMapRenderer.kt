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

class OrthogonalTiledMapRenderer : BatchTiledMapRenderer {
    constructor(map: TiledMap?) : super(map) {}
    constructor(map: TiledMap?, batch: Batch?) : super(map, batch) {}
    constructor(map: TiledMap?, unitScale: Float) : super(map, unitScale) {}
    constructor(map: TiledMap?, unitScale: Float, batch: Batch?) : super(map, unitScale, batch) {}

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
        val col1: Int = java.lang.Math.max(0, ((viewBounds.x - layerOffsetX) / layerTileWidth) as Int)
        val col2: Int = java.lang.Math.min(layerWidth,
            ((viewBounds.x + viewBounds.width + layerTileWidth - layerOffsetX) / layerTileWidth) as Int)
        val row1: Int = java.lang.Math.max(0, ((viewBounds.y - layerOffsetY) / layerTileHeight) as Int)
        val row2: Int = java.lang.Math.min(layerHeight,
            ((viewBounds.y + viewBounds.height + layerTileHeight - layerOffsetY) / layerTileHeight) as Int)
        var y = row2 * layerTileHeight + layerOffsetY
        val xStart = col1 * layerTileWidth + layerOffsetX
        val vertices: FloatArray = this.vertices
        for (row in row2 downTo row1) {
            var x = xStart
            for (col in col1 until col2) {
                val cell: TiledMapTileLayer.Cell = layer.getCell(col, row)
                if (cell == null) {
                    x += layerTileWidth
                    continue
                }
                val tile: TiledMapTile = cell.getTile()
                if (tile != null) {
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
                    vertices[X1] = x1
                    vertices[Y1] = y1
                    vertices[C1] = color
                    vertices[U1] = u1
                    vertices[V1] = v1
                    vertices[X2] = x1
                    vertices[Y2] = y2
                    vertices[C2] = color
                    vertices[U2] = u1
                    vertices[V2] = v2
                    vertices[X3] = x2
                    vertices[Y3] = y2
                    vertices[C3] = color
                    vertices[U3] = u2
                    vertices[V3] = v2
                    vertices[X4] = x2
                    vertices[Y4] = y1
                    vertices[C4] = color
                    vertices[U4] = u2
                    vertices[V4] = v1
                    if (flipX) {
                        var temp = vertices[U1]
                        vertices[U1] = vertices[U3]
                        vertices[U3] = temp
                        temp = vertices[U2]
                        vertices[U2] = vertices[U4]
                        vertices[U4] = temp
                    }
                    if (flipY) {
                        var temp = vertices[V1]
                        vertices[V1] = vertices[V3]
                        vertices[V3] = temp
                        temp = vertices[V2]
                        vertices[V2] = vertices[V4]
                        vertices[V4] = temp
                    }
                    if (rotations != 0) {
                        when (rotations) {
                            Cell.ROTATE_90 -> {
                                val tempV = vertices[V1]
                                vertices[V1] = vertices[V2]
                                vertices[V2] = vertices[V3]
                                vertices[V3] = vertices[V4]
                                vertices[V4] = tempV
                                val tempU = vertices[U1]
                                vertices[U1] = vertices[U2]
                                vertices[U2] = vertices[U3]
                                vertices[U3] = vertices[U4]
                                vertices[U4] = tempU
                            }
                            Cell.ROTATE_180 -> {
                                var tempU = vertices[U1]
                                vertices[U1] = vertices[U3]
                                vertices[U3] = tempU
                                tempU = vertices[U2]
                                vertices[U2] = vertices[U4]
                                vertices[U4] = tempU
                                var tempV = vertices[V1]
                                vertices[V1] = vertices[V3]
                                vertices[V3] = tempV
                                tempV = vertices[V2]
                                vertices[V2] = vertices[V4]
                                vertices[V4] = tempV
                            }
                            Cell.ROTATE_270 -> {
                                val tempV = vertices[V1]
                                vertices[V1] = vertices[V4]
                                vertices[V4] = vertices[V3]
                                vertices[V3] = vertices[V2]
                                vertices[V2] = tempV
                                val tempU = vertices[U1]
                                vertices[U1] = vertices[U4]
                                vertices[U4] = vertices[U3]
                                vertices[U3] = vertices[U2]
                                vertices[U2] = tempU
                            }
                        }
                    }
                    batch.draw(region.getTexture(), vertices, 0, NUM_VERTICES)
                }
                x += layerTileWidth
            }
            y -= layerTileHeight
        }
    }
}
