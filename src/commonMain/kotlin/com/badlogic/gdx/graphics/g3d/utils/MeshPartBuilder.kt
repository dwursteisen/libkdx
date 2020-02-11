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
package com.badlogic.gdx.graphics.g3d.utils

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.g3d.model.MeshPart
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3

interface MeshPartBuilder {
    /**
     * @return The [MeshPart] currently building.
     */
    val meshPart: MeshPart?

    /**
     * @return The primitive type used for building, e.g. [GL20.GL_TRIANGLES] or [GL20.GL_LINES].
     */
    val primitiveType: Int

    /**
     * @return The [VertexAttributes] available for building.
     */
    val attributes: VertexAttributes?

    /**
     * Set the color used to tint the vertex color, defaults to white. Only applicable for [Usage.ColorPacked] or
     * [Usage.ColorUnpacked].
     */
    fun setColor(color: Color?)

    /**
     * Set the color used to tint the vertex color, defaults to white. Only applicable for [Usage.ColorPacked] or
     * [Usage.ColorUnpacked].
     */
    fun setColor(r: Float, g: Float, b: Float, a: Float)

    /**
     * Set range of texture coordinates used (default is 0,0,1,1).
     */
    fun setUVRange(u1: Float, v1: Float, u2: Float, v2: Float)

    /**
     * Set range of texture coordinates from the specified TextureRegion.
     */
    fun setUVRange(r: TextureRegion?)

    /**
     * Get the current vertex transformation matrix.
     */
    fun getVertexTransform(out: Matrix4?): Matrix4?

    /**
     * Set the current vertex transformation matrix and enables vertex transformation.
     */
    fun setVertexTransform(transform: Matrix4?)

    /**
     * Indicates whether vertex transformation is enabled.
     */
    /**
     * Sets whether vertex transformation is enabled.
     */
    var isVertexTransformationEnabled: Boolean

    /**
     * Increases the size of the backing vertices array to accommodate the specified number of additional vertices. Useful before
     * adding many vertices to avoid multiple backing array resizes.
     *
     * @param numVertices The number of vertices you are about to add
     */
    fun ensureVertices(numVertices: Int)

    /**
     * Increases the size of the backing indices array to accommodate the specified number of additional indices. Useful before
     * adding many indices to avoid multiple backing array resizes.
     *
     * @param numIndices The number of indices you are about to add
     */
    fun ensureIndices(numIndices: Int)

    /**
     * Increases the size of the backing vertices and indices arrays to accommodate the specified number of additional vertices and
     * indices. Useful before adding many vertices and indices to avoid multiple backing array resizes.
     *
     * @param numVertices The number of vertices you are about to add
     * @param numIndices  The number of indices you are about to add
     */
    fun ensureCapacity(numVertices: Int, numIndices: Int)

    /**
     * Increases the size of the backing indices array to accommodate the specified number of additional triangles. Useful before
     * adding many triangles using [.triangle] to avoid multiple backing array resizes. The actual
     * number of indices accounted for depends on the primitive type (see [.getPrimitiveType]).
     *
     * @param numTriangles The number of triangles you are about to add
     */
    fun ensureTriangleIndices(numTriangles: Int)

    /**
     * Increases the size of the backing indices array to accommodate the specified number of additional rectangles. Useful before
     * adding many rectangles using [.rect] to avoid multiple backing array resizes.
     *
     * @param numRectangles The number of rectangles you are about to add
     */
    fun ensureRectangleIndices(numRectangles: Int)

    /**
     * Add one or more vertices, returns the index of the last vertex added. The length of values must a power of the vertex size.
     */
    fun vertex(vararg values: Float): Short

    /**
     * Add a vertex, returns the index. Null values are allowed. Use [.getAttributes] to check which values are available.
     */
    fun vertex(pos: Vector3?, nor: Vector3?, col: Color?, uv: Vector2?): Short

    /**
     * Add a vertex, returns the index. Use [.getAttributes] to check which values are available.
     */
    fun vertex(info: VertexInfo?): Short

    /**
     * @return The index of the last added vertex.
     */
    fun lastIndex(): Short

    /**
     * Add an index, MeshPartBuilder expects all meshes to be indexed.
     */
    fun index(value: Short)

    /**
     * Add multiple indices, MeshPartBuilder expects all meshes to be indexed.
     */
    fun index(value1: Short, value2: Short)

    /**
     * Add multiple indices, MeshPartBuilder expects all meshes to be indexed.
     */
    fun index(value1: Short, value2: Short, value3: Short)

