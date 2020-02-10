/*******************************************************************************
 * Copyright 2015 See AUTHORS file.
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
package com.badlogic.gdx.graphics.profiling

import java.lang.StackTraceElement
import java.nio.FloatBuffer
import java.nio.LongBuffer

/** @author Daniel Holderbaum
 * @author Jan Polák
 */
class GL20Interceptor(glProfiler: com.badlogic.gdx.graphics.profiling.GLProfiler, gl20: com.badlogic.gdx.graphics.GL20) : com.badlogic.gdx.graphics.profiling.GLInterceptor(glProfiler), com.badlogic.gdx.graphics.GL20 {

    val gl20: com.badlogic.gdx.graphics.GL20
    private fun check() {
        var error: Int = gl20.glGetError()
        while (error != com.badlogic.gdx.graphics.GL20.GL_NO_ERROR) {
            glProfiler.getListener().onError(error)
            error = gl20.glGetError()
        }
    }

    override fun glActiveTexture(texture: Int) {
        calls++
        gl20.glActiveTexture(texture)
        check()
    }

    override fun glBindTexture(target: Int, texture: Int) {
        textureBindings++
        calls++
        gl20.glBindTexture(target, texture)
        check()
    }

    override fun glBlendFunc(sfactor: Int, dfactor: Int) {
        calls++
        gl20.glBlendFunc(sfactor, dfactor)
        check()
    }

    override fun glClear(mask: Int) {
        calls++
        gl20.glClear(mask)
        check()
    }

    override fun glClearColor(red: Float, green: Float, blue: Float, alpha: Float) {
        calls++
        gl20.glClearColor(red, green, blue, alpha)
        check()
    }

    override fun glClearDepthf(depth: Float) {
        calls++
        gl20.glClearDepthf(depth)
        check()
    }

    override fun glClearStencil(s: Int) {
        calls++
        gl20.glClearStencil(s)
        check()
    }

    override fun glColorMask(red: Boolean, green: Boolean, blue: Boolean, alpha: Boolean) {
        calls++
        gl20.glColorMask(red, green, blue, alpha)
        check()
    }

    override fun glCompressedTexImage2D(target: Int, level: Int, internalformat: Int, width: Int, height: Int, border: Int,
                                        imageSize: Int, data: java.nio.Buffer) {
        calls++
        gl20.glCompressedTexImage2D(target, level, internalformat, width, height, border, imageSize, data)
        check()
    }

    override fun glCompressedTexSubImage2D(target: Int, level: Int, xoffset: Int, yoffset: Int, width: Int, height: Int, format: Int,
                                           imageSize: Int, data: java.nio.Buffer) {
        calls++
        gl20.glCompressedTexSubImage2D(target, level, xoffset, yoffset, width, height, format, imageSize, data)
        check()
    }

    override fun glCopyTexImage2D(target: Int, level: Int, internalformat: Int, x: Int, y: Int, width: Int, height: Int, border: Int) {
        calls++
        gl20.glCopyTexImage2D(target, level, internalformat, x, y, width, height, border)
        check()
    }

    override fun glCopyTexSubImage2D(target: Int, level: Int, xoffset: Int, yoffset: Int, x: Int, y: Int, width: Int, height: Int) {
        calls++
        gl20.glCopyTexSubImage2D(target, level, xoffset, yoffset, x, y, width, height)
        check()
    }

    override fun glCullFace(mode: Int) {
        calls++
        gl20.glCullFace(mode)
        check()
    }

    override fun glDeleteTextures(n: Int, textures: java.nio.IntBuffer) {
        calls++
        gl20.glDeleteTextures(n, textures)
        check()
    }

    override fun glDeleteTexture(texture: Int) {
        calls++
        gl20.glDeleteTexture(texture)
        check()
    }

    override fun glDepthFunc(func: Int) {
        calls++
        gl20.glDepthFunc(func)
        check()
    }

    override fun glDepthMask(flag: Boolean) {
        calls++
        gl20.glDepthMask(flag)
        check()
    }

    override fun glDepthRangef(zNear: Float, zFar: Float) {
        calls++
        gl20.glDepthRangef(zNear, zFar)
        check()
    }

    override fun glDisable(cap: Int) {
        calls++
        gl20.glDisable(cap)
        check()
    }

