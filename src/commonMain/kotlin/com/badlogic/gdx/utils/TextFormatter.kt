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

/**
 * `TextFormatter` is used by [I18NBundle] to perform argument replacement.
 *
 * @author davebaol
 */
internal class TextFormatter(locale: Locale?, useMessageFormat: Boolean) {

    private var messageFormat: MessageFormat? = null
    private val buffer: java.lang.StringBuilder

    /**
     * Formats the given `pattern` replacing its placeholders with the actual arguments specified by `args`.
     *
     *
     * If this `TextFormatter` has been instantiated with [TextFormatter(locale, true)][.TextFormatter]
     * [MessageFormat] is used to process the pattern, meaning that the actual arguments are properly localized with the
     * locale of this `TextFormatter`.
     *
     *
     * On the contrary, if this `TextFormatter` has been instantiated with [ TextFormatter(locale, false)][.TextFormatter] pattern's placeholders are expected to be in the simplified form {0}, {1}, {2} and so on and
     * they will be replaced with the corresponding object from `args` converted to a string with `toString()`, so
     * without taking into account the locale.
     *
     *
     * In both cases, there's only one simple escaping rule, i.e. a left curly bracket must be doubled if you want it to be part of
     * your string.
     *
     *
     * It's worth noting that the rules for using single quotes within [MessageFormat] patterns have shown to be somewhat
     * confusing. In particular, it isn't always obvious to localizers whether single quotes need to be doubled or not. For this
     * very reason we decided to offer the simpler escaping rule above without limiting the expressive power of message format
     * patterns. So, if you're used to MessageFormat's syntax, remember that with `TextFormatter` single quotes never need to
     * be escaped!
     *
     * @param pattern the pattern
     * @param args    the arguments
     * @return the formatted pattern
     * @throws IllegalArgumentException if the pattern is invalid
     */
    fun format(pattern: String, vararg args: Any?): String {
        if (messageFormat != null) {
            messageFormat.applyPattern(replaceEscapeChars(pattern))
            return messageFormat.format(args)
        }
        return simpleFormat(pattern, *args)
    }

    // This code is needed because a simple replacement like
    // pattern.replace("'", "''").replace("{{", "'{'");
    // can't properly manage some special cases.
    // For example, the expected output for {{{{ is {{ but you get {'{ instead.
    // Also this code is optimized since a new string is returned only if something has been replaced.
    private fun replaceEscapeChars(pattern: String): String {
        buffer.setLength(0)
        var changed = false
        val len = pattern.length
        var i = 0
        while (i < len) {
            val ch = pattern[i]
            if (ch == '\'') {
                changed = true
                buffer.append("''")
            } else if (ch == '{') {
                var j = i + 1
                while (j < len && pattern[j] == '{') j++
                var escaped = (j - i) / 2
                if (escaped > 0) {
                    changed = true
                    buffer.append('\'')
                    do {
                        buffer.append('{')
                    } while (--escaped > 0)
                    buffer.append('\'')
                }
                if ((j - i) % 2 != 0) buffer.append('{')
                i = j - 1
            } else {
                buffer.append(ch)
            }
            i++
        }
        return if (changed) buffer.toString() else pattern
    }

    /**
     * Formats the given `pattern` replacing any placeholder of the form {0}, {1}, {2} and so on with the corresponding
     * object from `args` converted to a string with `toString()`, so without taking into account the locale.
     *
     *
     * This method only implements a small subset of the grammar supported by [java.text.MessageFormat]. Especially,
     * placeholder are only made up of an index; neither the type nor the style are supported.
     *
     *
     * If nothing has been replaced this implementation returns the pattern itself.
     *
     * @param pattern the pattern
     * @param args    the arguments
     * @return the formatted pattern
     * @throws IllegalArgumentException if the pattern is invalid
     */
    private fun simpleFormat(pattern: String, vararg args: Any?): String {
        buffer.setLength(0)
        var changed = false
        var placeholder = -1
        val patternLength = pattern.length
        var i = 0
        while (i < patternLength) {
            val ch = pattern[i]
            if (placeholder < 0) { // processing constant part
                if (ch == '{') {
                    changed = true
                    if (i + 1 < patternLength && pattern[i + 1] == '{') {
                        buffer.append(ch) // handle escaped '{'
                        ++i
                    } else {
                        placeholder = 0 // switch to placeholder part
                    }
                } else {
                    buffer.append(ch)
                }
            } else { // processing placeholder part
                placeholder = if (ch == '}') {
                    if (placeholder >= args.size) throw java.lang.IllegalArgumentException("Argument index out of bounds: $placeholder")
                    if (pattern[i - 1] == '{') throw java.lang.IllegalArgumentException("Missing argument index after a left curly brace")
                    if (args[placeholder] == null) buffer.append("null") // append null argument
                    else buffer.append(args[placeholder].toString()) // append actual argument
                    -1 // switch to constant part
                } else {
                    if (ch < '0' || ch > '9') throw java.lang.IllegalArgumentException("Unexpected '$ch' while parsing argument index")
                    placeholder * 10 + (ch - '0')
                }
            }
            ++i
        }
        if (placeholder >= 0) throw java.lang.IllegalArgumentException("Unmatched braces in the pattern.")
        return if (changed) buffer.toString() else pattern
    }

    init {
        buffer = java.lang.StringBuilder()
        if (useMessageFormat) messageFormat = MessageFormat("", locale)
    }
}
