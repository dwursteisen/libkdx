/*
 **
 ** Copyright 2013, The Android Open Source Project
 **
 ** Licensed under the Apache License, Version 2.0 (the "License");
 ** you may not use this file except in compliance with the License.
 ** You may obtain a copy of the License at
 **
 **     http://www.apache.org/licenses/LICENSE-2.0
 **
 ** Unless required by applicable law or agreed to in writing, software
 ** distributed under the License is distributed on an "AS IS" BASIS,
 ** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ** See the License for the specific language governing permissions and
 ** limitations under the License.
 */
// This source file is automatically generated
package com.badlogic.gdx.graphics

import java.nio.FloatBuffer
import java.nio.LongBuffer

/** OpenGL ES 3.0  */
interface GL30 : GL20 {

    // C function void glReadBuffer ( GLenum mode )
    fun glReadBuffer(mode: Int)

    // C function void glDrawRangeElements ( GLenum mode, GLuint start, GLuint end, GLsizei count, GLenum type, const GLvoid
    // *indices )
    fun glDrawRangeElements(mode: Int, start: Int, end: Int, count: Int, type: Int, indices: java.nio.Buffer?)

    // C function void glDrawRangeElements ( GLenum mode, GLuint start, GLuint end, GLsizei count, GLenum type, GLsizei offset )
    fun glDrawRangeElements(mode: Int, start: Int, end: Int, count: Int, type: Int, offset: Int)

    // C function void glTexImage3D ( GLenum target, GLint level, GLint internalformat, GLsizei width, GLsizei height, GLsizei
    // depth, GLint border, GLenum format, GLenum type, const GLvoid *pixels )
    fun glTexImage3D(target: Int, level: Int, internalformat: Int, width: Int, height: Int, depth: Int, border: Int, format: Int,
                     type: Int, pixels: java.nio.Buffer?)

    // C function void glTexImage3D ( GLenum target, GLint level, GLint internalformat, GLsizei width, GLsizei height, GLsizei
    // depth, GLint border, GLenum format, GLenum type, GLsizei offset )
    fun glTexImage3D(target: Int, level: Int, internalformat: Int, width: Int, height: Int, depth: Int, border: Int, format: Int,
                     type: Int, offset: Int)

    // C function void glTexSubImage3D ( GLenum target, GLint level, GLint xoffset, GLint yoffset, GLint zoffset, GLsizei width,
    // GLsizei height, GLsizei depth, GLenum format, GLenum type, const GLvoid *pixels )
    fun glTexSubImage3D(target: Int, level: Int, xoffset: Int, yoffset: Int, zoffset: Int, width: Int, height: Int, depth: Int,
                        format: Int, type: Int, pixels: java.nio.Buffer?)

    // C function void glTexSubImage3D ( GLenum target, GLint level, GLint xoffset, GLint yoffset, GLint zoffset, GLsizei width,
    // GLsizei height, GLsizei depth, GLenum format, GLenum type, GLsizei offset )
    fun glTexSubImage3D(target: Int, level: Int, xoffset: Int, yoffset: Int, zoffset: Int, width: Int, height: Int, depth: Int,
                        format: Int, type: Int, offset: Int)

    // C function void glCopyTexSubImage3D ( GLenum target, GLint level, GLint xoffset, GLint yoffset, GLint zoffset, GLint x,
    // GLint y, GLsizei width, GLsizei height )
    fun glCopyTexSubImage3D(target: Int, level: Int, xoffset: Int, yoffset: Int, zoffset: Int, x: Int, y: Int, width: Int,
                            height: Int)

    // // C function void glCompressedTexImage3D ( GLenum target, GLint level, GLenum internalformat, GLsizei width, GLsizei height,
    // GLsizei depth, GLint border, GLsizei imageSize, const GLvoid *data )
    //
    // public void glCompressedTexImage3D(
    // int target,
    // int level,
    // int internalformat,
    // int width,
    // int height,
    // int depth,
    // int border,
    // int imageSize,
    // java.nio.Buffer data
    // );
    //
    // // C function void glCompressedTexImage3D ( GLenum target, GLint level, GLenum internalformat, GLsizei width, GLsizei height,
    // GLsizei depth, GLint border, GLsizei imageSize, GLsizei offset )
    //
    // public void glCompressedTexImage3D(
    // int target,
    // int level,
    // int internalformat,
    // int width,
    // int height,
    // int depth,
    // int border,
    // int imageSize,
    // int offset
    // );
    //
    // // C function void glCompressedTexSubImage3D ( GLenum target, GLint level, GLint xoffset, GLint yoffset, GLint zoffset, GLsizei
    // width, GLsizei height, GLsizei depth, GLenum format, GLsizei imageSize, const GLvoid *data )
    //
    // public void glCompressedTexSubImage3D(
    // int target,
    // int level,
    // int xoffset,
    // int yoffset,
    // int zoffset,
    // int width,
    // int height,
    // int depth,
    // int format,
    // int imageSize,
    // java.nio.Buffer data
    // );
    //
    // // C function void glCompressedTexSubImage3D ( GLenum target, GLint level, GLint xoffset, GLint yoffset, GLint zoffset, GLsizei
    // width, GLsizei height, GLsizei depth, GLenum format, GLsizei imageSize, GLsizei offset )
    //
    // public void glCompressedTexSubImage3D(
    // int target,
    // int level,
    // int xoffset,
    // int yoffset,
    // int zoffset,
    // int width,
    // int height,
    // int depth,
    // int format,
    // int imageSize,
    // int offset
    // );
    // C function void glGenQueries ( GLsizei n, GLuint *ids )
    fun glGenQueries(n: Int, ids: IntArray?, offset: Int)

    // C function void glGenQueries ( GLsizei n, GLuint *ids )
    fun glGenQueries(n: Int, ids: java.nio.IntBuffer?)

    // C function void glDeleteQueries ( GLsizei n, const GLuint *ids )
    fun glDeleteQueries(n: Int, ids: IntArray?, offset: Int)

    // C function void glDeleteQueries ( GLsizei n, const GLuint *ids )
    fun glDeleteQueries(n: Int, ids: java.nio.IntBuffer?)

    // C function GLboolean glIsQuery ( GLuint id )
    fun glIsQuery(id: Int): Boolean

    // C function void glBeginQuery ( GLenum target, GLuint id )
    fun glBeginQuery(target: Int, id: Int)

    // C function void glEndQuery ( GLenum target )
    fun glEndQuery(target: Int)

    // // C function void glGetQueryiv ( GLenum target, GLenum pname, GLint *params )
    //
    // public void glGetQueryiv(
    // int target,
    // int pname,
    // int[] params,
    // int offset
    // );
    // C function void glGetQueryiv ( GLenum target, GLenum pname, GLint *params )
    fun glGetQueryiv(target: Int, pname: Int, params: java.nio.IntBuffer?)

    // // C function void glGetQueryObjectuiv ( GLuint id, GLenum pname, GLuint *params )
    //
    // public void glGetQueryObjectuiv(
    // int id,
    // int pname,
    // int[] params,
    // int offset
    // );
    // C function void glGetQueryObjectuiv ( GLuint id, GLenum pname, GLuint *params )
    fun glGetQueryObjectuiv(id: Int, pname: Int, params: java.nio.IntBuffer?)

    // C function GLboolean glUnmapBuffer ( GLenum target )
    fun glUnmapBuffer(target: Int): Boolean

    // C function void glGetBufferPointerv ( GLenum target, GLenum pname, GLvoid** params )
    fun glGetBufferPointerv(target: Int, pname: Int): java.nio.Buffer?

