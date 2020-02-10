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
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.g3d.decals.CameraGroupStrategy
import com.badlogic.gdx.graphics.g3d.decals.DecalBatch
import com.badlogic.gdx.graphics.g3d.decals.DecalMaterial
import com.badlogic.gdx.graphics.g3d.decals.SimpleOrthoGroupStrategy
import com.badlogic.gdx.math.Quaternion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.NumberUtils

/**
 *
 *
 * Represents a sprite in 3d space. Typical 3d transformations such as translation, rotation and scaling are supported. The
 * position includes a z component other than setting the depth no manual layering has to be performed, correct overlay is
 * guaranteed by using the depth buffer.
 *
 *
 * Decals are handled by the [DecalBatch].
 */
class Decal {

    /**
     * Set a multipurpose value which can be queried and used for things like group identification.
     */
    var value = 0

    /**
     * Returns the vertices backing this sprite.<br></br>
     * The returned value should under no circumstances be modified.
     *
     * @return vertex array backing the decal
     */
    var vertices: FloatArray? = FloatArray(SIZE)
        protected set
    var position: Vector3? = Vector3()
    protected var rotation: Quaternion? = Quaternion()
    protected var scale: Vector2? = Vector2(1, 1)
    protected var color: Color? = Color()

    /**
     * The transformation offset can be used to change the pivot point for rotation and scaling. By default the pivot is the middle
     * of the decal.
     */
    var transformationOffset: Vector2? = null
    protected var dimensions: Vector2? = Vector2()
    var material: DecalMaterial?
    protected var updated = false

    constructor() {
        material = DecalMaterial()
    }

    constructor(material: DecalMaterial?) {
        this.material = material
    }

    /**
     * Sets the color of all four vertices to the specified color
     *
     * @param r Red component
     * @param g Green component
     * @param b Blue component
     * @param a Alpha component
     */
    fun setColor(r: Float, g: Float, b: Float, a: Float) {
        color.set(r, g, b, a)
        val intBits = (255 * a).toInt() shl 24 or ((255 * b).toInt() shl 16) or ((255 * g).toInt() shl 8) or (255 * r).toInt()
        val color: Float = NumberUtils.intToFloatColor(intBits)
        vertices!![C1] = color
        vertices!![C2] = color
        vertices!![C3] = color
        vertices!![C4] = color
    }

    /**
     * Sets the color used to tint this decal. Default is [Color.WHITE].
     */
    fun setColor(tint: Color?) {
        color.set(tint)
        val color: Float = tint.toFloatBits()
        vertices!![C1] = color
        vertices!![C2] = color
        vertices!![C3] = color
        vertices!![C4] = color
    }

    /**
     * Sets the color of this decal, expanding the alpha from 0-254 to 0-255.
     *
     * @see .setColor
     */
    fun setPackedColor(color: Float) {
        Color.abgr8888ToColor(this.color, color)
        vertices!![C1] = color
        vertices!![C2] = color
        vertices!![C3] = color
        vertices!![C4] = color
    }

    /**
     * Sets the rotation on the local X axis to the specified angle
     *
     * @param angle Angle in degrees to set rotation to
     */
    fun setRotationX(angle: Float) {
        rotation.set(Vector3.X, angle)
        updated = false
    }

    /**
     * Sets the rotation on the local Y axis to the specified angle
     *
     * @param angle Angle in degrees to set rotation to
     */
    fun setRotationY(angle: Float) {
        rotation.set(Vector3.Y, angle)
        updated = false
    }

    /**
     * Sets the rotation on the local Z axis to the specified angle
     *
     * @param angle Angle in degrees to set rotation to
     */
    fun setRotationZ(angle: Float) {
        rotation.set(Vector3.Z, angle)
        updated = false
    }

    /**
     * Rotates along local X axis by the specified angle
     *
     * @param angle Angle in degrees to rotate by
     */
    fun rotateX(angle: Float) {
        rotator.set(Vector3.X, angle)
        rotation.mul(rotator)
        updated = false
    }

    /**
     * Rotates along local Y axis by the specified angle
     *
     * @param angle Angle in degrees to rotate by
     */
    fun rotateY(angle: Float) {
        rotator.set(Vector3.Y, angle)
        rotation.mul(rotator)
        updated = false
    }

