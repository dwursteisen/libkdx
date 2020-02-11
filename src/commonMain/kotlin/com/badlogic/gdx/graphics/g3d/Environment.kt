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
package com.badlogic.gdx.graphics.g3d

class Environment : Attributes() {

    @Deprecated("Experimental, likely to change, do not use!")
    var shadowMap: ShadowMap? = null
    fun add(vararg lights: BaseLight?): Environment {
        for (light in lights) add(light)
        return this
    }

    fun add(lights: Array<BaseLight?>): Environment {
        for (light in lights) add(light)
        return this
    }

    fun add(light: BaseLight?): Environment {
        if (light is DirectionalLight) add(light as DirectionalLight?) else if (light is PointLight) {
            add(light as PointLight?)
        } else if (light is SpotLight) add(light as SpotLight?) else throw GdxRuntimeException("Unknown light type")
        return this
    }

    fun add(light: DirectionalLight?): Environment {
        var dirLights: DirectionalLightsAttribute? = get(DirectionalLightsAttribute.Type) as DirectionalLightsAttribute?
        if (dirLights == null) set(DirectionalLightsAttribute().also({ dirLights = it }))
        dirLights.lights.add(light)
        return this
    }

    fun add(light: PointLight?): Environment {
        var pointLights: PointLightsAttribute? = get(PointLightsAttribute.Type) as PointLightsAttribute?
        if (pointLights == null) set(PointLightsAttribute().also({ pointLights = it }))
        pointLights.lights.add(light)
        return this
    }

    fun add(light: SpotLight?): Environment {
        var spotLights: SpotLightsAttribute? = get(SpotLightsAttribute.Type) as SpotLightsAttribute?
        if (spotLights == null) set(SpotLightsAttribute().also({ spotLights = it }))
        spotLights.lights.add(light)
        return this
    }

    fun remove(vararg lights: BaseLight?): Environment {
        for (light in lights) remove(light)
        return this
    }

    fun remove(lights: Array<BaseLight?>): Environment {
        for (light in lights) remove(light)
        return this
    }

    fun remove(light: BaseLight?): Environment {
        if (light is DirectionalLight) remove(light as DirectionalLight?) else if (light is PointLight) remove(light as PointLight?) else if (light is SpotLight) remove(light as SpotLight?) else throw GdxRuntimeException("Unknown light type")
        return this
    }

    fun remove(light: DirectionalLight?): Environment {
        if (has(DirectionalLightsAttribute.Type)) {
            val dirLights: DirectionalLightsAttribute? = get(DirectionalLightsAttribute.Type) as DirectionalLightsAttribute?
            dirLights.lights.removeValue(light, false)
            if (dirLights.lights.size === 0) remove(DirectionalLightsAttribute.Type)
        }
        return this
    }

    fun remove(light: PointLight?): Environment {
        if (has(PointLightsAttribute.Type)) {
            val pointLights: PointLightsAttribute? = get(PointLightsAttribute.Type) as PointLightsAttribute?
            pointLights.lights.removeValue(light, false)
            if (pointLights.lights.size === 0) remove(PointLightsAttribute.Type)
        }
        return this
    }

    fun remove(light: SpotLight?): Environment {
        if (has(SpotLightsAttribute.Type)) {
            val spotLights: SpotLightsAttribute? = get(SpotLightsAttribute.Type) as SpotLightsAttribute?
            spotLights.lights.removeValue(light, false)
            if (spotLights.lights.size === 0) remove(SpotLightsAttribute.Type)
        }
        return this
    }
}
