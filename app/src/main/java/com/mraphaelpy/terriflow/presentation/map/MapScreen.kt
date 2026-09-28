package com.mraphaelpy.terriflow.presentation.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Point
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.net.Uri
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mraphaelpy.terriflow.domain.model.Territory
import com.mraphaelpy.terriflow.domain.model.TerritoryStatus
import com.mraphaelpy.terriflow.domain.model.label
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.Projection
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.compass.CompassOverlay
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider
import org.osmdroid.views.overlay.infowindow.InfoWindow
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import android.graphics.Color as AndroidColor

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

@Composable
fun AllTerritoriesMap(
    context: Context,
    territories: List<Territory>,
    onMarkerClick: (String) -> Unit,
    highlightId: String? = null,
    currentUserId: String? = null,
    onMapReady: (MapView) -> Unit = {}
) {
    val markersMap = remember { HashMap<String, Marker>() }
    val polygonsMap = remember { HashMap<String, MutableList<Polygon>>() }
    val iconCache = remember { HashMap<String, android.graphics.drawable.Drawable>() }
    var centeredOnHighlight by remember { mutableStateOf(false) }
    var centeredInitial by remember { mutableStateOf(false) }
    var locationOverlay by remember { mutableStateOf<GoogleMapsStyleLocationOverlay?>(null) }
    var compassOverlay by remember { mutableStateOf<CompassOverlay?>(null) }
    var internalMapView by remember { mutableStateOf<MapView?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            locationOverlay?.disableMyLocation()
            compassOverlay?.disableCompass()
            internalMapView?.onPause()
            internalMapView?.onDetach()
        }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            Configuration.getInstance().userAgentValue = ctx.packageName
            MapView(ctx).apply {
                internalMapView = this
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                controller.setZoom(12.0)
                controller.setCenter(GeoPoint(-23.5505, -46.6333))

                // Overlay de localização ao vivo estilo Google Maps (ponto azul + cone direcional)
                val myLocOverlay = GoogleMapsStyleLocationOverlay(ctx, this).apply {
                    enableMyLocation()       // ponto azul ao vivo + bússola
                    enableFollowLocation()   // centraliza automaticamente no início
                }
                overlays.add(myLocOverlay)
                locationOverlay = myLocOverlay

                // Compass overlay (bússola no canto superior direito)
                val compass = CompassOverlay(ctx, InternalCompassOrientationProvider(ctx), this)
                compass.enableCompass()
                overlays.add(compass)
                compassOverlay = compass

                onMapReady(this)
            }
        },
        update = { mapView ->
            val currentIds = territories.map { it.id }.toSet()

            markersMap.keys.filter { it !in currentIds }.forEach { id ->
                markersMap[id]?.let { mapView.overlays.remove(it) }
                markersMap.remove(id)
            }
            polygonsMap.keys.filter { it !in currentIds }.forEach { id ->
                polygonsMap[id]?.forEach { mapView.overlays.remove(it) }
                polygonsMap.remove(id)
            }

            territories.forEach { territory ->
                val isHighlighted = territory.id == highlightId
                val isMine = territory.currentResponsibleId == currentUserId && currentUserId != null
                val tColor = territoryColor(territory.id)
                val color = tColor.toArgb()

                val fillAlpha = when {
                    isHighlighted -> 0.45f
                    isMine        -> 0.32f
                    else          -> 0.20f
                }
                val strokeAlpha = when {
                    isHighlighted -> 255
                    isMine        -> 220
                    else          -> 180
                }
                val strokeWidth = when {
                    isHighlighted -> 5f
                    isMine        -> 3.5f
                    else          -> 2.5f
                }

                fun makePolyPaint(points: List<org.osmdroid.util.GeoPoint>): Polygon = Polygon().apply {
                    this.points = points.toMutableList()
                    val c = tColor.copy(alpha = fillAlpha)
                    fillPaint.color = android.graphics.Color.argb(
                        (c.alpha * 255).toInt(),
                        (c.red * 255).toInt(),
                        (c.green * 255).toInt(),
                        (c.blue * 255).toInt()
                    )
                    outlinePaint.color = android.graphics.Color.argb(
                        strokeAlpha,
                        android.graphics.Color.red(color),
                        android.graphics.Color.green(color),
                        android.graphics.Color.blue(color)
                    )
                    outlinePaint.strokeWidth = strokeWidth
                    setOnClickListener { _, _, _ ->
                        InfoWindow.closeAllInfoWindowsOn(mapView)
                        onMarkerClick(territory.id)
                        true
                    }
                }

                polygonsMap[territory.id]?.forEach { mapView.overlays.remove(it) }
                val newPolys = mutableListOf<Polygon>()

                when {
                    territory.blockPolygons.isNotEmpty() -> {
                        territory.blockPolygons.forEach { block ->
                            if (block.size >= 3) {
                                val pts = block.map { GeoPoint(it.lat, it.lng) }.toMutableList()
                                pts.add(pts.first())
                                val poly = makePolyPaint(pts)
                                newPolys.add(poly)
                                mapView.overlays.add(0, poly)
                            }
                        }
                    }
                    territory.boundaryPoints.size >= 3 -> {
                        val pts = territory.boundaryPoints.map { GeoPoint(it.lat, it.lng) }.toMutableList()
                        pts.add(pts.first())
                        val poly = makePolyPaint(pts)
                        newPolys.add(poly)
                        mapView.overlays.add(0, poly)
                    }
                }

                polygonsMap[territory.id] = newPolys

                val lat = territory.latitude
                    ?: if (territory.blockPolygons.isNotEmpty())
                        territory.blockPolygons.flatten().map { it.lat }.average()
                    else null
                val lng = territory.longitude
                    ?: if (territory.blockPolygons.isNotEmpty())
                        territory.blockPolygons.flatten().map { it.lng }.average()
                    else null

                val snippetText = buildString {
                    append(territory.status.label())
                    territory.currentResponsibleName?.let { append(" · $it") }
                }

                if (lat != null && lng != null) {
                    val geoPoint = GeoPoint(lat, lng)
                    val iconKey = "${territory.id}_${territory.code}_${territory.status.name}_${color}_${territory.currentResponsibleName.orEmpty()}_${isHighlighted}_$isMine"
                    val markerIcon = iconCache.getOrPut(iconKey) {
                        when {
                            isHighlighted -> createStatusMarkerIcon(context, color, territory.code, territory.currentResponsibleName, scale = 1.4f, showStar = true)
                            isMine        -> createStatusMarkerIcon(context, color, territory.code, territory.currentResponsibleName, scale = 1.2f, showStar = false)
                            else          -> createStatusMarkerIcon(context, color, territory.code, territory.currentResponsibleName, scale = 1.0f, showStar = false)
                        }
                    }

                    val existing = markersMap[territory.id]
                    if (existing != null) {
                        existing.position = geoPoint
                        existing.title = "${territory.code} — ${territory.name}"
                        existing.snippet = snippetText
                        existing.icon = markerIcon
                    } else {
                        val marker = Marker(mapView).apply {
                            position = geoPoint
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            title = "${territory.code} — ${territory.name}"
                            snippet = snippetText
                            icon = markerIcon
                            setOnMarkerClickListener { _, _ ->
                                InfoWindow.closeAllInfoWindowsOn(mapView)
                                onMarkerClick(territory.id)
                                true
                            }
                        }
                        markersMap[territory.id] = marker
                        mapView.overlays.add(marker)
                    }
                } else {
                    markersMap[territory.id]?.let { mapView.overlays.remove(it) }
                    markersMap.remove(territory.id)
                }
            }

            if (highlightId != null && !centeredOnHighlight) {
                val t = territories.find { it.id == highlightId }
                if (t != null) {
                    centeredOnHighlight = true
                    val allPts = t.blockPolygons.flatten()
                    val lat = t.latitude ?: t.boundaryPoints.firstOrNull()?.lat ?: allPts.firstOrNull()?.lat
                    val lng = t.longitude ?: t.boundaryPoints.firstOrNull()?.lng ?: allPts.firstOrNull()?.lng
                    if (lat != null && lng != null) {
                        if (allPts.size >= 2) {
                            val bbox = BoundingBox(allPts.maxOf { it.lat }, allPts.maxOf { it.lng }, allPts.minOf { it.lat }, allPts.minOf { it.lng })
                            mapView.post { mapView.zoomToBoundingBox(bbox.increaseByScale(1.4f), true) }
                        } else {
                            mapView.controller.animateTo(GeoPoint(lat, lng))
                            mapView.controller.setZoom(17.0)
                        }
                    }
                }
            } else if (highlightId == null && !centeredInitial) {
                val allLats = territories.mapNotNull { it.latitude } +
                    territories.flatMap { it.boundaryPoints.map { bp -> bp.lat } } +
                    territories.flatMap { t -> t.blockPolygons.flatten().map { it.lat } }
                val allLngs = territories.mapNotNull { it.longitude } +
                    territories.flatMap { it.boundaryPoints.map { bp -> bp.lng } } +
                    territories.flatMap { t -> t.blockPolygons.flatten().map { it.lng } }
                if (allLats.isNotEmpty() && allLngs.isNotEmpty()) {
                    centeredInitial = true
                    runCatching {
                        val box = BoundingBox(allLats.max(), allLngs.max(), allLats.min(), allLngs.min())
                        mapView.post { mapView.zoomToBoundingBox(box.increaseByScale(1.3f), true) }
                    }
                }
            }

            mapView.invalidate()
        }
    )
}

