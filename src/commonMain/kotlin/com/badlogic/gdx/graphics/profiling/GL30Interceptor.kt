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
class GL30Interceptor(glProfiler: com.badlogic.gdx.graphics.profiling.GLProfiler, gl30: com.badlogic.gdx.graphics.GL30) : com.badlogic.gdx.graphics.profiling.GLInterceptor(glProfiler), com.badlogic.gdx.graphics.GL30 {

    val gl30: com.badlogic.gdx.graphics.GL30
    private fun check() {
        var error: Int = gl30.glGetError()
        while (error != com.badlogic.gdx.graphics.GL20.GL_NO_ERROR) {
            glProfiler.getListener().onError(error)
            error = gl30.glGetError()
        }
    }

    override fun glActiveTexture(texture: Int) {
        calls++
        gl30.glActiveTexture(texture)
        check()
    }

    override fun glBindTexture(target: Int, texture: Int) {
        textureBindings++
        calls++
        gl30.glBindTexture(target, texture)
        check()
    }

    override fun glBlendFunc(sfactor: Int, dfactor: Int) {
        calls++
        gl30.glBlendFunc(sfactor, dfactor)
        check()
    }

    override fun glClear(mask: Int) {
        calls++
        gl30.glClear(mask)
        check()
    }

    override fun glClearColor(red: Float, green: Float, blue: Float, alpha: Float) {
        calls++
        gl30.glClearColor(red, green, blue, alpha)
        check()
    }

    override fun glClearDepthf(depth: Float) {
        calls++
        gl30.glClearDepthf(depth)
        check()
    }

    override fun glClearStencil(s: Int) {
        calls++
        gl30.glClearStencil(s)
        check()
    }

    override fun glColorMask(red: Boolean, green: Boolean, blue: Boolean, alpha: Boolean) {
        calls++
        gl30.glColorMask(red, green, blue, alpha)
        check()
    }

    override fun glCompressedTexImage2D(target: Int, level: Int, internalformat: Int, width: Int, height: Int, border: Int,
                                        imageSize: Int, data: java.nio.Buffer) {
        calls++
        gl30.glCompressedTexImage2D(target, level, internalformat, width, height, border, imageSize, data)
        check()
    }

    override fun glCompressedTexSubImage2D(target: Int, level: Int, xoffset: Int, yoffset: Int, width: Int, height: Int, format: Int,
                                           imageSize: Int, data: java.nio.Buffer) {
        calls++
        gl30.glCompressedTexSubImage2D(target, level, xoffset, yoffset, width, height, format, imageSize, data)
        check()
    }

    override fun glCopyTexImage2D(target: Int, level: Int, internalformat: Int, x: Int, y: Int, width: Int, height: Int, border: Int) {
        calls++
        gl30.glCopyTexImage2D(target, level, internalformat, x, y, width, height, border)
        check()
    }

    override fun glCopyTexSubImage2D(target: Int, level: Int, xoffset: Int, yoffset: Int, x: Int, y: Int, width: Int, height: Int) {
        calls++
        gl30.glCopyTexSubImage2D(target, level, xoffset, yoffset, x, y, width, height)
        check()
    }

    override fun glCullFace(mode: Int) {
        calls++
        gl30.glCullFace(mode)
        check()
    }

    override fun glDeleteTextures(n: Int, textures: java.nio.IntBuffer) {
        calls++
        gl30.glDeleteTextures(n, textures)
        check()
    }

    override fun glDeleteTexture(texture: Int) {
        calls++
        gl30.glDeleteTexture(texture)
        check()
    }

    override fun glDepthFunc(func: Int) {
        calls++
        gl30.glDepthFunc(func)
        check()
    }

    override fun glDepthMask(flag: Boolean) {
        calls++
        gl30.glDepthMask(flag)
        check()
    }

    override fun glDepthRangef(zNear: Float, zFar: Float) {
        calls++
        gl30.glDepthRangef(zNear, zFar)
        check()
    }

    override fun glDisable(cap: Int) {
        calls++
        gl30.glDisable(cap)
        check()
    }

    override fun glDrawArrays(mode: Int, first: Int, count: Int) {
        vertexCount.put(count.toFloat())
        drawCalls++
        calls++
        gl30.glDrawArrays(mode, first, count)
        check()
    }

    override fun glDrawElements(mode: Int, count: Int, type: Int, indices: java.nio.Buffer) {
        vertexCount.put(count.toFloat())
        drawCalls++
        calls++
        gl30.glDrawElements(mode, count, type, indices)
        check()
    }

    override fun glEnable(cap: Int) {
        calls++
        gl30.glEnable(cap)
        check()
    }

    override fun glFinish() {
        calls++
        gl30.glFinish()
        check()
    }

    override fun glFlush() {
        calls++
        gl30.glFlush()
        check()
    }

    override fun glFrontFace(mode: Int) {
        calls++
        gl30.glFrontFace(mode)
        check()
    }

    override fun glGenTextures(n: Int, textures: java.nio.IntBuffer) {
        calls++
        gl30.glGenTextures(n, textures)
        check()
    }

    override fun glGenTexture(): Int {
        calls++
        val result: Int = gl30.glGenTexture()
        check()
        return result
    }

    override fun glGetError(): Int {
        calls++
        //Errors by glGetError are undetectable
        return gl30.glGetError()
    }

    override fun glGetIntegerv(pname: Int, params: java.nio.IntBuffer) {
        calls++
        gl30.glGetIntegerv(pname, params)
        check()
    }

    override fun glGetString(name: Int): String {
        calls++
        val result: String = gl30.glGetString(name)
        check()
        return result
    }

    override fun glHint(target: Int, mode: Int) {
        calls++
        gl30.glHint(target, mode)
        check()
    }

    override fun glLineWidth(width: Float) {
        calls++
        gl30.glLineWidth(width)
        check()
    }

    override fun glPixelStorei(pname: Int, param: Int) {
        calls++
        gl30.glPixelStorei(pname, param)
        check()
    }

