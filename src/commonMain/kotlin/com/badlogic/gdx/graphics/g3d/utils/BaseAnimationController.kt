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
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController.CameraGestureListener
import com.badlogic.gdx.graphics.g3d.utils.DefaultTextureBinder

/**
 * Base class for applying one or more [Animation]s to a [ModelInstance]. This class only applies the actual
 * [Node] transformations, it does not manage animations or keep track of animation states. See [AnimationController]
 * for an implementation of this class which does manage animations.
 *
 * @author Xoppa
 */
class BaseAnimationController(target: ModelInstance?) {

    class Transform : Poolable {
        val translation: Vector3? = Vector3()
        val rotation: Quaternion? = Quaternion()
        val scale: Vector3? = Vector3(1, 1, 1)
        fun idt(): Transform? {
            translation.set(0, 0, 0)
            rotation.idt()
            scale.set(1, 1, 1)
            return this
        }

        operator fun set(t: Vector3?, r: Quaternion?, s: Vector3?): Transform? {
            translation.set(t)
            rotation.set(r)
            scale.set(s)
            return this
        }

        fun set(other: Transform?): Transform? {
            return set(other!!.translation, other.rotation, other.scale)
        }

        fun lerp(target: Transform?, alpha: Float): Transform? {
            return lerp(target!!.translation, target.rotation, target.scale, alpha)
        }

        fun lerp(targetT: Vector3?, targetR: Quaternion?, targetS: Vector3?, alpha: Float): Transform? {
            translation.lerp(targetT, alpha)
            rotation.slerp(targetR, alpha)
            scale.lerp(targetS, alpha)
            return this
        }

        fun toMatrix4(out: Matrix4?): Matrix4? {
            return out.set(translation, rotation, scale)
        }

        fun reset() {
            idt()
        }

        override fun toString(): String {
            return translation.toString().toString() + " - " + rotation.toString() + " - " + scale.toString()
        }
    }

    private val transformPool: Pool<Transform?>? = object : Pool<Transform?>() {
        protected fun newObject(): Transform? {
            return Transform()
        }
    }
    private var applying = false

    /**
     * The [ModelInstance] on which the animations are being performed.
     */
    val target: ModelInstance?

    /**
     * Begin applying multiple animations to the instance, must followed by one or more calls to {
     * [.apply] and finally {[.end].
     */
    protected fun begin() {
        if (applying) throw GdxRuntimeException("You must call end() after each call to being()")
        applying = true
    }

    /**
     * Apply an animation, must be called between {[.begin] and {[.end].
     *
     * @param weight The blend weight of this animation relative to the previous applied animations.
     */
    protected fun apply(animation: Animation?, time: Float, weight: Float) {
        if (!applying) throw GdxRuntimeException("You must call begin() before adding an animation")
        applyAnimation(transforms, transformPool, weight, animation, time)
    }

    /**
     * End applying multiple animations to the instance and update it to reflect the changes.
     */
    protected fun end() {
        if (!applying) throw GdxRuntimeException("You must call begin() first")
        for (entry in transforms.entries()) {
            entry.value.toMatrix4(entry.key.localTransform)
            transformPool.free(entry.value)
        }
        transforms.clear()
        target.calculateTransforms()
        applying = false
    }

    /**
     * Apply a single animation to the [ModelInstance] and update the it to reflect the changes.
     */
    protected fun applyAnimation(animation: Animation?, time: Float) {
        if (applying) throw GdxRuntimeException("Call end() first")
        applyAnimation(null, null, 1f, animation, time)
        target.calculateTransforms()
    }

    /**
     * Apply two animations, blending the second onto to first using weight.
     */
    protected fun applyAnimations(anim1: Animation?, time1: Float, anim2: Animation?, time2: Float,
                                  weight: Float) {
        if (anim2 == null || weight == 0f) applyAnimation(anim1, time1) else if (anim1 == null || weight == 1f) applyAnimation(anim2, time2) else if (applying) throw GdxRuntimeException("Call end() first") else {
            begin()
            apply(anim1, time1, 1f)
            apply(anim2, time2, weight)
            end()
        }
    }

    /**
     * Remove the specified animation, by marking the affected nodes as not animated. When switching animation, this should be call
     * prior to applyAnimation(s).
     */
    protected fun removeAnimation(animation: Animation?) {
        for (nodeAnim in animation.nodeAnimations) {
            nodeAnim.node.isAnimated = false
        }
    }

