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
package com.badlogic.gdx.graphics.g3d.shaders

class DepthShader(renderable: Renderable, config: Config, shaderProgram: ShaderProgram?) : DefaultShader(renderable, config, shaderProgram) {
    class Config : DefaultShader.Config {
        var depthBufferOnly = false
        var defaultAlphaTest = 0.5f

        constructor() : super() {
            defaultCullFace = GL20.GL_FRONT
        }

        constructor(vertexShader: String?, fragmentShader: String?) : super(vertexShader, fragmentShader) {}
    }

    val numBones: Int
    val weights: Int
    private val alphaTestAttribute: FloatAttribute

    constructor(renderable: Renderable?) : this(renderable, Config()) {}
    constructor(renderable: Renderable?, config: Config) : this(renderable, config, createPrefix(renderable, config)) {}
    constructor(renderable: Renderable?, config: Config, prefix: String) : this(renderable, config, prefix, (if (config.vertexShader != null) config.vertexShader else defaultVertexShader)!!,
        (if (config.fragmentShader != null) config.fragmentShader else defaultFragmentShader)!!) {
    }

    constructor(renderable: Renderable?, config: Config?, prefix: String, vertexShader: String,
                fragmentShader: String) : this(renderable, config, ShaderProgram(prefix + vertexShader, prefix + fragmentShader)) {
    }

    fun begin(camera: Camera?, context: RenderContext?) {
        super.begin(camera, context)
        // Gdx.gl20.glEnable(GL20.GL_POLYGON_OFFSET_FILL);
        // Gdx.gl20.glPolygonOffset(2.f, 100.f);
    }

    fun end() {
        super.end()
        // Gdx.gl20.glDisable(GL20.GL_POLYGON_OFFSET_FILL);
    }

    fun canRender(renderable: Renderable): Boolean {
        val attributes: Attributes = combineAttributes(renderable)
        if (attributes.has(BlendingAttribute.Type)) {
            if (attributesMask and BlendingAttribute.Type !== BlendingAttribute.Type) return false
            if (attributes.has(TextureAttribute.Diffuse) !== (attributesMask and TextureAttribute.Diffuse === TextureAttribute.Diffuse)) return false
        }
        val skinned = renderable.meshPart.mesh.getVertexAttributes().getMask() and Usage.BoneWeight === Usage.BoneWeight
        if (skinned != numBones > 0) return false
        if (!skinned) return true
        var w = 0
        val n: Int = renderable.meshPart.mesh.getVertexAttributes().size()
        for (i in 0 until n) {
            val attr: VertexAttribute = renderable.meshPart.mesh.getVertexAttributes().get(i)
            if (attr.usage === Usage.BoneWeight) w = w or (1 shl attr.unit)
        }
        return w == weights
    }

    fun render(renderable: Renderable?, combinedAttributes: Attributes) {
        if (combinedAttributes.has(BlendingAttribute.Type)) {
            val blending: BlendingAttribute = combinedAttributes.get(BlendingAttribute.Type) as BlendingAttribute
            combinedAttributes.remove(BlendingAttribute.Type)
            val hasAlphaTest: Boolean = combinedAttributes.has(FloatAttribute.AlphaTest)
            if (!hasAlphaTest) combinedAttributes.set(alphaTestAttribute)
            if (blending.opacity >= (combinedAttributes.get(FloatAttribute.AlphaTest) as FloatAttribute).value) super.render(renderable, combinedAttributes)
            if (!hasAlphaTest) combinedAttributes.remove(FloatAttribute.AlphaTest)
            combinedAttributes.set(blending)
        } else super.render(renderable, combinedAttributes)
    }

    companion object {
        var defaultVertexShader: String? = null
            get() {
                if (field == null) field = Gdx.files.classpath("com/badlogic/gdx/graphics/g3d/shaders/depth.vertex.glsl").readString()
                return field
            }
            private set

        var defaultFragmentShader: String? = null
            get() {
                if (field == null) field = Gdx.files.classpath("com/badlogic/gdx/graphics/g3d/shaders/depth.fragment.glsl").readString()
                return field
            }
            private set

        fun createPrefix(renderable: Renderable?, config: Config): String {
            var prefix: String = DefaultShader.createPrefix(renderable, config)
            if (!config.depthBufferOnly) prefix += "#define PackedDepthFlag\n"
            return prefix
        }

        private val tmpAttributes: Attributes = Attributes()

        // TODO: Move responsibility for combining attributes to RenderableProvider
        private fun combineAttributes(renderable: Renderable): Attributes {
            tmpAttributes.clear()
            if (renderable.environment != null) tmpAttributes.set(renderable.environment)
            if (renderable.material != null) tmpAttributes.set(renderable.material)
            return tmpAttributes
        }
    }

    init {
        val attributes: Attributes = combineAttributes(renderable)
        numBones = if (renderable.bones == null) 0 else config.numBones
        var w = 0
        val n: Int = renderable.meshPart.mesh.getVertexAttributes().size()
        for (i in 0 until n) {
            val attr: VertexAttribute = renderable.meshPart.mesh.getVertexAttributes().get(i)
            if (attr.usage === Usage.BoneWeight) w = w or (1 shl attr.unit)
        }
        weights = w
        alphaTestAttribute = FloatAttribute(FloatAttribute.AlphaTest, config.defaultAlphaTest)
    }
}