    /**
     * Rotates along local Z axis by the specified angle
     *
     * @param angle Angle in degrees to rotate by
     */
    fun rotateZ(angle: Float) {
        rotator.set(Vector3.Z, angle)
        rotation.mul(rotator)
        updated = false
    }

    /**
     * Sets the rotation of this decal to the given angles on all axes.
     *
     * @param yaw   Angle in degrees to rotate around the Y axis
     * @param pitch Angle in degrees to rotate around the X axis
     * @param roll  Angle in degrees to rotate around the Z axis
     */
    fun setRotation(yaw: Float, pitch: Float, roll: Float) {
        rotation.setEulerAngles(yaw, pitch, roll)
        updated = false
    }

    /**
     * Sets the rotation of this decal based on the (normalized) direction and up vector.
     *
     * @param dir the direction vector
     * @param up  the up vector
     */
    fun setRotation(dir: Vector3?, up: Vector3?) {
        tmp!!.set(up!!).crs(dir!!).nor()
        tmp2!!.set(dir).crs(tmp).nor()
        rotation.setFromAxes(tmp.x, tmp2.x, dir.x, tmp.y, tmp2.y, dir.y, tmp.z, tmp2.z, dir.z)
        updated = false
    }

    /**
     * Sets the rotation of this decal based on the provided Quaternion
     *
     * @param q desired Rotation
     */
    fun setRotation(q: Quaternion?) {
        rotation.set(q)
        updated = false
    }

    /**
     * Returns the rotation. The returned quaternion should under no circumstances be modified.
     *
     * @return Quaternion representing the rotation
     */
    fun getRotation(): Quaternion? {
        return rotation
    }

    /**
     * Moves by the specified amount of units along the x axis
     *
     * @param units Units to move the decal
     */
    fun translateX(units: Float) {
        position!!.x += units
        updated = false
    }

    /**
     * @return position on the x axis
     */
    /**
     * Sets the position on the x axis
     *
     * @param x Position to locate the decal at
     */
    var x: Float
        get() = position!!.x
        set(x) {
            position!!.x = x
            updated = false
        }

    /**
     * Moves by the specified amount of units along the y axis
     *
     * @param units Units to move the decal
     */
    fun translateY(units: Float) {
        position!!.y += units
        updated = false
    }

    /**
     * @return position on the y axis
     */
    /**
     * Sets the position on the y axis
     *
     * @param y Position to locate the decal at
     */
    var y: Float
        get() = position!!.y
        set(y) {
            position!!.y = y
            updated = false
        }

    /**
     * Moves by the specified amount of units along the z axis
     *
     * @param units Units to move the decal
     */
    fun translateZ(units: Float) {
        position!!.z += units
        updated = false
    }

    /**
     * @return position on the z axis
     */
    /**
     * Sets the position on the z axis
     *
     * @param z Position to locate the decal at
     */
    var z: Float
        get() = position!!.z
        set(z) {
            position!!.z = z
            updated = false
        }

    /**
     * Translates by the specified amount of units
     *
     * @param x Units to move along the x axis
     * @param y Units to move along the y axis
     * @param z Units to move along the z axis
     */
    fun translate(x: Float, y: Float, z: Float) {
        position!!.add(x, y, z)
        updated = false
    }

    /**
     * @see Decal.translate
     */
    fun translate(trans: Vector3?) {
        position!!.add(trans!!)
        updated = false
    }

    /**
     * Sets the position to the given world coordinates
     *
     * @param x X position
     * @param y Y Position
     * @param z Z Position
     */
    fun setPosition(x: Float, y: Float, z: Float) {
        position!![x, y] = z
        updated = false
    }

    /**
     * @see Decal.setPosition
     */
    fun setPosition(pos: Vector3?) {
        position!!.set(pos!!)
        updated = false
    }

    /**
     * Returns the color of this decal. The returned color should under no circumstances be modified.
     *
     * @return The color of this decal.
     */
    fun getColor(): Color? {
        return color
    }

