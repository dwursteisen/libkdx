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

class ColorAttribute(type: Long) : com.badlogic.gdx.graphics.g3d.Attribute(type) {
    @JvmField
    val color: com.badlogic.gdx.graphics.Color? = com.badlogic.gdx.graphics.Color()

    constructor(type: Long, color: com.badlogic.gdx.graphics.Color?) : this(type) {
        if (color != null) this.color.set(color)
    }

    constructor(type: Long, r: Float, g: Float, b: Float, a: Float) : this(type) {
        color.set(r, g, b, a)
    }

    constructor(copyFrom: ColorAttribute?) : this(copyFrom.type, copyFrom!!.color) {}

    override fun copy(): com.badlogic.gdx.graphics.g3d.Attribute? {
        return ColorAttribute(this)
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 953 * result + color.toIntBits()
        return result
    }

    override operator fun compareTo(o: com.badlogic.gdx.graphics.g3d.Attribute?): Int {
        return if (type != o.type) (type - o.type) else (o as ColorAttribute?)!!.color.toIntBits() - color.toIntBits()
    }

    companion object {
        @JvmField
        val DiffuseAlias: String? = "diffuseColor"
        @JvmField
        val Diffuse: Long = com.badlogic.gdx.graphics.g3d.Attribute.register(DiffuseAlias)
        @JvmField
        val SpecularAlias: String? = "specularColor"
        @JvmField
        val Specular: Long = com.badlogic.gdx.graphics.g3d.Attribute.register(SpecularAlias)
        val AmbientAlias: String? = "ambientColor"
        @JvmField
        val Ambient: Long = com.badlogic.gdx.graphics.g3d.Attribute.register(AmbientAlias)
        @JvmField
        val EmissiveAlias: String? = "emissiveColor"
        @JvmField
        val Emissive: Long = com.badlogic.gdx.graphics.g3d.Attribute.register(EmissiveAlias)
        @JvmField
        val ReflectionAlias: String? = "reflectionColor"
        @JvmField
        val Reflection: Long = com.badlogic.gdx.graphics.g3d.Attribute.register(ReflectionAlias)
        val AmbientLightAlias: String? = "ambientLightColor"
        @JvmField
        val AmbientLight: Long = com.badlogic.gdx.graphics.g3d.Attribute.register(AmbientLightAlias)
        val FogAlias: String? = "fogColor"
        @JvmField
        val Fog: Long = com.badlogic.gdx.graphics.g3d.Attribute.register(FogAlias)
        protected var Mask = Ambient or Diffuse or Specular or Emissive or Reflection or AmbientLight or Fog
        fun `is`(mask: Long): Boolean {
            return mask and Mask != 0L
        }

        fun createAmbient(color: com.badlogic.gdx.graphics.Color?): ColorAttribute? {
            return ColorAttribute(Ambient, color)
        }

        fun createAmbient(r: Float, g: Float, b: Float, a: Float): ColorAttribute? {
            return ColorAttribute(Ambient, r, g, b, a)
        }

        fun createDiffuse(color: com.badlogic.gdx.graphics.Color?): ColorAttribute? {
            return ColorAttribute(Diffuse, color)
        }

        fun createDiffuse(r: Float, g: Float, b: Float, a: Float): ColorAttribute? {
            return ColorAttribute(Diffuse, r, g, b, a)
        }

        fun createSpecular(color: com.badlogic.gdx.graphics.Color?): ColorAttribute? {
            return ColorAttribute(Specular, color)
        }

        fun createSpecular(r: Float, g: Float, b: Float, a: Float): ColorAttribute? {
            return ColorAttribute(Specular, r, g, b, a)
        }

        fun createReflection(color: com.badlogic.gdx.graphics.Color?): ColorAttribute? {
            return ColorAttribute(Reflection, color)
        }

        fun createReflection(r: Float, g: Float, b: Float, a: Float): ColorAttribute? {
            return ColorAttribute(Reflection, r, g, b, a)
        }
    }

    init {
        if (!`is`(type)) throw com.badlogic.gdx.utils.GdxRuntimeException("Invalid type specified")
    }
}
