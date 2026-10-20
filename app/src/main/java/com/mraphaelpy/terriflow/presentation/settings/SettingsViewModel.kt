package com.mraphaelpy.terriflow.presentation.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mraphaelpy.terriflow.core.image.ProfilePhotoEncoder
import com.mraphaelpy.terriflow.core.updater.AppUpdateInfo
import com.mraphaelpy.terriflow.core.updater.AppUpdateManager
import com.mraphaelpy.terriflow.domain.model.Congregation
import com.mraphaelpy.terriflow.domain.model.User
import com.mraphaelpy.terriflow.domain.repository.AppColor
import com.mraphaelpy.terriflow.domain.repository.AuthRepository
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
    val currentUser: User? = null,
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
    private val authRepository: AuthRepository,
    private val photoEncoder: ProfilePhotoEncoder,
    val updateManager: AppUpdateManager
) : ViewModel() {

    private data class CongregationState(val congregation: Congregation? = null, val isLoading: Boolean = true)

    private data class UpdateCheckState(
        val isChecking: Boolean = false,
        val availableUpdate: AppUpdateInfo? = null,
        val message: String? = null
    )

    private val congregationState = MutableStateFlow(CongregationState())
    private val updateState = MutableStateFlow(UpdateCheckState())

    val uiState = combine(
        authRepository.observeCurrentUser(),
        repository.themeMode,
        repository.appColor,
        congregationState,
        updateState
    ) { user, mode, color, congregation, update ->
        SettingsUiState(
            currentUser = user,
            themeMode = mode,
            appColor = color,
            congregation = congregation.congregation,
            congregationLoading = congregation.isLoading,
            isCheckingUpdate = update.isChecking,
            availableUpdate = update.availableUpdate,
            updateMessage = update.message
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    init {
        loadCongregation()
    }

    private fun loadCongregation() {
        viewModelScope.launch {
            // Sem conexão a consulta pode falhar; a tela apenas fica sem os dados da congregação
            val congregation = runCatching {
                congregationRepository.getCurrentCongregationId()?.let { congregationRepository.getById(it) }
            }.getOrNull()
            congregationState.value = CongregationState(congregation, isLoading = false)
        }
    }

    fun updateMyName(newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            runCatching { authRepository.updateName(trimmed) }
        }
    }

    fun updateProfilePicture(uri: Uri) {
        viewModelScope.launch {
            runCatching {
                photoEncoder.encode(uri)?.let { authRepository.updatePhotoUrl(it) }
            }
        }
    }

    fun checkForUpdates() {
        viewModelScope.launch {
            updateState.value = UpdateCheckState(isChecking = true)
            val info = updateManager.checkForUpdate()
            updateState.value = when {
                info == null -> UpdateCheckState(message = "Não foi possível verificar atualizações. Verifique sua conexão.")
                info.hasUpdate -> UpdateCheckState(availableUpdate = info)
                else -> UpdateCheckState(message = "O aplicativo já está na versão mais recente (v${info.currentVersion})")
            }
        }
    }

    fun dismissUpdateDialog() {
        updateState.update { it.copy(availableUpdate = null) }
    }

    fun clearUpdateMessage() {
        updateState.update { it.copy(message = null) }
    }

    fun updateThemeMode(mode: ThemeMode) {
        viewModelScope.launch { repository.setThemeMode(mode) }
    }

    fun updateAppColor(color: AppColor) {
        viewModelScope.launch { repository.setAppColor(color) }
    }
}
