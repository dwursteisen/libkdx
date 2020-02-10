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
import kotlin.jvm.JvmStatic

class BlendingAttribute(
    /** Whether this material should be considered blended (default: true). This is used for sorting (back to front instead of front
     * to back).  */
    var blended: Boolean,
    /** Specifies how the (incoming) red, green, blue, and alpha source blending factors are computed (default: GL_SRC_ALPHA)  */
    var sourceFunction: Int,
    /** Specifies how the (existing) red, green, blue, and alpha destination blending factors are computed (default:
     * GL_ONE_MINUS_SRC_ALPHA)  */
    var destFunction: Int, opacity: Float) : com.badlogic.gdx.graphics.g3d.Attribute(Type) {

    /** The opacity used as source alpha value, ranging from 0 (fully transparent) to 1 (fully opaque), (default: 1).  */
    @JvmField
    var opacity = 1f

    @JvmOverloads
    constructor(sourceFunc: Int, destFunc: Int, opacity: Float = 1f) : this(true, sourceFunc, destFunc, opacity) {
    }

    constructor(blended: Boolean, opacity: Float) : this(blended, com.badlogic.gdx.graphics.GL20.GL_SRC_ALPHA, com.badlogic.gdx.graphics.GL20.GL_ONE_MINUS_SRC_ALPHA, opacity) {}
    constructor(opacity: Float) : this(true, opacity) {}
    @JvmOverloads
    constructor(copyFrom: BlendingAttribute? = null) : this(copyFrom == null || copyFrom.blended, copyFrom?.sourceFunction
        ?: com.badlogic.gdx.graphics.GL20.GL_SRC_ALPHA,
        copyFrom?.destFunction ?: com.badlogic.gdx.graphics.GL20.GL_ONE_MINUS_SRC_ALPHA, copyFrom?.opacity ?: 1f) {
    }

    override fun copy(): BlendingAttribute? {
        return BlendingAttribute(this)
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 947 * result + if (blended) 1 else 0
        result = 947 * result + sourceFunction
        result = 947 * result + destFunction
        result = 947 * result + com.badlogic.gdx.utils.NumberUtils.floatToRawIntBits(opacity)
        return result
    }

    override operator fun compareTo(o: com.badlogic.gdx.graphics.g3d.Attribute?): Int {
        if (type != o.type) return (type - o.type)
        val other = o as BlendingAttribute?
        if (blended != other!!.blended) return if (blended) 1 else -1
        if (sourceFunction != other.sourceFunction) return sourceFunction - other.sourceFunction
        if (destFunction != other.destFunction) return destFunction - other.destFunction
        return if (com.badlogic.gdx.math.MathUtils.isEqual(opacity, other.opacity)) 0 else if (opacity < other.opacity) 1 else -1
    }

    companion object {
        @JvmField
        val Alias: String? = "blended"
        @JvmField
        val Type: Long = com.badlogic.gdx.graphics.g3d.Attribute.register(Alias)
        @JvmStatic
        fun `is`(mask: Long): Boolean {
            return mask and Type == mask
        }
    }

    init {
        this.opacity = opacity
    }
}
