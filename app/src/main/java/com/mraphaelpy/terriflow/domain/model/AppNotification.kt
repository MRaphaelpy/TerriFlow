package com.mraphaelpy.terriflow.domain.model

import java.util.Date
import java.util.UUID

enum class NotificationType {
    TERRITORY_ASSIGNED,
    TERRITORY_TRANSFERRED,
    TERRITORY_COMPLETED,
    TERRITORY_RETURNED,
    TERRITORY_STALE,
    GENERAL
}

data class AppNotification(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val body: String = "",
    val type: NotificationType = NotificationType.GENERAL,
    val territoryId: String? = null,
    val territoryCode: String? = null,
    val read: Boolean = false,
    val createdAt: Date = Date()
) {
    val isBroadcast: Boolean get() = userId.isBlank() || userId == BROADCAST_ALL

    companion object {
        /** Notificações com [userId] vazio são exibidas para todos da congregação. */
        const val BROADCAST = ""
        private const val BROADCAST_ALL = "ALL"

        fun aboutTerritory(
            territory: Territory,
            recipientId: String,
            title: String,
            body: String,
            type: NotificationType,
            createdAt: Date = Date(),
            id: String = UUID.randomUUID().toString()
        ) = AppNotification(
            id = id,
            userId = recipientId,
            title = title,
            body = body,
            type = type,
            territoryId = territory.id,
            territoryCode = territory.code,
            createdAt = createdAt
        )
    }
}

/** Texto padrão "T-00001 — Nome" usado nas notificações. */
val Territory.displayName: String get() = "$code — $name"
