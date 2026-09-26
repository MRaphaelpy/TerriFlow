package com.mraphaelpy.terriflow.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import com.mraphaelpy.terriflow.data.local.dao.UserDao
import com.mraphaelpy.terriflow.data.local.entity.UserEntity
import com.mraphaelpy.terriflow.data.remote.source.FirestoreUserSource
import com.mraphaelpy.terriflow.domain.model.User
import com.mraphaelpy.terriflow.domain.model.UserRole
import com.mraphaelpy.terriflow.domain.repository.AuthRepository
import com.mraphaelpy.terriflow.domain.repository.CongregationRepository
import com.mraphaelpy.terriflow.data.local.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val userDao: UserDao,
    private val remoteSource: FirestoreUserSource,
    private val congregationRepository: CongregationRepository,
    private val appDatabase: AppDatabase
) : AuthRepository {

    override val currentUserId: String?
        get() = auth.currentUser?.uid

    override val isLoggedIn: Boolean
        get() = auth.currentUser != null

    override fun observeCurrentUser(): Flow<User?> {
        val uid = currentUserId ?: return kotlinx.coroutines.flow.flowOf(null)
        return userDao.observeById(uid).map { it?.toDomain() }
    }

    override suspend fun getCurrentUser(): User? {
        val uid = currentUserId ?: return null
        val congregationId = congregationRepository.getCurrentCongregationId() ?: return null
        return userDao.getById(uid)?.toDomain()
            ?: remoteSource.getById(congregationId, uid)?.toDomain()
    }

    override suspend fun login(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password).await()
        val uid = auth.currentUser?.uid ?: return
        // Resolve congregation
        val congregationId = resolveAndSaveCongregationId() ?: return
        val remoteUser = remoteSource.getById(congregationId, uid)
        if (remoteUser != null) {
            userDao.upsert(UserEntity.fromDomain(remoteUser.toDomain(), synced = true))
        }
    }

    override suspend fun signInWithGoogle(idToken: String) {
        val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)
        val result = auth.signInWithCredential(credential).await()
        val uid = result.user?.uid ?: return
        val congregationId = resolveAndSaveCongregationId()
        if (congregationId != null) {
            val remoteUser = remoteSource.getById(congregationId, uid)
            if (remoteUser != null) {
                userDao.upsert(UserEntity.fromDomain(remoteUser.toDomain(), synced = true))
                return
            }
        }
        // New Google user without congregation — will be directed to join/create flow
        val user = User(
            id = uid,
            name = result.user?.displayName ?: "Usuário Google",
            email = result.user?.email ?: "",
            role = UserRole.RESPONSIBLE,
            createdAt = Date(),
            active = true,
            photoUrl = result.user?.photoUrl?.toString()
        )
        userDao.upsert(UserEntity.fromDomain(user, synced = true))
    }

    override suspend fun register(name: String, email: String, password: String) {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val uid = result.user?.uid ?: return
        val user = User(
            id = uid,
            name = name,
            email = email,
            role = UserRole.RESPONSIBLE,
            createdAt = Date(),
            active = true
        )
        userDao.upsert(UserEntity.fromDomain(user, synced = true))
    }

    override suspend fun registerWithCongregation(name: String, email: String, password: String, congregationId: String) {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val uid = result.user?.uid ?: return
        val user = User(
            id = uid,
            name = name,
            email = email,
            role = UserRole.RESPONSIBLE,
            createdAt = Date(),
            active = true,
            congregationId = congregationId
        )
        remoteSource.upsert(congregationId, user)
        userDao.upsert(UserEntity.fromDomain(user, synced = true))
        congregationRepository.saveCongregationIdLocally(congregationId)
    }

    override suspend fun logout() {
        val uid = currentUserId
        if (uid != null) {
            runCatching {
                val token = FirebaseMessaging.getInstance().token.await()
                val congregationId = congregationRepository.getCurrentCongregationId()
                if (congregationId != null) {
                    remoteSource.removeFcmToken(congregationId, uid, token)
                }
            }
        }
        congregationRepository.clearCongregationId()
        runCatching {
            withContext(Dispatchers.IO) {
                appDatabase.clearAllTables()
            }
        }
        auth.signOut()
    }

    override suspend fun sendPasswordReset(email: String) {
        auth.sendPasswordResetEmail(email).await()
    }

    override suspend fun updateFcmToken(token: String) {
        val uid = currentUserId ?: return
        val congregationId = congregationRepository.getCurrentCongregationId() ?: return
        remoteSource.addFcmToken(congregationId, uid, token)
    }

    override suspend fun updatePhotoUrl(photoUrl: String) {
        val uid = currentUserId ?: return
        val user = getCurrentUser() ?: return
        val congregationId = congregationRepository.getCurrentCongregationId() ?: return
        val updatedUser = user.copy(photoUrl = photoUrl)
        remoteSource.upsert(congregationId, updatedUser)
        userDao.upsert(UserEntity.fromDomain(updatedUser, synced = true))
    }

    override suspend fun resolveAndSaveCongregationId(): String? {
        val uid = currentUserId ?: return null
        // Check local cache first
        val cached = congregationRepository.getCurrentCongregationId()
        if (!cached.isNullOrEmpty()) return cached
        // Fetch from global users pointer
        val fromRemote = remoteSource.getCongregationIdForUser(uid)
        if (!fromRemote.isNullOrEmpty()) {
            congregationRepository.saveCongregationIdLocally(fromRemote)
            return fromRemote
        }
        return null
    }
}
