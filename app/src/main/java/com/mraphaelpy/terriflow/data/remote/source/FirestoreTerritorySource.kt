package com.mraphaelpy.terriflow.data.remote.source

import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.mraphaelpy.terriflow.data.remote.dto.TerritoryDto
import com.mraphaelpy.terriflow.domain.model.Territory
import com.mraphaelpy.terriflow.domain.model.TerritoryCode
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

data class TerritorySnapshot(
    val territories: List<TerritoryDto>,
    val isFromCache: Boolean
)

@Singleton
class FirestoreTerritorySource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private fun territoriesCollection(congregationId: String) =
        firestore.collection("congregations").document(congregationId).collection("territories")

    fun observeAll(congregationId: String): Flow<TerritorySnapshot> = callbackFlow {
        val sub = territoriesCollection(congregationId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                if (snapshot == null) return@addSnapshotListener
                trySend(
                    TerritorySnapshot(
                        territories = snapshot.documents.mapNotNull(TerritoryDto::fromDocument),
                        isFromCache = snapshot.metadata.isFromCache
                    )
                )
            }
        awaitClose { sub.remove() }
    }

    suspend fun getAll(congregationId: String): List<TerritoryDto> =
        territoriesCollection(congregationId).get().await().documents.mapNotNull(TerritoryDto::fromDocument)

    suspend fun getById(congregationId: String, id: String): TerritoryDto? =
        TerritoryDto.fromDocument(territoriesCollection(congregationId).document(id).get().await())

    suspend fun getByCode(congregationId: String, code: String): TerritoryDto? =
        territoriesCollection(congregationId)
            .whereEqualTo("code", code)
            .limit(1)
            .get()
            .await()
            .documents
            .firstOrNull()
            ?.let(TerritoryDto::fromDocument)

    fun upsert(congregationId: String, territory: Territory, syncVersion: Long): Task<Void> =
        territoriesCollection(congregationId)
            .document(territory.id)
            .set(TerritoryDto.fromDomain(territory, syncVersion).toMap(), SetOptions.merge())

    suspend fun delete(congregationId: String, id: String) {
        territoriesCollection(congregationId).document(id).delete().await()
    }

    suspend fun getLastAutomaticCode(congregationId: String): String? =
        territoriesCollection(congregationId)
            .whereGreaterThanOrEqualTo("code", TerritoryCode.PREFIX + "0")
            .whereLessThanOrEqualTo("code", TerritoryCode.PREFIX + "9")
            .orderBy("code", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .await()
            .documents
            .firstOrNull()
            ?.getString("code")
}
