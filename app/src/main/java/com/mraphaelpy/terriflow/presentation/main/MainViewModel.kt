package com.mraphaelpy.terriflow.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mraphaelpy.terriflow.core.updater.AppUpdateInfo
import com.mraphaelpy.terriflow.core.updater.AppUpdateManager
import com.mraphaelpy.terriflow.domain.repository.AppColor
import com.mraphaelpy.terriflow.domain.repository.AuthRepository
import com.mraphaelpy.terriflow.domain.repository.CongregationRepository
import com.mraphaelpy.terriflow.domain.repository.SettingsRepository
import com.mraphaelpy.terriflow.domain.repository.ThemeMode
import com.mraphaelpy.terriflow.presentation.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val congregationRepository: CongregationRepository,
    val updateManager: AppUpdateManager,
    settingsRepository: SettingsRepository
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> =
        settingsRepository.themeMode.stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.SYSTEM)
    val appColor: StateFlow<AppColor> =
        settingsRepository.appColor.stateIn(viewModelScope, SharingStarted.Eagerly, AppColor.DEFAULT)

    private val _startDestination = MutableStateFlow<String?>(null)
    val startDestination: StateFlow<String?> = _startDestination.asStateFlow()

    private val _availableUpdate = MutableStateFlow<AppUpdateInfo?>(null)
    val availableUpdate: StateFlow<AppUpdateInfo?> = _availableUpdate.asStateFlow()

    init {
        resolveStartDestination()
        registerDeviceToken()
        checkForUpdate()
    }

    fun dismissUpdate() {
        _availableUpdate.value?.let { updateManager.snooze(it.latestVersion) }
        _availableUpdate.value = null
    }

    private fun resolveStartDestination() {
        viewModelScope.launch {
            _startDestination.value = when {
                !authRepository.isLoggedIn -> Screen.Login.route
                congregationRepository.getCurrentCongregationId() != null -> Screen.Dashboard.route
                else -> Screen.CongregationSetup.route
            }
        }
    }

    private fun registerDeviceToken() {
        viewModelScope.launch {
            runCatching { authRepository.registerDeviceToken() }
        }
    }

    private fun checkForUpdate() {
        viewModelScope.launch {
            updateManager.cleanUpInstalledUpdate()
            val info = updateManager.checkForUpdate() ?: return@launch
            if (info.hasUpdate && !updateManager.isSnoozed(info.latestVersion)) {
                _availableUpdate.value = info
            }
        }
    }
}
