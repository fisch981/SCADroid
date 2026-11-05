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
        assertEquals(10f, parsed.maxBound.x - parsed.minBound.x, 1e-4f)
        assertEquals(20f, parsed.maxBound.y - parsed.minBound.y, 1e-4f)
        assertEquals(30f, parsed.maxBound.z - parsed.minBound.z, 1e-4f)
    }
}
