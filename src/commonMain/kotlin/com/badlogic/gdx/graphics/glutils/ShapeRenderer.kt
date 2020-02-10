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
package com.badlogic.gdx.graphics.glutils

import com.badlogic.gdx.graphics.glutils.HdpiMode
import com.badlogic.gdx.graphics.glutils.InstanceData
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType
import java.io.BufferedInputStream
import java.lang.IllegalStateException
import java.lang.NumberFormatException
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import java.util.HashMap
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/** Renders points, lines, shape outlines and filled shapes.
 *
 *
 * By default a 2D orthographic projection with the origin in the lower left corner is used and units are specified in screen
 * pixels. This can be changed by configuring the projection matrix, usually using the [Camera.combined] matrix. If the
 * screen resolution changes, the projection matrix may need to be updated.
 *
 *
 * Shapes are rendered in batches to increase performance. Standard usage pattern looks as follows:
 *
 * <pre>
 * `camera.update();
 * shapeRenderer.setProjectionMatrix(camera.combined);
 *
 * shapeRenderer.begin(ShapeType.Line);
 * shapeRenderer.setColor(1, 1, 0, 1);
 * shapeRenderer.line(x, y, x2, y2);
 * shapeRenderer.rect(x, y, width, height);
 * shapeRenderer.circle(x, y, radius);
 * shapeRenderer.end();
 *
 * shapeRenderer.begin(ShapeType.Filled);
 * shapeRenderer.setColor(0, 1, 0, 1);
 * shapeRenderer.rect(x, y, width, height);
 * shapeRenderer.circle(x, y, radius);
 * shapeRenderer.end();
` *
</pre> *
 *
 * ShapeRenderer has a second matrix called the transformation matrix which is used to rotate, scale and translate shapes in a
 * more flexible manner. The following example shows how to rotate a rectangle around its center using the z-axis as the rotation
 * axis and placing it's center at (20, 12, 2):
 *
 * <pre>
 * shapeRenderer.begin(ShapeType.Line);
 * shapeRenderer.identity();
 * shapeRenderer.translate(20, 12, 2);
 * shapeRenderer.rotate(0, 0, 1, 90);
 * shapeRenderer.rect(-width / 2, -height / 2, width, height);
 * shapeRenderer.end();
</pre> *
 *
 * Matrix operations all use postmultiplication and work just like glTranslate, glScale and glRotate. The last transformation
 * specified will be the first that is applied to a shape (rotate then translate in the above example).
 *
 *
 * The projection and transformation matrices are a state of the ShapeRenderer, just like the color, and will be applied to all
 * shapes until they are changed.
 * @author mzechner
 * @author stbachmann
 * @author Nathan Sweet
 */
class ShapeRenderer @JvmOverloads constructor(maxVertices: Int = 5000, defaultShader: com.badlogic.gdx.graphics.glutils.ShaderProgram? = null) : com.badlogic.gdx.utils.Disposable {

    /** Shape types to be used with [.begin].
     * @author mzechner, stbachmann
     */
    enum class ShapeType(private val glType: Int) {

        Point(com.badlogic.gdx.graphics.GL20.GL_POINTS), Line(com.badlogic.gdx.graphics.GL20.GL_LINES), Filled(com.badlogic.gdx.graphics.GL20.GL_TRIANGLES);

        fun getGlType(): Int {
            return glType
        }
    }

    private var renderer: com.badlogic.gdx.graphics.glutils.ImmediateModeRenderer? = null
    private var matrixDirty = false
    private val projectionMatrix: com.badlogic.gdx.math.Matrix4? = com.badlogic.gdx.math.Matrix4()
    private val transformMatrix: com.badlogic.gdx.math.Matrix4? = com.badlogic.gdx.math.Matrix4()
    private val combinedMatrix: com.badlogic.gdx.math.Matrix4? = com.badlogic.gdx.math.Matrix4()
    private val tmp: com.badlogic.gdx.math.Vector2? = com.badlogic.gdx.math.Vector2()
    private val color: com.badlogic.gdx.graphics.Color? = com.badlogic.gdx.graphics.Color(1, 1, 1, 1)
    private var shapeType: ShapeType? = null
    private var autoShapeType = false
    private val defaultRectLineWidth = 0.75f
    /** Sets the color to be used by the next shapes drawn.  */
    fun setColor(color: com.badlogic.gdx.graphics.Color?) {
        this.color.set(color)
    }

    /** Sets the color to be used by the next shapes drawn.  */
    fun setColor(r: Float, g: Float, b: Float, a: Float) {
        color.set(r, g, b, a)
    }

    fun getColor(): com.badlogic.gdx.graphics.Color? {
        return color
    }

    fun updateMatrices() {
        matrixDirty = true
    }

    /** Sets the projection matrix to be used for rendering. Usually this will be set to [Camera.combined].
     * @param matrix
     */
    fun setProjectionMatrix(matrix: com.badlogic.gdx.math.Matrix4?) {
        projectionMatrix.set(matrix)
        matrixDirty = true
    }

    /** If the matrix is modified, [.updateMatrices] must be called.  */
    fun getProjectionMatrix(): com.badlogic.gdx.math.Matrix4? {
        return projectionMatrix
    }

    fun setTransformMatrix(matrix: com.badlogic.gdx.math.Matrix4?) {
        transformMatrix.set(matrix)
        matrixDirty = true
    }

    /** If the matrix is modified, [.updateMatrices] must be called.  */
    fun getTransformMatrix(): com.badlogic.gdx.math.Matrix4? {
        return transformMatrix
    }

    /** Sets the transformation matrix to identity.  */
    fun identity() {
        transformMatrix.idt()
        matrixDirty = true
    }

    /** Multiplies the current transformation matrix by a translation matrix.  */
    fun translate(x: Float, y: Float, z: Float) {
        transformMatrix.translate(x, y, z)
        matrixDirty = true
    }

    /** Multiplies the current transformation matrix by a rotation matrix.  */
    fun rotate(axisX: Float, axisY: Float, axisZ: Float, degrees: Float) {
        transformMatrix.rotate(axisX, axisY, axisZ, degrees)
        matrixDirty = true
    }

    /** Multiplies the current transformation matrix by a scale matrix.  */
    fun scale(scaleX: Float, scaleY: Float, scaleZ: Float) {
        transformMatrix.scale(scaleX, scaleY, scaleZ)
        matrixDirty = true
    }

