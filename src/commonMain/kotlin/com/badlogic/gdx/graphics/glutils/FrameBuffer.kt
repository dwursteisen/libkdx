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
 * automatically create a texture for the color attachment and a renderbuffer for the depth buffer. You can get a hold of the
 * texture by [FrameBuffer.getColorBufferTexture]. This class will only work with OpenGL ES 2.0.
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
 * @author mzechner, realitix
 */
open class FrameBuffer : com.badlogic.gdx.graphics.glutils.GLFrameBuffer<com.badlogic.gdx.graphics.Texture?> {

    internal constructor() {}
    /**
     * Creates a GLFrameBuffer from the specifications provided by bufferBuilder
     *
     * @param bufferBuilder
     */
    constructor(bufferBuilder: com.badlogic.gdx.graphics.glutils.GLFrameBuffer.GLFrameBufferBuilder<out com.badlogic.gdx.graphics.glutils.GLFrameBuffer<com.badlogic.gdx.graphics.Texture?>?>?) : super(bufferBuilder) {}
    /** Creates a new FrameBuffer having the given dimensions and potentially a depth and a stencil buffer attached.
     *
     * @param format the format of the color buffer; according to the OpenGL ES 2.0 spec, only RGB565, RGBA4444 and RGB5_A1 are
     * color-renderable
     * @param width the width of the framebuffer in pixels
     * @param height the height of the framebuffer in pixels
     * @param hasDepth whether to attach a depth buffer
     * @throws com.badlogic.gdx.utils.GdxRuntimeException in case the FrameBuffer could not be created
     */
    /** Creates a new FrameBuffer having the given dimensions and potentially a depth buffer attached.  */
    @JvmOverloads
    constructor(format: com.badlogic.gdx.graphics.Pixmap.Format?, width: Int, height: Int, hasDepth: Boolean, hasStencil: Boolean = false) {
        val frameBufferBuilder: com.badlogic.gdx.graphics.glutils.GLFrameBuffer.FrameBufferBuilder = com.badlogic.gdx.graphics.glutils.GLFrameBuffer.FrameBufferBuilder(width, height)
        frameBufferBuilder!!.addBasicColorTextureAttachment(format)
        if (hasDepth) frameBufferBuilder!!.addBasicDepthRenderBuffer()
        if (hasStencil) frameBufferBuilder!!.addBasicStencilRenderBuffer()
        this.bufferBuilder = frameBufferBuilder
        build()
    }

    protected override fun createTexture(attachmentSpec: com.badlogic.gdx.graphics.glutils.GLFrameBuffer.FrameBufferTextureAttachmentSpec?): com.badlogic.gdx.graphics.Texture? {
        val data: com.badlogic.gdx.graphics.glutils.GLOnlyTextureData = com.badlogic.gdx.graphics.glutils.GLOnlyTextureData(bufferBuilder!!.width, bufferBuilder!!.height, 0, attachmentSpec!!.internalFormat, attachmentSpec!!.format, attachmentSpec!!.type)
        val result: com.badlogic.gdx.graphics.Texture = com.badlogic.gdx.graphics.Texture(data)
        result.setFilter(com.badlogic.gdx.graphics.Texture.TextureFilter.Linear, com.badlogic.gdx.graphics.Texture.TextureFilter.Linear)
        result.setWrap(com.badlogic.gdx.graphics.Texture.TextureWrap.ClampToEdge, com.badlogic.gdx.graphics.Texture.TextureWrap.ClampToEdge)
        return result
    }

    protected override fun disposeColorTexture(colorTexture: com.badlogic.gdx.graphics.Texture?) {
        colorTexture.dispose()
    }

    protected override fun attachFrameBufferColorTexture(texture: com.badlogic.gdx.graphics.Texture?) {
        com.badlogic.gdx.Gdx.gl20.glFramebufferTexture2D(com.badlogic.gdx.graphics.GL20.GL_FRAMEBUFFER, com.badlogic.gdx.graphics.GL20.GL_COLOR_ATTACHMENT0, com.badlogic.gdx.graphics.GL20.GL_TEXTURE_2D, texture.getTextureObjectHandle(), 0)
    }

    companion object {
        /** See [GLFrameBuffer.unbind]  */
        fun unbind() {
            com.badlogic.gdx.graphics.glutils.GLFrameBuffer.Companion.unbind()
        }
    }
}
