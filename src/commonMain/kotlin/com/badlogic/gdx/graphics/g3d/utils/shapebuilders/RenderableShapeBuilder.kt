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
package com.badlogic.gdx.graphics.g3d.utils.shapebuilders

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Mesh
import com.badlogic.gdx.graphics.VertexAttributes.Usage
import com.badlogic.gdx.graphics.g3d.Renderable
import com.badlogic.gdx.graphics.g3d.RenderableProvider
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.BaseShapeBuilder
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.BoxShapeBuilder
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.ConeShapeBuilder
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.CylinderShapeBuilder
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.EllipseShapeBuilder
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.FrustumShapeBuilder
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.PatchShapeBuilder
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.SphereShapeBuilder
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.FlushablePool

/**
 * RenderableShapeBuilder builds various properties of a renderable.
 *
 * @author realitix
 */
object RenderableShapeBuilder : BaseShapeBuilder {

    private var indices: ShortArray?
    private var vertices: FloatArray?
    private val renderablesPool: RenderablePool? = RenderablePool()
    private val renderables: Array<Renderable?>? = Array<Renderable?>()
    private const val FLOAT_BYTES = 4

    /**
     * Builds normal, tangent and binormal of a RenderableProvider with default colors (normal blue, tangent red, binormal green).
     *
     * @param builder
     * @param renderableProvider
     * @param vectorSize         Size of the normal vector
     */
    fun buildNormals(builder: MeshPartBuilder?, renderableProvider: RenderableProvider?, vectorSize: Float) {
        buildNormals(builder, renderableProvider, vectorSize, tmpColor0.set(0, 0, 1, 1), tmpColor1.set(1, 0, 0, 1),
            tmpColor2.set(0, 1, 0, 1))
    }

    /**
     * Builds normal, tangent and binormal of a RenderableProvider.
     *
     * @param builder
     * @param renderableProvider
     * @param vectorSize         Size of the normal vector
     * @param normalColor        Normal vector's color
     * @param tangentColor       Tangent vector's color
     * @param binormalColor      Binormal vector's color
     */
    fun buildNormals(builder: MeshPartBuilder?, renderableProvider: RenderableProvider?, vectorSize: Float,
                     normalColor: Color?, tangentColor: Color?, binormalColor: Color?) {
        renderableProvider.getRenderables(renderables, renderablesPool)
        for (renderable in renderables!!) {
            buildNormals(builder, renderable, vectorSize, normalColor, tangentColor, binormalColor)
        }
        renderablesPool.flush()
        renderables.clear()
    }

