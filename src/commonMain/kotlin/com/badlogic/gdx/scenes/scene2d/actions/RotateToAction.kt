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

import com.badlogic.gdx.math.MathUtils
import java.lang.Runnable

/**
 * Sets the actor's rotation from its current value to a specific value.
 *
 *
 * By default, the rotation will take you from the starting value to the specified value via simple subtraction. For example,
 * setting the start at 350 and the target at 10 will result in 340 degrees of movement.
 *
 *
 * If the action is instead set to useShortestDirection instead, it will rotate straight to the target angle, regardless of where
 * the angle starts and stops. For example, starting at 350 and rotating to 10 will cause 20 degrees of rotation.
 *
 * @author Nathan Sweet
 * @see com.badlogic.gdx.math.MathUtils.lerpAngleDeg
 */
class RotateToAction : TemporalAction {

    private var start = 0f
    var rotation = 0f
    var isUseShortestDirection = false

    constructor() {}

    /**
     * @param useShortestDirection Set to true to move directly to the closest angle
     */
    constructor(useShortestDirection: Boolean) {
        isUseShortestDirection = useShortestDirection
    }

    protected override fun begin() {
        start = target.getRotation()
    }

    protected override fun update(percent: Float) {
        val rotation: Float
        rotation = if (percent == 0f) start else if (percent == 1f) this.rotation else if (isUseShortestDirection) MathUtils.lerpAngleDeg(start, this.rotation, percent) else start + (this.rotation - start) * percent
        target.setRotation(rotation)
    }
}
