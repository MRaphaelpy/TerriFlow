package com.mraphaelpy.terriflow.domain.usecase.territory

import com.mraphaelpy.terriflow.domain.model.AppNotification
import com.mraphaelpy.terriflow.domain.model.DomainException
import com.mraphaelpy.terriflow.domain.model.EventType
import com.mraphaelpy.terriflow.domain.model.NotificationType
import com.mraphaelpy.terriflow.domain.model.TerritoryAction
import com.mraphaelpy.terriflow.domain.model.TerritoryEvent
import com.mraphaelpy.terriflow.domain.model.TerritoryStatus
import com.mraphaelpy.terriflow.domain.model.displayName
import com.mraphaelpy.terriflow.domain.repository.NotificationRepository
import com.mraphaelpy.terriflow.domain.repository.TerritoryEventRepository
import com.mraphaelpy.terriflow.domain.repository.TerritoryRepository
import com.mraphaelpy.terriflow.domain.repository.UserRepository
import java.util.Date
import javax.inject.Inject

class AssignTerritoryUseCase @Inject constructor(
    private val guard: TerritoryActionGuard,
    private val territoryRepository: TerritoryRepository,
    private val eventRepository: TerritoryEventRepository,
    private val notificationRepository: NotificationRepository,
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(territoryId: String, responsibleId: String) {
        val (actor, territory) = guard.authorize(territoryId, TerritoryAction.ASSIGN)
        val responsible = userRepository.getById(responsibleId) ?: throw DomainException.UserNotFound()
        val now = Date()

        eventRepository.save(
            TerritoryEvent.create(
                territoryId = territory.id,
                actor = actor,
                type = EventType.ASSIGNED,
                timestamp = now,
                extra = mapOf("responsibleId" to responsible.id, "responsibleName" to responsible.name)
            )
        )

        territoryRepository.save(
            territory.copy(
                status = TerritoryStatus.ASSIGNED,
                currentResponsibleId = responsible.id,
                currentResponsibleName = responsible.name,
                currentResponsiblePhotoUrl = responsible.photoUrl,
                pastResponsibleIds = (territory.pastResponsibleIds + responsible.id).distinct(),
                assignedAt = now,
                startedAt = null,
                completedAt = null,
                updatedAt = now
            )
        )

        notificationRepository.save(
            AppNotification.aboutTerritory(
                territory = territory,
                recipientId = responsible.id,
                title = "Novo território atribuído a você",
                body = territory.displayName,
                type = NotificationType.TERRITORY_ASSIGNED,
                createdAt = now
            )
        )
    }
}
