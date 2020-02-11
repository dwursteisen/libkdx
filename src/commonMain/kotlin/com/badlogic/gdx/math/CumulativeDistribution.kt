package com.badlogic.gdx.math

import com.badlogic.gdx.math.Affine2
import com.badlogic.gdx.math.BSpline
import com.badlogic.gdx.math.Bezier
import com.badlogic.gdx.math.CatmullRomSpline
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
 * This class represents a cumulative distribution.
 * It can be used in scenarios where there are values with different probabilities
 * and it's required to pick one of those respecting the probability.
 * For example one could represent the frequency of the alphabet letters using a cumulative distribution
 * and use it to randomly pick a letter respecting their probabilities (useful when generating random words).
 * Another example could be point generation on a mesh surface: one could generate a cumulative distribution using
 * triangles areas as interval size, in this way triangles with a large area will be picked more often than triangles with a smaller one.
 * See [Wikipedia](http://en.wikipedia.org/wiki/Cumulative_distribution_function) for a detailed explanation.
 *
 * @author Inferno
 */
class CumulativeDistribution<T> {

    inner class CumulativeValue(var value: T, var frequency: Float, var interval: Float)

    private val values: Array<CumulativeValue>

    /**
     * Adds a value with a given interval size to the distribution
     */
    fun add(value: T, intervalSize: Float) {
        values.add(CumulativeValue(value, 0, intervalSize))
    }

    /**
     * Adds a value with interval size equal to zero to the distribution
     */
    fun add(value: T) {
        values.add(CumulativeValue(value, 0, 0))
    }

    /**
     * Generate the cumulative distribution
     */
    fun generate() {
        var sum = 0f
        for (i in 0 until values.size) {
            sum += values.items.get(i).interval
            values.items.get(i).frequency = sum
        }
    }

    /**
     * Generate the cumulative distribution in [0,1] where each interval will get a frequency between [0,1]
     */
    fun generateNormalized() {
        var sum = 0f
        for (i in 0 until values.size) {
            sum += values.items.get(i).interval
        }
        var intervalSum = 0f
        for (i in 0 until values.size) {
            intervalSum += values.items.get(i).interval / sum
            values.items.get(i).frequency = intervalSum
        }
    }

    /**
     * Generate the cumulative distribution in [0,1] where each value will have the same frequency and interval size
     */
    fun generateUniform() {
        val freq = 1f / values.size
        for (i in 0 until values.size) {
            //reset the interval to the normalized frequency
            values.items.get(i).interval = freq
            values.items.get(i).frequency = (i + 1) * freq
        }
    }
    /**
     * Finds the value whose interval contains the given probability
     * Binary search algorithm is used to find the value.
     *
     * @param probability
     * @return the value whose interval contains the probability
     */
    /**
     * @return the value whose interval contains a random probability in [0,1]
     */
    @JvmOverloads
    fun value(probability: Float = MathUtils.random()): T {
        var value: CumulativeValue? = null
        var imax = values.size - 1
        var imin = 0
        var imid: Int
        while (imin <= imax) {
            imid = imin + (imax - imin) / 2
            value = values.items.get(imid)
            if (probability < value.frequency) imax = imid - 1 else if (probability > value.frequency) imin = imid + 1 else break
        }
        return values.items.get(imin).value
    }

    /**
     * @return the amount of values
     */
    fun size(): Int {
        return values.size
    }

    /**
     * @return the interval size for the value at the given position
     */
    fun getInterval(index: Int): Float {
        return values.items.get(index).interval
    }

    /**
     * @return the value at the given position
     */
    fun getValue(index: Int): T {
        return values.items.get(index).value
    }

    /**
     * Set the interval size on the passed in object.
     * The object must be present in the distribution.
     */
    fun setInterval(obj: T, intervalSize: Float) {
        for (value in values) if (value.value === obj) {
            value.interval = intervalSize
            return
        }
    }

    /**
     * Sets the interval size for the value at the given index
     */
    fun setInterval(index: Int, intervalSize: Float) {
        values.items.get(index).interval = intervalSize
    }

    /**
     * Removes all the values from the distribution
     */
    fun clear() {
        values.clear()
    }

    init {
        values = Array(false, 10, CumulativeValue::class.java)
    }
}
