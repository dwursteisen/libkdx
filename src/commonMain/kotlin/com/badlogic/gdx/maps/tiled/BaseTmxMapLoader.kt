package com.badlogic.gdx.maps.tiled

import Texture.TextureFilter
import com.badlogic.gdx.maps.ImageResolver
import com.badlogic.gdx.maps.MapLayer
import com.badlogic.gdx.maps.MapProperties
import com.badlogic.gdx.maps.tiled.AtlasTmxMapLoader.AtlasResolver
import com.badlogic.gdx.maps.tiled.AtlasTmxMapLoader.AtlasResolver.AssetManagerAtlasResolver
import com.badlogic.gdx.maps.tiled.AtlasTmxMapLoader.AtlasResolver.DirectAtlasResolver
import com.badlogic.gdx.maps.tiled.AtlasTmxMapLoader.AtlasTiledMapLoaderParameters
import com.badlogic.gdx.maps.tiled.TideMapLoader
import com.badlogic.gdx.maps.tiled.renderers.BatchTiledMapRenderer
import com.badlogic.gdx.maps.tiled.renderers.OrthoCachedTiledMapRenderer
import com.badlogic.gdx.maps.tiled.tiles.AnimatedTiledMapTile
import com.badlogic.gdx.maps.tiled.tiles.StaticTiledMapTile
import kotlin.jvm.Throws

abstract class BaseTmxMapLoader<P : BaseTmxMapLoader.Parameters?>(resolver: FileHandleResolver?) : AsynchronousAssetLoader<TiledMap?, P?>(resolver) {
    class Parameters : AssetLoaderParameters<TiledMap?>() {
        /**
         * generate mipmaps?
         */
        var generateMipMaps = false

        /**
         * The TextureFilter to use for minification
         */
        var textureMinFilter: TextureFilter? = TextureFilter.Nearest

        /**
         * The TextureFilter to use for magnification
         */
        var textureMagFilter: TextureFilter? = TextureFilter.Nearest

        /**
         * Whether to convert the objects' pixel position and size to the equivalent in tile space.
         */
        var convertObjectToTileSpace = false

        /**
         * Whether to flip all Y coordinates so that Y positive is up. All LibGDX renderers require flipped Y coordinates, and
         * thus flipY set to true. This parameter is included for non-rendering related purposes of TMX files, or custom renderers.
         */
        var flipY = true
    }

    protected var xml: XmlReader? = XmlReader()
    protected var root: Element? = null
    protected var convertObjectToTileSpace = false
    protected var flipY = true
    protected var mapTileWidth = 0
    protected var mapTileHeight = 0
    protected var mapWidthInPixels = 0
    protected var mapHeightInPixels = 0
    protected var map: TiledMap? = null
    fun getDependencies(fileName: String?, tmxFile: FileHandle?, parameter: P?): Array<AssetDescriptor?>? {
        root = xml.parse(tmxFile)
        val textureParameter: TextureLoader.TextureParameter = TextureParameter()
        if (parameter != null) {
            textureParameter.genMipMaps = parameter.generateMipMaps
            textureParameter.minFilter = parameter.textureMinFilter
            textureParameter.magFilter = parameter.textureMagFilter
        }
        return getDependencyAssetDescriptors(tmxFile, textureParameter)
    }

    protected abstract fun getDependencyAssetDescriptors(tmxFile: FileHandle?, textureParameter: TextureLoader.TextureParameter?): Array<AssetDescriptor?>?

