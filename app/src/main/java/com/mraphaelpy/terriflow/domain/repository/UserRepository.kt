package com.mraphaelpy.terriflow.domain.repository

import com.mraphaelpy.terriflow.domain.model.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun observeAll(): Flow<List<User>>
    fun observeById(id: String): Flow<User?>
    fun observeResponsibles(): Flow<List<User>>
    suspend fun getById(id: String): User?
    suspend fun save(user: User)
    suspend fun syncFromRemote()
}
