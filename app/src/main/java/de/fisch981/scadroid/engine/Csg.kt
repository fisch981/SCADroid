package de.fisch981.scadroid.engine

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * 3D Ebene für BSP-Clipping (Constructive Solid Geometry)
 */
class Plane(
    val normal: Vector3,
    val w: Float
) {
    fun flipped(): Plane = Plane(-normal, -w)

    fun splitPolygon(
        polygon: CsgPolygon,
        coplanarFront: MutableList<CsgPolygon>,
        coplanarBack: MutableList<CsgPolygon>,
        front: MutableList<CsgPolygon>,
        back: MutableList<CsgPolygon>
    ) {
        val EPSILON = 1e-4f
        var polygonType = 0
        val types = IntArray(polygon.vertices.size)

        for (i in polygon.vertices.indices) {
            val t = normal.dot(polygon.vertices[i]) - w
            val type = when {
                t < -EPSILON -> 2 // BACK
                t > EPSILON -> 1  // FRONT
                else -> 0         // COPLANAR
            }
            polygonType = polygonType or type
            types[i] = type
        }

        when (polygonType) {
            0 -> {
                if (normal.dot(polygon.plane.normal) > 0) {
                    coplanarFront.add(polygon)
                } else {
                    coplanarBack.add(polygon)
                }
            }
            1 -> front.add(polygon)
            2 -> back.add(polygon)
            3 -> {
                // Split polygon across plane
                val f = mutableListOf<Vector3>()
                val b = mutableListOf<Vector3>()
                for (i in polygon.vertices.indices) {
                    val j = (i + 1) % polygon.vertices.size
                    val ti = types[i]
                    val tj = types[j]
                    val vi = polygon.vertices[i]
                    val vj = polygon.vertices[j]

                    if (ti != 2) f.add(vi)
                    if (ti != 1) b.add(vi)

                    if ((ti or tj) == 3) {
                        val t = (w - normal.dot(vi)) / normal.dot(vj - vi)
                        val v = vi + (vj - vi) * t
                        f.add(v)
                        b.add(v)
                    }
                }
                if (f.size >= 3) front.add(CsgPolygon(f, polygon.plane, polygon.color))
                if (b.size >= 3) back.add(CsgPolygon(b, polygon.plane, polygon.color))
            }
        }
    }

    companion object {
        fun fromPoints(a: Vector3, b: Vector3, c: Vector3): Plane {
            val n = (b - a).cross(c - a).normalized()
            return Plane(n, n.dot(a))
        }
    }
}

/**
 * Konvexes 3D-Polygon
 */
class CsgPolygon(
    val vertices: List<Vector3>,
    val plane: Plane = Plane.fromPoints(vertices[0], vertices[1], vertices[2]),
    val color: Vector3 = Vector3(0.97f, 0.74f, 0.22f) // OpenSCAD Gold
) {
    fun flipped(): CsgPolygon {
        return CsgPolygon(vertices.reversed(), plane.flipped(), color)
    }
}

/**
 * BSP-Knoten für CSG-Operationen
 */
class CsgNode(polygons: List<CsgPolygon>? = null) {
    var plane: Plane? = null
    var front: CsgNode? = null
    var back: CsgNode? = null
    var polygons = mutableListOf<CsgPolygon>()

    init {
        if (polygons != null && polygons.isNotEmpty()) {
            build(polygons)
        }
    }

    fun invert() {
        for (i in polygons.indices) {
            polygons[i] = polygons[i].flipped()
        }
        plane = plane?.flipped()
        val temp = front
        front = back
        back = temp
        front?.invert()
        back?.invert()
    }

    fun clipPolygons(list: List<CsgPolygon>): List<CsgPolygon> {
        val p = plane ?: return list.toList()
        var f = mutableListOf<CsgPolygon>()
        var b = mutableListOf<CsgPolygon>()

        for (poly in list) {
            p.splitPolygon(poly, f, b, f, b)
        }

        f = (front?.clipPolygons(f) ?: f).toMutableList()
        b = (back?.clipPolygons(b) ?: mutableListOf()).toMutableList()

        f.addAll(b)
        return f
    }

    fun clipTo(bsp: CsgNode) {
        polygons = bsp.clipPolygons(polygons).toMutableList()
        front?.clipTo(bsp)
        back?.clipTo(bsp)
    }

    fun allPolygons(): List<CsgPolygon> {
        val list = mutableListOf<CsgPolygon>()
        list.addAll(polygons)
        front?.let { list.addAll(it.allPolygons()) }
        back?.let { list.addAll(it.allPolygons()) }
        return list
    }

