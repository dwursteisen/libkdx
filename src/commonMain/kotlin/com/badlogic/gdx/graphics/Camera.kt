package com.badlogic.gdx.graphics

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Graphics
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.math.Vector3

/**
 * Base class for [OrthographicCamera] and [PerspectiveCamera].
 *
 * @author mzechner
 */
abstract class Camera {

    /**
     * the position of the camera
     */
    val position = Vector3()

    /**
     * the unit length direction vector of the camera
     */
    val direction = Vector3(0, 0, -1)

    /**
     * the unit length up vector of the camera
     */
    val up = Vector3(0, 1, 0)

    /**
     * the projection matrix
     */
    val projection: Matrix4 = Matrix4()

    /**
     * the view matrix
     */
    val view: Matrix4 = Matrix4()

    /**
     * the combined projection and view matrix
     */
    val combined: Matrix4 = Matrix4()

    /**
     * the inverse combined projection and view matrix
     */
    val invProjectionView: Matrix4 = Matrix4()

    /**
     * the near clipping plane distance, has to be positive
     */
    var near = 1f

    /**
     * the far clipping plane distance, has to be positive
     */
    var far = 100f

    /**
     * the viewport width
     */
    var viewportWidth = 0f

    /**
     * the viewport height
     */
    var viewportHeight = 0f

    /**
     * the frustum
     */
    val frustum: Frustum = Frustum()
    private val tmpVec = Vector3()
    private val ray: Ray = Ray(Vector3(), Vector3())

    /**
     * Recalculates the projection and view matrix of this camera and the [Frustum] planes. Use this after you've manipulated
     * any of the attributes of the camera.
     */
    abstract fun update()

    /**
     * Recalculates the projection and view matrix of this camera and the [Frustum] planes if `updateFrustum` is
     * true. Use this after you've manipulated any of the attributes of the camera.
     */
    abstract fun update(updateFrustum: Boolean)

    /**
     * Recalculates the direction of the camera to look at the point (x, y, z). This function assumes the up vector is normalized.
     *
     * @param x the x-coordinate of the point to look at
     * @param y the y-coordinate of the point to look at
     * @param z the z-coordinate of the point to look at
     */
    fun lookAt(x: Float, y: Float, z: Float) {
        tmpVec.set(x, y, z).sub(position).nor()
        if (!tmpVec.isZero()) {
            val dot = tmpVec.dot(up) // up and direction must ALWAYS be orthonormal vectors
            if (java.lang.Math.abs(dot - 1) < 0.000000001f) {
                // Collinear
                up.set(direction).scl(-1f)
            } else if (java.lang.Math.abs(dot + 1) < 0.000000001f) {
                // Collinear opposite
                up.set(direction)
            }
            direction.set(tmpVec)
            normalizeUp()
        }
    }

    /**
     * Recalculates the direction of the camera to look at the point (x, y, z).
     *
     * @param target the point to look at
     */
    fun lookAt(target: Vector3) {
        lookAt(target.x, target.y, target.z)
    }

    /**
     * Normalizes the up vector by first calculating the right vector via a cross product between direction and up, and then
     * recalculating the up vector via a cross product between right and direction.
     */
    fun normalizeUp() {
        tmpVec.set(direction).crs(up).nor()
        up.set(tmpVec).crs(direction).nor()
    }

    /**
     * Rotates the direction and up vector of this camera by the given angle around the given axis. The direction and up vector
     * will not be orthogonalized.
     *
     * @param angle the angle
     * @param axisX the x-component of the axis
     * @param axisY the y-component of the axis
     * @param axisZ the z-component of the axis
     */
    fun rotate(angle: Float, axisX: Float, axisY: Float, axisZ: Float) {
        direction.rotate(angle, axisX, axisY, axisZ)
        up.rotate(angle, axisX, axisY, axisZ)
    }

    /**
     * Rotates the direction and up vector of this camera by the given angle around the given axis. The direction and up vector
     * will not be orthogonalized.
     *
     * @param axis  the axis to rotate around
     * @param angle the angle, in degrees
     */
    fun rotate(axis: Vector3?, angle: Float) {
        direction.rotate(axis, angle)
        up.rotate(axis, angle)
    }

