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
package com.badlogic.gdx.utils

import com.badlogic.gdx.utils.BooleanArray
import com.badlogic.gdx.utils.ByteArray
import java.lang.IllegalStateException
import java.lang.IndexOutOfBoundsException
import kotlin.LongArray

/**
 * A bitset, without size limitation, allows comparison via bitwise operators to other bitfields.
 *
 * @author mzechner
 * @author jshapcott
 */
class Bits {

    var bits = longArrayOf(0)

    constructor() {}

    /**
     * Creates a bit set whose initial size is large enough to explicitly represent bits with indices in the range 0 through
     * nbits-1.
     *
     * @param nbits the initial size of the bit set
     */
    constructor(nbits: Int) {
        checkCapacity(nbits ushr 6)
    }

    /**
     * @param index the index of the bit
     * @return whether the bit is set
     * @throws ArrayIndexOutOfBoundsException if index < 0
     */
    operator fun get(index: Int): Boolean {
        val word = index ushr 6
        return if (word >= bits.size) false else bits[word] and (1L shl (index and 0x3F)) != 0L
    }

    /**
     * Returns the bit at the given index and clears it in one go.
     *
     * @param index the index of the bit
     * @return whether the bit was set before invocation
     * @throws ArrayIndexOutOfBoundsException if index < 0
     */
    fun getAndClear(index: Int): Boolean {
        val word = index ushr 6
        if (word >= bits.size) return false
        val oldBits = bits[word]
        bits[word] = bits[word] and (1L shl (index and 0x3F)).inv()
        return bits[word] != oldBits
    }

    /**
     * Returns the bit at the given index and sets it in one go.
     *
     * @param index the index of the bit
     * @return whether the bit was set before invocation
     * @throws ArrayIndexOutOfBoundsException if index < 0
     */
    fun getAndSet(index: Int): Boolean {
        val word = index ushr 6
        checkCapacity(word)
        val oldBits = bits[word]
        bits[word] = bits[word] or 1L shl (index and 0x3F)
        return bits[word] == oldBits
    }

    /**
     * @param index the index of the bit to set
     * @throws ArrayIndexOutOfBoundsException if index < 0
     */
    fun set(index: Int) {
        val word = index ushr 6
        checkCapacity(word)
        bits[word] = bits[word] or 1L shl (index and 0x3F)
    }

    /**
     * @param index the index of the bit to flip
     */
    fun flip(index: Int) {
        val word = index ushr 6
        checkCapacity(word)
        bits[word] = bits[word] xor 1L shl (index and 0x3F)
    }

    private fun checkCapacity(len: Int) {
        if (len >= bits.size) {
            val newBits = LongArray(len + 1)
            java.lang.System.arraycopy(bits, 0, newBits, 0, bits.size)
            bits = newBits
        }
    }

    /**
     * @param index the index of the bit to clear
     * @throws ArrayIndexOutOfBoundsException if index < 0
     */
    fun clear(index: Int) {
        val word = index ushr 6
        if (word >= bits.size) return
        bits[word] = bits[word] and (1L shl (index and 0x3F)).inv()
    }

    /**
     * Clears the entire bitset
     */
    fun clear() {
        val bits = bits
        val length = bits.size
        for (i in 0 until length) {
            bits[i] = 0L
        }
    }

    /**
     * @return the number of bits currently stored, **not** the highset set bit!
     */
    fun numBits(): Int {
        return bits.size shl 6
    }

    /**
     * Returns the "logical size" of this bitset: the index of the highest set bit in the bitset plus one. Returns zero if the
     * bitset contains no set bits.
     *
     * @return the logical size of this bitset
     */
    fun length(): Int {
        val bits = bits
        for (word in bits.indices.reversed()) {
            val bitsAtWord = bits[word]
            if (bitsAtWord != 0L) {
                for (bit in 63 downTo 0) {
                    if (bitsAtWord and (1L shl (bit and 0x3F)) != 0L) {
                        return (word shl 6) + bit + 1
                    }
                }
            }
        }
        return 0
    }

    /**
     * @return true if this bitset contains at least one bit set to true
     */
    fun notEmpty(): Boolean {
        return !isEmpty
    }

    /**
     * @return true if this bitset contains no bits that are set to true
     */
    val isEmpty: Boolean
        get() {
            val bits = bits
            val length = bits.size
            for (i in 0 until length) {
                if (bits[i] != 0L) {
                    return false
                }
            }
            return true
        }

    /**
     * Returns the index of the first bit that is set to true that occurs on or after the specified starting index. If no such bit
     * exists then -1 is returned.
     */
    fun nextSetBit(fromIndex: Int): Int {
        val bits = bits
        var word = fromIndex ushr 6
        val bitsLength = bits.size
        if (word >= bitsLength) return -1
        var bitsAtWord = bits[word]
        if (bitsAtWord != 0L) {
            for (i in fromIndex and 0x3f..63) {
                if (bitsAtWord and (1L shl (i and 0x3F)) != 0L) {
                    return (word shl 6) + i
                }
            }
        }
        word++
        while (word < bitsLength) {
            if (word != 0) {
                bitsAtWord = bits[word]
                if (bitsAtWord != 0L) {
                    for (i in 0..63) {
                        if (bitsAtWord and (1L shl (i and 0x3F)) != 0L) {
                            return (word shl 6) + i
                        }
                    }
                }
            }
            word++
        }
        return -1
    }

