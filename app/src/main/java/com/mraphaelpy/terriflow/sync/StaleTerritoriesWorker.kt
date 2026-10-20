package com.mraphaelpy.terriflow.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkerParameters
import com.mraphaelpy.terriflow.domain.service.SystemNotifier
import com.mraphaelpy.terriflow.domain.usecase.notification.CheckStaleTerritoriesUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class StaleTerritoriesWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val checkStaleTerritories: CheckStaleTerritoriesUseCase,
    private val systemNotifier: SystemNotifier
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = runCatching {
        checkStaleTerritories().forEach { systemNotifier.show(it.title, it.body, it.territoryId) }
    }.fold(
        onSuccess = { Result.success() },
        onFailure = { Result.failure() }
    )

    companion object {
        const val WORK_NAME = "stale_territories_worker"

        fun buildRequest(): PeriodicWorkRequest =
            PeriodicWorkRequestBuilder<StaleTerritoriesWorker>(6, TimeUnit.HOURS).build()
    }
}
