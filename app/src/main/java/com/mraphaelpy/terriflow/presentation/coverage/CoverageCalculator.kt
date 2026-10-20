package com.mraphaelpy.terriflow.presentation.coverage

import com.mraphaelpy.terriflow.domain.model.EventType
import com.mraphaelpy.terriflow.domain.model.Territory
import com.mraphaelpy.terriflow.domain.model.TerritoryEvent

/** Cálculos de cobertura, sem dependência de Android (testável isoladamente). */
object CoverageCalculator {

    private const val HISTORY_PREVIEW_SIZE = 3

    fun compute(territories: List<Territory>, events: List<TerritoryEvent>): List<TerritoryCoverage> {
        val eventsByTerritory = events.groupBy { it.territoryId }
        return territories.map { territory ->
            val territoryEvents = eventsByTerritory[territory.id].orEmpty().sortedByDescending { it.timestamp }
            val completedEvents = territoryEvents.filter { it.type == EventType.COMPLETED }
            TerritoryCoverage(
                territory = territory,
                lastCompletedDate = completedEvents.firstOrNull()?.timestamp ?: territory.completedAt,
                completionCount = completedEvents.size,
                historyPreview = territoryEvents.take(HISTORY_PREVIEW_SIZE)
            )
        }
    }

    fun sort(coverages: List<TerritoryCoverage>, option: CoverageSortOption): List<TerritoryCoverage> =
        when (option) {
            CoverageSortOption.NEVER_WORKED -> coverages.filter { it.lastCompletedDate == null }
            CoverageSortOption.OLDEST_COMPLETED_FIRST ->
                coverages.sortedWith(compareBy(nullsFirst()) { it.lastCompletedDate })
            // compareByDescending(nullsLast()) inverteria também os nulos, colocando
            // territórios nunca trabalhados no topo
            CoverageSortOption.NEWEST_COMPLETED_FIRST ->
                coverages.sortedWith(compareBy(nullsLast(reverseOrder())) { it.lastCompletedDate })
            CoverageSortOption.MOST_WORKED -> coverages.sortedByDescending { it.completionCount }
        }
}
