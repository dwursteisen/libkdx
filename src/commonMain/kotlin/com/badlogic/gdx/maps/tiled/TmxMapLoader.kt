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
 * @brief synchronous loader for TMX maps created with the Tiled tool
 */
class TmxMapLoader : BaseTmxMapLoader<TmxMapLoader.Parameters?> {

    class Parameters : BaseTmxMapLoader.Parameters() {}

    constructor() : super(InternalFileHandleResolver()) {}

    /**
     * Creates loader
     *
     * @param resolver
     */
    constructor(resolver: FileHandleResolver?) : super(resolver) {}

    /**
     * Loads the [TiledMap] from the given file. The file is resolved via the [FileHandleResolver] set in the
     * constructor of this class. By default it will resolve to an internal file. The map will be loaded for a y-up coordinate
     * system.
     *
     * @param fileName the filename
     * @return the TiledMap
     */
    fun load(fileName: String?): TiledMap? {
        return load(fileName, Parameters())
    }

    /**
     * Loads the [TiledMap] from the given file. The file is resolved via the [FileHandleResolver] set in the
     * constructor of this class. By default it will resolve to an internal file.
     *
     * @param fileName  the filename
     * @param parameter specifies whether to use y-up, generate mip maps etc.
     * @return the TiledMap
     */
    fun load(fileName: String?, parameter: Parameters?): TiledMap? {
        val tmxFile: FileHandle = resolve(fileName)
        this.root = xml.parse(tmxFile)
        val textures: ObjectMap<String?, Texture?> = ObjectMap<String?, Texture?>()
        val textureFiles: Array<FileHandle?>? = getDependencyFileHandles(tmxFile)
        for (textureFile in textureFiles!!) {
            val texture = Texture(textureFile, parameter.generateMipMaps)
            texture.setFilter(parameter.textureMinFilter, parameter.textureMagFilter)
            textures.put(textureFile.path(), texture)
        }
        val map: TiledMap = loadTiledMap(tmxFile, parameter, DirectImageResolver(textures))
        map!!.setOwnedResources(textures.values().toArray())
        return map
    }

    fun loadAsync(manager: AssetManager?, fileName: String?, tmxFile: FileHandle?, parameter: Parameters?) {
        this.map = loadTiledMap(tmxFile, parameter, AssetManagerImageResolver(manager))
    }

    fun loadSync(manager: AssetManager?, fileName: String?, file: FileHandle?, parameter: Parameters?): TiledMap? {
        return map
    }

    protected override fun getDependencyAssetDescriptors(tmxFile: FileHandle?, textureParameter: TextureLoader.TextureParameter?): Array<AssetDescriptor?>? {
        val descriptors: Array<AssetDescriptor?> = Array<AssetDescriptor?>()
        val fileHandles: Array<FileHandle?>? = getDependencyFileHandles(tmxFile)
        for (handle in fileHandles!!) {
            descriptors.add(AssetDescriptor(handle, Texture::class.java, textureParameter))
        }
        return descriptors
    }

    private fun getDependencyFileHandles(tmxFile: FileHandle?): Array<FileHandle?>? {
        val fileHandles: Array<FileHandle?> = Array<FileHandle?>()

        // TileSet descriptors
        for (tileset in root.getChildrenByName("tileset")) {
            val source: String = tileset.getAttribute("source", null)
            if (source != null) {
                val tsxFile: FileHandle = getRelativeFileHandle(tmxFile, source)
                tileset = xml.parse(tsxFile)
                val imageElement: Element = tileset.getChildByName("image")
                if (imageElement != null) {
                    val imageSource: String = tileset.getChildByName("image").getAttribute("source")
                    val image: FileHandle = getRelativeFileHandle(tsxFile, imageSource)
                    fileHandles.add(image)
                } else {
                    for (tile in tileset.getChildrenByName("tile")) {
                        val imageSource: String = tile.getChildByName("image").getAttribute("source")
                        val image: FileHandle = getRelativeFileHandle(tsxFile, imageSource)
                        fileHandles.add(image)
                    }
                }
            } else {
                val imageElement: Element = tileset.getChildByName("image")
                if (imageElement != null) {
                    val imageSource: String = tileset.getChildByName("image").getAttribute("source")
                    val image: FileHandle = getRelativeFileHandle(tmxFile, imageSource)
                    fileHandles.add(image)
                } else {
                    for (tile in tileset.getChildrenByName("tile")) {
                        val imageSource: String = tile.getChildByName("image").getAttribute("source")
                        val image: FileHandle = getRelativeFileHandle(tmxFile, imageSource)
                        fileHandles.add(image)
                    }
                }
            }
        }

        // ImageLayer descriptors
        for (imageLayer in root.getChildrenByName("imagelayer")) {
            val image: Element = imageLayer.getChildByName("image")
            val source: String = image.getAttribute("source", null)
            if (source != null) {
                val handle: FileHandle = getRelativeFileHandle(tmxFile, source)
                fileHandles.add(handle)
            }
        }
        return fileHandles
    }

    protected fun addStaticTiles(tmxFile: FileHandle?, imageResolver: ImageResolver?, tileSet: TiledMapTileSet?, element: Element?,
                                 tileElements: Array<Element?>?, name: String?, firstgid: Int, tilewidth: Int, tileheight: Int, spacing: Int, margin: Int,
                                 source: String?, offsetX: Int, offsetY: Int, imageSource: String?, imageWidth: Int, imageHeight: Int, image: FileHandle?) {
        var imageSource = imageSource
        var image: FileHandle? = image
        val props: MapProperties = tileSet!!.getProperties()
        if (image != null) {
            // One image for the whole tileSet
            val texture: TextureRegion = imageResolver!!.getImage(image.path())
            props!!.put("imagesource", imageSource)
            props!!.put("imagewidth", imageWidth)
            props!!.put("imageheight", imageHeight)
            props!!.put("tilewidth", tilewidth)
            props!!.put("tileheight", tileheight)
            props!!.put("margin", margin)
            props!!.put("spacing", spacing)
            val stopWidth: Int = texture.getRegionWidth() - tilewidth
            val stopHeight: Int = texture.getRegionHeight() - tileheight
            var id = firstgid
            var y = margin
            while (y <= stopHeight) {
                var x = margin
                while (x <= stopWidth) {
                    val tileRegion = TextureRegion(texture, x, y, tilewidth, tileheight)
                    val tileId = id++
                    addStaticTiledMapTile(tileSet, tileRegion, tileId, offsetX.toFloat(), offsetY.toFloat())
                    x += tilewidth + spacing
                }
                y += tileheight + spacing
            }
        } else {
            // Every tile has its own image source
            for (tileElement in tileElements!!) {
                val imageElement: Element = tileElement.getChildByName("image")
                if (imageElement != null) {
                    imageSource = imageElement.getAttribute("source")
                    if (source != null) {
                        image = getRelativeFileHandle(getRelativeFileHandle(tmxFile, source), imageSource)
                    } else {
                        image = getRelativeFileHandle(tmxFile, imageSource)
                    }
                }
                val texture: TextureRegion = imageResolver!!.getImage(image.path())
                val tileId: Int = firstgid + tileElement.getIntAttribute("id")
                addStaticTiledMapTile(tileSet, texture, tileId, offsetX.toFloat(), offsetY.toFloat())
            }
        }
    }
}
