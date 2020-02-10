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
package com.badlogic.gdx.maps.tiled.objects

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
 * A [MapObject] with a [TiledMapTile]. Can be both [StaticTiledMapTile] or [AnimatedTiledMapTile]. For
 * compatibility reasons, this extends [TextureMapObject]. Use [TiledMapTile.getTextureRegion] instead of
 * [.getTextureRegion].
 *
 * @author Daniel Holderbaum
 */
class TiledMapTileMapObject(tile: TiledMapTile?, var isFlipHorizontally: Boolean, var isFlipVertically: Boolean) : TextureMapObject() {

    private var tile: TiledMapTile?

    fun getTile(): TiledMapTile? {
        return tile
    }

    fun setTile(tile: TiledMapTile?) {
        this.tile = tile
    }

    init {
        this.tile = tile
        val textureRegion = TextureRegion(tile.getTextureRegion())
        textureRegion.flip(isFlipHorizontally, isFlipVertically)
        setTextureRegion(textureRegion)
    }
}
