package com.mraphaelpy.terriflow.data.repository

import com.mraphaelpy.terriflow.data.local.dao.NotificationDao
import com.mraphaelpy.terriflow.data.local.entity.NotificationEntity
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
    private val notificationDao: NotificationDao,
    private val remoteSource: FirestoreNotificationSource,
    private val congregationRepository: CongregationRepository
) : NotificationRepository {

    override fun observeByUser(userId: String): Flow<List<AppNotification>> =
        notificationDao.observeByUser(userId).map { list -> list.map { it.toDomain() } }

    override fun countUnread(userId: String): Flow<Int> =
        notificationDao.countUnread(userId)

    override suspend fun save(notification: AppNotification) {
        notificationDao.insert(NotificationEntity.fromDomain(notification))
        val congregationId = congregationRepository.getCurrentCongregationId() ?: return
        runCatching { remoteSource.insert(congregationId, notification) }
    }

    override suspend fun markRead(id: String) {
        notificationDao.markRead(id)
    }

    override suspend fun markAllRead(userId: String) {
        notificationDao.markAllRead(userId)
    }
}
