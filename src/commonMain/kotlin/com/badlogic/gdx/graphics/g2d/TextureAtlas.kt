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
package com.badlogic.gdx.graphics.g2d

import Mesh.VertexDataType
import com.badlogic.gdx.Files.FileType
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Pixmap.Format
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.Texture.TextureFilter
import com.badlogic.gdx.graphics.Texture.TextureWrap
import com.badlogic.gdx.graphics.Texture.TextureWrap.*
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.SpriteCache
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion
import com.badlogic.gdx.graphics.g2d.TextureAtlas.TextureAtlasData.Page
import com.badlogic.gdx.graphics.g2d.TextureAtlas.TextureAtlasData.Region
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.GdxRuntimeException
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.ObjectSet
import com.badlogic.gdx.utils.Sort
import com.badlogic.gdx.utils.StreamUtils
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.lang.IllegalStateException
import java.nio.FloatBuffer
import kotlin.jvm.Throws

/** Loads images from texture atlases created by TexturePacker.<br></br>
 * <br></br>
 * A TextureAtlas must be disposed to free up the resources consumed by the backing textures.
 * @author Nathan Sweet
 */
class TextureAtlas : Disposable {

    private val textures: ObjectSet<Texture> = ObjectSet(4)

    /** Returns all regions in the atlas.  */
    val regions: Array<AtlasRegion> = Array()

    class TextureAtlasData(packFile: FileHandle, imagesDir: FileHandle, flip: Boolean) {
        class Page(val textureFile: FileHandle, val width: Float, val height: Float, val useMipMaps: Boolean, format: Format, minFilter: TextureFilter,
                   magFilter: TextureFilter, uWrap: TextureWrap, vWrap: TextureWrap) {

            var texture: Texture? = null
            val format: Format
            val minFilter: TextureFilter
            val magFilter: TextureFilter
            val uWrap: TextureWrap
            val vWrap: TextureWrap

            init {
                this.format = format
                this.minFilter = minFilter
                this.magFilter = magFilter
                this.uWrap = uWrap
                this.vWrap = vWrap
            }
        }

        class Region {
            var page: Page? = null
            var index = 0
            var name: String? = null
            var offsetX = 0f
            var offsetY = 0f
            var originalWidth = 0
            var originalHeight = 0
            var rotate = false
            var degrees = 0
            var left = 0
            var top = 0
            var width = 0
            var height = 0
            var flip = false
            var splits: IntArray
            var pads: IntArray
        }

        val pages: Array<Page> = Array()
        val regions: Array<Region> = Array()

        init {
            val reader = BufferedReader(InputStreamReader(packFile.read()), 64)
            try {
                var pageImage: Page? = null
                while (true) {
                    val line: String = reader.readLine() ?: break
                    if (line.trim { it <= ' ' }.length == 0) pageImage = null else if (pageImage == null) {
                        val file = imagesDir.child(line)
                        var width = 0f
                        var height = 0f
                        if (readTuple(reader) == 2) { // size is only optional for an atlas packed with an old TexturePacker.
                            width = tuple[0]!!.toInt().toFloat()
                            height = tuple[1]!!.toInt().toFloat()
                            readTuple(reader)
                        }
                        val format: Format = Format.valueOf(tuple[0])
                        readTuple(reader)
                        val min: TextureFilter = TextureFilter.valueOf(tuple[0])
                        val max: TextureFilter = TextureFilter.valueOf(tuple[1])
                        val direction = readValue(reader)
                        var repeatX: TextureWrap = ClampToEdge
                        var repeatY: TextureWrap = ClampToEdge
                        if (direction == "x") repeatX = Repeat else if (direction == "y") repeatY = Repeat else if (direction == "xy") {
                            repeatX = Repeat
                            repeatY = Repeat
                        }
                        pageImage = Page(file, width, height, min.isMipMap(), format, min, max, repeatX, repeatY)
                        pages.add(pageImage)
                    } else {
                        val rotateValue = readValue(reader)
                        var degrees: Int
                        degrees = if (rotateValue.equals("true", ignoreCase = true)) 90 else if (rotateValue.equals("false", ignoreCase = true)) 0 else java.lang.Integer.valueOf(rotateValue)
                        readTuple(reader)
                        val left = tuple[0]!!.toInt()
                        val top = tuple[1]!!.toInt()
                        readTuple(reader)
                        val width = tuple[0]!!.toInt()
                        val height = tuple[1]!!.toInt()
                        val region = Region()
                        region.page = pageImage
                        region.left = left
                        region.top = top
                        region.width = width
                        region.height = height
                        region.name = line
                        region.rotate = degrees == 90
                        region.degrees = degrees
                        if (readTuple(reader) == 4) { // split is optional
                            region.splits = intArrayOf(tuple[0]!!.toInt(), tuple[1]!!.toInt(), tuple[2]!!.toInt(), tuple[3]!!.toInt())
                            if (readTuple(reader) == 4) { // pad is optional, but only present with splits
                                region.pads = intArrayOf(tuple[0]!!.toInt(), tuple[1]!!.toInt(), tuple[2]!!.toInt(), tuple[3]!!.toInt())
                                readTuple(reader)
                            }
                        }
                        region.originalWidth = tuple[0]!!.toInt()
                        region.originalHeight = tuple[1]!!.toInt()
                        readTuple(reader)
                        region.offsetX = tuple[0]!!.toInt().toFloat()
                        region.offsetY = tuple[1]!!.toInt().toFloat()
                        region.index = readValue(reader).toInt()
                        if (flip) region.flip = true
                        regions.add(region)
                    }
                }
            } catch (ex: java.lang.Exception) {
                throw GdxRuntimeException("Error reading pack file: $packFile", ex)
            } finally {
                StreamUtils.closeQuietly(reader)
            }
            regions.sort(indexComparator)
        }
    }

