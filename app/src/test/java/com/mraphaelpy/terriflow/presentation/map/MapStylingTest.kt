package com.mraphaelpy.terriflow.presentation.map

import com.mraphaelpy.terriflow.testutil.Fixtures
import org.junit.Assert.assertEquals
import org.junit.Test

class MapStylingTest {

    @Test
    fun `cada territorio recebe uma cor unica e estavel`() {
        val territories = (1..60).map { Fixtures.territory().copy(id = "t$it", code = "T-%05d".format(it)) }

        val palette = territoryPalette(territories)

        assertEquals(territories.size, palette.values.toSet().size)
        assertEquals(palette, territoryPalette(territories.shuffled()))
    }

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