    /**
     * Loads the map data, given the XML root element
     *
     * @param tmxFile       the Filehandle of the tmx file
     * @param parameter
     * @param imageResolver
     * @return the [TiledMap]
     */
    protected fun loadTiledMap(tmxFile: FileHandle?, parameter: P?, imageResolver: ImageResolver?): TiledMap? {
        map = TiledMap()
        if (parameter != null) {
            convertObjectToTileSpace = parameter.convertObjectToTileSpace
            flipY = parameter.flipY
        } else {
            convertObjectToTileSpace = false
            flipY = true
        }
        val mapOrientation: String = root.getAttribute("orientation", null)
        val mapWidth: Int = root.getIntAttribute("width", 0)
        val mapHeight: Int = root.getIntAttribute("height", 0)
        val tileWidth: Int = root.getIntAttribute("tilewidth", 0)
        val tileHeight: Int = root.getIntAttribute("tileheight", 0)
        val hexSideLength: Int = root.getIntAttribute("hexsidelength", 0)
        val staggerAxis: String = root.getAttribute("staggeraxis", null)
        val staggerIndex: String = root.getAttribute("staggerindex", null)
        val mapBackgroundColor: String = root.getAttribute("backgroundcolor", null)
        val mapProperties: MapProperties = map.getProperties()
        if (mapOrientation != null) {
            mapProperties!!.put("orientation", mapOrientation)
        }
        mapProperties!!.put("width", mapWidth)
        mapProperties!!.put("height", mapHeight)
        mapProperties!!.put("tilewidth", tileWidth)
        mapProperties!!.put("tileheight", tileHeight)
        mapProperties!!.put("hexsidelength", hexSideLength)
        if (staggerAxis != null) {
            mapProperties!!.put("staggeraxis", staggerAxis)
        }
        if (staggerIndex != null) {
            mapProperties!!.put("staggerindex", staggerIndex)
        }
        if (mapBackgroundColor != null) {
            mapProperties!!.put("backgroundcolor", mapBackgroundColor)
        }
        mapTileWidth = tileWidth
        mapTileHeight = tileHeight
        mapWidthInPixels = mapWidth * tileWidth
        mapHeightInPixels = mapHeight * tileHeight
        if (mapOrientation != null) {
            if ("staggered" == mapOrientation) {
                if (mapHeight > 1) {
                    mapWidthInPixels += tileWidth / 2
                    mapHeightInPixels = mapHeightInPixels / 2 + tileHeight / 2
                }
            }
        }
        val properties: Element = root.getChildByName("properties")
        if (properties != null) {
            loadProperties(map.getProperties(), properties)
        }
        val tilesets: Array<Element?> = root.getChildrenByName("tileset")
        for (element in tilesets) {
            loadTileSet(element, tmxFile, imageResolver)
            root.removeChild(element)
        }
        var i = 0
        val j: Int = root.getChildCount()
        while (i < j) {
            val element: Element = root.getChild(i)
            loadLayer(map, map.getLayers(), element, tmxFile, imageResolver)
            i++
        }
        return map
    }

    protected fun loadLayer(map: TiledMap?, parentLayers: MapLayers?, element: Element?, tmxFile: FileHandle?, imageResolver: ImageResolver?) {
        val name: String = element.getName()
        if (name == "group") {
            loadLayerGroup(map, parentLayers, element, tmxFile, imageResolver)
        } else if (name == "layer") {
            loadTileLayer(map, parentLayers, element)
        } else if (name == "objectgroup") {
            loadObjectGroup(map, parentLayers, element)
        } else if (name == "imagelayer") {
            loadImageLayer(map, parentLayers, element, tmxFile, imageResolver)
        }
    }

    protected fun loadLayerGroup(map: TiledMap?, parentLayers: MapLayers?, element: Element?, tmxFile: FileHandle?, imageResolver: ImageResolver?) {
        if (element.getName().equals("group")) {
            val groupLayer = MapGroupLayer()
            loadBasicLayerInfo(groupLayer, element)
            val properties: Element = element.getChildByName("properties")
            if (properties != null) {
                loadProperties(groupLayer.getProperties(), properties)
            }
            var i = 0
            val j: Int = element.getChildCount()
            while (i < j) {
                val child: Element = element.getChild(i)
                loadLayer(map, groupLayer.getLayers(), child, tmxFile, imageResolver)
                i++
            }
            for (layer in groupLayer.getLayers()) {
                layer!!.setParent(groupLayer)
            }
            parentLayers.add(groupLayer)
        }
    }

