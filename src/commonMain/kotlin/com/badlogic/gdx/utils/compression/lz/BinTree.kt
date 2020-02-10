// LZ.BinTree
package com.badlogic.gdx.utils.compression.lz

import java.io.IOException
import java.lang.RuntimeException
import kotlin.jvm.Throws

class BinTree : com.badlogic.gdx.utils.compression.lz.InWindow() {
    var _cyclicBufferPos = 0
    var _cyclicBufferSize = 0
    var _matchMaxLen = 0
    var _son: IntArray
    var _hash: IntArray
    var _cutValue = 0xFF
    var _hashMask = 0
    var _hashSizeSum = 0
    var HASH_ARRAY = true
    var kNumHashDirectBytes = 0
    var kMinMatchCheck = 4
    var kFixHashSize = kHash2Size + kHash3Size
    fun SetType(numHashBytes: Int) {
        HASH_ARRAY = numHashBytes > 2
        if (HASH_ARRAY) {
            kNumHashDirectBytes = 0
            kMinMatchCheck = 4
            kFixHashSize = kHash2Size + kHash3Size
        } else {
            kNumHashDirectBytes = 2
            kMinMatchCheck = 2 + 1
            kFixHashSize = 0
        }
    }

    @Throws(IOException::class)
    override fun Init() {
        super.Init()
        for (i in 0 until _hashSizeSum) _hash[i] = kEmptyHashValue
        _cyclicBufferPos = 0
        ReduceOffsets(-1)
    }

    @Throws(IOException::class)
    override fun MovePos() {
        if (++_cyclicBufferPos >= _cyclicBufferSize) _cyclicBufferPos = 0
        super.MovePos()
        if (_pos == kMaxValForNormalize) Normalize()
    }

    fun Create(historySize: Int, keepAddBufferBefore: Int, matchMaxLen: Int, keepAddBufferAfter: Int): Boolean {
        if (historySize > kMaxValForNormalize - 256) return false
        _cutValue = 16 + (matchMaxLen shr 1)
        val windowReservSize = (historySize + keepAddBufferBefore + matchMaxLen + keepAddBufferAfter) / 2 + 256
        super.Create(historySize + keepAddBufferBefore, matchMaxLen + keepAddBufferAfter, windowReservSize)
        _matchMaxLen = matchMaxLen
        val cyclicBufferSize = historySize + 1
        if (_cyclicBufferSize != cyclicBufferSize) _son = IntArray(cyclicBufferSize.also { _cyclicBufferSize = it } * 2)
        var hs = kBT2HashSize
        if (HASH_ARRAY) {
            hs = historySize - 1
            hs = hs or (hs shr 1)
            hs = hs or (hs shr 2)
            hs = hs or (hs shr 4)
            hs = hs or (hs shr 8)
            hs = hs shr 1
            hs = hs or 0xFFFF
            if (hs > 1 shl 24) hs = hs shr 1
            _hashMask = hs
            hs++
            hs += kFixHashSize
        }
        if (hs != _hashSizeSum) _hash = IntArray(hs.also { _hashSizeSum = it })
        return true
    }

