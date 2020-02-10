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
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.VertexAttributes.Usage
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder.VertexInfo
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.BaseShapeBuilder
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.ConeShapeBuilder
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.CylinderShapeBuilder
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.EllipseShapeBuilder
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.FrustumShapeBuilder
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.PatchShapeBuilder
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.RenderableShapeBuilder
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.SphereShapeBuilder
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.collision.BoundingBox

/**
 * Helper class with static methods to build box shapes using [MeshPartBuilder].
 *
 * @author realitix, xoppa
 */
object BoxShapeBuilder : BaseShapeBuilder {

    /**
     * Build a box with the shape of the specified [BoundingBox].
     *
     * @param box
     */
    fun build(builder: MeshPartBuilder?, box: BoundingBox?) {
        builder.box(box.getCorner000(obtainV3()), box.getCorner010(obtainV3()), box.getCorner100(obtainV3()), box.getCorner110(obtainV3()),
            box.getCorner001(obtainV3()), box.getCorner011(obtainV3()), box.getCorner101(obtainV3()), box.getCorner111(obtainV3()))
        freeAll()
    }

    /**
     * Add a box. Requires GL_POINTS, GL_LINES or GL_TRIANGLES primitive type.
     */
    fun build(builder: MeshPartBuilder?, corner000: VertexInfo?, corner010: VertexInfo?, corner100: VertexInfo?,
              corner110: VertexInfo?, corner001: VertexInfo?, corner011: VertexInfo?, corner101: VertexInfo?, corner111: VertexInfo?) {
        builder.ensureVertices(8)
        val i000: Short = builder.vertex(corner000)
        val i100: Short = builder.vertex(corner100)
        val i110: Short = builder.vertex(corner110)
        val i010: Short = builder.vertex(corner010)
        val i001: Short = builder.vertex(corner001)
        val i101: Short = builder.vertex(corner101)
        val i111: Short = builder.vertex(corner111)
        val i011: Short = builder.vertex(corner011)
        val primitiveType: Int = builder.getPrimitiveType()
        if (primitiveType == GL20.GL_LINES) {
            builder.ensureIndices(24)
            builder.rect(i000, i100, i110, i010)
            builder.rect(i101, i001, i011, i111)
            builder.index(i000, i001, i010, i011, i110, i111, i100, i101)
        } else if (primitiveType == GL20.GL_POINTS) {
            builder.ensureRectangleIndices(2)
            builder.rect(i000, i100, i110, i010)
            builder.rect(i101, i001, i011, i111)
        } else { // GL20.GL_TRIANGLES
            builder.ensureRectangleIndices(6)
            builder.rect(i000, i100, i110, i010)
            builder.rect(i101, i001, i011, i111)
            builder.rect(i000, i010, i011, i001)
            builder.rect(i101, i111, i110, i100)
            builder.rect(i101, i100, i000, i001)
            builder.rect(i110, i111, i011, i010)
        }
    }

    /**
     * Add a box. Requires GL_POINTS, GL_LINES or GL_TRIANGLES primitive type.
     */
    fun build(builder: MeshPartBuilder?, corner000: Vector3?, corner010: Vector3?, corner100: Vector3?, corner110: Vector3?,
              corner001: Vector3?, corner011: Vector3?, corner101: Vector3?, corner111: Vector3?) {
        if (builder.getAttributes().getMask() and (Usage.Normal or Usage.BiNormal or Usage.Tangent or Usage.TextureCoordinates) === 0) {
            build(builder, vertTmp1.set(corner000, null, null, null), vertTmp2.set(corner010, null, null, null),
                vertTmp3.set(corner100, null, null, null), vertTmp4.set(corner110, null, null, null),
                vertTmp5.set(corner001, null, null, null), vertTmp6.set(corner011, null, null, null),
                vertTmp7.set(corner101, null, null, null), vertTmp8.set(corner111, null, null, null))
        } else {
            builder.ensureVertices(24)
            builder.ensureRectangleIndices(6)
            var nor: Vector3 = tmpV1.set(corner000).lerp(corner110, 0.5f).sub(tmpV2.set(corner001).lerp(corner111, 0.5f)).nor()
            builder.rect(corner000, corner010, corner110, corner100, nor)
            builder.rect(corner011, corner001, corner101, corner111, nor.scl(-1f))
            nor = tmpV1.set(corner000).lerp(corner101, 0.5f).sub(tmpV2.set(corner010).lerp(corner111, 0.5f)).nor()
            builder.rect(corner001, corner000, corner100, corner101, nor)
            builder.rect(corner010, corner011, corner111, corner110, nor.scl(-1f))
            nor = tmpV1.set(corner000).lerp(corner011, 0.5f).sub(tmpV2.set(corner100).lerp(corner111, 0.5f)).nor()
            builder.rect(corner001, corner011, corner010, corner000, nor)
            builder.rect(corner100, corner110, corner111, corner101, nor.scl(-1f))
        }
    }

    /**
     * Add a box given the matrix. Requires GL_POINTS, GL_LINES or GL_TRIANGLES primitive type.
     */
    fun build(builder: MeshPartBuilder?, transform: Matrix4?) {
        build(builder, obtainV3().set(-0.5f, -0.5f, -0.5f).mul(transform), obtainV3().set(-0.5f, 0.5f, -0.5f).mul(transform),
            obtainV3().set(0.5f, -0.5f, -0.5f).mul(transform), obtainV3().set(0.5f, 0.5f, -0.5f).mul(transform),
            obtainV3().set(-0.5f, -0.5f, 0.5f).mul(transform), obtainV3().set(-0.5f, 0.5f, 0.5f).mul(transform),
            obtainV3().set(0.5f, -0.5f, 0.5f).mul(transform), obtainV3().set(0.5f, 0.5f, 0.5f).mul(transform))
        freeAll()
    }

    /**
     * Add a box with the specified dimensions. Requires GL_POINTS, GL_LINES or GL_TRIANGLES primitive type.
     */
    fun build(builder: MeshPartBuilder?, width: Float, height: Float, depth: Float) {
        build(builder, 0f, 0f, 0f, width, height, depth)
    }

    /**
     * Add a box at the specified location, with the specified dimensions
     */
    fun build(builder: MeshPartBuilder?, x: Float, y: Float, z: Float, width: Float, height: Float, depth: Float) {
        val hw = width * 0.5f
        val hh = height * 0.5f
        val hd = depth * 0.5f
        val x0 = x - hw
        val y0 = y - hh
        val z0 = z - hd
        val x1 = x + hw
        val y1 = y + hh
        val z1 = z + hd
        build(builder,  //
            obtainV3().set(x0, y0, z0), obtainV3().set(x0, y1, z0), obtainV3().set(x1, y0, z0), obtainV3().set(x1, y1, z0),  //
            obtainV3().set(x0, y0, z1), obtainV3().set(x0, y1, z1), obtainV3().set(x1, y0, z1), obtainV3().set(x1, y1, z1))
        freeAll()
    }
}
