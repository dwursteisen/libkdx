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
package com.badlogic.gdx.graphics.g3d.utils

/**
 * Manages OpenGL state and tries to reduce state changes. Uses a [TextureBinder] to reduce texture binds as well. Call
 * [.begin] to setup the context, call [.end] to undo all state changes. Use the setters to change state, use
 * [.textureBinder] to bind textures.
 *
 * @author badlogic, Xoppa
 */
class RenderContext(
    /**
     * used to bind textures
     */
    val textureBinder: TextureBinder) {

    private var blending = false
    private var blendSFactor = 0
    private var blendDFactor = 0
    private var depthFunc = 0
    private var depthRangeNear = 0f
    private var depthRangeFar = 0f
    private var depthMask = false
    private var cullFace = 0

    /**
     * Sets up the render context, must be matched with a call to [.end].
     */
    fun begin() {
        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST)
        depthFunc = 0
        Gdx.gl.glDepthMask(true)
        depthMask = true
        Gdx.gl.glDisable(GL20.GL_BLEND)
        blending = false
        Gdx.gl.glDisable(GL20.GL_CULL_FACE)
        blendDFactor = 0
        blendSFactor = blendDFactor
        cullFace = blendSFactor
        textureBinder.begin()
    }

    /**
     * Resets all changed OpenGL states to their defaults.
     */
    fun end() {
        if (depthFunc != 0) Gdx.gl.glDisable(GL20.GL_DEPTH_TEST)
        if (!depthMask) Gdx.gl.glDepthMask(true)
        if (blending) Gdx.gl.glDisable(GL20.GL_BLEND)
        if (cullFace > 0) Gdx.gl.glDisable(GL20.GL_CULL_FACE)
        textureBinder.end()
    }

    fun setDepthMask(depthMask: Boolean) {
        if (this.depthMask != depthMask) Gdx.gl.glDepthMask(depthMask.also { this.depthMask = it })
    }

    fun setDepthTest(depthFunction: Int) {
        setDepthTest(depthFunction, 0f, 1f)
    }

    fun setDepthTest(depthFunction: Int, depthRangeNear: Float, depthRangeFar: Float) {
        val wasEnabled = depthFunc != 0
        val enabled = depthFunction != 0
        if (depthFunc != depthFunction) {
            depthFunc = depthFunction
            if (enabled) {
                Gdx.gl.glEnable(GL20.GL_DEPTH_TEST)
                Gdx.gl.glDepthFunc(depthFunction)
            } else Gdx.gl.glDisable(GL20.GL_DEPTH_TEST)
        }
        if (enabled) {
            if (!wasEnabled || depthFunc != depthFunction) Gdx.gl.glDepthFunc(depthFunction.also { depthFunc = it })
            if (!wasEnabled || this.depthRangeNear != depthRangeNear || this.depthRangeFar != depthRangeFar) Gdx.gl.glDepthRangef(depthRangeNear.also { this.depthRangeNear = it }, depthRangeFar.also { this.depthRangeFar = it })
        }
    }

    fun setBlending(enabled: Boolean, sFactor: Int, dFactor: Int) {
        if (enabled != blending) {
            blending = enabled
            if (enabled) Gdx.gl.glEnable(GL20.GL_BLEND) else Gdx.gl.glDisable(GL20.GL_BLEND)
        }
        if (enabled && (blendSFactor != sFactor || blendDFactor != dFactor)) {
            Gdx.gl.glBlendFunc(sFactor, dFactor)
            blendSFactor = sFactor
            blendDFactor = dFactor
        }
    }

    fun setCullFace(face: Int) {
        if (face != cullFace) {
            cullFace = face
            if (face == GL20.GL_FRONT || face == GL20.GL_BACK || face == GL20.GL_FRONT_AND_BACK) {
                Gdx.gl.glEnable(GL20.GL_CULL_FACE)
                Gdx.gl.glCullFace(face)
            } else Gdx.gl.glDisable(GL20.GL_CULL_FACE)
        }
    }
}
