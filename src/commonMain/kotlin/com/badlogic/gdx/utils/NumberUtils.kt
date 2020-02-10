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
package com.badlogic.gdx.utils

import com.badlogic.gdx.utils.JsonValue
import com.badlogic.gdx.utils.JsonValue.JsonIterator
import com.badlogic.gdx.utils.JsonValue.PrettyPrintSettings
import com.badlogic.gdx.utils.JsonWriter
import com.badlogic.gdx.utils.LongArray
import com.badlogic.gdx.utils.LongMap
import com.badlogic.gdx.utils.ObjectFloatMap
import java.lang.IllegalStateException
import java.lang.IndexOutOfBoundsException
import kotlin.jvm.Throws

object NumberUtils {
    fun floatToIntBits(value: Float): Int {
        return java.lang.Float.floatToIntBits(value)
    }

    fun floatToRawIntBits(value: Float): Int {
        return java.lang.Float.floatToRawIntBits(value)
    }

    /**
     * Converts the color from a float ABGR encoding to an int ABGR encoding. The alpha is expanded from 0-254 in the float
     * encoding (see [.intToFloatColor]) to 0-255, which means converting from int to float and back to int can be
     * lossy.
     */
    fun floatToIntColor(value: Float): Int {
        var intBits: Int = java.lang.Float.floatToRawIntBits(value)
        intBits = intBits or ((intBits ushr 24) * (255f / 254f)).toInt() shl 24
        return intBits
    }

    /**
     * Encodes the ABGR int color as a float. The alpha is compressed to 0-254 to avoid using bits in the NaN range (see
     * [Float.intBitsToFloat] javadocs). Rendering which uses colors encoded as floats should expand the 0-254 back to
     * 0-255.
     */
    fun intToFloatColor(value: Int): Float {
        return java.lang.Float.intBitsToFloat(value and -0x1000001)
    }

    fun intBitsToFloat(value: Int): Float {
        return java.lang.Float.intBitsToFloat(value)
    }

    fun doubleToLongBits(value: Double): Long {
        return java.lang.Double.doubleToLongBits(value)
    }

    fun longBitsToDouble(value: Long): Double {
        return java.lang.Double.longBitsToDouble(value)
    }
}
