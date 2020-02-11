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
package com.badlogic.gdx.math

import com.badlogic.gdx.math.Affine2
import com.badlogic.gdx.math.BSpline
import com.badlogic.gdx.math.Bezier
import com.badlogic.gdx.math.CatmullRomSpline
import com.badlogic.gdx.math.CumulativeDistribution.CumulativeValue
import com.badlogic.gdx.math.DelaunayTriangulator
import com.badlogic.gdx.math.EarClippingTriangulator
import com.badlogic.gdx.math.GeometryUtils
import com.badlogic.gdx.math.GridPoint2
import com.badlogic.gdx.math.GridPoint3
import com.badlogic.gdx.math.Intersector
import com.badlogic.gdx.math.Intersector.MinimumTranslationVector
import com.badlogic.gdx.math.Intersector.SplitTriangle
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.MathUtils.Sin
import com.badlogic.gdx.math.Matrix3
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Plane
import com.badlogic.gdx.math.Plane.PlaneSide
import com.badlogic.gdx.math.RandomXS128
import java.lang.RuntimeException

/**
 * A truncated rectangular pyramid. Used to define the viewable region and its projection onto the screen.
 *
 * @see Camera.frustum
 */
class Frustum {

    companion object {
        protected val clipSpacePlanePoints = arrayOf(Vector3(-1, -1, -1), Vector3(1, -1, -1),
            Vector3(1, 1, -1), Vector3(-1, 1, -1),  // near clip
            Vector3(-1, -1, 1), Vector3(1, -1, 1), Vector3(1, 1, 1), Vector3(-1, 1, 1)) // far clip
        protected val clipSpacePlanePointsArray = FloatArray(8 * 3)
        private val tmpV = Vector3()

        init {
            val j = 0
            for (v in clipSpacePlanePoints) {
                clipSpacePlanePointsArray[com.badlogic.gdx.math.j++] = com.badlogic.gdx.math.v.x
                clipSpacePlanePointsArray[com.badlogic.gdx.math.j++] = com.badlogic.gdx.math.v.y
                clipSpacePlanePointsArray[com.badlogic.gdx.math.j++] = com.badlogic.gdx.math.v.z
            }
        }
    }

    /**
     * the six clipping planes, near, far, left, right, top, bottom
     */
    val planes: Array<Plane?> = arrayOfNulls<Plane>(6)

    /**
     * eight points making up the near and far clipping "rectangles". order is counter clockwise, starting at bottom left
     */
    val planePoints = arrayOf(Vector3(), Vector3(), Vector3(), Vector3(), Vector3(),
        Vector3(), Vector3(), Vector3())
    protected val planePointsArray = FloatArray(8 * 3)

    /**
     * Updates the clipping plane's based on the given inverse combined projection and view matrix, e.g. from an
     * [OrthographicCamera] or [PerspectiveCamera].
     *
     * @param inverseProjectionView the combined projection and view matrices.
     */
    fun update(inverseProjectionView: Matrix4) {
        java.lang.System.arraycopy(clipSpacePlanePointsArray, 0, planePointsArray, 0, clipSpacePlanePointsArray.size)
        Matrix4.prj(inverseProjectionView.`val`, planePointsArray, 0, 8, 3)
        var i = 0
        var j = 0
        while (i < 8) {
            val v = planePoints[i]
            v.x = planePointsArray[j++]
            v.y = planePointsArray[j++]
            v.z = planePointsArray[j++]
            i++
        }
        planes[0]!!.set(planePoints[1], planePoints[0], planePoints[2])
        planes[1]!!.set(planePoints[4], planePoints[5], planePoints[7])
        planes[2]!!.set(planePoints[0], planePoints[4], planePoints[3])
        planes[3]!!.set(planePoints[5], planePoints[1], planePoints[6])
        planes[4]!!.set(planePoints[2], planePoints[3], planePoints[6])
        planes[5]!!.set(planePoints[4], planePoints[0], planePoints[1])
    }

    /**
     * Returns whether the point is in the frustum.
     *
     * @param point The point
     * @return Whether the point is in the frustum.
     */
    fun pointInFrustum(point: Vector3?): Boolean {
        for (i in planes.indices) {
            val result: PlaneSide = planes[i]!!.testPoint(point)
            if (result === PlaneSide.Back) return false
        }
        return true
    }

