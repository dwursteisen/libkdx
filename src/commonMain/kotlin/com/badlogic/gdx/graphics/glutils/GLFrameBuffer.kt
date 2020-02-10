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
 * automatically create a gltexture for the color attachment and a renderbuffer for the depth buffer. You can get a hold of the
 * gltexture by [GLFrameBuffer.getColorBufferTexture]. This class will only work with OpenGL ES 2.0.
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
abstract class GLFrameBuffer<T : com.badlogic.gdx.graphics.GLTexture?> : com.badlogic.gdx.utils.Disposable {

    /** the color buffer texture  */
    protected var textureAttachments: com.badlogic.gdx.utils.Array<T?>? = com.badlogic.gdx.utils.Array()
    /** @return The OpenGL handle of the framebuffer (see [GL20.glGenFramebuffer])
     */
    /** the framebuffer handle  */
    var framebufferHandle = 0
        protected set
    /** the depthbuffer render object handle  */
    protected var depthbufferHandle = 0
    /** the stencilbuffer render object handle  */
    protected var stencilbufferHandle = 0
    /** the depth stencil packed render buffer object handle  */
    protected var depthStencilPackedBufferHandle = 0
    /** if has depth stencil packed buffer  */
    protected var hasDepthStencilPackedBuffer = false
    /** if multiple texture attachments are present  */
    protected var isMRT = false
    protected var bufferBuilder: GLFrameBufferBuilder<out GLFrameBuffer<T?>?>? = null

    internal constructor() {}
    /** Creates a GLFrameBuffer from the specifications provided by bufferBuilder  */
    protected constructor(bufferBuilder: GLFrameBufferBuilder<out GLFrameBuffer<T?>?>?) {
        this.bufferBuilder = bufferBuilder
        build()
    }

    val colorBufferTexture: T?
        get() = textureAttachments.first()

    /** Convenience method to return the first Texture attachment present in the fbo  */
    fun getColorBufferTexture(): T? {
        return textureAttachments.first()
    }

    /** Return the Texture attachments attached to the fbo  */
    fun getTextureAttachments(): com.badlogic.gdx.utils.Array<T?>? {
        return textureAttachments
    }

    /** Override this method in a derived class to set up the backing texture as you like.  */
    protected abstract fun createTexture(attachmentSpec: FrameBufferTextureAttachmentSpec?): T?

    /** Override this method in a derived class to dispose the backing texture as you like.  */
    protected abstract fun disposeColorTexture(colorTexture: T?)

    /** Override this method in a derived class to attach the backing texture to the GL framebuffer object.  */
    protected abstract fun attachFrameBufferColorTexture(texture: T?)

