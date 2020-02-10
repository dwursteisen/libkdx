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
package com.badlogic.gdx.scenes.scene2d.utils

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.glutils.HdpiUtils.glScissor
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.scenes.scene2d.utils.DragScrollListener
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable
import com.badlogic.gdx.utils.Array
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

/**
 * A stack of [Rectangle] objects to be used for clipping via [GL20.glScissor]. When a new
 * Rectangle is pushed onto the stack, it will be merged with the current top of stack. The minimum area of overlap is then set as
 * the real top of the stack.
 *
 * @author mzechner
 */
object ScissorStack {

    private val scissors: Array<Rectangle> = Array<Rectangle>()
    var tmp: Vector3 = Vector3()
    val viewport: Rectangle = Rectangle()

    /**
     * Pushes a new scissor [Rectangle] onto the stack, merging it with the current top of the stack. The minimal area of
     * overlap between the top of stack rectangle and the provided rectangle is pushed onto the stack. This will invoke
     * [GL20.glScissor] with the final top of stack rectangle. In case no scissor is yet on the stack
     * this will also enable [GL20.GL_SCISSOR_TEST] automatically.
     *
     *
     * Any drawing should be flushed before pushing scissors.
     *
     * @return true if the scissors were pushed. false if the scissor area was zero, in this case the scissors were not pushed and
     * no drawing should occur.
     */
    fun pushScissors(scissor: Rectangle): Boolean {
        fix(scissor)
        if (scissors.isEmpty()) {
            if (scissor.width < 1 || scissor.height < 1) return false
            Gdx.gl.glEnable(GL20.GL_SCISSOR_TEST)
        } else {
            // merge scissors
            val parent: Rectangle = scissors[scissors.size - 1]
            val minX: Float = max(parent.x, scissor.x)
            val maxX: Float = min(parent.x + parent.width, scissor.x + scissor.width)
            if (maxX - minX < 1) return false
            val minY: Float = max(parent.y, scissor.y)
            val maxY: Float = min(parent.y + parent.height, scissor.y + scissor.height)
            if (maxY - minY < 1) return false
            scissor.x = minX
            scissor.y = minY
            scissor.width = maxX - minX
            scissor.height = max(1f, maxY - minY)
        }
        scissors.add(scissor)
        glScissor(scissor.x as Int, scissor.y as Int, scissor.width as Int, scissor.height as Int)
        return true
    }

    /**
     * Pops the current scissor rectangle from the stack and sets the new scissor area to the new top of stack rectangle. In case
     * no more rectangles are on the stack, [GL20.GL_SCISSOR_TEST] is disabled.
     *
     *
     * Any drawing should be flushed before popping scissors.
     */
    fun popScissors(): Rectangle {
        val old: Rectangle = scissors.pop()
        if (scissors.isEmpty()) Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST) else {
            val scissor: Rectangle = scissors.peek()
            glScissor(scissor.x as Int, scissor.y as Int, scissor.width as Int, scissor.height as Int)
        }
        return old
    }

    /**
     * @return null if there are no scissors.
     */
    fun peekScissors(): Rectangle? {
        return if (scissors.isEmpty()) null else scissors.peek()
    }

    private fun fix(rect: Rectangle) {
        rect.x = round(rect.x)
        rect.y = round(rect.y)
        rect.width = round(rect.width)
        rect.height = round(rect.height)
        if (rect.width < 0) {
            rect.width = -rect.width
            rect.x -= rect.width
        }
        if (rect.height < 0) {
            rect.height = -rect.height
            rect.y -= rect.height
        }
    }

    /**
     * Calculates a scissor rectangle using 0,0,Gdx.graphics.getWidth(),Gdx.graphics.getHeight() as the viewport.
     *
     * @see .calculateScissors
     */
    fun calculateScissors(camera: Camera, batchTransform: Matrix4?, area: Rectangle, scissor: Rectangle) {
        calculateScissors(camera, 0f, 0f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat(), batchTransform, area, scissor)
    }

    /**
     * Calculates a scissor rectangle in OpenGL ES window coordinates from a [Camera], a transformation [Matrix4] and
     * an axis aligned [Rectangle]. The rectangle will get transformed by the camera and transform matrices and is then
     * projected to screen coordinates. Note that only axis aligned rectangles will work with this method. If either the Camera or
     * the Matrix4 have rotational components, the output of this method will not be suitable for
     * [GL20.glScissor].
     *
     * @param camera         the [Camera]
     * @param batchTransform the transformation [Matrix4]
     * @param area           the [Rectangle] to transform to window coordinates
     * @param scissor        the Rectangle to store the result in
     */
    fun calculateScissors(camera: Camera, viewportX: Float, viewportY: Float, viewportWidth: Float,
                          viewportHeight: Float, batchTransform: Matrix4?, area: Rectangle, scissor: Rectangle) {
        tmp.set(area.x, area.y, 0)
        tmp.mul(batchTransform)
        camera.project(tmp, viewportX, viewportY, viewportWidth, viewportHeight)
        scissor.x = tmp.x
        scissor.y = tmp.y
        tmp.set(area.x + area.width, area.y + area.height, 0)
        tmp.mul(batchTransform)
        camera.project(tmp, viewportX, viewportY, viewportWidth, viewportHeight)
        scissor.width = tmp.x - scissor.x
        scissor.height = tmp.y - scissor.y
    }

    /**
     * @return the current viewport in OpenGL ES window coordinates based on the currently applied scissor
     */
    fun getViewport(): Rectangle {
        return if (scissors.isEmpty()) {
            viewport.set(0, 0, Gdx.graphics.width, Gdx.graphics.height)
            viewport
        } else {
            val scissor: Rectangle = scissors.peek()
            viewport.set(scissor)
            viewport
        }
    }
}
