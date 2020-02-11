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

/**
 * @author Xoppa A BaseShader is a wrapper around a ShaderProgram that keeps track of the uniform and attribute locations. It does
 * not manage the ShaderPogram, you are still responsible for disposing the ShaderProgram.
 */
abstract class BaseShader : Shader {

    interface Validator {
        /**
         * @return True if the input is valid for the renderable, false otherwise.
         */
        fun validate(shader: BaseShader?, inputID: Int, renderable: Renderable?): Boolean
    }

    interface Setter {
        /**
         * @return True if the uniform only has to be set once per render call, false if the uniform must be set for each renderable.
         */
        fun isGlobal(shader: BaseShader?, inputID: Int): Boolean
        operator fun set(shader: BaseShader?, inputID: Int, renderable: Renderable?, combinedAttributes: Attributes?)
    }

    abstract class GlobalSetter : Setter {
        override fun isGlobal(shader: BaseShader?, inputID: Int): Boolean {
            return true
        }
    }

    abstract class LocalSetter : Setter {
        override fun isGlobal(shader: BaseShader?, inputID: Int): Boolean {
            return false
        }
    }

    class Uniform @JvmOverloads constructor(val alias: String?, val materialMask: Long = 0, val environmentMask: Long = 0, val overallMask: Long = 0) : Validator {

        constructor(alias: String?, overallMask: Long) : this(alias, 0, 0, overallMask) {}

        override fun validate(shader: BaseShader?, inputID: Int, renderable: Renderable?): Boolean {
            val matFlags = if (renderable != null && renderable.material != null) renderable.material.getMask() else 0.toLong()
            val envFlags = if (renderable != null && renderable.environment != null) renderable.environment.getMask() else 0.toLong()
            return (matFlags and materialMask == materialMask && envFlags and environmentMask == environmentMask
                && matFlags or envFlags and overallMask == overallMask)
        }
    }

    private val uniforms = Array<String>()
    private val validators = Array<Validator?>()
    private val setters = Array<Setter?>()
    private var locations: IntArray?
    private val globalUniforms: IntArray = IntArray()
    private val localUniforms: IntArray = IntArray()
    private val attributes: IntIntMap = IntIntMap()
    var program: ShaderProgram? = null
    var context: RenderContext? = null
    var camera: Camera? = null
    private var currentMesh: Mesh? = null

    /**
     * Register an uniform which might be used by this shader. Only possible prior to the call to init().
     *
     * @return The ID of the uniform to use in this shader.
     */
    @JvmOverloads
    fun register(alias: String?, validator: Validator? = null, setter: Setter? = null): Int {
        if (locations != null) throw GdxRuntimeException("Cannot register an uniform after initialization")
        val existing = getUniformID(alias)
        if (existing >= 0) {
            validators[existing] = validator
            setters[existing] = setter
            return existing
        }
        uniforms.add(alias)
        validators.add(validator)
        setters.add(setter)
        return uniforms.size - 1
    }

    fun register(alias: String?, setter: Setter?): Int {
        return register(alias, null, setter)
    }

    @JvmOverloads
    fun register(uniform: Uniform, setter: Setter? = null): Int {
        return register(uniform.alias, uniform, setter)
    }

    /**
     * @return the ID of the input or negative if not available.
     */
    fun getUniformID(alias: String?): Int {
        val n = uniforms.size
        for (i in 0 until n) if (uniforms[i].equals(alias)) return i
        return -1
    }

    /**
     * @return The input at the specified id.
     */
    fun getUniformAlias(id: Int): String {
        return uniforms[id]
    }

    /**
     * Initialize this shader, causing all registered uniforms/attributes to be fetched.
     */
    fun init(program: ShaderProgram, renderable: Renderable?) {
        if (locations != null) throw GdxRuntimeException("Already initialized")
        if (!program.isCompiled()) throw GdxRuntimeException(program.getLog())
        this.program = program
        val n = uniforms.size
        locations = IntArray(n)
        for (i in 0 until n) {
            val input = uniforms[i]
            val validator = validators[i]
            val setter = setters[i]
            if (validator != null && !validator.validate(this, i, renderable)) locations!![i] = -1 else {
                locations!![i] = program.fetchUniformLocation(input, false)
                if (locations!![i] >= 0 && setter != null) {
                    if (setter.isGlobal(this, i)) globalUniforms.add(i) else localUniforms.add(i)
                }
            }
            if (locations!![i] < 0) {
                validators[i] = null
                setters[i] = null
            }
        }
        if (renderable != null) {
            val attrs: VertexAttributes = renderable.meshPart.mesh.getVertexAttributes()
            val c: Int = attrs.size()
            for (i in 0 until c) {
                val attr: VertexAttribute = attrs.get(i)
                val location: Int = program.getAttributeLocation(attr.alias)
                if (location >= 0) attributes.put(attr.getKey(), location)
            }
        }
    }

    fun begin(camera: Camera?, context: RenderContext?) {
        this.camera = camera
        this.context = context
        program.begin()
        currentMesh = null
        var u: Int
        var i = 0
        while (i < globalUniforms.size) {
            if (setters[globalUniforms[i].also { u = it }] != null) setters[u]!![this, u, null] = null
            ++i
        }
    }

