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
package com.badlogic.gdx.graphics

/**
 * A single vertex attribute defined by its [Usage], its number of components and its shader alias. The Usage is used
 * for uniquely identifying the vertex attribute from among its [VertexAttributes] siblings. The number of components
 * defines how many components the attribute has. The alias defines to which shader attribute this attribute should bind. The alias
 * is used by a [Mesh] when drawing with a [ShaderProgram]. The alias can be changed at any time.
 *
 * @author mzechner
 */
class VertexAttribute @JvmOverloads constructor(
    /**
     * The attribute [Usage], used for identification.
     */
    val usage: Int,
    /**
     * the number of components this attribute has
     */
    val numComponents: Int,
    /**
     * the OpenGL type of each component, e.g. [GL20.GL_FLOAT] or [GL20.GL_UNSIGNED_BYTE]
     */
    val type: Int,
    /**
     * For fixed types, whether the values are normalized to either -1f and +1f (signed) or 0f and +1f (unsigned)
     */
    val normalized: Boolean,
    /**
     * the alias for the attribute used in a [ShaderProgram]
     */
    var alias: String,
    /**
     * optional unit/index specifier, used for texture coordinates and bone weights
     */
    var unit: Int = 0) {

    /**
     * the offset of this attribute in bytes, don't change this!
     */
    var offset = 0

    private val usageIndex: Int
    /**
     * Constructs a new VertexAttribute. The GL data type is automatically selected based on the usage.
     *
     * @param usage         The attribute [Usage], used to select the [.type] and for identification.
     * @param numComponents the number of components of this attribute, must be between 1 and 4.
     * @param alias         the alias used in a shader for this attribute. Can be changed after construction.
     * @param unit          Optional unit/index specifier, used for texture coordinates and bone weights
     */
    /**
     * Constructs a new VertexAttribute. The GL data type is automatically selected based on the usage.
     *
     * @param usage         The attribute [Usage], used to select the [.type] and for identification.
     * @param numComponents the number of components of this attribute, must be between 1 and 4.
     * @param alias         the alias used in a shader for this attribute. Can be changed after construction.
     */
    @JvmOverloads
    constructor(usage: Int, numComponents: Int, alias: String, unit: Int = 0) : this(usage, numComponents, if (usage == Usage.ColorPacked) GL20.GL_UNSIGNED_BYTE else GL20.GL_FLOAT,
        usage == Usage.ColorPacked, alias, unit) {
    }

    /**
     * @return A copy of this VertexAttribute with the same parameters. The [.offset] is not copied and must
     * be recalculated, as is typically done by the [VertexAttributes] that owns the VertexAttribute.
     */
    fun copy(): VertexAttribute {
        return VertexAttribute(usage, numComponents, type, normalized, alias, unit)
    }

    /**
     * Tests to determine if the passed object was created with the same parameters
     */
    override fun equals(obj: Any?): Boolean {
        return if (obj !is VertexAttribute) {
            false
        } else equals(obj as VertexAttribute?)
    }

    fun equals(other: VertexAttribute?): Boolean {
        return other != null && usage == other.usage && numComponents == other.numComponents && type == other.type && normalized == other.normalized && alias == other.alias && unit == other.unit
    }

    /**
     * @return A unique number specifying the usage index (3 MSB) and unit (1 LSB).
     */
    val key: Int
        get() = (usageIndex shl 8) + (unit and 0xFF)

    /**
     * @return How many bytes this attribute uses.
     */
    val sizeInBytes: Int
        get() {
            when (type) {
                GL20.GL_FLOAT, GL20.GL_FIXED -> return 4 * numComponents
                GL20.GL_UNSIGNED_BYTE, GL20.GL_BYTE -> return numComponents
                GL20.GL_UNSIGNED_SHORT, GL20.GL_SHORT -> return 2 * numComponents
            }
            return 0
        }

    override fun hashCode(): Int {
        var result = key
        result = 541 * result + numComponents
        result = 541 * result + alias.hashCode()
        return result
    }

    companion object {
        fun Position(): VertexAttribute {
            return VertexAttribute(Usage.Position, 3, ShaderProgram.POSITION_ATTRIBUTE)
        }

        fun TexCoords(unit: Int): VertexAttribute {
            return VertexAttribute(Usage.TextureCoordinates, 2, ShaderProgram.TEXCOORD_ATTRIBUTE + unit, unit)
        }

        fun Normal(): VertexAttribute {
            return VertexAttribute(Usage.Normal, 3, ShaderProgram.NORMAL_ATTRIBUTE)
        }

        fun ColorPacked(): VertexAttribute {
            return VertexAttribute(Usage.ColorPacked, 4, GL20.GL_UNSIGNED_BYTE, true, ShaderProgram.COLOR_ATTRIBUTE)
        }

        fun ColorUnpacked(): VertexAttribute {
            return VertexAttribute(Usage.ColorUnpacked, 4, GL20.GL_FLOAT, false, ShaderProgram.COLOR_ATTRIBUTE)
        }

        fun Tangent(): VertexAttribute {
            return VertexAttribute(Usage.Tangent, 3, ShaderProgram.TANGENT_ATTRIBUTE)
        }

        fun Binormal(): VertexAttribute {
            return VertexAttribute(Usage.BiNormal, 3, ShaderProgram.BINORMAL_ATTRIBUTE)
        }

        fun BoneWeight(unit: Int): VertexAttribute {
            return VertexAttribute(Usage.BoneWeight, 2, ShaderProgram.BONEWEIGHT_ATTRIBUTE + unit, unit)
        }
    }
    /**
     * Constructs a new VertexAttribute.
     *
     * @param usage         The attribute [Usage], used for identification.
     * @param numComponents the number of components of this attribute, must be between 1 and 4.
     * @param type          the OpenGL type of each component, e.g. [GL20.GL_FLOAT] or [GL20.GL_UNSIGNED_BYTE]. Since [Mesh]
     * stores vertex data in 32bit floats, the total size of this attribute (type size times number of components) must be a
     * multiple of four bytes.
     * @param normalized    For fixed types, whether the values are normalized to either -1f and +1f (signed) or 0f and +1f (unsigned)
     * @param alias         The alias used in a shader for this attribute. Can be changed after construction.
     * @param unit          Optional unit/index specifier, used for texture coordinates and bone weights
     */
    /**
     * Constructs a new VertexAttribute.
     *
     * @param usage         The attribute [Usage], used for identification.
     * @param numComponents the number of components of this attribute, must be between 1 and 4.
     * @param type          the OpenGL type of each component, e.g. [GL20.GL_FLOAT] or [GL20.GL_UNSIGNED_BYTE]. Since [Mesh]
     * stores vertex data in 32bit floats, the total size of this attribute (type size times number of components) must be a
     * multiple of four.
     * @param normalized    For fixed types, whether the values are normalized to either -1f and +1f (signed) or 0f and +1f (unsigned)
     * @param alias         The alias used in a shader for this attribute. Can be changed after construction.
     */
    init {
        usageIndex = java.lang.Integer.numberOfTrailingZeros(usage)
    }
}
