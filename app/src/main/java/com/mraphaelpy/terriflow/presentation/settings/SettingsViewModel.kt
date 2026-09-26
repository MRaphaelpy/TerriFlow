package com.mraphaelpy.terriflow.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mraphaelpy.terriflow.domain.repository.AppColor
import com.mraphaelpy.terriflow.domain.repository.SettingsRepository
import com.mraphaelpy.terriflow.domain.repository.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val appColor: AppColor = AppColor.DEFAULT
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository
) : ViewModel() {

    val uiState = combine(repository.themeMode, repository.appColor) { mode, color ->
        SettingsUiState(mode, color)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    fun updateThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            repository.setThemeMode(mode)
        }
    }

    fun updateAppColor(color: AppColor) {
        viewModelScope.launch {
            repository.setAppColor(color)
        }
    }
}
