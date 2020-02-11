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

import com.badlogic.gdx.graphics.g3d.utils.AnimationController.AnimationDesc
import com.badlogic.gdx.graphics.g3d.utils.AnimationController.AnimationListener
import com.badlogic.gdx.graphics.g3d.utils.BaseAnimationController
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController.CameraGestureListener
import com.badlogic.gdx.graphics.g3d.utils.DefaultTextureBinder

class DepthShaderProvider @JvmOverloads constructor(config: DepthShader.Config? = null) : BaseShaderProvider() {
    val config: DepthShader.Config?

    constructor(vertexShader: String?, fragmentShader: String?) : this(Config(vertexShader, fragmentShader)) {}
    constructor(vertexShader: FileHandle?, fragmentShader: FileHandle?) : this(vertexShader.readString(), fragmentShader.readString()) {}

    protected override fun createShader(renderable: Renderable?): Shader? {
        return DepthShader(renderable, config)
    }

    init {
        this.config = if (config == null) Config() else config
    }
}