    override fun glPolygonOffset(factor: Float, units: Float) {
        calls++
        gl30.glPolygonOffset(factor, units)
        check()
    }

    override fun glReadPixels(x: Int, y: Int, width: Int, height: Int, format: Int, type: Int, pixels: java.nio.Buffer) {
        calls++
        gl30.glReadPixels(x, y, width, height, format, type, pixels)
        check()
    }

    override fun glScissor(x: Int, y: Int, width: Int, height: Int) {
        calls++
        gl30.glScissor(x, y, width, height)
        check()
    }

    override fun glStencilFunc(func: Int, ref: Int, mask: Int) {
        calls++
        gl30.glStencilFunc(func, ref, mask)
        check()
    }

    override fun glStencilMask(mask: Int) {
        calls++
        gl30.glStencilMask(mask)
        check()
    }

    override fun glStencilOp(fail: Int, zfail: Int, zpass: Int) {
        calls++
        gl30.glStencilOp(fail, zfail, zpass)
        check()
    }

    override fun glTexImage2D(target: Int, level: Int, internalformat: Int, width: Int, height: Int, border: Int, format: Int, type: Int,
                              pixels: java.nio.Buffer) {
        calls++
        gl30.glTexImage2D(target, level, internalformat, width, height, border, format, type, pixels)
        check()
    }

    override fun glTexParameterf(target: Int, pname: Int, param: Float) {
        calls++
        gl30.glTexParameterf(target, pname, param)
        check()
    }

    override fun glTexSubImage2D(target: Int, level: Int, xoffset: Int, yoffset: Int, width: Int, height: Int, format: Int, type: Int,
                                 pixels: java.nio.Buffer) {
        calls++
        gl30.glTexSubImage2D(target, level, xoffset, yoffset, width, height, format, type, pixels)
        check()
    }

    override fun glViewport(x: Int, y: Int, width: Int, height: Int) {
        calls++
        gl30.glViewport(x, y, width, height)
        check()
    }

    override fun glAttachShader(program: Int, shader: Int) {
        calls++
        gl30.glAttachShader(program, shader)
        check()
    }

    override fun glBindAttribLocation(program: Int, index: Int, name: String) {
        calls++
        gl30.glBindAttribLocation(program, index, name)
        check()
    }

    override fun glBindBuffer(target: Int, buffer: Int) {
        calls++
        gl30.glBindBuffer(target, buffer)
        check()
    }

    override fun glBindFramebuffer(target: Int, framebuffer: Int) {
        calls++
        gl30.glBindFramebuffer(target, framebuffer)
        check()
    }

    override fun glBindRenderbuffer(target: Int, renderbuffer: Int) {
        calls++
        gl30.glBindRenderbuffer(target, renderbuffer)
        check()
    }

    override fun glBlendColor(red: Float, green: Float, blue: Float, alpha: Float) {
        calls++
        gl30.glBlendColor(red, green, blue, alpha)
        check()
    }

    override fun glBlendEquation(mode: Int) {
        calls++
        gl30.glBlendEquation(mode)
        check()
    }

    override fun glBlendEquationSeparate(modeRGB: Int, modeAlpha: Int) {
        calls++
        gl30.glBlendEquationSeparate(modeRGB, modeAlpha)
        check()
    }

    override fun glBlendFuncSeparate(srcRGB: Int, dstRGB: Int, srcAlpha: Int, dstAlpha: Int) {
        calls++
        gl30.glBlendFuncSeparate(srcRGB, dstRGB, srcAlpha, dstAlpha)
        check()
    }

    override fun glBufferData(target: Int, size: Int, data: java.nio.Buffer, usage: Int) {
        calls++
        gl30.glBufferData(target, size, data, usage)
        check()
    }

    override fun glBufferSubData(target: Int, offset: Int, size: Int, data: java.nio.Buffer) {
        calls++
        gl30.glBufferSubData(target, offset, size, data)
        check()
    }

    override fun glCheckFramebufferStatus(target: Int): Int {
        calls++
        val result: Int = gl30.glCheckFramebufferStatus(target)
        check()
        return result
    }

    override fun glCompileShader(shader: Int) {
        calls++
        gl30.glCompileShader(shader)
        check()
    }

    override fun glCreateProgram(): Int {
        calls++
        val result: Int = gl30.glCreateProgram()
        check()
        return result
    }

    override fun glCreateShader(type: Int): Int {
        calls++
        val result: Int = gl30.glCreateShader(type)
        check()
        return result
    }

    override fun glDeleteBuffer(buffer: Int) {
        calls++
        gl30.glDeleteBuffer(buffer)
        check()
    }

    override fun glDeleteBuffers(n: Int, buffers: java.nio.IntBuffer) {
        calls++
        gl30.glDeleteBuffers(n, buffers)
        check()
    }

    override fun glDeleteFramebuffer(framebuffer: Int) {
        calls++
        gl30.glDeleteFramebuffer(framebuffer)
        check()
    }

    override fun glDeleteFramebuffers(n: Int, framebuffers: java.nio.IntBuffer) {
        calls++
        gl30.glDeleteFramebuffers(n, framebuffers)
        check()
    }

    override fun glDeleteProgram(program: Int) {
        calls++
        gl30.glDeleteProgram(program)
        check()
    }

    override fun glDeleteRenderbuffer(renderbuffer: Int) {
        calls++
        gl30.glDeleteRenderbuffer(renderbuffer)
        check()
    }

    override fun glDeleteRenderbuffers(n: Int, renderbuffers: java.nio.IntBuffer) {
        calls++
        gl30.glDeleteRenderbuffers(n, renderbuffers)
        check()
    }

    override fun glDeleteShader(shader: Int) {
        calls++
        gl30.glDeleteShader(shader)
        check()
    }

    override fun glDetachShader(program: Int, shader: Int) {
        calls++
        gl30.glDetachShader(program, shader)
        check()
    }

    override fun glDisableVertexAttribArray(index: Int) {
        calls++
        gl30.glDisableVertexAttribArray(index)
        check()
    }