    override fun glDrawArrays(mode: Int, first: Int, count: Int) {
        vertexCount.put(count.toFloat())
        drawCalls++
        calls++
        gl20.glDrawArrays(mode, first, count)
        check()
    }

    override fun glDrawElements(mode: Int, count: Int, type: Int, indices: java.nio.Buffer) {
        vertexCount.put(count.toFloat())
        drawCalls++
        calls++
        gl20.glDrawElements(mode, count, type, indices)
        check()
    }

    override fun glEnable(cap: Int) {
        calls++
        gl20.glEnable(cap)
        check()
    }

    override fun glFinish() {
        calls++
        gl20.glFinish()
        check()
    }

    override fun glFlush() {
        calls++
        gl20.glFlush()
        check()
    }

    override fun glFrontFace(mode: Int) {
        calls++
        gl20.glFrontFace(mode)
        check()
    }

    override fun glGenTextures(n: Int, textures: java.nio.IntBuffer) {
        calls++
        gl20.glGenTextures(n, textures)
        check()
    }

    override fun glGenTexture(): Int {
        calls++
        val result: Int = gl20.glGenTexture()
        check()
        return result
    }

    override fun glGetError(): Int {
        calls++
        //Errors by glGetError are undetectable
        return gl20.glGetError()
    }

    override fun glGetIntegerv(pname: Int, params: java.nio.IntBuffer) {
        calls++
        gl20.glGetIntegerv(pname, params)
        check()
    }

    override fun glGetString(name: Int): String {
        calls++
        val result: String = gl20.glGetString(name)
        check()
        return result
    }

    override fun glHint(target: Int, mode: Int) {
        calls++
        gl20.glHint(target, mode)
        check()
    }

    override fun glLineWidth(width: Float) {
        calls++
        gl20.glLineWidth(width)
        check()
    }

    override fun glPixelStorei(pname: Int, param: Int) {
        calls++
        gl20.glPixelStorei(pname, param)
        check()
    }

    override fun glPolygonOffset(factor: Float, units: Float) {
        calls++
        gl20.glPolygonOffset(factor, units)
        check()
    }

    override fun glReadPixels(x: Int, y: Int, width: Int, height: Int, format: Int, type: Int, pixels: java.nio.Buffer) {
        calls++
        gl20.glReadPixels(x, y, width, height, format, type, pixels)
        check()
    }

    override fun glScissor(x: Int, y: Int, width: Int, height: Int) {
        calls++
        gl20.glScissor(x, y, width, height)
        check()
    }

    override fun glStencilFunc(func: Int, ref: Int, mask: Int) {
        calls++
        gl20.glStencilFunc(func, ref, mask)
        check()
    }

    override fun glStencilMask(mask: Int) {
        calls++
        gl20.glStencilMask(mask)
        check()
    }

    override fun glStencilOp(fail: Int, zfail: Int, zpass: Int) {
        calls++
        gl20.glStencilOp(fail, zfail, zpass)
        check()
    }

    override fun glTexImage2D(target: Int, level: Int, internalformat: Int, width: Int, height: Int, border: Int, format: Int, type: Int,
                              pixels: java.nio.Buffer) {
        calls++
        gl20.glTexImage2D(target, level, internalformat, width, height, border, format, type, pixels)
        check()
    }

    override fun glTexParameterf(target: Int, pname: Int, param: Float) {
        calls++
        gl20.glTexParameterf(target, pname, param)
        check()
    }

    override fun glTexSubImage2D(target: Int, level: Int, xoffset: Int, yoffset: Int, width: Int, height: Int, format: Int, type: Int,
                                 pixels: java.nio.Buffer) {
        calls++
        gl20.glTexSubImage2D(target, level, xoffset, yoffset, width, height, format, type, pixels)
        check()
    }

    override fun glViewport(x: Int, y: Int, width: Int, height: Int) {
        calls++
        gl20.glViewport(x, y, width, height)
        check()
    }

    override fun glAttachShader(program: Int, shader: Int) {
        calls++
        gl20.glAttachShader(program, shader)
        check()
    }

    override fun glBindAttribLocation(program: Int, index: Int, name: String) {
        calls++
        gl20.glBindAttribLocation(program, index, name)
        check()
    }

