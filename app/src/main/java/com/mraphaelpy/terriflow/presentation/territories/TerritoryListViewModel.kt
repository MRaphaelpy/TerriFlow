package com.mraphaelpy.terriflow.presentation.territories

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mraphaelpy.terriflow.domain.model.Territory
import com.mraphaelpy.terriflow.domain.model.TerritoryCode
import com.mraphaelpy.terriflow.domain.model.TerritoryStatus
import com.mraphaelpy.terriflow.domain.repository.AuthRepository
import com.mraphaelpy.terriflow.domain.repository.TerritoryRepository
import com.mraphaelpy.terriflow.presentation.components.TerritoryFilter
import com.mraphaelpy.terriflow.presentation.components.TerritoryStatusDisplayOrder
import com.mraphaelpy.terriflow.presentation.components.territoryPalette
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class TerritorySection(val status: TerritoryStatus, val territories: List<Territory>)

data class TerritoryListUiState(
    val sections: List<TerritorySection> = emptyList(),
    val totalCount: Int = 0,
    val visibleCount: Int = 0,
    val statusCounts: Map<TerritoryStatus, Int> = emptyMap(),
    val myCount: Int = 0,
    val filter: TerritoryFilter = TerritoryFilter(),
    val palette: Map<String, Color> = emptyMap(),
    val currentUserId: String? = null,
    val isAdmin: Boolean = false,
    val isLoading: Boolean = true
)

@HiltViewModel
class TerritoryListViewModel @Inject constructor(
    territoryRepository: TerritoryRepository,
    authRepository: AuthRepository
) : ViewModel() {

    private val filter = MutableStateFlow(TerritoryFilter())

    val uiState: StateFlow<TerritoryListUiState> = combine(
        territoryRepository.observeAll(),
        authRepository.observeCurrentUser(),
        filter
    ) { all, user, currentFilter ->
        val visible = currentFilter.apply(all, user?.id)
        TerritoryListUiState(
            sections = groupByStatus(visible),
            totalCount = all.size,
            visibleCount = visible.size,
            statusCounts = all.groupingBy { it.status }.eachCount(),
            myCount = all.count { user != null && it.currentResponsibleId == user.id },
            filter = currentFilter,
            palette = territoryPalette(all),
            currentUserId = user?.id,
            isAdmin = user?.isAdmin == true,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TerritoryListUiState())

    fun updateQuery(query: String) = filter.update { it.copy(query = query) }

    fun toggleStatus(status: TerritoryStatus) =
        filter.update { it.copy(status = if (it.status == status) null else status) }

    fun clearStatus() = filter.update { it.copy(status = null) }

    fun toggleOnlyMine() = filter.update { it.copy(onlyMine = !it.onlyMine) }

    fun clearFilters() {
        filter.value = TerritoryFilter()
    }

    companion object {
        fun groupByStatus(territories: List<Territory>): List<TerritorySection> {
            val byStatus = territories.groupBy { it.status }
            return TerritoryStatusDisplayOrder.mapNotNull { status ->
                byStatus[status]
                    ?.sortedWith(compareBy({ TerritoryCode.numberOf(it.code) ?: Int.MAX_VALUE }, { it.code }))
                    ?.let { TerritorySection(status, it) }
            }
        }
    }
}