    protected fun build() {
        val gl: com.badlogic.gdx.graphics.GL20 = com.badlogic.gdx.Gdx.gl20
        checkValidBuilder()
        // iOS uses a different framebuffer handle! (not necessarily 0)
        if (!defaultFramebufferHandleInitialized) {
            defaultFramebufferHandleInitialized = true
            defaultFramebufferHandle = if (com.badlogic.gdx.Gdx.app.getType() == com.badlogic.gdx.Application.ApplicationType.iOS) {
                val intbuf: java.nio.IntBuffer = java.nio.ByteBuffer.allocateDirect(16 * java.lang.Integer.SIZE / 8).order(ByteOrder.nativeOrder()).asIntBuffer()
                gl.glGetIntegerv(com.badlogic.gdx.graphics.GL20.GL_FRAMEBUFFER_BINDING, intbuf)
                intbuf.get(0)
            } else {
                0
            }
        }
        framebufferHandle = gl.glGenFramebuffer()
        gl.glBindFramebuffer(com.badlogic.gdx.graphics.GL20.GL_FRAMEBUFFER, framebufferHandle)
        val width = bufferBuilder!!.width
        val height = bufferBuilder!!.height
        if (bufferBuilder!!.hasDepthRenderBuffer) {
            depthbufferHandle = gl.glGenRenderbuffer()
            gl.glBindRenderbuffer(com.badlogic.gdx.graphics.GL20.GL_RENDERBUFFER, depthbufferHandle)
            gl.glRenderbufferStorage(com.badlogic.gdx.graphics.GL20.GL_RENDERBUFFER, bufferBuilder!!.depthRenderBufferSpec!!.internalFormat, width, height)
        }
        if (bufferBuilder!!.hasStencilRenderBuffer) {
            stencilbufferHandle = gl.glGenRenderbuffer()
            gl.glBindRenderbuffer(com.badlogic.gdx.graphics.GL20.GL_RENDERBUFFER, stencilbufferHandle)
            gl.glRenderbufferStorage(com.badlogic.gdx.graphics.GL20.GL_RENDERBUFFER, bufferBuilder!!.stencilRenderBufferSpec!!.internalFormat, width, height)
        }
        if (bufferBuilder!!.hasPackedStencilDepthRenderBuffer) {
            depthStencilPackedBufferHandle = gl.glGenRenderbuffer()
            gl.glBindRenderbuffer(com.badlogic.gdx.graphics.GL20.GL_RENDERBUFFER, depthStencilPackedBufferHandle)
            gl.glRenderbufferStorage(com.badlogic.gdx.graphics.GL20.GL_RENDERBUFFER, bufferBuilder!!.packedStencilDepthRenderBufferSpec!!.internalFormat, width,
                height)
        }
        isMRT = bufferBuilder!!.textureAttachmentSpecs.size > 1
        var colorTextureCounter = 0
        if (isMRT) {
            for (attachmentSpec in bufferBuilder!!.textureAttachmentSpecs) {
                val texture = createTexture(attachmentSpec)
                textureAttachments.add(texture)
                if (attachmentSpec!!.isColorTexture()) {
                    gl.glFramebufferTexture2D(com.badlogic.gdx.graphics.GL20.GL_FRAMEBUFFER, com.badlogic.gdx.graphics.GL30.GL_COLOR_ATTACHMENT0 + colorTextureCounter, com.badlogic.gdx.graphics.GL30.GL_TEXTURE_2D,
                        texture.getTextureObjectHandle(), 0)
                    colorTextureCounter++
                } else if (attachmentSpec!!.isDepth) {
                    gl.glFramebufferTexture2D(com.badlogic.gdx.graphics.GL20.GL_FRAMEBUFFER, com.badlogic.gdx.graphics.GL20.GL_DEPTH_ATTACHMENT, com.badlogic.gdx.graphics.GL20.GL_TEXTURE_2D,
                        texture.getTextureObjectHandle(), 0)
                } else if (attachmentSpec!!.isStencil) {
                    gl.glFramebufferTexture2D(com.badlogic.gdx.graphics.GL20.GL_FRAMEBUFFER, com.badlogic.gdx.graphics.GL20.GL_STENCIL_ATTACHMENT, com.badlogic.gdx.graphics.GL20.GL_TEXTURE_2D,
                        texture.getTextureObjectHandle(), 0)
                }
            }
        } else {
            val texture = createTexture(bufferBuilder!!.textureAttachmentSpecs.first())
            textureAttachments.add(texture)
            gl.glBindTexture(texture.glTarget, texture.getTextureObjectHandle())
        }
        if (isMRT) {
            val buffer: java.nio.IntBuffer = com.badlogic.gdx.utils.BufferUtils.newIntBuffer(colorTextureCounter)
            for (i in 0 until colorTextureCounter) {
                buffer.put(com.badlogic.gdx.graphics.GL30.GL_COLOR_ATTACHMENT0 + i)
            }
            buffer.position(0)
            com.badlogic.gdx.Gdx.gl30.glDrawBuffers(colorTextureCounter, buffer)
        } else {
            attachFrameBufferColorTexture(textureAttachments.first())
        }
        if (bufferBuilder!!.hasDepthRenderBuffer) {
            gl.glFramebufferRenderbuffer(com.badlogic.gdx.graphics.GL20.GL_FRAMEBUFFER, com.badlogic.gdx.graphics.GL20.GL_DEPTH_ATTACHMENT, com.badlogic.gdx.graphics.GL20.GL_RENDERBUFFER, depthbufferHandle)
        }
        if (bufferBuilder!!.hasStencilRenderBuffer) {
            gl.glFramebufferRenderbuffer(com.badlogic.gdx.graphics.GL20.GL_FRAMEBUFFER, com.badlogic.gdx.graphics.GL20.GL_STENCIL_ATTACHMENT, com.badlogic.gdx.graphics.GL20.GL_RENDERBUFFER, stencilbufferHandle)
        }
        if (bufferBuilder!!.hasPackedStencilDepthRenderBuffer) {
            gl.glFramebufferRenderbuffer(com.badlogic.gdx.graphics.GL20.GL_FRAMEBUFFER, com.badlogic.gdx.graphics.GL30.GL_DEPTH_STENCIL_ATTACHMENT, com.badlogic.gdx.graphics.GL20.GL_RENDERBUFFER,
                depthStencilPackedBufferHandle)
        }
        gl.glBindRenderbuffer(com.badlogic.gdx.graphics.GL20.GL_RENDERBUFFER, 0)
        for (texture in textureAttachments) {
            gl.glBindTexture(texture.glTarget, 0)
        }
        var result: Int = gl.glCheckFramebufferStatus(com.badlogic.gdx.graphics.GL20.GL_FRAMEBUFFER)
        if (result == com.badlogic.gdx.graphics.GL20.GL_FRAMEBUFFER_UNSUPPORTED && bufferBuilder!!.hasDepthRenderBuffer && bufferBuilder!!.hasStencilRenderBuffer
            && (com.badlogic.gdx.Gdx.graphics.supportsExtension("GL_OES_packed_depth_stencil")
                || com.badlogic.gdx.Gdx.graphics.supportsExtension("GL_EXT_packed_depth_stencil"))) {
            if (bufferBuilder!!.hasDepthRenderBuffer) {
                gl.glDeleteRenderbuffer(depthbufferHandle)
                depthbufferHandle = 0
            }
            if (bufferBuilder!!.hasStencilRenderBuffer) {
                gl.glDeleteRenderbuffer(stencilbufferHandle)
                stencilbufferHandle = 0
            }
            if (bufferBuilder!!.hasPackedStencilDepthRenderBuffer) {
                gl.glDeleteRenderbuffer(depthStencilPackedBufferHandle)
                depthStencilPackedBufferHandle = 0
            }
            depthStencilPackedBufferHandle = gl.glGenRenderbuffer()
            hasDepthStencilPackedBuffer = true
            gl.glBindRenderbuffer(com.badlogic.gdx.graphics.GL20.GL_RENDERBUFFER, depthStencilPackedBufferHandle)
            gl.glRenderbufferStorage(com.badlogic.gdx.graphics.GL20.GL_RENDERBUFFER, GL_DEPTH24_STENCIL8_OES, width, height)
            gl.glBindRenderbuffer(com.badlogic.gdx.graphics.GL20.GL_RENDERBUFFER, 0)
            gl.glFramebufferRenderbuffer(com.badlogic.gdx.graphics.GL20.GL_FRAMEBUFFER, com.badlogic.gdx.graphics.GL20.GL_DEPTH_ATTACHMENT, com.badlogic.gdx.graphics.GL20.GL_RENDERBUFFER,
                depthStencilPackedBufferHandle)
            gl.glFramebufferRenderbuffer(com.badlogic.gdx.graphics.GL20.GL_FRAMEBUFFER, com.badlogic.gdx.graphics.GL20.GL_STENCIL_ATTACHMENT, com.badlogic.gdx.graphics.GL20.GL_RENDERBUFFER,
                depthStencilPackedBufferHandle)
            result = gl.glCheckFramebufferStatus(com.badlogic.gdx.graphics.GL20.GL_FRAMEBUFFER)
        }
        gl.glBindFramebuffer(com.badlogic.gdx.graphics.GL20.GL_FRAMEBUFFER, defaultFramebufferHandle)
        if (result != com.badlogic.gdx.graphics.GL20.GL_FRAMEBUFFER_COMPLETE) {
            for (texture in textureAttachments) {
                disposeColorTexture(texture)
            }
            if (hasDepthStencilPackedBuffer) {
                gl.glDeleteBuffer(depthStencilPackedBufferHandle)
            } else {
                if (bufferBuilder!!.hasDepthRenderBuffer) gl.glDeleteRenderbuffer(depthbufferHandle)
                if (bufferBuilder!!.hasStencilRenderBuffer) gl.glDeleteRenderbuffer(stencilbufferHandle)
            }
            gl.glDeleteFramebuffer(framebufferHandle)
            check(result != com.badlogic.gdx.graphics.GL20.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT) { "Frame buffer couldn't be constructed: incomplete attachment" }
            check(result != com.badlogic.gdx.graphics.GL20.GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS) { "Frame buffer couldn't be constructed: incomplete dimensions" }
            check(result != com.badlogic.gdx.graphics.GL20.GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT) { "Frame buffer couldn't be constructed: missing attachment" }
            check(result != com.badlogic.gdx.graphics.GL20.GL_FRAMEBUFFER_UNSUPPORTED) { "Frame buffer couldn't be constructed: unsupported combination of formats" }
            throw IllegalStateException("Frame buffer couldn't be constructed: unknown error $result")
        }
        addManagedFrameBuffer(com.badlogic.gdx.Gdx.app, this)
    }

