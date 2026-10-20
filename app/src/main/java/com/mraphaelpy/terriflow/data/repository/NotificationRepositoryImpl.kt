package com.mraphaelpy.terriflow.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.mraphaelpy.terriflow.data.local.dao.NotificationDao
import com.mraphaelpy.terriflow.data.local.entity.NotificationEntity
import com.mraphaelpy.terriflow.data.remote.RemoteWriter
import com.mraphaelpy.terriflow.data.remote.source.FirestoreNotificationSource
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
    private val congregationRepository: CongregationRepository
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
        runCatching {
            val notifications = remoteSource.getRecentForUser(congregationId, userId)
            notificationDao.insertAll(notifications.map(NotificationEntity::fromDomain))
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
    }

    override suspend fun markAllRead(userId: String) {
        notificationDao.markAllReadByUser(userId)
    }

    private companion object {
        const val TAG = "NotificationRepository"
    }
}
