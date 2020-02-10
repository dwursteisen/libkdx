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
package com.badlogic.gdx.graphics.glutils

import com.badlogic.gdx.graphics.glutils.HdpiMode
import com.badlogic.gdx.graphics.glutils.InstanceData
import java.io.BufferedInputStream
import java.lang.IllegalStateException
import java.lang.NumberFormatException
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import java.util.HashMap
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
 *
 *
 * Encapsulates OpenGL ES 2.0 frame buffer objects. This is a simple helper class which should cover most FBO uses. It will
 * automatically create a cubemap for the color attachment and a renderbuffer for the depth buffer. You can get a hold of the
 * cubemap by [FrameBufferCubemap.getColorBufferTexture]. This class will only work with OpenGL ES 2.0.
 *
 *
 *
 *
 * FrameBuffers are managed. In case of an OpenGL context loss, which only happens on Android when a user switches to another
 * application or receives an incoming call, the framebuffer will be automatically recreated.
 *
 *
 *
 *
 * A FrameBuffer must be disposed if it is no longer needed
 *
 *
 *
 *
 * Typical use: <br></br>
 * FrameBufferCubemap frameBuffer = new FrameBufferCubemap(Format.RGBA8888, fSize, fSize, true); <br></br>
 * frameBuffer.begin(); <br></br>
 * while( frameBuffer.nextSide() ) { <br></br>
 * frameBuffer.getSide().getUp(camera.up); <br></br>
 * frameBuffer.getSide().getDirection(camera.direction);<br></br>
 * camera.update(); <br></br>
 *
 * Gdx.gl.glClearColor(0, 0, 0, 1); <br></br>
 * Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT); <br></br>
 * modelBatch.begin(camera); <br></br>
 * modelBatch.render(renderableProviders); <br></br>
 * modelBatch.end(); <br></br>
 * } <br></br>
 * frameBuffer.end(); <br></br>
 * Cubemap cubemap = frameBuffer.getColorBufferCubemap();
 *
 *
 * @author realitix
 */
class FrameBufferCubemap : com.badlogic.gdx.graphics.glutils.GLFrameBuffer<com.badlogic.gdx.graphics.Cubemap?> {

    /** the zero-based index of the active side  */
    private var currentSide = 0

    internal constructor() {}
    /**
     * Creates a GLFrameBuffer from the specifications provided by bufferBuilder
     *
     * @param bufferBuilder
     */
    constructor(bufferBuilder: com.badlogic.gdx.graphics.glutils.GLFrameBuffer.GLFrameBufferBuilder<out com.badlogic.gdx.graphics.glutils.GLFrameBuffer<com.badlogic.gdx.graphics.Cubemap?>?>?) : super(bufferBuilder) {}
    /** Creates a new FrameBuffer having the given dimensions and potentially a depth and a stencil buffer attached.
     *
     * @param format the format of the color buffer; according to the OpenGL ES 2.0 spec, only RGB565, RGBA4444 and RGB5_A1 are
     * color-renderable
     * @param width the width of the cubemap in pixels
     * @param height the height of the cubemap in pixels
     * @param hasDepth whether to attach a depth buffer
     * @param hasStencil whether to attach a stencil buffer
     * @throws com.badlogic.gdx.utils.GdxRuntimeException in case the FrameBuffer could not be created
     */
    /** Creates a new FrameBuffer having the given dimensions and potentially a depth buffer attached.
     *
     * @param format
     * @param width
     * @param height
     * @param hasDepth
     */
    @JvmOverloads
    constructor(format: com.badlogic.gdx.graphics.Pixmap.Format?, width: Int, height: Int, hasDepth: Boolean, hasStencil: Boolean = false) {
        val frameBufferBuilder: com.badlogic.gdx.graphics.glutils.GLFrameBuffer.FrameBufferCubemapBuilder = com.badlogic.gdx.graphics.glutils.GLFrameBuffer.FrameBufferCubemapBuilder(width, height)
        frameBufferBuilder!!.addBasicColorTextureAttachment(format)
        if (hasDepth) frameBufferBuilder!!.addBasicDepthRenderBuffer()
        if (hasStencil) frameBufferBuilder!!.addBasicStencilRenderBuffer()
        this.bufferBuilder = frameBufferBuilder
        build()
    }

