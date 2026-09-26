package com.mraphaelpy.terriflow.data.remote.dto

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.mraphaelpy.terriflow.domain.model.LatLng
import com.mraphaelpy.terriflow.domain.model.Territory
import com.mraphaelpy.terriflow.domain.model.TerritoryStatus

data class TerritoryDto(
    val id: String = "",
    val code: String = "",
    val name: String = "",
    val description: String = "",
    val location: String = "",
    val notes: String = "",
    val status: String = "AVAILABLE",
    val currentResponsibleId: String? = null,
    val currentResponsibleName: String? = null,
    val currentResponsiblePhotoUrl: String? = null,
    val createdAt: Timestamp = Timestamp.now(),
    val assignedAt: Timestamp? = null,
    val startedAt: Timestamp? = null,
    val completedAt: Timestamp? = null,
    val returnedAt: Timestamp? = null,
    val updatedAt: Timestamp = Timestamp.now(),
    val latitude: Double? = null,
    val longitude: Double? = null,
    val boundaryPoints: List<Map<String, Double>> = emptyList(),
    val blockPolygons: List<String> = emptyList(),
    val syncVersion: Long = 0L
) {
    fun toDomain() = Territory(
        id = id,
        code = code,
        name = name,
        description = description,
        location = location,
        notes = notes,
        status = runCatching { TerritoryStatus.valueOf(status) }.getOrDefault(TerritoryStatus.AVAILABLE),
        currentResponsibleId = currentResponsibleId,
        currentResponsibleName = currentResponsibleName,
        currentResponsiblePhotoUrl = currentResponsiblePhotoUrl,
        createdAt = createdAt.toDate(),
        assignedAt = assignedAt?.toDate(),
        startedAt = startedAt?.toDate(),
        completedAt = completedAt?.toDate(),
        returnedAt = returnedAt?.toDate(),
        updatedAt = updatedAt.toDate(),
        latitude = latitude,
        longitude = longitude,
        boundaryPoints = boundaryPoints.mapNotNull {
            val lat = it["lat"] ?: return@mapNotNull null
            val lng = it["lng"] ?: return@mapNotNull null
            LatLng(lat, lng)
        },
        blockPolygons = blockPolygons.map { polyStr ->
            polyStr.split(";").mapNotNull { pt ->
                val parts = pt.split(",")
                if (parts.size == 2) {
                    val lat = parts[0].toDoubleOrNull()
                    val lng = parts[1].toDoubleOrNull()
                    if (lat != null && lng != null) LatLng(lat, lng) else null
                } else null
            }
        }.filter { it.size >= 3 }
    )

    fun toMap() = buildMap<String, Any?> {
        put("code", code)
        put("name", name)
        put("description", description)
        put("location", location)
        put("notes", notes)
        put("status", status)
        put("currentResponsibleId", currentResponsibleId)
        put("currentResponsibleName", currentResponsibleName)
        put("currentResponsiblePhotoUrl", currentResponsiblePhotoUrl)
        put("createdAt", createdAt)
        put("assignedAt", assignedAt)
        put("startedAt", startedAt)
        put("completedAt", completedAt)
        put("returnedAt", returnedAt)
        put("updatedAt", updatedAt)
        put("latitude", latitude)
        put("longitude", longitude)
        put("boundaryPoints", boundaryPoints)
        put("blockPolygons", blockPolygons)
        put("syncVersion", syncVersion)
    }

    companion object {
        fun fromDocument(doc: DocumentSnapshot): TerritoryDto? {
            if (!doc.exists()) return null
            return TerritoryDto(
                id = doc.id,
                code = doc.getString("code") ?: "",
                name = doc.getString("name") ?: "",
                description = doc.getString("description") ?: "",
                location = doc.getString("location") ?: "",
                notes = doc.getString("notes") ?: "",
                status = doc.getString("status") ?: "AVAILABLE",
                currentResponsibleId = doc.getString("currentResponsibleId"),
                currentResponsibleName = doc.getString("currentResponsibleName"),
                currentResponsiblePhotoUrl = doc.getString("currentResponsiblePhotoUrl"),
                createdAt = doc.getTimestamp("createdAt") ?: Timestamp.now(),
                assignedAt = doc.getTimestamp("assignedAt"),
                startedAt = doc.getTimestamp("startedAt"),
                completedAt = doc.getTimestamp("completedAt"),
                returnedAt = doc.getTimestamp("returnedAt"),
                updatedAt = doc.getTimestamp("updatedAt") ?: Timestamp.now(),
                latitude = doc.getDouble("latitude"),
                longitude = doc.getDouble("longitude"),
                boundaryPoints = (doc.get("boundaryPoints") as? List<*>)
                    ?.filterIsInstance<Map<*, *>>()
                    ?.map { m ->
                        mapOf(
                            "lat" to ((m["lat"] as? Number)?.toDouble() ?: 0.0),
                            "lng" to ((m["lng"] as? Number)?.toDouble() ?: 0.0)
                        )
                    } ?: emptyList(),
                blockPolygons = (doc.get("blockPolygons") as? List<*>)
                    ?.filterIsInstance<String>()
                    ?: emptyList(),
                syncVersion = doc.getLong("syncVersion") ?: 0L
            )
        }

        fun fromDomain(t: Territory, syncVersion: Long = 0L) = TerritoryDto(
            id = t.id,
            code = t.code,
            name = t.name,
            description = t.description,
            location = t.location,
            notes = t.notes,
            status = t.status.name,
            currentResponsibleId = t.currentResponsibleId,
            currentResponsibleName = t.currentResponsibleName,
            currentResponsiblePhotoUrl = t.currentResponsiblePhotoUrl,
            createdAt = Timestamp(t.createdAt),
            assignedAt = t.assignedAt?.let { Timestamp(it) },
            startedAt = t.startedAt?.let { Timestamp(it) },
            completedAt = t.completedAt?.let { Timestamp(it) },
            returnedAt = t.returnedAt?.let { Timestamp(it) },
            updatedAt = Timestamp(t.updatedAt),
            latitude = t.latitude,
            longitude = t.longitude,
            boundaryPoints = t.boundaryPoints.map { mapOf("lat" to it.lat, "lng" to it.lng) },
            blockPolygons = t.blockPolygons.map { polygon ->
                polygon.joinToString(";") { "${it.lat},${it.lng}" }
            },
            syncVersion = syncVersion
        )
    }
}