    override fun glBindBuffer(target: Int, buffer: Int) {
        calls++
        gl20.glBindBuffer(target, buffer)
        check()
    }

    override fun glBindFramebuffer(target: Int, framebuffer: Int) {
        calls++
        gl20.glBindFramebuffer(target, framebuffer)
        check()
    }

    override fun glBindRenderbuffer(target: Int, renderbuffer: Int) {
        calls++
        gl20.glBindRenderbuffer(target, renderbuffer)
        check()
    }

    override fun glBlendColor(red: Float, green: Float, blue: Float, alpha: Float) {
        calls++
        gl20.glBlendColor(red, green, blue, alpha)
        check()
    }

    override fun glBlendEquation(mode: Int) {
        calls++
        gl20.glBlendEquation(mode)
        check()
    }

    override fun glBlendEquationSeparate(modeRGB: Int, modeAlpha: Int) {
        calls++
        gl20.glBlendEquationSeparate(modeRGB, modeAlpha)
        check()
    }

    override fun glBlendFuncSeparate(srcRGB: Int, dstRGB: Int, srcAlpha: Int, dstAlpha: Int) {
        calls++
        gl20.glBlendFuncSeparate(srcRGB, dstRGB, srcAlpha, dstAlpha)
        check()
    }

    override fun glBufferData(target: Int, size: Int, data: java.nio.Buffer, usage: Int) {
        calls++
        gl20.glBufferData(target, size, data, usage)
        check()
    }

    override fun glBufferSubData(target: Int, offset: Int, size: Int, data: java.nio.Buffer) {
        calls++
        gl20.glBufferSubData(target, offset, size, data)
        check()
    }

    override fun glCheckFramebufferStatus(target: Int): Int {
        calls++
        val result: Int = gl20.glCheckFramebufferStatus(target)
        check()
        return result
    }

    override fun glCompileShader(shader: Int) {
        calls++
        gl20.glCompileShader(shader)
        check()
    }

    override fun glCreateProgram(): Int {
        calls++
        val result: Int = gl20.glCreateProgram()
        check()
        return result
    }

    override fun glCreateShader(type: Int): Int {
        calls++
        val result: Int = gl20.glCreateShader(type)
        check()
        return result
    }

    override fun glDeleteBuffer(buffer: Int) {
        calls++
        gl20.glDeleteBuffer(buffer)
        check()
    }

    override fun glDeleteBuffers(n: Int, buffers: java.nio.IntBuffer) {
        calls++
        gl20.glDeleteBuffers(n, buffers)
        check()
    }

    override fun glDeleteFramebuffer(framebuffer: Int) {
        calls++
        gl20.glDeleteFramebuffer(framebuffer)
        check()
    }

    override fun glDeleteFramebuffers(n: Int, framebuffers: java.nio.IntBuffer) {
        calls++
        gl20.glDeleteFramebuffers(n, framebuffers)
        check()
    }

    override fun glDeleteProgram(program: Int) {
        calls++
        gl20.glDeleteProgram(program)
        check()
    }

    override fun glDeleteRenderbuffer(renderbuffer: Int) {
        calls++
        gl20.glDeleteRenderbuffer(renderbuffer)
        check()
    }

    override fun glDeleteRenderbuffers(n: Int, renderbuffers: java.nio.IntBuffer) {
        calls++
        gl20.glDeleteRenderbuffers(n, renderbuffers)
        check()
    }

    override fun glDeleteShader(shader: Int) {
        calls++
        gl20.glDeleteShader(shader)
        check()
    }

    override fun glDetachShader(program: Int, shader: Int) {
        calls++
        gl20.glDetachShader(program, shader)
        check()
    }

    override fun glDisableVertexAttribArray(index: Int) {
        calls++
        gl20.glDisableVertexAttribArray(index)
        check()
    }

    override fun glDrawElements(mode: Int, count: Int, type: Int, indices: Int) {
        vertexCount.put(count.toFloat())
        drawCalls++
        calls++
        gl20.glDrawElements(mode, count, type, indices)
        check()
    }

    override fun glEnableVertexAttribArray(index: Int) {
        calls++
        gl20.glEnableVertexAttribArray(index)
        check()
    }

