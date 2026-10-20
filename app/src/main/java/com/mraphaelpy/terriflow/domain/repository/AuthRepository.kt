package com.mraphaelpy.terriflow.domain.repository

import com.mraphaelpy.terriflow.domain.model.User
import com.mraphaelpy.terriflow.domain.model.UserRole
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUserId: String?
    val isLoggedIn: Boolean

    fun observeCurrentUserId(): Flow<String?>

    fun observeCurrentUser(): Flow<User?>
    suspend fun getCurrentUser(): User?

    suspend fun login(email: String, password: String)
    suspend fun register(name: String, email: String, password: String)
    suspend fun signInWithGoogle(idToken: String)
    suspend fun logout()
    suspend fun sendPasswordReset(email: String)

    suspend fun registerDeviceToken()
    suspend fun updateFcmToken(token: String)

    suspend fun updatePhotoUrl(photoUrl: String)
    suspend fun updateName(name: String)

    suspend fun resolveAndSaveCongregationId(): String?

    suspend fun ensureCongregationProfile(congregationId: String, roleIfNew: UserRole): User
}