    /** Creates an empty atlas to which regions can be added.  */
    constructor() {}

    /** Loads the specified pack file using [FileType.Internal], using the parent directory of the pack file to find the page
     * images.  */
    constructor(internalPackFile: String?) : this(Gdx.files.internal(internalPackFile)) {}

    /** @param flip If true, all regions loaded will be flipped for use with a perspective where 0,0 is the upper left corner.
     * @see .TextureAtlas
     */
    constructor(packFile: FileHandle, flip: Boolean) : this(packFile, packFile.parent(), flip) {}
    /** @param flip If true, all regions loaded will be flipped for use with a perspective where 0,0 is the upper left corner.
     */
    /** Loads the specified pack file, using the parent directory of the pack file to find the page images.  */
    @JvmOverloads
    constructor(packFile: FileHandle, imagesDir: FileHandle = packFile.parent(), flip: Boolean = false) : this(TextureAtlasData(packFile, imagesDir, flip)) {
    }

    /** @param data May be null.
     */
    constructor(data: TextureAtlasData?) {
        data?.let { load(it) }
    }

    private fun load(data: TextureAtlasData) {
        val pageToTexture: ObjectMap<Page, Texture> = ObjectMap<Page, Texture>()
        for (page in data.pages) {
            var texture: Texture? = null
            if (page.texture == null) {
                texture = Texture(page.textureFile, page.format, page.useMipMaps)
                texture.setFilter(page.minFilter, page.magFilter)
                texture.setWrap(page.uWrap, page.vWrap)
            } else {
                texture = page.texture
                texture.setFilter(page.minFilter, page.magFilter)
                texture.setWrap(page.uWrap, page.vWrap)
            }
            textures.add(texture)
            pageToTexture.put(page, texture)
        }
        for (region in data.regions) {
            val width = region.width
            val height = region.height
            val atlasRegion = AtlasRegion(pageToTexture.get(region.page), region.left, region.top,
                if (region.rotate) height else width, if (region.rotate) width else height)
            atlasRegion.index = region.index
            atlasRegion.name = region.name
            atlasRegion.offsetX = region.offsetX
            atlasRegion.offsetY = region.offsetY
            atlasRegion.originalHeight = region.originalHeight
            atlasRegion.originalWidth = region.originalWidth
            atlasRegion.rotate = region.rotate
            atlasRegion.degrees = region.degrees
            atlasRegion.splits = region.splits
            atlasRegion.pads = region.pads
            if (region.flip) atlasRegion.flip(false, true)
            regions.add(atlasRegion)
        }
    }

