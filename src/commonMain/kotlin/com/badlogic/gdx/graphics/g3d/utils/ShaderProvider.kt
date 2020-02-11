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

import Texture.TextureFilter
import Texture.TextureWrap
import com.badlogic.gdx.graphics.g3d.utils.TextureDescriptor
import com.badlogic.gdx.graphics.g3d.utils.TextureProvider

/**
 * Returns [Shader] instances for a [Renderable] on request. Also responsible for disposing of any created
 * [ShaderProgram] instances on a call to [.dispose].
 *
 * @author badlogic
 */
interface ShaderProvider {

    /**
     * Returns a [Shader] for the given [Renderable]. The RenderInstance may already contain a Shader, in which case
     * the provider may decide to return that.
     *
     * @param renderable the Renderable
     * @return the Shader to be used for the RenderInstance
     */
    fun getShader(renderable: Renderable?): Shader?

    /**
     * Disposes all resources created by the provider
     */
    fun dispose()
}