    fun build(list: List<CsgPolygon>) {
        if (list.isEmpty()) return
        if (plane == null) plane = list[0].plane
        val p = plane ?: return

        val f = mutableListOf<CsgPolygon>()
        val b = mutableListOf<CsgPolygon>()

        for (poly in list) {
            p.splitPolygon(poly, polygons, polygons, f, b)
        }

        if (f.isNotEmpty()) {
            if (front == null) front = CsgNode()
            front?.build(f)
        }
        if (b.isNotEmpty()) {
            if (back == null) back = CsgNode()
            back?.build(b)
        }
    }
}

/**
 * CSG Solid - Repräsentiert einen 3D-Körper und bietet union, difference, intersection
 */
class CsgSolid(val polygons: List<CsgPolygon>) {

    fun union(other: CsgSolid): CsgSolid {
        val a = CsgNode(polygons)
        val b = CsgNode(other.polygons)
        a.clipTo(b)
        b.clipTo(a)
        b.invert()
        b.clipTo(a)
        b.invert()
        a.build(b.allPolygons())
        return CsgSolid(a.allPolygons())
    }

    fun difference(other: CsgSolid): CsgSolid {
        val a = CsgNode(polygons)
        val b = CsgNode(other.polygons)
        a.invert()
        a.clipTo(b)
        b.clipTo(a)
        b.invert()
        b.clipTo(a)
        b.invert()
        a.build(b.allPolygons())
        a.invert()
        return CsgSolid(a.allPolygons())
    }

    fun intersection(other: CsgSolid): CsgSolid {
        val a = CsgNode(polygons)
        val b = CsgNode(other.polygons)
        a.invert()
        b.clipTo(a)
        b.invert()
        a.clipTo(b)
        b.clipTo(a)
        a.build(b.allPolygons())
        a.invert()
        return CsgSolid(a.allPolygons())
    }

    fun translate(v: Vector3): CsgSolid {
        val newPolys = polygons.map { p ->
            CsgPolygon(p.vertices.map { it + v }, color = p.color)
        }
        return CsgSolid(newPolys)
    }

    fun rotate(degX: Float, degY: Float, degZ: Float): CsgSolid {
        val newPolys = polygons.map { p ->
            CsgPolygon(p.vertices.map { it.rotated(degX, degY, degZ) }, color = p.color)
        }
        return CsgSolid(newPolys)
    }

    fun scale(s: Vector3): CsgSolid {
        val newPolys = polygons.map { p ->
            CsgPolygon(p.vertices.map { Vector3(it.x * s.x, it.y * s.y, it.z * s.z) }, color = p.color)
        }
        return CsgSolid(newPolys)
    }

    fun mirror(normal: Vector3): CsgSolid {
        val len = normal.length()
        if (len < 1e-6f) return this
        val n = normal.normalized()
        val newPolys = polygons.map { p ->
            val reflected = p.vertices.map { v ->
                v - n * (2f * v.dot(n))
            }
            CsgPolygon(reflected.reversed(), color = p.color)
        }
        return CsgSolid(newPolys)
    }

    fun setColor(color: Vector3): CsgSolid {
        val newPolys = polygons.map { p ->
            CsgPolygon(p.vertices, p.plane, color)
        }
        return CsgSolid(newPolys)
    }

