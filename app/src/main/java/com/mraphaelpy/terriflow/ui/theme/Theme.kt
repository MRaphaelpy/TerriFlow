@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.mraphaelpy.terriflow.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.mraphaelpy.terriflow.domain.repository.AppColor
import com.mraphaelpy.terriflow.domain.repository.ThemeMode

private val LightColorScheme = lightColorScheme(
    primary = TerriGreen,
    onPrimary = Color.White,
    primaryContainer = TerriGreenContainer,
    onPrimaryContainer = TerriOnGreenContainer,

    secondary = TerriSecondary,
    onSecondary = Color.White,
    secondaryContainer = TerriSecondaryContainer,

    tertiary = TerriTertiary,
    onTertiary = Color.White,
    tertiaryContainer = TerriTertiaryContainer,

    background = Color(0xFFF7FBF7),
    onBackground = Color(0xFF171D19),

    surface = Color(0xFFF7FBF7),
    onSurface = Color(0xFF171D19),

    surfaceVariant = Color(0xFFDDE5DE),
    onSurfaceVariant = Color(0xFF414943),

    outline = Color(0xFF717971),
    outlineVariant = Color(0xFFC1CAC2)
)

private val DarkColorScheme = darkColorScheme(
    primary = TerriGreenDark,
    onPrimary = Color(0xFF00391F),
    primaryContainer = TerriGreenContainerDark,
    onPrimaryContainer = TerriOnGreenContainerDark,

    secondary = TerriSecondaryDark,
    onSecondary = Color(0xFF213524),
    secondaryContainer = TerriSecondaryContainerDark,

    tertiary = TerriTertiaryDark,
    onTertiary = Color(0xFF07372D),
    tertiaryContainer = TerriTertiaryContainerDark,

    background = Color(0xFF0F1511),
    onBackground = Color(0xFFE0E7E0),

    surface = Color(0xFF0F1511),
    onSurface = Color(0xFFE0E7E0),

    surfaceVariant = Color(0xFF414943),
    onSurfaceVariant = Color(0xFFC1CAC2),

    outline = Color(0xFF8B948C),
    outlineVariant = Color(0xFF414943)
)


private val BlueColorScheme = lightColorScheme(
    primary = TerriBlue, onPrimary = Color.White, primaryContainer = TerriBlueContainer, onPrimaryContainer = TerriOnBlueContainer,
    secondary = TerriBlue, onSecondary = Color.White, secondaryContainer = TerriBlueContainer,
    tertiary = TerriBlue, onTertiary = Color.White, tertiaryContainer = TerriBlueContainer,
    background = Color(0xFFF7F9FB), onBackground = Color(0xFF17191D),
    surface = Color(0xFFF7F9FB), onSurface = Color(0xFF17191D),
    surfaceVariant = Color(0xFFDDE1E5), onSurfaceVariant = Color(0xFF414549),
    outline = Color(0xFF717579), outlineVariant = Color(0xFFC1C5C9)
)

private val BlueDarkColorScheme = darkColorScheme(
    primary = TerriBlueDark, onPrimary = Color(0xFF00391F), primaryContainer = TerriBlueContainerDark, onPrimaryContainer = TerriOnBlueContainerDark,
    secondary = TerriBlueDark, onSecondary = Color(0xFF00391F), secondaryContainer = TerriBlueContainerDark,
    tertiary = TerriBlueDark, onTertiary = Color(0xFF00391F), tertiaryContainer = TerriBlueContainerDark,
    background = Color(0xFF0F1215), onBackground = Color(0xFFE0E3E7),
    surface = Color(0xFF0F1215), onSurface = Color(0xFFE0E3E7),
    surfaceVariant = Color(0xFF414549), onSurfaceVariant = Color(0xFFC1C5C9),
    outline = Color(0xFF8B8F94), outlineVariant = Color(0xFF414549)
)