    /** If true, when drawing a shape cannot be performed with the current shape type, the batch is flushed and the shape type is
     * changed automatically. This can increase the number of batch flushes if care is not taken to draw the same type of shapes
     * together. Default is false.  */
    fun setAutoShapeType(autoShapeType: Boolean) {
        this.autoShapeType = autoShapeType
    }

    /** Begins a new batch without specifying a shape type.
     * @throws IllegalStateException if [.autoShapeType] is false.
     */
    fun begin() {
        check(autoShapeType) { "autoShapeType must be true to use this method." }
        begin(ShapeType.Line)
    }

    /** Starts a new batch of shapes. Shapes drawn within the batch will attempt to use the type specified. The call to this method
     * must be paired with a call to [.end].
     * @see .setAutoShapeType
     */
    fun begin(type: ShapeType?) {
        check(shapeType == null) { "Call end() before beginning a new shape batch." }
        shapeType = type
        if (matrixDirty) {
            combinedMatrix.set(projectionMatrix)
            com.badlogic.gdx.math.Matrix4.mul(combinedMatrix.`val`, transformMatrix.`val`)
            matrixDirty = false
        }
        renderer!!.begin(combinedMatrix, shapeType!!.getGlType())
    }

    fun set(type: ShapeType?) {
        if (shapeType == type) return
        checkNotNull(shapeType) { "begin must be called first." }
        check(autoShapeType) { "autoShapeType must be enabled." }
        end()
        begin(type)
    }

    /** Draws a point using [ShapeType.Point], [ShapeType.Line] or [ShapeType.Filled].  */
    fun point(x: Float, y: Float, z: Float) {
        if (shapeType == ShapeType.Line) {
            val size = defaultRectLineWidth * 0.5f
            line(x - size, y - size, z, x + size, y + size, z)
            return
        } else if (shapeType == ShapeType.Filled) {
            val size = defaultRectLineWidth * 0.5f
            box(x - size, y - size, z - size, defaultRectLineWidth, defaultRectLineWidth, defaultRectLineWidth)
            return
        }
        check(ShapeType.Point, null, 1)
        renderer.color(color)
        renderer!!.vertex(x, y, z)
    }

    /** @see .line
     */
    fun line(v0: com.badlogic.gdx.math.Vector3?, v1: com.badlogic.gdx.math.Vector3?) {
        line(v0.x, v0.y, v0.z, v1.x, v1.y, v1.z, color, color)
    }

    /** @see .line
     */
    fun line(x: Float, y: Float, x2: Float, y2: Float) {
        line(x, y, 0.0f, x2, y2, 0.0f, color, color)
    }

    /** @see .line
     */
    fun line(v0: com.badlogic.gdx.math.Vector2?, v1: com.badlogic.gdx.math.Vector2?) {
        line(v0.x, v0.y, 0.0f, v1.x, v1.y, 0.0f, color, color)
    }

    /** @see .line
     */
    fun line(x: Float, y: Float, x2: Float, y2: Float, c1: com.badlogic.gdx.graphics.Color?, c2: com.badlogic.gdx.graphics.Color?) {
        line(x, y, 0.0f, x2, y2, 0.0f, c1, c2)
    }
    /** Draws a line using [ShapeType.Line] or [ShapeType.Filled]. The line is drawn with two colors interpolated
     * between the start and end points.  */
    /** Draws a line using [ShapeType.Line] or [ShapeType.Filled].  */
    @JvmOverloads
    fun line(x: Float, y: Float, z: Float, x2: Float, y2: Float, z2: Float, c1: com.badlogic.gdx.graphics.Color? = color, c2: com.badlogic.gdx.graphics.Color? = color) {
        if (shapeType == ShapeType.Filled) {
            rectLine(x, y, x2, y2, defaultRectLineWidth, c1, c2)
            return
        }
        check(ShapeType.Line, null, 2)
        renderer!!.color(c1.r, c1.g, c1.b, c1.a)
        renderer!!.vertex(x, y, z)
        renderer!!.color(c2.r, c2.g, c2.b, c2.a)
        renderer!!.vertex(x2, y2, z2)
    }

    /** Draws a curve using [ShapeType.Line].  */
    fun curve(x1: Float, y1: Float, cx1: Float, cy1: Float, cx2: Float, cy2: Float, x2: Float, y2: Float, segments: Int) {
        var segments = segments
        check(ShapeType.Line, null, segments * 2 + 2)
        val colorBits: Float = color.toFloatBits()
        // Algorithm from: http://www.antigrain.com/research/bezier_interpolation/index.html#PAGE_BEZIER_INTERPOLATION
        val subdiv_step = 1f / segments
        val subdiv_step2 = subdiv_step * subdiv_step
        val subdiv_step3 = subdiv_step * subdiv_step * subdiv_step
        val pre1 = 3 * subdiv_step
        val pre2 = 3 * subdiv_step2
        val pre4 = 6 * subdiv_step2
        val pre5 = 6 * subdiv_step3
        val tmp1x = x1 - cx1 * 2 + cx2
        val tmp1y = y1 - cy1 * 2 + cy2
        val tmp2x = (cx1 - cx2) * 3 - x1 + x2
        val tmp2y = (cy1 - cy2) * 3 - y1 + y2
        var fx = x1
        var fy = y1
        var dfx = (cx1 - x1) * pre1 + tmp1x * pre2 + tmp2x * subdiv_step3
        var dfy = (cy1 - y1) * pre1 + tmp1y * pre2 + tmp2y * subdiv_step3
        var ddfx = tmp1x * pre4 + tmp2x * pre5
        var ddfy = tmp1y * pre4 + tmp2y * pre5
        val dddfx = tmp2x * pre5
        val dddfy = tmp2y * pre5
        while (segments-- > 0) {
            renderer.color(colorBits)
            renderer!!.vertex(fx, fy, 0f)
            fx += dfx
            fy += dfy
            dfx += ddfx
            dfy += ddfy
            ddfx += dddfx
            ddfy += dddfy
            renderer.color(colorBits)
            renderer!!.vertex(fx, fy, 0f)
        }
        renderer.color(colorBits)
        renderer!!.vertex(fx, fy, 0f)
        renderer.color(colorBits)
        renderer!!.vertex(x2, y2, 0f)
    }

