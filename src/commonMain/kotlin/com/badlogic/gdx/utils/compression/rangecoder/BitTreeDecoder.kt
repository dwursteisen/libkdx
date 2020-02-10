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

class BitTreeDecoder(var NumBitLevels: Int) {
    var Models: ShortArray
    fun Init() {
        com.badlogic.gdx.utils.compression.rangecoder.Decoder.Companion.InitBitModels(Models)
    }

    @Throws(IOException::class)
    fun Decode(rangeDecoder: com.badlogic.gdx.utils.compression.rangecoder.Decoder): Int {
        var m = 1
        for (bitIndex in NumBitLevels downTo 1) m = (m shl 1) + rangeDecoder.DecodeBit(Models, m)
        return m - (1 shl NumBitLevels)
    }

    @Throws(IOException::class)
    fun ReverseDecode(rangeDecoder: com.badlogic.gdx.utils.compression.rangecoder.Decoder): Int {
        var m = 1
        var symbol = 0
        for (bitIndex in 0 until NumBitLevels) {
            val bit: Int = rangeDecoder.DecodeBit(Models, m)
            m = m shl 1
            m += bit
            symbol = symbol or (bit shl bitIndex)
        }
        return symbol
    }

    companion object {
        @Throws(IOException::class)
        fun ReverseDecode(Models: ShortArray, startIndex: Int, rangeDecoder: com.badlogic.gdx.utils.compression.rangecoder.Decoder, NumBitLevels: Int): Int {
            var m = 1
            var symbol = 0
            for (bitIndex in 0 until NumBitLevels) {
                val bit: Int = rangeDecoder.DecodeBit(Models, startIndex + m)
                m = m shl 1
                m += bit
                symbol = symbol or (bit shl bitIndex)
            }
            return symbol
        }
    }

    init {
        Models = ShortArray(1 shl NumBitLevels)
    }
}