    companion object {
        fun cube(size: Vector3, center: Boolean = false, color: Vector3 = Vector3(0.97f, 0.74f, 0.22f)): CsgSolid {
            val minX = if (center) -size.x / 2f else 0f
            val maxX = if (center) size.x / 2f else size.x
            val minY = if (center) -size.y / 2f else 0f
            val maxY = if (center) size.y / 2f else size.y
            val minZ = if (center) -size.z / 2f else 0f
            val maxZ = if (center) size.z / 2f else size.z

            val v0 = Vector3(minX, minY, minZ)
            val v1 = Vector3(maxX, minY, minZ)
            val v2 = Vector3(maxX, maxY, minZ)
            val v3 = Vector3(minX, maxY, minZ)
            val v4 = Vector3(minX, minY, maxZ)
            val v5 = Vector3(maxX, minY, maxZ)
            val v6 = Vector3(maxX, maxY, maxZ)
            val v7 = Vector3(minX, maxY, maxZ)

            val polys = listOf(
                // Bottom (-Z)
                CsgPolygon(listOf(v3, v2, v1, v0), color = color),
                // Top (+Z)
                CsgPolygon(listOf(v4, v5, v6, v7), color = color),
                // Front (-Y)
                CsgPolygon(listOf(v0, v1, v5, v4), color = color),
                // Back (+Y)
                CsgPolygon(listOf(v2, v3, v7, v6), color = color),
                // Left (-X)
                CsgPolygon(listOf(v3, v0, v4, v7), color = color),
                // Right (+X)
                CsgPolygon(listOf(v1, v2, v6, v5), color = color)
            )
            return CsgSolid(polys)
        }

        fun cylinder(
            h: Float,
            r1: Float,
            r2: Float,
            segments: Int = 32,
            center: Boolean = false,
            color: Vector3 = Vector3(0.97f, 0.74f, 0.22f)
        ): CsgSolid {
            val z0 = if (center) -h / 2f else 0f
            val z1 = if (center) h / 2f else h

            val polys = mutableListOf<CsgPolygon>()
            val bottomVerts = mutableListOf<Vector3>()
            val topVerts = mutableListOf<Vector3>()

            val seg = segments.coerceIn(3, 128)
            for (i in 0 until seg) {
                val theta = (i.toDouble() / seg * 2.0 * Math.PI).toFloat()
                val nextTheta = (((i + 1) % seg).toDouble() / seg * 2.0 * Math.PI).toFloat()

                val x0 = cos(theta) * r1
                val y0 = sin(theta) * r1
                val x1 = cos(nextTheta) * r1
                val y1 = sin(nextTheta) * r1

                val tx0 = cos(theta) * r2
                val ty0 = sin(theta) * r2
                val tx1 = cos(nextTheta) * r2
                val ty1 = sin(nextTheta) * r2

                val b0 = Vector3(x0, y0, z0)
                val b1 = Vector3(x1, y1, z0)
                val t0 = Vector3(tx0, ty0, z1)
                val t1 = Vector3(tx1, ty1, z1)

                if (r2 <= 1e-4f) {
                    // Kegelspitze oben: Dreieck mit Spitze bei t0
                    polys.add(CsgPolygon(listOf(b0, b1, t0), color = color))
                } else if (r1 <= 1e-4f) {
                    // Kegelspitze unten: Dreieck mit Spitze bei b0
                    polys.add(CsgPolygon(listOf(b0, t1, t0), color = color))
                } else {
                    // Zylinder / Kegelstumpf: Viereck
                    polys.add(CsgPolygon(listOf(b0, b1, t1, t0), color = color))
                }

                bottomVerts.add(b0)
                topVerts.add(t0)
            }

            // Bottom cap (reversed winding)
            if (r1 > 1e-4f) {
                polys.add(CsgPolygon(bottomVerts.reversed(), color = color))
            }
            // Top cap
            if (r2 > 1e-4f) {
                polys.add(CsgPolygon(topVerts, color = color))
            }

            return CsgSolid(polys)
        }

        fun sphere(
            r: Float,
            slices: Int = 16,
            stacks: Int = 12,
            color: Vector3 = Vector3(0.97f, 0.74f, 0.22f)
        ): CsgSolid {
            val polys = mutableListOf<CsgPolygon>()
            val sl = slices.coerceIn(8, 32)
            val st = stacks.coerceIn(6, 24)

            for (i in 0 until st) {
                val phi0 = Math.PI * (i.toDouble() / st - 0.5)
                val phi1 = Math.PI * ((i + 1).toDouble() / st - 0.5)

                val z0 = (sin(phi0) * r).toFloat()
                val z1 = (sin(phi1) * r).toFloat()
                val r0 = (cos(phi0) * r).toFloat()
                val r1 = (cos(phi1) * r).toFloat()

                for (j in 0 until sl) {
                    val theta0 = 2.0 * Math.PI * (j.toDouble() / sl)
                    val theta1 = 2.0 * Math.PI * ((j + 1).toDouble() / sl)

                    val p0 = Vector3((cos(theta0) * r0).toFloat(), (sin(theta0) * r0).toFloat(), z0)
                    val p1 = Vector3((cos(theta1) * r0).toFloat(), (sin(theta1) * r0).toFloat(), z0)
                    val p2 = Vector3((cos(theta1) * r1).toFloat(), (sin(theta1) * r1).toFloat(), z1)
                    val p3 = Vector3((cos(theta0) * r1).toFloat(), (sin(theta0) * r1).toFloat(), z1)

                    if (i == 0) {
                        polys.add(CsgPolygon(listOf(p0, p2, p3), color = color))
                    } else if (i == st - 1) {
                        polys.add(CsgPolygon(listOf(p0, p1, p2), color = color))
                    } else {
                        polys.add(CsgPolygon(listOf(p0, p1, p2, p3), color = color))
                    }
                }
            }
            return CsgSolid(polys)
        }
    }
}
