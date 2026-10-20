package com.mraphaelpy.terriflow.data.remote.source

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.mraphaelpy.terriflow.domain.model.Congregation
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreCongregationSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val congregationsCollection = firestore.collection("congregations")

    suspend fun create(congregation: Congregation, createdBy: String) {
        congregationsCollection.document(congregation.id).set(
            mapOf(
                "name" to congregation.name,
                "code" to congregation.code.uppercase(),
                "createdAt" to Timestamp(congregation.createdAt),
                // Usado pelas regras para permitir que apenas o criador vire SUPER_ADMIN
                "createdBy" to createdBy
            )
        ).await()
    }

    suspend fun findByCode(code: String): Congregation? {
        val snapshot = congregationsCollection
            .whereEqualTo("code", code.uppercase())
            .limit(1)
            .get()
            .await()
        return snapshot.documents.firstOrNull()?.toCongregation()
    }

    suspend fun getById(id: String): Congregation? {
        val doc = congregationsCollection.document(id).get().await()
        return if (doc.exists()) doc.toCongregation() else null
    }

    private fun DocumentSnapshot.toCongregation() = Congregation(
        id = id,
        name = getString("name") ?: "",
        code = getString("code") ?: "",
        createdAt = getTimestamp("createdAt")?.toDate() ?: Date()
    )
}
