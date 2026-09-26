package com.mraphaelpy.terriflow.domain.usecase.territory

import com.mraphaelpy.terriflow.domain.model.EventType
import com.mraphaelpy.terriflow.domain.model.LatLng
import com.mraphaelpy.terriflow.domain.model.TerritoryEvent
import com.mraphaelpy.terriflow.domain.repository.AuthRepository
import com.mraphaelpy.terriflow.domain.repository.TerritoryEventRepository
import com.mraphaelpy.terriflow.domain.repository.TerritoryRepository
import java.util.Date
import java.util.UUID
import javax.inject.Inject

class UpdateTerritoryUseCase @Inject constructor(
    private val territoryRepository: TerritoryRepository,
    private val eventRepository: TerritoryEventRepository,
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(
        id: String,
        code: String?,
        name: String,
        description: String,
        location: String,
        notes: String,
        latitude: Double?,
        longitude: Double?,
        boundaryPoints: List<LatLng> = emptyList()
    ) {
        val user = authRepository.getCurrentUser()
            ?: throw IllegalStateException("Usuário não autenticado")
        val existing = territoryRepository.getById(id)
            ?: throw IllegalStateException("Território não encontrado")
        val now = Date()

        val finalCode = if (!code.isNullOrBlank() && code.trim() != existing.code) {
            val duplicate = territoryRepository.getByCode(code.trim())
            if (duplicate != null && duplicate.id != id) {
                throw IllegalArgumentException("Já existe um território com o código '${code.trim()}'")
            }
            code.trim()
        } else {
            existing.code
        }

        val updated = existing.copy(
            code = finalCode,
            name = name.trim(),
            description = description.trim(),
            location = location.trim(),
            notes = notes.trim(),
            latitude = latitude,
            longitude = longitude,
            boundaryPoints = boundaryPoints,
            updatedAt = now
        )
        territoryRepository.save(updated)

        val event = TerritoryEvent(
            id = UUID.randomUUID().toString(),
            territoryId = id,
            userId = user.id,
            userName = user.name,
            type = EventType.UPDATED,
            timestamp = now,
            extra = buildMap {
                if (existing.code != finalCode) put("code", finalCode)
                if (existing.name != name) put("name", name)
                if (existing.location != location) put("location", location)
                if (latitude != null) put("lat", latitude.toString())
                if (longitude != null) put("lng", longitude.toString())
            }
        )
        eventRepository.save(event)
    }
}
