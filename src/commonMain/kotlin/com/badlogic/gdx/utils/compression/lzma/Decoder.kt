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
package com.badlogic.gdx.utils.compression.lzma

import java.io.IOException
import java.lang.RuntimeException
import kotlin.jvm.Throws

class Decoder {
    inner class LenDecoder {
        var m_Choice = ShortArray(2)
        var m_LowCoder: Array<com.badlogic.gdx.utils.compression.rangecoder.BitTreeDecoder?> = arrayOfNulls<com.badlogic.gdx.utils.compression.rangecoder.BitTreeDecoder>(com.badlogic.gdx.utils.compression.lzma.Base.kNumPosStatesMax)
        var m_MidCoder: Array<com.badlogic.gdx.utils.compression.rangecoder.BitTreeDecoder?> = arrayOfNulls<com.badlogic.gdx.utils.compression.rangecoder.BitTreeDecoder>(com.badlogic.gdx.utils.compression.lzma.Base.kNumPosStatesMax)
        var m_HighCoder: com.badlogic.gdx.utils.compression.rangecoder.BitTreeDecoder = com.badlogic.gdx.utils.compression.rangecoder.BitTreeDecoder(com.badlogic.gdx.utils.compression.lzma.Base.kNumHighLenBits)
        var m_NumPosStates = 0
        fun Create(numPosStates: Int) {
            while (m_NumPosStates < numPosStates) {
                m_LowCoder[m_NumPosStates] = com.badlogic.gdx.utils.compression.rangecoder.BitTreeDecoder(com.badlogic.gdx.utils.compression.lzma.Base.kNumLowLenBits)
                m_MidCoder[m_NumPosStates] = com.badlogic.gdx.utils.compression.rangecoder.BitTreeDecoder(com.badlogic.gdx.utils.compression.lzma.Base.kNumMidLenBits)
                m_NumPosStates++
            }
        }

        fun Init() {
            com.badlogic.gdx.utils.compression.rangecoder.Decoder.Companion.InitBitModels(m_Choice)
            for (posState in 0 until m_NumPosStates) {
                m_LowCoder[posState]!!.Init()
                m_MidCoder[posState]!!.Init()
            }
            m_HighCoder.Init()
        }

        @Throws(IOException::class)
        fun Decode(rangeDecoder: com.badlogic.gdx.utils.compression.rangecoder.Decoder, posState: Int): Int {
            if (rangeDecoder.DecodeBit(m_Choice, 0) == 0) return m_LowCoder[posState]!!.Decode(rangeDecoder)
            var symbol: Int = com.badlogic.gdx.utils.compression.lzma.Base.kNumLowLenSymbols
            if (rangeDecoder.DecodeBit(m_Choice, 1) == 0) symbol += m_MidCoder[posState]!!.Decode(rangeDecoder) else symbol += com.badlogic.gdx.utils.compression.lzma.Base.kNumMidLenSymbols + m_HighCoder.Decode(rangeDecoder)
            return symbol
        }
    }

