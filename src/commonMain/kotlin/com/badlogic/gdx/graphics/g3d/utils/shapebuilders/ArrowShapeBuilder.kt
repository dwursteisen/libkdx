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
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.BaseShapeBuilder
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.BoxShapeBuilder
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.ConeShapeBuilder
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.CylinderShapeBuilder
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.EllipseShapeBuilder
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.FrustumShapeBuilder
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.PatchShapeBuilder
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.RenderableShapeBuilder
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.SphereShapeBuilder
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector3

/**
 * Helper class with static methods to build arrow shapes using [MeshPartBuilder].
 *
 * @author xoppa
 */
object ArrowShapeBuilder : BaseShapeBuilder {

    /**
     * Build an arrow
     *
     * @param x1            source x
     * @param y1            source y
     * @param z1            source z
     * @param x2            destination x
     * @param y2            destination y
     * @param z2            destination z
     * @param capLength     is the height of the cap in percentage, must be in (0,1)
     * @param stemThickness is the percentage of stem diameter compared to cap diameter, must be in (0,1]
     * @param divisions     the amount of vertices used to generate the cap and stem ellipsoidal bases
     */
    fun build(builder: MeshPartBuilder?, x1: Float, y1: Float, z1: Float, x2: Float, y2: Float, z2: Float, capLength: Float, stemThickness: Float,
              divisions: Int) {
        val begin: Vector3 = obtainV3().set(x1, y1, z1)
        val end: Vector3 = obtainV3().set(x2, y2, z2)
        val length = begin.dst(end)
        val coneHeight = length * capLength
        val coneDiameter = 2 * (coneHeight * java.lang.Math.sqrt(1f / 3.toDouble())) as Float
        val stemLength = length - coneHeight
        val stemDiameter = coneDiameter * stemThickness
        val up: Vector3 = obtainV3().set(end).sub(begin).nor()
        val forward: Vector3 = obtainV3().set(up).crs(Vector3.Z)
        if (forward.isZero) forward.set(Vector3.X)
        forward.crs(up).nor()
        val left: Vector3 = obtainV3().set(up).crs(forward).nor()
        val direction: Vector3 = obtainV3().set(end).sub(begin).nor()

        // Matrices
        val userTransform: Matrix4 = builder.getVertexTransform(obtainM4())
        val transform: Matrix4 = obtainM4()
        val `val`: FloatArray = transform.`val`
        `val`[Matrix4.M00] = left.x
        `val`[Matrix4.M01] = up.x
        `val`[Matrix4.M02] = forward.x
        `val`[Matrix4.M10] = left.y
        `val`[Matrix4.M11] = up.y
        `val`[Matrix4.M12] = forward.y
        `val`[Matrix4.M20] = left.z
        `val`[Matrix4.M21] = up.z
        `val`[Matrix4.M22] = forward.z
        val temp: Matrix4 = obtainM4()

        // Stem
        transform.setTranslation(obtainV3().set(direction).scl(stemLength / 2).add(x1, y1, z1))
        builder.setVertexTransform(temp.set(transform).mul(userTransform))
        CylinderShapeBuilder.build(builder, stemDiameter, stemLength, stemDiameter, divisions)

        // Cap
        transform.setTranslation(obtainV3().set(direction).scl(stemLength).add(x1, y1, z1))
        builder.setVertexTransform(temp.set(transform).mul(userTransform))
        ConeShapeBuilder.build(builder, coneDiameter, coneHeight, coneDiameter, divisions)
        builder.setVertexTransform(userTransform)
        freeAll()
    }
}
