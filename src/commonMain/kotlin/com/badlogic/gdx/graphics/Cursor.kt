package com.badlogic.gdx.graphics

import com.badlogic.gdx.utils.Disposable

/**
 *
 *
 * Represents a mouse cursor. Create a cursor via
 * [Graphics.newCursor]. To
 * set the cursor use [Graphics.setCursor].
 * To use one of the system cursors, call Graphics#setSystemCursor
 *
 */
interface Cursor : Disposable {

    enum class SystemCursor {
        Arrow, Ibeam, Crosshair, Hand, HorizontalResize, VerticalResize
    }
}
