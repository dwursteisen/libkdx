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

import java.io.IOException
import java.lang.ArrayIndexOutOfBoundsException
import java.util.Arrays
import kotlin.jvm.Throws

/**
 * Wrapper around System.nanoTime() and System.currentTimeMillis(). Use this if you want to be compatible across all platforms!
 *
 * @author mzechner
 */
object TimeUtils {

    /**
     * @return The current value of the system timer, in nanoseconds.
     */
    fun nanoTime(): Long {
        return java.lang.System.nanoTime()
    }

    /**
     * @return the difference, measured in milliseconds, between the current time and midnight, January 1, 1970 UTC.
     */
    fun millis(): Long {
        return java.lang.System.currentTimeMillis()
    }

    private const val nanosPerMilli: Long = 1000000

    /**
     * Convert nanoseconds time to milliseconds
     *
     * @param nanos must be nanoseconds
     * @return time value in milliseconds
     */
    fun nanosToMillis(nanos: Long): Long {
        return nanos / nanosPerMilli
    }

    /**
     * Convert milliseconds time to nanoseconds
     *
     * @param millis must be milliseconds
     * @return time value in nanoseconds
     */
    fun millisToNanos(millis: Long): Long {
        return millis * nanosPerMilli
    }

    /**
     * Get the time in nanos passed since a previous time
     *
     * @param prevTime - must be nanoseconds
     * @return - time passed since prevTime in nanoseconds
     */
    fun timeSinceNanos(prevTime: Long): Long {
        return nanoTime() - prevTime
    }

    /**
     * Get the time in millis passed since a previous time
     *
     * @param prevTime - must be milliseconds
     * @return - time passed since prevTime in milliseconds
     */
    fun timeSinceMillis(prevTime: Long): Long {
        return millis() - prevTime
    }
}