    /** Draws a triangle in x/y plane using [ShapeType.Line] or [ShapeType.Filled].  */
    fun triangle(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float) {
        check(ShapeType.Line, ShapeType.Filled, 6)
        val colorBits: Float = color.toFloatBits()
        if (shapeType == ShapeType.Line) {
            renderer.color(colorBits)
            renderer!!.vertex(x1, y1, 0f)
            renderer.color(colorBits)
            renderer!!.vertex(x2, y2, 0f)
            renderer.color(colorBits)
            renderer!!.vertex(x2, y2, 0f)
            renderer.color(colorBits)
            renderer!!.vertex(x3, y3, 0f)
            renderer.color(colorBits)
            renderer!!.vertex(x3, y3, 0f)
            renderer.color(colorBits)
            renderer!!.vertex(x1, y1, 0f)
        } else {
            renderer.color(colorBits)
            renderer!!.vertex(x1, y1, 0f)
            renderer.color(colorBits)
            renderer!!.vertex(x2, y2, 0f)
            renderer.color(colorBits)
            renderer!!.vertex(x3, y3, 0f)
        }
    }

    /** Draws a triangle in x/y plane with colored corners using [ShapeType.Line] or [ShapeType.Filled].  */
    fun triangle(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float, col1: com.badlogic.gdx.graphics.Color?, col2: com.badlogic.gdx.graphics.Color?, col3: com.badlogic.gdx.graphics.Color?) {
        check(ShapeType.Line, ShapeType.Filled, 6)
        if (shapeType == ShapeType.Line) {
            renderer!!.color(col1.r, col1.g, col1.b, col1.a)
            renderer!!.vertex(x1, y1, 0f)
            renderer!!.color(col2.r, col2.g, col2.b, col2.a)
            renderer!!.vertex(x2, y2, 0f)
            renderer!!.color(col2.r, col2.g, col2.b, col2.a)
            renderer!!.vertex(x2, y2, 0f)
            renderer!!.color(col3.r, col3.g, col3.b, col3.a)
            renderer!!.vertex(x3, y3, 0f)
            renderer!!.color(col3.r, col3.g, col3.b, col3.a)
            renderer!!.vertex(x3, y3, 0f)
            renderer!!.color(col1.r, col1.g, col1.b, col1.a)
            renderer!!.vertex(x1, y1, 0f)
        } else {
            renderer!!.color(col1.r, col1.g, col1.b, col1.a)
            renderer!!.vertex(x1, y1, 0f)
            renderer!!.color(col2.r, col2.g, col2.b, col2.a)
            renderer!!.vertex(x2, y2, 0f)
            renderer!!.color(col3.r, col3.g, col3.b, col3.a)
            renderer!!.vertex(x3, y3, 0f)
        }
    }

    /** Draws a rectangle in the x/y plane using [ShapeType.Line] or [ShapeType.Filled].  */
    fun rect(x: Float, y: Float, width: Float, height: Float) {
        check(ShapeType.Line, ShapeType.Filled, 8)
        val colorBits: Float = color.toFloatBits()
        if (shapeType == ShapeType.Line) {
            renderer.color(colorBits)
            renderer!!.vertex(x, y, 0f)
            renderer.color(colorBits)
            renderer!!.vertex(x + width, y, 0f)
            renderer.color(colorBits)
            renderer!!.vertex(x + width, y, 0f)
            renderer.color(colorBits)
            renderer!!.vertex(x + width, y + height, 0f)
            renderer.color(colorBits)
            renderer!!.vertex(x + width, y + height, 0f)
            renderer.color(colorBits)
            renderer!!.vertex(x, y + height, 0f)
            renderer.color(colorBits)
            renderer!!.vertex(x, y + height, 0f)
            renderer.color(colorBits)
            renderer!!.vertex(x, y, 0f)
        } else {
            renderer.color(colorBits)
            renderer!!.vertex(x, y, 0f)
            renderer.color(colorBits)
            renderer!!.vertex(x + width, y, 0f)
            renderer.color(colorBits)
            renderer!!.vertex(x + width, y + height, 0f)
            renderer.color(colorBits)
            renderer!!.vertex(x + width, y + height, 0f)
            renderer.color(colorBits)
            renderer!!.vertex(x, y + height, 0f)
            renderer.color(colorBits)
            renderer!!.vertex(x, y, 0f)
        }
    }

