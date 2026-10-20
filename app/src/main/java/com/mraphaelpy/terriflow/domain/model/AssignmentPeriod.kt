package com.mraphaelpy.terriflow.domain.model

import java.util.Date
import java.util.concurrent.TimeUnit

/** Uma designação de território: de quando foi entregue a um dirigente até a conclusão ou devolução. */
data class AssignmentPeriod(
    val territoryId: String,
    val responsibleId: String?,
    val responsibleName: String,
    val assignedAt: Date?,
    val completedAt: Date? = null,
    val returnedAt: Date? = null
) {
    val endedAt: Date? get() = completedAt ?: returnedAt
    val isOpen: Boolean get() = endedAt == null
    val isCompleted: Boolean get() = completedAt != null

    fun durationDays(now: Date = Date()): Long? {
        val start = assignedAt ?: return null
        return TimeUnit.MILLISECONDS.toDays((endedAt ?: now).time - start.time)
    }
}

object AssignmentHistory {

    /** Reconstrói as designações de cada território a partir do histórico de eventos. */
    fun build(events: List<TerritoryEvent>): Map<String, List<AssignmentPeriod>> =
        events.groupBy { it.territoryId }.mapValues { (_, territoryEvents) -> buildForTerritory(territoryEvents) }

    private fun buildForTerritory(events: List<TerritoryEvent>): List<AssignmentPeriod> {
        val periods = mutableListOf<AssignmentPeriod>()
        var open: AssignmentPeriod? = null

        fun close(transform: (AssignmentPeriod) -> AssignmentPeriod) {
            open?.let { periods += transform(it) }
            open = null
        }

        events.sortedWith(compareBy<TerritoryEvent> { it.timestamp }.thenBy { it.type.ordinal }).forEach { event ->
            when (event.type) {
                EventType.ASSIGNED -> {
                    close { it.copy(returnedAt = event.timestamp) }
                    open = AssignmentPeriod(
                        territoryId = event.territoryId,
                        responsibleId = event.extra["responsibleId"],
                        responsibleName = event.extra["responsibleName"] ?: event.userName,
                        assignedAt = event.timestamp
                    )
                }
                EventType.TRANSFERRED -> {
                    close { it.copy(completedAt = event.timestamp) }
                    open = AssignmentPeriod(
                        territoryId = event.territoryId,
                        responsibleId = event.extra["toUserId"],
                        responsibleName = event.extra["toUserName"] ?: event.userName,
                        assignedAt = event.timestamp
                    )
                }
                EventType.COMPLETED -> {
                    if (open != null) {
                        close { it.copy(completedAt = event.timestamp) }
                    } else {
                        periods += AssignmentPeriod(
                            territoryId = event.territoryId,
                            responsibleId = event.userId,
                            responsibleName = event.userName,
                            assignedAt = null,
                            completedAt = event.timestamp
                        )
                    }
                }
                EventType.RETURNED -> close { it.copy(returnedAt = event.timestamp) }
                else -> Unit
            }
        }
        open?.let { periods += it }
        return periods
    }
}
