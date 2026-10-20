package com.mraphaelpy.terriflow.domain.usecase.territory

import com.mraphaelpy.terriflow.domain.model.DomainException
import com.mraphaelpy.terriflow.domain.model.EventType
import com.mraphaelpy.terriflow.domain.model.Territory
import com.mraphaelpy.terriflow.domain.model.TerritoryEvent
import com.mraphaelpy.terriflow.domain.model.TerritoryStatus
import com.mraphaelpy.terriflow.domain.repository.TerritoryEventRepository
import com.mraphaelpy.terriflow.domain.repository.TerritoryRepository
import java.util.Date
import java.util.UUID
import javax.inject.Inject

class CreateTerritoryUseCase @Inject constructor(
    private val guard: TerritoryActionGuard,
    private val territoryRepository: TerritoryRepository,
    private val eventRepository: TerritoryEventRepository
) {
    suspend operator fun invoke(
        code: String?,
        name: String,
        description: String,
        location: String,
        notes: String
    ): Territory {
        val actor = guard.requireCanCreate()

        val requestedCode = code?.trim().orEmpty()
        val finalCode = if (requestedCode.isNotEmpty()) {
            if (territoryRepository.getByCode(requestedCode) != null) {
                throw DomainException.DuplicateTerritoryCode(requestedCode)
            }
            requestedCode
        } else {
            territoryRepository.getNextCode()
        }

        val now = Date()
        val territory = Territory(
            id = UUID.randomUUID().toString(),
            code = finalCode,
            name = name.trim(),
            description = description.trim(),
            location = location.trim(),
            notes = notes.trim(),
            status = TerritoryStatus.AVAILABLE,
            createdAt = now,
            updatedAt = now
        )

        val saved = territoryRepository.save(territory)
        eventRepository.save(TerritoryEvent.create(saved.id, actor, EventType.CREATED, now))
        return saved
    }
}
