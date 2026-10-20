package com.mraphaelpy.terriflow.presentation.territorydetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mraphaelpy.terriflow.domain.model.Territory

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun BlockProgressSection(
    territory: Territory,
    color: Color,
    canMark: Boolean,
    onToggle: (Int) -> Unit
) {
    val total = territory.blockPolygons.size
    val done = territory.completedBlockCount
    Surface(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Quadras", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("$done de $total feitas", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            LinearWavyProgressIndicator(
                progress = { if (total == 0) 0f else done / total.toFloat() },
                color = color,
                modifier = Modifier.fillMaxWidth()
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                (0 until total).forEach { index ->
                    val completed = index in territory.completedBlocks
                    FilterChip(
                        selected = completed,
                        onClick = { onToggle(index) },
                        enabled = canMark,
                        label = { Text("${index + 1}") },
                        leadingIcon = if (completed) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        shape = CircleShape,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = color.copy(alpha = 0.25f),
                            disabledSelectedContainerColor = color.copy(alpha = 0.18f)
                        )
                    )
                }
            }
            if (!canMark) {
                Text(
                    "Inicie o trabalho para marcar as quadras concluídas.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
