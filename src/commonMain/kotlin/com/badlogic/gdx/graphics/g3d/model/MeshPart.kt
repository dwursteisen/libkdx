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

import com.badlogic.gdx.graphics.glutils.ShaderProgram
import kotlin.jvm.JvmField

/** A MeshPart is composed of a subset of vertices of a [Mesh], along with the primitive type. The vertices subset is
 * described by an offset and size. When the mesh is indexed (which is when [Mesh.getNumIndices] > 0), then the
 * [.offset] represents the offset in the indices array and [.size] represents the number of indices. When the mesh
 * isn't indexed, then the [.offset] member represents the offset in the vertices array and the [.size] member
 * represents the number of vertices.
 *
 * In other words: Regardless whether the mesh is indexed or not, when [.primitiveType] is not a strip, then [.size]
 * equals the number of primitives multiplied by the number of vertices per primitive. So if the MeshPart represents 4 triangles (
 * [.primitiveType] is GL_TRIANGLES), then the [.size] member is 12 (4 triangles * 3 vertices = 12 vertices total).
 * Likewise, if the part represents 12 lines ([.primitiveType] is GL_LINES), then the size is 24 (12 lines * 2 vertices = 24
 * vertices total).
 *
 * Note that some classes might require the mesh (part) to be indexed.
 *
 * The [Mesh] referenced by the [.mesh] member must outlive the MeshPart. When the mesh is disposed, the MeshPart is
 * unusable.
 * @author badlogic, Xoppa
 */
class MeshPart {

    /** Unique id within model, may be null. Will be ignored by [.equals]  */
    @JvmField
    var id: String? = null
    /** The primitive type, OpenGL constant e.g: [GL20.GL_TRIANGLES], [GL20.GL_POINTS], [GL20.GL_LINES],
     * [GL20.GL_LINE_STRIP], [GL20.GL_TRIANGLE_STRIP]  */
    @JvmField
    var primitiveType = 0
    /** The offset in the [.mesh] to this part. If the mesh is indexed ([Mesh.getNumIndices] > 0), this is the offset
     * in the indices array, otherwise it is the offset in the vertices array.  */
    @JvmField
    var offset = 0
    /** The size (in total number of vertices) of this part in the [.mesh]. When the mesh is indexed (
     * [Mesh.getNumIndices] > 0), this is the number of indices, otherwise it is the number of vertices.  */
    @JvmField
    var size = 0
    /** The Mesh the part references, also stored in [Model]  */
    @JvmField
    var mesh: com.badlogic.gdx.graphics.Mesh? = null
    /** The offset to the center of the bounding box of the shape, only valid after the call to [.update].  */
    @JvmField
    val center: com.badlogic.gdx.math.Vector3? = com.badlogic.gdx.math.Vector3()
    /** The location, relative to [.center], of the corner of the axis aligned bounding box of the shape. Or, in other words:
     * half the dimensions of the bounding box of the shape, where [Vector3.x] is half the width, [Vector3.y] is half
     * the height and [Vector3.z] is half the depth. Only valid after the call to [.update].  */
    @JvmField
    val halfExtents: com.badlogic.gdx.math.Vector3? = com.badlogic.gdx.math.Vector3()
    /** The radius relative to [.center] of the bounding sphere of the shape, or negative if not calculated yet. This is the
     * same as the length of the [.halfExtents] member. See [.update].  */
    @JvmField
    var radius = -1f

    /** Construct a new MeshPart, with null values. The MeshPart is unusable until you set all members.  */
    constructor() {}

    /** Construct a new MeshPart and set all its values.
     * @param id The id of the new part, may be null.
     * @param mesh The mesh which holds all vertices and (optional) indices of this part.
     * @param offset The offset within the mesh to this part.
     * @param size The size (in total number of vertices) of the part.
     * @param type The primitive type of the part (e.g. GL_TRIANGLES, GL_LINE_STRIP, etc.).
     */
    constructor(id: String?, mesh: com.badlogic.gdx.graphics.Mesh?, offset: Int, size: Int, type: Int) {
        set(id, mesh, offset, size, type)
    }

    /** Construct a new MeshPart which is an exact copy of the provided MeshPart.
     * @param copyFrom The MeshPart to copy.
     */
    constructor(copyFrom: MeshPart?) {
        set(copyFrom)
    }

    /** Set this MeshPart to be a copy of the other MeshPart
     * @param other The MeshPart from which to copy the values
     * @return this MeshPart, for chaining
     */
    fun set(other: MeshPart?): MeshPart? {
        id = other!!.id
        mesh = other.mesh
        offset = other.offset
        size = other.size
        primitiveType = other.primitiveType
        center.set(other.center)
        halfExtents.set(other.halfExtents)
        radius = other.radius
        return this
    }

    /** Set this MeshPart to given values, does not [.update] the bounding box values.
     * @return this MeshPart, for chaining.
     */
    operator fun set(id: String?, mesh: com.badlogic.gdx.graphics.Mesh?, offset: Int, size: Int, type: Int): MeshPart? {
        this.id = id
        this.mesh = mesh
        this.offset = offset
        this.size = size
        primitiveType = type
        center.set(0f, 0f, 0f)
        halfExtents.set(0f, 0f, 0f)
        radius = -1f
        return this
    }

    /** Calculates and updates the [.center], [.halfExtents] and [.radius] values. This is considered a costly
     * operation and should not be called frequently. All vertices (points) of the shape are traversed to calculate the maximum and
     * minimum x, y and z coordinate of the shape. Note that MeshPart is not aware of any transformation that might be applied when
     * rendering. It calculates the untransformed (not moved, not scaled, not rotated) values.  */
    fun update() {
        mesh.calculateBoundingBox(bounds, offset, size)
        bounds.getCenter(center)
        bounds.getDimensions(halfExtents).scl(0.5f)
        radius = halfExtents.len()
    }

    /** Compares this MeshPart to the specified MeshPart and returns true if they both reference the same [Mesh] and the
     * [.offset], [.size] and [.primitiveType] members are equal. The [.id] member is ignored.
     * @param other The other MeshPart to compare this MeshPart to.
     * @return True when this MeshPart equals the other MeshPart (ignoring the [.id] member), false otherwise.
     */
    fun equals(other: MeshPart?): Boolean {
        return (other === this
            || other != null && other.mesh === mesh && other.primitiveType == primitiveType && other.offset == offset && other.size == size)
    }

    override fun equals(arg0: Any?): Boolean {
        if (arg0 == null) return false
        if (arg0 === this) return true
        return if (arg0 !is MeshPart) false else equals(arg0 as MeshPart?)
    }

    /** Renders the mesh part using the specified shader, must be called in between [ShaderProgram.begin] and
     * [ShaderProgram.end].
     * @param shader the shader to be used
     * @param autoBind overrides the autoBind member of the Mesh
     */
    fun render(shader: ShaderProgram?, autoBind: Boolean) {
        mesh.render(shader, primitiveType, offset, size, autoBind)
    }

    /** Renders the mesh part using the specified shader, must be called in between [ShaderProgram.begin] and
     * [ShaderProgram.end].
     * @param shader the shader to be used
     */
    fun render(shader: ShaderProgram?) {
        mesh.render(shader, primitiveType, offset, size)
    }

    companion object {
        /** Temporary static [BoundingBox] instance, used in the [.update] method.  */
        private val bounds: com.badlogic.gdx.math.collision.BoundingBox? = com.badlogic.gdx.math.collision.BoundingBox()
    }
}