    /**
     * Add multiple indices, MeshPartBuilder expects all meshes to be indexed.
     */
    fun index(value1: Short, value2: Short, value3: Short, value4: Short)

    /**
     * Add multiple indices, MeshPartBuilder expects all meshes to be indexed.
     */
    fun index(value1: Short, value2: Short, value3: Short, value4: Short, value5: Short, value6: Short)

    /**
     * Add multiple indices, MeshPartBuilder expects all meshes to be indexed.
     */
    fun index(value1: Short, value2: Short, value3: Short, value4: Short, value5: Short, value6: Short, value7: Short,
              value8: Short)

    /**
     * Add a line by indices. Requires GL_LINES primitive type.
     */
    fun line(index1: Short, index2: Short)

    /**
     * Add a line. Requires GL_LINES primitive type.
     */
    fun line(p1: VertexInfo?, p2: VertexInfo?)

    /**
     * Add a line. Requires GL_LINES primitive type.
     */
    fun line(p1: Vector3?, p2: Vector3?)

    /**
     * Add a line. Requires GL_LINES primitive type.
     */
    fun line(x1: Float, y1: Float, z1: Float, x2: Float, y2: Float, z2: Float)

    /**
     * Add a line. Requires GL_LINES primitive type.
     */
    fun line(p1: Vector3?, c1: Color?, p2: Vector3?, c2: Color?)

    /**
     * Add a triangle by indices. Requires GL_POINTS, GL_LINES or GL_TRIANGLES primitive type.
     */
    fun triangle(index1: Short, index2: Short, index3: Short)

    /**
     * Add a triangle. Requires GL_POINTS, GL_LINES or GL_TRIANGLES primitive type.
     */
    fun triangle(p1: VertexInfo?, p2: VertexInfo?, p3: VertexInfo?)

    /**
     * Add a triangle. Requires GL_POINTS, GL_LINES or GL_TRIANGLES primitive type.
     */
    fun triangle(p1: Vector3?, p2: Vector3?, p3: Vector3?)

    /**
     * Add a triangle. Requires GL_POINTS, GL_LINES or GL_TRIANGLES primitive type.
     */
    fun triangle(p1: Vector3?, c1: Color?, p2: Vector3?, c2: Color?, p3: Vector3?, c3: Color?)

    /**
     * Add a rectangle by indices. Requires GL_POINTS, GL_LINES or GL_TRIANGLES primitive type.
     */
    fun rect(corner00: Short, corner10: Short, corner11: Short, corner01: Short)

    /**
     * Add a rectangle. Requires GL_POINTS, GL_LINES or GL_TRIANGLES primitive type.
     */
    fun rect(corner00: VertexInfo?, corner10: VertexInfo?, corner11: VertexInfo?, corner01: VertexInfo?)

    /**
     * Add a rectangle. Requires GL_POINTS, GL_LINES or GL_TRIANGLES primitive type.
     */
    fun rect(corner00: Vector3?, corner10: Vector3?, corner11: Vector3?, corner01: Vector3?, normal: Vector3?)

    /**
     * Add a rectangle Requires GL_POINTS, GL_LINES or GL_TRIANGLES primitive type.
     */
    fun rect(x00: Float, y00: Float, z00: Float, x10: Float, y10: Float, z10: Float, x11: Float, y11: Float, z11: Float,
             x01: Float, y01: Float, z01: Float, normalX: Float, normalY: Float, normalZ: Float)

    /**
     * Copies a mesh to the mesh (part) currently being build.
     *
     * @param mesh The mesh to copy, must have the same vertex attributes and must be indexed.
     */
    fun addMesh(mesh: Mesh?)

    /**
     * Copies a MeshPart to the mesh (part) currently being build.
     *
     * @param meshpart The MeshPart to copy, must have the same vertex attributes, primitive type and must be indexed.
     */
    fun addMesh(meshpart: MeshPart?)

    /**
     * Copies a (part of a) mesh to the mesh (part) currently being build.
     *
     * @param mesh        The mesh to (partly) copy, must have the same vertex attributes and must be indexed.
     * @param indexOffset The zero-based offset of the first index of the part of the mesh to copy.
     * @param numIndices  The number of indices of the part of the mesh to copy.
     */
    fun addMesh(mesh: Mesh?, indexOffset: Int, numIndices: Int)

    /**
     * Copies a mesh to the mesh (part) currently being build. The entire vertices array is added, even if some of the vertices are
     * not indexed by the indices array. If you want to add only the vertices that are actually indexed, then use the
     * [.addMesh] method instead.
     *
     * @param vertices The vertices to copy, must be in the same vertex layout as the mesh being build.
     * @param indices  Array containing the indices to copy, each index should be valid in the vertices array.
     */
    fun addMesh(vertices: FloatArray?, indices: ShortArray?)