    inner class LiteralDecoder {
        internal inner class Decoder2 {
            var m_Decoders = ShortArray(0x300)
            fun Init() {
                com.badlogic.gdx.utils.compression.rangecoder.Decoder.Companion.InitBitModels(m_Decoders)
            }

            @Throws(IOException::class)
            fun DecodeNormal(rangeDecoder: com.badlogic.gdx.utils.compression.rangecoder.Decoder): Byte {
                var symbol = 1
                do symbol = symbol shl 1 or rangeDecoder.DecodeBit(m_Decoders, symbol) while (symbol < 0x100)
                return symbol.toByte()
            }

            @Throws(IOException::class)
            fun DecodeWithMatchByte(rangeDecoder: com.badlogic.gdx.utils.compression.rangecoder.Decoder, matchByte: Byte): Byte {
                var matchByte = matchByte
                var symbol = 1
                do {
                    val matchBit: Int = matchByte shr 7 and 1
                    matchByte = matchByte shl 1
                    val bit: Int = rangeDecoder.DecodeBit(m_Decoders, (1 + matchBit shl 8) + symbol)
                    symbol = symbol shl 1 or bit
                    if (matchBit != bit) {
                        while (symbol < 0x100) symbol = symbol shl 1 or rangeDecoder.DecodeBit(m_Decoders, symbol)
                        break
                    }
                } while (symbol < 0x100)
                return symbol.toByte()
            }
        }

        var m_Coders: Array<Decoder2?>?
        var m_NumPrevBits = 0
        var m_NumPosBits = 0
        var m_PosMask = 0
        fun Create(numPosBits: Int, numPrevBits: Int) {
            if (m_Coders != null && m_NumPrevBits == numPrevBits && m_NumPosBits == numPosBits) return
            m_NumPosBits = numPosBits
            m_PosMask = (1 shl numPosBits) - 1
            m_NumPrevBits = numPrevBits
            val numStates = 1 shl m_NumPrevBits + m_NumPosBits
            m_Coders = arrayOfNulls(numStates)
            for (i in 0 until numStates) m_Coders!![i] = Decoder2()
        }

        fun Init() {
            val numStates = 1 shl m_NumPrevBits + m_NumPosBits
            for (i in 0 until numStates) m_Coders!![i]!!.Init()
        }

        fun GetDecoder(pos: Int, prevByte: Byte): Decoder2? {
            return m_Coders!![(pos and m_PosMask shl m_NumPrevBits) + (prevByte and 0xFF ushr 8 - m_NumPrevBits)]
        }
    }

    var m_OutWindow: com.badlogic.gdx.utils.compression.lz.OutWindow = com.badlogic.gdx.utils.compression.lz.OutWindow()
    var m_RangeDecoder: com.badlogic.gdx.utils.compression.rangecoder.Decoder = com.badlogic.gdx.utils.compression.rangecoder.Decoder()
    var m_IsMatchDecoders = ShortArray(com.badlogic.gdx.utils.compression.lzma.Base.kNumStates shl com.badlogic.gdx.utils.compression.lzma.Base.kNumPosStatesBitsMax)
    var m_IsRepDecoders = ShortArray(com.badlogic.gdx.utils.compression.lzma.Base.kNumStates)
    var m_IsRepG0Decoders = ShortArray(com.badlogic.gdx.utils.compression.lzma.Base.kNumStates)
    var m_IsRepG1Decoders = ShortArray(com.badlogic.gdx.utils.compression.lzma.Base.kNumStates)
    var m_IsRepG2Decoders = ShortArray(com.badlogic.gdx.utils.compression.lzma.Base.kNumStates)
    var m_IsRep0LongDecoders = ShortArray(com.badlogic.gdx.utils.compression.lzma.Base.kNumStates shl com.badlogic.gdx.utils.compression.lzma.Base.kNumPosStatesBitsMax)
    var m_PosSlotDecoder: Array<com.badlogic.gdx.utils.compression.rangecoder.BitTreeDecoder?> = arrayOfNulls<com.badlogic.gdx.utils.compression.rangecoder.BitTreeDecoder>(com.badlogic.gdx.utils.compression.lzma.Base.kNumLenToPosStates)
    var m_PosDecoders = ShortArray(com.badlogic.gdx.utils.compression.lzma.Base.kNumFullDistances - com.badlogic.gdx.utils.compression.lzma.Base.kEndPosModelIndex)
    var m_PosAlignDecoder: com.badlogic.gdx.utils.compression.rangecoder.BitTreeDecoder = com.badlogic.gdx.utils.compression.rangecoder.BitTreeDecoder(com.badlogic.gdx.utils.compression.lzma.Base.kNumAlignBits)
    var m_LenDecoder = LenDecoder()
    var m_RepLenDecoder = LenDecoder()
    var m_LiteralDecoder = LiteralDecoder()
    var m_DictionarySize = -1
    var m_DictionarySizeCheck = -1
    var m_PosStateMask = 0
    fun SetDictionarySize(dictionarySize: Int): Boolean {
        if (dictionarySize < 0) return false
        if (m_DictionarySize != dictionarySize) {
            m_DictionarySize = dictionarySize
            m_DictionarySizeCheck = max(m_DictionarySize, 1)
            m_OutWindow.Create(max(m_DictionarySizeCheck, 1 shl 12))
        }
        return true
    }

