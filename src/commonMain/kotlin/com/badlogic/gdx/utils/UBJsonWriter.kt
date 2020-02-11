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

import java.lang.IllegalStateException
import kotlin.jvm.Throws

/**
 * Builder style API for emitting UBJSON.
 *
 * @author Justin Shapcott
 */
class UBJsonWriter(out: OutputStream) : Closeable {

    val out: DataOutputStream
    private var current: JsonObject? = null
    private var named = false
    private val stack: Array<JsonObject?> = Array<Any?>()

    /**
     * Begins a new object container. To finish the object call [.pop].
     *
     * @return This writer, for chaining
     */
    @Throws(IOException::class)
    fun `object`(): UBJsonWriter {
        if (current != null) {
            if (!current!!.array) {
                check(named) { "Name must be set." }
                named = false
            }
        }
        stack.add(JsonObject(false).also { current = it })
        return this
    }

    /**
     * Begins a new named object container, having the given name. To finish the object call [.pop].
     *
     * @return This writer, for chaining
     */
    @Throws(IOException::class)
    fun `object`(name: String?): UBJsonWriter {
        name(name).`object`()
        return this
    }

    /**
     * Begins a new array container. To finish the array call [.pop].
     *
     * @return this writer, for chaining.
     */
    @Throws(IOException::class)
    fun array(): UBJsonWriter {
        if (current != null) {
            if (!current!!.array) {
                check(named) { "Name must be set." }
                named = false
            }
        }
        stack.add(JsonObject(true).also { current = it })
        return this
    }

    /**
     * Begins a new named array container, having the given name. To finish the array call [.pop].
     *
     * @return this writer, for chaining.
     */
    @Throws(IOException::class)
    fun array(name: String?): UBJsonWriter {
        name(name).array()
        return this
    }

    /**
     * Appends a name for the next object, array, or value.
     *
     * @return this writer, for chaining
     */
    @Throws(IOException::class)
    fun name(name: String?): UBJsonWriter {
        check(!(current == null || current!!.array)) { "Current item must be an object." }
        val bytes: ByteArray = name.toByteArray(charset("UTF-8"))
        if (bytes.size <= Byte.MAX_VALUE) {
            out.writeByte('i')
            out.writeByte(bytes.size)
        } else if (bytes.size <= Short.MAX_VALUE) {
            out.writeByte('I')
            out.writeShort(bytes.size)
        } else {
            out.writeByte('l')
            out.writeInt(bytes.size)
        }
        out.write(bytes)
        named = true
        return this
    }

    /**
     * Appends a `byte` value to the stream. This corresponds to the `int8` value type in the UBJSON specification.
     *
     * @return this writer, for chaining
     */
    @Throws(IOException::class)
    fun value(value: Byte): UBJsonWriter {
        checkName()
        out.writeByte('i')
        out.writeByte(value)
        return this
    }

    /**
     * Appends a `short` value to the stream. This corresponds to the `int16` value type in the UBJSON specification.
     *
     * @return this writer, for chaining
     */
    @Throws(IOException::class)
    fun value(value: Short): UBJsonWriter {
        checkName()
        out.writeByte('I')
        out.writeShort(value)
        return this
    }

    /**
     * Appends an `int` value to the stream. This corresponds to the `int32` value type in the UBJSON specification.
     *
     * @return this writer, for chaining
     */
    @Throws(IOException::class)
    fun value(value: Int): UBJsonWriter {
        checkName()
        out.writeByte('l')
        out.writeInt(value)
        return this
    }

    /**
     * Appends a `long` value to the stream. This corresponds to the `int64` value type in the UBJSON specification.
     *
     * @return this writer, for chaining
     */
    @Throws(IOException::class)
    fun value(value: Long): UBJsonWriter {
        checkName()
        out.writeByte('L')
        out.writeLong(value)
        return this
    }

