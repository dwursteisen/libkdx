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

class IsometricTiledMapRenderer : BatchTiledMapRenderer {
    private var isoTransform: Matrix4? = null
    private var invIsotransform: Matrix4? = null
    private val screenPos: Vector3? = Vector3()
    private val topRight: Vector2? = Vector2()
    private val bottomLeft: Vector2? = Vector2()
    private val topLeft: Vector2? = Vector2()
    private val bottomRight: Vector2? = Vector2()

    constructor(map: TiledMap?) : super(map) {
        init()
    }

    constructor(map: TiledMap?, batch: Batch?) : super(map, batch) {
        init()
    }

    constructor(map: TiledMap?, unitScale: Float) : super(map, unitScale) {
        init()
    }

    constructor(map: TiledMap?, unitScale: Float, batch: Batch?) : super(map, unitScale, batch) {
        init()
    }

    private fun init() {
        // create the isometric transform
        isoTransform = Matrix4()
        isoTransform.idt()

        // isoTransform.translate(0, 32, 0);
        isoTransform.scale((java.lang.Math.sqrt(2.0) / 2.0) as Float, (java.lang.Math.sqrt(2.0) / 4.0) as Float, 1.0f)
        isoTransform.rotate(0.0f, 0.0f, 1.0f, -45)

        // ... and the inverse matrix
        invIsotransform = Matrix4(isoTransform)
        invIsotransform.inv()
    }

    private fun translateScreenToIso(vec: Vector2?): Vector3? {
        screenPos.set(vec.x, vec.y, 0)
        screenPos.mul(invIsotransform)
        return screenPos
    }

    fun renderTileLayer(layer: TiledMapTileLayer?) {
        val batchColor: Color = batch.getColor()
        val color: Float = Color.toFloatBits(batchColor.r, batchColor.g, batchColor.b, batchColor.a * layer.getOpacity())
        val tileWidth: Float = layer.getTileWidth() * unitScale
        val tileHeight: Float = layer.getTileHeight() * unitScale
        val layerOffsetX: Float = layer.getRenderOffsetX() * unitScale
        // offset in tiled is y down, so we flip it
        val layerOffsetY: Float = -layer.getRenderOffsetY() * unitScale
        val halfTileWidth = tileWidth * 0.5f
        val halfTileHeight = tileHeight * 0.5f

        // setting up the screen points
        // COL1
        topRight.set(viewBounds.x + viewBounds.width - layerOffsetX, viewBounds.y - layerOffsetY)
        // COL2
        bottomLeft.set(viewBounds.x - layerOffsetX, viewBounds.y + viewBounds.height - layerOffsetY)
        // ROW1
        topLeft.set(viewBounds.x - layerOffsetX, viewBounds.y - layerOffsetY)
        // ROW2
        bottomRight.set(viewBounds.x + viewBounds.width - layerOffsetX, viewBounds.y + viewBounds.height - layerOffsetY)

        // transforming screen coordinates to iso coordinates
        val row1 = (translateScreenToIso(topLeft).y / tileWidth) as Int - 2
        val row2 = (translateScreenToIso(bottomRight).y / tileWidth) as Int + 2
        val col1 = (translateScreenToIso(bottomLeft).x / tileWidth) as Int - 2
        val col2 = (translateScreenToIso(topRight).x / tileWidth) as Int + 2
        for (row in row2 downTo row1) {
            for (col in col1..col2) {
                val x = col * halfTileWidth + row * halfTileWidth
                val y = row * halfTileHeight - col * halfTileHeight
                val cell: TiledMapTileLayer.Cell = layer.getCell(col, row) ?: continue
                val tile: TiledMapTile = cell.getTile()
                if (tile != null) {
                    val flipX: Boolean = cell.getFlipHorizontally()
                    val flipY: Boolean = cell.getFlipVertically()
                    val rotations: Int = cell.getRotation()
                    val region: TextureRegion = tile.getTextureRegion()
                    val x1: Float = x + tile.getOffsetX() * unitScale + layerOffsetX
                    val y1: Float = y + tile.getOffsetY() * unitScale + layerOffsetY
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