    protected fun loadTileLayer(map: TiledMap?, parentLayers: MapLayers?, element: Element?) {
        if (element.getName().equals("layer")) {
            val width: Int = element.getIntAttribute("width", 0)
            val height: Int = element.getIntAttribute("height", 0)
            val tileWidth: Int = map.getProperties().get("tilewidth", Int::class.java)
            val tileHeight: Int = map.getProperties().get("tileheight", Int::class.java)
            val layer = TiledMapTileLayer(width, height, tileWidth, tileHeight)
            loadBasicLayerInfo(layer, element)
            val ids = getTileIds(element, width, height)
            val tilesets: TiledMapTileSets = map.getTileSets()
            for (y in 0 until height) {
                for (x in 0 until width) {
                    val id = ids!![y * width + x]
                    val flipHorizontally = id and FLAG_FLIP_HORIZONTALLY != 0
                    val flipVertically = id and FLAG_FLIP_VERTICALLY != 0
                    val flipDiagonally = id and FLAG_FLIP_DIAGONALLY != 0
                    val tile: TiledMapTile = tilesets!!.getTile(id and MASK_CLEAR.inv())
                    if (tile != null) {
                        val cell: Cell? = createTileLayerCell(flipHorizontally, flipVertically, flipDiagonally)
                        cell.setTile(tile)
                        layer!!.setCell(x, if (flipY) height - 1 - y else y, cell)
                    }
                }
            }
            val properties: Element = element.getChildByName("properties")
            if (properties != null) {
                loadProperties(layer.getProperties(), properties)
            }
            parentLayers.add(layer)
        }
    }

    protected fun loadObjectGroup(map: TiledMap?, parentLayers: MapLayers?, element: Element?) {
        if (element.getName().equals("objectgroup")) {
            val layer = MapLayer()
            loadBasicLayerInfo(layer, element)
            val properties: Element = element.getChildByName("properties")
            if (properties != null) {
                loadProperties(layer.getProperties(), properties)
            }
            for (objectElement in element.getChildrenByName("object")) {
                loadObject(map, layer, objectElement)
            }
            parentLayers.add(layer)
        }
    }

    protected fun loadImageLayer(map: TiledMap?, parentLayers: MapLayers?, element: Element?, tmxFile: FileHandle?, imageResolver: ImageResolver?) {
        if (element.getName().equals("imagelayer")) {
            var x = 0f
            var y = 0f
            x = if (element.hasAttribute("offsetx")) {
                element.getAttribute("offsetx", "0").toFloat()
            } else {
                element.getAttribute("x", "0").toFloat()
            }
            y = if (element.hasAttribute("offsety")) {
                element.getAttribute("offsety", "0").toFloat()
            } else {
                element.getAttribute("y", "0").toFloat()
            }
            if (flipY) y = mapHeightInPixels - y
            var texture: TextureRegion? = null
            val image: Element = element.getChildByName("image")
            if (image != null) {
                val source: String = image.getAttribute("source")
                val handle: FileHandle? = getRelativeFileHandle(tmxFile, source)
                texture = imageResolver!!.getImage(handle.path())
                y -= texture.getRegionHeight()
            }
            val layer = TiledMapImageLayer(texture, x, y)
            loadBasicLayerInfo(layer, element)
            val properties: Element = element.getChildByName("properties")
            if (properties != null) {
                loadProperties(layer.getProperties(), properties)
            }
            parentLayers.add(layer)
        }
    }

    protected fun loadBasicLayerInfo(layer: MapLayer?, element: Element?) {
        val name: String = element.getAttribute("name", null)
        val opacity: Float = element.getAttribute("opacity", "1.0").toFloat()
        val visible = element.getIntAttribute("visible", 1) === 1
        val offsetX: Float = element.getFloatAttribute("offsetx", 0)
        val offsetY: Float = element.getFloatAttribute("offsety", 0)
        layer.setName(name)
        layer.setOpacity(opacity)
        layer.setVisible(visible)
        layer!!.setOffsetX(offsetX)
        layer!!.setOffsetY(offsetY)
    }

    protected fun loadObject(map: TiledMap?, layer: MapLayer?, element: Element?) {
        loadObject(map, layer.getObjects(), element, mapHeightInPixels.toFloat())
    }

