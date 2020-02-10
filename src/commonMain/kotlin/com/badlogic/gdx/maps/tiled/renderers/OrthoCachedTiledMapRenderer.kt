/*******************************************************************************
 * Copyright 2014 See AUTHORS file.
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
import com.badlogic.gdx.maps.tiled.tiles.AnimatedTiledMapTile
import com.badlogic.gdx.maps.tiled.tiles.StaticTiledMapTile
import kotlin.jvm.Throws

/**
 * Renders ortho tiles by caching geometry on the GPU. How much is cached is controlled by [.setOverCache]. When the
 * view reaches the edge of the cached tiles, the cache is rebuilt at the new view position.
 *
 *
 * This class may have poor performance when tiles are often changed dynamically, since the cache must be rebuilt after each
 * change.
 *
 * @author Justin Shapcott
 * @author Nathan Sweet
 */
class OrthoCachedTiledMapRenderer(map: TiledMap?, unitScale: Float, cacheSize: Int) : TiledMapRenderer, Disposable {

    protected val map: TiledMap?
    protected val spriteCache: SpriteCache?
    protected val vertices: FloatArray? = FloatArray(20)
    protected var blending = false
    protected var unitScale: Float
    protected val viewBounds: Rectangle? = Rectangle()
    protected val cacheBounds: Rectangle? = Rectangle()
    protected var overCache = 0.50f
    protected var maxTileWidth = 0f
    protected var maxTileHeight = 0f

    /**
     * Returns true if tiles are currently cached.
     */
    var isCached = false
        protected set
    protected var count = 0
    protected var canCacheMoreN = false
    protected var canCacheMoreE = false
    protected var canCacheMoreW = false
    protected var canCacheMoreS = false

    /**
     * Creates a renderer with a unit scale of 1 and cache size of 2000.
     */
    constructor(map: TiledMap?) : this(map, 1f, 2000) {}

    /**
     * Creates a renderer with a cache size of 2000.
     */
    constructor(map: TiledMap?, unitScale: Float) : this(map, unitScale, 2000) {}

    fun setView(camera: OrthographicCamera?) {
        spriteCache.setProjectionMatrix(camera.combined)
        val width: Float = camera.viewportWidth * camera.zoom + maxTileWidth * 2 * unitScale
        val height: Float = camera.viewportHeight * camera.zoom + maxTileHeight * 2 * unitScale
        viewBounds.set(camera.position.x - width / 2, camera.position.y - height / 2, width, height)
        if (canCacheMoreW && viewBounds.x < cacheBounds.x - tolerance ||  //
            canCacheMoreS && viewBounds.y < cacheBounds.y - tolerance ||  //
            canCacheMoreE && viewBounds.x + viewBounds.width > cacheBounds.x + cacheBounds.width + tolerance ||  //
            canCacheMoreN && viewBounds.y + viewBounds.height > cacheBounds.y + cacheBounds.height + tolerance //
        ) isCached = false
    }

    fun setView(projection: Matrix4?, x: Float, y: Float, width: Float, height: Float) {
        var x = x
        var y = y
        var width = width
        var height = height
        spriteCache.setProjectionMatrix(projection)
        x -= maxTileWidth * unitScale
        y -= maxTileHeight * unitScale
        width += maxTileWidth * 2 * unitScale
        height += maxTileHeight * 2 * unitScale
        viewBounds.set(x, y, width, height)
        if (canCacheMoreW && viewBounds.x < cacheBounds.x - tolerance ||  //
            canCacheMoreS && viewBounds.y < cacheBounds.y - tolerance ||  //
            canCacheMoreE && viewBounds.x + viewBounds.width > cacheBounds.x + cacheBounds.width + tolerance ||  //
            canCacheMoreN && viewBounds.y + viewBounds.height > cacheBounds.y + cacheBounds.height + tolerance //
        ) isCached = false
    }