    private val tempArray: IntArray = IntArray()
    private fun getAttributeLocations(attrs: VertexAttributes): IntArray {
        tempArray.clear()
        val n: Int = attrs.size()
        for (i in 0 until n) {
            tempArray.add(attributes.get(attrs.get(i).getKey(), -1))
        }
        tempArray.shrink()
        return tempArray.items
    }

    private val combinedAttributes: Attributes = Attributes()
    fun render(renderable: Renderable) {
        if (renderable.worldTransform.det3x3() === 0) return
        combinedAttributes.clear()
        if (renderable.environment != null) combinedAttributes.set(renderable.environment)
        if (renderable.material != null) combinedAttributes.set(renderable.material)
        render(renderable, combinedAttributes)
    }

    fun render(renderable: Renderable, combinedAttributes: Attributes?) {
        var u: Int
        var i = 0
        while (i < localUniforms.size) {
            if (setters[localUniforms[i].also { u = it }] != null) setters[u]!![this, u, renderable] = combinedAttributes
            ++i
        }
        if (currentMesh !== renderable.meshPart.mesh) {
            if (currentMesh != null) currentMesh.unbind(program, tempArray.items)
            currentMesh = renderable.meshPart.mesh
            currentMesh.bind(program, getAttributeLocations(renderable.meshPart.mesh.getVertexAttributes()))
        }
        renderable.meshPart.render(program, false)
    }

    fun end() {
        if (currentMesh != null) {
            currentMesh.unbind(program, tempArray.items)
            currentMesh = null
        }
        program.end()
    }

    fun dispose() {
        program = null
        uniforms.clear()
        validators.clear()
        setters.clear()
        localUniforms.clear()
        globalUniforms.clear()
        locations = null
    }

    /**
     * Whether this Shader instance implements the specified uniform, only valid after a call to init().
     */
    fun has(inputID: Int): Boolean {
        return inputID >= 0 && inputID < locations!!.size && locations!![inputID] >= 0
    }

    fun loc(inputID: Int): Int {
        return if (inputID >= 0 && inputID < locations!!.size) locations!![inputID] else -1
    }

    operator fun set(uniform: Int, value: Matrix4?): Boolean {
        if (locations!![uniform] < 0) return false
        program.setUniformMatrix(locations!![uniform], value)
        return true
    }

    operator fun set(uniform: Int, value: Matrix3?): Boolean {
        if (locations!![uniform] < 0) return false
        program.setUniformMatrix(locations!![uniform], value)
        return true
    }

    operator fun set(uniform: Int, value: Vector3?): Boolean {
        if (locations!![uniform] < 0) return false
        program.setUniformf(locations!![uniform], value)
        return true
    }

    operator fun set(uniform: Int, value: Vector2?): Boolean {
        if (locations!![uniform] < 0) return false
        program.setUniformf(locations!![uniform], value)
        return true
    }

    operator fun set(uniform: Int, value: Color?): Boolean {
        if (locations!![uniform] < 0) return false
        program.setUniformf(locations!![uniform], value)
        return true
    }

    operator fun set(uniform: Int, value: Float): Boolean {
        if (locations!![uniform] < 0) return false
        program.setUniformf(locations!![uniform], value)
        return true
    }

    operator fun set(uniform: Int, v1: Float, v2: Float): Boolean {
        if (locations!![uniform] < 0) return false
        program.setUniformf(locations!![uniform], v1, v2)
        return true
    }

    operator fun set(uniform: Int, v1: Float, v2: Float, v3: Float): Boolean {
        if (locations!![uniform] < 0) return false
        program.setUniformf(locations!![uniform], v1, v2, v3)
        return true
    }

    operator fun set(uniform: Int, v1: Float, v2: Float, v3: Float, v4: Float): Boolean {
        if (locations!![uniform] < 0) return false
        program.setUniformf(locations!![uniform], v1, v2, v3, v4)
        return true
    }

    operator fun set(uniform: Int, value: Int): Boolean {
        if (locations!![uniform] < 0) return false
        program.setUniformi(locations!![uniform], value)
        return true
    }

    operator fun set(uniform: Int, v1: Int, v2: Int): Boolean {
        if (locations!![uniform] < 0) return false
        program.setUniformi(locations!![uniform], v1, v2)
        return true
    }

    operator fun set(uniform: Int, v1: Int, v2: Int, v3: Int): Boolean {
        if (locations!![uniform] < 0) return false
        program.setUniformi(locations!![uniform], v1, v2, v3)
        return true
    }

    operator fun set(uniform: Int, v1: Int, v2: Int, v3: Int, v4: Int): Boolean {
        if (locations!![uniform] < 0) return false
        program.setUniformi(locations!![uniform], v1, v2, v3, v4)
        return true
    }

    operator fun set(uniform: Int, textureDesc: TextureDescriptor?): Boolean {
        if (locations!![uniform] < 0) return false
        program.setUniformi(locations!![uniform], context.textureBinder.bind(textureDesc))
        return true
    }

    operator fun set(uniform: Int, texture: GLTexture?): Boolean {
        if (locations!![uniform] < 0) return false
        program.setUniformi(locations!![uniform], context.textureBinder.bind(texture))
        return true
    }
}
