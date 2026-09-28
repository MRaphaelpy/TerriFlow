package com.mraphaelpy.terriflow.presentation.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mraphaelpy.terriflow.domain.model.TerritoryStatus
import com.mraphaelpy.terriflow.domain.model.label

/**
 * Chip visual que representa o status de um território.
 *
 * Usado em TerritoryListScreen (item da lista) e TerritoryDetailScreen (cabeçalho do card).
 */
@Composable
fun StatusChip(status: TerritoryStatus, modifier: Modifier = Modifier) {
    val color = when (status) {
        TerritoryStatus.AVAILABLE   -> MaterialTheme.colorScheme.secondaryContainer
        TerritoryStatus.ASSIGNED    -> MaterialTheme.colorScheme.tertiaryContainer
        TerritoryStatus.IN_PROGRESS -> MaterialTheme.colorScheme.primaryContainer
        TerritoryStatus.PAUSED      -> MaterialTheme.colorScheme.surfaceVariant
        TerritoryStatus.COMPLETED   -> MaterialTheme.colorScheme.secondaryContainer
        TerritoryStatus.RETURNED    -> MaterialTheme.colorScheme.errorContainer
        TerritoryStatus.ARCHIVED    -> MaterialTheme.colorScheme.surfaceVariant
    }
    Surface(
        shape = MaterialTheme.shapes.extraSmall,
        color = color,
        modifier = modifier
    ) {
        Text(
            text = status.label(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}
