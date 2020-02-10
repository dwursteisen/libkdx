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

import com.badlogic.gdx.utils.JsonValue
import com.badlogic.gdx.utils.JsonValue.JsonIterator
import com.badlogic.gdx.utils.JsonValue.PrettyPrintSettings
import com.badlogic.gdx.utils.LongArray
import com.badlogic.gdx.utils.LongMap
import com.badlogic.gdx.utils.ObjectFloatMap
import java.lang.IllegalStateException
import java.lang.IndexOutOfBoundsException
import kotlin.jvm.Throws

/**
 * Builder style API for emitting JSON.
 *
 * @author Nathan Sweet
 */
class JsonWriter(writer: Writer?) : Writer() {

    val writer: Writer
    private val stack: com.badlogic.gdx.utils.Array<JsonObject> = com.badlogic.gdx.utils.Array()
    private var current: JsonObject? = null
    private var named = false
    private var outputType = OutputType.json
    private var quoteLongValues = false
    fun getWriter(): Writer {
        return writer
    }

    /**
     * Sets the type of JSON output. Default is [OutputType.minimal].
     */
    fun setOutputType(outputType: OutputType) {
        this.outputType = outputType
    }

    /**
     * When true, quotes long, double, BigInteger, BigDecimal types to prevent truncation in languages like JavaScript and PHP.
     * This is not necessary when using libgdx, which handles these types without truncation. Default is false.
     */
    fun setQuoteLongValues(quoteLongValues: Boolean) {
        this.quoteLongValues = quoteLongValues
    }

    @Throws(IOException::class)
    fun name(name: String?): JsonWriter {
        check(!(current == null || current!!.array)) { "Current item must be an object." }
        if (!current!!.needsComma) current!!.needsComma = true else writer.write(',')
        writer.write(outputType.quoteName(name))
        writer.write(':')
        named = true
        return this
    }

    @Throws(IOException::class)
    fun `object`(): JsonWriter {
        requireCommaOrName()
        stack.add(JsonObject(false).also { current = it })
        return this
    }

    @Throws(IOException::class)
    fun array(): JsonWriter {
        requireCommaOrName()
        stack.add(JsonObject(true).also { current = it })
        return this
    }

    @Throws(IOException::class)
    fun value(value: Any): JsonWriter {
        var value = value
        if (quoteLongValues
            && (value is Long || value is Double || value is BigDecimal || value is BigInteger)) {
            value = value.toString()
        } else if (value is Number) {
            val number = value
            val longValue: Long = number.longValue()
            if (number.doubleValue() == longValue.toDouble()) value = longValue
        }
        requireCommaOrName()
        writer.write(outputType.quoteValue(value))
        return this
    }

    /**
     * Writes the specified JSON value, without quoting or escaping.
     */
    @Throws(IOException::class)
    fun json(json: String?): JsonWriter {
        requireCommaOrName()
        writer.write(json)
        return this
    }

    @Throws(IOException::class)
    private fun requireCommaOrName() {
        if (current == null) return
        if (current!!.array) {
            if (!current!!.needsComma) current!!.needsComma = true else writer.write(',')
        } else {
            check(named) { "Name must be set." }
            named = false
        }
    }

    @Throws(IOException::class)
    fun `object`(name: String?): JsonWriter {
        return name(name).`object`()
    }

    @Throws(IOException::class)
    fun array(name: String?): JsonWriter {
        return name(name).array()
    }

    @Throws(IOException::class)
    operator fun set(name: String?, value: Any): JsonWriter {
        return name(name).value(value)
    }

    /**
     * Writes the specified JSON value, without quoting or escaping.
     */
    @Throws(IOException::class)
    fun json(name: String?, json: String?): JsonWriter {
        return name(name).json(json)
    }

    @Throws(IOException::class)
    fun pop(): JsonWriter {
        check(!named) { "Expected an object, array, or value since a name was set." }
        stack.pop().close()
        current = if (stack.size == 0) null else stack.peek()
        return this
    }

    @Throws(IOException::class)
    fun write(cbuf: CharArray?, off: Int, len: Int) {
        writer.write(cbuf, off, len)
    }