    override fun glFramebufferRenderbuffer(target: Int, attachment: Int, renderbuffertarget: Int, renderbuffer: Int) {
        calls++
        gl20.glFramebufferRenderbuffer(target, attachment, renderbuffertarget, renderbuffer)
        check()
    }

    override fun glFramebufferTexture2D(target: Int, attachment: Int, textarget: Int, texture: Int, level: Int) {
        calls++
        gl20.glFramebufferTexture2D(target, attachment, textarget, texture, level)
        check()
    }

    override fun glGenBuffer(): Int {
        calls++
        val result: Int = gl20.glGenBuffer()
        check()
        return result
    }

    override fun glGenBuffers(n: Int, buffers: java.nio.IntBuffer) {
        calls++
        gl20.glGenBuffers(n, buffers)
        check()
    }

    override fun glGenerateMipmap(target: Int) {
        calls++
        gl20.glGenerateMipmap(target)
        check()
    }

    override fun glGenFramebuffer(): Int {
        calls++
        val result: Int = gl20.glGenFramebuffer()
        check()
        return result
    }

    override fun glGenFramebuffers(n: Int, framebuffers: java.nio.IntBuffer) {
        calls++
        gl20.glGenFramebuffers(n, framebuffers)
        check()
    }

    override fun glGenRenderbuffer(): Int {
        calls++
        val result: Int = gl20.glGenRenderbuffer()
        check()
        return result
    }

    override fun glGenRenderbuffers(n: Int, renderbuffers: java.nio.IntBuffer) {
        calls++
        gl20.glGenRenderbuffers(n, renderbuffers)
        check()
    }

    override fun glGetActiveAttrib(program: Int, index: Int, size: java.nio.IntBuffer, type: java.nio.Buffer): String {
        calls++
        val result: String = gl20.glGetActiveAttrib(program, index, size, type)
        check()
        return result
    }

    override fun glGetActiveUniform(program: Int, index: Int, size: java.nio.IntBuffer, type: java.nio.Buffer): String {
        calls++
        val result: String = gl20.glGetActiveUniform(program, index, size, type)
        check()
        return result
    }

    override fun glGetAttachedShaders(program: Int, maxcount: Int, count: java.nio.Buffer, shaders: java.nio.IntBuffer) {
        calls++
        gl20.glGetAttachedShaders(program, maxcount, count, shaders)
        check()
    }

    override fun glGetAttribLocation(program: Int, name: String): Int {
        calls++
        val result: Int = gl20.glGetAttribLocation(program, name)
        check()
        return result
    }

    override fun glGetBooleanv(pname: Int, params: java.nio.Buffer) {
        calls++
        gl20.glGetBooleanv(pname, params)
        check()
    }

    override fun glGetBufferParameteriv(target: Int, pname: Int, params: java.nio.IntBuffer) {
        calls++
        gl20.glGetBufferParameteriv(target, pname, params)
        check()
    }

    override fun glGetFloatv(pname: Int, params: FloatBuffer) {
        calls++
        gl20.glGetFloatv(pname, params)
        check()
    }

    override fun glGetFramebufferAttachmentParameteriv(target: Int, attachment: Int, pname: Int, params: java.nio.IntBuffer) {
        calls++
        gl20.glGetFramebufferAttachmentParameteriv(target, attachment, pname, params)
        check()
    }

    override fun glGetProgramiv(program: Int, pname: Int, params: java.nio.IntBuffer) {
        calls++
        gl20.glGetProgramiv(program, pname, params)
        check()
    }

    override fun glGetProgramInfoLog(program: Int): String {
        calls++
        val result: String = gl20.glGetProgramInfoLog(program)
        check()
        return result
    }

    override fun glGetRenderbufferParameteriv(target: Int, pname: Int, params: java.nio.IntBuffer) {
        calls++
        gl20.glGetRenderbufferParameteriv(target, pname, params)
        check()
    }

    override fun glGetShaderiv(shader: Int, pname: Int, params: java.nio.IntBuffer) {
        calls++
        gl20.glGetShaderiv(shader, pname, params)
        check()
    }

    override fun glGetShaderInfoLog(shader: Int): String {
        calls++
        val result: String = gl20.glGetShaderInfoLog(shader)
        check()
        return result
    }

