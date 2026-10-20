package com.mraphaelpy.terriflow.data.repository

import android.util.Log
import com.mraphaelpy.terriflow.data.remote.RemoteWriter
import com.mraphaelpy.terriflow.data.remote.source.FirestoreNoteSource
import com.mraphaelpy.terriflow.domain.model.DomainException
import com.mraphaelpy.terriflow.domain.model.TerritoryNote
import com.mraphaelpy.terriflow.domain.repository.CongregationRepository
import com.mraphaelpy.terriflow.domain.repository.TerritoryNoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TerritoryNoteRepositoryImpl @Inject constructor(
    private val remoteSource: FirestoreNoteSource,
    private val remoteWriter: RemoteWriter,
    private val congregationRepository: CongregationRepository
) : TerritoryNoteRepository {

    override fun observeByTerritory(territoryId: String): Flow<List<TerritoryNote>> = flow {
        val congregationId = congregationRepository.getCurrentCongregationId()
        if (congregationId == null) {
            emit(emptyList())
            return@flow
        }
        emitAll(remoteSource.observe(congregationId, territoryId))
    }.catch {
        Log.w(TAG, "Anotações indisponíveis para o território $territoryId", it)
        emit(emptyList())
    }

    override suspend fun add(note: TerritoryNote) {
        val congregationId = congregationRepository.getCurrentCongregationId() ?: throw DomainException.NotAuthenticated()
        remoteWriter.write(TAG, { remoteSource.insert(congregationId, note) })
    }

    override suspend fun delete(note: TerritoryNote) {
        val congregationId = congregationRepository.getCurrentCongregationId() ?: throw DomainException.NotAuthenticated()
        remoteWriter.write(TAG, { remoteSource.delete(congregationId, note) })
    }

    private companion object {
        const val TAG = "TerritoryNoteRepository"
    }
}
