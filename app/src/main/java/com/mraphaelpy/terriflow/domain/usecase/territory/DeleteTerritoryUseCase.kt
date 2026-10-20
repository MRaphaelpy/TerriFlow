package com.mraphaelpy.terriflow.domain.usecase.territory

import com.mraphaelpy.terriflow.domain.model.TerritoryAction
import com.mraphaelpy.terriflow.domain.repository.TerritoryRepository
import javax.inject.Inject

class DeleteTerritoryUseCase @Inject constructor(
    private val guard: TerritoryActionGuard,
    private val territoryRepository: TerritoryRepository
) {
    suspend operator fun invoke(territoryId: String) {
        guard.authorize(territoryId, TerritoryAction.DELETE)
        territoryRepository.delete(territoryId)
    }
}
