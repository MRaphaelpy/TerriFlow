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
