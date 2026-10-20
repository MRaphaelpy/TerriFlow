package com.mraphaelpy.terriflow.presentation.territories

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Hexagon
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mraphaelpy.terriflow.presentation.components.ErrorDialog
import com.mraphaelpy.terriflow.presentation.components.FullScreenLoadingWithMessage
import com.mraphaelpy.terriflow.presentation.components.LoadingButton
import com.mraphaelpy.terriflow.presentation.components.segmentedShape

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ImportKmlScreen(
    onNavigateBack: () -> Unit,
    onImportDone: () -> Unit,
    viewModel: ImportKmlViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) viewModel.parseFile(uri)
    }
    val pickFile = { filePicker.launch("*/*") }
    val hasItems = uiState.items.isNotEmpty() && !uiState.isDone

    uiState.error?.let { ErrorDialog(message = it, onDismiss = viewModel::clearError) }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("Importar KML / KMZ") },
                subtitle = { if (hasItems) Text(uiState.fileName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    if (hasItems) {
                        IconButton(onClick = pickFile) { Icon(Icons.Default.FolderOpen, contentDescription = "Escolher outro arquivo") }
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        bottomBar = {
            if (hasItems && !uiState.isImporting) {
                LoadingButton(
                    text = when (uiState.selectedCount) {
                        0 -> "Selecione territórios"
                        1 -> "Importar 1 território"
                        else -> "Importar ${uiState.selectedCount} territórios"
                    },
                    isLoading = false,
                    enabled = uiState.selectedCount > 0,
                    onClick = viewModel::import,
                    leadingIcon = Icons.Default.Upload,
                    height = 56.dp,
                    modifier = Modifier.navigationBarsPadding().padding(16.dp)
                )
            }
        }
    ) { padding ->
        val contentModifier = Modifier.padding(padding)
        when {
            uiState.isImporting -> FullScreenLoadingWithMessage("Importando territórios…", contentModifier)
            uiState.isParsing -> FullScreenLoadingWithMessage("Lendo arquivo…", contentModifier)
            uiState.isDone -> CenteredMessage(
                icon = Icons.Default.CheckCircle,
                title = if (uiState.importedCount == 1) "1 território importado" else "${uiState.importedCount} territórios importados",
                message = "Eles já aparecem na lista e no mapa. Agora é só atribuir aos dirigentes.",
                modifier = contentModifier
            ) { Button(onClick = onImportDone) { Text("Ir para territórios") } }
            uiState.items.isEmpty() -> CenteredMessage(
                icon = Icons.Default.UploadFile,
                title = "Importe do Google My Maps",
                message = "No My Maps, abra o mapa, toque em ⋮ › Exportar para KML/KMZ e escolha o arquivo aqui. " +
                    "Cada polígono vira um território e as quadras são reconhecidas automaticamente.",
                modifier = contentModifier
            ) {
                Button(onClick = pickFile) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Selecionar arquivo")
                }
            }
            else -> ImportList(uiState, viewModel, contentModifier)
        }
    }
}

@Composable
private fun ImportList(uiState: ImportKmlUiState, viewModel: ImportKmlViewModel, modifier: Modifier) {
    val allSelected = uiState.items.all { it.isSelected }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        item(key = "summary") {
            Row(modifier = Modifier.fillMaxWidth().padding(start = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "${uiState.items.size} encontrados · ${uiState.selectedCount} selecionados",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (uiState.duplicateCount > 0) {
                        Text(
                            "${uiState.duplicateCount} já existem e foram desmarcados",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                TextButton(onClick = { viewModel.toggleAll(!allSelected) }) {
                    Text(if (allSelected) "Desmarcar todos" else "Marcar todos")
                }
            }
        }
        itemsIndexed(uiState.items) { index, item ->
            ImportRow(item, segmentedShape(index, uiState.items.size)) { viewModel.toggleItem(index) }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ImportRow(item: ImportItem, shape: Shape, onToggle: () -> Unit) {
    val parsed = item.parsed
    val isPolygon = parsed.blockPolygons.isNotEmpty() || parsed.boundaryPoints.size >= 3
    Surface(
        onClick = onToggle,
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        color = if (item.isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Checkbox(checked = item.isSelected, onCheckedChange = { onToggle() })
            Box(
                modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.tertiaryContainer, MaterialShapes.Cookie6Sided.toShape()),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isPolygon) Icons.Default.Hexagon else Icons.Default.LocationOn,
                    contentDescription = if (isPolygon) "Polígono" else "Ponto",
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f).padding(start = 4.dp)) {
                Text(
                    parsed.name.ifBlank { "(sem nome)" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    when {
                        parsed.blockPolygons.isNotEmpty() -> "${parsed.blockPolygons.size} quadras"
                        parsed.boundaryPoints.size >= 3 -> "Contorno com ${parsed.boundaryPoints.size} pontos"
                        else -> "Apenas um ponto"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                if (item.alreadyExists) {
                    Text("Já existe um território com este nome", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CenteredMessage(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    action: @Composable () -> Unit
) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(120.dp).background(MaterialTheme.colorScheme.primaryContainer, MaterialShapes.Cookie12Sided.toShape()),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Spacer(Modifier.height(24.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        action()
    }
}
