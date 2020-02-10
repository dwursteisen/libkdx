// SevenZip/CRC.java
package com.badlogic.gdx.utils.compression

import java.io.IOException
import java.lang.RuntimeException
import kotlin.jvm.Throws

class CRC {
    companion object {
        var Table = IntArray(256)

        init {
            for (i in 0..255) {
                val r = i
                for (j in 0..7) if (com.badlogic.gdx.utils.compression.r and 1 != 0) com.badlogic.gdx.utils.compression.r = com.badlogic.gdx.utils.compression.r ushr 1 xor -0x12477ce0 else com.badlogic.gdx.utils.compression.r = com.badlogic.gdx.utils.compression.r ushr 1
                Table[i] = com.badlogic.gdx.utils.compression.r
            }
        }
    }

    var _value = -1
    fun Init() {
        _value = -1
    }

    fun Update(data: ByteArray, offset: Int, size: Int) {
        for (i in 0 until size) _value = Table[_value xor data[offset + i].toInt() and 0xFF] xor (_value ushr 8)
    }

    fun Update(data: ByteArray) {
        val size = data.size
        for (i in 0 until size) _value = Table[_value xor data[i].toInt() and 0xFF] xor (_value ushr 8)
    }

    fun UpdateByte(b: Int) {
        _value = Table[_value xor b and 0xFF] xor (_value ushr 8)
    }

    fun GetDigest(): Int {
        return _value xor -1
    }
}
