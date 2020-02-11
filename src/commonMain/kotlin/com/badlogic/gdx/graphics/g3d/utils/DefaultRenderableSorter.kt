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

class DefaultRenderableSorter : RenderableSorter, Comparator<Renderable?> {
    private var camera: Camera? = null
    private val tmpV1: Vector3? = Vector3()
    private val tmpV2: Vector3? = Vector3()
    fun sort(camera: Camera?, renderables: Array<Renderable?>?) {
        this.camera = camera
        renderables.sort(this)
    }

    private fun getTranslation(worldTransform: Matrix4?, center: Vector3?, output: Vector3?): Vector3? {
        if (center.isZero()) worldTransform.getTranslation(output) else if (!worldTransform.hasRotationOrScaling()) worldTransform.getTranslation(output).add(center) else output.set(center).mul(worldTransform)
        return output
    }

    override fun compare(o1: Renderable?, o2: Renderable?): Int {
        val b1 = o1.material.has(BlendingAttribute.Type) && (o1.material.get(BlendingAttribute.Type) as BlendingAttribute).blended
        val b2 = o2.material.has(BlendingAttribute.Type) && (o2.material.get(BlendingAttribute.Type) as BlendingAttribute).blended
        if (b1 != b2) return if (b1) 1 else -1
        // FIXME implement better sorting algorithm
        // final boolean same = o1.shader == o2.shader && o1.mesh == o2.mesh && (o1.lights == null) == (o2.lights == null) &&
        // o1.material.equals(o2.material);
        getTranslation(o1.worldTransform, o1.meshPart.center, tmpV1)
        getTranslation(o2.worldTransform, o2.meshPart.center, tmpV2)
        val dst: Float = (1000f * camera.position.dst2(tmpV1)) as Int - (1000f * camera.position.dst2(tmpV2)) as Int.toFloat()
        val result = if (dst < 0) -1 else if (dst > 0) 1 else 0
        return if (b1) -result else result
    }
}
