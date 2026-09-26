package com.mraphaelpy.terriflow.domain.usecase.territory

import com.mraphaelpy.terriflow.core.util.KmlParser
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

class ImportTerritoriesUseCase @Inject constructor(
    private val territoryRepository: TerritoryRepository,
    private val eventRepository: TerritoryEventRepository,
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(items: List<KmlParser.ParsedTerritory>): Int {
        val user = authRepository.getCurrentUser()
            ?: throw IllegalStateException("Usuário não autenticado")

        val now = Date()
        var imported = 0

        for (parsed in items) {
            val code = territoryRepository.getNextCode()
            val territory = Territory(
                id = UUID.randomUUID().toString(),
                code = code,
                name = parsed.name.ifBlank { "Território $code" },
                description = parsed.description,
                boundaryPoints = parsed.boundaryPoints,
                blockPolygons = parsed.blockPolygons,
                latitude = parsed.centroid?.lat,
                longitude = parsed.centroid?.lng,
                status = TerritoryStatus.AVAILABLE,
                createdAt = now,
                updatedAt = now
            )
            val saved = territoryRepository.save(territory)
            eventRepository.save(
                TerritoryEvent(
                    id = UUID.randomUUID().toString(),
                    territoryId = saved.id,
                    userId = user.id,
                    userName = user.name,
                    type = EventType.CREATED,
                    timestamp = now
                )
            )
            imported++
        }
        return imported
    }
}
