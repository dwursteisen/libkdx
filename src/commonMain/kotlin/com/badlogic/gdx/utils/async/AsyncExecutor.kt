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

/** Allows asnynchronous execution of [AsyncTask] instances on a separate thread. Needs to be disposed via a call to
 * [.dispose] when no longer used, in which case the executor waits for running tasks to finish. Scheduled but not yet
 * running tasks will not be executed.
 * @author badlogic
 */
class AsyncExecutor @JvmOverloads constructor(maxConcurrent: Int, name: String? = "AsynchExecutor-Thread") : com.badlogic.gdx.utils.Disposable {

    private val executor: ExecutorService?
    /** Submits a [Runnable] to be executed asynchronously. If maxConcurrent runnables are already running, the runnable will
     * be queued.
     * @param task the task to execute asynchronously
     */
    fun <T> submit(task: com.badlogic.gdx.utils.async.AsyncTask<T?>?): com.badlogic.gdx.utils.async.AsyncResult<T?>? {
        if (executor.isShutdown()) {
            throw com.badlogic.gdx.utils.GdxRuntimeException("Cannot run tasks on an executor that has been shutdown (disposed)")
        }
        return com.badlogic.gdx.utils.async.AsyncResult<Any?>(executor.submit(object : java.util.concurrent.Callable<T?>() {
            @Throws(java.lang.Exception::class)
            override fun call(): T? {
                return task!!.call()
            }
        }))
    }

    /** Waits for running [AsyncTask] instances to finish, then destroys any resources like threads. Can not be used after
     * this method is called.  */
    override fun dispose() {
        executor.shutdown()
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            throw com.badlogic.gdx.utils.GdxRuntimeException("Couldn't shutdown loading thread", e)
        }
    }
    /** Creates a new AsynchExecutor that allows maxConcurrent [Runnable] instances to run in parallel.
     * @param maxConcurrent
     * @param name The name of the threads.
     */
    /** Creates a new AsynchExecutor with the name "AsynchExecutor-Thread".  */
    init {
        executor = Executors.newFixedThreadPool(maxConcurrent, object : ThreadFactory() {
            override fun newThread(r: Runnable?): java.lang.Thread? {
                val thread: java.lang.Thread = java.lang.Thread(r, name)
                thread.setDaemon(true)
                return thread
            }
        })
    }
}
