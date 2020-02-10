// LZ.InWindow
package com.badlogic.gdx.utils.compression.lz

import java.io.IOException
import java.lang.RuntimeException
import kotlin.jvm.Throws

open class InWindow {
    var _bufferBase // pointer to buffer with data
        : ByteArray?
    var _stream: java.io.InputStream? = null
    var _posLimit // offset (from _buffer) of first byte when new block reading must be done = 0
    var _streamEndWasReached // if (true) then _streamPos shows real end of stream = false
    var _pointerToLastSafePosition = 0
    var _bufferOffset = 0
    var _blockSize // Size of Allocated memory block = 0
    var _pos // offset (from _buffer) of curent byte = 0
    var _keepSizeBefore // how many BYTEs must be kept in buffer before _pos = 0
    var _keepSizeAfter // how many BYTEs must be kept buffer after _pos = 0
    var _streamPos // offset (from _buffer) of first not read byte from Stream = 0

    fun MoveBlock() {
        var offset = _bufferOffset + _pos - _keepSizeBefore
        // we need one additional byte, since MovePos moves on 1 byte.
        if (offset > 0) offset--
        val numBytes = _bufferOffset + _streamPos - offset
        // check negative offset ????
        for (i in 0 until numBytes) _bufferBase!![i] = _bufferBase!![offset + i]
        _bufferOffset -= offset
    }

    @Throws(IOException::class)
    fun ReadBlock() {
        if (_streamEndWasReached) return
        while (true) {
            val size = 0 - _bufferOffset + _blockSize - _streamPos
            if (size == 0) return
            val numReadBytes: Int = _stream.read(_bufferBase, _bufferOffset + _streamPos, size)
            if (numReadBytes == -1) {
                _posLimit = _streamPos
                val pointerToPostion = _bufferOffset + _posLimit
                if (pointerToPostion > _pointerToLastSafePosition) _posLimit = _pointerToLastSafePosition - _bufferOffset
                _streamEndWasReached = true
                return
            }
            _streamPos += numReadBytes
            if (_streamPos >= _pos + _keepSizeAfter) _posLimit = _streamPos - _keepSizeAfter
        }
    }

    fun Free() {
        _bufferBase = null
    }

    fun Create(keepSizeBefore: Int, keepSizeAfter: Int, keepSizeReserv: Int) {
        _keepSizeBefore = keepSizeBefore
        _keepSizeAfter = keepSizeAfter
        val blockSize = keepSizeBefore + keepSizeAfter + keepSizeReserv
        if (_bufferBase == null || _blockSize != blockSize) {
            Free()
            _blockSize = blockSize
            _bufferBase = ByteArray(_blockSize)
        }
        _pointerToLastSafePosition = _blockSize - keepSizeAfter
    }

    fun SetStream(stream: java.io.InputStream?) {
        _stream = stream
    }

    fun ReleaseStream() {
        _stream = null
    }

    @Throws(IOException::class)
    open fun Init() {
        _bufferOffset = 0
        _pos = 0
        _streamPos = 0
        _streamEndWasReached = false
        ReadBlock()
    }

    @Throws(IOException::class)
    open fun MovePos() {
        _pos++
        if (_pos > _posLimit) {
            val pointerToPostion = _bufferOffset + _pos
            if (pointerToPostion > _pointerToLastSafePosition) MoveBlock()
            ReadBlock()
        }
    }

    fun GetIndexByte(index: Int): Byte {
        return _bufferBase!![_bufferOffset + _pos + index]
    }

    // index + limit have not to exceed _keepSizeAfter;
    fun GetMatchLen(index: Int, distance: Int, limit: Int): Int {
        var distance = distance
        var limit = limit
        if (_streamEndWasReached) if (_pos + index + limit > _streamPos) limit = _streamPos - (_pos + index)
        distance++
        // Byte *pby = _buffer + (size_t)_pos + index;
        val pby = _bufferOffset + _pos + index
        var i: Int
        i = 0
        while (i < limit && _bufferBase!![pby + i] == _bufferBase!![pby + i - distance]) {
            i++
        }
        return i
    }

    fun GetNumAvailableBytes(): Int {
        return _streamPos - _pos
    }

    fun ReduceOffsets(subValue: Int) {
        _bufferOffset += subValue
        _posLimit -= subValue
        _pos -= subValue
        _streamPos -= subValue
    }
}
