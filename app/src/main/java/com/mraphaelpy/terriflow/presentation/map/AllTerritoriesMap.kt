package com.mraphaelpy.terriflow.presentation.map

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.mraphaelpy.terriflow.domain.model.GeoBounds
import com.mraphaelpy.terriflow.domain.model.LatLng
import com.mraphaelpy.terriflow.domain.model.Territory
import com.mraphaelpy.terriflow.domain.model.bounds
import com.mraphaelpy.terriflow.domain.model.center
import com.mraphaelpy.terriflow.domain.model.displayName
import com.mraphaelpy.terriflow.domain.model.label
import com.mraphaelpy.terriflow.ui.theme.LocalStatusColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.luminance
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon

@Stable
class TerritoryMapState {
    internal var mapView: MapView? = null
    internal var locationOverlay: GoogleMapsStyleLocationOverlay? = null
    internal var initialFitDone = false

    fun fitTo(territories: List<Territory>) {
        territories.bounds()?.let(::fitTo)
    }

    fun focus(territory: Territory) {
        val shape = listOf(territory).bounds() ?: return
        fitTo(shape, padding = 1.6f)
    }

    fun distanceFromMe(lat: Double, lng: Double): Float? {
        val fix = locationOverlay?.lastFix ?: return null
        val result = FloatArray(1)
        android.location.Location.distanceBetween(fix.latitude, fix.longitude, lat, lng, result)
        return result[0]
    }

    fun centerOnMyLocation(): Boolean {
        val map = mapView ?: return false
        val overlay = locationOverlay ?: return false
        val location = overlay.myLocation
        if (location != null) {
            map.controller.animateTo(location, MY_LOCATION_ZOOM, ANIMATION_MS)
            return true
        }
        overlay.runOnFirstFix {
            map.post { overlay.myLocation?.let { map.controller.animateTo(it, MY_LOCATION_ZOOM, ANIMATION_MS) } }
        }
        return false
    }

    private fun fitTo(bounds: GeoBounds, padding: Float = 1.25f, animate: Boolean = true) {
        val map = mapView ?: return
        val action = { moveCamera(map, bounds, padding, animate) }
        if (map.width == 0 || map.height == 0) map.addOnFirstLayoutListener { _, _, _, _, _ -> action() } else action()
    }

    internal fun showInstantly(territories: List<Territory>, highlighted: Territory?) {
        val target = (highlighted?.let { listOf(it) } ?: territories).bounds() ?: return
        fitTo(target, padding = if (highlighted != null) 1.6f else 1.25f, animate = false)
    }

    internal fun limitTo(area: GeoBounds) {
        val map = mapView ?: return
        val action = {
            val limit = area.toBox().increaseByScale(AREA_LIMIT_SCALE)
            map.setScrollableAreaLimitDouble(limit)
            val fitZoom = MapView.getTileSystem().getBoundingBoxZoom(limit, map.width, map.height)
            map.minZoomLevel = (fitZoom - 0.5).coerceIn(MIN_ZOOM, MAX_MIN_ZOOM)
        }
        if (map.width == 0 || map.height == 0) map.addOnFirstLayoutListener { _, _, _, _, _ -> action() } else action()
    }

    private fun moveCamera(map: MapView, bounds: GeoBounds, padding: Float, animate: Boolean) {
        val isSinglePoint = bounds.north == bounds.south && bounds.east == bounds.west
        if (isSinglePoint) {
            val point = GeoPoint(bounds.north, bounds.east)
            if (animate) {
                map.controller.animateTo(point, SINGLE_POINT_ZOOM, ANIMATION_MS)
            } else {
                map.controller.setZoom(SINGLE_POINT_ZOOM)
                map.controller.setCenter(point)
            }
        } else {
            val box = bounds.toBox().increaseByScale(padding)
            if (animate) map.zoomToBoundingBox(box, true) else jumpTo(map, box)
        }
    }

