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
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

class G3dModelLoader @JvmOverloads constructor(reader: com.badlogic.gdx.utils.BaseJsonReader?, resolver: FileHandleResolver? = null) : ModelLoader<ModelLoader.ModelParameters?>(resolver) {
    protected val reader: com.badlogic.gdx.utils.BaseJsonReader?
    override fun loadModelData(fileHandle: FileHandle?, parameters: ModelParameters?): com.badlogic.gdx.graphics.g3d.model.data.ModelData? {
        return parseModel(fileHandle)
    }

    fun parseModel(handle: FileHandle?): com.badlogic.gdx.graphics.g3d.model.data.ModelData? {
        val json: com.badlogic.gdx.utils.JsonValue = reader.parse(handle)
        val model: com.badlogic.gdx.graphics.g3d.model.data.ModelData = com.badlogic.gdx.graphics.g3d.model.data.ModelData()
        val version: com.badlogic.gdx.utils.JsonValue = json.require("version")
        model.version.get(0) = version.getShort(0)
        model.version.get(1) = version.getShort(1)
        if (model.version.get(0) != VERSION_HI || model.version.get(1) != VERSION_LO) throw com.badlogic.gdx.utils.GdxRuntimeException("Model version not supported")
        model.id = json.getString("id", "")
        parseMeshes(model, json)
        parseMaterials(model, json, handle!!.parent().path())
        parseNodes(model, json)
        parseAnimations(model, json)
        return model
    }

    protected fun parseMeshes(model: com.badlogic.gdx.graphics.g3d.model.data.ModelData?, json: com.badlogic.gdx.utils.JsonValue?) {
        val meshes: com.badlogic.gdx.utils.JsonValue = json.get("meshes")
        if (meshes != null) {
            model.meshes.ensureCapacity(meshes.size)
            var mesh: com.badlogic.gdx.utils.JsonValue = meshes.child
            while (mesh != null) {
                val jsonMesh: com.badlogic.gdx.graphics.g3d.model.data.ModelMesh = com.badlogic.gdx.graphics.g3d.model.data.ModelMesh()
                val id: String = mesh.getString("id", "")
                jsonMesh.id = id
                val attributes: com.badlogic.gdx.utils.JsonValue = mesh.require("attributes")
                jsonMesh.attributes = parseAttributes(attributes)
                jsonMesh.vertices = mesh.require("vertices").asFloatArray()
                val meshParts: com.badlogic.gdx.utils.JsonValue = mesh.require("parts")
                val parts: com.badlogic.gdx.utils.Array<com.badlogic.gdx.graphics.g3d.model.data.ModelMeshPart?> = com.badlogic.gdx.utils.Array<com.badlogic.gdx.graphics.g3d.model.data.ModelMeshPart?>()
                var meshPart: com.badlogic.gdx.utils.JsonValue = meshParts.child
                while (meshPart != null) {
                    val jsonPart: com.badlogic.gdx.graphics.g3d.model.data.ModelMeshPart = com.badlogic.gdx.graphics.g3d.model.data.ModelMeshPart()
                    val partId: String = meshPart.getString("id", null)
                        ?: throw com.badlogic.gdx.utils.GdxRuntimeException("Not id given for mesh part")
                    for (other in parts) {
                        if (other.id == partId) {
                            throw com.badlogic.gdx.utils.GdxRuntimeException("Mesh part with id '$partId' already in defined")
                        }
                    }
                    jsonPart.id = partId
                    val type: String = meshPart.getString("type", null)
                        ?: throw com.badlogic.gdx.utils.GdxRuntimeException("No primitive type given for mesh part '$partId'")
                    jsonPart.primitiveType = parseType(type)
                    jsonPart.indices = meshPart.require("indices").asShortArray()
                    parts.add(jsonPart)
                    meshPart = meshPart.next
                }
                jsonMesh.parts = parts.toArray(com.badlogic.gdx.graphics.g3d.model.data.ModelMeshPart::class.java)
                model.meshes.add(jsonMesh)
                mesh = mesh.next
            }
        }
    }

