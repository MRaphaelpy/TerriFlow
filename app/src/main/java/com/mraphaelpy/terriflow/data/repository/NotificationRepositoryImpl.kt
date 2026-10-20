package com.mraphaelpy.terriflow.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.mraphaelpy.terriflow.data.local.dao.NotificationDao
import com.mraphaelpy.terriflow.data.local.entity.NotificationEntity
import com.mraphaelpy.terriflow.data.remote.RemoteWriter
import com.mraphaelpy.terriflow.data.remote.source.FirestoreNotificationSource
import com.mraphaelpy.terriflow.data.sync.SyncPlan
import com.mraphaelpy.terriflow.data.sync.SyncScope
import com.mraphaelpy.terriflow.data.sync.SyncStateStore
import com.mraphaelpy.terriflow.domain.model.AppNotification
import com.mraphaelpy.terriflow.domain.repository.CongregationRepository
import com.mraphaelpy.terriflow.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val notificationDao: NotificationDao,
    private val remoteSource: FirestoreNotificationSource,
    private val remoteWriter: RemoteWriter,
    private val congregationRepository: CongregationRepository,
    private val syncState: SyncStateStore
) : NotificationRepository {

    override fun observeByUser(userId: String): Flow<List<AppNotification>> =
        notificationDao.observeByUser(userId).map { list -> list.map { it.toDomain() } }

    override fun countUnread(userId: String): Flow<Int> =
        notificationDao.countUnreadByUser(userId)

    override suspend fun exists(id: String): Boolean =
        notificationDao.getById(id) != null

    override suspend fun syncFromRemote() {
        val userId = auth.currentUser?.uid ?: return
        val congregationId = congregationRepository.getCurrentCongregationId() ?: return
        val startedAt = System.currentTimeMillis()
        val plan = syncState.plan(SyncScope.NOTIFICATIONS, congregationId)
        runCatching {
            val notifications = when (plan) {
                SyncPlan.Full -> remoteSource.getRecentForUser(congregationId, userId)
                is SyncPlan.Incremental -> remoteSource.getForUserSince(congregationId, userId, plan.since)
            }
            notificationDao.insertAll(notifications.map(NotificationEntity::fromDomain))
            val readRemotely = notifications.filter { it.read }.map { it.id }
            if (readRemotely.isNotEmpty()) notificationDao.markReadIds(readRemotely)
        }.onSuccess {
            syncState.markSynced(SyncScope.NOTIFICATIONS, congregationId, plan, startedAt)
        }.onFailure {
            Log.e(TAG, "Erro ao sincronizar notificações", it)
        }
    }

    override suspend fun save(notification: AppNotification) {
        notificationDao.insert(NotificationEntity.fromDomain(notification))
        val congregationId = congregationRepository.getCurrentCongregationId() ?: return
        remoteWriter.write(TAG, { remoteSource.insert(congregationId, notification) })
    }

    override suspend fun saveLocally(notification: AppNotification) {
        notificationDao.insertAll(listOf(NotificationEntity.fromDomain(notification)))
    }

    override suspend fun markRead(id: String) {
        notificationDao.markRead(id)
        pushRead(listOf(id))
    }

    override suspend fun markAllRead(userId: String) {
        val unread = notificationDao.getUnreadIds(userId)
        notificationDao.markAllReadByUser(userId)
        pushRead(unread)
    }

    private suspend fun pushRead(ids: List<String>) {
        if (ids.isEmpty()) return
        val congregationId = congregationRepository.getCurrentCongregationId() ?: return
        ids.forEach { id -> remoteWriter.write(TAG, { remoteSource.markRead(congregationId, id) }) }
    }

    private companion object {
        const val TAG = "NotificationRepository"
    }
}
