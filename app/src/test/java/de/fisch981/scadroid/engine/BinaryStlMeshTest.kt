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
}
