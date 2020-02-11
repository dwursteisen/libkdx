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
package com.badlogic.gdx.graphics.g3d

import com.badlogic.gdx.graphics.g3d.ModelCache.MeshPool
import com.badlogic.gdx.graphics.g3d.ModelCache.SimpleMeshPool
import com.badlogic.gdx.graphics.g3d.ModelInstance

/**
 * Interface which is used to render one or more [Renderable]s.
 *
 *
 * A Shader is responsible for the actual rendering of an [Renderable]. Typically, when using OpenGL ES 2.0 or higher, it
 * encapsulates a [ShaderProgram] and takes care of all OpenGL calls necessary to render the [Renderable]. When using
 * OpenGL ES 1.x it takes care of the fixed pipeline.
 *
 *
 * To start rendering the [.begin] method must be called. After which the [.end] method must
 * be called to stop rendering. In between one or more calls to the [.render] method can be made to render a
 * [Renderable]. The [.render] method must not be called before a call to
 * [.begin] or after a call to [.end]. Each Shader needs exclusive access to the OpenGL state
 * and [RenderContext] between the [.begin] and [.end] methods, therefore only one
 * shader can be used at a time (they must not be nested).
 *
 *
 * A specific Shader instance might be (and usually is) dedicated to a specific type of [Renderable]. For example it might
 * use a [ShaderProgram] that is compiled with uniforms (shader input) for specific [Attribute] types. Therefore the
 * [.canRender] method can be used to check if the Shader instance can be used for a specific [Renderable]
 * . Rendering a [Renderable] using a Shader for which [.canRender] returns false might result in
 * unpredicted behavior or crash the application.
 *
 *
 * To manage multiple shaders and create a new shader when required, a [ShaderProvider] can be used. Therefore, in practice,
 * a specific Shader implementation is usually accompanied by a specific [ShaderProvider] implementation (usually extending
 * [BaseShaderProvider]).
 *
 *
 * When a Shader is constructed, the [.init] method must be called before it can be used. Most commonly, the
 * [.init] method compiles the [ShaderProgram], fetches uniform locations and performs other preparations for usage
 * of the Shader. When the shader is no longer needed, it must disposed using the [Disposable.dispose] method. This, for
 * example, disposed (unloads for memory) the used [ShaderProgram].
 *
 * @author Xoppa
 */
interface Shader : Disposable {

    /**
     * Initializes the Shader, must be called before the Shader can be used. This typically compiles a [ShaderProgram],
     * fetches uniform locations and performs other preparations for usage of the Shader.
     */
    fun init()

    /**
     * Compare this shader against the other, used for sorting, light weight shaders are rendered first.
     */
    operator fun compareTo(other: Shader?): Int // TODO: probably better to add some weight value to sort on

    /**
     * Checks whether this shader is intended to render the [Renderable]. Use this to make sure a call to the
     * [.render] method will succeed. This is expected to be a fast, non-blocking method. Note that this method
     * will only return true if it is intended to be used. Even when it returns false the Shader might still be capable of
     * rendering, but it's not preferred to do so.
     *
     * @param instance The renderable to check against this shader.
     * @return true if this shader is intended to render the [Renderable], false otherwise.
     */
    fun canRender(instance: Renderable?): Boolean

    /**
     * Initializes the context for exclusive rendering by this shader. Use the [.render] method to render a
     * [Renderable]. When done rendering the [.end] method must be called.
     *
     * @param camera  The camera to use when rendering
     * @param context The context to be used, which must be exclusive available for the shader until the call to the [.end]
     * method.
     */
    fun begin(camera: Camera?, context: RenderContext?)

    /**
     * Renders the [Renderable], must be called between [.begin] and [.end]. The Shader
     * instance might not be able to render every type of [Renderable]s. Use the [.canRender] method to
     * check if the Shader is capable of rendering a specific [Renderable].
     *
     * @param renderable The renderable to render, all required fields (e.g. [Renderable.material] and others) must be set.
     * The [Renderable.shader] field will be ignored.
     */
    fun render(renderable: Renderable?)

    /**
     * Cleanup the context so other shaders can render. Must be called when done rendering using the [.render]
     * method, which must be preceded by a call to [.begin]. After a call to this method an call to
     * the [.render] method will fail until the [.begin] is called.
     */
    fun end()
}
