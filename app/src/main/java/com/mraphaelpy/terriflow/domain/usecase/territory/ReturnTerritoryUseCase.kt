package com.mraphaelpy.terriflow.domain.usecase.territory

import com.mraphaelpy.terriflow.domain.model.EventType
import com.mraphaelpy.terriflow.domain.model.NotificationType
import com.mraphaelpy.terriflow.domain.model.TerritoryAction
import com.mraphaelpy.terriflow.domain.model.TerritoryEvent
import com.mraphaelpy.terriflow.domain.model.TerritoryStatus
import com.mraphaelpy.terriflow.domain.model.displayName
import com.mraphaelpy.terriflow.domain.repository.TerritoryEventRepository
import com.mraphaelpy.terriflow.domain.repository.TerritoryRepository
import com.mraphaelpy.terriflow.domain.usecase.notification.NotifyAdminsAndOwnerUseCase
import java.util.Date
import javax.inject.Inject

class ReturnTerritoryUseCase @Inject constructor(
    private val guard: TerritoryActionGuard,
    private val territoryRepository: TerritoryRepository,
    private val eventRepository: TerritoryEventRepository,
    private val notifyAdminsAndOwner: NotifyAdminsAndOwnerUseCase
) {
    suspend operator fun invoke(territoryId: String) {
        val (actor, territory) = guard.authorize(territoryId, TerritoryAction.RETURN)
        val now = Date()

        eventRepository.save(TerritoryEvent.create(territory.id, actor, EventType.RETURNED, now))

        territoryRepository.save(
            territory.copy(
                status = TerritoryStatus.RETURNED,
                pastResponsibleIds = (territory.pastResponsibleIds + listOfNotNull(territory.currentResponsibleId)).distinct(),
                currentResponsibleId = null,
                currentResponsibleName = null,
                currentResponsiblePhotoUrl = null,
                returnedAt = now,
                updatedAt = now
            )
        )

        notifyAdminsAndOwner(
            territory = territory,
            ownerId = territory.currentResponsibleId,
            actor = actor,
            title = "Território devolvido",
            body = "${territory.displayName} foi devolvido por ${actor.name}",
            type = NotificationType.TERRITORY_RETURNED,
            createdAt = now
        )
    }
}
