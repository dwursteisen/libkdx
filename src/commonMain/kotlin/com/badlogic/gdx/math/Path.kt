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
package com.badlogic.gdx.math

import com.badlogic.gdx.math.Affine2
import com.badlogic.gdx.math.BSpline
import com.badlogic.gdx.math.Bezier
import com.badlogic.gdx.math.CatmullRomSpline
import com.badlogic.gdx.math.CumulativeDistribution.CumulativeValue
import com.badlogic.gdx.math.DelaunayTriangulator
import com.badlogic.gdx.math.EarClippingTriangulator
import com.badlogic.gdx.math.Frustum
import com.badlogic.gdx.math.GeometryUtils
import com.badlogic.gdx.math.GridPoint2
import com.badlogic.gdx.math.GridPoint3
import com.badlogic.gdx.math.Intersector
import com.badlogic.gdx.math.Intersector.MinimumTranslationVector
import com.badlogic.gdx.math.Intersector.SplitTriangle
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.MathUtils.Sin
import com.badlogic.gdx.math.Matrix3
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Plane
import com.badlogic.gdx.math.Plane.PlaneSide
import com.badlogic.gdx.math.RandomXS128
import java.lang.RuntimeException

/**
 * Interface that specifies a path of type T within the window 0.0<=t<=1.0.
 *
 * @author Xoppa
 */
interface Path<T> {

    fun derivativeAt(out: T, t: Float): T

    /**
     * @return The value of the path at t where 0<=t<=1
     */
    fun valueAt(out: T, t: Float): T

    /**
     * @return The approximated value (between 0 and 1) on the path which is closest to the specified value. Note that the
     * implementation of this method might be optimized for speed against precision, see [.locate] for a more
     * precise (but more intensive) method.
     */
    fun approximate(v: T): Float

    /**
     * @return The precise location (between 0 and 1) on the path which is closest to the specified value. Note that the
     * implementation of this method might be CPU intensive, see [.approximate] for a faster (but less
     * precise) method.
     */
    fun locate(v: T): Float

    /**
     * @param samples The amount of divisions used to approximate length. Higher values will produce more precise results,
     * but will be more CPU intensive.
     * @return An approximated length of the spline through sampling the curve and accumulating the euclidean distances between
     * the sample points.
     */
    fun approxLength(samples: Int): Float
}