    @Throws(IOException::class)
    fun GetMatches(distances: IntArray): Int {
        val lenLimit: Int
        if (_pos + _matchMaxLen <= _streamPos) lenLimit = _matchMaxLen else {
            lenLimit = _streamPos - _pos
            if (lenLimit < kMinMatchCheck) {
                MovePos()
                return 0
            }
        }
        var offset = 0
        val matchMinPos = if (_pos > _cyclicBufferSize) _pos - _cyclicBufferSize else 0
        val cur: Int = _bufferOffset + _pos
        var maxLen = kStartMaxLen // to avoid items for len < hashSize;
        val hashValue: Int
        var hash2Value = 0
        var hash3Value = 0
        if (HASH_ARRAY) {
            var temp = CrcTable[_bufferBase!!.get(cur) and 0xFF] xor (_bufferBase!!.get(cur + 1) and 0xFF)
            hash2Value = temp and kHash2Size - 1
            temp = temp xor ((_bufferBase!!.get(cur + 2) and 0xFF) as Int shl 8)
            hash3Value = temp and kHash3Size - 1
            hashValue = temp xor (CrcTable[_bufferBase!!.get(cur + 3) and 0xFF] shl 5) and _hashMask
        } else hashValue = _bufferBase!!.get(cur) and 0xFF xor ((_bufferBase!!.get(cur + 1) and 0xFF) as Int shl 8)
        var curMatch = _hash[kFixHashSize + hashValue]
        if (HASH_ARRAY) {
            var curMatch2 = _hash[hash2Value]
            val curMatch3 = _hash[kHash3Offset + hash3Value]
            _hash[hash2Value] = _pos
            _hash[kHash3Offset + hash3Value] = _pos
            if (curMatch2 > matchMinPos) if (_bufferBase!!.get(_bufferOffset + curMatch2) == _bufferBase!!.get(cur)) {
                maxLen = 2
                distances[offset++] = maxLen
                distances[offset++] = _pos - curMatch2 - 1
            }
            if (curMatch3 > matchMinPos) if (_bufferBase!!.get(_bufferOffset + curMatch3) == _bufferBase!!.get(cur)) {
                if (curMatch3 == curMatch2) offset -= 2
                maxLen = 3
                distances[offset++] = maxLen
                distances[offset++] = _pos - curMatch3 - 1
                curMatch2 = curMatch3
            }
            if (offset != 0 && curMatch2 == curMatch) {
                offset -= 2
                maxLen = kStartMaxLen
            }
        }
        _hash[kFixHashSize + hashValue] = _pos
        var ptr0 = (_cyclicBufferPos shl 1) + 1
        var ptr1 = _cyclicBufferPos shl 1
        var len0: Int
        var len1: Int
        len1 = kNumHashDirectBytes
        len0 = len1
        if (kNumHashDirectBytes != 0) {
            if (curMatch > matchMinPos) {
                if (_bufferBase!!.get(_bufferOffset + curMatch + kNumHashDirectBytes) != _bufferBase!!.get(cur + kNumHashDirectBytes)) {
                    maxLen = kNumHashDirectBytes
                    distances[offset++] = maxLen
                    distances[offset++] = _pos - curMatch - 1
                }
            }
        }
        var count = _cutValue
        while (true) {
            if (curMatch <= matchMinPos || count-- == 0) {
                _son[ptr1] = kEmptyHashValue
                _son[ptr0] = _son[ptr1]
                break
            }
            val delta: Int = _pos - curMatch
            val cyclicPos = (if (delta <= _cyclicBufferPos) _cyclicBufferPos - delta else _cyclicBufferPos - delta + _cyclicBufferSize) shl 1
            val pby1: Int = _bufferOffset + curMatch
            var len: Int = java.lang.Math.min(len0, len1)
            if (_bufferBase!!.get(pby1 + len) == _bufferBase!!.get(cur + len)) {
                while (++len != lenLimit) if (_bufferBase!!.get(pby1 + len) != _bufferBase!!.get(cur + len)) break
                if (maxLen < len) {
                    maxLen = len
                    distances[offset++] = maxLen
                    distances[offset++] = delta - 1
                    if (len == lenLimit) {
                        _son[ptr1] = _son[cyclicPos]
                        _son[ptr0] = _son[cyclicPos + 1]
                        break
                    }
                }
            }
            if (_bufferBase!!.get(pby1 + len) and 0xFF < _bufferBase!!.get(cur + len) and 0xFF) {
                _son[ptr1] = curMatch
                ptr1 = cyclicPos + 1
                curMatch = _son[ptr1]
                len1 = len
            } else {
                _son[ptr0] = curMatch
                ptr0 = cyclicPos
                curMatch = _son[ptr0]
                len0 = len
            }
        }
        MovePos()
        return offset
    }