    override fun glGetShaderPrecisionFormat(shadertype: Int, precisiontype: Int, range: java.nio.IntBuffer, precision: java.nio.IntBuffer) {
        calls++
        gl20.glGetShaderPrecisionFormat(shadertype, precisiontype, range, precision)
        check()
    }

    override fun glGetTexParameterfv(target: Int, pname: Int, params: FloatBuffer) {
        calls++
        gl20.glGetTexParameterfv(target, pname, params)
        check()
    }

    override fun glGetTexParameteriv(target: Int, pname: Int, params: java.nio.IntBuffer) {
        calls++
        gl20.glGetTexParameteriv(target, pname, params)
        check()
    }

    override fun glGetUniformfv(program: Int, location: Int, params: FloatBuffer) {
        calls++
        gl20.glGetUniformfv(program, location, params)
        check()
    }

    override fun glGetUniformiv(program: Int, location: Int, params: java.nio.IntBuffer) {
        calls++
        gl20.glGetUniformiv(program, location, params)
        check()
    }

    override fun glGetUniformLocation(program: Int, name: String): Int {
        calls++
        val result: Int = gl20.glGetUniformLocation(program, name)
        check()
        return result
    }

    override fun glGetVertexAttribfv(index: Int, pname: Int, params: FloatBuffer) {
        calls++
        gl20.glGetVertexAttribfv(index, pname, params)
        check()
    }

    override fun glGetVertexAttribiv(index: Int, pname: Int, params: java.nio.IntBuffer) {
        calls++
        gl20.glGetVertexAttribiv(index, pname, params)
        check()
    }

    override fun glGetVertexAttribPointerv(index: Int, pname: Int, pointer: java.nio.Buffer) {
        calls++
        gl20.glGetVertexAttribPointerv(index, pname, pointer)
        check()
    }

    override fun glIsBuffer(buffer: Int): Boolean {
        calls++
        val result: Boolean = gl20.glIsBuffer(buffer)
        check()
        return result
    }

    override fun glIsEnabled(cap: Int): Boolean {
        calls++
        val result: Boolean = gl20.glIsEnabled(cap)
        check()
        return result
    }

    override fun glIsFramebuffer(framebuffer: Int): Boolean {
        calls++
        val result: Boolean = gl20.glIsFramebuffer(framebuffer)
        check()
        return result
    }

    override fun glIsProgram(program: Int): Boolean {
        calls++
        val result: Boolean = gl20.glIsProgram(program)
        check()
        return result
    }

    override fun glIsRenderbuffer(renderbuffer: Int): Boolean {
        calls++
        val result: Boolean = gl20.glIsRenderbuffer(renderbuffer)
        check()
        return result
    }

    override fun glIsShader(shader: Int): Boolean {
        calls++
        val result: Boolean = gl20.glIsShader(shader)
        check()
        return result
    }

    override fun glIsTexture(texture: Int): Boolean {
        calls++
        val result: Boolean = gl20.glIsTexture(texture)
        check()
        return result
    }

    override fun glLinkProgram(program: Int) {
        calls++
        gl20.glLinkProgram(program)
        check()
    }

    override fun glReleaseShaderCompiler() {
        calls++
        gl20.glReleaseShaderCompiler()
        check()
    }

    override fun glRenderbufferStorage(target: Int, internalformat: Int, width: Int, height: Int) {
        calls++
        gl20.glRenderbufferStorage(target, internalformat, width, height)
        check()
    }

    override fun glSampleCoverage(value: Float, invert: Boolean) {
        calls++
        gl20.glSampleCoverage(value, invert)
        check()
    }

    override fun glShaderBinary(n: Int, shaders: java.nio.IntBuffer, binaryformat: Int, binary: java.nio.Buffer, length: Int) {
        calls++
        gl20.glShaderBinary(n, shaders, binaryformat, binary, length)
        check()
    }

    override fun glShaderSource(shader: Int, string: String) {
        calls++
        gl20.glShaderSource(shader, string)
        check()
    }

    override fun glStencilFuncSeparate(face: Int, func: Int, ref: Int, mask: Int) {
        calls++
        gl20.glStencilFuncSeparate(face, func, ref, mask)
        check()
    }

