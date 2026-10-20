package com.mraphaelpy.terriflow.domain.model

import java.util.Date
import java.util.UUID

enum class TerritoryNoteKind { DO_NOT_CALL, FIELD_NOTE }

data class TerritoryNote(
    val id: String = UUID.randomUUID().toString(),
    val territoryId: String,
    val kind: TerritoryNoteKind,
    val address: String = "",
    val text: String = "",
    val authorId: String,
    val authorName: String,
    val createdAt: Date = Date()
)
