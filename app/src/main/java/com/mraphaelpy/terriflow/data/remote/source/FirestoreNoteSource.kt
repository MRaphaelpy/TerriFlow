package com.mraphaelpy.terriflow.data.remote.source

import com.google.android.gms.tasks.Task
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.mraphaelpy.terriflow.domain.model.TerritoryNote
import com.mraphaelpy.terriflow.domain.model.TerritoryNoteKind
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreNoteSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private fun notesCollection(congregationId: String, territoryId: String) =
        firestore.collection("congregations").document(congregationId)
            .collection("territories").document(territoryId)
            .collection("notes")

    fun observe(congregationId: String, territoryId: String): Flow<List<TerritoryNote>> = callbackFlow {
        val subscription = notesCollection(congregationId, territoryId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                trySend(snapshot?.documents?.mapNotNull { it.toNote(territoryId) } ?: emptyList())
            }
        awaitClose { subscription.remove() }
    }

    fun insert(congregationId: String, note: TerritoryNote): Task<Void> =
        notesCollection(congregationId, note.territoryId).document(note.id).set(
            mapOf(
                "kind" to note.kind.name,
                "address" to note.address,
                "text" to note.text,
                "authorId" to note.authorId,
                "authorName" to note.authorName,
                "createdAt" to Timestamp(note.createdAt)
            )
        )

    fun delete(congregationId: String, note: TerritoryNote): Task<Void> =
        notesCollection(congregationId, note.territoryId).document(note.id).delete()

    suspend fun deleteAllByTerritory(congregationId: String, territoryId: String) {
        val snapshot = notesCollection(congregationId, territoryId).get().await()
        if (snapshot.isEmpty) return
        val batch = firestore.batch()
        snapshot.documents.forEach { batch.delete(it.reference) }
        batch.commit().await()
    }

    private fun DocumentSnapshot.toNote(territoryId: String): TerritoryNote? {
        if (!exists()) return null
        return TerritoryNote(
            id = id,
            territoryId = territoryId,
            kind = runCatching { TerritoryNoteKind.valueOf(getString("kind").orEmpty()) }.getOrDefault(TerritoryNoteKind.FIELD_NOTE),
            address = getString("address").orEmpty(),
            text = getString("text").orEmpty(),
            authorId = getString("authorId").orEmpty(),
            authorName = getString("authorName").orEmpty(),
            createdAt = getTimestamp("createdAt")?.toDate() ?: Date()
        )
    }
}
