package com.mraphaelpy.terriflow.domain.model

import java.util.Date

data class ResponsibleRotationInfo(
    val user: User,
    val timesAssigned: Int = 0,
    val lastAssignedDate: Date? = null,
    val hasWorkedPreviously: Boolean = false,
    val currentTerritories: Int = 0,
    val isRecommended: Boolean = false
) {
    val isOverLimit: Boolean get() = currentTerritories >= TerritoryRotationHelper.MAX_ACTIVE_PER_RESPONSIBLE
}

object TerritoryRotationHelper {

    const val MAX_ACTIVE_PER_RESPONSIBLE = 3

    fun computeRotationInfo(
        responsibles: List<User>,
        territory: Territory?,
        events: List<TerritoryEvent>,
        allTerritories: List<Territory> = emptyList()
    ): List<ResponsibleRotationInfo> {
        val currentCounts = allTerritories
            .filter { it.currentResponsibleId != null && it.status in TerritoryStatistics.ACTIVE_STATUSES }
            .groupingBy { it.currentResponsibleId }
            .eachCount()

        val relevantEvents = events.filter { it.type == EventType.ASSIGNED || it.type == EventType.TRANSFERRED }

        val infos = responsibles.map { user ->
            val userAssignments = relevantEvents.filter { event ->
                (event.type == EventType.ASSIGNED && (event.extra["responsibleId"] == user.id)) ||
                    (event.type == EventType.TRANSFERRED && event.extra["toUserId"] == user.id)
            }
            val times = userAssignments.size
            val isCurrent = territory?.currentResponsibleId == user.id
            val lastDate = userAssignments.maxByOrNull { it.timestamp }?.timestamp
                ?: if (isCurrent) territory?.assignedAt else null
            val workedBefore = times > 0 || territory?.pastResponsibleIds?.contains(user.id) == true || isCurrent

            ResponsibleRotationInfo(
                user = user,
                timesAssigned = if (workedBefore && times == 0) 1 else times,
                lastAssignedDate = lastDate,
                hasWorkedPreviously = workedBefore,
                currentTerritories = currentCounts[user.id] ?: 0
            )
        }

        val ranked = infos.sortedWith(
            compareBy<ResponsibleRotationInfo> { it.hasWorkedPreviously }
                .thenBy { it.currentTerritories }
                .thenBy(nullsFirst()) { it.lastAssignedDate }
                .thenBy { it.user.name }
        )
        val recommendedId = ranked.firstOrNull { !it.isOverLimit && it.user.id != territory?.currentResponsibleId }?.user?.id
        return ranked.map { it.copy(isRecommended = it.user.id == recommendedId) }
    }
}
