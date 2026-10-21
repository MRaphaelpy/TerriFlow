package com.mraphaelpy.terriflow.presentation.map

import com.mraphaelpy.terriflow.presentation.navigation.floatingControlsBottomPadding
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mraphaelpy.terriflow.domain.model.center
import com.mraphaelpy.terriflow.presentation.components.PillSearchField
import com.mraphaelpy.terriflow.presentation.components.TerritoryFilterChips
import com.mraphaelpy.terriflow.presentation.components.statusColor
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    onNavigateBack: () -> Unit,
    onNavigateToTerritory: (String) -> Unit,
    highlightId: String? = null,
    viewModel: MapViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val mapState = remember { TerritoryMapState() }
    var selectedId by remember { mutableStateOf(highlightId) }
    var hasLocationPermission by remember { mutableStateOf(context.hasLocationPermission()) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        hasLocationPermission = result.values.any { it }
        if (!hasLocationPermission) {
            scope.launch { snackbarHostState.showSnackbar("Permita a localização para ver onde você está no mapa") }
        }
    }

    LaunchedEffect(Unit) {
        if (!hasLocationPermission) permissionLauncher.launch(LOCATION_PERMISSIONS)
    }

    LaunchedEffect(uiState.filter) {
        if (uiState.filter.isActive && uiState.visibleTerritories.isNotEmpty()) {
            mapState.fitTo(uiState.visibleTerritories)
        }
    }

    val palette = uiState.palette
    val selectedTerritory = uiState.mappedTerritories.firstOrNull { it.id == selectedId }

    if (selectedTerritory != null) {
        val distance = remember(selectedTerritory.id) {
            selectedTerritory.center?.let { mapState.distanceFromMe(it.lat, it.lng) }
        }
        ModalBottomSheet(onDismissRequest = { selectedId = null }) {
            TerritoryMapSheet(
                territory = selectedTerritory,
                territoryColor = palette[selectedTerritory.id] ?: statusColor(selectedTerritory.status),
                isMine = selectedTerritory.currentResponsibleId == uiState.currentUserId,
                distanceMeters = distance,
                onOpen = {
                    selectedId = null
                    if (highlightId == selectedTerritory.id) onNavigateBack() else onNavigateToTerritory(selectedTerritory.id)
                }
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AllTerritoriesMap(
            state = mapState,
            territories = uiState.visibleTerritories,
            colors = palette,
            highlightId = selectedId,
            currentUserId = uiState.currentUserId,
            locationEnabled = hasLocationPermission,
            onTerritoryClick = { selectedId = it }
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PillSearchField(
                query = uiState.filter.query,
                onQueryChange = viewModel::updateQuery,
                placeholder = "Buscar código, nome, rua ou responsável",
                trailingInfo = uiState.mappedTerritories.size.takeIf { it > 0 }?.let { total ->
                    val visible = uiState.visibleTerritories.size
                    if (visible == total) "$total" else "$visible/$total"
                },
                shadowElevation = 6.dp,
                leadingIcon = {
                    if (highlightId != null) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                        }
                    } else {
                        Icon(Icons.Default.Search, contentDescription = null)
                    }
                },
                modifier = Modifier.padding(horizontal = 12.dp)
            )
            TerritoryFilterChips(
                filter = uiState.filter,
                statusCounts = uiState.statusCounts,
                myCount = uiState.myCount,
                onAllClick = viewModel::clearStatus,
                onMineClick = viewModel::toggleOnlyMine,
                onStatusClick = viewModel::toggleStatus,
                elevated = true
            )
        }

        when {
            uiState.isLoading -> Unit
            uiState.mappedTerritories.isEmpty() -> MapMessageCard(
                icon = Icons.Default.LocationOff,
                title = "Nenhum território no mapa",
                message = if (uiState.unmappedCount > 0) {
                    "${uiState.unmappedCount} território(s) ainda sem localização. Edite-os para marcar o pino ou o contorno."
                } else {
                    "Cadastre territórios e marque a localização para vê-los aqui."
                },
                modifier = Modifier.align(Alignment.Center)
            )
            uiState.visibleTerritories.isEmpty() -> MapMessageCard(
                icon = Icons.Default.SearchOff,
                title = "Nenhum território encontrado",
                message = "Nenhum território corresponde aos filtros atuais.",
                actionLabel = "Limpar filtros",
                onAction = viewModel::clearFilters,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = floatingControlsBottomPadding()),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (uiState.visibleTerritories.isNotEmpty()) {
                SmallFloatingActionButton(
                    onClick = { mapState.fitTo(uiState.visibleTerritories) },
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Icon(Icons.Default.ZoomOutMap, contentDescription = "Ver todos os territórios")
                }
            }
            FloatingActionButton(
                onClick = {
                    when {
                        !hasLocationPermission -> permissionLauncher.launch(LOCATION_PERMISSIONS)
                        !mapState.centerOnMyLocation() -> scope.launch {
                            snackbarHostState.showSnackbar("Procurando sua localização…")
                        }
                    }
                },
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = "Minha localização")
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = floatingControlsBottomPadding())
        )
    }
}

@Composable
private fun MapMessageCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: () -> Unit = {}
) {
    Surface(
        modifier = modifier.padding(32.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 6.dp
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (actionLabel != null) {
                TextButton(onClick = onAction) { Text(actionLabel) }
            }
        }
    }
}

private val LOCATION_PERMISSIONS = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION
)

private fun Context.hasLocationPermission(): Boolean = LOCATION_PERMISSIONS.any {
    ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
}
