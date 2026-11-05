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
                        val denom = normal.dot(vj - vi)
                        val t = if (abs(denom) > 1e-6f) ((w - normal.dot(vi)) / denom).coerceIn(0f, 1f) else 0.5f
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

    fun clipPolygons(list: List<CsgPolygon>, depth: Int = 0): List<CsgPolygon> {
        if (depth > 25) return list
        val p = plane ?: return list.toList()
        var f = mutableListOf<CsgPolygon>()
        var b = mutableListOf<CsgPolygon>()

        for (poly in list) {
            p.splitPolygon(poly, f, b, f, b)
        }

        f = (front?.clipPolygons(f, depth + 1) ?: f).toMutableList()
        b = (back?.clipPolygons(b, depth + 1) ?: mutableListOf()).toMutableList()

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

    fun build(list: List<CsgPolygon>, depth: Int = 0) {
        if (list.isEmpty()) return
        if (depth > 40) {
            polygons.addAll(list)
            return
        }
        if (plane == null) plane = list[0].plane
        val p = plane ?: return

        val f = mutableListOf<CsgPolygon>()
        val b = mutableListOf<CsgPolygon>()

        for (poly in list) {
            p.splitPolygon(poly, polygons, polygons, f, b)
        }

        if (f.isNotEmpty()) {
            if (front == null) front = CsgNode()
            front?.build(f, depth + 1)
        }
        if (b.isNotEmpty()) {
            if (back == null) back = CsgNode()
            back?.build(b, depth + 1)
        }
    }
}

/**
 * BoundingBox für schnelle Kollisions- und Intersections-Prüfungen vor aufwändigen BSP-Operationen
 */
data class BoundingBox(
    val min: Vector3,
    val max: Vector3
) {
    fun intersects(other: BoundingBox, margin: Float = 1e-3f): Boolean {
        return (min.x - margin <= other.max.x && max.x + margin >= other.min.x) &&
               (min.y - margin <= other.max.y && max.y + margin >= other.min.y) &&
               (min.z - margin <= other.max.z && max.z + margin >= other.min.z)
    }

    companion object {
        fun fromPolygons(polygons: List<CsgPolygon>): BoundingBox {
            if (polygons.isEmpty()) {
                return BoundingBox(Vector3.ZERO, Vector3.ZERO)
            }
            var minX = Float.POSITIVE_INFINITY
            var minY = Float.POSITIVE_INFINITY
            var minZ = Float.POSITIVE_INFINITY
            var maxX = Float.NEGATIVE_INFINITY
            var maxY = Float.NEGATIVE_INFINITY
            var maxZ = Float.NEGATIVE_INFINITY

            for (p in polygons) {
                for (v in p.vertices) {
                    if (v.x < minX) minX = v.x
                    if (v.y < minY) minY = v.y
                    if (v.z < minZ) minZ = v.z
                    if (v.x > maxX) maxX = v.x
                    if (v.y > maxY) maxY = v.y
                    if (v.z > maxZ) maxZ = v.z
                }
            }
            return BoundingBox(Vector3(minX, minY, minZ), Vector3(maxX, maxY, maxZ))
        }
    }
}

/**
 * CSG Solid - Repräsentiert einen 3D-Körper und bietet union, difference, intersection
 */
class CsgSolid(val polygons: List<CsgPolygon>) {

    val bounds: BoundingBox by lazy { BoundingBox.fromPolygons(polygons) }