    /** Draws a rectangle in the x/y plane using [ShapeType.Line] or [ShapeType.Filled]. The x and y specify the lower
     * left corner.
     * @param col1 The color at (x, y).
     * @param col2 The color at (x + width, y).
     * @param col3 The color at (x + width, y + height).
     * @param col4 The color at (x, y + height).
     */
    fun rect(x: Float, y: Float, width: Float, height: Float, col1: com.badlogic.gdx.graphics.Color?, col2: com.badlogic.gdx.graphics.Color?, col3: com.badlogic.gdx.graphics.Color?, col4: com.badlogic.gdx.graphics.Color?) {
        check(ShapeType.Line, ShapeType.Filled, 8)
        if (shapeType == ShapeType.Line) {
            renderer!!.color(col1.r, col1.g, col1.b, col1.a)
            renderer!!.vertex(x, y, 0f)
            renderer!!.color(col2.r, col2.g, col2.b, col2.a)
            renderer!!.vertex(x + width, y, 0f)
            renderer!!.color(col2.r, col2.g, col2.b, col2.a)
            renderer!!.vertex(x + width, y, 0f)
            renderer!!.color(col3.r, col3.g, col3.b, col3.a)
            renderer!!.vertex(x + width, y + height, 0f)
            renderer!!.color(col3.r, col3.g, col3.b, col3.a)
            renderer!!.vertex(x + width, y + height, 0f)
            renderer!!.color(col4.r, col4.g, col4.b, col4.a)
            renderer!!.vertex(x, y + height, 0f)
            renderer!!.color(col4.r, col4.g, col4.b, col4.a)
            renderer!!.vertex(x, y + height, 0f)
            renderer!!.color(col1.r, col1.g, col1.b, col1.a)
            renderer!!.vertex(x, y, 0f)
        } else {
            renderer!!.color(col1.r, col1.g, col1.b, col1.a)
            renderer!!.vertex(x, y, 0f)
            renderer!!.color(col2.r, col2.g, col2.b, col2.a)
            renderer!!.vertex(x + width, y, 0f)
            renderer!!.color(col3.r, col3.g, col3.b, col3.a)
            renderer!!.vertex(x + width, y + height, 0f)
            renderer!!.color(col3.r, col3.g, col3.b, col3.a)
            renderer!!.vertex(x + width, y + height, 0f)
            renderer!!.color(col4.r, col4.g, col4.b, col4.a)
            renderer!!.vertex(x, y + height, 0f)
            renderer!!.color(col1.r, col1.g, col1.b, col1.a)
            renderer!!.vertex(x, y, 0f)
        }
    }
    /** Draws a rectangle in the x/y plane using [ShapeType.Line] or [ShapeType.Filled]. The x and y specify the lower
     * left corner. The originX and originY specify the point about which to rotate the rectangle.
     * @param col1 The color at (x, y)
     * @param col2 The color at (x + width, y)
     * @param col3 The color at (x + width, y + height)
     * @param col4 The color at (x, y + height)
     */
    /** Draws a rectangle in the x/y plane using [ShapeType.Line] or [ShapeType.Filled]. The x and y specify the lower
     * left corner. The originX and originY specify the point about which to rotate the rectangle.  */
    @JvmOverloads
    fun rect(x: Float, y: Float, originX: Float, originY: Float, width: Float, height: Float, scaleX: Float, scaleY: Float,
             degrees: Float, col1: com.badlogic.gdx.graphics.Color? = color, col2: com.badlogic.gdx.graphics.Color? = color, col3: com.badlogic.gdx.graphics.Color? = color, col4: com.badlogic.gdx.graphics.Color? = color) {
        check(ShapeType.Line, ShapeType.Filled, 8)
        val cos: Float = com.badlogic.gdx.math.MathUtils.cosDeg(degrees)
        val sin: Float = com.badlogic.gdx.math.MathUtils.sinDeg(degrees)
        var fx = -originX
        var fy = -originY
        var fx2 = width - originX
        var fy2 = height - originY
        if (scaleX != 1f || scaleY != 1f) {
            fx *= scaleX
            fy *= scaleY
            fx2 *= scaleX
            fy2 *= scaleY
        }
        val worldOriginX = x + originX
        val worldOriginY = y + originY
        val x1 = cos * fx - sin * fy + worldOriginX
        val y1 = sin * fx + cos * fy + worldOriginY
        val x2 = cos * fx2 - sin * fy + worldOriginX
        val y2 = sin * fx2 + cos * fy + worldOriginY
        val x3 = cos * fx2 - sin * fy2 + worldOriginX
        val y3 = sin * fx2 + cos * fy2 + worldOriginY
        val x4 = x1 + (x3 - x2)
        val y4 = y3 - (y2 - y1)
        if (shapeType == ShapeType.Line) {
            renderer!!.color(col1.r, col1.g, col1.b, col1.a)
            renderer!!.vertex(x1, y1, 0f)
            renderer!!.color(col2.r, col2.g, col2.b, col2.a)
            renderer!!.vertex(x2, y2, 0f)
            renderer!!.color(col2.r, col2.g, col2.b, col2.a)
            renderer!!.vertex(x2, y2, 0f)
            renderer!!.color(col3.r, col3.g, col3.b, col3.a)
            renderer!!.vertex(x3, y3, 0f)
            renderer!!.color(col3.r, col3.g, col3.b, col3.a)
            renderer!!.vertex(x3, y3, 0f)
            renderer!!.color(col4.r, col4.g, col4.b, col4.a)
            renderer!!.vertex(x4, y4, 0f)
            renderer!!.color(col4.r, col4.g, col4.b, col4.a)
            renderer!!.vertex(x4, y4, 0f)
            renderer!!.color(col1.r, col1.g, col1.b, col1.a)
            renderer!!.vertex(x1, y1, 0f)
        } else {
            renderer!!.color(col1.r, col1.g, col1.b, col1.a)
            renderer!!.vertex(x1, y1, 0f)
            renderer!!.color(col2.r, col2.g, col2.b, col2.a)
            renderer!!.vertex(x2, y2, 0f)
            renderer!!.color(col3.r, col3.g, col3.b, col3.a)
            renderer!!.vertex(x3, y3, 0f)
            renderer!!.color(col3.r, col3.g, col3.b, col3.a)
            renderer!!.vertex(x3, y3, 0f)
            renderer!!.color(col4.r, col4.g, col4.b, col4.a)
            renderer!!.vertex(x4, y4, 0f)
            renderer!!.color(col1.r, col1.g, col1.b, col1.a)
            renderer!!.vertex(x1, y1, 0f)
        }
    }

    /** Draws a line using a rotated rectangle, where with one edge is centered at x1, y1 and the opposite edge centered at x2, y2.  */
    fun rectLine(x1: Float, y1: Float, x2: Float, y2: Float, width: Float) {
        var width = width
        check(ShapeType.Line, ShapeType.Filled, 8)
        val colorBits: Float = color.toFloatBits()
        val t: com.badlogic.gdx.math.Vector2 = tmp.set(y2 - y1, x1 - x2).nor()
        width *= 0.5f
        val tx: Float = t.x * width
        val ty: Float = t.y * width
        if (shapeType == ShapeType.Line) {
            renderer.color(colorBits)
            renderer!!.vertex(x1 + tx, y1 + ty, 0f)
            renderer.color(colorBits)
            renderer!!.vertex(x1 - tx, y1 - ty, 0f)
            renderer.color(colorBits)
            renderer!!.vertex(x2 + tx, y2 + ty, 0f)
            renderer.color(colorBits)
            renderer!!.vertex(x2 - tx, y2 - ty, 0f)
            renderer.color(colorBits)
            renderer!!.vertex(x2 + tx, y2 + ty, 0f)
            renderer.color(colorBits)
            renderer!!.vertex(x1 + tx, y1 + ty, 0f)
            renderer.color(colorBits)
            renderer!!.vertex(x2 - tx, y2 - ty, 0f)
            renderer.color(colorBits)
            renderer!!.vertex(x1 - tx, y1 - ty, 0f)
        } else {
            renderer.color(colorBits)
            renderer!!.vertex(x1 + tx, y1 + ty, 0f)
            renderer.color(colorBits)
            renderer!!.vertex(x1 - tx, y1 - ty, 0f)
            renderer.color(colorBits)
            renderer!!.vertex(x2 + tx, y2 + ty, 0f)
            renderer.color(colorBits)
            renderer!!.vertex(x2 - tx, y2 - ty, 0f)
            renderer.color(colorBits)
            renderer!!.vertex(x2 + tx, y2 + ty, 0f)
            renderer.color(colorBits)
            renderer!!.vertex(x1 - tx, y1 - ty, 0f)
        }
    }

