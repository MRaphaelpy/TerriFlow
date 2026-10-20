package com.mraphaelpy.terriflow.presentation.territories

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.mraphaelpy.terriflow.core.util.MapColors
import com.mraphaelpy.terriflow.domain.model.LatLng
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon

// ---------------------------------------------------------------------------
// Mapa full-screen para edição de pino e contorno
// ---------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FullMapEditor(
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
internal fun PreviewMap(
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