    /**
     * Copies a (part of a) mesh to the mesh (part) currently being build.
     *
     * @param vertices    The vertices to (partly) copy, must be in the same vertex layout as the mesh being build.
     * @param indices     Array containing the indices to (partly) copy, each index should be valid in the vertices array.
     * @param indexOffset The zero-based offset of the first index of the part of indices array to copy.
     * @param numIndices  The number of indices of the part of the indices array to copy.
     */
    fun addMesh(vertices: FloatArray?, indices: ShortArray?, indexOffset: Int, numIndices: Int)

    /**
     * Class that contains all vertex information the builder can use.
     *
     * @author Xoppa
     */
    class VertexInfo : Poolable {

        val position: Vector3 = Vector3()
        var hasPosition = false
        val normal: Vector3 = Vector3(0, 1, 0)
        var hasNormal = false
        val color: Color = Color(1, 1, 1, 1)
        var hasColor = false
        val uv: Vector2 = Vector2()
        var hasUV = false
        fun reset() {
            position.set(0, 0, 0)
            normal.set(0, 1, 0)
            color.set(1, 1, 1, 1)
            uv.set(0, 0)
        }

        operator fun set(pos: Vector3?, nor: Vector3?, col: Color?, uv: Vector2?): VertexInfo {
            reset()
            hasPosition = pos != null
            if (hasPosition) position.set(pos)
            hasNormal = nor != null
            if (hasNormal) normal.set(nor)
            hasColor = col != null
            if (hasColor) color.set(col)
            hasUV = uv != null
            if (hasUV) this.uv.set(uv)
            return this
        }

        fun set(other: VertexInfo?): VertexInfo {
            if (other == null) return set(null, null, null, null)
            hasPosition = other.hasPosition
            position.set(other.position)
            hasNormal = other.hasNormal
            normal.set(other.normal)
            hasColor = other.hasColor
            color.set(other.color)
            hasUV = other.hasUV
            uv.set(other.uv)
            return this
        }

        fun setPos(x: Float, y: Float, z: Float): VertexInfo {
            position.set(x, y, z)
            hasPosition = true
            return this
        }

        fun setPos(pos: Vector3?): VertexInfo {
            hasPosition = pos != null
            if (hasPosition) position.set(pos)
            return this
        }

        fun setNor(x: Float, y: Float, z: Float): VertexInfo {
            normal.set(x, y, z)
            hasNormal = true
            return this
        }

        fun setNor(nor: Vector3?): VertexInfo {
            hasNormal = nor != null
            if (hasNormal) normal.set(nor)
            return this
        }

        fun setCol(r: Float, g: Float, b: Float, a: Float): VertexInfo {
            color.set(r, g, b, a)
            hasColor = true
            return this
        }

        fun setCol(col: Color?): VertexInfo {
            hasColor = col != null
            if (hasColor) color.set(col)
            return this
        }

        fun setUV(u: Float, v: Float): VertexInfo {
            uv.set(u, v)
            hasUV = true
            return this
        }

        fun setUV(uv: Vector2?): VertexInfo {
            hasUV = uv != null
            if (hasUV) this.uv.set(uv)
            return this
        }

        fun lerp(target: VertexInfo, alpha: Float): VertexInfo {
            if (hasPosition && target.hasPosition) position.lerp(target.position, alpha)
            if (hasNormal && target.hasNormal) normal.lerp(target.normal, alpha)
            if (hasColor && target.hasColor) color.lerp(target.color, alpha)
            if (hasUV && target.hasUV) uv.lerp(target.uv, alpha)
            return this
        }
    }
    // TODO: The following methods are deprecated and will be removed in a future release

    @Deprecated("use PatchShapeBuilder.build instead.")
    fun patch(corner00: VertexInfo?, corner10: VertexInfo?, corner11: VertexInfo?, corner01: VertexInfo?, divisionsU: Int,
              divisionsV: Int)

    @Deprecated("use PatchShapeBuilder.build instead.")
    fun patch(corner00: Vector3?, corner10: Vector3?, corner11: Vector3?, corner01: Vector3?, normal: Vector3?, divisionsU: Int,
              divisionsV: Int)

    @Deprecated("use PatchShapeBuilder.build instead.")
    fun patch(x00: Float, y00: Float, z00: Float, x10: Float, y10: Float, z10: Float, x11: Float, y11: Float, z11: Float,
              x01: Float, y01: Float, z01: Float, normalX: Float, normalY: Float, normalZ: Float, divisionsU: Int, divisionsV: Int)

