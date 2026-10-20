package com.mraphaelpy.terriflow.domain.model

data class GeoBounds(val north: Double, val east: Double, val south: Double, val west: Double) {
    companion object {
        fun of(points: List<LatLng>): GeoBounds? {
            if (points.isEmpty()) return null
            return GeoBounds(
                north = points.maxOf { it.lat },
                east = points.maxOf { it.lng },
                south = points.minOf { it.lat },
                west = points.minOf { it.lng }
            )
        }
    }
}

val Territory.isMapped: Boolean get() = hasLocation || hasBoundary || hasBlocks

val Territory.shapePoints: List<LatLng>
    get() = when {
        hasBlocks -> blockPolygons.flatten()
        hasBoundary -> boundaryPoints
        else -> emptyList()
    }

val Territory.center: LatLng?
    get() {
        if (latitude != null && longitude != null) return LatLng(latitude, longitude)
        val points = shapePoints
        if (points.isEmpty()) return null
        return LatLng(points.map { it.lat }.average(), points.map { it.lng }.average())
    }

val Territory.mapPoints: List<LatLng> get() = shapePoints + listOfNotNull(center)

fun List<Territory>.bounds(): GeoBounds? = GeoBounds.of(flatMap { it.mapPoints })

fun polygonCentroid(points: List<LatLng>): LatLng? {
    val ring = if (points.size > 1 && points.first() == points.last()) points.dropLast(1) else points
    if (ring.isEmpty()) return null
    var area = 0.0
    var cx = 0.0
    var cy = 0.0
    ring.indices.forEach { i ->
        val a = ring[i]
        val b = ring[(i + 1) % ring.size]
        val cross = a.lng * b.lat - b.lng * a.lat
        area += cross
        cx += (a.lng + b.lng) * cross
        cy += (a.lat + b.lat) * cross
    }
    if (kotlin.math.abs(area) < 1e-18) {
        return LatLng(ring.map { it.lat }.average(), ring.map { it.lng }.average())
    }
    area *= 0.5
    return LatLng(lat = cy / (6 * area), lng = cx / (6 * area))
}
