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
import com.badlogic.gdx.math.Frustum
import com.badlogic.gdx.math.GeometryUtils
import com.badlogic.gdx.math.GridPoint2
import com.badlogic.gdx.math.GridPoint3
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.MathUtils.Sin
import com.badlogic.gdx.math.Matrix3
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Plane
import com.badlogic.gdx.math.Plane.PlaneSide
import com.badlogic.gdx.math.RandomXS128
import java.lang.RuntimeException

/**
 * Class offering various static methods for intersection testing between different geometric objects.
 *
 * @author badlogicgames@gmail.com
 * @author jan.stria
 * @author Nathan Sweet
 */
object Intersector {

    private val v0 = Vector3()
    private val v1 = Vector3()
    private val v2 = Vector3()
    private val floatArray: FloatArray = FloatArray()
    private val floatArray2: FloatArray = FloatArray()

    /**
     * Returns whether the given point is inside the triangle. This assumes that the point is on the plane of the triangle. No
     * check is performed that this is the case.
     *
     * @param point the point
     * @param t1    the first vertex of the triangle
     * @param t2    the second vertex of the triangle
     * @param t3    the third vertex of the triangle
     * @return whether the point is in the triangle
     */
    fun isPointInTriangle(point: Vector3, t1: Vector3?, t2: Vector3?, t3: Vector3?): Boolean {
        v0.set(t1)!!.sub(point)
        v1.set(t2)!!.sub(point)
        v2.set(t3)!!.sub(point)
        val ab = v0.dot(v1)
        val ac = v0.dot(v2)
        val bc = v1.dot(v2)
        val cc = v2.dot(v2)
        if (bc * ac - cc * ab < 0) return false
        val bb = v1.dot(v1)
        return if (ab * bc - ac * bb < 0) false else true
    }

    /**
     * Returns true if the given point is inside the triangle.
     */
    fun isPointInTriangle(p: Vector2, a: Vector2, b: Vector2, c: Vector2): Boolean {
        val px1 = p.x - a.x
        val py1 = p.y - a.y
        val side12 = (b.x - a.x) * py1 - (b.y - a.y) * px1 > 0
        if ((c.x - a.x) * py1 - (c.y - a.y) * px1 > 0 == side12) return false
        return if ((c.x - b.x) * (p.y - b.y) - (c.y - b.y) * (p.x - b.x) > 0 != side12) false else true
    }

    /**
     * Returns true if the given point is inside the triangle.
     */
    fun isPointInTriangle(px: Float, py: Float, ax: Float, ay: Float, bx: Float, by: Float, cx: Float, cy: Float): Boolean {
        val px1 = px - ax
        val py1 = py - ay
        val side12 = (bx - ax) * py1 - (by - ay) * px1 > 0
        if ((cx - ax) * py1 - (cy - ay) * px1 > 0 == side12) return false
        return if ((cx - bx) * (py - by) - (cy - by) * (px - bx) > 0 != side12) false else true
    }

    fun intersectSegmentPlane(start: Vector3, end: Vector3?, plane: Plane, intersection: Vector3): Boolean {
        val dir = v0.set(end)!!.sub(start)
        val denom = dir.dot(plane.getNormal())
        if (denom == 0f) return false
        val t: Float = -(start.dot(plane.getNormal()) + plane.getD()) / denom
        if (t < 0 || t > 1) return false
        intersection.set(start).add(dir.scl(t))
        return true
    }

    /**
     * Determines on which side of the given line the point is. Returns -1 if the point is on the left side of the line, 0 if the
     * point is on the line and 1 if the point is on the right side of the line. Left and right are relative to the lines direction
     * which is linePoint1 to linePoint2.
     */
    fun pointLineSide(linePoint1: Vector2, linePoint2: Vector2, point: Vector2): Int {
        return java.lang.Math.signum(
            (linePoint2.x - linePoint1.x) * (point.y - linePoint1.y) - (linePoint2.y - linePoint1.y) * (point.x - linePoint1.x))
    }

    fun pointLineSide(linePoint1X: Float, linePoint1Y: Float, linePoint2X: Float, linePoint2Y: Float, pointX: Float,
                      pointY: Float): Int {
        return java.lang.Math
            .signum((linePoint2X - linePoint1X) * (pointY - linePoint1Y) - (linePoint2Y - linePoint1Y) * (pointX - linePoint1X))
    }

    /**
     * Checks whether the given point is in the polygon.
     *
     * @param polygon The polygon vertices passed as an array
     * @param point   The point
     * @return true if the point is in the polygon
     */
    fun isPointInPolygon(polygon: Array<Vector2>, point: Vector2): Boolean {
        var last: Vector2 = polygon.peek()
        val x = point.x
        val y = point.y
        var oddNodes = false
        for (i in 0 until polygon.size) {
            val vertex = polygon[i]
            if (vertex.y < y && last.y >= y || last.y < y && vertex.y >= y) {
                if (vertex.x + (y - vertex.y) / (last.y - vertex.y) * (last.x - vertex.x) < x) oddNodes = !oddNodes
            }
            last = vertex
        }
        return oddNodes
    }

    /**
     * Returns true if the specified point is in the polygon.
     *
     * @param offset Starting polygon index.
     * @param count  Number of array indices to use after offset.
     */
    fun isPointInPolygon(polygon: FloatArray, offset: Int, count: Int, x: Float, y: Float): Boolean {
        var oddNodes = false
        val sx = polygon[offset]
        val sy = polygon[offset + 1]
        var y1 = sy
        var yi = offset + 3
        val n = offset + count
        while (yi < n) {
            val y2 = polygon[yi]
            if (y2 < y && y1 >= y || y1 < y && y2 >= y) {
                val x2 = polygon[yi - 1]
                if (x2 + (y - y2) / (y1 - y2) * (polygon[yi - 3] - x2) < x) oddNodes = !oddNodes
            }
            y1 = y2
            yi += 2
        }
        if (sy < y && y1 >= y || y1 < y && sy >= y) {
            if (sx + (y - sy) / (y1 - sy) * (polygon[yi - 3] - sx) < x) oddNodes = !oddNodes
        }
        return oddNodes
    }

    private val ip = Vector2()
    private val ep1 = Vector2()
    private val ep2 = Vector2()
    private val s = Vector2()
    private val e = Vector2()

