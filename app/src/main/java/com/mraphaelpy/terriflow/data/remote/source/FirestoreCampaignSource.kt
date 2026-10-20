package com.mraphaelpy.terriflow.data.remote.source

import com.google.android.gms.tasks.Task
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.mraphaelpy.terriflow.domain.model.Campaign
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreCampaignSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private fun campaigns(congregationId: String) =
        firestore.collection("congregations").document(congregationId).collection("campaigns")

    fun observe(congregationId: String): Flow<List<Campaign>> = callbackFlow {
        val subscription = campaigns(congregationId)
            .orderBy("start", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                trySend(
                    snapshot?.documents?.mapNotNull { doc ->
                        Campaign(
                            id = doc.id,
                            name = doc.getString("name") ?: return@mapNotNull null,
                            start = doc.getTimestamp("start")?.toDate() ?: return@mapNotNull null,
                            end = doc.getTimestamp("end")?.toDate() ?: return@mapNotNull null,
                            createdBy = doc.getString("createdBy").orEmpty()
                        )
                    } ?: emptyList()
                )
            }
        awaitClose { subscription.remove() }
    }

    fun save(congregationId: String, campaign: Campaign): Task<Void> =
        campaigns(congregationId).document(campaign.id).set(
            mapOf(
                "name" to campaign.name,
                "start" to Timestamp(campaign.start),
                "end" to Timestamp(campaign.end),
                "createdBy" to campaign.createdBy
            )
        )

    fun delete(congregationId: String, campaignId: String): Task<Void> =
        campaigns(congregationId).document(campaignId).delete()
}
