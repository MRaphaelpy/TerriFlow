package com.mraphaelpy.terriflow.presentation.territorydetail

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mraphaelpy.terriflow.core.util.DateFormats
import com.mraphaelpy.terriflow.core.util.MapColors
import com.mraphaelpy.terriflow.domain.model.Territory
import com.mraphaelpy.terriflow.domain.model.TerritoryEvent
import com.mraphaelpy.terriflow.domain.model.TerritoryStatus
import com.mraphaelpy.terriflow.domain.model.User
import com.mraphaelpy.terriflow.domain.model.label
import com.mraphaelpy.terriflow.presentation.components.ConfirmDialog
import com.mraphaelpy.terriflow.presentation.components.DestructiveDialog
import com.mraphaelpy.terriflow.presentation.components.FullScreenLoading
import com.mraphaelpy.terriflow.presentation.components.StatusChip
import com.mraphaelpy.terriflow.presentation.components.eventIcon
import com.mraphaelpy.terriflow.presentation.map.openInGoogleMaps
import com.mraphaelpy.terriflow.presentation.map.statusColor
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon

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

    LaunchedEffect(uiState.actionSuccess) {
        uiState.actionSuccess?.let {
            if (it == "deleted") {
                onNavigateBack()
                return@let
            }
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
                viewModel.deleteTerritory()
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
                        if (uiState.isAdmin) {
                            IconButton(onClick = { showDeleteDialog = true }) {
                                Icon(Icons.Default.Delete, contentDescription = "Apagar", tint = MaterialTheme.colorScheme.error)
                            }
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

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { TerritoryInfoCard(territory, onViewMap = { onNavigateToMap(territory.id) }) }
            item {
                TerritoryMiniMap(
                    lat = territory.latitude,
                    lng = territory.longitude,
                    label = "${territory.code} — ${territory.name}",
                    status = territory.status,
                    boundaryPoints = territory.boundaryPoints,
                    blockPolygons = territory.blockPolygons,
                    onViewMap = { onNavigateToMap(territory.id) }
                )
            }
            item { ActionButtons(uiState, territory, onAssign = { showAssignDialog = true }, onStart = { viewModel.start() }, onPause = { viewModel.pause() }, onComplete = { showCompleteDialog = true }, onReturn = { showReturnDialog = true }) }
            if (uiState.isAdmin) {
                item {
                    Text("Histórico (Auditoria)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
                items(uiState.historyCycles) { cycle ->
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
    territory: Territory,
    onAssign: () -> Unit,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onComplete: () -> Unit,
    onReturn: () -> Unit
) {
    val currentUserId = uiState.currentUser?.id
    val isResponsible = currentUserId == territory.currentResponsibleId
    val isAdmin = uiState.isAdmin

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.animateContentSize(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        )
    ) {
        if (isAdmin && (territory.status == TerritoryStatus.AVAILABLE || territory.status == TerritoryStatus.RETURNED || territory.status == TerritoryStatus.COMPLETED)) {
            OutlinedButton(onClick = onAssign, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.PersonAdd, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Atribuir território")
            }
        }
        if ((isResponsible || isAdmin) && (territory.status == TerritoryStatus.ASSIGNED || territory.status == TerritoryStatus.PAUSED)) {
            Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (territory.status == TerritoryStatus.PAUSED) "Retomar trabalho" else "Iniciar trabalho")
            }
        }
        if ((isResponsible || isAdmin) && territory.status == TerritoryStatus.IN_PROGRESS) {
            OutlinedButton(onClick = onPause, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Pause, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Pausar trabalho")
            }
        }
        if ((isResponsible || isAdmin) && (territory.status == TerritoryStatus.IN_PROGRESS || territory.status == TerritoryStatus.PAUSED)) {
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
        if ((isResponsible || isAdmin) && territory.status != TerritoryStatus.AVAILABLE && territory.status != TerritoryStatus.RETURNED && territory.status != TerritoryStatus.ARCHIVED) {
            OutlinedButton(
                onClick = onReturn,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.Undo, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Devolver território")
            }
        }
    }
}

@Composable
private fun AssignDialog(
    responsibles: List<User>,
    onAssign: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var selected by remember { mutableStateOf<User?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Atribuir território", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Selecione o novo dirigente:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(16.dp))
                
                Box(modifier = Modifier.weight(1f, fill = false)) {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(responsibles) { user ->
                            Surface(
                                onClick = { selected = user },
                                shape = MaterialTheme.shapes.medium,
                                color = if (selected?.id == user.id) MaterialTheme.colorScheme.primaryContainer else androidx.compose.ui.graphics.Color.Transparent,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp).fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(selected = selected?.id == user.id, onClick = { selected = user })
                                    Spacer(Modifier.width(8.dp))
                                    com.mraphaelpy.terriflow.presentation.components.UserAvatar(user = user, size = 36.dp)
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        text = user.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = if (selected?.id == user.id) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { selected?.let { onAssign(it.id) } },
                enabled = selected != null
            ) { Text("Atribuir") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
private fun CompleteDialog(
    responsibles: List<User>,
    onComplete: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    var nextResponsible by remember { mutableStateOf<User?>(null) }
    var selectNone by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Finalizar território", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Deseja transferir o território para outro dirigente?", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(16.dp))
                
                Box(modifier = Modifier.weight(1f, fill = false)) {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            Surface(
                                onClick = { 
                                    selectNone = true
                                    nextResponsible = null 
                                },
                                shape = MaterialTheme.shapes.medium,
                                color = if (selectNone) MaterialTheme.colorScheme.primaryContainer else androidx.compose.ui.graphics.Color.Transparent,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp).fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(selected = selectNone, onClick = { 
                                        selectNone = true
                                        nextResponsible = null
                                    })
                                    Spacer(Modifier.width(8.dp))
                                    Surface(
                                        shape = androidx.compose.foundation.shape.CircleShape,
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(20.dp))
                                        }
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        text = "Nenhum (Apenas finalizar)",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = if (selectNone) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                        
                        items(responsibles) { user ->
                            Surface(
                                onClick = { 
                                    selectNone = false
                                    nextResponsible = user 
                                },
                                shape = MaterialTheme.shapes.medium,
                                color = if (nextResponsible?.id == user.id && !selectNone) MaterialTheme.colorScheme.primaryContainer else androidx.compose.ui.graphics.Color.Transparent,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp).fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(selected = nextResponsible?.id == user.id && !selectNone, onClick = { 
                                        selectNone = false
                                        nextResponsible = user 
                                    })
                                    Spacer(Modifier.width(8.dp))
                                    com.mraphaelpy.terriflow.presentation.components.UserAvatar(user = user, size = 36.dp)
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        text = user.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = if (nextResponsible?.id == user.id && !selectNone) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onComplete(nextResponsible?.id) }) { Text("Confirmar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
private fun HistoryCycleCard(cycle: HistoryCycle) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 16.dp)) {
                com.mraphaelpy.terriflow.presentation.components.UserAvatarByName(name = cycle.responsibleName, size = 40.dp)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Dirigente",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = cycle.responsibleName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            
            cycle.events.forEachIndexed { index, event ->
                TimelineEventItem(event)
                if (index < cycle.events.size - 1) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

@Composable
private fun TimelineEventItem(event: TerritoryEvent) {
    val formatter = DateFormats.fullDateTime()
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = eventIcon(event.type),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
        Column {
            Text(event.type.label(), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                com.mraphaelpy.terriflow.presentation.components.UserAvatarByName(name = event.userName, size = 16.dp)
                Text(event.userName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("•", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(formatter.format(event.timestamp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            event.extra["toUserName"]?.let { to ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.secondary)
                    Text("Para: $to", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun TerritoryMiniMap(
    lat: Double?,
    lng: Double?,
    label: String,
    status: TerritoryStatus,
    boundaryPoints: List<com.mraphaelpy.terriflow.domain.model.LatLng> = emptyList(),
    blockPolygons: List<List<com.mraphaelpy.terriflow.domain.model.LatLng>> = emptyList(),
    onViewMap: () -> Unit
) {
    val context = LocalContext.current
    val markerColor = statusColor(status).toArgb()
    val hasLocation = lat != null && lng != null
    val hasBoundary = boundaryPoints.size >= 3
    val hasBlocks = blockPolygons.isNotEmpty()
    var showBlocks by remember { mutableStateOf(hasBlocks) }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        AndroidView(
            modifier = Modifier.fillMaxWidth().height(260.dp),
            factory = { ctx ->
                Configuration.getInstance().userAgentValue = ctx.packageName
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    val centerLat = lat ?: boundaryPoints.firstOrNull()?.lat
                        ?: blockPolygons.firstOrNull()?.firstOrNull()?.lat ?: -23.5505
                    val centerLng = lng ?: boundaryPoints.firstOrNull()?.lng
                        ?: blockPolygons.firstOrNull()?.firstOrNull()?.lng ?: -46.6333
                    controller.setZoom(16.0)
                    controller.setCenter(GeoPoint(centerLat, centerLng))
                }
            },
            update = { mapView ->
                mapView.overlays.clear()

                if (hasLocation && lat != null && lng != null) {
                    val pinDrawable = android.graphics.drawable.GradientDrawable().apply {
                        shape = android.graphics.drawable.GradientDrawable.OVAL
                        setColor(markerColor)
                        setSize(40, 40)
                        setStroke(5, android.graphics.Color.WHITE)
                    }
                    mapView.overlays.add(Marker(mapView).apply {
                        position = GeoPoint(lat, lng)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        icon = pinDrawable
                        title = label
                    })
                }

                val zoomPoints: List<com.mraphaelpy.terriflow.domain.model.LatLng> = when {
                    showBlocks && blockPolygons.isNotEmpty() -> blockPolygons.flatten()
                    hasBoundary -> boundaryPoints
                    else -> emptyList()
                }

                if (showBlocks && blockPolygons.isNotEmpty()) {
                    blockPolygons.forEach { block ->
                        val poly = Polygon().apply {
                            val pts = block.map { GeoPoint(it.lat, it.lng) }.toMutableList()
                            if (pts.isNotEmpty()) pts.add(pts.first())
                            points = pts
                            fillPaint.color = MapColors.BLOCK_FILL
                            outlinePaint.color = MapColors.OUTLINE
                            outlinePaint.strokeWidth = 2f
                        }
                        mapView.overlays.add(0, poly)
                    }
                } else if (hasBoundary) {
                    val poly = Polygon().apply {
                        val pts = boundaryPoints.map { GeoPoint(it.lat, it.lng) }.toMutableList()
                        pts.add(pts.first())
                        points = pts
                        fillPaint.color = MapColors.TERRITORY_FILL
                        outlinePaint.color = MapColors.OUTLINE
                        outlinePaint.strokeWidth = 3f
                    }
                    mapView.overlays.add(0, poly)
                }

                if (zoomPoints.size >= 2) {
                    val bbox = BoundingBox(
                        zoomPoints.maxOf { it.lat },
                        zoomPoints.maxOf { it.lng },
                        zoomPoints.minOf { it.lat },
                        zoomPoints.minOf { it.lng }
                    )
                    mapView.post { mapView.zoomToBoundingBox(bbox, false, 60) }
                } else if (hasLocation && lat != null && lng != null) {
                    mapView.controller.setZoom(17.0)
                    mapView.controller.setCenter(GeoPoint(lat, lng))
                }

                mapView.invalidate()
            }
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    if (hasLocation || hasBoundary) Icons.Default.LocationOn else Icons.Default.LocationOff,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = if (hasLocation || hasBoundary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    if (hasLocation || hasBoundary) "Localização no mapa" else "Sem localização definida",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (hasBlocks) {
                    FilterChip(
                        selected = showBlocks,
                        onClick = { showBlocks = !showBlocks },
                        label = { Text(if (showBlocks) "Quadras" else "Geral", style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.height(28.dp)
                    )
                    Spacer(Modifier.width(2.dp))
                }
                if (hasLocation || hasBoundary) {
                    IconButton(
                        onClick = {
                            val destLat = lat ?: boundaryPoints.firstOrNull()?.lat
                            val destLng = lng ?: boundaryPoints.firstOrNull()?.lng
                            openInGoogleMaps(context, destLat, destLng, label)
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Google Maps", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                }
                IconButton(onClick = onViewMap, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Fullscreen, contentDescription = "Ampliar mapa", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
