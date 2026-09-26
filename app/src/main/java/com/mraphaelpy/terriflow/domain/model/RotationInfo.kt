package com.mraphaelpy.terriflow.domain.model

import java.util.Date

data class ResponsibleRotationInfo(
    val user: User,
    val timesAssigned: Int = 0,
    val lastAssignedDate: Date? = null,
    val hasWorkedPreviously: Boolean = false
)

object TerritoryRotationHelper {

    fun computeRotationInfo(
        responsibles: List<User>,
        territory: Territory?,
        events: List<TerritoryEvent>
    ): List<ResponsibleRotationInfo> {
        if (territory == null) return responsibles.map { ResponsibleRotationInfo(user = it) }

        val relevantEvents = events.filter { event ->
            event.type == EventType.ASSIGNED || event.type == EventType.TRANSFERRED
        }

        return responsibles.map { user ->
            val userAssignments = relevantEvents.filter { event ->
                (event.type == EventType.ASSIGNED && (event.extra["responsibleId"] == user.id || event.userId == user.id)) ||
                (event.type == EventType.TRANSFERRED && event.extra["toUserId"] == user.id)
            }
            val times = userAssignments.size
            val lastDate = userAssignments.maxByOrNull { it.timestamp }?.timestamp
                ?: if (territory.currentResponsibleId == user.id) territory.assignedAt else null

            val workedBefore = times > 0 ||
                territory.pastResponsibleIds.contains(user.id) ||
                territory.currentResponsibleId == user.id

            ResponsibleRotationInfo(
                user = user,
                timesAssigned = if (workedBefore && times == 0) 1 else times,
                lastAssignedDate = lastDate,
                hasWorkedPreviously = workedBefore
            )
        }
    }
}
