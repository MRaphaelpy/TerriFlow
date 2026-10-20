package com.mraphaelpy.terriflow.domain.usecase.territory

import com.mraphaelpy.terriflow.domain.model.EventType
import com.mraphaelpy.terriflow.domain.model.ImportedTerritory
import com.mraphaelpy.terriflow.domain.model.Territory
import com.mraphaelpy.terriflow.domain.model.TerritoryCode
import com.mraphaelpy.terriflow.domain.model.TerritoryEvent
import com.mraphaelpy.terriflow.domain.model.TerritoryStatus
import com.mraphaelpy.terriflow.domain.repository.TerritoryEventRepository
import com.mraphaelpy.terriflow.domain.repository.TerritoryRepository
import java.util.Date
import java.util.UUID
import javax.inject.Inject

class ImportTerritoriesUseCase @Inject constructor(
    private val guard: TerritoryActionGuard,
    private val territoryRepository: TerritoryRepository,
    private val eventRepository: TerritoryEventRepository
) {
    suspend operator fun invoke(items: List<ImportedTerritory>): Int {
        if (items.isEmpty()) return 0
        val actor = guard.requireCanCreate()
        val now = Date()

        var nextNumber = TerritoryCode.numberOf(territoryRepository.getNextCode()) ?: 1

        items.forEach { imported ->
            val code = TerritoryCode.format(nextNumber++)
            val territory = Territory(
                id = UUID.randomUUID().toString(),
                code = code,
                name = imported.name.ifBlank { "Território $code" },
                description = imported.description,
                boundaryPoints = imported.boundaryPoints,
                blockPolygons = imported.blockPolygons,
                latitude = imported.centroid?.lat,
                longitude = imported.centroid?.lng,
                status = TerritoryStatus.AVAILABLE,
                createdAt = now,
                updatedAt = now
            )
            val saved = territoryRepository.save(territory)
            eventRepository.save(TerritoryEvent.create(saved.id, actor, EventType.CREATED, now))
        }
        return items.size
    }
}
