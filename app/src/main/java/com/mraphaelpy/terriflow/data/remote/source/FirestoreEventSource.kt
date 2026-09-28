package com.mraphaelpy.terriflow.data.remote.source

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.mraphaelpy.terriflow.data.remote.dto.TerritoryEventDto
import com.mraphaelpy.terriflow.domain.model.TerritoryEvent
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreEventSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private fun eventsCollection(congregationId: String, territoryId: String) =
        firestore.collection("congregations").document(congregationId)
            .collection("territories").document(territoryId)
            .collection("events")

    fun observeRecentGlobal(congregationId: String, limit: Long = 50): Flow<List<TerritoryEventDto>> = callbackFlow {
        val sub = firestore.collection("congregations").document(congregationId)
            .collection("territories")
            .also { } // we use collectionGroup scoped to congregation path
        // Use collectionGroup filtered by path prefix approach:
        // Firestore collectionGroup doesn't support path-prefix filtering directly,
        // so we query the congregation territories sub-collection group
        val subscription = firestore.collectionGroup("events")
            .whereGreaterThanOrEqualTo(
                com.google.firebase.firestore.FieldPath.documentId(),
                "congregations/$congregationId/territories/"
            )
            .orderBy(com.google.firebase.firestore.FieldPath.documentId())
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(limit)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val list = snapshot?.documents?.mapNotNull { TerritoryEventDto.fromDocument(it) } ?: emptyList()
                trySend(list)
            }
        awaitClose { subscription.remove() }
    }

    fun observeByTerritory(congregationId: String, territoryId: String): Flow<List<TerritoryEventDto>> = callbackFlow {
        val sub = eventsCollection(congregationId, territoryId)
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val list = snapshot?.documents
                    ?.mapNotNull { TerritoryEventDto.fromDocument(it) } ?: emptyList()
                trySend(list)
            }
        awaitClose { sub.remove() }
    }

    suspend fun insert(congregationId: String, event: TerritoryEvent) {
        val dto = TerritoryEventDto.fromDomain(event)
        eventsCollection(congregationId, event.territoryId).document(event.id).set(dto.toMap()).await()
    }

    suspend fun getByTerritory(congregationId: String, territoryId: String): List<TerritoryEventDto> {
        val snapshot = eventsCollection(congregationId, territoryId)
            .orderBy("timestamp")
            .get()
            .await()
        return snapshot.documents.mapNotNull { TerritoryEventDto.fromDocument(it) }
    }

    suspend fun exists(congregationId: String, territoryId: String, eventId: String): Boolean {
        val doc = eventsCollection(congregationId, territoryId).document(eventId).get().await()
        return doc.exists()
    }

    suspend fun deleteAllByTerritory(congregationId: String, territoryId: String) {
        val snapshot = eventsCollection(congregationId, territoryId).get().await()
        if (snapshot.isEmpty) return
        snapshot.documents.chunked(400).forEach { chunk ->
            val batch = firestore.batch()
            for (doc in chunk) {
                batch.delete(doc.reference)
            }
            batch.commit().await()
        }
    }
}
