package com.mraphaelpy.terriflow.data.remote.source

import com.google.android.gms.tasks.Task
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.mraphaelpy.terriflow.data.remote.dto.NotificationDto
import com.mraphaelpy.terriflow.domain.model.AppNotification
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreNotificationSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private fun notificationsCollection(congregationId: String) =
        firestore.collection("congregations").document(congregationId).collection("notifications")

    private fun recentForUser(congregationId: String, userId: String, limit: Long) =
        notificationsCollection(congregationId)
            .whereEqualTo("userId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit)

    fun observeForUser(congregationId: String, userId: String, limit: Long = 50): Flow<List<AppNotification>> =
        callbackFlow {
            val sub = recentForUser(congregationId, userId, limit)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) { close(error); return@addSnapshotListener }
                    trySend(snapshot?.documents?.mapNotNull(NotificationDto::fromDocument) ?: emptyList())
                }
            awaitClose { sub.remove() }
        }

    fun insert(congregationId: String, notification: AppNotification): Task<Void> =
        notificationsCollection(congregationId).document(notification.id)
            .set(NotificationDto.toMap(notification) + (SERVER_CREATED_AT to FieldValue.serverTimestamp()))

    fun markRead(congregationId: String, id: String): Task<Void> =
        notificationsCollection(congregationId).document(id).update("read", true)

    suspend fun getRecentForUser(congregationId: String, userId: String, limit: Long = 100): List<AppNotification> =
        recentForUser(congregationId, userId, limit)
            .get()
            .await()
            .documents
            .mapNotNull(NotificationDto::fromDocument)

    suspend fun getForUserSince(congregationId: String, userId: String, since: Date): List<AppNotification> =
        notificationsCollection(congregationId)
            .whereEqualTo("userId", userId)
            .whereGreaterThan(SERVER_CREATED_AT, Timestamp(since))
            .get()
            .await()
            .documents
            .mapNotNull(NotificationDto::fromDocument)
}
