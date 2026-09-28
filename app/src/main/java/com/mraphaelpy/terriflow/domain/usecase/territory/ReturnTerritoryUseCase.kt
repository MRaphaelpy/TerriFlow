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
import java.util.Date
import java.util.UUID
import javax.inject.Inject

class ReturnTerritoryUseCase @Inject constructor(
    private val territoryRepository: TerritoryRepository,
    private val eventRepository: TerritoryEventRepository,
    private val notificationRepository: NotificationRepository,
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(territoryId: String) {
        val user = authRepository.getCurrentUser()
            ?: throw IllegalStateException("Usuário não autenticado")
        val territory = territoryRepository.getById(territoryId)
            ?: throw IllegalStateException("Território não encontrado")
        val now = Date()

        val basePast = (territory.pastResponsibleIds + listOfNotNull(territory.currentResponsibleId)).distinct()
        val updated = territory.copy(
            status = TerritoryStatus.RETURNED,
            pastResponsibleIds = basePast,
            currentResponsibleId = null,
            currentResponsibleName = null,
            currentResponsiblePhotoUrl = null,
            returnedAt = now,
            updatedAt = now
        )
        territoryRepository.save(updated)

        val event = TerritoryEvent(
            id = UUID.randomUUID().toString(),
            territoryId = territory.id,
            userId = user.id,
            userName = user.name,
            type = EventType.RETURNED,
            timestamp = now
        )
        eventRepository.save(event)

        val notification = AppNotification(
            id = UUID.randomUUID().toString(),
            userId = "",
            title = "Território devolvido",
            body = "${territory.code} — ${territory.name} foi devolvido por ${user.name}",
            type = NotificationType.TERRITORY_RETURNED,
            territoryId = territory.id,
            territoryCode = territory.code,
            createdAt = now
        )
        notificationRepository.save(notification)
    }
}