    companion object {
        private val transforms: ObjectMap<Node?, Transform?>? = ObjectMap<Node?, Transform?>()
        private val tmpT: Transform? = Transform()

        /**
         * Find first key frame index just before a given time
         *
         * @param arr  Key frames ordered by time ascending
         * @param time Time to search
         * @return key frame index, 0 if time is out of key frames time range
         */
        fun <T> getFirstKeyframeIndexAtTime(arr: Array<NodeKeyframe<T?>?>?, time: Float): Int {
            val lastIndex = arr!!.size - 1

            // edges cases : time out of range always return first index
            if (lastIndex <= 0 || time < arr[0].keytime || time > arr[lastIndex].keytime) {
                return 0
            }

            // binary search
            var minIndex = 0
            var maxIndex = lastIndex
            while (minIndex < maxIndex) {
                val i = (minIndex + maxIndex) / 2
                if (time > arr[i + 1].keytime) {
                    minIndex = i + 1
                } else if (time < arr[i].keytime) {
                    maxIndex = i - 1
                } else {
                    return i
                }
            }
            return minIndex
        }

        private fun getTranslationAtTime(nodeAnim: NodeAnimation?, time: Float, out: Vector3?): Vector3? {
            if (nodeAnim.translation == null) return out.set(nodeAnim.node.translation)
            if (nodeAnim.translation.size === 1) return out.set(nodeAnim.translation.get(0).value)
            var index = getFirstKeyframeIndexAtTime<Any?>(nodeAnim.translation, time)
            val firstKeyframe: NodeKeyframe = nodeAnim.translation.get(index)
            out.set(firstKeyframe.value as Vector3)
            if (++index < nodeAnim.translation.size) {
                val secondKeyframe: NodeKeyframe<Vector3?> = nodeAnim.translation.get(index)
                val t: Float = (time - firstKeyframe.keytime) / (secondKeyframe.keytime - firstKeyframe.keytime)
                out.lerp(secondKeyframe.value, t)
            }
            return out
        }

        private fun getRotationAtTime(nodeAnim: NodeAnimation?, time: Float, out: Quaternion?): Quaternion? {
            if (nodeAnim.rotation == null) return out.set(nodeAnim.node.rotation)
            if (nodeAnim.rotation.size === 1) return out.set(nodeAnim.rotation.get(0).value)
            var index = getFirstKeyframeIndexAtTime<Any?>(nodeAnim.rotation, time)
            val firstKeyframe: NodeKeyframe = nodeAnim.rotation.get(index)
            out.set(firstKeyframe.value as Quaternion)
            if (++index < nodeAnim.rotation.size) {
                val secondKeyframe: NodeKeyframe<Quaternion?> = nodeAnim.rotation.get(index)
                val t: Float = (time - firstKeyframe.keytime) / (secondKeyframe.keytime - firstKeyframe.keytime)
                out.slerp(secondKeyframe.value, t)
            }
            return out
        }

        private fun getScalingAtTime(nodeAnim: NodeAnimation?, time: Float, out: Vector3?): Vector3? {
            if (nodeAnim.scaling == null) return out.set(nodeAnim.node.scale)
            if (nodeAnim.scaling.size === 1) return out.set(nodeAnim.scaling.get(0).value)
            var index = getFirstKeyframeIndexAtTime<Any?>(nodeAnim.scaling, time)
            val firstKeyframe: NodeKeyframe = nodeAnim.scaling.get(index)
            out.set(firstKeyframe.value as Vector3)
            if (++index < nodeAnim.scaling.size) {
                val secondKeyframe: NodeKeyframe<Vector3?> = nodeAnim.scaling.get(index)
                val t: Float = (time - firstKeyframe.keytime) / (secondKeyframe.keytime - firstKeyframe.keytime)
                out.lerp(secondKeyframe.value, t)
            }
            return out
        }

        private fun getNodeAnimationTransform(nodeAnim: NodeAnimation?, time: Float): Transform? {
            val transform = tmpT
            getTranslationAtTime(nodeAnim, time, transform!!.translation)
            getRotationAtTime(nodeAnim, time, transform.rotation)
            getScalingAtTime(nodeAnim, time, transform.scale)
            return transform
        }

        private fun applyNodeAnimationDirectly(nodeAnim: NodeAnimation?, time: Float) {
            val node: Node = nodeAnim.node
            node.isAnimated = true
            val transform = getNodeAnimationTransform(nodeAnim, time)
            transform!!.toMatrix4(node.localTransform)
        }

        private fun applyNodeAnimationBlending(nodeAnim: NodeAnimation?, out: ObjectMap<Node?, Transform?>?,
                                               pool: Pool<Transform?>?, alpha: Float, time: Float) {
            val node: Node = nodeAnim.node
            node.isAnimated = true
            val transform = getNodeAnimationTransform(nodeAnim, time)
            val t: Transform = out.get(node, null)
            if (t != null) {
                if (alpha > 0.999999f) t.set(transform) else t.lerp(transform, alpha)
            } else {
                if (alpha > 0.999999f) out.put(node, pool.obtain().set(transform)) else out.put(node, pool.obtain().set(node.translation, node.rotation, node.scale).lerp(transform, alpha))
            }
        }

        /**
         * Helper method to apply one animation to either an objectmap for blending or directly to the bones.
         */
        protected fun applyAnimation(out: ObjectMap<Node?, Transform?>?, pool: Pool<Transform?>?, alpha: Float,
                                     animation: Animation?, time: Float) {
            if (out == null) {
                for (nodeAnim in animation.nodeAnimations) applyNodeAnimationDirectly(nodeAnim, time)
            } else {
                for (node in out.keys()) node.isAnimated = false
                for (nodeAnim in animation.nodeAnimations) applyNodeAnimationBlending(nodeAnim, out, pool, alpha, time)
                for (e in out.entries()) {
                    if (!e.key.isAnimated) {
                        e.key.isAnimated = true
                        e.value.lerp(e.key.translation, e.key.rotation, e.key.scale, alpha)
                    }
                }
            }
        }
    }

    /**
     * Construct a new BaseAnimationController.
     *
     * @param target The [ModelInstance] on which the animations are being performed.
     */
    init {
        this.target = target
    }
}
