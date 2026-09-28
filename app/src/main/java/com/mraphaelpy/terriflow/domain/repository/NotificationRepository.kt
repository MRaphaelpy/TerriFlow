package com.mraphaelpy.terriflow.domain.repository

import com.mraphaelpy.terriflow.domain.model.AppNotification
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {
    fun observeByUser(userId: String): Flow<List<AppNotification>>
    fun countUnread(userId: String = ""): Flow<Int>
    suspend fun syncFromRemote()
    suspend fun save(notification: AppNotification)
    suspend fun markRead(id: String)
    suspend fun markAllRead(userId: String = "")
}
