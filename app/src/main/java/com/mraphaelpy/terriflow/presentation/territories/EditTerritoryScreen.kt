package com.mraphaelpy.terriflow.presentation.territories

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mraphaelpy.terriflow.core.util.CoordinateParser
import com.mraphaelpy.terriflow.core.util.MapColors
import com.mraphaelpy.terriflow.domain.model.LatLng
import com.mraphaelpy.terriflow.presentation.components.FormErrorText
import com.mraphaelpy.terriflow.presentation.components.FullScreenLoading
import com.mraphaelpy.terriflow.presentation.components.LoadingButton
import com.mraphaelpy.terriflow.presentation.map.openInGoogleMaps
import com.mraphaelpy.terriflow.presentation.map.statusColor
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.views.overlay.MapEventsOverlay

enum class MapMode { PIN, BOUNDARY }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTerritoryScreen(
    onSaved: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: EditTerritoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var code by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var coordInput by remember { mutableStateOf("") }
    var coordError by remember { mutableStateOf<String?>(null) }
    var mapExpanded by remember { mutableStateOf(false) }
    var mapMode by remember { mutableStateOf(MapMode.PIN) }
    var initialized by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isLoading) {
        if (!uiState.isLoading && !initialized) {
            code = uiState.code
            name = uiState.name
            description = uiState.description
            location = uiState.location
            notes = uiState.notes
            initialized = true
        }
    }

    LaunchedEffect(uiState.success) { if (uiState.success) onSaved() }

    if (mapExpanded) {
        // Tela cheia de edição do mapa
        FullMapEditor(
            context = context,
            uiState = uiState,
            mapMode = mapMode,
            onPinSet = { lat, lng -> 
                viewModel.updatePin(lat, lng)
                location = String.format(java.util.Locale.US, "%.6f, %.6f", lat, lng)
            },
            onBoundaryPoint = { viewModel.addBoundaryPoint(it) },
            onUpdateBoundaryPoint = { idx, pt -> viewModel.updateBoundaryPoint(idx, pt) },
            onUndoBoundary = { viewModel.removeLastBoundaryPoint() },
            onClearBoundary = { viewModel.clearBoundary() },
            onClose = { mapExpanded = false }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Editar território")
                        if (uiState.code.isNotEmpty()) {
                            Text(uiState.code,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            FullScreenLoading(modifier = Modifier.padding(padding))
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Campos de texto
            OutlinedTextField(value = code, onValueChange = { code = it },
                label = { Text("Número/Código") }, singleLine = true,
                placeholder = { Text("Ex: 14") },
                modifier = Modifier.fillMaxWidth())

            OutlinedTextField(value = name, onValueChange = { name = it },
                label = { Text("Nome *") }, singleLine = true,
                modifier = Modifier.fillMaxWidth())

            OutlinedTextField(value = description, onValueChange = { description = it },
                label = { Text("Descrição") }, minLines = 2, maxLines = 4,
                modifier = Modifier.fillMaxWidth())

            OutlinedTextField(value = location, onValueChange = { location = it },
                label = { Text("Bairro / Localização") }, singleLine = true,
                modifier = Modifier.fillMaxWidth())

            OutlinedTextField(value = notes, onValueChange = { notes = it },
                label = { Text("Observações") }, minLines = 2, maxLines = 4,
                modifier = Modifier.fillMaxWidth())

            HorizontalDivider()

            // Seção de localização no mapa
            Text("Localização no mapa", style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold)

            // Campo de colar coordenadas do Google Maps
            OutlinedTextField(
                value = coordInput,
                onValueChange = {
                    coordInput = it
                    coordError = null
                },
                label = { Text("Colar coordenadas do Google Maps") },
                placeholder = { Text("-23.5505, -46.6333") },
                supportingText = {
                    Text(
                        coordError ?: "Cole lat,lng copiado do Google Maps (toque longo no mapa → copiar coordenadas)",
                        color = if (coordError != null) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                isError = coordError != null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                trailingIcon = {
                    if (coordInput.isNotEmpty()) {
                        IconButton(onClick = {
                            val parsed = CoordinateParser.parse(coordInput)
                            if (parsed != null) {
                                viewModel.updatePin(parsed.lat, parsed.lng)
                                coordInput = "%.6f, %.6f".format(parsed.lat, parsed.lng)
                                coordError = null
                            } else {
                                coordError = "Formato inválido. Use: -23.5505, -46.6333"
                            }
                        }) {
                            Icon(Icons.Default.MyLocation, "Usar coordenadas")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            // Status da localização atual
            if (uiState.latitude != null && uiState.longitude != null) {
                Card(colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.LocationOn, null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp))
                                Text("Local de navegação definido", style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Medium)
                            }
                            Text("%.6f, %.6f".format(uiState.latitude, uiState.longitude),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (uiState.hasBoundary) {
                                Text("${uiState.boundaryPoints.size} pontos de contorno",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Row {
                            // Abrir no Google Maps
                            IconButton(onClick = {
                                openInGoogleMaps(context, uiState.latitude, uiState.longitude)
                            }) {
                                Icon(Icons.Default.OpenInNew, "Abrir no Google Maps",
                                    tint = MaterialTheme.colorScheme.primary)
                            }
                            // Limpar pino
                            IconButton(onClick = { viewModel.clearPin() }) {
                                Icon(Icons.Default.LocationOff, "Remover localização",
                                    tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }

            // Botões de ação do mapa
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { mapExpanded = true; mapMode = MapMode.PIN },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.AddLocation, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Navegação")
                }
                OutlinedButton(
                    onClick = { mapExpanded = true; mapMode = MapMode.BOUNDARY },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Hexagon, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Contorno")
                }
            }

            // Mini-mapa preview (somente leitura)
            val previewLat = uiState.latitude
            val previewLng = uiState.longitude
            if (previewLat != null && previewLng != null) {
                Text("Pré-visualização", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Card(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                    PreviewMap(
                        context = context,
                        lat = previewLat,
                        lng = previewLng,
                        boundaryPoints = uiState.boundaryPoints,
                        onExpand = { mapExpanded = true }
                    )
                }
            }

            uiState.error?.let { FormErrorText(message = it) }

            Spacer(Modifier.height(8.dp))

            LoadingButton(
                text = "Salvar alterações",
                isLoading = uiState.isSaving,
                onClick = { viewModel.save(code, name, description, location, notes) }
            )

            Spacer(Modifier.height(16.dp))
        }
    }
}

// ---------------------------------------------------------------------------
// Mapa full-screen para edição de pino e contorno
// ---------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FullMapEditor(
    context: Context,
    uiState: EditTerritoryUiState,
    mapMode: MapMode,
    onPinSet: (Double, Double) -> Unit,
    onBoundaryPoint: (LatLng) -> Unit,
    onUpdateBoundaryPoint: (Int, LatLng) -> Unit,
    onUndoBoundary: () -> Unit,
    onClearBoundary: () -> Unit,
    onClose: () -> Unit
) {
    val boundaryMarkers = remember { mutableListOf<Marker>() }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                Configuration.getInstance().userAgentValue = ctx.packageName
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    val startLat = uiState.latitude ?: -23.5505
                    val startLng = uiState.longitude ?: -46.6333
                    controller.setZoom(if (uiState.latitude != null) 17.0 else 12.0)
                    controller.setCenter(GeoPoint(startLat, startLng))

                    val eventsOverlay = MapEventsOverlay(object : MapEventsReceiver {
                        override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                            when (mapMode) {
                                MapMode.PIN -> {
                                    onPinSet(p.latitude, p.longitude)
                                }
                                MapMode.BOUNDARY -> {
                                    onBoundaryPoint(LatLng(p.latitude, p.longitude))
                                }
                            }
                            return true
                        }
                        override fun longPressHelper(p: GeoPoint): Boolean = false
                    })
                    overlays.add(0, eventsOverlay)
                }
            },
            update = { mapView ->
                org.osmdroid.views.overlay.infowindow.InfoWindow.closeAllInfoWindowsOn(mapView)
                // Remover todos os marcadores e polígonos
                mapView.overlays.removeAll { it is Marker || it is Polygon }
                boundaryMarkers.clear()
                
                // 1. Adicionar Polígono
                if (uiState.boundaryPoints.size >= 2) {
                    val poly = buildPolygon(uiState.boundaryPoints)
                    mapView.overlays.add(0, poly) // Adiciona por baixo dos marcadores
                }
                
                // 2. Adicionar Pino de Localização Apenas no Modo PIN
                val lat = uiState.latitude
                val lng = uiState.longitude
                if (mapMode == MapMode.PIN && lat != null && lng != null) {
                    val pinMarker = Marker(mapView).apply {
                        position = GeoPoint(lat, lng)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        title = "Ponto de Referência"
                        infoWindow = ModernInfoWindow(mapView)
                        isDraggable = true
                        setOnMarkerClickListener { m, _ ->
                            if (m.isInfoWindowOpen) m.closeInfoWindow() else m.showInfoWindow()
                            true
                        }
                        setOnMarkerDragListener(object : Marker.OnMarkerDragListener {
                            override fun onMarkerDragStart(marker: Marker) {
                                mapView.performHapticFeedback(
                                    android.view.HapticFeedbackConstants.LONG_PRESS,
                                    android.view.HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
                                )
                            }
                            override fun onMarkerDrag(marker: Marker) {}
                            override fun onMarkerDragEnd(marker: Marker) {
                                onPinSet(marker.position.latitude, marker.position.longitude)
                            }
                        })
                    }
                    mapView.overlays.add(pinMarker)
                }
                
                // 3. Adicionar Pinos de Contorno
                if (mapMode == MapMode.BOUNDARY) {
                    uiState.boundaryPoints.forEachIndexed { index, pt ->
                        val m = Marker(mapView).apply {
                            position = GeoPoint(pt.lat, pt.lng)
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                            title = "Ponto ${index + 1} (Arraste)"
                            isDraggable = true
                            icon = createBoundaryMarkerIcon(context)
                            infoWindow = ModernInfoWindow(mapView)
                            setOnMarkerClickListener { m, _ ->
                                if (m.isInfoWindowOpen) m.closeInfoWindow() else m.showInfoWindow()
                                true
                            }
                            setOnMarkerDragListener(object : Marker.OnMarkerDragListener {
                                override fun onMarkerDragStart(marker: Marker) {
                                    mapView.performHapticFeedback(
                                        android.view.HapticFeedbackConstants.LONG_PRESS,
                                        android.view.HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
                                    )
                                }
                                override fun onMarkerDrag(marker: Marker) {
                                    // Redesenhar polígono em tempo real durante o arraste
                                    val poly = mapView.overlays.filterIsInstance<Polygon>().firstOrNull()
                                    if (poly != null) {
                                        val pts = boundaryMarkers.map { GeoPoint(it.position.latitude, it.position.longitude) }.toMutableList()
                                        if (pts.isNotEmpty()) pts.add(pts.first())
                                        poly.points = pts
                                        mapView.invalidate()
                                    }
                                }
                                override fun onMarkerDragEnd(marker: Marker) {
                                    onUpdateBoundaryPoint(index, LatLng(marker.position.latitude, marker.position.longitude))
                                }
                            })
                        }
                        boundaryMarkers.add(m)
                        mapView.overlays.add(m)
                    }
                }
                
                mapView.invalidate()
            }
        )

        // Top-Left Back/Close Button
        SmallFloatingActionButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .windowInsetsPadding(WindowInsets.statusBars),
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar")
        }

        // Bottom Control Panel
        Surface(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 20.dp)
                    .windowInsetsPadding(WindowInsets.navigationBars),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (mapMode == MapMode.BOUNDARY) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val count = uiState.boundaryPoints.size
                        Text(
                            when {
                                count == 0 -> "Toque no mapa para adicionar pontos"
                                count < 3  -> "$count pontos — faltam ${3 - count}"
                                else       -> "$count pontos (Arraste para mover)"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row {
                            IconButton(onClick = onUndoBoundary, enabled = uiState.boundaryPoints.isNotEmpty()) {
                                Icon(Icons.AutoMirrored.Filled.Undo, "Desfazer")
                            }
                            IconButton(onClick = onClearBoundary, enabled = uiState.boundaryPoints.isNotEmpty()) {
                                Icon(Icons.Default.DeleteSweep, "Limpar",
                                    tint = if (uiState.boundaryPoints.isNotEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                } else {
                    Text(
                        "Toque no mapa para definir o ponto de destino.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Button(
                    onClick = onClose,
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Text("Confirmar")
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Mini-mapa pré-visualização (somente leitura, clicável para expandir)
// ---------------------------------------------------------------------------
@Composable
private fun PreviewMap(
    context: Context,
    lat: Double,
    lng: Double,
    boundaryPoints: List<LatLng>,
    onExpand: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                Configuration.getInstance().userAgentValue = ctx.packageName
                MapView(ctx).apply {
                    val mapRef = this
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(false)
                    controller.setZoom(16.0)
                    
                    // Centralizar preferencialmente no polígono, se houver
                    if (boundaryPoints.isNotEmpty()) {
                        val centerLat = boundaryPoints.map { it.lat }.average()
                        val centerLng = boundaryPoints.map { it.lng }.average()
                        controller.setCenter(GeoPoint(centerLat, centerLng))
                    } else {
                        controller.setCenter(GeoPoint(lat, lng))
                    }

                    if (boundaryPoints.size >= 3) {
                        overlays.add(buildPolygon(boundaryPoints))
                    }
                }
            },
            update = { mapView ->
                // Limpa overlays anteriores
                mapView.overlays.removeAll { it is Marker || it is Polygon }
                
                if (boundaryPoints.size >= 3) {
                    mapView.overlays.add(buildPolygon(boundaryPoints))
                }
                
                if (boundaryPoints.isNotEmpty()) {
                    val centerLat = boundaryPoints.map { it.lat }.average()
                    val centerLng = boundaryPoints.map { it.lng }.average()
                    mapView.controller.setCenter(GeoPoint(centerLat, centerLng))
                } else {
                    mapView.controller.setCenter(GeoPoint(lat, lng))
                }
                
                mapView.invalidate()
            }
        )

        // Botão expandir sobreposto
        SmallFloatingActionButton(
            onClick = onExpand,
            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Icon(Icons.Default.OpenInFull, "Expandir mapa", modifier = Modifier.size(18.dp))
        }
    }
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

private fun buildPolygon(points: List<LatLng>): Polygon {
    return Polygon().apply {
        val geoPoints = points.map { GeoPoint(it.lat, it.lng) }.toMutableList()
        if (geoPoints.isNotEmpty()) geoPoints.add(geoPoints.first()) // fechar
        this.points = geoPoints
        fillPaint.color = MapColors.TERRITORY_FILL
        outlinePaint.color = MapColors.OUTLINE
        outlinePaint.strokeWidth = 4f
    }
}

val EditTerritoryUiState.hasBoundary: Boolean get() = boundaryPoints.size >= 3

private fun createBoundaryMarkerIcon(context: Context): android.graphics.drawable.Drawable {
    val density = context.resources.displayMetrics.density
    val size = (40 * density).toInt() // Thumb-sized (40dp)
    val bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    
    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF2196F3.toInt() // Primary blue
    }
    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt() // White border
        style = Paint.Style.STROKE
        strokeWidth = 3f * density
    }
    
    val radius = size / 2f - (2f * density)
    canvas.drawCircle(size / 2f, size / 2f, radius, fillPaint)
    canvas.drawCircle(size / 2f, size / 2f, radius, strokePaint)
    
    return android.graphics.drawable.BitmapDrawable(context.resources, bitmap)
}

// Balão de texto customizado e bonito para o mapa
private class ModernInfoWindow(mapView: MapView) : org.osmdroid.views.overlay.infowindow.InfoWindow(
    createModernBubbleView(mapView.context), mapView
) {
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private val closeRunnable = Runnable { close() }

    override fun onOpen(item: Any?) {
        val marker = item as? Marker
        val titleView = mView.findViewById<android.widget.TextView>(android.R.id.text1)
        titleView?.text = marker?.title ?: ""
        
        mView.setOnClickListener { close() }
        
        handler.removeCallbacks(closeRunnable)
        handler.postDelayed(closeRunnable, 2500) // Fecha após 2.5 segundos
    }
    
    override fun onClose() {
        handler.removeCallbacks(closeRunnable)
    }

    companion object {
        fun createModernBubbleView(context: Context): android.view.View {
            val linearLayout = android.widget.LinearLayout(context)
            linearLayout.orientation = android.widget.LinearLayout.VERTICAL
            
            val density = context.resources.displayMetrics.density
            
            val drawable = android.graphics.drawable.GradientDrawable()
            drawable.setColor(0xFF2196F3.toInt()) // Azul
            drawable.cornerRadius = 16f * density
            
            linearLayout.background = drawable
            linearLayout.elevation = 6f * density
            
            val paddingH = (16f * density).toInt()
            val paddingV = (8f * density).toInt()
            linearLayout.setPadding(paddingH, paddingV, paddingH, paddingV)
            
            val textView = android.widget.TextView(context)
            textView.id = android.R.id.text1
            textView.setTextColor(0xFFFFFFFF.toInt()) // Branco
            textView.textSize = 14f
            textView.setTypeface(null, android.graphics.Typeface.BOLD)
            
            linearLayout.addView(textView)
            return linearLayout
        }
    }
}
