package com.mraphaelpy.terriflow.domain.model

import java.util.Date
import java.util.UUID

enum class EventType {
    CREATED,
    ASSIGNED,
    STARTED,
    PAUSED,
    RESUMED,
    COMPLETED,
    RETURNED,
    TRANSFERRED,
    UPDATED,
    ARCHIVED
}

fun EventType.label(): String = when (this) {
    EventType.CREATED -> "Território criado"
    EventType.ASSIGNED -> "Atribuído"
    EventType.STARTED -> "Trabalho iniciado"
    EventType.PAUSED -> "Trabalho pausado"
    EventType.RESUMED -> "Trabalho retomado"
    EventType.COMPLETED -> "Finalizado"
    EventType.RETURNED -> "Devolvido"
    EventType.TRANSFERRED -> "Transferido"
    EventType.UPDATED -> "Atualizado"
    EventType.ARCHIVED -> "Arquivado"
}

data class TerritoryEvent(
    val id: String = "",
    val territoryId: String = "",
    val userId: String = "",
    val userName: String = "",
    val type: EventType = EventType.CREATED,
    val timestamp: Date = Date(),
    val extra: Map<String, String> = emptyMap(),
    val congregationId: String = ""
) {
    companion object {
        /** Cria um novo evento registrando [actor] como autor da ação. */
        fun create(
            territoryId: String,
            actor: User,
            type: EventType,
            timestamp: Date = Date(),
            extra: Map<String, String> = emptyMap()
        ) = TerritoryEvent(
            id = UUID.randomUUID().toString(),
            territoryId = territoryId,
            userId = actor.id,
            userName = actor.name,
            type = type,
            timestamp = timestamp,
            extra = extra,
            congregationId = actor.congregationId
        )
    }
}
