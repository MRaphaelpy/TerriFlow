package com.mraphaelpy.terriflow.domain.repository

import com.mraphaelpy.terriflow.domain.model.Campaign
import kotlinx.coroutines.flow.Flow

interface CampaignRepository {
    fun observeAll(): Flow<List<Campaign>>
    suspend fun save(campaign: Campaign)
    suspend fun delete(campaignId: String)
}