    /**
     * Intersects two convex polygons with clockwise vertices and sets the overlap polygon resulting from the intersection.
     * Follows the Sutherland-Hodgman algorithm.
     *
     * @param p1      The polygon that is being clipped
     * @param p2      The clip polygon
     * @param overlap The intersection of the two polygons (can be null, if an intersection polygon is not needed)
     * @return Whether the two polygons intersect.
     */
    fun intersectPolygons(p1: Polygon, p2: Polygon, overlap: Polygon?): Boolean {
        if (p1.getVertices().length === 0 || p2.getVertices().length === 0) {
            return false
        }
        val ip = ip
        val ep1 = ep1
        val ep2 = ep2
        val s = s
        val e = e
        val floatArray = floatArray
        val floatArray2 = floatArray2
        floatArray.clear()
        floatArray2.clear()
        floatArray2.addAll(p1.getTransformedVertices())
        val vertices2: FloatArray = p2.getTransformedVertices()
        run {
            var i = 0
            val last = vertices2.size - 2
            while (i <= last) {
                ep1[vertices2[i]] = vertices2[i + 1]
                // wrap around to beginning of array if index points to end;
                if (i < last) ep2[vertices2[i + 2]] = vertices2[i + 3] else ep2[vertices2[0]] = vertices2[1]
                if (floatArray2.size === 0) return false
                s[floatArray2[floatArray2.size - 2]] = floatArray2[floatArray2.size - 1]
                var j = 0
                while (j < floatArray2.size) {
                    e[floatArray2[j]] = floatArray2[j + 1]
                    // determine if point is inside clip edge
                    val side = pointLineSide(ep2, ep1, s) > 0
                    if (pointLineSide(ep2, ep1, e) > 0) {
                        if (!side) {
                            intersectLines(s, e, ep1, ep2, ip)
                            if (floatArray.size < 2 || floatArray[floatArray.size - 2] !== ip.x || floatArray[floatArray.size - 1] !== ip.y) {
                                floatArray.add(ip.x)
                                floatArray.add(ip.y)
                            }
                        }
                        floatArray.add(e.x)
                        floatArray.add(e.y)
                    } else if (side) {
                        intersectLines(s, e, ep1, ep2, ip)
                        floatArray.add(ip.x)
                        floatArray.add(ip.y)
                    }
                    s[e.x] = e.y
                    j += 2
                }
                floatArray2.clear()
                floatArray2.addAll(floatArray)
                floatArray.clear()
                i += 2
            }
        }
        if (floatArray2.size !== 0) {
            if (overlap != null) {
                if (overlap.getVertices().length === floatArray2.size) java.lang.System.arraycopy(floatArray2.items, 0, overlap.getVertices(), 0, floatArray2.size) else overlap.setVertices(floatArray2.toArray())
            }
            return true
        }
        return false
    }

    /**
     * Returns true if the specified poygons intersect.
     */
    fun intersectPolygons(polygon1: FloatArray, polygon2: FloatArray): Boolean {
        if (isPointInPolygon(polygon1.items, 0, polygon1.size, polygon2.items.get(0), polygon2.items.get(1))) return true
        return if (isPointInPolygon(polygon2.items, 0, polygon2.size, polygon1.items.get(0), polygon1.items.get(1))) true else intersectPolygonEdges(polygon1, polygon2)
    }

    /**
     * Returns true if the lines of the specified poygons intersect.
     */
    fun intersectPolygonEdges(polygon1: FloatArray, polygon2: FloatArray): Boolean {
        val last1 = polygon1.size - 2
        val last2 = polygon2.size - 2
        val p1: FloatArray = polygon1.items
        val p2: FloatArray = polygon2.items
        var x1 = p1[last1]
        var y1 = p1[last1 + 1]
        run {
            var i = 0
            while (i <= last1) {
                val x2 = p1[i]
                val y2 = p1[i + 1]
                var x3 = p2[last2]
                var y3 = p2[last2 + 1]
                var j = 0
                while (j <= last2) {
                    val x4 = p2[j]
                    val y4 = p2[j + 1]
                    if (intersectSegments(x1, y1, x2, y2, x3, y3, x4, y4, null)) return true
                    x3 = x4
                    y3 = y4
                    j += 2
                }
                x1 = x2
                y1 = y2
                i += 2
            }
        }
        return false
    }

    /**
     * Returns the distance between the given line and point. Note the specified line is not a line segment.
     */
    fun distanceLinePoint(startX: Float, startY: Float, endX: Float, endY: Float, pointX: Float, pointY: Float): Float {
        val normalLength = java.lang.Math.sqrt((endX - startX) * (endX - startX) + (endY - startY) * (endY - startY).toDouble()) as Float
        return java.lang.Math.abs((pointX - startX) * (endY - startY) - (pointY - startY) * (endX - startX)) / normalLength
    }

    /**
     * Returns the distance between the given segment and point.
     */
    fun distanceSegmentPoint(startX: Float, startY: Float, endX: Float, endY: Float, pointX: Float, pointY: Float): Float {
        return nearestSegmentPoint(startX, startY, endX, endY, pointX, pointY, v2tmp).dst(pointX, pointY)
    }

    /**
     * Returns the distance between the given segment and point.
     */
    fun distanceSegmentPoint(start: Vector2, end: Vector2, point: Vector2): Float {
        return nearestSegmentPoint(start, end, point, v2tmp).dst(point)
    }

    /**
     * Returns a point on the segment nearest to the specified point.
     */
    fun nearestSegmentPoint(start: Vector2, end: Vector2, point: Vector2, nearest: Vector2): Vector2 {
        val length2 = start.dst2(end)
        if (length2 == 0f) return nearest.set(start)
        val t = ((point.x - start.x) * (end.x - start.x) + (point.y - start.y) * (end.y - start.y)) / length2
        if (t < 0) return nearest.set(start)
        return if (t > 1) nearest.set(end) else nearest.set(start.x + t * (end.x - start.x), start.y + t * (end.y - start.y))
    }

    /**
     * Returns a point on the segment nearest to the specified point.
     */
    fun nearestSegmentPoint(startX: Float, startY: Float, endX: Float, endY: Float, pointX: Float, pointY: Float,
                            nearest: Vector2): Vector2 {
        val xDiff = endX - startX
        val yDiff = endY - startY
        val length2 = xDiff * xDiff + yDiff * yDiff
        if (length2 == 0f) return nearest.set(startX, startY)
        val t = ((pointX - startX) * (endX - startX) + (pointY - startY) * (endY - startY)) / length2
        if (t < 0) return nearest.set(startX, startY)
        return if (t > 1) nearest.set(endX, endY) else nearest.set(startX + t * (endX - startX), startY + t * (endY - startY))
    }

    /**
     * Returns whether the given line segment intersects the given circle.
     *
     * @param start        The start point of the line segment
     * @param end          The end point of the line segment
     * @param center       The center of the circle
     * @param squareRadius The squared radius of the circle
     * @return Whether the line segment and the circle intersect
     */
    fun intersectSegmentCircle(start: Vector2, end: Vector2, center: Vector2, squareRadius: Float): Boolean {
        tmp[end.x - start.x, end.y - start.y] = 0f
        tmp1[center.x - start.x, center.y - start.y] = 0f
        val l = tmp.len()
        val u = tmp1.dot(tmp.nor())
        if (u <= 0) {
            tmp2[start.x, start.y] = 0f
        } else if (u >= l) {
            tmp2[end.x, end.y] = 0f
        } else {
            tmp3.set(tmp.scl(u)) // remember tmp is already normalized
            tmp2[tmp3.x + start.x, tmp3.y + start.y] = 0f
        }
        val x = center.x - tmp2.x
        val y = center.y - tmp2.y
        return x * x + y * y <= squareRadius
    }