    /** Draws a line using a rotated rectangle, where with one edge is centered at x1, y1 and the opposite edge centered at x2, y2.  */
    fun rectLine(x1: Float, y1: Float, x2: Float, y2: Float, width: Float, c1: com.badlogic.gdx.graphics.Color?, c2: com.badlogic.gdx.graphics.Color?) {
        var width = width
        check(ShapeType.Line, ShapeType.Filled, 8)
        val col1Bits: Float = c1.toFloatBits()
        val col2Bits: Float = c2.toFloatBits()
        val t: com.badlogic.gdx.math.Vector2 = tmp.set(y2 - y1, x1 - x2).nor()
        width *= 0.5f
        val tx: Float = t.x * width
        val ty: Float = t.y * width
        if (shapeType == ShapeType.Line) {
            renderer.color(col1Bits)
            renderer!!.vertex(x1 + tx, y1 + ty, 0f)
            renderer.color(col1Bits)
            renderer!!.vertex(x1 - tx, y1 - ty, 0f)
            renderer.color(col2Bits)
            renderer!!.vertex(x2 + tx, y2 + ty, 0f)
            renderer.color(col2Bits)
            renderer!!.vertex(x2 - tx, y2 - ty, 0f)
            renderer.color(col2Bits)
            renderer!!.vertex(x2 + tx, y2 + ty, 0f)
            renderer.color(col1Bits)
            renderer!!.vertex(x1 + tx, y1 + ty, 0f)
            renderer.color(col2Bits)
            renderer!!.vertex(x2 - tx, y2 - ty, 0f)
            renderer.color(col1Bits)
            renderer!!.vertex(x1 - tx, y1 - ty, 0f)
        } else {
            renderer.color(col1Bits)
            renderer!!.vertex(x1 + tx, y1 + ty, 0f)
            renderer.color(col1Bits)
            renderer!!.vertex(x1 - tx, y1 - ty, 0f)
            renderer.color(col2Bits)
            renderer!!.vertex(x2 + tx, y2 + ty, 0f)
            renderer.color(col2Bits)
            renderer!!.vertex(x2 - tx, y2 - ty, 0f)
            renderer.color(col2Bits)
            renderer!!.vertex(x2 + tx, y2 + ty, 0f)
            renderer.color(col1Bits)
            renderer!!.vertex(x1 - tx, y1 - ty, 0f)
        }
    }

    /** @see .rectLine
     */
    fun rectLine(p1: com.badlogic.gdx.math.Vector2?, p2: com.badlogic.gdx.math.Vector2?, width: Float) {
        rectLine(p1.x, p1.y, p2.x, p2.y, width)
    }

    /** Draws a cube using [ShapeType.Line] or [ShapeType.Filled]. The x, y and z specify the bottom, left, front corner
     * of the rectangle.  */
    fun box(x: Float, y: Float, z: Float, width: Float, height: Float, depth: Float) {
        var depth = depth
        depth = -depth
        val colorBits: Float = color.toFloatBits()
        if (shapeType == ShapeType.Line) {
            check(ShapeType.Line, ShapeType.Filled, 24)
            renderer.color(colorBits)
            renderer!!.vertex(x, y, z)
            renderer.color(colorBits)
            renderer!!.vertex(x + width, y, z)
            renderer.color(colorBits)
            renderer!!.vertex(x + width, y, z)
            renderer.color(colorBits)
            renderer!!.vertex(x + width, y, z + depth)
            renderer.color(colorBits)
            renderer!!.vertex(x + width, y, z + depth)
            renderer.color(colorBits)
            renderer!!.vertex(x, y, z + depth)
            renderer.color(colorBits)
            renderer!!.vertex(x, y, z + depth)
            renderer.color(colorBits)
            renderer!!.vertex(x, y, z)
            renderer.color(colorBits)
            renderer!!.vertex(x, y, z)
            renderer.color(colorBits)
            renderer!!.vertex(x, y + height, z)
            renderer.color(colorBits)
            renderer!!.vertex(x, y + height, z)
            renderer.color(colorBits)
            renderer!!.vertex(x + width, y + height, z)
            renderer.color(colorBits)
            renderer!!.vertex(x + width, y + height, z)
            renderer.color(colorBits)
            renderer!!.vertex(x + width, y + height, z + depth)
            renderer.color(colorBits)
            renderer!!.vertex(x + width, y + height, z + depth)
            renderer.color(colorBits)
            renderer!!.vertex(x, y + height, z + depth)
            renderer.color(colorBits)
            renderer!!.vertex(x, y + height, z + depth)
            renderer.color(colorBits)
            renderer!!.vertex(x, y + height, z)
            renderer.color(colorBits)
            renderer!!.vertex(x + width, y, z)
            renderer.color(colorBits)
            renderer!!.vertex(x + width, y + height, z)
            renderer.color(colorBits)
            renderer!!.vertex(x + width, y, z + depth)
            renderer.color(colorBits)
            renderer!!.vertex(x + width, y + height, z + depth)
            renderer.color(colorBits)
            renderer!!.vertex(x, y, z + depth)
            renderer.color(colorBits)
            renderer!!.vertex(x, y + height, z + depth)
        } else {
            check(ShapeType.Line, ShapeType.Filled, 36)
            // Front
            renderer.color(colorBits)
            renderer!!.vertex(x, y, z)
            renderer.color(colorBits)
            renderer!!.vertex(x + width, y, z)
            renderer.color(colorBits)
            renderer!!.vertex(x + width, y + height, z)
            renderer.color(colorBits)
            renderer!!.vertex(x, y, z)
            renderer.color(colorBits)
            renderer!!.vertex(x + width, y + height, z)
            renderer.color(colorBits)
            renderer!!.vertex(x, y + height, z)
            // Back
            renderer.color(colorBits)
            renderer!!.vertex(x + width, y, z + depth)
            renderer.color(colorBits)
            renderer!!.vertex(x, y, z + depth)
            renderer.color(colorBits)
            renderer!!.vertex(x + width, y + height, z + depth)
            renderer.color(colorBits)
            renderer!!.vertex(x + width, y + height, z + depth)
            renderer.color(colorBits)
            renderer!!.vertex(x, y, z + depth)
            renderer.color(colorBits)
            renderer!!.vertex(x, y + height, z + depth)
            // Left
            renderer.color(colorBits)
            renderer!!.vertex(x, y, z + depth)
            renderer.color(colorBits)
            renderer!!.vertex(x, y, z)
            renderer.color(colorBits)
            renderer!!.vertex(x, y + height, z)
            renderer.color(colorBits)
            renderer!!.vertex(x, y, z + depth)
            renderer.color(colorBits)
            renderer!!.vertex(x, y + height, z)
            renderer.color(colorBits)
            renderer!!.vertex(x, y + height, z + depth)
            // Right
            renderer.color(colorBits)
            renderer!!.vertex(x + width, y, z)
            renderer.color(colorBits)
            renderer!!.vertex(x + width, y, z + depth)
            renderer.color(colorBits)
            renderer!!.vertex(x + width, y + height, z + depth)
            renderer.color(colorBits)
            renderer!!.vertex(x + width, y, z)
            renderer.color(colorBits)
            renderer!!.vertex(x + width, y + height, z + depth)
            renderer.color(colorBits)
            renderer!!.vertex(x + width, y + height, z)
            // Top
            renderer.color(colorBits)
            renderer!!.vertex(x, y + height, z)
            renderer.color(colorBits)
            renderer!!.vertex(x + width, y + height, z)
            renderer.color(colorBits)
            renderer!!.vertex(x + width, y + height, z + depth)
            renderer.color(colorBits)
            renderer!!.vertex(x, y + height, z)
            renderer.color(colorBits)
            renderer!!.vertex(x + width, y + height, z + depth)
            renderer.color(colorBits)
            renderer!!.vertex(x, y + height, z + depth)
            // Bottom
            renderer.color(colorBits)
            renderer!!.vertex(x, y, z + depth)
            renderer.color(colorBits)
            renderer!!.vertex(x + width, y, z + depth)
            renderer.color(colorBits)
            renderer!!.vertex(x + width, y, z)
            renderer.color(colorBits)
            renderer!!.vertex(x, y, z + depth)
            renderer.color(colorBits)
            renderer!!.vertex(x + width, y, z)
            renderer.color(colorBits)
            renderer!!.vertex(x, y, z)
        }
    }

