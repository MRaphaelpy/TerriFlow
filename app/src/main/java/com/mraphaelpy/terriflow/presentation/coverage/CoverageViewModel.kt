package com.mraphaelpy.terriflow.presentation.coverage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mraphaelpy.terriflow.domain.model.Territory
import com.mraphaelpy.terriflow.domain.model.TerritoryEvent
import com.mraphaelpy.terriflow.domain.repository.TerritoryEventRepository
import com.mraphaelpy.terriflow.domain.repository.TerritoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
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
    territoryRepository: TerritoryRepository,
    eventRepository: TerritoryEventRepository
) : ViewModel() {

    private val sortOption = MutableStateFlow(CoverageSortOption.OLDEST_COMPLETED_FIRST)

    private val allCoverages = combine(
        territoryRepository.observeAll(),
        eventRepository.observeAll(),
        CoverageCalculator::compute
    )

    // A ordenação é sempre aplicada sobre a lista completa; antes ela era aplicada
    // sobre o resultado do filtro anterior e "Nunca trabalhados" fazia territórios sumirem.
    val uiState: StateFlow<CoverageUiState> = combine(allCoverages, sortOption) { coverages, option ->
        CoverageUiState(
            coverages = CoverageCalculator.sort(coverages, option),
            sortOption = option,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CoverageUiState())

    fun setSortOption(option: CoverageSortOption) {
        sortOption.value = option
    }
}