    private fun checkValidBuilder() {
        val runningGL30: Boolean = com.badlogic.gdx.Gdx.graphics.isGL30Available()
        if (!runningGL30) {
            if (bufferBuilder!!.hasPackedStencilDepthRenderBuffer) {
                throw com.badlogic.gdx.utils.GdxRuntimeException("Packed Stencil/Render render buffers are not available on GLES 2.0")
            }
            if (bufferBuilder!!.textureAttachmentSpecs.size > 1) {
                throw com.badlogic.gdx.utils.GdxRuntimeException("Multiple render targets not available on GLES 2.0")
            }
            for (spec in bufferBuilder!!.textureAttachmentSpecs) {
                if (spec!!.isDepth) throw com.badlogic.gdx.utils.GdxRuntimeException("Depth texture FrameBuffer Attachment not available on GLES 2.0")
                if (spec!!.isStencil) throw com.badlogic.gdx.utils.GdxRuntimeException("Stencil texture FrameBuffer Attachment not available on GLES 2.0")
                if (spec!!.isFloat) {
                    if (!com.badlogic.gdx.Gdx.graphics.supportsExtension("OES_texture_float")) {
                        throw com.badlogic.gdx.utils.GdxRuntimeException("Float texture FrameBuffer Attachment not available on GLES 2.0")
                    }
                }
            }
        }
    }

