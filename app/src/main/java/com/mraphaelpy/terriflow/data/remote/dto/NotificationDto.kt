package com.mraphaelpy.terriflow.data.remote.dto

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.mraphaelpy.terriflow.domain.model.AppNotification
import com.mraphaelpy.terriflow.domain.model.NotificationType
import java.util.Date

object NotificationDto {

    fun fromDocument(doc: DocumentSnapshot): AppNotification? {
        if (!doc.exists()) return null
        return AppNotification(
            id = doc.id,
            userId = doc.getString("userId") ?: "",
            title = doc.getString("title") ?: "",
            body = doc.getString("body") ?: "",
            type = runCatching { NotificationType.valueOf(doc.getString("type") ?: "") }
                .getOrDefault(NotificationType.GENERAL),
            territoryId = doc.getString("territoryId"),
            territoryCode = doc.getString("territoryCode"),
            read = doc.getBoolean("read") ?: false,
            createdAt = doc.getTimestamp("createdAt")?.toDate() ?: Date()
        )
    }

    fun toMap(notification: AppNotification): Map<String, Any?> = mapOf(
        "userId" to notification.userId,
        "title" to notification.title,
        "body" to notification.body,
        "type" to notification.type.name,
        "territoryId" to notification.territoryId,
        "territoryCode" to notification.territoryCode,
        "read" to notification.read,
        "createdAt" to Timestamp(notification.createdAt)
    )
}
