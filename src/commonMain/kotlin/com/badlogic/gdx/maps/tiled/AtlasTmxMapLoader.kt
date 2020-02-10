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
 * A TiledMap Loader which loads tiles from a TextureAtlas instead of separate images.
 *
 *
 * It requires a map-level property called 'atlas' with its value being the relative path to the TextureAtlas. The atlas must have
 * in it indexed regions named after the tilesets used in the map. The indexes shall be local to the tileset (not the global id).
 * Strip whitespace and rotation should not be used when creating the atlas.
 *
 * @author Justin Shapcott
 * @author Manuel Bua
 */
class AtlasTmxMapLoader : BaseTmxMapLoader<AtlasTiledMapLoaderParameters?> {

    class AtlasTiledMapLoaderParameters : BaseTmxMapLoader.Parameters() {
        /**
         * force texture filters?
         */
        var forceTextureFilters = false
    }

    private interface AtlasResolver : ImageResolver {
        val atlas: TextureAtlas?

        class DirectAtlasResolver(atlas: TextureAtlas?) : AtlasResolver {
            private override val atlas: TextureAtlas?
            override fun getAtlas(): TextureAtlas? {
                return atlas
            }

            override fun getImage(name: String?): TextureRegion? {
                return atlas.findRegion(name)
            }

            init {
                this.atlas = atlas
            }
        }

        class AssetManagerAtlasResolver(assetManager: AssetManager?, atlasName: String?) : AtlasResolver {
            private val assetManager: AssetManager?
            private val atlasName: String?
            override fun getAtlas(): TextureAtlas? {
                return assetManager.get(atlasName, TextureAtlas::class.java)
            }

            override fun getImage(name: String?): TextureRegion? {
                return getAtlas().findRegion(name)
            }

            init {
                this.assetManager = assetManager
                this.atlasName = atlasName
            }
        }
    }

    protected var trackedTextures: Array<Texture?>? = Array<Texture?>()
    protected var atlasResolver: AtlasResolver? = null

    constructor() : super(InternalFileHandleResolver()) {}
    constructor(resolver: FileHandleResolver?) : super(resolver) {}

    fun load(fileName: String?): TiledMap? {
        return load(fileName, AtlasTiledMapLoaderParameters())
    }

    fun load(fileName: String?, parameter: AtlasTiledMapLoaderParameters?): TiledMap? {
        val tmxFile: FileHandle = resolve(fileName)
        this.root = xml.parse(tmxFile)
        val atlasFileHandle: FileHandle? = getAtlasFileHandle(tmxFile)
        val atlas = TextureAtlas(atlasFileHandle)
        atlasResolver = DirectAtlasResolver(atlas)
        val map: TiledMap = loadTiledMap(tmxFile, parameter, atlasResolver)
        map.setOwnedResources(Array<TextureAtlas?>(arrayOf<TextureAtlas?>(atlas)))
        setTextureFilters(parameter.textureMinFilter, parameter.textureMagFilter)
        return map
    }

    fun loadAsync(manager: AssetManager?, fileName: String?, tmxFile: FileHandle?, parameter: AtlasTiledMapLoaderParameters?) {
        val atlasHandle: FileHandle? = getAtlasFileHandle(tmxFile)
        atlasResolver = AssetManagerAtlasResolver(manager, atlasHandle.path())
        this.map = loadTiledMap(tmxFile, parameter, atlasResolver)
    }

    fun loadSync(manager: AssetManager?, fileName: String?, file: FileHandle?, parameter: AtlasTiledMapLoaderParameters?): TiledMap? {
        if (parameter != null) {
            setTextureFilters(parameter.textureMinFilter, parameter.textureMagFilter)
        }
        return map
    }

    protected override fun getDependencyAssetDescriptors(tmxFile: FileHandle?, textureParameter: TextureLoader.TextureParameter?): Array<AssetDescriptor?>? {
        val descriptors: Array<AssetDescriptor?> = Array<AssetDescriptor?>()

        // Atlas dependencies
        val atlasFileHandle: FileHandle? = getAtlasFileHandle(tmxFile)
        if (atlasFileHandle != null) {
            descriptors.add(AssetDescriptor(atlasFileHandle, TextureAtlas::class.java))
        }
        return descriptors
    }

    protected fun addStaticTiles(tmxFile: FileHandle?, imageResolver: ImageResolver?, tileSet: TiledMapTileSet?, element: Element?,
                                 tileElements: Array<Element?>?, name: String?, firstgid: Int, tilewidth: Int, tileheight: Int, spacing: Int, margin: Int,
                                 source: String?, offsetX: Int, offsetY: Int, imageSource: String?, imageWidth: Int, imageHeight: Int, image: FileHandle?) {
        val atlas: TextureAtlas? = atlasResolver!!.atlas
        for (texture in atlas.getTextures()) {
            trackedTextures.add(texture)
        }
        val props: MapProperties = tileSet!!.getProperties()
        props!!.put("imagesource", imageSource)
        props!!.put("imagewidth", imageWidth)
        props!!.put("imageheight", imageHeight)
        props!!.put("tilewidth", tilewidth)
        props!!.put("tileheight", tileheight)
        props!!.put("margin", margin)
        props!!.put("spacing", spacing)
        if (imageSource != null && imageSource.length > 0) {
            val lastgid = firstgid + imageWidth / tilewidth * (imageHeight / tileheight) - 1
            for (region in atlas.findRegions(name)) {
                // Handle unused tileIds
                if (region != null) {
                    val tileId: Int = firstgid + region.index
                    if (tileId >= firstgid && tileId <= lastgid) {
                        addStaticTiledMapTile(tileSet, region, tileId, offsetX.toFloat(), offsetY.toFloat())
                    }
                }
            }
        }

        // Add tiles with individual image sources
        for (tileElement in tileElements!!) {
            val tileId: Int = firstgid + tileElement.getIntAttribute("id", 0)
            val tile: TiledMapTile = tileSet!!.getTile(tileId)
            if (tile == null) {
                val imageElement: Element = tileElement.getChildByName("image")
                if (imageElement != null) {
                    var regionName: String = imageElement.getAttribute("source")
                    regionName = regionName.substring(0, regionName.lastIndexOf('.'))
                    val region: AtlasRegion = atlas.findRegion(regionName)
                        ?: throw GdxRuntimeException("Tileset atlasRegion not found: $regionName")
                    addStaticTiledMapTile(tileSet, region, tileId, offsetX.toFloat(), offsetY.toFloat())
                }
            }
        }
    }

    private fun getAtlasFileHandle(tmxFile: FileHandle?): FileHandle? {
        val properties: Element = root.getChildByName("properties")
        var atlasFilePath: String? = null
        if (properties != null) {
            for (property in properties.getChildrenByName("property")) {
                val name: String = property.getAttribute("name")
                if (name.startsWith("atlas")) {
                    atlasFilePath = property.getAttribute("value")
                    break
                }
            }
        }
        return if (atlasFilePath == null) {
            throw GdxRuntimeException("The map is missing the 'atlas' property")
        } else {
            val fileHandle: FileHandle = getRelativeFileHandle(tmxFile, atlasFilePath)
            if (!fileHandle.exists()) {
                throw GdxRuntimeException("The 'atlas' file could not be found: '$atlasFilePath'")
            }
            fileHandle
        }
    }

    private fun setTextureFilters(min: TextureFilter?, mag: TextureFilter?) {
        for (texture in trackedTextures!!) {
            texture.setFilter(min, mag)
        }
        trackedTextures.clear()
    }
}
