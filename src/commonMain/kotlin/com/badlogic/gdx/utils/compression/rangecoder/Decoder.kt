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
package com.badlogic.gdx.utils.compression.rangecoder

import java.io.IOException
import java.lang.RuntimeException
import kotlin.jvm.Throws

class Decoder {
    var Range = 0
    var Code = 0
    var Stream: java.io.InputStream? = null
    fun SetStream(stream: java.io.InputStream?) {
        Stream = stream
    }

    fun ReleaseStream() {
        Stream = null
    }

    @Throws(IOException::class)
    fun Init() {
        Code = 0
        Range = -1
        for (i in 0..4) Code = Code shl 8 or Stream.read()
    }

    @Throws(IOException::class)
    fun DecodeDirectBits(numTotalBits: Int): Int {
        var result = 0
        for (i in numTotalBits downTo 1) {
            Range = Range ushr 1
            val t = Code - Range ushr 31
            Code -= Range and t - 1
            result = result shl 1 or 1 - t
            if (Range and kTopMask == 0) {
                Code = Code shl 8 or Stream.read()
                Range = Range shl 8
            }
        }
        return result
    }

    @Throws(IOException::class)
    fun DecodeBit(probs: ShortArray, index: Int): Int {
        val prob = probs[index].toInt()
        val newBound = (Range ushr kNumBitModelTotalBits) * prob
        return if (Code xor -0x80000000 < newBound xor -0x80000000) {
            Range = newBound
            probs[index] = (prob + (kBitModelTotal - prob ushr kNumMoveBits)).toShort()
            if (Range and kTopMask == 0) {
                Code = Code shl 8 or Stream.read()
                Range = Range shl 8
            }
            0
        } else {
            Range -= newBound
            Code -= newBound
            probs[index] = (prob - (prob ushr kNumMoveBits)).toShort()
            if (Range and kTopMask == 0) {
                Code = Code shl 8 or Stream.read()
                Range = Range shl 8
            }
            1
        }
    }

    companion object {
        const val kTopMask = ((1 shl 24) - 1).inv()
        const val kNumBitModelTotalBits = 11
        const val kBitModelTotal = 1 shl kNumBitModelTotalBits
        const val kNumMoveBits = 5
        fun InitBitModels(probs: ShortArray) {
            for (i in probs.indices) probs[i] = (kBitModelTotal ushr 1).toShort()
        }
    }
}
