package com.mraphaelpy.terriflow.domain.usecase.territory

import com.mraphaelpy.terriflow.domain.model.EventType
import com.mraphaelpy.terriflow.domain.model.Territory
import com.mraphaelpy.terriflow.domain.model.TerritoryEvent
import com.mraphaelpy.terriflow.domain.model.TerritoryStatus
import com.mraphaelpy.terriflow.domain.repository.AuthRepository
import com.mraphaelpy.terriflow.domain.repository.TerritoryEventRepository
import com.mraphaelpy.terriflow.domain.repository.TerritoryRepository
import java.util.Date
import java.util.UUID
import javax.inject.Inject

class CreateTerritoryUseCase @Inject constructor(
    private val territoryRepository: TerritoryRepository,
    private val eventRepository: TerritoryEventRepository,
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(
        code: String?,
        name: String,
        description: String,
        location: String,
        notes: String
    ): Territory {
        val user = authRepository.getCurrentUser()
            ?: throw IllegalStateException("Usuário não autenticado")
            
        val finalCode = if (!code.isNullOrBlank()) {
            val existing = territoryRepository.getByCode(code.trim())
            if (existing != null) throw IllegalArgumentException("Já existe um território com o código '${code.trim()}'")
            code.trim()
        } else {
            territoryRepository.getNextCode()
        }
        
        val now = Date()

        val territory = Territory(
            id = UUID.randomUUID().toString(),
            code = finalCode,
            name = name,
            description = description,
            location = location,
            notes = notes,
            status = TerritoryStatus.AVAILABLE,
            createdAt = now,
            updatedAt = now
        )

        val saved = territoryRepository.save(territory)

        val event = TerritoryEvent(
            id = UUID.randomUUID().toString(),
            territoryId = saved.id,
            userId = user.id,
            userName = user.name,
            type = EventType.CREATED,
            timestamp = now
        )
        eventRepository.save(event)

        return saved
    }
}
