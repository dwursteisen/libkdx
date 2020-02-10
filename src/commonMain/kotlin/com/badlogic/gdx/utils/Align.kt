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
package com.badlogic.gdx.utils

import com.badlogic.gdx.utils.Array.ArrayIterable
import com.badlogic.gdx.utils.ArrayMap
import com.badlogic.gdx.utils.Base64Coder
import com.badlogic.gdx.utils.Base64Coder.CharMap
import java.lang.IllegalStateException
import java.lang.IndexOutOfBoundsException

/**
 * Provides bit flag constants for alignment.
 *
 * @author Nathan Sweet
 */
object Align {

    const val center = 1 shl 0
    const val top = 1 shl 1
    const val bottom = 1 shl 2
    const val left = 1 shl 3
    const val right = 1 shl 4
    const val topLeft = top or left
    const val topRight = top or right
    const val bottomLeft = bottom or left
    const val bottomRight = bottom or right
    fun isLeft(align: Int): Boolean {
        return align and left != 0
    }

    fun isRight(align: Int): Boolean {
        return align and right != 0
    }

    fun isTop(align: Int): Boolean {
        return align and top != 0
    }

    fun isBottom(align: Int): Boolean {
        return align and bottom != 0
    }

    fun isCenterVertical(align: Int): Boolean {
        return align and top == 0 && align and bottom == 0
    }

    fun isCenterHorizontal(align: Int): Boolean {
        return align and left == 0 && align and right == 0
    }

    fun toString(align: Int): String {
        val buffer = StringBuilder(13)
        if (align and top != 0) buffer.append("top,") else if (align and bottom != 0) buffer.append("bottom,") else buffer.append("center,")
        if (align and left != 0) buffer.append("left") else if (align and right != 0) buffer.append("right") else buffer.append("center")
        return buffer.toString()
    }
}