    /** Adds a region to the atlas. The specified texture will be disposed when the atlas is disposed.  */
    fun addRegion(name: String?, texture: Texture?, x: Int, y: Int, width: Int, height: Int): AtlasRegion {
        textures.add(texture)
        val region = AtlasRegion(texture, x, y, width, height)
        region.name = name
        region.index = -1
        regions.add(region)
        return region
    }

    /** Adds a region to the atlas. The texture for the specified region will be disposed when the atlas is disposed.  */
    fun addRegion(name: String?, textureRegion: TextureRegion): AtlasRegion {
        textures.add(textureRegion.texture)
        val region = AtlasRegion(textureRegion)
        region.name = name
        region.index = -1
        regions.add(region)
        return region
    }

    /** Returns the first region found with the specified name. This method uses string comparison to find the region, so the result
     * should be cached rather than calling this method multiple times.
     * @return The region, or null.
     */
    fun findRegion(name: String?): AtlasRegion? {
        var i = 0
        val n = regions.size
        while (i < n) {
            if (regions[i].name.equals(name)) return regions[i]
            i++
        }
        return null
    }

    /** Returns the first region found with the specified name and index. This method uses string comparison to find the region, so
     * the result should be cached rather than calling this method multiple times.
     * @return The region, or null.
     */
    fun findRegion(name: String, index: Int): AtlasRegion? {
        var i = 0
        val n = regions.size
        while (i < n) {
            val region = regions[i]
            if (region.name != name) {
                i++
                continue
            }
            if (region.index != index) {
                i++
                continue
            }
            return region
            i++
        }
        return null
    }

    /** Returns all regions with the specified name, ordered by smallest to largest [index][AtlasRegion.index]. This method
     * uses string comparison to find the regions, so the result should be cached rather than calling this method multiple times.  */
    fun findRegions(name: String): Array<AtlasRegion> {
        val matched: Array<AtlasRegion> = Array(AtlasRegion::class.java)
        var i = 0
        val n = regions.size
        while (i < n) {
            val region = regions[i]
            if (region.name == name) matched.add(AtlasRegion(region))
            i++
        }
        return matched
    }

    /** Returns all regions in the atlas as sprites. This method creates a new sprite for each region, so the result should be
     * stored rather than calling this method multiple times.
     * @see .createSprite
     */
    fun createSprites(): Array<Sprite> {
        val sprites = Array(true, regions.size, Sprite::class.java)
        var i = 0
        val n = regions.size
        while (i < n) {
            sprites.add(newSprite(regions[i]))
            i++
        }
        return sprites
    }

    /** Returns the first region found with the specified name as a sprite. If whitespace was stripped from the region when it was
     * packed, the sprite is automatically positioned as if whitespace had not been stripped. This method uses string comparison to
     * find the region and constructs a new sprite, so the result should be cached rather than calling this method multiple times.
     * @return The sprite, or null.
     */
    fun createSprite(name: String?): Sprite? {
        var i = 0
        val n = regions.size
        while (i < n) {
            if (regions[i].name.equals(name)) return newSprite(regions[i])
            i++
        }
        return null
    }

    /** Returns the first region found with the specified name and index as a sprite. This method uses string comparison to find the
     * region and constructs a new sprite, so the result should be cached rather than calling this method multiple times.
     * @return The sprite, or null.
     * @see .createSprite
     */
    fun createSprite(name: String, index: Int): Sprite? {
        var i = 0
        val n = regions.size
        while (i < n) {
            val region = regions[i]
            if (region.name != name) {
                i++
                continue
            }
            if (region.index != index) {
                i++
                continue
            }
            return newSprite(regions[i])
            i++
        }
        return null
    }

    /** Returns all regions with the specified name as sprites, ordered by smallest to largest [index][AtlasRegion.index]. This
     * method uses string comparison to find the regions and constructs new sprites, so the result should be cached rather than
     * calling this method multiple times.
     * @see .createSprite
     */
    fun createSprites(name: String): Array<Sprite> {
        val matched: Array<Sprite> = Array(Sprite::class.java)
        var i = 0
        val n = regions.size
        while (i < n) {
            val region = regions[i]
            if (region.name == name) matched.add(newSprite(region))
            i++
        }
        return matched
    }

