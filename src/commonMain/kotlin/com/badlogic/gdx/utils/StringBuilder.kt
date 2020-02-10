/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.badlogic.gdx.utils

import java.lang.Appendable
import java.lang.ArrayIndexOutOfBoundsException
import java.lang.IndexOutOfBoundsException
import java.lang.NegativeArraySizeException
import java.lang.NullPointerException
import java.lang.StringIndexOutOfBoundsException

/**
 * A [java.lang.StringBuilder] that implements equals and hashcode.
 *
 * @see CharSequence
 *
 * @see Appendable
 *
 * @see java.lang.StringBuilder
 *
 * @see String
 */
class StringBuilder : Appendable, CharSequence {

    /*
     * Returns the character array.
     */
    var value: CharArray
    override var length = 0

    /**
     * Constructs an instance with an initial capacity of `16`.
     *
     * @see .capacity
     */
    constructor() {
        value = CharArray(INITIAL_CAPACITY)
    }

    /**
     * Constructs an instance with the specified capacity.
     *
     * @param capacity the initial capacity to use.
     * @throws NegativeArraySizeException if the specified `capacity` is negative.
     * @see .capacity
     */
    constructor(capacity: Int) {
        if (capacity < 0) {
            throw NegativeArraySizeException()
        }
        value = CharArray(capacity)
    }

    /**
     * Constructs an instance that's initialized with the contents of the specified `CharSequence`. The capacity of the new
     * builder will be the length of the `CharSequence` plus 16.
     *
     * @param seq the `CharSequence` to copy into the builder.
     * @throws NullPointerException if `seq` is `null`.
     */
    constructor(seq: CharSequence) : this(seq.toString()) {}
    constructor(builder: StringBuilder) {
        length = builder.length
        value = CharArray(length + INITIAL_CAPACITY)
        java.lang.System.arraycopy(builder.value, 0, value, 0, length)
    }

    /**
     * Constructs an instance that's initialized with the contents of the specified `String`. The capacity of the new
     * builder will be the length of the `String` plus 16.
     *
     * @param string the `String` to copy into the builder.
     * @throws NullPointerException if `str` is `null`.
     */
    constructor(string: String) {
        length = string.length
        value = CharArray(length + INITIAL_CAPACITY)
        string.toCharArray(value, 0, 0, length)
    }

    private fun enlargeBuffer(min: Int) {
        val newSize = (value.size shr 1) + value.size + 2
        val newData = CharArray(if (min > newSize) min else newSize)
        java.lang.System.arraycopy(value, 0, newData, 0, length)
        value = newData
    }

    fun appendNull() {
        val newSize = length + 4
        if (newSize > value.size) {
            enlargeBuffer(newSize)
        }
        value[length++] = 'n'
        value[length++] = 'u'
        value[length++] = 'l'
        value[length++] = 'l'
    }

    fun append0(value: CharArray) {
        val newSize = length + value.size
        if (newSize > value.size) {
            enlargeBuffer(newSize)
        }
        java.lang.System.arraycopy(value, 0, this.value, length, value.size)
        length = newSize
    }

    fun append0(value: CharArray, offset: Int, length: Int) {
        // Force null check of chars first!
        if (offset > value.size || offset < 0) {
            throw ArrayIndexOutOfBoundsException("Offset out of bounds: $offset")
        }
        if (length < 0 || value.size - offset < length) {
            throw ArrayIndexOutOfBoundsException("Length out of bounds: $length")
        }
        val newSize = this.length + length
        if (newSize > value.size) {
            enlargeBuffer(newSize)
        }
        java.lang.System.arraycopy(value, offset, this.value, this.length, length)
        this.length = newSize
    }

    fun append0(ch: Char) {
        if (length == value.size) {
            enlargeBuffer(length + 1)
        }
        value[length++] = ch
    }

    fun append0(string: String?) {
        if (string == null) {
            appendNull()
            return
        }
        val adding = string.length
        val newSize = length + adding
        if (newSize > value.size) {
            enlargeBuffer(newSize)
        }
        string.toCharArray(value, length, 0, adding)
        length = newSize
    }

    fun append0(s: CharSequence?, start: Int, end: Int) {
        var s = s
        if (s == null) {
            s = "null"
        }
        if (start < 0 || end < 0 || start > end || end > s.length) {
            throw IndexOutOfBoundsException()
        }
        append0(s.subSequence(start, end).toString())
    }

    /**
     * Returns the number of characters that can be held without growing.
     *
     * @return the capacity
     * @see .ensureCapacity
     *
     * @see .length
     */
    fun capacity(): Int {
        return value.size
    }

    /**
     * Retrieves the character at the `index`.
     *
     * @param index the index of the character to retrieve.
     * @return the char value.
     * @throws IndexOutOfBoundsException if `index` is negative or greater than or equal to the current [.length].
     */
    override fun charAt(index: Int): Char {
        if (index < 0 || index >= length) {
            throw StringIndexOutOfBoundsException(index)
        }
        return value[index]
    }

    fun delete0(start: Int, end: Int) {
        var end = end
        if (start >= 0) {
            if (end > length) {
                end = length
            }
            if (end == start) {
                return
            }
            if (end > start) {
                val count = length - end
                if (count >= 0) java.lang.System.arraycopy(value, end, value, start, count)
                length -= end - start
                return
            }
        }
        throw StringIndexOutOfBoundsException()
    }