    override fun glStencilMaskSeparate(face: Int, mask: Int) {
        calls++
        gl20.glStencilMaskSeparate(face, mask)
        check()
    }

    override fun glStencilOpSeparate(face: Int, fail: Int, zfail: Int, zpass: Int) {
        calls++
        gl20.glStencilOpSeparate(face, fail, zfail, zpass)
        check()
    }

    override fun glTexParameterfv(target: Int, pname: Int, params: FloatBuffer) {
        calls++
        gl20.glTexParameterfv(target, pname, params)
        check()
    }

    override fun glTexParameteri(target: Int, pname: Int, param: Int) {
        calls++
        gl20.glTexParameteri(target, pname, param)
        check()
    }

    override fun glTexParameteriv(target: Int, pname: Int, params: java.nio.IntBuffer) {
        calls++
        gl20.glTexParameteriv(target, pname, params)
        check()
    }

    override fun glUniform1f(location: Int, x: Float) {
        calls++
        gl20.glUniform1f(location, x)
        check()
    }

    override fun glUniform1fv(location: Int, count: Int, v: FloatBuffer) {
        calls++
        gl20.glUniform1fv(location, count, v)
        check()
    }

    override fun glUniform1fv(location: Int, count: Int, v: FloatArray, offset: Int) {
        calls++
        gl20.glUniform1fv(location, count, v, offset)
        check()
    }

    override fun glUniform1i(location: Int, x: Int) {
        calls++
        gl20.glUniform1i(location, x)
        check()
    }

    override fun glUniform1iv(location: Int, count: Int, v: java.nio.IntBuffer) {
        calls++
        gl20.glUniform1iv(location, count, v)
        check()
    }

    override fun glUniform1iv(location: Int, count: Int, v: IntArray, offset: Int) {
        calls++
        gl20.glUniform1iv(location, count, v, offset)
        check()
    }

    override fun glUniform2f(location: Int, x: Float, y: Float) {
        calls++
        gl20.glUniform2f(location, x, y)
        check()
    }

    override fun glUniform2fv(location: Int, count: Int, v: FloatBuffer) {
        calls++
        gl20.glUniform2fv(location, count, v)
        check()
    }

    override fun glUniform2fv(location: Int, count: Int, v: FloatArray, offset: Int) {
        calls++
        gl20.glUniform2fv(location, count, v, offset)
        check()
    }

    override fun glUniform2i(location: Int, x: Int, y: Int) {
        calls++
        gl20.glUniform2i(location, x, y)
        check()
    }

    override fun glUniform2iv(location: Int, count: Int, v: java.nio.IntBuffer) {
        calls++
        gl20.glUniform2iv(location, count, v)
        check()
    }

    override fun glUniform2iv(location: Int, count: Int, v: IntArray, offset: Int) {
        calls++
        gl20.glUniform2iv(location, count, v, offset)
        check()
    }

    override fun glUniform3f(location: Int, x: Float, y: Float, z: Float) {
        calls++
        gl20.glUniform3f(location, x, y, z)
        check()
    }

    override fun glUniform3fv(location: Int, count: Int, v: FloatBuffer) {
        calls++
        gl20.glUniform3fv(location, count, v)
        check()
    }

    override fun glUniform3fv(location: Int, count: Int, v: FloatArray, offset: Int) {
        calls++
        gl20.glUniform3fv(location, count, v, offset)
        check()
    }

    override fun glUniform3i(location: Int, x: Int, y: Int, z: Int) {
        calls++
        gl20.glUniform3i(location, x, y, z)
        check()
    }

    override fun glUniform3iv(location: Int, count: Int, v: java.nio.IntBuffer) {
        calls++
        gl20.glUniform3iv(location, count, v)
        check()
    }

    override fun glUniform3iv(location: Int, count: Int, v: IntArray, offset: Int) {
        calls++
        gl20.glUniform3iv(location, count, v, offset)
        check()
    }

    override fun glUniform4f(location: Int, x: Float, y: Float, z: Float, w: Float) {
        calls++
        gl20.glUniform4f(location, x, y, z, w)
        check()
    }

    override fun glUniform4fv(location: Int, count: Int, v: FloatBuffer) {
        calls++
        gl20.glUniform4fv(location, count, v)
        check()
    }