    /**
     * Checks whether the line segment and the circle intersect and returns by how much and in what direction the line has to move
     * away from the circle to not intersect.
     *
     * @param start        The line segment starting point
     * @param end          The line segment end point
     * @param point        The center of the circle
     * @param radius       The radius of the circle
     * @param displacement The displacement vector set by the method having unit length
     * @return The displacement or Float.POSITIVE_INFINITY if no intersection is present
     */
    fun intersectSegmentCircleDisplace(start: Vector2, end: Vector2, point: Vector2, radius: Float,
                                       displacement: Vector2): Float {
        var u = (point.x - start.x) * (end.x - start.x) + (point.y - start.y) * (end.y - start.y)
        var d = start.dst(end)
        u /= d * d
        if (u < 0 || u > 1) return Float.POSITIVE_INFINITY
        tmp.set(end.x, end.y, 0f).sub(start.x, start.y, 0f)
        tmp2.set(start.x, start.y, 0f).add(tmp.scl(u))
        d = tmp2.dst(point.x, point.y, 0f)
        return if (d < radius) {
            displacement.set(point).sub(tmp2.x, tmp2.y).nor()
            d
        } else Float.POSITIVE_INFINITY
    }

    /**
     * Intersect two 2D Rays and return the scalar parameter of the first ray at the intersection point. You can get the
     * intersection point by: Vector2 point(direction1).scl(scalar).add(start1); For more information, check:
     * http://stackoverflow.com/a/565282/1091440
     *
     * @param start1     Where the first ray start
     * @param direction1 The direction the first ray is pointing
     * @param start2     Where the second ray start
     * @param direction2 The direction the second ray is pointing
     * @return scalar parameter on the first ray describing the point where the intersection happens. May be negative. In case the
     * rays are collinear, Float.POSITIVE_INFINITY will be returned.
     */
    fun intersectRayRay(start1: Vector2, direction1: Vector2, start2: Vector2, direction2: Vector2): Float {
        val difx = start2.x - start1.x
        val dify = start2.y - start1.y
        val d1xd2 = direction1.x * direction2.y - direction1.y * direction2.x
        if (d1xd2 == 0.0f) {
            return Float.POSITIVE_INFINITY // collinear
        }
        val d2sx = direction2.x / d1xd2
        val d2sy = direction2.y / d1xd2
        return difx * d2sy - dify * d2sx
    }

    /**
     * Intersects a [Ray] and a [Plane]. The intersection point is stored in intersection in case an intersection is
     * present.
     *
     * @param ray          The ray
     * @param plane        The plane
     * @param intersection The vector the intersection point is written to (optional)
     * @return Whether an intersection is present.
     */
    fun intersectRayPlane(ray: Ray, plane: Plane, intersection: Vector3?): Boolean {
        val denom: Float = ray.direction.dot(plane.getNormal())
        return if (denom != 0f) {
            val t: Float = -(ray.origin.dot(plane.getNormal()) + plane.getD()) / denom
            if (t < 0) return false
            intersection?.set(ray.origin)?.add(v0.set(ray.direction).scl(t))
            true
        } else if (plane.testPoint(ray.origin) === Plane.PlaneSide.OnPlane) {
            intersection?.set(ray.origin)
            true
        } else false
    }

    /**
     * Intersects a line and a plane. The intersection is returned as the distance from the first point to the plane. In case an
     * intersection happened, the return value is in the range [0,1]. The intersection point can be recovered by point1 + t *
     * (point2 - point1) where t is the return value of this method.
     *
     * @param x
     * @param y
     * @param z
     * @param x2
     * @param y2
     * @param z2
     * @param plane
     */
    fun intersectLinePlane(x: Float, y: Float, z: Float, x2: Float, y2: Float, z2: Float, plane: Plane,
                           intersection: Vector3?): Float {
        val direction = tmp.set(x2, y2, z2).sub(x, y, z)
        val origin = tmp2.set(x, y, z)
        val denom = direction.dot(plane.getNormal())
        if (denom != 0f) {
            val t: Float = -(origin.dot(plane.getNormal()) + plane.getD()) / denom
            intersection?.set(origin)?.add(direction.scl(t))
            return t
        } else if (plane.testPoint(origin) === Plane.PlaneSide.OnPlane) {
            intersection?.set(origin)
            return 0
        }
        return (-1).toFloat()
    }

    private val p: Plane = Plane(Vector3(), 0)
    private val i = Vector3()

    /**
     * Intersect a [Ray] and a triangle, returning the intersection point in intersection.
     *
     * @param ray          The ray
     * @param t1           The first vertex of the triangle
     * @param t2           The second vertex of the triangle
     * @param t3           The third vertex of the triangle
     * @param intersection The intersection point (optional)
     * @return True in case an intersection is present.
     */
    fun intersectRayTriangle(ray: Ray, t1: Vector3, t2: Vector3, t3: Vector3, intersection: Vector3?): Boolean {
        val edge1 = v0.set(t2).sub(t1)
        val edge2 = v1.set(t3).sub(t1)
        val pvec: Vector3 = v2.set(ray.direction).crs(edge2)
        var det = edge1.dot(pvec)
        if (MathUtils.isZero(det)) {
            p.set(t1, t2, t3)
            if (p.testPoint(ray.origin) === PlaneSide.OnPlane && isPointInTriangle(ray.origin, t1, t2, t3)) {
                intersection?.set(ray.origin)
                return true
            }
            return false
        }
        det = 1.0f / det
        val tvec: Vector3 = i.set(ray.origin).sub(t1)
        val u = tvec.dot(pvec) * det
        if (u < 0.0f || u > 1.0f) return false
        val qvec = tvec.crs(edge1)
        val v: Float = ray.direction.dot(qvec) * det
        if (v < 0.0f || u + v > 1.0f) return false
        val t = edge2.dot(qvec) * det
        if (t < 0) return false
        if (intersection != null) {
            if (t <= MathUtils.FLOAT_ROUNDING_ERROR) {
                intersection.set(ray.origin)
            } else {
                ray.getEndPoint(intersection, t)
            }
        }
        return true
    }

    private val dir = Vector3()
    private val start = Vector3()

    /**
     * Intersects a [Ray] and a sphere, returning the intersection point in intersection.
     *
     * @param ray          The ray, the direction component must be normalized before calling this method
     * @param center       The center of the sphere
     * @param radius       The radius of the sphere
     * @param intersection The intersection point (optional, can be null)
     * @return Whether an intersection is present.
     */
    fun intersectRaySphere(ray: Ray, center: Vector3, radius: Float, intersection: Vector3?): Boolean {
        val len: Float = ray.direction.dot(center.x - ray.origin.x, center.y - ray.origin.y, center.z - ray.origin.z)
        if (len < 0f) // behind the ray
            return false
        val dst2 = center.dst2(ray.origin.x + ray.direction.x * len, ray.origin.y + ray.direction.y * len,
            ray.origin.z + ray.direction.z * len)
        val r2 = radius * radius
        if (dst2 > r2) return false
        intersection?.set(ray.direction)?.scl(len - java.lang.Math.sqrt(r2 - dst2.toDouble()) as Float)?.add(ray.origin)
        return true
    }