    fun deleteCharAt0(location: Int) {
        if (0 > location || location >= length) {
            throw StringIndexOutOfBoundsException(location)
        }
        val count = length - location - 1
        if (count > 0) {
            java.lang.System.arraycopy(value, location + 1, value, location, count)
        }
        length--
    }

    /**
     * Ensures that this object has a minimum capacity available before requiring the internal buffer to be enlarged. The general
     * policy of this method is that if the `minimumCapacity` is larger than the current [.capacity], then the
     * capacity will be increased to the largest value of either the `minimumCapacity` or the current capacity multiplied by
     * two plus two. Although this is the general policy, there is no guarantee that the capacity will change.
     *
     * @param min the new minimum capacity to set.
     */
    fun ensureCapacity(min: Int) {
        if (min > value.size) {
            val twice = (value.size shl 1) + 2
            enlargeBuffer(if (twice > min) twice else min)
        }
    }

    /**
     * Copies the requested sequence of characters to the `char[]` passed starting at `destStart`.
     *
     * @param start     the inclusive start index of the characters to copy.
     * @param end       the exclusive end index of the characters to copy.
     * @param dest      the `char[]` to copy the characters to.
     * @param destStart the inclusive start index of `dest` to begin copying to.
     * @throws IndexOutOfBoundsException if the `start` is negative, the `destStart` is negative, the `start` is
     * greater than `end`, the `end` is greater than the current [.length] or
     * `destStart + end - begin` is greater than `dest.length`.
     */
    fun getChars(start: Int, end: Int, dest: CharArray?, destStart: Int) {
        if (start > length || end > length || start > end) {
            throw StringIndexOutOfBoundsException()
        }
        java.lang.System.arraycopy(value, start, dest, destStart, end - start)
    }

    fun insert0(index: Int, value: CharArray) {
        if (0 > index || index > length) {
            throw StringIndexOutOfBoundsException(index)
        }
        if (value.size != 0) {
            move(value.size, index)
            java.lang.System.arraycopy(value, 0, value, index, value.size)
            length += value.size
        }
    }

    fun insert0(index: Int, value: CharArray, start: Int, length: Int) {
        if (0 <= index && index <= length) {
            // start + length could overflow, start/length maybe MaxInt
            if (start >= 0 && 0 <= length && length <= value.size - start) {
                if (length != 0) {
                    move(length, index)
                    java.lang.System.arraycopy(value, start, this.value, index, length)
                    this.length += length
                }
                return
            }
            throw StringIndexOutOfBoundsException("offset " + start + ", length " + length + ", char[].length " + value.size)
        }
        throw StringIndexOutOfBoundsException(index)
    }

    fun insert0(index: Int, ch: Char) {
        if (0 > index || index > length) {
            // RI compatible exception type
            throw ArrayIndexOutOfBoundsException(index)
        }
        move(1, index)
        value[index] = ch
        length++
    }

    fun insert0(index: Int, string: String?) {
        var string = string
        if (0 <= index && index <= length) {
            if (string == null) {
                string = "null"
            }
            val min = string.length
            if (min != 0) {
                move(min, index)
                string.toCharArray(value, index, 0, min)
                length += min
            }
        } else {
            throw StringIndexOutOfBoundsException(index)
        }
    }

    fun insert0(index: Int, s: CharSequence?, start: Int, end: Int) {
        var s = s
        if (s == null) {
            s = "null"
        }
        if (index < 0 || index > length || start < 0 || end < 0 || start > end || end > s.length) {
            throw IndexOutOfBoundsException()
        }
        insert0(index, s.subSequence(start, end).toString())
    }

    /**
     * The current length.
     *
     * @return the number of characters contained in this instance.
     */
    override fun length(): Int {
        return length
    }

    private fun move(size: Int, index: Int) {
        if (value.size - length >= size) {
            java.lang.System.arraycopy(value, index, value, index + size, length - index) // index == count case is no-op
            return
        }
        val a = length + size
        val b = (value.size shl 1) + 2
        val newSize = if (a > b) a else b
        val newData = CharArray(newSize)
        java.lang.System.arraycopy(value, 0, newData, 0, index)
        // index == count case is no-op
        java.lang.System.arraycopy(value, index, newData, index + size, length - index)
        value = newData
    }

    fun replace0(start: Int, end: Int, string: String?) {
        var end = end
        if (start >= 0) {
            if (end > length) {
                end = length
            }
            if (end > start) {
                val stringLength = string!!.length
                val diff = end - start - stringLength
                if (diff > 0) { // replacing with fewer characters
                    // index == count case is no-op
                    java.lang.System.arraycopy(value, end, value, start + stringLength, length - end)
                } else if (diff < 0) {
                    // replacing with more characters...need some room
                    move(-diff, end)
                }
                string.toCharArray(value, start, 0, stringLength)
                length -= diff
                return
            }
            if (start == end) {
                if (string == null) {
                    throw NullPointerException()
                }
                insert0(start, string)
                return
            }
        }
        throw StringIndexOutOfBoundsException()
    }

