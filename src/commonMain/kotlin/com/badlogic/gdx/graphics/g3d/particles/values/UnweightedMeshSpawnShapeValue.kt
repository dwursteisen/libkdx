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
package com.badlogic.gdx.graphics.g3d.particles.values

/** Encapsulate the formulas to spawn a particle on a mesh shape.
 * @author Inferno
 */
class UnweightedMeshSpawnShapeValue : com.badlogic.gdx.graphics.g3d.particles.values.MeshSpawnShapeValue {

    private var vertices: FloatArray
    private var indices: ShortArray?
    private var positionOffset = 0
    private var vertexSize = 0
    private var vertexCount = 0
    private var triangleCount = 0

    constructor(value: UnweightedMeshSpawnShapeValue?) : super(value) {
        load(value!!)
    }

    constructor() {}

    override fun setMesh(mesh: com.badlogic.gdx.graphics.Mesh?, model: com.badlogic.gdx.graphics.g3d.Model?) {
        super.setMesh(mesh, model)
        vertexSize = mesh.getVertexSize() / 4
        positionOffset = mesh.getVertexAttribute(com.badlogic.gdx.graphics.VertexAttributes.Usage.Position).offset / 4
        val indicesCount: Int = mesh.getNumIndices()
        if (indicesCount > 0) {
            indices = ShortArray(indicesCount)
            mesh.getIndices(indices)
            triangleCount = indices!!.size / 3
        } else indices = null
        vertexCount = mesh.getNumVertices()
        vertices = FloatArray(vertexCount * vertexSize)
        mesh.getVertices(vertices)
    }

    override fun spawnAux(vector: com.badlogic.gdx.math.Vector3, percent: Float) {
        if (indices == null) { // Triangles
            val triangleIndex: Int = com.badlogic.gdx.math.MathUtils.random(vertexCount - 3) * vertexSize
            val p1Offset = triangleIndex + positionOffset
            val p2Offset = p1Offset + vertexSize
            val p3Offset = p2Offset + vertexSize
            val x1 = vertices[p1Offset]
            val y1 = vertices[p1Offset + 1]
            val z1 = vertices[p1Offset + 2]
            val x2 = vertices[p2Offset]
            val y2 = vertices[p2Offset + 1]
            val z2 = vertices[p2Offset + 2]
            val x3 = vertices[p3Offset]
            val y3 = vertices[p3Offset + 1]
            val z3 = vertices[p3Offset + 2]
            com.badlogic.gdx.graphics.g3d.particles.values.MeshSpawnShapeValue.Triangle.Companion.pick(x1, y1, z1, x2, y2, z2, x3, y3, z3, vector)
        } else { // Indices
            val triangleIndex: Int = com.badlogic.gdx.math.MathUtils.random(triangleCount - 1) * 3
            val p1Offset = indices!![triangleIndex] * vertexSize + positionOffset
            val p2Offset = (indices!![triangleIndex + 1] * vertexSize
                + positionOffset)
            val p3Offset = indices!![triangleIndex + 2] * vertexSize + positionOffset
            val x1 = vertices[p1Offset]
            val y1 = vertices[p1Offset + 1]
            val z1 = vertices[p1Offset + 2]
            val x2 = vertices[p2Offset]
            val y2 = vertices[p2Offset + 1]
            val z2 = vertices[p2Offset + 2]
            val x3 = vertices[p3Offset]
            val y3 = vertices[p3Offset + 1]
            val z3 = vertices[p3Offset + 2]
            com.badlogic.gdx.graphics.g3d.particles.values.MeshSpawnShapeValue.Triangle.Companion.pick(x1, y1, z1, x2, y2, z2, x3, y3, z3, vector)
        }
    }

    override fun copy(): com.badlogic.gdx.graphics.g3d.particles.values.SpawnShapeValue {
        return UnweightedMeshSpawnShapeValue(this)
    }
}
