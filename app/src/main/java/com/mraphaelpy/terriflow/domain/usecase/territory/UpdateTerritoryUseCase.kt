package com.mraphaelpy.terriflow.domain.usecase.territory

import com.mraphaelpy.terriflow.domain.model.EventType
import com.mraphaelpy.terriflow.domain.model.LatLng
import com.mraphaelpy.terriflow.domain.model.TerritoryAction
import com.mraphaelpy.terriflow.domain.model.TerritoryEvent
import com.mraphaelpy.terriflow.domain.repository.TerritoryEventRepository
import com.mraphaelpy.terriflow.domain.repository.TerritoryRepository
import java.util.Date
import javax.inject.Inject

data class TerritoryChanges(
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

        val updated = existing.copy(
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
                    fun change(key: String, before: String, after: String) {
                        if (before == after) return
                        put(key, after)
                        put("${key}From", before)
                    }
                    change("name", existing.name, updated.name)
                    change("location", existing.location, updated.location)
                    change("description", existing.description, updated.description)
                    change("notes", existing.notes, updated.notes)
                    if (existing.latitude != updated.latitude || existing.longitude != updated.longitude) {
                        put("pinChanged", "true")
                    }
                    if (existing.boundaryPoints != updated.boundaryPoints) put("boundaryChanged", "true")
                }
            )
        )
    }
}
