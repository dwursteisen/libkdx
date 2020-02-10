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

class DepthTestAttribute(type: Long, depthFunc: Int, depthRangeNear: Float, depthRangeFar: Float, depthMask: Boolean) : com.badlogic.gdx.graphics.g3d.Attribute(type) {
    /** The depth test function, or 0 to disable depth test (default: GL10.GL_LEQUAL)  */
    @JvmField
    var depthFunc: Int
    /** Mapping of near clipping plane to window coordinates (default: 0)  */
    @JvmField
    var depthRangeNear: Float
    /** Mapping of far clipping plane to window coordinates (default: 1)  */
    @JvmField
    var depthRangeFar: Float
    /** Whether to write to the depth buffer (default: true)  */
    @JvmField
    var depthMask: Boolean

    constructor(depthMask: Boolean) : this(com.badlogic.gdx.graphics.GL20.GL_LEQUAL, depthMask) {}
    @JvmOverloads
    constructor(depthFunc: Int = com.badlogic.gdx.graphics.GL20.GL_LEQUAL, depthMask: Boolean = true) : this(depthFunc, 0f, 1f, depthMask) {
    }

    @JvmOverloads
    constructor(depthFunc: Int, depthRangeNear: Float, depthRangeFar: Float, depthMask: Boolean = true) : this(Type, depthFunc, depthRangeNear, depthRangeFar, depthMask) {
    }

    constructor(rhs: DepthTestAttribute?) : this(rhs.type, rhs!!.depthFunc, rhs.depthRangeNear, rhs.depthRangeFar, rhs.depthMask) {}

    override fun copy(): com.badlogic.gdx.graphics.g3d.Attribute? {
        return DepthTestAttribute(this)
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 971 * result + depthFunc
        result = 971 * result + com.badlogic.gdx.utils.NumberUtils.floatToRawIntBits(depthRangeNear)
        result = 971 * result + com.badlogic.gdx.utils.NumberUtils.floatToRawIntBits(depthRangeFar)
        result = 971 * result + if (depthMask) 1 else 0
        return result
    }

    override operator fun compareTo(o: com.badlogic.gdx.graphics.g3d.Attribute?): Int {
        if (type != o.type) return (type - o.type)
        val other = o as DepthTestAttribute?
        if (depthFunc != other!!.depthFunc) return depthFunc - other.depthFunc
        if (depthMask != other.depthMask) return if (depthMask) -1 else 1
        if (!com.badlogic.gdx.math.MathUtils.isEqual(depthRangeNear, other.depthRangeNear)) return if (depthRangeNear < other.depthRangeNear) -1 else 1
        return if (!com.badlogic.gdx.math.MathUtils.isEqual(depthRangeFar, other.depthRangeFar)) if (depthRangeFar < other.depthRangeFar) -1 else 1 else 0
    }

    companion object {
        val Alias: String? = "depthStencil"
        @JvmField
        val Type: Long = com.badlogic.gdx.graphics.g3d.Attribute.register(Alias)
        protected var Mask = Type
        fun `is`(mask: Long): Boolean {
            return mask and Mask != 0L
        }
    }

    init {
        if (!`is`(type)) throw com.badlogic.gdx.utils.GdxRuntimeException("Invalid type specified")
        this.depthFunc = depthFunc
        this.depthRangeNear = depthRangeNear
        this.depthRangeFar = depthRangeFar
        this.depthMask = depthMask
    }
}
