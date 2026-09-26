package com.mraphaelpy.terriflow.data.repository

import com.mraphaelpy.terriflow.domain.repository.AuthRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.mraphaelpy.terriflow.data.local.dao.TerritoryDao
import com.mraphaelpy.terriflow.data.local.entity.TerritoryEntity
import com.mraphaelpy.terriflow.data.remote.dto.TerritoryDto
import com.mraphaelpy.terriflow.data.remote.source.FirestoreTerritorySource
import com.mraphaelpy.terriflow.domain.model.Territory
import com.mraphaelpy.terriflow.domain.model.TerritoryStatus
import com.mraphaelpy.terriflow.domain.repository.TerritoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import android.util.Log

@Singleton
class TerritoryRepositoryImpl @Inject constructor(
    private val territoryDao: TerritoryDao,
    private val remoteSource: FirestoreTerritorySource,
    private val firestore: FirebaseFirestore,
    private val authRepository: AuthRepository
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
        
        return runCatching { remoteSource.getById(id)?.toDomain() }.getOrNull()
    }

    override suspend fun getByCode(code: String): Territory? {
        val local = territoryDao.getByCode(code)
        if (local != null) return local.toDomain()
        
        return runCatching { remoteSource.getByCode(code)?.toDomain() }.getOrNull()
    }

    override suspend fun save(territory: Territory): Territory {
        val toSave = if (territory.id.isEmpty()) {
            territory.copy(id = UUID.randomUUID().toString())
        } else territory

        // 1. Salvar local imediatamente (offline-first)
        territoryDao.upsert(TerritoryEntity.fromDomain(toSave, synced = false))

        // 2. Tentar enviar ao Firestore
        runCatching {
            val localVersion = territoryDao.getById(toSave.id)?.syncVersion ?: 0L
            val nextVersion = localVersion + 1
            remoteSource.upsert(toSave, nextVersion)
            territoryDao.markSynced(toSave.id, nextVersion)
        }.onFailure {
            Log.e("TerritoryRepositoryImpl", "Error saving to remote", it)
        }

        return toSave
    }

    override suspend fun getNextCode(): String =
        runCatching { remoteSource.getNextCode() }.getOrElse {
            "T-%05d".format(System.currentTimeMillis() % 100000)
        }

    override suspend fun syncPendingToRemote() {
        territoryDao.getPending().forEach { entity ->
            runCatching {
                val nextVersion = entity.syncVersion + 1
                remoteSource.upsert(entity.toDomain(), nextVersion)
                territoryDao.markSynced(entity.id, nextVersion)
            }
        }
    }

    override suspend fun syncFromRemote() {
        runCatching {
            authRepository.getCurrentUser() ?: return@runCatching

            val query = firestore.collection("territories")

            val snapshot = query.get().await()
            val remoteDtos = snapshot.documents.mapNotNull { TerritoryDto.fromDocument(it) }

            remoteDtos.forEach { dto ->
                val local = territoryDao.getById(dto.id)
                when {
                    // Não existe localmente → aceitar remoto
                    local == null -> {
                        territoryDao.upsert(
                            TerritoryEntity.fromDomain(dto.toDomain(), synced = true)
                                .copy(syncVersion = dto.syncVersion)
                        )
                    }
                    // Local tem alterações pendentes (não sincronizadas) → local vence
                    // O SyncWorker vai enviar o local para o remoto em seguida
                    !local.synced -> return@forEach
                    // Local sincronizado: aceitar remoto se versão for >= local
                    dto.syncVersion >= local.syncVersion -> {
                        territoryDao.upsert(
                            TerritoryEntity.fromDomain(dto.toDomain(), synced = true)
                                .copy(syncVersion = dto.syncVersion)
                        )
                    }
                    // Local tem versão mais nova que remoto → não sobrescrever
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
        runCatching {
            firestore.collection("territories").document(id).delete().await()
        }
    }
}
