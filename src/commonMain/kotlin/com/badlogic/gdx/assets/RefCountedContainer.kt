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
package com.badlogic.gdx.assets

class RefCountedContainer(`object`: Any?) {
    var `object`: Any
    var refCount = 1
    fun incRefCount() {
        refCount++
    }

    fun decRefCount() {
        refCount--
    }

    fun <T> getObject(type: java.lang.Class<T>?): T {
        return `object` as T
    }

    fun setObject(asset: Any) {
        `object` = asset
    }

    init {
        if (`object` == null) throw java.lang.IllegalArgumentException("Object must not be null")
        this.`object` = `object`
    }
}
