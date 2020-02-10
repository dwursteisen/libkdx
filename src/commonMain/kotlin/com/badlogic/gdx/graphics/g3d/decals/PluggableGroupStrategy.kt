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
import com.badlogic.gdx.graphics.g3d.decals.CameraGroupStrategy
import com.badlogic.gdx.graphics.g3d.decals.Decal
import com.badlogic.gdx.graphics.g3d.decals.DecalBatch
import com.badlogic.gdx.graphics.g3d.decals.DecalMaterial
import com.badlogic.gdx.graphics.g3d.decals.SimpleOrthoGroupStrategy
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.IntMap

/**
 * This class in combination with the [GroupPlugs][GroupPlug] allows you to build a modular [GroupStrategy] out of
 * routines you already implemented.
 */
abstract class PluggableGroupStrategy : GroupStrategy {

    private val plugs: IntMap<GroupPlug?>? = IntMap<GroupPlug?>()
    override fun beforeGroup(group: Int, contents: Array<Decal?>?) {
        plugs.get(group).beforeGroup(contents)
    }

    override fun afterGroup(group: Int) {
        plugs.get(group).afterGroup()
    }

    /**
     * Set the plug used for a specific group. The plug will automatically be invoked.
     *
     * @param plug  Plug to use
     * @param group Group the plug is for
     */
    fun plugIn(plug: GroupPlug?, group: Int) {
        plugs.put(group, plug)
    }

    /**
     * Remove a plug from the strategy
     *
     * @param group Group to remove the plug from
     * @return removed plug, null if there was none for that group
     */
    fun unPlug(group: Int): GroupPlug? {
        return plugs.remove(group)
    }
}
