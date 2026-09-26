package com.mraphaelpy.terriflow.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import com.mraphaelpy.terriflow.data.local.dao.UserDao
import com.mraphaelpy.terriflow.data.local.entity.UserEntity
import com.mraphaelpy.terriflow.data.remote.source.FirestoreUserSource
import com.mraphaelpy.terriflow.domain.model.User
import com.mraphaelpy.terriflow.domain.model.UserRole
import com.mraphaelpy.terriflow.domain.repository.AuthRepository
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
    private val remoteSource: FirestoreUserSource
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
        return userDao.getById(uid)?.toDomain()
            ?: remoteSource.getById(uid)?.toDomain()
    }
    override suspend fun login(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password).await()
        val uid = auth.currentUser?.uid ?: return
        val remoteUser = remoteSource.getById(uid)
        if (remoteUser != null) {
            userDao.upsert(com.mraphaelpy.terriflow.data.local.entity.UserEntity.fromDomain(remoteUser.toDomain(), synced = true))
        }
    }

    override suspend fun signInWithGoogle(idToken: String) {
        val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)
        val result = auth.signInWithCredential(credential).await()
        val uid = result.user?.uid ?: return
        val remoteUser = remoteSource.getById(uid)
        if (remoteUser != null) {
            userDao.upsert(com.mraphaelpy.terriflow.data.local.entity.UserEntity.fromDomain(remoteUser.toDomain(), synced = true))
        } else {
            val user = com.mraphaelpy.terriflow.domain.model.User(
                id = uid,
                name = result.user?.displayName ?: "Usuário Google",
                email = result.user?.email ?: "",
                role = com.mraphaelpy.terriflow.domain.model.UserRole.RESPONSIBLE,
                createdAt = java.util.Date(),
                active = true,
                photoUrl = result.user?.photoUrl?.toString()
            )
            remoteSource.upsert(user)
            userDao.upsert(com.mraphaelpy.terriflow.data.local.entity.UserEntity.fromDomain(user, synced = true))
        }
    }

    override suspend fun register(name: String, email: String, password: String) {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val uid = result.user?.uid ?: return
        val user = com.mraphaelpy.terriflow.domain.model.User(
            id = uid,
            name = name,
            email = email,
            role = com.mraphaelpy.terriflow.domain.model.UserRole.RESPONSIBLE,
            createdAt = java.util.Date(),
            active = true
        )
        remoteSource.upsert(user)
        userDao.upsert(com.mraphaelpy.terriflow.data.local.entity.UserEntity.fromDomain(user, synced = true))
    }
    override suspend fun logout() {
        val uid = currentUserId
        if (uid != null) {
            runCatching {
                val token = FirebaseMessaging.getInstance().token.await()
                remoteSource.removeFcmToken(uid, token)
            }
        }
        auth.signOut()
    }

    override suspend fun sendPasswordReset(email: String) {
        auth.sendPasswordResetEmail(email).await()
    }

    override suspend fun updateFcmToken(token: String) {
        val uid = currentUserId ?: return
        remoteSource.addFcmToken(uid, token)
    }

    override suspend fun updatePhotoUrl(photoUrl: String) {
        val uid = currentUserId ?: return
        val user = getCurrentUser() ?: return
        val updatedUser = user.copy(photoUrl = photoUrl)
        remoteSource.upsert(updatedUser)
        userDao.upsert(com.mraphaelpy.terriflow.data.local.entity.UserEntity.fromDomain(updatedUser, synced = true))
    }
}
