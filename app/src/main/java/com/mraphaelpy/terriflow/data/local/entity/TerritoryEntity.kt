package com.mraphaelpy.terriflow.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mraphaelpy.terriflow.domain.model.LatLng
import com.mraphaelpy.terriflow.domain.model.Territory
import com.mraphaelpy.terriflow.domain.model.TerritoryStatus
import java.util.Date

@Entity(tableName = "territories")
data class TerritoryEntity(
    @PrimaryKey val id: String,
    val code: String,
    val name: String,
    val description: String,
    val location: String,
    val notes: String,
    val status: String,
    val currentResponsibleId: String?,
    val currentResponsibleName: String?,
    val currentResponsiblePhotoUrl: String? = null,
    val createdAt: Date,
    val assignedAt: Date?,
    val startedAt: Date?,
    val completedAt: Date?,
    val returnedAt: Date?,
    val updatedAt: Date,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val boundaryPoints: List<LatLng> = emptyList(),
    val blockPolygons: List<List<LatLng>> = emptyList(),
    val pastResponsibleIds: List<String> = emptyList(),
    val synced: Boolean = false,
    val deletedAt: Date? = null,
    val syncVersion: Long = 0L
) {
    fun toDomain() = Territory(
        id = id,
        code = code,
        name = name,
        description = description,
        location = location,
        notes = notes,
        status = TerritoryStatus.valueOf(status),
        currentResponsibleId = currentResponsibleId,
        currentResponsibleName = currentResponsibleName,
        currentResponsiblePhotoUrl = currentResponsiblePhotoUrl,
        createdAt = createdAt,
        assignedAt = assignedAt,
        startedAt = startedAt,
        completedAt = completedAt,
        returnedAt = returnedAt,
        updatedAt = updatedAt,
        latitude = latitude,
        longitude = longitude,
        boundaryPoints = boundaryPoints,
        blockPolygons = blockPolygons,
        pastResponsibleIds = pastResponsibleIds
    )

    companion object {
        fun fromDomain(territory: Territory, synced: Boolean = false) = TerritoryEntity(
            id = territory.id,
            code = territory.code,
            name = territory.name,
            description = territory.description,
            location = territory.location,
            notes = territory.notes,
            status = territory.status.name,
            currentResponsibleId = territory.currentResponsibleId,
            currentResponsibleName = territory.currentResponsibleName,
            currentResponsiblePhotoUrl = territory.currentResponsiblePhotoUrl,
            createdAt = territory.createdAt,
            assignedAt = territory.assignedAt,
            startedAt = territory.startedAt,
            completedAt = territory.completedAt,
            returnedAt = territory.returnedAt,
            updatedAt = territory.updatedAt,
            latitude = territory.latitude,
            longitude = territory.longitude,
            boundaryPoints = territory.boundaryPoints,
            blockPolygons = territory.blockPolygons,
            pastResponsibleIds = territory.pastResponsibleIds,
            synced = synced
        )
    }
}
