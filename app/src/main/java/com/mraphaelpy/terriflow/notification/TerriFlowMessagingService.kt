package com.mraphaelpy.terriflow.notification

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.mraphaelpy.terriflow.data.local.dao.NotificationDao
import com.mraphaelpy.terriflow.data.local.entity.NotificationEntity
import com.mraphaelpy.terriflow.domain.model.NotificationType
import com.mraphaelpy.terriflow.domain.repository.AuthRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.Date
import java.util.UUID
import javax.inject.Inject

@AndroidEntryPoint
class TerriFlowMessagingService : FirebaseMessagingService() {

    @Inject lateinit var authRepository: AuthRepository
    @Inject lateinit var notificationDao: NotificationDao
    @Inject lateinit var localNotificationHelper: LocalNotificationHelper

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        serviceScope.launch {
            runCatching { authRepository.updateFcmToken(token) }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val title = remoteMessage.notification?.title ?: remoteMessage.data["title"] ?: return
        val body = remoteMessage.notification?.body ?: remoteMessage.data["body"] ?: return
        val territoryId = remoteMessage.data["territoryId"]
        val territoryCode = remoteMessage.data["territoryCode"]
        val typeStr = remoteMessage.data["type"] ?: "GENERAL"
        val userId = authRepository.currentUserId ?: return

        localNotificationHelper.showLocalNotification(title, body, territoryId)

        serviceScope.launch {
            runCatching {
                val entity = NotificationEntity(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    title = title,
                    body = body,
                    type = runCatching { NotificationType.valueOf(typeStr) }
                        .getOrDefault(NotificationType.GENERAL).name,
                    territoryId = territoryId,
                    territoryCode = territoryCode,
                    read = false,
                    createdAt = Date()
                )
                notificationDao.insert(entity)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.coroutineContext[kotlinx.coroutines.Job]?.cancel()
    }
}
