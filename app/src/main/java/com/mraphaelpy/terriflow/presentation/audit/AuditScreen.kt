package com.mraphaelpy.terriflow.presentation.audit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mraphaelpy.terriflow.core.report.ReportFormat
import com.mraphaelpy.terriflow.core.util.DateFormats
import com.mraphaelpy.terriflow.domain.model.EventType
import com.mraphaelpy.terriflow.domain.model.label
import com.mraphaelpy.terriflow.presentation.components.FullScreenLoading
import com.mraphaelpy.terriflow.presentation.components.OptionSheet
import com.mraphaelpy.terriflow.presentation.components.SheetOption
import com.mraphaelpy.terriflow.presentation.components.TerritoryShapeBadge
import com.mraphaelpy.terriflow.presentation.components.UserAvatar
import com.mraphaelpy.terriflow.presentation.components.UserAvatarByName
import com.mraphaelpy.terriflow.presentation.components.eventIcon
import com.mraphaelpy.terriflow.presentation.components.segmentedShape

private enum class AuditSheet { PERIOD, TYPE, TERRITORY, USER }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AuditScreen(
    onNavigateBack: () -> Unit,
    onNavigateToTerritory: (String) -> Unit,
    viewModel: AuditViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val shareIntent by viewModel.shareIntent.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var openSheet by remember { mutableStateOf<AuditSheet?>(null) }
    var showExportMenu by remember { mutableStateOf(false) }

    LaunchedEffect(shareIntent) {
        shareIntent?.let {
            context.startActivity(it)
            viewModel.onShareHandled()
        }
    }

    FilterSheets(openSheet, uiState, viewModel) { openSheet = null }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("Auditoria") },
                subtitle = { Text("${uiState.matchingCount} registros · ${uiState.filter.period.label.lowercase()}") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showExportMenu = true }, enabled = uiState.matchingCount > 0) {
                            Icon(Icons.Default.FileDownload, contentDescription = "Exportar")
                        }
                        DropdownMenu(expanded = showExportMenu, onDismissRequest = { showExportMenu = false }) {
                            DropdownMenuItem(text = { Text("Exportar PDF") }, onClick = {
                                showExportMenu = false
                                viewModel.export(ReportFormat.PDF)
                            })
                            DropdownMenuItem(text = { Text("Exportar planilha (CSV)") }, onClick = {
                                showExportMenu = false
                                viewModel.export(ReportFormat.CSV)
                            })
                        }
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (isSyncing) {
                LinearWavyProgressIndicator(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp))
            }
            FilterRow(uiState, onOpen = { openSheet = it }, onClear = viewModel::clearFilters)

            when {
                uiState.isLoading -> FullScreenLoading()
                uiState.days.isEmpty() -> EmptyAudit()
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    uiState.days.forEach { day ->
                        item(key = "day-${day.label}") {
                            Text(
                                day.label,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 8.dp, top = 20.dp, bottom = 8.dp)
                            )
                        }
                        itemsIndexed(day.entries, key = { _, entry -> entry.event.id }) { index, entry ->
                            AuditItem(
                                entry = entry,
                                color = uiState.palette[entry.event.territoryId] ?: MaterialTheme.colorScheme.primary,
                                shape = segmentedShape(index, day.entries.size, outer = 20.dp),
                                onClick = { onNavigateToTerritory(entry.event.territoryId) }
                            )
                        }
                    }
                    if (uiState.hasMore) {
                        item(key = "more") {
                            Box(Modifier.fillMaxWidth().padding(top = 16.dp), contentAlignment = Alignment.Center) {
                                FilledTonalButton(onClick = viewModel::loadMore) { Text("Carregar mais") }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterRow(uiState: AuditUiState, onOpen: (AuditSheet) -> Unit, onClear: () -> Unit) {
    val filter = uiState.filter
    val territory = uiState.territories.firstOrNull { it.id == filter.territoryId }
    val user = uiState.users.firstOrNull { it.id == filter.userId }
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item { DropdownChip(filter.period.label, true) { onOpen(AuditSheet.PERIOD) } }
        item { DropdownChip(filter.type?.label() ?: "Ação", filter.type != null) { onOpen(AuditSheet.TYPE) } }
        item { DropdownChip(territory?.code ?: "Território", territory != null) { onOpen(AuditSheet.TERRITORY) } }
        item { DropdownChip(user?.name ?: "Pessoa", user != null) { onOpen(AuditSheet.USER) } }
        if (filter.isActive) {
            item {
                FilterChip(
                    selected = false,
                    onClick = onClear,
                    label = { Text("Limpar") },
                    leadingIcon = { Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    shape = CircleShape
                )
            }
        }
    }
}

@Composable
private fun DropdownChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(18.dp)) },
        shape = CircleShape
    )
}

@Composable
private fun FilterSheets(open: AuditSheet?, uiState: AuditUiState, viewModel: AuditViewModel, onDismiss: () -> Unit) {
    val filter = uiState.filter
    when (open) {
        AuditSheet.PERIOD -> OptionSheet(
            title = "Período",
            options = AuditPeriod.entries.map { SheetOption(it, it.label) },
            selected = filter.period,
            onSelect = viewModel::setPeriod,
            onDismiss = onDismiss
        )
        AuditSheet.TYPE -> OptionSheet(
            title = "Ação",
            options = listOf(SheetOption<EventType?>(null, "Todas")) + EventType.entries.map { SheetOption<EventType?>(it, it.label()) },
            selected = filter.type,
            onSelect = viewModel::setType,
            onDismiss = onDismiss
        )
        AuditSheet.TERRITORY -> OptionSheet(
            title = "Território",
            options = listOf(SheetOption<String?>(null, "Todos")) + uiState.territories.map { territory ->
                SheetOption<String?>(
                    territory.id,
                    territory.code,
                    supporting = territory.name,
                    leading = { TerritoryShapeBadge(territory.code, uiState.palette[territory.id] ?: Color.Gray, size = 36.dp) }
                )
            },
            selected = filter.territoryId,
            onSelect = viewModel::setTerritory,
            onDismiss = onDismiss
        )
        AuditSheet.USER -> OptionSheet(
            title = "Pessoa",
            options = listOf(SheetOption<String?>(null, "Todas")) + uiState.users.map { user ->
                SheetOption<String?>(user.id, user.name, supporting = user.role.label(), leading = { UserAvatar(user, size = 36.dp) })
            },
            selected = filter.userId,
            onSelect = viewModel::setUser,
            onDismiss = onDismiss
        )
        null -> Unit
    }
}

@Composable
private fun AuditItem(entry: AuditEntry, color: Color, shape: RoundedCornerShape, onClick: () -> Unit) {
    val event = entry.event
    val changes = describeChanges(event)
    val target = event.extra["responsibleName"] ?: event.extra["toUserName"]
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = shape, color = MaterialTheme.colorScheme.surfaceContainer) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier.size(40.dp).background(color.copy(alpha = 0.18f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(eventIcon(event.type), contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(event.type.label(), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Text(DateFormats.time().format(event.timestamp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    entry.territory?.let { "${it.code} · ${it.name}" } ?: "Território removido",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (target != null) {
                    Text("Para $target", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
                changes.forEach { change ->
                    Text(change, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    UserAvatarByName(name = event.userName, size = 18.dp, userId = event.userId)
                    Text("por ${event.userName}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun EmptyAudit() {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(112.dp).background(MaterialTheme.colorScheme.secondaryContainer, MaterialShapes.Cookie12Sided.toShape()),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(44.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
        }
        Spacer(Modifier.height(20.dp))
        Text("Nenhum registro neste período", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text("Ajuste o período ou os filtros.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
