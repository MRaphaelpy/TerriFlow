package com.mraphaelpy.terriflow.presentation.coverage

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mraphaelpy.terriflow.domain.model.label
import com.mraphaelpy.terriflow.presentation.components.FullScreenLoading
import com.mraphaelpy.terriflow.core.util.DateFormats
import java.util.Date
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoverageScreen(
    onNavigateBack: () -> Unit,
    onNavigateToTerritory: (String) -> Unit,
    viewModel: CoverageViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showSortMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Painel de Cobertura") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.Default.Sort, contentDescription = "Ordenar")
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Mais tempo sem trabalhar") },
                                onClick = { 
                                    viewModel.setSortOption(CoverageSortOption.OLDEST_COMPLETED_FIRST)
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Trabalhados recentemente") },
                                onClick = { 
                                    viewModel.setSortOption(CoverageSortOption.NEWEST_COMPLETED_FIRST)
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Nunca trabalhados") },
                                onClick = { 
                                    viewModel.setSortOption(CoverageSortOption.NEVER_WORKED)
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Mais trabalhados") },
                                onClick = { 
                                    viewModel.setSortOption(CoverageSortOption.MOST_WORKED)
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            FullScreenLoading(modifier = Modifier.padding(padding))
            return@Scaffold
        }

        if (uiState.coverages.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Nenhum território corresponde a este filtro.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = when(uiState.sortOption) {
                        CoverageSortOption.OLDEST_COMPLETED_FIRST -> "Listando do mais antigo para o mais recente"
                        CoverageSortOption.NEWEST_COMPLETED_FIRST -> "Listando do mais recente para o mais antigo"
                        CoverageSortOption.NEVER_WORKED -> "Territórios nunca concluídos"
                        CoverageSortOption.MOST_WORKED -> "Territórios mais vezes concluídos"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            
            items(uiState.coverages, key = { it.territory.id }) { coverage ->
                CoverageCard(
                    coverage = coverage,
                    onClick = { onNavigateToTerritory(coverage.territory.id) }
                )
            }
        }
    }
}

@Composable
private fun CoverageCard(
    coverage: TerritoryCoverage,
    onClick: () -> Unit
) {
    val formatter = DateFormats.fullDate()
    val now = Date().time
    val daysSinceCompleted = coverage.lastCompletedDate?.time?.let {
        TimeUnit.MILLISECONDS.toDays(now - it)
    }

    val isOverdue = daysSinceCompleted != null && daysSinceCompleted > 180
    val isNeverWorked = coverage.lastCompletedDate == null

    val containerColor = when {
        isOverdue -> MaterialTheme.colorScheme.errorContainer
        isNeverWorked -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.surfaceContainerLow
    }
    val onContainerColor = when {
        isOverdue -> MaterialTheme.colorScheme.onErrorContainer
        isNeverWorked -> MaterialTheme.colorScheme.onTertiaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }
    val accentColor = when {
        isOverdue -> MaterialTheme.colorScheme.error
        isNeverWorked -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
            .heightIn(140.dp)
        ,
        shape = RoundedCornerShape(5.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = coverage.territory.code,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = accentColor
                    )
                    Text(
                        text = coverage.territory.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = onContainerColor
                    )
                }
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = accentColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "${coverage.completionCount}x Feito",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = accentColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = onContainerColor.copy(alpha = 0.12f))
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (isOverdue || isNeverWorked) Icons.Default.Warning else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            "Última conclusão",
                            style = MaterialTheme.typography.labelSmall,
                            color = onContainerColor.copy(alpha = 0.6f)
                        )
                        Text(
                            text = if (coverage.lastCompletedDate != null) formatter.format(coverage.lastCompletedDate) else "Nunca",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = onContainerColor
                        )
                    }
                }
                if (daysSinceCompleted != null) {
                    Text(
                        text = "Há $daysSinceCompleted dias",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (isOverdue) MaterialTheme.colorScheme.error else onContainerColor.copy(alpha = 0.7f)
                    )
                }
            }

            if (coverage.historyPreview.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = onContainerColor.copy(alpha = 0.12f))
                Spacer(Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Default.History,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = onContainerColor.copy(alpha = 0.6f)
                    )
                    Text(
                        "Últimas atividades",
                        style = MaterialTheme.typography.labelSmall,
                        color = onContainerColor.copy(alpha = 0.6f)
                    )
                }
                Spacer(Modifier.height(6.dp))
                coverage.historyPreview.forEach { event ->
                    val eventDate = DateFormats.shortDate().format(event.timestamp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = androidx.compose.foundation.shape.CircleShape,
                            color = accentColor.copy(alpha = 0.5f),
                            modifier = Modifier.size(5.dp)
                        ) {}
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "$eventDate — ${event.type.label()} por ${event.userName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = onContainerColor.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}
