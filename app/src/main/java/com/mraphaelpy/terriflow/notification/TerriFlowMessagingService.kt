package com.mraphaelpy.terriflow.notification

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.mraphaelpy.terriflow.di.ApplicationScope
import com.mraphaelpy.terriflow.domain.model.AppNotification
import com.mraphaelpy.terriflow.domain.model.NotificationType
import com.mraphaelpy.terriflow.domain.repository.AuthRepository
import com.mraphaelpy.terriflow.domain.repository.NotificationRepository
import com.mraphaelpy.terriflow.domain.service.SystemNotifier
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@AndroidEntryPoint
class TerriFlowMessagingService : FirebaseMessagingService() {

    @Inject lateinit var authRepository: AuthRepository
    @Inject lateinit var notificationRepository: NotificationRepository
    @Inject lateinit var systemNotifier: SystemNotifier
    @Inject @ApplicationScope lateinit var appScope: CoroutineScope

    override fun onNewToken(token: String) {
        appScope.launch {
            runCatching { authRepository.updateFcmToken(token) }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val title = remoteMessage.notification?.title ?: remoteMessage.data["title"] ?: return
        val body = remoteMessage.notification?.body ?: remoteMessage.data["body"] ?: return
        val userId = authRepository.currentUserId ?: return
        val notificationId = remoteMessage.data["notificationId"]

        appScope.launch {
            if (notificationId != null && notificationRepository.exists(notificationId)) return@launch
            val notification = AppNotification(
                id = notificationId ?: UUID.randomUUID().toString(),
                userId = userId,
                title = title,
                body = body,
                type = runCatching { NotificationType.valueOf(remoteMessage.data["type"].orEmpty()) }
                    .getOrDefault(NotificationType.GENERAL),
                territoryId = remoteMessage.data["territoryId"],
                territoryCode = remoteMessage.data["territoryCode"]
            )
            runCatching { notificationRepository.saveLocally(notification) }
            systemNotifier.show(title, body, notification.territoryId)
        }
    }
}