    /**
     * Appends a `float` value to the stream. This corresponds to the `float32` value type in the UBJSON specification.
     *
     * @return this writer, for chaining
     */
    @Throws(IOException::class)
    fun value(value: Float): UBJsonWriter {
        checkName()
        out.writeByte('d')
        out.writeFloat(value)
        return this
    }

    /**
     * Appends a `double` value to the stream. This corresponds to the `float64` value type in the UBJSON
     * specification.
     *
     * @return this writer, for chaining
     */
    @Throws(IOException::class)
    fun value(value: Double): UBJsonWriter {
        checkName()
        out.writeByte('D')
        out.writeDouble(value)
        return this
    }

    /**
     * Appends a `boolean` value to the stream. This corresponds to the `boolean` value type in the UBJSON
     * specification.
     *
     * @return this writer, for chaining
     */
    @Throws(IOException::class)
    fun value(value: Boolean): UBJsonWriter {
        checkName()
        out.writeByte(if (value) 'T' else 'F')
        return this
    }

    /**
     * Appends a `char` value to the stream. Because, in Java, a `char` is 16 bytes, this corresponds to the
     * `int16` value type in the UBJSON specification.
     *
     * @return this writer, for chaining
     */
    @Throws(IOException::class)
    fun value(value: Char): UBJsonWriter {
        checkName()
        out.writeByte('I')
        out.writeChar(value)
        return this
    }

    /**
     * Appends a `String` value to the stream. This corresponds to the `string` value type in the UBJSON specification.
     *
     * @return this writer, for chaining
     */
    @Throws(IOException::class)
    fun value(value: String?): UBJsonWriter {
        checkName()
        val bytes: ByteArray = value.toByteArray(charset("UTF-8"))
        out.writeByte('S')
        if (bytes.size <= Byte.MAX_VALUE) {
            out.writeByte('i')
            out.writeByte(bytes.size)
        } else if (bytes.size <= Short.MAX_VALUE) {
            out.writeByte('I')
            out.writeShort(bytes.size)
        } else {
            out.writeByte('l')
            out.writeInt(bytes.size)
        }
        out.write(bytes)
        return this
    }

    /**
     * Appends an optimized `byte array` value to the stream. As an optimized array, the `int8` value type marker and
     * element count are encoded once at the array marker instead of repeating the type marker for each element.
     *
     * @return this writer, for chaining
     */
    @Throws(IOException::class)
    fun value(values: ByteArray): UBJsonWriter {
        array()
        out.writeByte('$')
        out.writeByte('i')
        out.writeByte('#')
        value(values.size)
        var i = 0
        val n = values.size
        while (i < n) {
            out.writeByte(values[i])
            i++
        }
        pop(true)
        return this
    }

    /**
     * Appends an optimized `short array` value to the stream. As an optimized array, the `int16` value type marker and
     * element count are encoded once at the array marker instead of repeating the type marker for each element.
     *
     * @return this writer, for chaining
     */
    @Throws(IOException::class)
    fun value(values: ShortArray): UBJsonWriter {
        array()
        out.writeByte('$')
        out.writeByte('I')
        out.writeByte('#')
        value(values.size)
        var i = 0
        val n = values.size
        while (i < n) {
            out.writeShort(values[i])
            i++
        }
        pop(true)
        return this
    }

    /**
     * Appends an optimized `int array` value to the stream. As an optimized array, the `int32` value type marker and
     * element count are encoded once at the array marker instead of repeating the type marker for each element.
     *
     * @return this writer, for chaining
     */
    @Throws(IOException::class)
    fun value(values: IntArray): UBJsonWriter {
        array()
        out.writeByte('$')
        out.writeByte('l')
        out.writeByte('#')
        value(values.size)
        var i = 0
        val n = values.size
        while (i < n) {
            out.writeInt(values[i])
            i++
        }
        pop(true)
        return this
    }