fun statusColor(status: TerritoryStatus): Color = when (status) {
    TerritoryStatus.AVAILABLE   -> Color(0xFF4CAF50)
    TerritoryStatus.ASSIGNED    -> Color(0xFF2196F3)
    TerritoryStatus.IN_PROGRESS -> Color(0xFFFF9800)
    TerritoryStatus.PAUSED      -> Color(0xFF9E9E9E)
    TerritoryStatus.COMPLETED   -> Color(0xFF8BC34A)
    TerritoryStatus.RETURNED    -> Color(0xFFF44336)
    TerritoryStatus.ARCHIVED    -> Color(0xFF607D8B)
}

private fun statusColorInt(status: TerritoryStatus): Int = statusColor(status).toArgb()

fun territoryColor(territoryId: String): Color {
    val h = territoryId.hashCode()
    val hue        = ((h and 0x7FFFFFFF) % 360).toFloat()
    val saturation = 0.55f + ((h ushr 8 and 0xFF) % 30) / 100f
    val lightness  = 0.36f + ((h ushr 16 and 0xFF) % 18) / 100f
    return Color.hsl(hue, saturation, lightness)
}

private fun createStatusMarkerIcon(
    context: Context,
    color: Int,
    code: String,
    responsibleName: String? = null,
    scale: Float = 1.0f,
    showStar: Boolean = false
): android.graphics.drawable.Drawable {
    val baseW = 140
    val hasResponsible = !responsibleName.isNullOrEmpty()
    val baseH = if (hasResponsible) 175 else 140
    val w = (baseW * scale).toInt()
    val h = (baseH * scale).toInt()
    val bodyH = ((h - 35 * scale)).toFloat()

    val bitmap = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val strokeWidth = 5f * scale
    val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = 0xFFFFFFFF.toInt()
        style = Paint.Style.STROKE
        this.strokeWidth = strokeWidth
    }
    val codePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = 0xFFFFFFFF.toInt()
        textSize = 26f * scale
        typeface = android.graphics.Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }
    val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = 0xFFFFFFFF.toInt()
        textSize = 18f * scale
        textAlign = Paint.Align.CENTER
    }

    if (showStar) {
        val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = 0xFFFFD700.toInt()
            style = Paint.Style.STROKE
            this.strokeWidth = 6f * scale
        }
        val rect2 = android.graphics.RectF(2f, 2f, w - 2f, bodyH + 2f)
        canvas.drawRoundRect(rect2, 22f, 22f, highlightPaint)
    }

    val rect = android.graphics.RectF(4f * scale, 4f * scale, w - 4f * scale, bodyH)
    canvas.drawRoundRect(rect, 20f * scale, 20f * scale, bodyPaint)
    canvas.drawRoundRect(rect, 20f * scale, 20f * scale, strokePaint)

    val ptrW = 14f * scale
    val triPath = android.graphics.Path().apply {
        moveTo(w / 2f - ptrW, bodyH)
        lineTo(w / 2f + ptrW, bodyH)
        lineTo(w / 2f, h.toFloat() - 4f * scale)
        close()
    }
    canvas.drawPath(triPath, bodyPaint)

    val shortCode = code.takeLast(3).trimStart('0').ifEmpty { "0" }
    if (hasResponsible) {
        canvas.drawText(shortCode, w / 2f, bodyH / 2f - 4f * scale, codePaint)
        canvas.drawLine(
            16f * scale, bodyH / 2f + 8f * scale,
            w - 16f * scale, bodyH / 2f + 8f * scale,
            strokePaint.apply { this.strokeWidth = 1f * scale }
        )
        val initials = responsibleName.split(" ")
            .take(2).joinToString(" ") { it.take(1).uppercase() + it.drop(1).take(4) }
        canvas.drawText(initials, w / 2f, bodyH - 10f * scale, namePaint)
    } else {
        canvas.drawText(shortCode, w / 2f, bodyH / 2f + codePaint.textSize / 3f, codePaint)
    }

    if (showStar) {
        val starPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = 0xFFFFD700.toInt() }
        canvas.drawCircle(w - 14f * scale, 14f * scale, 10f * scale, starPaint)
        val textPaint2 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = 0xFF333333.toInt()
            textSize = 14f * scale
            textAlign = Paint.Align.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        canvas.drawText("★", w - 14f * scale, 14f * scale + textPaint2.textSize / 3f, textPaint2)
    }

    return bitmap.toDrawable(context.resources)
}

