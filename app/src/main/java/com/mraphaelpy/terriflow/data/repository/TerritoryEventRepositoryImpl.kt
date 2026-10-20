package com.mraphaelpy.terriflow.data.repository

import android.util.Log
import com.mraphaelpy.terriflow.data.local.dao.TerritoryDao
import com.mraphaelpy.terriflow.data.local.dao.TerritoryEventDao
import com.mraphaelpy.terriflow.data.local.dao.UserDao
import com.mraphaelpy.terriflow.data.local.entity.TerritoryEventEntity
import com.mraphaelpy.terriflow.data.remote.RemoteWriter
import com.mraphaelpy.terriflow.data.remote.source.FirestoreEventSource
import com.mraphaelpy.terriflow.domain.model.TerritoryEvent
import com.mraphaelpy.terriflow.domain.model.withCurrentNames
import com.mraphaelpy.terriflow.domain.repository.CongregationRepository
import com.mraphaelpy.terriflow.domain.repository.TerritoryEventRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TerritoryEventRepositoryImpl @Inject constructor(
    private val eventDao: TerritoryEventDao,
    private val userDao: UserDao,
    private val territoryDao: TerritoryDao,
    private val remoteSource: FirestoreEventSource,
    private val remoteWriter: RemoteWriter,
    private val congregationRepository: CongregationRepository
) : TerritoryEventRepository {

    override fun observeAll(): Flow<List<TerritoryEvent>> =
        eventDao.observeAll().withCurrentNames()

    override fun observeByTerritory(territoryId: String): Flow<List<TerritoryEvent>> =
        eventDao.observeByTerritory(territoryId).withCurrentNames()

    override fun observeRecent(limit: Int): Flow<List<TerritoryEvent>> =
        eventDao.observeRecent(limit).withCurrentNames()

    private fun Flow<List<TerritoryEventEntity>>.withCurrentNames(): Flow<List<TerritoryEvent>> =
        combine(userDao.observeAll()) { events, users ->
            events.map { it.toDomain() }.withCurrentNames(users.map { it.toDomain() })
        }

    override suspend fun save(event: TerritoryEvent) {
        if (eventDao.getById(event.id) != null) return

        val congregationId = congregationRepository.getCurrentCongregationId()
        val stamped = event.copy(congregationId = event.congregationId.ifBlank { congregationId.orEmpty() })
        eventDao.insert(TerritoryEventEntity.fromDomain(stamped, synced = false))

        if (congregationId == null) return
        remoteWriter.write(TAG, { remoteSource.insert(congregationId, stamped) }) {
            eventDao.markSynced(stamped.id)
        }
    }

    override suspend fun syncPendingToRemote() {
        val congregationId = congregationRepository.getCurrentCongregationId() ?: return
        eventDao.getPending().forEach { entity ->
            val event = entity.toDomain().let { it.copy(congregationId = it.congregationId.ifBlank { congregationId }) }
            runCatching { remoteSource.exists(congregationId, event.territoryId, event.id) }
                .onSuccess { exists ->
                    if (exists) {
                        eventDao.markSynced(event.id)
                    } else {
                        remoteWriter.write(TAG, { remoteSource.insert(congregationId, event) }) {
                            eventDao.markSynced(event.id)
                        }
                    }
                }
                .onFailure { Log.e(TAG, "Erro ao verificar evento pendente ${event.id}", it) }
        }
    }

    override suspend fun syncFromRemote(territoryId: String) {
        val congregationId = congregationRepository.getCurrentCongregationId() ?: return
        runCatching {
            val entities = remoteSource.getByTerritory(congregationId, territoryId).map {
                TerritoryEventEntity.fromDomain(it.toDomain(), synced = true)
            }
            eventDao.insertAll(entities)
        }.onFailure {
            Log.e(TAG, "Erro ao sincronizar eventos do território $territoryId", it)
        }
    }

    override suspend fun syncAllFromRemote() {
        coroutineScope {
            territoryDao.getAllIds()
                .chunked(PARALLEL_TERRITORY_SYNCS)
                .forEach { chunk -> chunk.map { id -> async { syncFromRemote(id) } }.awaitAll() }
        }
    }

    private companion object {
        const val PARALLEL_TERRITORY_SYNCS = 8
        const val TAG = "TerritoryEventRepository"
    }
}
