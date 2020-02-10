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

import com.badlogic.gdx.files.FileHandle
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
 * A shader program encapsulates a vertex and fragment shader pair linked to form a shader program.
 *
 *
 *
 *
 * After construction a ShaderProgram can be used to draw [Mesh]. To make the GPU use a specific ShaderProgram the programs
 * [ShaderProgram.begin] method must be used which effectively binds the program.
 *
 *
 *
 *
 * When a ShaderProgram is bound one can set uniforms, vertex attributes and attributes as needed via the respective methods.
 *
 *
 *
 *
 * A ShaderProgram can be unbound with a call to [ShaderProgram.end]
 *
 *
 *
 *
 * A ShaderProgram must be disposed via a call to [ShaderProgram.dispose] when it is no longer needed
 *
 *
 *
 *
 * ShaderPrograms are managed. In case the OpenGL context is lost all shaders get invalidated and have to be reloaded. This
 * happens on Android when a user switches to another application or receives an incoming call. Managed ShaderPrograms are
 * automatically reloaded when the OpenGL context is recreated so you don't have to do this manually.
 *
 *
 * @author mzechner
 */
class ShaderProgram(vertexShader: String?, fragmentShader: String?) : com.badlogic.gdx.utils.Disposable {

    /** the log  */
    private var log: String? = ""
    /** whether this program compiled successfully  */
    private var isCompiled = false
    /** uniform lookup  */
    private val uniforms: com.badlogic.gdx.utils.ObjectIntMap<String?>? = com.badlogic.gdx.utils.ObjectIntMap()
    /** uniform types  */
    private val uniformTypes: com.badlogic.gdx.utils.ObjectIntMap<String?>? = com.badlogic.gdx.utils.ObjectIntMap()
    /** uniform sizes  */
    private val uniformSizes: com.badlogic.gdx.utils.ObjectIntMap<String?>? = com.badlogic.gdx.utils.ObjectIntMap()
    /** uniform names  */
    private var uniformNames: Array<String?>?
    /** attribute lookup  */
    private val attributes: com.badlogic.gdx.utils.ObjectIntMap<String?>? = com.badlogic.gdx.utils.ObjectIntMap()
    /** attribute types  */
    private val attributeTypes: com.badlogic.gdx.utils.ObjectIntMap<String?>? = com.badlogic.gdx.utils.ObjectIntMap()
    /** attribute sizes  */
    private val attributeSizes: com.badlogic.gdx.utils.ObjectIntMap<String?>? = com.badlogic.gdx.utils.ObjectIntMap()
    /** attribute names  */
    private var attributeNames: Array<String?>?
    /** program handle  */
    private var program = 0
    /** vertex shader handle  */
    private var vertexShaderHandle = 0
    /** fragment shader handle  */
    private var fragmentShaderHandle = 0
    /** matrix float buffer  */
    private val matrix: FloatBuffer?
    /** vertex shader source  */
    private val vertexShaderSource: String?
    /** fragment shader source  */
    private val fragmentShaderSource: String?
    /** whether this shader was invalidated  */
    private var invalidated = false
    /** reference count  */
    private val refCount = 0

    constructor(vertexShader: FileHandle?, fragmentShader: FileHandle?) : this(vertexShader!!.readString(), fragmentShader!!.readString()) {}

    /** Loads and compiles the shaders, creates a new program and links the shaders.
     *
     * @param vertexShader
     * @param fragmentShader
     */
    private fun compileShaders(vertexShader: String?, fragmentShader: String?) {
        vertexShaderHandle = loadShader(com.badlogic.gdx.graphics.GL20.GL_VERTEX_SHADER, vertexShader)
        fragmentShaderHandle = loadShader(com.badlogic.gdx.graphics.GL20.GL_FRAGMENT_SHADER, fragmentShader)
        if (vertexShaderHandle == -1 || fragmentShaderHandle == -1) {
            isCompiled = false
            return
        }
        program = linkProgram(createProgram())
        if (program == -1) {
            isCompiled = false
            return
        }
        isCompiled = true
    }

    private fun loadShader(type: Int, source: String?): Int {
        val gl: com.badlogic.gdx.graphics.GL20 = com.badlogic.gdx.Gdx.gl20
        val intbuf: java.nio.IntBuffer = com.badlogic.gdx.utils.BufferUtils.newIntBuffer(1)
        val shader: Int = gl.glCreateShader(type)
        if (shader == 0) return -1
        gl.glShaderSource(shader, source)
        gl.glCompileShader(shader)
        gl.glGetShaderiv(shader, com.badlogic.gdx.graphics.GL20.GL_COMPILE_STATUS, intbuf)
        val compiled: Int = intbuf.get(0)
        if (compiled == 0) { // gl.glGetShaderiv(shader, GL20.GL_INFO_LOG_LENGTH, intbuf);
// int infoLogLength = intbuf.get(0);
// if (infoLogLength > 1) {
            val infoLog: String = gl.glGetShaderInfoLog(shader)
            log += if (type == com.badlogic.gdx.graphics.GL20.GL_VERTEX_SHADER) "Vertex shader\n" else "Fragment shader:\n"
            log += infoLog
            // }
            return -1
        }
        return shader
    }