    /**
     * Appends an optimized `long array` value to the stream. As an optimized array, the `int64` value type marker and
     * element count are encoded once at the array marker instead of repeating the type marker for each element.
     *
     * @return this writer, for chaining
     */
    @Throws(IOException::class)
    fun value(values: LongArray): UBJsonWriter {
        array()
        out.writeByte('$')
        out.writeByte('L')
        out.writeByte('#')
        value(values.size)
        var i = 0
        val n = values.size
        while (i < n) {
            out.writeLong(values[i])
            i++
        }
        pop(true)
        return this
    }

    /**
     * Appends an optimized `float array` value to the stream. As an optimized array, the `float32` value type marker
     * and element count are encoded once at the array marker instead of repeating the type marker for each element.
     *
     * @return this writer, for chaining
     */
    @Throws(IOException::class)
    fun value(values: FloatArray): UBJsonWriter {
        array()
        out.writeByte('$')
        out.writeByte('d')
        out.writeByte('#')
        value(values.size)
        var i = 0
        val n = values.size
        while (i < n) {
            out.writeFloat(values[i])
            i++
        }
        pop(true)
        return this
    }

    /**
     * Appends an optimized `double array` value to the stream. As an optimized array, the `float64` value type marker
     * and element count are encoded once at the array marker instead of repeating the type marker for each element. element count
     * are encoded once at the array marker instead of for each element.
     *
     * @return this writer, for chaining
     */
    @Throws(IOException::class)
    fun value(values: DoubleArray): UBJsonWriter {
        array()
        out.writeByte('$')
        out.writeByte('D')
        out.writeByte('#')
        value(values.size)
        var i = 0
        val n = values.size
        while (i < n) {
            out.writeDouble(values[i])
            i++
        }
        pop(true)
        return this
    }

    /**
     * Appends a `boolean array` value to the stream.
     *
     * @return this writer, for chaining
     */
    @Throws(IOException::class)
    fun value(values: BooleanArray): UBJsonWriter {
        array()
        var i = 0
        val n = values.size
        while (i < n) {
            out.writeByte(if (values[i]) 'T' else 'F')
            i++
        }
        pop()
        return this
    }

    /**
     * Appends an optimized `char array` value to the stream. As an optimized array, the `int16` value type marker and
     * element count are encoded once at the array marker instead of repeating the type marker for each element.
     *
     * @return this writer, for chaining
     */
    @Throws(IOException::class)
    fun value(values: CharArray): UBJsonWriter {
        array()
        out.writeByte('$')
        out.writeByte('C')
        out.writeByte('#')
        value(values.size)
        var i = 0
        val n = values.size
        while (i < n) {
            out.writeChar(values[i])
            i++
        }
        pop(true)
        return this
    }

    /**
     * Appends an optimized `String array` value to the stream. As an optimized array, the `String` value type marker
     * and element count are encoded once at the array marker instead of repeating the type marker for each element.
     *
     * @return this writer, for chaining
     */
    @Throws(IOException::class)
    fun value(values: kotlin.Array<String>): UBJsonWriter {
        array()
        out.writeByte('$')
        out.writeByte('S')
        out.writeByte('#')
        value(values.size)
        var i = 0
        val n = values.size
        while (i < n) {
            val bytes: ByteArray = values[i].toByteArray(charset("UTF-8"))
            if (bytes.size <= Byte.MAX_VALUE) {
                out.writeByte('i')
                out.writeByte(bytes.size)
            } else if (bytes.size <= Short.MAX_VALUE) {
                out.writeByte('I')
                out.writeShort(bytes.size)
            } else {
                out.writeByte('l')
                out.writeInt(bytes.size)
            }
            out.write(bytes)
            i++
        }
        pop(true)
        return this
    }

