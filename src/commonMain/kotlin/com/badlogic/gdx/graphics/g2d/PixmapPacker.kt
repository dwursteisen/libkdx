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

import PixmapPacker.PixmapPackerRectangle
import com.badlogic.gdx.graphics.g2d.ParticleEmitter
import com.badlogic.gdx.graphics.g2d.ParticleEmitter.IndependentScaledNumericValue
import com.badlogic.gdx.graphics.g2d.ParticleEmitter.SpawnEllipseSide
import com.badlogic.gdx.graphics.g2d.ParticleEmitter.SpawnShape
import com.badlogic.gdx.graphics.g2d.ParticleEmitter.SpriteMode
import com.badlogic.gdx.graphics.g2d.PixmapPacker.PackStrategy
import com.badlogic.gdx.graphics.g2d.PixmapPacker.Page
import com.badlogic.gdx.graphics.g2d.PixmapPackerIO.ImageFormat
import com.badlogic.gdx.graphics.g2d.PixmapPackerIO.SaveParameters
import java.lang.RuntimeException
import kotlin.jvm.Throws

/**
 * Packs [pixmaps][Pixmap] into one or more [pages][Page] to generate an atlas of pixmap instances. Provides means to
 * directly convert the pixmap atlas to a [TextureAtlas]. The packer supports padding and border pixel duplication,
 * specified during construction. The packer supports incremental inserts and updates of TextureAtlases generated with this class.
 * How bin packing is performed can be customized via [PackStrategy].
 *
 *
 * All methods can be called from any thread unless otherwise noted.
 *
 *
 * One-off usage:
 *
 * <pre>
 * // 512x512 pixel pages, RGB565 format, 2 pixels of padding, border duplication
 * PixmapPacker packer = new PixmapPacker(512, 512, Format.RGB565, 2, true);
 * packer.pack(&quot;First Pixmap&quot;, pixmap1);
 * packer.pack(&quot;Second Pixmap&quot;, pixmap2);
 * TextureAtlas atlas = packer.generateTextureAtlas(TextureFilter.Nearest, TextureFilter.Nearest, false);
 * packer.dispose();
 * // ...
 * atlas.dispose();
</pre> *
 *
 *
 * With this usage pattern, disposing the packer will not dispose any pixmaps used by the texture atlas. The texture atlas must
 * also be disposed when no longer needed.
 *
 *
 * Incremental texture atlas usage:
 *
 * <pre>
 * // 512x512 pixel pages, RGB565 format, 2 pixels of padding, no border duplication
 * PixmapPacker packer = new PixmapPacker(512, 512, Format.RGB565, 2, false);
 * TextureAtlas atlas = new TextureAtlas();
 *
 * // potentially on a separate thread, e.g. downloading thumbnails
 * packer.pack(&quot;thumbnail&quot;, thumbnail);
 *
 * // on the rendering thread, every frame
 * packer.updateTextureAtlas(atlas, TextureFilter.Linear, TextureFilter.Linear, false);
 *
 * // once the atlas is no longer needed, make sure you get the final additions. This might
 * // be more elaborate depending on your threading model.
 * packer.updateTextureAtlas(atlas, TextureFilter.Linear, TextureFilter.Linear, false);
 * // ...
 * atlas.dispose();
</pre> *
 *
 *
 * Pixmap-only usage:
 *
 * <pre>
 * PixmapPacker packer = new PixmapPacker(512, 512, Format.RGB565, 2, true);
 * packer.pack(&quot;First Pixmap&quot;, pixmap1);
 * packer.pack(&quot;Second Pixmap&quot;, pixmap2);
 *
 * // do something interesting with the resulting pages
 * for (Page page : packer.getPages()) {
 * // ...
 * }
 *
 * packer.dispose();
</pre> *
 *
 * @author mzechner
 * @author Nathan Sweet
 * @author Rob Rendell
 */
