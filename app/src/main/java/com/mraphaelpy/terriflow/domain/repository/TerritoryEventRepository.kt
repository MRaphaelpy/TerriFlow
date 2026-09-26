package com.mraphaelpy.terriflow.domain.repository

import com.mraphaelpy.terriflow.domain.model.TerritoryEvent
import kotlinx.coroutines.flow.Flow

interface TerritoryEventRepository {
    fun observeAll(): Flow<List<TerritoryEvent>>
    fun observeByTerritory(territoryId: String): Flow<List<TerritoryEvent>>
    fun observeRecent(limit: Int): Flow<List<TerritoryEvent>>
    suspend fun save(event: TerritoryEvent)
    suspend fun syncPendingToRemote()
    suspend fun syncFromRemote(territoryId: String)
}