    protected fun createProgram(): Int {
        val gl: com.badlogic.gdx.graphics.GL20 = com.badlogic.gdx.Gdx.gl20
        val program: Int = gl.glCreateProgram()
        return if (program != 0) program else -1
    }

    private fun linkProgram(program: Int): Int {
        val gl: com.badlogic.gdx.graphics.GL20 = com.badlogic.gdx.Gdx.gl20
        if (program == -1) return -1
        gl.glAttachShader(program, vertexShaderHandle)
        gl.glAttachShader(program, fragmentShaderHandle)
        gl.glLinkProgram(program)
        val tmp: java.nio.ByteBuffer = java.nio.ByteBuffer.allocateDirect(4)
        tmp.order(ByteOrder.nativeOrder())
        val intbuf: java.nio.IntBuffer = tmp.asIntBuffer()
        gl.glGetProgramiv(program, com.badlogic.gdx.graphics.GL20.GL_LINK_STATUS, intbuf)
        val linked: Int = intbuf.get(0)
        if (linked == 0) { // Gdx.gl20.glGetProgramiv(program, GL20.GL_INFO_LOG_LENGTH, intbuf);
// int infoLogLength = intbuf.get(0);
// if (infoLogLength > 1) {
            log = com.badlogic.gdx.Gdx.gl20.glGetProgramInfoLog(program)
            // }
            return -1
        }
        return program
    }

    /** @return the log info for the shader compilation and program linking stage. The shader needs to be bound for this method to
     * have an effect.
     */
    fun getLog(): String? {
        return if (isCompiled) { // Gdx.gl20.glGetProgramiv(program, GL20.GL_INFO_LOG_LENGTH, intbuf);
// int infoLogLength = intbuf.get(0);
// if (infoLogLength > 1) {
            log = com.badlogic.gdx.Gdx.gl20.glGetProgramInfoLog(program)
            // }
            log
        } else {
            log
        }
    }

    /** @return whether this ShaderProgram compiled successfully.
     */
    fun isCompiled(): Boolean {
        return isCompiled
    }

    private fun fetchAttributeLocation(name: String?): Int {
        val gl: com.badlogic.gdx.graphics.GL20 = com.badlogic.gdx.Gdx.gl20
        // -2 == not yet cached
// -1 == cached but not found
        var location: Int
        if (attributes.get(name, -2).also({ location = it }) == -2) {
            location = gl.glGetAttribLocation(program, name)
            attributes.put(name, location)
        }
        return location
    }

    private fun fetchUniformLocation(name: String?): Int {
        return fetchUniformLocation(name, pedantic)
    }

    fun fetchUniformLocation(name: String?, pedantic: Boolean): Int {
        val gl: com.badlogic.gdx.graphics.GL20 = com.badlogic.gdx.Gdx.gl20
        // -2 == not yet cached
// -1 == cached but not found
        var location: Int
        if (uniforms.get(name, -2).also({ location = it }) == -2) {
            location = gl.glGetUniformLocation(program, name)
            if (location == -1 && pedantic) {
                if (isCompiled) throw java.lang.IllegalArgumentException("no uniform with name '$name' in shader")
                throw IllegalStateException("An attempted fetch uniform from uncompiled shader \n" + getLog())
            }
            uniforms.put(name, location)
        }
        return location
    }

    /** Sets the uniform with the given name. The [ShaderProgram] must be bound for this to work.
     *
     * @param name the name of the uniform
     * @param value the value
     */
    fun setUniformi(name: String?, value: Int) {
        val gl: com.badlogic.gdx.graphics.GL20 = com.badlogic.gdx.Gdx.gl20
        checkManaged()
        val location = fetchUniformLocation(name)
        gl.glUniform1i(location, value)
    }

    fun setUniformi(location: Int, value: Int) {
        val gl: com.badlogic.gdx.graphics.GL20 = com.badlogic.gdx.Gdx.gl20
        checkManaged()
        gl.glUniform1i(location, value)
    }

    /** Sets the uniform with the given name. The [ShaderProgram] must be bound for this to work.
     *
     * @param name the name of the uniform
     * @param value1 the first value
     * @param value2 the second value
     */
    fun setUniformi(name: String?, value1: Int, value2: Int) {
        val gl: com.badlogic.gdx.graphics.GL20 = com.badlogic.gdx.Gdx.gl20
        checkManaged()
        val location = fetchUniformLocation(name)
        gl.glUniform2i(location, value1, value2)
    }

