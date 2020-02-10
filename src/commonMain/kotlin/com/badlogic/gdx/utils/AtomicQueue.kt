/*
 * Copyright (c) 2008-2010, Matthias Mann
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following
 * conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following
 * disclaimer in the documentation and/or other materials provided with the distribution. * Neither the name of Matthias Mann nor
 * the names of its contributors may be used to endorse or promote products derived from this software without specific prior
 * written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING,
 * BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT
 * SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.badlogic.gdx.utils

import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.Array.ArrayIterable
import com.badlogic.gdx.utils.ArrayMap
import com.badlogic.gdx.utils.Base64Coder
import com.badlogic.gdx.utils.Base64Coder.CharMap
import java.lang.IllegalStateException
import java.lang.IndexOutOfBoundsException

/**
 * A queue that allows one thread to call [.put] and another thread to call [.poll]. Multiple threads must
 * not call these methods.
 *
 * @author Matthias Mann
 */
class AtomicQueue<T>(capacity: Int) {

    private val writeIndex: AtomicInteger = AtomicInteger()
    private val readIndex: AtomicInteger = AtomicInteger()
    private val queue: AtomicReferenceArray<T>
    private fun next(idx: Int): Int {
        return (idx + 1) % queue.length()
    }

    fun put(value: T): Boolean {
        val write: Int = writeIndex.get()
        val read: Int = readIndex.get()
        val next = next(write)
        if (next == read) return false
        queue.set(write, value)
        writeIndex.set(next)
        return true
    }

    fun poll(): T? {
        val read: Int = readIndex.get()
        val write: Int = writeIndex.get()
        if (read == write) return null
        val value: T = queue.get(read)
        readIndex.set(next(read))
        return value
    }

    init {
        queue = AtomicReferenceArray(capacity)
    }
}
