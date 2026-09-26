package com.mraphaelpy.terriflow.data.remote.source

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.mraphaelpy.terriflow.domain.model.AppNotification
import com.mraphaelpy.terriflow.domain.model.NotificationType
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreNotificationSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val notificationsCollection = firestore.collection("notifications")

    fun observeByUser(userId: String): Flow<List<AppNotification>> = callbackFlow {
        val sub = notificationsCollection
            .whereEqualTo("userId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    if (!doc.exists()) return@mapNotNull null
                    runCatching {
                        AppNotification(
                            id = doc.id,
                            userId = doc.getString("userId") ?: "",
                            title = doc.getString("title") ?: "",
                            body = doc.getString("body") ?: "",
                            type = runCatching { NotificationType.valueOf(doc.getString("type") ?: "") }
                                .getOrDefault(NotificationType.GENERAL),
                            territoryId = doc.getString("territoryId"),
                            territoryCode = doc.getString("territoryCode"),
                            read = doc.getBoolean("read") ?: false,
                            createdAt = doc.getTimestamp("createdAt")?.toDate() ?: java.util.Date()
                        )
                    }.getOrNull()
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { sub.remove() }
    }

    suspend fun insert(notification: AppNotification) {
        notificationsCollection.document(notification.id).set(
            mapOf(
                "userId" to notification.userId,
                "title" to notification.title,
                "body" to notification.body,
                "type" to notification.type.name,
                "territoryId" to notification.territoryId,
                "territoryCode" to notification.territoryCode,
                "read" to notification.read,
                "createdAt" to Timestamp(notification.createdAt)
            )
        ).await()
    }

    suspend fun getByUser(userId: String): List<AppNotification> {
        val snapshot = notificationsCollection
            .whereEqualTo("userId", userId)
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(50)
            .get()
            .await()
        return snapshot.documents.mapNotNull { doc ->
            if (!doc.exists()) return@mapNotNull null
            AppNotification(
                id = doc.id,
                userId = doc.getString("userId") ?: "",
                title = doc.getString("title") ?: "",
                body = doc.getString("body") ?: "",
                type = runCatching { NotificationType.valueOf(doc.getString("type") ?: "") }
                    .getOrDefault(NotificationType.GENERAL),
                territoryId = doc.getString("territoryId"),
                territoryCode = doc.getString("territoryCode"),
                read = doc.getBoolean("read") ?: false,
                createdAt = doc.getTimestamp("createdAt")?.toDate() ?: java.util.Date()
            )
        }
    }
}
