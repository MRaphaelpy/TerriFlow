package com.mraphaelpy.terriflow.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mraphaelpy.terriflow.data.local.entity.NotificationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {

    @Query("SELECT * FROM notifications ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM notifications WHERE userId = :userId OR userId = '' OR userId = 'ALL' ORDER BY createdAt DESC")
    fun observeByUser(userId: String): Flow<List<NotificationEntity>>

    @Query("SELECT COUNT(*) FROM notifications WHERE read = 0")
    fun countUnread(): Flow<Int>

    @Query("SELECT * FROM notifications WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): NotificationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(notification: NotificationEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(notifications: List<NotificationEntity>)

    @Query("UPDATE notifications SET read = 1 WHERE id = :id")
    suspend fun markRead(id: String)

    @Query("UPDATE notifications SET read = 1")
    suspend fun markAllRead()

    @Query("DELETE FROM notifications WHERE createdAt < :cutoff")
    suspend fun deleteOld(cutoff: Long)

    @Query("DELETE FROM notifications WHERE territoryId = :territoryId")
    suspend fun deleteByTerritory(territoryId: String)
}
