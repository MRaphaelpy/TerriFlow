package com.mraphaelpy.terriflow.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.mraphaelpy.terriflow.domain.repository.AppColor
import com.mraphaelpy.terriflow.domain.repository.ThemeMode

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TerriFlowTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    appColor: AppColor = AppColor.DEFAULT,
    content: @Composable () -> Unit
) {
    val isDarkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }
    val context = LocalContext.current
    val useDynamic = appColor == AppColor.DYNAMIC && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val colorScheme = remember(appColor, isDarkTheme, useDynamic) {
        when {
            useDynamic && isDarkTheme -> dynamicDarkColorScheme(context)
            useDynamic -> dynamicLightColorScheme(context)
            else -> SeedColorSchemes.of(appColor.seed, isDarkTheme)
        }
    }

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        typography = TerriFlowTypography,
        shapes = TerriFlowShapes,
        motionScheme = MotionScheme.expressive(),
        content = content
    )
}