    /**
     * Returns the position of this decal. The returned vector should under no circumstances be modified.
     *
     * @return vector representing the position
     */
    fun getPosition(): Vector3? {
        return position
    }

    /**
     * @return Scale on the x axis
     */
    /**
     * Sets scale along the x axis
     *
     * @param scale New scale along x axis
     */
    var scaleX: Float
        get() = scale!!.x
        set(scale) {
            this.scale!!.x = scale
            updated = false
        }

    /**
     * @return Scale on the y axis
     */
    /**
     * Sets scale along the y axis
     *
     * @param scale New scale along y axis
     */
    var scaleY: Float
        get() = scale!!.y
        set(scale) {
            this.scale!!.y = scale
            updated = false
        }

    /**
     * Sets scale along both the x and y axis
     *
     * @param scaleX Scale on the x axis
     * @param scaleY Scale on the y axis
     */
    fun setScale(scaleX: Float, scaleY: Float) {
        scale!![scaleX] = scaleY
        updated = false
    }

    /**
     * Sets scale along both the x and y axis
     *
     * @param scale New scale
     */
    fun setScale(scale: Float) {
        this.scale!![scale] = scale
        updated = false
    }

    /**
     * @return width in world units
     */
    /**
     * Sets the width in world units
     *
     * @param width Width in world units
     */
    var width: Float
        get() = dimensions!!.x
        set(width) {
            dimensions!!.x = width
            updated = false
        }

    /**
     * @return height in world units
     */
    /**
     * Sets the height in world units
     *
     * @param height Height in world units
     */
    var height: Float
        get() = dimensions!!.y
        set(height) {
            dimensions!!.y = height
            updated = false
        }

    /**
     * Sets the width and height in world units
     *
     * @param width  Width in world units
     * @param height Height in world units
     */
    fun setDimensions(width: Float, height: Float) {
        dimensions!![width] = height
        updated = false
    }

    /**
     * Recalculates vertices array if it grew out of sync with the properties (position, ..)
     */
    fun update() {
        if (!updated) {
            resetVertices()
            transformVertices()
        }
    }

