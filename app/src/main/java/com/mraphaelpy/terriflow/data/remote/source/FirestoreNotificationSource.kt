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
    private fun notificationsCollection(congregationId: String) =
        firestore.collection("congregations").document(congregationId).collection("notifications")

    fun observeAll(congregationId: String): Flow<List<AppNotification>> = callbackFlow {
        val sub = notificationsCollection(congregationId)
            .limit(100)
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
                }?.sortedByDescending { it.createdAt } ?: emptyList()
                trySend(list)
            }
        awaitClose { sub.remove() }
    }

    fun observeByUser(congregationId: String, userId: String): Flow<List<AppNotification>> = callbackFlow {
        val sub = notificationsCollection(congregationId)
            .whereEqualTo("userId", userId)
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
                }?.sortedByDescending { it.createdAt } ?: emptyList()
                trySend(list)
            }
        awaitClose { sub.remove() }
    }

    suspend fun insert(congregationId: String, notification: AppNotification) {
        notificationsCollection(congregationId).document(notification.id).set(
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

    suspend fun getAll(congregationId: String): List<AppNotification> {
        val snapshot = notificationsCollection(congregationId)
            .limit(100)
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
        }.sortedByDescending { it.createdAt }
    }

    suspend fun getByUser(congregationId: String, userId: String): List<AppNotification> {
        val snapshot = notificationsCollection(congregationId)
            .whereEqualTo("userId", userId)
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
        }.sortedByDescending { it.createdAt }
    }
}
