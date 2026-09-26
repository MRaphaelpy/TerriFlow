package com.mraphaelpy.terriflow.sync

import com.mraphaelpy.terriflow.data.local.dao.NotificationDao
import com.mraphaelpy.terriflow.data.local.dao.TerritoryDao
import com.mraphaelpy.terriflow.data.local.dao.TerritoryEventDao
import com.mraphaelpy.terriflow.data.local.entity.NotificationEntity
import com.mraphaelpy.terriflow.data.local.entity.TerritoryEntity
import com.mraphaelpy.terriflow.data.local.entity.TerritoryEventEntity
import com.mraphaelpy.terriflow.data.remote.source.FirestoreEventSource
import com.mraphaelpy.terriflow.data.remote.source.FirestoreNotificationSource
import com.mraphaelpy.terriflow.data.remote.source.FirestoreTerritorySource
import com.mraphaelpy.terriflow.domain.repository.AuthRepository
import com.mraphaelpy.terriflow.domain.repository.CongregationRepository
import com.mraphaelpy.terriflow.notification.LocalNotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RealtimeSyncManager @Inject constructor(
    private val authRepository: AuthRepository,
    private val congregationRepository: CongregationRepository,
    private val territoryDao: TerritoryDao,
    private val notificationDao: NotificationDao,
    private val eventDao: TerritoryEventDao,
    private val localNotificationHelper: LocalNotificationHelper,
    private val territoryRemoteSource: FirestoreTerritorySource,
    private val notificationRemoteSource: FirestoreNotificationSource,
    private val eventRemoteSource: FirestoreEventSource
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun start() {
        scope.launch {
            authRepository.observeCurrentUser().collectLatest { user ->
                if (user == null) return@collectLatest

                val congregationId = congregationRepository.getCurrentCongregationId()
                    ?: return@collectLatest

                val territoryFlow = territoryRemoteSource.observeAll(congregationId)

                supervisorScope {
                    launch {
                        runCatching {
                            territoryFlow.collect { dtos ->
                                dtos.forEach { dto ->
                                    val local = territoryDao.getById(dto.id)
                                    when {
                                        local == null -> territoryDao.upsert(
                                            TerritoryEntity.fromDomain(dto.toDomain(), synced = true)
                                                .copy(syncVersion = dto.syncVersion)
                                        )
                                        !local.synced -> Unit
                                        dto.syncVersion >= local.syncVersion -> territoryDao.upsert(
                                            TerritoryEntity.fromDomain(dto.toDomain(), synced = true)
                                                .copy(syncVersion = dto.syncVersion)
                                        )
                                        else -> Unit
                                    }
                                }
                            }
                        }
                    }

                    launch {
                        runCatching {
                            notificationRemoteSource.observeByUser(congregationId, user.id).collect { notifications ->
                                notifications.forEach { notification ->
                                    val existing = notificationDao.getById(notification.id)
                                    if (existing == null) {
                                        notificationDao.insert(NotificationEntity.fromDomain(notification))
                                        localNotificationHelper.showLocalNotification(
                                            notification.title,
                                            notification.body,
                                            notification.territoryId
                                        )
                                    }
                                }
                            }
                        }
                    }

                    launch {
                        runCatching {
                            eventRemoteSource.observeRecentGlobal(congregationId, 50).collect { dtos ->
                                val entities = dtos.map {
                                    TerritoryEventEntity.fromDomain(it.toDomain(), synced = true)
                                }
                                eventDao.insertAll(entities)
                            }
                        }
                    }
                }
            }
        }
    }
}