    fun SetLcLpPb(lc: Int, lp: Int, pb: Int): Boolean {
        if (lc > com.badlogic.gdx.utils.compression.lzma.Base.kNumLitContextBitsMax || lp > 4 || pb > com.badlogic.gdx.utils.compression.lzma.Base.kNumPosStatesBitsMax) return false
        m_LiteralDecoder.Create(lp, lc)
        val numPosStates = 1 shl pb
        m_LenDecoder.Create(numPosStates)
        m_RepLenDecoder.Create(numPosStates)
        m_PosStateMask = numPosStates - 1
        return true
    }

    @Throws(IOException::class)
    fun Init() {
        m_OutWindow.Init(false)
        com.badlogic.gdx.utils.compression.rangecoder.Decoder.Companion.InitBitModels(m_IsMatchDecoders)
        com.badlogic.gdx.utils.compression.rangecoder.Decoder.Companion.InitBitModels(m_IsRep0LongDecoders)
        com.badlogic.gdx.utils.compression.rangecoder.Decoder.Companion.InitBitModels(m_IsRepDecoders)
        com.badlogic.gdx.utils.compression.rangecoder.Decoder.Companion.InitBitModels(m_IsRepG0Decoders)
        com.badlogic.gdx.utils.compression.rangecoder.Decoder.Companion.InitBitModels(m_IsRepG1Decoders)
        com.badlogic.gdx.utils.compression.rangecoder.Decoder.Companion.InitBitModels(m_IsRepG2Decoders)
        com.badlogic.gdx.utils.compression.rangecoder.Decoder.Companion.InitBitModels(m_PosDecoders)
        m_LiteralDecoder.Init()
        var i: Int
        i = 0
        while (i < com.badlogic.gdx.utils.compression.lzma.Base.kNumLenToPosStates) {
            m_PosSlotDecoder[i]!!.Init()
            i++
        }
        m_LenDecoder.Init()
        m_RepLenDecoder.Init()
        m_PosAlignDecoder.Init()
        m_RangeDecoder.Init()
    }

