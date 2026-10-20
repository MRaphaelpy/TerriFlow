package com.mraphaelpy.terriflow.domain.repository

import com.mraphaelpy.terriflow.domain.model.Territory
import com.mraphaelpy.terriflow.domain.model.TerritoryStatus
import kotlinx.coroutines.flow.Flow

interface TerritoryRepository {
    fun observeAll(): Flow<List<Territory>>
    fun observeById(id: String): Flow<Territory?>
    fun observeByStatus(status: TerritoryStatus): Flow<List<Territory>>

    /** Filtros vazios são ignorados. [status] é o nome de um [TerritoryStatus]. */
    fun search(query: String, status: String, responsibleId: String): Flow<List<Territory>>
    fun countByStatus(status: TerritoryStatus): Flow<Int>

    suspend fun getById(id: String): Territory?
    suspend fun getByCode(code: String): Territory?

    /** Salva localmente na hora e envia ao servidor em segundo plano. */
    suspend fun save(territory: Territory): Territory

    /** Próximo código automático livre (`T-NNNNN`). */
    suspend fun getNextCode(): String
    suspend fun syncPendingToRemote()
    suspend fun syncFromRemote()
    suspend fun getAssignedNotStarted(): List<Territory>
    suspend fun getInProgress(): List<Territory>

    /** Remove o território com todo o histórico e notificações. */
    suspend fun delete(id: String)
}
