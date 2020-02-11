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
package com.badlogic.gdx.graphics.g3d.utils

import Texture.TextureFilter
import Texture.TextureWrap
import com.badlogic.gdx.graphics.g3d.utils.TextureDescriptor
import com.badlogic.gdx.graphics.g3d.utils.TextureProvider

/**
 * A relatively lightweight class which can be used to render basic shapes which don't need a node structure and alike. Can be
 * used for batching both static and dynamic shapes which share the same [Material] and transformation [Matrix4]
 * within the world. Use [ModelBatch] to render the `ShapeCache`. Must be disposed when no longer needed to release native
 * resources.
 *
 *
 * How to use it :
 *
 *
 * <pre>
 * // Create cache
 * ShapeCache cache = new ShapeCache();
 * // Build the cache, for dynamic shapes, this would be in the render method.
 * MeshPartBuilder builder = cache.begin();
 * FrustumShapeBuilder.build(builder, camera);
 * BoxShapeBuilder.build(builder, box);
 * cache.end()
 * // Render
 * modelBatch.render(cache);
 * // After using it
 * cache.dispose();
</pre> *
 *
 * @author realitix
 */
class ShapeCache @JvmOverloads constructor(maxVertices: Int = 5000, maxIndices: Int = 5000, attributes: VertexAttributes? = VertexAttributes(VertexAttribute(Usage.Position, 3, "a_position"), VertexAttribute(
    Usage.ColorPacked, 4, "a_color")), primitiveType: Int = GL20.GL_LINES) : Disposable, RenderableProvider {

    /**
     * Builder used to update the mesh
     */
    private val builder: MeshBuilder

    /**
     * Mesh being rendered
     */
    private val mesh: Mesh
    private var building = false
    private val id = "id"
    private val renderable: Renderable = Renderable()

    /**
     * Initialize ShapeCache for mesh generation with GL_LINES primitive type
     */
    fun begin(): MeshPartBuilder {
        return begin(GL20.GL_LINES)
    }

    /**
     * Initialize ShapeCache for mesh generation
     *
     * @param primitiveType OpenGL primitive type
     */
    fun begin(primitiveType: Int): MeshPartBuilder {
        if (building) throw GdxRuntimeException("Call end() after calling begin()")
        building = true
        builder.begin(mesh.getVertexAttributes())
        builder.part(id, primitiveType, renderable.meshPart)
        return builder
    }

    /**
     * Generate mesh and renderable
     */
    fun end() {
        if (!building) throw GdxRuntimeException("Call begin() prior to calling end()")
        building = false
        builder.end(mesh)
    }

    fun getRenderables(renderables: Array<Renderable?>, pool: Pool<Renderable?>?) {
        renderables.add(renderable)
    }

    /**
     * Allows to customize the material.
     *
     * @return material
     */
    val material: Material
        get() = renderable.material

    /**
     * Allows to customize the world transform matrix.
     *
     * @return world transform
     */
    val worldTransform: Matrix4
        get() = renderable.worldTransform

    fun dispose() {
        mesh.dispose()
    }
    /**
     * Create a ShapeCache with parameters
     *
     * @param maxVertices   max vertices in mesh
     * @param maxIndices    max indices in mesh
     * @param attributes    vertex attributes
     * @param primitiveType
     */
    /**
     * Create a ShapeCache with default values
     */
    init {
        // Init mesh
        mesh = Mesh(false, maxVertices, maxIndices, attributes)

        // Init builder
        builder = MeshBuilder()

        // Init renderable
        renderable.meshPart.mesh = mesh
        renderable.meshPart.primitiveType = primitiveType
        renderable.material = Material()
    }
}
