package com.mraphaelpy.terriflow.presentation.map

import android.Manifest
import android.content.pm.PackageManager
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.PersonPin
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mraphaelpy.terriflow.domain.model.Territory
import com.mraphaelpy.terriflow.domain.model.TerritoryStatus
import com.mraphaelpy.terriflow.domain.model.label
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

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
    var selectedStatus by remember { mutableStateOf<TerritoryStatus?>(null) }
    var selectedTerritory by remember { mutableStateOf<Territory?>(null) }
    var onlyMine by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    LaunchedEffect(Unit) {
        val fineGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (!fineGranted && !coarseGranted) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    LaunchedEffect(highlightId, uiState.territoriesWithLocation) {
        if (highlightId != null && selectedTerritory == null) {
            val t = uiState.territoriesWithLocation.find { it.id == highlightId }
            if (t != null) selectedTerritory = t
        }
    }

    val filtered = remember(uiState.territoriesWithLocation, selectedStatus, onlyMine, uiState.currentUserId) {
        uiState.territoriesWithLocation
            .let { list -> if (onlyMine && uiState.currentUserId != null)
                list.filter { it.currentResponsibleId == uiState.currentUserId } else list }
            .let { list -> if (selectedStatus != null) list.filter { it.status == selectedStatus } else list }
    }

    val myTerritories = remember(uiState.territoriesWithLocation, uiState.currentUserId) {
        uiState.territoriesWithLocation.filter { it.currentResponsibleId == uiState.currentUserId }
    }

    if (selectedTerritory != null) {
        val territory = selectedTerritory!!
        ModalBottomSheet(
            onDismissRequest = { selectedTerritory = null }
        ) {
            TerritoryMapCard(
                territory = territory,
                onOpen = {
                    selectedTerritory = null
                    if (highlightId == territory.id) {
                        onNavigateBack()
                    } else {
                        onNavigateToTerritory(territory.id)
                    }
                },
                onDismiss = { selectedTerritory = null }
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(if (onlyMine) "Meus territórios" else "Mapa de territórios")
                        Text(
                            "${filtered.size} de ${uiState.territoriesWithLocation.size} mapeados",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },

                actions = {
                    if (myTerritories.isNotEmpty()) {
                        IconButton(onClick = { onlyMine = !onlyMine }) {
                            Icon(
                                imageVector = if (onlyMine) Icons.Default.PersonPin else Icons.Default.People,
                                contentDescription = if (onlyMine) "Ver todos" else "Ver meus territórios",
                                tint = if (onlyMine) MaterialTheme.colorScheme.primary
                                       else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        var mapViewRef by remember { mutableStateOf<MapView?>(null) }

        Box(modifier = Modifier.fillMaxSize().padding(padding)) {

            AllTerritoriesMap(
                context = context,
                territories = filtered,
                highlightId = highlightId,
                currentUserId = uiState.currentUserId,
                onMapReady = { mapViewRef = it },
                onMarkerClick = { territoryId ->
                    selectedTerritory = uiState.territoriesWithLocation.find { it.id == territoryId }
                }
            )

            Column(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    shadowElevation = 4.dp
                ) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = selectedStatus == null,
                                onClick = { selectedStatus = null },
                                label = { Text("Todos (${uiState.territoriesWithLocation.size})") }
                            )
                        }
                        items(TerritoryStatus.entries) { status ->
                            val count = uiState.territoriesWithLocation.count { it.status == status }
                            if (count > 0) {
                                FilterChip(
                                    selected = selectedStatus == status,
                                    onClick = { selectedStatus = if (selectedStatus == status) null else status },
                                    label = { Text("${status.label()} ($count)") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = statusColor(status).copy(alpha = 0.3f)
                                    )
                                )
                            }
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.align(Alignment.BottomStart).padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 100.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                )
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Status", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    TerritoryStatus.entries.forEach { status ->
                        val count = uiState.territoriesWithLocation.count { it.status == status }
                        if (count > 0) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    modifier = Modifier.size(10.dp),
                                    shape = MaterialTheme.shapes.extraSmall,
                                    color = statusColor(status)
                                ) {}
                                Text("${status.label()} ($count)", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            if (uiState.territoriesWithLocation.isEmpty()) {
                Card(
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.MyLocation, contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Nenhum território com localização", fontWeight = FontWeight.Medium)
                        Text(
                            "Edite os territórios e marque a localização no mapa.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // FAB: centralizar na minha localização
            FloatingActionButton(
                onClick = {
                    mapViewRef?.let { mv ->
                        val myOverlay = mv.overlays
                            .filterIsInstance<MyLocationNewOverlay>()
                            .firstOrNull()
                        val loc = myOverlay?.myLocation
                        if (loc != null) {
                            mv.controller.animateTo(loc)
                            mv.controller.setZoom(18.0)
                        }
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 100.dp),
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(
                    Icons.Default.MyLocation,
                    contentDescription = "Minha localização",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TerritoryMapCard(
    territory: Territory,
    onOpen: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .background(territoryColor(territory.id), CircleShape)
                )
                Column {
                    Text(
                        territory.code,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        territory.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2
                    )
                }
            }
            Surface(
                shape = MaterialTheme.shapes.small,
                color = statusColor(territory.status).copy(alpha = 0.15f)
            ) {
                Text(
                    territory.status.label(),
                    style = MaterialTheme.typography.labelMedium,
                    color = statusColor(territory.status),
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        HorizontalDivider()
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = if (territory.currentResponsibleName != null) Icons.Default.Person else Icons.Default.PersonOff,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (territory.currentResponsibleName != null)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
            Column {
                Text(
                    "Responsável",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    territory.currentResponsibleName ?: "Sem responsável",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (territory.currentResponsibleName != null) FontWeight.Medium else FontWeight.Normal,
                    color = if (territory.currentResponsibleName != null)
                        MaterialTheme.colorScheme.onSurface
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (territory.location.isNotEmpty()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.LocationOn, contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(territory.location,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { 
                    val destLat = territory.latitude ?: territory.boundaryPoints.firstOrNull()?.lat
                    val destLng = territory.longitude ?: territory.boundaryPoints.firstOrNull()?.lng
                    openInGoogleMaps(context, destLat, destLng, territory.name)
                },
                modifier = Modifier.weight(1f),
                enabled = territory.hasLocation || territory.hasBoundary
            ) {
                Icon(Icons.Default.Map, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Google Maps")
            }
            Button(
                onClick = onOpen,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.OpenInFull, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Abrir")
            }
        }
    }
}
