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

import com.badlogic.gdx.math.MathUtils
import kotlin.math.pow

/**
 * Takes a linear value in the range of 0-1 and outputs a (usually) non-linear, interpolated value.
 *
 * @author Nathan Sweet
 */
abstract class Interpolation {

    /**
     * @param a Alpha value between 0 and 1.
     */
    abstract fun apply(a: Float): Float

    /**
     * @param a Alpha value between 0 and 1.
     */
    fun apply(start: Float, end: Float, a: Float): Float {
        return start + (end - start) * apply(a)
    }

    //
    open class Pow(val power: Int) : Interpolation() {

        override fun apply(a: Float): Float {
            return if (a <= 0.5f) {
                (a * 2f).pow(power.toFloat()) / 2f
            } else {
                ((a - 1) * 2f).pow(power.toFloat()) / (if (power % 2 == 0) -2 else 2) + 1
            }
        }
    }

    class PowIn(power: Int) : Pow(power) {
        override fun apply(a: Float): Float {
            return a.pow(this.power.toFloat())
        }
    }

    class PowOut(power: Int) : Pow(power) {
        override fun apply(a: Float): Float {
            return kotlin.math.pow(a - 1.toDouble(), power) as Float * (if (power % 2 === 0) -1 else 1) + 1
        }
    }

    //
    class Exp(val value: Float, val power: Float) : Interpolation() {

        val min: Float
        val scale: Float
        override fun apply(a: Float): Float {
            return if (a <= 0.5f) (kotlin.math.pow(value.toDouble(), power * (a * 2 - 1).toDouble()) as Float - min) * scale / 2 else (2 - (kotlin.math.pow(value.toDouble(), -power * (a * 2 - 1).toDouble()) as Float - min) * scale) / 2
        }

        init {
            min = kotlin.math.pow(value.toDouble(), -power.toDouble())
            scale = 1 / (1 - min)
        }
    }

    class ExpIn(value: Float, power: Float) : Exp(value, power) {
        override fun apply(a: Float): Float {
            return (kotlin.math.pow(value, power * (a - 1)) as Float - min) * scale
        }
    }

    class ExpOut(value: Float, power: Float) : Exp(value, power) {
        override fun apply(a: Float): Float {
            return 1 - (kotlin.math.pow(value, -power * a) as Float - min) * scale
        }
    }

    //
    class Elastic(val value: Float, val power: Float, bounces: Int, val scale: Float) : Interpolation() {

        val bounces: Float
        override fun apply(a: Float): Float {
            var a = a
            if (a <= 0.5f) {
                a *= 2f
                return kotlin.math.pow(value.toDouble(), power * (a - 1).toDouble()) as Float * MathUtils.sin(a * bounces) * scale / 2
            }
            a = 1 - a
            a *= 2f
            return 1 - kotlin.math.pow(value.toDouble(), power * (a - 1).toDouble()) as Float * MathUtils.sin(a * bounces) * scale / 2
        }

        init {
            this.bounces = bounces * MathUtils.PI * if (bounces % 2 == 0) 1 else -1
        }
    }

    class ElasticIn(value: Float, power: Float, bounces: Int, scale: Float) : Elastic(value, power, bounces, scale) {
        override fun apply(a: Float): Float {
            return if (a >= 0.99) 1 else kotlin.math.pow(value, power * (a - 1)) as Float * MathUtils.sin(a * bounces) * scale
        }
    }

    class ElasticOut(value: Float, power: Float, bounces: Int, scale: Float) : Elastic(value, power, bounces, scale) {
        override fun apply(a: Float): Float {
            var a = a
            if (a == 0f) return 0
            a = 1 - a
            return 1 - kotlin.math.pow(value, power * (a - 1)) as Float * MathUtils.sin(a * bounces) * scale
        }
    }

    //
    class Bounce : BounceOut {

        constructor(widths: FloatArray, heights: FloatArray) : super(widths, heights) {}
        constructor(bounces: Int) : super(bounces) {}

