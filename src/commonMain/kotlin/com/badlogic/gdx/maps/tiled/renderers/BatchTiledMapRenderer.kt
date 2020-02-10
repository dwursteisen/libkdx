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
import com.badlogic.gdx.maps.tiled.renderers.OrthoCachedTiledMapRenderer
import com.badlogic.gdx.maps.tiled.tiles.AnimatedTiledMapTile
import com.badlogic.gdx.maps.tiled.tiles.StaticTiledMapTile
import kotlin.jvm.Throws

abstract class BatchTiledMapRenderer : TiledMapRenderer, Disposable {
    protected var map: TiledMap?
    var unitScale: Float
        protected set
    protected var batch: Batch?
    protected var viewBounds: Rectangle?
    protected var imageBounds: Rectangle? = Rectangle()
    protected var ownsBatch: Boolean
    protected var vertices: FloatArray? = FloatArray(NUM_VERTICES)
    fun getMap(): TiledMap? {
        return map
    }

    fun setMap(map: TiledMap?) {
        this.map = map
    }

    fun getBatch(): Batch? {
        return batch
    }

    fun getViewBounds(): Rectangle? {
        return viewBounds
    }

    constructor(map: TiledMap?) : this(map, 1.0f) {}
    constructor(map: TiledMap?, unitScale: Float) {
        this.map = map
        this.unitScale = unitScale
        viewBounds = Rectangle()
        batch = SpriteBatch()
        ownsBatch = true
    }

    constructor(map: TiledMap?, batch: Batch?) : this(map, 1.0f, batch) {}
    constructor(map: TiledMap?, unitScale: Float, batch: Batch?) {
        this.map = map
        this.unitScale = unitScale
        viewBounds = Rectangle()
        this.batch = batch
        ownsBatch = false
    }

    fun setView(camera: OrthographicCamera?) {
        batch.setProjectionMatrix(camera.combined)
        val width: Float = camera.viewportWidth * camera.zoom
        val height: Float = camera.viewportHeight * camera.zoom
        val w: Float = width * java.lang.Math.abs(camera.up.y) + height * java.lang.Math.abs(camera.up.x)
        val h: Float = height * java.lang.Math.abs(camera.up.y) + width * java.lang.Math.abs(camera.up.x)
        viewBounds.set(camera.position.x - w / 2, camera.position.y - h / 2, w, h)
    }

    fun setView(projection: Matrix4?, x: Float, y: Float, width: Float, height: Float) {
        batch.setProjectionMatrix(projection)
        viewBounds.set(x, y, width, height)
    }

    fun render() {
        beginRender()
        for (layer in map.getLayers()) {
            renderMapLayer(layer)
        }
        endRender()
    }

    fun render(layers: IntArray?) {
        beginRender()
        for (layerIdx in layers!!) {
            val layer: MapLayer = map.getLayers().get(layerIdx)
            renderMapLayer(layer)
        }
        endRender()
    }

    protected fun renderMapLayer(layer: MapLayer?) {
        if (!layer!!.isVisible()) return
        if (layer is MapGroupLayer) {
            val childLayers: MapLayers = (layer as MapGroupLayer?).getLayers()
            for (i in 0 until childLayers.size()) {
                val childLayer: MapLayer = childLayers.get(i)
                if (!childLayer!!.isVisible()) continue
                renderMapLayer(childLayer)
            }
        } else {
            if (layer is TiledMapTileLayer) {
                renderTileLayer(layer as TiledMapTileLayer?)
            } else if (layer is TiledMapImageLayer) {
                renderImageLayer(layer as TiledMapImageLayer?)
            } else {
                renderObjects(layer)
            }
        }
    }

    fun renderObjects(layer: MapLayer?) {
        for (`object` in layer.getObjects()) {
            renderObject(`object`)
        }
    }

    fun renderObject(`object`: MapObject?) {}
    fun renderImageLayer(layer: TiledMapImageLayer?) {
        val batchColor: Color = batch.getColor()
        val color: Float = Color.toFloatBits(batchColor.r,
            batchColor.g,
            batchColor.b,
            batchColor.a * layer.getOpacity())
        val vertices = vertices
        val region: TextureRegion = layer.getTextureRegion() ?: return
        val x: Float = layer.getX()
        val y: Float = layer.getY()
        val x1 = x * unitScale
        val y1 = y * unitScale
        val x2: Float = x1 + region.getRegionWidth() * unitScale
        val y2: Float = y1 + region.getRegionHeight() * unitScale
        imageBounds.set(x1, y1, x2 - x1, y2 - y1)
        if (viewBounds.contains(imageBounds) || viewBounds.overlaps(imageBounds)) {
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
            batch.draw(region.getTexture(), vertices, 0, NUM_VERTICES)
        }
    }

    /**
     * Called before the rendering of all layers starts.
     */
    protected fun beginRender() {
        AnimatedTiledMapTile.updateAnimationBaseTime()
        batch.begin()
    }

    /**
     * Called after the rendering of all layers ended.
     */
    protected fun endRender() {
        batch.end()
    }

    fun dispose() {
        if (ownsBatch) {
            batch.dispose()
        }
    }

    companion object {
        protected const val NUM_VERTICES = 20
    }
}
