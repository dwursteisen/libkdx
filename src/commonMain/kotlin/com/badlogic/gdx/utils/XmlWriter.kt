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

//@off
/**
 * Builder style API for emitting XML. <pre>
 * StringWriter writer = new StringWriter();
 * XmlWriter xml = new XmlWriter(writer);
 * xml.element("meow")
 * .attribute("moo", "cow")
 * .element("child")
 * .attribute("moo", "cow")
 * .element("child")
 * .attribute("moo", "cow")
 * .text("XML is like violence. If it doesn't solve your problem, you're not using enough of it.")
 * .pop()
 * .pop()
 * .pop();
 * System.out.println(writer);
</pre> *
 *
 * @author Nathan Sweet
 */
//@on
class XmlWriter(writer: Writer) : Writer() {

    private val writer: Writer
    private val stack: Array<String> = Array()
    private var currentElement: String? = null
    private var indentNextClose = false
    var indent = 0

    @Throws(IOException::class)
    private fun indent() {
        var count = indent
        if (currentElement != null) count++
        for (i in 0 until count) writer.write('\t')
    }

    @Throws(IOException::class)
    fun element(name: String?): XmlWriter {
        if (startElementContent()) writer.write('\n')
        indent()
        writer.write('<')
        writer.write(name)
        currentElement = name
        return this
    }

    @Throws(IOException::class)
    fun element(name: String?, text: Any?): XmlWriter {
        return element(name).text(text).pop()
    }

    @Throws(IOException::class)
    private fun startElementContent(): Boolean {
        if (currentElement == null) return false
        indent++
        stack.add(currentElement)
        currentElement = null
        writer.write(">")
        return true
    }

    @Throws(IOException::class)
    fun attribute(name: String?, value: Any?): XmlWriter {
        checkNotNull(currentElement)
        writer.write(' ')
        writer.write(name)
        writer.write("=\"")
        writer.write(value?.toString() ?: "null")
        writer.write('"')
        return this
    }

    @Throws(IOException::class)
    fun text(text: Any?): XmlWriter {
        startElementContent()
        val string = text?.toString() ?: "null"
        indentNextClose = string.length > 64
        if (indentNextClose) {
            writer.write('\n')
            indent()
        }
        writer.write(string)
        if (indentNextClose) writer.write('\n')
        return this
    }

    @Throws(IOException::class)
    fun pop(): XmlWriter {
        if (currentElement != null) {
            writer.write("/>\n")
            currentElement = null
        } else {
            indent = java.lang.Math.max(indent - 1, 0)
            if (indentNextClose) indent()
            writer.write("</")
            writer.write(stack.pop())
            writer.write(">\n")
        }
        indentNextClose = true
        return this
    }

    /**
     * Calls [.pop] for each remaining open element, if any, and closes the stream.
     */
    @Throws(IOException::class)
    fun close() {
        while (stack.size !== 0) pop()
        writer.close()
    }

    @Throws(IOException::class)
    fun write(cbuf: CharArray?, off: Int, len: Int) {
        startElementContent()
        writer.write(cbuf, off, len)
    }

    @Throws(IOException::class)
    fun flush() {
        writer.flush()
    }

    init {
        this.writer = writer
    }
}
