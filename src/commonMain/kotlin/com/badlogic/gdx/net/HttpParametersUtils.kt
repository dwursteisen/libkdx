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
package com.badlogic.gdx.net

import java.io.UnsupportedEncodingException

/**
 * Provides utility methods to work with the [HttpRequest] content and parameters.
 */
object HttpParametersUtils {

    var defaultEncoding = "UTF-8"
    var nameValueSeparator = "="
    var parameterSeparator = "&"

    /**
     * Useful method to convert a map of key,value pairs to a String to be used as part of a GET or POST content.
     *
     * @param parameters A Map<String></String>, String> with the parameters to encode.
     * @return The String with the parameters encoded.
     */
    fun convertHttpParameters(parameters: Map<String, String?>): String {
        val keySet = parameters.keys
        val convertedParameters: java.lang.StringBuilder = java.lang.StringBuilder()
        for (name in keySet) {
            convertedParameters.append(encode(name, defaultEncoding))
            convertedParameters.append(nameValueSeparator)
            convertedParameters.append(encode(parameters[name], defaultEncoding))
            convertedParameters.append(parameterSeparator)
        }
        if (convertedParameters.length > 0) convertedParameters.deleteCharAt(convertedParameters.length - 1)
        return convertedParameters.toString()
    }

    private fun encode(content: String?, encoding: String): String {
        return try {
            java.net.URLEncoder.encode(content, encoding)
        } catch (e: UnsupportedEncodingException) {
            throw IllegalArgumentException(e)
        }
    }
}