    fun render() {
        if (!isCached) {
            isCached = true
            count = 0
            spriteCache.clear()
            val extraWidth: Float = viewBounds.width * overCache
            val extraHeight: Float = viewBounds.height * overCache
            cacheBounds.x = viewBounds.x - extraWidth
            cacheBounds.y = viewBounds.y - extraHeight
            cacheBounds.width = viewBounds.width + extraWidth * 2
            cacheBounds.height = viewBounds.height + extraHeight * 2
            for (layer in map.getLayers()) {
                spriteCache.beginCache()
                if (layer is TiledMapTileLayer) {
                    renderTileLayer(layer as TiledMapTileLayer)
                } else if (layer is TiledMapImageLayer) {
                    renderImageLayer(layer as TiledMapImageLayer)
                }
                spriteCache.endCache()
            }
        }
        if (blending) {
            Gdx.gl.glEnable(GL20.GL_BLEND)
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        }
        spriteCache.begin()
        val mapLayers: MapLayers = map.getLayers()
        var i = 0
        val j: Int = mapLayers.getCount()
        while (i < j) {
            val layer: MapLayer = mapLayers.get(i)
            if (layer!!.isVisible()) {
                spriteCache.draw(i)
                renderObjects(layer)
            }
            i++
        }
        spriteCache.end()
        if (blending) Gdx.gl.glDisable(GL20.GL_BLEND)
    }

    fun render(layers: IntArray?) {
        if (!isCached) {
            isCached = true
            count = 0
            spriteCache.clear()
            val extraWidth: Float = viewBounds.width * overCache
            val extraHeight: Float = viewBounds.height * overCache
            cacheBounds.x = viewBounds.x - extraWidth
            cacheBounds.y = viewBounds.y - extraHeight
            cacheBounds.width = viewBounds.width + extraWidth * 2
            cacheBounds.height = viewBounds.height + extraHeight * 2
            for (layer in map.getLayers()) {
                spriteCache.beginCache()
                if (layer is TiledMapTileLayer) {
                    renderTileLayer(layer as TiledMapTileLayer)
                } else if (layer is TiledMapImageLayer) {
                    renderImageLayer(layer as TiledMapImageLayer)
                }
                spriteCache.endCache()
            }
        }
        if (blending) {
            Gdx.gl.glEnable(GL20.GL_BLEND)
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        }
        spriteCache.begin()
        val mapLayers: MapLayers = map.getLayers()
        for (i in layers!!) {
            val layer: MapLayer = mapLayers.get(i)
            if (layer!!.isVisible()) {
                spriteCache.draw(i)
                renderObjects(layer)
            }
        }
        spriteCache.end()
        if (blending) Gdx.gl.glDisable(GL20.GL_BLEND)
    }

    fun renderObjects(layer: MapLayer?) {
        for (`object` in layer.getObjects()) {
            renderObject(`object`)
        }
    }