private val PurpleColorScheme = lightColorScheme(
    primary = TerriPurple, onPrimary = Color.White, primaryContainer = TerriPurpleContainer, onPrimaryContainer = TerriOnPurpleContainer,
    secondary = TerriPurple, onSecondary = Color.White, secondaryContainer = TerriPurpleContainer,
    tertiary = TerriPurple, onTertiary = Color.White, tertiaryContainer = TerriPurpleContainer,
    background = Color(0xFFF9F7FB), onBackground = Color(0xFF19171D),
    surface = Color(0xFFF9F7FB), onSurface = Color(0xFF19171D),
    surfaceVariant = Color(0xFFE3DDE5), onSurfaceVariant = Color(0xFF474149),
    outline = Color(0xFF777179), outlineVariant = Color(0xFFC7C1C9)
)

private val PurpleDarkColorScheme = darkColorScheme(
    primary = TerriPurpleDark, onPrimary = Color(0xFF3B1D79), primaryContainer = TerriPurpleContainerDark, onPrimaryContainer = TerriOnPurpleContainerDark,
    secondary = TerriPurpleDark, onSecondary = Color(0xFF3B1D79), secondaryContainer = TerriPurpleContainerDark,
    tertiary = TerriPurpleDark, onTertiary = Color(0xFF3B1D79), tertiaryContainer = TerriPurpleContainerDark,
    background = Color(0xFF110F15), onBackground = Color(0xFFE5E0E7),
    surface = Color(0xFF110F15), onSurface = Color(0xFFE5E0E7),
    surfaceVariant = Color(0xFF474149), onSurfaceVariant = Color(0xFFC7C1C9),
    outline = Color(0xFF918B94), outlineVariant = Color(0xFF474149)
)

private val OrangeColorScheme = lightColorScheme(
    primary = TerriOrange, onPrimary = Color.White, primaryContainer = TerriOrangeContainer, onPrimaryContainer = TerriOnOrangeContainer,
    secondary = TerriOrange, onSecondary = Color.White, secondaryContainer = TerriOrangeContainer,
    tertiary = TerriOrange, onTertiary = Color.White, tertiaryContainer = TerriOrangeContainer,
    background = Color(0xFFFBF9F7), onBackground = Color(0xFF1D1917),
    surface = Color(0xFFFBF9F7), onSurface = Color(0xFF1D1917),
    surfaceVariant = Color(0xFFE5E1DD), onSurfaceVariant = Color(0xFF494541),
    outline = Color(0xFF797571), outlineVariant = Color(0xFFC9C5C1)
)

private val OrangeDarkColorScheme = darkColorScheme(
    primary = TerriOrangeDark, onPrimary = Color(0xFF4D2700), primaryContainer = TerriOrangeContainerDark, onPrimaryContainer = TerriOnOrangeContainerDark,
    secondary = TerriOrangeDark, onSecondary = Color(0xFF4D2700), secondaryContainer = TerriOrangeContainerDark,
    tertiary = TerriOrangeDark, onTertiary = Color(0xFF4D2700), tertiaryContainer = TerriOrangeContainerDark,
    background = Color(0xFF15120F), onBackground = Color(0xFFE7E3E0),
    surface = Color(0xFF15120F), onSurface = Color(0xFFE7E3E0),
    surfaceVariant = Color(0xFF494541), onSurfaceVariant = Color(0xFFC9C5C1),
    outline = Color(0xFF948F8B), outlineVariant = Color(0xFF494541)
)

@Composable
fun TerriFlowTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    appColor: AppColor = AppColor.DEFAULT,
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val isDarkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemDark
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }

    val context = LocalContext.current
    val colorScheme = when (appColor) {
        AppColor.DYNAMIC -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (isDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } else {
                if (isDarkTheme) DarkColorScheme else LightColorScheme
            }
        }
        AppColor.BLUE -> if (isDarkTheme) BlueDarkColorScheme else BlueColorScheme
        AppColor.PURPLE -> if (isDarkTheme) PurpleDarkColorScheme else PurpleColorScheme
        AppColor.ORANGE -> if (isDarkTheme) OrangeDarkColorScheme else OrangeColorScheme
        AppColor.DEFAULT -> if (isDarkTheme) DarkColorScheme else LightColorScheme
    }

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        typography = TerriFlowTypography,
        shapes = TerriFlowShapes,
        content = content
    )
}