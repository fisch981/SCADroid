package de.fisch981.scadroid.engine

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayOutputStream

class BinaryStlMeshTest {

    @Test
    fun testBinaryStlRoundTrip() {
        val solid = CsgSolid.cube(Vector3(10f, 20f, 30f), center = false)
        val origMesh = TriangleMesh.fromSolid(solid)
        val baos = ByteArrayOutputStream()
        origMesh.exportBinaryStl(baos)

        val parsed = TriangleMesh.fromBinaryStl(baos.toByteArray())
        assertEquals(12, parsed.triangleCount)
        assertEquals(36, parsed.vertexCount)
        assertEquals(36, parsed.opaqueVertexCount)
        assertEquals(10f, parsed.maxBound.x - parsed.minBound.x, 1e-4f)
        assertEquals(20f, parsed.maxBound.y - parsed.minBound.y, 1e-4f)
        assertEquals(30f, parsed.maxBound.z - parsed.minBound.z, 1e-4f)
    }

    @Test
    fun testBinaryStlHighlightAndBackgroundModifiers() {
        val buf = java.nio.ByteBuffer.allocate(84 + 3 * 50).order(java.nio.ByteOrder.LITTLE_ENDIAN)
        buf.putInt(80, 3)

        // Triangle 0: Normal CSG (attr = 0)
        buf.putFloat(84 + 12, 0f); buf.putFloat(84 + 16, 0f); buf.putFloat(84 + 20, 0f)
        buf.putFloat(84 + 24, 1f); buf.putFloat(84 + 28, 0f); buf.putFloat(84 + 32, 0f)
        buf.putFloat(84 + 36, 0f); buf.putFloat(84 + 40, 1f); buf.putFloat(84 + 44, 0f)
        buf.putShort(84 + 48, 0)

        // Triangle 1: '#' Highlight modifier (attr = 1)
        val t1 = 84 + 50
        buf.putFloat(t1 + 12, 0f); buf.putFloat(t1 + 16, 0f); buf.putFloat(t1 + 20, 1f)
        buf.putFloat(t1 + 24, 1f); buf.putFloat(t1 + 28, 0f); buf.putFloat(t1 + 32, 1f)
        buf.putFloat(t1 + 36, 0f); buf.putFloat(t1 + 40, 1f); buf.putFloat(t1 + 44, 1f)
        buf.putShort(t1 + 48, 1)

        // Triangle 2: '%' Background modifier (attr = 2)
        val t2 = 84 + 100
        buf.putFloat(t2 + 12, 0f); buf.putFloat(t2 + 16, 0f); buf.putFloat(t2 + 20, 2f)
        buf.putFloat(t2 + 24, 1f); buf.putFloat(t2 + 28, 0f); buf.putFloat(t2 + 32, 2f)
        buf.putFloat(t2 + 36, 0f); buf.putFloat(t2 + 40, 1f); buf.putFloat(t2 + 44, 2f)
        buf.putShort(t2 + 48, 2)

        val mesh = TriangleMesh.fromBinaryStl(buf.array())
        assertEquals(3, mesh.triangleCount)
        assertEquals(9, mesh.vertexCount)
        assertEquals(3, mesh.opaqueVertexCount)

        // Check '#' highlight color (vertex 3 -> index 3 * 4 = 12 in colorBuffer)
        assertEquals(1.0f, mesh.colorBuffer.get(12), 1e-3f)
        assertEquals(0.35f, mesh.colorBuffer.get(13), 1e-3f)
        assertEquals(0.35f, mesh.colorBuffer.get(14), 1e-3f)
        assertEquals(0.40f, mesh.colorBuffer.get(15), 1e-3f)

        // Check '%' background color (vertex 6 -> index 6 * 4 = 24 in colorBuffer)
        assertEquals(0.75f, mesh.colorBuffer.get(24), 1e-3f)
        assertEquals(0.28f, mesh.colorBuffer.get(27), 1e-3f)

        // Export to Binary STL should only export the 1 opaque CSG triangle
        val exported = ByteArrayOutputStream()
        mesh.exportBinaryStl(exported)
        val reParsed = TriangleMesh.fromBinaryStl(exported.toByteArray())
        assertEquals(1, reParsed.triangleCount)
    }

