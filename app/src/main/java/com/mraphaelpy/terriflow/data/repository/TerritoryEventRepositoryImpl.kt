package com.mraphaelpy.terriflow.data.repository

import com.mraphaelpy.terriflow.data.local.dao.TerritoryEventDao
import com.mraphaelpy.terriflow.data.local.entity.TerritoryEventEntity
import com.mraphaelpy.terriflow.data.remote.source.FirestoreEventSource
import com.mraphaelpy.terriflow.domain.model.TerritoryEvent
import com.mraphaelpy.terriflow.domain.repository.CongregationRepository
import com.mraphaelpy.terriflow.domain.repository.TerritoryEventRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TerritoryEventRepositoryImpl @Inject constructor(
    private val eventDao: TerritoryEventDao,
    private val remoteSource: FirestoreEventSource,
    private val congregationRepository: CongregationRepository
) : TerritoryEventRepository {

    override fun observeAll(): Flow<List<TerritoryEvent>> =
        eventDao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeByTerritory(territoryId: String): Flow<List<TerritoryEvent>> =
        eventDao.observeByTerritory(territoryId).map { list -> list.map { it.toDomain() } }

    override fun observeRecent(limit: Int): Flow<List<TerritoryEvent>> =
        eventDao.observeRecent(limit).map { list -> list.map { it.toDomain() } }

    override suspend fun save(event: TerritoryEvent) {
        val existing = eventDao.getById(event.id)
        if (existing != null) return

        eventDao.insert(TerritoryEventEntity.fromDomain(event, synced = false))

        val congregationId = congregationRepository.getCurrentCongregationId() ?: return
        runCatching {
            if (!remoteSource.exists(congregationId, event.territoryId, event.id)) {
                remoteSource.insert(congregationId, event)
            }
        }.onSuccess {
            eventDao.markSynced(event.id)
        }
    }

    override suspend fun syncPendingToRemote() {
        val congregationId = congregationRepository.getCurrentCongregationId() ?: return
        val pending = eventDao.getPending()
        pending.forEach { entity ->
            runCatching {
                val event = entity.toDomain()
                if (!remoteSource.exists(congregationId, event.territoryId, event.id)) {
                    remoteSource.insert(congregationId, event)
                }
            }.onSuccess {
                eventDao.markSynced(entity.id)
            }
        }
    }

    override suspend fun syncFromRemote(territoryId: String) {
        val congregationId = congregationRepository.getCurrentCongregationId() ?: return
        runCatching {
            val remoteEvents = remoteSource.getByTerritory(congregationId, territoryId)
            val entities = remoteEvents.map {
                TerritoryEventEntity.fromDomain(it.toDomain(), synced = true)
            }
            eventDao.insertAll(entities)
        }
    }
}