    protected fun loadObject(map: TiledMap?, tile: TiledMapTile?, element: Element?) {
        loadObject(map, tile.getObjects(), element, tile!!.getTextureRegion().getRegionHeight())
    }

    protected fun loadObject(map: TiledMap?, objects: MapObjects?, element: Element?, heightInPixels: Float) {
        if (element.getName().equals("object")) {
            var `object`: MapObject? = null
            val scaleX = if (convertObjectToTileSpace) 1.0f / mapTileWidth else 1.0f
            val scaleY = if (convertObjectToTileSpace) 1.0f / mapTileHeight else 1.0f
            val x: Float = element.getFloatAttribute("x", 0) * scaleX
            val y: Float = (if (flipY) heightInPixels - element.getFloatAttribute("y", 0) else element.getFloatAttribute("y", 0)) * scaleY
            val width: Float = element.getFloatAttribute("width", 0) * scaleX
            val height: Float = element.getFloatAttribute("height", 0) * scaleY
            if (element.getChildCount() > 0) {
                var child: Element? = null
                if (element.getChildByName("polygon").also({ child = it }) != null) {
                    val points: Array<String?> = child.getAttribute("points").split(" ")
                    val vertices = FloatArray(points.size * 2)
                    for (i in points.indices) {
                        val point: Array<String?> = points[i]!!.split(",").toTypedArray()
                        vertices[i * 2] = point[0]!!.toFloat() * scaleX
                        vertices[i * 2 + 1] = point[1]!!.toFloat() * scaleY * if (flipY) -1 else 1
                    }
                    val polygon = Polygon(vertices)
                    polygon.setPosition(x, y)
                    `object` = PolygonMapObject(polygon)
                } else if (element.getChildByName("polyline").also({ child = it }) != null) {
                    val points: Array<String?> = child.getAttribute("points").split(" ")
                    val vertices = FloatArray(points.size * 2)
                    for (i in points.indices) {
                        val point: Array<String?> = points[i]!!.split(",").toTypedArray()
                        vertices[i * 2] = point[0]!!.toFloat() * scaleX
                        vertices[i * 2 + 1] = point[1]!!.toFloat() * scaleY * if (flipY) -1 else 1
                    }
                    val polyline = Polyline(vertices)
                    polyline.setPosition(x, y)
                    `object` = PolylineMapObject(polyline)
                } else if (element.getChildByName("ellipse").also({ child = it }) != null) {
                    `object` = EllipseMapObject(x, if (flipY) y - height else y, width, height)
                }
            }
            if (`object` == null) {
                var gid: String? = null
                if (element.getAttribute("gid", null).also({ gid = it }) != null) {
                    val id = gid!!.toLong().toInt()
                    val flipHorizontally = id and FLAG_FLIP_HORIZONTALLY != 0
                    val flipVertically = id and FLAG_FLIP_VERTICALLY != 0
                    val tile: TiledMapTile = map.getTileSets().getTile(id and MASK_CLEAR.inv())
                    val tiledMapTileMapObject = TiledMapTileMapObject(tile, flipHorizontally, flipVertically)
                    val textureRegion: TextureRegion = tiledMapTileMapObject.getTextureRegion()
                    tiledMapTileMapObject.getProperties().put("gid", id)
                    tiledMapTileMapObject.setX(x)
                    tiledMapTileMapObject.setY(if (flipY) y else y - height)
                    val objectWidth: Float = element.getFloatAttribute("width", textureRegion.getRegionWidth())
                    val objectHeight: Float = element.getFloatAttribute("height", textureRegion.getRegionHeight())
                    tiledMapTileMapObject.setScaleX(scaleX * (objectWidth / textureRegion.getRegionWidth()))
                    tiledMapTileMapObject.setScaleY(scaleY * (objectHeight / textureRegion.getRegionHeight()))
                    tiledMapTileMapObject.setRotation(element.getFloatAttribute("rotation", 0))
                    `object` = tiledMapTileMapObject
                } else {
                    `object` = RectangleMapObject(x, if (flipY) y - height else y, width, height)
                }
            }
            `object`.setName(element.getAttribute("name", null))
            val rotation: String = element.getAttribute("rotation", null)
            if (rotation != null) {
                `object`.getProperties().put("rotation", rotation.toFloat())
            }
            val type: String = element.getAttribute("type", null)
            if (type != null) {
                `object`.getProperties().put("type", type)
            }
            val id: Int = element.getIntAttribute("id", 0)
            if (id != 0) {
                `object`.getProperties().put("id", id)
            }
            `object`.getProperties().put("x", x)
            if (`object` is TiledMapTileMapObject) {
                `object`.getProperties().put("y", y)
            } else {
                `object`.getProperties().put("y", if (flipY) y - height else y)
            }
            `object`.getProperties().put("width", width)
            `object`.getProperties().put("height", height)
            `object`.setVisible(element.getIntAttribute("visible", 1) === 1)
            val properties: Element = element.getChildByName("properties")
            if (properties != null) {
                loadProperties(`object`.getProperties(), properties)
            }
            objects.add(`object`)
        }
    }