    /**
     * Rotates the direction and up vector of this camera by the given rotation matrix. The direction and up vector will not be
     * orthogonalized.
     *
     * @param transform The rotation matrix
     */
    fun rotate(transform: Matrix4?) {
        direction.rot(transform)
        up.rot(transform)
    }

    /**
     * Rotates the direction and up vector of this camera by the given [Quaternion]. The direction and up vector will not be
     * orthogonalized.
     *
     * @param quat The quaternion
     */
    fun rotate(quat: com.badlogic.gdx.math.Quaternion) {
        quat.transform(direction)
        quat.transform(up)
    }

    /**
     * Rotates the direction and up vector of this camera by the given angle around the given axis, with the axis attached to given
     * point. The direction and up vector will not be orthogonalized.
     *
     * @param point the point to attach the axis to
     * @param axis  the axis to rotate around
     * @param angle the angle, in degrees
     */
    fun rotateAround(point: Vector3?, axis: Vector3?, angle: Float) {
        tmpVec.set(point)
        tmpVec.sub(position)
        translate(tmpVec)
        rotate(axis, angle)
        tmpVec.rotate(axis, angle)
        translate(-tmpVec.x, -tmpVec.y, -tmpVec.z)
    }

    /**
     * Transform the position, direction and up vector by the given matrix
     *
     * @param transform The transform matrix
     */
    fun transform(transform: Matrix4?) {
        position.mul(transform)
        rotate(transform)
    }

    /**
     * Moves the camera by the given amount on each axis.
     *
     * @param x the displacement on the x-axis
     * @param y the displacement on the y-axis
     * @param z the displacement on the z-axis
     */
    fun translate(x: Float, y: Float, z: Float) {
        position.add(x, y, z)
    }

    /**
     * Moves the camera by the given vector.
     *
     * @param vec the displacement vector
     */
    fun translate(vec: Vector3?) {
        position.add(vec)
    }

    /**
     * Function to translate a point given in screen coordinates to world space. It's the same as GLU gluUnProject, but does not
     * rely on OpenGL. The x- and y-coordinate of vec are assumed to be in screen coordinates (origin is the top left corner, y
     * pointing down, x pointing to the right) as reported by the touch methods in [Input]. A z-coordinate of 0 will return a
     * point on the near plane, a z-coordinate of 1 will return a point on the far plane. This method allows you to specify the
     * viewport position and dimensions in the coordinate system expected by [GL20.glViewport], with the
     * origin in the bottom left corner of the screen.
     *
     * @param screenCoords   the point in screen coordinates (origin top left)
     * @param viewportX      the coordinate of the bottom left corner of the viewport in glViewport coordinates.
     * @param viewportY      the coordinate of the bottom left corner of the viewport in glViewport coordinates.
     * @param viewportWidth  the width of the viewport in pixels
     * @param viewportHeight the height of the viewport in pixels
     * @return the mutated and unprojected screenCoords [Vector3]
     */
    fun unproject(screenCoords: Vector3, viewportX: Float, viewportY: Float, viewportWidth: Float, viewportHeight: Float): Vector3 {
        var x = screenCoords.x
        var y = screenCoords.y
        x = x - viewportX
        y = Gdx.graphics.getHeight() - y - 1
        y = y - viewportY
        screenCoords.x = 2 * x / viewportWidth - 1
        screenCoords.y = 2 * y / viewportHeight - 1
        screenCoords.z = 2 * screenCoords.z - 1
        screenCoords.prj(invProjectionView)
        return screenCoords
    }

    /**
     * Function to translate a point given in screen coordinates to world space. It's the same as GLU gluUnProject but does not
     * rely on OpenGL. The viewport is assumed to span the whole screen and is fetched from [Graphics.getWidth] and
     * [Graphics.getHeight]. The x- and y-coordinate of vec are assumed to be in screen coordinates (origin is the top left
     * corner, y pointing down, x pointing to the right) as reported by the touch methods in [Input]. A z-coordinate of 0
     * will return a point on the near plane, a z-coordinate of 1 will return a point on the far plane.
     *
     * @param screenCoords the point in screen coordinates
     * @return the mutated and unprojected screenCoords [Vector3]
     */
    fun unproject(screenCoords: Vector3): Vector3 {
        unproject(screenCoords, 0f, 0f, Gdx.graphics.getWidth().toFloat(), Gdx.graphics.getHeight().toFloat())
        return screenCoords
    }

