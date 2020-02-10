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

/** Immediate mode rendering class for GLES 2.0. The renderer will allow you to specify vertices on the fly and provides a default
 * shader for (unlit) rendering. *
 *
 * @author mzechner
 */
class ImmediateModeRenderer20(private val maxVertices: Int, hasNormals: Boolean, hasColors: Boolean, private val numTexCoords: Int, shader: com.badlogic.gdx.graphics.glutils.ShaderProgram?) : com.badlogic.gdx.graphics.glutils.ImmediateModeRenderer {

    private var primitiveType = 0
    private var vertexIdx = 0
    private var numSetTexCoords = 0
    private var numVertices = 0
    private val mesh: com.badlogic.gdx.graphics.Mesh?
    private var shader: com.badlogic.gdx.graphics.glutils.ShaderProgram?
    private var ownsShader = false
    private val vertexSize: Int
    private val normalOffset: Int
    private val colorOffset: Int
    private val texCoordOffset: Int
    private val projModelView: com.badlogic.gdx.math.Matrix4? = com.badlogic.gdx.math.Matrix4()
    private val vertices: FloatArray?
    private val shaderUniformNames: Array<String?>?

    constructor(hasNormals: Boolean, hasColors: Boolean, numTexCoords: Int) : this(5000, hasNormals, hasColors, numTexCoords, createDefaultShader(hasNormals, hasColors, numTexCoords)) {
        ownsShader = true
    }

    constructor(maxVertices: Int, hasNormals: Boolean, hasColors: Boolean, numTexCoords: Int) : this(maxVertices, hasNormals, hasColors, numTexCoords, createDefaultShader(hasNormals, hasColors, numTexCoords)) {
        ownsShader = true
    }

    private fun buildVertexAttributes(hasNormals: Boolean, hasColor: Boolean, numTexCoords: Int): Array<com.badlogic.gdx.graphics.VertexAttribute?>? {
        val attribs: com.badlogic.gdx.utils.Array<com.badlogic.gdx.graphics.VertexAttribute?> = com.badlogic.gdx.utils.Array()
        attribs.add(com.badlogic.gdx.graphics.VertexAttribute(com.badlogic.gdx.graphics.VertexAttributes.Usage.Position, 3, com.badlogic.gdx.graphics.glutils.ShaderProgram.Companion.POSITION_ATTRIBUTE))
        if (hasNormals) attribs.add(com.badlogic.gdx.graphics.VertexAttribute(com.badlogic.gdx.graphics.VertexAttributes.Usage.Normal, 3, com.badlogic.gdx.graphics.glutils.ShaderProgram.Companion.NORMAL_ATTRIBUTE))
        if (hasColor) attribs.add(com.badlogic.gdx.graphics.VertexAttribute(com.badlogic.gdx.graphics.VertexAttributes.Usage.ColorPacked, 4, com.badlogic.gdx.graphics.glutils.ShaderProgram.Companion.COLOR_ATTRIBUTE))
        for (i in 0 until numTexCoords) {
            attribs.add(com.badlogic.gdx.graphics.VertexAttribute(com.badlogic.gdx.graphics.VertexAttributes.Usage.TextureCoordinates, 2, com.badlogic.gdx.graphics.glutils.ShaderProgram.Companion.TEXCOORD_ATTRIBUTE + i))
        }
        val array: Array<com.badlogic.gdx.graphics.VertexAttribute?> = arrayOfNulls<com.badlogic.gdx.graphics.VertexAttribute?>(attribs.size)
        for (i in 0 until attribs.size) array[i] = attribs.get(i)
        return array
    }

    fun setShader(shader: com.badlogic.gdx.graphics.glutils.ShaderProgram?) {
        if (ownsShader) this.shader!!.dispose()
        this.shader = shader
        ownsShader = false
    }

    override fun begin(projModelView: com.badlogic.gdx.math.Matrix4?, primitiveType: Int) {
        this.projModelView.set(projModelView)
        this.primitiveType = primitiveType
    }

    override fun color(color: com.badlogic.gdx.graphics.Color?) {
        vertices!![vertexIdx + colorOffset] = color.toFloatBits()
    }

    override fun color(r: Float, g: Float, b: Float, a: Float) {
        vertices!![vertexIdx + colorOffset] = com.badlogic.gdx.graphics.Color.toFloatBits(r, g, b, a)
    }

    override fun color(colorBits: Float) {
        vertices!![vertexIdx + colorOffset] = colorBits
    }

    override fun texCoord(u: Float, v: Float) {
        val idx = vertexIdx + texCoordOffset
        vertices!![idx + numSetTexCoords] = u
        vertices[idx + numSetTexCoords + 1] = v
        numSetTexCoords += 2
    }

    override fun normal(x: Float, y: Float, z: Float) {
        val idx = vertexIdx + normalOffset
        vertices!![idx] = x
        vertices[idx + 1] = y
        vertices[idx + 2] = z
    }

    override fun vertex(x: Float, y: Float, z: Float) {
        val idx = vertexIdx
        vertices!![idx] = x
        vertices[idx + 1] = y
        vertices[idx + 2] = z
        numSetTexCoords = 0
        vertexIdx += vertexSize
        numVertices++
    }

    override fun flush() {
        if (numVertices == 0) return
        shader!!.begin()
        shader.setUniformMatrix("u_projModelView", projModelView)
        for (i in 0 until numTexCoords) shader!!.setUniformi(shaderUniformNames!![i], i)
        mesh.setVertices(vertices, 0, vertexIdx)
        mesh.render(shader, primitiveType)
        shader!!.end()
        numSetTexCoords = 0
        vertexIdx = 0
        numVertices = 0
    }

