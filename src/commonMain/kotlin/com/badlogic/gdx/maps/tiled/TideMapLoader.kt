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
import com.badlogic.gdx.maps.tiled.renderers.BatchTiledMapRenderer
import com.badlogic.gdx.maps.tiled.renderers.OrthoCachedTiledMapRenderer
import com.badlogic.gdx.maps.tiled.tiles.AnimatedTiledMapTile
import com.badlogic.gdx.maps.tiled.tiles.StaticTiledMapTile
import kotlin.jvm.Throws

class TideMapLoader : SynchronousAssetLoader<TiledMap?, TideMapLoader.Parameters?> {
    class Parameters : AssetLoaderParameters<TiledMap?>()

    private val xml: XmlReader? = XmlReader()
    private var root: Element? = null

    constructor() : super(InternalFileHandleResolver()) {}
    constructor(resolver: FileHandleResolver?) : super(resolver) {}

    fun load(fileName: String?): TiledMap? {
        return try {
            val tideFile: FileHandle = resolve(fileName)
            root = xml.parse(tideFile)
            val textures: ObjectMap<String?, Texture?> = ObjectMap<String?, Texture?>()
            for (textureFile in loadTileSheets(root, tideFile)!!) {
                textures.put(textureFile.path(), Texture(textureFile))
            }
            val imageResolver = DirectImageResolver(textures)
            val map: TiledMap? = loadMap(root, tideFile, imageResolver)
            map!!.setOwnedResources(textures.values().toArray())
            map
        } catch (e: IOException) {
            throw GdxRuntimeException("Couldn't load tilemap '$fileName'", e)
        }
    }

    fun load(assetManager: AssetManager?, fileName: String?, tideFile: FileHandle?, parameter: Parameters?): TiledMap? {
        return try {
            loadMap(root, tideFile, AssetManagerImageResolver(assetManager))
        } catch (e: java.lang.Exception) {
            throw GdxRuntimeException("Couldn't load tilemap '$fileName'", e)
        }
    }

    fun getDependencies(fileName: String?, tmxFile: FileHandle?, parameter: Parameters?): Array<AssetDescriptor?>? {
        val dependencies: Array<AssetDescriptor?> = Array<AssetDescriptor?>()
        return try {
            root = xml.parse(tmxFile)
            for (image in loadTileSheets(root, tmxFile)!!) {
                dependencies.add(AssetDescriptor(image.path(), Texture::class.java))
            }
            dependencies
        } catch (e: IOException) {
            throw GdxRuntimeException("Couldn't load tilemap '$fileName'", e)
        }
    }

    /**
     * Loads the map data, given the XML root element and an [ImageResolver] used to return the tileset Textures
     *
     * @param root          the XML root element
     * @param tmxFile       the Filehandle of the tmx file
     * @param imageResolver the [ImageResolver]
     * @return the [TiledMap]
     */
    private fun loadMap(root: Element?, tmxFile: FileHandle?, imageResolver: ImageResolver?): TiledMap? {
        val map = TiledMap()
        val properties: Element = root.getChildByName("Properties")
        if (properties != null) {
            loadProperties(map.getProperties(), properties)
        }
        val tilesheets: Element = root.getChildByName("TileSheets")
        for (tilesheet in tilesheets.getChildrenByName("TileSheet")) {
            loadTileSheet(map, tilesheet, tmxFile, imageResolver)
        }
        val layers: Element = root.getChildByName("Layers")
        for (layer in layers.getChildrenByName("Layer")) {
            loadLayer(map, layer)
        }
        return map
    }

    /**
     * Loads the tilesets
     *
     * @param root the root XML element
     * @return a list of filenames for images containing tiles
     * @throws IOException
     */
    @Throws(IOException::class)
    private fun loadTileSheets(root: Element?, tideFile: FileHandle?): Array<FileHandle?>? {
        val images: Array<FileHandle?> = Array<FileHandle?>()
        val tilesheets: Element = root.getChildByName("TileSheets")
        for (tileset in tilesheets.getChildrenByName("TileSheet")) {
            val imageSource: Element = tileset.getChildByName("ImageSource")
            val image: FileHandle? = getRelativeFileHandle(tideFile, imageSource.getText())
            images.add(image)
        }
        return images
    }