    /**
     * Appends the given JsonValue, including all its fields recursively, to the stream.
     *
     * @return this writer, for chaining
     */
    @Throws(IOException::class)
    fun value(value: JsonValue): UBJsonWriter {
        if (value.isObject) {
            if (value.name != null) `object`(value.name) else `object`()
            var child = value.child
            while (child != null) {
                value(child)
                child = child.next
            }
            pop()
        } else if (value.isArray) {
            if (value.name != null) array(value.name) else array()
            var child = value.child
            while (child != null) {
                value(child)
                child = child.next
            }
            pop()
        } else if (value.isBoolean) {
            if (value.name != null) name(value.name)
            value(value.asBoolean())
        } else if (value.isDouble) {
            if (value.name != null) name(value.name)
            value(value.asDouble())
        } else if (value.isLong) {
            if (value.name != null) name(value.name)
            value(value.asLong())
        } else if (value.isString) {
            if (value.name != null) name(value.name)
            value(value.asString())
        } else if (value.isNull) {
            if (value.name != null) name(value.name)
            value()
        } else {
            throw IOException("Unhandled JsonValue type")
        }
        return this
    }

    /**
     * Appends the object to the stream, if it is a known value type. This is a convenience method that calls through to the
     * appropriate value method.
     *
     * @return this writer, for chaining
     */
    @Throws(IOException::class)
    fun value(`object`: Any?): UBJsonWriter {
        if (`object` == null) {
            return value()
        } else if (`object` is Number) {
            val number = `object`
            if (`object` is Byte) return value(number.byteValue())
            if (`object` is Short) return value(number.shortValue())
            if (`object` is Int) return value(number.intValue())
            if (`object` is Long) return value(number.longValue())
            if (`object` is Float) return value(number.floatValue())
            if (`object` is Double) return value(number.doubleValue())
        } else return if (`object` is Char) {
            value(`object`.toChar())
        } else if (`object` is CharSequence) {
            value(`object`.toString())
        } else throw IOException("Unknown object type.")
        return this
    }

    /**
     * Appends a `null` value to the stream.
     *
     * @return this writer, for chaining
     */
    @Throws(IOException::class)
    fun value(): UBJsonWriter {
        checkName()
        out.writeByte('Z')
        return this
    }

    /**
     * Appends a named `byte` value to the stream.
     *
     * @return this writer, for chaining
     */
    @Throws(IOException::class)
    operator fun set(name: String?, value: Byte): UBJsonWriter {
        return name(name).value(value)
    }

    /**
     * Appends a named `short` value to the stream.
     *
     * @return this writer, for chaining
     */
    @Throws(IOException::class)
    operator fun set(name: String?, value: Short): UBJsonWriter {
        return name(name).value(value)
    }

    /**
     * Appends a named `int` value to the stream.
     *
     * @return this writer, for chaining
     */
    @Throws(IOException::class)
    operator fun set(name: String?, value: Int): UBJsonWriter {
        return name(name).value(value)
    }

    /**
     * Appends a named `long` value to the stream.
     *
     * @return this writer, for chaining
     */
    @Throws(IOException::class)
    operator fun set(name: String?, value: Long): UBJsonWriter {
        return name(name).value(value)
    }

    /**
     * Appends a named `float` value to the stream.
     *
     * @return this writer, for chaining
     */
    @Throws(IOException::class)
    operator fun set(name: String?, value: Float): UBJsonWriter {
        return name(name).value(value)
    }

    /**
     * Appends a named `double` value to the stream.
     *
     * @return this writer, for chaining
     */
    @Throws(IOException::class)
    operator fun set(name: String?, value: Double): UBJsonWriter {
        return name(name).value(value)
    }

    /**
     * Appends a named `boolean` value to the stream.
     *
     * @return this writer, for chaining
     */
    @Throws(IOException::class)
    operator fun set(name: String?, value: Boolean): UBJsonWriter {
        return name(name).value(value)
    }

    /**
     * Appends a named `char` value to the stream.
     *
     * @return this writer, for chaining
     */
    @Throws(IOException::class)
    operator fun set(name: String?, value: Char): UBJsonWriter {
        return name(name).value(value)
    }

    /**
     * Appends a named `String` value to the stream.
     *
     * @return this writer, for chaining
     */
    @Throws(IOException::class)
    operator fun set(name: String?, value: String?): UBJsonWriter {
        return name(name).value(value)
    }

