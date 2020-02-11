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
package com.badlogic.gdx.graphics.g3d.utils

import Texture.TextureFilter
import Texture.TextureWrap
import com.badlogic.gdx.graphics.g3d.utils.TextureProvider

class TextureDescriptor<T : GLTexture?> : Comparable<TextureDescriptor<T>?> {
    var texture: T? = null
    var minFilter: TextureFilter? = null
    var magFilter: TextureFilter? = null
    var uWrap: TextureWrap? = null
    var vWrap: TextureWrap? = null

    // TODO add other values, see http://www.opengl.org/sdk/docs/man/xhtml/glTexParameter.xml
    @JvmOverloads
    constructor(texture: T, minFilter: TextureFilter? = null, magFilter: TextureFilter? = null,
                uWrap: TextureWrap? = null, vWrap: TextureWrap? = null) {
        set(texture, minFilter, magFilter, uWrap, vWrap)
    }

    constructor() {}

    operator fun set(texture: T, minFilter: TextureFilter?, magFilter: TextureFilter?,
                     uWrap: TextureWrap?, vWrap: TextureWrap?) {
        this.texture = texture
        this.minFilter = minFilter
        this.magFilter = magFilter
        this.uWrap = uWrap
        this.vWrap = vWrap
    }

    fun <V : T?> set(other: TextureDescriptor<V>) {
        texture = other.texture
        minFilter = other.minFilter
        magFilter = other.magFilter
        uWrap = other.uWrap
        vWrap = other.vWrap
    }

    override fun equals(obj: Any?): Boolean {
        if (obj == null) return false
        if (obj === this) return true
        if (obj !is TextureDescriptor<*>) return false
        val other = obj
        return other.texture === texture && other.minFilter === minFilter && other.magFilter === magFilter && other.uWrap === uWrap && other.vWrap === vWrap
    }

    override fun hashCode(): Int {
        var result = (if (texture == null) 0 else texture.glTarget).toLong()
        result = 811 * result + if (texture == null) 0 else texture.getTextureObjectHandle()
        result = 811 * result + if (minFilter == null) 0 else minFilter.getGLEnum()
        result = 811 * result + if (magFilter == null) 0 else magFilter.getGLEnum()
        result = 811 * result + if (uWrap == null) 0 else uWrap.getGLEnum()
        result = 811 * result + if (vWrap == null) 0 else vWrap.getGLEnum()
        return (result xor (result shr 32)).toInt()
    }

    override operator fun compareTo(o: TextureDescriptor<T?>): Int {
        if (o === this) return 0
        val t1 = if (texture == null) 0 else texture.glTarget
        val t2 = if (o.texture == null) 0 else o.texture.glTarget
        if (t1 != t2) return t1 - t2
        val h1 = if (texture == null) 0 else texture.getTextureObjectHandle()
        val h2 = if (o.texture == null) 0 else o.texture.getTextureObjectHandle()
        if (h1 != h2) return h1 - h2
        if (minFilter !== o.minFilter) return (if (minFilter == null) 0 else minFilter.getGLEnum()) - if (o.minFilter == null) 0 else o.minFilter.getGLEnum()
        if (magFilter !== o.magFilter) return (if (magFilter == null) 0 else magFilter.getGLEnum()) - if (o.magFilter == null) 0 else o.magFilter.getGLEnum()
        if (uWrap !== o.uWrap) return (if (uWrap == null) 0 else uWrap.getGLEnum()) - if (o.uWrap == null) 0 else o.uWrap.getGLEnum()
        return if (vWrap !== o.vWrap) (if (vWrap == null) 0 else vWrap.getGLEnum()) - (if (o.vWrap == null) 0 else o.vWrap.getGLEnum()) else 0
    }
}
