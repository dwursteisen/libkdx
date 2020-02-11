package com.badlogic.gdx.graphics

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Pixmap.Blending
import com.badlogic.gdx.graphics.Texture.TextureFilter
import com.badlogic.gdx.graphics.Texture.TextureWrap
import com.badlogic.gdx.graphics.TextureData.TextureDataType
import com.badlogic.gdx.graphics.glutils.MipMapGenerator
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.utils.BufferUtils
import com.badlogic.gdx.utils.Disposable
import kotlin.jvm.JvmOverloads
import kotlin.math.min

/**
 * Class representing an OpenGL texture by its target and handle. Keeps track of its state like the TextureFilter and TextureWrap.
 * Also provides some (protected) static methods to create TextureData and upload image data.
 *
 * @author badlogic, Xoppa
 */
abstract class GLTexture
/**
 * Generates a new OpenGL texture with the specified target.
 */ @JvmOverloads constructor(
    /**
     * The target of this texture, used when binding the texture, e.g. GL_TEXTURE_2D
     */
    val glTarget: Int,
    /**
     * @return The OpenGL handle for this texture.
     */
    var textureObjectHandle: Int = Gdx.gl.glGenTexture()) : Disposable {

    protected var minFilter: TextureFilter = TextureFilter.Nearest
    protected var magFilter: TextureFilter = TextureFilter.Nearest
    protected var uWrap: TextureWrap = TextureWrap.ClampToEdge
    protected var vWrap: TextureWrap = TextureWrap.ClampToEdge
    protected var anisotropicFilterLevel = 1.0f

    /**
     * @return The currently set anisotropic filtering level for the texture, or 1.0f if none has been set.
     */
    var anisotropicFilter = 1.0f
        protected set

    /**
     * @return the width of the texture in pixels
     */
    abstract val width: Int

    /**
     * @return the height of the texture in pixels
     */
    abstract val height: Int

    /**
     * @return the depth of the texture in pixels
     */
    abstract val depth: Int

    /**
     * @return whether this texture is managed or not.
     */
    abstract val isManaged: Boolean
    protected abstract fun reload()

    /**
     * Binds this texture. The texture will be bound to the currently active texture unit specified via
     * [GL20.glActiveTexture].
     */
    fun bind() {
        Gdx.gl.glBindTexture(glTarget, textureObjectHandle)
    }

    /**
     * Binds the texture to the given texture unit. Sets the currently active texture unit via [GL20.glActiveTexture].
     *
     * @param unit the unit (0 to MAX_TEXTURE_UNITS).
     */
    fun bind(unit: Int) {
        Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0 + unit)
        Gdx.gl.glBindTexture(glTarget, textureObjectHandle)
    }

    /**
     * @return The [Texture.TextureFilter] used for minification.
     */
    fun getMinFilter(): TextureFilter {
        return minFilter
    }

    /**
     * @return The [Texture.TextureFilter] used for magnification.
     */
    fun getMagFilter(): TextureFilter {
        return magFilter
    }

    /**
     * @return The [Texture.TextureWrap] used for horizontal (U) texture coordinates.
     */
    fun getUWrap(): TextureWrap {
        return uWrap
    }

    /**
     * @return The [Texture.TextureWrap] used for vertical (V) texture coordinates.
     */
    fun getVWrap(): TextureWrap {
        return vWrap
    }

    /**
     * Sets the [TextureWrap] for this texture on the u and v axis. Assumes the texture is bound and active!
     *
     * @param u the u wrap
     * @param v the v wrap
     */
    fun unsafeSetWrap(u: TextureWrap?, v: TextureWrap?) {
        unsafeSetWrap(u, v, false)
    }

    /**
     * Sets the [TextureWrap] for this texture on the u and v axis. Assumes the texture is bound and active!
     *
     * @param u     the u wrap
     * @param v     the v wrap
     * @param force True to always set the values, even if they are the same as the current values.
     */
    fun unsafeSetWrap(u: TextureWrap?, v: TextureWrap?, force: Boolean) {
        if (u != null && (force || uWrap !== u)) {
            Gdx.gl.glTexParameteri(glTarget, GL20.GL_TEXTURE_WRAP_S, u.gLEnum)
            uWrap = u
        }
        if (v != null && (force || vWrap !== v)) {
            Gdx.gl.glTexParameteri(glTarget, GL20.GL_TEXTURE_WRAP_T, v.gLEnum)
            vWrap = v
        }
    }

    /**
     * Sets the [TextureWrap] for this texture on the u and v axis. This will bind this texture!
     *
     * @param u the u wrap
     * @param v the v wrap
     */
    fun setWrap(u: TextureWrap, v: TextureWrap) {
        uWrap = u
        vWrap = v
        bind()
        Gdx.gl.glTexParameteri(glTarget, GL20.GL_TEXTURE_WRAP_S, u.gLEnum)
        Gdx.gl.glTexParameteri(glTarget, GL20.GL_TEXTURE_WRAP_T, v.gLEnum)
    }

    /**
     * Sets the [TextureFilter] for this texture for minification and magnification. Assumes the texture is bound and active!
     *
     * @param minFilter the minification filter
     * @param magFilter the magnification filter
     */
    fun unsafeSetFilter(minFilter: TextureFilter?, magFilter: TextureFilter?) {
        unsafeSetFilter(minFilter, magFilter, false)
    }

    /**
     * Sets the [TextureFilter] for this texture for minification and magnification. Assumes the texture is bound and active!
     *
     * @param minFilter the minification filter
     * @param magFilter the magnification filter
     * @param force     True to always set the values, even if they are the same as the current values.
     */
    fun unsafeSetFilter(minFilter: TextureFilter?, magFilter: TextureFilter?, force: Boolean) {
        if (minFilter != null && (force || this.minFilter !== minFilter)) {
            Gdx.gl.glTexParameteri(glTarget, GL20.GL_TEXTURE_MIN_FILTER, minFilter.gLEnum)
            this.minFilter = minFilter
        }
        if (magFilter != null && (force || this.magFilter !== magFilter)) {
            Gdx.gl.glTexParameteri(glTarget, GL20.GL_TEXTURE_MAG_FILTER, magFilter.gLEnum)
            this.magFilter = magFilter
        }
    }

    /**
     * Sets the [TextureFilter] for this texture for minification and magnification. This will bind this texture!
     *
     * @param minFilter the minification filter
     * @param magFilter the magnification filter
     */
    fun setFilter(minFilter: TextureFilter, magFilter: TextureFilter) {
        this.minFilter = minFilter
        this.magFilter = magFilter
        bind()
        Gdx.gl.glTexParameteri(glTarget, GL20.GL_TEXTURE_MIN_FILTER, minFilter.gLEnum)
        Gdx.gl.glTexParameteri(glTarget, GL20.GL_TEXTURE_MAG_FILTER, magFilter.gLEnum)
    }
    /**
     * Sets the anisotropic filter level for the texture. Assumes the texture is bound and active!
     *
     * @param level The desired level of filtering. The maximum level supported by the device up to this value will be used.
     * @param force True to always set the value, even if it is the same as the current values.
     * @return The actual level set, which may be lower than the provided value due to device limitations.
     */
    /**
     * Sets the anisotropic filter level for the texture. Assumes the texture is bound and active!
     *
     * @param level The desired level of filtering. The maximum level supported by the device up to this value will be used.
     * @return The actual level set, which may be lower than the provided value due to device limitations.
     */
    @JvmOverloads
    fun unsafeSetAnisotropicFilter(level: Float, force: Boolean = false): Float {
        var level = level
        val max = maxAnisotropicFilterLevel
        if (max == 1f) return 1f
        level = min(level, max)
        if (!force && MathUtils.isEqual(level, anisotropicFilter, 0.1f)) return anisotropicFilter
        Gdx.gl20.glTexParameterf(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_MAX_ANISOTROPY_EXT, level)
        return level.also { anisotropicFilter = it }
    }

    /**
     * Sets the anisotropic filter level for the texture. This will bind the texture!
     *
     * @param level The desired level of filtering. The maximum level supported by the device up to this value will be used.
     * @return The actual level set, which may be lower than the provided value due to device limitations.
     */
    fun setAnisotropicFilter(level: Float): Float {
        var level = level
        val max = maxAnisotropicFilterLevel
        if (max == 1f) return 1f
        level = min(level, max)
        if (MathUtils.isEqual(level, anisotropicFilter, 0.1f)) return level
        bind()
        Gdx.gl20.glTexParameterf(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_MAX_ANISOTROPY_EXT, level)
        return level.also { anisotropicFilter = it }
    }

    /**
     * Destroys the OpenGL Texture as specified by the glHandle.
     */
    protected fun delete() {
        if (textureObjectHandle != 0) {
            Gdx.gl.glDeleteTexture(textureObjectHandle)
            textureObjectHandle = 0
        }
    }

    open fun dispose() {
        delete()
    }

    companion object {
        /**
         * @return The maximum supported anisotropic filtering level supported by the device.
         */
        var maxAnisotropicFilterLevel = 0f
            get() {
                if (field > 0) return field
                if (Gdx.graphics.supportsExtension("GL_EXT_texture_filter_anisotropic")) {
                    val buffer: FloatBuffer = BufferUtils.newFloatBuffer(16)
                    buffer.position(0)
                    buffer.limit(buffer.capacity())
                    Gdx.gl20.glGetFloatv(GL20.GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT, buffer)
                    return buffer.get(0).also({ field = it })
                }
                return 1f.also { field = it }
            }
            private set

        protected fun uploadImageData(target: Int, data: TextureData?) {
            uploadImageData(target, data, 0)
        }

        fun uploadImageData(target: Int, data: TextureData?, miplevel: Int) {
            if (data == null) {
                // FIXME: remove texture on target?
                return
            }
            if (!data.isPrepared) data.prepare()
            val type: TextureDataType = data.type
            if (type === TextureDataType.Custom) {
                data.consumeCustomData(target)
                return
            }
            var pixmap = data.consumePixmap()!!
            var disposePixmap = data.disposePixmap()
            if (data.format != pixmap.format) {
                val tmp = Pixmap(pixmap.width, pixmap.height, data.format)
                tmp.blending = Blending.None
                tmp.drawPixmap(pixmap, 0, 0, 0, 0, pixmap.width, pixmap.height)
                if (data.disposePixmap()) {
                    pixmap.dispose()
                }
                pixmap = tmp
                disposePixmap = true
            }
            Gdx.gl.glPixelStorei(GL20.GL_UNPACK_ALIGNMENT, 1)
            if (data.useMipMaps()) {
                MipMapGenerator.generateMipMap(target, pixmap, pixmap.width, pixmap.height)
            } else {
                Gdx.gl.glTexImage2D(target, miplevel, pixmap.gLInternalFormat, pixmap.width, pixmap.height, 0,
                    pixmap.gLFormat, pixmap.gLType, pixmap.pixels)
            }
            if (disposePixmap) pixmap.dispose()
        }
    }
}