    protected fun parseType(type: String?): Int {
        return if (type == "TRIANGLES") {
            com.badlogic.gdx.graphics.GL20.GL_TRIANGLES
        } else if (type == "LINES") {
            com.badlogic.gdx.graphics.GL20.GL_LINES
        } else if (type == "POINTS") {
            com.badlogic.gdx.graphics.GL20.GL_POINTS
        } else if (type == "TRIANGLE_STRIP") {
            com.badlogic.gdx.graphics.GL20.GL_TRIANGLE_STRIP
        } else if (type == "LINE_STRIP") {
            com.badlogic.gdx.graphics.GL20.GL_LINE_STRIP
        } else {
            throw com.badlogic.gdx.utils.GdxRuntimeException("Unknown primitive type '" + type
                + "', should be one of triangle, trianglestrip, line, linestrip, lineloop or point")
        }
    }

    protected fun parseAttributes(attributes: com.badlogic.gdx.utils.JsonValue?): Array<com.badlogic.gdx.graphics.VertexAttribute?>? {
        val vertexAttributes: com.badlogic.gdx.utils.Array<com.badlogic.gdx.graphics.VertexAttribute?> = com.badlogic.gdx.utils.Array<com.badlogic.gdx.graphics.VertexAttribute?>()
        var unit = 0
        var blendWeightCount = 0
        var value: com.badlogic.gdx.utils.JsonValue = attributes.child
        while (value != null) {
            val attribute: String = value.asString()
            val attr = attribute
            if (attr == "POSITION") {
                vertexAttributes.add(com.badlogic.gdx.graphics.VertexAttribute.Position())
            } else if (attr == "NORMAL") {
                vertexAttributes.add(com.badlogic.gdx.graphics.VertexAttribute.Normal())
            } else if (attr == "COLOR") {
                vertexAttributes.add(com.badlogic.gdx.graphics.VertexAttribute.ColorUnpacked())
            } else if (attr == "COLORPACKED") {
                vertexAttributes.add(com.badlogic.gdx.graphics.VertexAttribute.ColorPacked())
            } else if (attr == "TANGENT") {
                vertexAttributes.add(com.badlogic.gdx.graphics.VertexAttribute.Tangent())
            } else if (attr == "BINORMAL") {
                vertexAttributes.add(com.badlogic.gdx.graphics.VertexAttribute.Binormal())
            } else if (attr.startsWith("TEXCOORD")) {
                vertexAttributes.add(com.badlogic.gdx.graphics.VertexAttribute.TexCoords(unit++))
            } else if (attr.startsWith("BLENDWEIGHT")) {
                vertexAttributes.add(com.badlogic.gdx.graphics.VertexAttribute.BoneWeight(blendWeightCount++))
            } else {
                throw com.badlogic.gdx.utils.GdxRuntimeException("Unknown vertex attribute '" + attr
                    + "', should be one of position, normal, uv, tangent or binormal")
            }
            value = value.next
        }
        return vertexAttributes.toArray(com.badlogic.gdx.graphics.VertexAttribute::class.java)
    }

