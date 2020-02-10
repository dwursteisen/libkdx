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
package com.badlogic.gdx.utils.async

import java.lang.InterruptedException
import java.lang.Runnable
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.TimeUnit
import kotlin.jvm.Throws

/** Returned by [AsyncExecutor.submit], allows to poll for the result of the asynch workload.
 * @author badlogic
 */
class AsyncResult<T> internal constructor(future: java.util.concurrent.Future<T?>?) {

    private val future: java.util.concurrent.Future<T?>?
    /** @return whether the [AsyncTask] is done
     */
    val isDone: Boolean
        get() = future.isDone()

    /** @return waits if necessary for the computation to complete and then returns the result
     * @throws GdxRuntimeException if there was an error
     */
    fun get(): T? {
        return try {
            future.get()
        } catch (ex: InterruptedException) {
            null
        } catch (ex: ExecutionException) {
            throw com.badlogic.gdx.utils.GdxRuntimeException(ex.cause)
        }
    }

    init {
        this.future = future
    }
}
