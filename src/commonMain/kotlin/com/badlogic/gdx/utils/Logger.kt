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
import com.badlogic.gdx.utils.JsonWriter
import com.badlogic.gdx.utils.LongArray
import com.badlogic.gdx.utils.LongMap
import com.badlogic.gdx.utils.ObjectFloatMap
import java.lang.IllegalStateException
import java.lang.IndexOutOfBoundsException
import kotlin.jvm.Throws

/**
 * Simple logger that uses the [Application] logging facilities to output messages. The log level set with
 * [Application.setLogLevel] overrides the log level set here.
 *
 * @author mzechner
 * @author Nathan Sweet
 */
class Logger @JvmOverloads constructor(private val tag: String,
                                       /**
                                        * Sets the log level. [.NONE] will mute all log output. [.ERROR] will only let error messages through.
                                        * [.INFO] will let all non-debug messages through, and [.DEBUG] will let all messages through.
                                        *
                                        * @param level [.NONE], [.ERROR], [.INFO], [.DEBUG].
                                        */
                                       var level: Int = ERROR) {

    fun debug(message: String?) {
        if (level >= DEBUG) Gdx.app.debug(tag, message)
    }

    fun debug(message: String?, exception: java.lang.Exception?) {
        if (level >= DEBUG) Gdx.app.debug(tag, message, exception)
    }

    fun info(message: String?) {
        if (level >= INFO) Gdx.app.log(tag, message)
    }

    fun info(message: String?, exception: java.lang.Exception?) {
        if (level >= INFO) Gdx.app.log(tag, message, exception)
    }

    fun error(message: String?) {
        if (level >= ERROR) Gdx.app.error(tag, message)
    }

    fun error(message: String?, exception: Throwable?) {
        if (level >= ERROR) Gdx.app.error(tag, message, exception)
    }

    companion object {
        const val NONE = 0
        const val ERROR = 1
        const val INFO = 2
        const val DEBUG = 3
    }
}
