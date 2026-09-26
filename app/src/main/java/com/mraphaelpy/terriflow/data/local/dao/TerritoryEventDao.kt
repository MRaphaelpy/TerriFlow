package com.mraphaelpy.terriflow.data.local.dao

import androidx.room.*
import com.mraphaelpy.terriflow.data.local.entity.TerritoryEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TerritoryEventDao {

    @Query("SELECT * FROM territory_events ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<TerritoryEventEntity>>

    @Query("SELECT * FROM territory_events WHERE territoryId = :territoryId ORDER BY timestamp ASC")
    fun observeByTerritory(territoryId: String): Flow<List<TerritoryEventEntity>>

    @Query("SELECT * FROM territory_events WHERE synced = 0")
    suspend fun getPending(): List<TerritoryEventEntity>

    @Query("SELECT * FROM territory_events ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecent(limit: Int = 20): Flow<List<TerritoryEventEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(event: TerritoryEventEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(events: List<TerritoryEventEntity>)

    @Query("UPDATE territory_events SET synced = 1 WHERE id = :id")
    suspend fun markSynced(id: String)

    @Query("SELECT * FROM territory_events WHERE id = :id")
    suspend fun getById(id: String): TerritoryEventEntity?
}
