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
package com.badlogic.gdx.graphics.g3d

import com.badlogic.gdx.graphics.g3d.ModelInstance

/**
 * ModelCache tries to combine multiple render calls into a single render call by merging them where possible. Can be used for
 * multiple type of models (e.g. varying vertex attributes or materials), the ModelCache will combine where possible. Can be used
 * dynamically (e.g. every frame) or statically (e.g. to combine part of scenery). Be aware that any combined vertices are
 * directly transformed, therefore the resulting [Renderable.worldTransform] might not be suitable for sorting anymore (such
 * as the default sorter of ModelBatch does).
 *
 * @author Xoppa
 */
class ModelCache @JvmOverloads constructor(sorter: RenderableSorter = Sorter(), meshPool: MeshPool = SimpleMeshPool()) : Disposable, RenderableProvider {

    /**
     * Allows to reuse one or more meshes while avoiding creating new objects. Depending on the implementation it might add memory
     * optimizations as well. Call the [.obtain] method to obtain a mesh which can at minimum the
     * specified amount of vertices and indices. Call the [.flush] method to flush the pool ant release all previously
     * obtained meshes.
     */
    interface MeshPool : Disposable {

        /**
         * Will try to reuse or, when not possible to reuse, optionally create a [Mesh] that meets the specified criteria.
         *
         * @param vertexAttributes the vertex attributes of the mesh to obtain
         * @param vertexCount      the minimum amount vertices the mesh should be able to store
         * @param indexCount       the minimum amount of indices the mesh should be able to store
         * @return the obtained Mesh, or null when no mesh could be obtained.
         */
        fun obtain(vertexAttributes: VertexAttributes?, vertexCount: Int, indexCount: Int): Mesh?

        /**
         * Releases all previously obtained [Mesh]es using the the [.obtain] method.
         */
        fun flush()
    }

    /**
     * A basic [MeshPool] implementation that avoids creating new meshes at the cost of memory usage. It does this by making
     * the mesh always the maximum (32k) size. Use this when for dynamic caching where you need to obtain meshes very frequently
     * (typically every frame).
     *
     * @author Xoppa
     */
    class SimpleMeshPool : MeshPool {

        // FIXME Make a better (preferable JNI) MeshPool implementation
        private val freeMeshes: Array<Mesh> = Array<Mesh>()
        private val usedMeshes: Array<Mesh> = Array<Mesh>()
        override fun flush() {
            freeMeshes.addAll(usedMeshes)
            usedMeshes.clear()
        }

        override fun obtain(vertexAttributes: VertexAttributes?, vertexCount: Int, indexCount: Int): Mesh {
            var vertexCount = vertexCount
            var indexCount = indexCount
            var i = 0
            val n = freeMeshes.size
            while (i < n) {
                val mesh: Mesh = freeMeshes[i]
                if (mesh.getVertexAttributes().equals(vertexAttributes) && mesh.getMaxVertices() >= vertexCount && mesh.getMaxIndices() >= indexCount) {
                    freeMeshes.removeIndex(i)
                    usedMeshes.add(mesh)
                    return mesh
                }
                ++i
            }
            vertexCount = 1 + Short.MAX_VALUE.toInt()
            indexCount = java.lang.Math.max(1 + Short.MAX_VALUE.toInt(), 1 shl 32 - java.lang.Integer.numberOfLeadingZeros(indexCount - 1))
            val result = Mesh(false, vertexCount, indexCount, vertexAttributes)
            usedMeshes.add(result)
            return result
        }

        fun dispose() {
            for (m in usedMeshes) m.dispose()
            usedMeshes.clear()
            for (m in freeMeshes) m.dispose()
            freeMeshes.clear()
        }
    }

    /**
     * A tight [MeshPool] implementation, which is typically used for static meshes (create once, use many).
     *
     * @author Xoppa
     */
    class TightMeshPool : MeshPool {

        private val freeMeshes: Array<Mesh> = Array<Mesh>()
        private val usedMeshes: Array<Mesh> = Array<Mesh>()
        override fun flush() {
            freeMeshes.addAll(usedMeshes)
            usedMeshes.clear()
        }

        override fun obtain(vertexAttributes: VertexAttributes?, vertexCount: Int, indexCount: Int): Mesh {
            var i = 0
            val n = freeMeshes.size
            while (i < n) {
                val mesh: Mesh = freeMeshes[i]
                if (mesh.getVertexAttributes().equals(vertexAttributes) && mesh.getMaxVertices() === vertexCount && mesh.getMaxIndices() === indexCount) {
                    freeMeshes.removeIndex(i)
                    usedMeshes.add(mesh)
                    return mesh
                }
                ++i
            }
            val result = Mesh(true, vertexCount, indexCount, vertexAttributes)
            usedMeshes.add(result)
            return result
        }