    /**
     * Projects the [Vector3] given in world space to screen coordinates. It's the same as GLU gluProject with one small
     * deviation: The viewport is assumed to span the whole screen. The screen coordinate system has its origin in the
     * **bottom** left, with the y-axis pointing **upwards** and the x-axis pointing to the right. This makes it easily
     * useable in conjunction with [Batch] and similar classes.
     *
     * @return the mutated and projected worldCoords [Vector3]
     */
    fun project(worldCoords: Vector3): Vector3 {
        project(worldCoords, 0f, 0f, Gdx.graphics.getWidth().toFloat(), Gdx.graphics.getHeight().toFloat())
        return worldCoords
    }

    /**
     * Projects the [Vector3] given in world space to screen coordinates. It's the same as GLU gluProject with one small
     * deviation: The viewport is assumed to span the whole screen. The screen coordinate system has its origin in the
     * **bottom** left, with the y-axis pointing **upwards** and the x-axis pointing to the right. This makes it easily
     * useable in conjunction with [Batch] and similar classes. This method allows you to specify the viewport position and
     * dimensions in the coordinate system expected by [GL20.glViewport], with the origin in the bottom
     * left corner of the screen.
     *
     * @param viewportX      the coordinate of the bottom left corner of the viewport in glViewport coordinates.
     * @param viewportY      the coordinate of the bottom left corner of the viewport in glViewport coordinates.
     * @param viewportWidth  the width of the viewport in pixels
     * @param viewportHeight the height of the viewport in pixels
     * @return the mutated and projected worldCoords [Vector3]
     */
    fun project(worldCoords: Vector3, viewportX: Float, viewportY: Float, viewportWidth: Float, viewportHeight: Float): Vector3 {
        worldCoords.prj(combined)
        worldCoords.x = viewportWidth * (worldCoords.x + 1) / 2 + viewportX
        worldCoords.y = viewportHeight * (worldCoords.y + 1) / 2 + viewportY
        worldCoords.z = (worldCoords.z + 1) / 2
        return worldCoords
    }

    /**
     * Creates a picking [Ray] from the coordinates given in screen coordinates. It is assumed that the viewport spans the
     * whole screen. The screen coordinates origin is assumed to be in the top left corner, its y-axis pointing down, the x-axis
     * pointing to the right. The returned instance is not a new instance but an internal member only accessible via this function.
     *
     * @param viewportX      the coordinate of the bottom left corner of the viewport in glViewport coordinates.
     * @param viewportY      the coordinate of the bottom left corner of the viewport in glViewport coordinates.
     * @param viewportWidth  the width of the viewport in pixels
     * @param viewportHeight the height of the viewport in pixels
     * @return the picking Ray.
     */
    fun getPickRay(screenX: Float, screenY: Float, viewportX: Float, viewportY: Float, viewportWidth: Float,
                   viewportHeight: Float): Ray {
        unproject(ray.origin.set(screenX, screenY, 0f), viewportX, viewportY, viewportWidth, viewportHeight)
        unproject(ray.direction.set(screenX, screenY, 1f), viewportX, viewportY, viewportWidth, viewportHeight)
        ray.direction.sub(ray.origin).nor()
        return ray
    }

    /**
     * Creates a picking [Ray] from the coordinates given in screen coordinates. It is assumed that the viewport spans the
     * whole screen. The screen coordinates origin is assumed to be in the top left corner, its y-axis pointing down, the x-axis
     * pointing to the right. The returned instance is not a new instance but an internal member only accessible via this function.
     *
     * @return the picking Ray.
     */
    fun getPickRay(screenX: Float, screenY: Float): Ray {
        return getPickRay(screenX, screenY, 0f, 0f, Gdx.graphics.getWidth().toFloat(), Gdx.graphics.getHeight().toFloat())
    }
}
