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

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.utils.JsonWriter.OutputType
import com.badlogic.gdx.utils.TimeUtils
import java.io.IOException
import java.lang.ArrayIndexOutOfBoundsException
import java.util.Arrays
import kotlin.jvm.Throws

/**
 * Lightweight UBJSON parser.<br></br>
 * <br></br>
 * The default behavior is to parse the JSON into a DOM containing [JsonValue] objects. Extend this class and override
 * methods to perform event driven parsing. When this is done, the parse methods will return null. <br></br>
 *
 * @author Xoppa
 */
class UBJsonReader : BaseJsonReader {

    var oldFormat = true

    /**
     * Parses the UBJSON from the given stream. <br></br>
     * For best performance you should provide buffered streams to this method!
     */
    fun parse(input: java.io.InputStream?): JsonValue? {
        var din: java.io.DataInputStream? = null
        return try {
            din = java.io.DataInputStream(input)
            parse(din)
        } catch (ex: IOException) {
            throw SerializationException(ex)
        } finally {
            StreamUtils.closeQuietly(din)
        }
    }

    fun parse(file: FileHandle?): JsonValue? {
        return try {
            parse(file!!.read(8192))
        } catch (ex: java.lang.Exception) {
            throw SerializationException("Error parsing file: $file", ex)
        }
    }

    @Throws(IOException::class)
    fun parse(din: java.io.DataInputStream?): JsonValue? {
        return try {
            parse(din, din.readByte())
        } finally {
            StreamUtils.closeQuietly(din)
        }
    }

    @Throws(IOException::class)
    protected fun parse(din: java.io.DataInputStream?, type: Byte): JsonValue? {
        return if (type == '['.toByte()) parseArray(din) else if (type == '{'.toByte()) parseObject(din) else if (type == 'Z'.toByte()) JsonValue(JsonValue.ValueType.nullValue) else if (type == 'T'.toByte()) JsonValue(true) else if (type == 'F'.toByte()) JsonValue(false) else if (type == 'B'.toByte()) JsonValue(readUChar(din).toLong()) else if (type == 'U'.toByte()) JsonValue(readUChar(din).toLong()) else if (type == 'i'.toByte()) JsonValue(if (oldFormat) din.readShort() else din.readByte()) else if (type == 'I'.toByte()) JsonValue(if (oldFormat) din.readInt() else din.readShort()) else if (type == 'l'.toByte()) JsonValue(din.readInt() as Long) else if (type == 'L'.toByte()) JsonValue(din.readLong()) else if (type == 'd'.toByte()) JsonValue(din.readFloat()) else if (type == 'D'.toByte()) JsonValue(din.readDouble()) else if (type == 's'.toByte() || type == 'S'.toByte()) JsonValue(parseString(din, type)) else if (type == 'a'.toByte() || type == 'A'.toByte()) parseData(din, type) else if (type == 'C'.toByte()) JsonValue(din.readChar()) else throw GdxRuntimeException("Unrecognized data type")
    }

    @Throws(IOException::class)
    protected fun parseArray(din: java.io.DataInputStream?): JsonValue? {
        val result = JsonValue(JsonValue.ValueType.array)
        var type: Byte = din.readByte()
        var valueType: Byte = 0
        if (type == '$'.toByte()) {
            valueType = din.readByte()
            type = din.readByte()
        }
        var size: Long = -1
        if (type == '#'.toByte()) {
            size = parseSize(din, false, -1)
            if (size < 0) throw GdxRuntimeException("Unrecognized data type")
            if (size == 0L) return result
            type = if (valueType.toInt() == 0) din.readByte() else valueType
        }
        var prev: JsonValue? = null
        var c: Long = 0
        while (din.available() > 0 && type != ']'.toByte()) {
            val `val`: JsonValue? = parse(din, type)
            `val`.parent = result
            if (prev != null) {
                `val`.prev = prev
                prev.next = `val`
                result.size++
            } else {
                result.child = `val`
                result.size = 1
            }
            prev = `val`
            if (size > 0 && ++c >= size) break
            type = if (valueType.toInt() == 0) din.readByte() else valueType
        }
        return result
    }

