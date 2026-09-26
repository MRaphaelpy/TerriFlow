package com.mraphaelpy.terriflow.presentation.users

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mraphaelpy.terriflow.domain.model.User
import com.mraphaelpy.terriflow.domain.model.UserRole
import com.mraphaelpy.terriflow.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.mraphaelpy.terriflow.domain.repository.AuthRepository
import kotlinx.coroutines.flow.combine

data class UsersUiState(
    val currentUser: User? = null,
    val users: List<User> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class UsersViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(UsersUiState())
    val uiState: StateFlow<UsersUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                authRepository.observeCurrentUser(),
                userRepository.observeAll()
            ) { currentUser, users ->
                UsersUiState(
                    currentUser = currentUser,
                    users = users,
                    isLoading = false
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun toggleUserRole(user: User) {
        val newRole = if (user.role == UserRole.ADMIN) UserRole.RESPONSIBLE else UserRole.ADMIN
        val updatedUser = user.copy(role = newRole)
        viewModelScope.launch {
            runCatching {
                userRepository.save(updatedUser)
            }.onFailure {
                _uiState.value = _uiState.value.copy(error = "Erro ao atualizar usuário")
            }
        }
    }

    fun toggleUserStatus(user: User) {
        val updatedUser = user.copy(active = !user.active)
        viewModelScope.launch {
            runCatching {
                userRepository.save(updatedUser)
            }.onFailure {
                _uiState.value = _uiState.value.copy(error = "Erro ao atualizar usuário")
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
