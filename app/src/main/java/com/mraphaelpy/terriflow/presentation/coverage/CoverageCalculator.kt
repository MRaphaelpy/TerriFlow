package com.mraphaelpy.terriflow.presentation.coverage

import com.mraphaelpy.terriflow.domain.model.AssignmentPeriod
import com.mraphaelpy.terriflow.domain.model.ServiceYear
import com.mraphaelpy.terriflow.domain.model.Territory
import com.mraphaelpy.terriflow.domain.model.TerritoryStatistics
import java.util.Calendar
import java.util.Date

enum class CoverageSortOption(val label: String) {
    OLDEST_COMPLETED_FIRST("Mais tempo sem concluir"),
    PENDING_IN_YEAR("Pendentes no ano de serviço"),
    OVER_12_MONTHS("Sem conclusão há 12 meses"),
    NEVER_WORKED("Nunca concluídos"),
    NEWEST_COMPLETED_FIRST("Concluídos recentemente"),
    MOST_WORKED("Mais concluídos")
}

data class TerritoryCoverage(
    val territory: Territory,
    val lastCompletedDate: Date?,
    val completionCount: Int,
    val completedInYear: Boolean,
    val averageDaysToComplete: Double?
)

object CoverageCalculator {

    fun compute(
        territories: List<Territory>,
        periodsByTerritory: Map<String, List<AssignmentPeriod>>,
        year: ServiceYear
    ): List<TerritoryCoverage> = territories.map { territory ->
        val periods = periodsByTerritory[territory.id].orEmpty()
        val completions = (periods.mapNotNull { it.completedAt } + listOfNotNull(territory.completedAt)).distinct()
        TerritoryCoverage(
            territory = territory,
            lastCompletedDate = TerritoryStatistics.lastCompletion(territory, periods),
            completionCount = periods.count { it.isCompleted }.coerceAtLeast(if (territory.completedAt != null) 1 else 0),
            completedInYear = completions.any { it in year },
            averageDaysToComplete = TerritoryStatistics.averageDaysToComplete(periods)
        )
    }

    fun sort(coverages: List<TerritoryCoverage>, option: CoverageSortOption, now: Date = Date()): List<TerritoryCoverage> =
        when (option) {
            CoverageSortOption.OLDEST_COMPLETED_FIRST ->
                coverages.sortedWith(compareBy(nullsFirst()) { it.lastCompletedDate })
            CoverageSortOption.PENDING_IN_YEAR ->
                coverages.filter { !it.completedInYear }.sortedWith(compareBy(nullsFirst()) { it.lastCompletedDate })
            CoverageSortOption.OVER_12_MONTHS -> {
                val cutoff = Calendar.getInstance().apply { time = now; add(Calendar.MONTH, -12) }.time
                coverages.filter { it.lastCompletedDate == null || it.lastCompletedDate.before(cutoff) }
                    .sortedWith(compareBy(nullsFirst()) { it.lastCompletedDate })
            }
            CoverageSortOption.NEVER_WORKED -> coverages.filter { it.lastCompletedDate == null }
            CoverageSortOption.NEWEST_COMPLETED_FIRST ->
                coverages.sortedWith(compareBy(nullsLast(reverseOrder())) { it.lastCompletedDate })
            CoverageSortOption.MOST_WORKED -> coverages.sortedByDescending { it.completionCount }
        }
}
