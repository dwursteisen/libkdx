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
package com.badlogic.gdx.graphics

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.TextureData.TextureDataType
import com.badlogic.gdx.graphics.glutils.ETC1TextureData
import com.badlogic.gdx.graphics.glutils.FileTextureData
import com.badlogic.gdx.graphics.glutils.KTXTextureData

/**
 * Used by a [Texture] to load the pixel data. A TextureData can either return a [Pixmap] or upload the pixel data
 * itself. It signals it's type via [.getType] to the Texture that's using it. The Texture will then either invoke
 * [.consumePixmap] or [.consumeCustomData]. These are the first methods to be called by Texture. After that
 * the Texture will invoke the other methods to find out about the size of the image data, the format, whether mipmaps should be
 * generated and whether the TextureData is able to manage the pixel data if the OpenGL ES context is lost.
 *
 *
 * In case the TextureData implementation has the type [TextureDataType.Custom], the implementation has to generate the
 * mipmaps itself if necessary. See [MipMapGenerator].
 *
 *
 * Before a call to either [.consumePixmap] or [.consumeCustomData], Texture will bind the OpenGL ES
 * texture.
 *
 *
 * Look at [FileTextureData] and [ETC1TextureData] for example implementations of this interface.
 *
 * @author mzechner
 */
interface TextureData {

    /**
     * The type of this [TextureData].
     *
     * @author mzechner
     */
    enum class TextureDataType {

        Pixmap, Custom
    }

    /**
     * @return the [TextureDataType]
     */
    val type: TextureDataType?

    /**
     * @return whether the TextureData is prepared or not.
     */
    val isPrepared: Boolean

    /**
     * Prepares the TextureData for a call to [.consumePixmap] or [.consumeCustomData]. This method can be
     * called from a non OpenGL thread and should thus not interact with OpenGL.
     */
    fun prepare()

    /**
     * Returns the [Pixmap] for upload by Texture. A call to [.prepare] must precede a call to this method. Any
     * internal data structures created in [.prepare] should be disposed of here.
     *
     * @return the pixmap.
     */
    fun consumePixmap(): Pixmap?

    /**
     * @return whether the caller of [.consumePixmap] should dispose the Pixmap returned by [.consumePixmap]
     */
    fun disposePixmap(): Boolean

    /**
     * Uploads the pixel data to the OpenGL ES texture. The caller must bind an OpenGL ES texture. A call to [.prepare]
     * must preceed a call to this method. Any internal data structures created in [.prepare] should be disposed of here.
     */
    fun consumeCustomData(target: Int)

    /**
     * @return the width of the pixel data
     */
    val width: Int

    /**
     * @return the height of the pixel data
     */
    val height: Int

    /**
     * @return the [Format] of the pixel data
     */
    val format: Pixmap.Format?

    /**
     * @return whether to generate mipmaps or not.
     */
    fun useMipMaps(): Boolean

    /**
     * @return whether this implementation can cope with a EGL context loss.
     */
    val isManaged: Boolean

    /**
     * Provides static method to instantiate the right implementation (Pixmap, ETC1, KTX).
     *
     * @author Vincent Bousquet
     */
    object Factory {

        fun loadFromFile(file: FileHandle?, useMipMaps: Boolean): TextureData? {
            return loadFromFile(file, null, useMipMaps)
        }

        fun loadFromFile(file: FileHandle?, format: Pixmap.Format?, useMipMaps: Boolean): TextureData? {
            if (file == null) return null
            if (file.name().endsWith(".cim")) return FileTextureData(file, PixmapIO.readCIM(file), format, useMipMaps)
            if (file.name().endsWith(".etc1")) return ETC1TextureData(file, useMipMaps)
            return if (file.name().endsWith(".ktx") || file.name().endsWith(".zktx")) KTXTextureData(file, useMipMaps) else FileTextureData(file, Pixmap(file), format, useMipMaps)
        }
    }
}
