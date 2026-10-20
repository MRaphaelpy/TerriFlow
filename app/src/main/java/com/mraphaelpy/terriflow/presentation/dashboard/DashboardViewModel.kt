package com.mraphaelpy.terriflow.presentation.dashboard

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mraphaelpy.terriflow.core.image.ProfilePhotoEncoder
import com.mraphaelpy.terriflow.domain.model.Territory
import com.mraphaelpy.terriflow.domain.model.TerritoryEvent
import com.mraphaelpy.terriflow.domain.model.TerritoryStatus
import com.mraphaelpy.terriflow.domain.model.User
import com.mraphaelpy.terriflow.domain.repository.AuthRepository
import com.mraphaelpy.terriflow.domain.repository.NotificationRepository
import com.mraphaelpy.terriflow.domain.repository.TerritoryEventRepository
import com.mraphaelpy.terriflow.domain.repository.TerritoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date
import java.util.concurrent.TimeUnit
import javax.inject.Inject

data class DashboardStats(
    val available: Int = 0,
    val assigned: Int = 0,
    val inProgress: Int = 0,
    val paused: Int = 0,
    val completed: Int = 0
)

data class DashboardUiState(
    val currentUser: User? = null,
    val stats: DashboardStats = DashboardStats(),
    val recentEvents: List<TerritoryEvent> = emptyList(),
    val overdueTerritories: List<Territory> = emptyList(),
    val unreadCount: Int = 0,
    val isLoggingOut: Boolean = false,
    val isRefreshing: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val territoryRepository: TerritoryRepository,
    private val eventRepository: TerritoryEventRepository,
    private val notificationRepository: NotificationRepository,
    private val photoEncoder: ProfilePhotoEncoder
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        observeDashboard()
        viewModelScope.launch { syncFromRemote() }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            syncFromRemote()
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    fun logout(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoggingOut = true) }
            authRepository.logout()
            _uiState.update { it.copy(isLoggingOut = false) }
            onSuccess()
        }
    }

    fun updateProfilePicture(uri: Uri) {
        viewModelScope.launch {
            runCatching {
                photoEncoder.encode(uri)?.let { authRepository.updatePhotoUrl(it) }
            }
        }
    }

    fun updateMyName(newName: String) {
        viewModelScope.launch { runCatching { authRepository.updateName(newName) } }
    }

    private suspend fun syncFromRemote() {
        runCatching { territoryRepository.syncFromRemote() }
        runCatching { notificationRepository.syncFromRemote() }
    }

    private fun observeDashboard() {
        authRepository.observeCurrentUser()
            .onEach { user -> _uiState.update { it.copy(currentUser = user) } }
            .flatMapLatest { user -> user?.let { notificationRepository.countUnread(it.id) } ?: flowOf(0) }
            .onEach { count -> _uiState.update { it.copy(unreadCount = count) } }
            .launchIn(viewModelScope)

        combine(
            territoryRepository.countByStatus(TerritoryStatus.AVAILABLE),
            territoryRepository.countByStatus(TerritoryStatus.ASSIGNED),
            territoryRepository.countByStatus(TerritoryStatus.IN_PROGRESS),
            territoryRepository.countByStatus(TerritoryStatus.PAUSED),
            territoryRepository.countByStatus(TerritoryStatus.COMPLETED),
            ::DashboardStats
        ).onEach { stats -> _uiState.update { it.copy(stats = stats) } }
            .launchIn(viewModelScope)

        eventRepository.observeRecent(RECENT_EVENTS_LIMIT)
            .onEach { events -> _uiState.update { it.copy(recentEvents = events) } }
            .launchIn(viewModelScope)

        territoryRepository.observeByStatus(TerritoryStatus.IN_PROGRESS)
            .onEach { inProgress -> _uiState.update { it.copy(overdueTerritories = overdue(inProgress)) } }
            .launchIn(viewModelScope)
    }

    private fun overdue(inProgress: List<Territory>): List<Territory> {
        val now = Date().time
        return inProgress.filter { territory ->
            val since = territory.startedAt?.time ?: territory.assignedAt?.time ?: return@filter false
            TimeUnit.MILLISECONDS.toDays(now - since) > OVERDUE_DAYS
        }
    }

    private companion object {
        const val RECENT_EVENTS_LIMIT = 20
        const val OVERDUE_DAYS = 90
    }
}
