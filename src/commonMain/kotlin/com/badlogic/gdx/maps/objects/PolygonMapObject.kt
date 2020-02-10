/**
 *
 */
package com.badlogic.gdx.maps.objects

import Texture.TextureFilter
import com.badlogic.gdx.maps.ImageResolver
import com.badlogic.gdx.maps.MapLayer
import com.badlogic.gdx.maps.MapProperties
import com.badlogic.gdx.maps.tiled.AtlasTmxMapLoader.AtlasResolver
import com.badlogic.gdx.maps.tiled.AtlasTmxMapLoader.AtlasResolver.AssetManagerAtlasResolver
import com.badlogic.gdx.maps.tiled.AtlasTmxMapLoader.AtlasResolver.DirectAtlasResolver
import com.badlogic.gdx.maps.tiled.AtlasTmxMapLoader.AtlasTiledMapLoaderParameters
import com.badlogic.gdx.maps.tiled.BaseTmxMapLoader
import com.badlogic.gdx.maps.tiled.TideMapLoader
import com.badlogic.gdx.maps.tiled.renderers.BatchTiledMapRenderer
import com.badlogic.gdx.maps.tiled.renderers.OrthoCachedTiledMapRenderer
import com.badlogic.gdx.maps.tiled.tiles.AnimatedTiledMapTile
import com.badlogic.gdx.maps.tiled.tiles.StaticTiledMapTile
import kotlin.jvm.Throws

/** @brief Represents [Polygon] map objects
 */
class PolygonMapObject : MapObject {

    private var polygon: Polygon?

    /** @return polygon shape
     */
    fun getPolygon(): Polygon? {
        return polygon
    }

    /** @param polygon new object's polygon shape
     */
    fun setPolygon(polygon: Polygon?) {
        this.polygon = polygon
    }
    /** @param vertices polygon defining vertices (at least 3)
     */
    /** Creates empty polygon map object  */
    @JvmOverloads
    constructor(vertices: FloatArray? = FloatArray(0)) {
        polygon = Polygon(vertices)
    }

    /** @param polygon the polygon
     */
    constructor(polygon: Polygon?) {
        this.polygon = polygon
    }
}