    protected fun loadProperties(properties: MapProperties?, element: Element?) {
        if (element == null) return
        if (element.getName().equals("properties")) {
            for (property in element.getChildrenByName("property")) {
                val name: String = property.getAttribute("name", null)
                var value: String = property.getAttribute("value", null)
                val type: String = property.getAttribute("type", null)
                if (value == null) {
                    value = property.getText()
                }
                val castValue = castProperty(name, value, type)
                properties!!.put(name, castValue)
            }
        }
    }

    private fun castProperty(name: String?, value: String?, type: String?): Any? {
        return if (type == null) {
            value
        } else if (type == "int") {
            java.lang.Integer.valueOf(value)
        } else if (type == "float") {
            java.lang.Float.valueOf(value)
        } else if (type == "bool") {
            java.lang.Boolean.valueOf(value)
        } else if (type == "color") {
            // Tiled uses the format #AARRGGBB
            val opaqueColor = value!!.substring(3)
            val alpha = value.substring(1, 3)
            Color.valueOf(opaqueColor + alpha)
        } else {
            throw GdxRuntimeException("Wrong type given for property " + name + ", given : " + type
                + ", supported : string, bool, int, float, color")
        }
    }

    protected fun createTileLayerCell(flipHorizontally: Boolean, flipVertically: Boolean, flipDiagonally: Boolean): Cell? {
        val cell = Cell()
        if (flipDiagonally) {
            if (flipHorizontally && flipVertically) {
                cell.setFlipHorizontally(true)
                cell.setRotation(Cell.ROTATE_270)
            } else if (flipHorizontally) {
                cell.setRotation(Cell.ROTATE_270)
            } else if (flipVertically) {
                cell.setRotation(Cell.ROTATE_90)
            } else {
                cell.setFlipVertically(true)
                cell.setRotation(Cell.ROTATE_270)
            }
        } else {
            cell.setFlipHorizontally(flipHorizontally)
            cell.setFlipVertically(flipVertically)
        }
        return cell
    }

