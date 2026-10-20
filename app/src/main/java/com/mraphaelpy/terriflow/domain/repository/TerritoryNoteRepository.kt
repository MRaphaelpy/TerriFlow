package com.mraphaelpy.terriflow.domain.repository

import com.mraphaelpy.terriflow.domain.model.TerritoryNote
import kotlinx.coroutines.flow.Flow

interface TerritoryNoteRepository {
    /** Emite lista vazia quando o usuário não tem acesso às anotações do território. */
    fun observeByTerritory(territoryId: String): Flow<List<TerritoryNote>>
    suspend fun add(note: TerritoryNote)
    suspend fun delete(note: TerritoryNote)
}