    private fun loadTileSheet(map: TiledMap?, element: Element?, tideFile: FileHandle?, imageResolver: ImageResolver?) {
        if (element.getName().equals("TileSheet")) {
            val id: String = element.getAttribute("Id")
            val description: String = element.getChildByName("Description").getText()
            val imageSource: String = element.getChildByName("ImageSource").getText()
            val alignment: Element = element.getChildByName("Alignment")
            val sheetSize: String = alignment.getAttribute("SheetSize")
            val tileSize: String = alignment.getAttribute("TileSize")
            val margin: String = alignment.getAttribute("Margin")
            val spacing: String = alignment.getAttribute("Spacing")
            val sheetSizeParts: Array<String?> = sheetSize.split(" x ").toTypedArray()
            val sheetSizeX = sheetSizeParts[0]!!.toInt()
            val sheetSizeY = sheetSizeParts[1]!!.toInt()
            val tileSizeParts: Array<String?> = tileSize.split(" x ").toTypedArray()
            val tileSizeX = tileSizeParts[0]!!.toInt()
            val tileSizeY = tileSizeParts[1]!!.toInt()
            val marginParts: Array<String?> = margin.split(" x ").toTypedArray()
            val marginX = marginParts[0]!!.toInt()
            val marginY = marginParts[1]!!.toInt()
            val spacingParts: Array<String?> = margin.split(" x ").toTypedArray()
            val spacingX = spacingParts[0]!!.toInt()
            val spacingY = spacingParts[1]!!.toInt()
            val image: FileHandle? = getRelativeFileHandle(tideFile, imageSource)
            val texture: TextureRegion = imageResolver!!.getImage(image.path())
            val tilesets: TiledMapTileSets = map.getTileSets()
            var firstgid = 1
            for (tileset in tilesets) {
                firstgid += tileset!!.size()
            }
            val tileset = TiledMapTileSet()
            tileset.setName(id)
            tileset!!.getProperties()!!.put("firstgid", firstgid)
            var gid = firstgid
            val stopWidth: Int = texture.getRegionWidth() - tileSizeX
            val stopHeight: Int = texture.getRegionHeight() - tileSizeY
            var y = marginY
            while (y <= stopHeight) {
                var x = marginX
                while (x <= stopWidth) {
                    val tile: TiledMapTile = StaticTiledMapTile(TextureRegion(texture, x, y, tileSizeX, tileSizeY))
                    tile.setId(gid)
                    tileset!!.putTile(gid++, tile)
                    x += tileSizeX + spacingX
                }
                y += tileSizeY + spacingY
            }
            val properties: Element = element.getChildByName("Properties")
            if (properties != null) {
                loadProperties(tileset!!.getProperties(), properties)
            }
            tilesets!!.addTileSet(tileset)
        }
    }