    // // C function void glDrawBuffers ( GLsizei n, const GLenum *bufs )
    //
    // public void glDrawBuffers(
    // int n,
    // int[] bufs,
    // int offset
    // );
    // C function void glDrawBuffers ( GLsizei n, const GLenum *bufs )
    fun glDrawBuffers(n: Int, bufs: java.nio.IntBuffer?)

    // // C function void glUniformMatrix2x3fv ( GLint location, GLsizei count, GLboolean transpose, const GLfloat *value )
    //
    // public void glUniformMatrix2x3fv(
    // int location,
    // int count,
    // boolean transpose,
    // float[] value,
    // int offset
    // );
    // C function void glUniformMatrix2x3fv ( GLint location, GLsizei count, GLboolean transpose, const GLfloat *value )
    fun glUniformMatrix2x3fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer?)

    // // C function void glUniformMatrix3x2fv ( GLint location, GLsizei count, GLboolean transpose, const GLfloat *value )
    //
    // public void glUniformMatrix3x2fv(
    // int location,
    // int count,
    // boolean transpose,
    // float[] value,
    // int offset
    // );
    // C function void glUniformMatrix3x2fv ( GLint location, GLsizei count, GLboolean transpose, const GLfloat *value )
    fun glUniformMatrix3x2fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer?)

    // // C function void glUniformMatrix2x4fv ( GLint location, GLsizei count, GLboolean transpose, const GLfloat *value )
    //
    // public void glUniformMatrix2x4fv(
    // int location,
    // int count,
    // boolean transpose,
    // float[] value,
    // int offset
    // );
    // C function void glUniformMatrix2x4fv ( GLint location, GLsizei count, GLboolean transpose, const GLfloat *value )
    fun glUniformMatrix2x4fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer?)

    // // C function void glUniformMatrix4x2fv ( GLint location, GLsizei count, GLboolean transpose, const GLfloat *value )
    //
    // public void glUniformMatrix4x2fv(
    // int location,
    // int count,
    // boolean transpose,
    // float[] value,
    // int offset
    // );
    // C function void glUniformMatrix4x2fv ( GLint location, GLsizei count, GLboolean transpose, const GLfloat *value )
    fun glUniformMatrix4x2fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer?)

    // // C function void glUniformMatrix3x4fv ( GLint location, GLsizei count, GLboolean transpose, const GLfloat *value )
    //
    // public void glUniformMatrix3x4fv(
    // int location,
    // int count,
    // boolean transpose,
    // float[] value,
    // int offset
    // );
    // C function void glUniformMatrix3x4fv ( GLint location, GLsizei count, GLboolean transpose, const GLfloat *value )
    fun glUniformMatrix3x4fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer?)

    // // C function void glUniformMatrix4x3fv ( GLint location, GLsizei count, GLboolean transpose, const GLfloat *value )
    //
    // public void glUniformMatrix4x3fv(
    // int location,
    // int count,
    // boolean transpose,
    // float[] value,
    // int offset
    // );
    // C function void glUniformMatrix4x3fv ( GLint location, GLsizei count, GLboolean transpose, const GLfloat *value )
    fun glUniformMatrix4x3fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer?)

    // C function void glBlitFramebuffer ( GLint srcX0, GLint srcY0, GLint srcX1, GLint srcY1, GLint dstX0, GLint dstY0, GLint
    // dstX1, GLint dstY1, GLbitfield mask, GLenum filter )
    fun glBlitFramebuffer(srcX0: Int, srcY0: Int, srcX1: Int, srcY1: Int, dstX0: Int, dstY0: Int, dstX1: Int, dstY1: Int,
                          mask: Int, filter: Int)

    // C function void glRenderbufferStorageMultisample ( GLenum target, GLsizei samples, GLenum internalformat, GLsizei width,
    // GLsizei height )
    fun glRenderbufferStorageMultisample(target: Int, samples: Int, internalformat: Int, width: Int, height: Int)

    // C function void glFramebufferTextureLayer ( GLenum target, GLenum attachment, GLuint texture, GLint level, GLint layer )
    fun glFramebufferTextureLayer(target: Int, attachment: Int, texture: Int, level: Int, layer: Int)

    // // C function GLvoid * glMapBufferRange ( GLenum target, GLintptr offset, GLsizeiptr length, GLbitfield access )
    //
    // public java.nio.Buffer glMapBufferRange(
    // int target,
    // int offset,
    // int length,
    // int access
    // );
    // C function void glFlushMappedBufferRange ( GLenum target, GLintptr offset, GLsizeiptr length )
    fun glFlushMappedBufferRange(target: Int, offset: Int, length: Int)

    // C function void glBindVertexArray ( GLuint array )
    fun glBindVertexArray(array: Int)

    // C function void glDeleteVertexArrays ( GLsizei n, const GLuint *arrays )
    fun glDeleteVertexArrays(n: Int, arrays: IntArray?, offset: Int)

    // C function void glDeleteVertexArrays ( GLsizei n, const GLuint *arrays )
    fun glDeleteVertexArrays(n: Int, arrays: java.nio.IntBuffer?)

    // C function void glGenVertexArrays ( GLsizei n, GLuint *arrays )
    fun glGenVertexArrays(n: Int, arrays: IntArray?, offset: Int)

    // C function void glGenVertexArrays ( GLsizei n, GLuint *arrays )
    fun glGenVertexArrays(n: Int, arrays: java.nio.IntBuffer?)

    // C function GLboolean glIsVertexArray ( GLuint array )
    fun glIsVertexArray(array: Int): Boolean

    //
    // // C function void glGetIntegeri_v ( GLenum target, GLuint index, GLint *data )
    //
    // public void glGetIntegeri_v(
    // int target,
    // int index,
    // int[] data,
    // int offset
    // );
    //
    // // C function void glGetIntegeri_v ( GLenum target, GLuint index, GLint *data )
    //
    // public void glGetIntegeri_v(
    // int target,
    // int index,
    // java.nio.IntBuffer data
    // );
    // C function void glBeginTransformFeedback ( GLenum primitiveMode )
    fun glBeginTransformFeedback(primitiveMode: Int)

    // C function void glEndTransformFeedback ( void )
    fun glEndTransformFeedback()

    // C function void glBindBufferRange ( GLenum target, GLuint index, GLuint buffer, GLintptr offset, GLsizeiptr size )
    fun glBindBufferRange(target: Int, index: Int, buffer: Int, offset: Int, size: Int)

    // C function void glBindBufferBase ( GLenum target, GLuint index, GLuint buffer )
    fun glBindBufferBase(target: Int, index: Int, buffer: Int)

    // C function void glTransformFeedbackVaryings ( GLuint program, GLsizei count, const GLchar *varyings, GLenum bufferMode )
    fun glTransformFeedbackVaryings(program: Int, varyings: Array<String?>?, bufferMode: Int)

    // // C function void glGetTransformFeedbackVarying ( GLuint program, GLuint index, GLsizei bufSize, GLsizei *length, GLint *size,
    // GLenum *type, GLchar *name )
    //
    // public void glGetTransformFeedbackVarying(
    // int program,
    // int index,
    // int bufsize,
    // int[] length,
    // int lengthOffset,
    // int[] size,
    // int sizeOffset,
    // int[] type,
    // int typeOffset,
    // byte[] name,
    // int nameOffset
    // );
    //
    // // C function void glGetTransformFeedbackVarying ( GLuint program, GLuint index, GLsizei bufSize, GLsizei *length, GLint *size,
    // GLenum *type, GLchar *name )
    //
    // public void glGetTransformFeedbackVarying(
    // int program,
    // int index,
    // int bufsize,
    // java.nio.IntBuffer length,
    // java.nio.IntBuffer size,
    // java.nio.IntBuffer type,
    // byte name
    // );
    //
    // // C function void glGetTransformFeedbackVarying ( GLuint program, GLuint index, GLsizei bufSize, GLsizei *length, GLint *size,
    // GLenum *type, GLchar *name )
    //
    // public String glGetTransformFeedbackVarying(
    // int program,
    // int index,
    // int[] size,
    // int sizeOffset,
    // int[] type,
    // int typeOffset
    // );
    //
    // // C function void glGetTransformFeedbackVarying ( GLuint program, GLuint index, GLsizei bufSize, GLsizei *length, GLint *size,
    // GLenum *type, GLchar *name )
    //
    // public String glGetTransformFeedbackVarying(
    // int program,
    // int index,
    // java.nio.IntBuffer size,
    // java.nio.IntBuffer type
    // );
    // C function void glVertexAttribIPointer ( GLuint index, GLint size, GLenum type, GLsizei stride, GLsizei offset )
    fun glVertexAttribIPointer(index: Int, size: Int, type: Int, stride: Int, offset: Int)

    // // C function void glGetVertexAttribIiv ( GLuint index, GLenum pname, GLint *params )
    //
    // public void glGetVertexAttribIiv(
    // int index,
    // int pname,
    // int[] params,
    // int offset
    // );
    // C function void glGetVertexAttribIiv ( GLuint index, GLenum pname, GLint *params )
    fun glGetVertexAttribIiv(index: Int, pname: Int, params: java.nio.IntBuffer?)

    // // C function void glGetVertexAttribIuiv ( GLuint index, GLenum pname, GLuint *params )
    //
    // public void glGetVertexAttribIuiv(
    // int index,
    // int pname,
    // int[] params,
    // int offset
    // );
    // C function void glGetVertexAttribIuiv ( GLuint index, GLenum pname, GLuint *params )
    fun glGetVertexAttribIuiv(index: Int, pname: Int, params: java.nio.IntBuffer?)

    // C function void glVertexAttribI4i ( GLuint index, GLint x, GLint y, GLint z, GLint w )
    fun glVertexAttribI4i(index: Int, x: Int, y: Int, z: Int, w: Int)

    // C function void glVertexAttribI4ui ( GLuint index, GLuint x, GLuint y, GLuint z, GLuint w )
    fun glVertexAttribI4ui(index: Int, x: Int, y: Int, z: Int, w: Int)

    // // C function void glVertexAttribI4iv ( GLuint index, const GLint *v )
    //
    // public void glVertexAttribI4iv(
    // int index,
    // int[] v,
    // int offset
    // );
    //
    // // C function void glVertexAttribI4iv ( GLuint index, const GLint *v )
    //
    // public void glVertexAttribI4iv(
    // int index,
    // java.nio.IntBuffer v
    // );
    //
    // // C function void glVertexAttribI4uiv ( GLuint index, const GLuint *v )
    //
    // public void glVertexAttribI4uiv(
    // int index,
    // int[] v,
    // int offset
    // );
    //
    // // C function void glVertexAttribI4uiv ( GLuint index, const GLuint *v )
    //
    // public void glVertexAttribI4uiv(
    // int index,
    // java.nio.IntBuffer v
    // );
    //
    // // C function void glGetUniformuiv ( GLuint program, GLint location, GLuint *params )
    //
    // public void glGetUniformuiv(
    // int program,
    // int location,
    // int[] params,
    // int offset
    // );
    // C function void glGetUniformuiv ( GLuint program, GLint location, GLuint *params )
    fun glGetUniformuiv(program: Int, location: Int, params: java.nio.IntBuffer?)

    // C function GLint glGetFragDataLocation ( GLuint program, const GLchar *name )
    fun glGetFragDataLocation(program: Int, name: String?): Int

    // // C function void glUniform1ui ( GLint location, GLuint v0 )
    //
    // public void glUniform1ui(
    // int location,
    // int v0
    // );
    //
    // // C function void glUniform2ui ( GLint location, GLuint v0, GLuint v1 )
    //
    // public void glUniform2ui(
    // int location,
    // int v0,
    // int v1
    // );
    //
    // // C function void glUniform3ui ( GLint location, GLuint v0, GLuint v1, GLuint v2 )
    //
    // public void glUniform3ui(
    // int location,
    // int v0,
    // int v1,
    // int v2
    // );
    //
    // // C function void glUniform4ui ( GLint location, GLuint v0, GLuint v1, GLuint v2, GLuint v3 )
    //
    // public void glUniform4ui(
    // int location,
    // int v0,
    // int v1,
    // int v2,
    // int v3
    // );
    //
    // // C function void glUniform1uiv ( GLint location, GLsizei count, const GLuint *value )
    //
    // public void glUniform1uiv(
    // int location,
    // int count,
    // int[] value,
    // int offset
    // );
    // C function void glUniform1uiv ( GLint location, GLsizei count, const GLuint *value )
    fun glUniform1uiv(location: Int, count: Int, value: java.nio.IntBuffer?)

    // // C function void glUniform2uiv ( GLint location, GLsizei count, const GLuint *value )
    //
    // public void glUniform2uiv(
    // int location,
    // int count,
    // int[] value,
    // int offset
    // );
    //
    // // C function void glUniform2uiv ( GLint location, GLsizei count, const GLuint *value )
    //
    // public void glUniform2uiv(
    // int location,
    // int count,
    // java.nio.IntBuffer value
    // );
    //
    // // C function void glUniform3uiv ( GLint location, GLsizei count, const GLuint *value )
    //
    // public void glUniform3uiv(
    // int location,
    // int count,
    // int[] value,
    // int offset
    // );
    // C function void glUniform3uiv ( GLint location, GLsizei count, const GLuint *value )
    fun glUniform3uiv(location: Int, count: Int, value: java.nio.IntBuffer?)

    // // C function void glUniform4uiv ( GLint location, GLsizei count, const GLuint *value )
    //
    // public void glUniform4uiv(
    // int location,
    // int count,
    // int[] value,
    // int offset
    // );
    // C function void glUniform4uiv ( GLint location, GLsizei count, const GLuint *value )
    fun glUniform4uiv(location: Int, count: Int, value: java.nio.IntBuffer?)

    // // C function void glClearBufferiv ( GLenum buffer, GLint drawbuffer, const GLint *value )
    //
    // public void glClearBufferiv(
    // int buffer,
    // int drawbuffer,
    // int[] value,
    // int offset
    // );
    // C function void glClearBufferiv ( GLenum buffer, GLint drawbuffer, const GLint *value )
    fun glClearBufferiv(buffer: Int, drawbuffer: Int, value: java.nio.IntBuffer?)

    // // C function void glClearBufferuiv ( GLenum buffer, GLint drawbuffer, const GLuint *value )
    //
    // public void glClearBufferuiv(
    // int buffer,
    // int drawbuffer,
    // int[] value,
    // int offset
    // );
    // C function void glClearBufferuiv ( GLenum buffer, GLint drawbuffer, const GLuint *value )
    fun glClearBufferuiv(buffer: Int, drawbuffer: Int, value: java.nio.IntBuffer?)

    // // C function void glClearBufferfv ( GLenum buffer, GLint drawbuffer, const GLfloat *value )
    //
    // public void glClearBufferfv(
    // int buffer,
    // int drawbuffer,
    // float[] value,
    // int offset
    // );
    // C function void glClearBufferfv ( GLenum buffer, GLint drawbuffer, const GLfloat *value )
    fun glClearBufferfv(buffer: Int, drawbuffer: Int, value: FloatBuffer?)

    // C function void glClearBufferfi ( GLenum buffer, GLint drawbuffer, GLfloat depth, GLint stencil )
    fun glClearBufferfi(buffer: Int, drawbuffer: Int, depth: Float, stencil: Int)

    // C function const GLubyte * glGetStringi ( GLenum name, GLuint index )
    fun glGetStringi(name: Int, index: Int): String?

    // C function void glCopyBufferSubData ( GLenum readTarget, GLenum writeTarget, GLintptr readOffset, GLintptr writeOffset,
    // GLsizeiptr size )
    fun glCopyBufferSubData(readTarget: Int, writeTarget: Int, readOffset: Int, writeOffset: Int, size: Int)

    // // C function void glGetUniformIndices ( GLuint program, GLsizei uniformCount, const GLchar *const *uniformNames, GLuint
    // *uniformIndices )
    //
    // public void glGetUniformIndices(
    // int program,
    // String[] uniformNames,
    // int[] uniformIndices,
    // int uniformIndicesOffset
    // );
    // C function void glGetUniformIndices ( GLuint program, GLsizei uniformCount, const GLchar *const *uniformNames, GLuint
    // *uniformIndices )
    fun glGetUniformIndices(program: Int, uniformNames: Array<String?>?, uniformIndices: java.nio.IntBuffer?)

    // // C function void glGetActiveUniformsiv ( GLuint program, GLsizei uniformCount, const GLuint *uniformIndices, GLenum pname,
    // GLint *params )
    //
    // public void glGetActiveUniformsiv(
    // int program,
    // int uniformCount,
    // int[] uniformIndices,
    // int uniformIndicesOffset,
    // int pname,
    // int[] params,
    // int paramsOffset
    // );
    // C function void glGetActiveUniformsiv ( GLuint program, GLsizei uniformCount, const GLuint *uniformIndices, GLenum pname,
    // GLint *params )
    fun glGetActiveUniformsiv(program: Int, uniformCount: Int, uniformIndices: java.nio.IntBuffer?, pname: Int,
                              params: java.nio.IntBuffer?)

    // C function GLuint glGetUniformBlockIndex ( GLuint program, const GLchar *uniformBlockName )
    fun glGetUniformBlockIndex(program: Int, uniformBlockName: String?): Int

    // // C function void glGetActiveUniformBlockiv ( GLuint program, GLuint uniformBlockIndex, GLenum pname, GLint *params )
    //
    // public void glGetActiveUniformBlockiv(
    // int program,
    // int uniformBlockIndex,
    // int pname,
    // int[] params,
    // int offset
    // );
    // C function void glGetActiveUniformBlockiv ( GLuint program, GLuint uniformBlockIndex, GLenum pname, GLint *params )
    fun glGetActiveUniformBlockiv(program: Int, uniformBlockIndex: Int, pname: Int, params: java.nio.IntBuffer?)

    // // C function void glGetActiveUniformBlockName ( GLuint program, GLuint uniformBlockIndex, GLsizei bufSize, GLsizei *length,
    // GLchar *uniformBlockName )
    //
    // public void glGetActiveUniformBlockName(
    // int program,
    // int uniformBlockIndex,
    // int bufSize,
    // int[] length,
    // int lengthOffset,
    // byte[] uniformBlockName,
    // int uniformBlockNameOffset
    // );
    // C function void glGetActiveUniformBlockName ( GLuint program, GLuint uniformBlockIndex, GLsizei bufSize, GLsizei *length,
    // GLchar *uniformBlockName )
    fun glGetActiveUniformBlockName(program: Int, uniformBlockIndex: Int, length: java.nio.Buffer?,
                                    uniformBlockName: java.nio.Buffer?)

    // C function void glGetActiveUniformBlockName ( GLuint program, GLuint uniformBlockIndex, GLsizei bufSize, GLsizei *length,
    // GLchar *uniformBlockName )
    fun glGetActiveUniformBlockName(program: Int, uniformBlockIndex: Int): String?

    // C function void glUniformBlockBinding ( GLuint program, GLuint uniformBlockIndex, GLuint uniformBlockBinding )
    fun glUniformBlockBinding(program: Int, uniformBlockIndex: Int, uniformBlockBinding: Int)

    // C function void glDrawArraysInstanced ( GLenum mode, GLint first, GLsizei count, GLsizei instanceCount )
    fun glDrawArraysInstanced(mode: Int, first: Int, count: Int, instanceCount: Int)

    // // C function void glDrawElementsInstanced ( GLenum mode, GLsizei count, GLenum type, const GLvoid *indices, GLsizei
    // instanceCount )
    //
    // public void glDrawElementsInstanced(
    // int mode,
    // int count,
    // int type,
    // java.nio.Buffer indices,
    // int instanceCount
    // );
    // C function void glDrawElementsInstanced ( GLenum mode, GLsizei count, GLenum type, const GLvoid *indices, GLsizei
    // instanceCount )
    fun glDrawElementsInstanced(mode: Int, count: Int, type: Int, indicesOffset: Int, instanceCount: Int)

    // // C function GLsync glFenceSync ( GLenum condition, GLbitfield flags )
    //
    // public long glFenceSync(
    // int condition,
    // int flags
    // );
    //
    // // C function GLboolean glIsSync ( GLsync sync )
    //
    // public boolean glIsSync(
    // long sync
    // );
    //
    // // C function void glDeleteSync ( GLsync sync )
    //
    // public void glDeleteSync(
    // long sync
    // );
    //
    // // C function GLenum glClientWaitSync ( GLsync sync, GLbitfield flags, GLuint64 timeout )
    //
    // public int glClientWaitSync(
    // long sync,
    // int flags,
    // long timeout
    // );
    //
    // // C function void glWaitSync ( GLsync sync, GLbitfield flags, GLuint64 timeout )
    //
    // public void glWaitSync(
    // long sync,
    // int flags,
    // long timeout
    // );
    // // C function void glGetInteger64v ( GLenum pname, GLint64 *params )
    //
    // public void glGetInteger64v(
    // int pname,
    // long[] params,
    // int offset
    // );
    // C function void glGetInteger64v ( GLenum pname, GLint64 *params )
    fun glGetInteger64v(pname: Int, params: LongBuffer?)

    // // C function void glGetSynciv ( GLsync sync, GLenum pname, GLsizei bufSize, GLsizei *length, GLint *values )
    //
    // public void glGetSynciv(
    // long sync,
    // int pname,
    // int bufSize,
    // int[] length,
    // int lengthOffset,
    // int[] values,
    // int valuesOffset
    // );
    //
    // // C function void glGetSynciv ( GLsync sync, GLenum pname, GLsizei bufSize, GLsizei *length, GLint *values )
    //
    // public void glGetSynciv(
    // long sync,
    // int pname,
    // int bufSize,
    // java.nio.IntBuffer length,
    // java.nio.IntBuffer values
    // );
    //
    // // C function void glGetInteger64i_v ( GLenum target, GLuint index, GLint64 *data )
    //
    // public void glGetInteger64i_v(
    // int target,
    // int index,
    // long[] data,
    // int offset
    // );
    //
    // // C function void glGetInteger64i_v ( GLenum target, GLuint index, GLint64 *data )
    //
    // public void glGetInteger64i_v(
    // int target,
    // int index,
    // java.nio.LongBuffer data
    // );
    //
    // // C function void glGetBufferParameteri64v ( GLenum target, GLenum pname, GLint64 *params )
    //
    // public void glGetBufferParameteri64v(
    // int target,
    // int pname,
    // long[] params,
    // int offset
    // );
    // C function void glGetBufferParameteri64v ( GLenum target, GLenum pname, GLint64 *params )
    fun glGetBufferParameteri64v(target: Int, pname: Int, params: LongBuffer?)

    // C function void glGenSamplers ( GLsizei count, GLuint *samplers )
    fun glGenSamplers(count: Int, samplers: IntArray?, offset: Int)

    // C function void glGenSamplers ( GLsizei count, GLuint *samplers )
    fun glGenSamplers(count: Int, samplers: java.nio.IntBuffer?)

    // C function void glDeleteSamplers ( GLsizei count, const GLuint *samplers )
    fun glDeleteSamplers(count: Int, samplers: IntArray?, offset: Int)

    // C function void glDeleteSamplers ( GLsizei count, const GLuint *samplers )
    fun glDeleteSamplers(count: Int, samplers: java.nio.IntBuffer?)

    // C function GLboolean glIsSampler ( GLuint sampler )
    fun glIsSampler(sampler: Int): Boolean

    // C function void glBindSampler ( GLuint unit, GLuint sampler )
    fun glBindSampler(unit: Int, sampler: Int)

    // C function void glSamplerParameteri ( GLuint sampler, GLenum pname, GLint param )
    fun glSamplerParameteri(sampler: Int, pname: Int, param: Int)

    // // C function void glSamplerParameteriv ( GLuint sampler, GLenum pname, const GLint *param )
    //
    // public void glSamplerParameteriv(
    // int sampler,
    // int pname,
    // int[] param,
    // int offset
    // );
    // C function void glSamplerParameteriv ( GLuint sampler, GLenum pname, const GLint *param )
    fun glSamplerParameteriv(sampler: Int, pname: Int, param: java.nio.IntBuffer?)

    // C function void glSamplerParameterf ( GLuint sampler, GLenum pname, GLfloat param )
    fun glSamplerParameterf(sampler: Int, pname: Int, param: Float)

    // // C function void glSamplerParameterfv ( GLuint sampler, GLenum pname, const GLfloat *param )
    //
    // public void glSamplerParameterfv(
    // int sampler,
    // int pname,
    // float[] param,
    // int offset
    // );
    // C function void glSamplerParameterfv ( GLuint sampler, GLenum pname, const GLfloat *param )
    fun glSamplerParameterfv(sampler: Int, pname: Int, param: FloatBuffer?)

    // // C function void glGetSamplerParameteriv ( GLuint sampler, GLenum pname, GLint *params )
    //
    // public void glGetSamplerParameteriv(
    // int sampler,
    // int pname,
    // int[] params,
    // int offset
    // );
    // C function void glGetSamplerParameteriv ( GLuint sampler, GLenum pname, GLint *params )
    fun glGetSamplerParameteriv(sampler: Int, pname: Int, params: java.nio.IntBuffer?)

    // // C function void glGetSamplerParameterfv ( GLuint sampler, GLenum pname, GLfloat *params )
    //
    // public void glGetSamplerParameterfv(
    // int sampler,
    // int pname,
    // float[] params,
    // int offset
    // );
    // C function void glGetSamplerParameterfv ( GLuint sampler, GLenum pname, GLfloat *params )
    fun glGetSamplerParameterfv(sampler: Int, pname: Int, params: FloatBuffer?)

    // C function void glVertexAttribDivisor ( GLuint index, GLuint divisor )
    fun glVertexAttribDivisor(index: Int, divisor: Int)

    // C function void glBindTransformFeedback ( GLenum target, GLuint id )
    fun glBindTransformFeedback(target: Int, id: Int)

    // C function void glDeleteTransformFeedbacks ( GLsizei n, const GLuint *ids )
    fun glDeleteTransformFeedbacks(n: Int, ids: IntArray?, offset: Int)

    // C function void glDeleteTransformFeedbacks ( GLsizei n, const GLuint *ids )
    fun glDeleteTransformFeedbacks(n: Int, ids: java.nio.IntBuffer?)

    // C function void glGenTransformFeedbacks ( GLsizei n, GLuint *ids )
    fun glGenTransformFeedbacks(n: Int, ids: IntArray?, offset: Int)

    // C function void glGenTransformFeedbacks ( GLsizei n, GLuint *ids )
    fun glGenTransformFeedbacks(n: Int, ids: java.nio.IntBuffer?)

    // C function GLboolean glIsTransformFeedback ( GLuint id )
    fun glIsTransformFeedback(id: Int): Boolean

    // C function void glPauseTransformFeedback ( void )
    fun glPauseTransformFeedback()

    // C function void glResumeTransformFeedback ( void )
    fun glResumeTransformFeedback()

    // // C function void glGetProgramBinary ( GLuint program, GLsizei bufSize, GLsizei *length, GLenum *binaryFormat, GLvoid *binary
    // )
    //
    // public void glGetProgramBinary(
    // int program,
    // int bufSize,
    // int[] length,
    // int lengthOffset,
    // int[] binaryFormat,
    // int binaryFormatOffset,
    // java.nio.Buffer binary
    // );
    //
    // // C function void glGetProgramBinary ( GLuint program, GLsizei bufSize, GLsizei *length, GLenum *binaryFormat, GLvoid *binary
    // )
    //
    // public void glGetProgramBinary(
    // int program,
    // int bufSize,
    // java.nio.IntBuffer length,
    // java.nio.IntBuffer binaryFormat,
    // java.nio.Buffer binary
    // );
    //
    // // C function void glProgramBinary ( GLuint program, GLenum binaryFormat, const GLvoid *binary, GLsizei length )
    //
    // public void glProgramBinary(
    // int program,
    // int binaryFormat,
    // java.nio.Buffer binary,
    // int length
    // );
    // C function void glProgramParameteri ( GLuint program, GLenum pname, GLint value )
    fun glProgramParameteri(program: Int, pname: Int, value: Int)

    // // C function void glInvalidateFramebuffer ( GLenum target, GLsizei numAttachments, const GLenum *attachments )
    //
    // public void glInvalidateFramebuffer(
    // int target,
    // int numAttachments,
    // int[] attachments,
    // int offset
    // );
    // C function void glInvalidateFramebuffer ( GLenum target, GLsizei numAttachments, const GLenum *attachments )
    fun glInvalidateFramebuffer(target: Int, numAttachments: Int, attachments: java.nio.IntBuffer?)

    // // C function void glInvalidateSubFramebuffer ( GLenum target, GLsizei numAttachments, const GLenum *attachments, GLint x,
    // GLint y, GLsizei width, GLsizei height )
    //
    // public void glInvalidateSubFramebuffer(
    // int target,
    // int numAttachments,
    // int[] attachments,
    // int offset,
    // int x,
    // int y,
    // int width,
    // int height
    // );
    // C function void glInvalidateSubFramebuffer ( GLenum target, GLsizei numAttachments, const GLenum *attachments, GLint x,
    // GLint y, GLsizei width, GLsizei height )
    fun glInvalidateSubFramebuffer(target: Int, numAttachments: Int, attachments: java.nio.IntBuffer?, x: Int, y: Int,
                                   width: Int, height: Int)

    // // C function void glTexStorage2D ( GLenum target, GLsizei levels, GLenum internalformat, GLsizei width, GLsizei height )
    //
    // public void glTexStorage2D(
    // int target,
    // int levels,
    // int internalformat,
    // int width,
    // int height
    // );
    //
    // // C function void glTexStorage3D ( GLenum target, GLsizei levels, GLenum internalformat, GLsizei width, GLsizei height,
    // GLsizei depth )
    //
    // public void glTexStorage3D(
    // int target,
    // int levels,
    // int internalformat,
    // int width,
    // int height,
    // int depth
    // );
    //
    // // C function void glGetInternalformativ ( GLenum target, GLenum internalformat, GLenum pname, GLsizei bufSize, GLint *params )
    //
    // public void glGetInternalformativ(
    // int target,
    // int internalformat,
    // int pname,
    // int bufSize,
    // int[] params,
    // int offset
    // );
    //
    // // C function void glGetInternalformativ ( GLenum target, GLenum internalformat, GLenum pname, GLsizei bufSize, GLint *params )
    //
    // public void glGetInternalformativ(
    // int target,
    // int internalformat,
    // int pname,
    // int bufSize,
    // java.nio.IntBuffer params
    // );
    @Deprecated("")
    override fun
        /**
         * In OpenGl core profiles (3.1+), passing a pointer to client memory is not valid.
         * Use the other version of this function instead, pass a zero-based offset which references
         * the buffer currently bound to GL_ARRAY_BUFFER.
         */
        glVertexAttribPointer(indx: Int, size: Int, type: Int, normalized: Boolean, stride: Int, ptr: java.nio.Buffer?)

    companion object {
        const val GL_READ_BUFFER = 0x0C02
        const val GL_UNPACK_ROW_LENGTH = 0x0CF2
        const val GL_UNPACK_SKIP_ROWS = 0x0CF3
        const val GL_UNPACK_SKIP_PIXELS = 0x0CF4
        const val GL_PACK_ROW_LENGTH = 0x0D02
        const val GL_PACK_SKIP_ROWS = 0x0D03
        const val GL_PACK_SKIP_PIXELS = 0x0D04
        const val GL_COLOR = 0x1800
        const val GL_DEPTH = 0x1801
        const val GL_STENCIL = 0x1802
        const val GL_RED = 0x1903
        const val GL_RGB8 = 0x8051
        const val GL_RGBA8 = 0x8058
        const val GL_RGB10_A2 = 0x8059
        const val GL_TEXTURE_BINDING_3D = 0x806A
        const val GL_UNPACK_SKIP_IMAGES = 0x806D
        const val GL_UNPACK_IMAGE_HEIGHT = 0x806E
        const val GL_TEXTURE_3D = 0x806F
        const val GL_TEXTURE_WRAP_R = 0x8072
        const val GL_MAX_3D_TEXTURE_SIZE = 0x8073
        const val GL_UNSIGNED_INT_2_10_10_10_REV = 0x8368
        const val GL_MAX_ELEMENTS_VERTICES = 0x80E8
        const val GL_MAX_ELEMENTS_INDICES = 0x80E9
        const val GL_TEXTURE_MIN_LOD = 0x813A
        const val GL_TEXTURE_MAX_LOD = 0x813B
        const val GL_TEXTURE_BASE_LEVEL = 0x813C
        const val GL_TEXTURE_MAX_LEVEL = 0x813D
        const val GL_MIN = 0x8007
        const val GL_MAX = 0x8008
        const val GL_DEPTH_COMPONENT24 = 0x81A6
        const val GL_MAX_TEXTURE_LOD_BIAS = 0x84FD
        const val GL_TEXTURE_COMPARE_MODE = 0x884C
        const val GL_TEXTURE_COMPARE_FUNC = 0x884D
        const val GL_CURRENT_QUERY = 0x8865
        const val GL_QUERY_RESULT = 0x8866
        const val GL_QUERY_RESULT_AVAILABLE = 0x8867
        const val GL_BUFFER_MAPPED = 0x88BC
        const val GL_BUFFER_MAP_POINTER = 0x88BD
        const val GL_STREAM_READ = 0x88E1
        const val GL_STREAM_COPY = 0x88E2
        const val GL_STATIC_READ = 0x88E5
        const val GL_STATIC_COPY = 0x88E6
        const val GL_DYNAMIC_READ = 0x88E9
        const val GL_DYNAMIC_COPY = 0x88EA
        const val GL_MAX_DRAW_BUFFERS = 0x8824
        const val GL_DRAW_BUFFER0 = 0x8825
        const val GL_DRAW_BUFFER1 = 0x8826
        const val GL_DRAW_BUFFER2 = 0x8827
        const val GL_DRAW_BUFFER3 = 0x8828
        const val GL_DRAW_BUFFER4 = 0x8829
        const val GL_DRAW_BUFFER5 = 0x882A
        const val GL_DRAW_BUFFER6 = 0x882B
        const val GL_DRAW_BUFFER7 = 0x882C
        const val GL_DRAW_BUFFER8 = 0x882D
        const val GL_DRAW_BUFFER9 = 0x882E
        const val GL_DRAW_BUFFER10 = 0x882F
        const val GL_DRAW_BUFFER11 = 0x8830
        const val GL_DRAW_BUFFER12 = 0x8831
        const val GL_DRAW_BUFFER13 = 0x8832
        const val GL_DRAW_BUFFER14 = 0x8833
        const val GL_DRAW_BUFFER15 = 0x8834
        const val GL_MAX_FRAGMENT_UNIFORM_COMPONENTS = 0x8B49
        const val GL_MAX_VERTEX_UNIFORM_COMPONENTS = 0x8B4A
        const val GL_SAMPLER_3D = 0x8B5F
        const val GL_SAMPLER_2D_SHADOW = 0x8B62
        const val GL_FRAGMENT_SHADER_DERIVATIVE_HINT = 0x8B8B
        const val GL_PIXEL_PACK_BUFFER = 0x88EB
        const val GL_PIXEL_UNPACK_BUFFER = 0x88EC
        const val GL_PIXEL_PACK_BUFFER_BINDING = 0x88ED
        const val GL_PIXEL_UNPACK_BUFFER_BINDING = 0x88EF
        const val GL_FLOAT_MAT2x3 = 0x8B65
        const val GL_FLOAT_MAT2x4 = 0x8B66
        const val GL_FLOAT_MAT3x2 = 0x8B67
        const val GL_FLOAT_MAT3x4 = 0x8B68
        const val GL_FLOAT_MAT4x2 = 0x8B69
        const val GL_FLOAT_MAT4x3 = 0x8B6A
        const val GL_SRGB = 0x8C40
        const val GL_SRGB8 = 0x8C41
        const val GL_SRGB8_ALPHA8 = 0x8C43
        const val GL_COMPARE_REF_TO_TEXTURE = 0x884E
        const val GL_MAJOR_VERSION = 0x821B
        const val GL_MINOR_VERSION = 0x821C
        const val GL_NUM_EXTENSIONS = 0x821D
        const val GL_RGBA32F = 0x8814
        const val GL_RGB32F = 0x8815
        const val GL_RGBA16F = 0x881A
        const val GL_RGB16F = 0x881B
        const val GL_VERTEX_ATTRIB_ARRAY_INTEGER = 0x88FD
        const val GL_MAX_ARRAY_TEXTURE_LAYERS = 0x88FF
        const val GL_MIN_PROGRAM_TEXEL_OFFSET = 0x8904
        const val GL_MAX_PROGRAM_TEXEL_OFFSET = 0x8905
        const val GL_MAX_VARYING_COMPONENTS = 0x8B4B
        const val GL_TEXTURE_2D_ARRAY = 0x8C1A
        const val GL_TEXTURE_BINDING_2D_ARRAY = 0x8C1D
        const val GL_R11F_G11F_B10F = 0x8C3A
        const val GL_UNSIGNED_INT_10F_11F_11F_REV = 0x8C3B
        const val GL_RGB9_E5 = 0x8C3D
        const val GL_UNSIGNED_INT_5_9_9_9_REV = 0x8C3E
        const val GL_TRANSFORM_FEEDBACK_VARYING_MAX_LENGTH = 0x8C76
        const val GL_TRANSFORM_FEEDBACK_BUFFER_MODE = 0x8C7F
        const val GL_MAX_TRANSFORM_FEEDBACK_SEPARATE_COMPONENTS = 0x8C80
        const val GL_TRANSFORM_FEEDBACK_VARYINGS = 0x8C83
        const val GL_TRANSFORM_FEEDBACK_BUFFER_START = 0x8C84
        const val GL_TRANSFORM_FEEDBACK_BUFFER_SIZE = 0x8C85
        const val GL_TRANSFORM_FEEDBACK_PRIMITIVES_WRITTEN = 0x8C88
        const val GL_RASTERIZER_DISCARD = 0x8C89
        const val GL_MAX_TRANSFORM_FEEDBACK_INTERLEAVED_COMPONENTS = 0x8C8A
        const val GL_MAX_TRANSFORM_FEEDBACK_SEPARATE_ATTRIBS = 0x8C8B
        const val GL_INTERLEAVED_ATTRIBS = 0x8C8C
        const val GL_SEPARATE_ATTRIBS = 0x8C8D
        const val GL_TRANSFORM_FEEDBACK_BUFFER = 0x8C8E
        const val GL_TRANSFORM_FEEDBACK_BUFFER_BINDING = 0x8C8F
        const val GL_RGBA32UI = 0x8D70
        const val GL_RGB32UI = 0x8D71
        const val GL_RGBA16UI = 0x8D76
        const val GL_RGB16UI = 0x8D77
        const val GL_RGBA8UI = 0x8D7C
        const val GL_RGB8UI = 0x8D7D
        const val GL_RGBA32I = 0x8D82
        const val GL_RGB32I = 0x8D83
        const val GL_RGBA16I = 0x8D88
        const val GL_RGB16I = 0x8D89
        const val GL_RGBA8I = 0x8D8E
        const val GL_RGB8I = 0x8D8F
        const val GL_RED_INTEGER = 0x8D94
        const val GL_RGB_INTEGER = 0x8D98
        const val GL_RGBA_INTEGER = 0x8D99
        const val GL_SAMPLER_2D_ARRAY = 0x8DC1
        const val GL_SAMPLER_2D_ARRAY_SHADOW = 0x8DC4
        const val GL_SAMPLER_CUBE_SHADOW = 0x8DC5
        const val GL_UNSIGNED_INT_VEC2 = 0x8DC6
        const val GL_UNSIGNED_INT_VEC3 = 0x8DC7
        const val GL_UNSIGNED_INT_VEC4 = 0x8DC8
        const val GL_INT_SAMPLER_2D = 0x8DCA
        const val GL_INT_SAMPLER_3D = 0x8DCB
        const val GL_INT_SAMPLER_CUBE = 0x8DCC
        const val GL_INT_SAMPLER_2D_ARRAY = 0x8DCF
        const val GL_UNSIGNED_INT_SAMPLER_2D = 0x8DD2
        const val GL_UNSIGNED_INT_SAMPLER_3D = 0x8DD3
        const val GL_UNSIGNED_INT_SAMPLER_CUBE = 0x8DD4
        const val GL_UNSIGNED_INT_SAMPLER_2D_ARRAY = 0x8DD7
        const val GL_BUFFER_ACCESS_FLAGS = 0x911F
        const val GL_BUFFER_MAP_LENGTH = 0x9120
        const val GL_BUFFER_MAP_OFFSET = 0x9121
        const val GL_DEPTH_COMPONENT32F = 0x8CAC
        const val GL_DEPTH32F_STENCIL8 = 0x8CAD
        const val GL_FLOAT_32_UNSIGNED_INT_24_8_REV = 0x8DAD
        const val GL_FRAMEBUFFER_ATTACHMENT_COLOR_ENCODING = 0x8210
        const val GL_FRAMEBUFFER_ATTACHMENT_COMPONENT_TYPE = 0x8211
        const val GL_FRAMEBUFFER_ATTACHMENT_RED_SIZE = 0x8212
        const val GL_FRAMEBUFFER_ATTACHMENT_GREEN_SIZE = 0x8213
        const val GL_FRAMEBUFFER_ATTACHMENT_BLUE_SIZE = 0x8214
        const val GL_FRAMEBUFFER_ATTACHMENT_ALPHA_SIZE = 0x8215
        const val GL_FRAMEBUFFER_ATTACHMENT_DEPTH_SIZE = 0x8216
        const val GL_FRAMEBUFFER_ATTACHMENT_STENCIL_SIZE = 0x8217
        const val GL_FRAMEBUFFER_DEFAULT = 0x8218
        const val GL_FRAMEBUFFER_UNDEFINED = 0x8219
        const val GL_DEPTH_STENCIL_ATTACHMENT = 0x821A
        const val GL_DEPTH_STENCIL = 0x84F9
        const val GL_UNSIGNED_INT_24_8 = 0x84FA
        const val GL_DEPTH24_STENCIL8 = 0x88F0
        const val GL_UNSIGNED_NORMALIZED = 0x8C17
        const val GL_DRAW_FRAMEBUFFER_BINDING = GL20.GL_FRAMEBUFFER_BINDING
        const val GL_READ_FRAMEBUFFER = 0x8CA8
        const val GL_DRAW_FRAMEBUFFER = 0x8CA9
        const val GL_READ_FRAMEBUFFER_BINDING = 0x8CAA
        const val GL_RENDERBUFFER_SAMPLES = 0x8CAB
        const val GL_FRAMEBUFFER_ATTACHMENT_TEXTURE_LAYER = 0x8CD4
        const val GL_MAX_COLOR_ATTACHMENTS = 0x8CDF
        const val GL_COLOR_ATTACHMENT1 = 0x8CE1
        const val GL_COLOR_ATTACHMENT2 = 0x8CE2
        const val GL_COLOR_ATTACHMENT3 = 0x8CE3
        const val GL_COLOR_ATTACHMENT4 = 0x8CE4
        const val GL_COLOR_ATTACHMENT5 = 0x8CE5
        const val GL_COLOR_ATTACHMENT6 = 0x8CE6
        const val GL_COLOR_ATTACHMENT7 = 0x8CE7
        const val GL_COLOR_ATTACHMENT8 = 0x8CE8
        const val GL_COLOR_ATTACHMENT9 = 0x8CE9
        const val GL_COLOR_ATTACHMENT10 = 0x8CEA
        const val GL_COLOR_ATTACHMENT11 = 0x8CEB
        const val GL_COLOR_ATTACHMENT12 = 0x8CEC
        const val GL_COLOR_ATTACHMENT13 = 0x8CED
        const val GL_COLOR_ATTACHMENT14 = 0x8CEE
        const val GL_COLOR_ATTACHMENT15 = 0x8CEF
        const val GL_FRAMEBUFFER_INCOMPLETE_MULTISAMPLE = 0x8D56
        const val GL_MAX_SAMPLES = 0x8D57
        const val GL_HALF_FLOAT = 0x140B
        const val GL_MAP_READ_BIT = 0x0001
        const val GL_MAP_WRITE_BIT = 0x0002
        const val GL_MAP_INVALIDATE_RANGE_BIT = 0x0004
        const val GL_MAP_INVALIDATE_BUFFER_BIT = 0x0008
        const val GL_MAP_FLUSH_EXPLICIT_BIT = 0x0010
        const val GL_MAP_UNSYNCHRONIZED_BIT = 0x0020
        const val GL_RG = 0x8227
        const val GL_RG_INTEGER = 0x8228
        const val GL_R8 = 0x8229
        const val GL_RG8 = 0x822B
        const val GL_R16F = 0x822D
        const val GL_R32F = 0x822E
        const val GL_RG16F = 0x822F
        const val GL_RG32F = 0x8230
        const val GL_R8I = 0x8231
        const val GL_R8UI = 0x8232
        const val GL_R16I = 0x8233
        const val GL_R16UI = 0x8234
        const val GL_R32I = 0x8235
        const val GL_R32UI = 0x8236
        const val GL_RG8I = 0x8237
        const val GL_RG8UI = 0x8238
        const val GL_RG16I = 0x8239
        const val GL_RG16UI = 0x823A
        const val GL_RG32I = 0x823B
        const val GL_RG32UI = 0x823C
        const val GL_VERTEX_ARRAY_BINDING = 0x85B5
        const val GL_R8_SNORM = 0x8F94
        const val GL_RG8_SNORM = 0x8F95
        const val GL_RGB8_SNORM = 0x8F96
        const val GL_RGBA8_SNORM = 0x8F97
        const val GL_SIGNED_NORMALIZED = 0x8F9C
        const val GL_PRIMITIVE_RESTART_FIXED_INDEX = 0x8D69
        const val GL_COPY_READ_BUFFER = 0x8F36
        const val GL_COPY_WRITE_BUFFER = 0x8F37
        const val GL_COPY_READ_BUFFER_BINDING = GL_COPY_READ_BUFFER
        const val GL_COPY_WRITE_BUFFER_BINDING = GL_COPY_WRITE_BUFFER
        const val GL_UNIFORM_BUFFER = 0x8A11
        const val GL_UNIFORM_BUFFER_BINDING = 0x8A28
        const val GL_UNIFORM_BUFFER_START = 0x8A29
        const val GL_UNIFORM_BUFFER_SIZE = 0x8A2A
        const val GL_MAX_VERTEX_UNIFORM_BLOCKS = 0x8A2B
        const val GL_MAX_FRAGMENT_UNIFORM_BLOCKS = 0x8A2D
        const val GL_MAX_COMBINED_UNIFORM_BLOCKS = 0x8A2E
        const val GL_MAX_UNIFORM_BUFFER_BINDINGS = 0x8A2F
        const val GL_MAX_UNIFORM_BLOCK_SIZE = 0x8A30
        const val GL_MAX_COMBINED_VERTEX_UNIFORM_COMPONENTS = 0x8A31
        const val GL_MAX_COMBINED_FRAGMENT_UNIFORM_COMPONENTS = 0x8A33
        const val GL_UNIFORM_BUFFER_OFFSET_ALIGNMENT = 0x8A34
        const val GL_ACTIVE_UNIFORM_BLOCK_MAX_NAME_LENGTH = 0x8A35
        const val GL_ACTIVE_UNIFORM_BLOCKS = 0x8A36
        const val GL_UNIFORM_TYPE = 0x8A37
        const val GL_UNIFORM_SIZE = 0x8A38
        const val GL_UNIFORM_NAME_LENGTH = 0x8A39
        const val GL_UNIFORM_BLOCK_INDEX = 0x8A3A
        const val GL_UNIFORM_OFFSET = 0x8A3B
        const val GL_UNIFORM_ARRAY_STRIDE = 0x8A3C
        const val GL_UNIFORM_MATRIX_STRIDE = 0x8A3D
        const val GL_UNIFORM_IS_ROW_MAJOR = 0x8A3E
        const val GL_UNIFORM_BLOCK_BINDING = 0x8A3F
        const val GL_UNIFORM_BLOCK_DATA_SIZE = 0x8A40
        const val GL_UNIFORM_BLOCK_NAME_LENGTH = 0x8A41
        const val GL_UNIFORM_BLOCK_ACTIVE_UNIFORMS = 0x8A42
        const val GL_UNIFORM_BLOCK_ACTIVE_UNIFORM_INDICES = 0x8A43
        const val GL_UNIFORM_BLOCK_REFERENCED_BY_VERTEX_SHADER = 0x8A44
        const val GL_UNIFORM_BLOCK_REFERENCED_BY_FRAGMENT_SHADER = 0x8A46

        // GL_INVALID_INDEX is defined as 0xFFFFFFFFu in C.
        const val GL_INVALID_INDEX = -1
        const val GL_MAX_VERTEX_OUTPUT_COMPONENTS = 0x9122
        const val GL_MAX_FRAGMENT_INPUT_COMPONENTS = 0x9125
        const val GL_MAX_SERVER_WAIT_TIMEOUT = 0x9111
        const val GL_OBJECT_TYPE = 0x9112
        const val GL_SYNC_CONDITION = 0x9113
        const val GL_SYNC_STATUS = 0x9114
        const val GL_SYNC_FLAGS = 0x9115
        const val GL_SYNC_FENCE = 0x9116
        const val GL_SYNC_GPU_COMMANDS_COMPLETE = 0x9117
        const val GL_UNSIGNALED = 0x9118
        const val GL_SIGNALED = 0x9119
        const val GL_ALREADY_SIGNALED = 0x911A
        const val GL_TIMEOUT_EXPIRED = 0x911B
        const val GL_CONDITION_SATISFIED = 0x911C
        const val GL_WAIT_FAILED = 0x911D
        const val GL_SYNC_FLUSH_COMMANDS_BIT = 0x00000001

        // GL_TIMEOUT_IGNORED is defined as 0xFFFFFFFFFFFFFFFFull in C.
        const val GL_TIMEOUT_IGNORED: Long = -1
        const val GL_VERTEX_ATTRIB_ARRAY_DIVISOR = 0x88FE
        const val GL_ANY_SAMPLES_PASSED = 0x8C2F
        const val GL_ANY_SAMPLES_PASSED_CONSERVATIVE = 0x8D6A
        const val GL_SAMPLER_BINDING = 0x8919
        const val GL_RGB10_A2UI = 0x906F
        const val GL_TEXTURE_SWIZZLE_R = 0x8E42
        const val GL_TEXTURE_SWIZZLE_G = 0x8E43
        const val GL_TEXTURE_SWIZZLE_B = 0x8E44
        const val GL_TEXTURE_SWIZZLE_A = 0x8E45
        const val GL_GREEN = 0x1904
        const val GL_BLUE = 0x1905
        const val GL_INT_2_10_10_10_REV = 0x8D9F
        const val GL_TRANSFORM_FEEDBACK = 0x8E22
        const val GL_TRANSFORM_FEEDBACK_PAUSED = 0x8E23
        const val GL_TRANSFORM_FEEDBACK_ACTIVE = 0x8E24
        const val GL_TRANSFORM_FEEDBACK_BINDING = 0x8E25
        const val GL_PROGRAM_BINARY_RETRIEVABLE_HINT = 0x8257
        const val GL_PROGRAM_BINARY_LENGTH = 0x8741
        const val GL_NUM_PROGRAM_BINARY_FORMATS = 0x87FE
        const val GL_PROGRAM_BINARY_FORMATS = 0x87FF
        const val GL_COMPRESSED_R11_EAC = 0x9270
        const val GL_COMPRESSED_SIGNED_R11_EAC = 0x9271
        const val GL_COMPRESSED_RG11_EAC = 0x9272
        const val GL_COMPRESSED_SIGNED_RG11_EAC = 0x9273
        const val GL_COMPRESSED_RGB8_ETC2 = 0x9274
        const val GL_COMPRESSED_SRGB8_ETC2 = 0x9275
        const val GL_COMPRESSED_RGB8_PUNCHTHROUGH_ALPHA1_ETC2 = 0x9276
        const val GL_COMPRESSED_SRGB8_PUNCHTHROUGH_ALPHA1_ETC2 = 0x9277
        const val GL_COMPRESSED_RGBA8_ETC2_EAC = 0x9278
        const val GL_COMPRESSED_SRGB8_ALPHA8_ETC2_EAC = 0x9279
        const val GL_TEXTURE_IMMUTABLE_FORMAT = 0x912F
        const val GL_MAX_ELEMENT_INDEX = 0x8D6B
        const val GL_NUM_SAMPLE_COUNTS = 0x9380
        const val GL_TEXTURE_IMMUTABLE_LEVELS = 0x82DF
    }
}
