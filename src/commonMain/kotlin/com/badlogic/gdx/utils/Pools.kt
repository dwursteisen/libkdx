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

import com.badlogic.gdx.utils.Predicate.PredicateIterator
import com.badlogic.gdx.utils.PropertiesUtils
import com.badlogic.gdx.utils.Queue.QueueIterable
import com.badlogic.gdx.utils.Queue.QueueIterator
import com.badlogic.gdx.utils.Scaling
import com.badlogic.gdx.utils.ScreenUtils
import java.lang.IndexOutOfBoundsException
import java.lang.NullPointerException
import java.lang.RuntimeException
import kotlin.jvm.Throws

/**
 * Stores a map of [Pool]s (usually [ReflectionPool]s) by type for convenient static access.
 *
 * @author Nathan Sweet
 */
object Pools {

    private val typePools: ObjectMap<java.lang.Class?, Pool<*>?> = ObjectMap<Any?, Any?>()

    /**
     * Returns a new or existing pool for the specified type, stored in a Class to [Pool] map. Note the max size is ignored
     * if this is not the first time this pool has been requested.
     */
    operator fun <T> get(type: java.lang.Class<T>, max: Int): Pool<T>? {
        var pool = typePools[type]
        if (pool == null) {
            pool = ReflectionPool(type, 4, max)
            typePools.put(type, pool)
        }
        return pool
    }

    /**
     * Returns a new or existing pool for the specified type, stored in a Class to [Pool] map. The max size of the pool used
     * is 100.
     */
    operator fun <T> get(type: java.lang.Class<T>): Pool<T>? {
        return get(type, 100)
    }

    /**
     * Sets an existing pool for the specified type, stored in a Class to [Pool] map.
     */
    operator fun <T> set(type: java.lang.Class<T>?, pool: Pool<T>?) {
        typePools.put(type, pool)
    }

    /**
     * Obtains an object from the [pool][.get].
     */
    fun <T> obtain(type: java.lang.Class<T>): T {
        return get(type).obtain()
    }

    /**
     * Frees an object from the [pool][.get].
     */
    fun free(`object`: Any?) {
        if (`object` == null) throw IllegalArgumentException("Object cannot be null.")
        val pool = typePools[`object`.javaClass] ?: return
        // Ignore freeing an object that was never retained.
        pool.free(`object`)
    }

    /**
     * Frees the specified objects from the [pool][.get]. Null objects within the array are silently ignored. Objects
     * don't need to be from the same pool.
     */
    fun freeAll(objects: Array?) {
        freeAll(objects, false)
    }

    /**
     * Frees the specified objects from the [pool][.get]. Null objects within the array are silently ignored.
     *
     * @param samePool If true, objects don't need to be from the same pool but the pool must be looked up for each object.
     */
    fun freeAll(objects: Array?, samePool: Boolean) {
        if (objects == null) throw IllegalArgumentException("Objects cannot be null.")
        var pool: Pool<*>? = null
        var i = 0
        val n: Int = objects.size
        while (i < n) {
            val `object`: Any = objects.get(i)
            if (`object` == null) {
                i++
                continue
            }
            if (pool == null) {
                pool = typePools[`object`.javaClass]
                if (pool == null) {
                    i++
                    continue  // Ignore freeing an object that was never retained.
                }
            }
            pool.free(`object`)
            if (!samePool) pool = null
            i++
        }
    }
}