    /**
     * Intersects a [Ray] and a [BoundingBox], returning the intersection point in intersection. This intersection is
     * defined as the point on the ray closest to the origin which is within the specified bounds.
     *
     *
     *
     * The returned intersection (if any) is guaranteed to be within the bounds of the bounding box, but it can occasionally
     * diverge slightly from ray, due to small floating-point errors.
     *
     *
     *
     *
     * If the origin of the ray is inside the box, this method returns true and the intersection point is set to the origin of the
     * ray, accordingly to the definition above.
     *
     *
     * @param ray          The ray
     * @param box          The box
     * @param intersection The intersection point (optional)
     * @return Whether an intersection is present.
     */
    fun intersectRayBounds(ray: Ray, box: BoundingBox, intersection: Vector3?): Boolean {
        if (box.contains(ray.origin)) {
            intersection?.set(ray.origin)
            return true
        }
        var lowest = 0f
        var t: Float
        var hit = false

        // min x
        if (ray.origin.x <= box.min.x && ray.direction.x > 0) {
            t = (box.min.x - ray.origin.x) / ray.direction.x
            if (t >= 0) {
                v2.set(ray.direction).scl(t).add(ray.origin)
                if (v2.y >= box.min.y && v2.y <= box.max.y && v2.z >= box.min.z && v2.z <= box.max.z && (!hit || t < lowest)) {
                    hit = true
                    lowest = t
                }
            }
        }
        // max x
        if (ray.origin.x >= box.max.x && ray.direction.x < 0) {
            t = (box.max.x - ray.origin.x) / ray.direction.x
            if (t >= 0) {
                v2.set(ray.direction).scl(t).add(ray.origin)
                if (v2.y >= box.min.y && v2.y <= box.max.y && v2.z >= box.min.z && v2.z <= box.max.z && (!hit || t < lowest)) {
                    hit = true
                    lowest = t
                }
            }
        }
        // min y
        if (ray.origin.y <= box.min.y && ray.direction.y > 0) {
            t = (box.min.y - ray.origin.y) / ray.direction.y
            if (t >= 0) {
                v2.set(ray.direction).scl(t).add(ray.origin)
                if (v2.x >= box.min.x && v2.x <= box.max.x && v2.z >= box.min.z && v2.z <= box.max.z && (!hit || t < lowest)) {
                    hit = true
                    lowest = t
                }
            }
        }
        // max y
        if (ray.origin.y >= box.max.y && ray.direction.y < 0) {
            t = (box.max.y - ray.origin.y) / ray.direction.y
            if (t >= 0) {
                v2.set(ray.direction).scl(t).add(ray.origin)
                if (v2.x >= box.min.x && v2.x <= box.max.x && v2.z >= box.min.z && v2.z <= box.max.z && (!hit || t < lowest)) {
                    hit = true
                    lowest = t
                }
            }
        }
        // min z
        if (ray.origin.z <= box.min.z && ray.direction.z > 0) {
            t = (box.min.z - ray.origin.z) / ray.direction.z
            if (t >= 0) {
                v2.set(ray.direction).scl(t).add(ray.origin)
                if (v2.x >= box.min.x && v2.x <= box.max.x && v2.y >= box.min.y && v2.y <= box.max.y && (!hit || t < lowest)) {
                    hit = true
                    lowest = t
                }
            }
        }
        // max y
        if (ray.origin.z >= box.max.z && ray.direction.z < 0) {
            t = (box.max.z - ray.origin.z) / ray.direction.z
            if (t >= 0) {
                v2.set(ray.direction).scl(t).add(ray.origin)
                if (v2.x >= box.min.x && v2.x <= box.max.x && v2.y >= box.min.y && v2.y <= box.max.y && (!hit || t < lowest)) {
                    hit = true
                    lowest = t
                }
            }
        }
        if (hit && intersection != null) {
            intersection.set(ray.direction).scl(lowest).add(ray.origin)
            if (intersection.x < box.min.x) {
                intersection.x = box.min.x
            } else if (intersection.x > box.max.x) {
                intersection.x = box.max.x
            }
            if (intersection.y < box.min.y) {
                intersection.y = box.min.y
            } else if (intersection.y > box.max.y) {
                intersection.y = box.max.y
            }
            if (intersection.z < box.min.z) {
                intersection.z = box.min.z
            } else if (intersection.z > box.max.z) {
                intersection.z = box.max.z
            }
        }
        return hit
    }

    /**
     * Quick check whether the given [Ray] and [BoundingBox] intersect.
     *
     * @param ray The ray
     * @param box The bounding box
     * @return Whether the ray and the bounding box intersect.
     */
    fun intersectRayBoundsFast(ray: Ray, box: BoundingBox): Boolean {
        return intersectRayBoundsFast(ray, box.getCenter(tmp1), box.getDimensions(tmp2))
    }

    /**
     * Quick check whether the given [Ray] and [BoundingBox] intersect.
     *
     * @param ray        The ray
     * @param center     The center of the bounding box
     * @param dimensions The dimensions (width, height and depth) of the bounding box
     * @return Whether the ray and the bounding box intersect.
     */
    fun intersectRayBoundsFast(ray: Ray, center: Vector3, dimensions: Vector3): Boolean {
        val divX: Float = 1f / ray.direction.x
        val divY: Float = 1f / ray.direction.y
        val divZ: Float = 1f / ray.direction.z
        var minx: Float = (center.x - dimensions.x * .5f - ray.origin.x) * divX
        var maxx: Float = (center.x + dimensions.x * .5f - ray.origin.x) * divX
        if (minx > maxx) {
            val t = minx
            minx = maxx
            maxx = t
        }
        var miny: Float = (center.y - dimensions.y * .5f - ray.origin.y) * divY
        var maxy: Float = (center.y + dimensions.y * .5f - ray.origin.y) * divY
        if (miny > maxy) {
            val t = miny
            miny = maxy
            maxy = t
        }
        var minz: Float = (center.z - dimensions.z * .5f - ray.origin.z) * divZ
        var maxz: Float = (center.z + dimensions.z * .5f - ray.origin.z) * divZ
        if (minz > maxz) {
            val t = minz
            minz = maxz
            maxz = t
        }
        val min: Float = java.lang.Math.max(java.lang.Math.max(minx, miny), minz)
        val max: Float = java.lang.Math.min(java.lang.Math.min(maxx, maxy), maxz)
        return max >= 0 && max >= min
    }

    var best = Vector3()
    var tmp = Vector3()
    var tmp1 = Vector3()
    var tmp2 = Vector3()
    var tmp3 = Vector3()
    var v2tmp = Vector2()

