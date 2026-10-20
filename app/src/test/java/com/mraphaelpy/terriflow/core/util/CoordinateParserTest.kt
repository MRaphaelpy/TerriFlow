package com.mraphaelpy.terriflow.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class CoordinateParserTest {

    @Test
    fun `aceita decimal separado por virgula ou espaco`() {
        assertEquals(-23.5505, CoordinateParser.parse("-23.5505, -46.6333")!!.lat, 1e-6)
        assertEquals(-46.6333, CoordinateParser.parse("-23.5505 -46.6333")!!.lng, 1e-6)
    }

    @Test
    fun `aceita url do google maps`() {
        val point = CoordinateParser.parse("https://maps.google.com/?q=-23.5505,-46.6333")
        assertNotNull(point)
        assertEquals(-46.6333, point!!.lng, 1e-6)
    }

    @Test
    fun `aceita graus minutos e segundos`() {
        val point = CoordinateParser.parse("23°33'01.8\"S 46°37'59.9\"W")!!
        assertEquals(-23.5505, point.lat, 1e-3)
        assertEquals(-46.6333, point.lng, 1e-3)
    }

    @Test
    fun `rejeita valores invalidos`() {
        assertNull(CoordinateParser.parse("abc"))
        assertNull(CoordinateParser.parse("95, 10"))
    }
}
