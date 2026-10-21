package com.mraphaelpy.terriflow.domain.repository

import kotlinx.coroutines.flow.Flow

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

enum class AppColor {
    DEFAULT, TEAL, BLUE, INDIGO, PURPLE, PINK, RED, ORANGE, AMBER, OLIVE, BROWN, DYNAMIC
}

interface SettingsRepository {
    val themeMode: Flow<ThemeMode>
    val appColor: Flow<AppColor>

    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setAppColor(color: AppColor)
}
