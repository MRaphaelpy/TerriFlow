package com.mraphaelpy.terriflow.sync

import android.util.Log
import com.mraphaelpy.terriflow.data.local.dao.NotificationDao
import com.mraphaelpy.terriflow.data.local.dao.TerritoryEventDao
import com.mraphaelpy.terriflow.data.local.entity.NotificationEntity
import com.mraphaelpy.terriflow.data.local.entity.TerritoryEventEntity
import com.mraphaelpy.terriflow.data.remote.source.FirestoreEventSource
import com.mraphaelpy.terriflow.data.remote.source.FirestoreNotificationSource
import com.mraphaelpy.terriflow.data.remote.source.FirestoreTerritorySource
import com.mraphaelpy.terriflow.data.sync.TerritoryRemoteMerger
import com.mraphaelpy.terriflow.di.ApplicationScope
import com.mraphaelpy.terriflow.domain.repository.AuthRepository
import com.mraphaelpy.terriflow.domain.repository.CongregationRepository
import com.mraphaelpy.terriflow.domain.service.SystemNotifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RealtimeSyncManager @Inject constructor(
    private val authRepository: AuthRepository,
    private val congregationRepository: CongregationRepository,
    private val territoryMerger: TerritoryRemoteMerger,
    private val notificationDao: NotificationDao,
    private val eventDao: TerritoryEventDao,
    private val systemNotifier: SystemNotifier,
    private val territoryRemoteSource: FirestoreTerritorySource,
    private val notificationRemoteSource: FirestoreNotificationSource,
    private val eventRemoteSource: FirestoreEventSource,
    @ApplicationScope private val scope: CoroutineScope
) {
    fun start() {
        scope.launch {
            authRepository.observeCurrentUserId().collectLatest { uid ->
                if (uid == null) return@collectLatest
                val congregationId = congregationRepository.getCurrentCongregationId() ?: return@collectLatest

                launch { syncTerritories(congregationId) }
                launch { syncNotifications(congregationId, uid) }
                launch { syncRecentEvents(congregationId) }
            }
        }
    }

    private suspend fun syncTerritories(congregationId: String) {
        territoryRemoteSource.observeAll(congregationId).collectSafely("territórios") { snapshot ->
            territoryMerger.merge(snapshot.territories, removeMissing = !snapshot.isFromCache)
        }
    }

    private suspend fun syncNotifications(congregationId: String, userId: String) {
        var isFirstSnapshot = true
        notificationRemoteSource.observeForUser(congregationId, userId).collectSafely("notificações") { notifications ->
            notifications.forEach { notification ->
                if (notificationDao.getById(notification.id) != null) return@forEach
                notificationDao.insert(NotificationEntity.fromDomain(notification))
                if (!isFirstSnapshot) {
                    systemNotifier.show(notification.title, notification.body, notification.territoryId)
                }
            }
            isFirstSnapshot = false
        }
    }

    private suspend fun syncRecentEvents(congregationId: String) {
        eventRemoteSource.observeRecent(congregationId).collectSafely("eventos") { dtos ->
            eventDao.insertAll(dtos.map { TerritoryEventEntity.fromDomain(it.toDomain(), synced = true) })
        }
    }

    private suspend fun <T> Flow<T>.collectSafely(name: String, action: suspend (T) -> Unit) {
        catch { Log.e(TAG, "Listener de $name encerrado", it) }.collect { value ->
            runCatching { action(value) }.onFailure { Log.e(TAG, "Erro ao aplicar $name", it) }
        }
    }

    private companion object {
        const val TAG = "RealtimeSyncManager"
    }
}
