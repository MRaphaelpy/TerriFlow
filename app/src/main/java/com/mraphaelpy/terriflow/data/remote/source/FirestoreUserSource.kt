package com.mraphaelpy.terriflow.data.remote.source

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.mraphaelpy.terriflow.data.remote.dto.UserDto
import com.mraphaelpy.terriflow.domain.model.User
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreUserSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    // Global user doc (stores congregationId so we can look it up after login)
    private val globalUsersCollection = firestore.collection("users")

    // Congregation-scoped user collection
    private fun congregationUsersCollection(congregationId: String) =
        firestore.collection("congregations").document(congregationId).collection("users")

    fun observeAll(congregationId: String): Flow<List<UserDto>> = callbackFlow {
        val sub = congregationUsersCollection(congregationId)
            .whereEqualTo("active", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val list = snapshot?.documents
                    ?.mapNotNull { UserDto.fromDocument(it) } ?: emptyList()
                trySend(list)
            }
        awaitClose { sub.remove() }
    }

    suspend fun getById(congregationId: String, id: String): UserDto? {
        val doc = congregationUsersCollection(congregationId).document(id).get().await()
        return UserDto.fromDocument(doc)
    }

    /** Returns the congregationId stored in the global /users/{uid} doc */
    suspend fun getCongregationIdForUser(uid: String): String? {
        val doc = globalUsersCollection.document(uid).get().await()
        if (!doc.exists()) return null
        return doc.getString("congregationId")
    }

    /** Saves the congregationId pointer in the global /users/{uid} doc */
    suspend fun saveGlobalPointer(uid: String, congregationId: String) {
        globalUsersCollection.document(uid).set(
            mapOf("congregationId" to congregationId),
            SetOptions.merge()
        ).await()
    }

    suspend fun upsert(congregationId: String, user: User) {
        val dto = UserDto.fromDomain(user)
        congregationUsersCollection(congregationId).document(user.id).set(dto.toMap(), SetOptions.merge()).await()
        // Keep global pointer up to date
        saveGlobalPointer(user.id, congregationId)
    }

    suspend fun addFcmToken(congregationId: String, userId: String, token: String) {
        congregationUsersCollection(congregationId).document(userId)
            .update("fcmTokens", com.google.firebase.firestore.FieldValue.arrayUnion(token))
            .await()
    }

    suspend fun removeFcmToken(congregationId: String, userId: String, token: String) {
        congregationUsersCollection(congregationId).document(userId)
            .update("fcmTokens", com.google.firebase.firestore.FieldValue.arrayRemove(token))
            .await()
    }

    suspend fun getFcmTokens(congregationId: String, userId: String): List<String> {
        val dto = getById(congregationId, userId) ?: return emptyList()
        return dto.fcmTokens
    }
}