    /**
     * Appends a named `byte` array value to the stream.
     *
     * @return this writer, for chaining
     */
    @Throws(IOException::class)
    operator fun set(name: String?, value: ByteArray): UBJsonWriter {
        return name(name).value(value)
    }

    /**
     * Appends a named `short` array value to the stream.
     *
     * @return this writer, for chaining
     */
    @Throws(IOException::class)
    operator fun set(name: String?, value: ShortArray): UBJsonWriter {
        return name(name).value(value)
    }

    /**
     * Appends a named `int` array value to the stream.
     *
     * @return this writer, for chaining
     */
    @Throws(IOException::class)
    operator fun set(name: String?, value: IntArray): UBJsonWriter {
        return name(name).value(value)
    }

    /**
     * Appends a named `long` array value to the stream.
     *
     * @return this writer, for chaining
     */
    @Throws(IOException::class)
    operator fun set(name: String?, value: LongArray): UBJsonWriter {
        return name(name).value(value)
    }

    /**
     * Appends a named `float` array value to the stream.
     *
     * @return this writer, for chaining
     */
    @Throws(IOException::class)
    operator fun set(name: String?, value: FloatArray): UBJsonWriter {
        return name(name).value(value)
    }

    /**
     * Appends a named `double` array value to the stream.
     *
     * @return this writer, for chaining
     */
    @Throws(IOException::class)
    operator fun set(name: String?, value: DoubleArray): UBJsonWriter {
        return name(name).value(value)
    }

    /**
     * Appends a named `boolean` array value to the stream.
     *
     * @return this writer, for chaining
     */
    @Throws(IOException::class)
    operator fun set(name: String?, value: BooleanArray): UBJsonWriter {
        return name(name).value(value)
    }

    /**
     * Appends a named `char` array value to the stream.
     *
     * @return this writer, for chaining
     */
    @Throws(IOException::class)
    operator fun set(name: String?, value: CharArray): UBJsonWriter {
        return name(name).value(value)
    }

    /**
     * Appends a named `String` array value to the stream.
     *
     * @return this writer, for chaining
     */
    @Throws(IOException::class)
    operator fun set(name: String?, value: kotlin.Array<String>): UBJsonWriter {
        return name(name).value(value)
    }

    /**
     * Appends a named `null` array value to the stream.
     *
     * @return this writer, for chaining
     */
    @Throws(IOException::class)
    fun set(name: String?): UBJsonWriter {
        return name(name).value()
    }

    private fun checkName() {
        if (current != null) {
            if (!current!!.array) {
                check(named) { "Name must be set." }
                named = false
            }
        }
    }

    /**
     * Ends the current object or array and pops it off of the element stack.
     *
     * @return This writer, for chaining
     */
    @Throws(IOException::class)
    fun pop(): UBJsonWriter {
        return pop(false)
    }

    @Throws(IOException::class)
    protected fun pop(silent: Boolean): UBJsonWriter {
        check(!named) { "Expected an object, array, or value since a name was set." }
        if (silent) stack.pop() else stack.pop()!!.close()
        current = if (stack.size == 0) null else stack.peek()
        return this
    }

    /**
     * Flushes the underlying stream. This forces any buffered output bytes to be written out to the stream.
     */
    @Throws(IOException::class)
    fun flush() {
        out.flush()
    }

    /**
     * Closes the underlying output stream and releases any system resources associated with the stream.
     */
    @Throws(IOException::class)
    fun close() {
        while (stack.size > 0) pop()
        out.close()
    }

    private inner class JsonObject internal constructor(val array: Boolean) {
        @Throws(IOException::class)
        fun close() {
            out.writeByte(if (array) ']' else '}')
        }

        init {
            out.writeByte(if (array) '[' else '{')
        }
    }

    init {
        var out: OutputStream = out
        if (out !is DataOutputStream) out = DataOutputStream(out)
        this.out = out as DataOutputStream
    }
}
