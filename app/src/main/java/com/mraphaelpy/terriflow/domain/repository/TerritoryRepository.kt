package com.mraphaelpy.terriflow.domain.repository

import com.mraphaelpy.terriflow.domain.model.Territory
import com.mraphaelpy.terriflow.domain.model.User
import kotlinx.coroutines.flow.Flow

interface TerritoryRepository {
    fun observeAll(): Flow<List<Territory>>
    fun observeById(id: String): Flow<Territory?>


    suspend fun getById(id: String): Territory?
    suspend fun getByCode(code: String): Territory?

    suspend fun save(territory: Territory): Territory

    suspend fun getNextCode(): String
    suspend fun syncPendingToRemote()
    suspend fun syncFromRemote()
    suspend fun getAssignedNotStarted(): List<Territory>
    suspend fun getInProgress(): List<Territory>
    suspend fun getByResponsible(userId: String): List<Territory>

    suspend fun delete(id: String)

    suspend fun updateResponsibleProfile(user: User): Int
}
