package com.mraphaelpy.terriflow.domain.model

import com.mraphaelpy.terriflow.testutil.Fixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TerritoryGeometryTest {

    private val square = listOf(LatLng(0.0, 0.0), LatLng(0.0, 2.0), LatLng(2.0, 2.0), LatLng(2.0, 0.0))

    @Test
    fun `centro usa o pino quando existe`() {
        val t = Fixtures.territory().copy(latitude = 5.0, longitude = 6.0, boundaryPoints = square)
        assertEquals(LatLng(5.0, 6.0), t.center)
    }

    @Test
    fun `centro usa o contorno quando nao ha pino`() {
        val t = Fixtures.territory().copy(boundaryPoints = square)
        assertEquals(LatLng(1.0, 1.0), t.center)
        assertTrue(t.isMapped)
    }

    @Test
    fun `quadras tem prioridade sobre o contorno`() {
        val block = listOf(LatLng(10.0, 10.0), LatLng(10.0, 12.0), LatLng(12.0, 12.0))
        val t = Fixtures.territory().copy(boundaryPoints = square, blockPolygons = listOf(block))
        assertEquals(block, t.shapePoints)
    }

    @Test
    fun `territorio sem localizacao nao tem centro`() {
        val t = Fixtures.territory()
        assertNull(t.center)
        assertFalse(t.isMapped)
        assertNull(listOf(t).bounds())
    }

    @Test
    fun `limites englobam todos os territorios`() {
        val a = Fixtures.territory().copy(boundaryPoints = square)
        val b = Fixtures.territory().copy(id = "t2", latitude = -3.0, longitude = 7.0)
        assertEquals(GeoBounds(north = 2.0, east = 7.0, south = -3.0, west = 0.0), listOf(a, b).bounds())
    }
}