    protected fun parseMaterials(model: com.badlogic.gdx.graphics.g3d.model.data.ModelData?, json: com.badlogic.gdx.utils.JsonValue?, materialDir: String?) {
        val materials: com.badlogic.gdx.utils.JsonValue = json.get("materials")
        if (materials == null) { // we should probably create some default material in this case
        } else {
            model.materials.ensureCapacity(materials.size)
            var material: com.badlogic.gdx.utils.JsonValue = materials.child
            while (material != null) {
                val jsonMaterial: com.badlogic.gdx.graphics.g3d.model.data.ModelMaterial = com.badlogic.gdx.graphics.g3d.model.data.ModelMaterial()
                val id: String = material.getString("id", null)
                    ?: throw com.badlogic.gdx.utils.GdxRuntimeException("Material needs an id.")
                jsonMaterial.id = id
                // Read material colors
                val diffuse: com.badlogic.gdx.utils.JsonValue = material.get("diffuse")
                if (diffuse != null) jsonMaterial.diffuse = parseColor(diffuse)
                val ambient: com.badlogic.gdx.utils.JsonValue = material.get("ambient")
                if (ambient != null) jsonMaterial.ambient = parseColor(ambient)
                val emissive: com.badlogic.gdx.utils.JsonValue = material.get("emissive")
                if (emissive != null) jsonMaterial.emissive = parseColor(emissive)
                val specular: com.badlogic.gdx.utils.JsonValue = material.get("specular")
                if (specular != null) jsonMaterial.specular = parseColor(specular)
                val reflection: com.badlogic.gdx.utils.JsonValue = material.get("reflection")
                if (reflection != null) jsonMaterial.reflection = parseColor(reflection)
                // Read shininess
                jsonMaterial.shininess = material.getFloat("shininess", 0.0f)
                // Read opacity
                jsonMaterial.opacity = material.getFloat("opacity", 1.0f)
                // Read textures
                val textures: com.badlogic.gdx.utils.JsonValue = material.get("textures")
                if (textures != null) {
                    var texture: com.badlogic.gdx.utils.JsonValue = textures.child
                    while (texture != null) {
                        val jsonTexture: com.badlogic.gdx.graphics.g3d.model.data.ModelTexture = com.badlogic.gdx.graphics.g3d.model.data.ModelTexture()
                        val textureId: String = texture.getString("id", null)
                            ?: throw com.badlogic.gdx.utils.GdxRuntimeException("Texture has no id.")
                        jsonTexture.id = textureId
                        val fileName: String = texture.getString("filename", null)
                            ?: throw com.badlogic.gdx.utils.GdxRuntimeException("Texture needs filename.")
                        jsonTexture.fileName = (materialDir + (if (materialDir!!.length == 0 || materialDir.endsWith("/")) "" else "/")
                            + fileName)
                        jsonTexture.uvTranslation = readVector2(texture.get("uvTranslation"), 0f, 0f)
                        jsonTexture.uvScaling = readVector2(texture.get("uvScaling"), 1f, 1f)
                        val textureType: String = texture.getString("type", null)
                            ?: throw com.badlogic.gdx.utils.GdxRuntimeException("Texture needs type.")
                        jsonTexture.usage = parseTextureUsage(textureType)
                        if (jsonMaterial.textures == null) jsonMaterial.textures = com.badlogic.gdx.utils.Array<com.badlogic.gdx.graphics.g3d.model.data.ModelTexture?>()
                        jsonMaterial.textures.add(jsonTexture)
                        texture = texture.next
                    }
                }
                model.materials.add(jsonMaterial)
                material = material.next
            }
        }
    }

    protected fun parseTextureUsage(value: String?): Int {
        if (value.equals("AMBIENT", ignoreCase = true)) return com.badlogic.gdx.graphics.g3d.model.data.ModelTexture.USAGE_AMBIENT else if (value.equals("BUMP", ignoreCase = true)) return com.badlogic.gdx.graphics.g3d.model.data.ModelTexture.USAGE_BUMP else if (value.equals("DIFFUSE", ignoreCase = true)) return com.badlogic.gdx.graphics.g3d.model.data.ModelTexture.USAGE_DIFFUSE else if (value.equals("EMISSIVE", ignoreCase = true)) return com.badlogic.gdx.graphics.g3d.model.data.ModelTexture.USAGE_EMISSIVE else if (value.equals("NONE", ignoreCase = true)) return com.badlogic.gdx.graphics.g3d.model.data.ModelTexture.USAGE_NONE else if (value.equals("NORMAL", ignoreCase = true)) return com.badlogic.gdx.graphics.g3d.model.data.ModelTexture.USAGE_NORMAL else if (value.equals("REFLECTION", ignoreCase = true)) return com.badlogic.gdx.graphics.g3d.model.data.ModelTexture.USAGE_REFLECTION else if (value.equals("SHININESS", ignoreCase = true)) return com.badlogic.gdx.graphics.g3d.model.data.ModelTexture.USAGE_SHININESS else if (value.equals("SPECULAR", ignoreCase = true)) return com.badlogic.gdx.graphics.g3d.model.data.ModelTexture.USAGE_SPECULAR else if (value.equals("TRANSPARENCY", ignoreCase = true)) return com.badlogic.gdx.graphics.g3d.model.data.ModelTexture.USAGE_TRANSPARENCY
        return com.badlogic.gdx.graphics.g3d.model.data.ModelTexture.USAGE_UNKNOWN
    }

