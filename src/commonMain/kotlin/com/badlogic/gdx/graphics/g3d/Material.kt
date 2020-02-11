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
package com.badlogic.gdx.graphics.g3d

class Material @JvmOverloads constructor(id: String? = "mtl" + ++counter) : Attributes() {
    var id: String? = null

    /**
     * Create a material with the specified attributes
     */
    constructor(vararg attributes: Attribute?) : this() {
        set(attributes)
    }

    /**
     * Create a material with the specified attributes
     */
    constructor(id: String?, vararg attributes: Attribute?) : this(id) {
        set(attributes)
    }

    /**
     * Create a material with the specified attributes
     */
    constructor(attributes: Array<Attribute?>?) : this() {
        set(attributes)
    }

    /**
     * Create a material with the specified attributes
     */
    constructor(id: String?, attributes: Array<Attribute?>?) : this(id) {
        set(attributes)
    }

    /**
     * Create a material which is an exact copy of the specified material
     */
    constructor(copyFrom: Material) : this(copyFrom.id, copyFrom) {}

    /**
     * Create a material which is an exact copy of the specified material
     */
    constructor(id: String?, copyFrom: Material) : this(id) {
        for (attr in copyFrom) set(attr.copy())
    }

    /**
     * Create a copy of this material
     */
    fun copy(): Material {
        return Material(this)
    }

    override fun hashCode(): Int {
        return super.hashCode() + 3 * id.hashCode()
    }

    override fun equals(other: Any): Boolean {
        return other is Material && (other === this || other.id == id && super.equals(other))
    }

    companion object {
        private var counter = 0
    }
    /**
     * Create an empty material
     */
    /**
     * Create an empty material
     */
    init {
        this.id = id
    }
}