    override fun glDrawElements(mode: Int, count: Int, type: Int, indices: Int) {
        vertexCount.put(count.toFloat())
        drawCalls++
        calls++
        gl30.glDrawElements(mode, count, type, indices)
        check()
    }

    override fun glEnableVertexAttribArray(index: Int) {
        calls++
        gl30.glEnableVertexAttribArray(index)
        check()
    }

    override fun glFramebufferRenderbuffer(target: Int, attachment: Int, renderbuffertarget: Int, renderbuffer: Int) {
        calls++
        gl30.glFramebufferRenderbuffer(target, attachment, renderbuffertarget, renderbuffer)
        check()
    }

    override fun glFramebufferTexture2D(target: Int, attachment: Int, textarget: Int, texture: Int, level: Int) {
        calls++
        gl30.glFramebufferTexture2D(target, attachment, textarget, texture, level)
        check()
    }

    override fun glGenBuffer(): Int {
        calls++
        val result: Int = gl30.glGenBuffer()
        check()
        return result
    }

    override fun glGenBuffers(n: Int, buffers: java.nio.IntBuffer) {
        calls++
        gl30.glGenBuffers(n, buffers)
        check()
    }

    override fun glGenerateMipmap(target: Int) {
        calls++
        gl30.glGenerateMipmap(target)
        check()
    }

    override fun glGenFramebuffer(): Int {
        calls++
        val result: Int = gl30.glGenFramebuffer()
        check()
        return result
    }

    override fun glGenFramebuffers(n: Int, framebuffers: java.nio.IntBuffer) {
        calls++
        gl30.glGenFramebuffers(n, framebuffers)
        check()
    }

    override fun glGenRenderbuffer(): Int {
        calls++
        val result: Int = gl30.glGenRenderbuffer()
        check()
        return result
    }

    override fun glGenRenderbuffers(n: Int, renderbuffers: java.nio.IntBuffer) {
        calls++
        gl30.glGenRenderbuffers(n, renderbuffers)
        check()
    }

    override fun glGetActiveAttrib(program: Int, index: Int, size: java.nio.IntBuffer, type: java.nio.Buffer): String {
        calls++
        val result: String = gl30.glGetActiveAttrib(program, index, size, type)
        check()
        return result
    }

    override fun glGetActiveUniform(program: Int, index: Int, size: java.nio.IntBuffer, type: java.nio.Buffer): String {
        calls++
        val result: String = gl30.glGetActiveUniform(program, index, size, type)
        check()
        return result
    }

    override fun glGetAttachedShaders(program: Int, maxcount: Int, count: java.nio.Buffer, shaders: java.nio.IntBuffer) {
        calls++
        gl30.glGetAttachedShaders(program, maxcount, count, shaders)
        check()
    }

    override fun glGetAttribLocation(program: Int, name: String): Int {
        calls++
        val result: Int = gl30.glGetAttribLocation(program, name)
        check()
        return result
    }

    override fun glGetBooleanv(pname: Int, params: java.nio.Buffer) {
        calls++
        gl30.glGetBooleanv(pname, params)
        check()
    }

    override fun glGetBufferParameteriv(target: Int, pname: Int, params: java.nio.IntBuffer) {
        calls++
        gl30.glGetBufferParameteriv(target, pname, params)
        check()
    }

    override fun glGetFloatv(pname: Int, params: FloatBuffer) {
        calls++
        gl30.glGetFloatv(pname, params)
        check()
    }

    override fun glGetFramebufferAttachmentParameteriv(target: Int, attachment: Int, pname: Int, params: java.nio.IntBuffer) {
        calls++
        gl30.glGetFramebufferAttachmentParameteriv(target, attachment, pname, params)
        check()
    }

    override fun glGetProgramiv(program: Int, pname: Int, params: java.nio.IntBuffer) {
        calls++
        gl30.glGetProgramiv(program, pname, params)
        check()
    }

    override fun glGetProgramInfoLog(program: Int): String {
        calls++
        val result: String = gl30.glGetProgramInfoLog(program)
        check()
        return result
    }

    override fun glGetRenderbufferParameteriv(target: Int, pname: Int, params: java.nio.IntBuffer) {
        calls++
        gl30.glGetRenderbufferParameteriv(target, pname, params)
        check()
    }

    override fun glGetShaderiv(shader: Int, pname: Int, params: java.nio.IntBuffer) {
        calls++
        gl30.glGetShaderiv(shader, pname, params)
        check()
    }

    override fun glGetShaderInfoLog(shader: Int): String {
        calls++
        val result: String = gl30.glGetShaderInfoLog(shader)
        check()
        return result
    }

    override fun glGetShaderPrecisionFormat(shadertype: Int, precisiontype: Int, range: java.nio.IntBuffer, precision: java.nio.IntBuffer) {
        calls++
        gl30.glGetShaderPrecisionFormat(shadertype, precisiontype, range, precision)
        check()
    }

    override fun glGetTexParameterfv(target: Int, pname: Int, params: FloatBuffer) {
        calls++
        gl30.glGetTexParameterfv(target, pname, params)
        check()
    }

    override fun glGetTexParameteriv(target: Int, pname: Int, params: java.nio.IntBuffer) {
        calls++
        gl30.glGetTexParameteriv(target, pname, params)
        check()
    }

    override fun glGetUniformfv(program: Int, location: Int, params: FloatBuffer) {
        calls++
        gl30.glGetUniformfv(program, location, params)
        check()
    }

    override fun glGetUniformiv(program: Int, location: Int, params: java.nio.IntBuffer) {
        calls++
        gl30.glGetUniformiv(program, location, params)
        check()
    }

    override fun glGetUniformLocation(program: Int, name: String): Int {
        calls++
        val result: Int = gl30.glGetUniformLocation(program, name)
        check()
        return result
    }

    override fun glGetVertexAttribfv(index: Int, pname: Int, params: FloatBuffer) {
        calls++
        gl30.glGetVertexAttribfv(index, pname, params)
        check()
    }

