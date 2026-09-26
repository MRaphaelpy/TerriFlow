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
    private val usersCollection = firestore.collection("users")

    fun observeAll(): Flow<List<UserDto>> = callbackFlow {
        val sub = usersCollection
            .whereEqualTo("active", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val list = snapshot?.documents
                    ?.mapNotNull { UserDto.fromDocument(it) } ?: emptyList()
                trySend(list)
            }
        awaitClose { sub.remove() }
    }

    suspend fun getById(id: String): UserDto? {
        val doc = usersCollection.document(id).get().await()
        return UserDto.fromDocument(doc)
    }

    suspend fun upsert(user: User) {
        val dto = UserDto.fromDomain(user)
        usersCollection.document(user.id).set(dto.toMap(), SetOptions.merge()).await()
    }

    suspend fun addFcmToken(userId: String, token: String) {
        usersCollection.document(userId)
            .update("fcmTokens", com.google.firebase.firestore.FieldValue.arrayUnion(token))
            .await()
    }

    suspend fun removeFcmToken(userId: String, token: String) {
        usersCollection.document(userId)
            .update("fcmTokens", com.google.firebase.firestore.FieldValue.arrayRemove(token))
            .await()
    }

    suspend fun getFcmTokens(userId: String): List<String> {
        val dto = getById(userId) ?: return emptyList()
        return dto.fcmTokens
    }
}
