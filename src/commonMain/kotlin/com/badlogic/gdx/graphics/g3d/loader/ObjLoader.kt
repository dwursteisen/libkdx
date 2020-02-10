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
package com.badlogic.gdx.graphics.g3d.loader

import com.badlogic.gdx.assets.loaders.FileHandleResolver
import com.badlogic.gdx.assets.loaders.ModelLoader
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

/** [ModelLoader] to load Wavefront OBJ files. Only intended for testing basic models/meshes and educational usage. The
 * Wavefront specification is NOT fully implemented, only a subset of the specification is supported. Especially the
 * [Material] ([Attributes]), e.g. the color or texture applied, might not or not correctly be loaded.
 *
 * This [ModelLoader] can be used to load very basic models without having to convert them to a more suitable format.
 * Therefore it can be used for educational purposes and to quickly test a basic model, but should not be used in production.
 * Instead use [G3dModelLoader].
 *
 * Because of above reasons, when an OBJ file is loaded using this loader, it will log and error. To prevent this error from being
 * logged, set the [.logWarning] flag to false. However, it is advised not to do so.
 *
 * An OBJ file only contains the mesh (shape). It may link to a separate MTL file, which is used to describe one or more
 * materials. In that case the MTL filename (might be case-sensitive) is expected to be located relative to the OBJ file. The MTL
 * file might reference one or more texture files, in which case those filename(s) are expected to be located relative to the MTL
 * file.
 * @author mzechner, espitz, xoppa
 */
class ObjLoader @JvmOverloads constructor(resolver: FileHandleResolver? = null) : ModelLoader<ObjLoader.ObjLoaderParameters?>(resolver) {

    class ObjLoaderParameters : ModelParameters {
        var flipV = false

        constructor() {}
        constructor(flipV: Boolean) {
            this.flipV = flipV
        }
    }

    val verts: com.badlogic.gdx.utils.FloatArray? = com.badlogic.gdx.utils.FloatArray(300)
    val norms: com.badlogic.gdx.utils.FloatArray? = com.badlogic.gdx.utils.FloatArray(300)
    val uvs: com.badlogic.gdx.utils.FloatArray? = com.badlogic.gdx.utils.FloatArray(200)
    val groups: com.badlogic.gdx.utils.Array<Group?>? = com.badlogic.gdx.utils.Array<Group?>(10)
    /** Directly load the model on the calling thread. The model with not be managed by an [AssetManager].  */
    fun loadModel(fileHandle: FileHandle?, flipV: Boolean): com.badlogic.gdx.graphics.g3d.Model? {
        return loadModel(fileHandle, ObjLoaderParameters(flipV))
    }

    override fun loadModelData(file: FileHandle?, parameters: ObjLoaderParameters?): com.badlogic.gdx.graphics.g3d.model.data.ModelData? {
        return loadModelData(file, parameters != null && parameters.flipV)
    }