    /**
     * Returns the index of the first bit that is set to false that occurs on or after the specified starting index.
     */
    fun nextClearBit(fromIndex: Int): Int {
        val bits = bits
        var word = fromIndex ushr 6
        val bitsLength = bits.size
        if (word >= bitsLength) return bits.size shl 6
        var bitsAtWord = bits[word]
        for (i in fromIndex and 0x3f..63) {
            if (bitsAtWord and (1L shl (i and 0x3F)) == 0L) {
                return (word shl 6) + i
            }
        }
        word++
        while (word < bitsLength) {
            if (word == 0) {
                return word shl 6
            }
            bitsAtWord = bits[word]
            for (i in 0..63) {
                if (bitsAtWord and (1L shl (i and 0x3F)) == 0L) {
                    return (word shl 6) + i
                }
            }
            word++
        }
        return bits.size shl 6
    }

    /**
     * Performs a logical **AND** of this target bit set with the argument bit set. This bit set is modified so that each bit in
     * it has the value true if and only if it both initially had the value true and the corresponding bit in the bit set argument
     * also had the value true.
     *
     * @param other a bit set
     */
    fun and(other: Bits) {
        val commonWords: Int = java.lang.Math.min(bits.size, other.bits.size)
        var i = 0
        while (commonWords > i) {
            bits[i] = bits[i] and other.bits[i]
            i++
        }
        if (bits.size > commonWords) {
            var i = commonWords
            val s = bits.size
            while (s > i) {
                bits[i] = 0L
                i++
            }
        }
    }

    /**
     * Clears all of the bits in this bit set whose corresponding bit is set in the specified bit set.
     *
     * @param other a bit set
     */
    fun andNot(other: Bits) {
        var i = 0
        val j = bits.size
        val k = other.bits.size
        while (i < j && i < k) {
            bits[i] = bits[i] and other.bits[i].inv()
            i++
        }
    }

    /**
     * Performs a logical **OR** of this bit set with the bit set argument. This bit set is modified so that a bit in it has the
     * value true if and only if it either already had the value true or the corresponding bit in the bit set argument has the
     * value true.
     *
     * @param other a bit set
     */
    fun or(other: Bits) {
        val commonWords: Int = java.lang.Math.min(bits.size, other.bits.size)
        var i = 0
        while (commonWords > i) {
            bits[i] = bits[i] or other.bits[i]
            i++
        }
        if (commonWords < other.bits.size) {
            checkCapacity(other.bits.size)
            var i = commonWords
            val s = other.bits.size
            while (s > i) {
                bits[i] = other.bits[i]
                i++
            }
        }
    }

    /**
     * Performs a logical **XOR** of this bit set with the bit set argument. This bit set is modified so that a bit in it has
     * the value true if and only if one of the following statements holds:
     *
     *  * The bit initially has the value true, and the corresponding bit in the argument has the value false.
     *  * The bit initially has the value false, and the corresponding bit in the argument has the value true.
     *
     *
     * @param other
     */
    fun xor(other: Bits) {
        val commonWords: Int = java.lang.Math.min(bits.size, other.bits.size)
        var i = 0
        while (commonWords > i) {
            bits[i] = bits[i] xor other.bits[i]
            i++
        }
        if (commonWords < other.bits.size) {
            checkCapacity(other.bits.size)
            var i = commonWords
            val s = other.bits.size
            while (s > i) {
                bits[i] = other.bits[i]
                i++
            }
        }
    }

    /**
     * Returns true if the specified BitSet has any bits set to true that are also set to true in this BitSet.
     *
     * @param other a bit set
     * @return boolean indicating whether this bit set intersects the specified bit set
     */
    fun intersects(other: Bits): Boolean {
        val bits = bits
        val otherBits = other.bits
        for (i in java.lang.Math.min(bits.size, otherBits.size) - 1 downTo 0) {
            if (bits[i] and otherBits[i] != 0L) {
                return true
            }
        }
        return false
    }

    /**
     * Returns true if this bit set is a super set of the specified set, i.e. it has all bits set to true that are also set to true
     * in the specified BitSet.
     *
     * @param other a bit set
     * @return boolean indicating whether this bit set is a super set of the specified set
     */
    fun containsAll(other: Bits): Boolean {
        val bits = bits
        val otherBits = other.bits
        val otherBitsLength = otherBits.size
        val bitsLength = bits.size
        for (i in bitsLength until otherBitsLength) {
            if (otherBits[i] != 0) {
                return false
            }
        }
        for (i in java.lang.Math.min(bitsLength, otherBitsLength) - 1 downTo 0) {
            if (bits[i] and otherBits[i] != otherBits[i]) {
                return false
            }
        }
        return true
    }

    override fun hashCode(): Int {
        val word = length() ushr 6
        var hash = 0
        var i = 0
        while (word >= i) {
            hash = 127 * hash + (bits[i] xor (bits[i] ushr 32)).toInt()
            i++
        }
        return hash
    }

    override fun equals(obj: Any?): Boolean {
        if (this === obj) return true
        if (obj == null) return false
        if (javaClass != obj.javaClass) return false
        val other = obj as Bits
        val otherBits = other.bits
        val commonWords: Int = java.lang.Math.min(bits.size, otherBits.size)
        var i = 0
        while (commonWords > i) {
            if (bits[i] != otherBits[i]) return false
            i++
        }
        return if (bits.size == otherBits.size) true else length() == other.length()
    }
}
