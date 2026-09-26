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

class AssignTerritoryUseCase @Inject constructor(
    private val territoryRepository: TerritoryRepository,
    private val eventRepository: TerritoryEventRepository,
    private val notificationRepository: NotificationRepository,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(territoryId: String, responsibleId: String) {
        val currentUser = authRepository.getCurrentUser()
            ?: throw IllegalStateException("Usuário não autenticado")
        val territory = territoryRepository.getById(territoryId)
            ?: throw IllegalStateException("Território não encontrado")
        val responsible = userRepository.getById(responsibleId)
            ?: throw IllegalStateException("Responsável não encontrado")
        val now = Date()

        val updated = territory.copy(
            status = TerritoryStatus.ASSIGNED,
            currentResponsibleId = responsible.id,
            currentResponsibleName = responsible.name,
            currentResponsiblePhotoUrl = responsible.photoUrl,
            pastResponsibleIds = (territory.pastResponsibleIds + responsible.id).distinct(),
            assignedAt = now,
            updatedAt = now
        )
        territoryRepository.save(updated)

        val event = TerritoryEvent(
            id = UUID.randomUUID().toString(),
            territoryId = territory.id,
            userId = currentUser.id,
            userName = currentUser.name,
            type = EventType.ASSIGNED,
            timestamp = now,
            extra = mapOf("responsibleId" to responsible.id, "responsibleName" to responsible.name)
        )
        eventRepository.save(event)

        val notification = AppNotification(
            id = UUID.randomUUID().toString(),
            userId = responsible.id,
            title = "Novo território atribuído a você",
            body = "${territory.code} — ${territory.name}",
            type = NotificationType.TERRITORY_ASSIGNED,
            territoryId = territory.id,
            territoryCode = territory.code,
            createdAt = now
        )
        notificationRepository.save(notification)
    }
}
