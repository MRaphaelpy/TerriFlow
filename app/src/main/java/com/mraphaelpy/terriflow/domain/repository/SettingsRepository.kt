package com.mraphaelpy.terriflow.domain.repository

import kotlinx.coroutines.flow.Flow

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

enum class AppColor {
    DEFAULT, TEAL, BLUE, INDIGO, PURPLE, PINK, RED, ORANGE, AMBER, OLIVE, BROWN, DYNAMIC
}

enum class TextSize(val scale: Float) {
    SMALL(0.9f), DEFAULT(1f), LARGE(1.15f), EXTRA_LARGE(1.3f), HUGE(1.5f)
}

enum class ContrastLevel(val value: Double) {
    STANDARD(0.0), MEDIUM(0.5), HIGH(1.0)
}

interface SettingsRepository {
    val themeMode: Flow<ThemeMode>
    val appColor: Flow<AppColor>
    val textSize: Flow<TextSize>
    val contrastLevel: Flow<ContrastLevel>

    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setAppColor(color: AppColor)
    suspend fun setTextSize(size: TextSize)
    suspend fun setContrastLevel(level: ContrastLevel)
}