    override fun glGetVertexAttribiv(index: Int, pname: Int, params: java.nio.IntBuffer) {
        calls++
        gl30.glGetVertexAttribiv(index, pname, params)
        check()
    }

    override fun glGetVertexAttribPointerv(index: Int, pname: Int, pointer: java.nio.Buffer) {
        calls++
        gl30.glGetVertexAttribPointerv(index, pname, pointer)
        check()
    }

    override fun glIsBuffer(buffer: Int): Boolean {
        calls++
        val result: Boolean = gl30.glIsBuffer(buffer)
        check()
        return result
    }

    override fun glIsEnabled(cap: Int): Boolean {
        calls++
        val result: Boolean = gl30.glIsEnabled(cap)
        check()
        return result
    }

    override fun glIsFramebuffer(framebuffer: Int): Boolean {
        calls++
        val result: Boolean = gl30.glIsFramebuffer(framebuffer)
        check()
        return result
    }

    override fun glIsProgram(program: Int): Boolean {
        calls++
        val result: Boolean = gl30.glIsProgram(program)
        check()
        return result
    }

    override fun glIsRenderbuffer(renderbuffer: Int): Boolean {
        calls++
        val result: Boolean = gl30.glIsRenderbuffer(renderbuffer)
        check()
        return result
    }

    override fun glIsShader(shader: Int): Boolean {
        calls++
        val result: Boolean = gl30.glIsShader(shader)
        check()
        return result
    }

    override fun glIsTexture(texture: Int): Boolean {
        calls++
        val result: Boolean = gl30.glIsTexture(texture)
        check()
        return result
    }

    override fun glLinkProgram(program: Int) {
        calls++
        gl30.glLinkProgram(program)
        check()
    }

    override fun glReleaseShaderCompiler() {
        calls++
        gl30.glReleaseShaderCompiler()
        check()
    }

    override fun glRenderbufferStorage(target: Int, internalformat: Int, width: Int, height: Int) {
        calls++
        gl30.glRenderbufferStorage(target, internalformat, width, height)
        check()
    }

    override fun glSampleCoverage(value: Float, invert: Boolean) {
        calls++
        gl30.glSampleCoverage(value, invert)
        check()
    }

    override fun glShaderBinary(n: Int, shaders: java.nio.IntBuffer, binaryformat: Int, binary: java.nio.Buffer, length: Int) {
        calls++
        gl30.glShaderBinary(n, shaders, binaryformat, binary, length)
        check()
    }

    override fun glShaderSource(shader: Int, string: String) {
        calls++
        gl30.glShaderSource(shader, string)
        check()
    }

    override fun glStencilFuncSeparate(face: Int, func: Int, ref: Int, mask: Int) {
        calls++
        gl30.glStencilFuncSeparate(face, func, ref, mask)
        check()
    }

    override fun glStencilMaskSeparate(face: Int, mask: Int) {
        calls++
        gl30.glStencilMaskSeparate(face, mask)
        check()
    }

    override fun glStencilOpSeparate(face: Int, fail: Int, zfail: Int, zpass: Int) {
        calls++
        gl30.glStencilOpSeparate(face, fail, zfail, zpass)
        check()
    }

    override fun glTexParameterfv(target: Int, pname: Int, params: FloatBuffer) {
        calls++
        gl30.glTexParameterfv(target, pname, params)
        check()
    }

    override fun glTexParameteri(target: Int, pname: Int, param: Int) {
        calls++
        gl30.glTexParameteri(target, pname, param)
        check()
    }

    override fun glTexParameteriv(target: Int, pname: Int, params: java.nio.IntBuffer) {
        calls++
        gl30.glTexParameteriv(target, pname, params)
        check()
    }

    override fun glUniform1f(location: Int, x: Float) {
        calls++
        gl30.glUniform1f(location, x)
        check()
    }

    override fun glUniform1fv(location: Int, count: Int, v: FloatBuffer) {
        calls++
        gl30.glUniform1fv(location, count, v)
        check()
    }

    override fun glUniform1fv(location: Int, count: Int, v: FloatArray, offset: Int) {
        calls++
        gl30.glUniform1fv(location, count, v, offset)
        check()
    }

    override fun glUniform1i(location: Int, x: Int) {
        calls++
        gl30.glUniform1i(location, x)
        check()
    }

    override fun glUniform1iv(location: Int, count: Int, v: java.nio.IntBuffer) {
        calls++
        gl30.glUniform1iv(location, count, v)
        check()
    }

    override fun glUniform1iv(location: Int, count: Int, v: IntArray, offset: Int) {
        calls++
        gl30.glUniform1iv(location, count, v, offset)
        check()
    }

    override fun glUniform2f(location: Int, x: Float, y: Float) {
        calls++
        gl30.glUniform2f(location, x, y)
        check()
    }

    override fun glUniform2fv(location: Int, count: Int, v: FloatBuffer) {
        calls++
        gl30.glUniform2fv(location, count, v)
        check()
    }

    override fun glUniform2fv(location: Int, count: Int, v: FloatArray, offset: Int) {
        calls++
        gl30.glUniform2fv(location, count, v, offset)
        check()
    }

    override fun glUniform2i(location: Int, x: Int, y: Int) {
        calls++
        gl30.glUniform2i(location, x, y)
        check()
    }

    override fun glUniform2iv(location: Int, count: Int, v: java.nio.IntBuffer) {
        calls++
        gl30.glUniform2iv(location, count, v)
        check()
    }

    override fun glUniform2iv(location: Int, count: Int, v: IntArray, offset: Int) {
        calls++
        gl30.glUniform2iv(location, count, v, offset)
        check()
    }

    override fun glUniform3f(location: Int, x: Float, y: Float, z: Float) {
        calls++
        gl30.glUniform3f(location, x, y, z)
        check()
    }

    override fun glUniform3fv(location: Int, count: Int, v: FloatBuffer) {
        calls++
        gl30.glUniform3fv(location, count, v)
        check()
    }