    /** Releases all resources associated with the FrameBuffer.  */
    override fun dispose() {
        val gl: com.badlogic.gdx.graphics.GL20 = com.badlogic.gdx.Gdx.gl20
        for (texture in textureAttachments) {
            disposeColorTexture(texture)
        }
        if (hasDepthStencilPackedBuffer) {
            gl.glDeleteRenderbuffer(depthStencilPackedBufferHandle)
        } else {
            if (bufferBuilder!!.hasDepthRenderBuffer) gl.glDeleteRenderbuffer(depthbufferHandle)
            if (bufferBuilder!!.hasStencilRenderBuffer) gl.glDeleteRenderbuffer(stencilbufferHandle)
        }
        gl.glDeleteFramebuffer(framebufferHandle)
        if (buffers!![com.badlogic.gdx.Gdx.app] != null) buffers[com.badlogic.gdx.Gdx.app].removeValue(this, true)
    }

    /** Makes the frame buffer current so everything gets drawn to it.  */
    open fun bind() {
        com.badlogic.gdx.Gdx.gl20.glBindFramebuffer(com.badlogic.gdx.graphics.GL20.GL_FRAMEBUFFER, framebufferHandle)
    }

    /** Binds the frame buffer and sets the viewport accordingly, so everything gets drawn to it.  */
    fun begin() {
        bind()
        setFrameBufferViewport()
    }

    /** Sets viewport to the dimensions of framebuffer. Called by [.begin].  */
    protected fun setFrameBufferViewport() {
        com.badlogic.gdx.Gdx.gl20.glViewport(0, 0, bufferBuilder!!.width, bufferBuilder!!.height)
    }
    /** Unbinds the framebuffer and sets viewport sizes, all drawing will be performed to the normal framebuffer from here on.
     *
     * @param x the x-axis position of the viewport in pixels
     * @param y the y-asis position of the viewport in pixels
     * @param width the width of the viewport in pixels
     * @param height the height of the viewport in pixels
     */
    /** Unbinds the framebuffer, all drawing will be performed to the normal framebuffer from here on.  */
    @JvmOverloads
    fun end(x: Int = 0, y: Int = 0, width: Int = com.badlogic.gdx.Gdx.graphics.getBackBufferWidth(), height: Int = com.badlogic.gdx.Gdx.graphics.getBackBufferHeight()) {
        unbind()
        com.badlogic.gdx.Gdx.gl20.glViewport(x, y, width, height)
    }

    /** @return The OpenGL handle of the (optional) depth buffer (see [GL20.glGenRenderbuffer]). May return 0 even if depth
     * buffer enabled
     */
    fun getDepthBufferHandle(): Int {
        return depthbufferHandle
    }

    /** @return The OpenGL handle of the (optional) stencil buffer (see [GL20.glGenRenderbuffer]). May return 0 even if
     * stencil buffer enabled
     */
    fun getStencilBufferHandle(): Int {
        return stencilbufferHandle
    }