    fun reverse0() {
        if (length < 2) {
            return
        }
        var end = length - 1
        var frontHigh = value[0]
        var endLow = value[end]
        var allowFrontSur = true
        var allowEndSur = true
        var i = 0
        val mid = length / 2
        while (i < mid) {
            val frontLow = value[i + 1]
            val endHigh = value[end - 1]
            val surAtFront = allowFrontSur && frontLow.toInt() >= 0xdc00 && frontLow.toInt() <= 0xdfff && frontHigh.toInt() >= 0xd800 && frontHigh.toInt() <= 0xdbff
            if (surAtFront && length < 3) {
                return
            }
            val surAtEnd = allowEndSur && endHigh.toInt() >= 0xd800 && endHigh.toInt() <= 0xdbff && endLow.toInt() >= 0xdc00 && endLow.toInt() <= 0xdfff
            allowEndSur = true
            allowFrontSur = allowEndSur
            if (surAtFront == surAtEnd) {
                if (surAtFront) {
                    // both surrogates
                    value[end] = frontLow
                    value[end - 1] = frontHigh
                    value[i] = endHigh
                    value[i + 1] = endLow
                    frontHigh = value[i + 2]
                    endLow = value[end - 2]
                    i++
                    end--
                } else {
                    // neither surrogates
                    value[end] = frontHigh
                    value[i] = endLow
                    frontHigh = frontLow
                    endLow = endHigh
                }
            } else {
                if (surAtFront) {
                    // surrogate only at the front
                    value[end] = frontLow
                    value[i] = endLow
                    endLow = endHigh
                    allowFrontSur = false
                } else {
                    // surrogate only at the end
                    value[end] = frontHigh
                    value[i] = endHigh
                    frontHigh = frontLow
                    allowEndSur = false
                }
            }
            i++
            --end
        }
        if (length and 1 == 1 && (!allowFrontSur || !allowEndSur)) {
            value[end] = if (allowFrontSur) endLow else frontHigh
        }
    }

    /**
     * Sets the character at the `index`.
     *
     * @param index the zero-based index of the character to replace.
     * @param ch    the character to set.
     * @throws IndexOutOfBoundsException if `index` is negative or greater than or equal to the current [.length].
     */
    fun setCharAt(index: Int, ch: Char) {
        if (0 > index || index >= length) {
            throw StringIndexOutOfBoundsException(index)
        }
        value[index] = ch
    }

    /**
     * Sets the current length to a new value. If the new length is larger than the current length, then the new characters at the
     * end of this object will contain the `char` value of `\u0000`.
     *
     * @param newLength the new length of this StringBuilder.
     * @throws IndexOutOfBoundsException if `length < 0`.
     * @see .length
     */
    fun setLength(newLength: Int) {
        if (newLength < 0) {
            throw StringIndexOutOfBoundsException(newLength)
        }
        if (newLength > value.size) {
            enlargeBuffer(newLength)
        } else {
            if (length < newLength) {
                Arrays.fill(value, length, newLength, 0.toChar())
            }
        }
        length = newLength
    }

    /**
     * Returns the String value of the subsequence from the `start` index to the current end.
     *
     * @param start the inclusive start index to begin the subsequence.
     * @return a String containing the subsequence.
     * @throws StringIndexOutOfBoundsException if `start` is negative or greater than the current [.length].
     */
    fun substring(start: Int): String {
        if (0 <= start && start <= length) {
            return if (start == length) {
                ""
            } else String(value, start, length - start)

            // Remove String sharing for more performance
        }
        throw StringIndexOutOfBoundsException(start)
    }

    /**
     * Returns the String value of the subsequence from the `start` index to the `end` index.
     *
     * @param start the inclusive start index to begin the subsequence.
     * @param end   the exclusive end index to end the subsequence.
     * @return a String containing the subsequence.
     * @throws StringIndexOutOfBoundsException if `start` is negative, greater than `end` or if `end` is greater
     * than the current [.length].
     */
    fun substring(start: Int, end: Int): String {
        if (0 <= start && start <= end && end <= length) {
            return if (start == end) {
                ""
            } else String(value, start, end - start)

            // Remove String sharing for more performance
        }
        throw StringIndexOutOfBoundsException()
    }

    /**
     * Returns the current String representation.
     *
     * @return a String containing the characters in this instance.
     */
    override fun toString(): String {
        return if (length == 0) "" else String(value, 0, length)
    }

    /**
     * Returns a `CharSequence` of the subsequence from the `start` index to the `end` index.
     *
     * @param start the inclusive start index to begin the subsequence.
     * @param end   the exclusive end index to end the subsequence.
     * @return a CharSequence containing the subsequence.
     * @throws IndexOutOfBoundsException if `start` is negative, greater than `end` or if `end` is greater than
     * the current [.length].
     * @since 1.4
     */
    override fun subSequence(start: Int, end: Int): CharSequence {
        return substring(start, end)
    }

    /**
     * Searches for the first index of the specified character. The search for the character starts at the beginning and moves
     * towards the end.
     *
     * @param string the string to find.
     * @return the index of the specified character, -1 if the character isn't found.
     * @see .lastIndexOf
     * @since 1.4
     */
    fun indexOf(string: String): Int {
        return indexOf(string, 0)
    }

    /**
     * Searches for the index of the specified character. The search for the character starts at the specified offset and moves
     * towards the end.
     *
     * @param subString the string to find.
     * @param start     the starting offset.
     * @return the index of the specified character, -1 if the character isn't found
     * @see .lastIndexOf
     * @since 1.4
     */
    fun indexOf(subString: String, start: Int): Int {
        var start = start
        if (start < 0) {
            start = 0
        }
        val subCount = subString.length
        if (subCount == 0) return if (start < length || start == 0) start else length
        val maxIndex = length - subCount
        if (start > maxIndex) return -1
        val firstChar = subString[0]
        while (true) {
            var i = start
            var found = false
            while (i <= maxIndex) {
                if (value[i] == firstChar) {
                    found = true
                    break
                }
                i++
            }
            if (!found) return -1
            var o1 = i
            var o2 = 0
            while (++o2 < subCount && value[++o1] == subString[o2]) {
                // Intentionally empty
            }
            if (o2 == subCount) return i
            start = i + 1
        }
    }

