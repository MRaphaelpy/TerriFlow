package com.mraphaelpy.terriflow

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import com.google.firebase.FirebaseApp
import com.mraphaelpy.terriflow.sync.RealtimeSyncManager
import com.mraphaelpy.terriflow.sync.StaleTerritoriesWorker
import com.mraphaelpy.terriflow.sync.SyncWorker
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class TerriFlowApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var realtimeSyncManager: RealtimeSyncManager

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(this)
        }
        realtimeSyncManager.start()
        scheduleWorkers()
    }

    private fun scheduleWorkers() {
        val workManager = WorkManager.getInstance(this)

        workManager.enqueueUniqueWork(
            SyncWorker.WORK_NAME_ONCE,
            ExistingWorkPolicy.KEEP,
            SyncWorker.buildOneTimeRequest()
        )

        workManager.enqueueUniquePeriodicWork(
            SyncWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            SyncWorker.buildRequest()
        )

        workManager.enqueueUniquePeriodicWork(
            StaleTerritoriesWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            StaleTerritoriesWorker.buildRequest()
        )
    }
}