    fun setUniformi(location: Int, value1: Int, value2: Int) {
        val gl: com.badlogic.gdx.graphics.GL20 = com.badlogic.gdx.Gdx.gl20
        checkManaged()
        gl.glUniform2i(location, value1, value2)
    }

    /** Sets the uniform with the given name. The [ShaderProgram] must be bound for this to work.
     *
     * @param name the name of the uniform
     * @param value1 the first value
     * @param value2 the second value
     * @param value3 the third value
     */
    fun setUniformi(name: String?, value1: Int, value2: Int, value3: Int) {
        val gl: com.badlogic.gdx.graphics.GL20 = com.badlogic.gdx.Gdx.gl20
        checkManaged()
        val location = fetchUniformLocation(name)
        gl.glUniform3i(location, value1, value2, value3)
    }

    fun setUniformi(location: Int, value1: Int, value2: Int, value3: Int) {
        val gl: com.badlogic.gdx.graphics.GL20 = com.badlogic.gdx.Gdx.gl20
        checkManaged()
        gl.glUniform3i(location, value1, value2, value3)
    }

    /** Sets the uniform with the given name. The [ShaderProgram] must be bound for this to work.
     *
     * @param name the name of the uniform
     * @param value1 the first value
     * @param value2 the second value
     * @param value3 the third value
     * @param value4 the fourth value
     */
    fun setUniformi(name: String?, value1: Int, value2: Int, value3: Int, value4: Int) {
        val gl: com.badlogic.gdx.graphics.GL20 = com.badlogic.gdx.Gdx.gl20
        checkManaged()
        val location = fetchUniformLocation(name)
        gl.glUniform4i(location, value1, value2, value3, value4)
    }

    fun setUniformi(location: Int, value1: Int, value2: Int, value3: Int, value4: Int) {
        val gl: com.badlogic.gdx.graphics.GL20 = com.badlogic.gdx.Gdx.gl20
        checkManaged()
        gl.glUniform4i(location, value1, value2, value3, value4)
    }

    /** Sets the uniform with the given name. The [ShaderProgram] must be bound for this to work.
     *
     * @param name the name of the uniform
     * @param value the value
     */
    fun setUniformf(name: String?, value: Float) {
        val gl: com.badlogic.gdx.graphics.GL20 = com.badlogic.gdx.Gdx.gl20
        checkManaged()
        val location = fetchUniformLocation(name)
        gl.glUniform1f(location, value)
    }

    fun setUniformf(location: Int, value: Float) {
        val gl: com.badlogic.gdx.graphics.GL20 = com.badlogic.gdx.Gdx.gl20
        checkManaged()
        gl.glUniform1f(location, value)
    }

    /** Sets the uniform with the given name. The [ShaderProgram] must be bound for this to work.
     *
     * @param name the name of the uniform
     * @param value1 the first value
     * @param value2 the second value
     */
    fun setUniformf(name: String?, value1: Float, value2: Float) {
        val gl: com.badlogic.gdx.graphics.GL20 = com.badlogic.gdx.Gdx.gl20
        checkManaged()
        val location = fetchUniformLocation(name)
        gl.glUniform2f(location, value1, value2)
    }

    fun setUniformf(location: Int, value1: Float, value2: Float) {
        val gl: com.badlogic.gdx.graphics.GL20 = com.badlogic.gdx.Gdx.gl20
        checkManaged()
        gl.glUniform2f(location, value1, value2)
    }

    /** Sets the uniform with the given name. The [ShaderProgram] must be bound for this to work.
     *
     * @param name the name of the uniform
     * @param value1 the first value
     * @param value2 the second value
     * @param value3 the third value
     */
    fun setUniformf(name: String?, value1: Float, value2: Float, value3: Float) {
        val gl: com.badlogic.gdx.graphics.GL20 = com.badlogic.gdx.Gdx.gl20
        checkManaged()
        val location = fetchUniformLocation(name)
        gl.glUniform3f(location, value1, value2, value3)
    }

    fun setUniformf(location: Int, value1: Float, value2: Float, value3: Float) {
        val gl: com.badlogic.gdx.graphics.GL20 = com.badlogic.gdx.Gdx.gl20
        checkManaged()
        gl.glUniform3f(location, value1, value2, value3)
    }

    /** Sets the uniform with the given name. The [ShaderProgram] must be bound for this to work.
     *
     * @param name the name of the uniform
     * @param value1 the first value
     * @param value2 the second value
     * @param value3 the third value
     * @param value4 the fourth value
     */
    fun setUniformf(name: String?, value1: Float, value2: Float, value3: Float, value4: Float) {
        val gl: com.badlogic.gdx.graphics.GL20 = com.badlogic.gdx.Gdx.gl20
        checkManaged()
        val location = fetchUniformLocation(name)
        gl.glUniform4f(location, value1, value2, value3, value4)
    }

