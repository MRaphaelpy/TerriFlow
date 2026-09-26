package com.mraphaelpy.terriflow.data.remote.source

import com.google.firebase.firestore.FirebaseFirestore
import com.mraphaelpy.terriflow.domain.model.Congregation
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreCongregationSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val congregationsCollection = firestore.collection("congregations")

    suspend fun create(congregation: Congregation) {
        congregationsCollection.document(congregation.id).set(
            mapOf(
                "name" to congregation.name,
                "code" to congregation.code.uppercase(),
                "createdAt" to com.google.firebase.Timestamp(congregation.createdAt)
            )
        ).await()
    }

    suspend fun findByCode(code: String): Congregation? {
        val snapshot = congregationsCollection
            .whereEqualTo("code", code.uppercase())
            .limit(1)
            .get()
            .await()
        val doc = snapshot.documents.firstOrNull() ?: return null
        return Congregation(
            id = doc.id,
            name = doc.getString("name") ?: "",
            code = doc.getString("code") ?: "",
            createdAt = doc.getTimestamp("createdAt")?.toDate() ?: java.util.Date()
        )
    }

    suspend fun getById(id: String): Congregation? {
        val doc = congregationsCollection.document(id).get().await()
        if (!doc.exists()) return null
        return Congregation(
            id = doc.id,
            name = doc.getString("name") ?: "",
            code = doc.getString("code") ?: "",
            createdAt = doc.getTimestamp("createdAt")?.toDate() ?: java.util.Date()
        )
    }

    fun congregationRef(congregationId: String) = congregationsCollection.document(congregationId)
}
