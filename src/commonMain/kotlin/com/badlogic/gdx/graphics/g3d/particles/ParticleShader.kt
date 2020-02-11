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
package com.badlogic.gdx.graphics.g3d.particles

import com.badlogic.gdx.graphics.g3d.particles.ParticleSorter
import com.badlogic.gdx.graphics.g3d.particles.ParticleSystem

/**
 * This is a custom shader to render the particles. Usually is not required, because the [DefaultShader] will be used
 * instead. This shader will be used when dealing with billboards using GPU mode or point sprites.
 *
 * @author inferno
 */
class ParticleShader(renderable: Renderable, protected val config: Config, shaderProgram: ShaderProgram) : BaseShader() {

    enum class ParticleType {
        Billboard, Point
    }

    enum class AlignMode {
        Screen, ViewPoint // , ParticleDirection
    }

    class Config {
        /**
         * The uber vertex shader to use, null to use the default vertex shader.
         */
        var vertexShader: String? = null

        /**
         * The uber fragment shader to use, null to use the default fragment shader.
         */
        var fragmentShader: String? = null
        var ignoreUnimplemented = true

        /**
         * Set to 0 to disable culling
         */
        var defaultCullFace = -1

        /**
         * Set to 0 to disable depth test
         */
        var defaultDepthFunc = -1
        var align = AlignMode.Screen
        var type = ParticleType.Billboard

        constructor() {}
        constructor(align: AlignMode, type: ParticleType) {
            this.align = align
            this.type = type
        }

        constructor(align: AlignMode) {
            this.align = align
        }

        constructor(type: ParticleType) {
            this.type = type
        }

        constructor(vertexShader: String?, fragmentShader: String?) {
            this.vertexShader = vertexShader
            this.fragmentShader = fragmentShader
        }
    }

    object Inputs {
        val cameraRight: Uniform = Uniform("u_cameraRight")
        val cameraInvDirection: Uniform = Uniform("u_cameraInvDirection")
        val screenWidth: Uniform = Uniform("u_screenWidth")
        val regionSize: Uniform = Uniform("u_regionSize")
    }

    object Setters {
        val cameraRight: Setter = object : Setter() {
            fun isGlobal(shader: BaseShader?, inputID: Int): Boolean {
                return true
            }

            operator fun set(shader: BaseShader, inputID: Int, renderable: Renderable?, combinedAttributes: Attributes?) {
                shader.set(inputID, TMP_VECTOR3.set(shader.camera.direction).crs(shader.camera.up).nor())
            }
        }
        val cameraUp: Setter = object : Setter() {
            fun isGlobal(shader: BaseShader?, inputID: Int): Boolean {
                return true
            }

            operator fun set(shader: BaseShader, inputID: Int, renderable: Renderable?, combinedAttributes: Attributes?) {
                shader.set(inputID, TMP_VECTOR3.set(shader.camera.up).nor())
            }
        }
        val cameraInvDirection: Setter = object : Setter() {
            fun isGlobal(shader: BaseShader?, inputID: Int): Boolean {
                return true
            }

            operator fun set(shader: BaseShader, inputID: Int, renderable: Renderable?, combinedAttributes: Attributes?) {
                shader.set(inputID,
                    TMP_VECTOR3.set(-shader.camera.direction.x, -shader.camera.direction.y, -shader.camera.direction.z).nor())
            }
        }
        val cameraPosition: Setter = object : Setter() {
            fun isGlobal(shader: BaseShader?, inputID: Int): Boolean {
                return true
            }

            operator fun set(shader: BaseShader, inputID: Int, renderable: Renderable?, combinedAttributes: Attributes?) {
                shader.set(inputID, shader.camera.position)
            }
        }
        val screenWidth: Setter = object : Setter() {
            fun isGlobal(shader: BaseShader?, inputID: Int): Boolean {
                return true
            }

            operator fun set(shader: BaseShader, inputID: Int, renderable: Renderable?, combinedAttributes: Attributes?) {
                shader.set(inputID, Gdx.graphics.getWidth() as Float)
            }
        }
        val worldViewTrans: Setter = object : Setter() {
            val temp: Matrix4 = Matrix4()
            fun isGlobal(shader: BaseShader?, inputID: Int): Boolean {
                return false
            }

            operator fun set(shader: BaseShader, inputID: Int, renderable: Renderable, combinedAttributes: Attributes?) {
                shader.set(inputID, temp.set(shader.camera.view).mul(renderable.worldTransform))
            }
        }
    }

    /**
     * The renderable used to create this shader, invalid after the call to init
     */
    private var renderable: Renderable?
    private val materialMask: Long
    private val vertexMask: Long

    constructor(renderable: Renderable?) : this(renderable, Config()) {}
    constructor(renderable: Renderable?, config: Config) : this(renderable, config, createPrefix(renderable, config)) {}
    constructor(renderable: Renderable?, config: Config, prefix: String) : this(renderable, config, prefix, if (config.vertexShader != null) config.vertexShader else defaultVertexShader,
        if (config.fragmentShader != null) config.fragmentShader else defaultFragmentShader) {
    }

    constructor(renderable: Renderable?, config: Config?, prefix: String, vertexShader: String?,
                fragmentShader: String?) : this(renderable, config, ShaderProgram(prefix + vertexShader, prefix + fragmentShader)) {
    }

    fun init() {
        var program: ShaderProgram = this.program
        program = null
        init(program, renderable)
        renderable = null
    }

