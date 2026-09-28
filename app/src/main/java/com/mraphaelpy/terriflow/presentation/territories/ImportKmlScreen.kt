package com.mraphaelpy.terriflow.presentation.territories

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Hexagon
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mraphaelpy.terriflow.presentation.components.EmptyState
import com.mraphaelpy.terriflow.presentation.components.ErrorDialog
import com.mraphaelpy.terriflow.presentation.components.FullScreenLoadingWithMessage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportKmlScreen(
    onNavigateBack: () -> Unit,
    onImportDone: () -> Unit,
    viewModel: ImportKmlViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) viewModel.parseFile(uri)
    }

    if (uiState.error != null) {
        ErrorDialog(
            message = uiState.error!!,
            onDismiss = { viewModel.clearError() }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Importar KML / KMZ") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    if (uiState.items.isNotEmpty() && !uiState.isDone) {
                        val allSelected = uiState.items.all { it.isSelected }
                        TextButton(onClick = { viewModel.toggleAll(!allSelected) }) {
                            Text(if (allSelected) "Desmarcar todos" else "Selecionar todos")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (uiState.items.isNotEmpty() && !uiState.isDone && !uiState.isImporting) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.import() },
                    icon = { Icon(Icons.Default.Upload, contentDescription = null) },
                    text = { Text("Importar ${uiState.selectedCount}") },
                    shape = MaterialTheme.shapes.extraLarge,
                    containerColor = if (uiState.selectedCount > 0)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    ) { padding ->

        when {
            uiState.isImporting -> {
                FullScreenLoadingWithMessage(
                    message = "Importando territórios...",
                    modifier = Modifier.padding(padding)
                )
            }

            uiState.isParsing -> {
                FullScreenLoadingWithMessage(
                    message = "Lendo arquivo...",
                    modifier = Modifier.padding(padding)
                )
            }

            uiState.isDone -> {
                EmptyState(
                    icon = Icons.Default.CheckCircle,
                    title = "${uiState.importedCount} territórios importados com sucesso!",
                    iconTint = MaterialTheme.colorScheme.primary,
                    action = {
                        Button(onClick = onImportDone, shape = MaterialTheme.shapes.extraLarge) {
                            Text("Ir para territórios")
                        }
                    },
                    modifier = Modifier.padding(padding)
                )
            }

            uiState.items.isEmpty() -> {
                EmptyState(
                    icon = Icons.Default.UploadFile,
                    title = "Selecione um arquivo KML ou KMZ",
                    subtitle = "Exporte seus territórios do Google My Maps como KML ou KMZ e importe aqui.",
                    action = {
                        Button(
                            onClick = { filePicker.launch("*/*") },
                            shape = MaterialTheme.shapes.extraLarge
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Selecionar arquivo")
                        }
                    },
                    modifier = Modifier.padding(padding)
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Column(modifier = Modifier.padding(bottom = 4.dp)) {
                            Text(
                                text = "${uiState.items.size} territórios encontrados",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = uiState.fileName,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    itemsIndexed(uiState.items) { index, item ->
                        ImportItemCard(
                            item = item,
                            onToggle = { viewModel.toggleItem(index) }
                        )
                    }

                    item { Spacer(Modifier.height(88.dp)) }
                }
            }
        }
    }
}

@Composable
private fun ImportItemCard(item: ImportItem, onToggle: () -> Unit) {
    val parsed = item.parsed
    Card(
        onClick = onToggle,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (item.isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Checkbox(
                checked = item.isSelected,
                onCheckedChange = { onToggle() }
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = parsed.name.ifBlank { "(sem nome)" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    if (parsed.blockPolygons.isNotEmpty()) {
                        Text(
                            text = "${parsed.blockPolygons.size} quadras",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else if (parsed.boundaryPoints.isNotEmpty()) {
                        Text(
                            text = "${parsed.boundaryPoints.size} pontos",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    parsed.centroid?.let { centroid ->
                        Text(
                            text = "%.4f, %.4f".format(centroid.lat, centroid.lng),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                }
                if (parsed.description.isNotBlank()) {
                    Text(
                        text = parsed.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            if (parsed.boundaryPoints.size >= 3) {
                Icon(
                    Icons.Default.Hexagon,
                    contentDescription = "Polígono",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            } else {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = "Ponto",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}
