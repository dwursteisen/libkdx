package com.badlogic.gdx.utils

class DataBuffer : DataOutput {
    private var outStream: OptimizedByteArrayOutputStream? = null

    fun DataBuffer() {
        this(32)
    }

    fun DataBuffer(initialSize: Int) {
        super(StreamUtils.OptimizedByteArrayOutputStream(initialSize))
        outStream = out as StreamUtils.OptimizedByteArrayOutputStream?
    }

    /**
     * Returns the backing array, which has 0 to [.size] items.
     */
    fun getBuffer(): ByteArray? {
        return outStream.getBuffer()
    }

    fun toArray(): ByteArray? {
        return outStream.toByteArray()
    }
}