    protected fun parseColor(colorArray: com.badlogic.gdx.utils.JsonValue?): com.badlogic.gdx.graphics.Color? {
        return if (colorArray.size >= 3) com.badlogic.gdx.graphics.Color(colorArray.getFloat(0), colorArray.getFloat(1), colorArray.getFloat(2), 1.0f) else throw com.badlogic.gdx.utils.GdxRuntimeException("Expected Color values <> than three.")
    }

    protected fun readVector2(vectorArray: com.badlogic.gdx.utils.JsonValue?, x: Float, y: Float): com.badlogic.gdx.math.Vector2? {
        return if (vectorArray == null) com.badlogic.gdx.math.Vector2(x, y) else if (vectorArray.size == 2) com.badlogic.gdx.math.Vector2(vectorArray.getFloat(0), vectorArray.getFloat(1)) else throw com.badlogic.gdx.utils.GdxRuntimeException("Expected Vector2 values <> than two.")
    }

    protected fun parseNodes(model: com.badlogic.gdx.graphics.g3d.model.data.ModelData?, json: com.badlogic.gdx.utils.JsonValue?): com.badlogic.gdx.utils.Array<com.badlogic.gdx.graphics.g3d.model.data.ModelNode?>? {
        val nodes: com.badlogic.gdx.utils.JsonValue = json.get("nodes")
        if (nodes != null) {
            model.nodes.ensureCapacity(nodes.size)
            var node: com.badlogic.gdx.utils.JsonValue = nodes.child
            while (node != null) {
                model.nodes.add(parseNodesRecursively(node))
                node = node.next
            }
        }
        return model.nodes
    }