    protected fun loadTileSet(element: Element?, tmxFile: FileHandle?, imageResolver: ImageResolver?) {
        var element: Element? = element
        if (element.getName().equals("tileset")) {
            val firstgid: Int = element.getIntAttribute("firstgid", 1)
            var imageSource = ""
            var imageWidth = 0
            var imageHeight = 0
            var image: FileHandle? = null
            val source: String = element.getAttribute("source", null)
            if (source != null) {
                val tsx: FileHandle? = getRelativeFileHandle(tmxFile, source)
                try {
                    element = xml.parse(tsx)
                    val imageElement: Element = element.getChildByName("image")
                    if (imageElement != null) {
                        imageSource = imageElement.getAttribute("source")
                        imageWidth = imageElement.getIntAttribute("width", 0)
                        imageHeight = imageElement.getIntAttribute("height", 0)
                        image = getRelativeFileHandle(tsx, imageSource)
                    }
                } catch (e: SerializationException) {
                    throw GdxRuntimeException("Error parsing external tileset.")
                }
            } else {
                val imageElement: Element = element.getChildByName("image")
                if (imageElement != null) {
                    imageSource = imageElement.getAttribute("source")
                    imageWidth = imageElement.getIntAttribute("width", 0)
                    imageHeight = imageElement.getIntAttribute("height", 0)
                    image = getRelativeFileHandle(tmxFile, imageSource)
                }
            }
            val name: String = element.get("name", null)
            val tilewidth: Int = element.getIntAttribute("tilewidth", 0)
            val tileheight: Int = element.getIntAttribute("tileheight", 0)
            val spacing: Int = element.getIntAttribute("spacing", 0)
            val margin: Int = element.getIntAttribute("margin", 0)
            val offset: Element = element.getChildByName("tileoffset")
            var offsetX = 0
            var offsetY = 0
            if (offset != null) {
                offsetX = offset.getIntAttribute("x", 0)
                offsetY = offset.getIntAttribute("y", 0)
            }
            val tileSet = TiledMapTileSet()

            // TileSet
            tileSet.setName(name)
            val tileSetProperties: MapProperties = tileSet!!.getProperties()
            val properties: Element = element.getChildByName("properties")
            if (properties != null) {
                loadProperties(tileSetProperties, properties)
            }
            tileSetProperties!!.put("firstgid", firstgid)

            // Tiles
            val tileElements: Array<Element?> = element.getChildrenByName("tile")
            addStaticTiles(tmxFile, imageResolver, tileSet, element, tileElements, name, firstgid, tilewidth, tileheight, spacing,
                margin, source, offsetX, offsetY, imageSource, imageWidth, imageHeight, image)
            val animatedTiles: Array<AnimatedTiledMapTile?> = Array<AnimatedTiledMapTile?>()
            for (tileElement in tileElements) {
                val localtid: Int = tileElement.getIntAttribute("id", 0)
                var tile: TiledMapTile? = tileSet!!.getTile(firstgid + localtid)
                if (tile != null) {
                    val animatedTile: AnimatedTiledMapTile? = createAnimatedTile(tileSet, tile, tileElement, firstgid)
                    if (animatedTile != null) {
                        animatedTiles.add(animatedTile)
                        tile = animatedTile
                    }
                    addTileProperties(tile, tileElement)
                    addTileObjectGroup(tile, tileElement)
                }
            }

            // replace original static tiles by animated tiles
            for (animatedTile in animatedTiles) {
                tileSet!!.putTile(animatedTile.getId(), animatedTile)
            }
            map.getTileSets().addTileSet(tileSet)
        }
    }

    protected abstract fun addStaticTiles(tmxFile: FileHandle?, imageResolver: ImageResolver?, tileset: TiledMapTileSet?,
                                          element: Element?, tileElements: Array<Element?>?, name: String?, firstgid: Int, tilewidth: Int, tileheight: Int, spacing: Int,
                                          margin: Int, source: String?, offsetX: Int, offsetY: Int, imageSource: String?, imageWidth: Int, imageHeight: Int, image: FileHandle?)

    protected fun addTileProperties(tile: TiledMapTile?, tileElement: Element?) {
        val terrain: String = tileElement.getAttribute("terrain", null)
        if (terrain != null) {
            tile.getProperties().put("terrain", terrain)
        }
        val probability: String = tileElement.getAttribute("probability", null)
        if (probability != null) {
            tile.getProperties().put("probability", probability)
        }
        val properties: Element = tileElement.getChildByName("properties")
        if (properties != null) {
            loadProperties(tile.getProperties(), properties)
        }
    }

    protected fun addTileObjectGroup(tile: TiledMapTile?, tileElement: Element?) {
        val objectgroupElement: Element = tileElement.getChildByName("objectgroup")
        if (objectgroupElement != null) {
            for (objectElement in objectgroupElement.getChildrenByName("object")) {
                loadObject(map, tile, objectElement)
            }
        }
    }

