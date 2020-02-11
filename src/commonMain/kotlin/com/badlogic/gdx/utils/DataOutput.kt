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

import kotlin.jvm.Throws

/**
 * Extends [DataOutputStream] with additional convenience methods.
 *
 * @author Nathan Sweet
 */
class DataOutput(out: OutputStream?) : DataOutputStream(out) {

    /**
     * Writes a 1-5 byte int.
     *
     * @param optimizePositive If true, small positive numbers will be more efficient (1 byte) and small negative numbers will be
     * inefficient (5 bytes).
     */
    @Throws(IOException::class)
    fun writeInt(value: Int, optimizePositive: Boolean): Int {
        var value = value
        if (!optimizePositive) value = value shl 1 xor (value shr 31)
        if (value ushr 7 == 0) {
            write(value.toByte())
            return 1
        }
        write((value and 0x7F or 0x80).toByte())
        if (value ushr 14 == 0) {
            write((value ushr 7).toByte())
            return 2
        }
        write((value ushr 7 or 0x80).toByte())
        if (value ushr 21 == 0) {
            write((value ushr 14).toByte())
            return 3
        }
        write((value ushr 14 or 0x80).toByte())
        if (value ushr 28 == 0) {
            write((value ushr 21).toByte())
            return 4
        }
        write((value ushr 21 or 0x80).toByte())
        write((value ushr 28).toByte())
        return 5
    }

    /**
     * Writes a length and then the string as UTF8.
     *
     * @param value May be null.
     */
    @Throws(IOException::class)
    fun writeString(value: String?) {
        if (value == null) {
            write(0)
            return
        }
        val charCount = value.length
        if (charCount == 0) {
            writeByte(1)
            return
        }
        writeInt(charCount + 1, true)
        // Try to write 8 bit chars.
        var charIndex = 0
        while (charIndex < charCount) {
            val c = value[charIndex].toInt()
            if (c > 127) break
            write(c.toByte())
            charIndex++
        }
        if (charIndex < charCount) writeString_slow(value, charCount, charIndex)
    }

    @Throws(IOException::class)
    private fun writeString_slow(value: String, charCount: Int, charIndex: Int) {
        var charIndex = charIndex
        while (charIndex < charCount) {
            val c = value[charIndex].toInt()
            if (c <= 0x007F) {
                write(c.toByte())
            } else if (c > 0x07FF) {
                write((0xE0 or c shr 12 and 0x0F).toByte())
                write((0x80 or c shr 6 and 0x3F).toByte())
                write((0x80 or c and 0x3F).toByte())
            } else {
                write((0xC0 or c shr 6 and 0x1F).toByte())
                write((0x80 or c and 0x3F).toByte())
            }
            charIndex++
        }
    }
}
