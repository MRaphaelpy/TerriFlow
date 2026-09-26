package com.mraphaelpy.terriflow.data.remote.source

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.mraphaelpy.terriflow.data.remote.dto.TerritoryDto
import com.mraphaelpy.terriflow.domain.model.Territory
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreTerritorySource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private fun territoriesCollection(congregationId: String) =
        firestore.collection("congregations").document(congregationId).collection("territories")

    fun observeAll(congregationId: String): Flow<List<TerritoryDto>> = callbackFlow {
        val sub = territoriesCollection(congregationId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val list = snapshot?.documents
                    ?.mapNotNull { TerritoryDto.fromDocument(it) } ?: emptyList()
                trySend(list)
            }
        awaitClose { sub.remove() }
    }

    fun observeByResponsible(congregationId: String, responsibleId: String): Flow<List<TerritoryDto>> = callbackFlow {
        val sub = territoriesCollection(congregationId)
            .whereEqualTo("currentResponsibleId", responsibleId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val list = snapshot?.documents
                    ?.mapNotNull { TerritoryDto.fromDocument(it) } ?: emptyList()
                trySend(list)
            }
        awaitClose { sub.remove() }
    }

    suspend fun getById(congregationId: String, id: String): TerritoryDto? {
        val doc = territoriesCollection(congregationId).document(id).get().await()
        return TerritoryDto.fromDocument(doc)
    }

    suspend fun getByCode(congregationId: String, code: String): TerritoryDto? {
        val snapshot = territoriesCollection(congregationId)
            .whereEqualTo("code", code)
            .limit(1)
            .get()
            .await()
        return snapshot.documents.firstOrNull()?.let { TerritoryDto.fromDocument(it) }
    }

    suspend fun upsert(congregationId: String, territory: Territory, syncVersion: Long = 0L) {
        val dto = TerritoryDto.fromDomain(territory, syncVersion)
        territoriesCollection(congregationId).document(territory.id).set(dto.toMap(), SetOptions.merge()).await()
    }

    suspend fun delete(congregationId: String, id: String) {
        territoriesCollection(congregationId).document(id).delete().await()
    }

    suspend fun getNextCode(congregationId: String): String {
        val snapshot = territoriesCollection(congregationId)
            .orderBy("code", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .await()
        val lastCode = snapshot.documents.firstOrNull()?.getString("code") ?: "T-00000"
        val number = lastCode.removePrefix("T-").toIntOrNull() ?: 0
        return "T-%05d".format(number + 1)
    }

    suspend fun getAllSince(congregationId: String, timestamp: com.google.firebase.Timestamp): List<TerritoryDto> {
        val snapshot = territoriesCollection(congregationId)
            .whereGreaterThan("updatedAt", timestamp)
            .get()
            .await()
        return snapshot.documents.mapNotNull { TerritoryDto.fromDocument(it) }
    }

    suspend fun getAll(congregationId: String): List<TerritoryDto> {
        val snapshot = territoriesCollection(congregationId).get().await()
        return snapshot.documents.mapNotNull { TerritoryDto.fromDocument(it) }
    }
}