    private fun newSprite(region: AtlasRegion): Sprite {
        if (region.packedWidth == region.originalWidth && region.packedHeight == region.originalHeight) {
            if (region.rotate) {
                val sprite = Sprite(region)
                sprite.setBounds(0, 0, region.getRegionHeight(), region.getRegionWidth())
                sprite.rotate90(true)
                return sprite
            }
            return Sprite(region)
        }
        return AtlasSprite(region)
    }

    /** Returns the first region found with the specified name as a [NinePatch]. The region must have been packed with
     * ninepatch splits. This method uses string comparison to find the region and constructs a new ninepatch, so the result should
     * be cached rather than calling this method multiple times.
     * @return The ninepatch, or null.
     */
    fun createPatch(name: String): NinePatch? {
        var i = 0
        val n = regions.size
        while (i < n) {
            val region = regions[i]
            if (region.name == name) {
                val splits = region.splits
                    ?: throw IllegalArgumentException("Region does not have ninepatch splits: $name")
                val patch = NinePatch(region, splits[0], splits[1], splits[2], splits[3])
                if (region.pads != null) patch.setPadding(region.pads!![0], region.pads!![1], region.pads!![2], region.pads!![3])
                return patch
            }
            i++
        }
        return null
    }

    /** @return the textures of the pages, unordered
     */
    fun getTextures(): ObjectSet<Texture> {
        return textures
    }

    /** Releases all resources associated with this TextureAtlas instance. This releases all the textures backing all TextureRegions
     * and Sprites, which should no longer be used after calling dispose.  */
    fun dispose() {
        for (texture in textures) texture.dispose()
        textures.clear(0)
    }

    /** Describes the region of a packed image and provides information about the original image before it was packed.  */
    class AtlasRegion : TextureRegion {

        /** The number at the end of the original image file name, or -1 if none.<br></br>
         * <br></br>
         * When sprites are packed, if the original file name ends with a number, it is stored as the index and is not considered as
         * part of the sprite's name. This is useful for keeping animation frames in order.
         * @see TextureAtlas.findRegions
         */
        var index = 0

        /** The name of the original image file, up to the first underscore. Underscores denote special instructions to the texture
         * packer.  */
        var name: String? = null

        /** The offset from the left of the original image to the left of the packed image, after whitespace was removed for packing.  */
        var offsetX = 0f

        /** The offset from the bottom of the original image to the bottom of the packed image, after whitespace was removed for
         * packing.  */
        var offsetY = 0f

        /** The width of the image, after whitespace was removed for packing.  */
        var packedWidth: Int

        /** The height of the image, after whitespace was removed for packing.  */
        var packedHeight: Int

        /** The width of the image, before whitespace was removed and rotation was applied for packing.  */
        var originalWidth: Int

        /** The height of the image, before whitespace was removed for packing.  */
        var originalHeight: Int

        /** If true, the region has been rotated 90 degrees counter clockwise.  */
        var rotate = false

        /** The degrees the region has been rotated, counter clockwise between 0 and 359. Most atlas region handling deals only with
         * 0 or 90 degree rotation (enough to handle rectangles). More advanced texture packing may support other rotations (eg, for
         * tightly packing polygons).  */
        var degrees = 0

        /** The ninepatch splits, or null if not a ninepatch. Has 4 elements: left, right, top, bottom.  */
        var splits: IntArray

        /** The ninepatch pads, or null if not a ninepatch or the has no padding. Has 4 elements: left, right, top, bottom.  */
        var pads: IntArray?

        constructor(texture: Texture?, x: Int, y: Int, width: Int, height: Int) : super(texture, x, y, width, height) {
            originalWidth = width
            originalHeight = height
            packedWidth = width
            packedHeight = height
        }

        constructor(region: AtlasRegion) {
            setRegion(region)
            index = region.index
            name = region.name
            offsetX = region.offsetX
            offsetY = region.offsetY
            packedWidth = region.packedWidth
            packedHeight = region.packedHeight
            originalWidth = region.originalWidth
            originalHeight = region.originalHeight
            rotate = region.rotate
            degrees = region.degrees
            splits = region.splits
        }