    @Test
    fun testScadCol1TrailerColorsAndTransparencyPartitioning() {
        val triCount = 3
        val stlSize = 84 + triCount * 50
        val buf = java.nio.ByteBuffer.allocate(stlSize + 8 + triCount * 4)
            .order(java.nio.ByteOrder.LITTLE_ENDIAN)
        buf.putInt(80, triCount)

        // Triangle 0: attr = 0, translucent green (placed first in input to verify reordering after opaque)
        val t0 = 84
        buf.putFloat(t0 + 12, 0f); buf.putFloat(t0 + 16, 0f); buf.putFloat(t0 + 20, 5f)
        buf.putFloat(t0 + 24, 1f); buf.putFloat(t0 + 28, 0f); buf.putFloat(t0 + 32, 5f)
        buf.putFloat(t0 + 36, 0f); buf.putFloat(t0 + 40, 1f); buf.putFloat(t0 + 44, 5f)
        buf.putShort(t0 + 48, 0)

        // Triangle 1: attr = 0, opaque red
        val t1 = 84 + 50
        buf.putFloat(t1 + 12, 0f); buf.putFloat(t1 + 16, 0f); buf.putFloat(t1 + 20, 1f)
        buf.putFloat(t1 + 24, 1f); buf.putFloat(t1 + 28, 0f); buf.putFloat(t1 + 32, 1f)
        buf.putFloat(t1 + 36, 0f); buf.putFloat(t1 + 40, 1f); buf.putFloat(t1 + 44, 1f)
        buf.putShort(t1 + 48, 0)

        // Triangle 2: attr = 2, background '%'
        val t2 = 84 + 100
        buf.putFloat(t2 + 12, 0f); buf.putFloat(t2 + 16, 0f); buf.putFloat(t2 + 20, 9f)
        buf.putFloat(t2 + 24, 1f); buf.putFloat(t2 + 28, 0f); buf.putFloat(t2 + 32, 9f)
        buf.putFloat(t2 + 36, 0f); buf.putFloat(t2 + 40, 1f); buf.putFloat(t2 + 44, 9f)
        buf.putShort(t2 + 48, 2)

        // SCADCOL1 Trailer
        val magic = "SCADCOL1"
        for (i in magic.indices) {
            buf.put(stlSize + i, magic[i].code.toByte())
        }
        val colStart = stlSize + 8
        // Tri 0: translucent green (0, 255, 0, 128)
        buf.put(colStart + 0, 0.toByte()); buf.put(colStart + 1, 255.toByte()); buf.put(colStart + 2, 0.toByte()); buf.put(colStart + 3, 128.toByte())
        // Tri 1: opaque red (255, 0, 0, 255)
        buf.put(colStart + 4, 255.toByte()); buf.put(colStart + 5, 0.toByte()); buf.put(colStart + 6, 0.toByte()); buf.put(colStart + 7, 255.toByte())
        // Tri 2: background tinted gray (191, 191, 199, 72)
        buf.put(colStart + 8, 191.toByte()); buf.put(colStart + 9, 191.toByte()); buf.put(colStart + 10, 199.toByte()); buf.put(colStart + 11, 72.toByte())

        val mesh = TriangleMesh.fromBinaryStl(buf.array())
        assertEquals(3, mesh.triangleCount)
        assertEquals(9, mesh.vertexCount)
        assertEquals(3, mesh.opaqueVertexCount)
        assertEquals(2, mesh.exportTriangleCount)

        // Opaque red triangle moved to vertex 0..2
        assertEquals(1.0f, mesh.colorBuffer.get(0), 1e-3f)
        assertEquals(0.0f, mesh.colorBuffer.get(1), 1e-3f)
        assertEquals(0.0f, mesh.colorBuffer.get(2), 1e-3f)
        assertEquals(1.0f, mesh.colorBuffer.get(3), 1e-3f)

        // Translucent green triangle moved to vertex 3..5 (index 12..15)
        assertEquals(0.0f, mesh.colorBuffer.get(12), 1e-3f)
        assertEquals(1.0f, mesh.colorBuffer.get(13), 1e-3f)
        assertEquals(0.0f, mesh.colorBuffer.get(14), 1e-3f)
        assertEquals(128f / 255f, mesh.colorBuffer.get(15), 1e-3f)

        // Background triangle at vertex 6..8 (index 24..27)
        assertEquals(72f / 255f, mesh.colorBuffer.get(27), 1e-3f)

        // Binary STL export includes both opaque red and translucent green model triangles, excluding background '%'
        val exported = ByteArrayOutputStream()
        mesh.exportBinaryStl(exported)
        val reParsed = TriangleMesh.fromBinaryStl(exported.toByteArray())
        assertEquals(2, reParsed.triangleCount)
    }
}
