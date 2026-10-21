package com.mraphaelpy.terriflow.presentation.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mraphaelpy.terriflow.domain.model.Territory
import com.mraphaelpy.terriflow.domain.model.center
import com.mraphaelpy.terriflow.presentation.components.TerritoryShapeBadge
import com.mraphaelpy.terriflow.presentation.components.UserAvatarByName
import com.mraphaelpy.terriflow.domain.model.label
import com.mraphaelpy.terriflow.presentation.components.statusColor
import com.mraphaelpy.terriflow.presentation.territorydetail.statusSince
import java.util.Locale

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun TerritoryMapSheet(
    territory: Territory,
    territoryColor: Color,
    isMine: Boolean,
    distanceMeters: Float?,
    onOpen: () -> Unit
) {
    val context = LocalContext.current
    val center = territory.center
    val since = statusSince(territory)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            TerritoryShapeBadge(code = territory.code, color = territoryColor, size = 64.dp)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    territory.name.ifBlank { territory.code },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusPill(territory)
                    distanceMeters?.let {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            Icon(Icons.Default.NearMe, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(formatDistance(it), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                ResponsibleRow(territory, isMine, since)
                if (territory.hasBlocks) BlocksProgress(territory)
                if (territory.location.isNotBlank()) {
                    DetailRow(icon = Icons.Default.LocationOn, text = territory.location)
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(
                onClick = { center?.let { runCatching { openDirections(context, it.lat, it.lng) } } },
                enabled = center != null,
                shapes = ButtonDefaults.shapes(),
                contentPadding = ButtonDefaults.MediumContentPadding,
                modifier = Modifier.weight(1f).height(56.dp)
            ) {
                Icon(Icons.Default.Directions, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Como chegar", style = MaterialTheme.typography.titleSmall)
            }
            Button(
                onClick = onOpen,
                shapes = ButtonDefaults.shapes(),
                contentPadding = ButtonDefaults.MediumContentPadding,
                modifier = Modifier.weight(1f).height(56.dp)
            ) {
                Text("Detalhes", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun StatusPill(territory: Territory) {
    val color = statusColor(territory.status)
    Surface(shape = CircleShape, color = color.copy(alpha = 0.18f)) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
            Text(territory.status.label(), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ResponsibleRow(territory: Territory, isMine: Boolean, since: String) {
    val name = territory.currentResponsibleName
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        if (name != null) {
            UserAvatarByName(name = name, size = 40.dp, userId = territory.currentResponsibleId)
        } else {
            Box(
                modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.surfaceContainerHighest, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.PersonOff, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                when {
                    name == null -> "Sem responsável"
                    isMine -> "$name (você)"
                    else -> name
                },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (since.isNotBlank()) {
                Text(since, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun BlocksProgress(territory: Territory) {
    val total = territory.blockPolygons.size
    val done = territory.completedBlockCount
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Default.GridView, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            Text(
                if (done == 0) "$total quadras" else "$done de $total quadras feitas",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            if (done > 0) {
                Text("${done * 100 / total}%", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
        }
        if (done > 0) {
            LinearWavyProgressIndicator(progress = { done.toFloat() / total }, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun DetailRow(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

private fun formatDistance(meters: Float): String = when {
    meters < 1000f -> "a ${(meters / 10f).toInt() * 10} m"
    meters < 10_000f -> String.format(Locale("pt", "BR"), "a %.1f km", meters / 1000f)
    else -> "a ${(meters / 1000f).toInt()} km"
}
