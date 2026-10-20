package com.mraphaelpy.terriflow.presentation.territorydetail

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mraphaelpy.terriflow.core.util.DateFormats
import com.mraphaelpy.terriflow.domain.model.Territory
import com.mraphaelpy.terriflow.domain.model.TerritoryAction
import com.mraphaelpy.terriflow.domain.model.TerritoryStatus
import com.mraphaelpy.terriflow.domain.model.isMapped
import com.mraphaelpy.terriflow.domain.model.label
import com.mraphaelpy.terriflow.presentation.components.ConfirmDialog
import com.mraphaelpy.terriflow.presentation.components.DestructiveDialog
import com.mraphaelpy.terriflow.presentation.components.FullScreenLoading
import com.mraphaelpy.terriflow.presentation.components.TerritoryShapeBadge
import com.mraphaelpy.terriflow.presentation.components.UserAvatarByName
import com.mraphaelpy.terriflow.presentation.components.statusColor

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TerritoryDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (String) -> Unit = {},
    onNavigateToMap: (String) -> Unit = {},
    viewModel: TerritoryDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    var showAssignDialog by remember { mutableStateOf(false) }
    var showCompleteDialog by remember { mutableStateOf(false) }
    var showReturnDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showOverflow by remember { mutableStateOf(false) }

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

    val territory = uiState.territory

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(territory?.name ?: "Território", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                subtitle = { territory?.let { Text(it.code) } },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    if (territory == null) return@LargeFlexibleTopAppBar
                    if (territory.isMapped) {
                        IconButton(onClick = { onNavigateToMap(territory.id) }) {
                            Icon(Icons.Default.Map, contentDescription = "Ver no mapa")
                        }
                    }
                    if (uiState.can(TerritoryAction.EDIT)) {
                        IconButton(onClick = { onNavigateToEdit(territory.id) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Editar")
                        }
                    }
                    if (uiState.can(TerritoryAction.DELETE) || uiState.can(TerritoryAction.REMIND)) {
                        Box {
                            IconButton(onClick = { showOverflow = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Mais opções")
                            }
                            DropdownMenu(expanded = showOverflow, onDismissRequest = { showOverflow = false }) {
                                if (uiState.can(TerritoryAction.REMIND)) {
                                    DropdownMenuItem(
                                        text = { Text("Cobrar devolução") },
                                        leadingIcon = { Icon(Icons.Default.NotificationsActive, contentDescription = null) },
                                        onClick = {
                                            showOverflow = false
                                            viewModel.remind()
                                        }
                                    )
                                }
                                if (uiState.can(TerritoryAction.DELETE)) {
                                    DropdownMenuItem(
                                        text = { Text("Apagar território", color = MaterialTheme.colorScheme.error) },
                                        leadingIcon = {
                                            Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                        },
                                        onClick = {
                                            showOverflow = false
                                            showDeleteDialog = true
                                        }
                                    )
                                }
                            }
                        }
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (territory == null) {
            if (uiState.isLoading) FullScreenLoading(modifier = Modifier.padding(padding))
            return@Scaffold
        }

        val color = uiState.territoryColor ?: statusColor(territory.status)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp, bottom = 24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            HeroCard(territory = territory, color = color, isMine = territory.currentResponsibleId == uiState.currentUser?.id)

            TerritoryActions(
                uiState = uiState,
                onAction = { action ->
                    when (action) {
                        TerritoryAction.ASSIGN -> showAssignDialog = true
                        TerritoryAction.START -> viewModel.start()
                        TerritoryAction.PAUSE -> viewModel.pause()
                        TerritoryAction.COMPLETE -> showCompleteDialog = true
                        TerritoryAction.RETURN -> showReturnDialog = true
                        else -> Unit
                    }
                }
            )

            TerritoryInfoGroup(territory)

            TerritoryMiniMap(
                lat = territory.latitude,
                lng = territory.longitude,
                label = "${territory.code} — ${territory.name}",
                color = color,
                boundaryPoints = territory.boundaryPoints,
                blockPolygons = territory.blockPolygons,
                completedBlocks = territory.completedBlocks,
                onViewMap = { onNavigateToMap(territory.id) }
            )

            if (territory.hasBlocks) {
                BlockProgressSection(
                    territory = territory,
                    color = color,
                    canMark = uiState.can(TerritoryAction.MARK_BLOCK),
                    onToggle = viewModel::toggleBlock
                )
            }

            if (uiState.canAccessNotes) {
                TerritoryNotesSection(
                    notes = uiState.notes,
                    canDelete = uiState::canDelete,
                    onAdd = viewModel::addNote,
                    onDelete = viewModel::deleteNote
                )
            }

            if (uiState.isAdmin) {
                RotationHistoryCard(
                    pastWorkers = uiState.pastWorkers,
                    suggestedWorkers = uiState.suggestedWorkers
                )
                if (uiState.historyCycles.isNotEmpty()) {
                    SectionTitle("Histórico")
                    uiState.historyCycles.forEach { cycle -> HistoryCycleCard(cycle) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun HeroCard(territory: Territory, color: Color, isMine: Boolean) {
    val statusTint = statusColor(territory.status)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                TerritoryShapeBadge(code = territory.code, color = color, size = 72.dp)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(shape = CircleShape, color = statusTint.copy(alpha = 0.18f)) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(Modifier.size(8.dp).background(statusTint, CircleShape))
                            Text(territory.status.label(), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    val since = statusSince(territory)
                    if (since.isNotEmpty()) {
                        Text(since, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = if (isMine) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val name = territory.currentResponsibleName
                    if (name != null) {
                        UserAvatarByName(name = name, photoUrl = territory.currentResponsiblePhotoUrl, size = 40.dp, userId = territory.currentResponsibleId)
                    } else {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                        }
                    }
                    Column {
                        Text("Dirigente", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            when {
                                name == null -> "Sem dirigente"
                                isMine -> "$name (você)"
                                else -> name
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TerritoryActions(uiState: TerritoryDetailUiState, onAction: (TerritoryAction) -> Unit) {
    val layout = actionLayout(uiState.allowedActions)
    if (layout.primary == null && layout.secondary.isEmpty()) return
    val enabled = !uiState.isPerformingAction
    val isPaused = uiState.territory?.status == TerritoryStatus.PAUSED

    Column(
        modifier = Modifier.animateContentSize(MaterialTheme.motionScheme.defaultSpatialSpec()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        layout.primary?.let { action ->
            val height = ButtonDefaults.MediumContainerHeight
            Button(
                onClick = { onAction(action) },
                shapes = ButtonDefaults.shapesFor(height),
                enabled = enabled,
                contentPadding = ButtonDefaults.contentPaddingFor(height),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = height)
            ) {
                Icon(actionIcon(action), contentDescription = null, modifier = Modifier.size(ButtonDefaults.iconSizeFor(height)))
                Spacer(Modifier.size(ButtonDefaults.iconSpacingFor(height)))
                Text(actionLabel(action, isPaused), style = ButtonDefaults.textStyleFor(height))
            }
        }
        if (layout.secondary.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                layout.secondary.forEach { action ->
                    SecondaryActionButton(action, isPaused, enabled) { onAction(action) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RowScope.SecondaryActionButton(
    action: TerritoryAction,
    isPaused: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val modifier = Modifier
        .weight(1f)
        .heightIn(min = ButtonDefaults.MinHeight)
    val content: @Composable RowScope.() -> Unit = {
        Icon(actionIcon(action), contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
        Text(actionLabel(action, isPaused), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
    if (action == TerritoryAction.RETURN) {
        OutlinedButton(
            onClick = onClick,
            shapes = ButtonDefaults.shapes(),
            enabled = enabled,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            modifier = modifier,
            content = content
        )
    } else {
        FilledTonalButton(
            onClick = onClick,
            shapes = ButtonDefaults.shapes(),
            enabled = enabled,
            modifier = modifier,
            content = content
        )
    }
}

private fun actionIcon(action: TerritoryAction): ImageVector = when (action) {
    TerritoryAction.ASSIGN -> Icons.Default.PersonAdd
    TerritoryAction.START -> Icons.Default.PlayArrow
    TerritoryAction.PAUSE -> Icons.Default.Pause
    TerritoryAction.COMPLETE -> Icons.Default.TaskAlt
    TerritoryAction.RETURN -> Icons.AutoMirrored.Filled.Undo
    else -> Icons.Default.Edit
}

private fun actionLabel(action: TerritoryAction, isPaused: Boolean): String = when (action) {
    TerritoryAction.ASSIGN -> "Atribuir"
    TerritoryAction.START -> if (isPaused) "Retomar trabalho" else "Iniciar trabalho"
    TerritoryAction.PAUSE -> "Pausar"
    TerritoryAction.COMPLETE -> "Finalizar"
    TerritoryAction.RETURN -> "Devolver"
    else -> action.label()
}

private data class InfoEntry(val icon: ImageVector, val label: String, val value: String)

@Composable
private fun TerritoryInfoGroup(territory: Territory) {
    val formatter = DateFormats.fullDate()
    val entries = buildList {
        if (territory.location.isNotBlank()) add(InfoEntry(Icons.Default.LocationOn, "Localização", territory.location))
        if (territory.description.isNotBlank()) add(InfoEntry(Icons.Default.Description, "Descrição", territory.description))
        if (territory.notes.isNotBlank()) add(InfoEntry(Icons.AutoMirrored.Filled.Notes, "Observações", territory.notes))
        territory.assignedAt?.let { add(InfoEntry(Icons.Default.CalendarMonth, "Atribuído em", formatter.format(it))) }
        territory.startedAt?.let { add(InfoEntry(Icons.Default.PlayArrow, "Iniciado em", formatter.format(it))) }
        territory.completedAt?.let { add(InfoEntry(Icons.Default.TaskAlt, "Finalizado em", formatter.format(it))) }
    }
    if (entries.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        entries.forEachIndexed { index, entry ->
            val top = if (index == 0) 24.dp else 6.dp
            val bottom = if (index == entries.lastIndex) 24.dp else 6.dp
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom),
                color = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(entry.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Column {
                        Text(entry.label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(entry.value, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}

@Composable
internal fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp)
    )
}