    fun indexOfIgnoreCase(subString: String, start: Int): Int {
        var start = start
        if (start < 0) {
            start = 0
        }
        val subCount = subString.length
        if (subCount == 0) return if (start < length || start == 0) start else length
        val maxIndex = length - subCount
        if (start > maxIndex) return -1
        val firstUpper: Char = java.lang.Character.toUpperCase(subString[0])
        val firstLower: Char = java.lang.Character.toLowerCase(firstUpper)
        while (true) {
            var i = start
            var found = false
            while (i <= maxIndex) {
                val c = value[i]
                if (c == firstUpper || c == firstLower) {
                    found = true
                    break
                }
                i++
            }
            if (!found) return -1
            var o1 = i
            var o2 = 0
            while (++o2 < subCount) {
                val c = value[++o1]
                val upper: Char = java.lang.Character.toUpperCase(subString[o2])
                if (c != upper && c != java.lang.Character.toLowerCase(upper)) break
            }
            if (o2 == subCount) return i
            start = i + 1
        }
    }

    operator fun contains(subString: String): Boolean {
        return indexOf(subString, 0) != -1
    }

    fun containsIgnoreCase(subString: String): Boolean {
        return indexOfIgnoreCase(subString, 0) != -1
    }

    /**
     * Searches for the last index of the specified character. The search for the character starts at the end and moves towards
     * the beginning.
     *
     * @param string the string to find.
     * @return the index of the specified character, -1 if the character isn't found.
     * @throws NullPointerException if `string` is `null`.
     * @see String.lastIndexOf
     * @since 1.4
     */
    fun lastIndexOf(string: String): Int {
        return lastIndexOf(string, length)
    }

    /**
     * Searches for the index of the specified character. The search for the character starts at the specified offset and moves
     * towards the beginning.
     *
     * @param subString the string to find.
     * @param start     the starting offset.
     * @return the index of the specified character, -1 if the character isn't found.
     * @throws NullPointerException if `subString` is `null`.
     * @see String.lastIndexOf
     * @since 1.4
     */
    fun lastIndexOf(subString: String, start: Int): Int {
        var start = start
        val subCount = subString.length
        if (subCount <= length && start >= 0) {
            if (subCount > 0) {
                if (start > length - subCount) {
                    start = length - subCount // count and subCount are both
                }
                // >= 1
                val firstChar = subString[0]
                while (true) {
                    var i = start
                    var found = false
                    while (i >= 0) {
                        if (value[i] == firstChar) {
                            found = true
                            break
                        }
                        --i
                    }
                    if (!found) {
                        return -1
                    }
                    var o1 = i
                    var o2 = 0
                    while (++o2 < subCount && value[++o1] == subString[o2]) {
                        // Intentionally empty
                    }
                    if (o2 == subCount) {
                        return i
                    }
                    start = i - 1
                }
            }
            return if (start < length) start else length
        }
        return -1
    }

    /**
     * Trims off any extra capacity beyond the current length. Note, this method is NOT guaranteed to change the capacity of this
     * object.
     *
     * @since 1.5
     */
    fun trimToSize() {
        if (length < value.size) {
            val newValue = CharArray(length)
            java.lang.System.arraycopy(value, 0, newValue, 0, length)
            value = newValue
        }
    }

    /**
     * Retrieves the Unicode code point value at the `index`.
     *
     * @param index the index to the `char` code unit.
     * @return the Unicode code point value.
     * @throws IndexOutOfBoundsException if `index` is negative or greater than or equal to [.length].
     * @see Character
     *
     * @see Character.codePointAt
     * @since 1.5
     */
    fun codePointAt(index: Int): Int {
        if (index < 0 || index >= length) {
            throw StringIndexOutOfBoundsException(index)
        }
        return java.lang.Character.codePointAt(value, index, length)
    }

    /**
     * Retrieves the Unicode code point value that precedes the `index`.
     *
     * @param index the index to the `char` code unit within this object.
     * @return the Unicode code point value.
     * @throws IndexOutOfBoundsException if `index` is less than 1 or greater than [.length].
     * @see Character
     *
     * @see Character.codePointBefore
     * @since 1.5
     */
    fun codePointBefore(index: Int): Int {
        if (index < 1 || index > length) {
            throw StringIndexOutOfBoundsException(index)
        }
        return java.lang.Character.codePointBefore(value, index)
    }

    /**
     * Calculates the number of Unicode code points between `beginIndex` and `endIndex`.
     *
     * @param beginIndex the inclusive beginning index of the subsequence.
     * @param endIndex   the exclusive end index of the subsequence.
     * @return the number of Unicode code points in the subsequence.
     * @throws IndexOutOfBoundsException if `beginIndex` is negative or greater than `endIndex` or `endIndex` is
     * greater than [.length].
     * @see Character
     *
     * @see Character.codePointCount
     * @since 1.5
     */
    fun codePointCount(beginIndex: Int, endIndex: Int): Int {
        if (beginIndex < 0 || endIndex > length || beginIndex > endIndex) {
            throw StringIndexOutOfBoundsException()
        }
        return java.lang.Character.codePointCount(value, beginIndex, endIndex - beginIndex)
    }

