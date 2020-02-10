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

class IntAttribute : com.badlogic.gdx.graphics.g3d.Attribute {
    @JvmField
    var value = 0

    constructor(type: Long) : super(type) {}
    constructor(type: Long, value: Int) : super(type) {
        this.value = value
    }

    override fun copy(): com.badlogic.gdx.graphics.g3d.Attribute? {
        return IntAttribute(type, value)
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 983 * result + value
        return result
    }

    override operator fun compareTo(o: com.badlogic.gdx.graphics.g3d.Attribute?): Int {
        return if (type != o.type) (type - o.type) else value - (o as IntAttribute?)!!.value
    }

    companion object {
        val CullFaceAlias: String? = "cullface"
        @JvmField
        val CullFace: Long = com.badlogic.gdx.graphics.g3d.Attribute.register(CullFaceAlias)
        fun createCullFace(value: Int): IntAttribute? {
            return IntAttribute(CullFace, value)
        }
    }
}
