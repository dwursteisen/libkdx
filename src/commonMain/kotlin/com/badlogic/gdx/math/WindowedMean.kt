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
 * A simple class keeping track of the mean of a stream of values within a certain window. the WindowedMean will only return a
 * value in case enough data has been sampled. After enough data has been sampled the oldest sample will be replaced by the newest
 * in case a new sample is added.
 *
 * @author badlogicgames@gmail.com
 */
class WindowedMean(window_size: Int) {

    var values: FloatArray
    var valueCount = 0
    var last_value = 0
    var mean = 0f
    var dirty = true

    /**
     * @return whether the value returned will be meaningful
     */
    fun hasEnoughData(): Boolean {
        return valueCount >= values.size
    }

    /**
     * clears this WindowedMean. The class will only return meaningful values after enough data has been added again.
     */
    fun clear() {
        valueCount = 0
        last_value = 0
        for (i in values.indices) values[i] = 0
        dirty = true
    }

    /**
     * adds a new sample to this mean. In case the window is full the oldest value will be replaced by this new value.
     *
     * @param value The value to add
     */
    fun addValue(value: Float) {
        if (valueCount < values.size) valueCount++
        values[last_value++] = value
        if (last_value > values.size - 1) last_value = 0
        dirty = true
    }

    /**
     * returns the mean of the samples added to this instance. Only returns meaningful results when at least window_size samples
     * as specified in the constructor have been added.
     *
     * @return the mean
     */
    fun getMean(): Float {
        return if (hasEnoughData()) {
            if (dirty) {
                var mean = 0f
                for (i in values.indices) mean += values[i]
                this.mean = mean / values.size
                dirty = false
            }
            mean
        } else 0
    }

    /**
     * @return the oldest value in the window
     */
    val oldest: Float
        get() = if (valueCount < values.size) values[0] else values[last_value]

    /**
     * @return the value last added
     */
    val latest: Float
        get() = values[if (last_value - 1 == -1) values.size - 1 else last_value - 1]

    /**
     * @return The standard deviation
     */
    fun standardDeviation(): Float {
        if (!hasEnoughData()) return 0
        val mean = getMean()
        var sum = 0f
        for (i in values.indices) {
            sum += (values[i] - mean) * (values[i] - mean)
        }
        return java.lang.Math.sqrt(sum / values.size.toDouble())
    }

    val lowest: Float
        get() {
            var lowest = Float.MAX_VALUE
            for (i in values.indices) lowest = java.lang.Math.min(lowest, values[i])
            return lowest
        }

    val highest: Float
        get() {
            var lowest: Float = java.lang.Float.MIN_NORMAL
            for (i in values.indices) lowest = java.lang.Math.max(lowest, values[i])
            return lowest
        }

    val windowSize: Int
        get() = values.size

    /**
     * @return A new `float[]` containing all values currently in the window of the stream, in order from oldest to
     * latest. The length of the array is smaller than the window size if not enough data has been added.
     */
    val windowValues: FloatArray
        get() {
            val windowValues = FloatArray(valueCount)
            if (hasEnoughData()) {
                for (i in windowValues.indices) {
                    windowValues[i] = values[(i + last_value) % values.size]
                }
            } else {
                java.lang.System.arraycopy(values, 0, windowValues, 0, valueCount)
            }
            return windowValues
        }

    /**
     * constructor, window_size specifies the number of samples we will continuously get the mean and variance from. the class
     * will only return meaning full values if at least window_size values have been added.
     *
     * @param window_size size of the sample window
     */
    init {
        values = FloatArray(window_size)
    }
}
