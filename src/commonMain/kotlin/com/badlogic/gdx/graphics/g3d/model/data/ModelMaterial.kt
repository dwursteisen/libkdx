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
package com.badlogic.gdx.graphics.g3d.model.data

import kotlin.jvm.JvmField

class ModelMaterial {
    enum class MaterialType {
        Lambert, Phong
    }

    @JvmField
    var id: String? = null
    var type: MaterialType? = null
    @JvmField
    var ambient: com.badlogic.gdx.graphics.Color? = null
    @JvmField
    var diffuse: com.badlogic.gdx.graphics.Color? = null
    @JvmField
    var specular: com.badlogic.gdx.graphics.Color? = null
    @JvmField
    var emissive: com.badlogic.gdx.graphics.Color? = null
    @JvmField
    var reflection: com.badlogic.gdx.graphics.Color? = null
    @JvmField
    var shininess = 0f
    @JvmField
    var opacity = 1f
    @JvmField
    var textures: com.badlogic.gdx.utils.Array<com.badlogic.gdx.graphics.g3d.model.data.ModelTexture?>? = null
}
