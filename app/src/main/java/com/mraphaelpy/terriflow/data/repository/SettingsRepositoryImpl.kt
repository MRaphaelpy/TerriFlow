package com.mraphaelpy.terriflow.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.mraphaelpy.terriflow.domain.repository.AppColor
import com.mraphaelpy.terriflow.domain.repository.SettingsRepository
import com.mraphaelpy.terriflow.domain.repository.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext context: Context
) : SettingsRepository {

    private val prefs: SharedPreferences = context.getSharedPreferences("terriflow_settings", Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(
        ThemeMode.valueOf(prefs.getString("theme_mode", ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name)
    )
    override val themeMode: Flow<ThemeMode> = _themeMode.asStateFlow()

    private val _appColor = MutableStateFlow(
        AppColor.valueOf(prefs.getString("app_color", AppColor.DEFAULT.name) ?: AppColor.DEFAULT.name)
    )
    override val appColor: Flow<AppColor> = _appColor.asStateFlow()

    override suspend fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString("theme_mode", mode.name).apply()
        _themeMode.value = mode
    }

    override suspend fun setAppColor(color: AppColor) {
        prefs.edit().putString("app_color", color.name).apply()
        _appColor.value = color
    }
}
