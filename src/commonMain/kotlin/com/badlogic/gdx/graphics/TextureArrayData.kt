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
import com.badlogic.gdx.graphics.glutils.FileTextureArrayData

/**
 * Used by a [TextureArray] to load the pixel data. The TextureArray will request the TextureArrayData to prepare itself through
 * [.prepare] and upload its data using [.consumeTextureArrayData]. These are the first methods to be called by TextureArray.
 * After that the TextureArray will invoke the other methods to find out about the size of the image data, the format, whether the
 * TextureArrayData is able to manage the pixel data if the OpenGL ES context is lost.
 *
 *
 * Before a call to either [.consumeTextureArrayData], TextureArray will bind the OpenGL ES texture.
 *
 *
 * Look at [FileTextureArrayData] for example implementation of this interface.
 *
 * @author Tomski
 */
interface TextureArrayData {

    /**
     * @return whether the TextureArrayData is prepared or not.
     */
    val isPrepared: Boolean

    /**
     * Prepares the TextureArrayData for a call to [.consumeTextureArrayData]. This method can be called from a non OpenGL thread and
     * should thus not interact with OpenGL.
     */
    fun prepare()

    /**
     * Uploads the pixel data of the TextureArray layers of the TextureArray to the OpenGL ES texture. The caller must bind an OpenGL ES texture. A
     * call to [.prepare] must preceed a call to this method. Any internal data structures created in [.prepare]
     * should be disposed of here.
     */
    fun consumeTextureArrayData()

    /**
     * @return the width of this TextureArray
     */
    val width: Int

    /**
     * @return the height of this TextureArray
     */
    val height: Int

    /**
     * @return the layer count of this TextureArray
     */
    val depth: Int

    /**
     * @return whether this implementation can cope with a EGL context loss.
     */
    val isManaged: Boolean

    /**
     * @return the internal format of this TextureArray
     */
    val internalFormat: Int

    /**
     * @return the GL type of this TextureArray
     */
    val gLType: Int

    /**
     * Provides static method to instantiate the right implementation.
     *
     * @author Tomski
     */
    object Factory {

        fun loadFromFiles(format: Pixmap.Format?, useMipMaps: Boolean, vararg files: FileHandle?): TextureArrayData {
            return FileTextureArrayData(format, useMipMaps, files)
        }
    }
}
