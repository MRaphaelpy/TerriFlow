package com.mraphaelpy.terriflow.data.remote.dto

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.mraphaelpy.terriflow.domain.model.EventType
import com.mraphaelpy.terriflow.domain.model.TerritoryEvent

data class TerritoryEventDto(
    val id: String = "",
    val territoryId: String = "",
    val userId: String = "",
    val userName: String = "",
    val type: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val extra: Map<String, String> = emptyMap()
) {
    fun toDomain() = TerritoryEvent(
        id = id,
        territoryId = territoryId,
        userId = userId,
        userName = userName,
        type = runCatching { EventType.valueOf(type) }.getOrDefault(EventType.UPDATED),
        timestamp = timestamp.toDate(),
        extra = extra
    )

    fun toMap() = mapOf(
        "territoryId" to territoryId,
        "userId" to userId,
        "userName" to userName,
        "type" to type,
        "timestamp" to timestamp,
        "extra" to extra
    )

    companion object {
        fun fromDocument(doc: DocumentSnapshot): TerritoryEventDto? {
            if (!doc.exists()) return null
            @Suppress("UNCHECKED_CAST")
            return TerritoryEventDto(
                id = doc.id,
                territoryId = doc.getString("territoryId") ?: "",
                userId = doc.getString("userId") ?: "",
                userName = doc.getString("userName") ?: "",
                type = doc.getString("type") ?: "",
                timestamp = doc.getTimestamp("timestamp") ?: Timestamp.now(),
                extra = (doc.get("extra") as? Map<String, String>) ?: emptyMap()
            )
        }

        fun fromDomain(event: TerritoryEvent) = TerritoryEventDto(
            id = event.id,
            territoryId = event.territoryId,
            userId = event.userId,
            userName = event.userName,
            type = event.type.name,
            timestamp = Timestamp(event.timestamp),
            extra = event.extra
        )
    }
}
