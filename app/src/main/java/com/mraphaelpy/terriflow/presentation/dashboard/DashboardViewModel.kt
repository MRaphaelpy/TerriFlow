package com.mraphaelpy.terriflow.presentation.dashboard

import android.net.Uri
import androidx.compose.ui.graphics.Color
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
import com.mraphaelpy.terriflow.domain.repository.UserRepository
import com.mraphaelpy.terriflow.domain.model.TerritoryStatistics
import com.mraphaelpy.terriflow.domain.usecase.notification.CheckStaleTerritoriesUseCase
import com.mraphaelpy.terriflow.domain.usecase.notification.RemindResponsibleUseCase
import com.mraphaelpy.terriflow.domain.usecase.user.RenameUserUseCase
import com.mraphaelpy.terriflow.domain.usecase.user.SyncResponsibleProfilesUseCase
import com.mraphaelpy.terriflow.domain.usecase.user.UpdateProfilePhotoUseCase
import com.mraphaelpy.terriflow.presentation.components.territoryPalette
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

data class RecentActivity(val event: TerritoryEvent, val territory: Territory?)

data class DashboardUiState(
    val currentUser: User? = null,
    val totalTerritories: Int = 0,
    val statusCounts: Map<TerritoryStatus, Int> = emptyMap(),
    val myTerritories: List<Territory> = emptyList(),
    val recentActivity: List<RecentActivity> = emptyList(),
    val overdueTerritories: List<Territory> = emptyList(),
    val palette: Map<String, Color> = emptyMap(),
    val unreadCount: Int = 0,
    val isLoggingOut: Boolean = false,
    val isRefreshing: Boolean = false,
    val message: String? = null
) {
    val isAdmin: Boolean get() = currentUser?.isAdmin == true
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val territoryRepository: TerritoryRepository,
    private val eventRepository: TerritoryEventRepository,
    private val notificationRepository: NotificationRepository,
    private val userRepository: UserRepository,
    private val photoEncoder: ProfilePhotoEncoder,
    private val renameUser: RenameUserUseCase,
    private val updateProfilePhoto: UpdateProfilePhotoUseCase,
    private val syncResponsibleProfiles: SyncResponsibleProfilesUseCase,
    private val remindResponsible: RemindResponsibleUseCase
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
                photoEncoder.encode(uri)?.let { updateProfilePhoto(it) }
            }
        }
    }

    fun updateMyName(newName: String) {
        val userId = authRepository.currentUserId ?: return
        viewModelScope.launch { runCatching { renameUser(userId, newName) } }
    }

    fun remind(territoryId: String) {
        viewModelScope.launch {
            val message = runCatching { remindResponsible(territoryId) }
                .fold(onSuccess = { "Pedido enviado ao dirigente" }, onFailure = { it.message ?: "Não foi possível enviar" })
            _uiState.update { it.copy(message = message) }
        }
    }

    fun clearMessage() = _uiState.update { it.copy(message = null) }

    private suspend fun syncFromRemote() {
        runCatching { userRepository.syncFromRemote() }
        runCatching { territoryRepository.syncFromRemote() }
        runCatching { notificationRepository.syncFromRemote() }
        runCatching { syncResponsibleProfiles() }
    }

    private fun observeDashboard() {
        authRepository.observeCurrentUser()
            .onEach { user -> _uiState.update { it.copy(currentUser = user) } }
            .flatMapLatest { user -> user?.let { notificationRepository.countUnread(it.id) } ?: flowOf(0) }
            .onEach { count -> _uiState.update { it.copy(unreadCount = count) } }
            .launchIn(viewModelScope)

        combine(
            territoryRepository.observeAll(),
            eventRepository.observeRecent(RECENT_EVENTS_LIMIT),
            authRepository.observeCurrentUser()
        ) { territories, events, user ->
            val byId = territories.associateBy { it.id }
            _uiState.update {
                it.copy(
                    totalTerritories = territories.size,
                    statusCounts = territories.groupingBy { territory -> territory.status }.eachCount(),
                    myTerritories = territories
                        .filter { territory -> user != null && territory.currentResponsibleId == user.id }
                        .sortedBy { territory -> MY_TERRITORY_ORDER.indexOf(territory.status) },
                    recentActivity = events.map { event -> RecentActivity(event, byId[event.territoryId]) },
                    overdueTerritories = overdue(territories),
                    palette = territoryPalette(territories)
                )
            }
        }.launchIn(viewModelScope)
    }

    private fun overdue(territories: List<Territory>): List<Territory> {
        val now = Date().time
        return territories.filter { territory ->
            if (territory.status !in TerritoryStatistics.ACTIVE_STATUSES) return@filter false
            val since = territory.assignedAt?.time ?: return@filter false
            TimeUnit.MILLISECONDS.toDays(now - since) > OVERDUE_DAYS
        }
    }

    private companion object {
        const val RECENT_EVENTS_LIMIT = 8
        val MY_TERRITORY_ORDER = listOf(
            TerritoryStatus.IN_PROGRESS, TerritoryStatus.PAUSED, TerritoryStatus.ASSIGNED, TerritoryStatus.COMPLETED
        )
        val OVERDUE_DAYS = CheckStaleTerritoriesUseCase.WORKING_MILESTONES.last()
    }
}