    /**
     * Intersects the given ray with list of triangles. Returns the nearest intersection point in intersection
     *
     * @param ray          The ray
     * @param triangles    The triangles, each successive 9 elements are the 3 vertices of a triangle, a vertex is made of 3
     * successive floats (XYZ)
     * @param intersection The nearest intersection point (optional)
     * @return Whether the ray and the triangles intersect.
     */
    fun intersectRayTriangles(ray: Ray, triangles: FloatArray, intersection: Vector3?): Boolean {
        var min_dist = Float.MAX_VALUE
        var hit = false
        if (triangles.size % 9 != 0) throw RuntimeException("triangles array size is not a multiple of 9")
        run {
            var i = 0
            while (i < triangles.size) {
                val result = intersectRayTriangle(ray, tmp1.set(triangles[i], triangles[i + 1], triangles[i + 2]),
                    tmp2.set(triangles[i + 3], triangles[i + 4], triangles[i + 5]),
                    tmp3.set(triangles[i + 6], triangles[i + 7], triangles[i + 8]), tmp)
                if (result) {
                    val dist: Float = ray.origin.dst2(tmp)
                    if (dist < min_dist) {
                        min_dist = dist
                        best.set(tmp)
                        hit = true
                    }
                }
                i += 9
            }
        }
        return if (!hit) false else {
            intersection?.set(best)
            true
        }
    }

    /**
     * Intersects the given ray with list of triangles. Returns the nearest intersection point in intersection
     *
     * @param ray          The ray
     * @param vertices     the vertices
     * @param indices      the indices, each successive 3 shorts index the 3 vertices of a triangle
     * @param vertexSize   the size of a vertex in floats
     * @param intersection The nearest intersection point (optional)
     * @return Whether the ray and the triangles intersect.
     */
    fun intersectRayTriangles(ray: Ray, vertices: FloatArray, indices: ShortArray, vertexSize: Int,
                              intersection: Vector3?): Boolean {
        var min_dist = Float.MAX_VALUE
        var hit = false
        if (indices.size % 3 != 0) throw RuntimeException("triangle list size is not a multiple of 3")
        run {
            var i = 0
            while (i < indices.size) {
                val i1 = indices[i] * vertexSize
                val i2 = indices[i + 1] * vertexSize
                val i3 = indices[i + 2] * vertexSize
                val result = intersectRayTriangle(ray, tmp1.set(vertices[i1], vertices[i1 + 1], vertices[i1 + 2]),
                    tmp2.set(vertices[i2], vertices[i2 + 1], vertices[i2 + 2]),
                    tmp3.set(vertices[i3], vertices[i3 + 1], vertices[i3 + 2]), tmp)
                if (result) {
                    val dist: Float = ray.origin.dst2(tmp)
                    if (dist < min_dist) {
                        min_dist = dist
                        best.set(tmp)
                        hit = true
                    }
                }
                i += 3
            }
        }
        return if (!hit) false else {
            intersection?.set(best)
            true
        }
    }

    /**
     * Intersects the given ray with list of triangles. Returns the nearest intersection point in intersection
     *
     * @param ray          The ray
     * @param triangles    The triangles, each successive 3 elements are the 3 vertices of a triangle
     * @param intersection The nearest intersection point (optional)
     * @return Whether the ray and the triangles intersect.
     */
    fun intersectRayTriangles(ray: Ray, triangles: List<Vector3>, intersection: Vector3?): Boolean {
        var min_dist = Float.MAX_VALUE
        var hit = false
        if (triangles.size() % 3 !== 0) throw RuntimeException("triangle list size is not a multiple of 3")
        run {
            var i = 0
            while (i < triangles.size()) {
                val result = intersectRayTriangle(ray, triangles[i], triangles[i + 1], triangles[i + 2], tmp)
                if (result) {
                    val dist: Float = ray.origin.dst2(tmp)
                    if (dist < min_dist) {
                        min_dist = dist
                        best.set(tmp)
                        hit = true
                    }
                }
                i += 3
            }
        }
        return if (!hit) false else {
            intersection?.set(best)
            true
        }
    }

    /**
     * Intersects the two lines and returns the intersection point in intersection.
     *
     * @param p1           The first point of the first line
     * @param p2           The second point of the first line
     * @param p3           The first point of the second line
     * @param p4           The second point of the second line
     * @param intersection The intersection point. May be null.
     * @return Whether the two lines intersect
     */
    fun intersectLines(p1: Vector2, p2: Vector2, p3: Vector2, p4: Vector2, intersection: Vector2?): Boolean {
        val x1 = p1.x
        val y1 = p1.y
        val x2 = p2.x
        val y2 = p2.y
        val x3 = p3.x
        val y3 = p3.y
        val x4 = p4.x
        val y4 = p4.y
        val d = (y4 - y3) * (x2 - x1) - (x4 - x3) * (y2 - y1)
        if (d == 0f) return false
        if (intersection != null) {
            val ua = ((x4 - x3) * (y1 - y3) - (y4 - y3) * (x1 - x3)) / d
            intersection[x1 + (x2 - x1) * ua] = y1 + (y2 - y1) * ua
        }
        return true
    }

    /**
     * Intersects the two lines and returns the intersection point in intersection.
     *
     * @param intersection The intersection point, or null.
     * @return Whether the two lines intersect
     */
    fun intersectLines(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float, x4: Float, y4: Float,
                       intersection: Vector2?): Boolean {
        val d = (y4 - y3) * (x2 - x1) - (x4 - x3) * (y2 - y1)
        if (d == 0f) return false
        if (intersection != null) {
            val ua = ((x4 - x3) * (y1 - y3) - (y4 - y3) * (x1 - x3)) / d
            intersection[x1 + (x2 - x1) * ua] = y1 + (y2 - y1) * ua
        }
        return true
    }

    /**
     * Check whether the given line and [Polygon] intersect.
     *
     * @param p1      The first point of the line
     * @param p2      The second point of the line
     * @param polygon The polygon
     * @return Whether polygon and line intersects
     */
    fun intersectLinePolygon(p1: Vector2, p2: Vector2, polygon: Polygon): Boolean {
        val vertices: FloatArray = polygon.getTransformedVertices()
        val x1 = p1.x
        val y1 = p1.y
        val x2 = p2.x
        val y2 = p2.y
        val n = vertices.size
        var x3 = vertices[n - 2]
        var y3 = vertices[n - 1]
        run {
            var i = 0
            while (i < n) {
                val x4 = vertices[i]
                val y4 = vertices[i + 1]
                val d = (y4 - y3) * (x2 - x1) - (x4 - x3) * (y2 - y1)
                if (d != 0f) {
                    val yd = y1 - y3
                    val xd = x1 - x3
                    val ua = ((x4 - x3) * yd - (y4 - y3) * xd) / d
                    if (ua >= 0 && ua <= 1) {
                        return true
                    }
                }
                x3 = x4
                y3 = y4
                i += 2
            }
        }
        return false
    }

