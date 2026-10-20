package com.mraphaelpy.terriflow.presentation.coverage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularWavyProgressIndicator
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
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
import com.mraphaelpy.terriflow.domain.model.Campaign
import com.mraphaelpy.terriflow.domain.model.ResponsibleStats
import com.mraphaelpy.terriflow.domain.model.ServiceYearCoverage
import com.mraphaelpy.terriflow.presentation.components.ConfirmDialog
import com.mraphaelpy.terriflow.presentation.components.FullScreenLoading
import com.mraphaelpy.terriflow.presentation.components.OptionSheet
import com.mraphaelpy.terriflow.presentation.components.SheetOption
import com.mraphaelpy.terriflow.presentation.components.TerritoryShapeBadge
import com.mraphaelpy.terriflow.presentation.components.UserAvatarByName
import com.mraphaelpy.terriflow.presentation.components.segmentedShape
import java.util.Date
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CoverageScreen(
    onNavigateBack: () -> Unit,
    onNavigateToTerritory: (String) -> Unit,
    viewModel: CoverageViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val shareIntent by viewModel.shareIntent.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var showExportMenu by remember { mutableStateOf(false) }
    var showSortSheet by remember { mutableStateOf(false) }
    var showCampaignDialog by remember { mutableStateOf(false) }
    var pendingCampaignDelete by remember { mutableStateOf<Campaign?>(null) }
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    if (showCampaignDialog) {
        CreateCampaignDialog(
            onCreate = { name, start, end ->
                viewModel.createCampaign(name, start, end)
                showCampaignDialog = false
            },
            onDismiss = { showCampaignDialog = false }
        )
    }

    pendingCampaignDelete?.let { campaign ->
        ConfirmDialog(
            title = "Remover campanha",
            text = "Remover \"${campaign.name}\"? Os territórios e o histórico não são afetados.",
            confirmLabel = "Remover",
            icon = Icons.Default.Delete,
            onConfirm = {
                viewModel.deleteCampaign(campaign.id)
                pendingCampaignDelete = null
            },
            onDismiss = { pendingCampaignDelete = null }
        )
    }

    LaunchedEffect(shareIntent) {
        shareIntent?.let {
            context.startActivity(it)
            viewModel.onShareHandled()
        }
    }

    if (showSortSheet) {
        OptionSheet(
            title = "Mostrar",
            options = CoverageSortOption.entries.map { SheetOption(it, it.label) },
            selected = uiState.sortOption,
            onSelect = viewModel::setSortOption,
            onDismiss = { showSortSheet = false }
        )
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("Cobertura") },
                subtitle = { Text("Ano de serviço ${uiState.year.label}") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showExportMenu = true }) {
                            Icon(Icons.Default.FileDownload, contentDescription = "Exportar S-13")
                        }
                        DropdownMenu(expanded = showExportMenu, onDismissRequest = { showExportMenu = false }) {
                            DropdownMenuItem(text = { Text("S-13 em PDF") }, onClick = {
                                showExportMenu = false
                                viewModel.exportS13(ReportFormat.PDF)
                            })
                            DropdownMenuItem(text = { Text("S-13 em planilha (CSV)") }, onClick = {
                                showExportMenu = false
                                viewModel.exportS13(ReportFormat.CSV)
                            })
                        }
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (uiState.isLoading) {
            FullScreenLoading(Modifier.padding(padding))
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            if (isSyncing) {
                item(key = "sync") { LinearWavyProgressIndicator(Modifier.fillMaxWidth().padding(bottom = 8.dp)) }
            }
            item(key = "years") {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 12.dp)) {
                    items(viewModel.availableYears) { year ->
                        FilterChip(
                            selected = year == uiState.year,
                            onClick = { viewModel.setYear(year) },
                            label = { Text(year.label) },
                            shape = CircleShape
                        )
                    }
                }
            }
            uiState.coverage?.let { coverage ->
                item(key = "goal") {
                    GoalCard(coverage, uiState.staleCount) { viewModel.setSortOption(CoverageSortOption.OVER_12_MONTHS) }
                }
            }
            item(key = "tabs") {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
                    CoverageTab.entries.forEachIndexed { index, tab ->
                        SegmentedButton(
                            selected = uiState.tab == tab,
                            onClick = { viewModel.setTab(tab) },
                            shape = SegmentedButtonDefaults.itemShape(index, CoverageTab.entries.size)
                        ) {
                            Text(tab.label)
                        }
                    }
                }
            }
            when (uiState.tab) {
                CoverageTab.TERRITORIES -> {
                    item(key = "sort") {
                        FilterChip(
                            selected = true,
                            onClick = { showSortSheet = true },
                            label = { Text("${uiState.sortOption.label} (${uiState.coverages.size})") },
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                            shape = CircleShape,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    itemsIndexed(uiState.coverages, key = { _, it -> it.territory.id }) { index, coverage ->
                        CoverageRow(
                            coverage = coverage,
                            color = uiState.palette[coverage.territory.id] ?: MaterialTheme.colorScheme.primary,
                            shape = segmentedShape(index, uiState.coverages.size, outer = 20.dp),
                            onClick = { onNavigateToTerritory(coverage.territory.id) }
                        )
                    }
                }
                CoverageTab.CAMPAIGNS -> {
                    item(key = "new-campaign") {
                        FilledTonalButton(onClick = { showCampaignDialog = true }, modifier = Modifier.padding(bottom = 8.dp)) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Nova campanha")
                        }
                    }
                    items(uiState.campaigns, key = { it.campaign.id }) { coverage ->
                        CampaignCard(
                            coverage = coverage,
                            palette = uiState.palette,
                            onTerritoryClick = onNavigateToTerritory,
                            onDelete = { pendingCampaignDelete = coverage.campaign },
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }
                    if (uiState.campaigns.isEmpty()) {
                        item {
                            Text(
                                "Crie uma campanha (ex.: Celebração) para acompanhar quais territórios foram trabalhados no período.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                CoverageTab.RESPONSIBLES -> {
                    itemsIndexed(uiState.responsibles, key = { _, it -> it.responsibleId ?: it.name }) { index, stats ->
                        ResponsibleRow(stats, segmentedShape(index, uiState.responsibles.size, outer = 20.dp))
                    }
                    if (uiState.responsibles.isEmpty()) {
                        item { Text("Nenhuma designação registrada.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun GoalCard(coverage: ServiceYearCoverage, staleCount: Int, onStaleClick: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.primaryContainer) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    CircularWavyProgressIndicator(
                        progress = { coverage.percent / 100f },
                        modifier = Modifier.size(96.dp)
                    )
                    Text("${coverage.percent}%", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "${coverage.worked.size} de ${coverage.total}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        "territórios concluídos no ano de serviço",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        "${coverage.pending.size} pendentes",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            if (staleCount > 0) {
                Surface(
                    onClick = onStaleClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                        Text(
                            "$staleCount sem conclusão há mais de 12 meses",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CoverageRow(coverage: TerritoryCoverage, color: Color, shape: RoundedCornerShape, onClick: () -> Unit) {
    val last = coverage.lastCompletedDate
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = shape, color = MaterialTheme.colorScheme.surfaceContainer) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            TerritoryShapeBadge(coverage.territory.code, color, size = 44.dp)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(coverage.territory.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    if (last == null) "Nunca concluído" else "Última conclusão: ${DateFormats.fullDate().format(last)} (${monthsAgo(last)})",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (last == null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
                val details = listOfNotNull(
                    "${coverage.completionCount}× concluído",
                    coverage.averageDaysToComplete?.let { "média de ${it.roundToInt()} dias" }
                )
                Text(details.joinToString(" · "), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (coverage.completedInYear) {
                Icon(Icons.Default.CheckCircle, contentDescription = "Concluído no ano", tint = Color(0xFF4CAF50))
            }
        }
    }
}

@Composable
private fun ResponsibleRow(stats: ResponsibleStats, shape: RoundedCornerShape) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = shape, color = MaterialTheme.colorScheme.surfaceContainer) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            UserAvatarByName(name = stats.name, size = 40.dp, userId = stats.responsibleId)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stats.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    listOfNotNull(
                        "${stats.completed} de ${stats.assignments} concluídos",
                        stats.averageDaysToComplete?.let { "média de ${it.roundToInt()} dias" }
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (stats.currentTerritories > 0) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
                    Text(
                        "${stats.currentTerritories} atual${if (stats.currentTerritories > 1) "is" else ""}",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

private fun monthsAgo(date: Date): String {
    val days = TimeUnit.MILLISECONDS.toDays(Date().time - date.time)
    return when {
        days < 31 -> "há $days dias"
        days < 365 -> "há ${days / 30} meses"
        else -> "há ${days / 365} ano${if (days >= 730) "s" else ""}"
    }
}
