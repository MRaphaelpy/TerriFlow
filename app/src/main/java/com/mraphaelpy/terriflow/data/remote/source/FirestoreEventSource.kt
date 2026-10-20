package com.mraphaelpy.terriflow.data.remote.source

import com.google.android.gms.tasks.Task
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.mraphaelpy.terriflow.data.remote.dto.TerritoryEventDto
import com.mraphaelpy.terriflow.domain.model.TerritoryEvent
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

const val SERVER_CREATED_AT = "serverCreatedAt"

@Singleton
class FirestoreEventSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private fun eventsCollection(congregationId: String, territoryId: String) =
        firestore.collection("congregations").document(congregationId)
            .collection("territories").document(territoryId)
            .collection("events")

    fun observeRecent(congregationId: String, limit: Long = 50): Flow<List<TerritoryEventDto>> = callbackFlow {
        val subscription = firestore.collectionGroup("events")
            .whereEqualTo("congregationId", congregationId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(limit)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                trySend(snapshot?.documents?.mapNotNull(TerritoryEventDto::fromDocument) ?: emptyList())
            }
        awaitClose { subscription.remove() }
    }

    fun insert(congregationId: String, event: TerritoryEvent): Task<Void> =
        eventsCollection(congregationId, event.territoryId)
            .document(event.id)
            .set(TerritoryEventDto.fromDomain(event).toMap() + (SERVER_CREATED_AT to FieldValue.serverTimestamp()))

    suspend fun getChangedSince(congregationId: String, since: Date): List<TerritoryEventDto> =
        firestore.collectionGroup("events")
            .whereEqualTo("congregationId", congregationId)
            .whereGreaterThan(SERVER_CREATED_AT, Timestamp(since))
            .get()
            .await()
            .documents
            .mapNotNull(TerritoryEventDto::fromDocument)

    suspend fun getByTerritory(congregationId: String, territoryId: String): List<TerritoryEventDto> =
        eventsCollection(congregationId, territoryId)
            .orderBy("timestamp")
            .get()
            .await()
            .documents
            .mapNotNull(TerritoryEventDto::fromDocument)

    suspend fun exists(congregationId: String, territoryId: String, eventId: String): Boolean =
        eventsCollection(congregationId, territoryId).document(eventId).get().await().exists()

    suspend fun deleteAllByTerritory(congregationId: String, territoryId: String) {
        val snapshot = eventsCollection(congregationId, territoryId).get().await()
        snapshot.documents.chunked(MAX_BATCH_SIZE).forEach { chunk ->
            val batch = firestore.batch()
            chunk.forEach { batch.delete(it.reference) }
            batch.commit().await()
        }
    }

    private companion object {
        const val MAX_BATCH_SIZE = 400
    }
}
