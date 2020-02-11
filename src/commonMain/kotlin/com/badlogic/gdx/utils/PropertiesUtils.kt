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

import com.badlogic.gdx.utils.Pools
import com.badlogic.gdx.utils.Predicate.PredicateIterator
import com.badlogic.gdx.utils.Queue.QueueIterable
import com.badlogic.gdx.utils.Queue.QueueIterator
import com.badlogic.gdx.utils.Scaling
import com.badlogic.gdx.utils.ScreenUtils
import java.lang.IndexOutOfBoundsException

import kotlin.jvm.Throws

/**
 * `PropertiesUtils` is a helper class that allows you to load and store key/value pairs of an
 * `ObjectMap<String,String>` with the same line-oriented syntax supported by `java.util.Properties`.
 */
object PropertiesUtils {

    private const val NONE = 0
    private const val SLASH = 1
    private const val UNICODE = 2
    private const val CONTINUE = 3
    private const val KEY_DONE = 4
    private const val IGNORE = 5
    private const val LINE_SEPARATOR = "\n"

    /**
     * Adds to the specified `ObjectMap` the key/value pairs loaded from the `Reader` in a simple line-oriented format
     * compatible with `java.util.Properties`.
     *
     *
     * The input stream remains open after this method returns.
     *
     * @param properties the map to be filled.
     * @param reader     the input character stream reader.
     * @throws IOException              if an error occurred when reading from the input stream.
     * @throws IllegalArgumentException if a malformed Unicode escape appears in the input.
     */
    @Throws(IOException::class)
    fun load(properties: ObjectMap<String?, String?>?, reader: Reader?) {
        if (properties == null) throw NullPointerException("ObjectMap cannot be null")
        if (reader == null) throw NullPointerException("Reader cannot be null")
        var mode = NONE
        var unicode = 0
        var count = 0
        var nextChar: Char
        var buf = CharArray(40)
        var offset = 0
        var keyLength = -1
        var intVal: Int
        var firstChar = true
        val br = BufferedReader(reader)
        while (true) {
            intVal = br.read()
            if (intVal == -1) {
                break
            }
            nextChar = intVal.toChar()
            if (offset == buf.size) {
                val newBuf = CharArray(buf.size * 2)
                java.lang.System.arraycopy(buf, 0, newBuf, 0, offset)
                buf = newBuf
            }
            if (mode == UNICODE) {
                val digit: Int = java.lang.Character.digit(nextChar, 16)
                if (digit >= 0) {
                    unicode = (unicode shl 4) + digit
                    if (++count < 4) {
                        continue
                    }
                } else if (count <= 4) {
                    throw IllegalArgumentException("Invalid Unicode sequence: illegal character")
                }
                mode = NONE
                buf[offset++] = unicode.toChar()
                if (nextChar != '\n') {
                    continue
                }
            }
            if (mode == SLASH) {
                mode = NONE
                when (nextChar) {
                    '\r' -> {
                        mode = CONTINUE // Look for a following \n
                        continue
                    }
                    '\n' -> {
                        mode = IGNORE // Ignore whitespace on the next line
                        continue
                    }
                    'b' -> nextChar = '\b'
                    'f' -> nextChar = '\f'
                    'n' -> nextChar = '\n'
                    'r' -> nextChar = '\r'
                    't' -> nextChar = '\t'
                    'u' -> {
                        mode = UNICODE
                        run {
                            count = 0
                            unicode = count
                        }
                        continue
                    }
                }
            } else {
                when (nextChar) {
                    '#', '!' -> if (firstChar) {
                        while (true) {
                            intVal = br.read()
                            if (intVal == -1) {
                                break
                            }
                            nextChar = intVal.toChar()
                            if (nextChar == '\r' || nextChar == '\n') {
                                break
                            }
                        }
                        continue
                    }
                    '\n' -> {
                        if (mode == CONTINUE) { // Part of a \r\n sequence
                            mode = IGNORE // Ignore whitespace on the next line
                            continue
                        }
                        mode = NONE
                        firstChar = true
                        if (offset > 0 || offset == 0 && keyLength == 0) {
                            if (keyLength == -1) {
                                keyLength = offset
                            }
                            val temp = String(buf, 0, offset)
                            properties.put(temp.substring(0, keyLength), temp.substring(keyLength))
                        }
                        keyLength = -1
                        offset = 0
                        continue
                    }
                    '\r' -> {
                        mode = NONE
                        firstChar = true
                        if (offset > 0 || offset == 0 && keyLength == 0) {
                            if (keyLength == -1) {
                                keyLength = offset
                            }
                            val temp = String(buf, 0, offset)
                            properties.put(temp.substring(0, keyLength), temp.substring(keyLength))
                        }
                        keyLength = -1
                        offset = 0
                        continue
                    }
                    '\\' -> {
                        if (mode == KEY_DONE) {
                            keyLength = offset
                        }
                        mode = SLASH
                        continue
                    }
                    ':', '=' -> if (keyLength == -1) { // if parsing the key
                        mode = NONE
                        keyLength = offset
                        continue
                    }
                }
                // if (Character.isWhitespace(nextChar)) { <-- not supported by GWT; replaced with isSpace.
                if (java.lang.Character.isSpace(nextChar)) {
                    if (mode == CONTINUE) {
                        mode = IGNORE
                    }
                    // if key length == 0 or value length == 0
                    if (offset == 0 || offset == keyLength || mode == IGNORE) {
                        continue
                    }
                    if (keyLength == -1) { // if parsing the key
                        mode = KEY_DONE
                        continue
                    }
                }
                if (mode == IGNORE || mode == CONTINUE) {
                    mode = NONE
                }
            }
            firstChar = false
            if (mode == KEY_DONE) {
                keyLength = offset
                mode = NONE
            }
            buf[offset++] = nextChar
        }
        if (mode == UNICODE && count <= 4) {
            throw IllegalArgumentException("Invalid Unicode sequence: expected format \\uxxxx")
        }
        if (keyLength == -1 && offset > 0) {
            keyLength = offset
        }
        if (keyLength >= 0) {
            val temp = String(buf, 0, offset)
            val key = temp.substring(0, keyLength)
            var value = temp.substring(keyLength)
            if (mode == SLASH) {
                value += "\u0000"
            }
            properties.put(key, value)
        }
    }

