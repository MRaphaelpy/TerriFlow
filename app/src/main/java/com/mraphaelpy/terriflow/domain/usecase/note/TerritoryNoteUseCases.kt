package com.mraphaelpy.terriflow.domain.usecase.note

import com.mraphaelpy.terriflow.domain.model.DomainException
import com.mraphaelpy.terriflow.domain.model.TerritoryNote
import com.mraphaelpy.terriflow.domain.model.TerritoryNoteKind
import com.mraphaelpy.terriflow.domain.policy.TerritoryActionPolicy
import com.mraphaelpy.terriflow.domain.repository.TerritoryNoteRepository
import com.mraphaelpy.terriflow.domain.repository.TerritoryRepository
import com.mraphaelpy.terriflow.domain.usecase.territory.TerritoryActionGuard
import javax.inject.Inject

class AddTerritoryNoteUseCase @Inject constructor(
    private val guard: TerritoryActionGuard,
    private val territoryRepository: TerritoryRepository,
    private val noteRepository: TerritoryNoteRepository
) {
    suspend operator fun invoke(territoryId: String, kind: TerritoryNoteKind, address: String, text: String) {
        val actor = guard.requireCurrentUser()
        val territory = territoryRepository.getById(territoryId) ?: throw DomainException.TerritoryNotFound()
        if (!TerritoryActionPolicy.canAccessNotes(actor, territory)) throw DomainException.NotesNotAllowed()
        if (address.isBlank() && text.isBlank()) throw DomainException.EmptyNote()

        noteRepository.add(
            TerritoryNote(
                territoryId = territoryId,
                kind = kind,
                address = address.trim(),
                text = text.trim(),
                authorId = actor.id,
                authorName = actor.name
            )
        )
    }
}

class DeleteTerritoryNoteUseCase @Inject constructor(
    private val guard: TerritoryActionGuard,
    private val noteRepository: TerritoryNoteRepository
) {
    suspend operator fun invoke(note: TerritoryNote) {
        val actor = guard.requireCurrentUser()
        if (!TerritoryActionPolicy.canDeleteNote(actor, note)) throw DomainException.NotesNotAllowed()
        noteRepository.delete(note)
    }
}