    fun renderObject(`object`: MapObject?) {}
    fun renderTileLayer(layer: TiledMapTileLayer?) {
        val color: Float = Color.toFloatBits(1, 1, 1, layer.getOpacity())
        val layerWidth: Int = layer.getWidth()
        val layerHeight: Int = layer.getHeight()
        val layerTileWidth: Float = layer.getTileWidth() * unitScale
        val layerTileHeight: Float = layer.getTileHeight() * unitScale
        val layerOffsetX: Float = layer.getRenderOffsetX() * unitScale
        // offset in tiled is y down, so we flip it
        val layerOffsetY: Float = -layer.getRenderOffsetY() * unitScale
        val col1: Int = java.lang.Math.max(0, ((cacheBounds.x - layerOffsetX) / layerTileWidth) as Int)
        val col2: Int = java.lang.Math.min(layerWidth,
            ((cacheBounds.x + cacheBounds.width + layerTileWidth - layerOffsetX) / layerTileWidth) as Int)
        val row1: Int = java.lang.Math.max(0, ((cacheBounds.y - layerOffsetY) / layerTileHeight) as Int)
        val row2: Int = java.lang.Math.min(layerHeight,
            ((cacheBounds.y + cacheBounds.height + layerTileHeight - layerOffsetY) / layerTileHeight) as Int)
        canCacheMoreN = row2 < layerHeight
        canCacheMoreE = col2 < layerWidth
        canCacheMoreW = col1 > 0
        canCacheMoreS = row1 > 0
        val vertices = vertices
        for (row in row2 downTo row1) {
            for (col in col1 until col2) {
                val cell: TiledMapTileLayer.Cell = layer.getCell(col, row) ?: continue
                val tile: TiledMapTile = cell.getTile() ?: continue
                count++
                val flipX: Boolean = cell.getFlipHorizontally()
                val flipY: Boolean = cell.getFlipVertically()
                val rotations: Int = cell.getRotation()
                val region: TextureRegion = tile.getTextureRegion()
                val texture: Texture = region.getTexture()
                val x1: Float = col * layerTileWidth + tile.getOffsetX() * unitScale + layerOffsetX
                val y1: Float = row * layerTileHeight + tile.getOffsetY() * unitScale + layerOffsetY
                val x2: Float = x1 + region.getRegionWidth() * unitScale
                val y2: Float = y1 + region.getRegionHeight() * unitScale
                val adjustX: Float = 0.5f / texture.getWidth()
                val adjustY: Float = 0.5f / texture.getHeight()
                val u1: Float = region.getU() + adjustX
                val v1: Float = region.getV2() - adjustY
                val u2: Float = region.getU2() - adjustX
                val v2: Float = region.getV() + adjustY
                vertices!![X1] = x1
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
                spriteCache.add(texture, vertices, 0, NUM_VERTICES)
            }
        }
    }

    fun renderImageLayer(layer: TiledMapImageLayer?) {
        val color: Float = Color.toFloatBits(1.0f, 1.0f, 1.0f, layer.getOpacity())
        val vertices = vertices
        val region: TextureRegion = layer.getTextureRegion() ?: return
        val x: Float = layer.getX()
        val y: Float = layer.getY()
        val x1 = x * unitScale
        val y1 = y * unitScale
        val x2: Float = x1 + region.getRegionWidth() * unitScale
        val y2: Float = y1 + region.getRegionHeight() * unitScale
        val u1: Float = region.getU()
        val v1: Float = region.getV2()
        val u2: Float = region.getU2()
        val v2: Float = region.getV()
        vertices!![X1] = x1
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
        spriteCache.add(region.getTexture(), vertices, 0, NUM_VERTICES)
    }

    /**
     * Causes the cache to be rebuilt the next time it is rendered.
     */
    fun invalidateCache() {
        isCached = false
    }

    /**
     * Sets the percentage of the view that is cached in each direction. Default is 0.5.
     *
     *
     * Eg, 0.75 will cache 75% of the width of the view to the left and right of the view, and 75% of the height of the view above
     * and below the view.
     */
    fun setOverCache(overCache: Float) {
        this.overCache = overCache
    }

    /**
     * Expands the view size in each direction, ensuring that tiles of this size or smaller are never culled from the visible
     * portion of the view. Default is 0,0.
     *
     *
     * The amount of tiles cached is computed using `(view size + max tile size) * overCache`, meaning the max tile size
     * increases the amount cached and possibly [.setOverCache] can be reduced.
     *
     *
     * If the view size and [.setOverCache] are configured so the size of the cached tiles is always larger than the
     * largest tile size, this setting is not needed.
     */
    fun setMaxTileSize(maxPixelWidth: Float, maxPixelHeight: Float) {
        maxTileWidth = maxPixelWidth
        maxTileHeight = maxPixelHeight
    }

    fun setBlending(blending: Boolean) {
        this.blending = blending
    }

    fun getSpriteCache(): SpriteCache? {
        return spriteCache
    }

    fun dispose() {
        spriteCache.dispose()
    }

    companion object {
        private const val tolerance = 0.00001f
        protected const val NUM_VERTICES = 20
    }

    /**
     * @param cacheSize The maximum number of tiles that can be cached.
     */
    init {
        this.map = map
        this.unitScale = unitScale
        spriteCache = SpriteCache(cacheSize, true)
    }
}