    /**
     * Returns the index that is offset `codePointOffset` code points from `index`.
     *
     * @param index           the index to calculate the offset from.
     * @param codePointOffset the number of code points to count.
     * @return the index that is `codePointOffset` code points away from index.
     * @throws IndexOutOfBoundsException if `index` is negative or greater than [.length] or if there aren't enough
     * code points before or after `index` to match `codePointOffset`.
     * @see Character
     *
     * @see Character.offsetByCodePoints
     * @since 1.5
     */
    fun offsetByCodePoints(index: Int, codePointOffset: Int): Int {
        return java.lang.Character.offsetByCodePoints(value, 0, length, index, codePointOffset)
    }

    /**
     * Appends the string representation of the specified `boolean` value. The `boolean` value is converted to a
     * String according to the rule defined by [String.valueOf].
     *
     * @param b the `boolean` value to append.
     * @return this builder.
     * @see String.valueOf
     */
    fun append(b: Boolean): StringBuilder {
        append0(if (b) "true" else "false") //$NON-NLS-1$ //$NON-NLS-2$
        return this
    }

    /**
     * Appends the string representation of the specified `char` value. The `char` value is converted to a string
     * according to the rule defined by [String.valueOf].
     *
     * @param c the `char` value to append.
     * @return this builder.
     * @see String.valueOf
     */
    override fun append(c: Char): StringBuilder {
        append0(c)
        return this
    }
    /**
     * Appends the string representation of the specified `int` value. The `int` value is converted to a string
     * without memory allocation.
     *
     * @param value     the `int` value to append.
     * @param minLength the minimum number of characters to add
     * @param prefix    the character to use as prefix
     * @return this builder.
     * @see String.valueOf
     */
    /**
     * Appends the string representation of the specified `int` value. The `int` value is converted to a string
     * without memory allocation.
     *
     * @param value     the `int` value to append.
     * @param minLength the minimum number of characters to add
     * @return this builder.
     * @see String.valueOf
     */
    /**
     * Appends the string representation of the specified `int` value. The `int` value is converted to a string
     * without memory allocation.
     *
     * @param value the `int` value to append.
     * @return this builder.
     * @see String.valueOf
     */
    @JvmOverloads
    fun append(value: Int, minLength: Int = 0, prefix: Char = '0'): StringBuilder {
        var value = value
        if (value == Int.MIN_VALUE) {
            append0("-2147483648")
            return this
        }
        if (value < 0) {
            append0('-')
            value = -value
        }
        if (minLength > 1) {
            for (j in minLength - numChars(value, 10) downTo 1) append(prefix)
        }
        if (value >= 10000) {
            if (value >= 1000000000) append0(digits[(value.toLong() % 10000000000L / 1000000000L).toInt()])
            if (value >= 100000000) append0(digits[value % 1000000000 / 100000000])
            if (value >= 10000000) append0(digits[value % 100000000 / 10000000])
            if (value >= 1000000) append0(digits[value % 10000000 / 1000000])
            if (value >= 100000) append0(digits[value % 1000000 / 100000])
            append0(digits[value % 100000 / 10000])
        }
        if (value >= 1000) append0(digits[value % 10000 / 1000])
        if (value >= 100) append0(digits[value % 1000 / 100])
        if (value >= 10) append0(digits[value % 100 / 10])
        append0(digits[value % 10])
        return this
    }
    /**
     * Appends the string representation of the specified `long` value. The `long` value is converted to a string
     * without memory allocation.
     *
     * @param value     the `long` value.
     * @param minLength the minimum number of characters to add
     * @param prefix    the character to use as prefix
     * @return this builder.
     */
    /**
     * Appends the string representation of the specified `long` value. The `long` value is converted to a string
     * without memory allocation.
     *
     * @param value     the `long` value.
     * @param minLength the minimum number of characters to add
     * @return this builder.
     */
    /**
     * Appends the string representation of the specified `long` value. The `long` value is converted to a string
     * without memory allocation.
     *
     * @param value the `long` value.
     * @return this builder.
     */
    @JvmOverloads
    fun append(value: Long, minLength: Int = 0, prefix: Char = '0'): StringBuilder {
        var value = value
        if (value == Long.MIN_VALUE) {
            append0("-9223372036854775808")
            return this
        }
        if (value < 0L) {
            append0('-')
            value = -value
        }
        if (minLength > 1) {
            for (j in minLength - numChars(value, 10) downTo 1) append(prefix)
        }
        if (value >= 10000) {
            if (value >= 1000000000000000000L) append0(digits[(value % 10000000000000000000.0 / 1000000000000000000L).toInt()])
            if (value >= 100000000000000000L) append0(digits[(value % 1000000000000000000L / 100000000000000000L).toInt()])
            if (value >= 10000000000000000L) append0(digits[(value % 100000000000000000L / 10000000000000000L).toInt()])
            if (value >= 1000000000000000L) append0(digits[(value % 10000000000000000L / 1000000000000000L).toInt()])
            if (value >= 100000000000000L) append0(digits[(value % 1000000000000000L / 100000000000000L).toInt()])
            if (value >= 10000000000000L) append0(digits[(value % 100000000000000L / 10000000000000L).toInt()])
            if (value >= 1000000000000L) append0(digits[(value % 10000000000000L / 1000000000000L).toInt()])
            if (value >= 100000000000L) append0(digits[(value % 1000000000000L / 100000000000L).toInt()])
            if (value >= 10000000000L) append0(digits[(value % 100000000000L / 10000000000L).toInt()])
            if (value >= 1000000000L) append0(digits[(value % 10000000000L / 1000000000L).toInt()])
            if (value >= 100000000L) append0(digits[(value % 1000000000L / 100000000L).toInt()])
            if (value >= 10000000L) append0(digits[(value % 100000000L / 10000000L).toInt()])
            if (value >= 1000000L) append0(digits[(value % 10000000L / 1000000L).toInt()])
            if (value >= 100000L) append0(digits[(value % 1000000L / 100000L).toInt()])
            append0(digits[(value % 100000L / 10000L).toInt()])
        }
        if (value >= 1000L) append0(digits[(value % 10000L / 1000L).toInt()])
        if (value >= 100L) append0(digits[(value % 1000L / 100L).toInt()])
        if (value >= 10L) append0(digits[(value % 100L / 10L).toInt()])
        append0(digits[(value % 10L).toInt()])
        return this
    }