    /** @return The OpenGL handle of the packed depth & stencil buffer (GL_DEPTH24_STENCIL8_OES) or 0 if not used.
     */
    protected fun getDepthStencilPackedBuffer(): Int {
        return depthStencilPackedBufferHandle
    }

    /** @return the height of the framebuffer in pixels
     */
    fun getHeight(): Int {
        return bufferBuilder!!.height
    }

    /** @return the width of the framebuffer in pixels
     */
    fun getWidth(): Int {
        return bufferBuilder!!.width
    }

    protected class FrameBufferTextureAttachmentSpec(var internalFormat: Int, var format: Int, var type: Int) {
        var isFloat = false
        var isGpuOnly = false
        var isDepth = false
        var isStencil = false
        fun isColorTexture(): Boolean {
            return !isDepth && !isStencil
        }
    }

    protected class FrameBufferRenderBufferAttachmentSpec(var internalFormat: Int)

    abstract class GLFrameBufferBuilder<U : GLFrameBuffer<out com.badlogic.gdx.graphics.GLTexture?>?>(var width: Int, var height: Int) {
        var textureAttachmentSpecs: com.badlogic.gdx.utils.Array<FrameBufferTextureAttachmentSpec?>? = com.badlogic.gdx.utils.Array()
        var stencilRenderBufferSpec: FrameBufferRenderBufferAttachmentSpec? = null
        var depthRenderBufferSpec: FrameBufferRenderBufferAttachmentSpec? = null
        var packedStencilDepthRenderBufferSpec: FrameBufferRenderBufferAttachmentSpec? = null
        var hasStencilRenderBuffer = false
        var hasDepthRenderBuffer = false
        var hasPackedStencilDepthRenderBuffer = false
        fun addColorTextureAttachment(internalFormat: Int, format: Int, type: Int): GLFrameBufferBuilder<U?>? {
            textureAttachmentSpecs.add(FrameBufferTextureAttachmentSpec(internalFormat, format, type))
            return this
        }

        fun addBasicColorTextureAttachment(format: com.badlogic.gdx.graphics.Pixmap.Format?): GLFrameBufferBuilder<U?>? {
            val glFormat: Int = com.badlogic.gdx.graphics.Pixmap.Format.toGlFormat(format)
            val glType: Int = com.badlogic.gdx.graphics.Pixmap.Format.toGlType(format)
            return addColorTextureAttachment(glFormat, glFormat, glType)
        }

        fun addFloatAttachment(internalFormat: Int, format: Int, type: Int, gpuOnly: Boolean): GLFrameBufferBuilder<U?>? {
            val spec = FrameBufferTextureAttachmentSpec(internalFormat, format, type)
            spec.isFloat = true
            spec.isGpuOnly = gpuOnly
            textureAttachmentSpecs.add(spec)
            return this
        }

        fun addDepthTextureAttachment(internalFormat: Int, type: Int): GLFrameBufferBuilder<U?>? {
            val spec = FrameBufferTextureAttachmentSpec(internalFormat, com.badlogic.gdx.graphics.GL30.GL_DEPTH_COMPONENT,
                type)
            spec.isDepth = true
            textureAttachmentSpecs.add(spec)
            return this
        }

        fun addStencilTextureAttachment(internalFormat: Int, type: Int): GLFrameBufferBuilder<U?>? {
            val spec = FrameBufferTextureAttachmentSpec(internalFormat, com.badlogic.gdx.graphics.GL30.GL_STENCIL_ATTACHMENT,
                type)
            spec.isStencil = true
            textureAttachmentSpecs.add(spec)
            return this
        }

        fun addDepthRenderBuffer(internalFormat: Int): GLFrameBufferBuilder<U?>? {
            depthRenderBufferSpec = FrameBufferRenderBufferAttachmentSpec(internalFormat)
            hasDepthRenderBuffer = true
            return this
        }

        fun addStencilRenderBuffer(internalFormat: Int): GLFrameBufferBuilder<U?>? {
            stencilRenderBufferSpec = FrameBufferRenderBufferAttachmentSpec(internalFormat)
            hasStencilRenderBuffer = true
            return this
        }

        fun addStencilDepthPackedRenderBuffer(internalFormat: Int): GLFrameBufferBuilder<U?>? {
            packedStencilDepthRenderBufferSpec = FrameBufferRenderBufferAttachmentSpec(internalFormat)
            hasPackedStencilDepthRenderBuffer = true
            return this
        }