    /** Draws two crossed lines using [ShapeType.Line] or [ShapeType.Filled].  */
    fun x(x: Float, y: Float, size: Float) {
        line(x - size, y - size, x + size, y + size)
        line(x - size, y + size, x + size, y - size)
    }

    /** @see .x
     */
    fun x(p: com.badlogic.gdx.math.Vector2?, size: Float) {
        x(p.x, p.y, size)
    }
    /** Draws an arc using [ShapeType.Line] or [ShapeType.Filled].  */
    /** Calls [.arc] by estimating the number of segments needed for a smooth arc.  */
    @JvmOverloads
    fun arc(x: Float, y: Float, radius: Float, start: Float, degrees: Float, segments: Int = max(1, (6 * java.lang.Math.cbrt(radius.toDouble()) as Float * (degrees / 360.0f)).toInt())) {
        if (segments <= 0) throw IllegalArgumentException("segments must be > 0.")
        val colorBits: Float = color.toFloatBits()
        val theta: Float = 2 * com.badlogic.gdx.math.MathUtils.PI * (degrees / 360.0f) / segments
        val cos: Float = com.badlogic.gdx.math.MathUtils.cos(theta)
        val sin: Float = com.badlogic.gdx.math.MathUtils.sin(theta)
        var cx: Float = radius * com.badlogic.gdx.math.MathUtils.cos(start * com.badlogic.gdx.math.MathUtils.degreesToRadians)
        var cy: Float = radius * com.badlogic.gdx.math.MathUtils.sin(start * com.badlogic.gdx.math.MathUtils.degreesToRadians)
        if (shapeType == ShapeType.Line) {
            check(ShapeType.Line, ShapeType.Filled, segments * 2 + 2)
            renderer.color(colorBits)
            renderer!!.vertex(x, y, 0f)
            renderer.color(colorBits)
            renderer!!.vertex(x + cx, y + cy, 0f)
            for (i in 0 until segments) {
                renderer.color(colorBits)
                renderer!!.vertex(x + cx, y + cy, 0f)
                val temp = cx
                cx = cos * cx - sin * cy
                cy = sin * temp + cos * cy
                renderer.color(colorBits)
                renderer!!.vertex(x + cx, y + cy, 0f)
            }
            renderer.color(colorBits)
            renderer!!.vertex(x + cx, y + cy, 0f)
        } else {
            check(ShapeType.Line, ShapeType.Filled, segments * 3 + 3)
            for (i in 0 until segments) {
                renderer.color(colorBits)
                renderer!!.vertex(x, y, 0f)
                renderer.color(colorBits)
                renderer!!.vertex(x + cx, y + cy, 0f)
                val temp = cx
                cx = cos * cx - sin * cy
                cy = sin * temp + cos * cy
                renderer.color(colorBits)
                renderer!!.vertex(x + cx, y + cy, 0f)
            }
            renderer.color(colorBits)
            renderer!!.vertex(x, y, 0f)
            renderer.color(colorBits)
            renderer!!.vertex(x + cx, y + cy, 0f)
        }
        val temp = cx
        cx = 0f
        cy = 0f
        renderer.color(colorBits)
        renderer!!.vertex(x + cx, y + cy, 0f)
    }
    /** Draws a circle using [ShapeType.Line] or [ShapeType.Filled].  */
    /** Calls [.circle] by estimating the number of segments needed for a smooth circle.  */
    @JvmOverloads
    fun circle(x: Float, y: Float, radius: Float, segments: Int = max(1, (6 * java.lang.Math.cbrt(radius.toDouble()) as Float).toInt())) {
        var segments = segments
        if (segments <= 0) throw IllegalArgumentException("segments must be > 0.")
        val colorBits: Float = color.toFloatBits()
        val angle: Float = 2 * com.badlogic.gdx.math.MathUtils.PI / segments
        val cos: Float = com.badlogic.gdx.math.MathUtils.cos(angle)
        val sin: Float = com.badlogic.gdx.math.MathUtils.sin(angle)
        var cx = radius
        var cy = 0f
        if (shapeType == ShapeType.Line) {
            check(ShapeType.Line, ShapeType.Filled, segments * 2 + 2)
            for (i in 0 until segments) {
                renderer.color(colorBits)
                renderer!!.vertex(x + cx, y + cy, 0f)
                val temp = cx
                cx = cos * cx - sin * cy
                cy = sin * temp + cos * cy
                renderer.color(colorBits)
                renderer!!.vertex(x + cx, y + cy, 0f)
            }
            // Ensure the last segment is identical to the first.
            renderer.color(colorBits)
            renderer!!.vertex(x + cx, y + cy, 0f)
        } else {
            check(ShapeType.Line, ShapeType.Filled, segments * 3 + 3)
            segments--
            for (i in 0 until segments) {
                renderer.color(colorBits)
                renderer!!.vertex(x, y, 0f)
                renderer.color(colorBits)
                renderer!!.vertex(x + cx, y + cy, 0f)
                val temp = cx
                cx = cos * cx - sin * cy
                cy = sin * temp + cos * cy
                renderer.color(colorBits)
                renderer!!.vertex(x + cx, y + cy, 0f)
            }
            // Ensure the last segment is identical to the first.
            renderer.color(colorBits)
            renderer!!.vertex(x, y, 0f)
            renderer.color(colorBits)
            renderer!!.vertex(x + cx, y + cy, 0f)
        }
        val temp = cx
        cx = radius
        cy = 0f
        renderer.color(colorBits)
        renderer!!.vertex(x + cx, y + cy, 0f)
    }
    /** Draws an ellipse using [ShapeType.Line] or [ShapeType.Filled].  */
    /** Calls [.ellipse] by estimating the number of segments needed for a smooth ellipse.  */
    @JvmOverloads
    fun ellipse(x: Float, y: Float, width: Float, height: Float, segments: Int = max(1, (12 * java.lang.Math.cbrt(max(width * 0.5f, height * 0.5f).toDouble()) as Float).toInt())) {
        if (segments <= 0) throw IllegalArgumentException("segments must be > 0.")
        check(ShapeType.Line, ShapeType.Filled, segments * 3)
        val colorBits: Float = color.toFloatBits()
        val angle: Float = 2 * com.badlogic.gdx.math.MathUtils.PI / segments
        val cx = x + width / 2
        val cy = y + height / 2
        if (shapeType == ShapeType.Line) {
            for (i in 0 until segments) {
                renderer.color(colorBits)
                renderer!!.vertex(cx + width * 0.5f * com.badlogic.gdx.math.MathUtils.cos(i * angle), cy + height * 0.5f * com.badlogic.gdx.math.MathUtils.sin(i * angle), 0f)
                renderer.color(colorBits)
                renderer!!.vertex(cx + width * 0.5f * com.badlogic.gdx.math.MathUtils.cos((i + 1) * angle),
                    cy + height * 0.5f * com.badlogic.gdx.math.MathUtils.sin((i + 1) * angle), 0f)
            }
        } else {
            for (i in 0 until segments) {
                renderer.color(colorBits)
                renderer!!.vertex(cx + width * 0.5f * com.badlogic.gdx.math.MathUtils.cos(i * angle), cy + height * 0.5f * com.badlogic.gdx.math.MathUtils.sin(i * angle), 0f)
                renderer.color(colorBits)
                renderer!!.vertex(cx, cy, 0f)
                renderer.color(colorBits)
                renderer!!.vertex(cx + width * 0.5f * com.badlogic.gdx.math.MathUtils.cos((i + 1) * angle),
                    cy + height * 0.5f * com.badlogic.gdx.math.MathUtils.sin((i + 1) * angle), 0f)
            }
        }
    }
    /** Draws an ellipse using [ShapeType.Line] or [ShapeType.Filled].  */
    /** Calls [.ellipse] by estimating the number of segments needed for a smooth ellipse.  */
    @JvmOverloads
    fun ellipse(x: Float, y: Float, width: Float, height: Float, rotation: Float, segments: Int = max(1, (12 * java.lang.Math.cbrt(max(width * 0.5f, height * 0.5f).toDouble()) as Float).toInt())) {
        var rotation = rotation
        if (segments <= 0) throw IllegalArgumentException("segments must be > 0.")
        check(ShapeType.Line, ShapeType.Filled, segments * 3)
        val colorBits: Float = color.toFloatBits()
        val angle: Float = 2 * com.badlogic.gdx.math.MathUtils.PI / segments
        rotation = com.badlogic.gdx.math.MathUtils.PI * rotation / 180f
        val sin: Float = com.badlogic.gdx.math.MathUtils.sin(rotation)
        val cos: Float = com.badlogic.gdx.math.MathUtils.cos(rotation)
        val cx = x + width / 2
        val cy = y + height / 2
        var x1 = width * 0.5f
        var y1 = 0f
        if (shapeType == ShapeType.Line) {
            for (i in 0 until segments) {
                renderer.color(colorBits)
                renderer!!.vertex(cx + cos * x1 - sin * y1, cy + sin * x1 + cos * y1, 0f)
                x1 = width * 0.5f * com.badlogic.gdx.math.MathUtils.cos((i + 1) * angle)
                y1 = height * 0.5f * com.badlogic.gdx.math.MathUtils.sin((i + 1) * angle)
                renderer.color(colorBits)
                renderer!!.vertex(cx + cos * x1 - sin * y1, cy + sin * x1 + cos * y1, 0f)
            }
        } else {
            for (i in 0 until segments) {
                renderer.color(colorBits)
                renderer!!.vertex(cx + cos * x1 - sin * y1, cy + sin * x1 + cos * y1, 0f)
                renderer.color(colorBits)
                renderer!!.vertex(cx, cy, 0f)
                x1 = width * 0.5f * com.badlogic.gdx.math.MathUtils.cos((i + 1) * angle)
                y1 = height * 0.5f * com.badlogic.gdx.math.MathUtils.sin((i + 1) * angle)
                renderer.color(colorBits)
                renderer!!.vertex(cx + cos * x1 - sin * y1, cy + sin * x1 + cos * y1, 0f)
            }
        }
    }
    /** Draws a cone using [ShapeType.Line] or [ShapeType.Filled].  */
    /** Calls [.cone] by estimating the number of segments needed for a smooth
     * circular base.  */
    @JvmOverloads
    fun cone(x: Float, y: Float, z: Float, radius: Float, height: Float, segments: Int = max(1, (4 * java.lang.Math.sqrt(radius.toDouble()) as Float).toInt())) {
        var segments = segments
        if (segments <= 0) throw IllegalArgumentException("segments must be > 0.")
        check(ShapeType.Line, ShapeType.Filled, segments * 4 + 2)
        val colorBits: Float = color.toFloatBits()
        val angle: Float = 2 * com.badlogic.gdx.math.MathUtils.PI / segments
        val cos: Float = com.badlogic.gdx.math.MathUtils.cos(angle)
        val sin: Float = com.badlogic.gdx.math.MathUtils.sin(angle)
        var cx = radius
        var cy = 0f
        if (shapeType == ShapeType.Line) {
            for (i in 0 until segments) {
                renderer.color(colorBits)
                renderer!!.vertex(x + cx, y + cy, z)
                renderer.color(colorBits)
                renderer!!.vertex(x, y, z + height)
                renderer.color(colorBits)
                renderer!!.vertex(x + cx, y + cy, z)
                val temp = cx
                cx = cos * cx - sin * cy
                cy = sin * temp + cos * cy
                renderer.color(colorBits)
                renderer!!.vertex(x + cx, y + cy, z)
            }
            // Ensure the last segment is identical to the first.
            renderer.color(colorBits)
            renderer!!.vertex(x + cx, y + cy, z)
        } else {
            segments--
            for (i in 0 until segments) {
                renderer.color(colorBits)
                renderer!!.vertex(x, y, z)
                renderer.color(colorBits)
                renderer!!.vertex(x + cx, y + cy, z)
                val temp = cx
                val temp2 = cy
                cx = cos * cx - sin * cy
                cy = sin * temp + cos * cy
                renderer.color(colorBits)
                renderer!!.vertex(x + cx, y + cy, z)
                renderer.color(colorBits)
                renderer!!.vertex(x + temp, y + temp2, z)
                renderer.color(colorBits)
                renderer!!.vertex(x + cx, y + cy, z)
                renderer.color(colorBits)
                renderer!!.vertex(x, y, z + height)
            }
            // Ensure the last segment is identical to the first.
            renderer.color(colorBits)
            renderer!!.vertex(x, y, z)
            renderer.color(colorBits)
            renderer!!.vertex(x + cx, y + cy, z)
        }
        val temp = cx
        val temp2 = cy
        cx = radius
        cy = 0f
        renderer.color(colorBits)
        renderer!!.vertex(x + cx, y + cy, z)
        if (shapeType != ShapeType.Line) {
            renderer.color(colorBits)
            renderer!!.vertex(x + temp, y + temp2, z)
            renderer.color(colorBits)
            renderer!!.vertex(x + cx, y + cy, z)
            renderer.color(colorBits)
            renderer!!.vertex(x, y, z + height)
        }
    }
    /** Draws a polygon in the x/y plane using [ShapeType.Line]. The vertices must contain at least 3 points (6 floats x,y).  */
    /** @see .polygon
     */
    @JvmOverloads
    fun polygon(vertices: FloatArray?, offset: Int = 0, count: Int = vertices!!.size) {
        if (count < 6) throw IllegalArgumentException("Polygons must contain at least 3 points.")
        if (count % 2 != 0) throw IllegalArgumentException("Polygons must have an even number of vertices.")
        check(ShapeType.Line, null, count)
        val colorBits: Float = color.toFloatBits()
        val firstX = vertices!![0]
        val firstY = vertices[1]
        var i = offset
        val n = offset + count
        while (i < n) {
            val x1 = vertices[i]
            val y1 = vertices[i + 1]
            var x2: Float
            var y2: Float
            if (i + 2 >= count) {
                x2 = firstX
                y2 = firstY
            } else {
                x2 = vertices[i + 2]
                y2 = vertices[i + 3]
            }
            renderer.color(colorBits)
            renderer!!.vertex(x1, y1, 0f)
            renderer.color(colorBits)
            renderer!!.vertex(x2, y2, 0f)
            i += 2
        }
    }
    /** Draws a polyline in the x/y plane using [ShapeType.Line]. The vertices must contain at least 2 points (4 floats x,y).  */
    /** @see .polyline
     */
    @JvmOverloads
    fun polyline(vertices: FloatArray?, offset: Int = 0, count: Int = vertices!!.size) {
        if (count < 4) throw IllegalArgumentException("Polylines must contain at least 2 points.")
        if (count % 2 != 0) throw IllegalArgumentException("Polylines must have an even number of vertices.")
        check(ShapeType.Line, null, count)
        val colorBits: Float = color.toFloatBits()
        var i = offset
        val n = offset + count - 2
        while (i < n) {
            val x1 = vertices!![i]
            val y1 = vertices[i + 1]
            var x2: Float
            var y2: Float
            x2 = vertices[i + 2]
            y2 = vertices[i + 3]
            renderer.color(colorBits)
            renderer!!.vertex(x1, y1, 0f)
            renderer.color(colorBits)
            renderer!!.vertex(x2, y2, 0f)
            i += 2
        }
    }