    /**
     * Appends the string representation of the specified `float` value. The `float` value is converted to a string
     * according to the rule defined by [String.valueOf].
     *
     * @param f the `float` value to append.
     * @return this builder.
     */
    fun append(f: Float): StringBuilder {
        append0(java.lang.Float.toString(f))
        return this
    }

    /**
     * Appends the string representation of the specified `double` value. The `double` value is converted to a string
     * according to the rule defined by [String.valueOf].
     *
     * @param d the `double` value to append.
     * @return this builder.
     * @see String.valueOf
     */
    fun append(d: Double): StringBuilder {
        append0(java.lang.Double.toString(d))
        return this
    }

    /**
     * Appends the string representation of the specified `Object`. The `Object` value is converted to a string
     * according to the rule defined by [String.valueOf].
     *
     * @param obj the `Object` to append.
     * @return this builder.
     * @see String.valueOf
     */
    fun append(obj: Any?): StringBuilder {
        if (obj == null) {
            appendNull()
        } else {
            append0(obj.toString())
        }
        return this
    }

    /**
     * Appends the contents of the specified string. If the string is `null`, then the string `"null"` is appended.
     *
     * @param str the string to append.
     * @return this builder.
     */
    fun append(str: String?): StringBuilder {
        append0(str)
        return this
    }

    /**
     * Appends the contents of the specified string, then create a new line. If the string is `null`, then the string
     * `"null"` is appended.
     *
     * @param str the string to append.
     * @return this builder.
     */
    fun appendLine(str: String?): StringBuilder {
        append0(str)
        append0('\n')
        return this
    }

    /**
     * Appends the string representation of the specified `char[]`. The `char[]` is converted to a string according to
     * the rule defined by [String.valueOf].
     *
     * @param ch the `char[]` to append..
     * @return this builder.
     * @see String.valueOf
     */
    fun append(ch: CharArray): StringBuilder {
        append0(ch)
        return this
    }

    /**
     * Appends the string representation of the specified subset of the `char[]`. The `char[]` value is converted to a
     * String according to the rule defined by [String.valueOf].
     *
     * @param str    the `char[]` to append.
     * @param offset the inclusive offset index.
     * @param len    the number of characters.
     * @return this builder.
     * @throws ArrayIndexOutOfBoundsException if `offset` and `len` do not specify a valid subsequence.
     * @see String.valueOf
     */
    fun append(str: CharArray, offset: Int, len: Int): StringBuilder {
        append0(str, offset, len)
        return this
    }

    /**
     * Appends the string representation of the specified `CharSequence`. If the `CharSequence` is `null`, then
     * the string `"null"` is appended.
     *
     * @param csq the `CharSequence` to append.
     * @return this builder.
     */
    override fun append(csq: CharSequence): StringBuilder {
        if (csq == null) {
            appendNull()
        } else if (csq is StringBuilder) {
            val builder = csq
            append0(builder.value, 0, builder.length)
        } else {
            append0(csq.toString())
        }
        return this
    }

    fun append(builder: StringBuilder?): StringBuilder {
        if (builder == null) appendNull() else append0(builder.value, 0, builder.length)
        return this
    }

    /**
     * Appends the string representation of the specified subsequence of the `CharSequence`. If the `CharSequence` is
     * `null`, then the string `"null"` is used to extract the subsequence from.
     *
     * @param csq   the `CharSequence` to append.
     * @param start the beginning index.
     * @param end   the ending index.
     * @return this builder.
     * @throws IndexOutOfBoundsException if `start` or `end` are negative, `start` is greater than `end` or
     * `end` is greater than the length of `csq`.
     */
    override fun append(csq: CharSequence, start: Int, end: Int): StringBuilder {
        append0(csq, start, end)
        return this
    }

    fun append(builder: StringBuilder?, start: Int, end: Int): StringBuilder {
        if (builder == null) appendNull() else append0(builder.value, start, end)
        return this
    }

    /**
     * Appends the encoded Unicode code point. The code point is converted to a `char[]` as defined by
     * [Character.toChars].
     *
     * @param codePoint the Unicode code point to encode and append.
     * @return this builder.
     * @see Character.toChars
     */
    fun appendCodePoint(codePoint: Int): StringBuilder {
        append0(java.lang.Character.toChars(codePoint))
        return this
    }

    /**
     * Deletes a sequence of characters specified by `start` and `end`. Shifts any remaining characters to the left.
     *
     * @param start the inclusive start index.
     * @param end   the exclusive end index.
     * @return this builder.
     * @throws StringIndexOutOfBoundsException if `start` is less than zero, greater than the current length or greater than
     * `end`.
     */
    fun delete(start: Int, end: Int): StringBuilder {
        delete0(start, end)
        return this
    }

