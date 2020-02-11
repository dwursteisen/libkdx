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
 * Track properties of a stream of float values. The properties (total value, minimum, etc) are updated as values are
 * [.put] into the stream.
 *
 * @author xoppa
 */
class FloatCounter(windowSize: Int) {

    /**
     * The amount of values added
     */
    var count = 0

    /**
     * The sum of all values
     */
    var total = 0f

    /**
     * The smallest value
     */
    var min = 0f

    /**
     * The largest value
     */
    var max = 0f

    /**
     * The average value (total / count)
     */
    var average = 0f

    /**
     * The latest raw value
     */
    var latest = 0f

    /**
     * The current windowed mean value
     */
    var value = 0f

    /**
     * Provides access to the WindowedMean if any (can be null)
     */
    val mean: WindowedMean?

    /**
     * Add a value and update all fields.
     *
     * @param value The value to add
     */
    fun put(value: Float) {
        latest = value
        total += value
        count++
        average = total / count
        if (mean != null) {
            mean.addValue(value)
            this.value = mean.getMean()
        } else this.value = latest
        if (mean == null || mean.hasEnoughData()) {
            if (this.value < min) min = this.value
            if (this.value > max) max = this.value
        }
    }

    /**
     * Reset all values to their default value.
     */
    fun reset() {
        count = 0
        total = 0f
        min = Float.MAX_VALUE
        max = -Float.MAX_VALUE
        average = 0f
        latest = 0f
        value = 0f
        if (mean != null) mean.clear()
    }

    /**
     * Construct a new FloatCounter
     *
     * @param windowSize The size of the mean window or 1 or below to not use a windowed mean.
     */
    init {
        mean = if (windowSize > 1) WindowedMean(windowSize) else null
        reset()
    }
}
