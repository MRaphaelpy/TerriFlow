package com.mraphaelpy.terriflow.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.mraphaelpy.terriflow.domain.repository.AppColor
import com.mraphaelpy.terriflow.domain.repository.ContrastLevel
import com.mraphaelpy.terriflow.domain.repository.TextSize
import com.mraphaelpy.terriflow.domain.repository.ThemeMode

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TerriFlowTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    appColor: AppColor = AppColor.DEFAULT,
    textSize: TextSize = TextSize.DEFAULT,
    contrastLevel: ContrastLevel = ContrastLevel.STANDARD,
    content: @Composable () -> Unit
) {
    val isDarkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }
    val context = LocalContext.current
    val useDynamic = appColor == AppColor.DYNAMIC && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val colorScheme = remember(appColor, isDarkTheme, useDynamic, contrastLevel) {
        val system = when {
            !useDynamic -> null
            isDarkTheme -> dynamicDarkColorScheme(context)
            else -> dynamicLightColorScheme(context)
        }
        when {
            system == null -> SeedColorSchemes.of(appColor.seed, isDarkTheme, contrastLevel.value)
            contrastLevel == ContrastLevel.STANDARD -> system
            else -> SeedColorSchemes.of(system.primary, isDarkTheme, contrastLevel.value)
        }
    }
    val statusColors = remember(colorScheme.primary, isDarkTheme) {
        StatusColors.harmonizedWith(colorScheme.primary, isDarkTheme)
    }
    val baseDensity = LocalDensity.current
    val density = remember(baseDensity, textSize) {
        Density(baseDensity.density, baseDensity.fontScale * textSize.scale)
    }

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        typography = TerriFlowTypography,
        shapes = TerriFlowShapes,
        motionScheme = MotionScheme.expressive()
    ) {
        CompositionLocalProvider(
            LocalDensity provides density,
            LocalStatusColors provides statusColors,
            content = content
        )
    }
}