        fun dispose() {
            for (m in usedMeshes) m.dispose()
            usedMeshes.clear()
            for (m in freeMeshes) m.dispose()
            freeMeshes.clear()
        }
    }

    /**
     * A [RenderableSorter] that sorts by vertex attributes, material attributes and primitive types (in that order), so that
     * meshes can be easily merged.
     *
     * @author Xoppa
     */
    class Sorter : RenderableSorter, Comparator<Renderable?> {

        fun sort(camera: Camera?, renderables: Array<Renderable?>) {
            renderables.sort(this)
        }

        fun compare(arg0: Renderable, arg1: Renderable): Int {
            val va0: VertexAttributes = arg0.meshPart.mesh.getVertexAttributes()
            val va1: VertexAttributes = arg1.meshPart.mesh.getVertexAttributes()
            val vc: Int = va0.compareTo(va1)
            if (vc == 0) {
                val mc: Int = arg0.material.compareTo(arg1.material)
                return if (mc == 0) {
                    arg0.meshPart.primitiveType - arg1.meshPart.primitiveType
                } else mc
            }
            return vc
        }
    }

    private val renderables: Array<Renderable> = Array<Renderable>()
    private val renderablesPool: FlushablePool<Renderable?> = object : FlushablePool<Renderable?>() {
        protected fun newObject(): Renderable {
            return Renderable()
        }
    }
    private val meshPartPool: FlushablePool<MeshPart> = object : FlushablePool<MeshPart?>() {
        protected fun newObject(): MeshPart {
            return MeshPart()
        }
    }
    private val items: Array<Renderable> = Array<Renderable>()
    private val tmp: Array<Renderable> = Array<Renderable>()
    private val meshBuilder: MeshBuilder
    private var building = false
    private val sorter: RenderableSorter
    private val meshPool: MeshPool
    private var camera: Camera? = null
    /**
     * Begin creating the cache, must be followed by a call to [.end], in between these calls one or more calls to one of
     * the add(...) methods can be made. Calling this method will clear the cache and prepare it for creating a new cache. The
     * cache is not valid until the call to [.end] is made. Use one of the add methods (e.g. [.add] or
     * [.add]) to add renderables to the cache.
     *
     * @param camera The [Camera] that will passed to the [RenderableSorter]
     */
    /**
     * Begin creating the cache, must be followed by a call to [.end], in between these calls one or more calls to one of
     * the add(...) methods can be made. Calling this method will clear the cache and prepare it for creating a new cache. The
     * cache is not valid until the call to [.end] is made. Use one of the add methods (e.g. [.add] or
     * [.add]) to add renderables to the cache.
     */
    @JvmOverloads
    fun begin(camera: Camera? = null) {
        if (building) throw GdxRuntimeException("Call end() after calling begin()")
        building = true
        this.camera = camera
        renderablesPool.flush()
        renderables.clear()
        items.clear()
        meshPartPool.flush()
        meshPool.flush()
    }

    private fun obtainRenderable(material: Material?, primitiveType: Int): Renderable {
        val result: Renderable = renderablesPool.obtain()
        result.bones = null
        result.environment = null
        result.material = material
        result.meshPart.mesh = null
        result.meshPart.offset = 0
        result.meshPart.size = 0
        result.meshPart.primitiveType = primitiveType
        result.meshPart.center.set(0, 0, 0)
        result.meshPart.halfExtents.set(0, 0, 0)
        result.meshPart.radius = -1f
        result.shader = null
        result.userData = null
        result.worldTransform.idt()
        return result
    }

