package com.mraphaelpy.terriflow.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material.icons.filled.TaskAlt
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
