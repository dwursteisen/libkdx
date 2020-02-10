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
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder.VertexInfo
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
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.FlushablePool
import com.badlogic.gdx.utils.Pool
import com.badlogic.gdx.utils.ShortArray

/**
 * This class allows to reduce the static allocation needed for shape builders. It contains all the objects used internally by
 * shape builders.
 *
 * @author realitix, xoppa
 */
object BaseShapeBuilder {

    /* Color */
    internal val tmpColor0: Color? = Color()
    internal val tmpColor1: Color? = Color()
    internal val tmpColor2: Color? = Color()
    internal val tmpColor3: Color? = Color()
    internal val tmpColor4: Color? = Color()

    /* Vector3 */
    internal val tmpV0: Vector3? = Vector3()
    internal val tmpV1: Vector3? = Vector3()
    internal val tmpV2: Vector3? = Vector3()
    internal val tmpV3: Vector3? = Vector3()
    internal val tmpV4: Vector3? = Vector3()
    internal val tmpV5: Vector3? = Vector3()
    internal val tmpV6: Vector3? = Vector3()
    internal val tmpV7: Vector3? = Vector3()

    /* VertexInfo */
    internal val vertTmp0: VertexInfo? = VertexInfo()
    internal val vertTmp1: VertexInfo? = VertexInfo()
    internal val vertTmp2: VertexInfo? = VertexInfo()
    internal val vertTmp3: VertexInfo? = VertexInfo()
    internal val vertTmp4: VertexInfo? = VertexInfo()
    internal val vertTmp5: VertexInfo? = VertexInfo()
    internal val vertTmp6: VertexInfo? = VertexInfo()
    internal val vertTmp7: VertexInfo? = VertexInfo()
    internal val vertTmp8: VertexInfo? = VertexInfo()

    /* Matrix4 */
    internal val matTmp1: Matrix4? = Matrix4()
    private val vectorPool: FlushablePool<Vector3?>? = object : FlushablePool<Vector3?>() {
        protected fun newObject(): Vector3? {
            return Vector3()
        }
    }
    private val matrices4Pool: FlushablePool<Matrix4?>? = object : FlushablePool<Matrix4?>() {
        protected fun newObject(): Matrix4? {
            return Matrix4()
        }
    }

    /**
     * Obtain a temporary [Vector3] object, must be free'd using [.freeAll].
     */
    internal fun obtainV3(): Vector3? {
        return vectorPool.obtain()
    }

    /**
     * Obtain a temporary [Matrix4] object, must be free'd using [.freeAll].
     */
    internal fun obtainM4(): Matrix4? {
        return matrices4Pool.obtain()
    }

    /**
     * Free all objects obtained using one of the `obtainXX` methods.
     */
    internal fun freeAll() {
        vectorPool.flush()
        matrices4Pool.flush()
    }
}
