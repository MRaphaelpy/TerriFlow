package com.mraphaelpy.terriflow.domain.usecase.notification

import com.mraphaelpy.terriflow.domain.model.AppNotification
import com.mraphaelpy.terriflow.domain.model.NotificationType
import com.mraphaelpy.terriflow.domain.model.TerritoryAction
import com.mraphaelpy.terriflow.domain.model.displayName
import com.mraphaelpy.terriflow.domain.repository.NotificationRepository
import com.mraphaelpy.terriflow.domain.usecase.territory.TerritoryActionGuard
import java.util.Date
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/** Administrador pede ao dirigente que conclua ou devolva o território. */
class RemindResponsibleUseCase @Inject constructor(
    private val guard: TerritoryActionGuard,
    private val notificationRepository: NotificationRepository
) {
    suspend operator fun invoke(territoryId: String) {
        val (actor, territory) = guard.authorize(territoryId, TerritoryAction.REMIND)
        val now = Date()
        val days = territory.assignedAt?.let { TimeUnit.MILLISECONDS.toDays(now.time - it.time) }
        notificationRepository.save(
            AppNotification.aboutTerritory(
                territory = territory,
                recipientId = territory.currentResponsibleId.orEmpty(),
                title = "Pedido de devolução",
                body = buildString {
                    append("${actor.name} pediu que você conclua ou devolva ${territory.displayName}")
                    days?.let { append(" (com você há $it dias)") }
                    append(".")
                },
                type = NotificationType.TERRITORY_STALE,
                createdAt = now
            )
        )
    }
}
