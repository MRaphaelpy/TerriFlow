package com.mraphaelpy.terriflow.presentation.audit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mraphaelpy.terriflow.domain.model.TerritoryEvent
import com.mraphaelpy.terriflow.domain.repository.TerritoryEventRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuditUiState(
    val events: List<TerritoryEvent> = emptyList(),
    val users: Map<String, com.mraphaelpy.terriflow.domain.model.User> = emptyMap(),
    val isLoading: Boolean = true
)

@HiltViewModel
class AuditViewModel @Inject constructor(
    private val eventRepository: TerritoryEventRepository,
    private val userRepository: com.mraphaelpy.terriflow.domain.repository.UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuditUiState())
    val uiState: StateFlow<AuditUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                eventRepository.observeRecent(200),
                userRepository.observeAll()
            ) { events, users ->
                AuditUiState(
                    events = events,
                    users = users.associateBy { it.id },
                    isLoading = false
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }
}
