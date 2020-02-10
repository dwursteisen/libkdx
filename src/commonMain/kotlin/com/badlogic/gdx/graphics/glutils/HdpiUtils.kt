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

/** To deal with HDPI monitors properly, use the glViewport and glScissor functions of this class instead of directly calling
 * OpenGL yourself. The logical coordinate system provided by the operating system may not have the same resolution as the actual
 * drawing surface to which OpenGL draws, also known as the backbuffer. This class will ensure, that you pass the correct values
 * to OpenGL for any function that expects backbuffer coordinates instead of logical coordinates.
 *
 * @author badlogic
 */
object HdpiUtils {

    private var mode: HdpiMode? = HdpiMode.Logical
    /** Allows applications to override HDPI coordinate conversion for glViewport and glScissor calls.
     *
     * This function can be used to ignore the default behavior, for example when rendering a UI stage
     * to an off-screen framebuffer:
     *
     * <pre>
     * HdpiUtils.setMode(HdpiMode.Pixels);
     * fb.begin();
     * stage.draw();
     * fb.end();
     * HdpiUtils.setMode(HdpiMode.Logical);
    </pre> *
     *
     * @param mode set to HdpiMode.Pixels to ignore HDPI conversion for glViewport and glScissor functions
     */
    fun setMode(mode: HdpiMode?) {
        HdpiUtils.mode = mode
    }

    /** Calls [GL20.glScissor], expecting the coordinates and sizes given in logical coordinates and
     * automatically converts them to backbuffer coordinates, which may be bigger on HDPI screens.  */
    @kotlin.jvm.JvmStatic
    fun glScissor(x: Int, y: Int, width: Int, height: Int) {
        if (mode == HdpiMode.Logical && (com.badlogic.gdx.Gdx.graphics.getWidth() != com.badlogic.gdx.Gdx.graphics.getBackBufferWidth()
                || com.badlogic.gdx.Gdx.graphics.getHeight() != com.badlogic.gdx.Gdx.graphics.getBackBufferHeight())) {
            com.badlogic.gdx.Gdx.gl.glScissor(toBackBufferX(x), toBackBufferY(y), toBackBufferX(width), toBackBufferY(height))
        } else {
            com.badlogic.gdx.Gdx.gl.glScissor(x, y, width, height)
        }
    }

    /** Calls [GL20.glViewport], expecting the coordinates and sizes given in logical coordinates and
     * automatically converts them to backbuffer coordinates, which may be bigger on HDPI screens.  */
    @kotlin.jvm.JvmStatic
    fun glViewport(x: Int, y: Int, width: Int, height: Int) {
        if (mode == HdpiMode.Logical && (com.badlogic.gdx.Gdx.graphics.getWidth() != com.badlogic.gdx.Gdx.graphics.getBackBufferWidth()
                || com.badlogic.gdx.Gdx.graphics.getHeight() != com.badlogic.gdx.Gdx.graphics.getBackBufferHeight())) {
            com.badlogic.gdx.Gdx.gl.glViewport(toBackBufferX(x), toBackBufferY(y), toBackBufferX(width), toBackBufferY(height))
        } else {
            com.badlogic.gdx.Gdx.gl.glViewport(x, y, width, height)
        }
    }

    /**
     * Converts an x-coordinate given in backbuffer coordinates to
     * logical screen coordinates.
     */
    fun toLogicalX(backBufferX: Int): Int {
        return (backBufferX * com.badlogic.gdx.Gdx.graphics.getWidth() / com.badlogic.gdx.Gdx.graphics.getBackBufferWidth() as Float)
    }

    /**
     * Convers an y-coordinate given in backbuffer coordinates to
     * logical screen coordinates
     */
    fun toLogicalY(backBufferY: Int): Int {
        return (backBufferY * com.badlogic.gdx.Gdx.graphics.getHeight() / com.badlogic.gdx.Gdx.graphics.getBackBufferHeight() as Float)
    }

    /**
     * Converts an x-coordinate given in logical screen coordinates to
     * backbuffer coordinates.
     */
    fun toBackBufferX(logicalX: Int): Int {
        return (logicalX * com.badlogic.gdx.Gdx.graphics.getBackBufferWidth() / com.badlogic.gdx.Gdx.graphics.getWidth() as Float)
    }

    /**
     * Convers an y-coordinate given in backbuffer coordinates to
     * logical screen coordinates
     */
    fun toBackBufferY(logicalY: Int): Int {
        return (logicalY * com.badlogic.gdx.Gdx.graphics.getBackBufferHeight() / com.badlogic.gdx.Gdx.graphics.getHeight() as Float)
    }
}