    /**
     * Determines whether the given rectangles intersect and, if they do, sets the supplied `intersection` rectangle to the
     * area of overlap.
     *
     * @return Whether the rectangles intersect
     */
    fun intersectRectangles(rectangle1: Rectangle, rectangle2: Rectangle, intersection: Rectangle): Boolean {
        if (rectangle1.overlaps(rectangle2)) {
            intersection.x = java.lang.Math.max(rectangle1.x, rectangle2.x)
            intersection.width = java.lang.Math.min(rectangle1.x + rectangle1.width, rectangle2.x + rectangle2.width) - intersection.x
            intersection.y = java.lang.Math.max(rectangle1.y, rectangle2.y)
            intersection.height = java.lang.Math.min(rectangle1.y + rectangle1.height, rectangle2.y + rectangle2.height) - intersection.y
            return true
        }
        return false
    }

    /**
     * Determines whether the given rectangle and segment intersect
     *
     * @param startX    x-coordinate start of line segment
     * @param startY    y-coordinate start of line segment
     * @param endX      y-coordinate end of line segment
     * @param endY      y-coordinate end of line segment
     * @param rectangle rectangle that is being tested for collision
     * @return whether the rectangle intersects with the line segment
     */
    fun intersectSegmentRectangle(startX: Float, startY: Float, endX: Float, endY: Float, rectangle: Rectangle): Boolean {
        val rectangleEndX = rectangle.x + rectangle.width
        val rectangleEndY = rectangle.y + rectangle.height
        if (intersectSegments(startX, startY, endX, endY, rectangle.x, rectangle.y, rectangle.x, rectangleEndY, null)) return true
        if (intersectSegments(startX, startY, endX, endY, rectangle.x, rectangle.y, rectangleEndX, rectangle.y, null)) return true
        if (intersectSegments(startX, startY, endX, endY, rectangleEndX, rectangle.y, rectangleEndX, rectangleEndY, null)) return true
        return if (intersectSegments(startX, startY, endX, endY, rectangle.x, rectangleEndY, rectangleEndX, rectangleEndY, null)) true else rectangle.contains(startX, startY)
    }

    /**
     * [.intersectSegmentRectangle]
     */
    fun intersectSegmentRectangle(start: Vector2, end: Vector2, rectangle: Rectangle): Boolean {
        return intersectSegmentRectangle(start.x, start.y, end.x, end.y, rectangle)
    }

    /**
     * Check whether the given line segment and [Polygon] intersect.
     *
     * @param p1 The first point of the segment
     * @param p2 The second point of the segment
     * @return Whether polygon and segment intersect
     */
    fun intersectSegmentPolygon(p1: Vector2, p2: Vector2, polygon: Polygon): Boolean {
        val vertices: FloatArray = polygon.getTransformedVertices()
        val x1 = p1.x
        val y1 = p1.y
        val x2 = p2.x
        val y2 = p2.y
        val n = vertices.size
        var x3 = vertices[n - 2]
        var y3 = vertices[n - 1]
        run {
            var i = 0
            while (i < n) {
                val x4 = vertices[i]
                val y4 = vertices[i + 1]
                val d = (y4 - y3) * (x2 - x1) - (x4 - x3) * (y2 - y1)
                if (d != 0f) {
                    val yd = y1 - y3
                    val xd = x1 - x3
                    val ua = ((x4 - x3) * yd - (y4 - y3) * xd) / d
                    if (ua >= 0 && ua <= 1) {
                        val ub = ((x2 - x1) * yd - (y2 - y1) * xd) / d
                        if (ub >= 0 && ub <= 1) {
                            return true
                        }
                    }
                }
                x3 = x4
                y3 = y4
                i += 2
            }
        }
        return false
    }

    /**
     * Intersects the two line segments and returns the intersection point in intersection.
     *
     * @param p1           The first point of the first line segment
     * @param p2           The second point of the first line segment
     * @param p3           The first point of the second line segment
     * @param p4           The second point of the second line segment
     * @param intersection The intersection point. May be null.
     * @return Whether the two line segments intersect
     */
    fun intersectSegments(p1: Vector2, p2: Vector2, p3: Vector2, p4: Vector2, intersection: Vector2?): Boolean {
        val x1 = p1.x
        val y1 = p1.y
        val x2 = p2.x
        val y2 = p2.y
        val x3 = p3.x
        val y3 = p3.y
        val x4 = p4.x
        val y4 = p4.y
        val d = (y4 - y3) * (x2 - x1) - (x4 - x3) * (y2 - y1)
        if (d == 0f) return false
        val yd = y1 - y3
        val xd = x1 - x3
        val ua = ((x4 - x3) * yd - (y4 - y3) * xd) / d
        if (ua < 0 || ua > 1) return false
        val ub = ((x2 - x1) * yd - (y2 - y1) * xd) / d
        if (ub < 0 || ub > 1) return false
        intersection?.set(x1 + (x2 - x1) * ua, y1 + (y2 - y1) * ua)
        return true
    }

    /**
     * @param intersection May be null.
     */
    fun intersectSegments(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float, x4: Float, y4: Float,
                          intersection: Vector2?): Boolean {
        val d = (y4 - y3) * (x2 - x1) - (x4 - x3) * (y2 - y1)
        if (d == 0f) return false
        val yd = y1 - y3
        val xd = x1 - x3
        val ua = ((x4 - x3) * yd - (y4 - y3) * xd) / d
        if (ua < 0 || ua > 1) return false
        val ub = ((x2 - x1) * yd - (y2 - y1) * xd) / d
        if (ub < 0 || ub > 1) return false
        intersection?.set(x1 + (x2 - x1) * ua, y1 + (y2 - y1) * ua)
        return true
    }

    fun det(a: Float, b: Float, c: Float, d: Float): Float {
        return a * d - b * c
    }

    fun detd(a: Double, b: Double, c: Double, d: Double): Double {
        return a * d - b * c
    }

    fun overlaps(c1: Circle, c2: Circle): Boolean {
        return c1.overlaps(c2)
    }

    fun overlaps(r1: Rectangle, r2: Rectangle?): Boolean {
        return r1.overlaps(r2!!)
    }

    fun overlaps(c: Circle, r: Rectangle): Boolean {
        var closestX: Float = c.x
        var closestY: Float = c.y
        if (c.x < r.x) {
            closestX = r.x
        } else if (c.x > r.x + r.width) {
            closestX = r.x + r.width
        }
        if (c.y < r.y) {
            closestY = r.y
        } else if (c.y > r.y + r.height) {
            closestY = r.y + r.height
        }
        closestX = closestX - c.x
        closestX *= closestX
        closestY = closestY - c.y
        closestY *= closestY
        return closestX + closestY < c.radius * c.radius
    }