    protected val tempQ: com.badlogic.gdx.math.Quaternion? = com.badlogic.gdx.math.Quaternion()
    protected fun parseNodesRecursively(json: com.badlogic.gdx.utils.JsonValue?): com.badlogic.gdx.graphics.g3d.model.data.ModelNode? {
        val jsonNode: com.badlogic.gdx.graphics.g3d.model.data.ModelNode = com.badlogic.gdx.graphics.g3d.model.data.ModelNode()
        val id: String = json.getString("id", null)
            ?: throw com.badlogic.gdx.utils.GdxRuntimeException("Node id missing.")
        jsonNode.id = id
        val translation: com.badlogic.gdx.utils.JsonValue = json.get("translation")
        if (translation != null && translation.size != 3) throw com.badlogic.gdx.utils.GdxRuntimeException("Node translation incomplete")
        jsonNode.translation = if (translation == null) null else com.badlogic.gdx.math.Vector3(translation.getFloat(0), translation.getFloat(1),
            translation.getFloat(2))
        val rotation: com.badlogic.gdx.utils.JsonValue = json.get("rotation")
        if (rotation != null && rotation.size != 4) throw com.badlogic.gdx.utils.GdxRuntimeException("Node rotation incomplete")
        jsonNode.rotation = if (rotation == null) null else com.badlogic.gdx.math.Quaternion(rotation.getFloat(0), rotation.getFloat(1),
            rotation.getFloat(2), rotation.getFloat(3))
        val scale: com.badlogic.gdx.utils.JsonValue = json.get("scale")
        if (scale != null && scale.size != 3) throw com.badlogic.gdx.utils.GdxRuntimeException("Node scale incomplete")
        jsonNode.scale = if (scale == null) null else com.badlogic.gdx.math.Vector3(scale.getFloat(0), scale.getFloat(1), scale.getFloat(2))
        val meshId: String = json.getString("mesh", null)
        if (meshId != null) jsonNode.meshId = meshId
        val materials: com.badlogic.gdx.utils.JsonValue = json.get("parts")
        if (materials != null) {
            jsonNode.parts = arrayOfNulls<com.badlogic.gdx.graphics.g3d.model.data.ModelNodePart?>(materials.size)
            var i = 0
            var material: com.badlogic.gdx.utils.JsonValue = materials.child
            while (material != null) {
                val nodePart: com.badlogic.gdx.graphics.g3d.model.data.ModelNodePart = com.badlogic.gdx.graphics.g3d.model.data.ModelNodePart()
                val meshPartId: String = material.getString("meshpartid", null)
                val materialId: String = material.getString("materialid", null)
                if (meshPartId == null || materialId == null) {
                    throw com.badlogic.gdx.utils.GdxRuntimeException("Node $id part is missing meshPartId or materialId")
                }
                nodePart.materialId = materialId
                nodePart.meshPartId = meshPartId
                val bones: com.badlogic.gdx.utils.JsonValue = material.get("bones")
                if (bones != null) {
                    nodePart.bones = com.badlogic.gdx.utils.ArrayMap<String?, com.badlogic.gdx.math.Matrix4?>(true, bones.size, String::class.java, com.badlogic.gdx.math.Matrix4::class.java)
                    var j = 0
                    var bone: com.badlogic.gdx.utils.JsonValue = bones.child
                    while (bone != null) {
                        val nodeId: String = bone.getString("node", null)
                            ?: throw com.badlogic.gdx.utils.GdxRuntimeException("Bone node ID missing")
                        val transform: com.badlogic.gdx.math.Matrix4 = com.badlogic.gdx.math.Matrix4()
                        var `val`: com.badlogic.gdx.utils.JsonValue = bone.get("translation")
                        if (`val` != null && `val`.size >= 3) transform.translate(`val`.getFloat(0), `val`.getFloat(1), `val`.getFloat(2))
                        `val` = bone.get("rotation")
                        if (`val` != null && `val`.size >= 4) transform.rotate(tempQ.set(`val`.getFloat(0), `val`.getFloat(1), `val`.getFloat(2), `val`.getFloat(3)))
                        `val` = bone.get("scale")
                        if (`val` != null && `val`.size >= 3) transform.scale(`val`.getFloat(0), `val`.getFloat(1), `val`.getFloat(2))
                        nodePart.bones.put(nodeId, transform)
                        bone = bone.next
                        j++
                    }
                }
                jsonNode.parts.get(i) = nodePart
                material = material.next
                i++
            }
        }
        val children: com.badlogic.gdx.utils.JsonValue = json.get("children")
        if (children != null) {
            jsonNode.children = arrayOfNulls<com.badlogic.gdx.graphics.g3d.model.data.ModelNode?>(children.size)
            var i = 0
            var child: com.badlogic.gdx.utils.JsonValue = children.child
            while (child != null) {
                jsonNode.children.get(i) = parseNodesRecursively(child)
                child = child.next
                i++
            }
        }
        return jsonNode
    }

