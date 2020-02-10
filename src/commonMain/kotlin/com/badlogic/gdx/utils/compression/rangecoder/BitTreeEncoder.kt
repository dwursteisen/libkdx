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

class BitTreeEncoder(var NumBitLevels: Int) {
    var Models: ShortArray
    fun Init() {
        com.badlogic.gdx.utils.compression.rangecoder.Decoder.Companion.InitBitModels(Models)
    }

    @Throws(IOException::class)
    fun Encode(rangeEncoder: com.badlogic.gdx.utils.compression.rangecoder.Encoder, symbol: Int) {
        var m = 1
        var bitIndex = NumBitLevels
        while (bitIndex != 0) {
            bitIndex--
            val bit = symbol ushr bitIndex and 1
            rangeEncoder.Encode(Models, m, bit)
            m = m shl 1 or bit
        }
    }

    @Throws(IOException::class)
    fun ReverseEncode(rangeEncoder: com.badlogic.gdx.utils.compression.rangecoder.Encoder, symbol: Int) {
        var symbol = symbol
        var m = 1
        for (i in 0 until NumBitLevels) {
            val bit = symbol and 1
            rangeEncoder.Encode(Models, m, bit)
            m = m shl 1 or bit
            symbol = symbol shr 1
        }
    }

    fun GetPrice(symbol: Int): Int {
        var price = 0
        var m = 1
        var bitIndex = NumBitLevels
        while (bitIndex != 0) {
            bitIndex--
            val bit = symbol ushr bitIndex and 1
            price += com.badlogic.gdx.utils.compression.rangecoder.Encoder.Companion.GetPrice(Models[m].toInt(), bit)
            m = (m shl 1) + bit
        }
        return price
    }

    fun ReverseGetPrice(symbol: Int): Int {
        var symbol = symbol
        var price = 0
        var m = 1
        for (i in NumBitLevels downTo 1) {
            val bit = symbol and 1
            symbol = symbol ushr 1
            price += com.badlogic.gdx.utils.compression.rangecoder.Encoder.Companion.GetPrice(Models[m].toInt(), bit)
            m = m shl 1 or bit
        }
        return price
    }

    companion object {
        fun ReverseGetPrice(Models: ShortArray, startIndex: Int, NumBitLevels: Int, symbol: Int): Int {
            var symbol = symbol
            var price = 0
            var m = 1
            for (i in NumBitLevels downTo 1) {
                val bit = symbol and 1
                symbol = symbol ushr 1
                price += com.badlogic.gdx.utils.compression.rangecoder.Encoder.Companion.GetPrice(Models[startIndex + m].toInt(), bit)
                m = m shl 1 or bit
            }
            return price
        }

        @Throws(IOException::class)
        fun ReverseEncode(Models: ShortArray, startIndex: Int, rangeEncoder: com.badlogic.gdx.utils.compression.rangecoder.Encoder, NumBitLevels: Int, symbol: Int) {
            var symbol = symbol
            var m = 1
            for (i in 0 until NumBitLevels) {
                val bit = symbol and 1
                rangeEncoder.Encode(Models, startIndex + m, bit)
                m = m shl 1 or bit
                symbol = symbol shr 1
            }
        }
    }

    init {
        Models = ShortArray(1 shl NumBitLevels)
    }
}
