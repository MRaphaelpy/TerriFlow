package com.mraphaelpy.terriflow.presentation.map

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mraphaelpy.terriflow.domain.model.Territory
import com.mraphaelpy.terriflow.domain.model.TerritoryStatus
import com.mraphaelpy.terriflow.domain.model.isMapped
import com.mraphaelpy.terriflow.domain.repository.AuthRepository
import com.mraphaelpy.terriflow.domain.repository.TerritoryRepository
import com.mraphaelpy.terriflow.presentation.components.TerritoryFilter
import com.mraphaelpy.terriflow.presentation.components.territoryPalette
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MapUiState(
    val mappedTerritories: List<Territory> = emptyList(),
    val visibleTerritories: List<Territory> = emptyList(),
    val unmappedCount: Int = 0,
    val statusCounts: Map<TerritoryStatus, Int> = emptyMap(),
    val myCount: Int = 0,
    val currentUserId: String? = null,
    val filter: TerritoryFilter = TerritoryFilter(),
    val palette: Map<String, Color> = emptyMap(),
    val isLoading: Boolean = true
)

@HiltViewModel
class MapViewModel @Inject constructor(
    private val territoryRepository: TerritoryRepository,
    authRepository: AuthRepository
) : ViewModel() {

    private val filter = MutableStateFlow(TerritoryFilter())

    val uiState: StateFlow<MapUiState> = combine(
        territoryRepository.observeAll(),
        authRepository.observeCurrentUser(),
        filter
    ) { all, user, currentFilter ->
        val mapped = all.filter { it.isMapped }
        MapUiState(
            mappedTerritories = mapped,
            visibleTerritories = currentFilter.apply(mapped, user?.id),
            unmappedCount = all.size - mapped.size,
            statusCounts = mapped.groupingBy { it.status }.eachCount(),
            myCount = mapped.count { user != null && it.currentResponsibleId == user.id },
            currentUserId = user?.id,
            filter = currentFilter,
            palette = territoryPalette(all),
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MapUiState())

    init {
        viewModelScope.launch { runCatching { territoryRepository.syncFromRemote() } }
    }

    fun toggleStatus(status: TerritoryStatus) =
        filter.update { it.copy(status = if (it.status == status) null else status) }

    fun clearStatus() = filter.update { it.copy(status = null) }

    fun toggleOnlyMine() = filter.update { it.copy(onlyMine = !it.onlyMine) }

    fun updateQuery(query: String) = filter.update { it.copy(query = query) }

    fun clearFilters() {
        filter.value = TerritoryFilter()
    }
}
