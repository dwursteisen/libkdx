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
package com.badlogic.gdx.graphics.g3d.decals

import Mesh.VertexDataType
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Mesh
import com.badlogic.gdx.graphics.VertexAttribute
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.g3d.decals.CameraGroupStrategy
import com.badlogic.gdx.graphics.g3d.decals.Decal
import com.badlogic.gdx.graphics.g3d.decals.DecalMaterial
import com.badlogic.gdx.graphics.g3d.decals.SimpleOrthoGroupStrategy
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.Pool
import com.badlogic.gdx.utils.SortedIntList

/**
 *
 *
 * Renderer for [Decal] objects.
 *
 *
 *
 * New objects are added using [DecalBatch.add], there is no limit on how many decals can be added.<br></br>
 * Once all the decals have been submitted a call to [DecalBatch.flush] will batch them together and send big chunks of
 * geometry to the GL.
 *
 *
 *
 * The size of the batch specifies the maximum number of decals that can be batched together before they have to be submitted to
 * the graphics pipeline. The default size is [DecalBatch.DEFAULT_SIZE]. If it is known before hand that not as many will be
 * needed on average the batch can be downsized to save memory. If the game is basically 3d based and decals will only be needed
 * for an orthogonal HUD it makes sense to tune the size down.
 *
 *
 *
 * The way the batch handles things depends on the [GroupStrategy]. Different strategies can be used to customize shaders,
 * states, culling etc. for more details see the [GroupStrategy] java doc.<br></br>
 * While it shouldn't be necessary to change strategies, if you have to do so, do it before calling [.add], and if
 * you already did, call [.flush] first.
 *
 */
class DecalBatch(size: Int, groupStrategy: GroupStrategy?) : Disposable {

    private var vertices: FloatArray?
    private var mesh: Mesh? = null
    private val groupList: SortedIntList<Array<Decal?>?>? = SortedIntList<Array<Decal?>?>()
    private var groupStrategy: GroupStrategy? = null
    private val groupPool: Pool<Array<Decal?>?>? = object : Pool<Array<Decal?>?>(16) {
        protected fun newObject(): Array<Decal?>? {
            return Array<Decal?>(false, 100)
        }
    }
    private val usedGroups: Array<Array<Decal?>?>? = Array<Array<Decal?>?>(16)

    /**
     * Creates a new DecalBatch using the given [GroupStrategy]. The most
     * commong strategy to use is a [CameraGroupStrategy]
     *
     * @param groupStrategy
     */
    constructor(groupStrategy: GroupStrategy?) : this(DEFAULT_SIZE, groupStrategy) {}

    /**
     * Sets the [GroupStrategy] used
     *
     * @param groupStrategy Group strategy to use
     */
    fun setGroupStrategy(groupStrategy: GroupStrategy?) {
        this.groupStrategy = groupStrategy
    }

    /**
     * Initializes the batch with the given amount of decal objects the buffer is able to hold when full.
     *
     * @param size Maximum size of decal objects to hold in memory
     */
    fun initialize(size: Int) {
        vertices = FloatArray(size * Decal.SIZE)
        var vertexDataType: VertexDataType = Mesh.VertexDataType.VertexArray
        if (Gdx.gl30 != null) {
            vertexDataType = Mesh.VertexDataType.VertexBufferObjectWithVAO
        }
        mesh = Mesh(vertexDataType, false, size * 4, size * 6, VertexAttribute(
            VertexAttributes.Usage.Position, 3, ShaderProgram.POSITION_ATTRIBUTE), VertexAttribute(
            VertexAttributes.Usage.ColorPacked, 4, ShaderProgram.COLOR_ATTRIBUTE), VertexAttribute(
            VertexAttributes.Usage.TextureCoordinates, 2, ShaderProgram.TEXCOORD_ATTRIBUTE + "0"))
        val indices = ShortArray(size * 6)
        var v = 0
        var i = 0
        while (i < indices.size) {
            indices[i] = v.toShort()
            indices[i + 1] = (v + 2).toShort()
            indices[i + 2] = (v + 1).toShort()
            indices[i + 3] = (v + 1).toShort()
            indices[i + 4] = (v + 2).toShort()
            indices[i + 5] = (v + 3).toShort()
            i += 6
            v += 4
        }
        mesh.setIndices(indices)
    }

    /**
     * @return maximum amount of decal objects this buffer can hold in memory
     */
    val size: Int
        get() = vertices!!.size / Decal.SIZE

    /**
     * Add a decal to the batch, marking it for later rendering
     *
     * @param decal Decal to add for rendering
     */
    fun add(decal: Decal?) {
        val groupIndex: Int = groupStrategy!!.decideGroup(decal)
        var targetGroup: Array<Decal?> = groupList.get(groupIndex)
        if (targetGroup == null) {
            targetGroup = groupPool.obtain()
            targetGroup.clear()
            usedGroups.add(targetGroup)
            groupList.insert(groupIndex, targetGroup)
        }
        targetGroup.add(decal)
    }

    /**
     * Flush this batch sending all contained decals to GL. After flushing the batch is empty once again.
     */
    fun flush() {
        render()
        clear()
    }

    /**
     * Renders all decals to the buffer and flushes the buffer to the GL when full/done
     */
    protected fun render() {
        groupStrategy!!.beforeGroups()
        for (group in groupList) {
            groupStrategy!!.beforeGroup(group.index, group.value)
            val shader: ShaderProgram = groupStrategy!!.getGroupShader(group.index)
            render(shader, group.value)
            groupStrategy!!.afterGroup(group.index)
        }
        groupStrategy!!.afterGroups()
    }

    /**
     * Renders a group of vertices to the buffer, flushing them to GL when done/full
     *
     * @param decals Decals to render
     */
    private fun render(shader: ShaderProgram?, decals: Array<Decal?>?) {
        // batch vertices
        var lastMaterial: DecalMaterial? = null
        var idx = 0
        for (decal in decals!!) {
            if (lastMaterial == null || !lastMaterial.equals(decal!!.getMaterial())) {
                if (idx > 0) {
                    flush(shader, idx)
                    idx = 0
                }
                decal!!.material!!.set()
                lastMaterial = decal!!.material
            }
            decal!!.update()
            java.lang.System.arraycopy(decal!!.vertices, 0, vertices, idx, decal!!.vertices.length)
            idx += decal!!.vertices.length
            // if our batch is full we have to flush it
            if (idx == vertices!!.size) {
                flush(shader, idx)
                idx = 0
            }
        }
        // at the end if there is stuff left in the batch we render that
        if (idx > 0) {
            flush(shader, idx)
        }
    }

    /**
     * Flushes vertices[0,verticesPosition[ to GL verticesPosition % Decal.SIZE must equal 0
     *
     * @param verticesPosition Amount of elements from the vertices array to flush
     */
    protected fun flush(shader: ShaderProgram?, verticesPosition: Int) {
        mesh.setVertices(vertices, 0, verticesPosition)
        mesh.render(shader, GL20.GL_TRIANGLES, 0, verticesPosition / 4)
    }

    /**
     * Remove all decals from batch
     */
    protected fun clear() {
        groupList.clear()
        groupPool.freeAll(usedGroups)
        usedGroups.clear()
    }

    /**
     * Frees up memory by dropping the buffer and underlying resources. If the batch is needed again after disposing it can be
     * [initialized][.initialize] again.
     */
    fun dispose() {
        clear()
        vertices = null
        mesh.dispose()
    }

    companion object {
        private const val DEFAULT_SIZE = 1000
    }

    init {
        initialize(size)
        setGroupStrategy(groupStrategy)
    }
}
