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
package com.badlogic.gdx.graphics.g2d

/**
 *
 *
 * An Animation stores a list of objects representing an animated sequence, e.g. for running or jumping. Each
 * object in the Animation is called a key frame, and multiple key frames make up the animation.
 *
 *
 * The animation's type is the class representing a frame of animation. For example, a typical 2D animation could be made
 * up of [TextureRegions][com.badlogic.gdx.graphics.g2d.TextureRegion] and would be specified as:
 *
 * `Animation<TextureRegion> myAnimation = new Animation<TextureRegion>(...);`
 *
 * @author mzechner
 */
class Animation<T> {

    /**
     * Defines possible playback modes for an [Animation].
     */
    enum class PlayMode {

        NORMAL, REVERSED, LOOP, LOOP_REVERSED, LOOP_PINGPONG, LOOP_RANDOM
    }

    /**
     * Length must not be modified without updating [.animationDuration]. See [.setKeyFrames].
     */
    var keyFrames: Array<T>
    private var frameDuration: Float

    /**
     * @return the duration of the entire animation, number of frames times frame duration, in seconds
     */
    var animationDuration = 0f
        private set
    private var lastFrameNumber = 0
    private var lastStateTime = 0f
    /**
     * Returns the animation play mode.
     */
    /**
     * Sets the animation play mode.
     *
     * @param playMode The animation [PlayMode] to use.
     */
    var playMode = PlayMode.NORMAL

    /**
     * Constructor, storing the frame duration and key frames.
     *
     * @param frameDuration the time between frames in seconds.
     * @param keyFrames     the objects representing the frames. If this Array is type-aware, [.getKeyFrames] can return the
     * correct type of array. Otherwise, it returns an Object[].
     */
    constructor(frameDuration: Float, keyFrames: Array<out T>) {
        this.frameDuration = frameDuration
        val arrayType: java.lang.Class = keyFrames.items.getClass().getComponentType()
        val frames = ArrayReflection.newInstance(arrayType, keyFrames.size) as Array<T>
        var i = 0
        val n = keyFrames.size
        while (i < n) {
            frames[i] = keyFrames[i]
            i++
        }
        setKeyFrames(*frames)
    }

    /**
     * Constructor, storing the frame duration and key frames.
     *
     * @param frameDuration the time between frames in seconds.
     * @param keyFrames     the objects representing the frames. If this Array is type-aware, [.getKeyFrames] can
     * return the correct type of array. Otherwise, it returns an Object[].
     */
    constructor(frameDuration: Float, keyFrames: Array<out T>, playMode: PlayMode) : this(frameDuration, keyFrames) {
        playMode = playMode
    }

    /**
     * Constructor, storing the frame duration and key frames.
     *
     * @param frameDuration the time between frames in seconds.
     * @param keyFrames     the objects representing the frames.
     */
    constructor(frameDuration: Float, vararg keyFrames: T) {
        this.frameDuration = frameDuration
        setKeyFrames(*keyFrames)
    }

    /**
     * Returns a frame based on the so called state time. This is the amount of seconds an object has spent in the
     * state this Animation instance represents, e.g. running, jumping and so on. The mode specifies whether the animation is
     * looping or not.
     *
     * @param stateTime the time spent in the state represented by this animation.
     * @param looping   whether the animation is looping or not.
     * @return the frame of animation for the given state time.
     */
    fun getKeyFrame(stateTime: Float, looping: Boolean): T {
        // we set the play mode by overriding the previous mode based on looping
        // parameter value
        val oldPlayMode = playMode
        if (looping && (playMode == PlayMode.NORMAL || playMode == PlayMode.REVERSED)) {
            playMode = if (playMode == PlayMode.NORMAL) PlayMode.LOOP else PlayMode.LOOP_REVERSED
        } else if (!looping && !(playMode == PlayMode.NORMAL || playMode == PlayMode.REVERSED)) {
            playMode = if (playMode == PlayMode.LOOP_REVERSED) PlayMode.REVERSED else PlayMode.LOOP
        }
        val frame = getKeyFrame(stateTime)
        playMode = oldPlayMode
        return frame
    }

    /**
     * Returns a frame based on the so called state time. This is the amount of seconds an object has spent in the
     * state this Animation instance represents, e.g. running, jumping and so on using the mode specified by
     * [.setPlayMode] method.
     *
     * @param stateTime
     * @return the frame of animation for the given state time.
     */
    fun getKeyFrame(stateTime: Float): T {
        val frameNumber = getKeyFrameIndex(stateTime)
        return keyFrames[frameNumber]
    }

    /**
     * Returns the current frame number.
     *
     * @param stateTime
     * @return current frame number
     */
    fun getKeyFrameIndex(stateTime: Float): Int {
        if (keyFrames.size == 1) return 0
        var frameNumber = (stateTime / frameDuration).toInt()
        when (playMode) {
            PlayMode.NORMAL -> frameNumber = java.lang.Math.min(keyFrames.size - 1, frameNumber)
            PlayMode.LOOP -> frameNumber = frameNumber % keyFrames.size
            PlayMode.LOOP_PINGPONG -> {
                frameNumber = frameNumber % (keyFrames.size * 2 - 2)
                if (frameNumber >= keyFrames.size) frameNumber = keyFrames.size - 2 - (frameNumber - keyFrames.size)
            }
            PlayMode.LOOP_RANDOM -> {
                val lastFrameNumber = (lastStateTime / frameDuration).toInt()
                frameNumber = if (lastFrameNumber != frameNumber) {
                    MathUtils.random(keyFrames.size - 1)
                } else {
                    this.lastFrameNumber
                }
            }
            PlayMode.REVERSED -> frameNumber = java.lang.Math.max(keyFrames.size - frameNumber - 1, 0)
            PlayMode.LOOP_REVERSED -> {
                frameNumber = frameNumber % keyFrames.size
                frameNumber = keyFrames.size - frameNumber - 1
            }
        }
        lastFrameNumber = frameNumber
        lastStateTime = stateTime
        return frameNumber
    }

    /**
     * Returns the keyframes[] array where all the frames of the animation are stored.
     *
     * @return The keyframes[] field. This array is an Object[] if the animation was instantiated with an Array that was not
     * type-aware.
     */
    fun getKeyFrames(): Array<T> {
        return keyFrames
    }

    protected fun setKeyFrames(vararg keyFrames: T) {
        this.keyFrames = keyFrames
        animationDuration = keyFrames.size * frameDuration
    }

    /**
     * Whether the animation would be finished if played without looping (PlayMode#NORMAL), given the state time.
     *
     * @param stateTime
     * @return whether the animation is finished.
     */
    fun isAnimationFinished(stateTime: Float): Boolean {
        val frameNumber = (stateTime / frameDuration).toInt()
        return keyFrames.size - 1 < frameNumber
    }

    /**
     * Sets duration a frame will be displayed.
     *
     * @param frameDuration in seconds
     */
    fun setFrameDuration(frameDuration: Float) {
        this.frameDuration = frameDuration
        animationDuration = keyFrames.size * frameDuration
    }

    /**
     * @return the duration of a frame in seconds
     */
    fun getFrameDuration(): Float {
        return frameDuration
    }
}