        constructor(region: TextureRegion) {
            setRegion(region)
            packedWidth = region.getRegionWidth()
            packedHeight = region.getRegionHeight()
            originalWidth = packedWidth
            originalHeight = packedHeight
        }

        /** Flips the region, adjusting the offset so the image appears to be flip as if no whitespace has been removed for packing.  */
        override fun flip(x: Boolean, y: Boolean) {
            super.flip(x, y)
            if (x) offsetX = originalWidth - offsetX - rotatedPackedWidth
            if (y) offsetY = originalHeight - offsetY - rotatedPackedHeight
        }

        /** Returns the packed width considering the [.rotate] value, if it is true then it returns the packedHeight,
         * otherwise it returns the packedWidth.  */
        val rotatedPackedWidth: Float
            get() = (if (rotate) packedHeight else packedWidth).toFloat()

        /** Returns the packed height considering the [.rotate] value, if it is true then it returns the packedWidth,
         * otherwise it returns the packedHeight.  */
        val rotatedPackedHeight: Float
            get() = (if (rotate) packedWidth else packedHeight).toFloat()

        override fun toString(): String {
            return name!!
        }
    }

    /** A sprite that, if whitespace was stripped from the region when it was packed, is automatically positioned as if whitespace
     * had not been stripped.  */
    class AtlasSprite : Sprite {

        val atlasRegion: AtlasRegion
        var originalOffsetX: Float
        var originalOffsetY: Float

        constructor(region: AtlasRegion) {
            atlasRegion = AtlasRegion(region)
            originalOffsetX = region.offsetX
            originalOffsetY = region.offsetY
            setRegion(region)
            setOrigin(region.originalWidth / 2f, region.originalHeight / 2f)
            val width: Int = region.getRegionWidth()
            val height: Int = region.getRegionHeight()
            if (region.rotate) {
                super.rotate90(true)
                super.setBounds(region.offsetX, region.offsetY, height.toFloat(), width.toFloat())
            } else super.setBounds(region.offsetX, region.offsetY, width.toFloat(), height.toFloat())
            setColor(1, 1, 1, 1)
        }

        constructor(sprite: AtlasSprite) {
            atlasRegion = sprite.atlasRegion
            originalOffsetX = sprite.originalOffsetX
            originalOffsetY = sprite.originalOffsetY
            set(sprite)
        }

        override fun setPosition(x: Float, y: Float) {
            super.setPosition(x + atlasRegion.offsetX, y + atlasRegion.offsetY)
        }

        override fun setX(x: Float) {
            super.setX(x + atlasRegion.offsetX)
        }

        override fun setY(y: Float) {
            super.setY(y + atlasRegion.offsetY)
        }

        override fun setBounds(x: Float, y: Float, width: Float, height: Float) {
            val widthRatio = width / atlasRegion.originalWidth
            val heightRatio = height / atlasRegion.originalHeight
            atlasRegion.offsetX = originalOffsetX * widthRatio
            atlasRegion.offsetY = originalOffsetY * heightRatio
            val packedWidth = if (atlasRegion.rotate) atlasRegion.packedHeight else atlasRegion.packedWidth
            val packedHeight = if (atlasRegion.rotate) atlasRegion.packedWidth else atlasRegion.packedHeight
            super.setBounds(x + atlasRegion.offsetX, y + atlasRegion.offsetY, packedWidth * widthRatio, packedHeight * heightRatio)
        }

        override fun setSize(width: Float, height: Float) {
            setBounds(getX(), getY(), width, height)
        }

        override fun setOrigin(originX: Float, originY: Float) {
            super.setOrigin(originX - atlasRegion.offsetX, originY - atlasRegion.offsetY)
        }

        override fun setOriginCenter() {
            super.setOrigin(width / 2 - atlasRegion.offsetX, height / 2 - atlasRegion.offsetY)
        }

