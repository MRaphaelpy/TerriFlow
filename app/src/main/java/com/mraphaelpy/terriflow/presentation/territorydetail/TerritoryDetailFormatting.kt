package com.mraphaelpy.terriflow.presentation.territorydetail

import com.mraphaelpy.terriflow.core.util.DateFormats
import com.mraphaelpy.terriflow.domain.model.Territory
import com.mraphaelpy.terriflow.domain.model.TerritoryAction
import com.mraphaelpy.terriflow.domain.model.TerritoryStatus
import java.util.Date
import java.util.concurrent.TimeUnit

internal data class ActionLayout(val primary: TerritoryAction?, val secondary: List<TerritoryAction>)

private val PrimaryPriority = listOf(TerritoryAction.START, TerritoryAction.COMPLETE, TerritoryAction.ASSIGN)
private val SecondaryOrder = listOf(
    TerritoryAction.PAUSE, TerritoryAction.COMPLETE, TerritoryAction.ASSIGN, TerritoryAction.RETURN
)

internal fun actionLayout(allowed: Set<TerritoryAction>): ActionLayout {
    val primary = PrimaryPriority.firstOrNull { it in allowed }
    return ActionLayout(primary, SecondaryOrder.filter { it in allowed && it != primary })
}

internal fun statusSince(territory: Territory, now: Date = Date()): String = when (territory.status) {
    TerritoryStatus.IN_PROGRESS -> territory.startedAt?.let { "Iniciado ${relativeDays(it, now)}" }
    TerritoryStatus.ASSIGNED -> territory.assignedAt?.let { "Atribuído ${relativeDays(it, now)}" }
    TerritoryStatus.PAUSED -> "Pausado ${relativeDays(territory.updatedAt, now)}"
    TerritoryStatus.COMPLETED -> territory.completedAt?.let { "Finalizado em ${DateFormats.fullDate().format(it)}" }
    TerritoryStatus.RETURNED -> territory.returnedAt?.let { "Devolvido em ${DateFormats.fullDate().format(it)}" }
    TerritoryStatus.AVAILABLE -> "Pronto para ser atribuído"
    TerritoryStatus.ARCHIVED -> "Arquivado"
} ?: ""

internal fun relativeDays(since: Date, now: Date = Date()): String {
    val days = TimeUnit.MILLISECONDS.toDays(now.time - since.time)
    return when {
        days <= 0 -> "hoje"
        days == 1L -> "ontem"
        else -> "há $days dias"
    }
}
