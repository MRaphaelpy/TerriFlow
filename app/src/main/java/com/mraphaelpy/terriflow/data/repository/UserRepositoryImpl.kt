package com.mraphaelpy.terriflow.data.repository

import android.util.Log
import com.mraphaelpy.terriflow.data.local.dao.UserDao
import com.mraphaelpy.terriflow.data.local.entity.UserEntity
import com.mraphaelpy.terriflow.data.remote.source.FirestoreUserSource
import com.mraphaelpy.terriflow.data.sync.SyncPlan
import com.mraphaelpy.terriflow.data.sync.SyncScope
import com.mraphaelpy.terriflow.data.sync.SyncStateStore
import com.mraphaelpy.terriflow.domain.model.User
import com.mraphaelpy.terriflow.domain.repository.CongregationRepository
import com.mraphaelpy.terriflow.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val userDao: UserDao,
    private val remoteSource: FirestoreUserSource,
    private val congregationRepository: CongregationRepository,
    private val syncState: SyncStateStore
) : UserRepository {

    override fun observeAll(): Flow<List<User>> =
        userDao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeById(id: String): Flow<User?> =
        userDao.observeById(id).map { it?.toDomain() }

    override fun observeResponsibles(): Flow<List<User>> =
        userDao.observeResponsibles().map { list -> list.map { it.toDomain() } }

    override suspend fun getById(id: String): User? =
        userDao.getById(id)?.toDomain()

    override suspend fun getActiveAdmins(): List<User> =
        userDao.getActiveAdmins().map { it.toDomain() }

    override suspend fun save(user: User) {
        val congregationId = congregationRepository.getCurrentCongregationId()
        if (congregationId != null) remoteSource.upsert(congregationId, user)
        userDao.upsert(UserEntity.fromDomain(user, synced = congregationId != null))
    }

    override suspend fun updateUserName(userId: String, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return
        val current = getById(userId) ?: return
        save(current.copy(name = trimmed))
    }

    override suspend fun syncFromRemote() {
        val congregationId = congregationRepository.getCurrentCongregationId() ?: return
        val startedAt = System.currentTimeMillis()
        val plan = syncState.plan(SyncScope.USERS, congregationId)
        runCatching {
            val users = when (plan) {
                SyncPlan.Full -> remoteSource.getAll(congregationId)
                is SyncPlan.Incremental -> remoteSource.getChangedSince(congregationId, plan.since)
            }
            userDao.upsertAll(users.map { UserEntity.fromDomain(it.toDomain(), synced = true) })
        }.onSuccess { syncState.markSynced(SyncScope.USERS, congregationId, plan, startedAt) }
            .onFailure { Log.e(TAG, "Erro ao sincronizar usuários", it) }
    }

    private companion object {
        const val TAG = "UserRepository"
    }
}