    fun setUniformf(location: Int, value1: Float, value2: Float, value3: Float, value4: Float) {
        val gl: com.badlogic.gdx.graphics.GL20 = com.badlogic.gdx.Gdx.gl20
        checkManaged()
        gl.glUniform4f(location, value1, value2, value3, value4)
    }

    fun setUniform1fv(name: String?, values: FloatArray?, offset: Int, length: Int) {
        val gl: com.badlogic.gdx.graphics.GL20 = com.badlogic.gdx.Gdx.gl20
        checkManaged()
        val location = fetchUniformLocation(name)
        gl.glUniform1fv(location, length, values, offset)
    }

    fun setUniform1fv(location: Int, values: FloatArray?, offset: Int, length: Int) {
        val gl: com.badlogic.gdx.graphics.GL20 = com.badlogic.gdx.Gdx.gl20
        checkManaged()
        gl.glUniform1fv(location, length, values, offset)
    }

    fun setUniform2fv(name: String?, values: FloatArray?, offset: Int, length: Int) {
        val gl: com.badlogic.gdx.graphics.GL20 = com.badlogic.gdx.Gdx.gl20
        checkManaged()
        val location = fetchUniformLocation(name)
        gl.glUniform2fv(location, length / 2, values, offset)
    }

    fun setUniform2fv(location: Int, values: FloatArray?, offset: Int, length: Int) {
        val gl: com.badlogic.gdx.graphics.GL20 = com.badlogic.gdx.Gdx.gl20
        checkManaged()
        gl.glUniform2fv(location, length / 2, values, offset)
    }

    fun setUniform3fv(name: String?, values: FloatArray?, offset: Int, length: Int) {
        val gl: com.badlogic.gdx.graphics.GL20 = com.badlogic.gdx.Gdx.gl20
        checkManaged()
        val location = fetchUniformLocation(name)
        gl.glUniform3fv(location, length / 3, values, offset)
    }

    fun setUniform3fv(location: Int, values: FloatArray?, offset: Int, length: Int) {
        val gl: com.badlogic.gdx.graphics.GL20 = com.badlogic.gdx.Gdx.gl20
        checkManaged()
        gl.glUniform3fv(location, length / 3, values, offset)
    }

    fun setUniform4fv(name: String?, values: FloatArray?, offset: Int, length: Int) {
        val gl: com.badlogic.gdx.graphics.GL20 = com.badlogic.gdx.Gdx.gl20
        checkManaged()
        val location = fetchUniformLocation(name)
        gl.glUniform4fv(location, length / 4, values, offset)
    }

    fun setUniform4fv(location: Int, values: FloatArray?, offset: Int, length: Int) {
        val gl: com.badlogic.gdx.graphics.GL20 = com.badlogic.gdx.Gdx.gl20
        checkManaged()
        gl.glUniform4fv(location, length / 4, values, offset)
    }

    /** Sets the uniform matrix with the given name. The [ShaderProgram] must be bound for this to work.
     *
     * @param name the name of the uniform
     * @param matrix the matrix
     */
    fun setUniformMatrix(name: String?, matrix: com.badlogic.gdx.math.Matrix4?) {
        setUniformMatrix(name, matrix, false)
    }

    /** Sets the uniform matrix with the given name. The [ShaderProgram] must be bound for this to work.
     *
     * @param name the name of the uniform
     * @param matrix the matrix
     * @param transpose whether the matrix should be transposed
     */
    fun setUniformMatrix(name: String?, matrix: com.badlogic.gdx.math.Matrix4?, transpose: Boolean) {
        setUniformMatrix(fetchUniformLocation(name), matrix, transpose)
    }

    fun setUniformMatrix(location: Int, matrix: com.badlogic.gdx.math.Matrix4?) {
        setUniformMatrix(location, matrix, false)
    }

    fun setUniformMatrix(location: Int, matrix: com.badlogic.gdx.math.Matrix4?, transpose: Boolean) {
        val gl: com.badlogic.gdx.graphics.GL20 = com.badlogic.gdx.Gdx.gl20
        checkManaged()
        gl.glUniformMatrix4fv(location, 1, transpose, matrix.`val`, 0)
    }

    /** Sets the uniform matrix with the given name. The [ShaderProgram] must be bound for this to work.
     *
     * @param name the name of the uniform
     * @param matrix the matrix
     */
    fun setUniformMatrix(name: String?, matrix: com.badlogic.gdx.math.Matrix3?) {
        setUniformMatrix(name, matrix, false)
    }