    private companion object {
        const val MY_LOCATION_ZOOM = 18.0
        const val SINGLE_POINT_ZOOM = 17.0
        const val ANIMATION_MS = 600L
        const val AREA_LIMIT_SCALE = 3f
        const val MIN_ZOOM = 4.0
        const val MAX_MIN_ZOOM = 14.0
    }
}

private fun GeoBounds.toBox() = BoundingBox(north, east, south, west)

private fun jumpTo(map: MapView, box: BoundingBox) {
    val zoom = MapView.getTileSystem().getBoundingBoxZoom(box, map.width, map.height)
        .coerceIn(map.minZoomLevel, map.maxZoomLevel)
    map.controller.setZoom(zoom)
    map.controller.setCenter(box.centerWithDateLine)
}

private data class RenderKey(
    val territory: Territory,
    val color: Color,
    val statusColor: Color,
    val highlighted: Boolean,
    val mine: Boolean
)

private class RenderedTerritory(
    val key: RenderKey,
    val polygons: List<Polygon>,
    val blockLabels: BlockLabelsOverlay?,
    val marker: Marker?
)

@Composable
fun AllTerritoriesMap(
    state: TerritoryMapState,
    territories: List<Territory>,
    colors: Map<String, Color>,
    onTerritoryClick: (String) -> Unit,
    allTerritories: List<Territory> = territories,
    modifier: Modifier = Modifier,
    highlightId: String? = null,
    currentUserId: String? = null,
    locationEnabled: Boolean = false
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnClick by rememberUpdatedState(onTerritoryClick)
    val rendered = remember { HashMap<String, RenderedTerritory>() }
    val iconCache = remember { HashMap<String, MarkerIcon>() }

    DisposableEffect(lifecycleOwner, locationEnabled) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    state.mapView?.onResume()
                    if (locationEnabled) state.locationOverlay?.enableMyLocation()
                }
                Lifecycle.Event.ON_PAUSE -> {
                    state.locationOverlay?.disableMyLocation()
                    state.mapView?.onPause()
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (locationEnabled && lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            state.locationOverlay?.enableMyLocation()
        }
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            state.locationOverlay?.disableMyLocation()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            state.mapView?.onDetach()
            state.mapView = null
            state.locationOverlay = null
        }
    }

    val statusColors = LocalStatusColors.current
    val darkTiles = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val areaStore = remember { MapAreaStore(context) }
    val area = remember(allTerritories) { allTerritories.bounds() }

    LaunchedEffect(area) {
        area ?: return@LaunchedEffect
        areaStore.save(area)
        state.limitTo(area)
    }

    LaunchedEffect(territories.isNotEmpty(), highlightId) {
        if (state.initialFitDone || territories.isEmpty()) return@LaunchedEffect
        state.initialFitDone = true
        state.showInstantly(territories, territories.firstOrNull { it.id == highlightId })
    }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { ctx ->
            Configuration.getInstance().userAgentValue = ctx.packageName
            MapView(ctx).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                isTilesScaledToDpi = true
                zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
                minZoomLevel = 4.0
                val savedArea = areaStore.load()
                if (savedArea != null) {
                    addOnFirstLayoutListener { _, _, _, _, _ ->
                        if (!state.initialFitDone) jumpTo(this, savedArea.toBox().increaseByScale(1.25f))
                    }
                    controller.setZoom(13.0)
                    controller.setCenter(GeoPoint((savedArea.north + savedArea.south) / 2, (savedArea.east + savedArea.west) / 2))
                } else {
                    controller.setZoom(12.0)
                    controller.setCenter(GeoPoint(-23.5505, -46.6333))
                }

                val overlay = GoogleMapsStyleLocationOverlay(ctx, this)
                overlays.add(overlay)
                state.locationOverlay = overlay
                state.mapView = this
            }
        },
        update = { mapView ->
            mapView.applyDarkTiles(darkTiles)
            val desired = territories.associate { territory ->
                territory.id to RenderKey(
                    territory = territory,
                    color = colors[territory.id] ?: statusColors[territory.status],
                    statusColor = statusColors[territory.status],
                    highlighted = territory.id == highlightId,
                    mine = currentUserId != null && territory.currentResponsibleId == currentUserId
                )
            }

            val stale = rendered.filter { (id, item) -> desired[id] != item.key }
            stale.forEach { (id, item) ->
                mapView.overlays.removeAll(item.polygons.toSet())
                item.blockLabels?.let { mapView.overlays.remove(it) }
                item.marker?.let { mapView.overlays.remove(it) }
                rendered.remove(id)
            }

            var added = false
            desired.forEach { (id, key) ->
                if (id in rendered) return@forEach
                added = true
                val polygons = buildPolygons(key) { currentOnClick(id) }
                polygons.forEach { mapView.overlays.add(0, it) }
                val blockLabels = key.territory.blockPolygons.takeIf { it.isNotEmpty() && (key.mine || key.highlighted) }?.let { blocks ->
                    BlockLabelsOverlay(blocks, mapView.resources.displayMetrics.density, minZoom = BLOCK_LABELS_MIN_ZOOM)
                }
                blockLabels?.let { labels ->
                    val belowMarkers = state.locationOverlay?.let { mapView.overlays.indexOf(it) }?.takeIf { it >= 0 } ?: mapView.overlays.size
                    mapView.overlays.add(belowMarkers, labels)
                }
                val marker = buildMarker(mapView, key, iconCache) { currentOnClick(id) }
                marker?.let { mapView.overlays.add(it) }
                rendered[id] = RenderedTerritory(key, polygons, blockLabels, marker)
            }

            if (stale.isNotEmpty() || added) mapView.invalidate()
        }
    )
}

