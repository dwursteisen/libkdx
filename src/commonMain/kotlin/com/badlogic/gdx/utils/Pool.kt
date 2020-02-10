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
 * A pool of objects that can be reused to avoid allocation.
 *
 * @author Nathan Sweet
 * @see Pools
 */
abstract class Pool<T> @JvmOverloads constructor(initialCapacity: Int = 16, max: Int = Int.MAX_VALUE) {

    /**
     * The maximum number of objects that will be pooled.
     */
    val max: Int

    /**
     * The highest number of free objects. Can be reset any time.
     */
    var peak = 0
    private val freeObjects: com.badlogic.gdx.utils.Array<T?>?
    protected abstract fun newObject(): T?

    /**
     * Returns an object from this pool. The object may be new (from [.newObject]) or reused (previously
     * [freed][.free]).
     */
    fun obtain(): T? {
        return if (freeObjects.size == 0) newObject() else freeObjects.pop()
    }

    /**
     * Puts the specified object in the pool, making it eligible to be returned by [.obtain]. If the pool already contains
     * [.max] free objects, the specified object is reset but not added to the pool.
     *
     *
     * The pool does not check if an object is already freed, so the same object must not be freed multiple times.
     */
    fun free(`object`: T?) {
        if (`object` == null) throw IllegalArgumentException("object cannot be null.")
        if (freeObjects.size < max) {
            freeObjects.add(`object`)
            peak = max(peak, freeObjects.size)
        }
        reset(`object`)
    }

    /**
     * Called when an object is freed to clear the state of the object for possible later reuse. The default implementation calls
     * [Poolable.reset] if the object is [Poolable].
     */
    protected fun reset(`object`: T?) {
        if (`object` is Poolable) (`object` as Poolable?)!!.reset()
    }

    /**
     * Puts the specified objects in the pool. Null objects within the array are silently ignored.
     *
     *
     * The pool does not check if an object is already freed, so the same object must not be freed multiple times.
     *
     * @see .free
     */
    fun freeAll(objects: com.badlogic.gdx.utils.Array<T?>?) {
        if (objects == null) throw IllegalArgumentException("objects cannot be null.")
        val freeObjects: com.badlogic.gdx.utils.Array<T?>? = freeObjects
        val max = max
        for (i in 0 until objects.size) {
            val `object`: T = objects.get(i) ?: continue
            if (freeObjects.size < max) freeObjects.add(`object`)
            reset(`object`)
        }
        peak = max(peak, freeObjects.size)
    }

    /**
     * Removes all free objects from this pool.
     */
    fun clear() {
        freeObjects.clear()
    }

    /**
     * The number of objects available to be obtained.
     */
    val free: Int
        get() = freeObjects.size

    /**
     * Objects implementing this interface will have [.reset] called when passed to [Pool.free].
     */
    interface Poolable {

        /**
         * Resets the object for reuse. Object references should be nulled and fields may be set to default values.
         */
        fun reset()
    }
    /**
     * @param max The maximum number of free objects to store in this pool.
     */
    /**
     * Creates a pool with an initial capacity of 16 and no maximum.
     */
    /**
     * Creates a pool with the specified initial capacity and no maximum.
     */
    init {
        freeObjects = com.badlogic.gdx.utils.Array(false, initialCapacity)
        this.max = max
    }
}