    /** Sets the uniform matrix with the given name. The [ShaderProgram] must be bound for this to work.
     *
     * @param name the name of the uniform
     * @param matrix the matrix
     * @param transpose whether the uniform matrix should be transposed
     */
    fun setUniformMatrix(name: String?, matrix: com.badlogic.gdx.math.Matrix3?, transpose: Boolean) {
        setUniformMatrix(fetchUniformLocation(name), matrix, transpose)
    }

    fun setUniformMatrix(location: Int, matrix: com.badlogic.gdx.math.Matrix3?) {
        setUniformMatrix(location, matrix, false)
    }

    fun setUniformMatrix(location: Int, matrix: com.badlogic.gdx.math.Matrix3?, transpose: Boolean) {
        val gl: com.badlogic.gdx.graphics.GL20 = com.badlogic.gdx.Gdx.gl20
        checkManaged()
        gl.glUniformMatrix3fv(location, 1, transpose, matrix.`val`, 0)
    }

    /** Sets an array of uniform matrices with the given name. The [ShaderProgram] must be bound for this to work.
     *
     * @param name the name of the uniform
     * @param buffer buffer containing the matrix data
     * @param transpose whether the uniform matrix should be transposed
     */
    fun setUniformMatrix3fv(name: String?, buffer: FloatBuffer?, count: Int, transpose: Boolean) {
        val gl: com.badlogic.gdx.graphics.GL20 = com.badlogic.gdx.Gdx.gl20
        checkManaged()
        buffer.position(0)
        val location = fetchUniformLocation(name)
        gl.glUniformMatrix3fv(location, count, transpose, buffer)
    }

    /** Sets an array of uniform matrices with the given name. The [ShaderProgram] must be bound for this to work.
     *
     * @param name the name of the uniform
     * @param buffer buffer containing the matrix data
     * @param transpose whether the uniform matrix should be transposed
     */
    fun setUniformMatrix4fv(name: String?, buffer: FloatBuffer?, count: Int, transpose: Boolean) {
        val gl: com.badlogic.gdx.graphics.GL20 = com.badlogic.gdx.Gdx.gl20
        checkManaged()
        buffer.position(0)
        val location = fetchUniformLocation(name)
        gl.glUniformMatrix4fv(location, count, transpose, buffer)
    }

    fun setUniformMatrix4fv(location: Int, values: FloatArray?, offset: Int, length: Int) {
        val gl: com.badlogic.gdx.graphics.GL20 = com.badlogic.gdx.Gdx.gl20
        checkManaged()
        gl.glUniformMatrix4fv(location, length / 16, false, values, offset)
    }

    fun setUniformMatrix4fv(name: String?, values: FloatArray?, offset: Int, length: Int) {
        setUniformMatrix4fv(fetchUniformLocation(name), values, offset, length)
    }

    /** Sets the uniform with the given name. The [ShaderProgram] must be bound for this to work.
     *
     * @param name the name of the uniform
     * @param values x and y as the first and second values respectively
     */
    fun setUniformf(name: String?, values: com.badlogic.gdx.math.Vector2?) {
        setUniformf(name, values.x, values.y)
    }

    fun setUniformf(location: Int, values: com.badlogic.gdx.math.Vector2?) {
        setUniformf(location, values.x, values.y)
    }

    /** Sets the uniform with the given name. The [ShaderProgram] must be bound for this to work.
     *
     * @param name the name of the uniform
     * @param values x, y and z as the first, second and third values respectively
     */
    fun setUniformf(name: String?, values: com.badlogic.gdx.math.Vector3?) {
        setUniformf(name, values.x, values.y, values.z)
    }

    fun setUniformf(location: Int, values: com.badlogic.gdx.math.Vector3?) {
        setUniformf(location, values.x, values.y, values.z)
    }

    /** Sets the uniform with the given name. The [ShaderProgram] must be bound for this to work.
     *
     * @param name the name of the uniform
     * @param values r, g, b and a as the first through fourth values respectively
     */
    fun setUniformf(name: String?, values: com.badlogic.gdx.graphics.Color?) {
        setUniformf(name, values.r, values.g, values.b, values.a)
    }

    fun setUniformf(location: Int, values: com.badlogic.gdx.graphics.Color?) {
        setUniformf(location, values.r, values.g, values.b, values.a)
    }

    /** Sets the vertex attribute with the given name. The [ShaderProgram] must be bound for this to work.
     *
     * @param name the attribute name
     * @param size the number of components, must be >= 1 and <= 4
     * @param type the type, must be one of GL20.GL_BYTE, GL20.GL_UNSIGNED_BYTE, GL20.GL_SHORT,
     * GL20.GL_UNSIGNED_SHORT,GL20.GL_FIXED, or GL20.GL_FLOAT. GL_FIXED will not work on the desktop
     * @param normalize whether fixed point data should be normalized. Will not work on the desktop
     * @param stride the stride in bytes between successive attributes
     * @param buffer the buffer containing the vertex attributes.
     */
    fun setVertexAttribute(name: String?, size: Int, type: Int, normalize: Boolean, stride: Int, buffer: java.nio.Buffer?) {
        val gl: com.badlogic.gdx.graphics.GL20 = com.badlogic.gdx.Gdx.gl20
        checkManaged()
        val location = fetchAttributeLocation(name)
        if (location == -1) return
        gl.glVertexAttribPointer(location, size, type, normalize, stride, buffer)
    }