    /**
     * Deletes the character at the specified index. shifts any remaining characters to the left.
     *
     * @param index the index of the character to delete.
     * @return this builder.
     * @throws StringIndexOutOfBoundsException if `index` is less than zero or is greater than or equal to the current
     * length.
     */
    fun deleteCharAt(index: Int): StringBuilder {
        deleteCharAt0(index)
        return this
    }

    /**
     * Sets length to 0.
     */
    fun clear() {
        length = 0
    }

    /**
     * Inserts the string representation of the specified `boolean` value at the specified `offset`. The
     * `boolean` value is converted to a string according to the rule defined by [String.valueOf].
     *
     * @param offset the index to insert at.
     * @param b      the `boolean` value to insert.
     * @return this builder.
     * @throws StringIndexOutOfBoundsException if `offset` is negative or greater than the current `length`.
     * @see String.valueOf
     */
    fun insert(offset: Int, b: Boolean): StringBuilder {
        insert0(offset, if (b) "true" else "false") //$NON-NLS-1$ //$NON-NLS-2$
        return this
    }

    /**
     * Inserts the string representation of the specified `char` value at the specified `offset`. The `char`
     * value is converted to a string according to the rule defined by [String.valueOf].
     *
     * @param offset the index to insert at.
     * @param c      the `char` value to insert.
     * @return this builder.
     * @throws IndexOutOfBoundsException if `offset` is negative or greater than the current `length()`.
     * @see String.valueOf
     */
    fun insert(offset: Int, c: Char): StringBuilder {
        insert0(offset, c)
        return this
    }

    /**
     * Inserts the string representation of the specified `int` value at the specified `offset`. The `int` value
     * is converted to a String according to the rule defined by [String.valueOf].
     *
     * @param offset the index to insert at.
     * @param i      the `int` value to insert.
     * @return this builder.
     * @throws StringIndexOutOfBoundsException if `offset` is negative or greater than the current `length()`.
     * @see String.valueOf
     */
    fun insert(offset: Int, i: Int): StringBuilder {
        insert0(offset, java.lang.Integer.toString(i))
        return this
    }

    /**
     * Inserts the string representation of the specified `long` value at the specified `offset`. The `long`
     * value is converted to a String according to the rule defined by [String.valueOf].
     *
     * @param offset the index to insert at.
     * @param l      the `long` value to insert.
     * @return this builder.
     * @throws StringIndexOutOfBoundsException if `offset` is negative or greater than the current {code length()}.
     * @see String.valueOf
     */
    fun insert(offset: Int, l: Long): StringBuilder {
        insert0(offset, java.lang.Long.toString(l))
        return this
    }

    /**
     * Inserts the string representation of the specified `float` value at the specified `offset`. The `float`
     * value is converted to a string according to the rule defined by [String.valueOf].
     *
     * @param offset the index to insert at.
     * @param f      the `float` value to insert.
     * @return this builder.
     * @throws StringIndexOutOfBoundsException if `offset` is negative or greater than the current `length()`.
     * @see String.valueOf
     */
    fun insert(offset: Int, f: Float): StringBuilder {
        insert0(offset, java.lang.Float.toString(f))
        return this
    }

    /**
     * Inserts the string representation of the specified `double` value at the specified `offset`. The `double`
     * value is converted to a String according to the rule defined by [String.valueOf].
     *
     * @param offset the index to insert at.
     * @param d      the `double` value to insert.
     * @return this builder.
     * @throws StringIndexOutOfBoundsException if `offset` is negative or greater than the current `length()`.
     * @see String.valueOf
     */
    fun insert(offset: Int, d: Double): StringBuilder {
        insert0(offset, java.lang.Double.toString(d))
        return this
    }

    /**
     * Inserts the string representation of the specified `Object` at the specified `offset`. The `Object` value
     * is converted to a String according to the rule defined by [String.valueOf].
     *
     * @param offset the index to insert at.
     * @param obj    the `Object` to insert.
     * @return this builder.
     * @throws StringIndexOutOfBoundsException if `offset` is negative or greater than the current `length()`.
     * @see String.valueOf
     */
    fun insert(offset: Int, obj: Any?): StringBuilder {
        insert0(offset, obj?.toString() ?: "null") //$NON-NLS-1$
        return this
    }

    /**
     * Inserts the specified string at the specified `offset`. If the specified string is null, then the String
     * `"null"` is inserted.
     *
     * @param offset the index to insert at.
     * @param str    the `String` to insert.
     * @return this builder.
     * @throws StringIndexOutOfBoundsException if `offset` is negative or greater than the current `length()`.
     */
    fun insert(offset: Int, str: String?): StringBuilder {
        insert0(offset, str)
        return this
    }

    /**
     * Inserts the string representation of the specified `char[]` at the specified `offset`. The `char[]` value
     * is converted to a String according to the rule defined by [String.valueOf].
     *
     * @param offset the index to insert at.
     * @param ch     the `char[]` to insert.
     * @return this builder.
     * @throws StringIndexOutOfBoundsException if `offset` is negative or greater than the current `length()`.
     * @see String.valueOf
     */
    fun insert(offset: Int, ch: CharArray): StringBuilder {
        insert0(offset, ch)
        return this
    }

