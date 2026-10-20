package com.mraphaelpy.terriflow.data.remote.source

import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.mraphaelpy.terriflow.data.remote.dto.NotificationDto
import com.mraphaelpy.terriflow.domain.model.AppNotification
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

    /** Notificações destinadas ao usuário ou a toda a congregação, mais recentes primeiro. */
    fun observeForUser(congregationId: String, userId: String, limit: Long = 50): Flow<List<AppNotification>> =
        callbackFlow {
            val sub = notificationsCollection(congregationId)
                .whereIn("userId", listOf(userId, AppNotification.BROADCAST))
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) { close(error); return@addSnapshotListener }
                    trySend(snapshot?.documents?.mapNotNull(NotificationDto::fromDocument) ?: emptyList())
                }
            awaitClose { sub.remove() }
        }

    fun insert(congregationId: String, notification: AppNotification): Task<Void> =
        notificationsCollection(congregationId).document(notification.id).set(NotificationDto.toMap(notification))

    suspend fun getRecent(congregationId: String, limit: Long = 100): List<AppNotification> =
        notificationsCollection(congregationId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit)
            .get()
            .await()
            .documents
            .mapNotNull(NotificationDto::fromDocument)
}
