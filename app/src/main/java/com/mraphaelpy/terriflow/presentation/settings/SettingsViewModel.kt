package com.mraphaelpy.terriflow.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mraphaelpy.terriflow.core.updater.AppUpdateInfo
import com.mraphaelpy.terriflow.core.updater.AppUpdateManager
import com.mraphaelpy.terriflow.domain.model.Congregation
import com.mraphaelpy.terriflow.domain.repository.AppColor
import com.mraphaelpy.terriflow.domain.repository.CongregationRepository
import com.mraphaelpy.terriflow.domain.repository.SettingsRepository
import com.mraphaelpy.terriflow.domain.repository.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val appColor: AppColor = AppColor.DEFAULT,
    val congregation: Congregation? = null,
    val congregationLoading: Boolean = true,
    val isCheckingUpdate: Boolean = false,
    val availableUpdate: AppUpdateInfo? = null,
    val updateMessage: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository,
    private val congregationRepository: CongregationRepository,
    val updateManager: AppUpdateManager
) : ViewModel() {

    private val _congregationState = MutableStateFlow<Pair<Congregation?, Boolean>>(null to true)
    private val _updateState = MutableStateFlow<Triple<Boolean, AppUpdateInfo?, String?>>(Triple(false, null, null))

    val uiState = combine(
        repository.themeMode,
        repository.appColor,
        _congregationState,
        _updateState
    ) { mode, color, (congregation, loading), (isChecking, updateInfo, message) ->
        SettingsUiState(
            themeMode = mode,
            appColor = color,
            congregation = congregation,
            congregationLoading = loading,
            isCheckingUpdate = isChecking,
            availableUpdate = updateInfo,
            updateMessage = message
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    init {
        loadCongregation()
    }

    private fun loadCongregation() {
        viewModelScope.launch {
            val id = congregationRepository.getCurrentCongregationId()
            val congregation = if (id != null) congregationRepository.getById(id) else null
            _congregationState.update { congregation to false }
        }
    }

    fun checkForUpdates() {
        viewModelScope.launch {
            _updateState.update { Triple(true, null, null) }
            val info = updateManager.checkForUpdate()
            if (info != null && info.hasUpdate) {
                _updateState.update { Triple(false, info, null) }
            } else if (info != null) {
                _updateState.update { Triple(false, null, "O aplicativo já está na versão mais recente (v${info.currentVersion})") }
            } else {
                _updateState.update { Triple(false, null, "Não foi possível verificar atualizações. Verifique sua conexão.") }
            }
        }
    }

    fun dismissUpdateDialog() {
        _updateState.update { it.copy(second = null) }
    }

    fun clearUpdateMessage() {
        _updateState.update { it.copy(third = null) }
    }

    fun updateThemeMode(mode: ThemeMode) {
        viewModelScope.launch { repository.setThemeMode(mode) }
    }

    fun updateAppColor(color: AppColor) {
        viewModelScope.launch { repository.setAppColor(color) }
    }
}