private const val BLOCK_LABELS_MIN_ZOOM = 16.0

private fun buildPolygons(key: RenderKey, onClick: () -> Unit): List<Polygon> {
    val territory = key.territory
    val shapes = when {
        territory.hasBlocks -> territory.blockPolygons.filter { it.size >= 3 }
        territory.hasBoundary -> listOf(territory.boundaryPoints)
        else -> emptyList()
    }
    val color = key.color
    val fillAlpha = when {
        key.highlighted -> 0.45f
        key.mine -> 0.32f
        else -> 0.2f
    }
    val strokeWidth = when {
        key.highlighted -> 6f
        key.mine -> 4f
        else -> 2.5f
    }
    return shapes.map { shape ->
        Polygon().apply {
            points = shape.toClosedGeoPoints()
            fillPaint.color = color.copy(alpha = fillAlpha).toArgb()
            outlinePaint.color = color.copy(alpha = if (key.highlighted) 1f else 0.85f).toArgb()
            outlinePaint.strokeWidth = strokeWidth
            infoWindow = null
            setOnClickListener { _, _, _ -> onClick(); true }
        }
    }
}

private fun buildMarker(
    mapView: MapView,
    key: RenderKey,
    iconCache: MutableMap<String, MarkerIcon>,
    onClick: () -> Unit
): Marker? {
    val territory = key.territory
    val center = territory.center ?: return null
    val owner = territory.currentResponsibleName?.let { if (key.mine) "Você" else shortPersonName(it) }
    val label = markerLabel(territory.code)
    val iconKey = listOf(key.color.toArgb(), territory.status, label, owner, key.highlighted).joinToString("|")
    val icon = iconCache.getOrPut(iconKey) {
        createTerritoryMarkerIcon(
            context = mapView.context,
            color = key.color.toArgb(),
            statusColor = key.statusColor.toArgb(),
            label = label,
            ownerLabel = owner,
            highlighted = key.highlighted
        )
    }
    return Marker(mapView).apply {
        position = GeoPoint(center.lat, center.lng)
        setAnchor(Marker.ANCHOR_CENTER, icon.anchorY)
        title = territory.displayName
        snippet = territory.status.label()
        this.icon = icon.drawable
        infoWindow = null
        setOnMarkerClickListener { _, _ -> onClick(); true }
    }
}

private fun List<LatLng>.toClosedGeoPoints(): List<GeoPoint> {
    val points = map { GeoPoint(it.lat, it.lng) }
    return if (points.first() == points.last()) points else points + points.first()
}
