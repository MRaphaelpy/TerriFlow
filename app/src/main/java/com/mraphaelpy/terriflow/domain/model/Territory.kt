package com.mraphaelpy.terriflow.domain.model

import java.util.Date

enum class TerritoryStatus {
    AVAILABLE,
    ASSIGNED,
    IN_PROGRESS,
    PAUSED,
    COMPLETED,
    RETURNED,
    ARCHIVED
}

fun TerritoryStatus.label(): String = when (this) {
    TerritoryStatus.AVAILABLE -> "Disponível"
    TerritoryStatus.ASSIGNED -> "Atribuído"
    TerritoryStatus.IN_PROGRESS -> "Em andamento"
    TerritoryStatus.PAUSED -> "Pausado"
    TerritoryStatus.COMPLETED -> "Finalizado"
    TerritoryStatus.RETURNED -> "Devolvido"
    TerritoryStatus.ARCHIVED -> "Arquivado"
}

data class Territory(
    val id: String = "",
    val code: String = "",
    val name: String = "",
    val description: String = "",
    val location: String = "",
    val notes: String = "",
    val status: TerritoryStatus = TerritoryStatus.AVAILABLE,
    val currentResponsibleId: String? = null,
    val currentResponsibleName: String? = null,
    val currentResponsiblePhotoUrl: String? = null,
    val createdAt: Date = Date(),
    val assignedAt: Date? = null,
    val startedAt: Date? = null,
    val completedAt: Date? = null,
    val returnedAt: Date? = null,
    val updatedAt: Date = Date(),
    val latitude: Double? = null,
    val longitude: Double? = null,
    val boundaryPoints: List<LatLng> = emptyList(),
    val blockPolygons: List<List<LatLng>> = emptyList()
) {
    val hasLocation: Boolean get() = latitude != null && longitude != null
    val hasBoundary: Boolean get() = boundaryPoints.size >= 3
    val hasBlocks: Boolean get() = blockPolygons.isNotEmpty()
}
