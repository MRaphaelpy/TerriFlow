package com.mraphaelpy.terriflow.presentation.settings

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
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
    val updateManager: AppUpdateManager
) : ViewModel() {

    private val _congregationState = MutableStateFlow<Pair<Congregation?, Boolean>>(null to true)
    private val _updateState = MutableStateFlow<Triple<Boolean, AppUpdateInfo?, String?>>(Triple(false, null, null))

    val uiState = combine(
        authRepository.observeCurrentUser(),
        repository.themeMode,
        repository.appColor,
        _congregationState,
        _updateState
    ) { user, mode, color, (congregation, loading), (isChecking, updateInfo, message) ->
        SettingsUiState(
            currentUser = user,
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

    fun updateMyName(newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            authRepository.updateName(trimmed)
        }
    }

    fun updateProfilePicture(uri: Uri, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                if (bitmap != null) {
                    val maxSize = 300
                    val ratio = bitmap.width.toFloat() / bitmap.height.toFloat()
                    val width = if (ratio > 1) maxSize else (maxSize * ratio).toInt()
                    val height = if (ratio > 1) (maxSize / ratio).toInt() else maxSize
                    val scaled = Bitmap.createScaledBitmap(bitmap, width, height, true)

                    val outputStream = ByteArrayOutputStream()
                    scaled.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
                    val byteArray = outputStream.toByteArray()
                    val base64 = Base64.encodeToString(byteArray, Base64.NO_WRAP)

                    val dataUri = "data:image/jpeg;base64,$base64"
                    authRepository.updatePhotoUrl(dataUri)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
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
