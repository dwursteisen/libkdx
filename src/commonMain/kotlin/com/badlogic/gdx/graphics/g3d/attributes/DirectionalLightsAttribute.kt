package com.badlogic.gdx.graphics.g3d.attributes

import kotlin.jvm.JvmField

/** An [Attribute] which can be used to send an [Array] of [DirectionalLight] instances to the [Shader].
 * The lights are stored by reference, the [.copy] or [.DirectionalLightsAttribute]
 * method will not create new lights.
 * @author Xoppa
 */
class DirectionalLightsAttribute() : com.badlogic.gdx.graphics.g3d.Attribute(Type) {

    @JvmField
    val lights: com.badlogic.gdx.utils.Array<com.badlogic.gdx.graphics.g3d.environment.DirectionalLight?>?

    constructor(copyFrom: DirectionalLightsAttribute?) : this() {
        lights.addAll(copyFrom!!.lights)
    }

    override fun copy(): DirectionalLightsAttribute? {
        return DirectionalLightsAttribute(this)
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        for (light in lights) result = 1229 * result + if (light == null) 0 else light.hashCode()
        return result
    }

    override operator fun compareTo(o: com.badlogic.gdx.graphics.g3d.Attribute?): Int {
        return if (type != o.type) if (type < o.type) -1 else 1 else 0
        // FIXME implement comparing
    }

    companion object {
        val Alias: String? = "directionalLights"
        @JvmField
        val Type: Long = com.badlogic.gdx.graphics.g3d.Attribute.register(Alias)
        fun `is`(mask: Long): Boolean {
            return mask and Type == mask
        }
    }

    init {
        lights = com.badlogic.gdx.utils.Array<com.badlogic.gdx.graphics.g3d.environment.DirectionalLight?>(1)
    }
}
