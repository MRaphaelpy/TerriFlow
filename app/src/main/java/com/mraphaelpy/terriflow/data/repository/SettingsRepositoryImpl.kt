package com.mraphaelpy.terriflow.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.mraphaelpy.terriflow.domain.repository.AppColor
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

    override suspend fun setThemeMode(mode: ThemeMode) {
        prefs.edit { putString(KEY_THEME_MODE, mode.name) }
        _themeMode.value = mode
    }

    override suspend fun setAppColor(color: AppColor) {
        prefs.edit { putString(KEY_APP_COLOR, color.name) }
        _appColor.value = color
    }

    /** Valor salvo inválido (ex.: opção removida numa atualização) volta ao padrão em vez de travar o app. */
    private inline fun <reified T : Enum<T>> readEnum(key: String, default: T): T =
        prefs.getString(key, null)
            ?.let { saved -> enumValues<T>().firstOrNull { it.name == saved } }
            ?: default

    private companion object {
        const val KEY_THEME_MODE = "theme_mode"
        const val KEY_APP_COLOR = "app_color"
    }
}
