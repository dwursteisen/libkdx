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
 * Resolves an image by a string, wrapper around a Map or AssetManager to load maps either directly or via AssetManager.
 *
 * @author mzechner
 */
interface ImageResolver {

    /**
     * @param name
     * @return the Texture for the given image name or null.
     */
    fun getImage(name: String?): TextureRegion?
    class DirectImageResolver(images: ObjectMap<String?, Texture?>?) : ImageResolver {
        private val images: ObjectMap<String?, Texture?>?
        override fun getImage(name: String?): TextureRegion? {
            return TextureRegion(images.get(name))
        }

        init {
            this.images = images
        }
    }

    class AssetManagerImageResolver(assetManager: AssetManager?) : ImageResolver {
        private val assetManager: AssetManager?
        override fun getImage(name: String?): TextureRegion? {
            return TextureRegion(assetManager.get(name, Texture::class.java))
        }

        init {
            this.assetManager = assetManager
        }
    }

    class TextureAtlasImageResolver(atlas: TextureAtlas?) : ImageResolver {
        private val atlas: TextureAtlas?
        override fun getImage(name: String?): TextureRegion? {
            return atlas.findRegion(name)
        }

        init {
            this.atlas = atlas
        }
    }
}
