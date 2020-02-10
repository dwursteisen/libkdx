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
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.CylinderShapeBuilder
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.EllipseShapeBuilder
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.FrustumShapeBuilder
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.PatchShapeBuilder
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.RenderableShapeBuilder
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.SphereShapeBuilder
import com.badlogic.gdx.math.MathUtils

/**
 * Helper class with static methods to build cone shapes using [MeshPartBuilder].
 *
 * @author xoppa
 */
object ConeShapeBuilder : BaseShapeBuilder {

    fun build(builder: MeshPartBuilder?, width: Float, height: Float, depth: Float, divisions: Int) {
        build(builder, width, height, depth, divisions, 0f, 360f)
    }

    fun build(builder: MeshPartBuilder?, width: Float, height: Float, depth: Float, divisions: Int, angleFrom: Float,
              angleTo: Float) {
        build(builder, width, height, depth, divisions, angleFrom, angleTo, true)
    }

    fun build(builder: MeshPartBuilder?, width: Float, height: Float, depth: Float, divisions: Int, angleFrom: Float,
              angleTo: Float, close: Boolean) {
        // FIXME create better cylinder method (- axis on which to create the cone (matrix?))
        builder.ensureVertices(divisions + 2)
        builder.ensureTriangleIndices(divisions)
        val hw = width * 0.5f
        val hh = height * 0.5f
        val hd = depth * 0.5f
        val ao: Float = MathUtils.degreesToRadians * angleFrom
        val step: Float = MathUtils.degreesToRadians * (angleTo - angleFrom) / divisions
        val us = 1f / divisions
        var u = 0f
        var angle = 0f
        val curr1: VertexInfo = vertTmp3.set(null, null, null, null)
        curr1.hasNormal = true
        curr1.hasPosition = curr1.hasNormal
        curr1.hasUV = curr1.hasPosition
        val curr2: VertexInfo = vertTmp4.set(null, null, null, null).setPos(0, hh, 0).setNor(0, 1, 0).setUV(0.5f, 0)
        val base: Short = builder.vertex(curr2)
        var i1: Short
        var i2: Short = 0
        for (i in 0..divisions) {
            angle = ao + step * i
            u = 1f - us * i
            curr1.position.set(MathUtils.cos(angle) * hw, 0f, MathUtils.sin(angle) * hd)
            curr1.normal.set(curr1.position).nor()
            curr1.position.y = -hh
            curr1.uv.set(u, 1)
            i1 = builder.vertex(curr1)
            if (i != 0) builder.triangle(base, i1, i2) // FIXME don't duplicate lines and points
            i2 = i1
        }
        if (close) EllipseShapeBuilder.build(builder, width, depth, 0, 0, divisions, 0, -hh, 0, 0, -1, 0, -1, 0, 0, 0, 0, 1,
            180f - angleTo, 180f - angleFrom)
    }
}