    fun setVertexAttribute(location: Int, size: Int, type: Int, normalize: Boolean, stride: Int, buffer: java.nio.Buffer?) {
        val gl: com.badlogic.gdx.graphics.GL20 = com.badlogic.gdx.Gdx.gl20
        checkManaged()
        gl.glVertexAttribPointer(location, size, type, normalize, stride, buffer)
    }

    /** Sets the vertex attribute with the given name. The [ShaderProgram] must be bound for this to work.
     *
     * @param name the attribute name
     * @param size the number of components, must be >= 1 and <= 4
     * @param type the type, must be one of GL20.GL_BYTE, GL20.GL_UNSIGNED_BYTE, GL20.GL_SHORT,
     * GL20.GL_UNSIGNED_SHORT,GL20.GL_FIXED, or GL20.GL_FLOAT. GL_FIXED will not work on the desktop
     * @param normalize whether fixed point data should be normalized. Will not work on the desktop
     * @param stride the stride in bytes between successive attributes
     * @param offset byte offset into the vertex buffer object bound to GL20.GL_ARRAY_BUFFER.
     */
    fun setVertexAttribute(name: String?, size: Int, type: Int, normalize: Boolean, stride: Int, offset: Int) {
        val gl: com.badlogic.gdx.graphics.GL20 = com.badlogic.gdx.Gdx.gl20
        checkManaged()
        val location = fetchAttributeLocation(name)
        if (location == -1) return
        gl.glVertexAttribPointer(location, size, type, normalize, stride, offset)
    }

    fun setVertexAttribute(location: Int, size: Int, type: Int, normalize: Boolean, stride: Int, offset: Int) {
        val gl: com.badlogic.gdx.graphics.GL20 = com.badlogic.gdx.Gdx.gl20
        checkManaged()
        gl.glVertexAttribPointer(location, size, type, normalize, stride, offset)
    }

    /** Makes OpenGL ES 2.0 use this vertex and fragment shader pair. When you are done with this shader you have to call
     * [ShaderProgram.end].  */
    fun begin() {
        val gl: com.badlogic.gdx.graphics.GL20 = com.badlogic.gdx.Gdx.gl20
        checkManaged()
        gl.glUseProgram(program)
    }

    /** Disables this shader. Must be called when one is done with the shader. Don't mix it with dispose, that will release the
     * shader resources.  */
    fun end() {
        val gl: com.badlogic.gdx.graphics.GL20 = com.badlogic.gdx.Gdx.gl20
        gl.glUseProgram(0)
    }

    /** Disposes all resources associated with this shader. Must be called when the shader is no longer used.  */
    override fun dispose() {
        val gl: com.badlogic.gdx.graphics.GL20 = com.badlogic.gdx.Gdx.gl20
        gl.glUseProgram(0)
        gl.glDeleteShader(vertexShaderHandle)
        gl.glDeleteShader(fragmentShaderHandle)
        gl.glDeleteProgram(program)
        if (shaders.get(com.badlogic.gdx.Gdx.app) != null) shaders.get(com.badlogic.gdx.Gdx.app).removeValue(this, true)
    }

    /** Disables the vertex attribute with the given name
     *
     * @param name the vertex attribute name
     */
    fun disableVertexAttribute(name: String?) {
        val gl: com.badlogic.gdx.graphics.GL20 = com.badlogic.gdx.Gdx.gl20
        checkManaged()
        val location = fetchAttributeLocation(name)
        if (location == -1) return
        gl.glDisableVertexAttribArray(location)
    }

    fun disableVertexAttribute(location: Int) {
        val gl: com.badlogic.gdx.graphics.GL20 = com.badlogic.gdx.Gdx.gl20
        checkManaged()
        gl.glDisableVertexAttribArray(location)
    }

    /** Enables the vertex attribute with the given name
     *
     * @param name the vertex attribute name
     */
    fun enableVertexAttribute(name: String?) {
        val gl: com.badlogic.gdx.graphics.GL20 = com.badlogic.gdx.Gdx.gl20
        checkManaged()
        val location = fetchAttributeLocation(name)
        if (location == -1) return
        gl.glEnableVertexAttribArray(location)
    }