    protected fun createAnimatedTile(tileSet: TiledMapTileSet?, tile: TiledMapTile?, tileElement: Element?,
                                     firstgid: Int): AnimatedTiledMapTile? {
        val animationElement: Element = tileElement.getChildByName("animation")
        if (animationElement != null) {
            val staticTiles: Array<StaticTiledMapTile?> = Array<StaticTiledMapTile?>()
            val intervals = IntArray()
            for (frameElement in animationElement.getChildrenByName("frame")) {
                staticTiles.add(tileSet!!.getTile(firstgid + frameElement.getIntAttribute("tileid")) as StaticTiledMapTile)
                intervals.add(frameElement.getIntAttribute("duration"))
            }
            val animatedTile = AnimatedTiledMapTile(intervals, staticTiles)
            animatedTile.setId(tile.getId())
            return animatedTile
        }
        return null
    }

    protected fun addStaticTiledMapTile(tileSet: TiledMapTileSet?, textureRegion: TextureRegion?, tileId: Int, offsetX: Float,
                                        offsetY: Float) {
        val tile: TiledMapTile = StaticTiledMapTile(textureRegion)
        tile.setId(tileId)
        tile.setOffsetX(offsetX)
        tile.setOffsetY(if (flipY) -offsetY else offsetY)
        tileSet!!.putTile(tileId, tile)
    }

    companion object {
        protected const val FLAG_FLIP_HORIZONTALLY = -0x80000000
        protected const val FLAG_FLIP_VERTICALLY = 0x40000000
        protected const val FLAG_FLIP_DIAGONALLY = 0x20000000
        protected const val MASK_CLEAR = -0x20000000
        fun getTileIds(element: Element?, width: Int, height: Int): IntArray? {
            val data: Element = element.getChildByName("data")
            val encoding: String = data.getAttribute("encoding", null)
                ?: // no 'encoding' attribute means that the encoding is XML
                throw GdxRuntimeException("Unsupported encoding (XML) for TMX Layer Data")
            val ids = IntArray(width * height)
            if (encoding == "csv") {
                val array: Array<String?> = data.getText().split(",")
                for (i in array.indices) ids[i] = array[i]!!.trim { it <= ' ' }.toLong().toInt()
            } else {
                if (true) if (encoding == "base64") {
                    var `is`: InputStream? = null
                    try {
                        val compression: String = data.getAttribute("compression", null)
                        val bytes: ByteArray = Base64Coder.decode(data.getText())
                        if (compression == null) `is` = ByteArrayInputStream(bytes) else if (compression == "gzip") `is` = BufferedInputStream(GZIPInputStream(ByteArrayInputStream(bytes), bytes.size)) else if (compression == "zlib") `is` = BufferedInputStream(InflaterInputStream(ByteArrayInputStream(bytes))) else throw GdxRuntimeException("Unrecognised compression ($compression) for TMX Layer Data")
                        val temp = ByteArray(4)
                        for (y in 0 until height) {
                            for (x in 0 until width) {
                                var read: Int = `is`.read(temp)
                                while (read < temp.size) {
                                    val curr: Int = `is`.read(temp, read, temp.size - read)
                                    if (curr == -1) break
                                    read += curr
                                }
                                if (read != temp.size) throw GdxRuntimeException("Error Reading TMX Layer Data: Premature end of tile data")
                                ids[y * width + x] = unsignedByteToInt(temp[0]) or (unsignedByteToInt(temp[1]) shl 8
                                    ) or (unsignedByteToInt(temp[2]) shl 16) or (unsignedByteToInt(temp[3]) shl 24)
                            }
                        }
                    } catch (e: IOException) {
                        throw GdxRuntimeException("Error Reading TMX Layer Data - IOException: " + e.getMessage())
                    } finally {
                        StreamUtils.closeQuietly(`is`)
                    }
                } else {
                    // any other value of 'encoding' is one we're not aware of, probably a feature of a future version of Tiled
                    // or another editor
                    throw GdxRuntimeException("Unrecognised encoding ($encoding) for TMX Layer Data")
                }
            }
            return ids
        }

        protected fun unsignedByteToInt(b: Byte): Int {
            return b and 0xFF
        }

        protected fun getRelativeFileHandle(file: FileHandle?, path: String?): FileHandle? {
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
