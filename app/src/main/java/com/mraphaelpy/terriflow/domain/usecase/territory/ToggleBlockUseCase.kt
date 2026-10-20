package com.mraphaelpy.terriflow.domain.usecase.territory

import com.mraphaelpy.terriflow.domain.model.TerritoryAction
import com.mraphaelpy.terriflow.domain.repository.TerritoryRepository
import java.util.Date
import javax.inject.Inject

/** Marca ou desmarca uma quadra como concluída no ciclo atual do território. */
class ToggleBlockUseCase @Inject constructor(
    private val guard: TerritoryActionGuard,
    private val territoryRepository: TerritoryRepository
) {
    suspend operator fun invoke(territoryId: String, blockIndex: Int) {
        val (_, territory) = guard.authorize(territoryId, TerritoryAction.MARK_BLOCK)
        if (blockIndex !in territory.blockPolygons.indices) return
        val blocks = territory.completedBlocks.let { if (blockIndex in it) it - blockIndex else it + blockIndex }
        territoryRepository.save(territory.copy(completedBlocks = blocks, updatedAt = Date()))
    }
}