    protected fun loadModelData(file: FileHandle?, flipV: Boolean): com.badlogic.gdx.graphics.g3d.model.data.ModelData? {
        if (logWarning) com.badlogic.gdx.Gdx.app.error("ObjLoader", "Wavefront (OBJ) is not fully supported, consult the documentation for more information")
        var line: String?
        var tokens: Array<String?>
        var firstChar: Char
        val mtl = MtlLoader()
        // Create a "default" Group and set it as the active group, in case
// there are no groups or objects defined in the OBJ file.
        var activeGroup: Group? = Group("default")
        groups.add(activeGroup)
        val reader = BufferedReader(InputStreamReader(file!!.read()), 4096)
        var id = 0
        try {
            while (reader.readLine().also({ line = it }) != null) {
                tokens = line!!.split("\\s+").toTypedArray()
                if (tokens.size < 1) break
                if (tokens[0]!!.length == 0) {
                    continue
                } else if (tokens[0]!!.toLowerCase()[0].also { firstChar = it } == '#') {
                    continue
                } else if (firstChar == 'v') {
                    if (tokens[0]!!.length == 1) {
                        verts.add(tokens[1]!!.toFloat())
                        verts.add(tokens[2]!!.toFloat())
                        verts.add(tokens[3]!!.toFloat())
                    } else if (tokens[0]!![1] == 'n') {
                        norms.add(tokens[1]!!.toFloat())
                        norms.add(tokens[2]!!.toFloat())
                        norms.add(tokens[3]!!.toFloat())
                    } else if (tokens[0]!![1] == 't') {
                        uvs.add(tokens[1]!!.toFloat())
                        uvs.add(if (flipV) 1 - tokens[2]!!.toFloat() else tokens[2]!!.toFloat())
                    }
                } else if (firstChar == 'f') {
                    var parts: Array<String?>
                    val faces: com.badlogic.gdx.utils.Array<Int?>? = activeGroup!!.faces
                    var i = 1
                    while (i < tokens.size - 2) {
                        parts = tokens[1]!!.split("/").toTypedArray()
                        faces.add(getIndex(parts[0], verts.size))
                        if (parts.size > 2) {
                            if (i == 1) activeGroup.hasNorms = true
                            faces.add(getIndex(parts[2], norms.size))
                        }
                        if (parts.size > 1 && parts[1]!!.length > 0) {
                            if (i == 1) activeGroup.hasUVs = true
                            faces.add(getIndex(parts[1], uvs.size))
                        }
                        parts = tokens[++i]!!.split("/").toTypedArray()
                        faces.add(getIndex(parts[0], verts.size))
                        if (parts.size > 2) faces.add(getIndex(parts[2], norms.size))
                        if (parts.size > 1 && parts[1]!!.length > 0) faces.add(getIndex(parts[1], uvs.size))
                        parts = tokens[++i]!!.split("/").toTypedArray()
                        faces.add(getIndex(parts[0], verts.size))
                        if (parts.size > 2) faces.add(getIndex(parts[2], norms.size))
                        if (parts.size > 1 && parts[1]!!.length > 0) faces.add(getIndex(parts[1], uvs.size))
                        activeGroup.numFaces++
                        i--
                    }
                } else if (firstChar == 'o' || firstChar == 'g') { // This implementation only supports single object or group
// definitions. i.e. "o group_a group_b" will set group_a
// as the active group, while group_b will simply be
// ignored.
                    activeGroup = if (tokens.size > 1) setActiveGroup(tokens[1]) else setActiveGroup("default")
                } else if (tokens[0] == "mtllib") {
                    mtl.load(file!!.parent().child(tokens[1]))
                } else if (tokens[0] == "usemtl") {
                    if (tokens.size == 1) activeGroup!!.materialName = "default" else activeGroup!!.materialName = tokens[1]!!.replace('.', '_')
                }
            }
            reader.close()
        } catch (e: IOException) {
            return null
        }
        // If the "default" group or any others were not used, get rid of them
        var i = 0
        while (i < groups.size) {
            if (groups.get(i).numFaces < 1) {
                groups.removeIndex(i)
                i--
            }
            i++
        }
        // If there are no groups left, there is no valid Model to return
        if (groups.size < 1) return null
        // Get number of objects/groups remaining after removing empty ones
        val numGroups: Int = groups.size
        val data: com.badlogic.gdx.graphics.g3d.model.data.ModelData = com.badlogic.gdx.graphics.g3d.model.data.ModelData()
        for (g in 0 until numGroups) {
            val group: Group = groups.get(g)
            val faces: com.badlogic.gdx.utils.Array<Int?>? = group.faces
            val numElements: Int = faces.size
            val numFaces = group.numFaces
            val hasNorms = group.hasNorms
            val hasUVs = group.hasUVs
            val finalVerts = FloatArray(numFaces * 3 * (3 + (if (hasNorms) 3 else 0) + if (hasUVs) 2 else 0))
            var i = 0
            var vi = 0
            while (i < numElements) {
                var vertIndex: Int = faces.get(i++) * 3
                finalVerts[vi++] = verts.get(vertIndex++)
                finalVerts[vi++] = verts.get(vertIndex++)
                finalVerts[vi++] = verts.get(vertIndex)
                if (hasNorms) {
                    var normIndex: Int = faces.get(i++) * 3
                    finalVerts[vi++] = norms.get(normIndex++)
                    finalVerts[vi++] = norms.get(normIndex++)
                    finalVerts[vi++] = norms.get(normIndex)
                }
                if (hasUVs) {
                    var uvIndex: Int = faces.get(i++) * 2
                    finalVerts[vi++] = uvs.get(uvIndex++)
                    finalVerts[vi++] = uvs.get(uvIndex)
                }
            }
            val numIndices = if (numFaces * 3 >= Short.MAX_VALUE) 0 else numFaces * 3
            val finalIndices = ShortArray(numIndices)
            // if there are too many vertices in a mesh, we can't use indices
            if (numIndices > 0) {
                for (i in 0 until numIndices) {
                    finalIndices[i] = i.toShort()
                }
            }
            val attributes: com.badlogic.gdx.utils.Array<com.badlogic.gdx.graphics.VertexAttribute?> = com.badlogic.gdx.utils.Array<com.badlogic.gdx.graphics.VertexAttribute?>()
            attributes.add(com.badlogic.gdx.graphics.VertexAttribute(com.badlogic.gdx.graphics.VertexAttributes.Usage.Position, 3, ShaderProgram.POSITION_ATTRIBUTE))
            if (hasNorms) attributes.add(com.badlogic.gdx.graphics.VertexAttribute(com.badlogic.gdx.graphics.VertexAttributes.Usage.Normal, 3, ShaderProgram.NORMAL_ATTRIBUTE))
            if (hasUVs) attributes.add(com.badlogic.gdx.graphics.VertexAttribute(com.badlogic.gdx.graphics.VertexAttributes.Usage.TextureCoordinates, 2, ShaderProgram.TEXCOORD_ATTRIBUTE + "0"))
            val stringId: String = java.lang.Integer.toString(++id)
            val nodeId = if ("default" == group.name) "node$stringId" else group.name
            val meshId = if ("default" == group.name) "mesh$stringId" else group.name
            val partId = if ("default" == group.name) "part$stringId" else group.name
            val node: com.badlogic.gdx.graphics.g3d.model.data.ModelNode = com.badlogic.gdx.graphics.g3d.model.data.ModelNode()
            node.id = nodeId
            node.meshId = meshId
            node.scale = com.badlogic.gdx.math.Vector3(1, 1, 1)
            node.translation = com.badlogic.gdx.math.Vector3()
            node.rotation = com.badlogic.gdx.math.Quaternion()
            val pm: com.badlogic.gdx.graphics.g3d.model.data.ModelNodePart = com.badlogic.gdx.graphics.g3d.model.data.ModelNodePart()
            pm.meshPartId = partId
            pm.materialId = group.materialName
            node.parts = arrayOf<com.badlogic.gdx.graphics.g3d.model.data.ModelNodePart?>(pm)
            val part: com.badlogic.gdx.graphics.g3d.model.data.ModelMeshPart = com.badlogic.gdx.graphics.g3d.model.data.ModelMeshPart()
            part.id = partId
            part.indices = finalIndices
            part.primitiveType = com.badlogic.gdx.graphics.GL20.GL_TRIANGLES
            val mesh: com.badlogic.gdx.graphics.g3d.model.data.ModelMesh = com.badlogic.gdx.graphics.g3d.model.data.ModelMesh()
            mesh.id = meshId
            mesh.attributes = attributes.toArray(com.badlogic.gdx.graphics.VertexAttribute::class.java)
            mesh.vertices = finalVerts
            mesh.parts = arrayOf<com.badlogic.gdx.graphics.g3d.model.data.ModelMeshPart?>(part)
            data.nodes.add(node)
            data.meshes.add(mesh)
            val mm: com.badlogic.gdx.graphics.g3d.model.data.ModelMaterial? = mtl.getMaterial(group.materialName)
            data.materials.add(mm)
        }
        // for (ModelMaterial m : mtl.materials)
// data.materials.add(m);
// An instance of ObjLoader can be used to load more than one OBJ.
// Clearing the Array cache instead of instantiating new
// Arrays should result in slightly faster load times for
// subsequent calls to loadObj
        if (verts.size > 0) verts.clear()
        if (norms.size > 0) norms.clear()
        if (uvs.size > 0) uvs.clear()
        if (groups.size > 0) groups.clear()
        return data
    }

