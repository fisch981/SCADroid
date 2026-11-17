package de.fisch981.scadroid.engine

import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.max
import kotlin.math.min

class TriangleMesh(
    val vertexBuffer: FloatBuffer,
    val normalBuffer: FloatBuffer,
    val colorBuffer: FloatBuffer,
    val vertexCount: Int,
    val triangleCount: Int,
    val minBound: Vector3,
    val maxBound: Vector3,
    val opaqueVertexCount: Int = vertexCount,
    val exportTriangleCount: Int = if (opaqueVertexCount >= 3) opaqueVertexCount / 3 else triangleCount
) {
    val center: Vector3 = (minBound + maxBound) * 0.5f
    val boundingRadius: Float = max((maxBound - minBound).length() * 0.5f, 10f)

    /**
     * Schreibt das Mesh im binären STL-Format (Standard für 3D-Druck und Slicer).
     */
    fun exportBinaryStl(out: java.io.OutputStream) {
        val header = ByteArray(80)
        "SCADroid Mobile OpenSCAD Exporter".toByteArray().copyInto(header)
        out.write(header)

        val exportTriCount = if (exportTriangleCount in 1..triangleCount) exportTriangleCount else triangleCount
        val countBuf = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)
        countBuf.putInt(exportTriCount)
        out.write(countBuf.array())

        val vBuf = vertexBuffer.duplicate()
        val nBuf = normalBuffer.duplicate()
        vBuf.position(0)
        nBuf.position(0)

        val triBuf = ByteBuffer.allocate(50).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until exportTriCount) {
            triBuf.clear()
            // Normal
            val nx = if (nBuf.hasRemaining()) nBuf.get() else 0f
            val ny = if (nBuf.hasRemaining()) nBuf.get() else 0f
            val nz = if (nBuf.hasRemaining()) nBuf.get() else 1f
            // Überspringe die anderen 2 Vertex-Normalen falls pro Vertex gespeichert
            if (nBuf.remaining() >= 6) {
                nBuf.get(); nBuf.get(); nBuf.get()
                nBuf.get(); nBuf.get(); nBuf.get()
            }

            triBuf.putFloat(nx)
            triBuf.putFloat(ny)
            triBuf.putFloat(nz)

            // 3 Vertices
            for (v in 0 until 3) {
                triBuf.putFloat(if (vBuf.hasRemaining()) vBuf.get() else 0f)
                triBuf.putFloat(if (vBuf.hasRemaining()) vBuf.get() else 0f)
                triBuf.putFloat(if (vBuf.hasRemaining()) vBuf.get() else 0f)
            }
            // Attribute byte count
            triBuf.putShort(0)
            out.write(triBuf.array())
        }
        out.flush()
    }

    fun exportBinaryStl(file: File) {
        FileOutputStream(file).use { exportBinaryStl(it) }
    }

    companion object {
        val EMPTY = fromSolid(CsgSolid(emptyList()))

        private class ParsedTri(
            val nx: Float, val ny: Float, val nz: Float,
            val x0: Float, val y0: Float, val z0: Float,
            val x1: Float, val y1: Float, val z1: Float,
            val x2: Float, val y2: Float, val z2: Float,
            val r: Float, val g: Float, val b: Float, val a: Float
        )

        private fun hasScadCol1Trailer(bytes: ByteArray, triCount: Int): Boolean {
            val trailerOffset = 84 + triCount * 50
            if (bytes.size < trailerOffset + 8 + triCount * 4) return false
            val magic = "SCADCOL1"
            for (k in 0 until 8) {
                if (bytes[trailerOffset + k] != magic[k].code.toByte()) return false
            }
            return true
        }

        fun fromBinaryStl(bytes: ByteArray, color: Vector3 = Vector3(0.97f, 0.74f, 0.22f)): TriangleMesh {
            if (bytes.size < 84) return EMPTY
            val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            val headerTriCount = buf.getInt(80)
            val maxPossibleTris = (bytes.size - 84) / 50
            val triCount = if (headerTriCount in 1..maxPossibleTris) headerTriCount else maxPossibleTris
            if (triCount <= 0) return EMPTY

            val hasColorTrailer = hasScadCol1Trailer(bytes, triCount)
            val trailerDataStart = 84 + triCount * 50 + 8

            var minX = Float.MAX_VALUE
            var minY = Float.MAX_VALUE
            var minZ = Float.MAX_VALUE
            var maxX = -Float.MAX_VALUE
            var maxY = -Float.MAX_VALUE
            var maxZ = -Float.MAX_VALUE

            val opaqueList = ArrayList<ParsedTri>(triCount)
            val translucentColorList = ArrayList<ParsedTri>()
            val highlightList = ArrayList<ParsedTri>()
            val backgroundList = ArrayList<ParsedTri>()

            var offset = 84
            for (i in 0 until triCount) {
                var nx = buf.getFloat(offset)
                var ny = buf.getFloat(offset + 4)
                var nz = buf.getFloat(offset + 8)

                val x0 = buf.getFloat(offset + 12)
                val y0 = buf.getFloat(offset + 16)
                val z0 = buf.getFloat(offset + 20)

                val x1 = buf.getFloat(offset + 24)
                val y1 = buf.getFloat(offset + 28)
                val z1 = buf.getFloat(offset + 32)

                val x2 = buf.getFloat(offset + 36)
                val y2 = buf.getFloat(offset + 40)
                val z2 = buf.getFloat(offset + 44)

                val attr = buf.getShort(offset + 48).toInt() and 0xFFFF
                val r: Float
                val g: Float
                val b: Float
                val a: Float
                if (hasColorTrailer) {
                    val cOff = trailerDataStart + i * 4
                    r = (bytes[cOff].toInt() and 0xFF) / 255f
                    g = (bytes[cOff + 1].toInt() and 0xFF) / 255f
                    b = (bytes[cOff + 2].toInt() and 0xFF) / 255f
                    a = (bytes[cOff + 3].toInt() and 0xFF) / 255f
                } else {
                    when (attr) {
                        1 -> {
                            // OpenSCAD '#' Highlight / Debug Modifier (Halbtransparentes Rot/Pink)
                            r = 1.0f; g = 0.35f; b = 0.35f; a = 0.40f
                        }
                        2 -> {
                            // OpenSCAD '%' Background / Transparent Modifier (Halbtransparentes Grau)
                            r = 0.75f; g = 0.75f; b = 0.78f; a = 0.28f
                        }
                        else -> {
                            r = color.x; g = color.y; b = color.z; a = 1.0f
                        }
                    }
                }

                if (nx * nx + ny * ny + nz * nz < 1e-6f) {
                    val ux = x1 - x0
                    val uy = y1 - y0
                    val uz = z1 - z0
                    val vx = x2 - x0
                    val vy = y2 - y0
                    val vz = z2 - z0
                    val cx = uy * vz - uz * vy
                    val cy = uz * vx - ux * vz
                    val cz = ux * vy - uy * vx
                    val len = kotlin.math.sqrt(cx * cx + cy * cy + cz * cz)
                    if (len > 1e-8f) {
                        nx = cx / len
                        ny = cy / len
                        nz = cz / len
                    } else {
                        nx = 0f; ny = 0f; nz = 1f
                    }
                }

                minX = min(minX, min(x0, min(x1, x2)))
                minY = min(minY, min(y0, min(y1, y2)))
                minZ = min(minZ, min(z0, min(z1, z2)))
                maxX = max(maxX, max(x0, max(x1, x2)))
                maxY = max(maxY, max(y0, max(y1, y2)))
                maxZ = max(maxZ, max(z0, max(z1, z2)))

                val parsedTri = ParsedTri(nx, ny, nz, x0, y0, z0, x1, y1, z1, x2, y2, z2, r, g, b, a)
                when {
                    attr == 2 -> backgroundList.add(parsedTri)
                    attr == 1 -> highlightList.add(parsedTri)
                    a < 0.995f -> translucentColorList.add(parsedTri)
                    else -> opaqueList.add(parsedTri)
                }

                offset += 50
            }

            val totalVertices = triCount * 3
            val vBuf = ByteBuffer.allocateDirect(totalVertices * 3 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
            val nBuf = ByteBuffer.allocateDirect(totalVertices * 3 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
            val cBuf = ByteBuffer.allocateDirect(totalVertices * 4 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()

            val orderedGroups = listOf(opaqueList, translucentColorList, highlightList, backgroundList)
            for (group in orderedGroups) {
                for (tri in group) {
                    vBuf.put(tri.x0); vBuf.put(tri.y0); vBuf.put(tri.z0)
                    vBuf.put(tri.x1); vBuf.put(tri.y1); vBuf.put(tri.z1)
                    vBuf.put(tri.x2); vBuf.put(tri.y2); vBuf.put(tri.z2)

                    nBuf.put(tri.nx); nBuf.put(tri.ny); nBuf.put(tri.nz)
                    nBuf.put(tri.nx); nBuf.put(tri.ny); nBuf.put(tri.nz)
                    nBuf.put(tri.nx); nBuf.put(tri.ny); nBuf.put(tri.nz)

                    cBuf.put(tri.r); cBuf.put(tri.g); cBuf.put(tri.b); cBuf.put(tri.a)
                    cBuf.put(tri.r); cBuf.put(tri.g); cBuf.put(tri.b); cBuf.put(tri.a)
                    cBuf.put(tri.r); cBuf.put(tri.g); cBuf.put(tri.b); cBuf.put(tri.a)
                }
            }

            vBuf.position(0)
            nBuf.position(0)
            cBuf.position(0)

            val opaqueVerts = opaqueList.size * 3
            val exportTris = if (opaqueList.size + translucentColorList.size > 0) {
                opaqueList.size + translucentColorList.size
            } else if (highlightList.isNotEmpty()) {
                highlightList.size
            } else {
                triCount
            }

            return TriangleMesh(
                vertexBuffer = vBuf,
                normalBuffer = nBuf,
                colorBuffer = cBuf,
                vertexCount = totalVertices,
                triangleCount = triCount,
                minBound = Vector3(minX, minY, minZ),
                maxBound = Vector3(maxX, maxY, maxZ),
                opaqueVertexCount = opaqueVerts,
                exportTriangleCount = exportTris
            )
        }

        fun fromStlString(stl: String, color: Vector3 = Vector3(0.97f, 0.74f, 0.22f)): TriangleMesh {
            val lines = stl.lineSequence()
            val vertices = ArrayList<Vector3>(10000)
            val normals = ArrayList<Vector3>(10000)

            var curNormal = Vector3.UNIT_Z
            var minX = Float.MAX_VALUE
            var minY = Float.MAX_VALUE
            var minZ = Float.MAX_VALUE
            var maxX = -Float.MAX_VALUE
            var maxY = -Float.MAX_VALUE
            var maxZ = -Float.MAX_VALUE

            for (rawLine in lines) {
                val line = rawLine.trim()
                if (line.startsWith("facet normal")) {
                    val parts = line.split("\\s+".toRegex())
                    if (parts.size >= 4) {
                        val nx = parts[2].toFloatOrNull() ?: 0f
                        val ny = parts[3].toFloatOrNull() ?: 0f
                        val nz = parts[4].toFloatOrNull() ?: 1f
                        curNormal = Vector3(nx, ny, nz)
                    }
                } else if (line.startsWith("vertex")) {
                    val parts = line.split("\\s+".toRegex())
                    if (parts.size >= 4) {
                        val x = parts[1].toFloatOrNull() ?: 0f
                        val y = parts[2].toFloatOrNull() ?: 0f
                        val z = parts[3].toFloatOrNull() ?: 0f
                        vertices.add(Vector3(x, y, z))
                        normals.add(curNormal)

                        if (x < minX) minX = x
                        if (y < minY) minY = y
                        if (z < minZ) minZ = z
                        if (x > maxX) maxX = x
                        if (y > maxY) maxY = y
                        if (z > maxZ) maxZ = z
                    }
                }
            }

            if (vertices.isEmpty()) {
                return EMPTY
            }

            val totalVertices = (vertices.size / 3) * 3
            val vBuf = ByteBuffer.allocateDirect(totalVertices * 3 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
            val nBuf = ByteBuffer.allocateDirect(totalVertices * 3 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
            val cBuf = ByteBuffer.allocateDirect(totalVertices * 4 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()

            for (i in 0 until totalVertices) {
                val v = vertices[i]
                vBuf.put(v.x)
                vBuf.put(v.y)
                vBuf.put(v.z)

                val n = normals[i]
                nBuf.put(n.x)
                nBuf.put(n.y)
                nBuf.put(n.z)

                cBuf.put(color.x)
                cBuf.put(color.y)
                cBuf.put(color.z)
                cBuf.put(1.0f)
            }

            vBuf.position(0)
            nBuf.position(0)
            cBuf.position(0)

            return TriangleMesh(
                vertexBuffer = vBuf,
                normalBuffer = nBuf,
                colorBuffer = cBuf,
                vertexCount = totalVertices,
                triangleCount = totalVertices / 3,
                minBound = Vector3(minX, minY, minZ),
                maxBound = Vector3(maxX, maxY, maxZ)
            )
        }

        fun fromSolid(solid: CsgSolid): TriangleMesh {
            val triangles = mutableListOf<Triple<Vector3, Vector3, Vector3>>()
            val normals = mutableListOf<Vector3>()
            val colors = mutableListOf<Vector3>()

            var minX = Float.MAX_VALUE
            var minY = Float.MAX_VALUE
            var minZ = Float.MAX_VALUE
            var maxX = -Float.MAX_VALUE
            var maxY = -Float.MAX_VALUE
            var maxZ = -Float.MAX_VALUE

            for (poly in solid.polygons) {
                if (poly.vertices.size < 3) continue
                val n = poly.plane.normal
                val c = poly.color

                // Fan triangulation
                val v0 = poly.vertices[0]
                for (i in 1 until poly.vertices.size - 1) {
                    val v1 = poly.vertices[i]
                    val v2 = poly.vertices[i + 1]
                    triangles.add(Triple(v0, v1, v2))

                    normals.add(n)
                    normals.add(n)
                    normals.add(n)

                    colors.add(c)
                    colors.add(c)
                    colors.add(c)

                    for (v in listOf(v0, v1, v2)) {
                        minX = min(minX, v.x)
                        minY = min(minY, v.y)
                        minZ = min(minZ, v.z)
                        maxX = max(maxX, v.x)
                        maxY = max(maxY, v.y)
                        maxZ = max(maxZ, v.z)
                    }
                }
            }

            if (triangles.isEmpty()) {
                minX = -10f; minY = -10f; minZ = -10f
                maxX = 10f; maxY = 10f; maxZ = 10f
            }

            val totalVertices = triangles.size * 3

            val vBuf = ByteBuffer.allocateDirect(totalVertices * 3 * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()

            val nBuf = ByteBuffer.allocateDirect(totalVertices * 3 * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()

            val cBuf = ByteBuffer.allocateDirect(totalVertices * 4 * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()

            var normalIdx = 0
            for (tri in triangles) {
                for (v in listOf(tri.first, tri.second, tri.third)) {
                    vBuf.put(v.x)
                    vBuf.put(v.y)
                    vBuf.put(v.z)

                    val norm = normals[normalIdx]
                    nBuf.put(norm.x)
                    nBuf.put(norm.y)
                    nBuf.put(norm.z)

                    val col = colors[normalIdx]
                    cBuf.put(col.x)
                    cBuf.put(col.y)
                    cBuf.put(col.z)
                    cBuf.put(1.0f)

                    normalIdx++
                }
            }

            vBuf.position(0)
            nBuf.position(0)
            cBuf.position(0)

            return TriangleMesh(
                vertexBuffer = vBuf,
                normalBuffer = nBuf,
                colorBuffer = cBuf,
                vertexCount = totalVertices,
                triangleCount = triangles.size,
                minBound = Vector3(minX, minY, minZ),
                maxBound = Vector3(maxX, maxY, maxZ)
            )
        }
    }
}
