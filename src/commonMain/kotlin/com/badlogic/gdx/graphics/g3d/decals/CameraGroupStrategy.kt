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
package com.badlogic.gdx.graphics.g3d.decals

import Mesh.VertexDataType
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g3d.decals.Decal
import com.badlogic.gdx.graphics.g3d.decals.DecalBatch
import com.badlogic.gdx.graphics.g3d.decals.DecalMaterial
import com.badlogic.gdx.graphics.g3d.decals.SimpleOrthoGroupStrategy
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.Pool

/**
 *
 *
 * Minimalistic grouping strategy that splits decals into opaque and transparent ones enabling and disabling blending as needed.
 * Opaque decals are rendered first (decal color is ignored in opacity check).<br></br>
 * Use this strategy only if the vast majority of your decals are opaque and the few transparent ones are unlikely to overlap.
 *
 *
 *
 * Can produce invisible artifacts when transparent decals overlap each other.
 *
 *
 *
 * Needs to be explicitly disposed as it might allocate a ShaderProgram when GLSL 2.0 is used.
 *
 *
 *
 * States (* = any, EV = entry value - same as value before flush):<br></br>
 * <table>
 * <tr>
 * <td></td>
 * <td>expects</td>
 * <td>exits on</td>
</tr> *
 * <tr>
 * <td>glDepthMask</td>
 * <td>true</td>
 * <td>EV</td>
</tr> *
 * <tr>
 * <td>GL_DEPTH_TEST</td>
 * <td>enabled</td>
 * <td>EV</td>
</tr> *
 * <tr>
 * <td>glDepthFunc</td>
 * <td>GL_LESS | GL_LEQUAL</td>
 * <td>EV</td>
</tr> *
 * <tr>
 * <td>GL_BLEND</td>
 * <td>disabled</td>
 * <td>EV | disabled</td>
</tr> *
 * <tr>
 * <td>glBlendFunc</td>
 * <td>*</td>
 * <td>*</td>
</tr> *
 * <tr>
 * <td>GL_TEXTURE_2D</td>
 * <td>*</td>
 * <td>disabled</td>
</tr> *
</table> *
 *
 */
class CameraGroupStrategy(camera: Camera?, sorter: java.util.Comparator<Decal?>?) : GroupStrategy, Disposable {

    var arrayPool: Pool<Array<Decal?>?>? = object : Pool<Array<Decal?>?>(16) {
        protected fun newObject(): Array<Decal?>? {
            return Array()
        }
    }
    var usedArrays: Array<Array<Decal?>?>? = Array<Array<Decal?>?>()
    var materialGroups: ObjectMap<DecalMaterial?, Array<Decal?>?>? = ObjectMap<DecalMaterial?, Array<Decal?>?>()
    var camera: Camera?
    var shader: ShaderProgram? = null
    private val cameraSorter: java.util.Comparator<Decal?>?

    constructor(camera: Camera?) : this(camera, object : java.util.Comparator<Decal?>() {
        override fun compare(o1: Decal?, o2: Decal?): Int {
            val dist1: Float = camera.position.dst(o1!!.position)
            val dist2: Float = camera.position.dst(o2!!.position)
            return java.lang.Math.signum(dist2 - dist1)
        }
    }) {
    }

    fun setCamera(camera: Camera?) {
        this.camera = camera
    }

    fun getCamera(): Camera? {
        return camera
    }

    override fun decideGroup(decal: Decal?): Int {
        return if (decal!!.getMaterial()!!.isOpaque()) GROUP_OPAQUE else GROUP_BLEND
    }

    override fun beforeGroup(group: Int, contents: Array<Decal?>?) {
        if (group == GROUP_BLEND) {
            Gdx.gl.glEnable(GL20.GL_BLEND)
            contents.sort(cameraSorter)
        } else {
            var i = 0
            val n = contents!!.size
            while (i < n) {
                val decal: Decal? = contents[i]
                var materialGroup: Array<Decal?> = materialGroups.get(decal!!.material)
                if (materialGroup == null) {
                    materialGroup = arrayPool.obtain()
                    materialGroup.clear()
                    usedArrays.add(materialGroup)
                    materialGroups.put(decal!!.material, materialGroup)
                }
                materialGroup.add(decal)
                i++
            }
            contents.clear()
            for (materialGroup in materialGroups.values()) {
                contents.addAll(materialGroup)
            }
            materialGroups.clear()
            arrayPool.freeAll(usedArrays)
            usedArrays.clear()
        }
    }

    override fun afterGroup(group: Int) {
        if (group == GROUP_BLEND) {
            Gdx.gl.glDisable(GL20.GL_BLEND)
        }
    }

    override fun beforeGroups() {
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST)
        shader!!.begin()
        shader.setUniformMatrix("u_projectionViewMatrix", camera.combined)
        shader!!.setUniformi("u_texture", 0)
    }

    override fun afterGroups() {
        shader!!.end()
        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST)
    }

    private fun createDefaultShader() {
        val vertexShader = """attribute vec4 ${ShaderProgram.POSITION_ATTRIBUTE};
attribute vec4 ${ShaderProgram.COLOR_ATTRIBUTE};
attribute vec2 ${ShaderProgram.TEXCOORD_ATTRIBUTE}0;
uniform mat4 u_projectionViewMatrix;
varying vec4 v_color;
varying vec2 v_texCoords;

void main()
{
   v_color = ${ShaderProgram.COLOR_ATTRIBUTE};
   v_color.a = v_color.a * (255.0/254.0);
   v_texCoords = ${ShaderProgram.TEXCOORD_ATTRIBUTE}0;
   gl_Position =  u_projectionViewMatrix * ${ShaderProgram.POSITION_ATTRIBUTE};
}
"""
        val fragmentShader = """#ifdef GL_ES
precision mediump float;
#endif
varying vec4 v_color;
varying vec2 v_texCoords;
uniform sampler2D u_texture;
void main()
{
  gl_FragColor = v_color * texture2D(u_texture, v_texCoords);
}"""
        shader = ShaderProgram(vertexShader, fragmentShader)
        if (!shader!!.isCompiled()) throw java.lang.IllegalArgumentException("couldn't compile shader: " + shader!!.getLog())
    }

    override fun getGroupShader(group: Int): ShaderProgram? {
        return shader
    }

    fun dispose() {
        if (shader != null) shader!!.dispose()
    }

    companion object {
        private const val GROUP_OPAQUE = 0
        private const val GROUP_BLEND = 1
    }

    init {
        this.camera = camera
        cameraSorter = sorter
        createDefaultShader()
    }
}
