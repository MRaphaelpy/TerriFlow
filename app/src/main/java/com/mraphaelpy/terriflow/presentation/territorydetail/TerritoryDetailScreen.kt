package com.mraphaelpy.terriflow.presentation.territorydetail

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mraphaelpy.terriflow.domain.model.Territory
import com.mraphaelpy.terriflow.domain.model.TerritoryAction
import com.mraphaelpy.terriflow.domain.model.TerritoryStatus
import com.mraphaelpy.terriflow.domain.model.label
import com.mraphaelpy.terriflow.presentation.components.ConfirmDialog
import com.mraphaelpy.terriflow.presentation.components.DestructiveDialog
import com.mraphaelpy.terriflow.presentation.components.FullScreenLoading
import com.mraphaelpy.terriflow.presentation.components.StatusChip

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerritoryDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (String) -> Unit = {},
    onNavigateToMap: (String) -> Unit = {},
    viewModel: TerritoryDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var showAssignDialog by remember { mutableStateOf(false) }
    var showCompleteDialog by remember { mutableStateOf(false) }
    var showReturnDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) onNavigateBack()
    }
    LaunchedEffect(uiState.actionSuccess) {
        uiState.actionSuccess?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    if (showAssignDialog) {
        AssignDialog(
            responsibles = uiState.responsibles.filter { it.id != uiState.territory?.currentResponsibleId },
            rotationInfos = uiState.rotationInfo,
            onAssign = { id ->
                viewModel.assign(id)
                showAssignDialog = false
            },
            onDismiss = { showAssignDialog = false }
        )
    }

    if (showCompleteDialog) {
        CompleteDialog(
            responsibles = uiState.responsibles.filter { it.id != uiState.territory?.currentResponsibleId },
            rotationInfos = uiState.rotationInfo,
            onComplete = { nextId ->
                viewModel.complete(nextId)
                showCompleteDialog = false
            },
            onDismiss = { showCompleteDialog = false }
        )
    }

    if (showReturnDialog) {
        ConfirmDialog(
            title = "Devolver território",
            text = "Deseja devolver este território? Ele voltará ao status disponível e poderá ser atribuído a outro dirigente.",
            confirmLabel = "Devolver",
            icon = Icons.AutoMirrored.Filled.Undo,
            onConfirm = {
                viewModel.returnTerritory()
                showReturnDialog = false
            },
            onDismiss = { showReturnDialog = false }
        )
    }

    if (showDeleteDialog) {
        DestructiveDialog(
            title = "Apagar território",
            text = "Esta ação não pode ser desfeita. O território será removido permanentemente de todos os dispositivos.",
            confirmLabel = "Apagar",
            onConfirm = {
                viewModel.delete()
                showDeleteDialog = false
            },
            onDismiss = { showDeleteDialog = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.territory?.code ?: "Território") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    uiState.territory?.let { t ->
                        if (t.hasLocation) {
                            IconButton(onClick = { onNavigateToMap(t.id) }) {
                                Icon(Icons.Default.Map, contentDescription = "Ver no mapa")
                            }
                        }
                        if (uiState.can(TerritoryAction.DELETE)) {
                            IconButton(onClick = { showDeleteDialog = true }) {
                                Icon(Icons.Default.Delete, contentDescription = "Apagar", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                        if (uiState.can(TerritoryAction.EDIT)) {
                            IconButton(onClick = { onNavigateToEdit(t.id) }) {
                                Icon(Icons.Default.Edit, contentDescription = "Editar")
                            }
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (uiState.isLoading && uiState.territory == null) {
            FullScreenLoading(modifier = Modifier.padding(padding))
            return@Scaffold
        }

        val territory = uiState.territory ?: return@Scaffold

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TerritoryInfoCard(territory, onViewMap = { onNavigateToMap(territory.id) })
            TerritoryMiniMap(
                lat = territory.latitude,
                lng = territory.longitude,
                label = "${territory.code} — ${territory.name}",
                status = territory.status,
                boundaryPoints = territory.boundaryPoints,
                blockPolygons = territory.blockPolygons,
                onViewMap = { onNavigateToMap(territory.id) }
            )
            ActionButtons(
                uiState,
                onAssign = { showAssignDialog = true },
                onStart = { viewModel.start() },
                onPause = { viewModel.pause() },
                onComplete = { showCompleteDialog = true },
                onReturn = { showReturnDialog = true }
            )
            if (uiState.isAdmin) {
                RotationHistoryCard(
                    pastWorkers = uiState.pastWorkers,
                    suggestedWorkers = uiState.suggestedWorkers
                )
                Text(
                    "Histórico (Auditoria)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                uiState.historyCycles.forEach { cycle ->
                    HistoryCycleCard(cycle)
                }
            }
        }
    }
}

@Composable
private fun TerritoryInfoCard(territory: Territory, onViewMap: () -> Unit = {}) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
            .heightIn(180.dp)
        ,
        shape = RoundedCornerShape(5.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = territory.code,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = territory.name,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f).padding(end = 12.dp)
                )
                StatusChip(territory.status)
            }

            if (territory.location.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(territory.location, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            if (territory.description.isNotEmpty()) {
                Text(territory.description, style = MaterialTheme.typography.bodyMedium)
            }

            territory.currentResponsibleName?.let { name ->
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    com.mraphaelpy.terriflow.presentation.components.UserAvatarByName(name = name, photoUrl = territory.currentResponsiblePhotoUrl, size = 32.dp)
                    Column {
                        Text("Responsável atual", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        Text(name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (territory.notes.isNotEmpty()) {
                HorizontalDivider()
                Text(territory.notes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ActionButtons(
    uiState: TerritoryDetailUiState,
    onAssign: () -> Unit,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onComplete: () -> Unit,
    onReturn: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.animateContentSize(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        )
    ) {
        if (uiState.can(TerritoryAction.ASSIGN)) {
            OutlinedButton(onClick = onAssign, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.PersonAdd, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Atribuir território")
            }
        }
        if (uiState.can(TerritoryAction.START)) {
            Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (uiState.territory?.status == TerritoryStatus.PAUSED) "Retomar trabalho" else "Iniciar trabalho")
            }
        }
        if (uiState.can(TerritoryAction.PAUSE)) {
            OutlinedButton(onClick = onPause, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Pause, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Pausar trabalho")
            }
        }
        if (uiState.can(TerritoryAction.COMPLETE)) {
            Button(
                onClick = onComplete,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Icon(Icons.Default.TaskAlt, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Finalizar território")
            }
        }
        if (uiState.can(TerritoryAction.RETURN)) {
            OutlinedButton(
                onClick = onReturn,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Devolver território")
            }
        }
    }
}
