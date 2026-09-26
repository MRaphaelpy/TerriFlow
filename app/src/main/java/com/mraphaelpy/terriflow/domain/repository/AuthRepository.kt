package com.mraphaelpy.terriflow.domain.repository

import com.mraphaelpy.terriflow.domain.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUserId: String?
    val isLoggedIn: Boolean
    fun observeCurrentUser(): Flow<User?>
    suspend fun getCurrentUser(): User?
    suspend fun login(email: String, password: String)
    suspend fun register(name: String, email: String, password: String)
    suspend fun registerWithCongregation(name: String, email: String, password: String, congregationId: String)
    suspend fun signInWithGoogle(idToken: String)
    suspend fun logout()
    suspend fun sendPasswordReset(email: String)
    suspend fun updateFcmToken(token: String)
    suspend fun updatePhotoUrl(photoUrl: String)
    suspend fun resolveAndSaveCongregationId(): String?
}
