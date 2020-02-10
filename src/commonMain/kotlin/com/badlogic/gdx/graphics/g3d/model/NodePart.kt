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
package com.badlogic.gdx.graphics.g3d.model

import kotlin.jvm.JvmField

/** A combination of [MeshPart] and [Material], used to represent a [Node]'s graphical properties. A NodePart is
 * the smallest visible part of a [Model], each NodePart implies a render call.
 * @author badlogic, Xoppa
 */
class NodePart {

    /** The MeshPart (shape) to render. Must not be null.  */
    @JvmField
    var meshPart: com.badlogic.gdx.graphics.g3d.model.MeshPart? = null
    /** The Material used to render the [.meshPart]. Must not be null.  */
    @JvmField
    var material: com.badlogic.gdx.graphics.g3d.Material? = null
    /** Mapping to each bone (node) and the inverse transform of the bind pose. Will be used to fill the [.bones] array. May
     * be null.  */
    @JvmField
    var invBoneBindTransforms: com.badlogic.gdx.utils.ArrayMap<com.badlogic.gdx.graphics.g3d.model.Node?, com.badlogic.gdx.math.Matrix4?>? = null
    /** The current transformation (relative to the bind pose) of each bone, may be null. When the part is skinned, this will be
     * updated by a call to [ModelInstance.calculateTransforms]. Do not set or change this value manually.  */
    @JvmField
    var bones: Array<com.badlogic.gdx.math.Matrix4?>?
    /** true by default. If set to false, this part will not participate in rendering and bounding box calculation.  */
    @JvmField
    var enabled = true

    /** Construct a new NodePart with null values. At least the [.meshPart] and [.material] member must be set before
     * the newly created part can be used.  */
    constructor() {}

    /** Construct a new NodePart referencing the provided [MeshPart] and [Material].
     * @param meshPart The MeshPart to reference.
     * @param material The Material to reference.
     */
    constructor(meshPart: com.badlogic.gdx.graphics.g3d.model.MeshPart?, material: com.badlogic.gdx.graphics.g3d.Material?) {
        this.meshPart = meshPart
        this.material = material
    }
    // FIXME add copy constructor and override #equals.
    /** Convenience method to set the material, mesh, meshPartOffset, meshPartSize, primitiveType and bones members of the specified
     * Renderable. The other member of the provided [Renderable] remain untouched. Note that the material, mesh and bones
     * members are referenced, not copied. Any changes made to those objects will be reflected in both the NodePart and Renderable
     * object.
     * @param out The Renderable of which to set the members to the values of this NodePart.
     */
    fun setRenderable(out: com.badlogic.gdx.graphics.g3d.Renderable?): com.badlogic.gdx.graphics.g3d.Renderable? {
        out.material = material
        out.meshPart.set(meshPart)
        out.bones = bones
        return out
    }

    fun copy(): NodePart? {
        return NodePart().set(this)
    }

    protected fun set(other: NodePart?): NodePart? {
        meshPart = com.badlogic.gdx.graphics.g3d.model.MeshPart(other!!.meshPart)
        material = other!!.material
        enabled = other.enabled
        if (other.invBoneBindTransforms == null) {
            invBoneBindTransforms = null
            bones = null
        } else {
            if (invBoneBindTransforms == null) invBoneBindTransforms = com.badlogic.gdx.utils.ArrayMap<com.badlogic.gdx.graphics.g3d.model.Node?, com.badlogic.gdx.math.Matrix4?>(true, other.invBoneBindTransforms.size, com.badlogic.gdx.graphics.g3d.model.Node::class.java, com.badlogic.gdx.math.Matrix4::class.java) else invBoneBindTransforms.clear()
            invBoneBindTransforms.putAll(other.invBoneBindTransforms)
            if (bones == null || bones!!.size != invBoneBindTransforms.size) bones = arrayOfNulls<com.badlogic.gdx.math.Matrix4?>(invBoneBindTransforms.size)
            for (i in bones!!.indices) {
                if (bones!![i] == null) bones!![i] = com.badlogic.gdx.math.Matrix4()
            }
        }
        return this
    }
}
