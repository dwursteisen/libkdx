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

class TextureAttribute(type: Long) : com.badlogic.gdx.graphics.g3d.Attribute(type) {
    @JvmField
    val textureDescription: com.badlogic.gdx.graphics.g3d.utils.TextureDescriptor<com.badlogic.gdx.graphics.Texture?>?
    @JvmField
    var offsetU = 0f
    @JvmField
    var offsetV = 0f
    @JvmField
    var scaleU = 1f
    @JvmField
    var scaleV = 1f
    /** The index of the texture coordinate vertex attribute to use for this TextureAttribute. Whether this value is used, depends
     * on the shader and [Attribute.type] value. For basic (model specific) types (e.g. [.Diffuse], [.Normal],
     * etc.), this value is usually ignored and the first texture coordinate vertex attribute is used.  */
    var uvIndex = 0

    constructor(type: Long, textureDescription: com.badlogic.gdx.graphics.g3d.utils.TextureDescriptor<T?>?) : this(type) {
        this.textureDescription.set(textureDescription)
    }

    constructor(type: Long, textureDescription: com.badlogic.gdx.graphics.g3d.utils.TextureDescriptor<T?>?, offsetU: Float,
                offsetV: Float, scaleU: Float, scaleV: Float, uvIndex: Int) : this(type, textureDescription) {
        this.offsetU = offsetU
        this.offsetV = offsetV
        this.scaleU = scaleU
        this.scaleV = scaleV
        this.uvIndex = uvIndex
    }

    constructor(type: Long, textureDescription: com.badlogic.gdx.graphics.g3d.utils.TextureDescriptor<T?>?, offsetU: Float,
                offsetV: Float, scaleU: Float, scaleV: Float) : this(type, textureDescription, offsetU, offsetV, scaleU, scaleV, 0) {
    }

    constructor(type: Long, texture: com.badlogic.gdx.graphics.Texture?) : this(type) {
        textureDescription.texture = texture
    }

    constructor(type: Long, region: com.badlogic.gdx.graphics.g2d.TextureRegion?) : this(type) {
        set(region)
    }

    constructor(copyFrom: TextureAttribute?) : this(copyFrom.type, copyFrom!!.textureDescription, copyFrom.offsetU, copyFrom.offsetV, copyFrom.scaleU, copyFrom.scaleV,
        copyFrom.uvIndex) {
    }

    fun set(region: com.badlogic.gdx.graphics.g2d.TextureRegion?) {
        textureDescription.texture = region.getTexture()
        offsetU = region.getU()
        offsetV = region.getV()
        scaleU = region.getU2() - offsetU
        scaleV = region.getV2() - offsetV
    }

    override fun copy(): com.badlogic.gdx.graphics.g3d.Attribute? {
        return TextureAttribute(this)
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 991 * result + textureDescription.hashCode()
        result = 991 * result + com.badlogic.gdx.utils.NumberUtils.floatToRawIntBits(offsetU)
        result = 991 * result + com.badlogic.gdx.utils.NumberUtils.floatToRawIntBits(offsetV)
        result = 991 * result + com.badlogic.gdx.utils.NumberUtils.floatToRawIntBits(scaleU)
        result = 991 * result + com.badlogic.gdx.utils.NumberUtils.floatToRawIntBits(scaleV)
        result = 991 * result + uvIndex
        return result
    }

    override operator fun compareTo(o: com.badlogic.gdx.graphics.g3d.Attribute?): Int {
        if (type != o.type) return if (type < o.type) -1 else 1
        val other = o as TextureAttribute?
        val c: Int = textureDescription.compareTo(other!!.textureDescription)
        if (c != 0) return c
        if (uvIndex != other.uvIndex) return uvIndex - other.uvIndex
        if (!com.badlogic.gdx.math.MathUtils.isEqual(scaleU, other.scaleU)) return if (scaleU > other.scaleU) 1 else -1
        if (!com.badlogic.gdx.math.MathUtils.isEqual(scaleV, other.scaleV)) return if (scaleV > other.scaleV) 1 else -1
        if (!com.badlogic.gdx.math.MathUtils.isEqual(offsetU, other.offsetU)) return if (offsetU > other.offsetU) 1 else -1
        return if (!com.badlogic.gdx.math.MathUtils.isEqual(offsetV, other.offsetV)) if (offsetV > other.offsetV) 1 else -1 else 0
    }

