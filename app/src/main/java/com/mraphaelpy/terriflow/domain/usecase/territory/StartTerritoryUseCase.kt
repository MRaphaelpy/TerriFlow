package com.mraphaelpy.terriflow.domain.usecase.territory

import com.mraphaelpy.terriflow.domain.model.EventType
import com.mraphaelpy.terriflow.domain.model.TerritoryAction
import com.mraphaelpy.terriflow.domain.model.TerritoryEvent
import com.mraphaelpy.terriflow.domain.model.TerritoryStatus
import com.mraphaelpy.terriflow.domain.repository.TerritoryEventRepository
import com.mraphaelpy.terriflow.domain.repository.TerritoryRepository
import java.util.Date
import javax.inject.Inject

/** Inicia o trabalho no território, ou o retoma se estiver pausado. */
class StartTerritoryUseCase @Inject constructor(
    private val guard: TerritoryActionGuard,
    private val territoryRepository: TerritoryRepository,
    private val eventRepository: TerritoryEventRepository
) {
    suspend operator fun invoke(territoryId: String) {
        val (actor, territory) = guard.authorize(territoryId, TerritoryAction.START)
        val now = Date()
        val type = if (territory.status == TerritoryStatus.PAUSED) EventType.RESUMED else EventType.STARTED

        eventRepository.save(TerritoryEvent.create(territory.id, actor, type, now))
        territoryRepository.save(
            territory.copy(
                status = TerritoryStatus.IN_PROGRESS,
                startedAt = territory.startedAt ?: now,
                updatedAt = now
            )
        )
    }
}
