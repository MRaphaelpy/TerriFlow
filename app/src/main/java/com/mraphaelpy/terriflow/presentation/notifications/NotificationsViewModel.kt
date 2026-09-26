package com.mraphaelpy.terriflow.presentation.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mraphaelpy.terriflow.domain.model.AppNotification
import com.mraphaelpy.terriflow.domain.repository.AuthRepository
import com.mraphaelpy.terriflow.domain.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotificationsUiState(
    val notifications: List<AppNotification> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    init {
        val userId = authRepository.currentUserId

        if (userId != null) {
            viewModelScope.launch {
                notificationRepository.observeByUser(userId).collect { notifications ->
                    _uiState.value = NotificationsUiState(
                        notifications = notifications,
                        isLoading = false
                    )
                }
            }
        } else {
            _uiState.value = NotificationsUiState(
                notifications = emptyList(),
                isLoading = false
            )
        }
    }

    fun markRead(id: String) {
        viewModelScope.launch { notificationRepository.markRead(id) }
    }

    fun markAllRead() {
        val userId = authRepository.currentUserId ?: return
        viewModelScope.launch { notificationRepository.markAllRead(userId) }
    }
}
