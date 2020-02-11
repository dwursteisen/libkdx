package com.badlogic.gdx.graphics

/**
 * Used by a [Cubemap] to load the pixel data. The Cubemap will request the CubemapData to prepare itself through
 * [.prepare] and upload its data using [.consumeCubemapData]. These are the first methods to be called by Cubemap.
 * After that the Cubemap will invoke the other methods to find out about the size of the image data, the format, whether the
 * CubemapData is able to manage the pixel data if the OpenGL ES context is lost.
 *
 *
 * Before a call to either [.consumeCubemapData], Cubemap will bind the OpenGL ES texture.
 *
 *
 * Look at [KTXTextureData] for example implementation of this interface.
 *
 * @author Vincent Bousquet
 */
interface CubemapData {

    /**
     * @return whether the TextureData is prepared or not.
     */
    val isPrepared: Boolean

    /**
     * Prepares the TextureData for a call to [.consumeCubemapData]. This method can be called from a non OpenGL thread and
     * should thus not interact with OpenGL.
     */
    fun prepare()

    /**
     * Uploads the pixel data for the 6 faces of the cube to the OpenGL ES texture. The caller must bind an OpenGL ES texture. A
     * call to [.prepare] must preceed a call to this method. Any internal data structures created in [.prepare]
     * should be disposed of here.
     */
    fun consumeCubemapData()

    /**
     * @return the width of the pixel data
     */
    val width: Int

    /**
     * @return the height of the pixel data
     */
    val height: Int

    /**
     * @return whether this implementation can cope with a EGL context loss.
     */
    val isManaged: Boolean
}