    override fun glUniform3fv(location: Int, count: Int, v: FloatArray, offset: Int) {
        calls++
        gl30.glUniform3fv(location, count, v, offset)
        check()
    }

    override fun glUniform3i(location: Int, x: Int, y: Int, z: Int) {
        calls++
        gl30.glUniform3i(location, x, y, z)
        check()
    }

    override fun glUniform3iv(location: Int, count: Int, v: java.nio.IntBuffer) {
        calls++
        gl30.glUniform3iv(location, count, v)
        check()
    }

    override fun glUniform3iv(location: Int, count: Int, v: IntArray, offset: Int) {
        calls++
        gl30.glUniform3iv(location, count, v, offset)
        check()
    }

    override fun glUniform4f(location: Int, x: Float, y: Float, z: Float, w: Float) {
        calls++
        gl30.glUniform4f(location, x, y, z, w)
        check()
    }

    override fun glUniform4fv(location: Int, count: Int, v: FloatBuffer) {
        calls++
        gl30.glUniform4fv(location, count, v)
        check()
    }

    override fun glUniform4fv(location: Int, count: Int, v: FloatArray, offset: Int) {
        calls++
        gl30.glUniform4fv(location, count, v, offset)
        check()
    }

    override fun glUniform4i(location: Int, x: Int, y: Int, z: Int, w: Int) {
        calls++
        gl30.glUniform4i(location, x, y, z, w)
        check()
    }

    override fun glUniform4iv(location: Int, count: Int, v: java.nio.IntBuffer) {
        calls++
        gl30.glUniform4iv(location, count, v)
        check()
    }

    override fun glUniform4iv(location: Int, count: Int, v: IntArray, offset: Int) {
        calls++
        gl30.glUniform4iv(location, count, v, offset)
        check()
    }