        override fun flip(x: Boolean, y: Boolean) {
            // Flip texture.
            if (atlasRegion.rotate) super.flip(y, x) else super.flip(x, y)
            val oldOriginX = originX
            val oldOriginY = originY
            val oldOffsetX = atlasRegion.offsetX
            val oldOffsetY = atlasRegion.offsetY
            val widthRatio = widthRatio
            val heightRatio = heightRatio
            atlasRegion.offsetX = originalOffsetX
            atlasRegion.offsetY = originalOffsetY
            atlasRegion.flip(x, y) // Updates x and y offsets.
            originalOffsetX = atlasRegion.offsetX
            originalOffsetY = atlasRegion.offsetY
            atlasRegion.offsetX *= widthRatio
            atlasRegion.offsetY *= heightRatio

            // Update position and origin with new offsets.
            translate(atlasRegion.offsetX - oldOffsetX, atlasRegion.offsetY - oldOffsetY)
            setOrigin(oldOriginX, oldOriginY)
        }

        override fun rotate90(clockwise: Boolean) {
            // Rotate texture.
            super.rotate90(clockwise)
            val oldOriginX = originX
            val oldOriginY = originY
            val oldOffsetX = atlasRegion.offsetX
            val oldOffsetY = atlasRegion.offsetY
            val widthRatio = widthRatio
            val heightRatio = heightRatio
            if (clockwise) {
                atlasRegion.offsetX = oldOffsetY
                atlasRegion.offsetY = atlasRegion.originalHeight * heightRatio - oldOffsetX - atlasRegion.packedWidth * widthRatio
            } else {
                atlasRegion.offsetX = atlasRegion.originalWidth * widthRatio - oldOffsetY - atlasRegion.packedHeight * heightRatio
                atlasRegion.offsetY = oldOffsetX
            }

            // Update position and origin with new offsets.
            translate(atlasRegion.offsetX - oldOffsetX, atlasRegion.offsetY - oldOffsetY)
            setOrigin(oldOriginX, oldOriginY)
        }

        override fun getX(): Float {
            return super.getX() - atlasRegion.offsetX
        }

        override fun getY(): Float {
            return super.getY() - atlasRegion.offsetY
        }

        override var originX: Float
            get() = super.getOriginX() + atlasRegion.offsetX
            set(originX) {
                super.originX = originX
            }

        override var originY: Float
            get() = super.getOriginY() + atlasRegion.offsetY
            set(originY) {
                super.originY = originY
            }

        override var width: Float
            get() = super.getWidth() / atlasRegion.rotatedPackedWidth * atlasRegion.originalWidth
            set(width) {
                super.width = width
            }

        override var height: Float
            get() = super.getHeight() / atlasRegion.rotatedPackedHeight * atlasRegion.originalHeight
            set(height) {
                super.height = height
            }

        val widthRatio: Float
            get() = super.getWidth() / atlasRegion.rotatedPackedWidth

        val heightRatio: Float
            get() = super.getHeight() / atlasRegion.rotatedPackedHeight

        override fun toString(): String {
            return atlasRegion.toString()
        }
    }

    companion object {
        val tuple = arrayOfNulls<String>(4)
        val indexComparator: java.util.Comparator<Region> = object : java.util.Comparator<Region?>() {
            override fun compare(region1: Region, region2: Region): Int {
                var i1 = region1.index
                if (i1 == -1) i1 = Int.MAX_VALUE
                var i2 = region2.index
                if (i2 == -1) i2 = Int.MAX_VALUE
                return i1 - i2
            }
        }

        @Throws(IOException::class)
        fun readValue(reader: BufferedReader): String {
            val line: String = reader.readLine()
            val colon = line.indexOf(':')
            if (colon == -1) throw GdxRuntimeException("Invalid line: $line")
            return line.substring(colon + 1).trim { it <= ' ' }
        }

        /** Returns the number of tuple values read (1, 2 or 4).  */
        @Throws(IOException::class)
        fun readTuple(reader: BufferedReader): Int {
            val line: String = reader.readLine()
            val colon = line.indexOf(':')
            if (colon == -1) throw GdxRuntimeException("Invalid line: $line")
            var i = 0
            var lastMatch = colon + 1
            i = 0
            while (i < 3) {
                val comma = line.indexOf(',', lastMatch)
                if (comma == -1) break
                tuple[i] = line.substring(lastMatch, comma).trim { it <= ' ' }
                lastMatch = comma + 1
                i++
            }
            tuple[i] = line.substring(lastMatch).trim { it <= ' ' }
            return i + 1
        }
    }
}
