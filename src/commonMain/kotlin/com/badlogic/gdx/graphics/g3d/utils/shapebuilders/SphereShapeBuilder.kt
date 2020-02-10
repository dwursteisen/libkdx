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

import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder.VertexInfo
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.BaseShapeBuilder
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.BoxShapeBuilder
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.ConeShapeBuilder
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.CylinderShapeBuilder
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.EllipseShapeBuilder
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.FrustumShapeBuilder
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.PatchShapeBuilder
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.RenderableShapeBuilder
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Matrix3
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.utils.ShortArray

/**
 * Helper class with static methods to build sphere shapes using [MeshPartBuilder].
 *
 * @author xoppa
 */
object SphereShapeBuilder : BaseShapeBuilder {

    private val tmpIndices: ShortArray? = ShortArray()
    private val normalTransform: Matrix3? = Matrix3()
    fun build(builder: MeshPartBuilder?, width: Float, height: Float, depth: Float, divisionsU: Int, divisionsV: Int) {
        build(builder, width, height, depth, divisionsU, divisionsV, 0f, 360f, 0f, 180f)
    }

    @Deprecated("use {@link MeshPartBuilder#setVertexTransform(Matrix4)} instead of using the method signature taking a matrix.")
    fun build(builder: MeshPartBuilder?, transform: Matrix4?, width: Float, height: Float, depth: Float,
              divisionsU: Int, divisionsV: Int) {
        build(builder, transform, width, height, depth, divisionsU, divisionsV, 0f, 360f, 0f, 180f)
    }

    fun build(builder: MeshPartBuilder?, width: Float, height: Float, depth: Float, divisionsU: Int, divisionsV: Int,
              angleUFrom: Float, angleUTo: Float, angleVFrom: Float, angleVTo: Float) {
        build(builder, matTmp1.idt(), width, height, depth, divisionsU, divisionsV, angleUFrom, angleUTo, angleVFrom, angleVTo)
    }

    @Deprecated("use {@link MeshPartBuilder#setVertexTransform(Matrix4)} instead of using the method signature taking a matrix.")
    fun build(builder: MeshPartBuilder?, transform: Matrix4?, width: Float, height: Float, depth: Float,
              divisionsU: Int, divisionsV: Int, angleUFrom: Float, angleUTo: Float, angleVFrom: Float, angleVTo: Float) {
        // FIXME create better sphere method (- only one vertex for each pole, - position)
        val hw = width * 0.5f
        val hh = height * 0.5f
        val hd = depth * 0.5f
        val auo: Float = MathUtils.degreesToRadians * angleUFrom
        val stepU: Float = MathUtils.degreesToRadians * (angleUTo - angleUFrom) / divisionsU
        val avo: Float = MathUtils.degreesToRadians * angleVFrom
        val stepV: Float = MathUtils.degreesToRadians * (angleVTo - angleVFrom) / divisionsV
        val us = 1f / divisionsU
        val vs = 1f / divisionsV
        var u = 0f
        var v = 0f
        var angleU = 0f
        var angleV = 0f
        val curr1: VertexInfo = vertTmp3.set(null, null, null, null)
        curr1.hasNormal = true
        curr1.hasPosition = curr1.hasNormal
        curr1.hasUV = curr1.hasPosition
        normalTransform.set(transform)
        val s = divisionsU + 3
        tmpIndices.clear()
        tmpIndices.ensureCapacity(divisionsU * 2)
        tmpIndices!!.size = s
        var tempOffset = 0
        builder.ensureVertices((divisionsV + 1) * (divisionsU + 1))
        builder.ensureRectangleIndices(divisionsU)
        for (iv in 0..divisionsV) {
            angleV = avo + stepV * iv
            v = vs * iv
            val t: Float = MathUtils.sin(angleV)
            val h: Float = MathUtils.cos(angleV) * hh
            for (iu in 0..divisionsU) {
                angleU = auo + stepU * iu
                u = 1f - us * iu
                curr1.position.set(MathUtils.cos(angleU) * hw * t, h, MathUtils.sin(angleU) * hd * t)
                curr1.normal.set(curr1.position).mul(normalTransform).nor()
                curr1.position.mul(transform)
                curr1.uv.set(u, v)
                tmpIndices[tempOffset] = builder.vertex(curr1)
                val o = tempOffset + s
                if (iv > 0 && iu > 0) // FIXME don't duplicate lines and points
                    builder.rect(tmpIndices[tempOffset], tmpIndices[(o - 1) % s], tmpIndices[(o - (divisionsU + 2)) % s],
                        tmpIndices[(o - (divisionsU + 1)) % s])
                tempOffset = (tempOffset + 1) % tmpIndices.size
            }
        }
    }
}
