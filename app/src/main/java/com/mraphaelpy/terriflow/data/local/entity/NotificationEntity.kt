package com.mraphaelpy.terriflow.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mraphaelpy.terriflow.domain.model.AppNotification
import com.mraphaelpy.terriflow.domain.model.NotificationType
import java.util.Date

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val title: String,
    val body: String,
    val type: String,
    val territoryId: String?,
    val territoryCode: String?,
    val read: Boolean = false,
    val createdAt: Date
) {
    fun toDomain() = AppNotification(
        id = id,
        userId = userId,
        title = title,
        body = body,
        type = NotificationType.valueOf(type),
        territoryId = territoryId,
        territoryCode = territoryCode,
        read = read,
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(n: AppNotification) = NotificationEntity(
            id = n.id,
            userId = n.userId,
            title = n.title,
            body = n.body,
            type = n.type.name,
            territoryId = n.territoryId,
            territoryCode = n.territoryCode,
            read = n.read,
            createdAt = n.createdAt
        )
    }
}
