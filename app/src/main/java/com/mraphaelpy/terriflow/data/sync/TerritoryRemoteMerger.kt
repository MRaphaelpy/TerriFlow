package com.mraphaelpy.terriflow.data.sync

import com.mraphaelpy.terriflow.data.local.dao.NotificationDao
import com.mraphaelpy.terriflow.data.local.dao.TerritoryDao
import com.mraphaelpy.terriflow.data.local.dao.TerritoryEventDao
import com.mraphaelpy.terriflow.data.local.entity.TerritoryEntity
import com.mraphaelpy.terriflow.data.remote.dto.TerritoryDto
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Aplica no Room a lista de territórios vinda do Firestore.
 *
 * - alterações locais ainda não enviadas (`synced = false`) nunca são sobrescritas;
 * - a versão remota só substitui a local se for igual ou mais nova (`syncVersion`);
 * - com [removeMissing], territórios já sincronizados que sumiram do servidor
 *   são apagados junto com histórico e notificações.
 */
@Singleton
class TerritoryRemoteMerger @Inject constructor(
    private val territoryDao: TerritoryDao,
    private val eventDao: TerritoryEventDao,
    private val notificationDao: NotificationDao
) {
    suspend fun merge(remote: List<TerritoryDto>, removeMissing: Boolean) {
        if (removeMissing) {
            val remoteIds = remote.mapTo(HashSet()) { it.id }
            territoryDao.getSyncedIds()
                .filterNot { it in remoteIds }
                .forEach { removeLocally(it) }
        }

        remote.forEach { dto ->
            val local = territoryDao.getById(dto.id)
            val shouldApply = local == null || (local.synced && dto.syncVersion >= local.syncVersion)
            if (shouldApply) {
                territoryDao.upsert(
                    TerritoryEntity.fromDomain(dto.toDomain(), synced = true).copy(syncVersion = dto.syncVersion)
                )
            }
        }
    }

    suspend fun removeLocally(territoryId: String) {
        territoryDao.deletePermanent(territoryId)
        eventDao.deleteByTerritory(territoryId)
        notificationDao.deleteByTerritory(territoryId)
    }
}
