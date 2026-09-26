package com.mraphaelpy.terriflow.domain.model

import java.util.Date

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
)