    override fun end() {
        flush()
    }

    override fun getNumVertices(): Int {
        return numVertices
    }

    override fun getMaxVertices(): Int {
        return maxVertices
    }

    override fun dispose() {
        if (ownsShader && shader != null) shader.dispose()
        mesh.dispose()
    }

    companion object {
        private fun createVertexShader(hasNormals: Boolean, hasColors: Boolean, numTexCoords: Int): String? {
            var shader = ("attribute vec4 " + com.badlogic.gdx.graphics.glutils.ShaderProgram.Companion.POSITION_ATTRIBUTE + ";\n"
                + (if (hasNormals) "attribute vec3 " + com.badlogic.gdx.graphics.glutils.ShaderProgram.Companion.NORMAL_ATTRIBUTE + ";\n" else "")
                + if (hasColors) "attribute vec4 " + com.badlogic.gdx.graphics.glutils.ShaderProgram.Companion.COLOR_ATTRIBUTE + ";\n" else "")
            for (i in 0 until numTexCoords) {
                shader += "attribute vec2 " + com.badlogic.gdx.graphics.glutils.ShaderProgram.Companion.TEXCOORD_ATTRIBUTE + i + ";\n"
            }
            shader += "uniform mat4 u_projModelView;\n"
            shader += if (hasColors) "varying vec4 v_col;\n" else ""
            for (i in 0 until numTexCoords) {
                shader += "varying vec2 v_tex$i;\n"
            }
            shader += ("void main() {\n" + "   gl_Position = u_projModelView * " + com.badlogic.gdx.graphics.glutils.ShaderProgram.Companion.POSITION_ATTRIBUTE + ";\n"
                + if (hasColors) "   v_col = " + com.badlogic.gdx.graphics.glutils.ShaderProgram.Companion.COLOR_ATTRIBUTE + ";\n" else "")
            for (i in 0 until numTexCoords) {
                shader += "   v_tex" + i + " = " + com.badlogic.gdx.graphics.glutils.ShaderProgram.Companion.TEXCOORD_ATTRIBUTE + i + ";\n"
            }
            shader += "   gl_PointSize = 1.0;\n"
            shader += "}\n"
            return shader
        }

        private fun createFragmentShader(hasNormals: Boolean, hasColors: Boolean, numTexCoords: Int): String? {
            var shader = "#ifdef GL_ES\n" + "precision mediump float;\n" + "#endif\n"
            if (hasColors) shader += "varying vec4 v_col;\n"
            for (i in 0 until numTexCoords) {
                shader += "varying vec2 v_tex$i;\n"
                shader += "uniform sampler2D u_sampler$i;\n"
            }
            shader += "void main() {\n" + "   gl_FragColor = " + if (hasColors) "v_col" else "vec4(1, 1, 1, 1)"
            if (numTexCoords > 0) shader += " * "
            for (i in 0 until numTexCoords) {
                shader += if (i == numTexCoords - 1) {
                    " texture2D(u_sampler$i,  v_tex$i)"
                } else {
                    " texture2D(u_sampler$i,  v_tex$i) *"
                }
            }
            shader += ";\n}"
            return shader
        }

        /** Returns a new instance of the default shader used by SpriteBatch for GL2 when no shader is specified.  */
        fun createDefaultShader(hasNormals: Boolean, hasColors: Boolean, numTexCoords: Int): com.badlogic.gdx.graphics.glutils.ShaderProgram? {
            val vertexShader = createVertexShader(hasNormals, hasColors, numTexCoords)
            val fragmentShader = createFragmentShader(hasNormals, hasColors, numTexCoords)
            return com.badlogic.gdx.graphics.glutils.ShaderProgram(vertexShader, fragmentShader)
        }
    }

    init {
        this.shader = shader
        val attribs: Array<com.badlogic.gdx.graphics.VertexAttribute?>? = buildVertexAttributes(hasNormals, hasColors, numTexCoords)
        mesh = com.badlogic.gdx.graphics.Mesh(false, maxVertices, 0, *attribs)
        vertices = FloatArray(maxVertices * (mesh.getVertexAttributes().vertexSize / 4))
        vertexSize = mesh.getVertexAttributes().vertexSize / 4
        normalOffset = if (mesh.getVertexAttribute(com.badlogic.gdx.graphics.VertexAttributes.Usage.Normal) != null) mesh.getVertexAttribute(com.badlogic.gdx.graphics.VertexAttributes.Usage.Normal).offset / 4 else 0
        colorOffset = if (mesh.getVertexAttribute(com.badlogic.gdx.graphics.VertexAttributes.Usage.ColorPacked) != null) mesh.getVertexAttribute(com.badlogic.gdx.graphics.VertexAttributes.Usage.ColorPacked).offset / 4 else 0
        texCoordOffset = if (mesh.getVertexAttribute(com.badlogic.gdx.graphics.VertexAttributes.Usage.TextureCoordinates) != null) mesh
            .getVertexAttribute(com.badlogic.gdx.graphics.VertexAttributes.Usage.TextureCoordinates).offset / 4 else 0
        shaderUniformNames = arrayOfNulls<String?>(numTexCoords)
        for (i in 0 until numTexCoords) {
            shaderUniformNames[i] = "u_sampler$i"
        }
    }
}
