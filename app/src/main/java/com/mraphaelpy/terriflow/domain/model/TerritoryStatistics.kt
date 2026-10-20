package com.mraphaelpy.terriflow.domain.model

import java.util.Calendar
import java.util.Date

data class ServiceYearCoverage(
    val year: ServiceYear,
    val total: Int,
    val worked: List<Territory>,
    val pending: List<Territory>
) {
    val percent: Int get() = if (total == 0) 0 else worked.size * 100 / total
}

data class ResponsibleStats(
    val responsibleId: String?,
    val name: String,
    val assignments: Int,
    val completed: Int,
    val averageDaysToComplete: Double?,
    val currentTerritories: Int
)

object TerritoryStatistics {

    fun lastCompletion(territory: Territory, periods: List<AssignmentPeriod>): Date? =
        listOfNotNull(periods.mapNotNull { it.completedAt }.maxOrNull(), territory.completedAt).maxOrNull()

    fun coverage(
        territories: List<Territory>,
        periodsByTerritory: Map<String, List<AssignmentPeriod>>,
        year: ServiceYear
    ): ServiceYearCoverage {
        val (worked, pending) = territories.partition { territory ->
            val completions = periodsByTerritory[territory.id].orEmpty().mapNotNull { it.completedAt } +
                listOfNotNull(territory.completedAt)
            completions.any { it in year }
        }
        return ServiceYearCoverage(year, territories.size, worked, pending)
    }

    /** Territórios sem conclusão nos últimos [months] meses (inclui os nunca trabalhados). */
    fun notCompletedSince(
        territories: List<Territory>,
        periodsByTerritory: Map<String, List<AssignmentPeriod>>,
        months: Int = 12,
        now: Date = Date()
    ): List<Territory> {
        val cutoff = Calendar.getInstance().apply {
            time = now
            add(Calendar.MONTH, -months)
        }.time
        return territories.filter { territory ->
            val last = lastCompletion(territory, periodsByTerritory[territory.id].orEmpty())
            last == null || last.before(cutoff)
        }
    }

    fun averageDaysToComplete(periods: List<AssignmentPeriod>): Double? =
        periods.filter { it.isCompleted && it.assignedAt != null }
            .mapNotNull { it.durationDays() }
            .takeIf { it.isNotEmpty() }
            ?.average()

    fun byResponsible(
        periodsByTerritory: Map<String, List<AssignmentPeriod>>,
        territories: List<Territory>
    ): List<ResponsibleStats> {
        val currentCounts = territories
            .filter { it.currentResponsibleId != null && it.status in ACTIVE_STATUSES }
            .groupingBy { it.currentResponsibleId }
            .eachCount()
        return periodsByTerritory.values.flatten()
            .groupBy { it.responsibleId ?: it.responsibleName }
            .map { (_, periods) ->
                val id = periods.first().responsibleId
                ResponsibleStats(
                    responsibleId = id,
                    name = periods.maxBy { it.assignedAt ?: it.endedAt ?: Date(0) }.responsibleName,
                    assignments = periods.size,
                    completed = periods.count { it.isCompleted },
                    averageDaysToComplete = averageDaysToComplete(periods),
                    currentTerritories = currentCounts[id] ?: 0
                )
            }
            .sortedWith(compareByDescending<ResponsibleStats> { it.completed }.thenBy { it.name })
    }

    /** Situação de cada território dentro do período da campanha. */
    fun campaignCoverage(
        territories: List<Territory>,
        periodsByTerritory: Map<String, List<AssignmentPeriod>>,
        campaign: Campaign
    ): CampaignCoverage {
        val completed = mutableListOf<Territory>()
        val inProgress = mutableListOf<Territory>()
        val notWorked = mutableListOf<Territory>()
        territories.forEach { territory ->
            val periods = periodsByTerritory[territory.id].orEmpty()
            val completedInCampaign = (periods.mapNotNull { it.completedAt } + listOfNotNull(territory.completedAt))
                .any { it in campaign }
            val workedInCampaign = periods.any { period ->
                val start = period.assignedAt ?: return@any false
                !start.after(campaign.end) && (period.endedAt?.let { !it.before(campaign.start) } ?: true)
            }
            when {
                completedInCampaign -> completed += territory
                workedInCampaign -> inProgress += territory
                else -> notWorked += territory
            }
        }
        return CampaignCoverage(campaign, completed, inProgress, notWorked)
    }

    val ACTIVE_STATUSES = setOf(TerritoryStatus.ASSIGNED, TerritoryStatus.IN_PROGRESS, TerritoryStatus.PAUSED)
}
