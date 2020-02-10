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
package com.badlogic.gdx.graphics.glutils

import com.badlogic.gdx.graphics.glutils.GLVersion.Type
import com.badlogic.gdx.graphics.glutils.HdpiMode
import com.badlogic.gdx.graphics.glutils.InstanceData
import java.io.BufferedInputStream
import java.lang.IllegalStateException
import java.lang.NumberFormatException
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import java.util.HashMap
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

class GLVersion(appType: com.badlogic.gdx.Application.ApplicationType?, versionString: String?, vendorString: String?, rendererString: String?) {
    /** @return the major version of current GL connection. -1 if running headless
     */
    var majorVersion = 0
        private set
    /** @return the minor version of the current GL connection. -1 if running headless
     */
    var minorVersion = 0
        private set
    /** @return the release version of the current GL connection. -1 if running headless
     */
    var releaseVersion = 0
        private set
    /** @return the vendor string associated with the current GL connection
     */
    val vendorString: String?
    /** @return the name of the renderer associated with the current GL connection.
     * This name is typically specific to a particular configuration of a hardware platform.
     */
    val rendererString: String?
    /** @return what [Type] of GL implementation this application has access to, e.g. [Type.OpenGL] or [Type.GLES]
     */
    var type: Type? = null
    private val TAG: String? = "GLVersion"
    private fun extractVersion(patternString: String?, versionString: String?) {
        val pattern: java.util.regex.Pattern = java.util.regex.Pattern.compile(patternString)
        val matcher: java.util.regex.Matcher = pattern.matcher(versionString)
        val found: Boolean = matcher.find()
        if (found) {
            val result: String = matcher.group(1)
            val resultSplit: Array<String?> = result.split("\\.").toTypedArray()
            majorVersion = parseInt(resultSplit[0], 2)
            minorVersion = if (resultSplit.size < 2) 0 else parseInt(resultSplit[1], 0)
            releaseVersion = if (resultSplit.size < 3) 0 else parseInt(resultSplit[2], 0)
        } else {
            com.badlogic.gdx.Gdx.app.log(TAG, "Invalid version string: $versionString")
            majorVersion = 2
            minorVersion = 0
            releaseVersion = 0
        }
    }

    /** Forgiving parsing of gl major, minor and release versions as some manufacturers don't adhere to spec  */
    private fun parseInt(v: String?, defaultValue: Int): Int {
        return try {
            v!!.toInt()
        } catch (nfe: NumberFormatException) {
            com.badlogic.gdx.Gdx.app.error("LibGDX GL", "Error parsing number: $v, assuming: $defaultValue")
            defaultValue
        }
    }

    /**
     * Checks to see if the current GL connection version is higher, or equal to the provided test versions.
     *
     * @param testMajorVersion the major version to test against
     * @param testMinorVersion the minor version to test against
     * @return true if the current version is higher or equal to the test version
     */
    fun isVersionEqualToOrHigher(testMajorVersion: Int, testMinorVersion: Int): Boolean {
        return majorVersion > testMajorVersion || majorVersion == testMajorVersion && minorVersion >= testMinorVersion
    }

    /** @return a string with the current GL connection data
     */
    val debugVersionString: String?
        get() = "Type: " + type + "\n" +
            "Version: " + majorVersion + ":" + minorVersion + ":" + releaseVersion + "\n" +
            "Vendor: " + vendorString + "\n" +
            "Renderer: " + rendererString

    enum class Type {
        OpenGL, GLES, WebGL, NONE
    }

    init {
        var vendorString = vendorString
        var rendererString = rendererString
        if (appType == com.badlogic.gdx.Application.ApplicationType.Android) type = Type.GLES else if (appType == com.badlogic.gdx.Application.ApplicationType.iOS) type = Type.GLES else if (appType == com.badlogic.gdx.Application.ApplicationType.Desktop) type = Type.OpenGL else if (appType == com.badlogic.gdx.Application.ApplicationType.Applet) type = Type.OpenGL else if (appType == com.badlogic.gdx.Application.ApplicationType.WebGL) type = Type.WebGL else type = Type.NONE
        if (type == Type.GLES) { //OpenGL<space>ES<space><version number><space><vendor-specific information>.
            extractVersion("OpenGL ES (\\d(\\.\\d){0,2})", versionString)
        } else if (type == Type.WebGL) { //WebGL<space><version number><space><vendor-specific information>
            extractVersion("WebGL (\\d(\\.\\d){0,2})", versionString)
        } else if (type == Type.OpenGL) { //<version number><space><vendor-specific information>
            extractVersion("(\\d(\\.\\d){0,2})", versionString)
        } else {
            majorVersion = -1
            minorVersion = -1
            releaseVersion = -1
            vendorString = ""
            rendererString = ""
        }
        this.vendorString = vendorString
        this.rendererString = rendererString
    }
}
