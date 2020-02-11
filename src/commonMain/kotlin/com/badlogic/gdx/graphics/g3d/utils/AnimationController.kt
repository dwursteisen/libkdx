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

import com.badlogic.gdx.graphics.g3d.utils.BaseAnimationController
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController.CameraGestureListener
import com.badlogic.gdx.graphics.g3d.utils.DefaultTextureBinder

/**
 * Class to control one or more [Animation]s on a [ModelInstance]. Use the
 * [.setAnimation] method to change the current animation. Use the
 * [.animate] method to start an animation, optionally blending onto the
 * current animation. Use the [.queue] method to queue an animation to be
 * played when the current animation is finished. Use the [.action] method to
 * play a (short) animation on top of the current animation.
 *
 *
 * You can use multiple AnimationControllers on the same ModelInstance, as long as they don't interfere with each other (don't
 * affect the same [Node]s).
 *
 * @author Xoppa
 */
class AnimationController
/**
 * Construct a new AnimationController.
 *
 * @param target The [ModelInstance] on which the animations will be performed.
 */(target: ModelInstance?) : BaseAnimationController(target) {

    /**
     * Listener that will be informed when an animation is looped or completed.
     *
     * @author Xoppa
     */
    interface AnimationListener {

        /**
         * Gets called when an animation is completed.
         *
         * @param animation The animation which just completed.
         */
        fun onEnd(animation: AnimationDesc?)

        /**
         * Gets called when an animation is looped. The [AnimationDesc.loopCount] is updated prior to this call and can be
         * read or written to alter the number of remaining loops.
         *
         * @param animation The animation which just looped.
         */
        fun onLoop(animation: AnimationDesc?)
    }

    /**
     * Class describing how to play and [Animation]. You can read the values within this class to get the progress of the
     * animation. Do not change the values. Only valid when the animation is currently played.
     *
     * @author Xoppa
     */
    class AnimationDesc {

        /**
         * Listener which will be informed when the animation is looped or ended.
         */
        var listener: AnimationListener? = null

        /**
         * The animation to be applied.
         */
        var animation: Animation? = null

        /**
         * The speed at which to play the animation (can be negative), 1.0 for normal speed.
         */
        var speed = 0f

        /**
         * The current animation time.
         */
        var time = 0f

        /**
         * The offset within the animation (animation time = offsetTime + time)
         */
        var offset = 0f

        /**
         * The duration of the animation
         */
        var duration = 0f

        /**
         * The number of remaining loops, negative for continuous, zero if stopped.
         */
        var loopCount = 0

        /**
         * @return the remaining time or 0 if still animating.
         */
        fun update(delta: Float): Float {
            return if (loopCount != 0 && animation != null) {
                var loops: Int
                val diff = speed * delta
                if (!MathUtils.isZero(duration)) {
                    time += diff
                    loops = java.lang.Math.abs(time / duration)
                    if (time < 0f) {
                        loops++
                        while (time < 0f) time += duration
                    }
                    time = java.lang.Math.abs(time % duration)
                } else loops = 1
                for (i in 0 until loops) {
                    if (loopCount > 0) loopCount--
                    if (loopCount != 0 && listener != null) listener!!.onLoop(this)
                    if (loopCount == 0) {
                        val result = (loops - 1 - i) * duration + if (diff < 0f) duration - time else time
                        time = if (diff < 0f) 0f else duration
                        if (listener != null) listener!!.onEnd(this)
                        return result
                    }
                }
                0f
            } else delta
        }
    }

    protected val animationPool: Pool<AnimationDesc?>? = object : Pool<AnimationDesc?>() {
        protected fun newObject(): AnimationDesc? {
            return AnimationDesc()
        }
    }

    /**
     * The animation currently playing. Do not alter this value.
     */
    var current: AnimationDesc? = null

    /**
     * The animation queued to be played when the [.current] animation is completed. Do not alter this value.
     */
    var queued: AnimationDesc? = null

    /**
     * The transition time which should be applied to the queued animation. Do not alter this value.
     */
    var queuedTransitionTime = 0f

    /**
     * The animation which previously played. Do not alter this value.
     */
    var previous: AnimationDesc? = null

    /**
     * The current transition time. Do not alter this value.
     */
    var transitionCurrentTime = 0f

    /**
     * The target transition time. Do not alter this value.
     */
    var transitionTargetTime = 0f

    /**
     * Whether an action is being performed. Do not alter this value.
     */
    var inAction = false

    /**
     * When true a call to [.update] will not be processed.
     */
    var paused = false

    /**
     * Whether to allow the same animation to be played while playing that animation.
     */
    var allowSameAnimation = false
    private var justChangedAnimation = false
    private fun obtain(anim: Animation?, offset: Float, duration: Float, loopCount: Int, speed: Float,
                       listener: AnimationListener?): AnimationDesc? {
        if (anim == null) return null
        val result: AnimationDesc = animationPool.obtain()
        result.animation = anim
        result.listener = listener
        result.loopCount = loopCount
        result.speed = speed
        result.offset = offset
        result.duration = if (duration < 0) anim.duration - offset else duration
        result.time = if (speed < 0) result.duration else 0f
        return result
    }

    private fun obtain(id: String?, offset: Float, duration: Float, loopCount: Int, speed: Float,
                       listener: AnimationListener?): AnimationDesc? {
        if (id == null) return null
        val anim: Animation = target.getAnimation(id) ?: throw GdxRuntimeException("Unknown animation: $id")
        return obtain(anim, offset, duration, loopCount, speed, listener)
    }

    private fun obtain(anim: AnimationDesc?): AnimationDesc? {
        return obtain(anim!!.animation, anim.offset, anim.duration, anim.loopCount, anim.speed, anim.listener)
    }

    /**
     * Update any animations currently being played.
     *
     * @param delta The time elapsed since last update, change this to alter the overall speed (can be negative).
     */
    fun update(delta: Float) {
        if (paused) return
        if (previous != null && delta.let { transitionCurrentTime += it; transitionCurrentTime } >= transitionTargetTime) {
            removeAnimation(previous!!.animation)
            justChangedAnimation = true
            animationPool.free(previous)
            previous = null
        }
        if (justChangedAnimation) {
            target.calculateTransforms()
            justChangedAnimation = false
        }
        if (current == null || current!!.loopCount == 0 || current!!.animation == null) return
        val remain = current!!.update(delta)
        if (remain != 0f && queued != null) {
            inAction = false
            animate(queued, queuedTransitionTime)
            queued = null
            update(remain)
            return
        }
        if (previous != null) applyAnimations(previous!!.animation, previous!!.offset + previous!!.time, current!!.animation, current!!.offset + current!!.time,
            transitionCurrentTime / transitionTargetTime) else applyAnimation(current!!.animation, current!!.offset + current!!.time)
    }

    /**
     * Set the active animation, replacing any current animation.
     *
     * @param id The ID of the [Animation] within the [ModelInstance].
     * @return The [AnimationDesc] which can be read to get the progress of the animation. Will be invalid when the animation
     * is completed.
     */
    fun setAnimation(id: String?): AnimationDesc? {
        return setAnimation(id, 1, 1.0f, null)
    }

    /**
     * Set the active animation, replacing any current animation.
     *
     * @param id        The ID of the [Animation] within the [ModelInstance].
     * @param loopCount The number of times to loop the animation, zero to play the animation only once, negative to continuously
     * loop the animation.
     * @return The [AnimationDesc] which can be read to get the progress of the animation. Will be invalid when the animation
     * is completed.
     */
    fun setAnimation(id: String?, loopCount: Int): AnimationDesc? {
        return setAnimation(id, loopCount, 1.0f, null)
    }

    /**
     * Set the active animation, replacing any current animation.
     *
     * @param id       The ID of the [Animation] within the [ModelInstance].
     * @param listener The [AnimationListener] which will be informed when the animation is looped or completed.
     * @return The [AnimationDesc] which can be read to get the progress of the animation. Will be invalid when the animation
     * is completed.
     */
    fun setAnimation(id: String?, listener: AnimationListener?): AnimationDesc? {
        return setAnimation(id, 1, 1.0f, listener)
    }

    /**
     * Set the active animation, replacing any current animation.
     *
     * @param id        The ID of the [Animation] within the [ModelInstance].
     * @param loopCount The number of times to loop the animation, zero to play the animation only once, negative to continuously
     * loop the animation.
     * @param listener  The [AnimationListener] which will be informed when the animation is looped or completed.
     * @return The [AnimationDesc] which can be read to get the progress of the animation. Will be invalid when the animation
     * is completed.
     */
    fun setAnimation(id: String?, loopCount: Int, listener: AnimationListener?): AnimationDesc? {
        return setAnimation(id, loopCount, 1.0f, listener)
    }

    /**
     * Set the active animation, replacing any current animation.
     *
     * @param id        The ID of the [Animation] within the [ModelInstance].
     * @param loopCount The number of times to loop the animation, zero to play the animation only once, negative to continuously
     * loop the animation.
     * @param speed     The speed at which the animation should be played. Default is 1.0f. A value of 2.0f will play the animation at
     * twice the normal speed, a value of 0.5f will play the animation at half the normal speed, etc. This value can be
     * negative, causing the animation to played in reverse. This value cannot be zero.
     * @param listener  The [AnimationListener] which will be informed when the animation is looped or completed.
     * @return The [AnimationDesc] which can be read to get the progress of the animation. Will be invalid when the animation
     * is completed.
     */
    fun setAnimation(id: String?, loopCount: Int, speed: Float, listener: AnimationListener?): AnimationDesc? {
        return setAnimation(id, 0f, -1f, loopCount, speed, listener)
    }

    /**
     * Set the active animation, replacing any current animation.
     *
     * @param id        The ID of the [Animation] within the [ModelInstance].
     * @param offset    The offset in seconds to the start of the animation.
     * @param duration  The duration in seconds of the animation (or negative to play till the end of the animation).
     * @param loopCount The number of times to loop the animation, zero to play the animation only once, negative to continuously
     * loop the animation.
     * @param speed     The speed at which the animation should be played. Default is 1.0f. A value of 2.0f will play the animation at
     * twice the normal speed, a value of 0.5f will play the animation at half the normal speed, etc. This value can be
     * negative, causing the animation to played in reverse. This value cannot be zero.
     * @param listener  The [AnimationListener] which will be informed when the animation is looped or completed.
     * @return The [AnimationDesc] which can be read to get the progress of the animation. Will be invalid when the animation
     * is completed.
     */
    fun setAnimation(id: String?, offset: Float, duration: Float, loopCount: Int, speed: Float,
                     listener: AnimationListener?): AnimationDesc? {
        return setAnimation(obtain(id, offset, duration, loopCount, speed, listener))
    }

    /**
     * Set the active animation, replacing any current animation.
     */
    protected fun setAnimation(anim: Animation?, offset: Float, duration: Float, loopCount: Int, speed: Float,
                               listener: AnimationListener?): AnimationDesc? {
        return setAnimation(obtain(anim, offset, duration, loopCount, speed, listener))
    }

    /**
     * Set the active animation, replacing any current animation.
     */
    protected fun setAnimation(anim: AnimationDesc?): AnimationDesc? {
        if (current == null) current = anim else {
            if (!allowSameAnimation && anim != null && current!!.animation === anim.animation) anim.time = current!!.time else removeAnimation(current!!.animation)
            animationPool.free(current)
            current = anim
        }
        justChangedAnimation = true
        return anim
    }

    /**
     * Changes the current animation by blending the new on top of the old during the transition time.
     *
     * @param id             The ID of the [Animation] within the [ModelInstance].
     * @param transitionTime The time to transition the new animation on top of the currently playing animation (if any).
     * @return The [AnimationDesc] which can be read to get the progress of the animation. Will be invalid when the animation
     * is completed.
     */
    fun animate(id: String?, transitionTime: Float): AnimationDesc? {
        return animate(id, 1, 1.0f, null, transitionTime)
    }

    /**
     * Changes the current animation by blending the new on top of the old during the transition time.
     *
     * @param id             The ID of the [Animation] within the [ModelInstance].
     * @param listener       The [AnimationListener] which will be informed when the animation is looped or completed.
     * @param transitionTime The time to transition the new animation on top of the currently playing animation (if any).
     * @return The [AnimationDesc] which can be read to get the progress of the animation. Will be invalid when the animation
     * is completed.
     */
    fun animate(id: String?, listener: AnimationListener?, transitionTime: Float): AnimationDesc? {
        return animate(id, 1, 1.0f, listener, transitionTime)
    }

    /**
     * Changes the current animation by blending the new on top of the old during the transition time.
     *
     * @param id             The ID of the [Animation] within the [ModelInstance].
     * @param loopCount      The number of times to loop the animation, zero to play the animation only once, negative to continuously
     * loop the animation.
     * @param listener       The [AnimationListener] which will be informed when the animation is looped or completed.
     * @param transitionTime The time to transition the new animation on top of the currently playing animation (if any).
     * @return The [AnimationDesc] which can be read to get the progress of the animation. Will be invalid when the animation
     * is completed.
     */
    fun animate(id: String?, loopCount: Int, listener: AnimationListener?, transitionTime: Float): AnimationDesc? {
        return animate(id, loopCount, 1.0f, listener, transitionTime)
    }

    /**
     * Changes the current animation by blending the new on top of the old during the transition time.
     *
     * @param id             The ID of the [Animation] within the [ModelInstance].
     * @param loopCount      The number of times to loop the animation, zero to play the animation only once, negative to continuously
     * loop the animation.
     * @param speed          The speed at which the animation should be played. Default is 1.0f. A value of 2.0f will play the animation at
     * twice the normal speed, a value of 0.5f will play the animation at half the normal speed, etc. This value can be
     * negative, causing the animation to played in reverse. This value cannot be zero.
     * @param listener       The [AnimationListener] which will be informed when the animation is looped or completed.
     * @param transitionTime The time to transition the new animation on top of the currently playing animation (if any).
     * @return The [AnimationDesc] which can be read to get the progress of the animation. Will be invalid when the animation
     * is completed.
     */
    fun animate(id: String?, loopCount: Int, speed: Float, listener: AnimationListener?,
                transitionTime: Float): AnimationDesc? {
        return animate(id, 0f, -1f, loopCount, speed, listener, transitionTime)
    }

    /**
     * Changes the current animation by blending the new on top of the old during the transition time.
     *
     * @param id             The ID of the [Animation] within the [ModelInstance].
     * @param offset         The offset in seconds to the start of the animation.
     * @param duration       The duration in seconds of the animation (or negative to play till the end of the animation).
     * @param loopCount      The number of times to loop the animation, zero to play the animation only once, negative to continuously
     * loop the animation.
     * @param speed          The speed at which the animation should be played. Default is 1.0f. A value of 2.0f will play the animation at
     * twice the normal speed, a value of 0.5f will play the animation at half the normal speed, etc. This value can be
     * negative, causing the animation to played in reverse. This value cannot be zero.
     * @param listener       The [AnimationListener] which will be informed when the animation is looped or completed.
     * @param transitionTime The time to transition the new animation on top of the currently playing animation (if any).
     * @return The [AnimationDesc] which can be read to get the progress of the animation. Will be invalid when the animation
     * is completed.
     */
    fun animate(id: String?, offset: Float, duration: Float, loopCount: Int, speed: Float,
                listener: AnimationListener?, transitionTime: Float): AnimationDesc? {
        return animate(obtain(id, offset, duration, loopCount, speed, listener), transitionTime)
    }

    /**
     * Changes the current animation by blending the new on top of the old during the transition time.
     */
    protected fun animate(anim: Animation?, offset: Float, duration: Float, loopCount: Int, speed: Float,
                          listener: AnimationListener?, transitionTime: Float): AnimationDesc? {
        return animate(obtain(anim, offset, duration, loopCount, speed, listener), transitionTime)
    }

    /**
     * Changes the current animation by blending the new on top of the old during the transition time.
     */
    protected fun animate(anim: AnimationDesc?, transitionTime: Float): AnimationDesc? {
        if (current == null) current = anim else if (inAction) queue(anim, transitionTime) else if (!allowSameAnimation && anim != null && current!!.animation === anim.animation) {
            anim.time = current!!.time
            animationPool.free(current)
            current = anim
        } else {
            if (previous != null) {
                removeAnimation(previous!!.animation)
                animationPool.free(previous)
            }
            previous = current
            current = anim
            transitionCurrentTime = 0f
            transitionTargetTime = transitionTime
        }
        return anim
    }

    /**
     * Queue an animation to be applied when the [.current] animation is finished. If the current animation is continuously
     * looping it will be synchronized on next loop.
     *
     * @param id             The ID of the [Animation] within the [ModelInstance].
     * @param loopCount      The number of times to loop the animation, zero to play the animation only once, negative to continuously
     * loop the animation.
     * @param speed          The speed at which the animation should be played. Default is 1.0f. A value of 2.0f will play the animation at
     * twice the normal speed, a value of 0.5f will play the animation at half the normal speed, etc. This value can be
     * negative, causing the animation to played in reverse. This value cannot be zero.
     * @param listener       The [AnimationListener] which will be informed when the animation is looped or completed.
     * @param transitionTime The time to transition the new animation on top of the currently playing animation (if any).
     * @return The [AnimationDesc] which can be read to get the progress of the animation. Will be invalid when the animation
     * is completed.
     */
    fun queue(id: String?, loopCount: Int, speed: Float, listener: AnimationListener?, transitionTime: Float): AnimationDesc? {
        return queue(id, 0f, -1f, loopCount, speed, listener, transitionTime)
    }

    /**
     * Queue an animation to be applied when the [.current] animation is finished. If the current animation is continuously
     * looping it will be synchronized on next loop.
     *
     * @param id             The ID of the [Animation] within the [ModelInstance].
     * @param offset         The offset in seconds to the start of the animation.
     * @param duration       The duration in seconds of the animation (or negative to play till the end of the animation).
     * @param loopCount      The number of times to loop the animation, zero to play the animation only once, negative to continuously
     * loop the animation.
     * @param speed          The speed at which the animation should be played. Default is 1.0f. A value of 2.0f will play the animation at
     * twice the normal speed, a value of 0.5f will play the animation at half the normal speed, etc. This value can be
     * negative, causing the animation to played in reverse. This value cannot be zero.
     * @param listener       The [AnimationListener] which will be informed when the animation is looped or completed.
     * @param transitionTime The time to transition the new animation on top of the currently playing animation (if any).
     * @return The [AnimationDesc] which can be read to get the progress of the animation. Will be invalid when the animation
     * is completed.
     */
    fun queue(id: String?, offset: Float, duration: Float, loopCount: Int, speed: Float,
              listener: AnimationListener?, transitionTime: Float): AnimationDesc? {
        return queue(obtain(id, offset, duration, loopCount, speed, listener), transitionTime)
    }

    /**
     * Queue an animation to be applied when the current is finished. If current is continuous it will be synced on next loop.
     */
    protected fun queue(anim: Animation?, offset: Float, duration: Float, loopCount: Int, speed: Float,
                        listener: AnimationListener?, transitionTime: Float): AnimationDesc? {
        return queue(obtain(anim, offset, duration, loopCount, speed, listener), transitionTime)
    }

    /**
     * Queue an animation to be applied when the current is finished. If current is continuous it will be synced on next loop.
     */
    protected fun queue(anim: AnimationDesc?, transitionTime: Float): AnimationDesc? {
        if (current == null || current!!.loopCount == 0) animate(anim, transitionTime) else {
            if (queued != null) animationPool.free(queued)
            queued = anim
            queuedTransitionTime = transitionTime
            if (current!!.loopCount < 0) current!!.loopCount = 1
        }
        return anim
    }

    /**
     * Apply an action animation on top of the current animation.
     *
     * @param id             The ID of the [Animation] within the [ModelInstance].
     * @param loopCount      The number of times to loop the animation, zero to play the animation only once, negative to continuously
     * loop the animation.
     * @param speed          The speed at which the animation should be played. Default is 1.0f. A value of 2.0f will play the animation at
     * twice the normal speed, a value of 0.5f will play the animation at half the normal speed, etc. This value can be
     * negative, causing the animation to played in reverse. This value cannot be zero.
     * @param listener       The [AnimationListener] which will be informed when the animation is looped or completed.
     * @param transitionTime The time to transition the new animation on top of the currently playing animation (if any).
     * @return The [AnimationDesc] which can be read to get the progress of the animation. Will be invalid when the animation
     * is completed.
     */
    fun action(id: String?, loopCount: Int, speed: Float, listener: AnimationListener?,
               transitionTime: Float): AnimationDesc? {
        return action(id, 0f, -1f, loopCount, speed, listener, transitionTime)
    }

    /**
     * Apply an action animation on top of the current animation.
     *
     * @param id             The ID of the [Animation] within the [ModelInstance].
     * @param offset         The offset in seconds to the start of the animation.
     * @param duration       The duration in seconds of the animation (or negative to play till the end of the animation).
     * @param loopCount      The number of times to loop the animation, zero to play the animation only once, negative to continuously
     * loop the animation.
     * @param speed          The speed at which the animation should be played. Default is 1.0f. A value of 2.0f will play the animation at
     * twice the normal speed, a value of 0.5f will play the animation at half the normal speed, etc. This value can be
     * negative, causing the animation to played in reverse. This value cannot be zero.
     * @param listener       The [AnimationListener] which will be informed when the animation is looped or completed.
     * @param transitionTime The time to transition the new animation on top of the currently playing animation (if any).
     * @return The [AnimationDesc] which can be read to get the progress of the animation. Will be invalid when the animation
     * is completed.
     */
    fun action(id: String?, offset: Float, duration: Float, loopCount: Int, speed: Float,
               listener: AnimationListener?, transitionTime: Float): AnimationDesc? {
        return action(obtain(id, offset, duration, loopCount, speed, listener), transitionTime)
    }

    /**
     * Apply an action animation on top of the current animation.
     */
    protected fun action(anim: Animation?, offset: Float, duration: Float, loopCount: Int, speed: Float,
                         listener: AnimationListener?, transitionTime: Float): AnimationDesc? {
        return action(obtain(anim, offset, duration, loopCount, speed, listener), transitionTime)
    }

    /**
     * Apply an action animation on top of the current animation.
     */
    protected fun action(anim: AnimationDesc?, transitionTime: Float): AnimationDesc? {
        if (anim!!.loopCount < 0) throw GdxRuntimeException("An action cannot be continuous")
        if (current == null || current!!.loopCount == 0) animate(anim, transitionTime) else {
            val toQueue = if (inAction) null else obtain(current)
            inAction = false
            animate(anim, transitionTime)
            inAction = true
            toQueue?.let { queue(it, transitionTime) }
        }
        return anim
    }
}
