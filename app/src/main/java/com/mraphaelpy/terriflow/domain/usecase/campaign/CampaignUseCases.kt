package com.mraphaelpy.terriflow.domain.usecase.campaign

import com.mraphaelpy.terriflow.domain.model.Campaign
import com.mraphaelpy.terriflow.domain.model.DomainException
import com.mraphaelpy.terriflow.domain.model.TerritoryAction
import com.mraphaelpy.terriflow.domain.repository.CampaignRepository
import com.mraphaelpy.terriflow.domain.usecase.territory.TerritoryActionGuard
import java.util.Date
import javax.inject.Inject

class CreateCampaignUseCase @Inject constructor(
    private val guard: TerritoryActionGuard,
    private val campaignRepository: CampaignRepository
) {
    suspend operator fun invoke(name: String, start: Date, end: Date) {
        val actor = guard.requireCurrentUser()
        if (!actor.isAdmin) throw DomainException.ActionNotAllowed(TerritoryAction.CREATE)
        if (name.isBlank() || end.before(start)) throw DomainException.InvalidCampaign()
        campaignRepository.save(Campaign(name = name.trim(), start = start, end = end, createdBy = actor.id))
    }
}

class DeleteCampaignUseCase @Inject constructor(
    private val guard: TerritoryActionGuard,
    private val campaignRepository: CampaignRepository
) {
    suspend operator fun invoke(campaignId: String) {
        val actor = guard.requireCurrentUser()
        if (!actor.isAdmin) throw DomainException.ActionNotAllowed(TerritoryAction.DELETE)
        campaignRepository.delete(campaignId)
    }
}