    fun enableVertexAttribute(location: Int) {
        val gl: com.badlogic.gdx.graphics.GL20 = com.badlogic.gdx.Gdx.gl20
        checkManaged()
        gl.glEnableVertexAttribArray(location)
    }

    private fun checkManaged() {
        if (invalidated) {
            compileShaders(vertexShaderSource, fragmentShaderSource)
            invalidated = false
        }
    }

    private fun addManagedShader(app: com.badlogic.gdx.Application?, shaderProgram: ShaderProgram?) {
        var managedResources: com.badlogic.gdx.utils.Array<ShaderProgram?> = shaders.get(app)
        if (managedResources == null) managedResources = com.badlogic.gdx.utils.Array()
        managedResources.add(shaderProgram)
        shaders.put(app, managedResources)
    }

    /** Sets the given attribute
     *
     * @param name the name of the attribute
     * @param value1 the first value
     * @param value2 the second value
     * @param value3 the third value
     * @param value4 the fourth value
     */
    fun setAttributef(name: String?, value1: Float, value2: Float, value3: Float, value4: Float) {
        val gl: com.badlogic.gdx.graphics.GL20 = com.badlogic.gdx.Gdx.gl20
        val location = fetchAttributeLocation(name)
        gl.glVertexAttrib4f(location, value1, value2, value3, value4)
    }

    var params: java.nio.IntBuffer? = com.badlogic.gdx.utils.BufferUtils.newIntBuffer(1)
    var type: java.nio.IntBuffer? = com.badlogic.gdx.utils.BufferUtils.newIntBuffer(1)
    private fun fetchUniforms() {
        params.clear()
        com.badlogic.gdx.Gdx.gl20.glGetProgramiv(program, com.badlogic.gdx.graphics.GL20.GL_ACTIVE_UNIFORMS, params)
        val numUniforms: Int = params.get(0)
        uniformNames = arrayOfNulls<String?>(numUniforms)
        for (i in 0 until numUniforms) {
            params.clear()
            params.put(0, 1)
            type.clear()
            val name: String = com.badlogic.gdx.Gdx.gl20.glGetActiveUniform(program, i, params, type)
            val location: Int = com.badlogic.gdx.Gdx.gl20.glGetUniformLocation(program, name)
            uniforms.put(name, location)
            uniformTypes.put(name, type.get(0))
            uniformSizes.put(name, params.get(0))
            uniformNames!![i] = name
        }
    }

    private fun fetchAttributes() {
        params.clear()
        com.badlogic.gdx.Gdx.gl20.glGetProgramiv(program, com.badlogic.gdx.graphics.GL20.GL_ACTIVE_ATTRIBUTES, params)
        val numAttributes: Int = params.get(0)
        attributeNames = arrayOfNulls<String?>(numAttributes)
        for (i in 0 until numAttributes) {
            params.clear()
            params.put(0, 1)
            type.clear()
            val name: String = com.badlogic.gdx.Gdx.gl20.glGetActiveAttrib(program, i, params, type)
            val location: Int = com.badlogic.gdx.Gdx.gl20.glGetAttribLocation(program, name)
            attributes.put(name, location)
            attributeTypes.put(name, type.get(0))
            attributeSizes.put(name, params.get(0))
            attributeNames!![i] = name
        }
    }

    /** @param name the name of the attribute
     * @return whether the attribute is available in the shader
     */
    fun hasAttribute(name: String?): Boolean {
        return attributes.containsKey(name)
    }

    /** @param name the name of the attribute
     * @return the type of the attribute, one of [GL20.GL_FLOAT], [GL20.GL_FLOAT_VEC2] etc.
     */
    fun getAttributeType(name: String?): Int {
        return attributeTypes.get(name, 0)
    }

    /** @param name the name of the attribute
     * @return the location of the attribute or -1.
     */
    fun getAttributeLocation(name: String?): Int {
        return attributes.get(name, -1)
    }

    /** @param name the name of the attribute
     * @return the size of the attribute or 0.
     */
    fun getAttributeSize(name: String?): Int {
        return attributeSizes.get(name, 0)
    }

    /** @param name the name of the uniform
     * @return whether the uniform is available in the shader
     */
    fun hasUniform(name: String?): Boolean {
        return uniforms.containsKey(name)
    }

    /** @param name the name of the uniform
     * @return the type of the uniform, one of [GL20.GL_FLOAT], [GL20.GL_FLOAT_VEC2] etc.
     */
    fun getUniformType(name: String?): Int {
        return uniformTypes.get(name, 0)
    }

    /** @param name the name of the uniform
     * @return the location of the uniform or -1.
     */
    fun getUniformLocation(name: String?): Int {
        return uniforms.get(name, -1)
    }

    /** @param name the name of the uniform
     * @return the size of the uniform or 0.
     */
    fun getUniformSize(name: String?): Int {
        return uniformSizes.get(name, 0)
    }

