package com.mraphaelpy.terriflow.presentation.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mraphaelpy.terriflow.core.util.DayGroup
import com.mraphaelpy.terriflow.core.util.DayGrouping
import com.mraphaelpy.terriflow.domain.model.AppNotification
import com.mraphaelpy.terriflow.domain.repository.AuthRepository
import com.mraphaelpy.terriflow.domain.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotificationsUiState(
    val days: List<DayGroup<AppNotification>> = emptyList(),
    val unreadCount: Int = 0,
    val totalCount: Int = 0,
    val onlyUnread: Boolean = false,
    val isLoading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val onlyUnread = MutableStateFlow(false)

    val uiState: StateFlow<NotificationsUiState> = combine(
        authRepository.observeCurrentUserId().flatMapLatest { uid ->
            uid?.let { notificationRepository.observeByUser(it) } ?: flowOf(emptyList())
        },
        onlyUnread
    ) { notifications, unreadOnly ->
        val visible = if (unreadOnly) notifications.filter { !it.read } else notifications
        NotificationsUiState(
            days = DayGrouping.group(visible) { it.createdAt },
            unreadCount = notifications.count { !it.read },
            totalCount = notifications.size,
            onlyUnread = unreadOnly,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NotificationsUiState())

    init {
        viewModelScope.launch { runCatching { notificationRepository.syncFromRemote() } }
    }

    fun setOnlyUnread(value: Boolean) {
        onlyUnread.value = value
    }

    fun markRead(id: String) {
        viewModelScope.launch { notificationRepository.markRead(id) }
    }

    fun markAllRead() {
        val userId = authRepository.currentUserId ?: return
        viewModelScope.launch { notificationRepository.markAllRead(userId) }
    }
}
