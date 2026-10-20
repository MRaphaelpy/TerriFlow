package com.mraphaelpy.terriflow.presentation.coverage

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mraphaelpy.terriflow.core.util.DateFormats
import com.mraphaelpy.terriflow.domain.model.CampaignCoverage
import com.mraphaelpy.terriflow.domain.model.Territory
import com.mraphaelpy.terriflow.presentation.components.TerritoryShapeBadge
import java.util.Calendar
import java.util.Date
import java.util.TimeZone

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun CampaignCard(
    coverage: CampaignCoverage,
    palette: Map<String, Color>,
    onTerritoryClick: (String) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val campaign = coverage.campaign
    val now = Date()
    var expanded by remember { mutableStateOf(campaign.isActive(now)) }
    val dates = DateFormats.fullDate()
    val status = when {
        campaign.isActive(now) -> "Em andamento"
        now.before(campaign.start) -> "Futura"
        else -> "Encerrada"
    }

    Surface(modifier = modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(campaign.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "${dates.format(campaign.start)} – ${dates.format(campaign.end)} · $status",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDelete) { Icon(Icons.Default.DeleteOutline, contentDescription = "Remover campanha") }
            }
            LinearWavyProgressIndicator(progress = { coverage.percent / 100f }, modifier = Modifier.fillMaxWidth())
            Text(
                "${coverage.percent}% concluído · ${coverage.completed.size} concluídos · ${coverage.inProgress.size} em andamento · ${coverage.notWorked.size} sem trabalho",
                style = MaterialTheme.typography.labelLarge
            )
            if (coverage.notWorked.isNotEmpty()) {
                TextButton(onClick = { expanded = !expanded }, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                    Text(if (expanded) "Ocultar sem trabalho" else "Ver territórios sem trabalho")
                    Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null)
                }
                AnimatedVisibility(visible = expanded) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        coverage.notWorked.forEach { territory ->
                            NotWorkedRow(territory, palette[territory.id] ?: MaterialTheme.colorScheme.primary) { onTerritoryClick(territory.id) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotWorkedRow(territory: Territory, color: Color, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TerritoryShapeBadge(territory.code, color, size = 32.dp)
            Text("${territory.code} · ${territory.name}", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CreateCampaignDialog(onCreate: (String, Date, Date) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    val rangeState = rememberDateRangePickerState()
    val start = rangeState.selectedStartDateMillis
    val end = rangeState.selectedEndDateMillis

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = { onCreate(name, utcDayToLocal(start!!, endOfDay = false), utcDayToLocal(end!!, endOfDay = true)) },
                enabled = name.isNotBlank() && start != null && end != null
            ) { Text("Criar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Nome da campanha") },
            placeholder = { Text("Ex.: Celebração 2026") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 16.dp)
        )
        DateRangePicker(
            state = rangeState,
            title = { Text("Período", modifier = Modifier.padding(start = 24.dp, top = 12.dp)) },
            showModeToggle = false,
            modifier = Modifier.weight(1f)
        )
    }
}

/** O seletor devolve meia-noite em UTC; converte para o mesmo dia no fuso local. */
private fun utcDayToLocal(utcMillis: Long, endOfDay: Boolean): Date {
    val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = utcMillis }
    return Calendar.getInstance().apply {
        clear()
        set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH), utc.get(Calendar.DAY_OF_MONTH))
        if (endOfDay) {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
        }
    }.time
}
