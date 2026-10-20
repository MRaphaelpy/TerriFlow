package com.mraphaelpy.terriflow.data.repository

import android.util.Log
import com.mraphaelpy.terriflow.data.local.dao.TerritoryDao
import com.mraphaelpy.terriflow.data.local.entity.TerritoryEntity
import com.mraphaelpy.terriflow.data.remote.RemoteWriter
import com.mraphaelpy.terriflow.data.remote.source.FirestoreEventSource
import com.mraphaelpy.terriflow.data.remote.source.FirestoreNoteSource
import com.mraphaelpy.terriflow.data.remote.source.FirestoreTerritorySource
import com.mraphaelpy.terriflow.data.sync.TerritoryRemoteMerger
import com.mraphaelpy.terriflow.di.ApplicationScope
import com.mraphaelpy.terriflow.domain.model.Territory
import com.mraphaelpy.terriflow.domain.model.TerritoryCode
import com.mraphaelpy.terriflow.domain.model.User
import com.mraphaelpy.terriflow.domain.repository.CongregationRepository
import com.mraphaelpy.terriflow.domain.repository.TerritoryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TerritoryRepositoryImpl @Inject constructor(
    private val territoryDao: TerritoryDao,
    private val remoteSource: FirestoreTerritorySource,
    private val remoteEventSource: FirestoreEventSource,
    private val remoteNoteSource: FirestoreNoteSource,
    private val remoteWriter: RemoteWriter,
    private val merger: TerritoryRemoteMerger,
    private val congregationRepository: CongregationRepository,
    @ApplicationScope private val appScope: CoroutineScope
) : TerritoryRepository {

    override fun observeAll(): Flow<List<Territory>> =
        territoryDao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeById(id: String): Flow<Territory?> =
        territoryDao.observeById(id).map { it?.toDomain() }

    override suspend fun getById(id: String): Territory? {
        territoryDao.getById(id)?.let { return it.toDomain() }
        val congregationId = congregationRepository.getCurrentCongregationId() ?: return null
        return runCatching { remoteSource.getById(congregationId, id)?.toDomain() }.getOrNull()
    }

    override suspend fun save(territory: Territory): Territory {
        val toSave = if (territory.id.isEmpty()) territory.copy(id = UUID.randomUUID().toString()) else territory
        val currentVersion = territoryDao.getById(toSave.id)?.syncVersion ?: 0L
        territoryDao.upsert(TerritoryEntity.fromDomain(toSave, synced = false).copy(syncVersion = currentVersion))
        congregationRepository.getCurrentCongregationId()?.let { push(it, toSave, currentVersion) }
        return toSave
    }

    override suspend fun getNextCode(): String {
        val remoteLast = congregationRepository.getCurrentCongregationId()?.let { congregationId ->
            runCatching { remoteSource.getLastAutomaticCode(congregationId) }
                .onFailure { Log.w(TAG, "Sem acesso ao servidor; usando apenas códigos locais", it) }
                .getOrNull()
        }
        return TerritoryCode.nextAfter(territoryDao.getAllCodes() + listOfNotNull(remoteLast))
    }

    override suspend fun syncPendingToRemote() {
        val congregationId = congregationRepository.getCurrentCongregationId() ?: return
        territoryDao.getPending().forEach { entity ->
            push(congregationId, entity.toDomain(), entity.syncVersion)
        }
    }

    override suspend fun syncFromRemote() {
        val congregationId = congregationRepository.getCurrentCongregationId() ?: return
        runCatching { merger.merge(remoteSource.getAll(congregationId), removeMissing = true) }
            .onFailure { Log.e(TAG, "Erro ao sincronizar territórios", it) }
    }

    override suspend fun getAssignedNotStarted(): List<Territory> =
        territoryDao.getAssignedNotStarted().map { it.toDomain() }

    override suspend fun getInProgress(): List<Territory> =
        territoryDao.getInProgress().map { it.toDomain() }

    override suspend fun getByResponsible(userId: String): List<Territory> =
        territoryDao.getByResponsible(userId).map { it.toDomain() }

    override suspend fun delete(id: String) {
        merger.removeLocally(id)
        val congregationId = congregationRepository.getCurrentCongregationId() ?: return
        appScope.launch {
            runCatching {
                remoteEventSource.deleteAllByTerritory(congregationId, id)
                remoteNoteSource.deleteAllByTerritory(congregationId, id)
                remoteSource.delete(congregationId, id)
            }.onFailure { Log.e(TAG, "Falha ao apagar território $id no servidor", it) }
        }
    }

    override suspend fun updateResponsibleProfile(user: User): Int {
        val outdated = territoryDao.getByResponsible(user.id)
            .map { it.toDomain() }
            .filter { it.currentResponsibleName != user.name || it.currentResponsiblePhotoUrl != user.photoUrl }
        val now = Date()
        outdated.forEach { territory ->
            save(
                territory.copy(
                    currentResponsibleName = user.name,
                    currentResponsiblePhotoUrl = user.photoUrl,
                    updatedAt = now
                )
            )
        }
        return outdated.size
    }

    private fun push(congregationId: String, territory: Territory, currentVersion: Long) {
        val nextVersion = currentVersion + 1
        remoteWriter.write(TAG, { remoteSource.upsert(congregationId, territory, nextVersion) }) {
            territoryDao.markSyncedIfUnchanged(territory.id, nextVersion, territory.updatedAt)
        }
    }

    private companion object {
        const val TAG = "TerritoryRepository"
    }
}
