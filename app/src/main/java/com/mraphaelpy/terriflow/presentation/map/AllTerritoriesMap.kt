package com.mraphaelpy.terriflow.presentation.map

import android.content.Context
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import com.mraphaelpy.terriflow.domain.model.Territory
import com.mraphaelpy.terriflow.domain.model.label
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.compass.CompassOverlay
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider
import org.osmdroid.views.overlay.infowindow.InfoWindow

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

                val myLocOverlay = GoogleMapsStyleLocationOverlay(ctx, this).apply {
                    enableMyLocation()
                    enableFollowLocation()
                }
                overlays.add(myLocOverlay)
                locationOverlay = myLocOverlay

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