    /**
     * Finishes creating the cache, must be called after a call to [.begin], only after this call the cache will be valid
     * (until the next call to [.begin]). Calling this method will process all renderables added using one of the add(...)
     * methods and will combine them if possible.
     */
    fun end() {
        if (!building) throw GdxRuntimeException("Call begin() prior to calling end()")
        building = false
        if (items.size === 0) return
        sorter.sort(camera, items)
        val itemCount = items.size
        val initCount = renderables.size
        val first: Renderable = items[0]
        var vertexAttributes: VertexAttributes? = first.meshPart.mesh.getVertexAttributes()
        var material: Material? = first.material
        var primitiveType: Int = first.meshPart.primitiveType
        var offset = renderables.size
        meshBuilder.begin(vertexAttributes)
        var part: MeshPart = meshBuilder.part("", primitiveType, meshPartPool.obtain())
        renderables.add(obtainRenderable(material, primitiveType))
        var i = 0
        val n = items.size
        while (i < n) {
            val renderable: Renderable = items[i]
            val va: VertexAttributes = renderable.meshPart.mesh.getVertexAttributes()
            val mat: Material = renderable.material
            val pt: Int = renderable.meshPart.primitiveType
            val sameMesh = (va.equals(vertexAttributes)
                && renderable.meshPart.size + meshBuilder.getNumVertices() < Short.MAX_VALUE) // comparing indices and vertices...
            val samePart = sameMesh && pt == primitiveType && mat.same(material, true)
            if (!samePart) {
                if (!sameMesh) {
                    val mesh: Mesh = meshBuilder.end(meshPool.obtain(vertexAttributes, meshBuilder.getNumVertices(),
                        meshBuilder.getNumIndices()))
                    while (offset < renderables.size) renderables[offset++].meshPart.mesh = mesh
                    meshBuilder.begin(va.also({ vertexAttributes = it }))
                }
                val newPart: MeshPart = meshBuilder.part("", pt, meshPartPool.obtain())
                val previous: Renderable = renderables[renderables.size - 1]
                previous.meshPart.offset = part.offset
                previous.meshPart.size = part.size
                part = newPart
                renderables.add(obtainRenderable(mat.also({ material = it }), pt.also { primitiveType = it }))
            }
            meshBuilder.setVertexTransform(renderable.worldTransform)
            meshBuilder.addMesh(renderable.meshPart.mesh, renderable.meshPart.offset, renderable.meshPart.size)
            ++i
        }
        val mesh: Mesh = meshBuilder.end(meshPool.obtain(vertexAttributes, meshBuilder.getNumVertices(),
            meshBuilder.getNumIndices()))
        while (offset < renderables.size) renderables[offset++].meshPart.mesh = mesh
        val previous: Renderable = renderables[renderables.size - 1]
        previous.meshPart.offset = part.offset
        previous.meshPart.size = part.size
    }

    /**
     * Adds the specified [Renderable] to the cache. Must be called in between a call to [.begin] and [.end].
     * All member objects might (depending on possibilities) be used by reference and should not change while the cache is used. If
     * the [Renderable.bones] member is not null then skinning is assumed and the renderable will be added as-is, by
     * reference. Otherwise the renderable will be merged with other renderables as much as possible, depending on the
     * [Mesh.getVertexAttributes], [Renderable.material] and primitiveType (in that order). The
     * [Renderable.environment], [Renderable.shader] and [Renderable.userData] values (if any) are removed.
     *
     * @param renderable The [Renderable] to add, should not change while the cache is needed.
     */
    fun add(renderable: Renderable) {
        if (!building) throw GdxRuntimeException("Can only add items to the ModelCache in between .begin() and .end()")
        if (renderable.bones == null) items.add(renderable) else renderables.add(renderable)
    }

    /**
     * Adds the specified [RenderableProvider] to the cache, see [.add].
     */
    fun add(renderableProvider: RenderableProvider) {
        renderableProvider.getRenderables(tmp, renderablesPool)
        var i = 0
        val n = tmp.size
        while (i < n) {
            add(tmp[i])
            ++i
        }
        tmp.clear()
    }

    /**
     * Adds the specified [RenderableProvider]s to the cache, see [.add].
     */
    fun <T : RenderableProvider?> add(renderableProviders: Iterable<T>) {
        for (renderableProvider in renderableProviders) add(renderableProvider)
    }

    fun getRenderables(renderables: Array<Renderable?>, pool: Pool<Renderable?>?) {
        if (building) throw GdxRuntimeException("Cannot render a ModelCache in between .begin() and .end()")
        for (r in this.renderables) {
            r.shader = null
            r.environment = null
        }
        renderables.addAll(this.renderables)
    }

    fun dispose() {
        if (building) throw GdxRuntimeException("Cannot dispose a ModelCache in between .begin() and .end()")
        meshPool.dispose()
    }
    /**
     * Create a ModelCache using the specified [RenderableSorter] and [MeshPool] implementation. The
     * [RenderableSorter] implementation will be called with the camera specified in [.begin]. By default this
     * will be null. The sorter is important for optimizing the cache. For the best result, make sure that renderables that can be
     * merged are next to each other.
     */
    /**
     * Create a ModelCache using the default [Sorter] and the [SimpleMeshPool] implementation. This might not be the
     * most optimal implementation for you use-case, but should be good to start with.
     */
    init {
        this.sorter = sorter
        this.meshPool = meshPool
        meshBuilder = MeshBuilder()
    }
}