    private fun loadLayer(map: TiledMap?, element: Element?) {
        if (element.getName().equals("Layer")) {
            val id: String = element.getAttribute("Id")
            val visible: String = element.getAttribute("Visible")
            val dimensions: Element = element.getChildByName("Dimensions")
            val layerSize: String = dimensions.getAttribute("LayerSize")
            val tileSize: String = dimensions.getAttribute("TileSize")
            val layerSizeParts: Array<String?> = layerSize.split(" x ").toTypedArray()
            val layerSizeX = layerSizeParts[0]!!.toInt()
            val layerSizeY = layerSizeParts[1]!!.toInt()
            val tileSizeParts: Array<String?> = tileSize.split(" x ").toTypedArray()
            val tileSizeX = tileSizeParts[0]!!.toInt()
            val tileSizeY = tileSizeParts[1]!!.toInt()
            val layer = TiledMapTileLayer(layerSizeX, layerSizeY, tileSizeX, tileSizeY)
            layer.setName(id)
            layer.setVisible(visible.equals("True", ignoreCase = true))
            val tileArray: Element = element.getChildByName("TileArray")
            val rows: Array<Element?> = tileArray.getChildrenByName("Row")
            val tilesets: TiledMapTileSets = map.getTileSets()
            var currentTileSet: TiledMapTileSet? = null
            var firstgid = 0
            var x: Int
            var y: Int
            var row = 0
            val rowCount = rows.size
            while (row < rowCount) {
                val currentRow: Element? = rows[row]
                y = rowCount - 1 - row
                x = 0
                var child = 0
                val childCount: Int = currentRow.getChildCount()
                while (child < childCount) {
                    val currentChild: Element = currentRow.getChild(child)
                    val name: String = currentChild.getName()
                    if (name == "TileSheet") {
                        currentTileSet = tilesets.getTileSet(currentChild.getAttribute("Ref"))
                        firstgid = currentTileSet!!.getProperties()!!.get("firstgid", Int::class.java)!!
                    } else if (name == "Null") {
                        x += currentChild.getIntAttribute("Count")
                    } else if (name == "Static") {
                        val cell = Cell()
                        cell.setTile(currentTileSet!!.getTile(firstgid + currentChild.getIntAttribute("Index")))
                        layer!!.setCell(x++, y, cell)
                    } else if (name == "Animated") {
                        // Create an AnimatedTile
                        val interval: Int = currentChild.getInt("Interval")
                        val frames: Element = currentChild.getChildByName("Frames")
                        val frameTiles: Array<StaticTiledMapTile?> = Array<StaticTiledMapTile?>()
                        var frameChild = 0
                        val frameChildCount: Int = frames.getChildCount()
                        while (frameChild < frameChildCount) {
                            val frame: Element = frames.getChild(frameChild)
                            val frameName: String = frame.getName()
                            if (frameName == "TileSheet") {
                                currentTileSet = tilesets.getTileSet(frame.getAttribute("Ref"))
                                firstgid = currentTileSet!!.getProperties()!!.get("firstgid", Int::class.java)!!
                            } else if (frameName == "Static") {
                                frameTiles.add(currentTileSet!!.getTile(firstgid + frame.getIntAttribute("Index")) as StaticTiledMapTile)
                            }
                            frameChild++
                        }
                        val cell = Cell()
                        cell.setTile(AnimatedTiledMapTile(interval / 1000f, frameTiles))
                        layer!!.setCell(x++, y, cell) // TODO: Reuse existing animated tiles
                    }
                    child++
                }
                row++
            }
            val properties: Element = element.getChildByName("Properties")
            if (properties != null) {
                loadProperties(layer.getProperties(), properties)
            }
            map.getLayers().add(layer)
        }
    }

    private fun loadProperties(properties: MapProperties?, element: Element?) {
        if (element.getName().equals("Properties")) {
            for (property in element.getChildrenByName("Property")) {
                val key: String = property.getAttribute("Key", null)
                val type: String = property.getAttribute("Type", null)
                val value: String = property.getText()
                if (type == "Int32") {
                    properties!!.put(key, value.toInt())
                } else if (type == "String") {
                    properties!!.put(key, value)
                } else if (type == "Boolean") {
                    properties!!.put(key, value.equals("true", ignoreCase = true))
                } else {
                    properties!!.put(key, value)
                }
            }
        }
    }

    companion object {
        private fun getRelativeFileHandle(file: FileHandle?, path: String?): FileHandle? {
            val tokenizer = StringTokenizer(path, "\\/")
            var result: FileHandle = file.parent()
            while (tokenizer.hasMoreElements()) {
                val token: String = tokenizer.nextToken()
                result = if (token == "..") result.parent() else {
                    result.child(token)
                }
            }
            return result
        }
    }
}