    /**
     * Returns whether the point is in the frustum.
     *
     * @param x The X coordinate of the point
     * @param y The Y coordinate of the point
     * @param z The Z coordinate of the point
     * @return Whether the point is in the frustum.
     */
    fun pointInFrustum(x: Float, y: Float, z: Float): Boolean {
        for (i in planes.indices) {
            val result: PlaneSide = planes[i]!!.testPoint(x, y, z)
            if (result === PlaneSide.Back) return false
        }
        return true
    }

    /**
     * Returns whether the given sphere is in the frustum.
     *
     * @param center The center of the sphere
     * @param radius The radius of the sphere
     * @return Whether the sphere is in the frustum
     */
    fun sphereInFrustum(center: Vector3, radius: Float): Boolean {
        for (i in 0..5) if (planes[i]!!.normal.x * center.x + planes[i]!!.normal.y * center.y + planes[i]!!.normal.z * center.z < -radius - planes[i]!!.d) return false
        return true
    }

    /**
     * Returns whether the given sphere is in the frustum.
     *
     * @param x      The X coordinate of the center of the sphere
     * @param y      The Y coordinate of the center of the sphere
     * @param z      The Z coordinate of the center of the sphere
     * @param radius The radius of the sphere
     * @return Whether the sphere is in the frustum
     */
    fun sphereInFrustum(x: Float, y: Float, z: Float, radius: Float): Boolean {
        for (i in 0..5) if (planes[i]!!.normal.x * x + planes[i]!!.normal.y * y + planes[i]!!.normal.z * z < -radius - planes[i]!!.d) return false
        return true
    }

    /**
     * Returns whether the given sphere is in the frustum not checking whether it is behind the near and far clipping plane.
     *
     * @param center The center of the sphere
     * @param radius The radius of the sphere
     * @return Whether the sphere is in the frustum
     */
    fun sphereInFrustumWithoutNearFar(center: Vector3, radius: Float): Boolean {
        for (i in 2..5) if (planes[i]!!.normal.x * center.x + planes[i]!!.normal.y * center.y + planes[i]!!.normal.z * center.z < -radius - planes[i]!!.d) return false
        return true
    }

    /**
     * Returns whether the given sphere is in the frustum not checking whether it is behind the near and far clipping plane.
     *
     * @param x      The X coordinate of the center of the sphere
     * @param y      The Y coordinate of the center of the sphere
     * @param z      The Z coordinate of the center of the sphere
     * @param radius The radius of the sphere
     * @return Whether the sphere is in the frustum
     */
    fun sphereInFrustumWithoutNearFar(x: Float, y: Float, z: Float, radius: Float): Boolean {
        for (i in 2..5) if (planes[i]!!.normal.x * x + planes[i]!!.normal.y * y + planes[i]!!.normal.z * z < -radius - planes[i]!!.d) return false
        return true
    }

    /**
     * Returns whether the given [BoundingBox] is in the frustum.
     *
     * @param bounds The bounding box
     * @return Whether the bounding box is in the frustum
     */
    fun boundsInFrustum(bounds: BoundingBox): Boolean {
        var i = 0
        val len2 = planes.size
        while (i < len2) {
            if (planes[i]!!.testPoint(bounds.getCorner000(tmpV)) !== PlaneSide.Back) {
                i++
                continue
            }
            if (planes[i]!!.testPoint(bounds.getCorner001(tmpV)) !== PlaneSide.Back) {
                i++
                continue
            }
            if (planes[i]!!.testPoint(bounds.getCorner010(tmpV)) !== PlaneSide.Back) {
                i++
                continue
            }
            if (planes[i]!!.testPoint(bounds.getCorner011(tmpV)) !== PlaneSide.Back) {
                i++
                continue
            }
            if (planes[i]!!.testPoint(bounds.getCorner100(tmpV)) !== PlaneSide.Back) {
                i++
                continue
            }
            if (planes[i]!!.testPoint(bounds.getCorner101(tmpV)) !== PlaneSide.Back) {
                i++
                continue
            }
            if (planes[i]!!.testPoint(bounds.getCorner110(tmpV)) !== PlaneSide.Back) {
                i++
                continue
            }
            if (planes[i]!!.testPoint(bounds.getCorner111(tmpV)) !== PlaneSide.Back) {
                i++
                continue
            }
            return false
            i++
        }
        return true
    }

