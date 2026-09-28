package com.mraphaelpy.terriflow.presentation.territories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mraphaelpy.terriflow.domain.model.Territory
import com.mraphaelpy.terriflow.domain.model.UserRole
import com.mraphaelpy.terriflow.domain.repository.AuthRepository
import com.mraphaelpy.terriflow.domain.repository.TerritoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TerritoryFilter(
    val query: String = "",
    val status: String = "",
    val responsibleId: String = ""
)

data class TerritoryListUiState(
    val territories: List<Territory> = emptyList(),
    val filter: TerritoryFilter = TerritoryFilter(),
    val isAdmin: Boolean = false,
    val isLoading: Boolean = true,
    val currentUserId: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TerritoryListViewModel @Inject constructor(
    private val territoryRepository: TerritoryRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _filter = MutableStateFlow(TerritoryFilter())
    private val _isAdmin = MutableStateFlow(false)
    private val _isLoading = MutableStateFlow(true)
    private val _currentUserId = MutableStateFlow<String?>(null)

    val uiState: StateFlow<TerritoryListUiState> = combine(
        _filter.flatMapLatest { f ->
            territoryRepository.search(f.query, f.status, f.responsibleId)
        },
        _filter,
        _isAdmin,
        _isLoading,
        _currentUserId
    ) { territories, filter, isAdmin, isLoading, currentUserId ->
        TerritoryListUiState(territories, filter, isAdmin, isLoading, currentUserId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TerritoryListUiState())

    init {
        viewModelScope.launch {
            val user = authRepository.getCurrentUser()
            _isAdmin.value = user?.role == UserRole.ADMIN || user?.role == UserRole.SUPER_ADMIN
            _currentUserId.value = user?.id
            _isLoading.value = false
        }
    }

    fun updateQuery(query: String) {
        _filter.update { it.copy(query = query) }
    }

    fun updateStatusFilter(status: String) {
        _filter.update { it.copy(status = status) }
    }

    fun clearFilters() {
        _filter.value = TerritoryFilter()
    }
}
