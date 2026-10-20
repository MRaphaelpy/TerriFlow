package com.mraphaelpy.terriflow.domain.repository

import com.mraphaelpy.terriflow.domain.model.AppNotification
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {
    fun observeByUser(userId: String): Flow<List<AppNotification>>
    fun countUnread(userId: String): Flow<Int>
    suspend fun exists(id: String): Boolean
    /** Baixa as notificações do usuário logado. */
    suspend fun syncFromRemote()

    /** Salva localmente e envia para a congregação. */
    suspend fun save(notification: AppNotification)

    /** Salva apenas neste aparelho (ex.: push recebido). Ignora se o id já existir. */
    suspend fun saveLocally(notification: AppNotification)
    suspend fun markRead(id: String)
    suspend fun markAllRead(userId: String)
}
