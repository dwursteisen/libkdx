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

import com.badlogic.gdx.graphics.g3d.utils.AnimationController.AnimationDesc
import com.badlogic.gdx.graphics.g3d.utils.AnimationController.AnimationListener
import com.badlogic.gdx.graphics.g3d.utils.BaseAnimationController
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController.CameraGestureListener
import com.badlogic.gdx.graphics.g3d.utils.DefaultTextureBinder

abstract class BaseShaderProvider : ShaderProvider {
    protected var shaders: Array<Shader?>? = Array<Shader?>()
    fun getShader(renderable: Renderable?): Shader? {
        val suggestedShader: Shader = renderable.shader
        if (suggestedShader != null && suggestedShader.canRender(renderable)) return suggestedShader
        for (shader in shaders!!) {
            if (shader.canRender(renderable)) return shader
        }
        val shader: Shader? = createShader(renderable)
        shader.init()
        shaders.add(shader)
        return shader
    }

    protected abstract fun createShader(renderable: Renderable?): Shader?
    fun dispose() {
        for (shader in shaders!!) {
            shader.dispose()
        }
        shaders.clear()
    }
}
