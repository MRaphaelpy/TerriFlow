package com.mraphaelpy.terriflow.domain.repository

import com.mraphaelpy.terriflow.domain.model.TerritoryNote
import kotlinx.coroutines.flow.Flow

interface TerritoryNoteRepository {
    fun observeByTerritory(territoryId: String): Flow<List<TerritoryNote>>
    suspend fun add(note: TerritoryNote)
    suspend fun delete(note: TerritoryNote)
}
