package com.mraphaelpy.terriflow.presentation.territorydetail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mraphaelpy.terriflow.core.util.DateFormats
import com.mraphaelpy.terriflow.domain.model.ResponsibleRotationInfo
import com.mraphaelpy.terriflow.domain.model.TerritoryRotationHelper
import com.mraphaelpy.terriflow.domain.model.User
import com.mraphaelpy.terriflow.presentation.components.UserAvatar

@Composable
internal fun AssignDialog(
    responsibles: List<User>,
    rotationInfos: List<ResponsibleRotationInfo>,
    onAssign: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val candidates = remember(responsibles, rotationInfos) { candidatesFor(responsibles, rotationInfos) }
    var selected by remember { mutableStateOf(candidates.firstOrNull { it.isRecommended }) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
        title = { Text("Atribuir território") },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                LazyColumn(modifier = Modifier.heightIn(max = 380.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(candidates, key = { it.user.id }) { info ->
                        CandidateRow(info, selected = selected?.user?.id == info.user.id) { selected = info }
                    }
                }
                RotationWarnings(selected)
            }
        },
        confirmButton = {
            Button(onClick = { selected?.let { onAssign(it.user.id) } }, enabled = selected != null) {
                Text(if (selected.needsConfirmation()) "Atribuir assim mesmo" else "Atribuir")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
internal fun CompleteDialog(
    responsibles: List<User>,
    rotationInfos: List<ResponsibleRotationInfo>,
    onComplete: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    val candidates = remember(responsibles, rotationInfos) { candidatesFor(responsibles, rotationInfos) }
    var next by remember { mutableStateOf<ResponsibleRotationInfo?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.TaskAlt, contentDescription = null) },
        title = { Text("Finalizar território") },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Deseja já passar o território para outro dirigente?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyColumn(modifier = Modifier.heightIn(max = 380.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    item(key = "none") {
                        OptionRow(
                            selected = next == null,
                            onClick = { next = null },
                            leading = { IconBadge(Icons.Default.Check) },
                            title = "Não, apenas finalizar"
                        )
                    }
                    items(candidates, key = { it.user.id }) { info ->
                        CandidateRow(info, selected = next?.user?.id == info.user.id) { next = info }
                    }
                }
                RotationWarnings(next)
            }
        },
        confirmButton = {
            Button(onClick = { onComplete(next?.user?.id) }) {
                Text(
                    when {
                        next == null -> "Finalizar"
                        next.needsConfirmation() -> "Transferir assim mesmo"
                        else -> "Finalizar e transferir"
                    }
                )
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

private fun candidatesFor(responsibles: List<User>, rotationInfos: List<ResponsibleRotationInfo>): List<ResponsibleRotationInfo> {
    val allowed = responsibles.mapTo(HashSet()) { it.id }
    val ranked = rotationInfos.filter { it.user.id in allowed }
    val missing = responsibles.filter { user -> ranked.none { it.user.id == user.id } }.map { ResponsibleRotationInfo(it) }
    return ranked + missing
}

private fun ResponsibleRotationInfo?.needsConfirmation(): Boolean =
    this != null && (hasWorkedPreviously || isOverLimit)

@Composable
private fun CandidateRow(info: ResponsibleRotationInfo, selected: Boolean, onClick: () -> Unit) {
    val details = buildList {
        add(
            when (info.currentTerritories) {
                0 -> "Sem territórios no momento"
                1 -> "Com 1 território"
                else -> "Com ${info.currentTerritories} territórios"
            }
        )
        if (info.hasWorkedPreviously) {
            val date = info.lastAssignedDate?.let { " em ${DateFormats.shortDate().format(it)}" }.orEmpty()
            add("Já trabalhou aqui (${info.timesAssigned}×)$date")
        }
    }
    OptionRow(
        selected = selected,
        onClick = onClick,
        leading = { UserAvatar(user = info.user, size = 36.dp) },
        title = info.user.name,
        subtitle = details.joinToString(" · "),
        subtitleColor = if (info.isOverLimit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        badge = if (info.isRecommended) "Recomendado" else null
    )
}

@Composable
private fun OptionRow(
    selected: Boolean,
    onClick: () -> Unit,
    leading: @Composable () -> Unit,
    title: String,
    subtitle: String? = null,
    subtitleColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    badge: String? = null
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = selected, onClick = onClick)
            leading()
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
                    badge?.let {
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary) {
                            Text(
                                it,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                subtitle?.let { Text(it, style = MaterialTheme.typography.labelMedium, color = subtitleColor) }
            }
        }
    }
}

@Composable
private fun IconBadge(icon: ImageVector) {
    Box(
        modifier = Modifier.size(36.dp).background(MaterialTheme.colorScheme.tertiaryContainer, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun RotationWarnings(info: ResponsibleRotationInfo?) {
    val messages = buildList {
        if (info?.isOverLimit == true) {
            add("${info.user.name} já está com ${info.currentTerritories} territórios (limite sugerido: ${TerritoryRotationHelper.MAX_ACTIVE_PER_RESPONSIBLE}).")
        }
        if (info?.hasWorkedPreviously == true) {
            add("Já trabalhou neste território. Para fazer rodízio, considere quem ainda não trabalhou aqui.")
        }
    }
    AnimatedVisibility(visible = messages.isNotEmpty()) {
        Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.errorContainer, modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(20.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    messages.forEach {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }
        }
    }
}
