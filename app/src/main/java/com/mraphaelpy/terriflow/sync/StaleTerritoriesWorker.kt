package com.mraphaelpy.terriflow.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.mraphaelpy.terriflow.domain.model.AppNotification
import com.mraphaelpy.terriflow.domain.model.NotificationType
import com.mraphaelpy.terriflow.domain.repository.NotificationRepository
import com.mraphaelpy.terriflow.domain.repository.TerritoryRepository
import com.mraphaelpy.terriflow.notification.LocalNotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.Date
import java.util.UUID
import java.util.concurrent.TimeUnit

@HiltWorker
class StaleTerritoriesWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val territoryRepository: TerritoryRepository,
    private val notificationRepository: NotificationRepository,
    private val notificationHelper: LocalNotificationHelper
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val WORK_NAME = "stale_territories_worker"
        private const val ASSIGNED_STALE_DAYS = 7L
        private const val IN_PROGRESS_STALE_DAYS = 14L

        fun buildRequest(): PeriodicWorkRequest =
            PeriodicWorkRequestBuilder<StaleTerritoriesWorker>(6, TimeUnit.HOURS)
                .build()
    }

    override suspend fun doWork(): Result {
        return runCatching {
            checkAssignedNotStarted()
            checkInProgressStale()
        }.fold(
            onSuccess = { Result.success() },
            onFailure = { Result.failure() }
        )
    }

    private suspend fun checkAssignedNotStarted() {
        val staleCutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(ASSIGNED_STALE_DAYS)
        val territories = territoryRepository.getAssignedNotStarted()
        territories.forEach { territory ->
            val assignedTime = territory.assignedAt?.time ?: return@forEach
            if (assignedTime < staleCutoff && territory.currentResponsibleId != null) {
                val daysSince = TimeUnit.MILLISECONDS.toDays(
                    System.currentTimeMillis() - assignedTime
                )
                val msg = "Território ${territory.code} está aguardando início há $daysSince dias."
                notificationHelper.showLocalNotification(
                    title = "Território parado",
                    body = msg,
                    territoryId = territory.id
                )
                val notification = AppNotification(
                    id = UUID.randomUUID().toString(),
                    userId = territory.currentResponsibleId,
                    title = "Território parado",
                    body = msg,
                    type = NotificationType.TERRITORY_STALE,
                    territoryId = territory.id,
                    territoryCode = territory.code,
                    createdAt = Date()
                )
                notificationRepository.save(notification)
            }
        }
    }

    private suspend fun checkInProgressStale() {
        val staleCutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(IN_PROGRESS_STALE_DAYS)
        val territories = territoryRepository.getInProgress()
        territories.forEach { territory ->
            val startTime = territory.startedAt?.time ?: return@forEach
            if (startTime < staleCutoff && territory.currentResponsibleId != null) {
                val daysSince = TimeUnit.MILLISECONDS.toDays(
                    System.currentTimeMillis() - startTime
                )
                val msg = "Território ${territory.code} está em andamento há $daysSince dias."
                notificationHelper.showLocalNotification(
                    title = "Território em andamento há muito tempo",
                    body = msg,
                    territoryId = territory.id
                )
                val notification = AppNotification(
                    id = UUID.randomUUID().toString(),
                    userId = territory.currentResponsibleId,
                    title = "Território em andamento há muito tempo",
                    body = msg,
                    type = NotificationType.TERRITORY_STALE,
                    territoryId = territory.id,
                    territoryCode = territory.code,
                    createdAt = Date()
                )
                notificationRepository.save(notification)
            }
        }
    }
}
