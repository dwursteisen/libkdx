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

import Mesh.VertexDataType
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.SpriteCache
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasSprite
import com.badlogic.gdx.graphics.g2d.TextureAtlas.TextureAtlasData
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ShortArray
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.lang.IllegalStateException
import java.nio.FloatBuffer
import kotlin.jvm.Throws

/**
 * Renders polygon filled with a repeating TextureRegion with specified density
 * Without causing an additional flush or render call
 *
 * @author Avetis Zakharyan
 */
class RepeatablePolygonSprite {

    private var region: TextureRegion? = null
    private val density = 0f
    private var dirty = true
    private val parts = Array<FloatArray>()
    private val vertices = Array<FloatArray>()
    private val indices = Array<ShortArray>()
    private var cols = 0
    private var rows = 0
    private var gridWidth = 0f
    private var gridHeight = 0f
    var x = 0f
    var y = 0f
    private var color: Color = Color.WHITE
    private val offset: Vector2 = Vector2()

    /**
     * Sets polygon with repeating texture region, the size of repeating grid is equal to region size
     * @param region - region to repeat
     * @param vertices - cw vertices of polygon
     */
    fun setPolygon(region: TextureRegion, vertices: FloatArray) {
        setPolygon(region, vertices, -1f)
    }

    /**
     * Sets polygon with repeating texture region, the size of repeating grid is equal to region size
     * @param region - region to repeat
     * @param vertices - cw vertices of polygon
     * @param density - number of regions per polygon width bound
     */
    fun setPolygon(region: TextureRegion, vertices: FloatArray, density: Float) {
        var vertices = vertices
        var density = density
        this.region = region
        vertices = offset(vertices)
        val polygon = Polygon(vertices)
        val tmpPoly = Polygon()
        val intersectionPoly = Polygon()
        val triangulator = EarClippingTriangulator()
        var idx: Int
        val boundRect: Rectangle = polygon.getBoundingRectangle()
        if (density == -1f) density = boundRect.getWidth() / region.getRegionWidth()
        val regionAspectRatio = region.getRegionHeight() as Float / region.getRegionWidth() as Float
        cols = java.lang.Math.ceil(density.toDouble())
        gridWidth = boundRect.getWidth() / density
        gridHeight = regionAspectRatio * gridWidth
        rows = java.lang.Math.ceil(boundRect.getHeight() / gridHeight)
        for (col in 0 until cols) {
            for (row in 0 until rows) {
                var verts = FloatArray(8)
                idx = 0
                verts[idx++] = col * gridWidth
                verts[idx++] = row * gridHeight
                verts[idx++] = col * gridWidth
                verts[idx++] = (row + 1) * gridHeight
                verts[idx++] = (col + 1) * gridWidth
                verts[idx++] = (row + 1) * gridHeight
                verts[idx++] = (col + 1) * gridWidth
                verts[idx] = row * gridHeight
                tmpPoly.setVertices(verts)
                Intersector.intersectPolygons(polygon, tmpPoly, intersectionPoly)
                verts = intersectionPoly.getVertices()
                if (verts.size > 0) {
                    parts.add(snapToGrid(verts))
                    val arr: ShortArray = triangulator.computeTriangles(verts)
                    indices.add(arr.toArray())
                } else {
                    // adding null for key consistancy, needed to get col/row from key
                    // the other alternative is to make parts - IntMap<FloatArray>
                    parts.add(null)
                }
            }
        }
        buildVertices()
    }

    /**
     * This is a garbage, due to Intersector returning values slightly different then the grid values
     * Snapping exactly to grid is important, so that during bulidVertices method, it can be figured out
     * if points is on the wall of it's own grid box or not, to set u/v properly.
     * Any other implementations are welcome
     */
    private fun snapToGrid(vertices: FloatArray): FloatArray {
        var i = 0
        while (i < vertices.size) {
            val numX = vertices[i] / gridWidth % 1
            val numY = vertices[i + 1] / gridHeight % 1
            if (numX > 0.99f || numX < 0.01f) {
                vertices[i] = gridWidth * java.lang.Math.round(vertices[i] / gridWidth)
            }
            if (numY > 0.99f || numY < 0.01f) {
                vertices[i + 1] = gridHeight * java.lang.Math.round(vertices[i + 1] / gridHeight)
            }
            i += 2
        }
        return vertices
    }

    /**
     * Offsets polygon to 0 coordinate for ease of calculations, later offset is put back on final render
     * @param vertices
     * @return offsetted vertices
     */
    private fun offset(vertices: FloatArray): FloatArray {
        offset.set(vertices[0], vertices[1])
        run {
            var i = 0
            while (i < vertices.size - 1) {
                if (offset.x > vertices[i]) {
                    offset.x = vertices[i]
                }
                if (offset.y > vertices[i + 1]) {
                    offset.y = vertices[i + 1]
                }
                i += 2
            }
        }
        var i = 0
        while (i < vertices.size) {
            vertices[i] -= offset.x
            vertices[i + 1] -= offset.y
            i += 2
        }
        return vertices
    }

    /**
     * Builds final vertices with vertex attributes like coordinates, color and region u/v
     */
    private fun buildVertices() {
        vertices.clear()
        for (i in 0 until parts.size) {
            val verts = parts[i] ?: continue
            val fullVerts = FloatArray(5 * verts.size / 2)
            var idx = 0
            val col = i / rows
            val row = i % rows
            var j = 0
            while (j < verts.size) {
                fullVerts[idx++] = verts[j] + offset.x + x
                fullVerts[idx++] = verts[j + 1] + offset.y + y
                fullVerts[idx++] = color.toFloatBits()
                var u = verts[j] % gridWidth / gridWidth
                var v = verts[j + 1] % gridHeight / gridHeight
                if (verts[j] == col * gridWidth) u = 0f
                if (verts[j] == (col + 1) * gridWidth) u = 1f
                if (verts[j + 1] == row * gridHeight) v = 0f
                if (verts[j + 1] == (row + 1) * gridHeight) v = 1f
                u = region!!.getU() + (region!!.getU2() - region!!.getU()) * u
                v = region!!.getV() + (region!!.getV2() - region!!.getV()) * v
                fullVerts[idx++] = u
                fullVerts[idx++] = v
                j += 2
            }
            vertices.add(fullVerts)
        }
        dirty = false
    }

    fun draw(batch: PolygonSpriteBatch) {
        if (dirty) {
            buildVertices()
        }
        for (i in 0 until vertices.size) {
            batch.draw(region!!.getTexture(), vertices[i], 0, vertices[i].length, indices[i], 0, indices[i].length)
        }
    }

    /**
     * @param color - Tint color to be applied to entire polygon
     */
    fun setColor(color: Color) {
        this.color = color
        dirty = true
    }

    fun setPosition(x: Float, y: Float) {
        this.x = x
        this.y = y
        dirty = true
    }
}