    protected fun parseAnimations(model: com.badlogic.gdx.graphics.g3d.model.data.ModelData?, json: com.badlogic.gdx.utils.JsonValue?) {
        val animations: com.badlogic.gdx.utils.JsonValue = json.get("animations") ?: return
        model.animations.ensureCapacity(animations.size)
        var anim: com.badlogic.gdx.utils.JsonValue = animations.child
        while (anim != null) {
            val nodes: com.badlogic.gdx.utils.JsonValue = anim.get("bones")
            if (nodes == null) {
                anim = anim.next
                continue
            }
            val animation: com.badlogic.gdx.graphics.g3d.model.data.ModelAnimation = com.badlogic.gdx.graphics.g3d.model.data.ModelAnimation()
            model.animations.add(animation)
            animation.nodeAnimations.ensureCapacity(nodes.size)
            animation.id = anim.getString("id")
            var node: com.badlogic.gdx.utils.JsonValue = nodes.child
            while (node != null) {
                val nodeAnim: com.badlogic.gdx.graphics.g3d.model.data.ModelNodeAnimation = com.badlogic.gdx.graphics.g3d.model.data.ModelNodeAnimation()
                animation.nodeAnimations.add(nodeAnim)
                nodeAnim.nodeId = node.getString("boneId")
                // For backwards compatibility (version 0.1):
                val keyframes: com.badlogic.gdx.utils.JsonValue = node.get("keyframes")
                if (keyframes != null && keyframes.isArray()) {
                    var keyframe: com.badlogic.gdx.utils.JsonValue = keyframes.child
                    while (keyframe != null) {
                        val keytime: Float = keyframe.getFloat("keytime", 0f) / 1000f
                        val translation: com.badlogic.gdx.utils.JsonValue = keyframe.get("translation")
                        if (translation != null && translation.size == 3) {
                            if (nodeAnim.translation == null) nodeAnim.translation = com.badlogic.gdx.utils.Array<com.badlogic.gdx.graphics.g3d.model.data.ModelNodeKeyframe<com.badlogic.gdx.math.Vector3?>?>()
                            val tkf: com.badlogic.gdx.graphics.g3d.model.data.ModelNodeKeyframe<com.badlogic.gdx.math.Vector3?> = com.badlogic.gdx.graphics.g3d.model.data.ModelNodeKeyframe()
                            tkf.keytime = keytime
                            tkf.value = com.badlogic.gdx.math.Vector3(translation.getFloat(0), translation.getFloat(1), translation.getFloat(2))
                            nodeAnim.translation.add(tkf)
                        }
                        val rotation: com.badlogic.gdx.utils.JsonValue = keyframe.get("rotation")
                        if (rotation != null && rotation.size == 4) {
                            if (nodeAnim.rotation == null) nodeAnim.rotation = com.badlogic.gdx.utils.Array<com.badlogic.gdx.graphics.g3d.model.data.ModelNodeKeyframe<com.badlogic.gdx.math.Quaternion?>?>()
                            val rkf: com.badlogic.gdx.graphics.g3d.model.data.ModelNodeKeyframe<com.badlogic.gdx.math.Quaternion?> = com.badlogic.gdx.graphics.g3d.model.data.ModelNodeKeyframe()
                            rkf.keytime = keytime
                            rkf.value = com.badlogic.gdx.math.Quaternion(rotation.getFloat(0), rotation.getFloat(1), rotation.getFloat(2), rotation.getFloat(3))
                            nodeAnim.rotation.add(rkf)
                        }
                        val scale: com.badlogic.gdx.utils.JsonValue = keyframe.get("scale")
                        if (scale != null && scale.size == 3) {
                            if (nodeAnim.scaling == null) nodeAnim.scaling = com.badlogic.gdx.utils.Array<com.badlogic.gdx.graphics.g3d.model.data.ModelNodeKeyframe<com.badlogic.gdx.math.Vector3?>?>()
                            val skf: com.badlogic.gdx.graphics.g3d.model.data.ModelNodeKeyframe<com.badlogic.gdx.math.Vector3?> = com.badlogic.gdx.graphics.g3d.model.data.ModelNodeKeyframe()
                            skf.keytime = keytime
                            skf.value = com.badlogic.gdx.math.Vector3(scale.getFloat(0), scale.getFloat(1), scale.getFloat(2))
                            nodeAnim.scaling.add(skf)
                        }
                        keyframe = keyframe.next
                    }
                } else { // Version 0.2:
                    val translationKF: com.badlogic.gdx.utils.JsonValue = node.get("translation")
                    if (translationKF != null && translationKF.isArray()) {
                        nodeAnim.translation = com.badlogic.gdx.utils.Array<com.badlogic.gdx.graphics.g3d.model.data.ModelNodeKeyframe<com.badlogic.gdx.math.Vector3?>?>()
                        nodeAnim.translation.ensureCapacity(translationKF.size)
                        var keyframe: com.badlogic.gdx.utils.JsonValue = translationKF.child
                        while (keyframe != null) {
                            val kf: com.badlogic.gdx.graphics.g3d.model.data.ModelNodeKeyframe<com.badlogic.gdx.math.Vector3?> = com.badlogic.gdx.graphics.g3d.model.data.ModelNodeKeyframe()
                            nodeAnim.translation.add(kf)
                            kf.keytime = keyframe.getFloat("keytime", 0f) / 1000f
                            val translation: com.badlogic.gdx.utils.JsonValue = keyframe.get("value")
                            if (translation != null && translation.size >= 3) kf.value = com.badlogic.gdx.math.Vector3(translation.getFloat(0), translation.getFloat(1), translation.getFloat(2))
                            keyframe = keyframe.next
                        }
                    }
                    val rotationKF: com.badlogic.gdx.utils.JsonValue = node.get("rotation")
                    if (rotationKF != null && rotationKF.isArray()) {
                        nodeAnim.rotation = com.badlogic.gdx.utils.Array<com.badlogic.gdx.graphics.g3d.model.data.ModelNodeKeyframe<com.badlogic.gdx.math.Quaternion?>?>()
                        nodeAnim.rotation.ensureCapacity(rotationKF.size)
                        var keyframe: com.badlogic.gdx.utils.JsonValue = rotationKF.child
                        while (keyframe != null) {
                            val kf: com.badlogic.gdx.graphics.g3d.model.data.ModelNodeKeyframe<com.badlogic.gdx.math.Quaternion?> = com.badlogic.gdx.graphics.g3d.model.data.ModelNodeKeyframe()
                            nodeAnim.rotation.add(kf)
                            kf.keytime = keyframe.getFloat("keytime", 0f) / 1000f
                            val rotation: com.badlogic.gdx.utils.JsonValue = keyframe.get("value")
                            if (rotation != null && rotation.size >= 4) kf.value = com.badlogic.gdx.math.Quaternion(rotation.getFloat(0), rotation.getFloat(1), rotation.getFloat(2), rotation.getFloat(3))
                            keyframe = keyframe.next
                        }
                    }
                    val scalingKF: com.badlogic.gdx.utils.JsonValue = node.get("scaling")
                    if (scalingKF != null && scalingKF.isArray()) {
                        nodeAnim.scaling = com.badlogic.gdx.utils.Array<com.badlogic.gdx.graphics.g3d.model.data.ModelNodeKeyframe<com.badlogic.gdx.math.Vector3?>?>()
                        nodeAnim.scaling.ensureCapacity(scalingKF.size)
                        var keyframe: com.badlogic.gdx.utils.JsonValue = scalingKF.child
                        while (keyframe != null) {
                            val kf: com.badlogic.gdx.graphics.g3d.model.data.ModelNodeKeyframe<com.badlogic.gdx.math.Vector3?> = com.badlogic.gdx.graphics.g3d.model.data.ModelNodeKeyframe()
                            nodeAnim.scaling.add(kf)
                            kf.keytime = keyframe.getFloat("keytime", 0f) / 1000f
                            val scaling: com.badlogic.gdx.utils.JsonValue = keyframe.get("value")
                            if (scaling != null && scaling.size >= 3) kf.value = com.badlogic.gdx.math.Vector3(scaling.getFloat(0), scaling.getFloat(1), scaling.getFloat(2))
                            keyframe = keyframe.next
                        }
                    }
                }
                node = node.next
            }
            anim = anim.next
        }
    }

    companion object {
        const val VERSION_HI: Short = 0
        const val VERSION_LO: Short = 1
    }

    init {
        this.reader = reader
    }
}