    companion object {
        @JvmField
        val DiffuseAlias: String? = "diffuseTexture"
        @JvmField
        val Diffuse: Long = com.badlogic.gdx.graphics.g3d.Attribute.register(DiffuseAlias)
        @JvmField
        val SpecularAlias: String? = "specularTexture"
        @JvmField
        val Specular: Long = com.badlogic.gdx.graphics.g3d.Attribute.register(SpecularAlias)
        val BumpAlias: String? = "bumpTexture"
        @JvmField
        val Bump: Long = com.badlogic.gdx.graphics.g3d.Attribute.register(BumpAlias)
        @JvmField
        val NormalAlias: String? = "normalTexture"
        @JvmField
        val Normal: Long = com.badlogic.gdx.graphics.g3d.Attribute.register(NormalAlias)
        @JvmField
        val AmbientAlias: String? = "ambientTexture"
        @JvmField
        val Ambient: Long = com.badlogic.gdx.graphics.g3d.Attribute.register(AmbientAlias)
        @JvmField
        val EmissiveAlias: String? = "emissiveTexture"
        @JvmField
        val Emissive: Long = com.badlogic.gdx.graphics.g3d.Attribute.register(EmissiveAlias)
        @JvmField
        val ReflectionAlias: String? = "reflectionTexture"
        @JvmField
        val Reflection: Long = com.badlogic.gdx.graphics.g3d.Attribute.register(ReflectionAlias)
        protected var Mask = Diffuse or Specular or Bump or Normal or Ambient or Emissive or Reflection
        fun `is`(mask: Long): Boolean {
            return mask and Mask != 0L
        }

        fun createDiffuse(texture: com.badlogic.gdx.graphics.Texture?): TextureAttribute? {
            return TextureAttribute(Diffuse, texture)
        }

        fun createDiffuse(region: com.badlogic.gdx.graphics.g2d.TextureRegion?): TextureAttribute? {
            return TextureAttribute(Diffuse, region)
        }

        fun createSpecular(texture: com.badlogic.gdx.graphics.Texture?): TextureAttribute? {
            return TextureAttribute(Specular, texture)
        }

        fun createSpecular(region: com.badlogic.gdx.graphics.g2d.TextureRegion?): TextureAttribute? {
            return TextureAttribute(Specular, region)
        }

        fun createNormal(texture: com.badlogic.gdx.graphics.Texture?): TextureAttribute? {
            return TextureAttribute(Normal, texture)
        }

        fun createNormal(region: com.badlogic.gdx.graphics.g2d.TextureRegion?): TextureAttribute? {
            return TextureAttribute(Normal, region)
        }

        fun createBump(texture: com.badlogic.gdx.graphics.Texture?): TextureAttribute? {
            return TextureAttribute(Bump, texture)
        }

        fun createBump(region: com.badlogic.gdx.graphics.g2d.TextureRegion?): TextureAttribute? {
            return TextureAttribute(Bump, region)
        }

        fun createAmbient(texture: com.badlogic.gdx.graphics.Texture?): TextureAttribute? {
            return TextureAttribute(Ambient, texture)
        }

        fun createAmbient(region: com.badlogic.gdx.graphics.g2d.TextureRegion?): TextureAttribute? {
            return TextureAttribute(Ambient, region)
        }

        fun createEmissive(texture: com.badlogic.gdx.graphics.Texture?): TextureAttribute? {
            return TextureAttribute(Emissive, texture)
        }

        fun createEmissive(region: com.badlogic.gdx.graphics.g2d.TextureRegion?): TextureAttribute? {
            return TextureAttribute(Emissive, region)
        }

        fun createReflection(texture: com.badlogic.gdx.graphics.Texture?): TextureAttribute? {
            return TextureAttribute(Reflection, texture)
        }

        fun createReflection(region: com.badlogic.gdx.graphics.g2d.TextureRegion?): TextureAttribute? {
            return TextureAttribute(Reflection, region)
        }
    }

    init {
        if (!`is`(type)) throw com.badlogic.gdx.utils.GdxRuntimeException("Invalid type specified")
        textureDescription = com.badlogic.gdx.graphics.g3d.utils.TextureDescriptor()
    }
}
