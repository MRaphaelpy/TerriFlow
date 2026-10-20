package com.mraphaelpy.terriflow.data.repository

import android.util.Log
import com.mraphaelpy.terriflow.data.remote.RemoteWriter
import com.mraphaelpy.terriflow.data.remote.source.FirestoreCampaignSource
import com.mraphaelpy.terriflow.domain.model.Campaign
import com.mraphaelpy.terriflow.domain.model.DomainException
import com.mraphaelpy.terriflow.domain.repository.CampaignRepository
import com.mraphaelpy.terriflow.domain.repository.CongregationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CampaignRepositoryImpl @Inject constructor(
    private val remoteSource: FirestoreCampaignSource,
    private val remoteWriter: RemoteWriter,
    private val congregationRepository: CongregationRepository
) : CampaignRepository {

    override fun observeAll(): Flow<List<Campaign>> = flow {
        val congregationId = congregationRepository.getCurrentCongregationId()
        if (congregationId == null) emit(emptyList()) else emitAll(remoteSource.observe(congregationId))
    }.catch {
        Log.w(TAG, "Campanhas indisponíveis", it)
        emit(emptyList())
    }

    override suspend fun save(campaign: Campaign) {
        val congregationId = congregationRepository.getCurrentCongregationId() ?: throw DomainException.NotAuthenticated()
        remoteWriter.write(TAG, { remoteSource.save(congregationId, campaign) })
    }

    override suspend fun delete(campaignId: String) {
        val congregationId = congregationRepository.getCurrentCongregationId() ?: throw DomainException.NotAuthenticated()
        remoteWriter.write(TAG, { remoteSource.delete(congregationId, campaignId) })
    }

    private companion object {
        const val TAG = "CampaignRepository"
    }
}
