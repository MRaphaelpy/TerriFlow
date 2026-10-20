package com.mraphaelpy.terriflow.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mraphaelpy.terriflow.data.local.entity.TerritoryEntity
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface TerritoryDao {

    @Query("SELECT * FROM territories WHERE deletedAt IS NULL ORDER BY code ASC")
    fun observeAll(): Flow<List<TerritoryEntity>>

    @Query("SELECT * FROM territories WHERE id = :id")
    fun observeById(id: String): Flow<TerritoryEntity?>

    @Query("SELECT * FROM territories WHERE id = :id")
    suspend fun getById(id: String): TerritoryEntity?

    @Query("SELECT * FROM territories WHERE code = :code AND deletedAt IS NULL LIMIT 1")
    suspend fun getByCode(code: String): TerritoryEntity?

    @Query("SELECT * FROM territories WHERE status = :status AND deletedAt IS NULL ORDER BY code ASC")
    fun observeByStatus(status: String): Flow<List<TerritoryEntity>>

    @Query("""
        SELECT * FROM territories
        WHERE deletedAt IS NULL
        AND (
            (:query = '') OR
            (code LIKE '%' || :query || '%') OR
            (name LIKE '%' || :query || '%') OR
            (location LIKE '%' || :query || '%')
        )
        AND (:status = '' OR status = :status)
        AND (:responsibleId = '' OR currentResponsibleId = :responsibleId)
        ORDER BY code ASC
    """)
    fun search(query: String, status: String, responsibleId: String): Flow<List<TerritoryEntity>>

    @Query("SELECT * FROM territories WHERE synced = 0 AND deletedAt IS NULL")
    suspend fun getPending(): List<TerritoryEntity>

    @Query("""
        SELECT * FROM territories
        WHERE status IN ('ASSIGNED')
        AND assignedAt IS NOT NULL
        AND startedAt IS NULL
        AND deletedAt IS NULL
    """)
    suspend fun getAssignedNotStarted(): List<TerritoryEntity>

    @Query("""
        SELECT * FROM territories
        WHERE status = 'IN_PROGRESS'
        AND startedAt IS NOT NULL
        AND deletedAt IS NULL
    """)
    suspend fun getInProgress(): List<TerritoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(territory: TerritoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(territories: List<TerritoryEntity>)

    @Query("UPDATE territories SET synced = 1, syncVersion = :version WHERE id = :id AND updatedAt = :updatedAt")
    suspend fun markSyncedIfUnchanged(id: String, version: Long, updatedAt: Date)

    @Query("SELECT COUNT(*) FROM territories WHERE status = :status AND deletedAt IS NULL")
    fun countByStatus(status: String): Flow<Int>

    @Query("SELECT code FROM territories WHERE deletedAt IS NULL")
    suspend fun getAllCodes(): List<String>

    @Query("SELECT * FROM territories WHERE currentResponsibleId = :userId AND deletedAt IS NULL")
    suspend fun getByResponsible(userId: String): List<TerritoryEntity>

    @Query("DELETE FROM territories WHERE id = :id")
    suspend fun deletePermanent(id: String)

    @Query("SELECT id FROM territories WHERE synced = 1")
    suspend fun getSyncedIds(): List<String>
}