    @Throws(IOException::class)
    fun Skip(num: Int) {
        var num = num
        do {
            var lenLimit: Int
            if (_pos + _matchMaxLen <= _streamPos) lenLimit = _matchMaxLen else {
                lenLimit = _streamPos - _pos
                if (lenLimit < kMinMatchCheck) {
                    MovePos()
                    continue
                }
            }
            val matchMinPos = if (_pos > _cyclicBufferSize) _pos - _cyclicBufferSize else 0
            val cur: Int = _bufferOffset + _pos
            var hashValue: Int
            if (HASH_ARRAY) {
                var temp = CrcTable[_bufferBase!!.get(cur) and 0xFF] xor (_bufferBase!!.get(cur + 1) and 0xFF)
                val hash2Value = temp and kHash2Size - 1
                _hash[hash2Value] = _pos
                temp = temp xor ((_bufferBase!!.get(cur + 2) and 0xFF) as Int shl 8)
                val hash3Value = temp and kHash3Size - 1
                _hash[kHash3Offset + hash3Value] = _pos
                hashValue = temp xor (CrcTable[_bufferBase!!.get(cur + 3) and 0xFF] shl 5) and _hashMask
            } else hashValue = _bufferBase!!.get(cur) and 0xFF xor ((_bufferBase!!.get(cur + 1) and 0xFF) as Int shl 8)
            var curMatch = _hash[kFixHashSize + hashValue]
            _hash[kFixHashSize + hashValue] = _pos
            var ptr0 = (_cyclicBufferPos shl 1) + 1
            var ptr1 = _cyclicBufferPos shl 1
            var len0: Int
            var len1: Int
            len1 = kNumHashDirectBytes
            len0 = len1
            var count = _cutValue
            while (true) {
                if (curMatch <= matchMinPos || count-- == 0) {
                    _son[ptr1] = kEmptyHashValue
                    _son[ptr0] = _son[ptr1]
                    break
                }
                val delta: Int = _pos - curMatch
                val cyclicPos = (if (delta <= _cyclicBufferPos) _cyclicBufferPos - delta else _cyclicBufferPos - delta + _cyclicBufferSize) shl 1
                val pby1: Int = _bufferOffset + curMatch
                var len: Int = java.lang.Math.min(len0, len1)
                if (_bufferBase!!.get(pby1 + len) == _bufferBase!!.get(cur + len)) {
                    while (++len != lenLimit) if (_bufferBase!!.get(pby1 + len) != _bufferBase!!.get(cur + len)) break
                    if (len == lenLimit) {
                        _son[ptr1] = _son[cyclicPos]
                        _son[ptr0] = _son[cyclicPos + 1]
                        break
                    }
                }
                if (_bufferBase!!.get(pby1 + len) and 0xFF < _bufferBase!!.get(cur + len) and 0xFF) {
                    _son[ptr1] = curMatch
                    ptr1 = cyclicPos + 1
                    curMatch = _son[ptr1]
                    len1 = len
                } else {
                    _son[ptr0] = curMatch
                    ptr0 = cyclicPos
                    curMatch = _son[ptr0]
                    len0 = len
                }
            }
            MovePos()
        } while (--num != 0)
    }

    fun NormalizeLinks(items: IntArray, numItems: Int, subValue: Int) {
        for (i in 0 until numItems) {
            var value = items[i]
            if (value <= subValue) value = kEmptyHashValue else value -= subValue
            items[i] = value
        }
    }

    fun Normalize() {
        val subValue: Int = _pos - _cyclicBufferSize
        NormalizeLinks(_son, _cyclicBufferSize * 2, subValue)
        NormalizeLinks(_hash, _hashSizeSum, subValue)
        ReduceOffsets(subValue)
    }

    fun SetCutValue(cutValue: Int) {
        _cutValue = cutValue
    }

    companion object {
        const val kHash2Size = 1 shl 10
        const val kHash3Size = 1 shl 16
        const val kBT2HashSize = 1 shl 16
        const val kStartMaxLen = 1
        const val kHash3Offset = kHash2Size
        const val kEmptyHashValue = 0
        const val kMaxValForNormalize = (1 shl 30) - 1
        private val CrcTable = IntArray(256)

        init {
            for (i in 0..255) {
                val r = i
                for (j in 0..7) if (com.badlogic.gdx.utils.compression.lz.r and 1 != 0) com.badlogic.gdx.utils.compression.lz.r = com.badlogic.gdx.utils.compression.lz.r ushr 1 xor -0x12477ce0 else com.badlogic.gdx.utils.compression.lz.r = com.badlogic.gdx.utils.compression.lz.r ushr 1
                CrcTable[i] = com.badlogic.gdx.utils.compression.lz.r
            }
        }
    }
}