    /**
     * Transforms the position component of the vertices using properties such as position, scale, etc.
     */
    protected fun transformVertices() {
        /** It would be possible to also load the x,y,z into a Vector3 and apply all the transformations using already existing
         * methods. Especially the quaternion rotation already exists in the Quaternion class, it then would look like this:
         * ----------------------------------------------------------------------------------------------------
         * v3.set(vertices[xIndex] * scale.x, vertices[yIndex] * scale.y, vertices[zIndex]); rotation.transform(v3);
         * v3.add(position); vertices[xIndex] = v3.x; vertices[yIndex] = v3.y; vertices[zIndex] = v3.z;
         * ---------------------------------------------------------------------------------------------------- However, a half ass
         * benchmark with dozens of thousands decals showed that doing it "by hand", as done here, is about 10% faster. So while
         * duplicate code should be avoided for maintenance reasons etc. the performance gain is worth it. The math doesn't change.  */
        var x: Float
        var y: Float
        var z: Float
        var w: Float
        val tx: Float
        val ty: Float
        if (transformationOffset != null) {
            tx = -transformationOffset!!.x
            ty = -transformationOffset!!.y
        } else {
            ty = 0f
            tx = ty
        }
        /** Transform the first vertex  */
        // first apply the scale to the vector
        x = (vertices!![X1] + tx) * scale!!.x
        y = (vertices!![Y1] + ty) * scale!!.y
        z = vertices!![Z1]
        // then transform the vector using the rotation quaternion
        vertices!![X1] = rotation.w * x + rotation.y * z - rotation.z * y
        vertices!![Y1] = rotation.w * y + rotation.z * x - rotation.x * z
        vertices!![Z1] = rotation.w * z + rotation.x * y - rotation.y * x
        w = -rotation.x * x - rotation.y * y - rotation.z * z
        rotation.conjugate()
        x = vertices!![X1]
        y = vertices!![Y1]
        z = vertices!![Z1]
        vertices!![X1] = w * rotation.x + x * rotation.w + y * rotation.z - z * rotation.y
        vertices!![Y1] = w * rotation.y + y * rotation.w + z * rotation.x - x * rotation.z
        vertices!![Z1] = w * rotation.z + z * rotation.w + x * rotation.y - y * rotation.x
        rotation.conjugate() // <- don't forget to conjugate the rotation back to normal
        // finally translate the vector according to position
        vertices!![X1] += position!!.x - tx
        vertices!![Y1] += position!!.y - ty
        vertices!![Z1] += position!!.z
        /** Transform the second vertex  */
        // first apply the scale to the vector
        x = (vertices!![X2] + tx) * scale!!.x
        y = (vertices!![Y2] + ty) * scale!!.y
        z = vertices!![Z2]
        // then transform the vector using the rotation quaternion
        vertices!![X2] = rotation.w * x + rotation.y * z - rotation.z * y
        vertices!![Y2] = rotation.w * y + rotation.z * x - rotation.x * z
        vertices!![Z2] = rotation.w * z + rotation.x * y - rotation.y * x
        w = -rotation.x * x - rotation.y * y - rotation.z * z
        rotation.conjugate()
        x = vertices!![X2]
        y = vertices!![Y2]
        z = vertices!![Z2]
        vertices!![X2] = w * rotation.x + x * rotation.w + y * rotation.z - z * rotation.y
        vertices!![Y2] = w * rotation.y + y * rotation.w + z * rotation.x - x * rotation.z
        vertices!![Z2] = w * rotation.z + z * rotation.w + x * rotation.y - y * rotation.x
        rotation.conjugate() // <- don't forget to conjugate the rotation back to normal
        // finally translate the vector according to position
        vertices!![X2] += position!!.x - tx
        vertices!![Y2] += position!!.y - ty
        vertices!![Z2] += position!!.z
        /** Transform the third vertex  */
        // first apply the scale to the vector
        x = (vertices!![X3] + tx) * scale!!.x
        y = (vertices!![Y3] + ty) * scale!!.y
        z = vertices!![Z3]
        // then transform the vector using the rotation quaternion
        vertices!![X3] = rotation.w * x + rotation.y * z - rotation.z * y
        vertices!![Y3] = rotation.w * y + rotation.z * x - rotation.x * z
        vertices!![Z3] = rotation.w * z + rotation.x * y - rotation.y * x
        w = -rotation.x * x - rotation.y * y - rotation.z * z
        rotation.conjugate()
        x = vertices!![X3]
        y = vertices!![Y3]
        z = vertices!![Z3]
        vertices!![X3] = w * rotation.x + x * rotation.w + y * rotation.z - z * rotation.y
        vertices!![Y3] = w * rotation.y + y * rotation.w + z * rotation.x - x * rotation.z
        vertices!![Z3] = w * rotation.z + z * rotation.w + x * rotation.y - y * rotation.x
        rotation.conjugate() // <- don't forget to conjugate the rotation back to normal
        // finally translate the vector according to position
        vertices!![X3] += position!!.x - tx
        vertices!![Y3] += position!!.y - ty
        vertices!![Z3] += position!!.z
        /** Transform the fourth vertex  */
        // first apply the scale to the vector
        x = (vertices!![X4] + tx) * scale!!.x
        y = (vertices!![Y4] + ty) * scale!!.y
        z = vertices!![Z4]
        // then transform the vector using the rotation quaternion
        vertices!![X4] = rotation.w * x + rotation.y * z - rotation.z * y
        vertices!![Y4] = rotation.w * y + rotation.z * x - rotation.x * z
        vertices!![Z4] = rotation.w * z + rotation.x * y - rotation.y * x
        w = -rotation.x * x - rotation.y * y - rotation.z * z
        rotation.conjugate()
        x = vertices!![X4]
        y = vertices!![Y4]
        z = vertices!![Z4]
        vertices!![X4] = w * rotation.x + x * rotation.w + y * rotation.z - z * rotation.y
        vertices!![Y4] = w * rotation.y + y * rotation.w + z * rotation.x - x * rotation.z
        vertices!![Z4] = w * rotation.z + z * rotation.w + x * rotation.y - y * rotation.x
        rotation.conjugate() // <- don't forget to conjugate the rotation back to normal
        // finally translate the vector according to position
        vertices!![X4] += position!!.x - tx
        vertices!![Y4] += position!!.y - ty
        vertices!![Z4] += position!!.z
        updated = true
    }

