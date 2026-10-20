package com.mraphaelpy.terriflow.presentation.map

import org.junit.Assert.assertEquals
import org.junit.Test

class MapStylingTest {

    @Test
    fun `nome curto usa primeiro nome e inicial do sobrenome`() {
        assertEquals("Marcos S.", shortPersonName("Marcos Rafael Silva"))
        assertEquals("Ana", shortPersonName(" Ana "))
    }

    @Test
    fun `rotulo do marcador usa o numero ou o inicio do codigo personalizado`() {
        assertEquals("6", markerLabel("T-00006"))
        assertEquals("Centr", markerLabel("Centro"))
    }
}