    fun union(other: CsgSolid): CsgSolid {
        if (polygons.isEmpty()) return other
        if (other.polygons.isEmpty()) return this
        if (!bounds.intersects(other.bounds)) {
            return CsgSolid(polygons + other.polygons)
        }
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
        if (polygons.isEmpty()) return this
        if (other.polygons.isEmpty()) return this
        if (!bounds.intersects(other.bounds)) {
            return this
        }
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
        if (polygons.isEmpty() || other.polygons.isEmpty()) return CsgSolid(emptyList())
        if (!bounds.intersects(other.bounds)) {
            return CsgSolid(emptyList())
        }
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

    fun minkowski(other: CsgSolid): CsgSolid = minkowski(this, other)

    fun hull(other: CsgSolid): CsgSolid = hull(listOf(this, other))

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

            val seg = segments.coerceIn(3, 36)
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

        fun polygonPrism(
            points: List<Vector3>,
            height: Float = 1f,
            color: Vector3 = Vector3(0.97f, 0.74f, 0.22f)
        ): CsgSolid {
            if (points.size < 3) return CsgSolid(emptyList())
            val n = points.size

            // Prüfe Orientierung in 2D (XY)
            var area = 0.0
            for (i in 0 until n) {
                val p0 = points[i]
                val p1 = points[(i + 1) % n]
                area += (p0.x * p1.y - p1.x * p0.y)
            }
            val pts = if (area < 0) points.asReversed() else points
            val polys = mutableListOf<CsgPolygon>()

            // Boden: Normale nach unten (-Z) -> invertierte Punktreihenfolge
            val bottomVerts = pts.asReversed().map { Vector3(it.x, it.y, 0f) }
            polys.add(CsgPolygon(bottomVerts, color = color))

            // Deckel: Normale nach oben (+Z) -> Punktreihenfolge beibehalten
            val topVerts = pts.map { Vector3(it.x, it.y, height) }
            polys.add(CsgPolygon(topVerts, color = color))

            // Seitenwände: Quad für jede Kante
            for (i in 0 until n) {
                val p0 = pts[i]
                val p1 = pts[(i + 1) % n]
                val side = listOf(
                    Vector3(p0.x, p0.y, 0f),
                    Vector3(p1.x, p1.y, 0f),
                    Vector3(p1.x, p1.y, height),
                    Vector3(p0.x, p0.y, height)
                )
                polys.add(CsgPolygon(side, color = color))
            }
            return CsgSolid(polys)
        }

        fun polygon2d(points: List<Vector3>, color: Vector3 = Vector3(0.97f, 0.74f, 0.22f)): CsgSolid {
            return polygonPrism(points, 1f, color)
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

        fun hull(solids: List<CsgSolid>, color: Vector3 = Vector3(0.97f, 0.74f, 0.22f)): CsgSolid {
            if (solids.isEmpty()) return CsgSolid(emptyList())
            if (solids.size == 1) return solids[0]
            val allVerts = mutableListOf<Vector3>()
            for (s in solids) {
                for (p in s.polygons) {
                    allVerts.addAll(p.vertices)
                }
            }
            return convexHull(allVerts, color)
        }

        fun minkowski(a: CsgSolid, b: CsgSolid, color: Vector3 = Vector3(0.97f, 0.74f, 0.22f)): CsgSolid {
            if (a.polygons.isEmpty()) return b
            if (b.polygons.isEmpty()) return a

            val vertsA = a.polygons.flatMap { it.vertices }.distinct()
            var vertsB = b.polygons.flatMap { it.vertices }.distinct()

            if (vertsA.size * vertsB.size > 800 && vertsB.size > 32) {
                val step = (vertsB.size / 32).coerceAtLeast(1)
                vertsB = vertsB.filterIndexed { index, _ -> index % step == 0 }
            }

            val sumPoints = ArrayList<Vector3>(vertsA.size * vertsB.size)
            for (va in vertsA) {
                for (vb in vertsB) {
                    sumPoints.add(va + vb)
                }
            }
            return convexHull(sumPoints, color)
        }

        fun convexHull(points: List<Vector3>, color: Vector3 = Vector3(0.97f, 0.74f, 0.22f)): CsgSolid {
            if (points.size < 4) return CsgSolid(emptyList())

            val unique = mutableListOf<Vector3>()
            for (p in points) {
                if (unique.none { (it - p).lengthSquared() < 1e-6f }) {
                    unique.add(p)
                }
            }
            if (unique.size < 4) return CsgSolid(emptyList())

            val sampledUnique = if (unique.size > 48) {
                val extremes = LinkedHashSet<Vector3>()
                extremes.add(unique.minByOrNull { it.x }!!)
                extremes.add(unique.maxByOrNull { it.x }!!)
                extremes.add(unique.minByOrNull { it.y }!!)
                extremes.add(unique.maxByOrNull { it.y }!!)
                extremes.add(unique.minByOrNull { it.z }!!)
                extremes.add(unique.maxByOrNull { it.z }!!)
                val step = (unique.size / 32).coerceAtLeast(1)
                for (i in unique.indices step step) {
                    extremes.add(unique[i])
                }
                extremes.toList()
            } else {
                unique
            }

            val p0 = sampledUnique.minByOrNull { it.x }!!
            val p1 = sampledUnique.maxByOrNull { (it - p0).lengthSquared() }!!
            val lineDir = (p1 - p0).normalized()
            val p2 = sampledUnique.maxByOrNull { p ->
                val v = p - p0
                (v - lineDir * v.dot(lineDir)).lengthSquared()
            }!!
            val normal0 = (p1 - p0).cross(p2 - p0).normalized()
            val p3 = sampledUnique.maxByOrNull { abs((it - p0).dot(normal0)) }!!

            val distP3 = abs((p3 - p0).dot(normal0))
            if (distP3 < 1e-4f) {
                return makeCoplanarHull(sampledUnique, color)
            }

            data class Tri(val a: Vector3, val b: Vector3, val c: Vector3) {
                val normal: Vector3 = (b - a).cross(c - a).normalized()
                val d: Float = normal.dot(a)
                fun canSee(p: Vector3): Boolean = (normal.dot(p) - d) > 1e-4f
            }

            val center = (p0 + p1 + p2 + p3) * 0.25f
            val faces = mutableListOf<Tri>()

            fun addOrientedTri(a: Vector3, b: Vector3, c: Vector3) {
                val n = (b - a).cross(c - a).normalized()
                if (n.dot(center - a) > 0) {
                    faces.add(Tri(a, c, b))
                } else {
                    faces.add(Tri(a, b, c))
                }
            }

            addOrientedTri(p0, p1, p2)
            addOrientedTri(p0, p1, p3)
            addOrientedTri(p0, p2, p3)
            addOrientedTri(p1, p2, p3)

            data class UnorderedEdge(val u: Vector3, val v: Vector3)

            for (p in sampledUnique) {
                if (p == p0 || p == p1 || p == p2 || p == p3) continue

                val visible = faces.filter { it.canSee(p) }
                if (visible.isEmpty()) continue

                val edgeCount = mutableMapOf<UnorderedEdge, Int>()
                val directedEdges = mutableListOf<Pair<Vector3, Vector3>>()

                for (f in visible) {
                    val edges = listOf(Pair(f.a, f.b), Pair(f.b, f.c), Pair(f.c, f.a))
                    for (e in edges) {
                        val key = UnorderedEdge(e.first, e.second)
                        edgeCount[key] = (edgeCount[key] ?: 0) + 1
                        directedEdges.add(e)
                    }
                }

                val horizon = directedEdges.filter { e ->
                    edgeCount[UnorderedEdge(e.first, e.second)] == 1
                }

                faces.removeAll(visible)
                for (e in horizon) {
                    addOrientedTri(e.first, e.second, p)
                }
            }

            val polygons = faces.map {
                CsgPolygon(listOf(it.a, it.b, it.c), color = color)
            }
            return CsgSolid(polygons)
        }

        private fun makeCoplanarHull(points: List<Vector3>, color: Vector3): CsgSolid {
            val sorted = points.sortedWith(compareBy({ it.x }, { it.y }))
            if (sorted.size < 3) return CsgSolid(emptyList())

            val lower = mutableListOf<Vector3>()
            for (p in sorted) {
                while (lower.size >= 2) {
                    val a = lower[lower.size - 2]
                    val b = lower[lower.size - 1]
                    if ((b.x - a.x) * (p.y - a.y) - (b.y - a.y) * (p.x - a.x) <= 0) {
                        lower.removeAt(lower.size - 1)
                    } else break
                }
                lower.add(p)
            }

            val upper = mutableListOf<Vector3>()
            for (p in sorted.reversed()) {
                while (upper.size >= 2) {
                    val a = upper[upper.size - 2]
                    val b = upper[upper.size - 1]
                    if ((b.x - a.x) * (p.y - a.y) - (b.y - a.y) * (p.x - a.x) <= 0) {
                        upper.removeAt(upper.size - 1)
                    } else break
                }
                upper.add(p)
            }

            lower.removeAt(lower.size - 1)
            upper.removeAt(upper.size - 1)
            val ring2d = lower + upper
            if (ring2d.size < 3) return CsgSolid(emptyList())

            val minZ = points.minOf { it.z }
            val maxZ = points.maxOf { it.z }
            val h = if (maxZ - minZ > 1e-4f) maxZ - minZ else 1f
            val baseZ = if (maxZ - minZ > 1e-4f) minZ else 0f

            val bRing = ring2d.map { Vector3(it.x, it.y, baseZ) }
            val tRing = ring2d.map { Vector3(it.x, it.y, baseZ + h) }

            val polys = mutableListOf<CsgPolygon>()
            polys.add(CsgPolygon(bRing.reversed(), color = color))
            polys.add(CsgPolygon(tRing, color = color))

            val n = ring2d.size
            for (i in 0 until n) {
                val next = (i + 1) % n
                polys.add(CsgPolygon(listOf(bRing[i], bRing[next], tRing[next], tRing[i]), color = color))
            }
            return CsgSolid(polys)
        }
    }
}
