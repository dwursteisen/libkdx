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
package com.badlogic.gdx.scenes.scene2d.actions

/**
 * An action that has a float, whose value is transitioned over time.
 *
 * @author Nathan Sweet
 */
class FloatAction : TemporalAction {

    /**
     * Sets the value to transition from.
     */
    var start: Float

    /**
     * Sets the value to transition to.
     */
    var end: Float
    /**
     * Gets the current float value.
     */
    /**
     * Sets the current float value.
     */
    var value = 0f

    /**
     * Creates a FloatAction that transitions from 0 to 1.
     */
    constructor() {
        start = 0f
        end = 1f
    }

    /**
     * Creates a FloatAction that transitions from start to end.
     */
    constructor(start: Float, end: Float) {
        this.start = start
        this.end = end
    }

    /**
     * Creates a FloatAction that transitions from start to end.
     */
    constructor(start: Float, end: Float, duration: Float) : super(duration) {
        this.start = start
        this.end = end
    }

    /**
     * Creates a FloatAction that transitions from start to end.
     */
    constructor(start: Float, end: Float, duration: Float, interpolation: Interpolation?) : super(duration, interpolation) {
        this.start = start
        this.end = end
    }

    override fun begin() {
        value = start
    }

    override fun update(percent: Float) {
        value = if (percent == 0f) start else if (percent == 1f) end else start + (end - start) * percent
    }
}
