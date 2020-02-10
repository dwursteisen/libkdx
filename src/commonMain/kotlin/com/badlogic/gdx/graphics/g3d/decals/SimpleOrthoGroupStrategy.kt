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
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g3d.decals.CameraGroupStrategy
import com.badlogic.gdx.graphics.g3d.decals.Decal
import com.badlogic.gdx.graphics.g3d.decals.DecalBatch
import com.badlogic.gdx.graphics.g3d.decals.DecalMaterial
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.Sort

/**
 *
 *
 * Minimalistic grouping strategy useful for orthogonal scenes where the camera faces the negative z axis. Handles enabling and
 * disabling of blending and uses world-z only front to back sorting for transparent decals.
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
 * <td>EV | true</td>
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
class SimpleOrthoGroupStrategy : GroupStrategy {

    private val comparator: Comparator? = Comparator()
    override fun decideGroup(decal: Decal?): Int {
        return if (decal!!.getMaterial()!!.isOpaque()) GROUP_OPAQUE else GROUP_BLEND
    }

    override fun beforeGroup(group: Int, contents: Array<Decal?>?) {
        if (group == GROUP_BLEND) {
            Sort.instance().sort(contents, comparator)
            Gdx.gl.glEnable(GL20.GL_BLEND)
            // no need for writing into the z buffer if transparent decals are the last thing to be rendered
            // and they are rendered back to front
            Gdx.gl.glDepthMask(false)
        } else {
            // FIXME sort by material
        }
    }

    override fun afterGroup(group: Int) {
        if (group == GROUP_BLEND) {
            Gdx.gl.glDepthMask(true)
            Gdx.gl.glDisable(GL20.GL_BLEND)
        }
    }

    override fun beforeGroups() {
        Gdx.gl.glEnable(GL20.GL_TEXTURE_2D)
    }

    override fun afterGroups() {
        Gdx.gl.glDisable(GL20.GL_TEXTURE_2D)
    }

    internal inner class Comparator : java.util.Comparator<Decal?> {
        override fun compare(a: Decal?, b: Decal?): Int {
            if (a.getZ() === b.getZ()) return 0
            return if (a.getZ() - b.getZ() < 0) -1 else 1
        }
    }

    override fun getGroupShader(group: Int): ShaderProgram? {
        return null
    }

    companion object {
        private const val GROUP_OPAQUE = 0
        private const val GROUP_BLEND = 1
    }
}
