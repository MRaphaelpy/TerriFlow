package com.mraphaelpy.terriflow.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.mraphaelpy.terriflow.data.local.dao.UserDao
import com.mraphaelpy.terriflow.data.local.entity.UserEntity
import com.mraphaelpy.terriflow.data.remote.dto.UserDto
import com.mraphaelpy.terriflow.data.remote.source.FirestoreUserSource
import com.mraphaelpy.terriflow.domain.model.User
import com.mraphaelpy.terriflow.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val userDao: UserDao,
    private val remoteSource: FirestoreUserSource,
    private val firestore: FirebaseFirestore
) : UserRepository {

    override fun observeAll(): Flow<List<User>> =
        userDao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeById(id: String): Flow<User?> =
        userDao.observeById(id).map { it?.toDomain() }

    override fun observeResponsibles(): Flow<List<User>> =
        userDao.observeResponsibles().map { list -> list.map { it.toDomain() } }

    override suspend fun getById(id: String): User? =
        userDao.getById(id)?.toDomain()

    override suspend fun save(user: User) {
        userDao.upsert(UserEntity.fromDomain(user, synced = false))
        runCatching { remoteSource.upsert(user) }
            .onSuccess { userDao.markSynced(user.id) }
    }

    override suspend fun syncFromRemote() {
        runCatching {
            val snapshot = firestore.collection("users").get().await()
            val entities = snapshot.documents
                .mapNotNull { UserDto.fromDocument(it) }
                .map { UserEntity.fromDomain(it.toDomain(), synced = true) }
            userDao.upsertAll(entities)
        }
    }
}
