package com.badlogic.gdx.graphics.glutils

import com.badlogic.gdx.graphics.glutils.InstanceData
import java.io.BufferedInputStream
import java.lang.IllegalStateException
import java.lang.NumberFormatException
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import java.util.HashMap
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

enum class HdpiMode {
    /**
     * mouse coordinates, [Graphics.getWidth] and
     * [Graphics.getHeight] will return logical coordinates
     * according to the system defined HDPI scaling. Rendering will be
     * performed to a backbuffer at raw resolution. Use [HdpiUtils]
     * when calling [GL20.glScissor] or [GL20.glViewport] which
     * expect raw coordinates.
     */
    Logical,
    /**
     * Mouse coordinates, [Graphics.getWidth] and
     * [Graphics.getHeight] will return raw pixel coordinates
     * irrespective of the system defined HDPI scaling.
     */
    Pixels
}
