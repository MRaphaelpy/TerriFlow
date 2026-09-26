package com.mraphaelpy.terriflow.domain.usecase.territory

import com.mraphaelpy.terriflow.domain.model.EventType
import com.mraphaelpy.terriflow.domain.model.TerritoryEvent
import com.mraphaelpy.terriflow.domain.model.TerritoryStatus
import com.mraphaelpy.terriflow.domain.repository.AuthRepository
import com.mraphaelpy.terriflow.domain.repository.TerritoryEventRepository
import com.mraphaelpy.terriflow.domain.repository.TerritoryRepository
import java.util.Date
import java.util.UUID
import javax.inject.Inject

class StartTerritoryUseCase @Inject constructor(
    private val territoryRepository: TerritoryRepository,
    private val eventRepository: TerritoryEventRepository,
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(territoryId: String) {
        val user = authRepository.getCurrentUser()
            ?: throw IllegalStateException("Usuário não autenticado")
        val territory = territoryRepository.getById(territoryId)
            ?: throw IllegalStateException("Território não encontrado")
        val now = Date()

        val updated = territory.copy(
            status = TerritoryStatus.IN_PROGRESS,
            startedAt = territory.startedAt ?: now,
            updatedAt = now
        )
        territoryRepository.save(updated)

        val type = if (territory.status == TerritoryStatus.PAUSED) EventType.RESUMED else EventType.STARTED
        val event = TerritoryEvent(
            id = UUID.randomUUID().toString(),
            territoryId = territory.id,
            userId = user.id,
            userName = user.name,
            type = type,
            timestamp = now
        )
        eventRepository.save(event)
    }
}
