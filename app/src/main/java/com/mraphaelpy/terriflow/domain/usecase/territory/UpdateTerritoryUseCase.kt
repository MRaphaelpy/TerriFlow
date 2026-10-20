package com.mraphaelpy.terriflow.domain.usecase.territory

import com.mraphaelpy.terriflow.domain.model.DomainException
import com.mraphaelpy.terriflow.domain.model.EventType
import com.mraphaelpy.terriflow.domain.model.LatLng
import com.mraphaelpy.terriflow.domain.model.TerritoryAction
import com.mraphaelpy.terriflow.domain.model.TerritoryEvent
import com.mraphaelpy.terriflow.domain.repository.TerritoryEventRepository
import com.mraphaelpy.terriflow.domain.repository.TerritoryRepository
import java.util.Date
import javax.inject.Inject

data class TerritoryChanges(
    val code: String?,
    val name: String,
    val description: String,
    val location: String,
    val notes: String,
    val latitude: Double?,
    val longitude: Double?,
    val boundaryPoints: List<LatLng> = emptyList()
)

class UpdateTerritoryUseCase @Inject constructor(
    private val guard: TerritoryActionGuard,
    private val territoryRepository: TerritoryRepository,
    private val eventRepository: TerritoryEventRepository
) {
    suspend operator fun invoke(id: String, changes: TerritoryChanges) {
        val (actor, existing) = guard.authorize(id, TerritoryAction.EDIT)
        val now = Date()

        val requestedCode = changes.code?.trim().orEmpty()
        val finalCode = if (requestedCode.isNotEmpty() && requestedCode != existing.code) {
            val duplicate = territoryRepository.getByCode(requestedCode)
            if (duplicate != null && duplicate.id != id) {
                throw DomainException.DuplicateTerritoryCode(requestedCode)
            }
            requestedCode
        } else {
            existing.code
        }

        val updated = existing.copy(
            code = finalCode,
            name = changes.name.trim(),
            description = changes.description.trim(),
            location = changes.location.trim(),
            notes = changes.notes.trim(),
            latitude = changes.latitude,
            longitude = changes.longitude,
            boundaryPoints = changes.boundaryPoints,
            updatedAt = now
        )
        territoryRepository.save(updated)

        eventRepository.save(
            TerritoryEvent.create(
                territoryId = id,
                actor = actor,
                type = EventType.UPDATED,
                timestamp = now,
                extra = buildMap {
                    if (existing.code != updated.code) put("code", updated.code)
                    if (existing.name != updated.name) put("name", updated.name)
                    if (existing.location != updated.location) put("location", updated.location)
                    updated.latitude?.let { put("lat", it.toString()) }
                    updated.longitude?.let { put("lng", it.toString()) }
                }
            )
        )
    }
}