    /** @param other May be null.
     */
    private fun check(preferred: ShapeType?, other: ShapeType?, newVertices: Int) {
        checkNotNull(shapeType) { "begin must be called first." }
        if (shapeType != preferred && shapeType != other) { // Shape type is not valid.
            if (!autoShapeType) {
                checkNotNull(other) { "Must call begin(ShapeType.$preferred)." }
                throw IllegalStateException("Must call begin(ShapeType.$preferred) or begin(ShapeType.$other).")
            }
            end()
            begin(preferred)
        } else if (matrixDirty) { // Matrix has been changed.
            val type = shapeType
            end()
            begin(type)
        } else if (renderer!!.getMaxVertices() - renderer!!.getNumVertices() < newVertices) { // Not enough space.
            val type = shapeType
            end()
            begin(type)
        }
    }

    /** Finishes the batch of shapes and ensures they get rendered.  */
    fun end() {
        renderer!!.end()
        shapeType = null
    }

    fun flush() {
        val type = shapeType ?: return
        end()
        begin(type)
    }

    /** Returns the current shape type.  */
    fun getCurrentType(): ShapeType? {
        return shapeType
    }

    fun getRenderer(): com.badlogic.gdx.graphics.glutils.ImmediateModeRenderer? {
        return renderer
    }

    /** @return true if currently between begin and end.
     */
    fun isDrawing(): Boolean {
        return shapeType != null
    }

    override fun dispose() {
        renderer!!.dispose()
    }

    init {
        if (defaultShader == null) {
            renderer = com.badlogic.gdx.graphics.glutils.ImmediateModeRenderer20(maxVertices, false, true, 0)
        } else {
            renderer = com.badlogic.gdx.graphics.glutils.ImmediateModeRenderer20(maxVertices, false, true, 0, defaultShader)
        }
        projectionMatrix.setToOrtho2D(0f, 0f, com.badlogic.gdx.Gdx.graphics.getWidth().toFloat(), com.badlogic.gdx.Gdx.graphics.getHeight().toFloat())
        matrixDirty = true
    }
}
