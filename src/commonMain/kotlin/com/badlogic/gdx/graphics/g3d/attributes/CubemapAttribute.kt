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
package com.badlogic.gdx.graphics.g3d.attributes

import kotlin.jvm.JvmField

class CubemapAttribute(type: Long) : com.badlogic.gdx.graphics.g3d.Attribute(type) {
    @JvmField
    val textureDescription: com.badlogic.gdx.graphics.g3d.utils.TextureDescriptor<com.badlogic.gdx.graphics.Cubemap?>?

    constructor(type: Long, textureDescription: com.badlogic.gdx.graphics.g3d.utils.TextureDescriptor<T?>?) : this(type) {
        this.textureDescription.set(textureDescription)
    }

    constructor(type: Long, texture: com.badlogic.gdx.graphics.Cubemap?) : this(type) {
        textureDescription.texture = texture
    }

    constructor(copyFrom: CubemapAttribute?) : this(copyFrom.type, copyFrom!!.textureDescription) {}

    override fun copy(): com.badlogic.gdx.graphics.g3d.Attribute? {
        return CubemapAttribute(this)
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 967 * result + textureDescription.hashCode()
        return result
    }

    override operator fun compareTo(o: com.badlogic.gdx.graphics.g3d.Attribute?): Int {
        return if (type != o.type) (type - o.type) else textureDescription.compareTo((o as CubemapAttribute?)!!.textureDescription)
    }

    companion object {
        val EnvironmentMapAlias: String? = "environmentCubemap"
        @JvmField
        val EnvironmentMap: Long = com.badlogic.gdx.graphics.g3d.Attribute.register(EnvironmentMapAlias)
        protected var Mask = EnvironmentMap
        fun `is`(mask: Long): Boolean {
            return mask and Mask != 0L
        }
    }

    init {
        if (!`is`(type)) throw com.badlogic.gdx.utils.GdxRuntimeException("Invalid type specified")
        textureDescription = com.badlogic.gdx.graphics.g3d.utils.TextureDescriptor()
    }
}
