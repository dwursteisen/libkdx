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

class FloatAttribute : com.badlogic.gdx.graphics.g3d.Attribute {
    @JvmField
    var value = 0f

    constructor(type: Long) : super(type) {}
    constructor(type: Long, value: Float) : super(type) {
        this.value = value
    }

    override fun copy(): com.badlogic.gdx.graphics.g3d.Attribute? {
        return FloatAttribute(type, value)
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 977 * result + com.badlogic.gdx.utils.NumberUtils.floatToRawIntBits(value)
        return result
    }

    override operator fun compareTo(o: com.badlogic.gdx.graphics.g3d.Attribute?): Int {
        if (type != o.type) return (type - o.type)
        val v = (o as FloatAttribute?)!!.value
        return if (com.badlogic.gdx.math.MathUtils.isEqual(value, v)) 0 else if (value < v) -1 else 1
    }

    companion object {
        @JvmField
        val ShininessAlias: String? = "shininess"
        @JvmField
        val Shininess: Long = com.badlogic.gdx.graphics.g3d.Attribute.register(ShininessAlias)
        fun createShininess(value: Float): FloatAttribute? {
            return FloatAttribute(Shininess, value)
        }

        @JvmField
        val AlphaTestAlias: String? = "alphaTest"
        @JvmField
        val AlphaTest: Long = com.badlogic.gdx.graphics.g3d.Attribute.register(AlphaTestAlias)
        fun createAlphaTest(value: Float): FloatAttribute? {
            return FloatAttribute(AlphaTest, value)
        }
    }
}