    @Throws(IOException::class)
    fun Code(inStream: java.io.InputStream?, outStream: java.io.OutputStream?, outSize: Long): Boolean {
        m_RangeDecoder.SetStream(inStream)
        m_OutWindow.SetStream(outStream)
        Init()
        var state: Int = com.badlogic.gdx.utils.compression.lzma.Base.StateInit()
        var rep0 = 0
        var rep1 = 0
        var rep2 = 0
        var rep3 = 0
        var nowPos64: Long = 0
        var prevByte: Byte = 0
        while (outSize < 0 || nowPos64 < outSize) {
            val posState = nowPos64.toInt() and m_PosStateMask
            if (m_RangeDecoder.DecodeBit(m_IsMatchDecoders, (state shl com.badlogic.gdx.utils.compression.lzma.Base.kNumPosStatesBitsMax) + posState) == 0) {
                val decoder2 = m_LiteralDecoder.GetDecoder(nowPos64.toInt(), prevByte)
                prevByte = if (!com.badlogic.gdx.utils.compression.lzma.Base.StateIsCharState(state)) decoder2!!.DecodeWithMatchByte(m_RangeDecoder, m_OutWindow.GetByte(rep0)) else decoder2!!.DecodeNormal(m_RangeDecoder)
                m_OutWindow.PutByte(prevByte)
                state = com.badlogic.gdx.utils.compression.lzma.Base.StateUpdateChar(state)
                nowPos64++
            } else {
                var len: Int
                if (m_RangeDecoder.DecodeBit(m_IsRepDecoders, state) == 1) {
                    len = 0
                    if (m_RangeDecoder.DecodeBit(m_IsRepG0Decoders, state) == 0) {
                        if (m_RangeDecoder.DecodeBit(m_IsRep0LongDecoders, (state shl com.badlogic.gdx.utils.compression.lzma.Base.kNumPosStatesBitsMax) + posState) == 0) {
                            state = com.badlogic.gdx.utils.compression.lzma.Base.StateUpdateShortRep(state)
                            len = 1
                        }
                    } else {
                        var distance: Int
                        if (m_RangeDecoder.DecodeBit(m_IsRepG1Decoders, state) == 0) distance = rep1 else {
                            if (m_RangeDecoder.DecodeBit(m_IsRepG2Decoders, state) == 0) distance = rep2 else {
                                distance = rep3
                                rep3 = rep2
                            }
                            rep2 = rep1
                        }
                        rep1 = rep0
                        rep0 = distance
                    }
                    if (len == 0) {
                        len = m_RepLenDecoder.Decode(m_RangeDecoder, posState) + com.badlogic.gdx.utils.compression.lzma.Base.kMatchMinLen
                        state = com.badlogic.gdx.utils.compression.lzma.Base.StateUpdateRep(state)
                    }
                } else {
                    rep3 = rep2
                    rep2 = rep1
                    rep1 = rep0
                    len = com.badlogic.gdx.utils.compression.lzma.Base.kMatchMinLen + m_LenDecoder.Decode(m_RangeDecoder, posState)
                    state = com.badlogic.gdx.utils.compression.lzma.Base.StateUpdateMatch(state)
                    val posSlot: Int = m_PosSlotDecoder[com.badlogic.gdx.utils.compression.lzma.Base.GetLenToPosState(len)]!!.Decode(m_RangeDecoder)
                    if (posSlot >= com.badlogic.gdx.utils.compression.lzma.Base.kStartPosModelIndex) {
                        val numDirectBits = (posSlot shr 1) - 1
                        rep0 = 2 or (posSlot and 1) shl numDirectBits
                        if (posSlot < com.badlogic.gdx.utils.compression.lzma.Base.kEndPosModelIndex) rep0 += com.badlogic.gdx.utils.compression.rangecoder.BitTreeDecoder.Companion.ReverseDecode(m_PosDecoders, rep0 - posSlot - 1, m_RangeDecoder, numDirectBits) else {
                            rep0 += m_RangeDecoder.DecodeDirectBits(numDirectBits - com.badlogic.gdx.utils.compression.lzma.Base.kNumAlignBits) shl com.badlogic.gdx.utils.compression.lzma.Base.kNumAlignBits
                            rep0 += m_PosAlignDecoder.ReverseDecode(m_RangeDecoder)
                            if (rep0 < 0) {
                                if (rep0 == -1) break
                                return false
                            }
                        }
                    } else rep0 = posSlot
                }
                if (rep0 >= nowPos64 || rep0 >= m_DictionarySizeCheck) { // m_OutWindow.Flush();
                    return false
                }
                m_OutWindow.CopyBlock(rep0, len)
                nowPos64 += len.toLong()
                prevByte = m_OutWindow.GetByte(0)
            }
        }
        m_OutWindow.Flush()
        m_OutWindow.ReleaseStream()
        m_RangeDecoder.ReleaseStream()
        return true
    }

    fun SetDecoderProperties(properties: ByteArray): Boolean {
        if (properties.size < 5) return false
        val `val`: Int = properties[0] and 0xFF
        val lc = `val` % 9
        val remainder = `val` / 9
        val lp = remainder % 5
        val pb = remainder / 5
        var dictionarySize = 0
        for (i in 0..3) dictionarySize += properties[1 + i].toInt() and 0xFF shl i * 8
        return if (!SetLcLpPb(lc, lp, pb)) false else SetDictionarySize(dictionarySize)
    }

    init {
        for (i in 0 until com.badlogic.gdx.utils.compression.lzma.Base.kNumLenToPosStates) m_PosSlotDecoder[i] = com.badlogic.gdx.utils.compression.rangecoder.BitTreeDecoder(com.badlogic.gdx.utils.compression.lzma.Base.kNumPosSlotBits)
    }
}