    /**
     * Builds normal, tangent and binormal of a Renderable.
     *
     * @param builder
     * @param renderable
     * @param vectorSize    Size of the normal vector
     * @param normalColor   Normal vector's color
     * @param tangentColor  Tangent vector's color
     * @param binormalColor Binormal vector's color
     */
    fun buildNormals(builder: MeshPartBuilder?, renderable: Renderable?, vectorSize: Float, normalColor: Color?,
                     tangentColor: Color?, binormalColor: Color?) {
        val mesh: Mesh = renderable.meshPart.mesh

        // Position
        var positionOffset = -1
        if (mesh.getVertexAttribute(Usage.Position) != null) positionOffset = mesh.getVertexAttribute(Usage.Position).offset / FLOAT_BYTES

        // Normal
        var normalOffset = -1
        if (mesh.getVertexAttribute(Usage.Normal) != null) normalOffset = mesh.getVertexAttribute(Usage.Normal).offset / FLOAT_BYTES

        // Tangent
        var tangentOffset = -1
        if (mesh.getVertexAttribute(Usage.Tangent) != null) tangentOffset = mesh.getVertexAttribute(Usage.Tangent).offset / FLOAT_BYTES

        // Binormal
        var binormalOffset = -1
        if (mesh.getVertexAttribute(Usage.BiNormal) != null) binormalOffset = mesh.getVertexAttribute(Usage.BiNormal).offset / FLOAT_BYTES
        val attributesSize: Int = mesh.getVertexSize() / FLOAT_BYTES
        var verticesOffset = 0
        var verticesQuantity = 0
        if (mesh.getNumIndices() > 0) {
            // Get min vertice to max vertice in indices array
            ensureIndicesCapacity(mesh.getNumIndices())
            mesh.getIndices(renderable.meshPart.offset, renderable.meshPart.size, indices, 0)
            val minVertice = minVerticeInIndices()
            val maxVertice = maxVerticeInIndices()
            verticesOffset = minVertice.toInt()
            verticesQuantity = maxVertice - minVertice
        } else {
            verticesOffset = renderable.meshPart.offset
            verticesQuantity = renderable.meshPart.size
        }
        ensureVerticesCapacity(verticesQuantity * attributesSize)
        mesh.getVertices(verticesOffset * attributesSize, verticesQuantity * attributesSize, vertices, 0)
        for (i in verticesOffset until verticesQuantity) {
            val id = i * attributesSize

            // Vertex position
            tmpV0.set(vertices!![id + positionOffset], vertices!![id + positionOffset + 1], vertices!![id + positionOffset + 2])

            // Vertex normal, tangent, binormal
            if (normalOffset != -1) {
                tmpV1.set(vertices!![id + normalOffset], vertices!![id + normalOffset + 1], vertices!![id + normalOffset + 2])
                tmpV2.set(tmpV0).add(tmpV1.scl(vectorSize))
            }
            if (tangentOffset != -1) {
                tmpV3.set(vertices!![id + tangentOffset], vertices!![id + tangentOffset + 1], vertices!![id + tangentOffset + 2])
                tmpV4.set(tmpV0).add(tmpV3.scl(vectorSize))
            }
            if (binormalOffset != -1) {
                tmpV5.set(vertices!![id + binormalOffset], vertices!![id + binormalOffset + 1], vertices!![id + binormalOffset + 2])
                tmpV6.set(tmpV0).add(tmpV5.scl(vectorSize))
            }

            // World transform
            tmpV0.mul(renderable.worldTransform)
            tmpV2.mul(renderable.worldTransform)
            tmpV4.mul(renderable.worldTransform)
            tmpV6.mul(renderable.worldTransform)

            // Draws normal, tangent, binormal
            if (normalOffset != -1) {
                builder.setColor(normalColor)
                builder.line(tmpV0, tmpV2)
            }
            if (tangentOffset != -1) {
                builder.setColor(tangentColor)
                builder.line(tmpV0, tmpV4)
            }
            if (binormalOffset != -1) {
                builder.setColor(binormalColor)
                builder.line(tmpV0, tmpV6)
            }
        }
    }

    private fun ensureVerticesCapacity(capacity: Int) {
        if (vertices == null || vertices!!.size < capacity) vertices = FloatArray(capacity)
    }

    private fun ensureIndicesCapacity(capacity: Int) {
        if (indices == null || indices!!.size < capacity) indices = ShortArray(capacity)
    }

    private fun minVerticeInIndices(): Short {
        var min = 32767.toShort()
        for (i in indices!!.indices) if (indices!![i] < min) min = indices!![i]
        return min
    }

    private fun maxVerticeInIndices(): Short {
        var max = (-32768).toShort()
        for (i in indices!!.indices) if (indices!![i] > max) max = indices!![i]
        return max
    }

    private class RenderablePool : FlushablePool<Renderable?>() {
        protected fun newObject(): Renderable? {
            return Renderable()
        }

        fun obtain(): Renderable? {
            val renderable: Renderable = super.obtain()
            renderable.environment = null
            renderable.material = null
            renderable.meshPart.set("", null, 0, 0, 0)
            renderable.shader = null
            renderable.userData = null
            return renderable
        }
    }
}
