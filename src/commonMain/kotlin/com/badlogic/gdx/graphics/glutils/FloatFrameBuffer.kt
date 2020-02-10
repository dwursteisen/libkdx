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

/** This is a [FrameBuffer] variant backed by a float texture.  */
class FloatFrameBuffer : com.badlogic.gdx.graphics.glutils.FrameBuffer {

    internal constructor() {}
    /**
     * Creates a GLFrameBuffer from the specifications provided by bufferBuilder
     *
     * @param bufferBuilder
     */
    constructor(bufferBuilder: com.badlogic.gdx.graphics.glutils.GLFrameBuffer.GLFrameBufferBuilder<out com.badlogic.gdx.graphics.glutils.GLFrameBuffer<com.badlogic.gdx.graphics.Texture?>?>?) : super(bufferBuilder) {}

    /** Creates a new FrameBuffer with a float backing texture, having the given dimensions and potentially a depth buffer attached.
     *
     * @param width the width of the framebuffer in pixels
     * @param height the height of the framebuffer in pixels
     * @param hasDepth whether to attach a depth buffer
     * @throws GdxRuntimeException in case the FrameBuffer could not be created
     */
    constructor(width: Int, height: Int, hasDepth: Boolean) {
        var bufferBuilder: com.badlogic.gdx.graphics.glutils.GLFrameBuffer.FloatFrameBufferBuilder = com.badlogic.gdx.graphics.glutils.GLFrameBuffer.FloatFrameBufferBuilder(width, height)
        bufferBuilder!!.addFloatAttachment(com.badlogic.gdx.graphics.GL30.GL_RGBA32F, com.badlogic.gdx.graphics.GL30.GL_RGBA, com.badlogic.gdx.graphics.GL30.GL_FLOAT, false)
        if (hasDepth) bufferBuilder!!.addBasicDepthRenderBuffer()
        bufferBuilder = bufferBuilder
        build()
    }

    protected override fun createTexture(attachmentSpec: com.badlogic.gdx.graphics.glutils.GLFrameBuffer.FrameBufferTextureAttachmentSpec?): com.badlogic.gdx.graphics.Texture? {
        val data: com.badlogic.gdx.graphics.glutils.FloatTextureData = com.badlogic.gdx.graphics.glutils.FloatTextureData(
            bufferBuilder!!.width, bufferBuilder!!.height,
            attachmentSpec!!.internalFormat, attachmentSpec!!.format, attachmentSpec!!.type,
            attachmentSpec!!.isGpuOnly
        )
        val result: com.badlogic.gdx.graphics.Texture = com.badlogic.gdx.graphics.Texture(data)
        if (com.badlogic.gdx.Gdx.app.getType() == com.badlogic.gdx.Application.ApplicationType.Desktop || com.badlogic.gdx.Gdx.app.getType() == com.badlogic.gdx.Application.ApplicationType.Applet) result.setFilter(com.badlogic.gdx.graphics.Texture.TextureFilter.Linear, com.badlogic.gdx.graphics.Texture.TextureFilter.Linear) else  // no filtering for float textures in OpenGL ES
            result.setFilter(com.badlogic.gdx.graphics.Texture.TextureFilter.Nearest, com.badlogic.gdx.graphics.Texture.TextureFilter.Nearest)
        result.setWrap(com.badlogic.gdx.graphics.Texture.TextureWrap.ClampToEdge, com.badlogic.gdx.graphics.Texture.TextureWrap.ClampToEdge)
        return result
    }
}
