package com.mraphaelpy.terriflow.domain.repository

import com.mraphaelpy.terriflow.domain.model.Congregation

interface CongregationRepository {
    suspend fun create(name: String, adminUserId: String): Congregation
    suspend fun joinByCode(code: String, userId: String): Congregation
    suspend fun getById(id: String): Congregation?
    suspend fun getCurrentCongregationId(): String?
    suspend fun saveCongregationIdLocally(id: String)
    suspend fun clearCongregationId()
}