    /**
     * Check whether specified counter-clockwise wound convex polygons overlap.
     *
     * @param p1 The first polygon.
     * @param p2 The second polygon.
     * @return Whether polygons overlap.
     */
    fun overlapConvexPolygons(p1: Polygon, p2: Polygon): Boolean {
        return overlapConvexPolygons(p1, p2, null)
    }

    /**
     * Check whether specified counter-clockwise wound convex polygons overlap. If they do, optionally obtain a Minimum
     * Translation Vector indicating the minimum magnitude vector required to push the polygon p1 out of collision with polygon p2.
     *
     * @param p1  The first polygon.
     * @param p2  The second polygon.
     * @param mtv A Minimum Translation Vector to fill in the case of a collision, or null (optional).
     * @return Whether polygons overlap.
     */
    fun overlapConvexPolygons(p1: Polygon, p2: Polygon, mtv: MinimumTranslationVector?): Boolean {
        return overlapConvexPolygons(p1.getTransformedVertices(), p2.getTransformedVertices(), mtv)
    }

    /**
     * @see .overlapConvexPolygons
     */
    fun overlapConvexPolygons(verts1: FloatArray?, verts2: FloatArray?, mtv: MinimumTranslationVector?): Boolean {
        return overlapConvexPolygons(verts1, 0, verts1!!.size, verts2, 0, verts2!!.size, mtv)
    }

    /**
     * Check whether polygons defined by the given counter-clockwise wound vertex arrays overlap. If they do, optionally obtain a
     * Minimum Translation Vector indicating the minimum magnitude vector required to push the polygon defined by verts1 out of the
     * collision with the polygon defined by verts2.
     *
     * @param verts1 Vertices of the first polygon.
     * @param verts2 Vertices of the second polygon.
     * @param mtv    A Minimum Translation Vector to fill in the case of a collision, or null (optional).
     * @return Whether polygons overlap.
     */
    fun overlapConvexPolygons(verts1: FloatArray?, offset1: Int, count1: Int, verts2: FloatArray?, offset2: Int, count2: Int,
                              mtv: MinimumTranslationVector?): Boolean {
        var overlap = Float.MAX_VALUE
        var smallestAxisX = 0f
        var smallestAxisY = 0f
        var numInNormalDir: Int
        val end1 = offset1 + count1
        val end2 = offset2 + count2

        // Get polygon1 axes
        run {
            var i = offset1
            while (i < end1) {
                val x1 = verts1!![i]
                val y1 = verts1[i + 1]
                val x2 = verts1[(i + 2) % count1]
                val y2 = verts1[(i + 3) % count1]
                var axisX = y1 - y2
                var axisY = -(x1 - x2)
                val length = java.lang.Math.sqrt(axisX * axisX + axisY * axisY.toDouble()) as Float
                axisX /= length
                axisY /= length

                // -- Begin check for separation on this axis --//

                // Project polygon1 onto this axis
                var min1 = axisX * verts1[0] + axisY * verts1[1]
                var max1 = min1
                run {
                    var j = offset1
                    while (j < end1) {
                        val p = axisX * verts1[j] + axisY * verts1[j + 1]
                        if (p < min1) {
                            min1 = p
                        } else if (p > max1) {
                            max1 = p
                        }
                        j += 2
                    }
                }

                // Project polygon2 onto this axis
                numInNormalDir = 0
                var min2 = axisX * verts2!![0] + axisY * verts2[1]
                var max2 = min2
                var j = offset2
                while (j < end2) {

                    // Counts the number of points that are within the projected area.
                    numInNormalDir -= pointLineSide(x1, y1, x2, y2, verts2[j], verts2[j + 1])
                    val p = axisX * verts2[j] + axisY * verts2[j + 1]
                    if (p < min2) {
                        min2 = p
                    } else if (p > max2) {
                        max2 = p
                    }
                    j += 2
                }
                if (!(min1 <= min2 && max1 >= min2 || min2 <= min1 && max2 >= min1)) {
                    return false
                } else {
                    var o: Float = java.lang.Math.min(max1, max2) - java.lang.Math.max(min1, min2)
                    if (min1 < min2 && max1 > max2 || min2 < min1 && max2 > max1) {
                        val mins: Float = java.lang.Math.abs(min1 - min2)
                        val maxs: Float = java.lang.Math.abs(max1 - max2)
                        o += if (mins < maxs) {
                            mins
                        } else {
                            maxs
                        }
                    }
                    if (o < overlap) {
                        overlap = o
                        // Adjusts the direction based on the number of points found
                        smallestAxisX = if (numInNormalDir >= 0) axisX else -axisX
                        smallestAxisY = if (numInNormalDir >= 0) axisY else -axisY
                    }
                }
                i += 2
            }
        }

        // Get polygon2 axes
        run {
            var i = offset2
            while (i < end2) {
                val x1 = verts2!![i]
                val y1 = verts2[i + 1]
                val x2 = verts2[(i + 2) % count2]
                val y2 = verts2[(i + 3) % count2]
                var axisX = y1 - y2
                var axisY = -(x1 - x2)
                val length = java.lang.Math.sqrt(axisX * axisX + axisY * axisY.toDouble()) as Float
                axisX /= length
                axisY /= length

                // -- Begin check for separation on this axis --//
                numInNormalDir = 0

                // Project polygon1 onto this axis
                var min1 = axisX * verts1!![0] + axisY * verts1[1]
                var max1 = min1
                run {
                    var j = offset1
                    while (j < end1) {
                        val p = axisX * verts1[j] + axisY * verts1[j + 1]
                        // Counts the number of points that are within the projected area.
                        numInNormalDir -= pointLineSide(x1, y1, x2, y2, verts1[j], verts1[j + 1])
                        if (p < min1) {
                            min1 = p
                        } else if (p > max1) {
                            max1 = p
                        }
                        j += 2
                    }
                }

                // Project polygon2 onto this axis
                var min2 = axisX * verts2[0] + axisY * verts2[1]
                var max2 = min2
                var j = offset2
                while (j < end2) {
                    val p = axisX * verts2[j] + axisY * verts2[j + 1]
                    if (p < min2) {
                        min2 = p
                    } else if (p > max2) {
                        max2 = p
                    }
                    j += 2
                }
                if (!(min1 <= min2 && max1 >= min2 || min2 <= min1 && max2 >= min1)) {
                    return false
                } else {
                    var o: Float = java.lang.Math.min(max1, max2) - java.lang.Math.max(min1, min2)
                    if (min1 < min2 && max1 > max2 || min2 < min1 && max2 > max1) {
                        val mins: Float = java.lang.Math.abs(min1 - min2)
                        val maxs: Float = java.lang.Math.abs(max1 - max2)
                        o += if (mins < maxs) {
                            mins
                        } else {
                            maxs
                        }
                    }
                    if (o < overlap) {
                        overlap = o
                        // Adjusts the direction based on the number of points found
                        smallestAxisX = if (numInNormalDir < 0) axisX else -axisX
                        smallestAxisY = if (numInNormalDir < 0) axisY else -axisY
                    }
                }
                i += 2
            }
        }
        if (mtv != null) {
            mtv.normal[smallestAxisX] = smallestAxisY
            mtv.depth = overlap
        }
        return true
    }

