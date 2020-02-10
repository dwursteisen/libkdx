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
package com.badlogic.gdx.graphics.g3d.environment

import com.badlogic.gdx.graphics.glutils.FrameBuffer

/** @author Xoppa
 */
@Deprecated("Experimental, likely to change, do not use!\n" + "  ")
class DirectionalShadowLight @Deprecated("Experimental, likely to change, do not use! ") constructor(shadowMapWidth: Int, shadowMapHeight: Int, shadowViewportWidth: Float, shadowViewportHeight: Float,
                                                                                                     shadowNear: Float, shadowFar: Float) : com.badlogic.gdx.graphics.g3d.environment.DirectionalLight(), com.badlogic.gdx.graphics.g3d.environment.ShadowMap, com.badlogic.gdx.utils.Disposable {

    var frameBuffer: FrameBuffer?
        protected set
    protected var cam: com.badlogic.gdx.graphics.Camera
    protected var halfDepth: Float
    protected var halfHeight: Float
    protected val tmpV: com.badlogic.gdx.math.Vector3 = com.badlogic.gdx.math.Vector3()
    protected val textureDesc: com.badlogic.gdx.graphics.g3d.utils.TextureDescriptor<*>
    fun update(camera: com.badlogic.gdx.graphics.Camera) {
        update(tmpV.set(camera.direction).scl(halfHeight), camera.direction)
    }

    fun update(center: com.badlogic.gdx.math.Vector3?, forward: com.badlogic.gdx.math.Vector3?) { // cam.position.set(10,10,10);
        cam.position.set(direction).scl(-halfDepth).add(center)
        cam.direction.set(direction).nor()
        // cam.up.set(forward).nor();
        cam.normalizeUp()
        cam.update()
    }

    fun begin(camera: com.badlogic.gdx.graphics.Camera) {
        update(camera)
        begin()
    }

    fun begin(center: com.badlogic.gdx.math.Vector3?, forward: com.badlogic.gdx.math.Vector3?) {
        update(center, forward)
        begin()
    }

    fun begin() {
        val w = frameBuffer!!.getWidth()
        val h = frameBuffer!!.getHeight()
        frameBuffer!!.begin()
        com.badlogic.gdx.Gdx.gl.glViewport(0, 0, w, h)
        com.badlogic.gdx.Gdx.gl.glClearColor(1f, 1f, 1f, 1f)
        com.badlogic.gdx.Gdx.gl.glClear(com.badlogic.gdx.graphics.GL20.GL_COLOR_BUFFER_BIT or com.badlogic.gdx.graphics.GL20.GL_DEPTH_BUFFER_BIT)
        com.badlogic.gdx.Gdx.gl.glEnable(com.badlogic.gdx.graphics.GL20.GL_SCISSOR_TEST)
        com.badlogic.gdx.Gdx.gl.glScissor(1, 1, w - 2, h - 2)
    }

    fun end() {
        com.badlogic.gdx.Gdx.gl.glDisable(com.badlogic.gdx.graphics.GL20.GL_SCISSOR_TEST)
        frameBuffer!!.end()
    }

    val camera: com.badlogic.gdx.graphics.Camera
        get() = cam

    override val projViewTrans: com.badlogic.gdx.math.Matrix4?
        get() = cam.combined

    override val depthMap: com.badlogic.gdx.graphics.g3d.utils.TextureDescriptor<*>
        get() {
            textureDesc.texture = frameBuffer!!.getColorBufferTexture()
            return textureDesc
        }

    override fun dispose() {
        if (frameBuffer != null) frameBuffer!!.dispose()
        frameBuffer = null
    }

    init {
        frameBuffer = FrameBuffer(com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888, shadowMapWidth, shadowMapHeight, true)
        cam = com.badlogic.gdx.graphics.OrthographicCamera(shadowViewportWidth, shadowViewportHeight)
        cam.near = shadowNear
        cam.far = shadowFar
        halfHeight = shadowViewportHeight * 0.5f
        halfDepth = shadowNear + 0.5f * (shadowFar - shadowNear)
        textureDesc = com.badlogic.gdx.graphics.g3d.utils.TextureDescriptor()
        textureDesc.magFilter = com.badlogic.gdx.graphics.Texture.TextureFilter.Nearest
        textureDesc.minFilter = textureDesc.magFilter
        textureDesc.vWrap = com.badlogic.gdx.graphics.Texture.TextureWrap.ClampToEdge
        textureDesc.uWrap = textureDesc.vWrap
    }
}
