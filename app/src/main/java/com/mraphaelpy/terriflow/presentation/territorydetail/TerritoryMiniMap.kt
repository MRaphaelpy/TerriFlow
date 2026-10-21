package com.mraphaelpy.terriflow.presentation.territorydetail

import com.mraphaelpy.terriflow.presentation.map.applyDarkTiles
import androidx.compose.ui.graphics.luminance
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import com.mraphaelpy.terriflow.core.report.MapImageSharer
import com.mraphaelpy.terriflow.domain.model.label
import com.mraphaelpy.terriflow.presentation.map.BlockLabelsOverlay
import com.mraphaelpy.terriflow.presentation.map.openInGoogleMaps
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon

@Composable
internal fun TerritoryMiniMap(
    lat: Double?,
    lng: Double?,
    label: String,
    color: Color,
    boundaryPoints: List<com.mraphaelpy.terriflow.domain.model.LatLng> = emptyList(),
    blockPolygons: List<List<com.mraphaelpy.terriflow.domain.model.LatLng>> = emptyList(),
    completedBlocks: Set<Int> = emptySet(),
    onViewMap: () -> Unit
) {
    val context = LocalContext.current
    val markerColor = color.toArgb()
    val fillColor = color.copy(alpha = 0.28f).toArgb()
    val completedFillColor = color.copy(alpha = 0.7f).toArgb()
    val outlineColor = color.toArgb()
    val hasLocation = lat != null && lng != null
    val hasBoundary = boundaryPoints.size >= 3
    val hasBlocks = blockPolygons.isNotEmpty()
    var showBlocks by remember { mutableStateOf(hasBlocks) }
    var initialZoomDone by remember { mutableStateOf(false) }
    var detailMapView by remember { mutableStateOf<MapView?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            detailMapView?.onPause()
            detailMapView?.onDetach()
        }
    }

    var lastDrawnKey by remember { mutableStateOf("") }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
            ) {
                val darkTiles = MaterialTheme.colorScheme.surface.luminance() < 0.5f
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        Configuration.getInstance().userAgentValue = ctx.packageName
                        MapView(ctx).apply {
                            applyDarkTiles(darkTiles)
                            detailMapView = this
                            setTileSource(TileSourceFactory.MAPNIK)
                            setMultiTouchControls(false)
                            isClickable = false
                            isFocusable = false
                            val centerLat = lat ?: boundaryPoints.firstOrNull()?.lat
                                ?: blockPolygons.firstOrNull()?.firstOrNull()?.lat ?: -23.5505
                            val centerLng = lng ?: boundaryPoints.firstOrNull()?.lng
                                ?: blockPolygons.firstOrNull()?.firstOrNull()?.lng ?: -46.6333
                            controller.setZoom(16.0)
                            controller.setCenter(GeoPoint(centerLat, centerLng))
                        }
                    },
                    update = { mapView ->
                        mapView.applyDarkTiles(darkTiles)
                        val currentKey = "$lat,$lng,$markerColor,$showBlocks,${boundaryPoints.size},${blockPolygons.size},${completedBlocks.sorted()}"
                        if (currentKey != lastDrawnKey) {
                            lastDrawnKey = currentKey
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

                            if (showBlocks && blockPolygons.isNotEmpty()) {
                                blockPolygons.forEachIndexed { index, block ->
                                    val poly = Polygon().apply {
                                        val pts = block.map { GeoPoint(it.lat, it.lng) }.toMutableList()
                                        if (pts.isNotEmpty()) pts.add(pts.first())
                                        points = pts
                                        fillPaint.color = if (index in completedBlocks) completedFillColor else fillColor
                                        outlinePaint.color = outlineColor
                                        outlinePaint.strokeWidth = 2f
                                    }
                                    mapView.overlays.add(0, poly)
                                }
                                mapView.overlays.add(
                                    blockPolygons.size,
                                    BlockLabelsOverlay(blockPolygons, context.resources.displayMetrics.density)
                                )
                            } else if (hasBoundary) {
                                val poly = Polygon().apply {
                                    val pts = boundaryPoints.map { GeoPoint(it.lat, it.lng) }.toMutableList()
                                    pts.add(pts.first())
                                    points = pts
                                    fillPaint.color = fillColor
                                    outlinePaint.color = outlineColor
                                    outlinePaint.strokeWidth = 3f
                                }
                                mapView.overlays.add(0, poly)
                            }

                            mapView.invalidate()
                        }

                        val zoomPoints: List<com.mraphaelpy.terriflow.domain.model.LatLng> = when {
                            showBlocks && blockPolygons.isNotEmpty() -> blockPolygons.flatten()
                            hasBoundary -> boundaryPoints
                            else -> emptyList()
                        }

                        if (!initialZoomDone) {
                            if (zoomPoints.size >= 2) {
                                initialZoomDone = true
                                val bbox = BoundingBox(
                                    zoomPoints.maxOf { it.lat },
                                    zoomPoints.maxOf { it.lng },
                                    zoomPoints.minOf { it.lat },
                                    zoomPoints.minOf { it.lng }
                                )
                                mapView.post { mapView.zoomToBoundingBox(bbox, false, 60) }
                            } else if (hasLocation && lat != null && lng != null) {
                                initialZoomDone = true
                                mapView.controller.setZoom(17.0)
                                mapView.controller.setCenter(GeoPoint(lat, lng))
                            }
                        }
                    }
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(onClick = onViewMap)
                )
            }

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
                    if (hasLocation || hasBoundary || hasBlocks) {
                        IconButton(
                            onClick = {
                                detailMapView?.let { mapView ->
                                    val subtitle = if (hasBlocks) {
                                        "${completedBlocks.count { it in blockPolygons.indices }} de ${blockPolygons.size} quadras feitas"
                                    } else {
                                        "Gerado pelo TerriFlow"
                                    }
                                    MapImageSharer.share(context, mapView, label, subtitle)
                                }
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Compartilhar mapa", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                        }
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
}
