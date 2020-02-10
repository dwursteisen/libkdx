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

class ModelTexture {
    var id: String? = null
    @JvmField
    var fileName: String? = null
    @JvmField
    var uvTranslation: com.badlogic.gdx.math.Vector2? = null
    @JvmField
    var uvScaling: com.badlogic.gdx.math.Vector2? = null
    @JvmField
    var usage = 0

    companion object {
        const val USAGE_UNKNOWN = 0
        const val USAGE_NONE = 1
        const val USAGE_DIFFUSE = 2
        const val USAGE_EMISSIVE = 3
        const val USAGE_AMBIENT = 4
        const val USAGE_SPECULAR = 5
        const val USAGE_SHININESS = 6
        const val USAGE_NORMAL = 7
        const val USAGE_BUMP = 8
        const val USAGE_TRANSPARENCY = 9
        const val USAGE_REFLECTION = 10
    }
}
