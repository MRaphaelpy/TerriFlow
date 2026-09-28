package com.mraphaelpy.terriflow.presentation.users

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mraphaelpy.terriflow.domain.model.User
import com.mraphaelpy.terriflow.domain.model.UserRole
import com.mraphaelpy.terriflow.domain.repository.AuthRepository
import com.mraphaelpy.terriflow.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UsersUiState(
    val currentUser: User? = null,
    val users: List<User> = emptyList(),
    val filteredUsers: List<User> = emptyList(),
    val searchQuery: String = "",
    val filterRole: UserRole? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val successMessage: String? = null
) {
    val isAdmin: Boolean
        get() = currentUser?.role in listOf(UserRole.ADMIN, UserRole.SUPER_ADMIN)

    val isSuperAdmin: Boolean
        get() = currentUser?.role == UserRole.SUPER_ADMIN
}

@HiltViewModel
class UsersViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(UsersUiState())
    val uiState: StateFlow<UsersUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    private val _filterRole = MutableStateFlow<UserRole?>(null)

    init {
        viewModelScope.launch {
            runCatching { userRepository.syncFromRemote() }
        }

        viewModelScope.launch {
            combine(
                authRepository.observeCurrentUser(),
                userRepository.observeAll(),
                _searchQuery,
                _filterRole
            ) { currentUser, users, query, roleFilter ->
                val filtered = users.filter { user ->
                    val matchesQuery = query.isBlank() ||
                        user.name.contains(query, ignoreCase = true) ||
                        user.email.contains(query, ignoreCase = true)
                    val matchesRole = roleFilter == null || user.role == roleFilter
                    matchesQuery && matchesRole
                }
                UsersUiState(
                    currentUser = currentUser,
                    users = users,
                    filteredUsers = filtered,
                    searchQuery = query,
                    filterRole = roleFilter,
                    isLoading = false
                )
            }.collect { state ->
                _uiState.update { current ->
                    state.copy(
                        error = current.error,
                        successMessage = current.successMessage
                    )
                }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onFilterRoleSelected(role: UserRole?) {
        _filterRole.value = role
    }

    fun updateUserRole(user: User, newRole: UserRole) {
        val current = _uiState.value.currentUser
        if (current?.role !in listOf(UserRole.ADMIN, UserRole.SUPER_ADMIN)) {
            _uiState.update { it.copy(error = "Você não tem permissão para alterar funções") }
            return
        }

        if (user.role == UserRole.SUPER_ADMIN && current?.role != UserRole.SUPER_ADMIN) {
            _uiState.update { it.copy(error = "Apenas o Super Admin pode alterar a função do criador") }
            return
        }

        viewModelScope.launch {
            val updatedUser = user.copy(role = newRole)
            runCatching {
                userRepository.save(updatedUser)
            }.onSuccess {
                val roleName = when (newRole) {
                    UserRole.SUPER_ADMIN -> "Super Administrador"
                    UserRole.ADMIN -> "Administrador"
                    UserRole.RESPONSIBLE -> "Responsável"
                }
                _uiState.update { it.copy(successMessage = "${user.name} agora é $roleName") }
            }.onFailure { e ->
                _uiState.update { it.copy(error = "Erro ao alterar função: ${e.message ?: "Tente novamente"}") }
            }
        }
    }

    fun updateUserName(user: User, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isBlank()) {
            _uiState.update { it.copy(error = "O nome não pode ficar em branco") }
            return
        }

        val current = _uiState.value.currentUser
        val isSelf = current?.id == user.id
        val isAdmin = current?.role in listOf(UserRole.ADMIN, UserRole.SUPER_ADMIN)

        if (!isSelf && !isAdmin) {
            _uiState.update { it.copy(error = "Você não tem permissão para alterar o nome de outro usuário") }
            return
        }

        viewModelScope.launch {
            runCatching {
                if (isSelf) {
                    authRepository.updateName(trimmed)
                } else {
                    userRepository.updateUserName(user.id, trimmed)
                }
            }.onSuccess {
                _uiState.update { it.copy(successMessage = "Nome atualizado para \"$trimmed\" com sucesso") }
            }.onFailure { e ->
                _uiState.update { it.copy(error = "Erro ao atualizar nome: ${e.message ?: "Tente novamente"}") }
            }
        }
    }

    fun toggleUserStatus(user: User) {
        val current = _uiState.value.currentUser
        if (current?.role !in listOf(UserRole.ADMIN, UserRole.SUPER_ADMIN)) {
            _uiState.update { it.copy(error = "Você não tem permissão para alterar o status do usuário") }
            return
        }

        if (user.role == UserRole.SUPER_ADMIN) {
            _uiState.update { it.copy(error = "Não é possível desativar o Super Admin") }
            return
        }

        if (current != null && user.id == current.id) {
            _uiState.update { it.copy(error = "Você não pode desativar sua própria conta") }
            return
        }

        val updatedUser = user.copy(active = !user.active)
        viewModelScope.launch {
            runCatching {
                userRepository.save(updatedUser)
            }.onSuccess {
                val statusText = if (updatedUser.active) "reativado" else "desativado"
                _uiState.update { it.copy(successMessage = "${user.name} foi $statusText") }
            }.onFailure { e ->
                _uiState.update { it.copy(error = "Erro ao alterar status: ${e.message ?: "Tente novamente"}") }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearSuccessMessage() {
        _uiState.update { it.copy(successMessage = null) }
    }
}
