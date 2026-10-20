package com.mraphaelpy.terriflow.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mraphaelpy.terriflow.domain.model.TerritoryStatus
import com.mraphaelpy.terriflow.domain.model.label

@Composable
fun TerritoryFilterChips(
    filter: TerritoryFilter,
    statusCounts: Map<TerritoryStatus, Int>,
    myCount: Int,
    onAllClick: () -> Unit,
    onMineClick: () -> Unit,
    onStatusClick: (TerritoryStatus) -> Unit,
    modifier: Modifier = Modifier,
    elevated: Boolean = false
) {
    val colors = FilterChipDefaults.filterChipColors(
        containerColor = if (elevated) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surface,
        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer
    )
    val elevation = if (elevated) FilterChipDefaults.filterChipElevation(elevation = 2.dp) else null
    val statuses = TerritoryStatus.entries.filter { (statusCounts[it] ?: 0) > 0 }

    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                selected = filter.status == null,
                onClick = onAllClick,
                label = { Text("Todos") },
                shape = CircleShape,
                colors = colors,
                elevation = elevation
            )
        }
        if (myCount > 0) {
            item {
                FilterChip(
                    selected = filter.onlyMine,
                    onClick = onMineClick,
                    label = { Text("Meus ($myCount)") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    shape = CircleShape,
                    colors = colors,
                    elevation = elevation
                )
            }
        }
        items(statuses, key = { it.name }) { status ->
            FilterChip(
                selected = filter.status == status,
                onClick = { onStatusClick(status) },
                label = { Text("${status.label()} (${statusCounts[status] ?: 0})") },
                leadingIcon = {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(statusColor(status), CircleShape)
                    )
                },
                shape = CircleShape,
                colors = colors,
                elevation = elevation
            )
        }
    }
}
