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
import java.lang.RuntimeException

/**
 * This class implements the xorshift128+ algorithm that is a very fast, top-quality 64-bit pseudo-random number generator. The
 * quality of this PRNG is much higher than [Random]'s, and its cycle length is 2<sup>128</sup>&nbsp;&nbsp;1, which
 * is more than enough for any single-thread application. More details and algorithms can be found [here](http://xorshift.di.unimi.it/).
 *
 *
 * Instances of RandomXS128 are not thread-safe.
 *
 * @author Inferno
 * @author davebaol
 */
class RandomXS128 : Random {

    /**
     * The first half of the internal state of this pseudo-random number generator.
     */
    private var seed0: Long = 0

    /**
     * The second half of the internal state of this pseudo-random number generator.
     */
    private var seed1: Long = 0

    /**
     * Creates a new random number generator. This constructor sets the seed of the random number generator to a value very likely
     * to be distinct from any other invocation of this constructor.
     *
     *
     * This implementation creates a [Random] instance to generate the initial seed.
     */
    constructor() {
        setSeed(Random().nextLong())
    }

    /**
     * Creates a new random number generator using a single `long` seed.
     *
     * @param seed the initial seed
     */
    constructor(seed: Long) {
        setSeed(seed)
    }

    /**
     * Creates a new random number generator using two `long` seeds.
     *
     * @param seed0 the first part of the initial seed
     * @param seed1 the second part of the initial seed
     */
    constructor(seed0: Long, seed1: Long) {
        setState(seed0, seed1)
    }

    /**
     * Returns the next pseudo-random, uniformly distributed `long` value from this random number generator's sequence.
     *
     *
     * Subclasses should override this, as this is used by all other methods.
     */
    fun nextLong(): Long {
        var s1 = seed0
        val s0 = seed1
        seed0 = s0
        s1 = s1 xor s1 shl 23
        return (s1 xor s0 xor (s1 ushr 17) xor (s0 ushr 26)).also { seed1 = it } + s0
    }

    /**
     * This protected method is final because, contrary to the superclass, it's not used anymore by the other methods.
     */
    protected fun next(bits: Int): Int {
        return (nextLong() and (1L shl bits) - 1).toInt()
    }

    /**
     * Returns the next pseudo-random, uniformly distributed `int` value from this random number generator's sequence.
     *
     *
     * This implementation uses [.nextLong] internally.
     */
    fun nextInt(): Int {
        return nextLong().toInt()
    }

    /**
     * Returns a pseudo-random, uniformly distributed `int` value between 0 (inclusive) and the specified value (exclusive),
     * drawn from this random number generator's sequence.
     *
     *
     * This implementation uses [.nextLong] internally.
     *
     * @param n the positive bound on the random number to be returned.
     * @return the next pseudo-random `int` value between `0` (inclusive) and `n` (exclusive).
     */
    fun nextInt(n: Int): Int {
        return nextLong(n.toLong()).toInt()
    }

    /**
     * Returns a pseudo-random, uniformly distributed `long` value between 0 (inclusive) and the specified value (exclusive),
     * drawn from this random number generator's sequence. The algorithm used to generate the value guarantees that the result is
     * uniform, provided that the sequence of 64-bit values produced by this generator is.
     *
     *
     * This implementation uses [.nextLong] internally.
     *
     * @param n the positive bound on the random number to be returned.
     * @return the next pseudo-random `long` value between `0` (inclusive) and `n` (exclusive).
     */
    fun nextLong(n: Long): Long {
        if (n <= 0) throw java.lang.IllegalArgumentException("n must be positive")
        while (true) {
            val bits = nextLong() ushr 1
            val value = bits % n
            if (bits - value + (n - 1) >= 0) return value
        }
    }

    /**
     * Returns a pseudo-random, uniformly distributed `double` value between 0.0 and 1.0 from this random number generator's
     * sequence.
     *
     *
     * This implementation uses [.nextLong] internally.
     */
    fun nextDouble(): Double {
        return (nextLong() ushr 11) * NORM_DOUBLE
    }

    /**
     * Returns a pseudo-random, uniformly distributed `float` value between 0.0 and 1.0 from this random number generator's
     * sequence.
     *
     *
     * This implementation uses [.nextLong] internally.
     */
    fun nextFloat(): Float {
        return ((nextLong() ushr 40) * NORM_FLOAT).toFloat()
    }

    /**
     * Returns a pseudo-random, uniformly distributed `boolean ` value from this random number generator's sequence.
     *
     *
     * This implementation uses [.nextLong] internally.
     */
    fun nextBoolean(): Boolean {
        return nextLong() and 1 != 0L
    }

    /**
     * Generates random bytes and places them into a user-supplied byte array. The number of random bytes produced is equal to the
     * length of the byte array.
     *
     *
     * This implementation uses [.nextLong] internally.
     */
    fun nextBytes(bytes: ByteArray) {
        var n = 0
        var i = bytes.size
        while (i != 0) {
            n = if (i < 8) i else 8 // min(i, 8);
            var bits = nextLong()
            while (n-- != 0) {
                bytes[--i] = bits.toByte()
                bits = bits shr 8
            }
        }
    }

    /**
     * Sets the internal seed of this generator based on the given `long` value.
     *
     *
     * The given seed is passed twice through a hash function. This way, if the user passes a small value we avoid the short
     * irregular transient associated with states having a very small number of bits set.
     *
     * @param seed a nonzero seed for this generator (if zero, the generator will be seeded with [Long.MIN_VALUE]).
     */
    fun setSeed(seed: Long) {
        val seed0 = murmurHash3(if (seed == 0L) Long.MIN_VALUE else seed)
        setState(seed0, murmurHash3(seed0))
    }

    /**
     * Sets the internal state of this generator.
     *
     * @param seed0 the first part of the internal state
     * @param seed1 the second part of the internal state
     */
    fun setState(seed0: Long, seed1: Long) {
        this.seed0 = seed0
        this.seed1 = seed1
    }

    /**
     * Returns the internal seeds to allow state saving.
     *
     * @param seed must be 0 or 1, designating which of the 2 long seeds to return
     * @return the internal seed that can be used in setState
     */
    fun getState(seed: Int): Long {
        return if (seed == 0) seed0 else seed1
    }

    companion object {
        /**
         * Normalization constant for double.
         */
        private const val NORM_DOUBLE = 1.0 / (1L shl 53)

        /**
         * Normalization constant for float.
         */
        private const val NORM_FLOAT = 1.0 / (1L shl 24)
        private fun murmurHash3(x: Long): Long {
            var x = x
            x = x xor x ushr 33
            x *= -0xae502812aa7333L
            x = x xor x ushr 33
            x *= -0x3b314601e57a13adL
            x = x xor x ushr 33
            return x
        }
    }
}
