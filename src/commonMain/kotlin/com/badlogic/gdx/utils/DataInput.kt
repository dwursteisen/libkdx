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

import kotlin.CharArray
import kotlin.jvm.Throws

/**
 * Extends [DataInputStream] with additional convenience methods.
 *
 * @author Nathan Sweet
 */
class DataInput(`in`: InputStream?) : DataInputStream(`in`) {

    private var chars = CharArray(32)

    /**
     * Reads a 1-5 byte int.
     */
    @Throws(IOException::class)
    fun readInt(optimizePositive: Boolean): Int {
        var b: Int = read()
        var result = b and 0x7F
        if (b and 0x80 != 0) {
            b = read()
            result = result or (b and 0x7F) shl 7
            if (b and 0x80 != 0) {
                b = read()
                result = result or (b and 0x7F) shl 14
                if (b and 0x80 != 0) {
                    b = read()
                    result = result or (b and 0x7F) shl 21
                    if (b and 0x80 != 0) {
                        b = read()
                        result = result or (b and 0x7F) shl 28
                    }
                }
            }
        }
        return if (optimizePositive) result else result ushr 1 xor -(result and 1)
    }

    /**
     * Reads the length and string of UTF8 characters, or null.
     *
     * @return May be null.
     */
    @Throws(IOException::class)
    fun readString(): String? {
        var charCount = readInt(true)
        when (charCount) {
            0 -> return null
            1 -> return ""
        }
        charCount--
        if (chars.size < charCount) chars = CharArray(charCount)
        val chars = chars
        // Try to read 7 bit ASCII chars.
        var charIndex = 0
        var b = 0
        while (charIndex < charCount) {
            b = read()
            if (b > 127) break
            chars[charIndex++] = b.toChar()
        }
        // If a char was not ASCII, finish with slow path.
        if (charIndex < charCount) readUtf8_slow(charCount, charIndex, b)
        return String(chars, 0, charCount)
    }

    @Throws(IOException::class)
    private fun readUtf8_slow(charCount: Int, charIndex: Int, b: Int) {
        var charIndex = charIndex
        var b = b
        val chars = chars
        while (true) {
            when (b shr 4) {
                0, 1, 2, 3, 4, 5, 6, 7 -> chars[charIndex] = b.toChar()
                12, 13 -> chars[charIndex] = (b and 0x1F shl 6 or read() and 0x3F).toChar()
                14 -> chars[charIndex] = (b and 0x0F shl 12 or (read() and 0x3F shl 6) or (read() and 0x3F)).toChar()
            }
            if (++charIndex >= charCount) break
            b = read() and 0xFF
        }
    }
}
