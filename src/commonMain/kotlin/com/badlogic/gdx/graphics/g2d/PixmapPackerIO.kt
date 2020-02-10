package com.badlogic.gdx.graphics.g2d

import PixmapPacker.PixmapPackerRectangle
import com.badlogic.gdx.graphics.g2d.ParticleEmitter
import com.badlogic.gdx.graphics.g2d.ParticleEmitter.IndependentScaledNumericValue
import com.badlogic.gdx.graphics.g2d.ParticleEmitter.SpawnEllipseSide
import com.badlogic.gdx.graphics.g2d.ParticleEmitter.SpawnShape
import com.badlogic.gdx.graphics.g2d.ParticleEmitter.SpriteMode
import com.badlogic.gdx.graphics.g2d.PixmapPacker
import com.badlogic.gdx.graphics.g2d.PixmapPacker.GuillotineStrategy
import com.badlogic.gdx.graphics.g2d.PixmapPacker.GuillotineStrategy.GuillotinePage
import com.badlogic.gdx.graphics.g2d.PixmapPacker.PackStrategy
import com.badlogic.gdx.graphics.g2d.PixmapPacker.PixmapPackerRectangle
import com.badlogic.gdx.graphics.g2d.PixmapPacker.SkylineStrategy.SkylinePage
import java.lang.RuntimeException
import kotlin.jvm.Throws

/**
 * Saves PixmapPackers to files.
 *
 * @author jshapcott
 */
class PixmapPackerIO {

    /**
     * Image formats which can be used when saving a PixmapPacker.
     */
    enum class ImageFormat(val extension: String?) {

        /**
         * A simple compressed image format which is libgdx specific.
         */
        CIM(".cim"),

        /**
         * A standard compressed image format which is not libgdx specific.
         */
        PNG(".png");

        /**
         * Returns the file extension for the image format.
         */

    }

    /**
     * Additional parameters which will be used when writing a PixmapPacker.
     */
    class SaveParameters {

        var format: ImageFormat? = ImageFormat.PNG
        var minFilter: TextureFilter? = TextureFilter.Nearest
        var magFilter: TextureFilter? = TextureFilter.Nearest
        var useIndexes = false
    }

    /**
     * Saves the provided PixmapPacker to the provided file. The resulting file will use the standard TextureAtlas file format and
     * can be loaded by TextureAtlas as if it had been created using TexturePacker. Default [SaveParameters] will be used.
     *
     * @param file   the file to which the atlas descriptor will be written, images will be written as siblings
     * @param packer the PixmapPacker to be written
     * @throws IOException if the atlas file can not be written
     */
    @Throws(IOException::class)
    fun save(file: FileHandle?, packer: PixmapPacker?) {
        save(file, packer, SaveParameters())
    }

    /**
     * Saves the provided PixmapPacker to the provided file. The resulting file will use the standard TextureAtlas file format and
     * can be loaded by TextureAtlas as if it had been created using TexturePacker.
     *
     * @param file       the file to which the atlas descriptor will be written, images will be written as siblings
     * @param packer     the PixmapPacker to be written
     * @param parameters the SaveParameters specifying how to save the PixmapPacker
     * @throws IOException if the atlas file can not be written
     */
    @Throws(IOException::class)
    fun save(file: FileHandle?, packer: PixmapPacker?, parameters: SaveParameters?) {
        val writer: Writer = file.writer(false)
        var index = 0
        for (page in packer!!.pages) {
            if (page.rects.size > 0) {
                val pageFile: FileHandle = file.sibling(file.nameWithoutExtension().toString() + "_" + ++index + parameters!!.format!!.extension)
                when (parameters.format) {
                    ImageFormat.CIM -> {
                        PixmapIO.writeCIM(pageFile, page.image)
                    }
                    ImageFormat.PNG -> {
                        PixmapIO.writePNG(pageFile, page.image)
                    }
                }
                writer.write("\n")
                writer.write(pageFile.name().toString() + "\n")
                writer.write("""
    size: ${page.image.getWidth().toString()},${page.image.getHeight().toString()}
    
    """.trimIndent())
                writer.write("""
    format: ${packer!!.pageFormat.name().toString()}
    
    """.trimIndent())
                writer.write("""
    filter: ${parameters.minFilter.name().toString()},${parameters.magFilter.name().toString()}
    
    """.trimIndent())
                writer.write("""
    repeat: none
    
    """.trimIndent())
                for (name in page.rects.keys()) {
                    var imageIndex = -1
                    var imageName = name
                    if (parameters.useIndexes) {
                        val matcher: Matcher = indexPattern.matcher(imageName)
                        if (matcher.matches()) {
                            imageName = matcher.group(1)
                            imageIndex = matcher.group(2).toInt()
                        }
                    }
                    writer.write(imageName + "\n")
                    val rect: PixmapPackerRectangle = page.rects.get(name)
                    writer.write("""  rotate: false
""")
                    writer.write("""  xy: ${rect.x as Int},${rect.y as Int}
""")
                    writer.write("""  size: ${rect.width as Int},${rect.height as Int}
""")
                    if (rect!!.splits != null) {
                        writer.write("""  split: ${rect!!.splits.get(0).toString()}, ${rect!!.splits.get(1).toString()}, ${rect!!.splits.get(2).toString()}, ${rect!!.splits.get(3).toString()}
""")
                        if (rect!!.pads != null) {
                            writer.write("""  pad: ${rect!!.pads.get(0).toString()}, ${rect!!.pads.get(1).toString()}, ${rect!!.pads.get(2).toString()}, ${rect!!.pads.get(3).toString()}
""")
                        }
                    }
                    writer.write("""  orig: ${rect!!.originalWidth.toString()}, ${rect!!.originalHeight.toString()}
""")
                    writer.write("  offset: " + rect!!.offsetX.toString() + ", " + (rect!!.originalHeight - rect.height - rect!!.offsetY) as Int.toString() + "\n")
                    writer.write("  index: $imageIndex\n")
                }
            }
        }
        writer.close()
    }
}
