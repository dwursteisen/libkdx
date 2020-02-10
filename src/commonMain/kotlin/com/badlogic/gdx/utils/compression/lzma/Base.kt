// Base.java
package com.badlogic.gdx.utils.compression.lzma

import java.io.IOException
import java.lang.RuntimeException
import kotlin.jvm.Throws

object Base {
    const val kNumRepDistances = 4
    const val kNumStates = 12
    fun StateInit(): Int {
        return 0
    }

    fun StateUpdateChar(index: Int): Int {
        if (index < 4) return 0
        return if (index < 10) index - 3 else index - 6
    }

    fun StateUpdateMatch(index: Int): Int {
        return if (index < 7) 7 else 10
    }

    fun StateUpdateRep(index: Int): Int {
        return if (index < 7) 8 else 11
    }

    fun StateUpdateShortRep(index: Int): Int {
        return if (index < 7) 9 else 11
    }

    fun StateIsCharState(index: Int): Boolean {
        return index < 7
    }

    const val kNumPosSlotBits = 6
    const val kDicLogSizeMin = 0
    // public static final int kDicLogSizeMax = 28;
// public static final int kDistTableSizeMax = kDicLogSizeMax * 2;
    const val kNumLenToPosStatesBits = 2 // it's for speed optimization
    const val kNumLenToPosStates = 1 shl kNumLenToPosStatesBits
    const val kMatchMinLen = 2
    fun GetLenToPosState(len: Int): Int {
        var len = len
        len -= kMatchMinLen
        return if (len < kNumLenToPosStates) len else (kNumLenToPosStates - 1)
    }

    const val kNumAlignBits = 4
    const val kAlignTableSize = 1 shl kNumAlignBits
    const val kAlignMask = kAlignTableSize - 1
    const val kStartPosModelIndex = 4
    const val kEndPosModelIndex = 14
    const val kNumPosModels = kEndPosModelIndex - kStartPosModelIndex
    const val kNumFullDistances = 1 shl kEndPosModelIndex / 2
    const val kNumLitPosStatesBitsEncodingMax = 4
    const val kNumLitContextBitsMax = 8
    const val kNumPosStatesBitsMax = 4
    const val kNumPosStatesMax = 1 shl kNumPosStatesBitsMax
    const val kNumPosStatesBitsEncodingMax = 4
    const val kNumPosStatesEncodingMax = 1 shl kNumPosStatesBitsEncodingMax
    const val kNumLowLenBits = 3
    const val kNumMidLenBits = 3
    const val kNumHighLenBits = 8
    const val kNumLowLenSymbols = 1 shl kNumLowLenBits
    const val kNumMidLenSymbols = 1 shl kNumMidLenBits
    const val kNumLenSymbols = kNumLowLenSymbols + kNumMidLenSymbols + (1 shl kNumHighLenBits)
    const val kMatchMaxLen = kMatchMinLen + kNumLenSymbols - 1
}
