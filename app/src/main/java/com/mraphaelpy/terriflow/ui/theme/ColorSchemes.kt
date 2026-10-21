package com.mraphaelpy.terriflow.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.materialkolor.dynamiccolor.DynamicColor
import com.materialkolor.dynamiccolor.MaterialDynamicColors
import com.materialkolor.hct.Hct
import com.materialkolor.scheme.DynamicScheme
import com.materialkolor.scheme.SchemeTonalSpot
import java.util.concurrent.ConcurrentHashMap

object SeedColorSchemes {

    private val cache = ConcurrentHashMap<Pair<Int, Boolean>, ColorScheme>()
    private val colors = MaterialDynamicColors()

    fun of(seed: Color, dark: Boolean): ColorScheme =
        cache.getOrPut(seed.toArgb() to dark) { build(SchemeTonalSpot(Hct.fromInt(seed.toArgb()), dark, 0.0)) }

    private fun build(scheme: DynamicScheme): ColorScheme {
        fun DynamicColor.color() = Color(getArgb(scheme))
        return with(colors) {
            val base = if (scheme.isDark) darkColorScheme() else lightColorScheme()
            base.copy(
                primary = primary().color(),
                onPrimary = onPrimary().color(),
                primaryContainer = primaryContainer().color(),
                onPrimaryContainer = onPrimaryContainer().color(),
                inversePrimary = inversePrimary().color(),
                secondary = secondary().color(),
                onSecondary = onSecondary().color(),
                secondaryContainer = secondaryContainer().color(),
                onSecondaryContainer = onSecondaryContainer().color(),
                tertiary = tertiary().color(),
                onTertiary = onTertiary().color(),
                tertiaryContainer = tertiaryContainer().color(),
                onTertiaryContainer = onTertiaryContainer().color(),
                background = background().color(),
                onBackground = onBackground().color(),
                surface = surface().color(),
                onSurface = onSurface().color(),
                surfaceVariant = surfaceVariant().color(),
                onSurfaceVariant = onSurfaceVariant().color(),
                surfaceTint = surfaceTint().color(),
                inverseSurface = inverseSurface().color(),
                inverseOnSurface = inverseOnSurface().color(),
                error = error().color(),
                onError = onError().color(),
                errorContainer = errorContainer().color(),
                onErrorContainer = onErrorContainer().color(),
                outline = outline().color(),
                outlineVariant = outlineVariant().color(),
                scrim = scrim().color(),
                surfaceBright = surfaceBright().color(),
                surfaceDim = surfaceDim().color(),
                surfaceContainer = surfaceContainer().color(),
                surfaceContainerHigh = surfaceContainerHigh().color(),
                surfaceContainerHighest = surfaceContainerHighest().color(),
                surfaceContainerLow = surfaceContainerLow().color(),
                surfaceContainerLowest = surfaceContainerLowest().color(),
                primaryFixed = primaryFixed().color(),
                primaryFixedDim = primaryFixedDim().color(),
                onPrimaryFixed = onPrimaryFixed().color(),
                onPrimaryFixedVariant = onPrimaryFixedVariant().color(),
                secondaryFixed = secondaryFixed().color(),
                secondaryFixedDim = secondaryFixedDim().color(),
                onSecondaryFixed = onSecondaryFixed().color(),
                onSecondaryFixedVariant = onSecondaryFixedVariant().color(),
                tertiaryFixed = tertiaryFixed().color(),
                tertiaryFixedDim = tertiaryFixedDim().color(),
                onTertiaryFixed = onTertiaryFixed().color(),
                onTertiaryFixedVariant = onTertiaryFixedVariant().color()
            )
        }
    }
}
