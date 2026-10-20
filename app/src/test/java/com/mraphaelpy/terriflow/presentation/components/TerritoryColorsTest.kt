package com.mraphaelpy.terriflow.presentation.components

import com.mraphaelpy.terriflow.testutil.Fixtures
import org.junit.Assert.assertEquals
import org.junit.Test

class TerritoryColorsTest {

    @Test
    fun `cada territorio recebe uma cor unica e estavel`() {
        val territories = (1..60).map { Fixtures.territory().copy(id = "t$it", code = "T-%05d".format(it)) }

        val palette = territoryPalette(territories)

        assertEquals(territories.size, palette.values.toSet().size)
        assertEquals(palette, territoryPalette(territories.shuffled()))
    }
}
