package com.mraphaelpy.terriflow.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.mraphaelpy.terriflow.domain.repository.AppColor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SeedColorSchemesTest {

    private val seeds = AppColor.entries.filter { it != AppColor.DYNAMIC }

    @Test
    fun `texto sobre as cores principais tem contraste suficiente`() {
        for (option in seeds) for (dark in listOf(false, true)) {
            val scheme = SeedColorSchemes.of(option.seed, dark)
            val pairs = listOf(
                scheme.primary to scheme.onPrimary,
                scheme.primaryContainer to scheme.onPrimaryContainer,
                scheme.secondaryContainer to scheme.onSecondaryContainer,
                scheme.surface to scheme.onSurface,
                scheme.surfaceContainer to scheme.onSurface,
                scheme.errorContainer to scheme.onErrorContainer
            )
            pairs.forEach { (background, text) ->
                val ratio = contrast(background, text)
                assertTrue("${option.label} ${if (dark) "escuro" else "claro"}: contraste $ratio", ratio >= 4.5)
            }
        }
    }

    @Test
    fun `cada opcao gera uma cor principal diferente`() {
        val primaries = seeds.map { SeedColorSchemes.of(it.seed, false).primary }
        assertEquals(seeds.size, primaries.toSet().size)
    }

    @Test
    fun `tema escuro tem fundo escuro e claro tem fundo claro`() {
        for (option in seeds) {
            assertTrue(SeedColorSchemes.of(option.seed, true).surface.luminance() < 0.1f)
            assertTrue(SeedColorSchemes.of(option.seed, false).surface.luminance() > 0.8f)
        }
    }

    private fun contrast(a: Color, b: Color): Float {
        val (light, dark) = listOf(a.luminance(), b.luminance()).sortedDescending()
        return (light + 0.05f) / (dark + 0.05f)
    }
}
