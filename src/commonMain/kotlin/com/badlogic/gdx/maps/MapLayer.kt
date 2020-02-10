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
package com.badlogic.gdx.maps

import Texture.TextureFilter
import com.badlogic.gdx.maps.ImageResolver
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
 * Map layer containing a set of objects and properties
 */
class MapLayer {

    /**
     * @return layer's name
     */
    /**
     * @param name new name for the layer
     */
    var name: String? = ""
    /**
     * @return layer's opacity
     */
    /**
     * @param opacity new opacity for the layer
     */
    var opacity = 1.0f
    /**
     * @return whether the layer is visible or not
     */
    /**
     * @param visible toggles layer's visibility
     */
    var isVisible = true
    private var offsetX = 0f
    private var offsetY = 0f
    private var renderOffsetX = 0f
    private var renderOffsetY = 0f
    private var renderOffsetDirty = true
    private var parent: MapLayer? = null

    /**
     * @return collection of objects contained in the layer
     */
    val objects: MapObjects? = MapObjects()

    /**
     * @return layer's set of properties
     */
    val properties: MapProperties? = MapProperties()

    /**
     * @return layer's x offset
     */
    fun getOffsetX(): Float {
        return offsetX
    }

    /**
     * @param offsetX new x offset for the layer
     */
    fun setOffsetX(offsetX: Float) {
        this.offsetX = offsetX
        invalidateRenderOffset()
    }

    /**
     * @return layer's y offset
     */
    fun getOffsetY(): Float {
        return offsetY
    }

    /**
     * @param offsetY new y offset for the layer
     */
    fun setOffsetY(offsetY: Float) {
        this.offsetY = offsetY
        invalidateRenderOffset()
    }

    /**
     * @return the layer's x render offset, this takes into consideration all parent layers' offsets
     */
    fun getRenderOffsetX(): Float {
        if (renderOffsetDirty) calculateRenderOffsets()
        return renderOffsetX
    }

    /**
     * @return the layer's y render offset, this takes into consideration all parent layers' offsets
     */
    fun getRenderOffsetY(): Float {
        if (renderOffsetDirty) calculateRenderOffsets()
        return renderOffsetY
    }

    /**
     * set the renderOffsetDirty state to true, when this layer or any parents' offset has changed
     */
    fun invalidateRenderOffset() {
        renderOffsetDirty = true
    }

    /**
     * @return the layer's parent [MapLayer], or null if the layer does not have a parent
     */
    fun getParent(): MapLayer? {
        return parent
    }

    /**
     * @param parent the layer's new parent {@MapLayer}, internal use only
     */
    fun setParent(parent: MapLayer?) {
        if (parent === this) throw GdxRuntimeException("Can't set self as the parent")
        this.parent = parent
    }

    protected fun calculateRenderOffsets() {
        if (parent != null) {
            parent!!.calculateRenderOffsets()
            renderOffsetX = parent!!.getRenderOffsetX() + offsetX
            renderOffsetY = parent!!.getRenderOffsetY() + offsetY
        } else {
            renderOffsetX = offsetX
            renderOffsetY = offsetY
        }
        renderOffsetDirty = false
    }
}