    /**
     * Splits the triangle by the plane. The result is stored in the SplitTriangle instance. Depending on where the triangle is
     * relative to the plane, the result can be:
     *
     *
     *  * Triangle is fully in front/behind: [SplitTriangle.front] or [SplitTriangle.back] will contain the original
     * triangle, [SplitTriangle.total] will be one.
     *  * Triangle has two vertices in front, one behind: [SplitTriangle.front] contains 2 triangles,
     * [SplitTriangle.back] contains 1 triangles, [SplitTriangle.total] will be 3.
     *  * Triangle has one vertex in front, two behind: [SplitTriangle.front] contains 1 triangle,
     * [SplitTriangle.back] contains 2 triangles, [SplitTriangle.total] will be 3.
     *
     *
     *
     * The input triangle should have the form: x, y, z, x2, y2, z2, x3, y3, z3. One can add additional attributes per vertex which
     * will be interpolated if split, such as texture coordinates or normals. Note that these additional attributes won't be
     * normalized, as might be necessary in case of normals.
     *
     * @param triangle
     * @param plane
     * @param split    output SplitTriangle
     */
    fun splitTriangle(triangle: FloatArray, plane: Plane, split: SplitTriangle) {
        val stride = triangle.size / 3
        val r1 = plane.testPoint(triangle[0], triangle[1], triangle[2]) === PlaneSide.Back
        val r2 = plane.testPoint(triangle[0 + stride], triangle[1 + stride], triangle[2 + stride]) === PlaneSide.Back
        val r3 = plane.testPoint(triangle[0 + stride * 2], triangle[1 + stride * 2],
            triangle[2 + stride * 2]) === PlaneSide.Back
        split.reset()

        // easy case, triangle is on one side (point on plane means front).
        if (r1 == r2 && r2 == r3) {
            split.total = 1
            if (r1) {
                split.numBack = 1
                java.lang.System.arraycopy(triangle, 0, split.back, 0, triangle.size)
            } else {
                split.numFront = 1
                java.lang.System.arraycopy(triangle, 0, split.front, 0, triangle.size)
            }
            return
        }

        // set number of triangles
        split.total = 3
        split.numFront = (if (r1) 0 else 1) + (if (r2) 0 else 1) + if (r3) 0 else 1
        split.numBack = split.total - split.numFront

        // hard case, split the three edges on the plane
        // determine which array to fill first, front or back, flip if we
        // cross the plane
        split.side = !r1

        // split first edge
        var first = 0
        var second = stride
        if (r1 != r2) {
            // split the edge
            splitEdge(triangle, first, second, stride, plane, split.edgeSplit, 0)

            // add first edge vertex and new vertex to current side
            split.add(triangle, first, stride)
            split.add(split.edgeSplit, 0, stride)

            // flip side and add new vertex and second edge vertex to current side
            split.side = !split.side
            split.add(split.edgeSplit, 0, stride)
        } else {
            // add both vertices
            split.add(triangle, first, stride)
        }

        // split second edge
        first = stride
        second = stride + stride
        if (r2 != r3) {
            // split the edge
            splitEdge(triangle, first, second, stride, plane, split.edgeSplit, 0)

            // add first edge vertex and new vertex to current side
            split.add(triangle, first, stride)
            split.add(split.edgeSplit, 0, stride)

            // flip side and add new vertex and second edge vertex to current side
            split.side = !split.side
            split.add(split.edgeSplit, 0, stride)
        } else {
            // add both vertices
            split.add(triangle, first, stride)
        }

        // split third edge
        first = stride + stride
        second = 0
        if (r3 != r1) {
            // split the edge
            splitEdge(triangle, first, second, stride, plane, split.edgeSplit, 0)

            // add first edge vertex and new vertex to current side
            split.add(triangle, first, stride)
            split.add(split.edgeSplit, 0, stride)

            // flip side and add new vertex and second edge vertex to current side
            split.side = !split.side
            split.add(split.edgeSplit, 0, stride)
        } else {
            // add both vertices
            split.add(triangle, first, stride)
        }

        // triangulate the side with 2 triangles
        if (split.numFront == 2) {
            java.lang.System.arraycopy(split.front, stride * 2, split.front, stride * 3, stride * 2)
            java.lang.System.arraycopy(split.front, 0, split.front, stride * 5, stride)
        } else {
            java.lang.System.arraycopy(split.back, stride * 2, split.back, stride * 3, stride * 2)
            java.lang.System.arraycopy(split.back, 0, split.back, stride * 5, stride)
        }
    }

    var intersection = Vector3()
    private fun splitEdge(vertices: FloatArray, s: Int, e: Int, stride: Int, plane: Plane, split: FloatArray, offset: Int) {
        val t = intersectLinePlane(vertices[s], vertices[s + 1], vertices[s + 2], vertices[e], vertices[e + 1],
            vertices[e + 2], plane, intersection)
        split[offset + 0] = intersection.x
        split[offset + 1] = intersection.y
        split[offset + 2] = intersection.z
        for (i in 3 until stride) {
            val a = vertices[s + i]
            val b = vertices[e + i]
            split[offset + i] = a + t * (b - a)
        }
    }

    class SplitTriangle(numAttributes: Int) {
        var front: FloatArray
        var back: FloatArray
        var edgeSplit: FloatArray
        var numFront = 0
        var numBack = 0
        var total = 0
        var side = false
        var frontOffset = 0
        var backOffset = 0
        override fun toString(): String {
            return "SplitTriangle [front=" + Arrays.toString(front).toString() + ", back=" + Arrays.toString(back).toString() + ", numFront=" + numFront
                .toString() + ", numBack=" + numBack.toString() + ", total=" + total.toString() + "]"
        }

        fun add(vertex: FloatArray?, offset: Int, stride: Int) {
            if (side) {
                java.lang.System.arraycopy(vertex, offset, front, frontOffset, stride)
                frontOffset += stride
            } else {
                java.lang.System.arraycopy(vertex, offset, back, backOffset, stride)
                backOffset += stride
            }
        }

        fun reset() {
            side = false
            frontOffset = 0
            backOffset = 0
            numFront = 0
            numBack = 0
            total = 0
        }

        /**
         * Creates a new instance, assuming numAttributes attributes per triangle vertex.
         *
         * @param numAttributes must be >= 3
         */
        init {
            front = FloatArray(numAttributes * 3 * 2)
            back = FloatArray(numAttributes * 3 * 2)
            edgeSplit = FloatArray(numAttributes)
        }
    }

    /**
     * Minimum translation required to separate two polygons.
     */
    class MinimumTranslationVector {

        /**
         * Unit length vector that indicates the direction for the separation
         */
        var normal = Vector2()

        /**
         * Distance of the translation required for the separation
         */
        var depth = 0f
    }
}
