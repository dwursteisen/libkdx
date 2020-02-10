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

import PixmapPacker.PixmapPackerRectangle
import com.badlogic.gdx.graphics.g2d.ParticleEmitter
import com.badlogic.gdx.graphics.g2d.ParticleEmitter.IndependentScaledNumericValue
import com.badlogic.gdx.graphics.g2d.ParticleEmitter.SpawnEllipseSide
import com.badlogic.gdx.graphics.g2d.ParticleEmitter.SpawnShape
import com.badlogic.gdx.graphics.g2d.ParticleEmitter.SpriteMode
import com.badlogic.gdx.graphics.g2d.PixmapPacker
import com.badlogic.gdx.graphics.g2d.PixmapPacker.GuillotineStrategy
import com.badlogic.gdx.graphics.g2d.PixmapPacker.GuillotineStrategy.GuillotinePage
import com.badlogic.gdx.graphics.g2d.PixmapPacker.PackStrategy
import com.badlogic.gdx.graphics.g2d.PixmapPacker.PixmapPackerRectangle
import com.badlogic.gdx.graphics.g2d.PixmapPacker.SkylineStrategy.SkylinePage
import com.badlogic.gdx.graphics.g2d.PixmapPackerIO.ImageFormat
import com.badlogic.gdx.graphics.g2d.PixmapPackerIO.SaveParameters
import java.lang.RuntimeException
import kotlin.jvm.Throws

/**
 * A PolygonBatch is an extension of the Batch interface that provides additional render methods specifically for rendering
 * polygons.
 *
 * @author mzechner
 * @author Nathan Sweet
 */
interface PolygonBatch : Batch {

    /**
     * Draws a polygon region with the bottom left corner at x,y having the width and height of the region.
     */
    override fun draw(region: PolygonRegion?, x: Float, y: Float)

    /**
     * Draws a polygon region with the bottom left corner at x,y and stretching the region to cover the given width and height.
     */
    override fun draw(region: PolygonRegion?, x: Float, y: Float, width: Float, height: Float)

    /**
     * Draws the polygon region with the bottom left corner at x,y and stretching the region to cover the given width and height.
     * The polygon region is offset by originX, originY relative to the origin. Scale specifies the scaling factor by which the
     * polygon region should be scaled around originX, originY. Rotation specifies the angle of counter clockwise rotation of the
     * rectangle around originX, originY.
     */
    fun draw(region: PolygonRegion?, x: Float, y: Float, originX: Float, originY: Float, width: Float, height: Float, scaleX: Float,
             scaleY: Float, rotation: Float)

    /**
     * Draws the polygon using the given vertices and triangles. Each vertices must be made up of 5 elements in this order: x, y,
     * color, u, v.
     */
    fun draw(texture: Texture?, polygonVertices: FloatArray?, verticesOffset: Int, verticesCount: Int, polygonTriangles: ShortArray?,
             trianglesOffset: Int, trianglesCount: Int)
}