        private fun out(a: Float): Float {
            val test: Float = a + widths.get(0) / 2
            return if (test < widths.get(0)) test / (widths.get(0) / 2) - 1 else super.apply(a)
        }

        override fun apply(a: Float): Float {
            return if (a <= 0.5f) (1 - out(1 - a * 2)) / 2 else out(a * 2 - 1) / 2 + 0.5f
        }
    }

    class BounceOut : Interpolation {
        val widths: FloatArray
        val heights: FloatArray

        constructor(widths: FloatArray, heights: FloatArray) {
            if (widths.size != heights.size) throw java.lang.IllegalArgumentException("Must be the same number of widths and heights.")
            this.widths = widths
            this.heights = heights
        }

        constructor(bounces: Int) {
            if (bounces < 2 || bounces > 5) throw java.lang.IllegalArgumentException("bounces cannot be < 2 or > 5: $bounces")
            widths = FloatArray(bounces)
            heights = FloatArray(bounces)
            heights[0] = 1
            when (bounces) {
                2 -> {
                    widths[0] = 0.6f
                    widths[1] = 0.4f
                    heights[1] = 0.33f
                }
                3 -> {
                    widths[0] = 0.4f
                    widths[1] = 0.4f
                    widths[2] = 0.2f
                    heights[1] = 0.33f
                    heights[2] = 0.1f
                }
                4 -> {
                    widths[0] = 0.34f
                    widths[1] = 0.34f
                    widths[2] = 0.2f
                    widths[3] = 0.15f
                    heights[1] = 0.26f
                    heights[2] = 0.11f
                    heights[3] = 0.03f
                }
                5 -> {
                    widths[0] = 0.3f
                    widths[1] = 0.3f
                    widths[2] = 0.2f
                    widths[3] = 0.1f
                    widths[4] = 0.1f
                    heights[1] = 0.45f
                    heights[2] = 0.3f
                    heights[3] = 0.15f
                    heights[4] = 0.06f
                }
            }
            widths[0] *= 2
        }

        override fun apply(a: Float): Float {
            var a = a
            if (a == 1f) return 1
            a += widths[0] / 2
            var width = 0f
            var height = 0f
            var i = 0
            val n = widths.size
            while (i < n) {
                width = widths[i]
                if (a <= width) {
                    height = heights[i]
                    break
                }
                a -= width
                i++
            }
            a /= width
            val z = 4 / width * height * a
            return 1 - (z - z * a) * width
        }
    }

    class BounceIn : BounceOut {
        constructor(widths: FloatArray, heights: FloatArray) : super(widths, heights) {}
        constructor(bounces: Int) : super(bounces) {}

        override fun apply(a: Float): Float {
            return 1 - super.apply(1 - a)
        }
    }

    //
    class Swing(scale: Float) : Interpolation() {

        private val scale: Float
        override fun apply(a: Float): Float {
            var a = a
            if (a <= 0.5f) {
                a *= 2f
                return a * a * ((scale + 1) * a - scale) / 2
            }
            a--
            a *= 2f
            return a * a * ((scale + 1) * a + scale) / 2 + 1
        }

        init {
            this.scale = scale * 2
        }
    }

    class SwingOut(private val scale: Float) : Interpolation() {
        override fun apply(a: Float): Float {
            var a = a
            a--
            return a * a * ((scale + 1) * a + scale) + 1
        }
    }

    class SwingIn(private val scale: Float) : Interpolation() {
        override fun apply(a: Float): Float {
            return a * a * ((scale + 1) * a - scale)
        }
    }

