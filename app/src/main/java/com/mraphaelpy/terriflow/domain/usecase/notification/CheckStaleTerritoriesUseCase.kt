package com.mraphaelpy.terriflow.domain.usecase.notification

import com.mraphaelpy.terriflow.domain.model.AppNotification
import com.mraphaelpy.terriflow.domain.model.NotificationType
import com.mraphaelpy.terriflow.domain.model.Territory
import com.mraphaelpy.terriflow.domain.repository.AuthRepository
import com.mraphaelpy.terriflow.domain.repository.NotificationRepository
import com.mraphaelpy.terriflow.domain.repository.TerritoryRepository
import java.util.Date
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Gera lembretes para os territórios do usuário logado que estão parados.
 *
 * Cada lembrete tem id determinístico (território + início do período + semana),
 * então rodar o worker várias vezes ao dia ou em vários aparelhos não duplica:
 * o responsável recebe no máximo um lembrete por semana para cada território.
 */
class CheckStaleTerritoriesUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val territoryRepository: TerritoryRepository,
    private val notificationRepository: NotificationRepository
) {
    /** Retorna apenas as notificações novas criadas nesta execução. */
    suspend operator fun invoke(now: Date = Date()): List<AppNotification> {
        val userId = authRepository.currentUserId ?: return emptyList()

        val waitingToStart = territoryRepository.getAssignedNotStarted()
            .filter { it.currentResponsibleId == userId }
            .mapNotNull { territory ->
                reminderFor(territory, territory.assignedAt, ASSIGNED_STALE_DAYS, now, "assigned") { days ->
                    "Território parado" to "Território ${territory.code} está aguardando início há $days dias."
                }
            }

        val runningTooLong = territoryRepository.getInProgress()
            .filter { it.currentResponsibleId == userId }
            .mapNotNull { territory ->
                reminderFor(territory, territory.startedAt, IN_PROGRESS_STALE_DAYS, now, "in-progress") { days ->
                    "Território em andamento há muito tempo" to
                        "Território ${territory.code} está em andamento há $days dias."
                }
            }

        return (waitingToStart + runningTooLong).filter { notification ->
            if (notificationRepository.exists(notification.id)) return@filter false
            notificationRepository.save(notification)
            true
        }
    }

    private fun reminderFor(
        territory: Territory,
        since: Date?,
        thresholdDays: Long,
        now: Date,
        kind: String,
        text: (days: Long) -> Pair<String, String>
    ): AppNotification? {
        val start = since ?: return null
        val days = TimeUnit.MILLISECONDS.toDays(now.time - start.time)
        if (days < thresholdDays) return null

        val week = (days - thresholdDays) / DAYS_PER_WEEK
        val (title, body) = text(days)
        return AppNotification.aboutTerritory(
            territory = territory,
            recipientId = territory.currentResponsibleId.orEmpty(),
            title = title,
            body = body,
            type = NotificationType.TERRITORY_STALE,
            createdAt = now,
            id = "stale-$kind-${territory.id}-${start.time}-$week"
        )
    }

    private companion object {
        const val ASSIGNED_STALE_DAYS = 7L
        const val IN_PROGRESS_STALE_DAYS = 14L
        const val DAYS_PER_WEEK = 7L
    }
}
