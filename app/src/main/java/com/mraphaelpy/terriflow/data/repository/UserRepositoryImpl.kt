package com.mraphaelpy.terriflow.data.repository

import android.util.Log
import com.mraphaelpy.terriflow.data.local.dao.TerritoryDao
import com.mraphaelpy.terriflow.data.local.dao.UserDao
import com.mraphaelpy.terriflow.data.local.entity.UserEntity
import com.mraphaelpy.terriflow.data.remote.source.FirestoreUserSource
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
    private val territoryDao: TerritoryDao,
    private val remoteSource: FirestoreUserSource,
    private val congregationRepository: CongregationRepository
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

    /** Grava no servidor e no Room. Lança exceção se o servidor recusar (ex.: sem permissão). */
    override suspend fun save(user: User) {
        val congregationId = congregationRepository.getCurrentCongregationId()
        if (congregationId != null) remoteSource.upsert(congregationId, user)
        userDao.upsert(UserEntity.fromDomain(user, synced = congregationId != null))
        territoryDao.updateResponsibleName(user.id, user.name)
    }

    override suspend fun updateUserName(userId: String, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return
        val current = getById(userId) ?: return
        save(current.copy(name = trimmed))
    }

    override suspend fun syncFromRemote() {
        val congregationId = congregationRepository.getCurrentCongregationId() ?: return
        runCatching {
            userDao.upsertAll(
                remoteSource.getAll(congregationId).map { UserEntity.fromDomain(it.toDomain(), synced = true) }
            )
        }.onFailure { Log.e(TAG, "Erro ao sincronizar usuários", it) }
    }

    private companion object {
        const val TAG = "UserRepository"
    }
}