    /**
     * Resets the position components of the vertices array based ont he dimensions (preparation for transformation)
     */
    protected fun resetVertices() {
        val left = -dimensions!!.x / 2f
        val right = left + dimensions!!.x
        val top = dimensions!!.y / 2f
        val bottom = top - dimensions!!.y

        // left top
        vertices!![X1] = left
        vertices!![Y1] = top
        vertices!![Z1] = 0
        // right top
        vertices!![X2] = right
        vertices!![Y2] = top
        vertices!![Z2] = 0
        // left bot
        vertices!![X3] = left
        vertices!![Y3] = bottom
        vertices!![Z3] = 0
        // right bot
        vertices!![X4] = right
        vertices!![Y4] = bottom
        vertices!![Z4] = 0
        updated = false
    }

    /**
     * Re-applies the uv coordinates from the material's texture region to the uv components of the vertices array
     */
    protected fun updateUVs() {
        val tr: TextureRegion = material!!.textureRegion
        // left top
        vertices!![U1] = tr.getU()
        vertices!![V1] = tr.getV()
        // right top
        vertices!![U2] = tr.getU2()
        vertices!![V2] = tr.getV()
        // left bot
        vertices!![U3] = tr.getU()
        vertices!![V3] = tr.getV2()
        // right bot
        vertices!![U4] = tr.getU2()
        vertices!![V4] = tr.getV2()
    }

    /**
     * @return the texture region this Decal uses. Do not modify it!
     */
    /**
     * Sets the texture region
     *
     * @param textureRegion Texture region to apply
     */
    var textureRegion: TextureRegion?
        get() = material!!.textureRegion
        set(textureRegion) {
            material!!.textureRegion = textureRegion
            updateUVs()
        }

    /**
     * Sets the blending parameters for this decal
     *
     * @param srcBlendFactor Source blend factor used by glBlendFunc
     * @param dstBlendFactor Destination blend factor used by glBlendFunc
     */
    fun setBlending(srcBlendFactor: Int, dstBlendFactor: Int) {
        material!!.srcBlendFactor = srcBlendFactor
        material!!.dstBlendFactor = dstBlendFactor
    }

    fun getMaterial(): DecalMaterial? {
        return material
    }

    /**
     * Set material
     *
     * @param material custom material
     */
    fun setMaterial(material: DecalMaterial?) {
        this.material = material
    }

    /**
     * Sets the rotation of the Decal to face the given point. Useful for billboarding.
     *
     * @param position
     * @param up
     */
    fun lookAt(position: Vector3?, up: Vector3?) {
        dir!!.set(position!!).sub(this.position)!!.nor()
        setRotation(dir, up)
    }

