package com.mraphaelpy.terriflow.domain.usecase.notification

import com.mraphaelpy.terriflow.domain.model.AppNotification
import com.mraphaelpy.terriflow.domain.model.NotificationType
import com.mraphaelpy.terriflow.domain.model.Territory
import com.mraphaelpy.terriflow.domain.model.User
import com.mraphaelpy.terriflow.domain.repository.NotificationRepository
import com.mraphaelpy.terriflow.domain.repository.UserRepository
import java.util.Date
import javax.inject.Inject

/**
 * Avisa os administradores e o responsável pelo território sobre uma mudança.
 * Cada destinatário recebe a própria notificação; quem executou a ação não é avisado.
 */
class NotifyAdminsAndOwnerUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val notificationRepository: NotificationRepository
) {
    suspend operator fun invoke(
        territory: Territory,
        ownerId: String?,
        actor: User,
        title: String,
        body: String,
        type: NotificationType,
        createdAt: Date = Date()
    ) {
        val recipients = (userRepository.getActiveAdmins().map { it.id } + listOfNotNull(ownerId))
            .filter { it.isNotBlank() && it != actor.id }
            .distinct()

        recipients.forEach { recipientId ->
            notificationRepository.save(
                AppNotification.aboutTerritory(
                    territory = territory,
                    recipientId = recipientId,
                    title = title,
                    body = body,
                    type = type,
                    createdAt = createdAt
                )
            )
        }
    }
}