    protected override fun createTexture(attachmentSpec: com.badlogic.gdx.graphics.glutils.GLFrameBuffer.FrameBufferTextureAttachmentSpec?): com.badlogic.gdx.graphics.Cubemap? {
        val data: com.badlogic.gdx.graphics.glutils.GLOnlyTextureData = com.badlogic.gdx.graphics.glutils.GLOnlyTextureData(bufferBuilder!!.width, bufferBuilder!!.height, 0, attachmentSpec!!.internalFormat, attachmentSpec!!.format, attachmentSpec!!.type)
        val result: com.badlogic.gdx.graphics.Cubemap = com.badlogic.gdx.graphics.Cubemap(data, data, data, data, data, data)
        result.setFilter(com.badlogic.gdx.graphics.Texture.TextureFilter.Linear, com.badlogic.gdx.graphics.Texture.TextureFilter.Linear)
        result.setWrap(com.badlogic.gdx.graphics.Texture.TextureWrap.ClampToEdge, com.badlogic.gdx.graphics.Texture.TextureWrap.ClampToEdge)
        return result
    }

    protected override fun disposeColorTexture(colorTexture: com.badlogic.gdx.graphics.Cubemap?) {
        colorTexture.dispose()
    }

    protected override fun attachFrameBufferColorTexture(texture: com.badlogic.gdx.graphics.Cubemap?) {
        val gl: com.badlogic.gdx.graphics.GL20 = com.badlogic.gdx.Gdx.gl20
        val glHandle: Int = texture.getTextureObjectHandle()
        val sides: Array<com.badlogic.gdx.graphics.Cubemap.CubemapSide?> = com.badlogic.gdx.graphics.Cubemap.CubemapSide.values()
        for (side in sides) {
            gl.glFramebufferTexture2D(com.badlogic.gdx.graphics.GL20.GL_FRAMEBUFFER, com.badlogic.gdx.graphics.GL20.GL_COLOR_ATTACHMENT0, side.glEnum,
                glHandle, 0)
        }
    }

    /** Makes the frame buffer current so everything gets drawn to it, must be followed by call to either [.nextSide] or
     * [.bindSide] to activate the side to render onto.  */
    override fun bind() {
        currentSide = -1
        super.bind()
    }

    /** Bind the next side of cubemap and return false if no more side. Should be called in between a call to [.begin] and
     * #end to cycle to each side of the cubemap to render on.  */
    fun nextSide(): Boolean {
        if (currentSide > 5) {
            throw com.badlogic.gdx.utils.GdxRuntimeException("No remaining sides.")
        } else if (currentSide == 5) {
            return false
        }
        currentSide++
        bindSide(getSide())
        return true
    }

    /** Bind the side, making it active to render on. Should be called in between a call to [.begin] and [.end].
     * @param side The side to bind
     */
    protected fun bindSide(side: com.badlogic.gdx.graphics.Cubemap.CubemapSide?) {
        com.badlogic.gdx.Gdx.gl20.glFramebufferTexture2D(com.badlogic.gdx.graphics.GL20.GL_FRAMEBUFFER, com.badlogic.gdx.graphics.GL20.GL_COLOR_ATTACHMENT0, side.glEnum, getColorBufferTexture().getTextureObjectHandle(), 0)
    }

    /** Get the currently bound side.  */
    fun getSide(): com.badlogic.gdx.graphics.Cubemap.CubemapSide? {
        return if (currentSide < 0) null else cubemapSides!![currentSide]
    }

    companion object {
        /** cubemap sides cache  */
        private val cubemapSides: Array<com.badlogic.gdx.graphics.Cubemap.CubemapSide?>? = com.badlogic.gdx.graphics.Cubemap.CubemapSide.values()
    }
}
