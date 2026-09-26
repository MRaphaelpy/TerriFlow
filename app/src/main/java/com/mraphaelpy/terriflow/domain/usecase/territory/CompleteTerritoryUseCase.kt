package com.mraphaelpy.terriflow.domain.usecase.territory

import com.mraphaelpy.terriflow.domain.model.AppNotification
import com.mraphaelpy.terriflow.domain.model.EventType
import com.mraphaelpy.terriflow.domain.model.NotificationType
import com.mraphaelpy.terriflow.domain.model.TerritoryEvent
import com.mraphaelpy.terriflow.domain.model.TerritoryStatus
import com.mraphaelpy.terriflow.domain.repository.AuthRepository
import com.mraphaelpy.terriflow.domain.repository.NotificationRepository
import com.mraphaelpy.terriflow.domain.repository.TerritoryEventRepository
import com.mraphaelpy.terriflow.domain.repository.TerritoryRepository
import com.mraphaelpy.terriflow.domain.repository.UserRepository
import java.util.Date
import java.util.UUID
import javax.inject.Inject

class CompleteTerritoryUseCase @Inject constructor(
    private val territoryRepository: TerritoryRepository,
    private val eventRepository: TerritoryEventRepository,
    private val notificationRepository: NotificationRepository,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(territoryId: String, nextResponsibleId: String? = null) {
        val user = authRepository.getCurrentUser()
            ?: throw IllegalStateException("Usuário não autenticado")
        val territory = territoryRepository.getById(territoryId)
            ?: throw IllegalStateException("Território não encontrado")
        val now = Date()

        val basePast = (territory.pastResponsibleIds + listOfNotNull(territory.currentResponsibleId)).distinct()
        val updated = territory.copy(
            status = TerritoryStatus.COMPLETED,
            pastResponsibleIds = basePast,
            completedAt = now,
            updatedAt = now
        )
        territoryRepository.save(updated)

        val completedEvent = TerritoryEvent(
            id = UUID.randomUUID().toString(),
            territoryId = territory.id,
            userId = user.id,
            userName = user.name,
            type = EventType.COMPLETED,
            timestamp = now
        )
        eventRepository.save(completedEvent)

        if (nextResponsibleId != null) {
            val nextResponsible = userRepository.getById(nextResponsibleId)
            if (nextResponsible != null) {
                val transferred = updated.copy(
                    status = TerritoryStatus.ASSIGNED,
                    currentResponsibleId = nextResponsible.id,
                    currentResponsibleName = nextResponsible.name,
                    currentResponsiblePhotoUrl = nextResponsible.photoUrl,
                    pastResponsibleIds = (basePast + nextResponsible.id).distinct(),
                    assignedAt = now,
                    completedAt = null,
                    startedAt = null,
                    updatedAt = now
                )
                territoryRepository.save(transferred)

                val transferEvent = TerritoryEvent(
                    id = UUID.randomUUID().toString(),
                    territoryId = territory.id,
                    userId = user.id,
                    userName = user.name,
                    type = EventType.TRANSFERRED,
                    timestamp = now,
                    extra = mapOf(
                        "fromUserId" to (territory.currentResponsibleId ?: ""),
                        "fromUserName" to (territory.currentResponsibleName ?: ""),
                        "toUserId" to nextResponsible.id,
                        "toUserName" to nextResponsible.name
                    )
                )
                eventRepository.save(transferEvent)

                val notification = AppNotification(
                    id = UUID.randomUUID().toString(),
                    userId = nextResponsible.id,
                    title = "Território transferido para você",
                    body = "${territory.code} — ${territory.name}",
                    type = NotificationType.TERRITORY_TRANSFERRED,
                    territoryId = territory.id,
                    territoryCode = territory.code,
                    createdAt = now
                )
                notificationRepository.save(notification)
            }
        }
    }
}
