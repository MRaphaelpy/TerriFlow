package com.mraphaelpy.terriflow.data.sync

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Date
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

enum class SyncScope(val fullInterval: Long) {
    TERRITORIES(TimeUnit.DAYS.toMillis(1)),
    USERS(TimeUnit.DAYS.toMillis(1)),
    EVENTS(TimeUnit.DAYS.toMillis(7)),
    NOTIFICATIONS(TimeUnit.DAYS.toMillis(7))
}

sealed interface SyncPlan {
    data object Full : SyncPlan
    data class Incremental(val since: Date) : SyncPlan
}

object SyncPlanner {
    val SAFETY_MARGIN_MS = TimeUnit.MINUTES.toMillis(2)

    fun plan(lastSync: Long?, lastFullSync: Long?, fullInterval: Long, now: Long): SyncPlan {
        if (lastSync == null || lastFullSync == null || now - lastFullSync >= fullInterval) return SyncPlan.Full
        return SyncPlan.Incremental(Date(lastSync - SAFETY_MARGIN_MS))
    }
}

@Singleton
class SyncStateStore @Inject constructor(@ApplicationContext context: Context) {

    private val prefs = context.getSharedPreferences("terriflow_sync", Context.MODE_PRIVATE)

    fun plan(scope: SyncScope, congregationId: String, now: Long = System.currentTimeMillis()): SyncPlan =
        SyncPlanner.plan(
            lastSync = prefs.getLong(key(scope, congregationId, "last"), 0L).takeIf { it > 0 },
            lastFullSync = prefs.getLong(key(scope, congregationId, "full"), 0L).takeIf { it > 0 },
            fullInterval = scope.fullInterval,
            now = now
        )

    fun markSynced(scope: SyncScope, congregationId: String, plan: SyncPlan, startedAt: Long) {
        prefs.edit {
            putLong(key(scope, congregationId, "last"), startedAt)
            if (plan is SyncPlan.Full) putLong(key(scope, congregationId, "full"), startedAt)
        }
    }

    fun lastSync(scope: SyncScope, congregationId: String): Date? =
        prefs.getLong(key(scope, congregationId, "last"), 0L).takeIf { it > 0 }?.let(::Date)

    fun clear() = prefs.edit { clear() }

    private fun key(scope: SyncScope, congregationId: String, kind: String) = "${scope.name}_${kind}_$congregationId"
}
