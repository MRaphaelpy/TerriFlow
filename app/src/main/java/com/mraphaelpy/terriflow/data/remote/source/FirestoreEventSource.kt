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
    private fun eventsCollection(territoryId: String) =
        firestore.collection("territories").document(territoryId).collection("events")

    fun observeRecentGlobal(limit: Long = 50): Flow<List<TerritoryEventDto>> = callbackFlow {
        val sub = firestore.collectionGroup("events")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(limit)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val list = snapshot?.documents?.mapNotNull { TerritoryEventDto.fromDocument(it) } ?: emptyList()
                trySend(list)
            }
        awaitClose { sub.remove() }
    }

    fun observeByTerritory(territoryId: String): Flow<List<TerritoryEventDto>> = callbackFlow {
        val sub = eventsCollection(territoryId)
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val list = snapshot?.documents
                    ?.mapNotNull { TerritoryEventDto.fromDocument(it) } ?: emptyList()
                trySend(list)
            }
        awaitClose { sub.remove() }
    }

    suspend fun insert(event: TerritoryEvent) {
        val dto = TerritoryEventDto.fromDomain(event)
        eventsCollection(event.territoryId).document(event.id).set(dto.toMap()).await()
    }

    suspend fun getByTerritory(territoryId: String): List<TerritoryEventDto> {
        val snapshot = eventsCollection(territoryId)
            .orderBy("timestamp")
            .get()
            .await()
        return snapshot.documents.mapNotNull { TerritoryEventDto.fromDocument(it) }
    }

    suspend fun exists(territoryId: String, eventId: String): Boolean {
        val doc = eventsCollection(territoryId).document(eventId).get().await()
        return doc.exists()
    }
}