    companion object {
        //
        val linear: Interpolation = object : Interpolation() {
            override fun apply(a: Float): Float {
                return a
            }
        }
        //
        /**
         * Aka "smoothstep".
         */
        val smooth: Interpolation = object : Interpolation() {
            override fun apply(a: Float): Float {
                return a * a * (3 - 2 * a)
            }
        }
        val smooth2: Interpolation = object : Interpolation() {
            override fun apply(a: Float): Float {
                var a = a
                a = a * a * (3 - 2 * a)
                return a * a * (3 - 2 * a)
            }
        }

        /**
         * By Ken Perlin.
         */
        val smoother: Interpolation = object : Interpolation() {
            override fun apply(a: Float): Float {
                return a * a * a * (a * (a * 6 - 15) + 10)
            }
        }
        val fade = smoother

        //
        val pow2 = Pow(2)

        /**
         * Slow, then fast.
         */
        val pow2In = PowIn(2)
        val slowFast = pow2In

        /**
         * Fast, then slow.
         */
        val pow2Out = PowOut(2)
        val fastSlow = pow2Out
        val pow2InInverse: Interpolation = object : Interpolation() {
            override fun apply(a: Float): Float {
                return java.lang.Math.sqrt(a.toDouble())
            }
        }
        val pow2OutInverse: Interpolation = object : Interpolation() {
            override fun apply(a: Float): Float {
                return 1 - java.lang.Math.sqrt(-(a - 1).toDouble()) as Float
            }
        }
        val pow3 = Pow(3)
        val pow3In = PowIn(3)
        val pow3Out = PowOut(3)
        val pow3InInverse: Interpolation = object : Interpolation() {
            override fun apply(a: Float): Float {
                return java.lang.Math.cbrt(a.toDouble())
            }
        }
        val pow3OutInverse: Interpolation = object : Interpolation() {
            override fun apply(a: Float): Float {
                return 1 - java.lang.Math.cbrt(-(a - 1).toDouble()) as Float
            }
        }
        val pow4 = Pow(4)
        val pow4In = PowIn(4)
        val pow4Out = PowOut(4)
        val pow5 = Pow(5)
        val pow5In = PowIn(5)
        val pow5Out = PowOut(5)
        val sine: Interpolation = object : Interpolation() {
            override fun apply(a: Float): Float {
                return (1 - MathUtils.cos(a * MathUtils.PI)) / 2
            }
        }
        val sineIn: Interpolation = object : Interpolation() {
            override fun apply(a: Float): Float {
                return 1 - MathUtils.cos(a * MathUtils.PI / 2)
            }
        }
        val sineOut: Interpolation = object : Interpolation() {
            override fun apply(a: Float): Float {
                return MathUtils.sin(a * MathUtils.PI / 2)
            }
        }
        val exp10 = Exp(2, 10)
        val exp10In = ExpIn(2, 10)
        val exp10Out = ExpOut(2, 10)
        val exp5 = Exp(2, 5)
        val exp5In = ExpIn(2, 5)
        val exp5Out = ExpOut(2, 5)
        val circle: Interpolation = object : Interpolation() {
            override fun apply(a: Float): Float {
                var a = a
                if (a <= 0.5f) {
                    a *= 2f
                    return (1 - java.lang.Math.sqrt(1 - a * a.toDouble()) as Float) / 2
                }
                a--
                a *= 2f
                return (java.lang.Math.sqrt(1 - a * a.toDouble()) as Float + 1) / 2
            }
        }
        val circleIn: Interpolation = object : Interpolation() {
            override fun apply(a: Float): Float {
                return 1 - java.lang.Math.sqrt(1 - a * a.toDouble()) as Float
            }
        }
        val circleOut: Interpolation = object : Interpolation() {
            override fun apply(a: Float): Float {
                var a = a
                a--
                return java.lang.Math.sqrt(1 - a * a.toDouble())
            }
        }
        val elastic = Elastic(2, 10, 7, 1)
        val elasticIn = ElasticIn(2, 10, 6, 1)
        val elasticOut = ElasticOut(2, 10, 7, 1)
        val swing = Swing(1.5f)
        val swingIn = SwingIn(2f)
        val swingOut = SwingOut(2f)
        val bounce = Bounce(4)
        val bounceIn = BounceIn(4)
        val bounceOut = BounceOut(4)
    }
}
