package com.mraphaelpy.terriflow.presentation.coverage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mraphaelpy.terriflow.domain.model.EventType
import com.mraphaelpy.terriflow.domain.model.Territory
import com.mraphaelpy.terriflow.domain.model.TerritoryEvent
import com.mraphaelpy.terriflow.domain.repository.TerritoryEventRepository
import com.mraphaelpy.terriflow.domain.repository.TerritoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

enum class CoverageSortOption {
    NEVER_WORKED,
    OLDEST_COMPLETED_FIRST,
    NEWEST_COMPLETED_FIRST,
    MOST_WORKED
}

data class TerritoryCoverage(
    val territory: Territory,
    val lastCompletedDate: Date?,
    val completionCount: Int,
    val historyPreview: List<TerritoryEvent>
)

data class CoverageUiState(
    val coverages: List<TerritoryCoverage> = emptyList(),
    val sortOption: CoverageSortOption = CoverageSortOption.OLDEST_COMPLETED_FIRST,
    val isLoading: Boolean = true
)

@HiltViewModel
class CoverageViewModel @Inject constructor(
    private val territoryRepository: TerritoryRepository,
    private val eventRepository: TerritoryEventRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CoverageUiState())
    val uiState: StateFlow<CoverageUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            combine(
                territoryRepository.observeAll(),
                eventRepository.observeAll()
            ) { territories, events ->
                
                val eventsByTerritory = events.groupBy { it.territoryId }
                
                val coverages = territories.map { territory ->
                    val tEvents = eventsByTerritory[territory.id]?.sortedByDescending { it.timestamp } ?: emptyList()
                    val completedEvents = tEvents.filter { it.type == EventType.COMPLETED }
                    
                    val completionCount = completedEvents.size
                    val lastCompletedDate = completedEvents.firstOrNull()?.timestamp ?: territory.completedAt
                    
                    TerritoryCoverage(
                        territory = territory,
                        lastCompletedDate = lastCompletedDate,
                        completionCount = completionCount,
                        historyPreview = tEvents.take(3)
                    )
                }
                
                coverages
            }.collect { unsorted ->
                _uiState.update { it.copy(isLoading = false) }
                applySort(unsorted, _uiState.value.sortOption)
            }
        }
    }

    fun setSortOption(option: CoverageSortOption) {
        _uiState.update { it.copy(sortOption = option) }
        applySort(_uiState.value.coverages, option)
    }

    private fun applySort(list: List<TerritoryCoverage>, option: CoverageSortOption) {
        val sorted = when (option) {
            CoverageSortOption.NEVER_WORKED -> {
                list.filter { it.lastCompletedDate == null }
            }
            CoverageSortOption.OLDEST_COMPLETED_FIRST -> {
                list.sortedWith(compareBy(nullsFirst()) { it.lastCompletedDate })
            }
            CoverageSortOption.NEWEST_COMPLETED_FIRST -> {
                list.sortedWith(compareByDescending(nullsLast()) { it.lastCompletedDate })
            }
            CoverageSortOption.MOST_WORKED -> {
                list.sortedByDescending { it.completionCount }
            }
        }
        _uiState.update { it.copy(coverages = sorted) }
    }
}