    override fun glUniform4fv(location: Int, count: Int, v: FloatArray, offset: Int) {
        calls++
        gl20.glUniform4fv(location, count, v, offset)
        check()
    }

    override fun glUniform4i(location: Int, x: Int, y: Int, z: Int, w: Int) {
        calls++
        gl20.glUniform4i(location, x, y, z, w)
        check()
    }

    override fun glUniform4iv(location: Int, count: Int, v: java.nio.IntBuffer) {
        calls++
        gl20.glUniform4iv(location, count, v)
        check()
    }

    override fun glUniform4iv(location: Int, count: Int, v: IntArray, offset: Int) {
        calls++
        gl20.glUniform4iv(location, count, v, offset)
        check()
    }

    override fun glUniformMatrix2fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer) {
        calls++
        gl20.glUniformMatrix2fv(location, count, transpose, value)
        check()
    }

    override fun glUniformMatrix2fv(location: Int, count: Int, transpose: Boolean, value: FloatArray, offset: Int) {
        calls++
        gl20.glUniformMatrix2fv(location, count, transpose, value, offset)
        check()
    }

    override fun glUniformMatrix3fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer) {
        calls++
        gl20.glUniformMatrix3fv(location, count, transpose, value)
        check()
    }

    override fun glUniformMatrix3fv(location: Int, count: Int, transpose: Boolean, value: FloatArray, offset: Int) {
        calls++
        gl20.glUniformMatrix3fv(location, count, transpose, value, offset)
        check()
    }

    override fun glUniformMatrix4fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer) {
        calls++
        gl20.glUniformMatrix4fv(location, count, transpose, value)
        check()
    }

    override fun glUniformMatrix4fv(location: Int, count: Int, transpose: Boolean, value: FloatArray, offset: Int) {
        calls++
        gl20.glUniformMatrix4fv(location, count, transpose, value, offset)
        check()
    }

    override fun glUseProgram(program: Int) {
        shaderSwitches++
        calls++
        gl20.glUseProgram(program)
        check()
    }

    override fun glValidateProgram(program: Int) {
        calls++
        gl20.glValidateProgram(program)
        check()
    }

    override fun glVertexAttrib1f(indx: Int, x: Float) {
        calls++
        gl20.glVertexAttrib1f(indx, x)
        check()
    }

    override fun glVertexAttrib1fv(indx: Int, values: FloatBuffer) {
        calls++
        gl20.glVertexAttrib1fv(indx, values)
        check()
    }

    override fun glVertexAttrib2f(indx: Int, x: Float, y: Float) {
        calls++
        gl20.glVertexAttrib2f(indx, x, y)
        check()
    }

    override fun glVertexAttrib2fv(indx: Int, values: FloatBuffer) {
        calls++
        gl20.glVertexAttrib2fv(indx, values)
        check()
    }

    override fun glVertexAttrib3f(indx: Int, x: Float, y: Float, z: Float) {
        calls++
        gl20.glVertexAttrib3f(indx, x, y, z)
        check()
    }

    override fun glVertexAttrib3fv(indx: Int, values: FloatBuffer) {
        calls++
        gl20.glVertexAttrib3fv(indx, values)
        check()
    }

    override fun glVertexAttrib4f(indx: Int, x: Float, y: Float, z: Float, w: Float) {
        calls++
        gl20.glVertexAttrib4f(indx, x, y, z, w)
        check()
    }

    override fun glVertexAttrib4fv(indx: Int, values: FloatBuffer) {
        calls++
        gl20.glVertexAttrib4fv(indx, values)
        check()
    }

    override fun glVertexAttribPointer(indx: Int, size: Int, type: Int, normalized: Boolean, stride: Int, ptr: java.nio.Buffer) {
        calls++
        gl20.glVertexAttribPointer(indx, size, type, normalized, stride, ptr)
        check()
    }

    override fun glVertexAttribPointer(indx: Int, size: Int, type: Int, normalized: Boolean, stride: Int, ptr: Int) {
        calls++
        gl20.glVertexAttribPointer(indx, size, type, normalized, stride, ptr)
        check()
    }

    init {
        this.gl20 = gl20
    }
}
