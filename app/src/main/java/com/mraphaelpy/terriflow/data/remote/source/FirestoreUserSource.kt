package com.mraphaelpy.terriflow.data.remote.source

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.mraphaelpy.terriflow.data.remote.dto.UserDto
import com.mraphaelpy.terriflow.domain.model.User
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreUserSource @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private val globalUsersCollection = firestore.collection("users")

    private fun congregationUsersCollection(congregationId: String) =
        firestore.collection("congregations").document(congregationId).collection("users")

    suspend fun getAll(congregationId: String): List<UserDto> =
        congregationUsersCollection(congregationId).get().await().documents.mapNotNull(UserDto::fromDocument)

    suspend fun getById(congregationId: String, id: String): UserDto? =
        UserDto.fromDocument(congregationUsersCollection(congregationId).document(id).get().await())

    suspend fun getCongregationIdForUser(uid: String): String? {
        val doc = globalUsersCollection.document(uid).get().await()
        if (!doc.exists()) return null
        return doc.getString("congregationId")
    }

    suspend fun saveGlobalPointer(uid: String, congregationId: String) {
        globalUsersCollection.document(uid).set(
            mapOf("congregationId" to congregationId),
            SetOptions.merge()
        ).await()
    }

    suspend fun upsert(congregationId: String, user: User) {
        congregationUsersCollection(congregationId)
            .document(user.id)
            .set(UserDto.fromDomain(user).toMap(), SetOptions.merge())
            .await()
        if (auth.currentUser?.uid == user.id) {
            runCatching { saveGlobalPointer(user.id, congregationId) }
        }
    }

    suspend fun addFcmToken(congregationId: String, userId: String, token: String) {
        congregationUsersCollection(congregationId).document(userId)
            .update("fcmTokens", FieldValue.arrayUnion(token))
            .await()
    }

    suspend fun removeFcmToken(congregationId: String, userId: String, token: String) {
        congregationUsersCollection(congregationId).document(userId)
            .update("fcmTokens", FieldValue.arrayRemove(token))
            .await()
    }
}