    /** @return the attributes
     */
    fun getAttributes(): Array<String?>? {
        return attributeNames
    }

    /** @return the uniforms
     */
    fun getUniforms(): Array<String?>? {
        return uniformNames
    }

    /** @return the source of the vertex shader
     */
    fun getVertexShaderSource(): String? {
        return vertexShaderSource
    }

    /** @return the source of the fragment shader
     */
    fun getFragmentShaderSource(): String? {
        return fragmentShaderSource
    }

    companion object {
        /** default name for position attributes  */
        @kotlin.jvm.JvmField
        val POSITION_ATTRIBUTE: String? = "a_position"
        /** default name for normal attributes  */
        @kotlin.jvm.JvmField
        val NORMAL_ATTRIBUTE: String? = "a_normal"
        /** default name for color attributes  */
        @kotlin.jvm.JvmField
        val COLOR_ATTRIBUTE: String? = "a_color"
        /** default name for texcoords attributes, append texture unit number  */
        @kotlin.jvm.JvmField
        val TEXCOORD_ATTRIBUTE: String? = "a_texCoord"
        /** default name for tangent attribute  */
        @kotlin.jvm.JvmField
        val TANGENT_ATTRIBUTE: String? = "a_tangent"
        /** default name for binormal attribute  */
        @kotlin.jvm.JvmField
        val BINORMAL_ATTRIBUTE: String? = "a_binormal"
        /** default name for boneweight attribute  */
        @kotlin.jvm.JvmField
        val BONEWEIGHT_ATTRIBUTE: String? = "a_boneWeight"
        /** flag indicating whether attributes & uniforms must be present at all times  */
        var pedantic = true
        /** code that is always added to the vertex shader code, typically used to inject a #version line. Note that this is added
         * as-is, you should include a newline (`\n`) if needed.  */
        var prependVertexCode: String? = ""
        /** code that is always added to every fragment shader code, typically used to inject a #version line. Note that this is added
         * as-is, you should include a newline (`\n`) if needed.  */
        var prependFragmentCode: String? = ""
        /** the list of currently available shaders  */
        private val shaders: com.badlogic.gdx.utils.ObjectMap<com.badlogic.gdx.Application?, com.badlogic.gdx.utils.Array<ShaderProgram?>?>? = com.badlogic.gdx.utils.ObjectMap()
        val intbuf: java.nio.IntBuffer? = com.badlogic.gdx.utils.BufferUtils.newIntBuffer(1)
        /** Invalidates all shaders so the next time they are used new handles are generated
         * @param app
         */
        fun invalidateAllShaderPrograms(app: com.badlogic.gdx.Application?) {
            if (com.badlogic.gdx.Gdx.gl20 == null) return
            val shaderArray: com.badlogic.gdx.utils.Array<ShaderProgram?> = shaders.get(app) ?: return
            for (i in 0 until shaderArray.size) {
                shaderArray.get(i).invalidated = true
                shaderArray.get(i).checkManaged()
            }
        }

        fun clearAllShaderPrograms(app: com.badlogic.gdx.Application?) {
            shaders.remove(app)
        }

        fun getManagedStatus(): String? {
            val builder: java.lang.StringBuilder = java.lang.StringBuilder()
            val i = 0
            builder.append("Managed shaders/app: { ")
            for (app in shaders.keys()) {
                builder.append(shaders.get(app).size)
                builder.append(" ")
            }
            builder.append("}")
            return builder.toString()
        }

        /** @return the number of managed shader programs currently loaded
         */
        fun getNumManagedShaderPrograms(): Int {
            return shaders.get(com.badlogic.gdx.Gdx.app).size
        }
    }

    /** Constructs a new ShaderProgram and immediately compiles it.
     *
     * @param vertexShader the vertex shader
     * @param fragmentShader the fragment shader
     */
    init {
        var vertexShader = vertexShader
        var fragmentShader = fragmentShader
        if (vertexShader == null) throw java.lang.IllegalArgumentException("vertex shader must not be null")
        if (fragmentShader == null) throw java.lang.IllegalArgumentException("fragment shader must not be null")
        if (prependVertexCode != null && prependVertexCode!!.length > 0) vertexShader = prependVertexCode + vertexShader
        if (prependFragmentCode != null && prependFragmentCode!!.length > 0) fragmentShader = prependFragmentCode + fragmentShader
        vertexShaderSource = vertexShader
        fragmentShaderSource = fragmentShader
        matrix = com.badlogic.gdx.utils.BufferUtils.newFloatBuffer(16)
        compileShaders(vertexShader, fragmentShader)
        if (isCompiled()) {
            fetchAttributes()
            fetchUniforms()
            addManagedShader(com.badlogic.gdx.Gdx.app, this)
        }
    }
}
