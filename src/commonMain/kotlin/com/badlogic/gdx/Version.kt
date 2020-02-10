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
package com.badlogic.gdx

import com.badlogic.gdx.utils.GdxRuntimeException

/** The version of libgdx
 *
 * @author mzechner
 */
object Version {

    /** the current version of libgdx as a String in the major.minor.revision format  */
    const val VERSION = "1.9.11"

    /** the current major version of libgdx  */
    var MAJOR = 0

    /** the current minor version of libgdx  */
    var MINOR = 0

    /** the current revision version of libgdx  */
    var REVISION = 0
    fun isHigher(major: Int, minor: Int, revision: Int): Boolean {
        return isHigherEqual(major, minor, revision + 1)
    }

    fun isHigherEqual(major: Int, minor: Int, revision: Int): Boolean {
        if (MAJOR != major) return MAJOR > major
        return if (MINOR != minor) MINOR > minor else REVISION >= revision
    }

    fun isLower(major: Int, minor: Int, revision: Int): Boolean {
        return isLowerEqual(major, minor, revision - 1)
    }

    fun isLowerEqual(major: Int, minor: Int, revision: Int): Boolean {
        if (MAJOR != major) return MAJOR < major
        return if (MINOR != minor) MINOR < minor else REVISION <= revision
    }

    init {
        try {
            val v = VERSION.split("\\.").toTypedArray()
            MAJOR = if (v.size < 1) 0 else v.get(0).toInt()
            MINOR = if (v.size < 2) 0 else v.get(1).toInt()
            REVISION = if (v.size < 3) 0 else v.get(2).toInt()
        } catch (t: Throwable) {
            // Should never happen
            throw GdxRuntimeException("Invalid version $VERSION", t)
        }
    }
}