    fun canRender(renderable: Renderable): Boolean {
        return (materialMask == renderable.material.getMask() or optionalAttributes
            && vertexMask == renderable.meshPart.mesh.getVertexAttributes().getMask())
    }

    operator fun compareTo(other: Shader?): Int {
        if (other == null) return -1
        return if (other === this) 0 else 0
        // FIXME compare shaders on their impact on performance
    }

    override fun equals(obj: Any?): Boolean {
        return obj is ParticleShader && equals(obj)
    }

    fun equals(obj: ParticleShader): Boolean {
        return obj === this
    }

    fun begin(camera: Camera?, context: RenderContext?) {
        super.begin(camera, context)
    }

    fun render(renderable: Renderable) {
        if (!renderable.material.has(BlendingAttribute.Type)) context.setBlending(false, GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        bindMaterial(renderable)
        super.render(renderable)
    }

    fun end() {
        currentMaterial = null
        super.end()
    }

    var currentMaterial: Material? = null
    protected fun bindMaterial(renderable: Renderable) {
        if (currentMaterial === renderable.material) return
        val cullFace = if (config.defaultCullFace == -1) GL20.GL_BACK else config.defaultCullFace
        var depthFunc = if (config.defaultDepthFunc == -1) GL20.GL_LEQUAL else config.defaultDepthFunc
        var depthRangeNear = 0f
        var depthRangeFar = 1f
        var depthMask = true
        currentMaterial = renderable.material
        for (attr in currentMaterial) {
            val t: Long = attr.type
            if (BlendingAttribute.`is`(t)) {
                context.setBlending(true, (attr as BlendingAttribute).sourceFunction, (attr as BlendingAttribute).destFunction)
            } else if (t and DepthTestAttribute.Type === DepthTestAttribute.Type) {
                val dta: DepthTestAttribute = attr as DepthTestAttribute
                depthFunc = dta.depthFunc
                depthRangeNear = dta.depthRangeNear
                depthRangeFar = dta.depthRangeFar
                depthMask = dta.depthMask
            } else if (!config.ignoreUnimplemented) throw GdxRuntimeException("Unknown material attribute: " + attr.toString())
        }
        context.setCullFace(cullFace)
        context.setDepthTest(depthFunc, depthRangeNear, depthRangeFar)
        context.setDepthMask(depthMask)
    }

    fun dispose() {
        program.dispose()
        super.dispose()
    }

    var defaultCullFace: Int
        get() = if (config.defaultCullFace == -1) GL20.GL_BACK else config.defaultCullFace
        set(cullFace) {
            config.defaultCullFace = cullFace
        }

    var defaultDepthFunc: Int
        get() = if (config.defaultDepthFunc == -1) GL20.GL_LEQUAL else config.defaultDepthFunc
        set(depthFunc) {
            config.defaultDepthFunc = depthFunc
        }

    companion object {
        var defaultVertexShader: String? = null
            get() {
                if (field == null) field = Gdx.files.classpath("com/badlogic/gdx/graphics/g3d/particles/particles.vertex.glsl").readString()
                return field
            }
            private set

        var defaultFragmentShader: String? = null
            get() {
                if (field == null) field = Gdx.files.classpath("com/badlogic/gdx/graphics/g3d/particles/particles.fragment.glsl")
                    .readString()
                return field
            }
            private set

        protected var implementedFlags: Long = BlendingAttribute.Type or TextureAttribute.Diffuse
        val TMP_VECTOR3: Vector3 = Vector3()

        /**
         * Material attributes which are not required but always supported.
         */
        private val optionalAttributes: Long = IntAttribute.CullFace or DepthTestAttribute.Type
        fun createPrefix(renderable: Renderable?, config: Config): String {
            var prefix = ""
            prefix += if (Gdx.app.getType() === ApplicationType.Desktop) "#version 120\n" else "#version 100\n"
            if (config.type == ParticleType.Billboard) {
                prefix += "#define billboard\n"
                if (config.align == AlignMode.Screen) prefix += "#define screenFacing\n" else if (config.align == AlignMode.ViewPoint) prefix += "#define viewPointFacing\n"
                // else if(config.align == AlignMode.ParticleDirection)
                // prefix += "#define paticleDirectionFacing\n";
            }
            return prefix
        }
    }

    init {
        this.program = shaderProgram
        this.renderable = renderable
        materialMask = renderable.material.getMask() or optionalAttributes
        vertexMask = renderable.meshPart.mesh.getVertexAttributes().getMask()
        if (!config.ignoreUnimplemented && implementedFlags and materialMask != materialMask) throw GdxRuntimeException("Some attributes not implemented yet ($materialMask)")

        // Global uniforms
        register(DefaultShader.Inputs.viewTrans, DefaultShader.Setters.viewTrans)
        register(DefaultShader.Inputs.projViewTrans, DefaultShader.Setters.projViewTrans)
        register(DefaultShader.Inputs.projTrans, DefaultShader.Setters.projTrans)
        register(Inputs.screenWidth, Setters.screenWidth)
        register(DefaultShader.Inputs.cameraUp, Setters.cameraUp)
        register(Inputs.cameraRight, Setters.cameraRight)
        register(Inputs.cameraInvDirection, Setters.cameraInvDirection)
        register(DefaultShader.Inputs.cameraPosition, Setters.cameraPosition)

        // Object uniforms
        register(DefaultShader.Inputs.diffuseTexture, DefaultShader.Setters.diffuseTexture)
    }
}