        fun addBasicDepthRenderBuffer(): GLFrameBufferBuilder<U?>? {
            return addDepthRenderBuffer(com.badlogic.gdx.graphics.GL20.GL_DEPTH_COMPONENT16)
        }

        fun addBasicStencilRenderBuffer(): GLFrameBufferBuilder<U?>? {
            return addStencilRenderBuffer(com.badlogic.gdx.graphics.GL20.GL_STENCIL_INDEX8)
        }

        fun addBasicStencilDepthPackedRenderBuffer(): GLFrameBufferBuilder<U?>? {
            return addStencilDepthPackedRenderBuffer(com.badlogic.gdx.graphics.GL30.GL_DEPTH24_STENCIL8)
        }

        abstract fun build(): U?
    }

    class FrameBufferBuilder(width: Int, height: Int) : GLFrameBufferBuilder<com.badlogic.gdx.graphics.glutils.FrameBuffer?>(width, height) {
        override fun build(): com.badlogic.gdx.graphics.glutils.FrameBuffer? {
            return com.badlogic.gdx.graphics.glutils.FrameBuffer(this)
        }
    }

    class FloatFrameBufferBuilder(width: Int, height: Int) : GLFrameBufferBuilder<com.badlogic.gdx.graphics.glutils.FloatFrameBuffer?>(width, height) {
        override fun build(): com.badlogic.gdx.graphics.glutils.FloatFrameBuffer? {
            return com.badlogic.gdx.graphics.glutils.FloatFrameBuffer(this)
        }
    }

    class FrameBufferCubemapBuilder(width: Int, height: Int) : GLFrameBufferBuilder<com.badlogic.gdx.graphics.glutils.FrameBufferCubemap?>(width, height) {
        override fun build(): com.badlogic.gdx.graphics.glutils.FrameBufferCubemap? {
            return com.badlogic.gdx.graphics.glutils.FrameBufferCubemap(this)
        }
    }

    companion object {
        /** the frame buffers  */
        protected val buffers: MutableMap<com.badlogic.gdx.Application?, com.badlogic.gdx.utils.Array<GLFrameBuffer<*>?>?>? = HashMap<com.badlogic.gdx.Application?, com.badlogic.gdx.utils.Array<GLFrameBuffer<*>?>?>()
        protected const val GL_DEPTH24_STENCIL8_OES = 0x88F0
        /** the default framebuffer handle, a.k.a screen.  */
        protected var defaultFramebufferHandle = 0
        /** true if we have polled for the default handle already.  */
        protected var defaultFramebufferHandleInitialized = false

        /** Unbinds the framebuffer, all drawing will be performed to the normal framebuffer from here on.  */
        fun unbind() {
            com.badlogic.gdx.Gdx.gl20.glBindFramebuffer(com.badlogic.gdx.graphics.GL20.GL_FRAMEBUFFER, defaultFramebufferHandle)
        }

        private fun addManagedFrameBuffer(app: com.badlogic.gdx.Application?, frameBuffer: GLFrameBuffer<*>?) {
            var managedResources: com.badlogic.gdx.utils.Array<GLFrameBuffer<*>?>? = buffers!![app]
            if (managedResources == null) managedResources = com.badlogic.gdx.utils.Array()
            managedResources.add(frameBuffer)
            buffers[app] = managedResources
        }

        /** Invalidates all frame buffers. This can be used when the OpenGL context is lost to rebuild all managed frame buffers. This
         * assumes that the texture attached to this buffer has already been rebuild! Use with care.  */
        fun invalidateAllFrameBuffers(app: com.badlogic.gdx.Application?) {
            if (com.badlogic.gdx.Gdx.gl20 == null) return
            val bufferArray: com.badlogic.gdx.utils.Array<GLFrameBuffer<*>?> = buffers!![app] ?: return
            for (i in 0 until bufferArray.size) {
                bufferArray.get(i).build()
            }
        }

        fun clearAllFrameBuffers(app: com.badlogic.gdx.Application?) {
            buffers!!.remove(app)
        }

        fun getManagedStatus(builder: java.lang.StringBuilder?): java.lang.StringBuilder? {
            builder.append("Managed buffers/app: { ")
            for (app in buffers!!.keys) {
                builder.append(buffers[app].size)
                builder.append(" ")
            }
            builder.append("}")
            return builder
        }

        fun getManagedStatus(): String? {
            return getManagedStatus(java.lang.StringBuilder()).toString()
        }
    }
}