    @Throws(IOException::class)
    protected fun parseObject(din: java.io.DataInputStream?): JsonValue? {
        val result = JsonValue(JsonValue.ValueType.`object`)
        var type: Byte = din.readByte()
        var valueType: Byte = 0
        if (type == '$'.toByte()) {
            valueType = din.readByte()
            type = din.readByte()
        }
        var size: Long = -1
        if (type == '#'.toByte()) {
            size = parseSize(din, false, -1)
            if (size < 0) throw GdxRuntimeException("Unrecognized data type")
            if (size == 0L) return result
            type = din.readByte()
        }
        var prev: JsonValue? = null
        var c: Long = 0
        while (din.available() > 0 && type != '}'.toByte()) {
            val key = parseString(din, true, type)
            val child: JsonValue? = parse(din, if (valueType.toInt() == 0) din.readByte() else valueType)
            child.setName(key)
            child.parent = result
            if (prev != null) {
                child.prev = prev
                prev.next = child
                result.size++
            } else {
                result.child = child
                result.size = 1
            }
            prev = child
            if (size > 0 && ++c >= size) break
            type = din.readByte()
        }
        return result
    }

    @Throws(IOException::class)
    protected fun parseData(din: java.io.DataInputStream?, blockType: Byte): JsonValue? {
        // FIXME: a/A is currently not following the specs because it lacks strong typed, fixed sized containers,
        // see: https://github.com/thebuzzmedia/universal-binary-json/issues/27
        val dataType: Byte = din.readByte()
        val size = if (blockType == 'A'.toByte()) readUInt(din) else readUChar(din).toLong()
        val result = JsonValue(JsonValue.ValueType.array)
        var prev: JsonValue? = null
        for (i in 0 until size) {
            val `val`: JsonValue? = parse(din, dataType)
            `val`.parent = result
            if (prev != null) {
                prev.next = `val`
                result.size++
            } else {
                result.child = `val`
                result.size = 1
            }
            prev = `val`
        }
        return result
    }

    @Throws(IOException::class)
    protected fun parseString(din: java.io.DataInputStream?, type: Byte): String? {
        return parseString(din, false, type)
    }

    @Throws(IOException::class)
    protected fun parseString(din: java.io.DataInputStream?, sOptional: Boolean, type: Byte): String? {
        var size: Long = -1
        if (type == 'S'.toByte()) {
            size = parseSize(din, true, -1)
        } else if (type == 's'.toByte()) size = readUChar(din).toLong() else if (sOptional) size = parseSize(din, type, false, -1)
        if (size < 0) throw GdxRuntimeException("Unrecognized data type, string expected")
        return if (size > 0) readString(din, size) else ""
    }

    @Throws(IOException::class)
    protected fun parseSize(din: java.io.DataInputStream?, useIntOnError: Boolean, defaultValue: Long): Long {
        return parseSize(din, din.readByte(), useIntOnError, defaultValue)
    }

    @Throws(IOException::class)
    protected fun parseSize(din: java.io.DataInputStream?, type: Byte, useIntOnError: Boolean, defaultValue: Long): Long {
        if (type == 'i'.toByte()) return readUChar(din).toLong()
        if (type == 'I'.toByte()) return readUShort(din).toLong()
        if (type == 'l'.toByte()) return readUInt(din)
        if (type == 'L'.toByte()) return din.readLong()
        if (useIntOnError) {
            var result = (type.toShort() and 0xFF) as Long shl 24
            result = result or (din.readByte() as Short and 0xFF) as Long shl 16
            result = result or (din.readByte() as Short and 0xFF) as Long shl 8
            result = result or (din.readByte() as Short and 0xFF) as Long
            return result
        }
        return defaultValue
    }

    @Throws(IOException::class)
    protected fun readUChar(din: java.io.DataInputStream?): Short {
        return (din.readByte() as Short and 0xFF) as Short
    }

    @Throws(IOException::class)
    protected fun readUShort(din: java.io.DataInputStream?): Int {
        return din.readShort() as Int and 0xFFFF
    }

    @Throws(IOException::class)
    protected fun readUInt(din: java.io.DataInputStream?): Long {
        return din.readInt() as Long and -0x1
    }

    @Throws(IOException::class)
    protected fun readString(din: java.io.DataInputStream?, size: Long): String? {
        val data = ByteArray(size.toInt())
        din.readFully(data)
        return String(data, "UTF-8")
    }
}