    @Throws(IOException::class)
    fun flush() {
        writer.flush()
    }

    @Throws(IOException::class)
    fun close() {
        while (stack.size > 0) pop()
        writer.close()
    }

    private inner class JsonObject internal constructor(val array: Boolean) {
        var needsComma = false

        @Throws(IOException::class)
        fun close() {
            writer.write(if (array) ']' else '}')
        }

        init {
            writer.write(if (array) '[' else '{')
        }
    }

    enum class OutputType {
        /**
         * Normal JSON, with all its double quotes.
         */
        json,

        /**
         * Like JSON, but names are only double quoted if necessary.
         */
        javascript,
/**
 * Like JSON, but:
 *
 *  * Names only require double quotes if they start with `space` or any of `":,}/` or they contain
 * `//` or `/*` or `:`.
 *  * Values only require double quotes if they start with `space` or any of `":,{[]/` or they
 * contain `//` or `/*` or any of `}],` or they are equal to `true`,
 * `false` , or `null`.
 *  * Newlines are treated as commas, making commas optional in many cases.
 *  * C style comments may be used: `//...` or `/*...*****/`
 *
*/
minimal;
fun /*@@nqrcax@@*/quoteValue(  value:/*@@bnamru@@*/kotlin.Any?): /*@@zmbdus@@*/kotlin.String?{
if (value == null)return "null"
var  string: /*@@zmbdus@@*/kotlin.String? = value.toString()
if (value is /*@@cgnywv@@*/kotlin.Number || value is /*@@ekspft@@*/Boolean)return string
var  buffer: /*@@kywmuo@@*/com.badlogic.gdx.utils.StringBuilder? = com.badlogic.gdx.utils.StringBuilder(string)
buffer.replace('\\', "\\\\").replace('\r', "\\r").replace('\n', "\\n").replace('\t', "\\t")
if (((this == com.badlogic.gdx.utils.JsonWriter.OutputType.minimal) && !(string == "true") && !(string == "false") && !(string == "null")
&& !string.contains("//") && !string.contains("/*"))){
var  length: /*@@anldhw@@*/Int = buffer.length()
if ((length > 0) && (buffer.charAt(length - 1) != ' ') && com.badlogic.gdx.utils.JsonWriter.OutputType.Companion.minimalValuePattern.matcher(buffer).matches())return buffer.toString()
}
return '"'.toString() + buffer.replace('"', "\\\"").toString() + '"'
}
fun /*@@qkicqk@@*/quoteName(  value:/*@@zmbdus@@*/kotlin.String?): /*@@zmbdus@@*/kotlin.String?{
var  buffer: /*@@kywmuo@@*/com.badlogic.gdx.utils.StringBuilder? = com.badlogic.gdx.utils.StringBuilder(value)
buffer.replace('\\', "\\\\").replace('\r', "\\r").replace('\n', "\\n").replace('\t', "\\t")
when(this){com.badlogic.gdx.utils.JsonWriter.OutputType.minimal -> {
if (!value.contains("//") && !value.contains("/*") && com.badlogic.gdx.utils.JsonWriter.OutputType.Companion.minimalNamePattern.matcher(buffer).matches())return buffer.toString()
if (com.badlogic.gdx.utils.JsonWriter.OutputType.Companion.javascriptPattern.matcher(buffer).matches())return buffer.toString()
}
com.badlogic.gdx.utils.JsonWriter.OutputType.javascript -> if (com.badlogic.gdx.utils.JsonWriter.OutputType.Companion.javascriptPattern.matcher(buffer).matches())return buffer.toString()}
return '"'.toString() + buffer.replace('"', "\\\"").toString() + '"'
}
companion object  {
private  var  javascriptPattern:/*@@qxvmvq@@*/Pattern? = Pattern.compile("^[a-zA-Z_$][a-zA-Z_$0-9]*$")
private  var  minimalNamePattern:/*@@dwnsru@@*/Pattern? = Pattern.compile("^[^\":,}/ ][^:]*$")
private  var  minimalValuePattern:/*@@higrxr@@*/Pattern? = Pattern.compile("^[^\":,{\\[\\]/ ][^}\\],]*$")
}
}
init {
this.writer = writer
}
}