    /**
     * Writes the key/value pairs of the specified `ObjectMap` to the output character stream in a simple line-oriented
     * format compatible with `java.util.Properties`.
     *
     *
     * Every entry in the `ObjectMap` is written out, one per line. For each entry the key string is written, then an
     * ASCII `=`, then the associated element string. For the key, all space characters are written with a preceding
     * `\` character. For the element, leading space characters, but not embedded or trailing space characters, are
     * written with a preceding `\` character. The key and element characters `#`, `!`,
     * `=`, and `:` are written with a preceding backslash to ensure that they are properly loaded.
     *
     *
     * After the entries have been written, the output stream is flushed. The output stream remains open after this method returns.
     *
     * @param properties the `ObjectMap`.
     * @param writer     an output character stream writer.
     * @param comment    an optional comment to be written, or null.
     * @throws IOException          if writing this property list to the specified output stream throws an <tt>IOException</tt>.
     * @throws NullPointerException if `writer` is null.
     */
    @Throws(IOException::class)
    fun store(properties: ObjectMap<String, String>, writer: Writer, comment: String?) {
        storeImpl(properties, writer, comment, false)
    }

    @Throws(IOException::class)
    private fun storeImpl(properties: ObjectMap<String, String>, writer: Writer, comment: String?, escapeUnicode: Boolean) {
        comment?.let { writeComment(writer, it) }
        writer.write("#")
        writer.write(Date().toString())
        writer.write(LINE_SEPARATOR)
        val sb = StringBuilder(200)
        for (entry in properties.entries()!!) {
            dumpString(sb, entry.key, true, escapeUnicode)
            sb.append('=')
            dumpString(sb, entry.value, false, escapeUnicode)
            writer.write(LINE_SEPARATOR)
            writer.write(sb.toString())
            sb.setLength(0)
        }
        writer.flush()
    }

    private fun dumpString(outBuffer: StringBuilder, string: String, escapeSpace: Boolean, escapeUnicode: Boolean) {
        val len = string.length
        for (i in 0 until len) {
            val ch = string[i]
            // Handle common case first
            if (ch.toInt() > 61 && ch.toInt() < 127) {
                outBuffer.append(if (ch == '\\') "\\\\" else ch)
                continue
            }
            when (ch) {
                ' ' -> if (i == 0 || escapeSpace) {
                    outBuffer.append("\\ ")
                } else {
                    outBuffer.append(ch)
                }
                '\n' -> outBuffer.append("\\n")
                '\r' -> outBuffer.append("\\r")
                '\t' -> outBuffer.append("\\t")
                '\f' -> outBuffer.append("\\f")
                '=', ':', '#', '!' -> outBuffer.append('\\').append(ch)
                else -> if ((ch.toInt() < 0x0020 || ch.toInt() > 0x007e) and escapeUnicode) {
                    val hex: String = java.lang.Integer.toHexString(ch.toInt())
                    outBuffer.append("\\u")
                    var j = 0
                    while (j < 4 - hex.length) {
                        outBuffer.append('0')
                        j++
                    }
                    outBuffer.append(hex)
                } else {
                    outBuffer.append(ch)
                }
            }
        }
    }

    @Throws(IOException::class)
    private fun writeComment(writer: Writer, comment: String) {
        writer.write("#")
        val len = comment.length
        var curIndex = 0
        var lastIndex = 0
        while (curIndex < len) {
            val c = comment[curIndex]
            if (c > '\u00ff' || c == '\n' || c == '\r') {
                if (lastIndex != curIndex) writer.write(comment.substring(lastIndex, curIndex))
                if (c > '\u00ff') {
                    val hex: String = java.lang.Integer.toHexString(c.toInt())
                    writer.write("\\u")
                    for (j in 0 until 4 - hex.length) {
                        writer.write('0')
                    }
                    writer.write(hex)
                } else {
                    writer.write(LINE_SEPARATOR)
                    if (c == '\r' && curIndex != len - 1 && comment[curIndex + 1] == '\n') {
                        curIndex++
                    }
                    if (curIndex == len - 1 || comment[curIndex + 1] != '#' && comment[curIndex + 1] != '!') writer.write("#")
                }
                lastIndex = curIndex + 1
            }
            curIndex++
        }
        if (lastIndex != curIndex) writer.write(comment.substring(lastIndex, curIndex))
        writer.write(LINE_SEPARATOR)
    }
}
