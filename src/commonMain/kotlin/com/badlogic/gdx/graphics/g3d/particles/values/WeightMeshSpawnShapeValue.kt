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

/** Encapsulate the formulas to spawn a particle on a mesh shape dealing with not uniform area triangles.
 * @author Inferno
 */
class WeightMeshSpawnShapeValue : com.badlogic.gdx.graphics.g3d.particles.values.MeshSpawnShapeValue {

    private var distribution: com.badlogic.gdx.math.CumulativeDistribution<com.badlogic.gdx.graphics.g3d.particles.values.MeshSpawnShapeValue.Triangle>

    constructor(value: WeightMeshSpawnShapeValue?) : super(value) {
        distribution = com.badlogic.gdx.math.CumulativeDistribution()
        load(value!!)
    }

    constructor() : super() {
        distribution = com.badlogic.gdx.math.CumulativeDistribution()
    }

    override fun init() {
        calculateWeights()
    }

    /** Calculate the weights of each triangle of the wrapped mesh. If the mesh has indices: the function will calculate the weight
     * of those triangles. If the mesh has not indices: the function will consider the vertices as a triangle strip.  */
    fun calculateWeights() {
        distribution.clear()
        val attributes: com.badlogic.gdx.graphics.VertexAttributes = mesh.getVertexAttributes()
        val indicesCount: Int = mesh.getNumIndices()
        val vertexCount: Int = mesh.getNumVertices()
        val vertexSize: Int = (attributes.vertexSize / 4) as Short.toInt()
        val positionOffset: Int = (attributes.findByUsage(com.badlogic.gdx.graphics.VertexAttributes.Usage.Position).offset / 4) as Short.toInt()
        val vertices = FloatArray(vertexCount * vertexSize)
        mesh.getVertices(vertices)
        if (indicesCount > 0) {
            val indices = ShortArray(indicesCount)
            mesh.getIndices(indices)
            // Calculate the Area
            var i = 0
            while (i < indicesCount) {
                val p1Offset = indices[i] * vertexSize + positionOffset
                val p2Offset = indices[i + 1] * vertexSize + positionOffset
                val p3Offset = indices[i + 2].toInt()
                * vertexSize + positionOffset
                val x1 = vertices[p1Offset]
                val y1 = vertices[p1Offset + 1]
                val z1 = vertices[p1Offset + 2]
                val x2 = vertices[p2Offset]
                val y2 = vertices[p2Offset + 1]
                val z2 = vertices[p2Offset + 2]
                val x3 = vertices[p3Offset]
                val y3 = vertices[p3Offset + 1]
                val z3 = vertices[p3Offset + 2]
                val area: Float = java.lang.Math.abs((x1 * (y2 - y3) + x2 * (y3 - y1) + x3 * (y1 - y2)) / 2f)
                distribution.add(com.badlogic.gdx.graphics.g3d.particles.values.MeshSpawnShapeValue.Triangle(x1, y1, z1, x2, y2, z2, x3, y3, z3), area)
                i += 3
            }
        } else { // Calculate the Area
            var i = 0
            while (i < vertexCount) {
                val p1Offset = i + positionOffset
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
                val area: Float = java.lang.Math.abs((x1 * (y2 - y3) + x2 * (y3 - y1) + x3 * (y1 - y2)) / 2f)
                distribution.add(com.badlogic.gdx.graphics.g3d.particles.values.MeshSpawnShapeValue.Triangle(x1, y1, z1, x2, y2, z2, x3, y3, z3), area)
                i += vertexSize
            }
        }
        // Generate cumulative distribution
        distribution.generateNormalized()
    }

    override fun spawnAux(vector: com.badlogic.gdx.math.Vector3, percent: Float) {
        val t: com.badlogic.gdx.graphics.g3d.particles.values.MeshSpawnShapeValue.Triangle = distribution.value()
        val a: Float = com.badlogic.gdx.math.MathUtils.random()
        val b: Float = com.badlogic.gdx.math.MathUtils.random()
        vector.set(t.x1 + a * (t.x2 - t.x1) + b * (t.x3 - t.x1), t.y1 + a * (t.y2 - t.y1) + b * (t.y3 - t.y1), t.z1 + (a
            * (t.z2 - t.z1)) + b * (t.z3 - t.z1))
    }

    override fun copy(): com.badlogic.gdx.graphics.g3d.particles.values.SpawnShapeValue {
        return WeightMeshSpawnShapeValue(this)
    }
}
