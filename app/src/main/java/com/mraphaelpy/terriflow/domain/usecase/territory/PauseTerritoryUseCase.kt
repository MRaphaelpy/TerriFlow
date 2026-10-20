package com.mraphaelpy.terriflow.domain.usecase.territory

import com.mraphaelpy.terriflow.domain.model.EventType
import com.mraphaelpy.terriflow.domain.model.TerritoryAction
import com.mraphaelpy.terriflow.domain.model.TerritoryEvent
import com.mraphaelpy.terriflow.domain.model.TerritoryStatus
import com.mraphaelpy.terriflow.domain.repository.TerritoryEventRepository
import com.mraphaelpy.terriflow.domain.repository.TerritoryRepository
import java.util.Date
import javax.inject.Inject

class PauseTerritoryUseCase @Inject constructor(
    private val guard: TerritoryActionGuard,
    private val territoryRepository: TerritoryRepository,
    private val eventRepository: TerritoryEventRepository
) {
    suspend operator fun invoke(territoryId: String) {
        val (actor, territory) = guard.authorize(territoryId, TerritoryAction.PAUSE)
        val now = Date()

        eventRepository.save(TerritoryEvent.create(territory.id, actor, EventType.PAUSED, now))
        territoryRepository.save(territory.copy(status = TerritoryStatus.PAUSED, updatedAt = now))
    }
}