    override fun glUniformMatrix2fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer) {
        calls++
        gl30.glUniformMatrix2fv(location, count, transpose, value)
        check()
    }

    override fun glUniformMatrix2fv(location: Int, count: Int, transpose: Boolean, value: FloatArray, offset: Int) {
        calls++
        gl30.glUniformMatrix2fv(location, count, transpose, value, offset)
        check()
    }

    override fun glUniformMatrix3fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer) {
        calls++
        gl30.glUniformMatrix3fv(location, count, transpose, value)
        check()
    }

    override fun glUniformMatrix3fv(location: Int, count: Int, transpose: Boolean, value: FloatArray, offset: Int) {
        calls++
        gl30.glUniformMatrix3fv(location, count, transpose, value, offset)
        check()
    }

    override fun glUniformMatrix4fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer) {
        calls++
        gl30.glUniformMatrix4fv(location, count, transpose, value)
        check()
    }

    override fun glUniformMatrix4fv(location: Int, count: Int, transpose: Boolean, value: FloatArray, offset: Int) {
        calls++
        gl30.glUniformMatrix4fv(location, count, transpose, value, offset)
        check()
    }

    override fun glUseProgram(program: Int) {
        shaderSwitches++
        calls++
        gl30.glUseProgram(program)
        check()
    }

    override fun glValidateProgram(program: Int) {
        calls++
        gl30.glValidateProgram(program)
        check()
    }

    override fun glVertexAttrib1f(indx: Int, x: Float) {
        calls++
        gl30.glVertexAttrib1f(indx, x)
        check()
    }

    override fun glVertexAttrib1fv(indx: Int, values: FloatBuffer) {
        calls++
        gl30.glVertexAttrib1fv(indx, values)
        check()
    }

    override fun glVertexAttrib2f(indx: Int, x: Float, y: Float) {
        calls++
        gl30.glVertexAttrib2f(indx, x, y)
        check()
    }

    override fun glVertexAttrib2fv(indx: Int, values: FloatBuffer) {
        calls++
        gl30.glVertexAttrib2fv(indx, values)
        check()
    }

    override fun glVertexAttrib3f(indx: Int, x: Float, y: Float, z: Float) {
        calls++
        gl30.glVertexAttrib3f(indx, x, y, z)
        check()
    }

    override fun glVertexAttrib3fv(indx: Int, values: FloatBuffer) {
        calls++
        gl30.glVertexAttrib3fv(indx, values)
        check()
    }

    override fun glVertexAttrib4f(indx: Int, x: Float, y: Float, z: Float, w: Float) {
        calls++
        gl30.glVertexAttrib4f(indx, x, y, z, w)
        check()
    }

    override fun glVertexAttrib4fv(indx: Int, values: FloatBuffer) {
        calls++
        gl30.glVertexAttrib4fv(indx, values)
        check()
    }

    override fun glVertexAttribPointer(indx: Int, size: Int, type: Int, normalized: Boolean, stride: Int, ptr: java.nio.Buffer) {
        calls++
        gl30.glVertexAttribPointer(indx, size, type, normalized, stride, ptr)
        check()
    }

    override fun glVertexAttribPointer(indx: Int, size: Int, type: Int, normalized: Boolean, stride: Int, ptr: Int) {
        calls++
        gl30.glVertexAttribPointer(indx, size, type, normalized, stride, ptr)
        check()
    }

    // GL30 Unique
    override fun glReadBuffer(mode: Int) {
        calls++
        gl30.glReadBuffer(mode)
        check()
    }

    override fun glDrawRangeElements(mode: Int, start: Int, end: Int, count: Int, type: Int, indices: java.nio.Buffer) {
        vertexCount.put(count.toFloat())
        drawCalls++
        calls++
        gl30.glDrawRangeElements(mode, start, end, count, type, indices)
        check()
    }

    override fun glDrawRangeElements(mode: Int, start: Int, end: Int, count: Int, type: Int, offset: Int) {
        vertexCount.put(count.toFloat())
        drawCalls++
        calls++
        gl30.glDrawRangeElements(mode, start, end, count, type, offset)
        check()
    }

    override fun glTexImage3D(target: Int, level: Int, internalformat: Int, width: Int, height: Int, depth: Int, border: Int, format: Int,
                              type: Int, pixels: java.nio.Buffer) {
        calls++
        gl30.glTexImage3D(target, level, internalformat, width, height, depth, border, format, type, pixels)
        check()
    }

    override fun glTexImage3D(target: Int, level: Int, internalformat: Int, width: Int, height: Int, depth: Int, border: Int, format: Int,
                              type: Int, offset: Int) {
        calls++
        gl30.glTexImage3D(target, level, internalformat, width, height, depth, border, format, type, offset)
        check()
    }

    override fun glTexSubImage3D(target: Int, level: Int, xoffset: Int, yoffset: Int, zoffset: Int, width: Int, height: Int, depth: Int,
                                 format: Int, type: Int, pixels: java.nio.Buffer) {
        calls++
        gl30.glTexSubImage3D(target, level, xoffset, yoffset, zoffset, width, height, depth, format, type, pixels)
        check()
    }

    override fun glTexSubImage3D(target: Int, level: Int, xoffset: Int, yoffset: Int, zoffset: Int, width: Int, height: Int, depth: Int,
                                 format: Int, type: Int, offset: Int) {
        calls++
        gl30.glTexSubImage3D(target, level, xoffset, yoffset, zoffset, width, height, depth, format, type, offset)
        check()
    }

    override fun glCopyTexSubImage3D(target: Int, level: Int, xoffset: Int, yoffset: Int, zoffset: Int, x: Int, y: Int, width: Int,
                                     height: Int) {
        calls++
        gl30.glCopyTexSubImage3D(target, level, xoffset, yoffset, zoffset, x, y, width, height)
        check()
    }

    override fun glGenQueries(n: Int, ids: IntArray, offset: Int) {
        calls++
        gl30.glGenQueries(n, ids, offset)
        check()
    }

    override fun glGenQueries(n: Int, ids: java.nio.IntBuffer) {
        calls++
        gl30.glGenQueries(n, ids)
        check()
    }

    override fun glDeleteQueries(n: Int, ids: IntArray, offset: Int) {
        calls++
        gl30.glDeleteQueries(n, ids, offset)
        check()
    }

    override fun glDeleteQueries(n: Int, ids: java.nio.IntBuffer) {
        calls++
        gl30.glDeleteQueries(n, ids)
        check()
    }

    override fun glIsQuery(id: Int): Boolean {
        calls++
        val result: Boolean = gl30.glIsQuery(id)
        check()
        return result
    }

    override fun glBeginQuery(target: Int, id: Int) {
        calls++
        gl30.glBeginQuery(target, id)
        check()
    }

    override fun glEndQuery(target: Int) {
        calls++
        gl30.glEndQuery(target)
        check()
    }

    override fun glGetQueryiv(target: Int, pname: Int, params: java.nio.IntBuffer) {
        calls++
        gl30.glGetQueryiv(target, pname, params)
        check()
    }

    override fun glGetQueryObjectuiv(id: Int, pname: Int, params: java.nio.IntBuffer) {
        calls++
        gl30.glGetQueryObjectuiv(id, pname, params)
        check()
    }

    override fun glUnmapBuffer(target: Int): Boolean {
        calls++
        val result: Boolean = gl30.glUnmapBuffer(target)
        check()
        return result
    }

    override fun glGetBufferPointerv(target: Int, pname: Int): java.nio.Buffer {
        calls++
        val result: java.nio.Buffer = gl30.glGetBufferPointerv(target, pname)
        check()
        return result
    }

    override fun glDrawBuffers(n: Int, bufs: java.nio.IntBuffer) {
        drawCalls++
        calls++
        gl30.glDrawBuffers(n, bufs)
        check()
    }

    override fun glUniformMatrix2x3fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer) {
        calls++
        gl30.glUniformMatrix2x3fv(location, count, transpose, value)
        check()
    }

    override fun glUniformMatrix3x2fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer) {
        calls++
        gl30.glUniformMatrix3x2fv(location, count, transpose, value)
        check()
    }

    override fun glUniformMatrix2x4fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer) {
        calls++
        gl30.glUniformMatrix2x4fv(location, count, transpose, value)
        check()
    }

    override fun glUniformMatrix4x2fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer) {
        calls++
        gl30.glUniformMatrix4x2fv(location, count, transpose, value)
        check()
    }

    override fun glUniformMatrix3x4fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer) {
        calls++
        gl30.glUniformMatrix3x4fv(location, count, transpose, value)
        check()
    }

    override fun glUniformMatrix4x3fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer) {
        calls++
        gl30.glUniformMatrix4x3fv(location, count, transpose, value)
        check()
    }

    override fun glBlitFramebuffer(srcX0: Int, srcY0: Int, srcX1: Int, srcY1: Int, dstX0: Int, dstY0: Int, dstX1: Int, dstY1: Int,
                                   mask: Int, filter: Int) {
        calls++
        gl30.glBlitFramebuffer(srcX0, srcY0, srcX1, srcY1, dstX0, dstY0, dstX1, dstY1, mask, filter)
        check()
    }

    override fun glRenderbufferStorageMultisample(target: Int, samples: Int, internalformat: Int, width: Int, height: Int) {
        calls++
        gl30.glRenderbufferStorageMultisample(target, samples, internalformat, width, height)
        check()
    }

    override fun glFramebufferTextureLayer(target: Int, attachment: Int, texture: Int, level: Int, layer: Int) {
        calls++
        gl30.glFramebufferTextureLayer(target, attachment, texture, level, layer)
        check()
    }

    override fun glFlushMappedBufferRange(target: Int, offset: Int, length: Int) {
        calls++
        gl30.glFlushMappedBufferRange(target, offset, length)
        check()
    }

    override fun glBindVertexArray(array: Int) {
        calls++
        gl30.glBindVertexArray(array)
        check()
    }

    override fun glDeleteVertexArrays(n: Int, arrays: IntArray, offset: Int) {
        calls++
        gl30.glDeleteVertexArrays(n, arrays, offset)
        check()
    }

    override fun glDeleteVertexArrays(n: Int, arrays: java.nio.IntBuffer) {
        calls++
        gl30.glDeleteVertexArrays(n, arrays)
        check()
    }

    override fun glGenVertexArrays(n: Int, arrays: IntArray, offset: Int) {
        calls++
        gl30.glGenVertexArrays(n, arrays, offset)
        check()
    }

    override fun glGenVertexArrays(n: Int, arrays: java.nio.IntBuffer) {
        calls++
        gl30.glGenVertexArrays(n, arrays)
        check()
    }

    override fun glIsVertexArray(array: Int): Boolean {
        calls++
        val result: Boolean = gl30.glIsVertexArray(array)
        check()
        return result
    }

    override fun glBeginTransformFeedback(primitiveMode: Int) {
        calls++
        gl30.glBeginTransformFeedback(primitiveMode)
        check()
    }

    override fun glEndTransformFeedback() {
        calls++
        gl30.glEndTransformFeedback()
        check()
    }

    override fun glBindBufferRange(target: Int, index: Int, buffer: Int, offset: Int, size: Int) {
        calls++
        gl30.glBindBufferRange(target, index, buffer, offset, size)
        check()
    }

    override fun glBindBufferBase(target: Int, index: Int, buffer: Int) {
        calls++
        gl30.glBindBufferBase(target, index, buffer)
        check()
    }

    override fun glTransformFeedbackVaryings(program: Int, varyings: Array<String>, bufferMode: Int) {
        calls++
        gl30.glTransformFeedbackVaryings(program, varyings, bufferMode)
        check()
    }

    override fun glVertexAttribIPointer(index: Int, size: Int, type: Int, stride: Int, offset: Int) {
        calls++
        gl30.glVertexAttribIPointer(index, size, type, stride, offset)
        check()
    }

    override fun glGetVertexAttribIiv(index: Int, pname: Int, params: java.nio.IntBuffer) {
        calls++
        gl30.glGetVertexAttribIiv(index, pname, params)
        check()
    }

    override fun glGetVertexAttribIuiv(index: Int, pname: Int, params: java.nio.IntBuffer) {
        calls++
        gl30.glGetVertexAttribIuiv(index, pname, params)
        check()
    }

    override fun glVertexAttribI4i(index: Int, x: Int, y: Int, z: Int, w: Int) {
        calls++
        gl30.glVertexAttribI4i(index, x, y, z, w)
        check()
    }

    override fun glVertexAttribI4ui(index: Int, x: Int, y: Int, z: Int, w: Int) {
        calls++
        gl30.glVertexAttribI4ui(index, x, y, z, w)
        check()
    }

    override fun glGetUniformuiv(program: Int, location: Int, params: java.nio.IntBuffer) {
        calls++
        gl30.glGetUniformuiv(program, location, params)
        check()
    }

    override fun glGetFragDataLocation(program: Int, name: String): Int {
        calls++
        val result: Int = gl30.glGetFragDataLocation(program, name)
        check()
        return result
    }

    override fun glUniform1uiv(location: Int, count: Int, value: java.nio.IntBuffer) {
        calls++
        gl30.glUniform1uiv(location, count, value)
        check()
    }

    override fun glUniform3uiv(location: Int, count: Int, value: java.nio.IntBuffer) {
        calls++
        gl30.glUniform3uiv(location, count, value)
        check()
    }

    override fun glUniform4uiv(location: Int, count: Int, value: java.nio.IntBuffer) {
        calls++
        gl30.glUniform4uiv(location, count, value)
        check()
    }

    override fun glClearBufferiv(buffer: Int, drawbuffer: Int, value: java.nio.IntBuffer) {
        calls++
        gl30.glClearBufferiv(buffer, drawbuffer, value)
        check()
    }

    override fun glClearBufferuiv(buffer: Int, drawbuffer: Int, value: java.nio.IntBuffer) {
        calls++
        gl30.glClearBufferuiv(buffer, drawbuffer, value)
        check()
    }

    override fun glClearBufferfv(buffer: Int, drawbuffer: Int, value: FloatBuffer) {
        calls++
        gl30.glClearBufferfv(buffer, drawbuffer, value)
        check()
    }

    override fun glClearBufferfi(buffer: Int, drawbuffer: Int, depth: Float, stencil: Int) {
        calls++
        gl30.glClearBufferfi(buffer, drawbuffer, depth, stencil)
        check()
    }

    override fun glGetStringi(name: Int, index: Int): String {
        calls++
        val result: String = gl30.glGetStringi(name, index)
        check()
        return result
    }

    override fun glCopyBufferSubData(readTarget: Int, writeTarget: Int, readOffset: Int, writeOffset: Int, size: Int) {
        calls++
        gl30.glCopyBufferSubData(readTarget, writeTarget, readOffset, writeOffset, size)
        check()
    }

    override fun glGetUniformIndices(program: Int, uniformNames: Array<String>, uniformIndices: java.nio.IntBuffer) {
        calls++
        gl30.glGetUniformIndices(program, uniformNames, uniformIndices)
        check()
    }

    override fun glGetActiveUniformsiv(program: Int, uniformCount: Int, uniformIndices: java.nio.IntBuffer, pname: Int, params: java.nio.IntBuffer) {
        calls++
        gl30.glGetActiveUniformsiv(program, uniformCount, uniformIndices, pname, params)
        check()
    }

    override fun glGetUniformBlockIndex(program: Int, uniformBlockName: String): Int {
        calls++
        val result: Int = gl30.glGetUniformBlockIndex(program, uniformBlockName)
        check()
        return result
    }

    override fun glGetActiveUniformBlockiv(program: Int, uniformBlockIndex: Int, pname: Int, params: java.nio.IntBuffer) {
        calls++
        gl30.glGetActiveUniformBlockiv(program, uniformBlockIndex, pname, params)
        check()
    }

    override fun glGetActiveUniformBlockName(program: Int, uniformBlockIndex: Int, length: java.nio.Buffer, uniformBlockName: java.nio.Buffer) {
        calls++
        gl30.glGetActiveUniformBlockName(program, uniformBlockIndex, length, uniformBlockName)
        check()
    }

    override fun glGetActiveUniformBlockName(program: Int, uniformBlockIndex: Int): String {
        calls++
        val result: String = gl30.glGetActiveUniformBlockName(program, uniformBlockIndex)
        check()
        return result
    }

    override fun glUniformBlockBinding(program: Int, uniformBlockIndex: Int, uniformBlockBinding: Int) {
        calls++
        gl30.glUniformBlockBinding(program, uniformBlockIndex, uniformBlockBinding)
        check()
    }

    override fun glDrawArraysInstanced(mode: Int, first: Int, count: Int, instanceCount: Int) {
        vertexCount.put(count.toFloat())
        drawCalls++
        calls++
        gl30.glDrawArraysInstanced(mode, first, count, instanceCount)
        check()
    }

    override fun glDrawElementsInstanced(mode: Int, count: Int, type: Int, indicesOffset: Int, instanceCount: Int) {
        vertexCount.put(count.toFloat())
        drawCalls++
        calls++
        gl30.glDrawElementsInstanced(mode, count, type, indicesOffset, instanceCount)
        check()
    }

    override fun glGetInteger64v(pname: Int, params: LongBuffer) {
        calls++
        gl30.glGetInteger64v(pname, params)
        check()
    }

    override fun glGetBufferParameteri64v(target: Int, pname: Int, params: LongBuffer) {
        calls++
        gl30.glGetBufferParameteri64v(target, pname, params)
        check()
    }

    override fun glGenSamplers(count: Int, samplers: IntArray, offset: Int) {
        calls++
        gl30.glGenSamplers(count, samplers, offset)
        check()
    }

    override fun glGenSamplers(count: Int, samplers: java.nio.IntBuffer) {
        calls++
        gl30.glGenSamplers(count, samplers)
        check()
    }

    override fun glDeleteSamplers(count: Int, samplers: IntArray, offset: Int) {
        calls++
        gl30.glDeleteSamplers(count, samplers, offset)
        check()
    }

    override fun glDeleteSamplers(count: Int, samplers: java.nio.IntBuffer) {
        calls++
        gl30.glDeleteSamplers(count, samplers)
        check()
    }

    override fun glIsSampler(sampler: Int): Boolean {
        calls++
        val result: Boolean = gl30.glIsSampler(sampler)
        check()
        return result
    }

    override fun glBindSampler(unit: Int, sampler: Int) {
        calls++
        gl30.glBindSampler(unit, sampler)
        check()
    }

    override fun glSamplerParameteri(sampler: Int, pname: Int, param: Int) {
        calls++
        gl30.glSamplerParameteri(sampler, pname, param)
        check()
    }

    override fun glSamplerParameteriv(sampler: Int, pname: Int, param: java.nio.IntBuffer) {
        calls++
        gl30.glSamplerParameteriv(sampler, pname, param)
        check()
    }

    override fun glSamplerParameterf(sampler: Int, pname: Int, param: Float) {
        calls++
        gl30.glSamplerParameterf(sampler, pname, param)
        check()
    }

    override fun glSamplerParameterfv(sampler: Int, pname: Int, param: FloatBuffer) {
        calls++
        gl30.glSamplerParameterfv(sampler, pname, param)
        check()
    }

    override fun glGetSamplerParameteriv(sampler: Int, pname: Int, params: java.nio.IntBuffer) {
        calls++
        gl30.glGetSamplerParameteriv(sampler, pname, params)
        check()
    }

    override fun glGetSamplerParameterfv(sampler: Int, pname: Int, params: FloatBuffer) {
        calls++
        gl30.glGetSamplerParameterfv(sampler, pname, params)
        check()
    }

    override fun glVertexAttribDivisor(index: Int, divisor: Int) {
        calls++
        gl30.glVertexAttribDivisor(index, divisor)
        check()
    }

    override fun glBindTransformFeedback(target: Int, id: Int) {
        calls++
        gl30.glBindTransformFeedback(target, id)
        check()
    }

    override fun glDeleteTransformFeedbacks(n: Int, ids: IntArray, offset: Int) {
        calls++
        gl30.glDeleteTransformFeedbacks(n, ids, offset)
        check()
    }

    override fun glDeleteTransformFeedbacks(n: Int, ids: java.nio.IntBuffer) {
        calls++
        gl30.glDeleteTransformFeedbacks(n, ids)
        check()
    }

    override fun glGenTransformFeedbacks(n: Int, ids: IntArray, offset: Int) {
        calls++
        gl30.glGenTransformFeedbacks(n, ids, offset)
        check()
    }

    override fun glGenTransformFeedbacks(n: Int, ids: java.nio.IntBuffer) {
        calls++
        gl30.glGenTransformFeedbacks(n, ids)
        check()
    }

    override fun glIsTransformFeedback(id: Int): Boolean {
        calls++
        val result: Boolean = gl30.glIsTransformFeedback(id)
        check()
        return result
    }

    override fun glPauseTransformFeedback() {
        calls++
        gl30.glPauseTransformFeedback()
        check()
    }

    override fun glResumeTransformFeedback() {
        calls++
        gl30.glResumeTransformFeedback()
        check()
    }

    override fun glProgramParameteri(program: Int, pname: Int, value: Int) {
        calls++
        gl30.glProgramParameteri(program, pname, value)
        check()
    }

    override fun glInvalidateFramebuffer(target: Int, numAttachments: Int, attachments: java.nio.IntBuffer) {
        calls++
        gl30.glInvalidateFramebuffer(target, numAttachments, attachments)
        check()
    }

    override fun glInvalidateSubFramebuffer(target: Int, numAttachments: Int, attachments: java.nio.IntBuffer, x: Int, y: Int, width: Int,
                                            height: Int) {
        calls++
        gl30.glInvalidateSubFramebuffer(target, numAttachments, attachments, x, y, width, height)
        check()
    }

    init {
        this.gl30 = gl30
    }
}
