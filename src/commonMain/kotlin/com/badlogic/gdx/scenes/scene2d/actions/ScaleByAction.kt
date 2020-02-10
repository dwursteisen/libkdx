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

import java.lang.Runnable

/**
 * Scales an actor's scale to a relative size.
 *
 * @author Nathan Sweet
 */
class ScaleByAction : RelativeTemporalAction() {

    var amountX = 0f
    var amountY = 0f
    protected fun updateRelative(percentDelta: Float) {
        target.scaleBy(amountX * percentDelta, amountY * percentDelta)
    }

    fun setAmount(x: Float, y: Float) {
        amountX = x
        amountY = y
    }

    fun setAmount(scale: Float) {
        amountX = scale
        amountY = scale
    }
}