class PixmapPacker(var pageWidth: Int, var pageHeight: Int, pageFormat: Format?, padding: Int, duplicateBorder: Boolean, stripWhitespaceX: Boolean,
                   stripWhitespaceY: Boolean, packStrategy: PackStrategy?) : Disposable {

    /**
     * If true, when a pixmap is packed to a page that has a texture, the portion of the texture where the pixmap was packed is
     * updated using glTexSubImage2D. Note if packing many pixmaps, this may be slower than reuploading the whole texture. This
     * setting is ignored if [.getDuplicateBorder] is true.
     */
    var packToTexture = false
    var disposed = false
    var pageFormat: Format?
    var padding: Int
    var duplicateBorder: Boolean
    var stripWhitespaceX: Boolean
    var stripWhitespaceY: Boolean
    var alphaThreshold = 0
    var transparentColor: Color? = Color(0f, 0f, 0f, 0f)

    /**
     * @return the [Page] instances created so far. If multiple threads are accessing the packer, iterating over the pages
     * must be done only after synchronizing on the packer.
     */
    val pages: Array<Page?>? = Array()
    var packStrategy: PackStrategy?

    /**
     * Uses [GuillotineStrategy].
     *
     * @see PixmapPacker.PixmapPacker
     */
    constructor(pageWidth: Int, pageHeight: Int, pageFormat: Format?, padding: Int, duplicateBorder: Boolean) : this(pageWidth, pageHeight, pageFormat, padding, duplicateBorder, false, false, GuillotineStrategy()) {}

    /**
     * Uses [GuillotineStrategy].
     *
     * @see PixmapPacker.PixmapPacker
     */
    constructor(pageWidth: Int, pageHeight: Int, pageFormat: Format?, padding: Int, duplicateBorder: Boolean, packStrategy: PackStrategy?) : this(pageWidth, pageHeight, pageFormat, padding, duplicateBorder, false, false, packStrategy) {}

    /**
     * Sorts the images to the optimzal order they should be packed. Some packing strategies rely heavily on the images being
     * sorted.
     */
    fun sort(images: Array<Pixmap?>?) {
        packStrategy!!.sort(images)
    }

    /**
     * Inserts the pixmap without a name. It cannot be looked up by name.
     *
     * @see .pack
     */
    @Synchronized
    fun pack(image: Pixmap?): Rectangle? {
        return pack(null, image)
    }

    /**
     * Inserts the pixmap. If name was not null, you can later retrieve the image's position in the output image via
     * [.getRect].
     *
     * @param name If null, the image cannot be looked up by name.
     * @return Rectangle describing the area the pixmap was rendered to.
     * @throws GdxRuntimeException in case the image did not fit due to the page size being too small or providing a duplicate
     * name.
     */
    @Synchronized
    fun pack(name: String?, image: Pixmap?): Rectangle? {
        var name = name
        var image: Pixmap? = image
        if (disposed) return null
        if (name != null && getRect(name) != null) throw GdxRuntimeException("Pixmap has already been packed with name: $name")
        val isPatch = name != null && name.endsWith(".9")
        val rect: PixmapPackerRectangle
        var pixmapToDispose: Pixmap? = null
        if (isPatch) {
            rect = PixmapPackerRectangle(0, 0, image.getWidth() - 2, image.getHeight() - 2)
            pixmapToDispose = Pixmap(image.getWidth() - 2, image.getHeight() - 2, image.getFormat())
            pixmapToDispose.setBlending(Blending.None)
            rect.splits = getSplits(image)
            rect.pads = getPads(image, rect.splits)
            pixmapToDispose.drawPixmap(image, 0, 0, 1, 1, image.getWidth() - 1, image.getHeight() - 1)
            image = pixmapToDispose
            name = name!!.split("\\.").toTypedArray()[0]
        } else {
            if (stripWhitespaceX || stripWhitespaceY) {
                val originalWidth: Int = image.getWidth()
                val originalHeight: Int = image.getHeight()
                //Strip whitespace, manipulate the pixmap and return corrected Rect
                var top = 0
                var bottom: Int = image.getHeight()
                if (stripWhitespaceY) {
                    outer@ for (y in 0 until image.getHeight()) {
                        for (x in 0 until image.getWidth()) {
                            val pixel: Int = image.getPixel(x, y)
                            val alpha = pixel and 0x000000ff
                            if (alpha > alphaThreshold) break@outer
                        }
                        top++
                    }
                    var y: Int = image.getHeight()
                    outer@ while (--y >= top) {
                        for (x in 0 until image.getWidth()) {
                            val pixel: Int = image.getPixel(x, y)
                            val alpha = pixel and 0x000000ff
                            if (alpha > alphaThreshold) break@outer
                        }
                        bottom--
                    }
                }
                var left = 0
                var right: Int = image.getWidth()
                if (stripWhitespaceX) {
                    outer@ for (x in 0 until image.getWidth()) {
                        for (y in top until bottom) {
                            val pixel: Int = image.getPixel(x, y)
                            val alpha = pixel and 0x000000ff
                            if (alpha > alphaThreshold) break@outer
                        }
                        left++
                    }
                    var x: Int = image.getWidth()
                    outer@ while (--x >= left) {
                        for (y in top until bottom) {
                            val pixel: Int = image.getPixel(x, y)
                            val alpha = pixel and 0x000000ff
                            if (alpha > alphaThreshold) break@outer
                        }
                        right--
                    }
                }
                val newWidth = right - left
                val newHeight = bottom - top
                pixmapToDispose = Pixmap(newWidth, newHeight, image.getFormat())
                pixmapToDispose.setBlending(Blending.None)
                pixmapToDispose.drawPixmap(image, 0, 0, left, top, newWidth, newHeight)
                image = pixmapToDispose
                rect = PixmapPackerRectangle(0, 0, newWidth, newHeight, left, top, originalWidth, originalHeight)
            } else {
                rect = PixmapPackerRectangle(0, 0, image.getWidth(), image.getHeight())
            }
        }
        if (rect.getWidth() > pageWidth || rect.getHeight() > pageHeight) {
            if (name == null) throw GdxRuntimeException("Page size too small for pixmap.")
            throw GdxRuntimeException("Page size too small for pixmap: $name")
        }
        val page = packStrategy!!.pack(this, name, rect)
        if (name != null) {
            page!!.rects.put(name, rect)
            page.addedRects.add(name)
        }
        val rectX = rect.x as Int
        val rectY = rect.y as Int
        val rectWidth = rect.width as Int
        val rectHeight = rect.height as Int
        if (packToTexture && !duplicateBorder && page!!.texture != null && !page.dirty) {
            page.texture.bind()
            Gdx.gl.glTexSubImage2D(page.texture.glTarget, 0, rectX, rectY, rectWidth, rectHeight, image.getGLFormat(),
                image.getGLType(), image.getPixels())
        } else page!!.dirty = true
        page.image.drawPixmap(image, rectX, rectY)
        if (duplicateBorder) {
            val imageWidth: Int = image.getWidth()
            val imageHeight: Int = image.getHeight()
            // Copy corner pixels to fill corners of the padding.
            page.image.drawPixmap(image, 0, 0, 1, 1, rectX - 1, rectY - 1, 1, 1)
            page.image.drawPixmap(image, imageWidth - 1, 0, 1, 1, rectX + rectWidth, rectY - 1, 1, 1)
            page.image.drawPixmap(image, 0, imageHeight - 1, 1, 1, rectX - 1, rectY + rectHeight, 1, 1)
            page.image.drawPixmap(image, imageWidth - 1, imageHeight - 1, 1, 1, rectX + rectWidth, rectY + rectHeight, 1, 1)
            // Copy edge pixels into padding.
            page.image.drawPixmap(image, 0, 0, imageWidth, 1, rectX, rectY - 1, rectWidth, 1)
            page.image.drawPixmap(image, 0, imageHeight - 1, imageWidth, 1, rectX, rectY + rectHeight, rectWidth, 1)
            page.image.drawPixmap(image, 0, 0, 1, imageHeight, rectX - 1, rectY, 1, rectHeight)
            page.image.drawPixmap(image, imageWidth - 1, 0, 1, imageHeight, rectX + rectWidth, rectY, 1, rectHeight)
        }
        if (pixmapToDispose != null) {
            pixmapToDispose.dispose()
        }
        return rect
    }

    /**
     * @param name the name of the image
     * @return the rectangle for the image in the page it's stored in or null
     */
    @Synchronized
    fun getRect(name: String?): Rectangle? {
        for (page in pages!!) {
            val rect: Rectangle = page!!.rects.get(name)
            if (rect != null) return rect
        }
        return null
    }

    /**
     * @param name the name of the image
     * @return the page the image is stored in or null
     */
    @Synchronized
    fun getPage(name: String?): Page? {
        for (page in pages!!) {
            val rect: Rectangle = page!!.rects.get(name)
            if (rect != null) return page
        }
        return null
    }

    /**
     * Returns the index of the page containing the given packed rectangle.
     *
     * @param name the name of the image
     * @return the index of the page the image is stored in or -1
     */
    @Synchronized
    fun getPageIndex(name: String?): Int {
        for (i in 0 until pages!!.size) {
            val rect: Rectangle = pages[i]!!.rects.get(name)
            if (rect != null) return i
        }
        return -1
    }

    /**
     * Disposes any pixmap pages which don't have a texture. Page pixmaps that have a texture will not be disposed until their
     * texture is disposed.
     */
    @Synchronized
    fun dispose() {
        for (page in pages!!) {
            if (page!!.texture == null) {
                page.image.dispose()
            }
        }
        disposed = true
    }

    /**
     * Generates a new [TextureAtlas] from the pixmaps inserted so far. After calling this method, disposing the packer will
     * no longer dispose the page pixmaps.
     */
    @Synchronized
    fun generateTextureAtlas(minFilter: TextureFilter?, magFilter: TextureFilter?, useMipMaps: Boolean): TextureAtlas? {
        val atlas = TextureAtlas()
        updateTextureAtlas(atlas, minFilter, magFilter, useMipMaps)
        return atlas
    }

    /**
     * Updates the [TextureAtlas], adding any new [Pixmap] instances packed since the last call to this method. This
     * can be used to insert Pixmap instances on a separate thread via [.pack] and update the TextureAtlas on
     * the rendering thread. This method must be called on the rendering thread. After calling this method, disposing the packer
     * will no longer dispose the page pixmaps. Has useIndexes on by default so as to keep backwards compatibility
     */
    @Synchronized
    fun updateTextureAtlas(atlas: TextureAtlas?, minFilter: TextureFilter?, magFilter: TextureFilter?,
                           useMipMaps: Boolean) {
        updateTextureAtlas(atlas, minFilter, magFilter, useMipMaps, true)
    }

    /**
     * Updates the [TextureAtlas], adding any new [Pixmap] instances packed since the last call to this method. This
     * can be used to insert Pixmap instances on a separate thread via [.pack] and update the TextureAtlas on
     * the rendering thread. This method must be called on the rendering thread. After calling this method, disposing the packer
     * will no longer dispose the page pixmaps.
     */
    @Synchronized
    fun updateTextureAtlas(atlas: TextureAtlas?, minFilter: TextureFilter?, magFilter: TextureFilter?,
                           useMipMaps: Boolean, useIndexes: Boolean) {
        updatePageTextures(minFilter, magFilter, useMipMaps)
        for (page in pages!!) {
            if (page!!.addedRects!!.size > 0) {
                for (name in page.addedRects!!) {
                    val rect: PixmapPackerRectangle = page.rects.get(name)
                    val region = TextureAtlas.AtlasRegion(page.texture, rect.x as Int, rect.y as Int, rect.width as Int, rect.height as Int)
                    if (rect.splits != null) {
                        region.splits = rect.splits!!
                        region.pads = rect.pads
                    }
                    var imageIndex = -1
                    var imageName = name
                    if (useIndexes) {
                        val matcher: Matcher = indexPattern.matcher(imageName)
                        if (matcher.matches()) {
                            imageName = matcher.group(1)
                            imageIndex = matcher.group(2).toInt()
                        }
                    }
                    region.name = imageName
                    region.index = imageIndex
                    region.offsetX = rect.offsetX.toFloat()
                    region.offsetY = ((rect.originalHeight - rect.height - rect.offsetY) as Int).toFloat()
                    region.originalWidth = rect.originalWidth
                    region.originalHeight = rect.originalHeight
                    atlas!!.regions.add(region)
                }
                page.addedRects.clear()
                atlas!!.getTextures().add(page.texture)
            }
        }
    }

    /**
     * Calls [updateTexture][Page.updateTexture] for each page and adds a region to
     * the specified array for each page texture.
     */
    @Synchronized
    fun updateTextureRegions(regions: Array<TextureRegion?>?, minFilter: TextureFilter?, magFilter: TextureFilter?,
                             useMipMaps: Boolean) {
        updatePageTextures(minFilter, magFilter, useMipMaps)
        while (regions!!.size < pages!!.size) regions.add(TextureRegion(pages[regions.size]!!.texture))
    }

    /**
     * Calls [updateTexture][Page.updateTexture] for each page.
     */
    @Synchronized
    fun updatePageTextures(minFilter: TextureFilter?, magFilter: TextureFilter?, useMipMaps: Boolean) {
        for (page in pages!!) page!!.updateTexture(minFilter, magFilter, useMipMaps)
    }

    fun getPageFormat(): Format? {
        return pageFormat
    }

    fun setPageFormat(pageFormat: Format?) {
        this.pageFormat = pageFormat
    }

    /**
     * @author mzechner
     * @author Nathan Sweet
     * @author Rob Rendell
     */
    class Page(packer: PixmapPacker?) {

        var rects: OrderedMap<String?, PixmapPackerRectangle?>? = OrderedMap()
        var image: Pixmap?
        var texture: Texture? = null
        val addedRects: Array<String?>? = Array()
        var dirty = false
        val pixmap: Pixmap?
            get() = image

        fun getRects(): OrderedMap<String?, PixmapPackerRectangle?>? {
            return rects
        }

        /**
         * Returns the texture for this page, or null if the texture has not been created.
         *
         * @see .updateTexture
         */
        fun getTexture(): Texture? {
            return texture
        }

        /**
         * Creates the texture if it has not been created, else reuploads the entire page pixmap to the texture if the pixmap has
         * changed since this method was last called.
         *
         * @return true if the texture was created or reuploaded.
         */
        fun updateTexture(minFilter: TextureFilter?, magFilter: TextureFilter?, useMipMaps: Boolean): Boolean {
            if (texture != null) {
                if (!dirty) return false
                texture.load(texture.getTextureData())
            } else {
                texture = object : Texture(PixmapTextureData(image, image.getFormat(), useMipMaps, false, true)) {
                    fun dispose() {
                        super.dispose()
                        image.dispose()
                    }
                }
                texture.setFilter(minFilter, magFilter)
            }
            dirty = false
            return true
        }

        /**
         * Creates a new page filled with the color provided by the [PixmapPacker.getTransparentColor]
         */
        init {
            image = Pixmap(packer!!.pageWidth, packer.pageHeight, packer.pageFormat)
            image.setBlending(Blending.None)
            image.setColor(packer!!.getTransparentColor())
            image.fill()
        }
    }

    /**
     * Choose the page and location for each rectangle.
     *
     * @author Nathan Sweet
     */
    interface PackStrategy {

        fun sort(images: Array<Pixmap?>?)

        /**
         * Returns the page the rectangle should be placed in and modifies the specified rectangle position.
         */
        fun pack(packer: PixmapPacker?, name: String?, rect: Rectangle?): Page?
    }

    /**
     * Does bin packing by inserting to the right or below previously packed rectangles. This is good at packing arbitrarily sized
     * images.
     *
     * @author mzechner
     * @author Nathan Sweet
     * @author Rob Rendell
     */
    class GuillotineStrategy : PackStrategy {

        var comparator: Comparator<Pixmap?>? = null
        override fun sort(pixmaps: Array<Pixmap?>?) {
            if (comparator == null) {
                comparator = object : Comparator<Pixmap?>() {
                    override fun compare(o1: Pixmap?, o2: Pixmap?): Int {
                        return java.lang.Math.max(o1.getWidth(), o1.getHeight()) - java.lang.Math.max(o2.getWidth(), o2.getHeight())
                    }
                }
            }
            pixmaps.sort(comparator)
        }

        override fun pack(packer: PixmapPacker?, name: String?, rect: Rectangle?): Page? {
            var page: GuillotinePage?
            if (packer!!.pages!!.size === 0) {
                // Add a page if empty.
                page = GuillotinePage(packer)
                packer!!.pages.add(page)
            } else {
                // Always try to pack into the last page.
                page = packer!!.pages.peek() as GuillotinePage?
            }
            val padding = packer.padding
            rect.width += padding
            rect.height += padding
            var node = insert(page!!.root, rect)
            if (node == null) {
                // Didn't fit, pack into a new page.
                page = GuillotinePage(packer)
                packer.pages.add(page)
                node = insert(page.root, rect)
            }
            node!!.full = true
            rect.set(node.rect.x, node.rect.y, node.rect.width - padding, node.rect.height - padding)
            return page
        }

        private fun insert(node: Node?, rect: Rectangle?): Node? {
            return if (!node!!.full && node.leftChild != null && node.rightChild != null) {
                var newNode = insert(node.leftChild, rect)
                if (newNode == null) newNode = insert(node.rightChild, rect)
                newNode
            } else {
                if (node.full) return null
                if (node.rect.width === rect.width && node.rect.height === rect.height) return node
                if (node.rect.width < rect.width || node.rect.height < rect.height) return null
                node.leftChild = Node()
                node.rightChild = Node()
                val deltaWidth = node.rect.width as Int - rect.width as Int
                val deltaHeight = node.rect.height as Int - rect.height as Int
                if (deltaWidth > deltaHeight) {
                    node.leftChild!!.rect.x = node.rect.x
                    node.leftChild!!.rect.y = node.rect.y
                    node.leftChild!!.rect.width = rect.width
                    node.leftChild!!.rect.height = node.rect.height
                    node.rightChild!!.rect.x = node.rect.x + rect.width
                    node.rightChild!!.rect.y = node.rect.y
                    node.rightChild!!.rect.width = node.rect.width - rect.width
                    node.rightChild!!.rect.height = node.rect.height
                } else {
                    node.leftChild!!.rect.x = node.rect.x
                    node.leftChild!!.rect.y = node.rect.y
                    node.leftChild!!.rect.width = node.rect.width
                    node.leftChild!!.rect.height = rect.height
                    node.rightChild!!.rect.x = node.rect.x
                    node.rightChild!!.rect.y = node.rect.y + rect.height
                    node.rightChild!!.rect.width = node.rect.width
                    node.rightChild!!.rect.height = node.rect.height - rect.height
                }
                insert(node.leftChild, rect)
            }
        }

        internal class Node {
            var leftChild: Node? = null
            var rightChild: Node? = null
            val rect: Rectangle? = Rectangle()
            var full = false
        }

        internal class GuillotinePage(packer: PixmapPacker?) : Page(packer) {
            var root: Node?

            init {
                root = Node()
                root!!.rect.x = packer!!.padding
                root!!.rect.y = packer.padding
                root!!.rect.width = packer.pageWidth - packer.padding * 2
                root!!.rect.height = packer.pageHeight - packer.padding * 2
            }
        }
    }

    /**
     * Does bin packing by inserting in rows. This is good at packing images that have similar heights.
     *
     * @author Nathan Sweet
     */
    class SkylineStrategy : PackStrategy {

        var comparator: Comparator<Pixmap?>? = null
        override fun sort(images: Array<Pixmap?>?) {
            if (comparator == null) {
                comparator = object : Comparator<Pixmap?>() {
                    override fun compare(o1: Pixmap?, o2: Pixmap?): Int {
                        return o1.getHeight() - o2.getHeight()
                    }
                }
            }
            images.sort(comparator)
        }

        override fun pack(packer: PixmapPacker?, name: String?, rect: Rectangle?): Page? {
            val padding = packer!!.padding
            val pageWidth = packer.pageWidth - padding * 2
            val pageHeight = packer.pageHeight - padding * 2
            val rectWidth = rect.width as Int + padding
            val rectHeight = rect.height as Int + padding
            var i = 0
            val n = packer.pages!!.size
            while (i < n) {
                val page = packer.pages!![i] as SkylinePage?
                var bestRow: Row? = null
                // Fit in any row before the last.
                var ii = 0
                val nn = page!!.rows!!.size - 1
                while (ii < nn) {
                    val row: Row? = page.rows!![ii]
                    if (row.x + rectWidth >= pageWidth) {
                        ii++
                        continue
                    }
                    if (row.y + rectHeight >= pageHeight) {
                        ii++
                        continue
                    }
                    if (rectHeight > row.height) {
                        ii++
                        continue
                    }
                    if (bestRow == null || row.height < bestRow.height) bestRow = row
                    ii++
                }
                if (bestRow == null) {
                    // Fit in last row, increasing height.
                    val row: Row = page.rows.peek()
                    if (row.y + rectHeight >= pageHeight) {
                        i++
                        continue
                    }
                    if (row.x + rectWidth < pageWidth) {
                        row.height = java.lang.Math.max(row.height, rectHeight)
                        bestRow = row
                    } else if (row.y + row.height + rectHeight < pageHeight) {
                        // Fit in new row.
                        bestRow = Row()
                        bestRow.y = row.y + row.height
                        bestRow.height = rectHeight
                        page.rows.add(bestRow)
                    }
                }
                if (bestRow != null) {
                    rect.x = bestRow.x
                    rect.y = bestRow.y
                    bestRow.x += rectWidth
                    return page
                }
                i++
            }
            // Fit in new page.
            val page = SkylinePage(packer)
            packer.pages.add(page)
            val row = Row()
            row.x = padding + rectWidth
            row.y = padding
            row.height = rectHeight
            page.rows.add(row)
            rect.x = padding
            rect.y = padding
            return page
        }

        internal class SkylinePage(packer: PixmapPacker?) : Page(packer) {
            var rows: Array<Row?>? = Array()

            internal class Row {
                var x = 0
                var y = 0
                var height = 0
            }
        }
    }

    /**
     * @see PixmapPacker.setTransparentColor
     */
    fun getTransparentColor(): Color? {
        return transparentColor
    }

    /**
     * Sets the default `color` of the whole [PixmapPacker.Page] when a new one created. Helps to avoid texture
     * bleeding or to highlight the page for debugging.
     *
     * @see Page.Page
     */
    fun setTransparentColor(color: Color?) {
        transparentColor.set(color)
    }

    private fun getSplits(raster: Pixmap?): IntArray? {
        var startX = getSplitPoint(raster, 1, 0, true, true)
        var endX = getSplitPoint(raster, startX, 0, false, true)
        var startY = getSplitPoint(raster, 0, 1, true, false)
        var endY = getSplitPoint(raster, 0, startY, false, false)

        // Ensure pixels after the end are not invalid.
        getSplitPoint(raster, endX + 1, 0, true, true)
        getSplitPoint(raster, 0, endY + 1, true, false)

        // No splits, or all splits.
        if (startX == 0 && endX == 0 && startY == 0 && endY == 0) return null

        // Subtraction here is because the coordinates were computed before the 1px border was stripped.
        endX = if (startX != 0) {
            startX--
            raster.getWidth() - 2 - (endX - 1)
        } else {
            // If no start point was ever found, we assume full stretch.
            raster.getWidth() - 2
        }
        endY = if (startY != 0) {
            startY--
            raster.getHeight() - 2 - (endY - 1)
        } else {
            // If no start point was ever found, we assume full stretch.
            raster.getHeight() - 2
        }
        return intArrayOf(startX, endX, startY, endY)
    }

    private fun getPads(raster: Pixmap?, splits: IntArray?): IntArray? {
        val bottom: Int = raster.getHeight() - 1
        val right: Int = raster.getWidth() - 1
        var startX = getSplitPoint(raster, 1, bottom, true, true)
        var startY = getSplitPoint(raster, right, 1, true, false)

        // No need to hunt for the end if a start was never found.
        var endX = 0
        var endY = 0
        if (startX != 0) endX = getSplitPoint(raster, startX + 1, bottom, false, true)
        if (startY != 0) endY = getSplitPoint(raster, right, startY + 1, false, false)

        // Ensure pixels after the end are not invalid.
        getSplitPoint(raster, endX + 1, bottom, true, true)
        getSplitPoint(raster, right, endY + 1, true, false)

        // No pads.
        if (startX == 0 && endX == 0 && startY == 0 && endY == 0) {
            return null
        }

        // -2 here is because the coordinates were computed before the 1px border was stripped.
        if (startX == 0 && endX == 0) {
            startX = -1
            endX = -1
        } else {
            endX = if (startX > 0) {
                startX--
                raster.getWidth() - 2 - (endX - 1)
            } else {
                // If no start point was ever found, we assume full stretch.
                raster.getWidth() - 2
            }
        }
        if (startY == 0 && endY == 0) {
            startY = -1
            endY = -1
        } else {
            endY = if (startY > 0) {
                startY--
                raster.getHeight() - 2 - (endY - 1)
            } else {
                // If no start point was ever found, we assume full stretch.
                raster.getHeight() - 2
            }
        }
        val pads = intArrayOf(startX, endX, startY, endY)
        return if (splits != null && Arrays.equals(pads, splits)) {
            null
        } else pads
    }

    private val c: Color? = Color()
    private fun getSplitPoint(raster: Pixmap?, startX: Int, startY: Int, startPoint: Boolean, xAxis: Boolean): Int {
        val rgba = IntArray(4)
        var next = if (xAxis) startX else startY
        val end: Int = if (xAxis) raster.getWidth() else raster.getHeight()
        val breakA = if (startPoint) 255 else 0
        var x = startX
        var y = startY
        while (next != end) {
            if (xAxis) x = next else y = next
            val colint: Int = raster.getPixel(x, y)
            c.set(colint)
            rgba[0] = (c.r * 255) as Int
            rgba[1] = (c.g * 255) as Int
            rgba[2] = (c.b * 255) as Int
            rgba[3] = (c.a * 255) as Int
            if (rgba[3] == breakA) return next
            if (!startPoint && (rgba[0] != 0 || rgba[1] != 0 || rgba[2] != 0 || rgba[3] != 255)) println("$x  $y $rgba ")
            next++
        }
        return 0
    }

    class PixmapPackerRectangle : Rectangle {
        var splits: IntArray?
        var pads: IntArray?
        var offsetX: Int
        var offsetY: Int
        var originalWidth: Int
        var originalHeight: Int

        internal constructor(x: Int, y: Int, width: Int, height: Int) : super(x, y, width, height) {
            offsetX = 0
            offsetY = 0
            originalWidth = width
            originalHeight = height
        }

        internal constructor(x: Int, y: Int, width: Int, height: Int, left: Int, top: Int, originalWidth: Int, originalHeight: Int) : super(x, y, width, height) {
            offsetX = left
            offsetY = top
            this.originalWidth = originalWidth
            this.originalHeight = originalHeight
        }
    }

    companion object {
        var indexPattern: Pattern? = Pattern.compile("(.+)_(\\d+)$")
    }

    /**
     * Creates a new ImagePacker which will insert all supplied pixmaps into one or more `pageWidth` by
     * `pageHeight` pixmaps using the specified strategy.
     *
     * @param padding          the number of blank pixels to insert between pixmaps.
     * @param duplicateBorder  duplicate the border pixels of the inserted images to avoid seams when rendering with bi-linear
     * filtering on.
     * @param stripWhitespaceX strip whitespace in x axis
     * @param stripWhitespaceY strip whitespace in y axis
     */
    init {
        this.pageFormat = pageFormat
        this.padding = padding
        this.duplicateBorder = duplicateBorder
        this.stripWhitespaceX = stripWhitespaceX
        this.stripWhitespaceY = stripWhitespaceY
        this.packStrategy = packStrategy
    }
}
