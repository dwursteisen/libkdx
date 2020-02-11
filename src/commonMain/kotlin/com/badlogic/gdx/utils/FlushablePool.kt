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
 * A [Pool] which keeps track of the obtained items (see [.obtain]), which can be free'd all at once using the
 * [.flush] method.
 *
 * @author Xoppa
 */
abstract class FlushablePool<T> : Pool<T> {

    protected var obtained = Array<T?>()

    constructor() : super() {}
    constructor(initialCapacity: Int) : super(initialCapacity) {}
    constructor(initialCapacity: Int, max: Int) : super(initialCapacity, max) {}

    override fun obtain(): T? {
        val result = super.obtain()
        obtained.add(result)
        return result
    }

    /**
     * Frees all obtained instances.
     */
    fun flush() {
        super.freeAll(obtained)
        obtained.clear()
    }

    override fun free(`object`: T?) {
        obtained.removeValue(`object`, true)
        super.free(`object`)
    }

    override fun freeAll(objects: Array<T?>?) {
        obtained.removeAll(objects!!, true)
        super.freeAll(objects)
    }
}
