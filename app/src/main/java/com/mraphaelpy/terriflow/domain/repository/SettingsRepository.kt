package com.mraphaelpy.terriflow.domain.repository

import kotlinx.coroutines.flow.Flow

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

enum class AppColor {
    DEFAULT, DYNAMIC, BLUE, PURPLE, ORANGE
}

interface SettingsRepository {
    val themeMode: Flow<ThemeMode>
    val appColor: Flow<AppColor>

    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setAppColor(color: AppColor)
}
