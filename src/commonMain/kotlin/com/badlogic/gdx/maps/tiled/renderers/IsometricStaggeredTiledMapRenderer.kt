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

class IsometricStaggeredTiledMapRenderer : BatchTiledMapRenderer {
    constructor(map: TiledMap?) : super(map) {}
    constructor(map: TiledMap?, batch: Batch?) : super(map, batch) {}
    constructor(map: TiledMap?, unitScale: Float) : super(map, unitScale) {}
    constructor(map: TiledMap?, unitScale: Float, batch: Batch?) : super(map, unitScale, batch) {}

    fun renderTileLayer(layer: TiledMapTileLayer?) {
        val batchColor: Color = batch.getColor()
        val color: Float = Color.toFloatBits(batchColor.r, batchColor.g, batchColor.b, batchColor.a * layer.getOpacity())
        val layerWidth: Int = layer.getWidth()
        val layerHeight: Int = layer.getHeight()
        val layerOffsetX: Float = layer.getRenderOffsetX() * unitScale
        // offset in tiled is y down, so we flip it
        val layerOffsetY: Float = -layer.getRenderOffsetY() * unitScale
        val layerTileWidth: Float = layer.getTileWidth() * unitScale
        val layerTileHeight: Float = layer.getTileHeight() * unitScale
        val layerTileWidth50 = layerTileWidth * 0.50f
        val layerTileHeight50 = layerTileHeight * 0.50f
        val minX: Int = java.lang.Math.max(0, ((viewBounds.x - layerTileWidth50 - layerOffsetX) / layerTileWidth) as Int)
        val maxX: Int = java.lang.Math.min(layerWidth,
            ((viewBounds.x + viewBounds.width + layerTileWidth + layerTileWidth50 - layerOffsetX) / layerTileWidth) as Int)
        val minY: Int = java.lang.Math.max(0, ((viewBounds.y - layerTileHeight - layerOffsetY) / layerTileHeight) as Int)
        val maxY: Int = java.lang.Math.min(layerHeight,
            ((viewBounds.y + viewBounds.height + layerTileHeight - layerOffsetY) / layerTileHeight50) as Int)
        for (y in maxY - 1 downTo minY) {
            val offsetX: Float = if (y % 2 == 1) layerTileWidth50 else 0
            for (x in maxX - 1 downTo minX) {
                val cell: TiledMapTileLayer.Cell = layer.getCell(x, y) ?: continue
                val tile: TiledMapTile = cell.getTile()
                if (tile != null) {
                    val flipX: Boolean = cell.getFlipHorizontally()
                    val flipY: Boolean = cell.getFlipVertically()
                    val rotations: Int = cell.getRotation()
                    val region: TextureRegion = tile.getTextureRegion()
                    val x1: Float = x * layerTileWidth - offsetX + tile.getOffsetX() * unitScale + layerOffsetX
                    val y1: Float = y * layerTileHeight50 + tile.getOffsetY() * unitScale + layerOffsetY
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
                    if (rotations != 0) {
                        when (rotations) {
                            Cell.ROTATE_90 -> {
                                val tempV: Float = vertices!!.get(V1)
                                vertices!!.get(V1) = vertices!!.get(V2)
                                vertices!!.get(V2) = vertices!!.get(V3)
                                vertices!!.get(V3) = vertices!!.get(V4)
                                vertices!!.get(V4) = tempV
                                val tempU: Float = vertices!!.get(U1)
                                vertices!!.get(U1) = vertices!!.get(U2)
                                vertices!!.get(U2) = vertices!!.get(U3)
                                vertices!!.get(U3) = vertices!!.get(U4)
                                vertices!!.get(U4) = tempU
                            }
                            Cell.ROTATE_180 -> {
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
                            Cell.ROTATE_270 -> {
                                val tempV: Float = vertices!!.get(V1)
                                vertices!!.get(V1) = vertices!!.get(V4)
                                vertices!!.get(V4) = vertices!!.get(V3)
                                vertices!!.get(V3) = vertices!!.get(V2)
                                vertices!!.get(V2) = tempV
                                val tempU: Float = vertices!!.get(U1)
                                vertices!!.get(U1) = vertices!!.get(U4)
                                vertices!!.get(U4) = vertices!!.get(U3)
                                vertices!!.get(U3) = vertices!!.get(U2)
                                vertices!!.get(U2) = tempU
                            }
                        }
                    }
                    batch.draw(region.getTexture(), vertices, 0, NUM_VERTICES)
                }
            }
        }
    }
}