    /**
     * Inserts the string representation of the specified subsequence of the `char[]` at the specified `offset`. The
     * `char[]` value is converted to a String according to the rule defined by [String.valueOf].
     *
     * @param offset    the index to insert at.
     * @param str       the `char[]` to insert.
     * @param strOffset the inclusive index.
     * @param strLen    the number of characters.
     * @return this builder.
     * @throws StringIndexOutOfBoundsException if `offset` is negative or greater than the current `length()`, or
     * `strOffset` and `strLen` do not specify a valid subsequence.
     * @see String.valueOf
     */
    fun insert(offset: Int, str: CharArray, strOffset: Int, strLen: Int): StringBuilder {
        insert0(offset, str, strOffset, strLen)
        return this
    }

    /**
     * Inserts the string representation of the specified `CharSequence` at the specified `offset`. The
     * `CharSequence` is converted to a String as defined by [CharSequence.toString]. If `s` is `null`,
     * then the String `"null"` is inserted.
     *
     * @param offset the index to insert at.
     * @param s      the `CharSequence` to insert.
     * @return this builder.
     * @throws IndexOutOfBoundsException if `offset` is negative or greater than the current `length()`.
     * @see CharSequence.toString
     */
    fun insert(offset: Int, s: CharSequence?): StringBuilder {
        insert0(offset, s?.toString() ?: "null") //$NON-NLS-1$
        return this
    }

    /**
     * Inserts the string representation of the specified subsequence of the `CharSequence` at the specified `offset`.
     * The `CharSequence` is converted to a String as defined by [CharSequence.subSequence]. If the
     * `CharSequence` is `null`, then the string `"null"` is used to determine the subsequence.
     *
     * @param offset the index to insert at.
     * @param s      the `CharSequence` to insert.
     * @param start  the start of the subsequence of the character sequence.
     * @param end    the end of the subsequence of the character sequence.
     * @return this builder.
     * @throws IndexOutOfBoundsException if `offset` is negative or greater than the current `length()`, or
     * `start` and `end` do not specify a valid subsequence.
     * @see CharSequence.subSequence
     */
    fun insert(offset: Int, s: CharSequence?, start: Int, end: Int): StringBuilder {
        insert0(offset, s, start, end)
        return this
    }

    /**
     * Replaces the specified subsequence in this builder with the specified string.
     *
     * @param start the inclusive begin index.
     * @param end   the exclusive end index.
     * @param str   the replacement string.
     * @return this builder.
     * @throws StringIndexOutOfBoundsException if `start` is negative, greater than the current `length()` or greater
     * than `end`.
     * @throws NullPointerException            if `str` is `null`.
     */
    fun replace(start: Int, end: Int, str: String?): StringBuilder {
        replace0(start, end, str)
        return this
    }

    /**
     * Replaces all instances of `find` with `replace`.
     */
    fun replace(find: String, replace: String): StringBuilder {
        val findLength = find.length
        val replaceLength = replace.length
        var index = 0
        while (true) {
            index = indexOf(find, index)
            if (index == -1) break
            replace0(index, index + findLength, replace)
            index += replaceLength
        }
        return this
    }

    /**
     * Replaces all instances of `find` with `replace`.
     */
    fun replace(find: Char, replace: String): StringBuilder {
        val replaceLength = replace.length
        var index = 0
        while (true) {
            while (true) {
                if (index == length) return this
                if (value[index] == find) break
                index++
            }
            replace0(index, index + 1, replace)
            index += replaceLength
        }
    }

    /**
     * Reverses the order of characters in this builder.
     *
     * @return this buffer.
     */
    fun reverse(): StringBuilder {
        reverse0()
        return this
    }

    val isEmpty: Boolean
        get() = length == 0

    fun notEmpty(): Boolean {
        return length != 0
    }

    override fun hashCode(): Int {
        val prime = 31
        var result = 1
        result = prime + length
        result = prime * result + Arrays.hashCode(value)
        return result
    }

    override fun equals(obj: Any?): Boolean {
        if (this === obj) return true
        if (obj == null) return false
        if (javaClass != obj.javaClass) return false
        val other = obj as StringBuilder
        val length = length
        if (length != other.length) return false
        val chars = value
        val chars2 = other.value
        for (i in 0 until length) if (chars[i] != chars2[i]) return false
        return true
    }

    /**
     * @param other May be null.
     */
    fun equalsIgnoreCase(other: StringBuilder?): Boolean {
        if (this === other) return true
        if (other == null) return false
        val length = length
        if (length != other.length) return false
        val chars = value
        val chars2 = other.value
        for (i in 0 until length) {
            val c = chars[i]
            val upper: Char = java.lang.Character.toUpperCase(chars2[i])
            if (c != upper && c != java.lang.Character.toLowerCase(upper)) return false
        }
        return true
    }

    /**
     * @param other May be null.
     */
    fun equalsIgnoreCase(other: String?): Boolean {
        if (other == null) return false
        val length = length
        if (length != other.length) return false
        val chars = value
        for (i in 0 until length) {
            val c = chars[i]
            val upper: Char = java.lang.Character.toUpperCase(other[i])
            if (c != upper && c != java.lang.Character.toLowerCase(upper)) return false
        }
        return true
    }

    companion object {
        const val INITIAL_CAPACITY = 16
        private val digits = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9')

        /**
         * @return the number of characters required to represent the specified value with the specified radix
         */
        fun numChars(value: Int, radix: Int): Int {
            var value = value
            var result = if (value < 0) 2 else 1
            while (radix.let { value /= it; value } != 0) ++result
            return result
        }

        /**
         * @return the number of characters required to represent the specified value with the specified radix
         */
        fun numChars(value: Long, radix: Int): Int {
            var value = value
            var result = if (value < 0) 2 else 1
            while (radix.let { value /= it; value } != 0L) ++result
            return result
        }
    }
}
