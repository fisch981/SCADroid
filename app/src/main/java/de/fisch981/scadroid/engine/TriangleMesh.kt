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
    val maxBound: Vector3
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

        val countBuf = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)
        countBuf.putInt(triangleCount)
        out.write(countBuf.array())

        val vBuf = vertexBuffer.duplicate()
        val nBuf = normalBuffer.duplicate()
        vBuf.position(0)
        nBuf.position(0)

        val triBuf = ByteBuffer.allocate(50).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until triangleCount) {
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
