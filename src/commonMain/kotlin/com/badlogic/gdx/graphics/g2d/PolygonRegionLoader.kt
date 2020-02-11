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

import com.badlogic.gdx.graphics.g2d.PolygonRegionLoader.PolygonRegionParameters
import com.badlogic.gdx.graphics.g2d.PolygonSprite

/**
 * loads [PolygonRegions][PolygonRegion] using a [com.badlogic.gdx.graphics.g2d.PolygonRegionLoader]
 *
 * @author dermetfan
 */
class PolygonRegionLoader @JvmOverloads constructor(resolver: FileHandleResolver? = InternalFileHandleResolver()) : SynchronousAssetLoader<PolygonRegion?, PolygonRegionParameters?>(resolver) {

    class PolygonRegionParameters : AssetLoaderParameters<PolygonRegion?>() {
        /**
         * what the line starts with that contains the file name of the texture for this `PolygonRegion`
         */
        var texturePrefix = "i "

        /**
         * what buffer size of the reader should be used to read the [.texturePrefix] line
         *
         * @see FileHandle.reader
         */
        var readerBuffer = 1024

        /**
         * the possible file name extensions of the texture file
         */
        var textureExtensions: Array<String>? = arrayOf("png", "PNG", "jpeg", "JPEG", "jpg", "JPG", "cim", "CIM", "etc1", "ETC1",
            "ktx", "KTX", "zktx", "ZKTX")
    }

    private val defaultParameters = PolygonRegionParameters()
    private val triangulator: EarClippingTriangulator = EarClippingTriangulator()
    fun load(manager: AssetManager, fileName: String?, file: FileHandle, parameter: PolygonRegionParameters?): PolygonRegion {
        val texture: Texture = manager.get(manager.getDependencies(fileName).first())
        return load(TextureRegion(texture), file)
    }

    /**
     * If the PSH file contains a line starting with [params.texturePrefix][PolygonRegionParameters.texturePrefix], an
     * [AssetDescriptor] for the file referenced on that line will be added to the returned Array. Otherwise a sibling of the
     * given file with the same name and the first found extension in [ params.textureExtensions][PolygonRegionParameters.textureExtensions] will be used. If no suitable file is found, the returned Array will be empty.
     */
    fun getDependencies(fileName: String, file: FileHandle, params: PolygonRegionParameters?): Array<AssetDescriptor>? {
        var params = params
        if (params == null) params = defaultParameters
        var image: String? = null
        try {
            val reader: BufferedReader = file.reader(params.readerBuffer)
            var line: String = reader.readLine()
            while (line != null) {
                if (line.startsWith(params.texturePrefix)) {
                    image = line.substring(params.texturePrefix.length)
                    break
                }
                line = reader.readLine()
            }
            reader.close()
        } catch (e: IOException) {
            throw GdxRuntimeException("Error reading $fileName", e)
        }
        if (image == null && params.textureExtensions != null) for (extension in params.textureExtensions!!) {
            val sibling: FileHandle = file.sibling(file.nameWithoutExtension().concat(".$extension"))
            if (sibling.exists()) image = sibling.name()
        }
        if (image != null) {
            val deps: Array<AssetDescriptor> = Array<AssetDescriptor>(1)
            deps.add(AssetDescriptor<Texture>(file.sibling(image), Texture::class.java))
            return deps
        }
        return null
    }

    /**
     * Loads a PolygonRegion from a PSH (Polygon SHape) file. The PSH file format defines the polygon vertices before
     * triangulation:
     *
     *
     * s 200.0, 100.0, ...
     *
     *
     * Lines not prefixed with "s" are ignored. PSH files can be created with external tools, eg: <br></br>
     * https://code.google.com/p/libgdx-polygoneditor/ <br></br>
     * http://www.codeandweb.com/physicseditor/
     *
     * @param file file handle to the shape definition file
     */
    fun load(textureRegion: TextureRegion, file: FileHandle): PolygonRegion {
        val reader: BufferedReader = file.reader(256)
        try {
            while (true) {
                val line: String = reader.readLine() ?: break
                if (line.startsWith("s")) {
                    // Read shape.
                    val polygonStrings = line.substring(1).trim { it <= ' ' }.split(",").toTypedArray()
                    val vertices = FloatArray(polygonStrings.size)
                    var i = 0
                    val n = vertices.size
                    while (i < n) {
                        vertices[i] = polygonStrings[i].toFloat()
                        i++
                    }
                    // It would probably be better if PSH stored the vertices and triangles, then we don't have to triangulate here.
                    return PolygonRegion(textureRegion, vertices, triangulator.computeTriangles(vertices).toArray())
                }
            }
        } catch (ex: IOException) {
            throw GdxRuntimeException("Error reading polygon shape file: $file", ex)
        } finally {
            StreamUtils.closeQuietly(reader)
        }
        throw GdxRuntimeException("Polygon shape not found: $file")
    }
}