    /**
     * Returns whether the given bounding box is in the frustum.
     *
     * @return Whether the bounding box is in the frustum
     */
    fun boundsInFrustum(center: Vector3, dimensions: Vector3): Boolean {
        return boundsInFrustum(center.x, center.y, center.z, dimensions.x / 2, dimensions.y / 2, dimensions.z / 2)
    }

    /**
     * Returns whether the given bounding box is in the frustum.
     *
     * @return Whether the bounding box is in the frustum
     */
    fun boundsInFrustum(x: Float, y: Float, z: Float, halfWidth: Float, halfHeight: Float, halfDepth: Float): Boolean {
        var i = 0
        val len2 = planes.size
        while (i < len2) {
            if (planes[i]!!.testPoint(x + halfWidth, y + halfHeight, z + halfDepth) !== PlaneSide.Back) {
                i++
                continue
            }
            if (planes[i]!!.testPoint(x + halfWidth, y + halfHeight, z - halfDepth) !== PlaneSide.Back) {
                i++
                continue
            }
            if (planes[i]!!.testPoint(x + halfWidth, y - halfHeight, z + halfDepth) !== PlaneSide.Back) {
                i++
                continue
            }
            if (planes[i]!!.testPoint(x + halfWidth, y - halfHeight, z - halfDepth) !== PlaneSide.Back) {
                i++
                continue
            }
            if (planes[i]!!.testPoint(x - halfWidth, y + halfHeight, z + halfDepth) !== PlaneSide.Back) {
                i++
                continue
            }
            if (planes[i]!!.testPoint(x - halfWidth, y + halfHeight, z - halfDepth) !== PlaneSide.Back) {
                i++
                continue
            }
            if (planes[i]!!.testPoint(x - halfWidth, y - halfHeight, z + halfDepth) !== PlaneSide.Back) {
                i++
                continue
            }
            if (planes[i]!!.testPoint(x - halfWidth, y - halfHeight, z - halfDepth) !== PlaneSide.Back) {
                i++
                continue
            }
            return false
            i++
        }
        return true
    } // /**

    // * Calculates the pick ray for the given window coordinates. Assumes the window coordinate system has it's y downwards. The
    // * returned Ray is a member of this instance so don't reuse it outside this class.
    // *
    // * @param screen_width The window width in pixels
    // * @param screen_height The window height in pixels
    // * @param mouse_x The window x-coordinate
    // * @param mouse_y The window y-coordinate
    // * @param pos The camera position
    // * @param dir The camera direction, having unit length
    // * @param up The camera up vector, having unit length
    // * @return the picking ray.
    // */
    // public Ray calculatePickRay (float screen_width, float screen_height, float mouse_x, float mouse_y, Vector3 pos, Vector3 dir,
    // Vector3 up) {
    // float n_x = mouse_x - screen_width / 2.0f;
    // float n_y = mouse_y - screen_height / 2.0f;
    // n_x /= screen_width / 2.0f;
    // n_y /= screen_height / 2.0f;
    //
    // Z.set(dir.tmp().mul(-1)).nor();
    // X.set(up.tmp().crs(Z)).nor();
    // Y.set(Z.tmp().crs(X)).nor();
    // near_center.set(pos.tmp3().sub(Z.tmp2().mul(near)));
    // Vector3 near_point = X.tmp3().mul(near_width).mul(n_x).add(Y.tmp2().mul(near_height).mul(n_y));
    // near_point.add(near_center);
    //
    // return ray.set(near_point.tmp(), near_point.sub(pos).nor());
    // }
    // public static void main(String[] argv) {
    // PerspectiveCamera camera = new PerspectiveCamera(45, 2, 2);
    // // camera.rotate(90, 0, 1, 0);
    // camera.update();
    // System.out.println(camera.direction);
    // System.out.println(Arrays.toString(camera.frustum.planes));
    //
    // OrthographicCamera camOrtho = new OrthographicCamera(2, 2);
    // camOrtho.near = 1;
    // // camOrtho.rotate(90, 1, 0, 0);
    // camOrtho.update();
    // System.out.println(camOrtho.direction);
    // System.out.println(Arrays.toString(camOrtho.frustum.planes));
    // }
    init {
        for (i in 0..5) {
            planes[i] = Plane(Vector3(), 0)
        }
    }
}
