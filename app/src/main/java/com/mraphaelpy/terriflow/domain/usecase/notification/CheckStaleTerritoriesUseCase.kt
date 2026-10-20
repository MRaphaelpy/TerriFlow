package com.mraphaelpy.terriflow.domain.usecase.notification

import com.mraphaelpy.terriflow.domain.model.AppNotification
import com.mraphaelpy.terriflow.domain.model.NotificationType
import com.mraphaelpy.terriflow.domain.model.Territory
import com.mraphaelpy.terriflow.domain.model.TerritoryStatus
import com.mraphaelpy.terriflow.domain.model.displayName
import com.mraphaelpy.terriflow.domain.repository.AuthRepository
import com.mraphaelpy.terriflow.domain.repository.NotificationRepository
import com.mraphaelpy.terriflow.domain.repository.TerritoryRepository
import java.util.Date
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class CheckStaleTerritoriesUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val territoryRepository: TerritoryRepository,
    private val notificationRepository: NotificationRepository
) {
    suspend operator fun invoke(now: Date = Date()): List<AppNotification> {
        val userId = authRepository.currentUserId ?: return emptyList()
        return territoryRepository.getByResponsible(userId)
            .mapNotNull { reminderFor(it, now) }
            .filter { notification ->
                if (notificationRepository.exists(notification.id)) return@filter false
                notificationRepository.save(notification)
                true
            }
    }

    private fun reminderFor(territory: Territory, now: Date): AppNotification? {
        val assignedAt = territory.assignedAt ?: return null
        val days = TimeUnit.MILLISECONDS.toDays(now.time - assignedAt.time)
        val notStarted = territory.status == TerritoryStatus.ASSIGNED && territory.startedAt == null
        val milestones = when {
            notStarted -> NOT_STARTED_MILESTONES
            territory.status == TerritoryStatus.IN_PROGRESS || territory.status == TerritoryStatus.PAUSED -> WORKING_MILESTONES
            else -> return null
        }
        val milestone = milestones.lastOrNull { days >= it } ?: return null

        val (title, body) = if (notStarted) {
            "Território aguardando início" to
                "${territory.displayName} foi designado a você há $days dias e ainda não foi iniciado."
        } else {
            "Território com você há ${describe(milestone)}" to
                "Lembre-se de concluir ou devolver ${territory.displayName}."
        }
        return AppNotification.aboutTerritory(
            territory = territory,
            recipientId = territory.currentResponsibleId.orEmpty(),
            title = title,
            body = body,
            type = NotificationType.TERRITORY_STALE,
            createdAt = now,
            id = "reminder-${territory.id}-${assignedAt.time}-$milestone"
        )
    }

    private fun describe(days: Long): String = if (days >= 60) "${days / 30} meses" else "$days dias"

    companion object {
        val NOT_STARTED_MILESTONES = listOf(7L, 30L)
        val WORKING_MILESTONES = listOf(60L, 120L)
    }
}
