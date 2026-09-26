package com.mraphaelpy.terriflow.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.mraphaelpy.terriflow.domain.model.EventType
import com.mraphaelpy.terriflow.domain.model.TerritoryEvent
import java.util.Date

@Entity(
    tableName = "territory_events",
    indices = [Index("territoryId"), Index("userId")]
)
data class TerritoryEventEntity(
    @PrimaryKey val id: String,
    val territoryId: String,
    val userId: String,
    val userName: String,
    val type: String,
    val timestamp: Date,
    val extra: Map<String, String> = emptyMap(),
    val congregationId: String = "",
    val synced: Boolean = false
) {
    fun toDomain() = TerritoryEvent(
        id = id,
        territoryId = territoryId,
        userId = userId,
        userName = userName,
        type = EventType.valueOf(type),
        timestamp = timestamp,
        extra = extra,
        congregationId = congregationId
    )

    companion object {
        fun fromDomain(event: TerritoryEvent, synced: Boolean = false) = TerritoryEventEntity(
            id = event.id,
            territoryId = event.territoryId,
            userId = event.userId,
            userName = event.userName,
            type = event.type.name,
            timestamp = event.timestamp,
            extra = event.extra,
            congregationId = event.congregationId,
            synced = synced
        )
    }
}
