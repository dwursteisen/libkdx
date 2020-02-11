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

import com.badlogic.gdx.graphics.g2d.PolygonRegionLoader.PolygonRegionParameters
import com.badlogic.gdx.graphics.g2d.PolygonSprite

/**
 * Defines a polygon shape on top of a texture region to avoid drawing transparent pixels.
 *
 * @author Stefan Bachmann
 * @author Nathan Sweet
 * @see PolygonRegionLoader
 */
class PolygonRegion(val region: TextureRegion,
                    /**
                     * Returns the vertices in local space.
                     */
                    val vertices // pixel coordinates relative to source image.
                    : FloatArray, val triangles: ShortArray) {

    val textureCoords // texture coordinates in atlas coordinates
        : FloatArray

    /**
     * Creates a PolygonRegion by triangulating the polygon coordinates in vertices and calculates uvs based on that.
     * TextureRegion can come from an atlas.
     *
     * @param region   the region used for drawing
     * @param vertices contains 2D polygon coordinates in pixels relative to source region
     */
    init {
        textureCoords = FloatArray(vertices.size)
        val textureCoords = textureCoords
        val u = region.u
        val v = region.v
        val uvWidth = region.u2 - u
        val uvHeight = region.v2 - v
        val width = region.regionWidth
        val height = region.regionHeight
        var i = 0
        val n = vertices.size
        while (i < n) {
            textureCoords[i] = u + uvWidth * (vertices[i] / width)
            textureCoords[i + 1] = v + uvHeight * (1 - vertices[i + 1] / height)
            i += 2
        }
    }
}