    companion object {
        // 3(x,y,z) + 1(color) + 2(u,v)
        /**
         * Size of a decal vertex in floats
         */
        private const val VERTEX_SIZE = 3 + 1 + 2

        /**
         * Size of the decal in floats. It takes a float[SIZE] to hold the decal.
         */
        const val SIZE = 4 * VERTEX_SIZE

        /**
         * Temporary vector for various calculations.
         */
        private val tmp: Vector3? = Vector3()
        private val tmp2: Vector3? = Vector3()
        val dir: Vector3? = Vector3()

        // meaning of the floats in the vertices array
        const val X1 = 0
        const val Y1 = 1
        const val Z1 = 2
        const val C1 = 3
        const val U1 = 4
        const val V1 = 5
        const val X2 = 6
        const val Y2 = 7
        const val Z2 = 8
        const val C2 = 9
        const val U2 = 10
        const val V2 = 11
        const val X3 = 12
        const val Y3 = 13
        const val Z3 = 14
        const val C3 = 15
        const val U3 = 16
        const val V3 = 17
        const val X4 = 18
        const val Y4 = 19
        const val Z4 = 20
        const val C4 = 21
        const val U4 = 22
        const val V4 = 23
        protected var rotator: Quaternion? = Quaternion(0, 0, 0, 0)

        /**
         * Creates a decal assuming the dimensions of the texture region
         *
         * @param textureRegion Texture region to use
         * @return Created decal
         */
        fun newDecal(textureRegion: TextureRegion?): Decal? {
            return newDecal(textureRegion!!.getRegionWidth().toFloat(), textureRegion.getRegionHeight().toFloat(), textureRegion, DecalMaterial.NO_BLEND,
                DecalMaterial.NO_BLEND)
        }

        /**
         * Creates a decal assuming the dimensions of the texture region and adding transparency
         *
         * @param textureRegion   Texture region to use
         * @param hasTransparency Whether or not this sprite will be treated as having transparency (transparent png, etc.)
         * @return Created decal
         */
        fun newDecal(textureRegion: TextureRegion?, hasTransparency: Boolean): Decal? {
            return newDecal(textureRegion!!.getRegionWidth().toFloat(), textureRegion.getRegionHeight().toFloat(), textureRegion,
                if (hasTransparency) GL20.GL_SRC_ALPHA else DecalMaterial.NO_BLEND, if (hasTransparency) GL20.GL_ONE_MINUS_SRC_ALPHA else DecalMaterial.NO_BLEND)
        }

        /**
         * Creates a decal using the region for texturing
         *
         * @param width           Width of the decal in world units
         * @param height          Height of the decal in world units
         * @param textureRegion   TextureRegion to use
         * @param hasTransparency Whether or not this sprite will be treated as having transparency (transparent png, etc.)
         * @return Created decal
         */
        fun newDecal(width: Float, height: Float, textureRegion: TextureRegion?, hasTransparency: Boolean): Decal? {
            return newDecal(width, height, textureRegion, if (hasTransparency) GL20.GL_SRC_ALPHA else DecalMaterial.NO_BLEND,
                if (hasTransparency) GL20.GL_ONE_MINUS_SRC_ALPHA else DecalMaterial.NO_BLEND)
        }
        /**
         * Creates a decal using the region for texturing and the specified blending parameters for blending
         *
         * @param width          Width of the decal in world units
         * @param height         Height of the decal in world units
         * @param textureRegion  TextureRegion to use
         * @param srcBlendFactor Source blend used by glBlendFunc
         * @param dstBlendFactor Destination blend used by glBlendFunc
         * @return Created decal
         */
        /**
         * Creates a decal using the region for texturing
         *
         * @param width         Width of the decal in world units
         * @param height        Height of the decal in world units
         * @param textureRegion TextureRegion to use
         * @return Created decal
         */
        // TODO : it would be convenient if {@link com.badlogic.gdx.graphics.Texture} had a getFormat() method to assume transparency
        // from RGBA,..
        @JvmOverloads
        fun newDecal(width: Float, height: Float, textureRegion: TextureRegion?, srcBlendFactor: Int = DecalMaterial.NO_BLEND, dstBlendFactor: Int = DecalMaterial.NO_BLEND): Decal? {
            val decal = Decal()
            decal.textureRegion = textureRegion
            decal.setBlending(srcBlendFactor, dstBlendFactor)
            decal.dimensions!!.x = width
            decal.dimensions!!.y = height
            decal.setColor(1f, 1f, 1f, 1f)
            return decal
        }

        /**
         * Creates a decal using the region for texturing and the specified blending parameters for blending
         *
         * @param width          Width of the decal in world units
         * @param height         Height of the decal in world units
         * @param textureRegion  TextureRegion to use
         * @param srcBlendFactor Source blend used by glBlendFunc
         * @param dstBlendFactor Destination blend used by glBlendFunc
         * @param material       Custom decal material
         * @return Created decal
         */
        fun newDecal(width: Float, height: Float, textureRegion: TextureRegion?, srcBlendFactor: Int, dstBlendFactor: Int,
                     material: DecalMaterial?): Decal? {
            val decal = Decal(material)
            decal.textureRegion = textureRegion
            decal.setBlending(srcBlendFactor, dstBlendFactor)
            decal.dimensions!!.x = width
            decal.dimensions!!.y = height
            decal.setColor(1f, 1f, 1f, 1f)
            return decal
        }
    }
}
