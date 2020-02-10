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

interface ImmediateModeRenderer {
    fun begin(projModelView: com.badlogic.gdx.math.Matrix4?, primitiveType: Int)
    fun flush()
    fun color(color: com.badlogic.gdx.graphics.Color?)
    fun color(r: Float, g: Float, b: Float, a: Float)
    fun color(colorBits: Float)
    fun texCoord(u: Float, v: Float)
    fun normal(x: Float, y: Float, z: Float)
    fun vertex(x: Float, y: Float, z: Float)
    fun end()
    fun getNumVertices(): Int
    fun getMaxVertices(): Int
    fun dispose()
}