    @Deprecated("use BoxShapeBuilder.build instead.")
    fun box(corner000: VertexInfo?, corner010: VertexInfo?, corner100: VertexInfo?, corner110: VertexInfo?, corner001: VertexInfo?,
            corner011: VertexInfo?, corner101: VertexInfo?, corner111: VertexInfo?)

    @Deprecated("use BoxShapeBuilder.build instead.")
    fun box(corner000: Vector3?, corner010: Vector3?, corner100: Vector3?, corner110: Vector3?, corner001: Vector3?,
            corner011: Vector3?, corner101: Vector3?, corner111: Vector3?)

    @Deprecated("use BoxShapeBuilder.build instead.")
    fun box(transform: Matrix4?)

    @Deprecated("use BoxShapeBuilder.build instead.")
    fun box(width: Float, height: Float, depth: Float)

    @Deprecated("use BoxShapeBuilder.build instead.")
    fun box(x: Float, y: Float, z: Float, width: Float, height: Float, depth: Float)

    @Deprecated("Use EllipseShapeBuilder.build instead.")
    fun circle(radius: Float, divisions: Int, centerX: Float, centerY: Float, centerZ: Float, normalX: Float, normalY: Float,
               normalZ: Float)

    @Deprecated("Use EllipseShapeBuilder.build instead.")
    fun circle(radius: Float, divisions: Int, center: Vector3?, normal: Vector3?)

    @Deprecated("Use EllipseShapeBuilder.build instead.")
    fun circle(radius: Float, divisions: Int, center: Vector3?, normal: Vector3?, tangent: Vector3?,
               binormal: Vector3?)

    @Deprecated("Use EllipseShapeBuilder.build instead.")
    fun circle(radius: Float, divisions: Int, centerX: Float, centerY: Float, centerZ: Float, normalX: Float, normalY: Float,
               normalZ: Float, tangentX: Float, tangentY: Float, tangentZ: Float, binormalX: Float, binormalY: Float, binormalZ: Float)

    @Deprecated("Use EllipseShapeBuilder.build instead.")
    fun circle(radius: Float, divisions: Int, centerX: Float, centerY: Float, centerZ: Float, normalX: Float, normalY: Float,
               normalZ: Float, angleFrom: Float, angleTo: Float)

    @Deprecated("Use EllipseShapeBuilder.build instead.")
    fun circle(radius: Float, divisions: Int, center: Vector3?, normal: Vector3?, angleFrom: Float, angleTo: Float)

    @Deprecated("Use EllipseShapeBuilder.build instead.")
    fun circle(radius: Float, divisions: Int, center: Vector3?, normal: Vector3?, tangent: Vector3?,
               binormal: Vector3?, angleFrom: Float, angleTo: Float)

    @Deprecated("Use EllipseShapeBuilder.build instead.")
    fun circle(radius: Float, divisions: Int, centerX: Float, centerY: Float, centerZ: Float, normalX: Float, normalY: Float,
               normalZ: Float, tangentX: Float, tangentY: Float, tangentZ: Float, binormalX: Float, binormalY: Float, binormalZ: Float,
               angleFrom: Float, angleTo: Float)

    @Deprecated("Use EllipseShapeBuilder.build instead.")
    fun ellipse(width: Float, height: Float, divisions: Int, centerX: Float, centerY: Float, centerZ: Float, normalX: Float,
                normalY: Float, normalZ: Float)

    @Deprecated("Use EllipseShapeBuilder.build instead.")
    fun ellipse(width: Float, height: Float, divisions: Int, center: Vector3?, normal: Vector3?)

    @Deprecated("Use EllipseShapeBuilder.build instead.")
    fun ellipse(width: Float, height: Float, divisions: Int, center: Vector3?, normal: Vector3?,
                tangent: Vector3?, binormal: Vector3?)

    @Deprecated("Use EllipseShapeBuilder.build instead.")
    fun ellipse(width: Float, height: Float, divisions: Int, centerX: Float, centerY: Float, centerZ: Float, normalX: Float,
                normalY: Float, normalZ: Float, tangentX: Float, tangentY: Float, tangentZ: Float, binormalX: Float, binormalY: Float,
                binormalZ: Float)

    @Deprecated("Use EllipseShapeBuilder.build instead.")
    fun ellipse(width: Float, height: Float, divisions: Int, centerX: Float, centerY: Float, centerZ: Float, normalX: Float,
                normalY: Float, normalZ: Float, angleFrom: Float, angleTo: Float)

