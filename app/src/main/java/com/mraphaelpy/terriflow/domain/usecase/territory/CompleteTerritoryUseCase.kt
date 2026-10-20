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
import com.mraphaelpy.terriflow.domain.usecase.notification.NotifyAdminsAndOwnerUseCase
import java.util.Date
import javax.inject.Inject

/**
 * Finaliza o território. Se [nextResponsibleId] for informado, o território é
 * transferido diretamente para o próximo responsável.
 */
class CompleteTerritoryUseCase @Inject constructor(
    private val guard: TerritoryActionGuard,
    private val territoryRepository: TerritoryRepository,
    private val eventRepository: TerritoryEventRepository,
    private val notificationRepository: NotificationRepository,
    private val userRepository: UserRepository,
    private val notifyAdminsAndOwner: NotifyAdminsAndOwnerUseCase
) {
    suspend operator fun invoke(territoryId: String, nextResponsibleId: String? = null) {
        val (actor, territory) = guard.authorize(territoryId, TerritoryAction.COMPLETE)
        val nextResponsible = nextResponsibleId?.let {
            userRepository.getById(it) ?: throw DomainException.UserNotFound()
        }
        val now = Date()
        val pastResponsibleIds =
            (territory.pastResponsibleIds + listOfNotNull(territory.currentResponsibleId)).distinct()

        // Os eventos são gravados antes do território: as regras do Firestore só aceitam
        // eventos de um não-admin enquanto ele ainda é o responsável atual.
        eventRepository.save(
            TerritoryEvent.create(territory.id, actor, EventType.COMPLETED, now)
        )

        // Finalização (com ou sem transferência) é avisada aos admins e ao responsável
        notifyAdminsAndOwner(
            territory = territory,
            ownerId = territory.currentResponsibleId,
            actor = actor,
            title = "Território finalizado",
            body = "${territory.displayName} foi finalizado por ${actor.name}",
            type = NotificationType.TERRITORY_COMPLETED,
            createdAt = now
        )

        if (nextResponsible == null) {
            territoryRepository.save(
                territory.copy(
                    status = TerritoryStatus.COMPLETED,
                    pastResponsibleIds = pastResponsibleIds,
                    completedAt = now,
                    updatedAt = now
                )
            )
            return
        }

        eventRepository.save(
            TerritoryEvent.create(
                territoryId = territory.id,
                actor = actor,
                type = EventType.TRANSFERRED,
                timestamp = now,
                extra = mapOf(
                    "fromUserId" to (territory.currentResponsibleId ?: ""),
                    "fromUserName" to (territory.currentResponsibleName ?: ""),
                    "toUserId" to nextResponsible.id,
                    "toUserName" to nextResponsible.name
                )
            )
        )
        territoryRepository.save(
            territory.copy(
                status = TerritoryStatus.ASSIGNED,
                currentResponsibleId = nextResponsible.id,
                currentResponsibleName = nextResponsible.name,
                currentResponsiblePhotoUrl = nextResponsible.photoUrl,
                pastResponsibleIds = (pastResponsibleIds + nextResponsible.id).distinct(),
                assignedAt = now,
                startedAt = null,
                completedAt = null,
                updatedAt = now
            )
        )
        notificationRepository.save(
            AppNotification.aboutTerritory(
                territory = territory,
                recipientId = nextResponsible.id,
                title = "Território transferido para você",
                body = territory.displayName,
                type = NotificationType.TERRITORY_TRANSFERRED,
                createdAt = now
            )
        )
    }
}
