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

/**
 * The ApplicationLogger provides an interface for a LibGDX Application to log messages and exceptions.
 * A default implementations is provided for each backend, custom implementations can be provided and set using
 * [Application.setApplicationLogger]
 */
interface ApplicationLogger {

    /** Logs a message with a tag  */
    fun log(tag: String, message: String)

    /** Logs a message and exception with a tag  */
    fun log(tag: String, message: String, exception: Throwable)

    /** Logs an error message with a tag  */
    fun error(tag: String, message: String)

    /** Logs an error message and exception with a tag  */
    fun error(tag: String, message: String, exception: Throwable)

    /** Logs a debug message with a tag  */
    fun debug(tag: String, message: String)

    /** Logs a debug message and exception with a tag  */
    fun debug(tag: String, message: String, exception: Throwable)
}