    @Deprecated("Use EllipseShapeBuilder.build instead.")
    fun ellipse(width: Float, height: Float, divisions: Int, center: Vector3?, normal: Vector3?, angleFrom: Float,
                angleTo: Float)

    @Deprecated("Use EllipseShapeBuilder.build instead.")
    fun ellipse(width: Float, height: Float, divisions: Int, center: Vector3?, normal: Vector3?,
                tangent: Vector3?, binormal: Vector3?, angleFrom: Float, angleTo: Float)

    @Deprecated("Use EllipseShapeBuilder.build instead.")
    fun ellipse(width: Float, height: Float, divisions: Int, centerX: Float, centerY: Float, centerZ: Float, normalX: Float,
                normalY: Float, normalZ: Float, tangentX: Float, tangentY: Float, tangentZ: Float, binormalX: Float, binormalY: Float,
                binormalZ: Float, angleFrom: Float, angleTo: Float)

    @Deprecated("Use EllipseShapeBuilder.build instead.")
    fun ellipse(width: Float, height: Float, innerWidth: Float, innerHeight: Float, divisions: Int, centerX: Float,
                centerY: Float, centerZ: Float, normalX: Float, normalY: Float, normalZ: Float, tangentX: Float, tangentY: Float, tangentZ: Float,
                binormalX: Float, binormalY: Float, binormalZ: Float, angleFrom: Float, angleTo: Float)

    @Deprecated("Use EllipseShapeBuilder.build instead.")
    fun ellipse(width: Float, height: Float, innerWidth: Float, innerHeight: Float, divisions: Int, centerX: Float,
                centerY: Float, centerZ: Float, normalX: Float, normalY: Float, normalZ: Float, angleFrom: Float, angleTo: Float)

    @Deprecated("Use EllipseShapeBuilder.build instead.")
    fun ellipse(width: Float, height: Float, innerWidth: Float, innerHeight: Float, divisions: Int, centerX: Float,
                centerY: Float, centerZ: Float, normalX: Float, normalY: Float, normalZ: Float)

    @Deprecated("Use EllipseShapeBuilder.build instead.")
    fun ellipse(width: Float, height: Float, innerWidth: Float, innerHeight: Float, divisions: Int, center: Vector3?,
                normal: Vector3?)

    @Deprecated("Use CylinderShapeBuilder.build instead.")
    fun cylinder(width: Float, height: Float, depth: Float, divisions: Int)

    @Deprecated("Use CylinderShapeBuilder.build instead.")
    fun cylinder(width: Float, height: Float, depth: Float, divisions: Int, angleFrom: Float, angleTo: Float)

    @Deprecated("Use CylinderShapeBuilder.build instead.")
    fun cylinder(width: Float, height: Float, depth: Float, divisions: Int, angleFrom: Float, angleTo: Float, close: Boolean)

    @Deprecated("Use ConeShapeBuilder.build instead.")
    fun cone(width: Float, height: Float, depth: Float, divisions: Int)

    @Deprecated("Use ConeShapeBuilder.build instead.")
    fun cone(width: Float, height: Float, depth: Float, divisions: Int, angleFrom: Float, angleTo: Float)

    @Deprecated("Use SphereShapeBuilder.build instead.")
    fun sphere(width: Float, height: Float, depth: Float, divisionsU: Int, divisionsV: Int)

    @Deprecated("Use SphereShapeBuilder.build instead.")
    fun sphere(transform: Matrix4?, width: Float, height: Float, depth: Float, divisionsU: Int, divisionsV: Int)

    @Deprecated("Use SphereShapeBuilder.build instead.")
    fun sphere(width: Float, height: Float, depth: Float, divisionsU: Int, divisionsV: Int, angleUFrom: Float, angleUTo: Float,
               angleVFrom: Float, angleVTo: Float)

    @Deprecated("Use SphereShapeBuilder.build instead.")
    fun sphere(transform: Matrix4?, width: Float, height: Float, depth: Float, divisionsU: Int, divisionsV: Int,
               angleUFrom: Float, angleUTo: Float, angleVFrom: Float, angleVTo: Float)

    @Deprecated("Use CapsuleShapeBuilder.build instead.")
    fun capsule(radius: Float, height: Float, divisions: Int)

    @Deprecated("Use ArrowShapeBuilder.build instead.")
    fun arrow(x1: Float, y1: Float, z1: Float, x2: Float, y2: Float, z2: Float, capLength: Float, stemThickness: Float,
              divisions: Int)
}