@SuppressLint("QueryPermissionsNeeded")
fun openInGoogleMaps(context: Context, lat: Double?, lng: Double?, label: String = "") {
    if (lat == null || lng == null) return
    val encodedLabel = Uri.encode(label)
    val uri = "geo:$lat,$lng?q=$lat,$lng($encodedLabel)".toUri()
    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
        setPackage("com.google.android.apps.maps")
    }
    if (intent.resolveActivity(context.packageManager) != null) {
        context.startActivity(intent)
    } else {
        val webUri = "https://maps.google.com/?q=$lat,$lng".toUri()
        context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
    }
}

/**
 * Overlay de localização ao vivo estilo Google Maps:
 * - Ponto central azul vibrante com contorno branco e leve sombra
 * - Círculo de precisão do GPS (halo suave)
 * - Cone de visão direcional (feixe que aponta para a direção em que o usuário está olhando via bússola do aparelho)
 */
class GoogleMapsStyleLocationOverlay(
    context: Context,
    mapView: MapView
) : org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay(GpsMyLocationProvider(context), mapView), SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val rotationSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        ?: sensorManager?.getDefaultSensor(Sensor.TYPE_ORIENTATION)

    private var currentHeading: Float = 0f
    private var lastHeadingTime: Long = 0L
    private val rotationMatrix = FloatArray(9)
    private val orientationVals = FloatArray(3)

    private val accuracyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.argb(35, 66, 133, 244)
        style = Paint.Style.FILL
    }
    private val accuracyBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.argb(90, 66, 133, 244)
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }
    private val whiteHaloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.WHITE
        style = Paint.Style.FILL
        setShadowLayer(8f, 0f, 2f, AndroidColor.argb(70, 0, 0, 0))
    }
    private val blueDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.rgb(66, 133, 244)
        style = Paint.Style.FILL
    }

    override fun enableMyLocation(): Boolean {
        rotationSensor?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        return super.enableMyLocation()
    }

    override fun disableMyLocation() {
        sensorManager?.unregisterListener(this)
        super.disableMyLocation()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        val now = System.currentTimeMillis()
        if (now - lastHeadingTime < 80) return

        val newHeading: Float
        if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            SensorManager.getOrientation(rotationMatrix, orientationVals)
            val azimuth = Math.toDegrees(orientationVals[0].toDouble()).toFloat()
            newHeading = (azimuth + 360f) % 360f
        } else if (event.sensor.type == Sensor.TYPE_ORIENTATION) {
            newHeading = (event.values[0] + 360f) % 360f
        } else {
            return
        }

        if (kotlin.math.abs(newHeading - currentHeading) > 2.0f) {
            currentHeading = newHeading
            lastHeadingTime = now
            mMapView.postInvalidate()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun drawMyLocation(canvas: Canvas, pj: Projection, lastFix: Location) {
        val screenPt = Point()
        pj.toPixels(GeoPoint(lastFix.latitude, lastFix.longitude), screenPt)
        val cx = screenPt.x.toFloat()
        val cy = screenPt.y.toFloat()

        // 1. Círculo de precisão do GPS (halo suave azul)
        if (lastFix.hasAccuracy() && lastFix.accuracy > 0) {
            val radiusPx = pj.metersToEquatorPixels(lastFix.accuracy)
            if (radiusPx > 12f && radiusPx < 1200f) {
                canvas.drawCircle(cx, cy, radiusPx, accuracyPaint)
                canvas.drawCircle(cx, cy, radiusPx, accuracyBorderPaint)
            }
        }

        // 2. Cone de visão direcional (feixe estilo Google Maps)
        val heading = if (currentHeading != 0f) currentHeading else if (lastFix.hasBearing()) lastFix.bearing else 0f
        val coneRadius = 90f
        val sweepAngle = 55f
        val startAngle = heading - (sweepAngle / 2f) - 90f

        val conePath = Path().apply {
            moveTo(cx, cy)
            arcTo(
                RectF(cx - coneRadius, cy - coneRadius, cx + coneRadius, cy + coneRadius),
                startAngle,
                sweepAngle
            )
            close()
        }

        val conePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                cx, cy, coneRadius,
                intArrayOf(
                    AndroidColor.argb(140, 66, 133, 244),
                    AndroidColor.argb(45, 66, 133, 244),
                    AndroidColor.TRANSPARENT
                ),
                floatArrayOf(0f, 0.60f, 1f),
                Shader.TileMode.CLAMP
            )
            style = Paint.Style.FILL
        }
        canvas.drawPath(conePath, conePaint)

        // 3. Ponto branco externo com sombra suave
        canvas.drawCircle(cx, cy, 18f, whiteHaloPaint)

        // 4. Ponto azul central (Google Maps)
        canvas.drawCircle(cx, cy, 12f, blueDotPaint)
    }
}
