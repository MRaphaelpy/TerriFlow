package com.mraphaelpy.terriflow.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mraphaelpy.terriflow.domain.model.Territory
import com.mraphaelpy.terriflow.domain.model.TerritoryEvent
import com.mraphaelpy.terriflow.domain.model.TerritoryStatus
import com.mraphaelpy.terriflow.domain.model.User
import com.mraphaelpy.terriflow.domain.repository.AuthRepository
import com.mraphaelpy.terriflow.domain.repository.NotificationRepository
import com.mraphaelpy.terriflow.domain.repository.TerritoryEventRepository
import com.mraphaelpy.terriflow.domain.repository.TerritoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
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
    val completed: Int = 0,
    val returned: Int = 0,
    val total: Int = 0
)

data class DashboardUiState(
    val currentUser: User? = null,
    val stats: DashboardStats = DashboardStats(),
    val recentEvents: List<TerritoryEvent> = emptyList(),
    val overdueTerritories: List<Territory> = emptyList(),
    val unreadCount: Int = 0,
    val isLoading: Boolean = true,
    val isLoggingOut: Boolean = false,
    val isRefreshing: Boolean = false
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val territoryRepository: TerritoryRepository,
    private val eventRepository: TerritoryEventRepository,
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboard()
        syncNow()
    }

    private fun syncNow() {
        viewModelScope.launch {
            runCatching {
                territoryRepository.syncFromRemote()
            }
            runCatching {
                notificationRepository.syncFromRemote()
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            runCatching {
                territoryRepository.syncFromRemote()
            }
            runCatching {
                notificationRepository.syncFromRemote()
            }
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    private fun loadDashboard() {
        viewModelScope.launch {
            authRepository.observeCurrentUser()
                .filterNotNull()
                .collect { user ->
                    _uiState.update { it.copy(currentUser = user, isLoading = false) }
                    observeStats(user)
                    observeUnread(user.id)
                }
        }
        viewModelScope.launch {
            eventRepository.observeRecent(20).collect { events ->
                _uiState.update { it.copy(recentEvents = events) }
            }
        }
        viewModelScope.launch {
            territoryRepository.observeByStatus(TerritoryStatus.IN_PROGRESS).collect { inProgress ->
                val now = Date().time
                val overdue = inProgress.filter { territory ->
                    val startedAt = territory.startedAt?.time ?: territory.assignedAt?.time ?: return@filter false
                    val diff = now - startedAt
                    TimeUnit.MILLISECONDS.toDays(diff) > 90
                }
                _uiState.update { it.copy(overdueTerritories = overdue) }
            }
        }
    }

    private fun observeStats(user: User) {
        viewModelScope.launch {
            combine(
                territoryRepository.countByStatus(TerritoryStatus.AVAILABLE),
                territoryRepository.countByStatus(TerritoryStatus.ASSIGNED),
                territoryRepository.countByStatus(TerritoryStatus.IN_PROGRESS),
                territoryRepository.countByStatus(TerritoryStatus.PAUSED),
                territoryRepository.countByStatus(TerritoryStatus.COMPLETED)
            ) { available, assigned, inProgress, paused, completed ->
                DashboardStats(
                    available = available,
                    assigned = assigned,
                    inProgress = inProgress,
                    paused = paused,
                    completed = completed,
                    total = available + assigned + inProgress + paused + completed
                )
            }.collect { stats ->
                _uiState.update { it.copy(stats = stats) }
            }
        }
    }

    private fun observeUnread(userId: String) {
        viewModelScope.launch {
            notificationRepository.countUnread(userId).collect { count ->
                _uiState.update { it.copy(unreadCount = count) }
            }
        }
    }

    fun logout(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoggingOut = true) }
            authRepository.logout()
            _uiState.update { it.copy(isLoggingOut = false) }
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                onSuccess()
            }
        }
    }

    fun updateProfilePicture(uri: android.net.Uri, context: android.content.Context) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                if (bitmap != null) {
                    val maxSize = 300
                    val ratio = bitmap.width.toFloat() / bitmap.height.toFloat()
                    val width = if (ratio > 1) maxSize else (maxSize * ratio).toInt()
                    val height = if (ratio > 1) (maxSize / ratio).toInt() else maxSize
                    val scaled = android.graphics.Bitmap.createScaledBitmap(bitmap, width, height, true)
                    
                    val outputStream = java.io.ByteArrayOutputStream()
                    scaled.compress(android.graphics.Bitmap.CompressFormat.JPEG, 70, outputStream)
                    val byteArray = outputStream.toByteArray()
                    val base64 = android.util.Base64.encodeToString(byteArray, android.util.Base64.NO_WRAP)
                    
                    val dataUri = "data:image/jpeg;base64,$base64"
                    authRepository.updatePhotoUrl(dataUri)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateMyName(newName: String) {
        viewModelScope.launch {
            authRepository.updateName(newName)
        }
    }
}