    private fun setActiveGroup(name: String?): Group? { // TODO: Check if a HashMap.get calls are faster than iterating
// through an Array
        for (group in groups) {
            if (group!!.name == name) return group
        }
        val group = Group(name)
        groups.add(group)
        return group
    }

    private fun getIndex(index: String?, size: Int): Int {
        if (index == null || index.length == 0) return 0
        val idx = index.toInt()
        return if (idx < 0) size + idx else idx - 1
    }

    inner class Group internal constructor(val name: String?) {
        var materialName: String?
        var faces: com.badlogic.gdx.utils.Array<Int?>?
        var numFaces: Int
        var hasNorms = false
        var hasUVs = false
        var mat: com.badlogic.gdx.graphics.g3d.Material?

        init {
            faces = com.badlogic.gdx.utils.Array<Int?>(200)
            numFaces = 0
            mat = com.badlogic.gdx.graphics.g3d.Material("")
            materialName = "default"
        }
    }

    companion object {
        /** Set to false to prevent a warning from being logged when this class is used. Do not change this value, unless you are
         * absolutely sure what you are doing. Consult the documentation for more information.  */
        var logWarning = false
    }
}

internal class MtlLoader {
    var materials: com.badlogic.gdx.utils.Array<com.badlogic.gdx.graphics.g3d.model.data.ModelMaterial?>? = com.badlogic.gdx.utils.Array<com.badlogic.gdx.graphics.g3d.model.data.ModelMaterial?>()
    /** loads .mtl file  */
    fun load(file: FileHandle?) {
        var line: String?
        var tokens: Array<String?>
        var curMatName: String? = "default"
        var difcolor: com.badlogic.gdx.graphics.Color? = com.badlogic.gdx.graphics.Color.WHITE
        var speccolor: com.badlogic.gdx.graphics.Color? = com.badlogic.gdx.graphics.Color.WHITE
        var opacity = 1f
        var shininess = 0f
        var texFilename: String? = null
        if (file == null || !file.exists()) return
        val reader = BufferedReader(InputStreamReader(file.read()), 4096)
        try {
            while (reader.readLine().also({ line = it }) != null) {
                if (line!!.length > 0 && line!![0] == '\t') line = line!!.substring(1).trim { it <= ' ' }
                tokens = line!!.split("\\s+").toTypedArray()
                if (tokens[0]!!.length == 0) {
                    continue
                } else if (tokens[0]!![0] == '#') continue else {
                    val key = tokens[0]!!.toLowerCase()
                    if (key == "newmtl") {
                        val mat: com.badlogic.gdx.graphics.g3d.model.data.ModelMaterial = com.badlogic.gdx.graphics.g3d.model.data.ModelMaterial()
                        mat.id = curMatName
                        mat.diffuse = com.badlogic.gdx.graphics.Color(difcolor)
                        mat.specular = com.badlogic.gdx.graphics.Color(speccolor)
                        mat.opacity = opacity
                        mat.shininess = shininess
                        if (texFilename != null) {
                            val tex: com.badlogic.gdx.graphics.g3d.model.data.ModelTexture = com.badlogic.gdx.graphics.g3d.model.data.ModelTexture()
                            tex.usage = com.badlogic.gdx.graphics.g3d.model.data.ModelTexture.USAGE_DIFFUSE
                            tex.fileName = String(texFilename)
                            if (mat.textures == null) mat.textures = com.badlogic.gdx.utils.Array<com.badlogic.gdx.graphics.g3d.model.data.ModelTexture?>(1)
                            mat.textures.add(tex)
                        }
                        materials.add(mat)
                        if (tokens.size > 1) {
                            curMatName = tokens[1]
                            curMatName = curMatName!!.replace('.', '_')
                        } else curMatName = "default"
                        difcolor = com.badlogic.gdx.graphics.Color.WHITE
                        speccolor = com.badlogic.gdx.graphics.Color.WHITE
                        opacity = 1f
                        shininess = 0f
                    } else if (key == "kd" || key == "ks") // diffuse or specular
                    {
                        val r = tokens[1]!!.toFloat()
                        val g = tokens[2]!!.toFloat()
                        val b = tokens[3]!!.toFloat()
                        var a = 1f
                        if (tokens.size > 4) a = tokens[4]!!.toFloat()
                        if (tokens[0]!!.toLowerCase() == "kd") {
                            difcolor = com.badlogic.gdx.graphics.Color()
                            difcolor.set(r, g, b, a)
                        } else {
                            speccolor = com.badlogic.gdx.graphics.Color()
                            speccolor.set(r, g, b, a)
                        }
                    } else if (key == "tr" || key == "d") {
                        opacity = tokens[1]!!.toFloat()
                    } else if (key == "ns") {
                        shininess = tokens[1]!!.toFloat()
                    } else if (key == "map_kd") {
                        texFilename = file.parent().child(tokens[1]).path()
                    }
                }
            }
            reader.close()
        } catch (e: IOException) {
            return
        }
        // last material
        val mat: com.badlogic.gdx.graphics.g3d.model.data.ModelMaterial = com.badlogic.gdx.graphics.g3d.model.data.ModelMaterial()
        mat.id = curMatName
        mat.diffuse = com.badlogic.gdx.graphics.Color(difcolor)
        mat.specular = com.badlogic.gdx.graphics.Color(speccolor)
        mat.opacity = opacity
        mat.shininess = shininess
        if (texFilename != null) {
            val tex: com.badlogic.gdx.graphics.g3d.model.data.ModelTexture = com.badlogic.gdx.graphics.g3d.model.data.ModelTexture()
            tex.usage = com.badlogic.gdx.graphics.g3d.model.data.ModelTexture.USAGE_DIFFUSE
            tex.fileName = String(texFilename)
            if (mat.textures == null) mat.textures = com.badlogic.gdx.utils.Array<com.badlogic.gdx.graphics.g3d.model.data.ModelTexture?>(1)
            mat.textures.add(tex)
        }
        materials.add(mat)
        return
    }

    fun getMaterial(name: String?): com.badlogic.gdx.graphics.g3d.model.data.ModelMaterial? {
        for (m in materials) if (m.id == name) return m
        val mat: com.badlogic.gdx.graphics.g3d.model.data.ModelMaterial = com.badlogic.gdx.graphics.g3d.model.data.ModelMaterial()
        mat.id = name
        mat.diffuse = com.badlogic.gdx.graphics.Color(com.badlogic.gdx.graphics.Color.WHITE)
        materials.add(mat)
        return mat
    }
}
