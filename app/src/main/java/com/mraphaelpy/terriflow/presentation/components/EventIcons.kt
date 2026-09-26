package com.mraphaelpy.terriflow.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.mraphaelpy.terriflow.domain.model.EventType

/**
 * Mapeia um [EventType] para o ícone correspondente na UI.
 *
 * Anteriormente duplicado em TerritoryDetailScreen e AuditScreen.
 */
fun eventIcon(type: EventType): ImageVector = when (type) {
    EventType.CREATED -> Icons.Default.AddCircle
    EventType.ASSIGNED -> Icons.Default.PersonAdd
    EventType.STARTED -> Icons.Default.PlayArrow
    EventType.PAUSED -> Icons.Default.PauseCircle
    EventType.RESUMED -> Icons.Default.PlayCircle
    EventType.COMPLETED -> Icons.Default.TaskAlt
    EventType.RETURNED -> Icons.AutoMirrored.Filled.Undo
    EventType.TRANSFERRED -> Icons.Default.SyncAlt
    EventType.UPDATED -> Icons.Default.Edit
    EventType.ARCHIVED -> Icons.Default.Archive
}
