package com.mraphaelpy.terriflow.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.mraphaelpy.terriflow.data.local.dao.TerritoryDao
import com.mraphaelpy.terriflow.data.local.entity.TerritoryEntity
import com.mraphaelpy.terriflow.data.remote.dto.TerritoryDto
import com.mraphaelpy.terriflow.data.remote.source.FirestoreTerritorySource
import com.mraphaelpy.terriflow.domain.model.Territory
import com.mraphaelpy.terriflow.domain.model.TerritoryStatus
import com.mraphaelpy.terriflow.domain.repository.AuthRepository
import com.mraphaelpy.terriflow.domain.repository.CongregationRepository
import com.mraphaelpy.terriflow.domain.repository.TerritoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TerritoryRepositoryImpl @Inject constructor(
    private val territoryDao: TerritoryDao,
    private val remoteSource: FirestoreTerritorySource,
    private val firestore: FirebaseFirestore,
    private val authRepository: AuthRepository,
    private val congregationRepository: CongregationRepository
) : TerritoryRepository {

    override fun observeAll(): Flow<List<Territory>> =
        territoryDao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeById(id: String): Flow<Territory?> =
        territoryDao.observeById(id).map { it?.toDomain() }

    override fun observeByResponsible(userId: String): Flow<List<Territory>> =
        territoryDao.observeByResponsible(userId).map { list -> list.map { it.toDomain() } }

    override fun observeByStatus(status: TerritoryStatus): Flow<List<Territory>> =
        territoryDao.observeByStatus(status.name).map { list -> list.map { it.toDomain() } }

    override fun search(query: String, status: String, responsibleId: String): Flow<List<Territory>> =
        territoryDao.search(query, status, responsibleId).map { list -> list.map { it.toDomain() } }

    override fun countByStatus(status: TerritoryStatus): Flow<Int> =
        territoryDao.countByStatus(status.name)

    override suspend fun getById(id: String): Territory? {
        val local = territoryDao.getById(id)
        if (local != null) return local.toDomain()
        val congregationId = congregationRepository.getCurrentCongregationId() ?: return null
        return runCatching { remoteSource.getById(congregationId, id)?.toDomain() }.getOrNull()
    }

    override suspend fun getByCode(code: String): Territory? {
        val local = territoryDao.getByCode(code)
        if (local != null) return local.toDomain()
        val congregationId = congregationRepository.getCurrentCongregationId() ?: return null
        return runCatching { remoteSource.getByCode(congregationId, code)?.toDomain() }.getOrNull()
    }

    override suspend fun save(territory: Territory): Territory {
        val toSave = if (territory.id.isEmpty()) {
            territory.copy(id = UUID.randomUUID().toString())
        } else territory

        territoryDao.upsert(TerritoryEntity.fromDomain(toSave, synced = false))

        val congregationId = congregationRepository.getCurrentCongregationId()
        if (congregationId != null) {
            runCatching {
                val localVersion = territoryDao.getById(toSave.id)?.syncVersion ?: 0L
                val nextVersion = localVersion + 1
                remoteSource.upsert(congregationId, toSave, nextVersion)
                territoryDao.markSynced(toSave.id, nextVersion)
            }.onFailure {
                Log.e("TerritoryRepositoryImpl", "Error saving to remote", it)
            }
        }

        return toSave
    }

    override suspend fun getNextCode(): String {
        val congregationId = congregationRepository.getCurrentCongregationId()
            ?: return "T-%05d".format(System.currentTimeMillis() % 100000)
        return runCatching { remoteSource.getNextCode(congregationId) }.getOrElse {
            "T-%05d".format(System.currentTimeMillis() % 100000)
        }
    }

    override suspend fun syncPendingToRemote() {
        val congregationId = congregationRepository.getCurrentCongregationId() ?: return
        territoryDao.getPending().forEach { entity ->
            runCatching {
                val nextVersion = entity.syncVersion + 1
                remoteSource.upsert(congregationId, entity.toDomain(), nextVersion)
                territoryDao.markSynced(entity.id, nextVersion)
            }
        }
    }

    override suspend fun syncFromRemote() {
        runCatching {
            authRepository.getCurrentUser() ?: return@runCatching
            val congregationId = congregationRepository.getCurrentCongregationId() ?: return@runCatching

            val remoteDtos = remoteSource.getAll(congregationId)

            remoteDtos.forEach { dto ->
                val local = territoryDao.getById(dto.id)
                when {
                    local == null -> {
                        territoryDao.upsert(
                            TerritoryEntity.fromDomain(dto.toDomain(), synced = true)
                                .copy(syncVersion = dto.syncVersion)
                        )
                    }
                    !local.synced -> return@forEach
                    dto.syncVersion >= local.syncVersion -> {
                        territoryDao.upsert(
                            TerritoryEntity.fromDomain(dto.toDomain(), synced = true)
                                .copy(syncVersion = dto.syncVersion)
                        )
                    }
                    else -> return@forEach
                }
            }
        }.onFailure {
            Log.e("TerritoryRepositoryImpl", "Error syncing from remote", it)
        }
    }

    override suspend fun getAssignedNotStarted(): List<Territory> =
        territoryDao.getAssignedNotStarted().map { it.toDomain() }

    override suspend fun getInProgress(): List<Territory> =
        territoryDao.getInProgress().map { it.toDomain() }

    override suspend fun delete(id: String) {
        territoryDao.softDelete(id, System.currentTimeMillis())
        val congregationId = congregationRepository.getCurrentCongregationId()
        if (congregationId != null) {
            runCatching { remoteSource.delete(congregationId, id) }
        }
    }
}
