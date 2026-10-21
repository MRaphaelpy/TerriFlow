package com.mraphaelpy.terriflow.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.mraphaelpy.terriflow.domain.repository.AppColor
import com.mraphaelpy.terriflow.domain.repository.ContrastLevel
import com.mraphaelpy.terriflow.domain.repository.TextSize
import com.mraphaelpy.terriflow.domain.repository.SettingsRepository
import com.mraphaelpy.terriflow.domain.repository.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext context: Context
) : SettingsRepository {

    private val prefs: SharedPreferences = context.getSharedPreferences("terriflow_settings", Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(readEnum(KEY_THEME_MODE, ThemeMode.SYSTEM))
    override val themeMode: Flow<ThemeMode> = _themeMode.asStateFlow()

    private val _appColor = MutableStateFlow(readEnum(KEY_APP_COLOR, AppColor.DEFAULT))
    override val appColor: Flow<AppColor> = _appColor.asStateFlow()

    private val _textSize = MutableStateFlow(readEnum(KEY_TEXT_SIZE, TextSize.DEFAULT))
    override val textSize: Flow<TextSize> = _textSize.asStateFlow()

    private val _contrastLevel = MutableStateFlow(readEnum(KEY_CONTRAST, ContrastLevel.STANDARD))
    override val contrastLevel: Flow<ContrastLevel> = _contrastLevel.asStateFlow()

    override suspend fun setThemeMode(mode: ThemeMode) {
        prefs.edit { putString(KEY_THEME_MODE, mode.name) }
        _themeMode.value = mode
    }

    override suspend fun setAppColor(color: AppColor) {
        prefs.edit { putString(KEY_APP_COLOR, color.name) }
        _appColor.value = color
    }

    override suspend fun setTextSize(size: TextSize) {
        prefs.edit { putString(KEY_TEXT_SIZE, size.name) }
        _textSize.value = size
    }

    override suspend fun setContrastLevel(level: ContrastLevel) {
        prefs.edit { putString(KEY_CONTRAST, level.name) }
        _contrastLevel.value = level
    }

    private inline fun <reified T : Enum<T>> readEnum(key: String, default: T): T =
        prefs.getString(key, null)
            ?.let { saved -> enumValues<T>().firstOrNull { it.name == saved } }
            ?: default

    private companion object {
        const val KEY_THEME_MODE = "theme_mode"
        const val KEY_APP_COLOR = "app_color"
        const val KEY_TEXT_SIZE = "text_size"
        const val KEY_CONTRAST = "contrast_level"
    }
}
