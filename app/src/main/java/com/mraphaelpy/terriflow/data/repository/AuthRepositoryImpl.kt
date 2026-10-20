package com.mraphaelpy.terriflow.data.repository

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.messaging.FirebaseMessaging
import com.mraphaelpy.terriflow.data.local.AppDatabase
import com.mraphaelpy.terriflow.data.local.dao.UserDao
import com.mraphaelpy.terriflow.data.local.entity.UserEntity
import com.mraphaelpy.terriflow.data.remote.source.FirestoreUserSource
import com.mraphaelpy.terriflow.domain.model.AuthException
import com.mraphaelpy.terriflow.domain.model.User
import com.mraphaelpy.terriflow.domain.model.UserRole
import com.mraphaelpy.terriflow.domain.repository.AuthRepository
import com.mraphaelpy.terriflow.domain.repository.CongregationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val messaging: FirebaseMessaging,
    private val userDao: UserDao,
    private val remoteSource: FirestoreUserSource,
    private val congregationRepository: CongregationRepository,
    private val appDatabase: AppDatabase
) : AuthRepository {

    override val currentUserId: String?
        get() = auth.currentUser?.uid

    override val isLoggedIn: Boolean
        get() = auth.currentUser != null

    override fun observeCurrentUserId(): Flow<String?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser?.uid) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }.distinctUntilChanged()

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeCurrentUser(): Flow<User?> =
        observeCurrentUserId().flatMapLatest { uid ->
            if (uid == null) flowOf(null) else userDao.observeById(uid).map { it?.toDomain() }
        }

    override suspend fun getCurrentUser(): User? {
        val uid = currentUserId ?: return null
        userDao.getById(uid)?.let { return it.toDomain() }
        val congregationId = congregationRepository.getCurrentCongregationId() ?: return null
        return runCatching { remoteSource.getById(congregationId, uid)?.toDomain() }.getOrNull()
    }

    override suspend fun login(email: String, password: String) {
        mapAuthErrors { auth.signInWithEmailAndPassword(email, password).await() }
        cacheRemoteProfile()
    }

    override suspend fun signInWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val firebaseUser = mapAuthErrors { auth.signInWithCredential(credential).await() }.user ?: return
        if (cacheRemoteProfile()) return

        saveLocalProfile(
            User(
                id = firebaseUser.uid,
                name = firebaseUser.displayName ?: "Usuário Google",
                email = firebaseUser.email ?: "",
                photoUrl = firebaseUser.photoUrl?.toString()
            )
        )
    }

    override suspend fun register(name: String, email: String, password: String) {
        val result = mapAuthErrors { auth.createUserWithEmailAndPassword(email, password).await() }
        val uid = result.user?.uid ?: return
        runCatching {
            val request = UserProfileChangeRequest.Builder().setDisplayName(name).build()
            result.user?.updateProfile(request)?.await()
        }
        saveLocalProfile(User(id = uid, name = name, email = email))
    }

    override suspend fun logout() {
        currentUserId?.let { uid ->
            runCatching {
                val token = messaging.token.await()
                val congregationId = congregationRepository.getCurrentCongregationId()
                if (congregationId != null) remoteSource.removeFcmToken(congregationId, uid, token)
            }
        }
        congregationRepository.clearCongregationId()
        runCatching { withContext(Dispatchers.IO) { appDatabase.clearAllTables() } }
        auth.signOut()
    }

    override suspend fun sendPasswordReset(email: String) {
        mapAuthErrors { auth.sendPasswordResetEmail(email).await() }
    }

    override suspend fun registerDeviceToken() {
        if (!isLoggedIn) return
        updateFcmToken(messaging.token.await())
    }

    override suspend fun updateFcmToken(token: String) {
        val uid = currentUserId ?: return
        val congregationId = congregationRepository.getCurrentCongregationId() ?: return
        remoteSource.addFcmToken(congregationId, uid, token)
    }

    override suspend fun updatePhotoUrl(photoUrl: String) {
        updateProfile { it.copy(photoUrl = photoUrl) }
    }

    override suspend fun updateName(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        runCatching {
            val request = UserProfileChangeRequest.Builder().setDisplayName(trimmed).build()
            auth.currentUser?.updateProfile(request)?.await()
        }
        updateProfile { it.copy(name = trimmed) }
    }

    override suspend fun resolveAndSaveCongregationId(): String? =
        if (isLoggedIn) congregationRepository.getCurrentCongregationId() else null

    override suspend fun ensureCongregationProfile(congregationId: String, roleIfNew: UserRole): User {
        val firebaseUser = auth.currentUser ?: throw AuthException.UserNotFound()
        val existing = remoteSource.getById(congregationId, firebaseUser.uid)?.toDomain()
        val profile = existing?.copy(congregationId = congregationId)
            ?: (userDao.getById(firebaseUser.uid)?.toDomain() ?: User(
                id = firebaseUser.uid,
                name = firebaseUser.displayName.orEmpty().ifBlank { firebaseUser.email.orEmpty() },
                email = firebaseUser.email.orEmpty(),
                photoUrl = firebaseUser.photoUrl?.toString()
            )).copy(
                role = roleIfNew,
                active = true,
                createdAt = Date(),
                congregationId = congregationId
            )

        remoteSource.upsert(congregationId, profile)
        saveLocalProfile(profile)
        return profile
    }

    private suspend fun cacheRemoteProfile(): Boolean {
        val uid = currentUserId ?: return false
        val congregationId = resolveAndSaveCongregationId() ?: return false
        val remoteUser = remoteSource.getById(congregationId, uid) ?: return false
        saveLocalProfile(remoteUser.toDomain())
        return true
    }

    private suspend fun updateProfile(transform: (User) -> User): User? {
        val user = getCurrentUser() ?: return null
        val congregationId = congregationRepository.getCurrentCongregationId() ?: return null
        val updated = transform(user)
        remoteSource.upsert(congregationId, updated)
        saveLocalProfile(updated)
        return updated
    }

    private suspend fun saveLocalProfile(user: User) {
        userDao.upsert(UserEntity.fromDomain(user, synced = true))
    }

    private inline fun <T> mapAuthErrors(block: () -> T): T = try {
        block()
    } catch (e: FirebaseAuthWeakPasswordException) {
        throw AuthException.WeakPassword(e)
    } catch (e: FirebaseAuthUserCollisionException) {
        throw AuthException.EmailAlreadyInUse(e)
    } catch (e: FirebaseAuthInvalidUserException) {
        throw AuthException.UserNotFound(e)
    } catch (e: FirebaseAuthInvalidCredentialsException) {
        if (e.errorCode == "ERROR_INVALID_EMAIL") throw AuthException.InvalidEmail(e)
        throw AuthException.InvalidCredentials(e)
    } catch (e: FirebaseNetworkException) {
        throw AuthException.Network(e)
    }
}
