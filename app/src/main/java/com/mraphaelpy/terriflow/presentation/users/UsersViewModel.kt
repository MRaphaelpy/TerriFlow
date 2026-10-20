package com.mraphaelpy.terriflow.presentation.users

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mraphaelpy.terriflow.core.util.normalizedForSearch
import com.mraphaelpy.terriflow.domain.model.TerritoryStatistics
import com.mraphaelpy.terriflow.domain.model.User
import com.mraphaelpy.terriflow.domain.model.UserRole
import com.mraphaelpy.terriflow.domain.model.label
import com.mraphaelpy.terriflow.domain.policy.UserManagementPolicy
import com.mraphaelpy.terriflow.domain.repository.AuthRepository
import com.mraphaelpy.terriflow.domain.repository.TerritoryRepository
import com.mraphaelpy.terriflow.domain.repository.UserRepository
import com.mraphaelpy.terriflow.domain.usecase.user.RenameUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class UserFilter(val label: String) {
    ALL("Todos"),
    ADMINS("Administradores"),
    RESPONSIBLES("Dirigentes"),
    INACTIVE("Inativos");

    fun matches(user: User): Boolean = when (this) {
        ALL -> true
        ADMINS -> user.active && user.isAdmin
        RESPONSIBLES -> user.active && !user.isAdmin
        INACTIVE -> !user.active
    }
}

data class UserSection(val title: String, val users: List<User>)

data class UsersUiState(
    val currentUser: User? = null,
    val sections: List<UserSection> = emptyList(),
    val counts: Map<UserFilter, Int> = emptyMap(),
    val currentTerritories: Map<String, Int> = emptyMap(),
    val query: String = "",
    val filter: UserFilter = UserFilter.ALL,
    val isLoading: Boolean = true
) {
    val total: Int get() = counts[UserFilter.ALL] ?: 0
    val visibleCount: Int get() = sections.sumOf { it.users.size }

    fun canRename(target: User) = UserManagementPolicy.canRename(currentUser, target)
    fun roleChanges(target: User) = UserManagementPolicy.roleChanges(currentUser, target)
    fun canToggleActive(target: User) = UserManagementPolicy.canToggleActive(currentUser, target)
}

@HiltViewModel
class UsersViewModel @Inject constructor(
    private val userRepository: UserRepository,
    authRepository: AuthRepository,
    territoryRepository: TerritoryRepository,
    private val renameUser: RenameUserUseCase
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val filter = MutableStateFlow(UserFilter.ALL)

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    val uiState: StateFlow<UsersUiState> = combine(
        authRepository.observeCurrentUser(),
        userRepository.observeAll(),
        territoryRepository.observeAll(),
        query,
        filter
    ) { currentUser, users, territories, currentQuery, currentFilter ->
        val normalized = currentQuery.normalizedForSearch()
        val visible = users.filter { user ->
            currentFilter.matches(user) && (normalized.isEmpty() ||
                user.name.normalizedForSearch().contains(normalized) ||
                user.email.normalizedForSearch().contains(normalized))
        }
        UsersUiState(
            currentUser = currentUser,
            sections = listOf(
                UserSection("Administradores", visible.filter { it.active && it.isAdmin }),
                UserSection("Dirigentes", visible.filter { it.active && !it.isAdmin }),
                UserSection("Inativos", visible.filter { !it.active })
            ).filter { it.users.isNotEmpty() },
            counts = UserFilter.entries.associateWith { f -> users.count(f::matches) },
            currentTerritories = territories
                .filter { it.status in TerritoryStatistics.ACTIVE_STATUSES }
                .mapNotNull { it.currentResponsibleId }
                .groupingBy { it }
                .eachCount(),
            query = currentQuery,
            filter = currentFilter,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UsersUiState())

    init {
        viewModelScope.launch { runCatching { userRepository.syncFromRemote() } }
    }

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun onFilterSelected(value: UserFilter) {
        filter.value = value
    }

    fun clearMessage() {
        _message.value = null
    }

    fun updateUserRole(user: User, newRole: UserRole) {
        if (newRole !in uiState.value.roleChanges(user)) {
            _message.value = "Você não tem permissão para alterar esta função"
            return
        }
        run("${user.name} agora é ${newRole.label()}", "Erro ao alterar função") {
            userRepository.save(user.copy(role = newRole))
        }
    }

    fun updateUserName(user: User, newName: String) {
        val trimmed = newName.trim()
        when {
            trimmed.isBlank() -> _message.value = "O nome não pode ficar em branco"
            !uiState.value.canRename(user) -> _message.value = "Você não tem permissão para alterar este nome"
            else -> run("Nome atualizado para \"$trimmed\"", "Erro ao atualizar nome") { renameUser(user.id, trimmed) }
        }
    }

    fun toggleUserStatus(user: User) {
        if (!uiState.value.canToggleActive(user)) {
            _message.value = "Você não tem permissão para alterar o status deste usuário"
            return
        }
        val updated = user.copy(active = !user.active)
        run("${user.name} foi ${if (updated.active) "reativado" else "desativado"}", "Erro ao alterar status") {
            userRepository.save(updated)
        }
    }

    private fun run(success: String, failure: String, block: suspend () -> Unit) {
        viewModelScope.launch {
            _message.value = runCatching { block() }.fold(
                onSuccess = { success },
                onFailure = { "$failure: ${it.message ?: "tente novamente"}" }
            )
        }
    }
}
